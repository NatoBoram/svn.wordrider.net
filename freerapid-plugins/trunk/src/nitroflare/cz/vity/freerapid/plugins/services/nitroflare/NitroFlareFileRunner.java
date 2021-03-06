package cz.vity.freerapid.plugins.services.nitroflare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class NitroFlareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NitroFlareFileRunner.class.getName());
    private final static int MAX_FREE_PAGE_ATTEMPT = 5;
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:32.0) Gecko/20100101 Firefox/32.0";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        checkURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getMethod);
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems(getMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "span title=\"", "\"");
        Matcher matcher = PlugUtils.matcher("<span [^<>]*?dir=\"ltr\"[^<>]*>(.+?)</", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(1));
        logger.info("File size: " + filesize);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems(method);
            checkNameAndSize(getContentAsString());
            fileURL = method.getURI().toString(); //http://www.nitroflare redirected to http://nitroflare

            String fileId = getFileId(fileURL);
            String freePageContent = null;
            int i = 0;
            while (getContentAsString().contains("goToFreePage") && (i++ < MAX_FREE_PAGE_ATTEMPT)) {
                setCookie(fileId);
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(fileURL)
                        .setParameter("goToFreePage", "")
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems(method);
                    logger.warning(getContentAsString()); //sometimes httpcode >= 500, probably cloudflare thingy
                    throw new ServiceConnectionProblemException();
                }
                checkProblems(method);
                freePageContent = getContentAsString();
            }
            if ((freePageContent == null) || (i >= MAX_FREE_PAGE_ATTEMPT)) {
                throw new PluginImplementationException("Error getting free page content");
            }
            int waitTime = 180;
            try {
                waitTime = PlugUtils.getNumberBetween(getContentAsString(), "data-timer=\"", "\"");
            } catch (PluginImplementationException e) {
                LogUtils.processException(logger, e);
            }
            fileURL = method.getURI().toString() + "/free"; //redirected to /free, explicit because of POST
            setCookie(fileId);

            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction("http://nitroflare.com/ajax/freeDownload.php")
                    .setParameter("fileId", fileId)
                    .setParameter("method", "startTimer")
                    .setAjax()
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems(method);
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException();
            }
            checkProblems(method);

            downloadTask.sleep(waitTime);
            do {
                stepCaptcha(freePageContent);
            } while (getContentAsString().contains("The captcha wasn't entered correctly")
                    || getContentAsString().contains("You have to fill the captcha"));

            try {
                method = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Click here to download").toGetMethod();
            } catch (BuildMethodException e) {
                throw new PluginImplementationException("Download URL not found", e);
            }
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems(method);
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File doesn't exist") || (method.getStatusCode() == 404)) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("Free download is currently unavailable")) {
            throw new YouHaveToWaitException("Free download is currently unavailable", 5 * 60);
        }
        if (contentAsString.contains("file is available with Premium only")) {
            throw new NotRecoverableDownloadException("File is available with Premium only");
        }
        if (contentAsString.contains("You have to wait")) {
            Matcher matcher = PlugUtils.matcher("You have to wait (\\d+) minutes?", contentAsString);
            if (!matcher.find()) {
                throw new PluginImplementationException("Waiting time not found");
            }
            int waitingTime = Integer.parseInt(matcher.group(1).trim());
            throw new YouHaveToWaitException("You have to wait " + waitingTime + " minute(s) to download your next file", waitingTime * 60);
        }
    }

    private void checkURL() {
        fileURL = fileURL.replaceFirst("https://", "http://");
    }

    private String getFileId(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("/view/([^/]+?)(?:/.*)?$", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("File ID not found");
        }
        return matcher.group(1);
    }

    private void setCookie(String fileId) throws IOException, ErrorDuringDownloadingException {
        HttpMethod method;
        method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://nitroflare.com/ajax/setCookie.php")
                .setParameter("fileId", fileId)
                .setAjax()
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
        checkProblems(method);
    }

    private void stepCaptcha(String content) throws Exception {
        HttpMethod method;
        Matcher matcher = PlugUtils.matcher("noscript\\?k=([^\"']+?)['\"]", content);
        if (matcher.find()) {
            String captchaKey = matcher.group(1);

            ReCaptcha reCaptcha = new ReCaptcha(captchaKey, client);
            String captchaResponse = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
            if (captchaResponse == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            reCaptcha.setRecognized(captchaResponse);

            method = reCaptcha.modifyResponseMethod(getMethodBuilder()
                    .setReferer(fileURL)
                    .setAjax()
                    .setAction("http://nitroflare.com/ajax/freeDownload.php")
                    .setParameter("method", "fetchDownload"))
                    .toPostMethod();
        }
        else {
            final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("<div class.+?data-sitekey=\"(.+?)\"", content);
            if (!reCaptchaKeyMatcher.find()) {
                throw new PluginImplementationException("ReCaptcha key not found");
            }
            final String reCaptchaKey = reCaptchaKeyMatcher.group(1).trim();
            final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);

            MethodBuilder builder = getMethodBuilder(content)
                    .setReferer(fileURL).setAjax()
                    .setActionFromFormWhereTagContains("fetchDownload", true)
                    .setAction(fileURL);
            r.modifyResponseMethod(builder);
            method = builder.toPostMethod();
        }
        if (!makeRedirectedRequest(method)) {
            checkProblems(method);
            logger.warning(getContentAsString());
            throw new ServiceConnectionProblemException();
        }
        checkProblems(method);
    }

}
