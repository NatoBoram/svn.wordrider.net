package cz.vity.freerapid.plugins.services.watchvideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class WatchVideoServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "WatchVideo";
    }

    @Override
    public String getName() {
        return "watchvideo.us";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WatchVideoFileRunner();
    }

}