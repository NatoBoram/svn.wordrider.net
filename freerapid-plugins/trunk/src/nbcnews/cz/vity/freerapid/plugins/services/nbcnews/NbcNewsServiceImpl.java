package cz.vity.freerapid.plugins.services.nbcnews;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class NbcNewsServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_NbcNewsSettings.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "nbcnews.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NbcNewsFileRunner();
    }


    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "NBC News settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public SettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new SettingsConfig();
            } else {
                try {
                    config = storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class);
                } catch (Exception e) {
                    config = new SettingsConfig();
                }
            }
        }
        return config;
    }

    public void setConfig(final SettingsConfig config) {
        synchronized (NbcNewsServiceImpl.class) {
            this.config = config;
        }
    }
}