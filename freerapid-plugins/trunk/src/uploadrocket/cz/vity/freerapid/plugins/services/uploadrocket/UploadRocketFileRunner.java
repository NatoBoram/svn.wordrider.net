package cz.vity.freerapid.plugins.services.uploadrocket;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UploadRocketFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                // no file name displayed
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes =  new LinkedList<String>();
        downloadLinkRegexes.add("<a[^<>]*href=\"(.+?)\"[^<>]*>[^<>]*Download Link");
        downloadLinkRegexes.add("<a[^<>]*href=\"(.+?)\"[^<>]*><[^<>]*free-download.png");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        final String link = super.getDownloadLinkFromRegexes();
        httpFile.setFileName(link.substring(1 + link.lastIndexOf("/")));
        return link;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder = super.getXFSMethodBuilder();
        if ((methodBuilder.getParameters().get("method_isfree") != null) && (!methodBuilder.getParameters().get("method_isfree").isEmpty())) {
            methodBuilder.removeParameter("method_ispremium");
        }
        return methodBuilder;
    }

    @Override
    protected boolean handleDirectDownload(final HttpMethod method) throws Exception {
        fileURL = method.getResponseHeader("Location").getValue();
        if (!makeRedirectedRequest(redirectToLocation(method))) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        return false;
    }

    protected List<String> getFalseProblemRegexes() {
        final List<String> falseProblemRegexes = super.getFalseProblemRegexes();
        falseProblemRegexes.add("<h3[^<>]+?color:black.+?</h3>");
        return falseProblemRegexes;
    }

    @Override
    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkDownloadProblems(content);
        if (content.contains("No such file")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        URI downloadUri = method.getURI();
        downloadUri.setEscapedAuthority(downloadUri.getAuthority().toLowerCase(Locale.ENGLISH));
        return super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(downloadUri.getEscapedURI()).toGetMethod());
    }
}