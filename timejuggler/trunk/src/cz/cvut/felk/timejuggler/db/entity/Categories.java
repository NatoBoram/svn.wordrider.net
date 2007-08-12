package cz.cvut.felk.timejuggler.db.entity;

import cz.cvut.felk.timejuggler.db.TimeJugglerJDBCTemplate;
import cz.cvut.felk.timejuggler.db.entity.interfaces.CategoryEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Jan Struz
 * @version 0.1
 *          <p/>
 *          <p/>
 *          Propojovaci tabulka ke kategoriim a eventum
 */

public class Categories extends DbElement {
    private final static Logger logger = Logger.getLogger(Categories.class.getName());

    private int categoryId;
    private int componentId;

    //TODO: ....
    private List<Category> categories;

    public Categories() {
        categories = new ArrayList<Category>();
    }

    /**
     * Method saveOrUpdate
     * @param template
     */
    public void saveOrUpdate(TimeJugglerJDBCTemplate template) {

        if (getId() > 0) {

            /* TODO update nebude asi potreba.. pri zmene kategorii v eventu provest DELETE + INSERT ?
            
            logger.info("Database - Update: Categories[" + getId() + "]:" + name + "...");
            Object params[] = {name, componentId, getId()};
            String updateQuery = "UPDATE Categories SET name=?,calComponentID=? WHERE categoryID = ? ";
            template.executeUpdate(updateQuery, params);
            
            */
        } else {
            logger.info("Database - Insert: Categories[]:...");
            String insertQuery = "INSERT INTO Categories (categoryID,calComponentID) VALUES (?,?) ";
            for (Category c : categories) {
                Object params[] = {c.getId(), componentId};
                template.executeUpdate(insertQuery, params);
                /*setId(template.getGeneratedId());*/
            }
        }
    }

    /**
     * Method delete
     * @param template
     */
    public void delete(TimeJugglerJDBCTemplate template) {
        if (getId() > 0) {
            Object params[] = {getId()};
            String deleteQuery = "DELETE FROM Categories WHERE categoriesID = ? ";
            template.executeUpdate(deleteQuery, params);
            setId(-1);
        }
    }

    public void store() {

    }

    public void addCategory(Category cat) {
        categories.add(cat);
    }

    public void removeCategory(CategoryEntity cat) {
        categories.remove(cat);
    }

    public void setComponentId(int componentId) {
        this.componentId = componentId;
    }

    public int getComponentId() {
        return (this.componentId);
    }
}
