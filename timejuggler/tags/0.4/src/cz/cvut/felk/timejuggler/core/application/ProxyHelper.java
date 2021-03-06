package cz.cvut.felk.timejuggler.core.application;

import cz.cvut.felk.timejuggler.core.AppPrefs;
import cz.cvut.felk.timejuggler.utilities.Utils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class ProxyHelper {
    private final static Logger logger = Logger.getLogger(ProxyHelper.class.getName());

    private ProxyHelper() {
    }

    public static void initProxy() {
        if (AppPrefs.getProperty(AppPrefs.PROXY_USE, false)) {

            final StringBuilder builder = new StringBuilder();
            final String url = AppPrefs.getProperty(AppPrefs.PROXY_URL, "localhost");
            final String port = AppPrefs.getProperty(AppPrefs.PROXY_PORT, "8080");
            builder.append("\nUrl-").append(url).append("\nPort-").append(port);
            System.setProperty("proxySet", "true");
            System.setProperty("https.proxyHost", url);

            System.setProperty("https.proxyPort", port);
            System.setProperty("proxyHost", url);
            System.setProperty("proxyPort", port);
            if (AppPrefs.getProperty(AppPrefs.PROXY_LOGIN, false)) {
                final String userName = AppPrefs.getProperty(AppPrefs.PROXY_USERNAME, "");
                final String password = Utils.generateXorString(AppPrefs.getProperty(AppPrefs.PROXY_PASSWORD, ""));
                builder.append("\nProxy Login Name -").append(userName);
                Authenticator.setDefault(new HttpAuthenticateProxy(userName, password));
            }
            logger.config("Setting proxy configuration ON with configuration: " + builder.toString());
        } else {
            System.setProperty("proxySet", "false");
            logger.config("Setting proxy configuration OFF");
        }

    }

    private static final class HttpAuthenticateProxy extends Authenticator {
        private final String proxyUsername;
        private final String proxyPassword;

        public HttpAuthenticateProxy(final String userName, final String password) {
            super();
            this.proxyUsername = userName;
            this.proxyPassword = password;
        }

        protected final PasswordAuthentication getPasswordAuthentication() {
            // username, password
            // sets http authentication
            return new PasswordAuthentication(proxyUsername,
                    proxyPassword.toCharArray());
        }
    }

}
