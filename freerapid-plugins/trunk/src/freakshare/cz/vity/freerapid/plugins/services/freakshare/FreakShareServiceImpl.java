package cz.vity.freerapid.plugins.services.freakshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Thumb
 */
public class FreakShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "freakshare.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FreakShareFileRunner();
    }

}
