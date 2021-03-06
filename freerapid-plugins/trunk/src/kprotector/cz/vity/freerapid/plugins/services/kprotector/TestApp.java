package cz.vity.freerapid.plugins.services.kprotector;

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
            httpFile.setNewURL(new URL("http://www.kprotector.com/p0/5757bbd288ada"));     //recaptcha
            //httpFile.setNewURL(new URL("http://www.kprotector.com/p100/576052e6c7967"));   //basic captcha
            //httpFile.setNewURL(new URL("http://www.kprotector.com/p100/5760526cb2e07"));   //fancy captcha ##
            //httpFile.setNewURL(new URL("http://www.kprotector.com/p100/57605333ce95e"));   //simple captcha
            //httpFile.setNewURL(new URL("http://www.kprotector.com/p100/5760535e6085f"));   //cool captcha
            //httpFile.setNewURL(new URL("http://www.kprotector.com/p100/5760538b562ad"));   //pass=FreeRapid
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final KProtectorServiceImpl service = new KProtectorServiceImpl(); //instance of service - of our plugin
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