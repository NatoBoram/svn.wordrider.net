package cz.vity.freerapid.plugins.services.estream;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class EStreamServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "estream.to";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EStreamFileRunner();
    }

}