package cz.vity.freerapid.plugins.services.fileloby;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
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
class FileLobyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileLobyFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems(getMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher match = PlugUtils.matcher("<div[^<>]*class=\"heading[^<>]*>(.+?)</div>", content);
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        match = PlugUtils.matcher("\\s*?\\((\\d.+?)\\)<", content);
        if (!match.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems(method);//check problems
            checkNameAndSize(contentAsString);
            final HttpMethod nextMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromTextBetween("download-timer').html(\"<a class='btn btn-free' href='", "'")
                    .toGetMethod();
            final int wait = PlugUtils.getNumberBetween(contentAsString, "var seconds =", ";");
            downloadTask.sleep(wait + 1);
            if (!makeRedirectedRequest(nextMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final HttpMethod captchaMethod = stepCaptcha(getMethodBuilder()
                    .setActionFromFormWhereTagContains(httpFile.getFileName(), true)
                    .setReferer(fileURL)
                    , fileURL).toPostMethod();
            if (!tryDownloadAndSaveFile(captchaMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws ErrorDuringDownloadingException {
        if ((method.getStatusCode() == 404) || (method.getStatusText().equals("Not Found")))
            throw new URLNotAvailableAnymoreException("File not found");
        checkProblems();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder builder, final String referrer) throws Exception {
        final Matcher m = getMatcherAgainstContent("['\"]?sitekey['\"]?\\s*[:=]\\s*['\"]([^\"]+)['\"]");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, referrer);
        return r.modifyResponseMethod(builder);
    }
}