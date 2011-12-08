package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda, JPEXS, ntoskrnl
 * @since 0.82
 */
class YouTubeRunner extends AbstractRtmpRunner {
    private static final Logger logger = Logger.getLogger(YouTubeRunner.class.getName());

    private YouTubeSettingsConfig config;
    private int fmt = 0;
    private String fileExtension = ".flv";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
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

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            fileURL = method.getURI().toString();

            checkProblems();
            setConfig();
            checkName();

            if (isUserPage()) {
                parseUserPage();
                return;
            }

            if (getContentAsString().contains("&amp;fmt_stream_map=")) {
                RtmpSession rtmpSession = handleStreamMap();
                tryDownloadAndSaveFile(rtmpSession);

            } else {
                checkFmtParameter();
                checkName();

                String fmt_url_map = PlugUtils.getStringBetween(getContentAsString(), "\"url_encoded_fmt_stream_map\": \"", "\"");
                Matcher matcher = PlugUtils.matcher("url=([^,]+?)\\\\u0026[^,]*itag=" + fmt + ",", fmt_url_map + ",");

                if (matcher.find()) {
                    setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
                    method = getGetMethod(URLDecoder.decode(matcher.group(1), "UTF-8"));
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                } else {
                    throw new PluginImplementationException("Cannot find specified video format (" + fmt + ")");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws Exception {
        if (getContentAsString().contains("I confirm that I am 18 years of age or older")) {
            if (!makeRedirectedRequest(getGetMethod(fileURL + "&has_verified=1"))) {
                throw new ServiceConnectionProblemException();
            }
        }
        /* Causes false positives
        final Matcher matcher = getMatcherAgainstContent("<div\\s+?class=\"yt-alert-content\">\\s*([^<>]+?)\\s*</div>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(matcher.group(1));
        }
        */
    }

    private void checkName() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "<meta name=\"title\" content=\"", "\"");
        String fileName = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(httpFile.getFileName()));
        if (!isUserPage()) {
            fileName += fileExtension;
        }
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isUserPage() {
        return fileURL.contains("/user/");
    }

