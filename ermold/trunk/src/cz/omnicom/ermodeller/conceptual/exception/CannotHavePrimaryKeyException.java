package cz.omnicom.ermodeller.conceptual.exception;

import cz.omnicom.ermodeller.conceptual.Entity;

/**
 * Entity cannot have primary key.
 */
public class CannotHavePrimaryKeyException extends ConceptualException {
    private Entity entity = null;

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
