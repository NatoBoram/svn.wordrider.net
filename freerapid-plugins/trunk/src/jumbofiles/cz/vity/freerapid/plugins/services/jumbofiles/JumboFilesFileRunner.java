package cz.vity.freerapid.plugins.services.jumbofiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class JumboFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(JumboFilesFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String filename = PlugUtils.unescapeHtml(PlugUtils.getStringBetween(content, "<TR><TD>", "<small>")).trim();
        httpFile.setFileName(filename);
        PlugUtils.checkFileSize(httpFile, content, "<small>(", ")</small></TD></TR>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean login() throws Exception {
        synchronized (JumboFilesFileRunner.class) {
            JumboFilesServiceImpl service = (JumboFilesServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();

            //for testing purpose
            //pa.setPassword("freerapid");
            //pa.setUsername("freerapid");

            if (pa == null || !pa.isSet()) {
                logger.info("No account data set, skipping login");
                return false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction("http://jumbofiles.com/login.html")
                    .setParameter("op", "login")
                    .setParameter("redirect", "")
                    .setParameter("login", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("submit", "")
                    .toPostMethod();
            addCookie(new Cookie(".jumbofiles.com", "login", pa.getUsername(), "/", null, false));
            addCookie(new Cookie(".jumbofiles.com", "xfss", "", "/", null, false));
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");
            if (getContentAsString().contains("Incorrect Login or Password"))
                throw new NotRecoverableDownloadException("Invalid JumboFiles registered account login information!");

            return true;
        }
    }


    @Override
    public void run() throws Exception {
        super.run();

        login();

        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request
        if (!makeRedirectedRequest(method)) { //we make the main request
            logger.warning(getContentAsString());
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }

        checkFileProblems();//check problems
        checkNameAndSize(getContentAsString());//extract file name and size from the page

        HttpMethod httpMethod = getMethodBuilder()
                .setReferer(fileURL)
                .setBaseURL(fileURL)
                .setActionFromFormWhereTagContains("method_free", true)
                .removeParameter("method_premium")
                .toPostMethod();

        if (!makeRedirectedRequest(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            logger.warning(getContentAsString());//log the info
            throw new PluginImplementationException();//some unknown problem
        }

        checkDownloadProblems();

        final MethodBuilder methodBuilder = getMethodBuilder()
                .setActionFromFormWhereActionContains(httpFile.getFileName(), true);

        httpMethod = methodBuilder.toGetMethod();

        //logger.info("Download file URL : "+httpMethod.getURI().toString());

        //here is the download link extraction
        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkDownloadProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File is deleted or not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private void checkDownloadProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("seconds till next download")) {
            final int waitTime = PlugUtils.getWaitTimeBetween(contentAsString, "You have to wait ", " seconds till next download", TimeUnit.SECONDS);
            throw new YouHaveToWaitException("Wait between download", waitTime);
        }
        if (contentAsString.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Server problem");
        }
        if (contentAsString.contains("Error happened when generating Download Link")) {
            throw new PluginImplementationException("Server Problem : Error happened when generating Download Link");
        }
    }

}