    private void setConfig() throws Exception {
        YouTubeServiceImpl service = (YouTubeServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private void checkFmtParameter() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("fmt=(\\d+)", fileURL.toLowerCase(Locale.ENGLISH));

        if (matcher.find()) {
            final String fmtCode = matcher.group(1);

            if (fmtCode.length() <= 2) {
                fmt = Integer.parseInt(fmtCode);
                setFileExtension(fmt);
            }
        } else {
            processConfig();
        }
    }

    private void processConfig() throws ErrorDuringDownloadingException {
        String fmt_map = PlugUtils.getStringBetween(getContentAsString(), "\"fmt_list\": \"", "\"").replace("\\/", "/");
        //Example: 37/1920x1080/9/0/115,22/1280x720/9/0/115,35/854x480/9/0/115,34/640x360/9/0/115,5/320x240/7/0/0
        String[] formats = fmt_map.split(",");
        String[][] formatParts = new String[formats.length][];
        for (int f = 0; f < formats.length; f++) {
            //Example: 37/1920x1080/9/0/115
            formatParts[f] = formats[f].split("/");
        }
        int qualityWidth = config.getQualityWidth();
        int qualityIndex = -1;
        if (qualityWidth == YouTubeSettingsConfig.MAX_WIDTH) {
            logger.info("Selecting maximum quality");
            qualityIndex = 0;
        } else if (qualityWidth == YouTubeSettingsConfig.MIN_WIDTH) {
            logger.info("Selecting minimum quality");
            qualityIndex = formatParts.length - 1;
        } else {
            int nearestGreater = Integer.MAX_VALUE;
            int nearestGreaterIndex = -1;
            int nearestLower = Integer.MIN_VALUE;
            int nearestLowerIndex = -1;
            for (int f = 0; f < formatParts.length; f++) {
                String[] wh = formatParts[f][1].split("x");
                int h = Integer.parseInt(wh[1]);
                if (h == qualityWidth) {
                    qualityIndex = f;
                    break;
                } else {
                    if ((h > qualityWidth) && (nearestGreater > h)) {
                        nearestGreater = h;
                        nearestGreaterIndex = f;
                    }
                    if ((h < qualityWidth) && (nearestLower < h)) {
                        nearestLower = h;
                        nearestLowerIndex = f;
                    }
                }
            }
            if (qualityIndex == -1) {
                if (nearestLowerIndex != -1) {
                    qualityIndex = nearestLowerIndex;
                    logger.info("Selected quality not found, using nearest lower");
                } else {
                    qualityIndex = nearestGreaterIndex;
                    logger.info("Selected quality not found, using nearest better");
                }
            }
        }

        if (qualityIndex == -1) throw new PluginImplementationException("Cannot select quality");
        logger.info("Quality to download: fmt" + formatParts[qualityIndex][0] + " " + formatParts[qualityIndex][1]);
        fmt = Integer.parseInt(formatParts[qualityIndex][0]);
        setFileExtension(fmt);
    }

    private void setFileExtension(int fmtCode) {
        switch (fmtCode) {
            case 13:
            case 17:
                fileExtension = ".3gp";
                break;
            case 18:
            case 22:
            case 37:
            case 38:
                fileExtension = ".mp4";
                break;
            case 43:
                fileExtension = ".webm";
                break;
        }
    }

    private RtmpSession handleStreamMap() throws Exception {
        String fmt_stream_map = PlugUtils.getStringBetween(getContentAsString(), "&amp;fmt_stream_map=", "&");
        try {
            fmt_stream_map = URLDecoder.decode(fmt_stream_map, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtils.processException(logger, e);
        }
        //Example: 34|c4/id/31ff2b12315eb686/itag/34|rtmpe://...,18|mp4:c1/id/31ff2b12315eb686/itag/18|rtmpe://...,5|c1/id/31ff2b12315eb686/itag/5|rtmpe://...
        String[] formats = fmt_stream_map.split(",");
        String[][] formatParts = new String[formats.length][];
        for (int f = 0; f < formats.length; f++) {
            //Example: 34|c4/id/31ff2b12315eb686/itag/34|rtmpe://...
            formatParts[f] = formats[f].split("\\|");
        }
        //TODO quality selection?
        final RtmpSession rtmpSession = new RtmpSession(formatParts[0][2], formatParts[0][1]);
        final String swfUrl = getSwfUrl();
        rtmpSession.getConnectParams().put("swfUrl", swfUrl);
        rtmpSession.getConnectParams().put("pageUrl", fileURL);
        new SwfVerificationHelper(swfUrl).setSwfVerification(rtmpSession, client);
        return rtmpSession;
    }

    private String getSwfUrl() throws ErrorDuringDownloadingException {
        return PlugUtils.getStringBetween(getContentAsString(), "<param name=\\\"movie\\\" value=\\\"", "\\\"").replace("\\/", "/");
    }

    private void parseUserPage() throws Exception {
        final String user = getUserFromUrl();
        final List<URI> uriList = new LinkedList<URI>();
        final int MAX_RESULTS = 50;
        for (int index = 1; ; index += MAX_RESULTS) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(null)
                    .setAction("http://gdata.youtube.com/feeds/api/users/" + user + "/uploads")
                    .setParameter("start-index", String.valueOf(index))
                    .setParameter("max-results", String.valueOf(MAX_RESULTS))
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final int previousSize = uriList.size();
            final Matcher matcher = getMatcherAgainstContent("<media:player url='(.+?)'/>");
            while (matcher.find()) {
                try {
                    final String link = PlugUtils.replaceEntities(matcher.group(1)).replace("&feature=youtube_gdata_player", "");
                    uriList.add(new URI(link));
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            if (uriList.size() - previousSize < MAX_RESULTS) {
                break;
            }
        }
        // YouTube returns the videos in descending date order, which is a bit illogical.
        // If the user wants them that way, don't reverse.
        if (!config.isReversePlaylistOrder()) {
            Collections.reverse(uriList);
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        logger.info(uriList.size() + " videos added");
        if (!uriList.isEmpty()) {
            httpFile.getProperties().put("removeCompleted", true);
        }
    }

    private String getUserFromUrl() throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher(".+/([^\\?&#]+)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing file URL");
        }
        return matcher.group(1);
    }

}