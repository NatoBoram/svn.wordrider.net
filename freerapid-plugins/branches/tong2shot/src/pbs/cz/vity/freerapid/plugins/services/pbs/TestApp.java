package cz.vity.freerapid.plugins.services.pbs;

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
            //log everything
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://video.pbs.org/video/2163057527/")); //rtmp
            //httpFile.setNewURL(new URL("http://video.pbs.org/video/2365607036/")); //hls
            httpFile.setNewURL(new URL("http://www.pbs.org/video/2365629405/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 9040, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final PbsServiceImpl service = new PbsServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            SettingsConfig config = new SettingsConfig();
            config.setDownloadSubtitles(false);
            service.setConfig(config);
            //setUseTempFiles(true);
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