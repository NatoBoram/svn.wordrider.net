package cz.vity.freerapid.plugins.services.mega;

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
            httpFile.setNewURL(new URL("https://mega.co.nz/#!oswTzRzC!Yk7R2QwDTJrAe7T0c4d3OigsRZk2V7ip-57KcD0Mx7U"));
            httpFile.setNewURL(new URL("https://mega.co.nz/#%21ap4E1RDI%21-UA3HeIG_2odhIoJH1ZGbCscB3DQAGXB1n8281ZQIFg"));
            //httpFile.setNewURL(new URL("https://mega.co.nz/#N!dIhiDbiJ!e1xjNjyua5nlOJiEbxBBM3sDkr12kWVhi04aFGTl7f0"));
            //httpFile.setNewURL(new URL("https://mega.co.nz/#F!scRgGChR!PzcvIBCITggtpI8mA37x3g"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8118); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MegaServiceImpl service = new MegaServiceImpl(); //instance of service - of our plugin
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