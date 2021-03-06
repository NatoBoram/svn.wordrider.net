package cz.vity.freerapid.plugins.dev.plugimpl;

import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.MaintainQueueSupport;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class DevQueueSupport implements MaintainQueueSupport {
    private final static Logger logger = Logger.getLogger(DevQueueSupport.class.getName());

    @Override
    public boolean addLinksToQueue(HttpFile parentFile, List<URI> uriList) {
        logger.info("Following files were added into the queue:");
        StringBuilder builder = new StringBuilder();
        for (URI uri : uriList) {
            builder.append(String.format("%s%n", uri));
        }
        logger.info(builder.toString());
        return false;
    }
}
