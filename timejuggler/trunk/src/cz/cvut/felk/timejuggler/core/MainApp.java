package cz.cvut.felk.timejuggler.core;

import application.*;
import cz.cvut.felk.timejuggler.swing.Swinger;
import cz.cvut.felk.timejuggler.gui.MainPanelManager;

import javax.swing.*;
import java.util.EventObject;

/**
 * Hlavni trida aplikace
 * @author Vity
 */
public class MainApp extends SingleFrameApplication {

    private MainPanelManager mainPanel;

    @Override
    protected void startup(String[] args) {
        mainPanel = new MainPanelManager();
        //final ApplicationContext appContext = ApplicationContext.getInstance();
        //ResourceMap resourceMap = appContext.getResourceMap(getClass());
        final JFrame frame = getMainFrame();
        frame.setIconImage(Swinger.getIconImage("iconMainFrame").getImage());
        getMainFrame().setJMenuBar(mainPanel.getMenuManager().getMenuBar());
        frame.getContentPane().add(getMainPanelComponent());
        //resourceMap.injectComponents(frame);
        this.addExitListener(new MainAppExitListener());
        frame.pack();
        show(frame);

    }

    private JComponent getMainPanelComponent() {
        return mainPanel.getComponent();
    }


    /**
     * Vraci komponentu hlavniho panelu obsahujici dalsi komponenty
     * @return hlavni panel
     */
    public MainPanelManager getMainPanel() {
        return mainPanel;
    }

    /**
     * Hlavni spousteci metoda programu
     * @param args vstupni parametry pro program
     */
    public static void main(String[] args) {
        //zde prijde overovani vstupnich pridavnych parametru
        Swinger.initLaF(); //inicializace LaFu, musi to byt pred vznikem MainApp
        Application.launch(MainApp.class, args); //spusteni
    }

    /**
     * Exit listener.
     * Pri ukoncovani provede ulozeni uzivatelskych properties.
     */
    private static class MainAppExitListener implements Application.ExitListener {

        public boolean canExit(EventObject event) {
            return true;
        }

        public void willExit(EventObject event) {
            AppPrefs.store();
        }
    }


}
