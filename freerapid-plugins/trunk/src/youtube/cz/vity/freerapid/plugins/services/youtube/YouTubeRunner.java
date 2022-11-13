package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.youtube.srt.Transcription2SrtUtil;
import cz.vity.freerapid.plugins.video2audio.AbstractVideo2AudioRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kajda
 * @author JPEXS
 * @author ntoskrnl
 * @author tong2shot
 * @since 0.82
 */
class YouTubeRunner extends AbstractVideo2AudioRunner {
    private static final Logger logger = Logger.getLogger(YouTubeRunner.class.getName());
    private static final String DEFAULT_FILE_EXT = ".mp4";
    private static final String AUDIO_FILE_EXT = ".m4a";
    private static final String DASH_AUDIO_ITAG = "dashaudioitag";
    private static final String SECONDARY_DASH_AUDIO_ITAG = "secondarydashaudioitag"; //as backup, in case the primary fails

    private YouTubeSettingsConfig config;
    private int dashAudioItagValue = -1;
    private int secondaryDashAudioItagValue = -1;

    private String playerJsCode;
    private String playerJsFunction;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        addCookie(new Cookie(".youtube.com", "CONSENT", "YES+", "/", 86400, false));
        if (isAttributionLink()) {
            processAttributionLink();
        }
        if (isVideo()) {
            checkFileProblems();
        }
        if (isDashAudio()) {
            normalizeDashAudioUrl();
        }
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkName();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkUrl();
        setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36");
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        addCookie(new Cookie(".youtube.com", "CONSENT", "YES+", "/", 86400, false));
        setConfig();
        if (isAttributionLink()) {
            processAttributionLink();
        }
        if (checkSubtitles()) {
            return;
        }
        if (isVideo()) {
            checkFileProblems();
        }
        if (isDashAudio()) {
            normalizeDashAudioUrl();
        }
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            fileURL = method.getURI().toString();
            checkName();

            if (isUserPage()) {
                parseUserPage(getUserFromUrl());
                return;
            }
            if (isChannelPage()) {
                parseChannelPage(getChannelIdFromUrl());
                return;
            }
            if (isPlaylist()) {
                parsePlaylist();
                return;
            }
            if (isCourseList()) {
                parseCourseList();
                return;
            }

