package cz.vity.freerapid.plugins.services.billionuploads;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
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
 * @author birchie
 */
class BillionUploadsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BillionUploadsFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "Filename:</b>", "<br>");
        PlugUtils.checkFileSize(httpFile, content, "Size:</b>", "<br>");
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
            checkNameAndSize(contentAsString);//extract file name and size from the page


            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction(fileURL)
                    .setParameter("op", PlugUtils.getStringBetween(contentAsString, "name=\"op\" value=\"", "\">"))
                    .setParameter("id", PlugUtils.getStringBetween(contentAsString, "name=\"id\" value=\"", "\">"))
                    .setParameter("rand", PlugUtils.getStringBetween(contentAsString, "name=\"rand\" value=\"", "\">"))
                    .setParameter("method_free", "1")
                    .setParameter("down_direct", "1")
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download-1");
            }
            httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toHttpMethod();
            setFileStreamContentTypes("text/plain");
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
        if (contentAsString.contains(">File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}