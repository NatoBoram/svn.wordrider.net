package cz.felk.cvut.erm.errorlog;

import cz.felk.cvut.erm.conceptual.beans.ConceptualConstruct;

/**
 * Construct shoul be connected to at least 2 cardinalities.
 *
 * @see cz.felk.cvut.erm.conceptual.beans.ConceptualConstruct
 * @see cz.felk.cvut.erm.conceptual.beans.Cardinality
 */
public class HaveManyCardinalitiesValidationError extends ConceptualObjectValidationError {
    /**
     * Constructor.
     *
     * @param anObject cz.omnicom.ermodeller.conceptual.ConceptualObject
     */
    public HaveManyCardinalitiesValidationError(ConceptualConstruct anObject) {
        super(anObject);
    }

    /**
     * Returns the title associated with the tree node.
     *
     * @return java.lang.String
     */
    public String toString() {
        return "Relationship cannot have more than 2 cardinalities in binary or UML notation";
    }
}
