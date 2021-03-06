package cz.vity.freerapid.plugins.services.filenext;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileNextServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileNext";
    }

    @Override
    public String getName() {
        return "filenext.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileNextFileRunner();
    }

}