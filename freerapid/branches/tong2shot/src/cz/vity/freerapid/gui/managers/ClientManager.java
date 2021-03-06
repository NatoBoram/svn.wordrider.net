package cz.vity.freerapid.gui.managers;

import cz.vity.freerapid.core.AppPrefs;
import cz.vity.freerapid.core.FWProp;
import cz.vity.freerapid.core.UserProp;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.ssl.EasySSLProtocolSocketFactory;
import cz.vity.freerapid.plugins.webclient.ssl.SSLProtocolSocketFactory;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.jdesktop.application.ApplicationContext;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vity
 */
public class ClientManager {

    private final static String SOCKS_PREFIX_REGEXP = "^(\\$SOCKS\\$|SOCKS\\:)";

    private final static Logger logger = Logger.getLogger(ClientManager.class.getName());

    private final List<ConnectionSettings> availableConnections = new ArrayList<ConnectionSettings>(2);
    private static final String PROXY_LIST_DEFAULT_PATH = new File(Utils.getAppPath(), "proxy.list").getAbsolutePath();

    private ConnectionSettings defaultConnectionSettings = new ConnectionSettings();

    private final Object connectionSettingsLock = new Object();
    private final ManagerDirector managerDirector;
    private final ApplicationContext context;
    private final Map<String, Integer> rotate = new HashMap<String, Integer>();

    private final Object proxyPerPluginConnectionSettingsLock = new Object();
    private final Map<String, Integer> proxyPerPluginRotate = new HashMap<String, Integer>();

    public ClientManager(ManagerDirector managerDirector) {
        this.managerDirector = managerDirector;
        this.context = managerDirector.getContext();
        defaultConnectionSettings.setDefault(true);
        final boolean useSystemProxies = AppPrefs.getProperty(UserProp.USE_SYSTEM_PROXIES, UserProp.USE_SYSTEM_PROXIES_DEFAULT);
        if (!useSystemProxies)
            ProxySelector.setDefault(null);
        //System.setProperty("java.net.useSystemProxies", useSystemProxies);

        initSSL();
        updateConnectionSettings();
    }

    private void initSSL() {
        try {
            boolean verifyHostname = AppPrefs.getProperty(UserProp.SSL_VERIFY_HOSTNAME, UserProp.SSL_VERIFY_HOSTNAME_DEFAULT);
            ProtocolSocketFactory sf = (AppPrefs.getProperty(UserProp.SSL_CUSTOM_CA_CERT, UserProp.SSL_CUSTOM_CA_CERT_DEFAULT) ?
                    new SSLProtocolSocketFactory(context, verifyHostname) :
                    new EasySSLProtocolSocketFactory());
            Protocol p = new Protocol("https", sf, 443);
            Protocol.registerProtocol("https", p);
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            logger.warning("SSL initialization failed - some plugins won't work");
        }
    }

