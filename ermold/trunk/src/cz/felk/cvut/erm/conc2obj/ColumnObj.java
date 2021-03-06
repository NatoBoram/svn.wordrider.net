package cz.felk.cvut.erm.conc2obj;

import cz.felk.cvut.erm.conc2rela.AtributeC2R;
import cz.felk.cvut.erm.icontree.IconNode;

import javax.swing.*;

/**
 * Column in the table - atribute.
 */
public class ColumnObj extends RowObj {
    /**
     * Atribute describing the column
     *
     * @see cz.felk.cvut.erm.conc2rela.AtributeC2R
     */
    private AtributeC2R atributeC2R = null;

    /**
     * Constructor.
     *
     * @param cz.omnicom.ermodeller.conc2rela.AtributeC2R
     *
     */
    public ColumnObj(AtributeC2R anAtribute) {
        atributeC2R = anAtribute;
    }

    /**
     * Creates string representation column (atribute)
     *
     * @param countTabs intendation from the left
     * @return java.lang.String
     * @see cz.felk.cvut.erm.conc2rela.AtributeC2R#toString
     */
    public String createSubSQL(int countTabs) {
        return TabCreatorObj.getTabs(countTabs) + toString();
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
     * Returns icon for representing the column in the SQL tree.
     *
     * @return javax.swing.Icon
     */
    public Icon getIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/newcolumn.gif"));
    }

    /**
     * Returns only name of the column, not definition.
     *
     * @return java.lang.String
     */
    public String getNameString() {
        return atributeC2R.getNameC2R().toString();
    }

    /**
     * Returns string representation of the atribute.
     *
     * @return java.lang.String
     * @see #createSubSQL
     */
    public String toString() {
        return atributeC2R.toString(1);
    }
}
