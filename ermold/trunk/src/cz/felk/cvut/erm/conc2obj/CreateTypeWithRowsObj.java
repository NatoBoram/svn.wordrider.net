package cz.felk.cvut.erm.conc2obj;

import cz.felk.cvut.erm.datatype.ObjectDataType;
import cz.felk.cvut.erm.icontree.IconNode;
import cz.felk.cvut.erm.typeseditor.UserTypeStorage;

import java.util.Enumeration;
import java.util.Vector;

public class CreateTypeWithRowsObj extends CreateTypeObj {
    /**
     * Corresponding data type.
     */
    private ObjectDataType objectDataType = null;
    /**
     * name of the type
     */
    private String name = null;

    private Vector<RowObj> rows = null;

    /**
     * Constructor.
     *
     * @param aRelation corresponding relation
     */
    public CreateTypeWithRowsObj(ObjectDataType aType, String aName) {
        objectDataType = aType;
        name = aName;
        fillRows();
    }

    private void fillRows() {
        for (Enumeration<UserTypeStorage> elements = objectDataType.getItemVector().elements(); elements.hasMoreElements();) {
            UserTypeStorage column = elements.nextElement();
            addColumn(new ObjectTypeColumnObj(column));
        }
    }

    /**
     * Creates string representation of the command -
     * "drop TAB with constraints".
     *
     * @param countTabs intendation from the left
     * @return java.lang.String
     */
    public String createSubSQL(int countTabs) {
        String result = TabCreatorObj.getTabs(countTabs) + toString() + " (\n";
        for (Enumeration<RowObj> elements = getRows().elements(); elements.hasMoreElements();) {
            RowObj commandSQL = elements.nextElement();
            result += commandSQL.createSubSQL(countTabs + 1) + (elements.hasMoreElements() ? "," : "") + "\n";
        }
        return result + TabCreatorObj.getTabs(countTabs) + ")";
    }

    /**
     * Creates subtree from nodes.
     *
     * @return cz.omnicom.ermodeller.errorlog.icontree.IconNode
     */
    public IconNode createSubTree() {
        IconNode top = new IconNode(this, true, getIcon());
        for (Enumeration<RowObj> elements = getRows().elements(); elements.hasMoreElements();) {
            RowObj commandSQL = elements.nextElement();
            top.add(commandSQL.createSubTree());
        }
        return top;
    }

    /**
     * Returns rows of the command.
     *
     * @return java.util.Vector
     */
    protected Vector<RowObj> getRows() {
        if (rows == null)
            rows = new Vector<RowObj>();
        return rows;
    }

    /**
     * Returns whether command has some rows
     *
     * @return boolean
     */
    public boolean isEmpty() {
        return getRows().isEmpty();
    }

    public void addColumn(ObjectTypeColumnObj aColumn) {
        addRowSQL(aColumn);
    }

    /**
     * Adds row.
     *
     * @param aRow cz.omnicom.ermodeller.sql.RowSQL
     */
    protected void addRowSQL(RowObj aRow) {
        if (getRows().contains(aRow)) {
//		throw AlreadyContainsExceptionSQL();
        }
        getRows().addElement(aRow);
    }

    /**
     * Returns string representation of group.
     * "create type TYPE as ...".
     *
     * @return java.lang.String
     */
    public String toString() {
        return "Create type " + name + " as " + objectDataType.toString();
    }
}