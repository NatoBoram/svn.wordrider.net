package cz.omnicom.ermodeller.sql;


/**
 * Row is element of command. Commands contain only rows.
 *
 * @see cz.omnicom.ermodeller.sql.CommandSQL
 */
public abstract class RowSQL implements SubSQLProducer, SubTreeProducer {
    /**
     * @return javax.swing.Icon
     */
    public abstract javax.swing.Icon getIcon();
}
