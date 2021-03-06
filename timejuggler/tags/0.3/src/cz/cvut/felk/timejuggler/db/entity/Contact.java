package cz.cvut.felk.timejuggler.db.entity;

import cz.cvut.felk.timejuggler.db.DatabaseException;
import cz.cvut.felk.timejuggler.db.TimeJugglerJDBCTemplate;
import cz.cvut.felk.timejuggler.db.entity.interfaces.PropertyEntity;

import java.util.logging.Logger;

/**
 * @author Jan Struz
 * @version 0.1
 * @created 12-V-2007 23:41:01 Hotovo
 */
public class Contact extends DbElement implements PropertyEntity {
    private final static Logger logger = Logger.getLogger(Contact.class.getName());
    private String contact = "";

    private int componentId;

    public Contact() {

    }

    public Contact(String contact) {
        this.contact = contact;
    }

    public void store() {

    }

    /**
     * Method saveOrUpdate
     * @param template
     */

    public void saveOrUpdate(TimeJugglerJDBCTemplate template) {
        if (getId() > 0) {
            Object params[] = {contact, componentId, getId()};
            String insertQuery = "UPDATE Contact SET contact=?,calComponentID=? WHERE contactID = ?";
        } else {
            Object params[] = {contact, componentId};
            String insertQuery = "INSERT INTO Contact (contact,calComponentID) VALUES (?,?)";
            try {
                template.executeUpdate(insertQuery, params);
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            setId(template.getGeneratedId());
        }
    }

    /**
     * Method delete
     * @param template
     */
    public void delete(TimeJugglerJDBCTemplate template) {
        if (getId() > 0) {
            Object params[] = {getId()};
            String deleteQuery = "DELETE FROM Contact WHERE contactID = ?";
            try {
                template.executeUpdate(deleteQuery, params);
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            setId(-1);
        }
    }


    public String getContact() {
        return contact;
    }

    /**
     * @param newVal
     */
    public void setContact(String newVal) {
        contact = newVal;
    }


    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public int getComponentId() {
        return (this.componentId);
    }

    /**
     * Method getValue
     * @return
     */
    public String getValue() {
        return contact;
    }

    /**
     * Method setValue
     * @param newVal
     */
    public void setValue(String newVal) {
		contact = newVal;
	}

}