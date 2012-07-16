package cz.vity.freerapid.plugins.services.speedfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity,tong2shot
 */
class SpeedFileFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SpeedFileFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkFileURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "og:title\" content=\"", "\" />");
        PlugUtils.checkFileSize(httpFile, content, "<strong><big>", "</big></strong>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkFileURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("<span>").toHttpMethod();
            httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            int waitTime = 10;
            try {
                waitTime = PlugUtils.getNumberBetween(getContentAsString(), "\"allowedAt\":", ",") - PlugUtils.getNumberBetween(getContentAsString(), "\"requestedAt\":", ",");
            } catch (PluginImplementationException e) {
                //
            }
            downloadTask.sleep(waitTime + 1);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")
                || contentAsString.contains("file was deleted")
                || contentAsString.contains("chyba 404")
                || contentAsString.contains("page you requested could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkFileURL() {
        fileURL = fileURL.replaceFirst("speedfile\\.cz/[a-z]{2}/", "speedfile.cz/en/");
    }

}