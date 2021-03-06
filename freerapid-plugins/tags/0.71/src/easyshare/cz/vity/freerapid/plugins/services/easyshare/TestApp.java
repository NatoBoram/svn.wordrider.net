package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.dev.PluginApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginApplication {
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setFileUrl(new URL("http://w17.easy-share.com/1701853057.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.setProxy("localhost", 8081);
            run(new EasyShareServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
