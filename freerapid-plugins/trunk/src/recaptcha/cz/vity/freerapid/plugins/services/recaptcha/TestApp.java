package cz.vity.freerapid.plugins.services.recaptcha;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

/**
 * @author Vity+Team
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            final HttpDownloadClient client = new DownloadClient();
            final ConnectionSettings settings = new ConnectionSettings();
            //settings.setProxy("127.0.0.1", 3128);
            client.initClient(settings);
            client.setReferer("http://turbobit.net/download/free/5v30tjw1n1ey");
            //ReCaptchaSlimerJs reCaptcha = new ReCaptchaSlimerJs("6Lenx_USAAAAAF5L1pmTWvWcH73dipAEzNnmNLgy", client);
            //ReCaptcha reCaptcha = new ReCaptcha("6Lenx_USAAAAAF5L1pmTWvWcH73dipAEzNnmNLgy", client);
            ReCaptchaNoCaptcha reCaptcha = new ReCaptchaNoCaptcha("6LdcJxcTAAAAAATBzGX5KsNZLM4NdxJWgEtIQ6wR", "http://turbobit.net/download/free/5v30tjw1n1ey");
            //CaptchaSupport captchaSupport = new CaptchaSupport(client, new DevDialogSupport(null));
            //captchaSupport.getCaptcha(reCaptcha.getImageURL());
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