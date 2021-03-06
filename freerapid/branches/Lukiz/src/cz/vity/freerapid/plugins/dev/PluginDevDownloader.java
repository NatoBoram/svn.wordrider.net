package cz.vity.freerapid.plugins.dev;

import cz.vity.freerapid.plugins.exceptions.FailedToLoadCaptchaPictureException;
import cz.vity.freerapid.plugins.webclient.*;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Vity
 */
class PluginDevDownloader implements HttpFileDownloader {
    private final static Logger logger = Logger.getLogger(PluginDevDownloader.class.getName());

    private HttpFile file;
    private DownloadClient downloadClient;


    PluginDevDownloader(HttpFile file, ConnectionSettings settings) {
        this.file = file;
        downloadClient = new DownloadClient();
        downloadClient.initClient(settings);
    }

    public HttpFile getDownloadFile() {
        return file;
    }

    public HttpDownloadClient getClient() {
        return downloadClient;
    }

    public void saveToFile(InputStream inputStream) throws Exception {
        logger.info("Simulating saving file from stream");
        sleep(1);
        logger.info("File succesfully saved");
    }

    public void sleep(int seconds) throws InterruptedException {
        file.setState(DownloadState.WAITING);

        logger.info("Going to sleep on " + (seconds) + " seconds");
        for (int i = seconds; i > 0; i--) {
            if (isTerminated())
                break;
            Thread.sleep(1000);
        }

    }

    public BufferedImage loadCaptcha(InputStream inputStream) throws FailedToLoadCaptchaPictureException {
        return null;
    }

    public String askForCaptcha(BufferedImage image) throws Exception {
        return null;
    }

    public String getCaptcha(String url) throws FailedToLoadCaptchaPictureException {
                            InputStreamReader inp = new InputStreamReader(System.in);
                       BufferedReader br = new BufferedReader(inp);
                        System.out.println("Enter text of :" + url);
        String cp = null;
        try {
            cp = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return cp;
    }

    public boolean isTerminated() {
        return false;
    }
}

