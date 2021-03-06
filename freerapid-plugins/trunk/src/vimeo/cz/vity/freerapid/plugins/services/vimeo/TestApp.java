package cz.vity.freerapid.plugins.services.vimeo;

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
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://vimeo.com/9673593"));
            httpFile.setNewURL(new URL("http://vimeo.com/47839067")); //pass : testing
            //httpFile.setNewURL(new URL("http://vimeo.com/20042866"));
            //httpFile.setNewURL(new URL("http://vimeo.com/59023363")); //on-demand trailer
            //httpFile.setNewURL(new URL("http://vimeo.com/ondemand/6596/59023363")); //on-demand trailer
            //httpFile.setNewURL(new URL("https://player.vimeo.com/video/58611141"));
            //httpFile.setNewURL(new URL("http://vimeo.com/ondemand/6596/80025907?autoplay=1"));
            //httpFile.setNewURL(new URL("https://vimeo.com/160776966"));
            //httpFile.setNewURL(new URL("https://vimeo.com/54004198")); //Original
            //httpFile.setNewURL(new URL("https://vimeo.com/44954005"));
            httpFile.setNewURL(new URL("https://player.vimeo.com/video/141511597"));

            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final VimeoServiceImpl service = new VimeoServiceImpl();

            VimeoSettingsConfig config = new VimeoSettingsConfig();
            config.setVideoQuality(VideoQuality.Original);
            service.setConfig(config);

            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
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