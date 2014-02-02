package cz.vity.freerapid.plugins.services.played;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PlayedServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Played";
    }

    @Override
    public String getName() {
        return "played.to";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PlayedFileRunner();
    }

}