package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.utilities.LogUtils;
import org.jdesktop.application.Application;

import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    private final static Logger logger = Logger.getLogger(TestApp.class.getName());

    @Override
    protected void startup() {
//        final HttpFile httpFile = getHttpFile();
//        try {
//            httpFile.setNewURL(new URL("http://rapidshare.com/files/169450403/samantha.who.s02e08.hdtv.xvid-xor.avi"));
//            testRun(new RapidShareServiceImpl(), httpFile, new ConnectionSettings());
//        } catch (Exception e) {
//            LogUtils.processException(logger, e);
//        }

        try {
            testOptions();
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        this.exit();

    }

    private void testOptions() throws Exception {
        final RapidShareServiceImpl service = new RapidShareServiceImpl();
        service.setPluginContext(super.getPluginContext());
        service.showOptions();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
