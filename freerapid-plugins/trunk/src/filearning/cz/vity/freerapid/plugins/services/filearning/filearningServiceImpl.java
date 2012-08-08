package cz.vity.freerapid.plugins.services.filearning;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Tommy
 */
public class filearningServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filearning.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new filearningFileRunner();
    }

}