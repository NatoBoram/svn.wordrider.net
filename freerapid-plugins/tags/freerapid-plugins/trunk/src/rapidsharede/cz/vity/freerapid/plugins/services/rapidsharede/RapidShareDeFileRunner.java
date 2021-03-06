package cz.vity.freerapid.plugins.services.rapidsharede;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 */
class RapidShareDeFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(RapidShareDeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkSeriousProblems();
            client.setReferer(fileURL);
            final String redirectURL = "http://rapidshare.de/";
            final PostMethod postMethod = getPostMethod(redirectURL);
            PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"uri"});
            postMethod.addParameter("dl.start", "Free");

            if (makeRedirectedRequest(postMethod)) {
                checkNameAndSize();
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            checkAllProblems();
            client.setReferer(fileURL);
            final String redirectURL = "http://rapidshare.de/";
            PostMethod postMethod = getPostMethod(redirectURL);
            PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"uri"});
            postMethod.addParameter("dl.start", "Free");

            if (makeRedirectedRequest(postMethod)) {
                checkAllProblems();
                checkNameAndSize();

                Matcher matcher = getMatcherAgainstContent("unescape\\(\'(.+?)\'\\)");

                if (matcher.find()) {
                    final String downloadForm = URLDecoder.decode(matcher.group(1), "ISO-8859-1");

                    matcher = getMatcherAgainstContent("var c = (.+?);");

                    if (matcher.find()) {
                        int waitSeconds = Integer.parseInt(matcher.group(1));
                        downloadTask.sleep(waitSeconds);
                        client.setReferer(redirectURL);
                        postMethod = stepCaptcha(downloadForm);

                        if (!tryDownloadAndSaveFile(postMethod)) {
                            checkAllProblems();
                            logger.warning(getContentAsString());
                            throw new IOException("File input stream is empty");
                        }
                    } else {
                        throw new PluginImplementationException();
                    }
                } else {
                    throw new PluginImplementationException();
                }
            } else {
                throw new ServiceConnectionProblemException();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }

        if (contentAsString.contains("This file has been deleted")) {
            throw new URLNotAvailableAnymoreException("This file has been deleted");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();
        final Matcher matcher;

        if (contentAsString.contains("Access-code wrong")) {
            throw new ServiceConnectionProblemException("Access-code wrong");
        }

        if (contentAsString.contains("You have reached the download limit for free users")) {
            matcher = getMatcherAgainstContent("\\(Or wait (.+?) minute.?\\)");

            int waitMinutes = (matcher.find()) ? Integer.parseInt(matcher.group(1)) : 2;
            throw new YouHaveToWaitException("You have reached the download limit for free users", 60 * waitMinutes);
        }

        matcher = getMatcherAgainstContent("Your IP-address (.+?) is already downloading a file");

        if (matcher.find()) {
            throw new ServiceConnectionProblemException(String.format("Your IP-address %s is already downloading a file. You have to wait until it is finished", matcher.group(1)));
        }

        if (contentAsString.contains("Download-session invalid")) {
            throw new ServiceConnectionProblemException("Download-session invalid");
        }

        if (contentAsString.contains("Download-Ticket nicht bereit")) {
            throw new ServiceConnectionProblemException("Download-Ticket nicht bereit");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("You have requested the file <b>(.+?)</b>");

        if (matcher.find()) {
            final String fileName = matcher.group(1).trim();
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

            matcher = getMatcherAgainstContent("</b> \\((.+?)\\)\\.</p>");

            if (matcher.find()) {
                final long fileSize = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + fileSize);
                httpFile.setFileSize(fileSize);
            } else {
                logger.warning("File size was not found");
                throw new PluginImplementationException();
            }
        } else {
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private PostMethod stepCaptcha(String downloadForm) throws ErrorDuringDownloadingException {
        final CaptchaSupport captchaSupport = getCaptchaSupport();

        Matcher matcher = PlugUtils.matcher("img src=\"(.+?)\"", downloadForm);

        if (matcher.find()) {
            final String captchaSrc = matcher.group(1);
            logger.info("Captcha URL " + captchaSrc);
            final String captcha = captchaSupport.getCaptcha(captchaSrc);

            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            } else {
                matcher = PlugUtils.matcher("action=\"(.+?)\"", downloadForm);

                if (matcher.find()) {
                    final String finalURL = matcher.group(1);
                    final PostMethod postMethod = getPostMethod(finalURL);
                    postMethod.addParameter("captcha", captcha);

                    return postMethod;
                } else {
                    throw new PluginImplementationException("Download link was not found");
                }
            }
        } else {
            throw new PluginImplementationException("Captcha picture was not found");
        }
    }
}