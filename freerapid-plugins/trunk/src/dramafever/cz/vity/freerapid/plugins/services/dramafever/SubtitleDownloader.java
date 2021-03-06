package cz.vity.freerapid.plugins.services.dramafever;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.DefaultFileStreamRecognizer;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author tong2shot
 */
class SubtitleDownloader {
    private final static Logger logger = Logger.getLogger(SubtitleDownloader.class.getName());

    public void downloadSubtitle(HttpDownloadClient client, HttpFile httpFile, String subtitleUrl) throws Exception {
        logger.info("Downloading subtitle");
        client.getHTTPClient().getParams().setParameter(DownloadClientConsts.FILE_STREAM_RECOGNIZER,
                new DefaultFileStreamRecognizer(new String[]{}, new String[]{"application/ttml+xml", "application/x-octet-stream"}, false));
        HttpMethod method = client.getGetMethod(subtitleUrl);
        if (200 != client.makeRequest(method, true)) {
            throw new PluginImplementationException("Failed to request subtitle");
        }
        String timedTextXml = client.getContentAsString();

        String fnameNoExt = HttpUtils.replaceInvalidCharsForFileSystem(httpFile.getFileName().replaceFirst("\\.[^\\.]{3,4}$", ""), "_");
        String fnameOutput = fnameNoExt + ".srt";
        File outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
        BufferedWriter bw = null;
        int outputFileCounter = 2;
        try {
            while (outputFile.exists()) {
                fnameOutput = fnameNoExt + "-" + outputFileCounter++ + ".srt";
                outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
            }
            bw = new BufferedWriter(new FileWriter((outputFile)));
            bw.write(TimedText2Srt.convert(timedTextXml));
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

}
