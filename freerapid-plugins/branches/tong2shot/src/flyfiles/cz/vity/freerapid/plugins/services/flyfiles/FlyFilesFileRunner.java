package cz.vity.freerapid.plugins.services.flyfiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FlyFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FlyFilesFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<div id=\"file_det\"[^>]*?>\\s*?(.+?) - (\\d.+?)<br>", content);
        if (!match.find())
            throw new NotRecoverableDownloadException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);

            final int wait = PlugUtils.getNumberBetween(contentAsString, "timeWait = ", ";");
            if (wait > 0)
                throw new YouHaveToWaitException("Wait between downloads", wait+1);

            boolean captchaLoop;
            do {
                captchaLoop = false;
                final String captcha = doCaptcha(PlugUtils.getStringBetween(contentAsString, "Captcha: <img src=\"", "\""));
                final String sess = PlugUtils.getStringBetween(contentAsString, "Download('", "'");
                final HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://flyfiles.net/")
                        .setParameter("getDownLink", sess)
                        .setParameter("captcha_value", captcha)
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().contains("downlinkCaptcha|0")) {
                    captchaLoop = true;
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
            } while(captchaLoop);
            final String downloadStr = getContentAsString().replace("#downlink|", "").trim();
            if (downloadStr.matches("#")) {
                throw new YouHaveToWaitException("You need to wait between downloads", 300);
            }
            if (!tryDownloadAndSaveFile(getGetMethod(downloadStr))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found!")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String doCaptcha(final String captchaUrl) throws Exception {
        final String captcha;
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setAction(captchaUrl).setReferer(fileURL).getEscapedURI();
        captcha = captchaSupport.getCaptcha(captchaSrc);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        return captcha;
    }
}