package cz.vity.freerapid.plugins.services.bbc;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.Proxy;
import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //log everything
            //InputStream is = new BufferedInputStream(new FileInputStream("C:\\Users\\Administrator\\Desktop\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b02148kx/The_Bottom_Line_Recruitment/"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b043xdmw/eastenders-12052014"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b04470xx/the-next-step-10-road-to-joy"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b042twvq/the-first-georgians-the-german-kings-who-made-britain-episode-2"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/b042z1g4"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b049nv1q/T_in_the_Park_2014_Arctic_Monkeys_and_Example/"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/b043pmd0"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/cbbc/episode/b03y70vb/"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/p027f9q0"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/b007wvmw"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/cbbc/episode/b03wc2yj/"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/b0540ndg"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/p00fpv1w")); //non RTMP for non UK ip address
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b05pl7rt/top-of-the-pops-20031980")); //contains 2 vpids, only 1 works
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b05rhtyw/masterchef-series-11-episode-18")); //get all streams from all possible vpids
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b08pnn6c/pointless-series-17-episode-11"));
            httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b08ny6yt/world-championship-snooker-extra-2017-day-11"));
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/programmes/b08p51wz")); //radio

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            connectionSettings.setProxy("localhost", 9040, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final BbcServiceImpl service = new BbcServiceImpl(); //instance of service - of our plugin
            SettingsConfig config = new SettingsConfig();
            config.setStreamType(StreamType.HDS);
            config.setVideoQuality(VideoQuality._396_923);
            config.setDownloadSubtitles(true);
            config.setRtmpPort(RtmpPort._1935);
            config.setEnableTor(false);
            config.setCdn(Cdn.Limelight);
            service.setConfig(config);
            //setUseTempFiles(true);
            //runcheck makes the validation

            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}