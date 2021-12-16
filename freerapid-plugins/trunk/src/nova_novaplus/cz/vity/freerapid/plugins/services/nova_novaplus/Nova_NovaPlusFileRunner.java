package cz.vity.freerapid.plugins.services.nova_novaplus;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.applehls.HlsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot, AStudna, homer
 * @since 0.9u4
 */
class Nova_NovaPlusFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Nova_NovaPlusFileRunner.class.getName());
    private SettingsConfig config;

    private void setConfig() throws Exception {
        Nova_NovaPlusServiceImpl service = (Nova_NovaPlusServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("(?s)<div class=\".*?b-article-main.*?<h2 class=\"subtitle\">(.*?)</h2>", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        String filename = matcher.group(1).trim();
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            setConfig();
            Matcher matcher = PlugUtils.matcher("(?s)<iframe src=\"(https?://media.cms.nova.cz/embed/\\w*\\?\\S*)\"", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Config URL not found");
            }
            String configUrl = matcher.group(1).trim();
            HttpMethod httpMethod = getMethodBuilder().setAction(configUrl).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            boolean hasVideo = false;
            if (!hasVideo) {
                hasVideo = this.getMP4Video(getContentAsString());
            }

            if (!hasVideo) {
                hasVideo = this.getHLSVideo2(getContentAsString());
            }

            if (!hasVideo) {
                hasVideo = this.getHLSVideo(getContentAsString());
            }

            if (!hasVideo) {
                throw new PluginImplementationException("Video URL not found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private boolean getMP4Video(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("(?s)src\\s*=\\s*(\\{\\s*\"mp4\"\\s*:\\s*\\[\\s*[^;]*);", content);
        if (matcher.find()) {
            String videoUrl = this.getVideoUrl(matcher.group(1).trim());
            if (videoUrl != null) {
                this.httpFile.setFileName(this.httpFile.getFileName() + ".mp4");
                HttpMethod httpMethod = this.getMethodBuilder().setReferer(this.fileURL).setAction(videoUrl).toGetMethod();
                if (!this.tryDownloadAndSaveFile(httpMethod)) {
                    this.checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }

                return true;
            }
        }

        return false;
    }

    private boolean getHLSVideo(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("\"hls\":\\s*\"(.*)\"", content);
        if (matcher.find()) {
            String playlist = matcher.group(1).trim();
            this.httpFile.setFileName(this.httpFile.getFileName() + ".ts");
            HlsDownloader hlsDownloader = new HlsDownloader(this.client, this.httpFile, this.downloadTask);
            hlsDownloader.tryDownloadAndSaveFile(playlist);

            return true;
        } else {
            return false;
        }
    }

    private boolean getHLSVideo2(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("Player\\.init\\('.*', personalizedAds \\? processAdTagModifier\\((\\{.*\\})\\) :", content);
        if (matcher.find()) {
            ObjectMapper objectMapper = (new JsonMapper()).getObjectMapper();
            JsonNode tracks = objectMapper.readTree(matcher.group(1).trim());
            JsonNode hls = tracks.findValue("HLS");
            String playlist = hls.findValue("src").getValueAsText();
            if (playlist != null) {
                this.httpFile.setFileName(this.httpFile.getFileName() + ".ts");
                HlsDownloader hlsDownloader = new HlsDownloader(this.client, this.httpFile, this.downloadTask);
                hlsDownloader.tryDownloadAndSaveFile(playlist);
                return true;
            }
        }

        return false;
    }

    public String getVideoUrl(String config) throws PluginImplementationException {
        ObjectMapper objectMapper = (new JsonMapper()).getObjectMapper();
        String videoUrl = null;

        try {
            JsonNode parsedConfig = objectMapper.readTree(config.replace('\'', '"'));
            JsonNode urlNodes = parsedConfig.findValue("mp4");
            if (urlNodes.size() == 0) {
                throw new PluginImplementationException("Video URL not found");
            } else if (urlNodes.size() == 1) {
                return urlNodes.get(0).getValueAsText();
            } else {
                String url;
                for (Iterator<JsonNode> i = urlNodes.iterator(); i.hasNext(); videoUrl = url) {
                    JsonNode urlNode = i.next();
                    url = urlNode.getValueAsText();
                    if (url.endsWith(this.config.getVideoQuality().getLabel() + ".mp4")) {
                        return url;
                    }
                }

                return videoUrl;
            }
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing video config", e);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("ale hledáte stránku, která neexistuje")
                || contentAsString.contains("Stránka nenalezena")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (contentAsString.contains("Platnost tohoto videa již vypršela")) {
            throw new URLNotAvailableAnymoreException("File not available anymore");
        }
    }

}
