package cz.vity.freerapid.plugins.services.spankbang;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class SpankBangServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_SpankBang.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "spankbang.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpankBangFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "SpankBang settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    SettingsConfig getConfig() throws Exception {
        synchronized (SpankBangServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new SettingsConfig();
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class);
                }
            }
            return config;
        }
    }

    void setConfig(final SettingsConfig config) {
        synchronized (SpankBangServiceImpl.class) {
            this.config = config;
        }
    }

}