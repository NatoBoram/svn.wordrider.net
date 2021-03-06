package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
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
import org.apache.commons.httpclient.HttpStatus;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
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
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0";
    private static final String DEFAULT_FILE_EXT = ".flv";
    private static final String DASH_AUDIO_ITAG = "dashaudioitag";

    private YouTubeSettingsConfig config;
    private int dashAudioItag = -1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        if (isVideo()) {
            checkFileProblems();
        }
        if (isDashAudio()) {
            normalizeDashAudioUrl();
        }
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            bypassAgeVerification(method);
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
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
        setConfig();

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
            bypassAgeVerification(method);
            checkProblems();
            fileURL = method.getURI().toString();
            checkName();

            if (isUserPage()) {
                parseUserPage(getUserFromUrl());
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

            String swfUrl = PlugUtils.getStringBetween(getContentAsString(), "\"url\": \"", "\"").replace("\\/", "/");
            String fmtStreamMapContent = PlugUtils.getStringBetween(getContentAsString(), "\"url_encoded_fmt_stream_map\": \"", "\"");
            String dashStreamMapContent = null;
            if (getContentAsString().contains("\"dashmpd\": \"")) {
                String dashUrl;
                dashUrl = PlugUtils.getStringBetween(getContentAsString(), "\"dashmpd\": \"", "\"").replace("\\/", "/");
                method = getMethodBuilder().setReferer(fileURL).setAction(dashUrl).toGetMethod();
                setTextContentTypes("video/vnd.mpeg.dash.mpd");
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                dashStreamMapContent = getContentAsString();
            }

            logger.info("Swf URL : " + swfUrl);
            YouTubeMedia youTubeMedia = null;
            Map<Integer, YouTubeMedia> dashStreamMap = getDashStreamMap(dashStreamMapContent);
            if (dashAudioItag == -1) { //not dash audio
                Map<Integer, YouTubeMedia> fmtStreamMap = getFmtStreamMap(fmtStreamMapContent);
                Map<Integer, YouTubeMedia> ytStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
                ytStreamMap.putAll(fmtStreamMap); //put fmtStreamMap at the top of the map
                if (config.isEnableDash()) {
                    ytStreamMap.putAll(dashStreamMap);
                }
                youTubeMedia = getSelectedYouTubeMedia(ytStreamMap);
                if (youTubeMedia.getContainer() == Container.dash_v) {
                    queueDashAudio(dashStreamMap);
                }
            } else { //dash audio
                youTubeMedia = dashStreamMap.get(dashAudioItag);
                if (youTubeMedia == null) {
                    throw new PluginImplementationException("DASH audio stream with itag='" + dashAudioItag + "' not found");
                }
            }

            httpFile.setFileName(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_FILE_EXT) + "$", youTubeMedia.getContainer().getFileExt()));

            String videoURL = youTubeMedia.getUrl();
            if (URLUtil.getQueryParams(videoURL, "UTF-8").get("signature") == null) { //if there is no "signature" param in url
                String signature;
                if (youTubeMedia.isCipherSignature()) { //signature is encrypted
                    logger.info("Cipher signature : " + youTubeMedia.getSignature());
                    InputStream is = client.makeRequestForFile(getGetMethod(swfUrl));
                    if (is == null) {
                        throw new ServiceConnectionProblemException("Error downloading SWF");
                    }
                    signature = new YouTubeSigDecipher(is).decipher(youTubeMedia.getSignature());
                    logger.info("Deciphered signature : " + signature);
                } else {
                    signature = youTubeMedia.getSignature();
                }
                videoURL += "&signature=" + signature;
            }

            logger.info("Config setting : " + config);
            logger.info("Downloading media : " + youTubeMedia);
            method = getGetMethod(videoURL);
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (config.isConvertToAudio()) {
                httpFile.setFileName(httpFile.getFileName().replaceFirst("\\..{3,4}$", ".mp3"));
                final int bitrate = youTubeMedia.getAudioBitrate();
                final boolean mp4 = ".mp4".equalsIgnoreCase(youTubeMedia.getContainer().getFileExt());
                if (!tryDownloadAndSaveFile(method, bitrate, mp4)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws Exception {
        logger.info("Checking file problems");
        HttpMethod method = getGetMethod(String.format("https://gdata.youtube.com/feeds/api/videos/%s?v=2", getIdFromUrl()));
        int httpCode = client.makeRequest(method, true);
        if ((httpCode == HttpStatus.SC_NOT_FOUND)
                || (httpCode == HttpStatus.SC_FORBIDDEN)
                || getContentAsString().contains("ResourceNotFoundException")
                || getContentAsString().contains("ServiceForbiddenException")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkProblems() throws Exception {
        //
    }

    private void checkName() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        } catch (final PluginImplementationException e) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- YouTube\n</title>");
        }
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (isVideo()) {
            fileName += DEFAULT_FILE_EXT;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        final YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private Map<Integer, YouTubeMedia> getFmtStreamMap(String content) throws Exception {
        Map<Integer, YouTubeMedia> fmtStreamMap = new LinkedHashMap<Integer, YouTubeMedia>();
        String fmtStreams[] = content.split(",");
        for (String fmtStream : fmtStreams) {
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
            logger.info("Found video : " + youTubeMedia);
            fmtStreamMap.put(itag, youTubeMedia);
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
                        int itag = -1;
                        String url = null;
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

                        if (itag == -1 || url == null || signature == null) {
                            throw new PluginImplementationException("Invalid YouTube DASH media : " + representationElement.getTextContent());
                        }

                        YouTubeMedia youTubeMedia = new YouTubeMedia(itag, url, signature, cipherSig);
                        logger.info("Found DASH video : " + youTubeMedia);
                        dashStreamMap.put(itag, youTubeMedia);
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                    }
                }
            } catch (Exception e) {
                throw new PluginImplementationException("Error parsing playlist XML", e);
            }
        }
        return dashStreamMap;
    }

    private YouTubeMedia getSelectedYouTubeMedia(Map<Integer, YouTubeMedia> ytMediaMap) throws ErrorDuringDownloadingException {
        if (ytMediaMap.isEmpty()) {
            throw new PluginImplementationException("No available YouTube media");
        }
        int selectedItagCode = -1;

        if (config.isConvertToAudio()) { //convert to audio
            final int NOT_SUPPORTED_PENALTY = 10000;
            final int LOWER_QUALITY_PENALTY = 5;
            int configAudioBitrate = config.getAudioQuality().getBitrate();

            //select audio bitrate
            int selectedAudioBitrate = -1;
            int weight = Integer.MAX_VALUE;
            for (Map.Entry<Integer, YouTubeMedia> ytMediaEntry : ytMediaMap.entrySet()) {
                YouTubeMedia ytMedia = ytMediaEntry.getValue();
                if ((ytMedia.getContainer() == Container.dash_v) || (ytMedia.getContainer() == Container.dash_a)) { //skip DASH
                    continue;
                }
                int audioBitrate = ytMedia.getAudioBitrate();
                int deltaAudioBitrate = audioBitrate - configAudioBitrate;
                int tempWeight = (deltaAudioBitrate < 0 ? Math.abs(deltaAudioBitrate) + LOWER_QUALITY_PENALTY : deltaAudioBitrate);
                if (!isVid2AudSupported(ytMedia)) {
                    tempWeight += NOT_SUPPORTED_PENALTY;
                }
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedAudioBitrate = audioBitrate;
                }
            }

            //calc (the lowest) video quality + penalty to get the fittest itagcode  -> select video quality
            weight = Integer.MAX_VALUE;
            for (Map.Entry<Integer, YouTubeMedia> ytMediaEntry : ytMediaMap.entrySet()) {
                YouTubeMedia ytMedia = ytMediaEntry.getValue();
                if ((ytMedia.getContainer() == Container.dash_v) || (ytMedia.getContainer() == Container.dash_a)) { //skip DASH
                    continue;
                }
                if (ytMedia.getAudioBitrate() == selectedAudioBitrate) {
                    int tempWeight = ytMedia.getVideoQuality();
                    if (!isVid2AudSupported(ytMedia)) {
                        tempWeight += NOT_SUPPORTED_PENALTY;
                    }
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedItagCode = ytMedia.getItagCode();
                    }
                }
            }
            if (!isVid2AudSupported(ytMediaMap.get(selectedItagCode))) {
                throw new PluginImplementationException("Only supports MP3 or AAC audio encoding");
            }

        } else { //not converted to audio
            //select video quality
            VideoQuality configVideoQuality = config.getVideoQuality();
            if (configVideoQuality == VideoQuality.Highest) {
                List<YouTubeMedia> sortedYtMediaMap = new LinkedList<YouTubeMedia>(ytMediaMap.values());
                Collections.sort(sortedYtMediaMap, new Comparator<YouTubeMedia>() {
                    @Override
                    public int compare(YouTubeMedia o1, YouTubeMedia o2) {
                        return Integer.compare(o2.getVideoQuality(), o1.getVideoQuality()); //reverse order
                    }
                });
                selectedItagCode = sortedYtMediaMap.iterator().next().getItagCode();
            } else if (configVideoQuality == VideoQuality.Lowest) {
                for (Integer itagCode : ytMediaMap.keySet()) {
                    if ((YouTubeMedia.getContainer(itagCode) == Container.dash_v) || (YouTubeMedia.getContainer(itagCode) == Container.dash_a)) { //skip DASH
                        continue;
                    }
                    selectedItagCode = itagCode; //last key of fmtStreamMap
                }
            } else {
                final int LOWER_QUALITY_PENALTY = 10;
                int weight = Integer.MAX_VALUE;
                for (Map.Entry<Integer, YouTubeMedia> ytMediaEntry : ytMediaMap.entrySet()) {
                    YouTubeMedia ytMedia = ytMediaEntry.getValue();
                    int deltaQ = ytMedia.getVideoQuality() - configVideoQuality.getQuality();
                    int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedItagCode = ytMedia.getItagCode();
                    }
                }
            }

            //select container
            final Container configContainer = config.getContainer();
            if ((configContainer != Container.Any) && (ytMediaMap.get(selectedItagCode).getContainer() != Container.dash_v)) {
                final int selectedVideoQuality = ytMediaMap.get(selectedItagCode).getVideoQuality();
                int weight = Integer.MIN_VALUE;
                for (Map.Entry<Integer, YouTubeMedia> ytMediaEntry : ytMediaMap.entrySet()) {
                    YouTubeMedia ytMedia = ytMediaEntry.getValue();
                    if (ytMedia.getVideoQuality() == selectedVideoQuality) {
                        int tempWeight = 0;
                        Container container = ytMedia.getContainer();
                        if (config.getContainer() == container) {
                            tempWeight = 100;
                        } else if (container == Container.mp4) { //mp4 > flv > webm > 3gp
                            tempWeight = 50;
                        } else if (container == Container.flv) {
                            tempWeight = 49;
                        } else if (container == Container.webm) {
                            tempWeight = 48;
                        } else if (container == Container._3gp) {
                            tempWeight = 47;
                        }
                        if (tempWeight > weight) {
                            weight = tempWeight;
                            selectedItagCode = ytMedia.getItagCode();
                        }
                    }
                }
            }
        }

        return ytMediaMap.get(selectedItagCode);
    }

    private void queueDashAudio(Map<Integer, YouTubeMedia> dashStreamMap) throws Exception {
        int selectedItag = -1;
        int weight = Integer.MIN_VALUE;
        for (YouTubeMedia ytMedia : dashStreamMap.values()) {
            if (ytMedia.getContainer() != Container.dash_a) { //skip non DASH audio
                continue;
            }
            int tempWeight = Integer.MIN_VALUE;
            switch (ytMedia.getAudioBitrate()) { //audio bitrate as criteria
                case 128: //128 > 256 > 48
                    tempWeight = 50;
                    break;
                case 256:
                    tempWeight = 49;
                    break;
                case 48:
                    tempWeight = 48;
                    break;
            }
            if (tempWeight > weight) {
                weight = tempWeight;
                selectedItag = ytMedia.getItagCode();
            }
        }
        if (selectedItag == -1) {
            throw new PluginImplementationException("Preferred DASH audio stream not found");
        }
        List<URI> uriList = new LinkedList<URI>();
        String url = fileURL + "&" + DASH_AUDIO_ITAG + "=" + selectedItag;
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
        dashAudioItag = Integer.parseInt(matcher.group(1));
        fileURL = fileURL.replaceFirst("&" + DASH_AUDIO_ITAG + "=.+", ""); //remove dash audio tag param;
    }

    private boolean isVideo() {
        return !isUserPage() && !isPlaylist() && !isCourseList() && !isSubtitles();
    }

    private boolean isVid2AudSupported(YouTubeMedia ytMedia) {
        String container = ytMedia.getContainer().getName();
        String audioEncoding = ytMedia.getAudioEncoding();
        return ((container.equals("MP4") || container.equals("FLV")) && (audioEncoding.equals("MP3") || audioEncoding.equals("AAC")));
    }

    private String getIdFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("(?:[\\?&]v=|\\.be/)([^\\?&#]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting video id");
        }
        return matcher.group(1);
    }

    private LinkedList<URI> getURIList(String action, final String URIRegex) throws Exception {
        final LinkedList<URI> uriList = new LinkedList<URI>();
        setFileStreamContentTypes(new String[0], new String[]{"application/atom+xml"});
        do {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(null)
                    .setAction(action)
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            Matcher matcher = getMatcherAgainstContent(URIRegex);
            while (matcher.find()) {
                try {
                    final String link = PlugUtils.replaceEntities(matcher.group(1));
                    final URI uri = new URI(link);
                    if (!uriList.contains(uri)) {
                        uriList.add(uri);
                    }
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            matcher = getMatcherAgainstContent("<link rel='next'.*? href='(.+?)'");
            if (!matcher.find()) {
                break;
            }
            action = PlugUtils.replaceEntities(matcher.group(1));
        } while (getContentAsString().contains("<link rel='next'"));
        return uriList;
    }

    private LinkedList<URI> getVideoURIList(final String action) throws Exception {
        return getURIList(action, "<media:player url='(.+?)(?:&.+?)?'");
    }

    private LinkedList<URI> getLectureCourseMaterialURIList(final String action) throws Exception {
        return getURIList(action, "<yt:material.*? url='(.+?)'");
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
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#User_Uploaded_Videos
    private void parseUserPage(final String user) throws Exception {
        final String action = "http://gdata.youtube.com/feeds/api/users/" + user + "/uploads";
        final List<URI> uriList = getVideoURIList(action);
        // YouTube returns the videos in descending date order, which is a bit illogical.
        // If the user wants them that way, don't reverse.
        if (!config.isReversePlaylistOrder()) {
            Collections.reverse(uriList);
        }
        queueLinks(uriList);
    }

    private boolean isPlaylist() {
        return fileURL.contains("/playlist?");
    }

    private String getPlaylistIdFromUrl() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("list=(?:PL|UU|FL)?([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting playlist id");
        }
        return matcher.group(1);
    }

    private String getUserFromContent() throws PluginImplementationException {
        return PlugUtils.getStringBetween(getContentAsString(), "<a class=\"profile-thumb\" href=\"/user/", "\"");
    }

    //Favorite List and Playlist
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Favorite_Videos
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Retrieving_a_playlist
    private void parsePlaylist() throws Exception {
        if (fileURL.contains("list=UU")) { //user uploaded video
            final String user = getUserFromContent();
            parseUserPage(user);
        } else if (fileURL.contains("list=FL")) { //favorite list
            final String user = getUserFromContent();
            final String action = String.format("http://gdata.youtube.com/feeds/api/users/%s/favorites", user);
            final List<URI> uriList = getVideoURIList(action);
            queueLinks(uriList);
        } else { //playlist
            final String playlistId = getPlaylistIdFromUrl();
            final String action = String.format("http://gdata.youtube.com/feeds/api/playlists/%s?v=2", playlistId);
            final List<URI> uriList = getVideoURIList(action);
            queueLinks(uriList);
        }
    }

    private boolean isCourseList() {
        return fileURL.contains("/course?list=");
    }

    private String getCourseIdFromUrl() throws PluginImplementationException {
        final Matcher matcher = PlugUtils.matcher("list=(?:EC)?([^\\?&#/]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting course id");
        }
        return matcher.group(1);
    }

    //Course list contains video playlist, lecture materials, and course materials
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Courses
    //reference : https://developers.google.com/youtube/2.0/developers_guide_protocol#Lectures
    private void parseCourseList() throws Exception {
        //Step #1 queue video playlist related to the course
        final String courseId = getCourseIdFromUrl();
        String action = String.format("http://gdata.youtube.com/feeds/api/playlists/%s?v=2", courseId);
        List<URI> uriList = getVideoURIList(action);
        if (uriList.isEmpty()) {
            logger.info("No video links found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " videos added");
        }

        //Step #2 queue lecture materials
        action = String.format("https://stage.gdata.youtube.com/feeds/api/edu/lectures?course=%s", courseId);
        uriList = getLectureCourseMaterialURIList(action);
        if (uriList.isEmpty()) {
            logger.info("No lecture material links found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " lecture materials added");
        }

        //Step #3 queue course materials
        action = String.format("http://gdata.youtube.com/feeds/api/edu/courses/%s?v=2", courseId);
        uriList = getLectureCourseMaterialURIList(action);
        if (uriList.isEmpty()) {
            logger.info("No course material links found");
        } else {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            logger.info(uriList.size() + " course materials added");
        }
        httpFile.getProperties().put("removeCompleted", true);
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
        } else if (config.isDownloadSubtitles() && isVideo()) {
            final String id = getIdFromUrl();
            final HttpMethod method = getGetMethod("http://www.youtube.com/api/timedtext?type=list&v=" + id);
            if (makeRedirectedRequest(method)) {
                final List<URI> list = new LinkedList<URI>();
                matcher = getMatcherAgainstContent("<track id=\"\\d*\" name=\"(.*?)\" lang_code=\"(.*?)\"");
                while (matcher.find()) {
                    final String name = matcher.group(1);
                    final String lang = matcher.group(2);
                    final String url = fileURL + "#subtitles:" + lang + ":&v=" + id + "&name=" + name + "&lang=" + lang;
                    try {
                        list.add(new URI(url));
                    } catch (final URISyntaxException e) {
                        LogUtils.processException(logger, e);
                    }
                }
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            }
        }
        return false;
    }

    private void bypassAgeVerification(HttpMethod method) throws Exception {
        if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_age.*")
                || getContentAsString().contains("watch7-player-age-gate-content")
                || getContentAsString().contains("Sign in to confirm your age")
                || getContentAsString().contains("<script>window.location = \"https:\\/\\/www.youtube.com\\/verify_age")
                || getContentAsString().contains("<script>window.location = \"http:\\/\\/www.youtube.com\\/verify_age")) {
            setClientParameter(DownloadClientConsts.USER_AGENT, "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
            method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }

            //controversy
            if (getContentAsString().contains("<script>window.location = \"http:\\/\\/www.youtube.com\\/verify_controversy")
                    || getContentAsString().contains("<script>window.location = \"https:\\/\\/www.youtube.com\\/verify_controversy")) {
                method = getMethodBuilder()
                        .setAction(PlugUtils.getStringBetween(getContentAsString(), "window.location = \"", "\"").replace("\\/", "/"))
                        .toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            }
            if (method.getURI().toString().matches("https?://(www\\.)?youtube\\.com/verify_controversy.*")
                    || getContentAsString().contains("verify_controversy?action_confirm=1")) {
                method = getMethodBuilder()
                        .setBaseURL("https://www.youtube.com")
                        .setActionFromFormWhereActionContains("verify_controversy", true)
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException();
                }
            }
            setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
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