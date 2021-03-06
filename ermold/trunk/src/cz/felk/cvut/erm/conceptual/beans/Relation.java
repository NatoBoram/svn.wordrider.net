package cz.felk.cvut.erm.conceptual.beans;

import cz.felk.cvut.erm.conceptual.exception.AlreadyContainsException;
import cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException;
import cz.felk.cvut.erm.conceptual.exception.WasNotFoundException;
import cz.felk.cvut.erm.ermodeller.ConceptualConstructItem;
import cz.felk.cvut.erm.errorlog.*;
import cz.felk.cvut.erm.errorlog.exception.CheckNameDuplicityValidationException;

import javax.swing.*;
import java.util.Enumeration;

/**
 * <code>Relation</code> is <code>ConceptualObject</code> and it can create
 * and dispose cardinalities.
 *
 * @see Cardinality
 */
public class Relation extends ConceptualConstruct {
    /**
     * Creates new cardinality and connects it to the construct. Parameter
     * <code>anEntity</code> is the entity which is connected through the new
     * cardinality to the relation.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return cz.omnicom.ermodeller.conceptual.Cardinality
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @see Cardinality
     * @see #disposeCardinality
     * @see Cardinality#setRelation
     * @see Cardinality#setEntity
     */
    public synchronized Cardinality createCardinality(Entity anEntity) throws ParameterCannotBeNullException/*, IsAlreadyConnectedException*/ {
        if (anEntity == null)
            throw new ParameterCannotBeNullException();
/*	// If an entity is in the current relation, then cannot create
	//    new cardinality!!!!
	if (isConnectedToEntity(anEntity)) {
		throw new IsAlreadyConnectedException();
	}
*/
        Cardinality cardinality = new Cardinality();
        cardinality.setSchema(getSchema());
        cardinality.setName("Role" + getSchema().getCardinalityIDCounter());
        try {
            // Sets bidirectional connection to relation
            cardinality.setRelation(this);
        }
        catch (WasNotFoundException e) {
        } // Cannot be thrown.
        catch (AlreadyContainsException e) {
        } // Cannot be thrown.
        try {
            // Sets bidirectional connection to entity
            cardinality.setEntity(anEntity);
        }
        catch (WasNotFoundException e) { // Cannot be thrown.
            try {
                cardinality.setRelation(null);
            }
            catch (WasNotFoundException ex) {
            } // Cannot be thrown.
            catch (AlreadyContainsException ex) {
            } // Cannot be thrown.
        }
        catch (AlreadyContainsException e) { // Cannot be thrown.
            try {
                cardinality.setRelation(null);
            }
            catch (WasNotFoundException ex) {
            } // Cannot be thrown.
            catch (AlreadyContainsException ex) {
            } // Cannot be thrown.
        }
        return cardinality;
    }

    /**
     * Removes the cardinality from the schema.
     * Disconnects from bidirectional connection to entity and relation by calling:
     * <blockquote>
     * <pre>
     * aCardinality.empty();
     * </pre>
     * </blockquote>
     *
     * @param aCardinality cz.omnicom.ermodeller.conceptual.Cardinality
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @see #createCardinality
     * @see Cardinality#empty
     */
    public void disposeCardinality(Cardinality aCardinality) throws ParameterCannotBeNullException {
        if (aCardinality == null)
            throw new ParameterCannotBeNullException();

        aCardinality.empty();
    }

    /**
     * Gets the icon for representation in list of errors in <code>ErrorLogDialog</code>.
     * This returns icon for invalid state.
     *
     * @return Icon represented invalid state of the atribute.
     * @see cz.felk.cvut.erm.dialogs.ErrorLogDialog
     */
    public Icon getInvalidIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/relinvalid.gif"));
    }

    /**
     * Gets the icon for representation in list of errors in <code>ErrorLogDialog</code>.
     * This returns icon for valid state.
     *
     * @return Icon represented valid state of the atribute.
     * @see cz.felk.cvut.erm.dialogs.ErrorLogDialog
     */
    public Icon getValidIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/relvalid.gif"));
    }

    /**
     * Returns whether <code>anAtribute</code> is a member of primary key or not.
     * It doesn't support primary key, returns <code>false</code>.
     *
     * @param anAtribute cz.omnicom.ermodeller.conceptual.Atribute
     * @return boolean
     */
    protected boolean isAtributeMemberOfPrimaryKey(Atribute anAtribute) {
        return false;
    }

    /**
     * Returns whether <code>anEntity</code> is already connected via some cardinality to the <code>Relation</code>
     * or not.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return boolean
     */
    private synchronized boolean isConnectedToEntity(Entity anEntity) {
        if (anEntity == null)
            return false;
        for (Cardinality cardinality : getCardinalities()) {
            if (cardinality.getEntity() == anEntity)
                return true;
        }
        return false;
    }

    /**
     * Sets the <code>Relation</code> and all its cardinalities unvalidated.
     *
     * @see #valid
     * @see ConceptualObject#validate
     */
    protected void setAllUnvalidated() {
        // all objects set unvalidated
        //    - cardinalities
        for (Cardinality cardinality : getCardinalities()) {
            cardinality.setAllUnvalidated();
        }
        //    - atributes are set here:
        super.setAllUnvalidated();
    }

    /**
     * Checks the <code>Relation</code> and returns error list.
     *
     * @return cz.omnicom.ermodeller.errorlog.ErrorLogList
     * @throws cz.felk.cvut.erm.errorlog.exception.CheckNameDuplicityValidationException
     *
     * @see ConceptualObject#validate
     */
    protected synchronized ErrorLogList valid() throws CheckNameDuplicityValidationException {
        ErrorLogList superErrorLogList = super.valid();
        ErrorLogList errorLogList = new ErrorLogList();
        errorLogList.concatErrorLogList(superErrorLogList);
        ValidationError error;
        if (getCardinalities().size() < 2) {
            error = new DoesntHaveCardinalitiesValidationError(this);
            error.connectErrorToObject(this);
            errorLogList.addElement(error);
        }

        if (getSchema().getNotationType() != ConceptualConstructItem.CHEN &&
                getCardinalities().size() > 2) {
            error = new HaveManyCardinalitiesValidationError(this);
            error.connectErrorToObject(this);
            errorLogList.addElement(error);
        }
        for (Cardinality cardinality : getCardinalities()) {
            ErrorLogList cardErrorLogList = cardinality.validate();
            errorLogList.concatErrorLogList(cardErrorLogList);
        }
        for (Enumeration atributes = getAtributes().elements(); atributes.hasMoreElements();) {
            Atribute a = (Atribute) atributes.nextElement();
            if (a.isUnique()) {
                error = new HaveUniqueAtributeinRelationValidationError(this);
                error.connectErrorToObject(this);
                errorLogList.addElement(error);
            }
        }

        return errorLogList;
    }
}
