package cz.felk.cvut.erm.conc2rela;

import cz.felk.cvut.erm.conc2rela.exception.AlreadyContainsExceptionC2R;
import cz.felk.cvut.erm.conc2rela.exception.ListExceptionC2R;
import cz.felk.cvut.erm.conceptual.beans.Atribute;
import cz.felk.cvut.erm.sql.ConstraintSQL;
import cz.felk.cvut.erm.sql.UniqueKeySQL;
import cz.felk.cvut.erm.sql.interfaces.SQLConstraintProducer;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Relational unique key group of atributes.
 */
public class UniqueKeyC2R extends ElementOfRelationC2R implements SQLConstraintProducer {
    /**
     * Atributes in unique group.
     *
     * @see cz.felk.cvut.erm.conc2rela.AtributeC2R
     */
    private final Vector<AtributeC2R> atributesC2R = new Vector<AtributeC2R>();
    /**
     * Corresponding conceptual unique key.
     *
     * @see cz.felk.cvut.erm.conceptual.beans.UniqueKey
     */
    private Vector<Atribute> conceptualUniqueKey = null;

    /**
     * UniqueKey constructor.
     *
     * @param aSchemaC2R           relational schema owning this object
     * @param aRelationC2R         owner of the unique key
     * @param aConceptualUniqueKey corresponding conceptual unique key
     * @throws cz.felk.cvut.erm.conc2rela.exception.WasNotFoundByConceptualExceptionC2R
     *
     * @throws cz.felk.cvut.erm.conc2rela.exception.AlreadyContainsExceptionC2R
     *
     */
    public UniqueKeyC2R(SchemaC2R aSchemaC2R, RelationC2R aRelationC2R, Vector<Atribute> primaryKeys, Vector<Atribute> uniqueKeys, boolean isPrimary) throws AlreadyContainsExceptionC2R {
        super(null, aSchemaC2R, aRelationC2R);

        if (primaryKeys != null) {
            this.conceptualUniqueKey = primaryKeys;
            /*for (Atribute a:primaryKeys){
                AtributeC2R atribute=aRelationC2R.findAtributeC2RByConceptualAtribute(a);
                addAtributeC2R(atribute);
            }*/
            if (isPrimary) {
                for (Atribute a : primaryKeys) {
                    AtributeC2R atribute = aRelationC2R.findAtributeC2RByConceptualAtribute(a);
                    addAtributeC2R(atribute);
                }
            } else {
                for (Atribute a : uniqueKeys) {
                    if (a.isUnique() && !a.isPrimary()) {
                        AtributeC2R atribute = aRelationC2R.findAtributeC2RByConceptualAtribute(a);
                        addAtributeC2R(atribute);
                    }
                }
            }
        }
        /*atributesC2R=primaryKeys;
          for (Enumeration elements = aConceptualUniqueKey.getAtributes().elements(); elements.hasMoreElements();) {
              Atribute atribute = (Atribute) elements.nextElement();
              AtributeC2R atributeC2R = aRelationC2R.findAtributeC2RByConceptualAtribute(atribute);
              if (atributeC2R == null) {
                  throw new WasNotFoundByConceptualExceptionC2R(aRelationC2R, atribute, ListByConceptualExceptionC2R.ATRIBUTES_LIST);
              }
              addAtributeC2R(atributeC2R); // can throw exception
          */

    }

    /**
     * Adds an atribute.
     *
     * @param anAtribute cz.omnicom.ermodeller.conc2rela.AtributeC2R
     * @throws cz.felk.cvut.erm.conc2rela.exception.AlreadyContainsExceptionC2R
     *
     */
    public void addAtributeC2R(AtributeC2R anAtributeC2R) throws AlreadyContainsExceptionC2R {
        if (getAtributesC2R().contains(anAtributeC2R))
            throw new AlreadyContainsExceptionC2R(this, anAtributeC2R, ListExceptionC2R.ATRIBUTES_LIST);
        else
            getAtributesC2R().addElement(anAtributeC2R);
    }

    /**
     * Creates unique key SQL constraint.
     *
     * @return cz.omnicom.ermodeller.sql.ConstraintSQL
     */
    public ConstraintSQL createConstraintSQL() {
        return new UniqueKeySQL(this);
    }

    /**
     * @return java.util.Vector
     */
    public Vector<AtributeC2R> getAtributesC2R() {
        return atributesC2R;
    }

    /**
     * @return cz.omnicom.ermodeller.conceptual.UniqueKey
     */
    protected Vector<Atribute> getConceptualUniqueKey() {
        return conceptualUniqueKey;
    }

    /**
     * Returns string representation of group.
     * "(atr, atr, ...)"
     *
     * @return java.lang.String
     */
    public String getGroupString() {
        String result = "(";
        if (getAtributesC2R().isEmpty())
            return "";
        for (Enumeration<AtributeC2R> atributesC2R = getAtributesC2R().elements(); atributesC2R.hasMoreElements();) {
            AtributeC2R atributeC2R = atributesC2R.nextElement();
            result += atributeC2R.getNameC2R();
            if (atributesC2R.hasMoreElements())
                result += ", ";
        }
        result += ")";
        return result;
    }

    /**
     * This method was created in VisualAge.
     *
     * @return boolean
     */
    public boolean isPrimaryKey() {
        return false;
    }

    /**
     * Returns whether unique key is primary or not.
     *
     * @return boolean
     */
    public boolean isPrimaryKeyC2R() {
        return getRelationC2R() != null && getRelationC2R().isPrimaryKeyC2R(this);
    }

    /**
     * Returns string representation of the unique key.
     * "unique key (group)"
     *
     * @return java.lang.String
     * @see #getGroupString
     */
    public String toString() {
        return "Unique " + getGroupString();
    }
}
