package cz.vity.freerapid.plugins.services.prefiles;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author CrazyCoder, Abinash Bishoyi
 */
class PreFilesFileRunner extends XFileSharingRunner {
    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                httpFile.setFileName(PlugUtils.getStringBetween(content, "<div class=\"filename_bar\"><i></i><h3>", " <small>"));
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher match = PlugUtils.matcher("<sup[^<>]*>(.+?)</sup>", content);
                if (!match.find())
                    throw new PluginImplementationException("File size not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("duration\\s*\\:\\s*\\{\\s*hours\\s*\\:\\s*(.+?),\\s*minutes\\s*\\:\\s*(.+?),\\s*seconds\\s*:\\s*(.+?)\\s*\\}");
        if (matcher.find()) {
            return  (60 * (60 * Integer.parseInt(matcher.group(1))) + Integer.parseInt(matcher.group(2))) + Integer.parseInt(matcher.group(3)) + 6;
        }
        return 0;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("This direct link is only available");
        return downloadPageMarkers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return super.getXFSMethodBuilder(content, "method_").setAction(fileURL)
                .setParameter("method_free", "method_free").removeParameter("method_premium");
    }

    @Override
    protected void doLogin(final PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(getBaseURL())
                .setAction(getBaseURL() + "/login")            // line changed
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder()
                .setReferer(getBaseURL() + "/login")            // line changed
                .setAction(getBaseURL())
                .setActionFromFormWhereActionContains("login", true)            // line changed
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("Incorrect Username or password")) {  // line changed
            throw new BadLoginException("Invalid account login information");
        }
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a[^>]+href\\s*=\\s*['\"](?:.+)?(http[^=]+?" + Pattern.quote(httpFile.getFileName()) + ")['\"][^>]*>.+?Download.*<");
        return downloadLinkRegexes;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        super.checkFileProblems();
        if (getContentAsString().contains("The file you were looking for could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("error\">This file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("File is for Premium Users only");
        }
        if (getContentAsString().contains("File owner set free user can download max file size")) {
            throw new NotSupportedDownloadByServiceException("File exceeds free user max file size set by file owner");
        }
    }
}
