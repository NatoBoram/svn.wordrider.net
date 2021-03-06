package cz.vity.freerapid.plugins.services.katfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class KatFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "KatFile";
    }

    @Override
    public String getName() {
        return "katfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KatFileFileRunner();
    }

}