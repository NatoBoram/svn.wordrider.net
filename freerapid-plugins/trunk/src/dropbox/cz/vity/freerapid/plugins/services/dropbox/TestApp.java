package cz.vity.freerapid.plugins.services.dropbox;

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
            //httpFile.setNewURL(new URL("https://www.dropbox.com/s/gn3sr42r1gtuiah/The%20Ironwood%20Tree%20%28The%20Spiderwick%20Chronicles%20%234%29%20by%20Holly%20Black%2C%20Tony%20DiTerlizzi.epub"));
            //httpFile.setNewURL(new URL("https://www.dropbox.com/s/0oth4k2q7eh6xda/%5BMINDS%5DThe.Heirs.E01.LIMO.srt"));
            httpFile.setNewURL(new URL("https://www.dropbox.com/s/f8i51fq8ikfcs5v/3D%20covers.zip"));
            httpFile.setNewURL(new URL("https://www.dropbox.com/sh/eninofzoma41rt9/AAD_M88SJNLpQXzZfcTIl-6-a/ROM.bin?dl=0"));
            httpFile.setNewURL(new URL("https://www.dropbox.com/sh/w9wockk5d045zsl/AAANY2dfZoeenhjSAHPuTzSHa/DSC_6266.NEF?dl=0"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final DropBoxServiceImpl service = new DropBoxServiceImpl(); //instance of service - of our plugin
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