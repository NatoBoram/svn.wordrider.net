package cz.felk.cvut.erm.conc2obj;

import javax.swing.*;

/**
 * Groups of columns, which should be forced to be NOT NULL.
 *
 * @see cz.felk.cvut.erm.sql.ColumnSQL
 */
class IsNotNullAtributeGroupVectorObj extends AtributeGroupVectorObj {
    /**
     * Returns icon for representing the group in the SQL tree.
     *
     * @return javax.swing.Icon
     */
    public Icon getIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/andcheck.gif"));
    }

    /**
     * Returns <code>true</code>.
     * Returns whether check is provided like:
     * "atr IS NOT NULL".
     */
    public boolean getIsNotNull() {
        return true;
    }

    /**
     * Returns string representation of group.
     * "Is Not Null group".
     *
     * @return java.lang.String
     */
    public String toString() {
        return "Is Not Null group";
    }
}
