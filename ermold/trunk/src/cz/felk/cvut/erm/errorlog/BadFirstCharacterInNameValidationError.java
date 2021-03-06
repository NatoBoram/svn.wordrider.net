package cz.felk.cvut.erm.errorlog;

import cz.felk.cvut.erm.conceptual.beans.ConceptualObject;

/**
 * Name contains unallowed characters.
 */
public class BadFirstCharacterInNameValidationError extends ConceptualObjectValidationError {
    /**
     * found bad characters.
     */
    final char badFirstChar;

    /**
     * BadCharacterInNameValidationError constructor comment.
     *
     * @param anObject cz.omnicom.ermodeller.conceptual.ConceptualObject
     */
    public BadFirstCharacterInNameValidationError(ConceptualObject anObject, char firstBadChar) {
        super(anObject);
        this.badFirstChar = firstBadChar;
    }

    /**
     * Returns the title associated with the tree node.
     *
     * @return java.lang.String
     */
    public String toString() {
        return "Bad first character ('" + badFirstChar + "')in object's name";
    }
}
