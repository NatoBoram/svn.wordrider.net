package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author JPEXS
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1143638030-ct-live/20754215404-ct-live-vlasta-redl/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1095875447-cestomanie/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10588743864-denik-dity-p/213562260300003-piknik/video/281044"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/specialy/hydepark-civilizace/14.9.2013/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10084897100-kluci-v-akci/211562221900012/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10084897100-kluci-v-akci/211562221900012/obsah/155251-pastiera-napoletana/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1126672097-otazky-vaclava-moravce/213411030510609-otazky-vaclava-moravce-2-cast/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1126672097-otazky-vaclava-moravce/213411030510609-otazky-vaclava-moravce-2-cast/obsah/265416-pokracovani-debaty-z-1-hodiny-poradu/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1104873554-nadmerne-malickosti/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1183909575-tyden-v-regionech-ostrava/413231100212014-tyden-v-regionech/obsah/252368-majiteli-reznictvi-v-centru-ostravy-hrozi-az-milionova-pokuta/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10306517828-mala-farma/313292320310028/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10361564316-sanitka-2/210512120330009/"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/mazalove"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/player?width=560&IDEC=211+513+13003%2F0010&fname=Mazalov%C3%A9+-+Je%C5%BE%C3%AD%C5%A1ek%3F+Existuje%21"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/bludiste"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/player?width=560&IDEC=409+234+10001%2F1007&fname=Bludi%C5%A1t%C4%9B+-+19.+2.+2009"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10213448544-evropska-liga-ve-fotbalu/214471291124205-fc-viktoria-plzen-olympique-lyon")); //multiparts
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10213448544-evropska-liga-ve-fotbalu/214471291124205-fc-viktoria-plzen-olympique-lyon/?switchitemid=2-214+471+29112%2F4205"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10213448544-evropska-liga-ve-fotbalu/214471291124205-fc-viktoria-plzen-olympique-lyon/?switchitemid=2-214+471+29112%2F4205&fname=Evropsk%C3%A1+liga+ve+fotbalu+-+FC+Viktoria+Plze%C5%88+-+Olympique+Lyon-3"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10639901181-trabantem-jizni-amerikou/213562260150012/bonus/16881"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/zpravodajstvi-brno/zpravy/237520-vosy-se-premnozily-utoku-jejich-zihadel-pribyva/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/878516-willy-fog-na-ceste-za-dobrodruzstvim/298381420460002-cesta-na-island/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10090925908-vsechnoparty/215522161600033"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/video/316%20294%2034001_0001"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/player?width=560&IDEC=316+294+34001%2F0001&fname=Pir%C3%A1tsk%C3%A9+vys%C3%ADl%C3%A1n%C3%AD+-+11.+4.+2016"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10702670652-rozsudek/414235100181005-znasilneni-manzelky/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/rio2016/videoarchiv/zaznamy/337302-zaznam-utkani-petra-kvitova-caroline-wozniacka/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/sport/zimni-sporty/rychlobrusleni/348672-sablikova-je-podevate-mistryni-sveta-na-petce/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz:8080/ivysilani/11042149014-spravedlnost/415233100011003"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10266819072-vypravej/video/bonusy/12013-album-serialu-vypravej/")); //bonus
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10266819072-vypravej/ve-stopach-doby/2005/672-zemrel-papez-jan-pavel-ii/")); //bonus in non-bonus URL
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10205517508-doktor-martin/218381482750001-svatebni-noc/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10195164142-vypravej/208522161400006-embecko/"));

            // 2021 test URLs:
            httpFile.setNewURL(new URL("https://www.ceskatelevize.cz/porady/14021364946-bilance/221452801250007/"));
            //httpFile.setNewURL(new URL("https://www.ceskatelevize.cz/porady/11663933151-jack-london-americky-dobrodruh/"));
            //httpFile.setNewURL(new URL("https://www.ceskatelevize.cz/porady/1185966822-na-ceste/211562260130001-na-ceste-po-pardubicku/"));

            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("127.0.0.1", 9150, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            final CeskaTelevizeServiceImpl service = new CeskaTelevizeServiceImpl();
            CeskaTelevizeSettingsConfig config = new CeskaTelevizeSettingsConfig();
            config.setVideoQuality(VideoQuality.Highest);
            service.setConfig(config);
            //setUseTempFiles(true);
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}