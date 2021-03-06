package cz.cvut.felk.erm.db;

/**
 * @author Ladislav Vitasek
 */
public class DatabaseException extends Exception {

    private Object params[] = new Object[0];
    private String sql = "";

    public DatabaseException(Throwable cause) {
        super(cause);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message, Throwable cause, String sql, Object params[]) {
        super(message, cause);
        this.sql = sql;
        this.params = params;
    }

    public Object[] getparams() {
        return params;
    }

    /**
     * @param newVal
     */
    public void setParams(Object[] newVal) {
        params = newVal;
    }

    public String getSql() {
        return sql;
    }

    /**
     * @param newVal
     */
    public void setSql(String newVal) {
        sql = newVal;
    }

}