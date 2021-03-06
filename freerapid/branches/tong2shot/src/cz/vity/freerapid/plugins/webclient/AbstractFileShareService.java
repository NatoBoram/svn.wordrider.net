package cz.vity.freerapid.plugins.webclient;

import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.*;
import cz.vity.freerapid.utilities.LogUtils;
import org.java.plugin.Plugin;
import org.java.plugin.PluginClassLoader;
import org.java.plugin.registry.PluginAttribute;
import org.java.plugin.registry.PluginDescriptor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Implements ShareDownloadService and adds basic functionality
 *
 * @author Vity
 */
public abstract class AbstractFileShareService extends Plugin implements ShareDownloadService {
    /**
     * Field logger
     */
    private final static Logger logger = Logger.getLogger(AbstractFileShareService.class.getName());

    /**
     * Field pluginContext
     */
    private PluginContext pluginContext;
    /**
     * Field image
     */
    private Icon image;

    /**
     * Constructor AbstractFileShareService creates a new AbstractFileShareService instance.
     */
    public AbstractFileShareService() {
        super();
    }

    @Override
    final protected void doStart() throws Exception {
        final PluginDescriptor desc = this.getDescriptor();
        final PluginAttribute attr = desc.getAttribute("faviconImage");
        if (attr != null) {
            final PluginClassLoader loader = getManager().getPluginClassLoader(desc);
            if (loader != null) {
                final URL resource = loader.getResource(attr.getValue());
                if (resource == null) {
                    logger.warning("Icon image for plugin '" + desc.getId() + "' was not found");
                } else {
                    try {
                        image = new ImageIcon(ImageIO.read(resource));
                    } catch (IOException e) {
                        logger.warning("Icon image for plugin '" + desc.getId() + "' reading failed");
                    }
                }
            }
        }
        pluginInit();
    }

    @Override
    public void pluginInit() {

    }

    @Override
    public void pluginStop() {

    }

    @Override
    final protected void doStop() throws Exception {
        pluginStop();
    }

    @Override
    public Icon getFaviconImage() {
        return image;
    }

