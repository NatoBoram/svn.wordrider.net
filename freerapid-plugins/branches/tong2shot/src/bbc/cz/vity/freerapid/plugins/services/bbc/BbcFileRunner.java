package cz.vity.freerapid.plugins.services.bbc;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.services.rtmp.SwfVerificationHelper;
import cz.vity.freerapid.plugins.services.tor.TorProxyClient;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class BbcFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(BbcFileRunner.class.getName());
    private final static String SWF_URL = "http://emp.bbci.co.uk/emp/SMPf/1.9.36/StandardMediaPlayerChromelessFlash.swf";
    private final static String LIMELIGHT_SWF_URL = "http://www.bbc.co.uk/emp/releases/iplayer/revisions/617463_618125_4/617463_618125_4_emp.swf";
    private final static SwfVerificationHelper limelightHelper = new SwfVerificationHelper(LIMELIGHT_SWF_URL);
    private final static SwfVerificationHelper helper = new SwfVerificationHelper(SWF_URL);
    private final static String DEFAULT_EXT = ".flv";
    private final static String MEDIA_SELECTOR_HASH = "7dff7671d0c697fedb1d905d9a121719938b92bf";
    private final static String MEDIA_SELECTOR_ASN = "1";

    private SettingsConfig config;
    private boolean video = false;

    private void setConfig() throws Exception {
        final BbcServiceImpl service = (BbcServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            requestPlaylist(getPid(fileURL));
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String playlistContent) throws ErrorDuringDownloadingException {
        String name;
        try {
            name = PlugUtils.getStringBetween(playlistContent, "\"title\":\"", "\"").replace("\\/", "/").replace(": ", " - ");
        } catch (PluginImplementationException e) {
            try {
                name = PlugUtils.getStringBetween(playlistContent, "<title>", "</title>").replace(": ", " - ");
            } catch (PluginImplementationException e1) {
                throw new PluginImplementationException("Programme title not found");
            }
        }
        httpFile.setFileName(name + DEFAULT_EXT);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            //sometimes they redirect, set fileURL to the new page
            fileURL = method.getURI().toString();
            String mainPageContent = getContentAsString();
            requestPlaylist(getPid(fileURL));
            String playlistContent = getContentAsString();
            checkNameAndSize(playlistContent);
            setConfig();

            Set<String> vpids = getVpids(mainPageContent, playlistContent);
            logger.info("VPIDs: " + vpids);
            List<Stream> streamList = new LinkedList<Stream>();
            boolean mediaSelectorOk = true;
            for (String vpid : vpids) {
                mediaSelectorOk = true;
                logger.info("Requesting media selector for VPID: " + vpid);
                String atk = Hex.encodeHexString(DigestUtils.sha(MEDIA_SELECTOR_HASH + vpid));
                String mediaSelector = String.format("http://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/mediaset/pc/vpid/%s/atk/%s/asn/%s/", vpid, atk, MEDIA_SELECTOR_ASN);
                try {
                    method = getGetMethod(mediaSelector);
                    if (config.isEnableTor()) {
                        //internally, UK & proxy users don't use Tor
                        final TorProxyClient torClient = TorProxyClient.forCountry("gb", client, getPluginService().getPluginContext().getConfigurationStorageSupport());
                        if (!torClient.makeRequest(method)) {
                            checkMediaSelectorProblems();
                            throw new ServiceConnectionProblemException();
                        }
                    } else {
                        if (!makeRedirectedRequest(method)) {
                            checkMediaSelectorProblems();
                            throw new ServiceConnectionProblemException();
                        }
                    }
                    checkMediaSelectorProblems();
                    streamList.addAll(getStreams(getContentAsString()));
                } catch (Exception e) {
                    logger.warning("Error getting media selector for VPID: " + vpid);
                    mediaSelectorOk = false;
                }
            }
            if (!mediaSelectorOk) {
                checkMediaSelectorProblems();
                throw new ServiceConnectionProblemException();
            }

            Stream selectedStream = getSelectedStream(streamList);
            final RtmpSession rtmpSession = getRtmpSession(selectedStream);
            boolean isLimelight = selectedStream.supplier.equalsIgnoreCase("limelight");
            rtmpSession.getConnectParams().put("pageUrl", fileURL);
            rtmpSession.getConnectParams().put("swfUrl", isLimelight ? LIMELIGHT_SWF_URL : SWF_URL);
            if (isLimelight) {
                limelightHelper.setSwfVerification(rtmpSession, client);
            } else {
                helper.setSwfVerification(rtmpSession, client);
            }
            tryDownloadAndSaveFile(rtmpSession);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private Set<String> getVpids(String mainPageContent, String playlistContent) throws PluginImplementationException {
        /*
        http://www.bbc.co.uk/iplayer/episode/b05pl7rt/top-of-the-pops-20031980
        {"info":{"readme":"For the use of Radio, Music and Programmes only"},"statsObject":{"parentPID":"b05pl7rt","parentPIDType":"episode"},"defaultAvailableVersion":{"pid":"b05pvl0w","types":["Original"],"smpConfig":{"title":"Top of the Pops, 20\/03\/1980","summary":"With Squeeze, Shakin' Stevens, Martha and the Muffins, UB40,  the Lambrettas and the Jam.","masterBrandName":"BBC One","items":[{"vpid":"b05pvl0w","kind":"programme","duration":2100}],"holdingImageURL":"http:\/\/ichef.bbci.co.uk\/images\/ic\/$recipe\/p02mx9ml.jpg","guidance":null,"embedRights":"blocked"},"markers":[]},"allAvailableVersions":[{"pid":"b05pvl0w","types":["Original"],"smpConfig":{"title":"Top of the Pops, 20\/03\/1980","summary":"With Squeeze, Shakin' Stevens, Martha and the Muffins, UB40,  the Lambrettas and the Jam.","masterBrandName":"BBC One","items":[{"vpid":"b05pvl0w","kind":"programme","duration":2100}],"holdingImageURL":"http:\/\/ichef.bbci.co.uk\/images\/ic\/$recipe\/p02mx9ml.jpg","guidance":null,"embedRights":"blocked"},"markers":[]},{"pid":"b05pl7rr","types":["Shortened"],"smpConfig":{"title":"Top of the Pops, 20\/03\/1980","summary":"With Squeeze, Shakin' Stevens, Martha and the Muffins, UB40,  the Lambrettas and the Jam.","masterBrandName":"BBC One","items":[{"vpid":"b05pl7rr","kind":"programme","duration":1800}],"holdingImageURL":"http:\/\/ichef.bbci.co.uk\/images\/ic\/$recipe\/p02mx9ml.jpg","guidance":null,"embedRights":"blocked"},"markers":[]}],"holdingImage":"http:\/\/ichef.bbci.co.uk\/images\/ic\/976x549\/p02mx9ml.jpg"}
         */
        Set<String> vpids = new LinkedHashSet<String>();
        Matcher matcher = PlugUtils.matcher("\"vpid\":\"([^\"]+)\"", playlistContent);
        while (matcher.find()) {
            vpids.add(matcher.group(1));
        }
        if (vpids.size() <= 0) {
            matcher = PlugUtils.matcher("<item[^<>]*?identifier=\"([^<>]+?)\"", playlistContent);
            while (matcher.find()) {
                vpids.add(matcher.group(1));
            }
            if (vpids.size() <= 0) {
                matcher = PlugUtils.matcher("\"vpid\":\"([^\"]+)\"", mainPageContent);
                while (matcher.find()) {
                    vpids.add(matcher.group(1));
                }
                if (vpids.size() <= 0) {
                    throw new PluginImplementationException("Identifier not found");
                }
            }
        }
        return vpids;
    }

    private void checkPlaylistProblems() throws NotRecoverableDownloadException {
        String content = getContentAsString();
        if (content.contains("\"defaultAvailableVersion\":null")) {
            throw new URLNotAvailableAnymoreException("This programme is not available anymore");
        }
        if (content.contains("<h1>404</h1>")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private void checkMediaSelectorProblems() throws NotRecoverableDownloadException {
        Matcher matcher = getMatcherAgainstContent("<error id=\"(.+?)\"");
        if (matcher.find()) {
            final String id = matcher.group(1);
            if (id.equals("notavailable")) {
                throw new URLNotAvailableAnymoreException("Media not found");
            } else if (id.equals("notukerror") || id.equals("geolocation")) {
                throw new NotRecoverableDownloadException("This video is not available in your area");
            } else {
                throw new NotRecoverableDownloadException("Error fetching media selector: '" + id + "'");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        String content = getContentAsString();
        if (content.contains("this programme is not available")
                || content.contains("Not currently available on BBC iPlayer")) {
            throw new URLNotAvailableAnymoreException("This programme is not available anymore");
        }
        if (content.contains("Page not found") || content.contains("page was not found")) {
            throw new URLNotAvailableAnymoreException("Page not found");
        }
    }

    private String getPid(String fileUrl) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("/(?:programmes|iplayer(?:/[^/]+?)*|i(?:/[^/]+?)?)/([a-z\\d]{8})(?:/.*)?$", fileUrl);
        if (!matcher.find()) {
            throw new PluginImplementationException("PID not found");
        }
        String pid = matcher.group(1);
        logger.info("PID: " + pid);
        return pid;
    }

    private void requestPlaylist(String pid) throws Exception {
        //some programmes use xml (old) style, while the rest use json style
        GetMethod method = getGetMethod(String.format("http://www.bbc.co.uk/programmes/%s/playlist.json", pid));
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {
            method = getGetMethod("http://www.bbc.co.uk/iplayer/playlist/" + pid);
            if (!makeRedirectedRequest(method)) {
                checkPlaylistProblems();
                throw new ServiceConnectionProblemException();
            }
            checkPlaylistProblems();
        } else if (httpStatus != 200) {
            checkPlaylistProblems();
            throw new ServiceConnectionProblemException();
        }
        checkPlaylistProblems();
    }

    private RtmpSession getRtmpSession(Stream stream) {
        return new RtmpSession(stream.server, config.getRtmpPort().getPort(), stream.app, stream.play, stream.encrypted);
    }

    private List<Stream> getStreams(String content) throws Exception {
        final List<Stream> streamList = new ArrayList<Stream>();
        try {
            final NodeList mediaElements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(content.getBytes("UTF-8"))
            ).getElementsByTagName("media");
            for (int i = 0, n = mediaElements.getLength(); i < n; i++) {
                try {
                    final Element mediaElement = (Element) mediaElements.item(i);
                    if (config.isDownloadSubtitles() && mediaElement.getAttribute("kind").equals("captions")) {
                        downloadSubtitle(mediaElement);
                    } else {
                        NodeList connectionElements = mediaElement.getElementsByTagName("connection");
                        for (int j = 0, connectionElementsLength = connectionElements.getLength(); j < connectionElementsLength; j++) {
                            Element connectionElement = (Element) connectionElements.item(j);
                            final Stream stream = Stream.build(mediaElement, connectionElement);
                            if (stream != null) {
                                streamList.add(stream);
                                if (!video && mediaElement.getAttribute("kind").equals("video")) {
                                    video = true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing playlist XML", e);
        }
        return streamList;
    }

    private Stream getSelectedStream(List<Stream> streamList) throws PluginImplementationException {
        if (streamList.isEmpty()) {
            throw new PluginImplementationException("No suitable streams found");
        }
        Stream selectedStream = null;
        if (video) { //video/tv
            final int LOWER_QUALITY_PENALTY = 10;
            int weight = Integer.MAX_VALUE;
            for (Stream stream : streamList) {
                int deltaQ = stream.quality - config.getVideoQuality().getQuality();
                int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                if (tempWeight < weight) {
                    weight = tempWeight;
                    selectedStream = stream;
                }
            }
            if (selectedStream == null) {
                throw new PluginImplementationException("Unable to select stream");
            }
            int selectedQuality = selectedStream.quality;

            //select the highest bitrate for the selected quality
            int selectedBitrate = Integer.MIN_VALUE;
            for (Stream stream : streamList) {
                if ((stream.quality == selectedQuality) && (stream.bitrate > selectedBitrate)) {
                    selectedBitrate = stream.bitrate;
                    selectedStream = stream;
                }
            }

            //select CDN
            weight = Integer.MIN_VALUE;
            for (Stream stream : streamList) {
                if ((stream.quality == selectedQuality) && (stream.bitrate == selectedBitrate)) {
                    int tempWeight = 0;
                    if (stream.supplier.equalsIgnoreCase(config.getCdn().toString())) {
                        tempWeight = 100;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Level3.toString())) { //level3>limelight>akamai
                        tempWeight = 50;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Limelight.toString())) {
                        tempWeight = 49;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Akamai.toString())) {
                        tempWeight = 48;
                    }
                    if (tempWeight > weight) {
                        weight = tempWeight;
                        selectedStream = stream;
                    }
                }
            }

        } else { //audio/radio, ignore config, always pick the highest bitrate
            selectedStream = Collections.max(streamList, new Comparator<Stream>() {
                @Override
                public int compare(Stream o1, Stream o2) {
                    return Integer.valueOf(o1.bitrate).compareTo(o2.bitrate);
                }
            });
            int selectedBitrate = selectedStream.bitrate;

            //select CDN
            int weight = Integer.MIN_VALUE;
            for (Stream stream : streamList) {
                if (stream.bitrate == selectedBitrate) {
                    int tempWeight = 0;
                    if (stream.supplier.equalsIgnoreCase(config.getCdn().toString())) {
                        tempWeight = 100;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Level3.toString())) { //level3>limelight>akamai
                        tempWeight = 50;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Limelight.toString())) {
                        tempWeight = 49;
                    } else if (stream.supplier.equalsIgnoreCase(Cdn.Akamai.toString())) {
                        tempWeight = 48;
                    }
                    if (tempWeight > weight) {
                        weight = tempWeight;
                        selectedStream = stream;
                    }
                }
            }
        }

        logger.info("Stream kind : " + (video ? "TV" : "Radio"));
        logger.info("Config settings : " + config);
        logger.info("Selected stream : " + selectedStream);
        return selectedStream;
    }

    private void downloadSubtitle(Element media) throws Exception {
        String subtitleUrl = null;
        try {
            Element connection = (Element) media.getElementsByTagName("connection").item(0);
            subtitleUrl = connection.getAttribute("href");
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        if ((subtitleUrl != null) && !subtitleUrl.isEmpty()) {
            SubtitleDownloader subtitleDownloader = new SubtitleDownloader();
            subtitleDownloader.downloadSubtitle(client, httpFile, subtitleUrl);
        }
    }

    private static class Stream implements Comparable<Stream> {
        private final String server;
        private final String app;
        private final String play;
        private final boolean encrypted;
        private final int bitrate;
        private final int quality;
        private final String supplier;

        public static Stream build(final Element media, final Element connection) {
            String protocol = connection.getAttribute("protocol");
            if (protocol == null || protocol.isEmpty()) {
                protocol = connection.getAttribute("href");
            }
            if (protocol == null || protocol.isEmpty() || !protocol.startsWith("rtmp")) {
                logger.info("Not supported: " + media.getAttribute("service"));
                return null;//of what they serve, only RTMP streams are supported at the moment
            }
            final String server = connection.getAttribute("server");
            String app = connection.getAttribute("application");
            app = (app == null || app.isEmpty() ? "ondemand" : app) + "?" + PlugUtils.replaceEntities(connection.getAttribute("authString"));
            final String play = connection.getAttribute("identifier");
            final boolean encrypted = protocol.startsWith("rtmpe") || protocol.startsWith("rtmpte");
            final int bitrate = Integer.parseInt(media.getAttribute("bitrate"));
            final boolean video = media.getAttribute("kind").equals("video");
            final int quality = video ? Integer.parseInt(media.getAttribute("height")) : -1; //height as quality;
            final String supplier = connection.getAttribute("supplier");
            return new Stream(server, app, play, encrypted, bitrate, quality, supplier);
        }

        private Stream(String server, String app, String play, boolean encrypted, int bitrate, int quality, String supplier) {
            this.server = server;
            this.app = app;
            this.play = play;
            this.encrypted = encrypted;
            this.bitrate = bitrate;
            this.quality = quality;
            this.supplier = supplier;
            logger.info("Found stream : " + this);
        }

        @Override
        public String toString() {
            return "Stream{" +
                    "server='" + server + '\'' +
                    ", app='" + app + '\'' +
                    ", play='" + play + '\'' +
                    ", encrypted=" + encrypted +
                    ", bitrate=" + bitrate +
                    ", quality=" + quality +
                    ", supplier='" + supplier + '\'' +
                    '}';
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(Stream that) {
            return Integer.valueOf(this.quality).compareTo(that.quality);
        }

    }
}
