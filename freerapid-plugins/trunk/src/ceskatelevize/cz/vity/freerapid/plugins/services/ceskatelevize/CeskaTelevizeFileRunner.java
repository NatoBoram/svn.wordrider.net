package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.applehls.AdjustableBitrateHlsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author JPEXS
 * @author tong2shot
 */
class CeskaTelevizeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CeskaTelevizeFileRunner.class.getName());
    private final static String FNAME = "fname";
    private final static String SWITCH_ITEM_ID = "switchitemid";
    private final static String DEFAULT_EXT = ".ts";
    private String switchItemIdFromUrl = null;
    private CeskaTelevizeSettingsConfig config;

    private void setConfig() throws Exception {
        CeskaTelevizeServiceImpl service = (CeskaTelevizeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private boolean isBonus(String fileUrl) {
        return fileUrl.contains("/bonus/") || fileUrl.contains("/bonusy/");
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        if (isArchiveEpisode(fileURL) || isArchiveProgramme(fileURL) || isMultiparts(fileURL)) {
            return;
        }
        final GetMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkName();
    }

    private void checkName() throws Exception {
        String filename;
        Matcher matcher;
        String content;

        matcher = getMatcherAgainstContent("(?i)charset\\s*=\\s*windows-1250");
        if (matcher.find()) {
            setPageEncoding("Windows-1250"); //usually they use "UTF-8" charset, but sometimes they use "windows-1250" charset
            GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            setPageEncoding("UTF-8");
        }
        content = getContentAsString();

        if (content.contains("<h1 id=\"nazev\">")) {
            matcher = getMatcherAgainstContent("<h1 id=\"nazev\">(?:<a[^<>]+>)?(.+?)(?:</a>)?</h1>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (1)");
            }
            filename = matcher.group(1).replace("<span id=\"dil\">", "").replace("</span>", "").trim();
            if (content.contains("<h2 id=\"nazevcasti\">")) {
                matcher = getMatcherAgainstContent("<h2 id=\"nazevcasti\">(?:<a[^<>]+>)?(.+?)(?:</a>)?</h2>");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting episode name (1)");
                }
                filename += " - " + matcher.group(1).trim();
            }
        } else if (content.contains("id=\"programmeInfoView\"")) {
            matcher = getMatcherAgainstContent("(?s)\"programmeInfoView\".+?<h2>(?:<a[^<>]+>)?(.+?)(?:</a>)?</h2>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (2)");
            }
            filename = matcher.group(1).trim();
            if (content.contains("\"episode-title\"")) {
                matcher = getMatcherAgainstContent("\"episode-title\">(.+?)</");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error getting episode name (2)");
                }
                filename += " - " + matcher.group(1).trim();
            }
        } else if (content.contains("id=\"global\"")) {
            matcher = getMatcherAgainstContent("(?s)id=\"global\".+?<h1>(?:<a[^<>]+>)?(.+?)(?:</a>)?</h1>");
            if (!matcher.find()) {
                filename = getGenericTitle(content);
            } else {
                filename = matcher.group(1).trim();
                if (content.contains("id=\"titleBox\"")) {
                    matcher = getMatcherAgainstContent("<h2>(?:<a[^<>]+>)?(.+?)(?:</a>)?</h2>");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Error getting episode name (3)");
                    }
                    filename += " - " + matcher.group(1).trim();
                }
            }
        } else {
            filename = getGenericTitle(content);
        }

        filename = filename.replaceAll("[\\t\\n]", "").trim();
        filename += DEFAULT_EXT;
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getGenericTitle(String content) throws PluginImplementationException {
        if (content.contains("<title>")) {
            Matcher matcher = getMatcherAgainstContent("<title>(.+?)</title>");
            if (!matcher.find()) {
                throw new PluginImplementationException("Error getting programme title (4)");
            }
            return PlugUtils.unescapeHtml(matcher.group(1).trim())
                    .replaceAll("Video .", "")
                    .replaceAll(". Česká televize", "")
                    .replaceAll(". iVysílání", "");
        } else {
            throw new PluginImplementationException("Error getting programme title (5)");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (isArchiveEpisode(fileURL)) {
            processArchiveEpisode();
        } else if (isMultiparts(fileURL)) {
            processMultiparts();
        } else {
            final GetMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (isArchiveProgramme(fileURL)) {
                parseArchiveProgramme(getContentAsString());
                return;
            }
            checkName();
        }

        //Find a way to get playlist URL.
        //If the content doesn't give us a straight way to get playlist URL,
        //then we have to find a way to get video player URL.
        HttpMethod httpMethod;
        String referer = fileURL;
        /*
        if (!getContentAsString().contains("getPlaylistUrl(")) {
            //check if it has specific iframe that contains video player URL
            Matcher iframeMatcher = Pattern.compile("(<i?frame(.*?)>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(getContentAsString());
            Matcher srcMatcher = Pattern.compile("src\\s?=\\s?[\"'](.+?)[\"']", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(getContentAsString());
            String action = null;
            final String lower = "iFramePlayer".toLowerCase();
            String iFrameUrl = null;
            while (iframeMatcher.find()) {
                srcMatcher.region(iframeMatcher.start(1), iframeMatcher.end(1));
                final String content = iframeMatcher.group(1);
                if (content.toLowerCase().contains(lower) && srcMatcher.find()) {
                    iFrameUrl = PlugUtils.replaceEntities(srcMatcher.group(1));
                    //check bonus, non bonus rule
                    if ((!isBonus(fileURL) && !iFrameUrl.contains("bonus=")) || (isBonus(fileURL) && iFrameUrl.contains("bonus="))) {
                        action = iFrameUrl;
                        break;
                    }
                }
            }

            //ignore bonus, nonbonus rule
            if (action == null && iFrameUrl != null) {
                action = iFrameUrl;
            }

            //if it doesn't have the iframe, then find another way to get video player url.
            if (action == null) {
                //check some signature,
                //whether it uses hidden command burried in some javascript
                if (getContentAsString().contains("CT_VideoPlayer.config.ajaxUrl")) {
                    Matcher matcher = getMatcherAgainstContent("CT_VideoPlayer.config.ajaxUrl\\s*?=\\s*?['\"]([^'\"]+)['\"]");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Get video player URL path not found");
                    }
                    String getVideoPlayer = matcher.group(1);
                    matcher = getMatcherAgainstContent("q\\s*?=\\s*?'([^']+)'");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Get video player query param not found");
                    }
                    String q = matcher.group(1);

                    HttpMethod method = getMethodBuilder()
                            .setAction(getVideoPlayer)
                            .setParameter("cmd", "getVideoPlayerUrl")
                            .setParameter("q", q)
                            .setParameter("autoStart", "true")
                            .toPostMethod();
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();

                    matcher = getMatcherAgainstContent("\"videoPlayerUrl\"\\s*?:\\s*?\"([^\"]+)\"");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Video player URL not found");
                    }
                    action = matcher.group(1).replace("\\/", "/");

                    //it doesn't, check other signature then
                } else {
                    String urlContent;
                    try {
                        urlContent = PlugUtils.getStringBetween(getContentAsString(), "og:url\" content=\"", "\"");
                        if (!urlContent.startsWith("http")) { // plugin api bug - doesn't resolve relative protocol urls properly
                            urlContent = "https:" + urlContent;
                        }
                    } catch (PluginImplementationException e) {
                        throw new PluginImplementationException("Error getting playlist URL(1)");
                    }
                    if (!makeRedirectedRequest(getMethodBuilder().setAction(urlContent).toGetMethod())) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();

                    try {
                        action = URLDecoder.decode(PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "data-url=\"", "\"")), "UTF-8");
                        if (!action.startsWith("http")) { // plugin api bug - doesn't resolve relative protocol urls properly
                            action = "https:" + action;
                        }
                    } catch (PluginImplementationException e) {
                        throw new PluginImplementationException("Error getting playlist URL(2)");
                    }
                }
            }

            //yes!! we have the video player URL (==action)!!
            httpMethod = getMethodBuilder().setReferer(referer).setAction(action).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            referer = httpMethod.getURI().toString();

            //Wait a minute.., oh well... still can't find a way to get playlist URL.
            //Okay then, let's check some signature
            if (!getContentAsString().contains("getPlaylistUrl(")) {
                httpMethod = getMethodBuilder().setReferer(referer).setActionFromAHrefWhereATagContains("t video").toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                referer = httpMethod.getURI().toString();
            }

            //Something is missing here...?
        }

        //So here we are...
        //So we have a way to get playlist URL.
        //Let's get some required params
        URL requestUrl = new URL(referer);
        */
        String videoId;
        try {
            videoId = PlugUtils.getStringBetween(getContentAsString(), "\"idec\":\"", "\"");
        } catch (PluginImplementationException e) {
            try {
                videoId = PlugUtils.getStringBetween(getContentAsString(), "&amp;IDEC=", "&amp;");
                videoId = videoId.replaceAll("[^\\d]+", "");
            } catch (PluginImplementationException e2) {
                throw new PluginImplementationException("Video ID not found");
            }
        }
        logger.info("Video ID: " + videoId);

        //Request getting playlist with the required params
        httpMethod = getMethodBuilder()
                .setReferer(referer)
                .setAjax()
                .setAction("https://www.ceskatelevize.cz/ivysilani/ajax/get-client-playlist/")
                .setParameter("playlist[0][type]", "episode")
                .setParameter("playlist[0][id]", videoId)
                .setParameter("playlist[0][startTime]", "")
                .setParameter("playlist[0][stopTime]", "")
                .setParameter("requestUrl", "/ivysilani/embed/iFramePlayer.php")
                .setParameter("requestSource", "iVysilani")
                .setParameter("requestUrl", "html")
                .setParameter("canPlayDRM", "true")
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .setHeader("x-addr", "127.0.0.1")
                .toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error loading playlist URL");
        }
        checkProblems();

        Matcher matcher = getMatcherAgainstContent("\"url\":\"(http.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Playlist URL not found");
        }
        //Yahooo!! We finally have playlist URL..
        String playlistUrl = URLDecoder.decode(matcher.group(1).replace("\\/", "/"), "UTF-8").replace("hashedId", "id");
        httpMethod = new GetMethod(playlistUrl);
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error connecting to playlist");
        }
        checkProblems();
        setConfig();

        JsonNode rootNode;
        try {
            rootNode = new ObjectMapper().readTree(getContentAsString());
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing playlist", e);
        }

        List<SwitchItem> switchItems = getSwitchItems(rootNode);
        //Check whether the playlist contains one part or multipart videos
        if (switchItems.size() == 1) {
            if (isBonus(fileURL)) {
                String title = rootNode.findPath("playlist").findPath("title").getTextValue();
                if (title != null) {
                    httpFile.setFileName(title + DEFAULT_EXT);
                }
            }
            SwitchItem selectedSwitchItem = switchItems.get(0);
            logger.info("Selected switch item : " + selectedSwitchItem);
            logger.info("Settings config: " + config);
            final AdjustableBitrateHlsDownloader downloader = new AdjustableBitrateHlsDownloader(client, httpFile, downloadTask, config.getVideoQuality().getBitrate());
            downloader.tryDownloadAndSaveFile(selectedSwitchItem.getUrl());
        } else {
            queueMultiparts(switchItems);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Stránka nenalezena")
                || contentAsString.contains("Video není k dispozici")
                || contentAsString.contains("Video není k&nbsp;dispozici")
                || contentAsString.contains("Stránka nebyla nalezena")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("content is not available at")
                || contentAsString.contains("\"url\":\"error_region\"")) {
            throw new PluginImplementationException("This content is not available at your territory due to limited copyright");
        }
        if (contentAsString.contains("došlo k chybě při práci s databází")) {
            throw new ServiceConnectionProblemException("An error occurred while working with databases");
        }
    }

    private List<SwitchItem> getSwitchItems(JsonNode rootNode) throws PluginImplementationException {
        JsonNode playlistNode = rootNode.findPath("playlist");
        List<SwitchItem> switchItems = new ArrayList<SwitchItem>();
        for (JsonNode playlistItem : playlistNode) {
            String swItemId = playlistItem.findPath("id").getTextValue();
            double duration = playlistItem.findPath("duration").getValueAsDouble();
            String url = playlistItem.findPath("main").getTextValue();
            if ((swItemId == null) || (duration == 0.0) || (url == null)) {
                throw new PluginImplementationException("Error parsing playlist (2)");
            }
            if ((switchItemIdFromUrl != null) && (!swItemId.equals(switchItemIdFromUrl))) {
                continue;
            }
            SwitchItem switchItem = new SwitchItem(swItemId, duration, url);
            switchItems.add(switchItem);
            logger.info("Found switch item: " + switchItem);
        }

        if (switchItems.size() > 1) {
            for (int i = 0; i < switchItems.size(); ) {
                SwitchItem switchItem = switchItems.get(i);
                if (switchItem.getDuration() <= 60) {  //assumption: advertisement<=60s, content>60s
                    switchItems.remove(i); //remove advertisement
                } else {
                    i++;
                }
            }
        }
        if (switchItems.isEmpty()) {
            throw new PluginImplementationException("No streams found");
        }
        return switchItems;
    }

    private void queueMultiparts(List<SwitchItem> switchItems) throws Exception {
        List<URI> list = new LinkedList<URI>();
        for (int i = 0; i < switchItems.size(); i++) {
            SwitchItem switchItem = switchItems.get(i);
            String switchIdParam = SWITCH_ITEM_ID + "=" + URLEncoder.encode(switchItem.getId(), "UTF-8");
            String fnameParam = "&" + FNAME + "=" + URLEncoder.encode(httpFile.getFileName().replaceFirst(Pattern.quote(DEFAULT_EXT) + "$", "-" + (i + 1)), "UTF-8");
            try {
                list.add(new URI(fileURL + (fileURL.endsWith("/") ? "?" : "/?") + switchIdParam + fnameParam));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No switch items available");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(list.size() + " switch items added");
    }

    private boolean isArchiveEpisode(String fileUrl) {
        return fileUrl.matches("https?://decko\\.ceskatelevize\\.cz/player\\?.+");
    }

    private void processArchiveEpisode() throws Exception {
        URL url = new URL(fileURL);
        String filename = null;
        try {
            filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(FNAME);
        } catch (Exception e) {
            //
        }
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8") + DEFAULT_EXT, "_"));
        fileURL = fileURL.replaceFirst("&" + FNAME + "=.+", "");

        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private boolean isArchiveProgramme(String fileUrl) {
        return fileUrl.matches("https?://decko\\.ceskatelevize\\.cz/.+");
    }

    private void parseArchiveProgramme(String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("var IDEC\\s*?=\\s*?'([^']+?)'", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Episode ID not found");
        }
        String idec = matcher.group(1).trim();
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAjax()
                .setAction("https://decko.ceskatelevize.cz/rest/Programme/relatedVideosForEpisode")
                .setAndEncodeParameter("IDEC", idec)
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        List<URI> list = new LinkedList<URI>();
        matcher = getMatcherAgainstContent("\"title\":\"([^\"]+?)\".+?\"programmeTitle\":\"([^\"]+?)\".+?\"IDEC\":\"([^\"]+?)\"");
        while (matcher.find()) {
            String episodeTitle = matcher.group(1).trim();
            String programmeTitle = matcher.group(2).trim();
            idec = matcher.group(3).trim();
            String fname = programmeTitle + " - " + episodeTitle;
            String episodeUrl = String.format("https://decko.ceskatelevize.cz/player?width=560&IDEC=%s&%s=%s", URLEncoder.encode(idec, "UTF-8"), FNAME, URLEncoder.encode(fname, "UTF-8"));
            try {
                list.add(new URI(episodeUrl));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No episodes available");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(list.size() + " episodes added");
    }

    private boolean isMultiparts(String fileUrl) {
        return fileUrl.contains("?" + SWITCH_ITEM_ID + "=");
    }

    private void processMultiparts() throws Exception {
        URL url = new URL(fileURL);
        String filename = null;
        try {
            filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(FNAME);
        } catch (Exception e) {
            //
        }
        if (filename == null) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8") + DEFAULT_EXT, "_"));

        switchItemIdFromUrl = null;
        try {
            switchItemIdFromUrl = URLUtil.getQueryParams(fileURL, "UTF-8").get(SWITCH_ITEM_ID);
        } catch (Exception e) {
            //
        }
        if (switchItemIdFromUrl == null) {
            throw new PluginImplementationException("Switch item ID param not found");
        }

        fileURL = fileURL.replaceFirst("\\?" + SWITCH_ITEM_ID + "=.+", "");
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }
}
