package cz.vity.freerapid.plugins.services.ifolder;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author JPEXS
 */
public class IFolderServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "ifolder plugin";
    }

    public int getMaxDownloadsFromOneIP() {
        //I don't know how exactly Ifolder limits downloads...
        //But multiple simultaneous downloads are allowed
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IFolderFileRunner();
    }

}
