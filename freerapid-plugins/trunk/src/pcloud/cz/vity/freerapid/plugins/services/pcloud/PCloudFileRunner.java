package cz.vity.freerapid.plugins.services.pcloud;

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
 * @author birchie
 */
class PCloudFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PCloudFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        correctURL();
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
        PlugUtils.checkName(httpFile, content, "\"name\": \"", "\",");
        PlugUtils.checkFileSize(httpFile, content, "\"size\": ", ",");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void correctURL() {
        fileURL = fileURL.replaceFirst("http://pc.cd/", "https://my.pcloud.com/publink/show?code=");
    }

    @Override
    public void run() throws Exception {
        super.run();
        correctURL();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            String downloadlink = PlugUtils.getStringBetween(contentAsString, "\"downloadlink\": \"", "\",");
            downloadlink = downloadlink.replace("\\/", "/");
            final HttpMethod httpMethod = getGetMethod(downloadlink);
            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (contentAsString.contains("File Not Found") ||
                contentAsString.contains("Invalid link")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}