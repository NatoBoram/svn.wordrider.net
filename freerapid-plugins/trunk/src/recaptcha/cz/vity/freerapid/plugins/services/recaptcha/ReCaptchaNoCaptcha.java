package cz.vity.freerapid.plugins.services.recaptcha;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author tong2shot
 */
public class ReCaptchaNoCaptcha {
    private final static Logger logger = Logger.getLogger(ReCaptchaNoCaptcha.class.getName());
    private final static String CAPTCHA_TOKEN_SIGN = "SLIMERJSRECAPTCHANOCAPTCHAFRD";

    private final String response;
    private final String publicKey;
    private final String referer;

    /**
     * Constructor of ReCaptchaNoCaptcha
     *
     * @param publicKey Public API key for ReCaptcha (Included in every page which uses ReCaptcha)
     * @param referer   Referer
     * @throws Exception When something goes wrong
     */
    public ReCaptchaNoCaptcha(String publicKey, String referer) throws Exception {
        if (!publicKey.matches("[0-9a-zA-Z_-]{40}")) {
            throw new PluginImplementationException("Invalid recaptcha public key");
        }
        if (referer == null || referer.isEmpty()) {
            throw new PluginImplementationException("Referer cannot be null/empty");
        }
        this.publicKey = publicKey;
        this.referer = referer;
        this.response = getResponseWithSlimerJs();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String getResponseWithSlimerJs() throws IOException, ErrorDuringDownloadingException {

        final String command;
        if (Utils.isWindows()) {
            command = Utils.addFileSeparator(Utils.getAppPath()) + ReCaptchaSlimerJs.PATH_WINDOWS;
        } else {
            command = Utils.addFileSeparator(Utils.getAppPath()) + ReCaptchaSlimerJs.PATH_LINUX;
        }

        if (!new File(command).exists()) {
            throw new ErrorDuringDownloadingException("SlimerJS not found. Please refer to http://wordrider.net/forum/10/17077/ on how to setup SlimerJS");
        }

        String jsContent =
                "var page = require(\"webpage\").create();\n" +
                        "page.onAlert = function(text) {\n" +
                        "    console.log('SLIMERJSRECAPTCHANOCAPTCHAFRD'+text+'SLIMERJSRECAPTCHANOCAPTCHAFRD');\n" +
                        "    page.close();\n" +
                        "    slimer.exit();\n" +
                        "}\n" +
                        "\n" +
                        "var htmlContent = \"<script src='https://www.google.com/recaptcha/api.js?hl=en'></script>\"+\n" +
                        "\"<form action='javascript:window.alert(grecaptcha.getResponse())'>\"+\n" +
                        "\"<div class='g-recaptcha' data-sitekey='" + publicKey + "'></div>\"+\n" +
                        "\"<input type='submit' value='Submit'>\"+\n" +
                        "\"</form>\"\n" +
                        ";\n" +
                        "page.viewportSize = { width:800, height:640 };\n" +
                        "page.setContent(htmlContent,\"" + referer + "\", function(status){\n" +
                        "})";
        String tempFileName = "recaptcha_" + System.currentTimeMillis() + new Random().nextInt();
        File tempFile = File.createTempFile(tempFileName, ".js");
        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
        bw.write(jsContent);
        bw.close();

        logger.info("Temp recaptcha script file location: " + tempFile.getCanonicalPath());
        //logger.info(jsContent);

        Scanner scanner = null;
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(command, tempFile.getCanonicalPath());
            if (Utils.isWindows()) { //windows batch (processor) bug workaround
                processBuilder.environment().put("SLIMERJSLAUNCHER", Utils.addFileSeparator(Utils.getAppPath()) + ReCaptchaSlimerJs.PATH_XULRUNNER_WINDOWS);
            }
            final Process process = processBuilder.start();
            scanner = new Scanner(process.getInputStream());
            StringBuilder builder = new StringBuilder();
            String s;
            while (scanner.hasNext()) {
                builder.append(scanner.next());
            }
            s = builder.toString();
            logger.info(s);
            if (s.isEmpty())
                throw new IllegalStateException("No SlimerJS output");
            process.waitFor();
            if (process.exitValue() != 0)
                throw new IOException("SlimerJS process exited abnormally");
            Matcher matcher = PlugUtils.matcher(CAPTCHA_TOKEN_SIGN + "(.+?)" + CAPTCHA_TOKEN_SIGN, s);
            if (!matcher.find())
                throw new IOException("ReCaptcha (SlimerJS) challenge not found");
            s = matcher.group(1);
            logger.info(s);
            return s;
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            throw new ErrorDuringDownloadingException(e);
        } finally {
            if (scanner != null)
                try {
                    scanner.close();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            tempFile.delete();
        }
    }

    public String getResponse() {
        if (response.contains("0.0") && !response.endsWith("0.0"))
            return response.substring(3 + response.lastIndexOf("0.0"));
        return response;
    }

    public MethodBuilder modifyResponseMethod(MethodBuilder methodBuilder) {
        return methodBuilder.setParameter("g-recaptcha-response", getResponse());
    }
}
