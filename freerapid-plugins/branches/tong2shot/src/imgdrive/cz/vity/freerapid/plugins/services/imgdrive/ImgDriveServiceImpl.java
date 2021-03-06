package cz.vity.freerapid.plugins.services.imgdrive;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ImgDriveServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "imgdrive.co";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImgDriveFileRunner();
    }

}