package cz.vity.freerapid.plugins.services.appletrailers;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://trailers.apple.com/trailers/paramount/ironman/"));
            httpFile.setNewURL(new URL("http://trailers.apple.com/movies/paramount/ironman2/ironman2-clip2_h1080p.mov"));

            //httpFile.setNewURL(new URL("http://trailers.apple.com/trailers/wb/pacificrim/#videos-large"));
            //httpFile.setNewURL(new URL("http://trailers.apple.com/movies/wb/pacificrim/pacificrim-fte2_720p.mov"));
            //httpFile.setNewURL(new URL("http://movietrailers.apple.com/movies/wb/pacificrim/pacificrim-fte15m_1080p.mov?title=13+min.+Making+Of+Featurette"));
            //httpFile.setNewURL(new URL("http://movietrailers.apple.com/movies/wb/pacificrim/pacificrim-fte15m_a720p.m4v?title=13+min.+Making+Of+Featurette"));

            //httpFile.setNewURL(new URL("http://trailers.apple.com/trailers/sony_pictures/grownups2/"));
            //httpFile.setNewURL(new URL("http://trailers.apple.com/movies/sony_pictures/grownups2/grownups2-sneakpeak_720p.mov"));

            //httpFile.setNewURL(new URL("http://trailers.apple.com/trailers/wb/prisoners/"));
            //httpFile.setNewURL(new URL("http://trailers.apple.com/trailers/paramount/worldwarz/"));
            //httpFile.setNewURL(new URL("http://trailers.apple.com/trailers/dreamworksstudios/bridgeofspies/"));
            //httpFile.setNewURL(new URL("http://movietrailers.apple.com/movies/paramount/arrival/arrival-teaser-1_h480p.mov"));

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final AppleTrailersServiceImpl service = new AppleTrailersServiceImpl(); //instance of service - of our plugin
            SettingsConfig config = new SettingsConfig();
            config.setVideoFormat(VideoFormat.M4V);
            service.setConfig(config);
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