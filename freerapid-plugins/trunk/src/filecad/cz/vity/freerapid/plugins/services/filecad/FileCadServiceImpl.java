package cz.vity.freerapid.plugins.services.filecad;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileCadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "filecad.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileCadFileRunner();
    }

}