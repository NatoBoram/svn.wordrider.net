package cz.vity.freerapid.plugins.services.bbc;

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
            httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b00tp89x/BBC_Proms_2010_Prom_76_Last_Night_of_the_Proms_Part_1/"));//radio
            //httpFile.setNewURL(new URL("http://www.bbc.co.uk/iplayer/episode/b00ttj2r/Corrie_The_Road_to_Coronation_Street/"));//tv
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 9050, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final BbcServiceImpl service = new BbcServiceImpl(); //instance of service - of our plugin
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