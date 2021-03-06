package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author JPEXS
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://play.iprima.cz/all/55946/all"));//stream.cz
            //httpFile.setNewURL(new URL("http://play.iprima.cz/vinari/vinari-ii-16")); //non-geo
            //httpFile.setNewURL(new URL("http://play.iprima.cz/hruza-v-oblacich-4")); //geo blocked.
            //httpFile.setNewURL(new URL("http://fresh.iprima.cz/jak-na-to/video-kysane-zeli-jako-elixir-zdravi-nalozte-si-vlastni"));
            //httpFile.setNewURL(new URL("http://autosalon.iprima.cz/videa/mercedes-amg-45-4matic-vs-mercedes-amg-gle-63-4matic-s-coupe"));
            //httpFile.setNewURL(new URL("http://play.iprima.cz/particka/particka-117"));
            //httpFile.setNewURL(new URL("http://play.iprima.cz/helix/helix-ii-13"));
            httpFile.setNewURL(new URL("http://play.iprima.cz/podraz")); //account is required
            //httpFile.setNewURL(new URL("http://prima.iprima.cz/prostreno/prostreno-xv-96"));
            //httpFile.setNewURL(new URL("http://play.iprima.cz/take-andele-jedi-fazole"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 9150, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final iPrimaServiceImpl service = new iPrimaServiceImpl(); //instance of service - of our plugin
            /*
            iPrimaSettingsConfig config = new iPrimaSettingsConfig();
            config.setVideoQuality(VideoQuality._400_1600);
            config.setUsername("*****");
            config.setPassword("*****");
            service.setConfig(config);
            */
            //Instead of declaring config explicitly,
            //copy 'iPrimaSettings.xml' to local storage directory
            service.setPluginContext(getPluginContext());
            service.getConfig(); //load config from local storage

            //runcheck makes the validation
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