    private void updateProxies() {
        if (AppPrefs.getProperty(UserProp.USE_PROXY_LIST, UserProp.USE_PROXY_LIST_DEFAULT)) {
            final String file = AppPrefs.getProperty(UserProp.PROXY_LIST_PATH, PROXY_LIST_DEFAULT_PATH);
            final File f = new File(file);
            if (f.exists() && f.isFile() && f.canRead()) {
                try {
                    readProxyList(f);
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

    private void updateDefault() {
        if (useDefaultConnection()) {
            final boolean isEnabled = defaultConnectionSettings.isEnabled();
            defaultConnectionSettings = new ConnectionSettings();
            defaultConnectionSettings.setDefault(true);
            initDefaultProxySettings(defaultConnectionSettings);
            defaultConnectionSettings.setEnabled(isEnabled);
            availableConnections.add(defaultConnectionSettings);
        }

        setAuthenticator();
    }

    private boolean useDefaultConnection() {
        return AppPrefs.getProperty(UserProp.USE_DEFAULT_CONNECTION, UserProp.USE_DEFAULT_CONNECTION_DEFAULT);
    }

    private void initDefaultProxySettings(ConnectionSettings connectionSettings) {
        final String proxySetSystemDefault = System.getProperty("proxySet", "false");
        logger.info("Proxy set system default connection: " + proxySetSystemDefault);
        if (AppPrefs.getProperty(FWProp.PROXY_USE, Boolean.valueOf(proxySetSystemDefault))) {

            final String url = AppPrefs.getProperty(FWProp.PROXY_URL, System.getProperty("proxyHost", "localhost"));
            int port;
            try {
                port = Integer.valueOf(AppPrefs.getProperty(FWProp.PROXY_PORT, System.getProperty("proxyPort", "8080")));
            } catch (NumberFormatException e) {
                port = 8080;
            }

            if (AppPrefs.getProperty(FWProp.PROXY_LOGIN, false)) {
                final String userName = AppPrefs.getProperty(FWProp.PROXY_USERNAME, "");
                final String password = Utils.generateXorString(AppPrefs.getProperty(FWProp.PROXY_PASSWORD, ""));
                connectionSettings.setProxy(url, port, Proxy.Type.HTTP, userName, password);
            } else
                connectionSettings.setProxy(url, port, Proxy.Type.HTTP);

            final boolean socks = AppPrefs.getProperty(UserProp.DEFAULT_CONNECTION_SOCKS, UserProp.DEFAULT_CONNECTION_SOCKS_DEFAULT);
            if (socks)
                connectionSettings.setProxyType(Proxy.Type.SOCKS);

            logger.info("Setting proxy configuration ON with configuration: " + connectionSettings.toString());
        } else {
            logger.info("Setting proxy configuration OFF for default connection");
        }
    }

    private void setAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                //called from SocksSocketImpl
                if ("SOCKS5".equals(this.getRequestingProtocol())) {
                    final ConnectionSettings conn = findConnectionByParameters(this.getRequestingHost(), this.getRequestingPort(), Proxy.Type.SOCKS);
                    if (conn != null) {
                        final String pass = conn.getPassword();
                        final char[] password = (pass == null) ? new char[0] : pass.toCharArray();
                        return new PasswordAuthentication(conn.getUserName(), password);
                    }
                }
                return null;
            }
        });
    }


    private ConnectionSettings findConnectionByParameters(String proxyURL, int proxyPort, Proxy.Type proxyType) {
        final List<ConnectionSettings> conns = getAvailableConnections();
        for (ConnectionSettings conn : conns) {
            if (conn.getProxyPort() == proxyPort && conn.getProxyType() == proxyType && conn.getProxyURL().equalsIgnoreCase(proxyURL))
                return conn;
        }
        return null;
    }

    public ConnectionSettings getProxyConnection(String strProxy, boolean autodetectSOCKS) {
        Proxy.Type proxyType;

        final Pattern patternWhole = Pattern.compile("((\\w*)(:(.*?))?@)?(.*?):(\\d{2,5})");
        final Pattern socksPattern = Pattern.compile(SOCKS_PREFIX_REGEXP, Pattern.CASE_INSENSITIVE);

        if (strProxy.isEmpty())
            return null;
        final Matcher matcherSocks = socksPattern.matcher(strProxy);
        if (matcherSocks.find()) {
            proxyType = Proxy.Type.SOCKS;
            strProxy = strProxy.substring(matcherSocks.group(1).length());
        } else
            proxyType = Proxy.Type.HTTP;

        final Matcher matcher = patternWhole.matcher(strProxy);
        if (matcher.matches()) {
            int i = strProxy.lastIndexOf('@');
            final String s2;
            final ConnectionSettings settings = new ConnectionSettings();
            if (i >= 0) {
                String s1 = strProxy.substring(0, i);
                s2 = strProxy.substring(i + 1);
                i = s1.indexOf(':');
                final String[] hostPort = s2.split(":");
                final Integer port = Integer.valueOf(hostPort[1]);
                if (port > 65535)
                    return null;
                if (autodetectSOCKS && port >= 1080 && port <= 1090)
                    proxyType = Proxy.Type.SOCKS;
                if (i > 0)
                    settings.setProxy(hostPort[0], port, proxyType, s1.substring(0, i), s1.substring(i + 1));
                else
                    settings.setProxy(hostPort[0], port, proxyType, s1, null);
            } else {
                s2 = strProxy;
                final String[] hostPort = s2.split(":");
                final Integer port = Integer.valueOf(hostPort[1]);
                if (port > 65535)
                    return null;
                if (autodetectSOCKS && port >= 1080 && port <= 1090)
                    proxyType = Proxy.Type.SOCKS;
                settings.setProxy(hostPort[0], port, proxyType);
            }
            logger.info("Reading proxy definition " + settings.toString());
            return settings;
        } else {
            logger.warning("String " + strProxy + " does not match to proxy definition pattern - [username[:password@]]host:port");
            return null;
        }
    }

