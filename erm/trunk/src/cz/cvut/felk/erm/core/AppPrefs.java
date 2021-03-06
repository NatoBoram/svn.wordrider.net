package cz.cvut.felk.erm.core;

import cz.cvut.felk.erm.utilities.LogUtils;
import org.jdesktop.application.LocalStorage;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Sprava uzivatelskych properties
 *
 * @author Ladislav Vitasek
 */
public final class AppPrefs {
    private final static Logger logger = Logger.getLogger(AppPrefs.class.getName());

    private static volatile String appPath = null;

    /**
     * Soubor pod kterym jsou polozky ulozeny
     */
    private static final String DEFAULT_PROPERTIES = "erm.xml";

    private static volatile Preferences properties = loadProperties();

    // vychozi hodnoty pro uzivatelska nastaveni

    private AppPrefs() {
    }


    /**
     * Vrati nastaveni z properties fajlu
     *
     * @param key          klic property
     * @param defaultValue defaultni hodnota, ktera se pouzije pokud neni hodnota nalezena
     * @return hodnota uzivatelskeho nastaveni
     */
    public static int getProperty(final String key, final int defaultValue) {
        return properties.getInt(key, defaultValue);
    }

    /**
     * Vrati nastaveni z properties fajlu
     *
     * @param key          klic property
     * @param defaultValue defaultni hodnota, ktera se pouzije pokud neni hodnota nalezena
     * @return hodnota uzivatelskeho nastaveni
     */
    public static boolean getProperty(final String key, final boolean defaultValue) {
        return properties.getBoolean(key, defaultValue);
    }

    /**
     * Vrati nastaveni z properties fajlu. Pokud neni hodnota klice nalezena, vraci null!
     *
     * @param key klic property
     * @return hodnota uzivatelskeho nastaveni
     */
    public static String getProperty(final String key) {
        return properties.get(key, null);
    }

    /**
     * Provede ulozeni uzivatelskeho nastaveni do Properties
     *
     * @param key   hodnota klice
     * @param value hodnota uzivatelskeho nastaveni
     */
    public static void storeProperty(final String key, final boolean value) {
        properties.putBoolean(key, value);
    }


    /**
     * Provede ulozeni uzivatelskeho nastaveni do Properties
     *
     * @param key   hodnota klice
     * @param value hodnota uzivatelskeho nastaveni
     */
    public static void storeProperty(final String key, final String value) {
        properties.put(key, value);
    }

    /**
     * Provede ulozeni uzivatelskeho nastaveni do Properties
     *
     * @param key   hodnota klice
     * @param value hodnota uzivatelskeho nastaveni
     */
    public static void storeProperty(final String key, final int value) {
        properties.putInt(key, value);
    }

    /**
     * Provede ulozeni uzivatelskeho nastaveni do Properties
     *
     * @param key          hodnota klice
     * @param defaultValue hodnota uzivatelskeho nastaveni
     */
    public static String getProperty(final String key, final String defaultValue) {
        return properties.get(key, defaultValue);
    }


    /**
     * Odstraneni klic-hodnota z properties fajlu
     *
     * @param key klic property k odstaneni
     */
    public static void removeProperty(final String key) {
        properties.remove(key);
    }

    /**
     * Provede ulozeni properties do souboru definovaneho systemem. Uklada se do XML.
     */
    public static void store() {
        OutputStream outputStream = null;
        try {
//            final File f = new File(propertiesFile);
//            if (!f.exists()) {
//                final File parentFile = f.getParentFile();
//                if (parentFile != null)
//                    parentFile.mkdirs();
//            }
//
            if (!AppPrefs.getProperty(FWProp.PROXY_SAVEPASSWORD, false))
                removeProperty(FWProp.PROXY_PASSWORD);
            final LocalStorage localStorage = MainApp.getAContext().getLocalStorage();
            final File outDir = localStorage.getDirectory();
            outDir.mkdirs();
            //outputStream = localStorage.openOutputFile(DEFAULT_PROPERTIES);
            outputStream = new FileOutputStream(new File(outDir, DEFAULT_PROPERTIES));
            properties.exportNode(outputStream);
            outputStream.close();
        } catch (IOException e) {
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, e.getMessage(), ex);
            }
            logger.severe("Couldn't save app properties. This is a fatal error. Please reinstall the application.");
        } catch (Exception e) {
            e.printStackTrace();//bez logovani
        }
        logger.info("Preferences were saved successfuly");
    }

    /**
     * Provede nacteni properties ze souboru definovaneho systemem. Pokud nacteni selze, vraci prazdne properties.
     * Properties se nacitaji z XML.
     */
    public static Preferences loadProperties() {
        final LocalStorage localStorage = MainApp.getAContext().getLocalStorage();
        final File storageDir = localStorage.getDirectory();
        final File userFile = new File(storageDir, DEFAULT_PROPERTIES);
        if (!(userFile.exists())) {
            logger.log(Level.INFO, "File with user settings " + userFile + " was not found. First run. Using default settings");
            return Preferences.userRoot().node("erm");
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(storageDir, DEFAULT_PROPERTIES));
            //props.loadFromXML(inputStream);
            Preferences.importPreferences(inputStream);
            inputStream.close();
        } catch (FileNotFoundException e) {
            logger.log(Level.CONFIG, "User preferences file was not found (first application launch?)");
        } catch (Exception e) {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
            }
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return Preferences.userRoot().node("erm");
    }

    /**
     * Vraci aktualni cestu k adresari programu ve kterem je jar spusten
     *
     * @return cesta do adresare
     */
    public static String getAppPath() {
        if (appPath != null)
            return appPath;
        try {
            final URI uri = MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            if (!uri.getScheme().equalsIgnoreCase("file")) {
                logger.info("Running Webstart application");
                return appPath = "";
            }
            appPath = new File(uri).getParent();
        } catch (URISyntaxException e) {
            LogUtils.processException(logger, e);
            return appPath = "";
        }
        logger.info("App Path is " + appPath);
        return appPath;
    }

    public static Preferences getPreferences() {
        return properties;
    }
}
