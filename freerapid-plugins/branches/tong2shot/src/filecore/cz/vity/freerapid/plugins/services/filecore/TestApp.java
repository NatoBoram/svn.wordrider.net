package cz.vity.freerapid.plugins.services.filecore;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author birchie
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://filecore.co.nz/h1u7lq5v8mwu"));
            //httpFile.setNewURL(new URL("http://filecore.co.nz/xkfrrpenoi1u/danger.5.s01e00.the.diamond.girls.webrip.x264-w4f.mp4"));
            //httpFile.setNewURL(new URL("http://fcore.eu/7lhwc5tvwc18"));
            httpFile.setNewURL(new URL("http://filecore.co/507goc2kdr4n/GT.S04E01SD.x264-DM.mkv.html"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final FileCoreServiceImpl service = new FileCoreServiceImpl(); //instance of service - of our plugin
            /*
            //we set premium account details
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("****");
            config.setPassword("****");
            service.setConfig(config);
            */
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