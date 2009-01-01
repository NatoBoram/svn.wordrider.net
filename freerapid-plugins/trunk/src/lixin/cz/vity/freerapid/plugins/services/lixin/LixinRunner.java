package cz.vity.freerapid.plugins.services.lixin;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
//import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

//import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.net.URL;

/**
 * @author Alex
 */
class LixinRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LixinRunner.class.getName());
    public boolean Result;
    public boolean Fin;
    public String capt;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        logger.info(fileURL);
        if (makeRequest(method)) {
            String content = getContentAsString();
            while (!Fin) {
                logger.info("Making Request");
                makeRequest(method);
                content = getContentAsString();
                Fin = stepCaptcha(content);
            }
        }   else throw new PluginImplementationException("Can't load main page");

    }
    private String getCaptcha(String mContent) throws Exception {
        Matcher matcher = PlugUtils.matcher("<img src=\"(captcha_img[^\"]+)", mContent);
            if (matcher.find()) {
                String cImage = "http://lix.in/" + matcher.group(1);
                logger.info("Captcha image = "+ cImage);
                String cVal= getCaptchaSupport().getCaptcha(cImage);
                if (cVal==null) {
                    throw new PluginImplementationException("Cancelled");
                } else  Result = !(cVal=="");
                logger.info("Result : "+ Result);

                if (cVal=="") {
                    Result=false;
                    return "";
                }
                return cVal;
            } else  {
                Result=false;
                return "" ;
            }
    }

    private boolean stepCaptcha(String content) throws Exception {
             if (content.contains("captcha_img")) {
                 while (!Result)   {
                     capt = getCaptcha(content);
                     logger.info("Captcha inserted " + Result + " " + capt);
                }
             }



            Matcher matcher = PlugUtils.matcher("form action=\'(http://[^\']+)", content);
            if (matcher.find()) {
                client.setReferer(fileURL);

                final PostMethod pMethod = getPostMethod(matcher.group(1));
                String tiny = getValue("tiny",content);
                String submit = getValue("submit", content);
                pMethod.addParameter("tiny",tiny);
                pMethod.addParameter("submit", submit);
                if (content.contains("captcha_img")) {pMethod.addParameter("capt", capt);}
                logger.info("Posting : "+ matcher.group(1));
                if (makeRequest(pMethod)) {
                    content = getContentAsString();
                    //logger.info(content);
                    matcher = PlugUtils.matcher("<iframe.+?name=\"ifram\" src=\"([^\"]+)\"", content);
                    if (matcher.find()) {
                        logger.info("New URL :" + matcher.group(1));
                        this.httpFile.setNewURL(new URL(matcher.group(1)));
                        this.httpFile.setPluginID("");
                        this.httpFile.setState(DownloadState.QUEUED);
                        //Fin = true;
                        return true;
                    }  else {
                        //Fin=false;
                        Result=false;
                        return false;
                    }
                }  else throw new PluginImplementationException("Can't load link page");
            }   else throw new PluginImplementationException("Can't find post action");
            //final PostMethod method = getPostMethod(s);


    }




    private String getValue(String mParam, String mContent) throws Exception {
         Matcher matcher = PlugUtils.matcher("<input.+?name=('|\")?("+mParam+")('|\")?.+?value=('|\")?([a-z0-9\\-]+)('|\")+", mContent);
         if (matcher.find()) {
             logger.info("value "+ mParam + " = " + matcher.group(5));

             return matcher.group(5);
         }
        //return "Error";
        throw new PluginImplementationException("Can't find value");
    }
    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }

}
