package cz.vity.freerapid.plugins.services.fastshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www.fastshare.cz/1850648/bryan-adams-live-in-lisbon.part2.rar"));
            httpFile.setNewURL(new URL("https://fastshare.live/3e05efffb6d5818a6dc5e255f0e87b037fea6e5cdbf7daaf276179b4bbd7176d7fe4f935034e9ff15772ba9384677b882e642be317d020b117ec7e1e9816b748"));
            //httpFile.setNewURL(new URL("http://fastshare.cz/4147762/lib7.avi"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final FastShareServiceImpl service = new FastShareServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}