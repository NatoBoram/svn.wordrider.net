package cz.felk.cvut.erm.sql;

import cz.felk.cvut.erm.icontree.IconNode;

import javax.swing.*;

/**
 * Commands representing drop table command.
 */
public class Intro extends CommandSQL {

/**
 * Constructor.
 *
 * @param aRelation corresponding relation
 */
    /**
     * Creates string representation of the command -
     * "drop TAB with constraints".
     *
     * @param countTabs intendation from the left
     * @return java.lang.String
     */
    public String createSubSQL(int countTabs) {
        return TabCreator.getTabs(countTabs) + toString();
    }

    /**
     * Creates subtree from nodes.
     *
     * @return cz.omnicom.ermodeller.errorlog.icontree.IconNode
     */
    public IconNode createSubTree() {
        return new IconNode(this, false, getIcon());
    }

    /**
     * Returns icon for representing the drop command in the SQL tree.
     *
     * @return javax.swing.Icon
     */
    public Icon getIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/intro.gif"));
    }

    /**
     * Returns string representation of group.
     * "drop table TAB cascade constraints".
     *
     * @return java.lang.String
     */
    public String toString() {
        return "Set Echo on\nPurge Recyclebin;\n/* pou��vate-li verzi ni��� ne� Oracle 10g,\n zakomentujte p��kaz PURGE */\n";
    }
}
