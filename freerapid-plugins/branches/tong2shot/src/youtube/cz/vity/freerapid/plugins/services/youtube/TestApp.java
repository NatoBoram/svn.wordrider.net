package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Kajda
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=IJdOcdk_J1E"));//normal
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=DVFbA1AbEUw"));//rtmp
            //httpFile.setNewURL(new URL("http://www.youtube.com/user/HDstarcraft"));//user page
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=meCIER_s7Ng"));//transcript - subtitles
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=l3hTJOAG5lQ")); //age verification
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=giHIJgJS2sE")); //age & controversy verification
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=mdtwvmFlhz8")); //age verification
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?list=UUv3nmgyia2M2FU2TAIRXSLw&feature=plcp")); //user uploaded video
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?list=PLE963AD215F0C4BE5")); //playlist
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?list=FL2pmfLm7iq6Ov1UwYrWYkZA"));// favorite list
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=Phl57XmsPQ8"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=PM4kLnRr0ZI"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECA89DCFA6ADACE599")); //course list
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECB24BC7956EE040CD"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECD9DDFBDC338226CA"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/user/HDstarcraft/videos?view=0"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECBD4C7FD29B0C6D0C")); //course list
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=sQ8T9b-uGVE&amp"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=BvEwiyHsCLI&amp"));
            //http://www.youtube.com/watch?v=BvEwiyHsCLI&amp //corrupt sorrensen park
            //http://www.youtube.com/watch?v=_ALzsu2cTNA&amp //corrupt sorrensen park
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=RxDPvPqOmv0"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=ZiH6CDl5kII"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=ShVRP09NCO4"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=MwpMEbgC7DA")); //sig decipher
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=oRS5p60yX_E"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=zoKj7TdJk98")); //1080
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=zoKj7TdJk98&dashaudioitag=140"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?annotation_id=annotation_442536&feature=iv&list=PL4FCB61165E3892A7&src_vid=PgMUphc-80Y"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=5iobEYR487Q")); //DASH url sig decipher
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=FbcEs-o0d-g"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=dEA_8A9TBGM")); //4K
            //httpFile.setNewURL(new URL("http://www.youtube.com/attribution_link?a=G1xmCJAKUUM&u=/watch%3Fv%3DGQp_Xn_DbI8%26feature%3Dem-uploademail"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=E7WuQoF3fAQ")); //primary dash audio fails for the highest video quality
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=E7WuQoF3fAQ&dashaudioitag=141&secondarydashaudioitag=140"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=kTqgsKxv-0Q"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=gY5rztWa1TM"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=5laMCk6JQN0")); //60 fps
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=Yx9t-Q84l60"));  // 18+
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=TSodL9uBKqc")); //contains subtitle
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=TSodL9uBKqc#subtitles:en:&v=TSodL9uBKqc&name=Beginner+Ski+Lesson+2.2+-+Commitment+Exercise&lang=en"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=bEBIAfZ0iW4")); //subtitle silent download
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=e-GYrbecb88")); //2160
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=e-GYrbecb88&dashaudioitag=141&secondarydashaudioitag=140"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=pzVaesO6-8o")); //invalid duration for subtitle
            //httpFile.setNewURL(new URL("https://www.youtube.com/user/judofighter96"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/user/nickelbacktv"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/playlist?list=FLWYfHW4VtXNcycoPQcsBUCg"));
            //httpFile.setNewURL(new URL("https://www.youtube.com/watch?v=JMla5Ua6bHw")); //18+ embedding failed
            httpFile.setNewURL(new URL("https://www.youtube.com/channel/UC-AJ211gH31kz1A01y8ls9Q"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8118); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final YouTubeServiceImpl service = new YouTubeServiceImpl(); //instance of service - of our plugin

            YouTubeSettingsConfig config = new YouTubeSettingsConfig();
            config.setEnableDash(true);
            config.setVideoQuality(VideoQuality._1080);
            config.setContainer(Container.flv);
            config.setReversePlaylistOrder(false);
            config.setDownloadSubtitles(true);
            config.setDownloadMode(DownloadMode.downloadVideo);
            config.setConvertAudioQuality(AudioQuality._128);
            config.setFrameRate(FrameRate._60);
            service.setConfig(config);

            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}