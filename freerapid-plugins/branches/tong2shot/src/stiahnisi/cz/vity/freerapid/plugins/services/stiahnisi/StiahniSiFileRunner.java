package cz.vity.freerapid.plugins.services.stiahnisi;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class StiahniSiFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(StiahniSiFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        setLang();
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "name\">", "</");
        PlugUtils.checkFileSize(httpFile, content, "fileSize\" content=\"", "\"/>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setLang() {
        if (PlugUtils.matcher("stiahni.si/\\w+?/file", fileURL).find())
            fileURL = fileURL.replaceFirst("stiahni.si/\\w+?/file", "stiahni.si/en/file");
        else
            fileURL = fileURL.replaceFirst("stiahni.si/file", "stiahni.si/en/file");
    }

    @Override
    public void run() throws Exception {
        setLang();
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkFileProblems();
            checkNameAndSize(contentAsString);
            checkDownloadProblems();
            final int waitTime = PlugUtils.getNumberBetween(getContentAsString(), "var parselimit =", ";");
            downloadTask.sleep(waitTime + 1);
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromTextBetween("window.location='", "';")
                    .toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkDownloadProblems();
                logger.info(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("bol zmazan??") ||
                contentAsString.contains("Soubor nikdo nest??hnul v??ce") ||
                contentAsString.contains("Soubor obsahoval neleg??ln?? obsah") ||
                contentAsString.contains("Soubor byl smazan?? uploaderem") ||
                contentAsString.contains("This file does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("V??etky free sloty s?? obsaden??") ||
                contentAsString.contains("V??echny free sloty jsou obsazen??")) {
            throw new YouHaveToWaitException("All free slots are occupied", 5 * 60);
        }
        if (contentAsString.contains("Paraleln?? s??ahovanie nieje pre free uzivate??ov povolen??") ||
                contentAsString.contains("Free download dont support parallel downloading")) {
            throw new PluginImplementationException("Parallel download for free users is not allowed");
        }
    }

}