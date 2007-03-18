package cz.cvut.felk.timejuggler.gui;

import application.ApplicationActionMap;
import application.ApplicationContext;
import cz.cvut.felk.timejuggler.core.AppPrefs;
import cz.cvut.felk.timejuggler.gui.actions.*;

import javax.swing.*;


/**
 * Sprava a vytvoreni hlavniho menu
 * @author Vity
 */
public class MenuManager {
    private JMenuBar menuBar;

    public MenuManager() {
        super();
        initActions(new FileActions());
        initActions(new EditActions());
        initActions(new ViewActions());
        initActions(new GoActions());
        initActions(new ViewActions());
        initActions(new HelpActions());
    }

    private void init() {

        final Object[] fileMenuActionNames = {
                "newEvent",
                "newTask",
                "newCalendar",
                "openCalendarFile",
                "---",
                "importCalendar",
                "exportSelection",
                "exportCalendar",
                "---",
                "pageSetup",
                "print",
                "---",
                "quit"
        };
        final Object[] editMenuActionNames = {
                "cut",
                "copy",
                "paste",
                "editSelection",
                "---",
                "options",
        };
        final Object[] goMenuActionNames = {
                "goToday",
                "goToDate",
                "---",
                "previousDate",
                "nextDate",
        };
        final Object[] helpMenuActionNames = {
                "help",
                "---",
                "about"
        };


        menuBar.add(createMenu("fileMenu", fileMenuActionNames));
        menuBar.add(createMenu("editMenu", editMenuActionNames));
        menuBar.add(createViewMenu());
        menuBar.add(createMenu("goMenu", goMenuActionNames));
        menuBar.add(createMenu("helpMenu", helpMenuActionNames));
    }

    private void initActions(Object actionsObject) {
        final ApplicationActionMap globalMap = ApplicationContext.getInstance().getActionMap();
        final ApplicationActionMap actionMap = ApplicationContext.getInstance().getActionMap(actionsObject);
        for (Object key : actionMap.keys()) {
            globalMap.put(key, actionMap.get(key));
        }
    }

    private JMenu createViewMenu() {
        final JMenu jMenu = new JMenu();
        jMenu.setName("viewMenu");
        jMenu.add(new JCheckBoxMenuItem(getAction("showToolbar")));
        jMenu.add(new JCheckBoxMenuItem(getAction("showSearchBar")));
        jMenu.add(new JCheckBoxMenuItem(getAction("showStatusBar")));
        jMenu.addSeparator();
        final ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(getAction("dayView"));
        buttonGroup.add(item);
        jMenu.add(item);
        item = new JRadioButtonMenuItem(getAction("weekView"));
        buttonGroup.add(item);
        jMenu.add(item);
        item = new JRadioButtonMenuItem(getAction("multiWeekView"));
        buttonGroup.add(item);
        jMenu.add(item);
        item = new JRadioButtonMenuItem(getAction("monthView"));
        buttonGroup.add(item);
        jMenu.add(item);
        setDefaultCalendarView(); // prozatim tady

        return jMenu;
    }

    public JMenuBar getMenuBar() {
        if (menuBar == null) {
            this.menuBar = new JMenuBar();
            //final ApplicationActionMap map = new ApplicationActionMap();
//            map.setParent(ApplicationContext.getInstance().getActionMap());
//            map.
            init();
        }
        return menuBar;
    }

    private JMenu createMenu(String menuName, Object[] actionNames) {
        JMenu menu = new JMenu();
        menu.setName(menuName);
        for (Object actionName : actionNames) {
            if (actionName.equals("---")) {
                menu.addSeparator();
            } else {
                JMenuItem menuItem = new JMenuItem();
                menuItem.setAction(getAction(actionName));
                menu.add(menuItem);
            }
        }
        return menu;
    }

    public static javax.swing.Action getAction(Object actionName) {
        final ApplicationContext ac = ApplicationContext.getInstance();
        final Action action = ac.getActionMap().get(actionName);
        assert action != null;
        return action;
    }

    private void setDefaultCalendarView() {
        final int selectedView = AppPrefs.getProperty(AppPrefs.CALENDAR_VIEW, ViewActions.DAY_VIEW);
        getAction("dayView").putValue(Action.SELECTED_KEY, selectedView == ViewActions.DAY_VIEW);
        getAction("weekView").putValue(Action.SELECTED_KEY, selectedView == ViewActions.WEEK_VIEW);
        getAction("multiWeekView").putValue(Action.SELECTED_KEY, selectedView == ViewActions.MULTIWEEK_VIEW);
        getAction("monthView").putValue(Action.SELECTED_KEY, selectedView == ViewActions.MONTH_VIEW);
    }
}
