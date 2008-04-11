package cz.green.ermodeller;

import java.awt.*;

/**
 * Default Constants for application
 *
 * @author Ladislav Vitasek
 */
public class Consts {
    /**
     * Application version
     */
    public static final String APPVERSION = "4.23";
    /**
     * Application name + version
     */
    public static final String APPVERSION_FULL = "ER Modeller " + APPVERSION;
    /**
     * Default encoding for xml schemas
     */
    public static final String DEF_ENCODING = "ISO-8859-1";
    /**
     * default JDBC connect driver
     */
    public static final String DEF_DBCONNECT_DRIVER = "oracle.jdbc.driver.OracleDriver";
    /**
     * default JDBC connection url
     */
    public static final String DEF_DBCONNECT_URL = "jdbc:oracle:thin:@cs:1526:oracle";
    /**
     * default JDBC connection user name
     */
    public static final String DEF_DBCONNECT_USER = "username";
    /**
     * default notation to use
     */
    public static final int DEF_GENERAL_DEFNOTATION = ConceptualConstruct.CHEN;
    /**
     * default value for pkshowuml
     */
    public static final int DEF_GENERAL_PKSHOWUML = ConceptualConstruct.SHOW_PK_IN_UML;
    /**
     * default value for 'shorten cards uml'
     */
    public static final int DEF_GENERAL_SHORTEN_CARDS_UML = ConceptualConstruct.SHOW_SHORTEN_CARD_IN_UML;
    /**
     * default color value for foreground of the object
     */
    public static final Color DEF_COLORS_OBJECT_FG = cz.green.event.Window.OBJECT_FOREGROUND_COLOR;
    /**
     * default color value for background of the object
     */
    public static final Color DEF_COLORS_OBJECT_BG = cz.green.event.Window.OBJECT_BACKGROUND_COLOR;
    /**
     * default color value for background of the selected object
     */
    public static final Color DEF_COLORS_SELOBJECT_BG = cz.green.event.Window.SELECTED_OBJECT_BACKGROUND_COLOR;
    /**
     * default color value for background of canvas
     */
    public static final Color DEF_COLORS_BG = cz.green.event.Window.BACKGROUND_COLOR;
    /**
     * default directory for load&store
     */
    public static String DEF_LOAD_STORE_DIR = System.getProperty("user.home", "");
    /**
     * default path to help documentation
     */
    public static String DEF_HELPPATH = "help/obecne.htm";
    /**
     * Basic stroke for lines
     */
    public static final float STROKE_WIDTH = 1.3F;

    /**
     * private constructor
     */
    private Consts() {
    }
}
