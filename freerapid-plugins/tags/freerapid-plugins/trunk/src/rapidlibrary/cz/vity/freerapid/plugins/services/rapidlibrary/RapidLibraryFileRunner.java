package cz.vity.freerapid.plugins.services.rapidlibrary;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class RapidLibraryFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(RapidLibraryFileRunner.class.getName());
    private String HTTP_SITE;
    private int captchaCounter = 1, captchaMax = 5;


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkService();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkService() throws InvalidURLOrServiceProblemException {
        if ((httpFile.getFileUrl().getHost().contains("rapidlibrary.com"))) {
            HTTP_SITE = "http://rapidlibrary.com";
        } else if ((httpFile.getFileUrl().getHost().contains("4megaupload.com"))) {
            HTTP_SITE = "http://4megaupload.com";
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
        logger.info("Service " + HTTP_SITE);
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();

        String nameBefore, nameAfter, sizeBefore, sizeAfter;
        if (HTTP_SITE.equals("http://rapidlibrary.com")) {
            nameBefore = "<td class=zae3><font color=\"#0374F1\"><b>";
            nameAfter = "</b>";
            sizeBefore = "Size:</td><td class=zae3>";
            sizeAfter = "M</td>";
        } else if (HTTP_SITE.equals("http://4megaupload.com")) {
            nameBefore = "dwn_text_fullname\">";
            nameAfter = "</td>";
            sizeBefore = "mb_text\">";
            sizeAfter = "M</span>";
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }

        PlugUtils.checkName(httpFile, content, nameBefore, nameAfter);
        final String size = PlugUtils.getStringBetween(content, sizeBefore, sizeAfter);
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size + "MB"));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        checkService();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            HttpMethod httpMethod;
            if (getContentAsString().contains("Please ENTER CODE")) {
                while (getContentAsString().contains("Please ENTER CODE")) {
                    httpMethod = stepCaptcha();
                    if (!makeRedirectedRequest(httpMethod)) {
                        throw new ServiceConnectionProblemException();
                    }
                }
            } else {
                throw new PluginImplementationException("Captcha not found");
            }
            logger.info("Captcha OK");

            checkProblems();

            String newUrl;
            if (HTTP_SITE.equals("http://rapidlibrary.com")) {
                newUrl = getMethodBuilder().setActionFromAHrefWhereATagContains("Download from rapidshare").getAction();
            } else if (HTTP_SITE.equals("http://4megaupload.com")) {
                //newUrl = getMethodBuilder().setActionFromAHrefWhereATagContains("File Download").getAction();//doesn't work because of no quotes around link, method expects them
                newUrl = (getMethodBuilder().setActionFromTextBetween("download_link_dwn><a href=", "target=\"_blank\" rel=\"nofollow\">File Download").getAction()).replace(" ", "");
            } else {
                throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
            }
            logger.info("New URL " + newUrl);
            httpFile.setNewURL(new URL(newUrl));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("file not found") || content.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private HttpMethod stepCaptcha() throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        String captchaSrc;
        if (HTTP_SITE.equals("http://rapidlibrary.com")) {
            captchaSrc = "http://rapidlibrary.com/code2.php";
        } else if (HTTP_SITE.equals("http://4megaupload.com")) {
            captchaSrc = "http://4megaupload.com/code.php";
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
        //logger.info("Captcha URL " + captchaSrc);

        String captcha;
        if (captchaCounter <= captchaMax) {
            captcha = PlugUtils.recognize(captchaSupport.getCaptchaImage(captchaSrc), "-d -1 -C A-Z");
            logger.info("OCR attempt " + captchaCounter + " of " + captchaMax + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("c_code", captcha).setParameter("act", " Download ").toPostMethod();
    }

}