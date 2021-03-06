package cz.felk.cvut.erm.typeseditor;

import cz.felk.cvut.erm.datatype.DataType;
import cz.felk.cvut.erm.datatype.ObjectDataType;

public class UserTypeStorage {

    protected String typeName = null;
    protected DataType dataType = null;
    protected UserTypesEditorPanel panel = null;

    public UserTypeStorage(String name, DataType type, UserTypesEditorPanel aPanel) {
        typeName = name;
        dataType = type;
        panel = aPanel;
    }

    public String getTypeName() {
        return typeName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public UserTypesEditorPanel getPanel() {
        return panel;
    }

    public void setTypeName(String name) {
        typeName = name;
    }

    public void setDataType(DataType type) {
        dataType = type;
    }

    public void setPanel(UserTypesEditorPanel aPanel) {
        panel = aPanel;
    }

    public void write(java.io.PrintWriter pw) {
        pw.println("\t<usertype>");
        pw.print("\t\t<typename>");
        pw.print(getTypeName());
        pw.println("</typename>");
        pw.println("\t\t<datatypedef>");
        pw.print("\t\t\t<datatype>");
        pw.print(getDataType().toString());
        pw.println("</datatype>");
        if (getDataType() instanceof ObjectDataType) {
            UserTypeStorageVector itemVector = ((ObjectDataType) getDataType()).getItemVector();
            for (UserTypeStorage u : itemVector.getUserTypeStorageVector()) {
                pw.println("\t\t\t<item>");
                pw.print("\t\t\t\t<itemname>");
                pw.print(u.getTypeName());
                pw.println("</itemname>");
                pw.print("\t\t\t\t<datatype>");
                pw.print(u.getDataType());
                pw.println("</datatype>");
                pw.println("\t\t\t</item>");
            }
        }
        pw.println("\t\t</datatypedef>");
        pw.println("\t</usertype>");
    }
}