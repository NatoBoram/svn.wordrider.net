package cz.vity.freerapid.plugins.services.filerio;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.util.URIUtil;

import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FileRioFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        if (fileURL.matches("http://(?:www\\.)?filerio\\.com/.+")) {
            httpFile.setNewURL(new URL(fileURL.replaceFirst("filerio\\.com", "filerio.in")));
        }
    }

    @Override
    protected void checkFileSize() throws ErrorDuringDownloadingException {
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("eval\\(unescape\\('(.+?)'\\)\\)");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
            final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
            if (matcher.find()) {
                try {
                    final String unEscStr = URLDecoder.decode(matcher.group(1), "UTF-8");
                    return URIUtil.encodePath(PlugUtils.getStringBetween(unEscStr, "location.href=\"", "\";"));
                } catch (Exception e) {
                    throw new PluginImplementationException("Error reading download URL");
                }
            }
        }
        throw new PluginImplementationException("Download link not found");
    }

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("File has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems(content);
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        super.checkDownloadProblems();
    }

}