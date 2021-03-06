package cz.vity.freerapid.plugins.services.fileshd;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FilesHDServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FilesHD";
    }

    @Override
    public String getName() {
        return "fileshd.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilesHDFileRunner();
    }

}