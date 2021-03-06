package cz.vity.freerapid.plugins.services.admy;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AdMyServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "admy.link";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AdMyFileRunner();
    }

}