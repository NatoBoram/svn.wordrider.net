package cz.vity.freerapid.plugins.services.turbobit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author Arthur Gunawan, RickCL, ntoskrnl, tong2shot, Abinash Bishoyi, birchie
 */
public class TurboBitFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());
    private String fileID;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = fileURL.replace("//forum.flacmania.ru/", "//turbobit.net/");
        fileURL = checkFileURL(fileURL);
        final HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize();
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String checkFileURL(final String fileURL) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("^https?://(?:(?:www|new)\\.)?(turbobit\\.net|dl\\.rapidlinks\\.org|hitfile\\.net|sibit\\.net|files\\.uz-translations\\.uz)/(?:download/free/)?(\\w+)", fileURL.replaceFirst("turo-bit.net/", "turbobit.net/"));
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing download link");
        }
        addCookie(new Cookie("." + matcher.group(1), "user_lang", "en", "/", 86400, false));
        fileID = matcher.group(2);
        return "http://" + matcher.group(1) + "/download/free/" + matcher.group(2);
    }

    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher matcher = getMatcherAgainstContent("<span class.*?>(.+?)</span>\\s*\\((.+?)\\)");
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        } else {
            PlugUtils.checkName(httpFile, getContentAsString(), "file-title\">", "<");
            PlugUtils.checkFileSize(httpFile, getContentAsString(), "file-size\">", "</");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        fileURL = fileURL.replace("//forum.flacmania.ru/", "//turbobit.net/");
        fileURL = checkFileURL(fileURL);

        HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            method = getGetMethod(fileURL);     //once more to avoid possible redirection to original url by previous request
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            while (getContentAsString().contains("class=\"g-recaptcha\"") || getContentAsString().contains("/captcha/")) {
                if (!makeRedirectedRequest(stepCaptcha(method.getURI().toString()))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }

            String ref = method.getURI().toString();
            method = getMethodBuilder()
                    .setReferer(ref)
                    .setAction("/download/getLinkTimeout/" + fileID)
                    .setBaseURL(method.getURI().toString().split("/download/")[0])
                    .toGetMethod();
            method.addRequestHeader("X-Requested-With", "XMLHttpRequest");

            downloadTask.sleep(1 + getWaitTime());

            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (getContentAsString().contains("Error: 2965")) {
                throw new PluginImplementationException("Plugin is broken");
            }
            Matcher match = getMatcherAgainstContent("<a[^>]+href=['\"]([^'\"]+)['\"][^>]+>Download");
            try {
                do { match.find();
                } while (match.group(1).contains("/started/"));
            } catch (Exception x) {
                match = getMatcherAgainstContent("['\"]([^'\"]*/redirect/[^'\"]+)['\"]");
                if (!match.find())
                    throw new PluginImplementationException("Download link not found");
            }
            method = getMethodBuilder()
                    .setReferer(ref)
                    .setAction(match.group(1))
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    protected int getWaitTime() {
        Matcher match = PlugUtils.matcher("[Ll]imit\\s*:\\s*(\\d+)", getContentAsString());
        if (match.find())
            return Integer.parseInt(match.group(1));
        return 60;
    }

    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("File was not found")
                || getContentAsString().contains("Probably it was deleted")
                || getContentAsString().contains("File was deleted or not found")
                || getContentAsString().contains("It could possibly be deleted"))
            throw new URLNotAvailableAnymoreException("File not found");
        if (getContentAsString().contains("Our service is currently unavailable in your country"))
            throw new NotRecoverableDownloadException("Service is unavailable in your country");
        if (getContentAsString().contains("Limit reached for free download of this file"))
            throw new NotRecoverableDownloadException("Limit reached for free download of this file");
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file is not available now because of technical problems")) {
            throw new ServiceConnectionProblemException("The file is not available now because of technical problems");
        }
        Matcher matcher = getMatcherAgainstContent("limit\\s*:\\s*(\\d+)");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Download limit reached", Integer.parseInt(matcher.group(1)));
        }
        if (getContentAsString().contains("The site is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The site is temporarily unavailable");
        }
        if (getContentAsString().contains("From your IP range the limit of connections is reached")
                || getContentAsString().contains("You have reached the limit of connections")) {
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), " id='timeout'>", "</");
            throw new YouHaveToWaitException("Download limit reached from your IP range", waitTime);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private HttpMethod stepCaptcha(final String action) throws Exception {
        if (getContentAsString().contains("recaptcha") && getContentAsString().contains("data-sitekey")) {
            logger.info("Handling ReCaptcha");
            final Matcher m = getMatcherAgainstContent("data-sitekey=\"([^\"]+)\"");
            if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
            final String reCaptchaKey = m.group(1);

            final String content = getContentAsString();
            final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, action);

            return r.modifyResponseMethod(
                    getMethodBuilder(content)
                            .setReferer(action)
                            .setActionFromFormWhereTagContains("data-sitekey", true)
                            .setAction(action)
            ).toPostMethod();
        } else {
            logger.info("Handling regular captcha");
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();

            final String captcha;
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();

            return getMethodBuilder()
                    .setReferer(action)
                    .setActionFromFormWhereTagContains("captcha", true)
                    .setAction(action)
                    .setParameter("captcha_response", captcha)
                    .toPostMethod();
        }
    }

}