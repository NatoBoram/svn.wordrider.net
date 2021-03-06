package cz.vity.freerapid.plugins.services.superbshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SuperBShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SuperBShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkFileProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matchN = PlugUtils.matcher("Název:</.+?>\\s*?<.+?>(.+?)<", content);
        if (!matchN.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matchN.group(1));
        final Matcher matchS = PlugUtils.matcher("Velikost:</.+?>\\s*?<.+?>.*?\\((.+?)\\)<", content);
        if (!matchS.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matchS.group(1)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Stáhnout").toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkFileProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Požadovaný soubor není dostupný")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Soubor byl označen jako zdroj se závadným obsahem")) {
            throw new URLNotAvailableAnymoreException("Soubor byl označen jako zdroj se závadným obsahem - File has been flagged as a source with malicious content"); //let to know user in FRD
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        checkFileProblems();
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Stahovat soubory může jen registrovaný uživatel")) {
            throw new NotRecoverableDownloadException("Stahovat soubory může jen registrovaný uživatel - Only registered users can download"); //let to know user in FRD
        }
    }

    private void login() throws Exception {
        synchronized (SuperBShareFileRunner.class) {
            final SuperBShareServiceImpl service = (SuperBShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                if (!makeRedirectedRequest(getGetMethod("http://www.superbshare.com/sign/in"))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                final String eq = PlugUtils.getStringBetween(getContentAsString(), "var js_val =", ";");
                final String result = "" + evaluate(eq);
                logger.info("Human check equation : " + eq + " = " + result);
                final HttpMethod httpMethod = getMethodBuilder()
                        .setActionFromFormWhereActionContains("signIn", true)
                        .setParameter("username", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setParameter("js_check", result)
                        .setParameter("Submit", "Sign In")
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException("Error posting login info");
                if (getContentAsString().contains("Heslo nebo jméno není správné") || getContentAsString().contains("Přihlásit se")||
                        getContentAsString().contains("Sign In Failed"))
                    throw new BadLoginException("Invalid SuperBShare account login information!");
                logger.info("Logged in.");
            }
        }
    }

    private int evaluate(final String equationString) throws ErrorDuringDownloadingException {
        try {
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            int value;
            try {
                value = ((Double) engine.eval(equationString)).intValue();
            } catch (Exception e) {
                value = (Integer) engine.eval(equationString);
            }
            return value;
        } catch (Exception e) {
            throw new PluginImplementationException(e.getMessage());
        }
    }
}