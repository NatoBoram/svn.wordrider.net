package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import cz.vity.freerapid.plugins.webclient.PlugUtils;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class UlozToRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UlozToRunner.class.getName());

    public UlozToRunner() {
        super();
    }

    public void runCheck(HttpFileDownloader downloader) throws Exception {
        super.runCheck(downloader);
        final GetMethod getMethod = client.getGetMethod(fileURL);
        client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            Matcher matcher;
            matcher = PlugUtils.matcher("soubor nebyl nalezen", client.getContentAsString());
            if (matcher.find()) {
                throw new URLNotAvailableAnymoreException(String.format("<b>Po�adovan� soubor nebyl nalezen.</b><br>"));
            }
            //     logger.warning(client.getContentAsString());
            //     throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");

            matcher = PlugUtils.matcher("\\|\\s*([^|]+) \\| </title>", client.getContentAsString());
            // odebiram jmeno
            String fn;
            if (matcher.find()) {
                fn = matcher.group(1);
            } else fn = sicherName(fileURL);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
            // konec odebirani jmena

            matcher = Pattern.compile("([0-9.]+ .B)</b>", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                httpFile.setFileSize(a);
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);

        final GetMethod getMethod = client.getGetMethod(fileURL);
        client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {
            if (client.getContentAsString().contains("uloz.to")) {

                while (client.getContentAsString().contains("id=\"captcha\"")) {
                    PostMethod method = stepCaptcha(client.getContentAsString());
                    //    method.setFollowRedirects(true);
                    if (super.tryDownload(method)) break;
                }

            } else {
                checkProblems();
                logger.info(client.getContentAsString());
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String sicherName(String s) throws UnsupportedEncodingException {
        Matcher matcher = PlugUtils.matcher("(.*/)([^/]*)$", s);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "file01";
    }

    private PostMethod stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("id=\"captcha\"")) {
            CaptchaSupport captchaSupport = getCaptchaSupport();
            Matcher matcher = PlugUtils.matcher("src=\"([^\"]*captcha[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);

                logger.info("Captcha URL " + s);
                String captcha = captchaSupport.getCaptcha(s);
                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {

                    String captcha_nb = getParameter("captcha_nb", contentAsString);

                    matcher = PlugUtils.matcher("form name=\"dwn\" action=\"([^\"]*)\"", contentAsString);
                    if (!matcher.find()) {
                        logger.info(client.getContentAsString());
                        throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
                    }
                    String postTargetURL;
                    postTargetURL = matcher.group(1);
                    logger.info("Captcha target URL " + postTargetURL);
                    client.setReferer(fileURL);
                    final PostMethod postMethod = client.getPostMethod(postTargetURL);
                    postMethod.addParameter("captcha_nb", captcha_nb);
                    postMethod.addParameter("captcha_user", captcha);
                    postMethod.addParameter("download", PlugUtils.unescapeHtml("--%3E+St%C3%A1hnout+soubor+%3C--"));
                    return postMethod;

                }
            } else {
                logger.warning(contentAsString);
                throw new PluginImplementationException("Captcha picture was not found");
            }

        }
        return null;
    }

    private String getParameter(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("name=\"" + s + "\"[^v>]*value=\"([^\"]*)\"", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }


    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = PlugUtils.matcher("soubor nebyl nalezen", client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Po�adovan� soubor nebyl nalezen.</b><br>"));
        }
        matcher = PlugUtils.matcher("stahovat pouze jeden soubor", client.getContentAsString());
        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("<b>M��ete stahovat pouze jeden soubor nar�z</b><br>"));

        }


    }

}
