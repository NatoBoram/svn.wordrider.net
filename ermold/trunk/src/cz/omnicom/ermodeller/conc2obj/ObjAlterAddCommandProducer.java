package cz.omnicom.ermodeller.conc2obj;

/**
 * Object, which creates alter table add command
 * should implement this interface.
 *
 * @see cz.omnicom.ermodeller.sql.AlterAddCommandSQL
 */
public interface ObjAlterAddCommandProducer {
    /**
     * @return cz.omnicom.ermodeller.sql.AlterAddCommandSQL
     */
    public AlterAddCommandObj createAlterAddCommandSQL();
}
