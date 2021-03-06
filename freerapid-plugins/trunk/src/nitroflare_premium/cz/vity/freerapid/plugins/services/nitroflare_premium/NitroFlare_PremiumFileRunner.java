package cz.vity.freerapid.plugins.services.nitroflare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 * @author ntoskrnl
 */
class NitroFlare_PremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NitroFlare_PremiumFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getMethod);
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems(getMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "span title=\"", "\"");
        Matcher matcher = PlugUtils.matcher("File Size: </b><span[^<>]+?>([^<>]+?)</", content);
        if (!matcher.find()) {
            matcher = PlugUtils.matcher(">([^<]+?)</span>\\]<", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("File size not found");
            }
        }
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(1));
        logger.info("File size: " + filesize);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        //client.getHTTPClient().getState().clearCookies(); //uncomment this in dev environment
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems(method);
            checkNameAndSize(getContentAsString());
            fileURL = method.getURI().toString(); //http redirected to https
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Click here to download").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems(httpMethod);//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File doesn't exist") || (method.getStatusCode() == 404)) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("You have to wait")) {
            Matcher matcher = PlugUtils.matcher("You have to wait (\\d+) minutes?", contentAsString);
            if (!matcher.find()) {
                throw new PluginImplementationException("Waiting time not found");
            }
            int waitingTime = Integer.parseInt(matcher.group(1).trim());
            throw new YouHaveToWaitException("You have to wait " + waitingTime + "minute(s) to download your next file", waitingTime * 60);
        }
    }

    private static final int MAX_CAPTCHA_ATTEMPTS = 5;
    private int captchaAttempts = 0;

    private static Cookie userCookie;

    private void login() throws Exception {
        synchronized (NitroFlare_PremiumFileRunner.class) {
            if (userCookie != null) {
                addCookie(userCookie);
                logger.info("LOGGED IN Using Cookie :)");
            } else {
                final NitroFlare_PremiumServiceImpl service = (NitroFlare_PremiumServiceImpl) getPluginService();
                PremiumAccount pa = service.getConfig();
                if (!pa.isSet()) {
                    pa = service.showConfigDialog();
                    if (pa == null || !pa.isSet()) {
                        throw new BadLoginException("No NitroFlare Premium account login information!");
                    }
                }
                do {
                    if (!makeRedirectedRequest(getGetMethod("https://nitroflare.com/login"))) {
                        throw new ServiceConnectionProblemException("Error getting login page");
                    }
                    final MethodBuilder builder = getMethodBuilder()
                            .setActionFromFormWhereTagContains("login", true)
                            .setAction("https://nitroflare.com/login")
                            .setReferer("https://nitroflare.com/login")
                            .setParameter("email", pa.getUsername())
                            .setParameter("password", pa.getPassword())
                            .setParameter("login", "")
                            .setAjax();
                    if (getContentAsString().contains("recaptcha")) {
                        String key = PlugUtils.getStringBetween(getContentAsString(), "/recaptcha/api/noscript?k=", "\"");
                        final ReCaptcha reCaptcha = new ReCaptcha(key, client);
                        final String captcha = getCaptchaSupport().getCaptcha(reCaptcha.getImageURL());
                        if (captcha == null)
                            throw new CaptchaEntryInputMismatchException();
                        reCaptcha.setRecognized(captcha);
                        reCaptcha.modifyResponseMethod(builder);
                    } else if (getContentAsString().contains("captcha")) {
                        final CaptchaSupport captchaSupport = getCaptchaSupport();
                        final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
                        final String captcha;
                        if (captchaAttempts < MAX_CAPTCHA_ATTEMPTS) {
                            captchaAttempts++;
                            captcha = recognizeCaptcha(captchaSupport.getCaptchaImage(captchaSrc));
                            logger.info("Captcha attempt " + captchaAttempts + " of " + MAX_CAPTCHA_ATTEMPTS + ": " + captcha);
                        } else {
                            captcha = captchaSupport.getCaptcha(captchaSrc);
                            if (captcha == null)
                                throw new CaptchaEntryInputMismatchException();
                            logger.info("Manual captcha: " + captcha);
                        }
                        builder.setParameter("captcha", captcha);
                    }
                    if (!makeRedirectedRequest(builder.toPostMethod())) {
                        throw new ServiceConnectionProblemException("Error posting login info");
                    }
                    final Matcher matcher = getMatcherAgainstContent("Please try again in (\\d+) minutes");
                    if (matcher.find()) {
                        throw new YouHaveToWaitException(matcher.group(), Integer.parseInt(matcher.group(1)) * 60 + 5);
                    }
                } while (getContentAsString().contains("CAPTCHA error"));

                if (getContentAsString().contains("Account does not exist") ||
                        getContentAsString().contains("Forgot your password") ||
                        getContentAsString().contains("Login failed")) {
                    throw new BadLoginException("Invalid NitroFlare Premium account login information!");
                }
                userCookie = getCookieByName("user");
                logger.info("LOGGED IN :)");
            }
        }
    }

    private static String recognizeCaptcha(final BufferedImage image) {
        return PlugUtils.recognize(prepareCaptcha(image), "-d -1 -C a-z-0-9");
    }

    private static BufferedImage prepareCaptcha(final BufferedImage image) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final BufferedImage i = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int color = image.getRGB(x, y);
                final int luminosity = (((color >>> 16) & 0xff) + ((color >>> 8) & 0xff) + (color & 0xff)) / 3;
                i.setRGB(x, y, luminosity < 90 ? 0 : 0xffffff);
            }
        }
        //JOptionPane.showMessageDialog(null, new ImageIcon(i));
        return i;
    }

}