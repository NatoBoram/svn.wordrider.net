package cz.vity.freerapid.plugins.services.depositfiles_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class DepositFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DepositFilesFileRunner.class.getName());

    private void setLanguageEN() {
        setCookieOnAllDomains("lang_current", "en");
        fileURL = fileURL.replaceFirst("/[^/]{2}/(files|folders)/", "/$1/"); // remove language id from URL
    }

    private void setCookieOnAllDomains(final String name, final String value) {
        addCookie(new Cookie(".depositfiles.com", name, value, "/", 86400, false));
        addCookie(new Cookie(".depositfiles.org", name, value, "/", 86400, false));
        addCookie(new Cookie(".dfiles.eu", name, value, "/", 86400, false));
        addCookie(new Cookie(".dfiles.ru", name, value, "/", 86400, false));
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setLanguageEN();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (!isFolder()) checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        setLanguageEN();
        logger.info("Starting download in TASK " + fileURL);

        if (isFolder()) {
            runFolder();
            httpFile.getProperties().put("removeCompleted", true);
            return;
        }

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            fileURL = method.getURI().toString();

            if (getContentAsString().contains("Advantages of the Gold account"))
                throw new BadLoginException("Problem logging in, account not premium?");

            while (getContentAsString().contains("password_check")) {
                final String password = getDialogSupport().askForPassword("Enter Password:");
                if (password == null) throw new NotRecoverableDownloadException("File is password protected");
                HttpMethod hMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("file_password", password).toPostMethod();
                if (!makeRedirectedRequest(hMethod)) {
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            final Matcher matcher = getMatcherAgainstContent("=\"download_url\">\\s*<a href=\"(.+?)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found");
            }
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(matcher.group(1))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("eval\\s*\\(\\s*unescape\\s*\\(\\s*'(.+?)'");
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (1)");
        }
        final String nameContent = PlugUtils.unescapeUnicode(matcher.group(1).replace("%u", "\\u"));
        matcher = PlugUtils.matcher("(?:File name:\\s*?)?<b title=\"(.+?)\"", nameContent);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found (2)");
        }
        httpFile.setFileName(matcher.group(1));

        matcher = getMatcherAgainstContent("file_size\">.+?: <b[^<>]*?>(.+?)</b>");
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file does not exist") || content.contains("<h1>404 Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("File is checked")) {
            throw new YouHaveToWaitException("File is checked, please try again in a minute", 60);
        }
    }

    private void login() throws Exception {
        synchronized (DepositFilesFileRunner.class) {
            DepositFilesServiceImpl service = (DepositFilesServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No DepositFiles Premium account login information");
                }
            }
            HttpMethod httpMethod = getMethodBuilder()
                    .setAction("/api/user/login")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                throw new ServiceConnectionProblemException();
            }
            if (getContentAsString().contains("CaptchaRequired")) {
                do {
                    final ReCaptcha rc = new ReCaptcha("6LdRTL8SAAAAAE9UOdWZ4d0Ky-aeA7XfSqyWDM2m", client);
                    final String captcha = getCaptchaSupport().getCaptcha(rc.getImageURL());
                    if (captcha == null) {
                        throw new CaptchaEntryInputMismatchException();
                    }
                    rc.setRecognized(captcha);
                    httpMethod = rc.modifyResponseMethod(getMethodBuilder()
                            .setAction("/api/user/login")
                            .setParameter("login", pa.getUsername())
                            .setParameter("password", pa.getPassword()))
                            .toPostMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
                } while (getContentAsString().contains("CaptchaInvalid"));
            }
            if (getContentAsString().contains("Error")) {
                throw new BadLoginException("Invalid DepositFiles Premium account login information");
            }
            for (final Header h : httpMethod.getResponseHeaders("Set-Cookie")) {
                final Matcher matcher = PlugUtils.matcher("autologin=(.+?);", h.getValue());
                if (matcher.find()) {
                    setCookieOnAllDomains("autologin", matcher.group(1));
                    break;
                }
            }
        }
    }

    private boolean isFolder() {
        return fileURL.contains("/folders/");
    }

    public void runFolder() throws Exception {
        final List<URI> uriList = new LinkedList<URI>();

        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();

            int i = 1;
            final Matcher pages = getMatcherAgainstContent("href=\".+?\\?page=(\\d+?)\">\\d");
            do {
                final MethodBuilder mb = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(fileURL);
                if (i > 1) mb.setParameter("page", String.valueOf(i));
                mb.setParameter("format", "text");
                if (!makeRedirectedRequest(mb.toGetMethod())) throw new ServiceConnectionProblemException();

                final BufferedReader reader = new BufferedReader(new StringReader(getContentAsString().replaceAll("(<.+?>)", "")));
                String s;
                while ((s = reader.readLine()) != null) {
                    if (!s.isEmpty()) {
                        try {
                            uriList.add(new URI(s));
                        } catch (URISyntaxException e) {
                            LogUtils.processException(logger, e);
                        }
                    }
                }
                i++;
            } while (pages.find());

            if (uriList.isEmpty()) throw new URLNotAvailableAnymoreException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

}