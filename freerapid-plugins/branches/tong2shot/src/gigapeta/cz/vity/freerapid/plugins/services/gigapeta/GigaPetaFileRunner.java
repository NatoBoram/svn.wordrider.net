/*
 * PLEASE, READ THIS
 *
 * This file is in UTF-8 encoding
 *
 * If you want to edit it, you can edit it as ASCII provided
 * you don't touch the UTF-8 characters and don't change the encoding
 *
 * If you want to compile it, set the encoding in your IDE or
 * use the -encoding option to javac
 *
 * If you want to build this plugin, use build.xml which already
 * deals with this
 *
 * Otherwise, don't touch the strings if you can't check their meaning
 */
package cz.vity.freerapid.plugins.services.gigapeta;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Thumb
 */
class GigaPetaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaPetaFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();

        HttpMethod getMethod = getGetMethod(fileURL);//make first request
        if (!makeRedirectedRequest(getMethod))
            throw new ServiceConnectionProblemException();

        getMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("http://gigapeta.com/?lang=ru") //make request in RU
                .toGetMethod();

        if (!makeRedirectedRequest(getMethod))
            throw new ServiceConnectionProblemException();

        checkFileProblems();
        checkNameAndSize();//ok let's extract file name and size from the page
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher name_match = PlugUtils.matcher("<tr class=\"name\">(?:\\s|<[^>]*>)*(.+?)\\s*</t[rd]>", getContentAsString());
        if (!name_match.find())
            unimplemented();

        httpFile.setFileName(name_match.group(1));

        final Matcher size_match = PlugUtils.matcher("????????????(?:\\s|<[^>]*>)*([^<>]+)\\s*<", getContentAsString());
        if (!size_match.find())
            unimplemented();

        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size_match.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    /**
     * @throws PluginImplementationException
     */
    private void unimplemented() throws PluginImplementationException {
        logger.warning(getContentAsString());
        throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        runCheck();
        checkDownloadProblems();

        String contentAsString = getContentAsString();
        final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "??????????, ???????????????? <b>", "</b>", TimeUnit.SECONDS);
        downloadTask.sleep(waitTime);

        String captcha_id = String.format("%d", (int) Math.ceil(Math.random() * 1e8));

        final HttpMethod httpMethod = getMethodBuilder(contentAsString)
                .setParameter("captcha_key", captcha_id)
                .setParameter("captcha", getCaptcha(captcha_id))
                .setParameter("download", "??????????????")
                .setAction(fileURL)
                .toPostMethod();

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            unimplemented();
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<div id=\"page_error\">")) {
            if (contentAsString.contains("?????????? ?? ???????????????? ?????????????? ??????????????"))
                throw new CaptchaEntryInputMismatchException();
            if (PlugUtils.matcher("?????? ???????????? ?????? IP [0-9.]* ????????????", contentAsString).find())
                throw new YouHaveToWaitException("Download streams for your IP exhausted", 1800);
            if (contentAsString.contains("????????????????! ???????????? ???????? ?????? ????????????"))
                throw new URLNotAvailableAnymoreException("File was deleted");
            unimplemented();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        Matcher err_match = PlugUtils.matcher("<h1 class=\"big_error\">([^>]+)</h1>", getContentAsString());
        if (err_match.find()) {
            if (err_match.group(1).equals("404"))
                throw new URLNotAvailableAnymoreException("File not found");
            unimplemented();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        checkDownloadProblems();
    }

    private String getCaptcha(String id) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchas = getCaptchaSupport();
        final String ret = captchas.getCaptcha("http://gigapeta.com/img/captcha.gif?x=" + id);
        if (ret == null)
            throw new CaptchaEntryInputMismatchException();
        return ret;
    }

    private void login() throws Exception {
        synchronized (GigaPetaFileRunner.class) {
            GigaPetaServiceImpl service = (GigaPetaServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                if (!makeRedirectedRequest(getGetMethod("http://gigapeta.com/")))
                    throw new ServiceConnectionProblemException();
                final HttpMethod method = getMethodBuilder()
                        .setActionFromFormWhereTagContains("auth_", true)
                        .setReferer("http://gigapeta.com/").setAction("http://gigapeta.com/")
                        .setParameter("auth_login", pa.getUsername())
                        .setParameter("auth_passwd", pa.getPassword())
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().contains("???????????? ?????? ?????????? ?????????????? ???? ??????????") ||
                        getContentAsString().contains("Wrong password or login")) {
                    throw new BadLoginException("Invalid GigaPeta account login information!");
                }
                logger.info("Logged in :)");
            }
        }
    }

}
