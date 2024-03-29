package cz.vity.freerapid.plugins.services.czshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Jan Smejkal (edit from CZshare and RapidShare premium to CZshare profi)
 * @author ntoskrnl
 */
class CzshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CzshareRunner.class.getName());
    private final static int WAIT_TIME = 30;
    private final static String BASE_URL = "http://sdilej.cz";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        normalizeFileURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        final Matcher filenameMatcher = getMatcherAgainstContent("<h1[^>]*>(.+?)<");
        if (!filenameMatcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(filenameMatcher.group(1));

        final Matcher filesizeMatcher = getMatcherAgainstContent("Velikost\\s*:\\s*(?:<[^>]+>\\s*)(.+?)\\s*<");
        if (!filesizeMatcher.find()) {
            throw new PluginImplementationException("File size not found");
        }
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1).replace("i", "")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        normalizeFileURL();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getMethodBuilder().setAction(fileURL).setReferer("").setBaseURL(BASE_URL).toGetMethod();
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            method = getMethodBuilder().setActionFromAHrefWhereATagContains("Stáhnout").setReferer(fileURL).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void login() throws Exception {
        synchronized (CzshareRunner.class) {
            CzshareServiceImpl service = (CzshareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No CZShare/Sdilej premium account login information");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setReferer("https://sdilej.cz/prihlasit")
                    .setAction("https://sdilej.cz/sql.php")
                    .setParameter("login", pa.getUsername())
                    .setParameter("heslo", pa.getPassword())
                    .toPostMethod();
            final int status = client.makeRequest(method, false);
            if (status/100 == 3) {
                if (method.getResponseHeader("Location").getValue().contains("error"))
                    throw new PluginImplementationException("Login Error");
            }
            else if (status != 200){
                throw new ServiceConnectionProblemException("Unknown login error");
            }
            if (getContentAsString().contains("Zadané jméno se neshoduje s heslem")) {
                throw new BadLoginException("Invalid CZShare/Sdilej premium account login information");
            }
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        Matcher matcher;
        matcher = getMatcherAgainstContent("Soubor nenalezen");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor nenalezen</b><br>");
        }
        if (getContentAsString().contains("Tento soubor byl smazán")) {
            throw new URLNotAvailableAnymoreException("Tento soubor byl smazán");
        }
        if (getContentAsString().contains("Chyba 404 Nenalezeno")) {
            throw new URLNotAvailableAnymoreException("Chyba 404 Nenalezeno");
        }
        matcher = getMatcherAgainstContent("Soubor expiroval");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor expiroval</b><br>");
        }
        matcher = getMatcherAgainstContent("Soubor byl smaz.n jeho odesilatelem</strong>");
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException("<b>Soubor byl smaz�n jeho odesilatelem</b><br>");
        }
        if (getContentAsString().contains("Tento soubor byl na upozornění identifikován jako warez")) {
            throw new URLNotAvailableAnymoreException("Tento soubor byl na upozornění identifikován jako warez");
        }
        if (getContentAsString().contains("Tato IP není u tohoto účtu povolena")) {
            throw new NotRecoverableDownloadException("Tato IP není u tohoto účtu povolena");
        }
        matcher = getMatcherAgainstContent("Bohu.el je vy.erp.na maxim.ln. kapacita FREE download.");
        if (matcher.find()) {
            throw new YouHaveToWaitException("Bohužel je vyčerpána maximální kapacita FREE downloadů", WAIT_TIME);
        }
    }

    private void normalizeFileURL() {
        fileURL = fileURL.replaceFirst("czshare\\.cz", "czshare.com");
        fileURL = fileURL.replaceFirst("czshare\\.com", "sdilej.cz");
        fileURL = fileURL.replaceFirst("https:", "http:");
    }

}