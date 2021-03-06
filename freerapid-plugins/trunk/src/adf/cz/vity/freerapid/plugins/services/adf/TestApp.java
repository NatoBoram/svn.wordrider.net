package cz.vity.freerapid.plugins.services.adf;

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
            //httpFile.setNewURL(new URL("http://adf.ly/5XR"));
            //httpFile.setNewURL(new URL("http://adf.ly/DA73i"));
//            httpFile.setNewURL(new URL("http://adf.ly/1409722/http://www.wowebook.be/download/4314/"));
            //httpFile.setNewURL(new URL("http://adf.ly/PPqGu"));  //http://www.putlocker.com/file/A2A1F1C572C8C542
            httpFile.setNewURL(new URL("http://adf.ly/Rp4za"));
            //httpFile.setNewURL(new URL("http://adf.acb.im/iO"));
            //httpFile.setNewURL(new URL("http://goo.gl/efMsK"));
            httpFile.setNewURL(new URL("http://adf.ly/redirecting/aHR0cDovL3NoLnN0LzFXaGhC"));
            httpFile.setNewURL(new URL("https://goo.gl/ueSVUs"));         //direct redirect
            httpFile.setNewURL(new URL("http://pintient.com/VG"));
            httpFile.setNewURL(new URL("http://queuecosm.bid/-26034LSWX/VG?rndad=2077433976-1517687968"));
            httpFile.setNewURL(new URL("http://activeation.com/4K1z"));
            httpFile.setNewURL(new URL("http://uclaut.net/-28382UNVR/A6D8?rndad=2044594076-1550633139"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final AdfServiceImpl service = new AdfServiceImpl(); //instance of service - of our plugin
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
