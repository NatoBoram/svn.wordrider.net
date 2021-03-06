package cz.vity.freerapid.plugins.services.imgcredit;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ImgCreditFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImgCreditFileRunner.class.getName());

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
        if (fileURL.contains("/image/")) {
            Matcher matcher = PlugUtils.matcher("download=\"([^\"]+)\"[^<>]+?(\\d[\\d.,]*\\s\\w*?B(ytes)?)\">", content);
            if (!matcher.find())
                throw new PluginImplementationException("File name/size not found");
            httpFile.setFileName(matcher.group(1).trim());
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2).trim()));
        }
        if (fileURL.contains("/album/")) {
            PlugUtils.checkName(httpFile, content, "description\" content=\"", "\"");
            httpFile.setFileName("Album: " + httpFile.getFileName());
            PlugUtils.checkFileSize(httpFile, content, "<b data-text=\"image-count\">", "</b>");
        }
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
            if (fileURL.contains("/image/")) {
                Matcher matcher = PlugUtils.matcher("(?:image\" content=|image_src\" href=)\"([^\"]+)\"", contentAsString);
                if (!matcher.find())
                    throw new PluginImplementationException("Image source not found");
                if (!tryDownloadAndSaveFile(getGetMethod(matcher.group(1).trim()))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
            if (fileURL.contains("/album/")) {
                final List<URI> list = new LinkedList<URI>();
                boolean morePages;
                do {
                    morePages = false;
                    final Matcher matcher = PlugUtils.matcher("<a href=\"(http[^\"]+?)\" class=\"image-container\">", getContentAsString());
                    while (matcher.find()) {
                        list.add(new URI((matcher.group(1).trim())));
                    }
                    if (getContentAsString().contains("class=\"pagination-next\"")) {
                        String nextPageUrl = PlugUtils.getStringBetween(getContentAsString(), "class=\"pagination-next\"><a href=\"", "\"");
                        if (!makeRedirectedRequest(getGetMethod(nextPageUrl))) {
                            checkProblems();
                            throw new ServiceConnectionProblemException();
                        }
                        morePages = true;
                    }
                } while (morePages);
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("page doesn't exist") || contentAsString.contains("(404)")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}