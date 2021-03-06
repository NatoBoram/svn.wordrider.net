package cz.vity.freerapid.plugins.services.multiup;

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
            httpFile.setNewURL(new URL("http://www.multiup.org/download/74b503fbd602a5b1e096c05332326f50/TS.102.x264.tuserie.com.zip"));
            httpFile.setNewURL(new URL("http://multiup.org/download/c66915b7effd54672db74114f6187e0b/Phaeton.rar"));
            //httpFile.setNewURL(new URL("https://www.multiup.org/download/cb0a1a9b0084b0f09b46df6d442d3eb5"));             // no-text recaptcha
            //httpFile.setNewURL(new URL("http://www.multiup.org/download/acf44b0e4b2588b05cbc07380d8b29d2/SAw.720p.YourSerie.CoM.mp4"));  //pass=069bc
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MultiUpServiceImpl service = new MultiUpServiceImpl(); //instance of service - of our plugin
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