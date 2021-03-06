package cz.vity.freerapid.plugins.services.mexashare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MexaShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MexaShare";
    }

    @Override
    public String getName() {
        return "mexashare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MexaShareFileRunner();
    }

}