package cz.vity.freerapid.plugins.services.tusfiles;

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
            //  httpFile.setNewURL(new URL("https://www.tusfiles.net/hq9ggbkbosc2/easysup.rar"));
            httpFile.setNewURL(new URL("https://tusfiles.net/eqcddwfv01gn"));
            //httpFile.setNewURL(new URL("http://www.tusfiles.net/33fiymm0c74l/Prime.minister.and.i.e01.131209.hdtv.h264.limo_minidrama.net.mkv"));
            //httpFile.setNewURL(new URL("https://www.tusfiles.net/go/gohk5ekgtexa/"));  // folder
            httpFile.setNewURL(new URL("https://tusfiles.com/q8mjma4h56ei"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final TusFilesServiceImpl service = new TusFilesServiceImpl(); //instance of service - of our plugin
            /*
            //we set premium account details
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("****");
            config.setPassword("****");
            service.setConfig(config);
            //*/
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