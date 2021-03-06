package cz.vity.freerapid.plugins.services.gcash;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class GCashServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "gca.sh";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GCashFileRunner();
    }

}