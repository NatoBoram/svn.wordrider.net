package cz.vity.freerapid.plugins.services.speedyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SpeedyShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SpeedyShareFileRunner.class.getName());
    private final static String SERVICE_COOKIE_DOMAIN = ".speedyshare.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "trans", "en", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "Download File:", "\"");
        PlugUtils.checkFileSize(httpFile, content, "<div class=sizetagtext>", "</div>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        addCookie(new Cookie(SERVICE_COOKIE_DOMAIN, "trans", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            final Matcher matcher;
            if (fileURL.contains("speedyshare.com/files/")) {
                matcher = getMatcherAgainstContent("href=[\"'](/files/(\\d+?)/download/([^/<>\"']+?))[\"']>");
            } else {
                matcher = getMatcherAgainstContent("<a href.+?id=dlink.+?data-href=['\"](.+?)['\"]");
            }

            if (!matcher.find()) throw new PluginImplementationException("Download link not found");

            if (matcher.find(matcher.end())) { //multiple files
                httpFile.setFileName(httpFile.getFileName() + " (multiple files)");

                int start = 0;
                final List<URI> uriList = new LinkedList<URI>();
                while (matcher.find(start)) {
                    final String link;
                    if (fileURL.contains("speedyshare.com/files/")) {
                        link = "http://www.speedyshare.com/files/" + matcher.group(2) + "/" + matcher.group(3);
                    } else {
                        link = "http://www.speedyshare.com" + new String(Base64.decodeBase64(matcher.group(1))).replaceFirst("/download/", "/");
                    }
                    try {
                        uriList.add(new URI(link));
                    } catch (URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                    start = matcher.end();
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);

            } else { //single file
                matcher.find(0);
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction("http://www.speedyshare.com" + new String(Base64.decodeBase64(matcher.group(1)))).toGetMethod();

                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkURL() {
        fileURL = fileURL.replaceFirst("speedy\\.sh", "speedyshare.com").replaceFirst("speedyshare.com/file/", "speedyshare.com/");
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Not Found") || content.contains("Not valid anymore") || content.contains("The file has been deleted") || content.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("This file can only be downloaded with SpeedyShare Premium")) {
            throw new NotRecoverableDownloadException("Premium download only");
        }
        if (content.contains("The one-hour limit")) {
            throw new YouHaveToWaitException("The one-hour limit for this download has been exceeded", 15 * 60);
        }
    }

}