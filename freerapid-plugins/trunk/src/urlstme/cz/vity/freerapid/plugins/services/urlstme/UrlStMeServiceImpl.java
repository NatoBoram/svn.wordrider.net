package cz.vity.freerapid.plugins.services.urlstme;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UrlStMeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "urlst.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UrlStMeFileRunner();
    }

}