    private void readProxyList(File f) {
        final String[] strings = Utils.loadFile(f).split("(\\s)");
        final boolean autodetectSOCKS = AppPrefs.getProperty(UserProp.AUTODETECT_SOCKSPROXY, UserProp.AUTODETECT_SOCKSPROXY_DEFAULT);
        for (String s : strings) {
            final ConnectionSettings settings = getProxyConnection(s, autodetectSOCKS);
            if (settings != null)
                availableConnections.add(settings);
        }
    }

    public List<ConnectionSettings> getAvailableConnections() {
        synchronized (connectionSettingsLock) {
            return Collections.unmodifiableList(availableConnections);
        }
    }

    public List<ConnectionSettings> getEnabledConnections() {
        synchronized (connectionSettingsLock) {
            return Collections.unmodifiableList(getEnabled());
        }
    }

    public List<ConnectionSettings> getRotatedEnabledConnections(String id) {
        synchronized (connectionSettingsLock) {
            final List<ConnectionSettings> list = new ArrayList<ConnectionSettings>(availableConnections.size());
            for (ConnectionSettings settings : availableConnections) {
                if (settings.isEnabled() && !settings.isDefault()) {
                    list.add(settings);
                }
            }

            if (list.size() > 1) {//rotate enabled proxies
                Collections.rotate(list, getNextRotation(id));
            }
            if (useDefaultConnection() && defaultConnectionSettings.isEnabled())
                list.add(0, defaultConnectionSettings);
            return list;
        }
    }

    private int getNextRotation(String id) {
        if (rotate.containsKey(id)) {
            int r = rotate.get(id);
            r++;
            rotate.put(id, r);
            logger.info("Rotate for '" + id + "' is: " + r);
            return r;
        } else {
            rotate.put(id, 0);
            return 0;
        }
    }

    public List<ConnectionSettings> getProxyForPluginConnections(String pluginId) {
        List<String> proxies = managerDirector.getProxyForPluginManager().getProxies(pluginId);
        List<ConnectionSettings> result = new ArrayList<ConnectionSettings>();
        if (proxies != null && proxies.size() > 0) {
            for (String proxy : proxies) {
                ConnectionSettings connectionSettings = getProxyConnection(proxy, false);
                if (connectionSettings != null) {
                    result.add(connectionSettings);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public ConnectionSettings getProxyForPluginRotatedConnection(String id, List<String> proxies) {
        synchronized (proxyPerPluginConnectionSettingsLock) {
            if (proxies == null || proxies.isEmpty()) {
                return getRotatedEnabledConnections(id).get(0); //as fallback
            }
            for (int i = 0; i < proxies.size(); i++) {
                Collections.rotate(proxies, getProxyForPluginNextRotation(id));
                ConnectionSettings proxy = getProxyConnection(proxies.get(0), false);
                if (proxy != null) {
                    return proxy;
                }
            }
            logger.warning("Invalid 'proxy for plugin' connection settings. Using application's connections settings as fallback");
            return getRotatedEnabledConnections(id).get(0); //as fallback
        }
    }

    private int getProxyForPluginNextRotation(String id) {
        if (proxyPerPluginRotate.containsKey(id)) {
            int r = proxyPerPluginRotate.get(id);
            r++;
            proxyPerPluginRotate.put(id, r);
            logger.info("Proxy for plugin rotate for '" + id + "' is: " + r);
            return r;
        } else {
            proxyPerPluginRotate.put(id, 0);
            return 0;
        }
    }

    private List<ConnectionSettings> getEnabled() {
        final List<ConnectionSettings> list = new LinkedList<ConnectionSettings>();
        for (ConnectionSettings settings : availableConnections) {
            if (settings.isEnabled()) {
                list.add(settings);
            }
        }
        return list;
    }

    public void setConnectionEnabled(ConnectionSettings settings, boolean enabled) {
        synchronized (connectionSettingsLock) {
            settings.setEnabled(enabled);
        }
    }

    public void updateConnectionSettings() {
        synchronized (connectionSettingsLock) {
            availableConnections.clear();
            updateDefault();

            updateProxies();
        }
        final MenuManager menuManager = managerDirector.getMenuManager();
        if (menuManager != null)
            menuManager.updateConnectionSettings(getAvailableConnections());
    }

}
