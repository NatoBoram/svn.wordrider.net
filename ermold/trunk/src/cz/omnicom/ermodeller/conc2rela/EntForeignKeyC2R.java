package cz.omnicom.ermodeller.conc2rela;

import cz.omnicom.ermodeller.conc2rela.exception.AlreadyContainsExceptionC2R;
import cz.omnicom.ermodeller.conc2rela.exception.ListExceptionC2R;
import cz.omnicom.ermodeller.sql.ConstraintSQL;
import cz.omnicom.ermodeller.sql.ForeignKeySQL;
import cz.omnicom.ermodeller.sql.SQLConstraintProducer;

import java.util.Vector;

/**
 * Foreign key is created while creating full primary keys.
 *
 * @see cz.omnicom.ermodeller.conc2rela.SchemaC2R#createPrimaryKeysC2R
 */
public class EntForeignKeyC2R extends ElementOfRelationC2R implements SQLConstraintProducer {
    /**
     * Home atributes.
     */
    private Vector atributesC2R = new Vector();
    /**
     * Foreign primary key
     */
    private PrimaryKeyC2R foreignPrimaryKeyC2R = null;

    /**
     * EntForeignKeyC2R constructor.
     *
     * @param aSchemaC2R     cz.omnicom.ermodeller.conc2rela.SchemaC2R
     * @param aRelationC2R   cz.omnicom.ermodeller.conc2rela.RelationC2R
     * @param aPrimaryKeyC2R foreign primary key
     */
    public EntForeignKeyC2R(SchemaC2R aSchemaC2R, RelationC2R aRelationC2R, PrimaryKeyC2R aPrimaryKeyC2R) {
        super(new NameC2R(), aSchemaC2R, aRelationC2R);
        this.foreignPrimaryKeyC2R = aPrimaryKeyC2R;
    }

    /**
     * Adds atribute which has corresponding atribute in foreign primary key.
     *
     * @param anAtributeC2R cz.omnicom.ermodeller.conc2rela.AtributeC2R
     * @throws cz.omnicom.ermodeller.conc2rela.AlreadyContainsExceptionC2R
     *
     */
    void addForeignAtributeC2R(AtributeC2R anAtributeC2R) throws AlreadyContainsExceptionC2R {
        if (getAtributesC2R().contains(anAtributeC2R))
            throw new AlreadyContainsExceptionC2R(this, anAtributeC2R, ListExceptionC2R.ATRIBUTES_LIST);

        getAtributesC2R().addElement(anAtributeC2R);
    }

    /**
     * Creates SQL foreign key constraint.
     *
     * @return ConstraintSQL
     */
    public ConstraintSQL createConstraintSQL() {
        ForeignKeySQL foreignKey = new ForeignKeySQL(getAtributesC2R(), getForeignPrimaryKeyC2R().getUniqueKeyGroupC2R(), getNameC2R());
        return foreignKey;
    }

    /**
     * @return java.util.Vector
     */
    public Vector getAtributesC2R() {
        if (atributesC2R == null)
            atributesC2R = new Vector();
        return atributesC2R;
    }

    /**
     * @return cz.omnicom.ermodeller.conc2rela.PrimaryKeyC2R
     */
    PrimaryKeyC2R getForeignPrimaryKeyC2R() {
        return foreignPrimaryKeyC2R;
    }
}
