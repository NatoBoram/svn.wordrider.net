package cz.felk.cvut.erm.conc2obj;

import cz.felk.cvut.erm.conc2rela.RelationC2R;

/**
 * Part of check which enforces constraints for corresponding
 * conceptual entity in glued relation.
 */
class EntGlueControlPartOfCheckObj extends GlueControlPartOfCheckObj {
    /**
     * Corresponding relational entity.
     */
    private RelationC2R relation = null;

    /**
     * Constructor.
     *
     * @param aRelationC2R cz.omnicom.ermodeller.conc2rela.RelationC2R
     */
    public EntGlueControlPartOfCheckObj(RelationC2R aRelationC2R) {
        relation = aRelationC2R;
    }

    /**
     * Returns string representation of the part of check.
     * "Conceptual construct EEE present".
     *
     * @return java.lang.String
     */
    public String toString() {
        return "Conceptual construct " + relation.getNameC2R() + " present";
    }
}
