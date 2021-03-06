package cz.vity.freerapid.plugins.services.userscloud;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UsersCloudFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        final Matcher match = PlugUtils.matcher("userscloud\\.com/([\\w\\d]+)", fileURL);
        if (!match.find())
            throw new InvalidURLOrServiceProblemException("File ID missing from URL");
        fileURL = "https://userscloud.com/" + match.group(1);
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher match = PlugUtils.matcher(">([^<>]+?) - \\d[\\d.,]*?\\s\\w*?B(ytes)?<", content);
                if (!match.find())
                    throw  new PluginImplementationException("File name not found");
                httpFile.setFileName(match.group(1).trim());
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
                final Matcher match = PlugUtils.matcher(" - (\\d[\\d.,]+?\\s\\w*?B(ytes)?)[\\[<]", content);
                if (!match.find())
                    throw  new PluginImplementationException("File size not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim()));
            }
        });
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher match = PlugUtils.matcher("size\\s*:\\s*(?:<[^>]+>\\s*)*(\\d[\\d.,]*?\\s*\\w*?B(ytes)?)<", content);
                if (!match.find())
                    throw  new PluginImplementationException("File size not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim()));
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("dl_manager = new DownloadManager(");
        downloadPageMarkers.add("If your download doesn't start automatically");
        downloadPageMarkers.add("Your Download is ready");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("url\\s*[=:]\\s*[\"'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"']");
        downloadLinkRegexes.add(0, "<a[^<>]+?href=\"(http.+?" + Pattern.quote(httpFile.getFileName()) + ")\"[^<>]*>(?:<[^<>]*>\\s*)*.*click here");
        return downloadLinkRegexes;
    }

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("file you are trying to download is no longer available") ||
                content.contains("file is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems(content);
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        MethodBuilder builder = getXFSMethodBuilder(getContentAsString());
        if (!((UsersCloudServiceImpl) getPluginService()).getConfig().isSet())
            builder.setParameter("method_free", "Free Download");
        return builder;
    }
}