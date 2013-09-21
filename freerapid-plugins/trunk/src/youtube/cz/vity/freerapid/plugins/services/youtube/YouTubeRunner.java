package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.youtube.srt.Transcription2SrtUtil;
import cz.vity.freerapid.plugins.video2audio.AbstractVideo2AudioRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @author JPEXS
 * @author ntoskrnl
 * @author tong2shot
 * @since 0.82
 */
class YouTubeRunner extends AbstractVideo2AudioRunner {
    private static final Logger logger = Logger.getLogger(YouTubeRunner.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:23.0) Gecko/20100101 Firefox/24.0";

    private YouTubeSettingsConfig config;
    private YouTubeMedia youTubeMedia = null;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        setClientParameter(DownloadClientConsts.USER_AGENT, USER_AGENT);
        addCookie(new Cookie(".youtube.com", "PREF", "hl=en", "/", 86400, false));
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

            youTubeMedia = getSelectedYouTubeMedia();
            checkName();

            final String fmtStreamMap = PlugUtils.getStringBetween(getContentAsString(), "\"url_encoded_fmt_stream_map\": \"", "\"");
            logger.info("fmtStreamMap : " + fmtStreamMap);
            int itagCode = youTubeMedia.getItagCode();
            Matcher matcher = PlugUtils.matcher("([^,]*\\\\u0026itag=" + itagCode + "[^,]*|[^,]*itag=" + itagCode + "\\\\u0026[^,]*)", fmtStreamMap);
            if (!matcher.find()) {
                throw new PluginImplementationException("Cannot find specified video format (" + itagCode + ")");
            }
            final String swfUrl = PlugUtils.getStringBetween(getContentAsString(), "\"url\": \"", "\"").replace("\\/", "/");
            final String formatContent = matcher.group(1);
            logger.info("Swf URL : " + swfUrl);
            if (formatContent.contains("rtmp")) {
                matcher = PlugUtils.matcher("conn=(.+?)(?:\\\\u0026.+)?$", formatContent);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find stream address");
                }
                final String conn = URLDecoder.decode(matcher.group(1), "UTF-8");
                matcher = PlugUtils.matcher("stream=(.+?)(?:\\\\u0026.+)?$", formatContent);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find stream params");
                }
                final String sparams = URLDecoder.decode(matcher.group(1), "UTF-8");
                final RtmpSession rtmpSession = new RtmpSession(conn, sparams);
                rtmpSession.getConnectParams().put("swfUrl", swfUrl);
                rtmpSession.getConnectParams().put("pageUrl", fileURL);
                new SwfVerificationHelper(swfUrl).setSwfVerification(rtmpSession, client);
                if (!tryDownloadAndSaveFile(rtmpSession)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                matcher = PlugUtils.matcher("url=(.+?)(?:\\\\u0026.+)?$", formatContent);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Cannot find stream URL");
                }
                String videoURL = matcher.group(1);
                if (!videoURL.contains("signature")) {
                    if (formatContent.contains("sig=")) {
                        matcher = PlugUtils.matcher("sig=(.+?)(?:\\\\u0026.+)?$", formatContent);
                        if (matcher.find()) {
                            videoURL = videoURL + "&signature=" + matcher.group(1);
                        }
                    } else {
                        matcher = PlugUtils.matcher("(?:\\\\u0026)?s=([A-Z0-9\\.]+?)(?:\\\\u0026|$)", formatContent);
                        if (matcher.find()) {
                            logger.info("Cipher signature : " + matcher.group(1));
                            InputStream is = client.makeRequestForFile(getGetMethod(swfUrl));
                            if (is == null) {
                                throw new ServiceConnectionProblemException("Error downloading SWF");
                            }
                            final String signature = new YouTubeSigDecipher(is).decipher(matcher.group(1));
                            logger.info("Deciphered signature : " + signature);
                            videoURL += "&signature=" + signature;
                        }
                    }
                }

                method = getGetMethod(URLDecoder.decode(videoURL, "UTF-8"));
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
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    //@TODO : implement https://developers.google.com/youtube/2.0/developers_guide_protocol_video_entries
    //@TODO : https://gdata.youtube.com/feeds/api/videos/$videoid?v=2    , where $videoid = video id from url
    private void checkProblems() throws Exception {
        if (getContentAsString().contains("video you have requested is not available")
                || getContentAsString().contains("video is no longer available")
                || getContentAsString().contains("This channel is not available")
                || getContentAsString().contains("video has been removed")
                || getContentAsString().contains("page you requested cannot be found")
                || getContentAsString().contains("blocked it in your country on copyright grounds")
                || getContentAsString().contains("has not made this video available")
                || getContentAsString().contains("account associated with this video has been terminated")) {
            //|| getContentAsString().contains("This video is unavailable")) { //false positive
            throw new URLNotAvailableAnymoreException("File not found");
        }
        /* Causes false positives
        final Matcher matcher = getMatcherAgainstContent("<div\\s+?class=\"yt-alert-content\">\\s*([^<>]+?)\\s*</div>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
        */
    }

    private void checkName() throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        } catch (final PluginImplementationException e) {
            PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- YouTube\n</title>");
        }
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (!isUserPage() && !isPlaylist() && !isCourseList() && !isSubtitles()) {
            fileName += (youTubeMedia == null ? ".flv" : youTubeMedia.getContainer().getFileExt());
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setConfig() throws Exception {
        final YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private YouTubeMedia getSelectedYouTubeMedia() throws ErrorDuringDownloadingException {
        final String fmt_map = PlugUtils.getStringBetween(getContentAsString(), "\"fmt_list\": \"", "\"").replace("\\/", "/");
        logger.info("fmt_map : " + fmt_map);
        //Example: 37/1920x1080/9/0/115,22/1280x720/9/0/115,35/854x480/9/0/115,34/640x360/9/0/115,5/320x240/7/0/0
        final String[] formats = fmt_map.split(",");
        final Map<Integer, YouTubeMedia> ytMediaMap = new LinkedHashMap<Integer, YouTubeMedia>(); // k=itagcode, v=YTMedia, preserve ordering
        logger.info("Available YouTube media : ");
        for (String format : formats) {
            //Example: 37/1920x1080/9/0/115
            String formatParts[] = format.split("/");
            int itagCode = Integer.parseInt(formatParts[0]);
            int videoResolution = Integer.parseInt(formatParts[1].split("x")[1]);
            YouTubeMedia ytMedia = new YouTubeMedia(itagCode, videoResolution);
            ytMediaMap.put(itagCode, ytMedia);
            logger.info(ytMedia.toString());
        }
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
                selectedItagCode = ytMediaMap.keySet().iterator().next(); //first key
            } else if (configVideoQuality == VideoQuality.Lowest) {
                for (Integer itagCode : ytMediaMap.keySet()) {
                    selectedItagCode = itagCode; //last key
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
            if (configContainer != Container.Any) {
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

        YouTubeMedia youTubeMedia = ytMediaMap.get(selectedItagCode);
        logger.info("Config setting : " + config);
        logger.info("Media to be downloaded : " + youTubeMedia);
        return youTubeMedia;
    }

    private boolean isVid2AudSupported(YouTubeMedia ytMedia) {
        String container = ytMedia.getContainer().getName().toUpperCase();
        String audioEncoding = ytMedia.getAudioEncoding().toUpperCase();
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
        final Matcher matcher = PlugUtils.matcher(".+?/playlist\\?list=(?:PL|UU|FL)?([^\\?&#/]+)", fileURL);
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
        final Matcher matcher = PlugUtils.matcher(".+?/course\\?list=(?:EC)?([^\\?&#/]+)", fileURL);
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
        } else if (config.isDownloadSubtitles()) {
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