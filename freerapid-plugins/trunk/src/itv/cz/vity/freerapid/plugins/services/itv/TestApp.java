package cz.vity.freerapid.plugins.services.itv;

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
            //InputStream is = new BufferedInputStream(new FileInputStream("C:\\Users\\Administrator\\Desktop\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.itv.com/hub/a-touch-of-frost/Ya1774a0037"));
            httpFile.setNewURL(new URL("http://www.itv.com/hub/the-jonathan-ross-show/2a1166a0125"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 9040, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final ItvServiceImpl service = new ItvServiceImpl(); //instance of service - of our plugin
            SettingsConfig config = new SettingsConfig();
            config.setDownloadSubtitles(false);
            config.setVideoQuality(VideoQuality._1500);
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