    @Override
    public String getId() {
        return this.getDescriptor().getId();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void run(HttpFileDownloadTask downloadTask) throws Exception {
        //checkSupportedURL(downloadTask);
        final PluginRunner pluginRunner = getPluginRunnerInstance();
        if (pluginRunner != null) {
            pluginRunner.init(this, downloadTask);
            pluginRunner.run();
        } else throw new NullPointerException("getPluginRunnerInstance must no return null");

    }

    @Override
    public void runCheck(HttpFileDownloadTask downloadTask) throws Exception {
        //checkSupportedURL(downloadTask);
        final PluginRunner pluginRunner = getPluginRunnerInstance();
        if (pluginRunner != null) {
            pluginRunner.init(this, downloadTask);
            pluginRunner.runCheck();
        } else throw new NullPointerException("getPluginRunnerInstance must no return null");
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    public void showOptions() throws Exception {

    }

    @Override
    public void showLocalOptions(HttpFile httpFile) throws Exception {
        //if it's not implemented, use the global one (showOptions)
        showOptions();
    }

    @Override
    public PluginContext getPluginContext() {
        return pluginContext;
    }

    @Override
    public void setPluginContext(PluginContext pluginContext) {
        this.pluginContext = pluginContext;
    }

    /**
     * Shows standard account dialog with given account
     *
     * @param account          account with user name and password
     * @param dialogTitle      title for dialog
     * @param pluginConfigFile file name for storing configuration
     * @param emptyAllowed     if true, empty account is allowed - for free user, multi account types eg. XFS plugin
     * @return returns account parametr, if user pressed Cancel button, otherwise it returns updated account instance
     */
    protected PremiumAccount showAccountDialog(final PremiumAccount account, String dialogTitle, final String pluginConfigFile, final boolean emptyAllowed) {
        final DialogSupport dialogSupport = getPluginContext().getDialogSupport();
        try {//saving new username/password
            final PremiumAccount pa = dialogSupport.showAccountDialog(account, dialogTitle, emptyAllowed);//vysledek bude Premium ucet - Rapidshare
            if (pa != null) {
                getPluginContext().getConfigurationStorageSupport().storeConfigToFile(pa, pluginConfigFile);
                return pa;//return new username/password
            }
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        return account;
    }

    /**
     * Shows standard account dialog with given account
     *
     * @param account          account with user name and password
     * @param dialogTitle      title for dialog
     * @param pluginConfigFile file name for storing configuration
     * @return returns account parametr, if user pressed Cancel button, otherwise it returns updated account instance
     */
    protected PremiumAccount showAccountDialog(final PremiumAccount account, String dialogTitle, final String pluginConfigFile) {
        return showAccountDialog(account, dialogTitle, pluginConfigFile, false);
    }

    /**
     * Loads PremiumAccount information from file.<br>
     * Returns new PremiumAccount instance if there is no configuration file yet.
     *
     * @param pluginConfigFile file name of configuration file
     * @return instance of PremiumAccount - loaded from file or new instance if there is no configuration on disk yet
     */
    protected PremiumAccount getAccountConfigFromFile(final String pluginConfigFile) {
        if (getPluginContext().getConfigurationStorageSupport().configFileExists(pluginConfigFile)) {
            try {
                return getPluginContext().getConfigurationStorageSupport().loadConfigFromFile(pluginConfigFile, PremiumAccount.class);
            } catch (Exception e) {
                LogUtils.processException(logger, e);
                return new PremiumAccount();
            }
        } else {
            return new PremiumAccount();
        }
    }


    /**
     * Returns new instance of "plugin's worker" - its methods are called from this class
     * Instance should not be cached. It should return always new instance.
     *
     * @return instance of PluginRunner
     */
    protected abstract PluginRunner getPluginRunnerInstance();

    /**
     * Load configuration data from string into Object.
     * Internal implementation uses XMLEncoder.
     *
     * @param content config content
     * @param type    class of the stored object
     * @return returns new instance, null if
     * @throws Exception throwed when reading went wrong
     */
    @SuppressWarnings("unchecked")
    public <E> E loadConfigFromString(String content, Class<E> type) throws Exception {
        XMLDecoder xmlDecoder = null;
        try {
            xmlDecoder = new XMLDecoder(new ByteArrayInputStream(content.getBytes()), null, null, type.getClassLoader());
            return (E) xmlDecoder.readObject();
        } catch (RuntimeException e) {
            LogUtils.processException(logger, e);
            throw new Exception(e);
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            throw e;
        } finally {
            if (xmlDecoder != null) {
                try {
                    xmlDecoder.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    /**
     * Store plugin's configuration data from Object into string.
     * Internal implementation uses XMLEncoder.
     *
     * @return config data as string
     * @throws Exception throwed when reading went wrong
     */
    public String storeConfigToString(Object object) throws Exception {
        XMLEncoder xmlEncoder = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            xmlEncoder = new XMLEncoder(baos);
            xmlEncoder.writeObject(object);
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            throw e;
        } finally {
            if (xmlEncoder != null) {
                try {
                    xmlEncoder.close();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        String result = new String(baos.toByteArray());
        try {
            baos.close();
        } catch (IOException e) {
            LogUtils.processException(logger, e);
        }
        return result;
    }

    /**
     * Clone config using XMLEncoder and XMLDecoder
     *
     * @param config config to be cloned
     * @param type   class of the stored object
     * @return config clone
     * @throws Exception if there's something wrong
     */
    public <E> E cloneConfig(E config, Class<E> type) throws Exception {
        String configAsString = storeConfigToString(config);
        return loadConfigFromString(configAsString, type);
    }

    /**
     * Get local config from file item, if it's none then clone the global config
     *
     * @param httpFile               file item
     * @param globalConfigToBeCloned global config to be cloned
     * @param type                   global config class type
     * @return local config
     * @throws Exception if there's something wrong
     */
    public <E> E getLocalConfig(HttpFile httpFile, E globalConfigToBeCloned, Class<E> type) throws Exception {
        E result;
        String configAsString = httpFile.getLocalPluginConfig();
        if (configAsString == null) {
            result = cloneConfig(globalConfigToBeCloned, type);
        } else {
            try {
                result = loadConfigFromString(configAsString, type);
            } catch (Exception e) {
                result = cloneConfig(globalConfigToBeCloned, type);
            }
        }
        return result;
    }
}
