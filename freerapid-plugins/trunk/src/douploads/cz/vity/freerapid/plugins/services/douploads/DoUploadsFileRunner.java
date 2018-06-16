package cz.vity.freerapid.plugins.services.douploads;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
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
class DoUploadsFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher match = PlugUtils.matcher("\\]([^\\[]+?)\\s*-\\s*\\d[^-\\[]+\\[/", content);
                if (!match.find())
                    throw new PluginImplementationException("File name not found");
                httpFile.setFileName(match.group(1).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher matcher = PlugUtils.matcher("\\][^\\[]+?\\s*-\\s*(\\d[^-\\[]+)\\[/", content);
                if (!matcher.find()) {
                    throw new PluginImplementationException("File size not found");
                }
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        MethodBuilder builder = getXFSMethodBuilder(getContentAsString());
        builder.removeParameter("chkIsAdd");
        return builder;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.clear();
        downloadLinkRegexes.add("<a href\\s*=\\s*[\"'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"'].+?Download Now.+?</a");
        return downloadLinkRegexes;
    }
}