package cz.vity.freerapid.plugins.services.adobehds;

import cz.vity.freerapid.plugins.webclient.DefaultFileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class HdsDownloader {
    /*
     * Useful resources:
     *
     * https://github.com/K-S-V/Scripts/blob/master/AdobeHDS.php
     * http://download.macromedia.com/f4v/video_file_format_spec_v10_1.pdf
     * http://sourceforge.net/apps/mediawiki/osmf.adobe/index.php?title=Flash_Media_Manifest_(F4M)_File_Format_obsolete
     * http://code.google.com/p/mp-onlinevideos2/source/browse/trunk/
     */

    private static final Logger logger = Logger.getLogger(HdsDownloader.class.getName());

    protected final HttpDownloadClient client;
    protected final HttpFile httpFile;
    protected final HttpFileDownloadTask downloadTask;

    public HdsDownloader(final HttpDownloadClient client, final HttpFile httpFile, final HttpFileDownloadTask downloadTask) {
        this.client = client;
        this.httpFile = httpFile;
        this.downloadTask = downloadTask;
    }

    public void tryDownloadAndSaveFile(final String manifestUrl) throws Exception {
        client.getHTTPClient().getParams().setParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER, new DefaultFileStreamRecognizer(new String[0], new String[]{"video/f4m"}, false));
        final HdsManifest manifest = new HdsManifest(client, manifestUrl);
        final HdsMedia media = getSelectedMedia(manifest.getMedias());
        logger.info("Downloading media: " + media);

        httpFile.setState(DownloadState.GETTING);
        logger.info("Starting HDS download");

        if ((httpFile.getStoreFile() == null) || (httpFile.getStoreFile().length() == 0)) {  //cancelled
            httpFile.getProperties().remove(HdsConsts.FRAGMENT_LAST_POS);
            httpFile.getProperties().remove(HdsConsts.CURRENT_FRAGMENT);
            httpFile.getProperties().remove(HdsConsts.AVC_SEQUENCE_HEADER_WRITTEN);
            httpFile.getProperties().remove(HdsConsts.AAC_SEQUENCE_HEADER_WRITTEN);
        }

        Long fragmentLastPos = (Long) httpFile.getProperties().get(HdsConsts.FRAGMENT_LAST_POS);
        if (fragmentLastPos == null) {
            httpFile.getProperties().remove(DownloadClient.START_POSITION);
        } else {
            httpFile.getProperties().put(DownloadClient.START_POSITION, fragmentLastPos);
        }
        httpFile.getProperties().remove(DownloadClient.SUPPOSE_TO_DOWNLOAD);
        httpFile.setResumeSupported(true);

        final String fn = httpFile.getFileName();
        if (fn == null || fn.isEmpty())
            throw new IOException("No defined file name");
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));

        client.getHTTPClient().getParams().setBooleanParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);

        final FragmentRequester requester = getFragmentRequester(media);
        final InputStream in = getInputStream(requester);

        try {
            downloadTask.saveToFile(in);
        } finally {
            in.close();
        }
    }

    protected HdsMedia getSelectedMedia(List<HdsMedia> mediaList) throws Exception {
        return Collections.max(mediaList);
    }

    protected FragmentRequester getFragmentRequester(HdsMedia media) {
        return new FragmentRequester(httpFile, client, media);
    }

    protected InputStream getInputStream(FragmentRequester requester) {
        return new HdsInputStream(requester);
    }

}
