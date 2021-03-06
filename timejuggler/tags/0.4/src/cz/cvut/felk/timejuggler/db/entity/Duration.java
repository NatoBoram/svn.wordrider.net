package cz.cvut.felk.timejuggler.db.entity;

import cz.cvut.felk.timejuggler.db.DatabaseException;
import cz.cvut.felk.timejuggler.db.TimeJugglerJDBCTemplate;
import cz.cvut.felk.timejuggler.db.entity.interfaces.DurationEntity;

import java.util.logging.Logger;

/**
 * @author Jan Struz
 * @version 0.1
 * @created 27-IV-2007 22:45:50
 * <p/>
 * Trida reprezentujici delku trvani udalosti, pokud nema nastaven presny datum konce platnosti
 * ma interface
 */
public class Duration extends DbElement implements DurationEntity {
    private final static Logger logger = Logger.getLogger(Duration.class.getName());

    private boolean negative = false;
    private int days;
    private int weeks;
    private int hours;
    private int minutes;
    private int seconds;

    public Duration() {

    }

    public void store() {


    }

    /**
     * Method saveOrUpdate
     * @param template Ulozeni do databaze, nebo update
     */
    @Override
    public void saveOrUpdate(TimeJugglerJDBCTemplate template) throws DatabaseException {
        if (getId() > 0) {
            Object params[] = {(negative ? 1 : 0), days, weeks, hours, minutes, seconds, getId()};
            String updateQuery = "UPDATE Duration SET negative=?,days=?,weeks=?,hours=?,minutes=?,seconds=? WHERE durationID = ? ";

            template.executeUpdate(updateQuery, params);
        } else {
            Object params[] = {(negative ? 1 : 0), days, weeks, hours, minutes, seconds};
            String insertQuery = "INSERT INTO Duration (negative,days,weeks,hours,minutes,seconds) VALUES (?,?,?,?,?,?) ";
            template.executeUpdate(insertQuery, params);
            setId(template.getGeneratedId());
            logger.info("Duration: generated ID:" + getId());
        }

    }

    /**
     * Method delete
     * @param template
     */
    public void delete(TimeJugglerJDBCTemplate template) throws DatabaseException {
        if (getId() > 0) {
            String deleteQuery = "DELETE FROM Duration WHERE durationID = ? ";
            Object params[] = {getId()};
            template.executeUpdate(deleteQuery, params);
            setId(-1);
        }
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public void setWeeks(int weeks) {
        this.weeks = weeks;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public boolean isNegative() {
        return (this.negative);
    }

    public int getDays() {
        return (this.days);
    }

    public int getWeeks() {
        return (this.weeks);
    }

    public int getHours() {
        return (this.hours);
    }

    public int getMinutes() {
        return (this.minutes);
    }

    public int getSeconds() {
        return (this.seconds);
    }

}