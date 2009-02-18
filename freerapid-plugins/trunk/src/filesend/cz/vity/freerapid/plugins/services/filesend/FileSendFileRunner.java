package cz.vity.freerapid.plugins.services.filesend;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Downloading itself...
 */
class FileSendFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileSendFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception {
        super.runCheck();//here we make validation
        final GetMethod getMethod = getGetMethod(fileURL);//creating GET request
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString()); //checking for file and name of file
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        //we have to extract file name and size from the page
        Matcher matcher = PlugUtils.matcher("File Name:</strong>\\s*(.+?)\\s*</td>", content);
        if (matcher.find()) {
            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            //: <strong>204800</strong>KB<br>
            matcher = PlugUtils.matcher("File Size:</strong>\\s*(.+?)\\s*</td>", content);
            if (matcher.find()) {
                final long size = PlugUtils.getFileSizeFromString(matcher.group(1));
                httpFile.setFileSize(size);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            client.setReferer(fileURL);
            //here  we extract download link from the page
            final Matcher matcher = getMatcherAgainstContent("method=\"POST\" action=\"(http.+?)\"");
            if (matcher.find()) {
                final PostMethod postMethod = getPostMethod(matcher.group(1));
                PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"sid"});

                if (!tryDownloadAndSaveFile(postMethod)) { //let's download file
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException();
                }
            } else throw new PluginImplementationException("Plugin error: Download link not found");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");//we throw exception that the file was not found
        }
    }

}