            Matcher matcher = getMatcherAgainstContent("\"([^\"]*?/base\\.js)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Player js url not found");
            }
            String playerJsUrl = matcher.group(1);

            bypassAgeVerification(method);

            matcher = getMatcherAgainstContent("\"formats\":(\\[.+?\\])");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting formats");
            }
            logger.info("Parsing formats");
            Map<Integer, YouTubeMedia> formatMap = parseFormats(matcher.group(1));

            if (config.isEnableDash()
                    || (config.getDownloadMode() == DownloadMode.convertToAudio)
                    || (config.getDownloadMode() == DownloadMode.extractAudio)) {
                matcher = getMatcherAgainstContent("\"adaptiveFormats\":(\\[.+?\\])");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting adaptive formats");
                }
                logger.info("Parsing adaptiveFormats");
                Map<Integer, YouTubeMedia> afMap = parseFormats(matcher.group(1));
                formatMap.putAll(afMap);
            }

            YouTubeMedia youTubeMedia;
            if (dashAudioItagValue == -1) { //not dash audio
                youTubeMedia = getSelectedYouTubeMedia(formatMap);
                if (youTubeMedia.isDashVideo()) {
                    queueDashAudio(formatMap, youTubeMedia);
                }
                queueSubtitle();
            } else { //dash audio
                youTubeMedia = formatMap.get(dashAudioItagValue);
                if (youTubeMedia == null) {
                    throw new PluginImplementationException("DASH audio stream with itag='" + dashAudioItagValue + "' not found");
                }
            }

            Container container = youTubeMedia.getContainer();
            httpFile.setFileName(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_FILE_EXT) + "$", youTubeMedia.getFileExt()));
            logger.info("Config setting : " + config);
            logger.info("Downloading media : " + youTubeMedia);
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(getGetMethod(getMediaUrl(playerJsUrl, youTubeMedia)))) {
                if (secondaryDashAudioItagValue != -1) { //try secondary dash audio
                    youTubeMedia = formatMap.get(secondaryDashAudioItagValue);
                    if (youTubeMedia == null) {
                        throw new PluginImplementationException("DASH audio stream with itag='" + secondaryDashAudioItagValue + "' not found");
                    }
                    logger.info("Primary DASH audio failed, trying to download secondary DASH audio");
                    logger.info("Downloading media : " + youTubeMedia);
                    if (!tryDownloadAndSaveFile(getGetMethod(getMediaUrl(playerJsUrl, youTubeMedia)))) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error downloading secondary DASH audio");
                    }
                } else {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }

            if (config.getDownloadMode() == DownloadMode.convertToAudio) {
                convertToAudio(youTubeMedia.getAudioBitrate(), (container == Container.mp4));
            } else if (config.getDownloadMode() == DownloadMode.extractAudio) {
                if ((container == Container.flv) && (youTubeMedia.getAudioEncoding() == AudioEncoding.MP3)) { //to mp3
                    //for MP3 track inside FLV container, convertToAudio is extraction, not conversion
                    convertToAudio(youTubeMedia.getAudioBitrate(), false);
                } else if ((container == Container.flv) || (container == Container.mp4)) { //to m4a
                    MediaOperations.extractAudio(downloadTask, container);
                } else {
                    throw new PluginImplementationException("Unsupported container : " + container);
                }
            } else if ((config.getDownloadMode() == DownloadMode.downloadVideo)
                    && config.isEnableInternalMultiplexer()
                    && youTubeMedia.isDash()) {
                MediaOperations.multiplexDash(downloadTask, youTubeMedia.isDashAudio());
            }

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getMediaUrl(String playerJsUrl, YouTubeMedia youTubeMedia) throws Exception {
        String videoURL = youTubeMedia.getUrl();
        String signatureInUrl = null;
        try {
            signatureInUrl = URLUtil.getQueryParams(videoURL, "UTF-8").get("signature");
        } catch (Exception e) {
            //
        }
        if (signatureInUrl == null) { //no "signature" param in url
            String signature;
            if (youTubeMedia.isCipherSignature()) { //signature is encrypted
                logger.info("Cipher signature : " + youTubeMedia.getSignature());
                signature = decipherSignature(playerJsUrl, youTubeMedia.getSignature());
                logger.info("Deciphered signature : " + signature);
            } else {
                signature = youTubeMedia.getSignature();
            }
            videoURL += "&sig=" + signature;
        }
        return videoURL;
    }

    private String decipherSignature(String playerJsUrl, String signature) throws Exception {
        if (playerJsCode == null || playerJsFunction == null) {
            HttpMethod method = getMethodBuilder().setAction(playerJsUrl).setReferer(fileURL).toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Failed to load player js");
            }
            Matcher matcher = getMatcherAgainstContent("=(function\\(a\\)\\{a=a\\.split\\(\"\"\\);(..)\\.(?:[^\r\n]+?)return a.join\\(\"\"\\)});");
            if (!matcher.find()) {
                throw new PluginImplementationException("Failed to parse player js (1)");
            }
            playerJsCode = matcher.group(1);
            matcher = getMatcherAgainstContent("(?s)(var " + matcher.group(2) + "=.+?\\}\\};)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Failed to parse player js (2)");
            }
            playerJsFunction = matcher.group(1);
        }

        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine engine = mgr.getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("JavaScript engine not found");
        }
        try {
            engine.eval(playerJsFunction);
            return engine.eval("(" + playerJsCode + ")(\"" + signature + "\");").toString();
        } catch (Exception e) {
            logger.warning("function:\n" + playerJsFunction);
            logger.warning("code:\n" + playerJsCode);
            throw new PluginImplementationException("Failed to execute player js", e);
        }
    }

    private void checkFileProblems() throws Exception {
        /* APIv2 has been discontinued
        logger.info("Checking file problems");
        HttpMethod method = getGetMethod(String.format("https://gdata.youtube.com/feeds/api/videos/%s?v=2", fileURL));
        int httpCode = client.makeRequest(method, true);
        if ((httpCode == HttpStatus.SC_NOT_FOUND)
                || (httpCode == HttpStatus.SC_FORBIDDEN)
                || getContentAsString().contains("ResourceNotFoundException")
                || getContentAsString().contains("ServiceForbiddenException")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        */
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //
    }

    private void checkEmbeddingProblems() throws ErrorDuringDownloadingException {
        String content = getContentAsString();
        if (content.contains("status=fail")) {
            throw new PluginImplementationException("This video cannot be embedded. Failed to bypass age verification");
        }
    }

    private void checkName() throws ErrorDuringDownloadingException, IOException {
        if (getContentAsString().contains("https:\\/\\/www.youtube.com\\/verify_controversy?next_url=")) {
            Matcher matcher = getMatcherAgainstContent("\"(https?:\\\\/\\\\/www\\.youtube\\.com\\\\/verify_controversy\\?next_url=[^\"]+)\"");
            if (!matcher.find()) {
                throw new PluginImplementationException("Verifier URL not found");
            }
            String verifierUrl = matcher.group(1).replace("\\/", "/");
            if (!makeRedirectedRequest(getGetMethod(verifierUrl))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
        }

        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        } catch (final PluginImplementationException e) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "</title>");
        }
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName())).replaceFirst("- YouTube$", "").trim();
        if (dashAudioItagValue != -1) {
            fileName += AUDIO_FILE_EXT;
        } else if (isVideo()) {
            fileName += DEFAULT_FILE_EXT;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkUrl() {
        fileURL = fileURL
                .replaceFirst("http://", "https://")
                .replaceFirst("://m\\.", "://www.")
                .replaceFirst("//(www\\.)?youtu\\.be/", "//www.youtube.com/watch?v=")
                .replaceFirst("/v/", "/watch?v=")
                .replaceFirst("/embed/", "/watch?v=")
                .replaceFirst("(\\?.*?)\\?", "$1&");
    }

    @Override
    protected String getBaseURL() {
        return "https://www.youtube.com/";
    }

    private void setConfig() throws Exception {
        final YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private Map<Integer, YouTubeMedia> parseFormats(String content) throws Exception {
        Map<Integer, YouTubeMedia> fmtStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
        JsonNode rootNode;
        try {
            rootNode = new ObjectMapper().readTree(content);
        } catch (Exception e) {
            logger.warning("Error parsing JSON: " + content);
            throw new PluginImplementationException("Error parsing JSON", e);
        }
        for (JsonNode node : rootNode) {
            try {
                JsonNode itagNode = node.get("itag");
                JsonNode urlNode = node.get("url");
                JsonNode signatureCipherNode = node.get("signatureCipher");
                JsonNode signatureNode = node.get("signature");
                JsonNode sigNode = node.get("sig");
                JsonNode sNode = node.get("s");
                if (itagNode == null || !itagNode.isNumber()) {
                    throw new PluginImplementationException("Invalid media (itag): " + node);
                }
                int itag = itagNode.getIntValue();
                String url;
                String signature;
                boolean cipherSig = false;
                if (urlNode != null) {
                    url = urlNode.getTextValue();
                    if (signatureNode != null) {
                        signature = signatureNode.getTextValue();
                    } else if (sigNode != null) {
                        signature = sigNode.getTextValue();
                    } else if (sNode != null) {
                        signature = sNode.getTextValue();
                        cipherSig = true;
                    } else {
                        signature = URLUtil.getQueryParams(url, "UTF-8").get("sig");
                        if (signature == null) {
                            throw new PluginImplementationException("Invalid media (signature): " + node);
                        }
                    }
                } else if (signatureCipherNode != null) {
                    Matcher matcher = PlugUtils.matcher("url=([^&]+)", signatureCipherNode.getTextValue());
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Invalid media (signatureCipher 1): " + node);
                    }
                    url = URLDecoder.decode(matcher.group(1), "UTF-8");
                    matcher = PlugUtils.matcher("s=([^&]+)", signatureCipherNode.getTextValue());
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Invalid media (signatureCipher 2): " + node);
                    }
                    signature = URLDecoder.decode(matcher.group(1), "UTF-8");
                    cipherSig = true;
                } else {
                    throw new PluginImplementationException("Invalid media (url): " + node);
                }
                YouTubeMedia youTubeMedia = new YouTubeMedia(itag, url, signature, cipherSig);
                logger.info("Found " + (youTubeMedia.isDash() ? "DASH " : "") + "media : " + youTubeMedia);
                fmtStreamMap.put(itag, youTubeMedia);
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
        return fmtStreamMap;
    }

    private YouTubeMedia getSelectedYouTubeMedia(Map<Integer, YouTubeMedia> ytMediaMap) throws ErrorDuringDownloadingException {
        if (ytMediaMap.isEmpty()) {
            throw new PluginImplementationException("No available YouTube media");
        }
        int selectedItag = -1;

        if ((config.getDownloadMode() == DownloadMode.convertToAudio) || (config.getDownloadMode() == DownloadMode.extractAudio)) {
            final int LOWER_QUALITY_PENALTY = 5;
            final boolean isConvertToAudio = (config.getDownloadMode() == DownloadMode.convertToAudio);
            int configAudioBitrate = (isConvertToAudio ? config.getConvertAudioQuality().getBitrate() : config.getExtractAudioQuality().getBitrate());

            //select audio bitrate
            int selectedAudioBitrate = -1;
            int weight = Integer.MAX_VALUE;
            for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                if (ytMedia.isDashVideo()
                        || (isConvertToAudio && !ytMedia.isVid2AudSupported())
                        || (!isConvertToAudio && !ytMedia.isAudioExtractSupported())) {
                    continue;
                }
                int audioBitrate = ytMedia.getAudioBitrate();
                int deltaAudioBitrate = audioBitrate - configAudioBitrate;
                int tempWeight = (deltaAudioBitrate < 0 ? Math.abs(deltaAudioBitrate) + LOWER_QUALITY_PENALTY : deltaAudioBitrate);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedAudioBitrate = audioBitrate;
                }
            }
            if (selectedAudioBitrate == -1) {
                throw new PluginImplementationException("Unable to select audio bitrate");
            }

            //calc (the lowest) video quality to get the fittest itag  -> select video quality
            //prefer DASH audio (videoQuality==-1)
            weight = Integer.MAX_VALUE;
            for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                if (ytMedia.isDashVideo()
                        || (isConvertToAudio && !ytMedia.isVid2AudSupported())
                        || (!isConvertToAudio && !ytMedia.isAudioExtractSupported())) {
                    continue;
                }
                if (ytMedia.getAudioBitrate() == selectedAudioBitrate) {
                    int tempWeight = ytMedia.getVideoQuality();
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedItag = ytMedia.getItag();
                    }
                }
            }
            if (selectedItag == -1) {
                throw new PluginImplementationException("Unable to select YouTube media");
            }

        } else { //download video
            //select video quality
            VideoQuality configVideoQuality = config.getVideoQuality();
            if (configVideoQuality == VideoQuality.Highest) {
                selectedItag = Collections.max(ytMediaMap.values(), new Comparator<YouTubeMedia>() {
                    @Override
                    public int compare(YouTubeMedia o1, YouTubeMedia o2) {
                        return Integer.valueOf(o1.getVideoQuality()).compareTo(o2.getVideoQuality());
                    }
                }).getItag();
            } else if (configVideoQuality == VideoQuality.Lowest) {
                for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                    if (ytMedia.isDash()) { //skip DASH
                        continue;
                    }
                    selectedItag = ytMedia.getItag(); //last key of fmtStreamMap
                }
            } else {
                final int LOWER_QUALITY_PENALTY = 10;
                int weight = Integer.MAX_VALUE;
                for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                    if (config.isEnableInternalMultiplexer() && ytMedia.isDashVideo() && ytMedia.getVideoEncoding() != VideoEncoding.H264) {
                        continue;
                    }
                    int deltaQ = ytMedia.getVideoQuality() - configVideoQuality.getQuality();
                    int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedItag = ytMedia.getItag();
                    }
                }
            }
            if (selectedItag == -1) {
                throw new PluginImplementationException("Unable to select YouTube media");
            }
            final int selectedVideoQuality = ytMediaMap.get(selectedItag).getVideoQuality();

            //select frame rate
            selectedItag = -1;
            final FrameRate configFrameRate = config.getFrameRate();
            int weight = Integer.MIN_VALUE;
            for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                if (config.isEnableInternalMultiplexer() && ytMedia.isDashVideo() && ytMedia.getVideoEncoding() != VideoEncoding.H264) {
                    continue;
                }
                if (ytMedia.getVideoQuality() == selectedVideoQuality) {
                    int tempWeight = 0;
                    int frameRate = ytMedia.getFrameRate();
                    if (configFrameRate.getFrameRate() == frameRate) {
                        tempWeight = 100;
                    } else if (frameRate == FrameRate._60.getFrameRate()) {
                        tempWeight = 50;
                    } else if (frameRate == FrameRate._30.getFrameRate()) {
                        tempWeight = 49;
                    }
                    if (tempWeight > weight) {
                        weight = tempWeight;
                        selectedItag = ytMedia.getItag();
                    }
                }
            }
            if (selectedItag == -1) {
                throw new PluginImplementationException("Unable to select YouTube media");
            }
            final int selectedFrameRate = ytMediaMap.get(selectedItag).getFrameRate();

            //select container
            final Container configContainer = config.getContainer();
            if (configContainer != Container.Any) {
                selectedItag = -1;
                weight = Integer.MIN_VALUE;
                for (YouTubeMedia ytMedia : ytMediaMap.values()) {
                    if (config.isEnableInternalMultiplexer() && ytMedia.isDashVideo() && ytMedia.getVideoEncoding() != VideoEncoding.H264) {
                        continue;
                    }
                    if ((ytMedia.getVideoQuality() == selectedVideoQuality) && (ytMedia.getFrameRate() == selectedFrameRate)) {
                        int tempWeight = 0;
                        Container container = ytMedia.getContainer();
                        if (configContainer == container) {
                            tempWeight = 100;
                        } else if (container == Container.mp4) { //mp4 > flv > webm > 3gp > DASH
                            tempWeight = 50;
                        } else if (container == Container.flv) {
                            tempWeight = 49;
                        } else if (container == Container.webm) {
                            tempWeight = 48;
                        } else if (container == Container._3gp) {
                            tempWeight = 47;
                        }
                        if (ytMedia.isDash()) {
                            tempWeight -= 20;
                        }
                        if (tempWeight > weight) {
                            weight = tempWeight;
                            selectedItag = ytMedia.getItag();
                        }
                    }
                }
                if (selectedItag == -1) {
                    throw new PluginImplementationException("Unable to select YouTube media");
                }
            }
        }

        return ytMediaMap.get(selectedItag);
    }

    private void queueDashAudio(Map<Integer, YouTubeMedia> afDashStreamMap, YouTubeMedia selectedDashVideoStream) throws Exception {
        int selectedItag = -1;
        int secondaryItag = -1; //as backup, in case the primary fails
        int weight = Integer.MIN_VALUE;
        int secondaryWeight = Integer.MIN_VALUE;
        for (YouTubeMedia ytMedia : afDashStreamMap.values()) {
            if (!ytMedia.isDashAudio() || (config.isEnableInternalMultiplexer() && ytMedia.getAudioEncoding() != AudioEncoding.AAC)) { //skip non DASH audio or non AAC
                continue;
            }
            int tempWeight = Integer.MIN_VALUE;
            //audio bitrate as criteria, it'd be better if we have videoQ-audioBitRate map for the qualifier
            switch (ytMedia.getAudioBitrate()) {
                case 128:
                    tempWeight = 50;
                    break;
                case 256:
                    tempWeight = (selectedDashVideoStream.getVideoQuality() >= VideoQuality._720.getQuality() ? 51 : 47);
                    break;
                case 48:
                    tempWeight = 48;
                    break;
            }
            if (tempWeight > weight) {
                secondaryWeight = weight;
                weight = tempWeight;
                secondaryItag = selectedItag;
                selectedItag = ytMedia.getItag();
            } else if (tempWeight > secondaryWeight) {
                secondaryWeight = tempWeight;
                secondaryItag = ytMedia.getItag();
            }
        }
        if (selectedItag == -1) {
            throw new PluginImplementationException("DASH audio pair not found");
        }
        logger.info("Queueing DASH audio stream : " + afDashStreamMap.get(selectedItag));
        List<URI> uriList = new LinkedList<URI>();
        String url = fileURL + "&" + DASH_AUDIO_ITAG + "=" + selectedItag + (secondaryItag == -1 ? "" : "&" + SECONDARY_DASH_AUDIO_ITAG + "=" + secondaryItag);
        try {
            uriList.add(new URI(url));
        } catch (final URISyntaxException e) {
            LogUtils.processException(logger, e);
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private boolean isDashAudio() {
        return fileURL.contains("&" + DASH_AUDIO_ITAG + "=");
    }

    private void normalizeDashAudioUrl() throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("&" + DASH_AUDIO_ITAG + "=(\\d+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("DASH audio itag param not found");
        }
        dashAudioItagValue = Integer.parseInt(matcher.group(1));

        matcher = PlugUtils.matcher("&" + SECONDARY_DASH_AUDIO_ITAG + "=(\\d+)", fileURL);
        if (matcher.find()) {
            secondaryDashAudioItagValue = Integer.parseInt(matcher.group(1));
        }
        fileURL = fileURL.replaceFirst("&" + DASH_AUDIO_ITAG + "=.+", ""); //remove dash audio itag param
    }

    private boolean isVideo() {
        return !isUserPage() && !isPlaylist() && !isCourseList() && !isSubtitles();
    }

    private boolean isAttributionLink() {
        return fileURL.contains("/attribution_link?");
    }

    private void processAttributionLink() throws Exception {
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        fileURL = method.getURI().toString();
    }

    private String getIdFromUrl(String url) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("(?:[\\?&]v=|\\.be/)([^\\?&#]+)", url);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video id");
        }
        return matcher.group(1);
    }

    private List<URI> getUriListFromContent(String content) throws ErrorDuringDownloadingException, URISyntaxException, IOException {
        List<URI> uriList = new LinkedList<URI>();
        Matcher matcher = PlugUtils.matcher("<a href=\"/watch(\\?v=[^\"]+?)\"", content);
        while (matcher.find()) {
            String videoId = getIdFromUrl(matcher.group(1));
            URI uri = new URI("https://www.youtube.com/watch?v=" + videoId);
            if (!uriList.contains(uri)) {
                uriList.add(uri);
            }
        }
        return uriList;
    }

    private void parseContinuation(List<URI> uriList) throws IOException, ErrorDuringDownloadingException, URISyntaxException {
        Matcher matcher = getMatcherAgainstContent("\"(/browse_ajax\\?action_continuation=[^\"]+?)\"");
        if (matcher.find()) {
            String continuationUrl = PlugUtils.replaceEntities(matcher.group(1));
            ObjectMapper mapper = new ObjectMapper();
            do {
                if (!makeRedirectedRequest(getGetMethod("https://www.youtube.com" + continuationUrl))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();

                JsonNode rootNode;
                try {
                    rootNode = mapper.readTree(getContentAsString());
                } catch (IOException e) {
                    throw new PluginImplementationException("Error parsing continuation content", e);
                }
                JsonNode contentHtmlNode = rootNode.get("content_html");
                JsonNode loadMoreWidgetHtmlNode = rootNode.get("load_more_widget_html");
                if (contentHtmlNode != null) {
                    uriList.addAll(getUriListFromContent(contentHtmlNode.getTextValue()));
                }
                if (loadMoreWidgetHtmlNode != null) {
                    matcher = PlugUtils.matcher("\"(/browse_ajax\\?action_continuation=[^\"]+?)\"", loadMoreWidgetHtmlNode.getTextValue());
                    if (matcher.find()) {
                        continuationUrl = PlugUtils.replaceEntities(matcher.group(1));
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } while (true);
        }
    }

    private void queueLinks(final List<URI> uriList) throws PluginImplementationException {
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No video links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(uriList.size() + " videos added");
    }

    private boolean isUserPage() {
        return fileURL.contains("/user/");
    }

    private String getUserFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/user/([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting user id");
        }
        return matcher.group(1);
    }

    //user uploaded video
    private void parseUserPage(final String user) throws Exception {
        String userVideosUrl = String.format("https://www.youtube.com/user/%s/videos", user);
        if (!makeRedirectedRequest(getGetMethod(userVideosUrl))) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        List<URI> uriList = getUriListFromContent(getContentAsString());
        parseContinuation(uriList);
        // YouTube returns the videos in descending date order, which is a bit illogical.
        // If the user wants them that way, don't reverse.
        if (!config.isReversePlaylistOrder()) {
            Collections.reverse(uriList);
        }
        queueLinks(uriList);
    }

    private void parseAndQueueLinks() throws ErrorDuringDownloadingException, URISyntaxException, IOException {
        List<URI> uriList = getUriListFromContent(getContentAsString());
        parseContinuation(uriList);
        queueLinks(uriList);
    }

    private boolean isChannelPage() {
        return fileURL.contains("/channel/");
    }

    private String getChannelIdFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("/channel/([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting channel id");
        }
        return matcher.group(1);
    }

    //Channel
    private void parseChannelPage(final String channelId) throws Exception {
        String channelVideosUrl = String.format("https://www.youtube.com/channel/%s/videos", channelId);
        if (!makeRedirectedRequest(getGetMethod(channelVideosUrl))) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        parseAndQueueLinks();
    }

    private boolean isPlaylist() {
        return fileURL.contains("/playlist?");
    }

    //Playlist
    private void parsePlaylist() throws Exception {
        parseAndQueueLinks();
    }

    private boolean isCourseList() {
        return fileURL.contains("/course?list=");
    }

    //Course list contains video playlist, lecture materials, and course materials
    private void parseCourseList() throws Exception {
        //Step #1 queue video playlist related to the course
        parseAndQueueLinks();

        /*
        Lecture materials and course materials - pending.
        APIv2 has been deprecated.
        */
    }

    private boolean isSubtitles() {
        return fileURL.contains("#subtitles:");
    }

    private boolean checkSubtitles() throws Exception {
        Matcher matcher = PlugUtils.matcher("#subtitles:(.*?):(.+)", fileURL);
        if (matcher.find()) {
            runCheck();
            final String lang = matcher.group(1);
            String fileExtension;
            if (!lang.isEmpty()) {
                fileExtension = "." + lang + ".srt";
            } else {
                fileExtension = ".srt";
            }
            httpFile.setFileName(httpFile.getFileName() + fileExtension);
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(httpFile.getFileName()), "_"));

            final String url = "http://www.youtube.com/api/timedtext?type=track" + matcher.group(2);
            final HttpMethod method = getGetMethod(url);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            try {
                final byte[] converted = Transcription2SrtUtil.convert(getContentAsString()).getBytes("UTF-8");
                httpFile.setFileSize(converted.length);
                downloadTask.saveToFile(new ByteArrayInputStream(converted));
            } catch (final Exception e) {
                LogUtils.processException(logger, e);
                throw new PluginImplementationException("Error converting and saving subtitles", e);
            }
            return true;
        }
        return false;
    }

    private void queueSubtitle() throws Exception {
        if (config.isDownloadSubtitles() && isVideo()) {
            final String id = getIdFromUrl(fileURL);
            HttpMethod method = getGetMethod("http://www.youtube.com/api/timedtext?type=list&v=" + id);
            if (makeRedirectedRequest(method)) {
                final List<URI> list = new LinkedList<URI>();
                Matcher matcher = getMatcherAgainstContent("<track id=\"\\d*\" name=\"(.*?)\" lang_code=\"(.*?)\"");
                while (matcher.find()) {
                    final String name = matcher.group(1);
                    final String lang = matcher.group(2);
                    final String url = fileURL + "#subtitles:" + lang + ":&v=" + id + "&name=" + URLEncoder.encode(name, "UTF-8") + "&lang=" + lang;
                    try {
                        list.add(new URI(url));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
                if (!list.isEmpty()) {
                    getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                } else {
                    String captionTracks = null;
                    matcher = getMatcherAgainstContent("\"caption_tracks\":\"(.*?)\"");
                    if (matcher.find() && !matcher.group(1).trim().isEmpty()) {
                        captionTracks = matcher.group(1);
                    }
                    //it's probably possible that caption tracks contain more than one language,
                    //at this moment only one language is supported, can't find sample for multilanguage
                    if (captionTracks != null) {
                        try {
                            String captionTracksComponents[] = PlugUtils.unescapeUnicode(captionTracks).split("&");
                            String url = null;
                            String lang = null;
                            for (String captionTracksComponent : captionTracksComponents) {
                                String[] captionTracksComponentParts = captionTracksComponent.split("=");
                                String key = captionTracksComponentParts[0];
                                String value = captionTracksComponentParts[1];
                                if (key.equals("lc")) {
                                    lang = value;
                                } else if (key.equals("u")) {
                                    url = URLDecoder.decode(value, "UTF-8");
                                }
                            }
                            if (url != null) {
                                //silent download, because it contains "expire" and "signature" params
                                String fileExtension;
                                if (lang != null) {
                                    fileExtension = "." + lang + ".srt";
                                } else {
                                    fileExtension = ".srt";
                                }
                                method = getGetMethod(url);
                                if (makeRedirectedRequest(method)) {
                                    String fnameNoExt = PlugUtils.unescapeHtml(URLDecoder.decode(HttpUtils.replaceInvalidCharsForFileSystem(
                                            httpFile.getFileName().replaceFirst("\\.[^\\.]{3,4}$", ""), "_"), "UTF-8"));
                                    String fnameOutput = fnameNoExt + fileExtension;
                                    File outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
                                    BufferedWriter bw = null;
                                    int outputFileCounter = 2;
                                    try {
                                        while (outputFile.exists()) {
                                            fnameOutput = fnameNoExt + "-" + outputFileCounter++ + fileExtension;
                                            outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
                                        }
                                        bw = new BufferedWriter(new FileWriter((outputFile)));
                                        bw.write(Transcription2SrtUtil.convert(getContentAsString()));
                                    } catch (Exception e) {
                                        LogUtils.processException(logger, e);
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
                        } catch (Exception e) {
                            LogUtils.processException(logger, e);
                        }
                    }
                }
            }
        }
    }

    private void bypassAgeVerification(HttpMethod method) throws Exception {
        if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_age.*")
                || getContentAsString().contains("watch7-player-age-gate-content")
                || getContentAsString().contains("Sign in to confirm your age")
                || getContentAsString().contains("<script>window.location = \"https:\\/\\/www.youtube.com\\/verify_age")
                || getContentAsString().contains("<script>window.location = \"http:\\/\\/www.youtube.com\\/verify_age")
                || getContentAsString().contains("/verify_controversy?action_confirm")) {
            logger.info("Trying to bypass age verification");
            //Request embed format to bypass age verification
            String videoId = getIdFromUrl(fileURL);
            method = getMethodBuilder()
                    .setReferer("https://www.youtube.com/embed/" + videoId)
                    .setAction("https://www.youtube.com/get_video_info")
                    .setParameter("html5", "1")
                    .setParameter("video_id", videoId)
                    .setParameter("cpn", "Gbrb-xC5Q_KYOrTg")
                    .setParameter("eurl", "")
                    .setParameter("el", "embedded")
                    .setParameter("hl", "en_US")
                    .setParameter("sts", "17579")
                    .setParameter("c", "WEB_EMBEDDED_PLAYER")
                    .setParameter("cver", "20180222")
                    .setParameter("cplayer", "UNIPLAYER")
                    .setParameter("cbr", "Chrome")
                    .setParameter("cbrver", "64.0.3282.170")
                    .setParameter("cos", "Windows")
                    .setParameter("cosver", "6.1")
                    .setParameter("width", "1920")
                    .setParameter("height", "1080")
                    .setParameter("ei", "ZnWRWpG6EtLOyAWOs5ywBA")
                    .setParameter("iframe", "1")
                    .setParameter("embed_config", "%7B%7D")
                    .toGetMethod();
            setTextContentTypes("application/x-www-form-urlencoded");
            if (!makeRedirectedRequest(method)) {
                checkEmbeddingProblems();
                throw new ServiceConnectionProblemException();
            }
            checkEmbeddingProblems();
        } else if (getContentAsString().contains("I confirm that I am 18 years of age or older")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL + "&has_verified=1"))) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

}
