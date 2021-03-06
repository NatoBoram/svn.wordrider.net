package cz.vity.freerapid.plugins.services.kumpulbagi;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class KumpulBagiFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KumpulBagiFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matchName = PlugUtils.matcher("<h2.+>(.+?)</h2>", content);
        if (!matchName.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matchName.group(1).trim());
        final Matcher matchSize = PlugUtils.matcher("class=\"file_size\">\\s*(.+?)\\s*</div>", content);
        if (!matchSize.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchSize.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            fileURL = method.getURI().getURI();
            login();
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setBaseURL(getBaseUrl())
                    .setActionFromFormWhereTagContains("download_form", true)
                    .setAjax().toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final String url = PlugUtils.getStringBetween(getContentAsString(), "DownloadUrl\":\"", "\"");
            if (!tryDownloadAndSaveFile(getGetMethod(url))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<p>404</p>") || // contentAsString.contains("error404") ||
                contentAsString.contains("The requested resource is not found") ||
                contentAsString.contains("tidak bisa menemukan pencarian Anda")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("dialog_sign_up")) {
            throw new NotRecoverableDownloadException("This file needs an account for download.");
        }
    }

    private void login() throws Exception {
        synchronized (KumpulBagiFileRunner.class) {
            KumpulBagiServiceImpl service = (KumpulBagiServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                Matcher matcher = getMatcherAgainstContent("<a[^>]+?href\\s*=\\s*[\"']([^\"']+?/Account/Login[^\"']+)[\"']");
                if (!matcher.find())
                    throw new ServiceConnectionProblemException("Error finding login url");
                HttpMethod method = getMethodBuilder().setReferer(fileURL)
                        .setAction(matcher.group(1).trim()).toGetMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException("Error loading login page");
                }
                method = getMethodBuilder().setReferer(fileURL)
                        .setActionFromFormWhereActionContains("n/Account/Login", true)
                        .setParameter("UserName", pa.getUsername())
                        .setParameter("Password", pa.getPassword())
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    throw new ServiceConnectionProblemException("Error logging in");
                }
                if (getContentAsString().contains("input-validation-error") || getContentAsString().contains("field-validation-error"))
                    throw new PluginImplementationException("Incorrect account details");
        }}
    }

    private String getBaseUrl() throws PluginImplementationException {
        URL url;
        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            throw new PluginImplementationException("Invalid fileURL");
        }
        return url.getProtocol() + "://" + url.getAuthority();
    }
}