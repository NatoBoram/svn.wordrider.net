package cz.felk.cvut.erm.conceptual.exception;

import cz.felk.cvut.erm.conceptual.beans.Entity;

/**
 * Entity cannot have primary key.
 */
public class CannotHavePrimaryKeyException extends ConceptualException {
    Entity entity = null;

    /**
     * CannotHavePrimaryKey constructor comment.
     */
    public CannotHavePrimaryKeyException(Entity aEntity) {
        this.entity = aEntity;
    }

    /**
     * Message in exception.
     *
     * @return java.lang.String
     */
    public String getMessage() {
        return "Entity " + entity.getName() + " have primary key";
    }
}
