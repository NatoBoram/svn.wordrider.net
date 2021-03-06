package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.youtube.srt.Transcription2SrtUtil;
import cz.vity.freerapid.plugins.video2audio.AbstractVideo2AudioRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
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
    private static final String DEFAULT_FILE_EXT = ".flv";
    private static final String AUDIO_FILE_EXT = ".m4a";
    private static final String DASH_AUDIO_ITAG = "dashaudioitag";
    private static final String SECONDARY_DASH_AUDIO_ITAG = "secondarydashaudioitag"; //as backup, in case the primary fails

    private YouTubeSettingsConfig config;
    private int dashAudioItagValue = -1;
    private int secondaryDashAudioItagValue = -1;
    private String swfUrl = null;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
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
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
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

            bypassAgeVerification(method);
            Matcher matcher;
            String mainpageContent = getContentAsString();
            if (swfUrl == null) {
                matcher = getMatcherAgainstContent("\"url\":\\s*?\"([^\"]+?)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("SWF URL not found");
                }
                swfUrl = matcher.group(1).replace("\\/", "/");
            }
            //"url_encoded_fmt_stream_map":"type=vi...    " //normal
            //url_encoded_fmt_stream_map=url%3Dhttp%253A... & //embedded
            matcher = getMatcherAgainstContent("\"?url_encoded_fmt_stream_map\"?(=|:)(?: ?\")?([^&\"$]+)(?:\"|&|$)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Fmt stream map not found");
            }
            String fmtStreamMapContent = matcher.group(1).equals(":") ? matcher.group(2) : URLDecoder.decode(matcher.group(2), "UTF-8");
            YouTubeSigDecipher ytSigDecipher;
            Map<Integer, YouTubeMedia> afDashStreamMap = new LinkedHashMap<Integer, YouTubeMedia>(); //union between afStreamMap and dashStreamMap
            logger.info("SWF URL : " + swfUrl);
            if (config.isEnableDash()
                    || (config.getDownloadMode() == DownloadMode.convertToAudio)
                    || (config.getDownloadMode() == DownloadMode.extractAudio)) {
                Map<Integer, YouTubeMedia> afStreamMap = null; //streams from 'adaptive_fmts', not to be confused with afDashStreamMap
                Map<Integer, YouTubeMedia> dashStreamMap = null; //streams from 'dashmpd'
                if (getContentAsString().contains("adaptive_fmts")) {
                    matcher = getMatcherAgainstContent("\"?adaptive_fmts\"?(=|:)(?: ?\")?([^&\"$]+)(?:\"|&|$)");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Error getting adaptive fmts");
                    }
                    String afContent = matcher.group(1).equals(":") ? matcher.group(2) : URLDecoder.decode(matcher.group(2), "UTF-8");
                    logger.info("Parsing adaptive_fmts");
                    //'adaptive_fmts' parser is similar with 'url_encoded_fmt_stream_map' parser
                    afStreamMap = getFmtStreamMap(afContent);
                }
                if (getContentAsString().contains("dashmpd")) {
                    matcher = getMatcherAgainstContent("\"?dashmpd\"?(=|:)(?: ?\")?([^&\"$]+)(?:\"|&|$)");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Error getting dash URL");
                    }
                    String dashUrl = matcher.group(1).equals(":") ? matcher.group(2).replace("\\/", "/") : URLDecoder.decode(matcher.group(2), "UTF-8");
                    logger.info("DASH URL : " + dashUrl);
                    if (!(dashUrl.contains("/sig/") || dashUrl.contains("/signature/"))) {  //cipher signature
                        matcher = PlugUtils.matcher("/s/([^/]+)", dashUrl);
                        if (!matcher.find()) {
                            throw new PluginImplementationException("Cipher signature not found");
                        }
                        String signature = matcher.group(1);
                        ytSigDecipher = YouTubeSigDecipher.getInstance(swfUrl, client);
                        try {
                            signature = ytSigDecipher.decipher(signature); //deciphered signature
                        } catch (Exception e) {
                            logger.warning("SWF URL: " + swfUrl);
                            throw e;
                        }
                        dashUrl = dashUrl.replaceFirst("/s/[^/]+", "/signature/" + signature);
                        logger.info("DASH URL (deciphered) : " + dashUrl);
                    }
                    method = getMethodBuilder().setReferer(fileURL).setAction(dashUrl).toGetMethod();
                    setTextContentTypes("video/vnd.mpeg.dash.mpd");
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                    logger.info("Parsing dashmpd");
                    dashStreamMap = getDashStreamMap(getContentAsString());
                }
                if (afStreamMap != null) {
                    afDashStreamMap.putAll(afStreamMap);
                }
                if (dashStreamMap != null) {
                    afDashStreamMap.putAll(dashStreamMap);
                }
            }

            YouTubeMedia youTubeMedia;
            if (dashAudioItagValue == -1) { //not dash audio
                logger.info("Parsing url_encoded_fmt_stream_map");
                Map<Integer, YouTubeMedia> fmtStreamMap = getFmtStreamMap(fmtStreamMapContent); //streams from 'url_encoded_fmt_stream_map'
                Map<Integer, YouTubeMedia> ytStreamMap = new LinkedHashMap<Integer, YouTubeMedia>(); //union between fmtStreamMap and afDashStreamMap
                ytStreamMap.putAll(fmtStreamMap); //put fmtStreamMap on top of the map
                ytStreamMap.putAll(afDashStreamMap);
                youTubeMedia = getSelectedYouTubeMedia(ytStreamMap);
                if ((youTubeMedia.isDashVideo())) {
                    //fmtStreamMap doesn't contain dash, use afDashStreamMap instead of ytStreamMap
                    queueDashAudio(afDashStreamMap, youTubeMedia);
                }
                queueSubtitle(mainpageContent);
            } else { //dash audio
                //at this moment dash audio set from afStreamMap is subset of dash audio set from dashStreamMap,
                //so it is ok to get youTubeMedia from dashStreamMap, but it's safer to get it from afDashStream.
                youTubeMedia = afDashStreamMap.get(dashAudioItagValue);
                if (youTubeMedia == null) {
                    throw new PluginImplementationException("DASH audio stream with itag='" + dashAudioItagValue + "' not found");
                }
            }

            Container container = youTubeMedia.getContainer();
            httpFile.setFileName(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_FILE_EXT) + "$", youTubeMedia.getFileExt()));
            logger.info("Config setting : " + config);
            logger.info("Downloading media : " + youTubeMedia);
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(getGetMethod(getMediaUrl(swfUrl, youTubeMedia)))) {
                if (secondaryDashAudioItagValue != -1) { //try secondary dash audio
                    youTubeMedia = afDashStreamMap.get(secondaryDashAudioItagValue);
                    if (youTubeMedia == null) {
                        throw new PluginImplementationException("DASH audio stream with itag='" + secondaryDashAudioItagValue + "' not found");
                    }
                    logger.info("Primary DASH audio failed, trying to download secondary DASH audio");
                    logger.info("Downloading media : " + youTubeMedia);
                    if (!tryDownloadAndSaveFile(getGetMethod(getMediaUrl(swfUrl, youTubeMedia)))) {
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

    private String getMediaUrl(String swfUrl, YouTubeMedia youTubeMedia) throws Exception {
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
                YouTubeSigDecipher ytSigDecipher = YouTubeSigDecipher.getInstance(swfUrl, client);
                try {
                    signature = ytSigDecipher.decipher(youTubeMedia.getSignature());
                } catch (Exception e) {
                    logger.warning("SWF URL: " + swfUrl);
                    throw e;
                }
                logger.info("Deciphered signature : " + signature);
            } else {
                signature = youTubeMedia.getSignature();
            }
            videoURL += "&signature=" + signature;
        }
        return videoURL;
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

    private void checkName() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        } catch (final PluginImplementationException e) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- YouTube\n</title>");
        }
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (dashAudioItagValue != -1) {
            fileName += AUDIO_FILE_EXT;
        } else if (isVideo()) {
            fileName += DEFAULT_FILE_EXT;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        final YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getLocalConfig(httpFile);
    }

    private Map<Integer, YouTubeMedia> getFmtStreamMap(String content) {
        Map<Integer, YouTubeMedia> fmtStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
        String fmtStreams[] = content.split(",");
        for (String fmtStream : fmtStreams) {
            try {
                String fmtStreamComponents[] = PlugUtils.unescapeUnicode(fmtStream).split("&"); // \u0026 as separator
                int itag = -1;
                String url = null;
                String signature = null;
                boolean cipherSig = false;
                for (String fmtStreamComponent : fmtStreamComponents) {
                    String fmtStreamComponentParts[] = fmtStreamComponent.split("=");
                    String key = fmtStreamComponentParts[0];
                    String value = fmtStreamComponentParts[1];
                    if (key.equals("itag")) {
                        itag = Integer.parseInt(value);
                    } else if (key.equals("url")) {
                        url = URLDecoder.decode(value, "UTF-8");
                        String sigParam = null;
                        try {
                            sigParam = URLUtil.getQueryParams(url, "UTF-8").get("signature");
                        } catch (Exception e) {
                            //
                        }
                        if (sigParam != null) { //contains "signature" param
                            signature = sigParam;
                        }
                    } else if (key.equals("signature") || key.equals("sig") || key.equals("s")) {
                        signature = value;
                        cipherSig = key.equals("s");
                    }
                }
                if (itag == -1 || url == null || signature == null) {
                    throw new PluginImplementationException("Invalid YouTube media : " + fmtStream);
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

    private Map<Integer, YouTubeMedia> getDashStreamMap(String dashContent) throws Exception {
        Map<Integer, YouTubeMedia> dashStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
        if (dashContent != null) {
            try {
                final NodeList representationElements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(dashContent
                        .getBytes("UTF-8"))).getElementsByTagName("Representation");
                for (int i = 0, n = representationElements.getLength(); i < n; i++) {
                    try {
                        final Element representationElement = (Element) representationElements.item(i);
                        int itag;
                        String url;
                        String signature = null;
                        boolean cipherSig = false; //assume there is cipher sig in the future, couldn't find sample at the moment.
                        String sigParam = null;

                        itag = Integer.parseInt(representationElement.getAttribute("id"));
                        url = representationElement.getElementsByTagName("BaseURL").item(0).getTextContent();
                        try {
                            sigParam = URLUtil.getQueryParams(url, "UTF-8").get("signature");
                        } catch (Exception e) {
                            //
                        }
                        if (sigParam != null) { //contains "signature" param
                            signature = sigParam;
                        }
                        if (signature == null) {
                            throw new PluginImplementationException("Invalid YouTube DASH media : " + representationElement.getTextContent());
                        }
                        YouTubeMedia youTubeMedia = new YouTubeMedia(itag, url, signature, cipherSig);
                        logger.info("Found DASH media : " + youTubeMedia);
                        dashStreamMap.put(itag, youTubeMedia);
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                    }
                }
            } catch (Exception e) {
                throw new PluginImplementationException("Error parsing DASH descriptor", e);
            }
        }
        return dashStreamMap;
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
                    if (ytMedia.isDashVideo() && ytMedia.getVideoEncoding() != VideoEncoding.H264) {
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
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueueNextTo(httpFile, uriList, true);
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
            ObjectMapper mapper = new JsonMapper().getObjectMapper();
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

    private void queueSubtitle(String mainpageContent) throws Exception {
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
                    try {
                        captionTracks = PlugUtils.getStringBetween(mainpageContent, "\"caption_tracks\":\"", "\"");
                    } catch (PluginImplementationException e) {
                        //
                    }
                    //it's probably possible that caption tracks contain more than one language,
                    //at this moment only one language is supported, can't find sample for multilanguage
                    if (captionTracks != null) {
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
                || getContentAsString().contains("<script>window.location = \"http:\\/\\/www.youtube.com\\/verify_age")) {
            logger.info("Trying to bypass age verification");
            //Request embed format to bypass age verification
            String videoId = getIdFromUrl(fileURL);
            String embedSwfUrl = "https://www.youtube.com/v/" + videoId;
            logger.info("Requesting embed SWF: " + embedSwfUrl);
            InputStream is = client.makeRequestForFile(client.getGetMethod(embedSwfUrl));
            if (is == null) {
                throw new ServiceConnectionProblemException("Error downloading embed SWF");
            }
            String embedSwfContent = YouTubeSigDecipher.readSwfStreamToString(is);

            Matcher matcher = PlugUtils.matcher("swf.*?(https?://.+?\\.swf)", embedSwfContent);
            if (!matcher.find()) {
                throw new PluginImplementationException("SWF URL not found");
            }
            swfUrl = matcher.group(1).replace("cps.swf", "watch_as3.swf");
            method = getMethodBuilder()
                    .setReferer(embedSwfUrl)
                    .setAction("https://www.youtube.com/get_video_info")
                    .setParameter("asv", "3")
                    .setParameter("hl", "en_US")
                    .setParameter("el", "embedded")
                    .setParameter("video_id", videoId)
                    .setParameter("width", "1366")
                    .setParameter("sts", "16345")
                    .setParameter("height", "239")
                    .setAndEncodeParameter("eurl", fileURL)
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
        if (getContentAsString().contains("Sign in to view this video")
                || getContentAsString().contains("Sign in to confirm your age")) {  //just in case they change age verification mechanism
            throw new PluginImplementationException("Age verification is broken");
        }
    }
}
