package cz.felk.cvut.erm.conceptual.beans;

import cz.felk.cvut.erm.conceptual.exception.*;
import cz.felk.cvut.erm.errorlog.*;
import cz.felk.cvut.erm.errorlog.exception.CheckNameDuplicityValidationException;

import javax.swing.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <code>Entity</code> is <code>ConceptualObject</code> and it can have primary key,
 * unique keys,
 * it can be member of ISA hierarchy and it can be addicted to another
 * <code>Entity</code> or some <code>Entities</code> can be addicted to this one.
 */
public class Entity extends ConceptualConstruct {
    /**
     * Holds the group of atributes as a primary key of the <code>Entity</code>.
     */
    protected final Vector<Atribute> primaryKey = new Vector<Atribute>();
    /**
     * Constraints of <code>Entity</code>.
     */
    protected String constraints = "";
    /**
     * Parent in ISA hierarchy (superentity of this <code>Entity</code>).
     */
    protected Entity isaParent = null;
    /**
     * Sons in ISA hierarchy (subentities of this one).
     */
    protected Vector<Entity> isaSons = new Vector<Entity>();
    /**
     * <code>Entities</code> which this <code>Entity</code> is addicted to.
     */
    protected Vector<Entity> strongAddictionsParents = new Vector<Entity>();
    /**
     * <code>Entities</code> which are addicted to this <code>Entity</code>.
     */
    protected Vector<Entity> strongAddictionsSons = new Vector<Entity>();
    /**
     * Unique keys held by construct.
     *
     * @see UniqueKey
     */
    protected Vector<UniqueKey> uniqueKeys = new Vector<UniqueKey>();

    private static final int NO_SUBSET = 0;
    private static final int FIRST_SUBSET = 1;
    private static final int SECOND_SUBSET = 2;
    private static final int BOTH_SUBSET = 3;

    public static final String PRIMARYKEY_PROPERTY_CHANGE = "primaryKey";
    public static final String CONSTRAINTS_PROPERTY_CHANGE = "constraints";
    public static final String ISASONS_PROPERTY_CHANGE = "isaSons";
    public static final String ISAPARENTS_PROPERTY_CHANGE = "isaParent";
    public static final String UNIQUEKEYS_PROPERTY_CHANGE = "uniqueKeys";
    public static final String STRONGADDICTIONSSONS_PROPERTY_CHANGE = "strongAddictionsSons";
    public static final String STRONGADDICTIONSPARENTS_PROPERTY_CHANGE = "strongAddictionsParents";

    /**
     * Adds the ISA son to the <code>Entity</code>.
     * Is called by <code>setISAParent()</code> method.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @see #setISAParent
     */
    private synchronized void addISASon(Entity anEntity) throws ParameterCannotBeNullException {
        if (anEntity == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getISASons().clone();
        getISASons().addElement(anEntity);
        firePropertyChange(ISASONS_PROPERTY_CHANGE, oldValue, getISASons());
    }

    /**
     * Adds the strong addiction parent <code>anEntity</code>. Also adds the <code>Entity</code>
     * to the list of strong addiction sons of <code>anEntity</code>.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.MustHavePrimaryKeyException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.CycleWouldAppearException
     *
     * @see #addStrongAddictionSon
     */
    public synchronized void addStrongAddictionParent(Entity anEntity) throws ParameterCannotBeNullException, /*AlreadyAddictedException,*/ CycleWouldAppearException {
        // Sets bidirectional connection.
        if (anEntity == null)
            throw new ParameterCannotBeNullException();
/*  P�	if (getPrimaryKey() == null)
		throw new MustHavePrimaryKeyException(this);
*/
// comment allows parallel addictions and other constructs 
/*	if (containsStrongAddictionParent(anEntity) || haveHigherStrongAddictionParent(anEntity) || haveTransitivelyStrongAddictionParent(anEntity)) {
		// It has got already some kind of addiction on that anEntity.
		throw new AlreadyAddictedException();
	}
*/
        if (this == anEntity || /*anEntity.haveHigherStrongAddictionParent(this) || */anEntity.haveHigherCombinedParent(this))
            // A cycle would appear in the Addiction graph or in combined (also ISA) graph
            throw new CycleWouldAppearException(this, anEntity, CycleWouldAppearException.STRONG_ADDICTION_CYCLE);

        Vector oldValue = (Vector) getStrongAddictionsParents().clone();
        getStrongAddictionsParents().addElement(anEntity);
        try {
            anEntity.addStrongAddictionSon(this);
        }
        catch (ParameterCannotBeNullException e) {
        } // Cannot be thrown.
        firePropertyChange(STRONGADDICTIONSPARENTS_PROPERTY_CHANGE, oldValue, getStrongAddictionsParents());
    }

    /**
     * Adds the strong addiction son to the <code>Entity</code>.
     * Is called by <code>addStrongAddictionParent()</code> method.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @see #addStrongAddictionParent
     */
    private synchronized void addStrongAddictionSon(Entity anEntity) throws ParameterCannotBeNullException {
        if (anEntity == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getStrongAddictionsSons().clone();
        getStrongAddictionsSons().addElement(anEntity);
        firePropertyChange(STRONGADDICTIONSSONS_PROPERTY_CHANGE, oldValue, getStrongAddictionsSons());
    }

    /**
     * Adds <code>aUniqueKey</code> to the list of construct's unique keys.
     *
     * @param anUniqueKey cz.omnicom.ermodeller.conceptual.UniqueKey
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.CannotBeResetException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.AlreadyContainsException
     *
     * @see #removeUniqueKey
     */
    private synchronized void addUniqueKey(UniqueKey aUniqueKey) throws ParameterCannotBeNullException, AlreadyContainsException, CannotBeResetException {
        // Sets bidirectional connection to unique key
        if (aUniqueKey == null)
            throw new ParameterCannotBeNullException();
        if (containsUniqueKey(aUniqueKey))
            throw new AlreadyContainsException(this, aUniqueKey, AlreadyContainsException.UNIQUEKEYS_LIST);

        Vector oldValue = (Vector) getUniqueKeys().clone();
        aUniqueKey.setEntity(this); // If not new Unique Key, then throws CannotBeResetException.
        getUniqueKeys().addElement(aUniqueKey);
        firePropertyChange(UNIQUEKEYS_PROPERTY_CHANGE, oldValue, getUniqueKeys());
    }

    /**
     * Returns whether <code>anEntity</code> is already strong addiction parent of the <code>Entity</code>.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return boolean
     */
    private boolean containsStrongAddictionParent(Entity anEntity) {
        return getStrongAddictionsParents().contains(anEntity);
    }

    /**
     * Returns whether <code>anUniqueKey</code> is member of this <code>ConceptualConstruct</code> or not.
     *
     * @param anCardinality cz.omnicom.ermodeller.conceptual.UniqueKey
     * @return boolean
     */
    private boolean containsUniqueKey(UniqueKey anUniqueKey) {
        return getUniqueKeys().contains(anUniqueKey);
    }

    /**
     * Creates new unique key and adds it to the construct's list of unique keys.
     *
     * @return cz.omnicom.ermodeller.conceptual.UniqueKey
     * @see UniqueKey
     * @see #disposeUniqueKey
     */
    public synchronized UniqueKey createUniqueKey() {
        UniqueKey uniqueKey = new UniqueKey();
        uniqueKey.setName("");
        uniqueKey.setSchema(getSchema());
        try {
            addUniqueKey(uniqueKey);
        }
        catch (ParameterCannotBeNullException e) {
        } // Cannot be thrown.
        catch (AlreadyContainsException e) {
        } // Cannot be thrown.
        catch (CannotBeResetException e) {
        } // Cannot be thrown.
        return uniqueKey;
    }

    /**
     * Disposes all construct's atributes.
     */
    protected synchronized void disposeAllAtributes() {
        // Disconnects each atribute from all unique keys.
        for (Enumeration elements = getAtributes().elements(); elements.hasMoreElements();) {
            Atribute atribute = ((Atribute) elements.nextElement());
            removeAtributeFromAllUniqueKeys(atribute);
        }
        // Construct disposes each atribute itself.
        super.disposeAllAtributes();
    }

    /**
     * Disposes all ISA sons from the <code>Entity's</code> schema.
     */
    private synchronized void disposeAllISASons() {
        for (Enumeration<Entity> elements = getISASons().elements(); elements.hasMoreElements();) {
            try {
                getSchema().disposeEntity(elements.nextElement());
            }
            catch (ParameterCannotBeNullException e) {
            } // Cannot be thrown.
            catch (WasNotFoundException e) {
            } // Never mind, but shouldn't be thrown.
        }
    }

    /**
     * Empties the <code>Entity</code>.
     *
     * @see ConceptualConstruct#empty
     */
    protected synchronized void empty() {
        super.empty();
        // Disposes all ISA sons
        disposeAllISASons();
        try {
            // Disconnects from ISA parent
            setISAParent(null);
        }
        catch (WasNotFoundException e) {
        }
        catch (CycleWouldAppearException e) {
        }
        catch (CannotHavePrimaryKeyException e) {
        }
        // Disconnects from addiction parents
        removeAllAddictionParents();
        // Disconnects from addiction sons
        removeAllAddictionSons();
/*P�	try {
		// Resets the PK
		setPrimaryKey(null);
		disposeAllUniqueKeys();
	}
	catch (IsStrongAddictedException e) {}
	catch (IsISASonException e) {}*/
    }

    /**
     * Gets the icon for representation in list of errors in <code>ErrorLogDialog</code>.
     * This returns icon for invalid state.
     *
     * @return Icon represented invalid state of the atribute.
     * @see cz.felk.cvut.erm.dialogs.ErrorLogDialog
     */
    public Icon getInvalidIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/entinvalid.gif"));
    }

    /**
     * gets the ISA parent of the entity.
     *
     * @return cz.omnicom.ermodeller.conceptual.Entity
     * @see #setISAParent
     */
    public Entity getISAParent() {
        return isaParent;
    }

    /**
     * Returns ISA sons.
     *
     * @return java.util.Vector
     */
    public Vector<Entity> getISASons() {
        if (isaSons == null)
            isaSons = new Vector<Entity>();
        return isaSons;
    }

    /**
     * Returns primary key of the <code>Entity</code>.
     *
     * @return cz.omnicom.ermodeller.conceptual.UniqueKey
     * @see #setPrimaryKey
     */
    public Vector<Atribute> getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Returns strong addiction parents.
     *
     * @return java.util.Vector
     */
    public Vector<Entity> getStrongAddictionsParents() {
        if (strongAddictionsParents == null)
            strongAddictionsParents = new Vector<Entity>();
        return strongAddictionsParents;
    }

    /**
     * Returns strong addiction sons.
     *
     * @return java.util.Vector
     */
    public Vector<Entity> getStrongAddictionsSons() {
        if (strongAddictionsSons == null)
            strongAddictionsSons = new Vector<Entity>();
        return strongAddictionsSons;
    }

    /**
     * Returns uniqueKeys held by the construct.
     *
     * @return java.util.Vector
     */
    public Vector<UniqueKey> getUniqueKeys() {
        if (uniqueKeys == null)
            uniqueKeys = new Vector<UniqueKey>();
        return uniqueKeys;
    }

    /**
     * Gets the icon for representation in list of errors in <code>ErrorLogDialog</code>.
     * This returns icon for valid state.
     *
     * @return Icon represented valid state of the atribute.
     * @see cz.felk.cvut.erm.dialogs.ErrorLogDialog
     */
    public Icon getValidIcon() {
        return new ImageIcon(ClassLoader.getSystemResource("img/entvalid.gif"));
    }

    /**
     * Returns whether there is any addiction (strong or ISA) on <code>anEntity</code> in addiction hierarchy.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return boolean
     */
    private synchronized boolean haveHigherCombinedParent(Entity anEntity) {
        // For each strong addiction and ISA parent P {
        //    if (P == anEntity) return true;
        //    if (P.haveHigherCombinedParent(anEntity)) return true;
        // }
        // return false;
        if (anEntity == null)
            return false;
        for (Enumeration<Entity> elements = getStrongAddictionsParents().elements(); elements.hasMoreElements();) {
            Entity parent = elements.nextElement();
            if (parent == anEntity)
                return true;
            if (parent.haveHigherCombinedParent(anEntity))
                return true;
        }
        return isaParent == anEntity || isaParent != null && isaParent.haveHigherCombinedParent(anEntity);
    }

    /**
     * Returns whether <code>Entity</code> has <code>anEntity</code> somewhere higher in the ISA hierarchy.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return boolean
     */
    private synchronized boolean haveHigherISAParent(Entity anEntity) {
        return anEntity != null && (isaParent == anEntity || isaParent != null && isaParent.haveHigherISAParent(anEntity));
    }

    /**
     * Returns whether <code>Entity</code> has <code>anEntity</code> somewhere higher in the strong addiction hierarchy.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return boolean
     */
    private synchronized boolean haveHigherStrongAddictionParent(Entity anEntity) {
        // For each strong addiction parent P {
        //    if (P == anEntity) return true;
        //    if (P.haveHigherStrongAddictionParent(anEntity)) return true;
        // }
        // return false;
        if (anEntity == null)
            return false;
        for (Enumeration<Entity> elements = getStrongAddictionsParents().elements(); elements.hasMoreElements();) {
            Entity parent = elements.nextElement();
            if (parent == anEntity)
                return true;
            if (parent.haveHigherStrongAddictionParent(anEntity))
                return true;
        }
        return false;
    }

    /**
     * Returns whether <code>anEntity</code> has some <code>Entity's</code> strong addiction parent
     * somewhere higher in the strong addiction hierarchy.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @return boolean
     */
    private synchronized boolean haveTransitivelyStrongAddictionParent(Entity anEntity) {
        // For each strong addiction parent P {
        //    if (anEntity.haveHigherStrongAddictionParent(P))
        //        return true;
        // }
        // return false;
        if (anEntity == null)
            return false;
        for (Enumeration<Entity> elements = getStrongAddictionsParents().elements(); elements.hasMoreElements();) {
            Entity parent = elements.nextElement();
            if (anEntity.haveHigherStrongAddictionParent(parent))
                return true;
            if (parent.haveTransitivelyStrongAddictionParent(anEntity))
                return true;
        }
        return false;
    }
/**
 * Checks whether any unique key is subset of primary key or not.
 *
 * @return cz.omnicom.ermodeller.errorlog.ErrorLogList
 * @see #valid
 */
    /*private ErrorLogList checkUniqueKeyAreSubsetPrimaryKey() {
        ErrorLogList errorLogList = new ErrorLogList();
        UniqueKey primaryKey = getPrimaryKey();
        if (primaryKey == null)
            return errorLogList;
        Vector primaryKeyAtributes = primaryKey.getAtributes();
        UniqueKeyIsSubsetPrimaryKeyValidationError error = new UniqueKeyIsSubsetPrimaryKeyValidationError();

        boolean errorAppeared = false;
        for (Enumeration elements = getUniqueKeys().elements(); elements.hasMoreElements();) {
            UniqueKey uniqueKey = (UniqueKey) elements.nextElement();
            if (primaryKey != uniqueKey && !uniqueKey.getAtributes().isEmpty()) {
                Vector uniqueKeyAtributes = uniqueKey.getAtributes();
                int result = isSubset(primaryKeyAtributes, uniqueKeyAtributes);
                if (result == SECOND_SUBSET) {
                    errorAppeared = true;
                    // add to new Error
                    error.connectErrorToObject(uniqueKey);
                }

            }
        }
        if (errorAppeared) {
            // if error appeared, then add it to list of errors
            error.connectErrorToObject(primaryKey);
            errorLogList.addElement(error);
        }
        return errorLogList;
    }*/
/**
 * Checks the equivalency of all unique keys.
 *
 * @return cz.omnicom.ermodeller.errorlog.ErrorLogList
 * @see #valid
 */
    private ErrorLogList checkUniqueKeyEquality() {
        ErrorLogList errorLogList = new ErrorLogList();
        Vector<ConceptualObjectNameController> vectorToCheck = new Vector<ConceptualObjectNameController>();
        for (Enumeration<UniqueKey> elements = getUniqueKeys().elements(); elements.hasMoreElements();) {
            vectorToCheck.addElement(new ConceptualObjectNameController(elements.nextElement()));
        }

        for (int i = 0; i < vectorToCheck.size(); i++) {
            ConceptualObjectNameController firstController = vectorToCheck.elementAt(i);
            if (!firstController.isAlreadyWrong()) {
                UniqueKey firstUniqueKey = (UniqueKey) firstController.getConceptualObject();
                Vector firstAtributes = firstUniqueKey.getAtributes();
                // create new Error
                UniqueKeyEqualValidationError error = new UniqueKeyEqualValidationError();
                boolean errorAppeared = false;
                for (int j = i + 1; j < vectorToCheck.size(); j++) {
                    ConceptualObjectNameController secondController = vectorToCheck.elementAt(j);
                    if (!secondController.isAlreadyWrong()) {
                        UniqueKey secondUniqueKey = (UniqueKey) secondController.getConceptualObject();
                        Vector secondAtributes = secondUniqueKey.getAtributes();
                        int result = isSubset(firstAtributes, secondAtributes);
                        if (result == BOTH_SUBSET) {
                            errorAppeared = true;
                            // add to new Error
                            error.connectErrorToObject(secondUniqueKey);
                            firstController.setWrong(true);
                            secondController.setWrong(true);
                        }
                    }
                }
                if (errorAppeared) {
                    // if error appeared, then add it to list of errors
                    error.connectErrorToObject(firstUniqueKey);
                    errorLogList.addElement(error);
                }
            }
        }
        return errorLogList;
    }

    /**
     * Checks whether any unique key is subset of another.
     *
     * @return cz.omnicom.ermodeller.errorlog.ErrorLogList
     */
    private ErrorLogList checkUniqueKeySubsets() {
        ErrorLogList errorLogList = new ErrorLogList();
        Vector<ConceptualObjectNameController> vectorToCheck = new Vector<ConceptualObjectNameController>();
        for (Enumeration<UniqueKey> elements = getUniqueKeys().elements(); elements.hasMoreElements();) {
            vectorToCheck.addElement(new ConceptualObjectNameController(elements.nextElement()));
        }

        for (int i = 0; i < vectorToCheck.size(); i++) {
            ConceptualObjectNameController firstController = vectorToCheck.elementAt(i);
            if (!firstController.isAlreadyWrong()) {
                UniqueKey firstUniqueKey = (UniqueKey) firstController.getConceptualObject();
                Vector firstAtributes = firstUniqueKey.getAtributes();
                // create new Error
                UniqueKeySubsetValidationError error = new UniqueKeySubsetValidationError();
                boolean errorAppeared = false;
                for (int j = i + 1; j < vectorToCheck.size(); j++) {
                    ConceptualObjectNameController secondController = vectorToCheck.elementAt(j);
                    if (!secondController.isAlreadyWrong()) {
                        UniqueKey secondUniqueKey = (UniqueKey) secondController.getConceptualObject();
                        Vector secondAtributes = secondUniqueKey.getAtributes();
                        int result = isSubset(firstAtributes, secondAtributes);
                        if (result != NO_SUBSET) {
                            errorAppeared = true;
                            // add to new Error
                            error.connectErrorToObject(secondUniqueKey);
                            if (result == FIRST_SUBSET)
                                firstController.setWrong(true);
                            if (result == SECOND_SUBSET)
                                secondController.setWrong(true);
                            if (result == BOTH_SUBSET) {
                                firstController.setWrong(true);
                                secondController.setWrong(true);
                            }
                        }
                    }
                }
                if (errorAppeared) {
                    // if error appeared, then add it to list of errors
                    error.connectErrorToObject(firstUniqueKey);
                    errorLogList.addElement(error);
                }
            }
        }
        return errorLogList;
    }

    /**
     * Returns whether <code>anAtribute</code> is a member of primary key or not.
     *
     * @param anAtribute cz.omnicom.ermodeller.conceptual.Atribute
     * @return boolean
     */
    protected boolean isAtributeMemberOfPrimaryKey(Atribute anAtribute) {
        if (anAtribute == null)
            return false;
        Vector<Atribute> primaryKey = getPrimaryKey();
        return primaryKey != null && primaryKey.contains(anAtribute);
    }

    /**
     * Returns whether <code>Entity</code> is ISA son or not.
     *
     * @return boolean
     */
    public boolean isISASon() {
        return isaParent != null;
    }
/**
 * Returns whether <code>anUniqueKey</code> is a primary key or not.
 *
 * @return boolean
 * @param anUniqueKay cz.omnicom.ermodeller.conceptual.UniqueKey
 */
    /*protected boolean isPrimaryKey(UniqueKey anUniqueKey) {
        if (anUniqueKey == null)
            return false;
        UniqueKey primaryKey = getPrimaryKey();
        if (primaryKey != null)
            return (primaryKey == anUniqueKey);
        return false;
    }*/
/**
 * Returns whether <code>Entity</code> is strong addicted or not.
 *
 * @return boolean
 */
    public boolean isStrongAddicted() {
        return !(getStrongAddictionsParents().isEmpty());
    }

    /**
     * Checks two vectors, if one is subset of another.
     * <p/>
     * Returns NO_SUBSET, FIRST_SUBSET, SECOND_SUBSET, BOTH_SUBSET.
     *
     * @param firstVector  java.util.Vector
     * @param secondVector java.util.Vector
     * @return boolean
     */
    private static int isSubset(Vector firstVector, Vector secondVector) {
        int result = NO_SUBSET;
        synchronized (firstVector) {
            synchronized (secondVector) {
                boolean firstSubset = true;
                // first > second
                for (Enumeration elements = firstVector.elements(); elements.hasMoreElements();) {
                    if (!secondVector.contains(elements.nextElement())) {
                        firstSubset = false;
                        break;
                    }
                }
                boolean secondSubset = true;
                // second > first
                for (Enumeration elements = secondVector.elements(); elements.hasMoreElements();) {
                    if (!firstVector.contains(elements.nextElement())) {
                        secondSubset = false;
                        break;
                    }
                }
                if (firstSubset)
                    result = FIRST_SUBSET;
                if (secondSubset)
                    result = SECOND_SUBSET;
                if (firstSubset && secondSubset)
                    result = BOTH_SUBSET;
            }
        }
        return result;
    }

    /**
     * Removes all strong addiction parents from the <code>Entity</code>.
     */
    private synchronized void removeAllAddictionParents() {
        for (Enumeration<Entity> elements = getStrongAddictionsParents().elements(); elements.hasMoreElements();) {
            try {
                removeStrongAddictionParent(elements.nextElement());
            }
            catch (ParameterCannotBeNullException e) {
                // Cannot be thrown.
            }
            catch (WasNotFoundException e) {
                // Never mind, but shouldn't be thrown.
            }
        }
        getStrongAddictionsParents().trimToSize();
    }

    /**
     * Removes all strong addiction sons from the <code>Entity</code>.
     */
    private synchronized void removeAllAddictionSons() {
        for (Enumeration<Entity> elements = getStrongAddictionsSons().elements(); elements.hasMoreElements();) {
            try {
                (elements.nextElement()).removeStrongAddictionParent(this);
            }
            catch (ParameterCannotBeNullException e) {
            } // Cannot be thrown.
            catch (WasNotFoundException e) {
            } // Never mind, but shouldn't be thrown.
        }
        getStrongAddictionsSons().trimToSize();
    }

    /**
     * Removes <code>anAtribute</code> from the list of construct's atributes.
     *
     * @param anAtribute cz.omnicom.ermodeller.conceptual.Atribute
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.WasNotFoundException
     *
     * @see #addAtribute
     */
    protected synchronized void removeAtribute(Atribute anAtribute) throws ParameterCannotBeNullException, WasNotFoundException {
        if (anAtribute == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getAtributes().clone();
        if (!(getAtributes().removeElement(anAtribute))) { // was it removed?
            // No, it wasn't found.
            throw new WasNotFoundException(this, anAtribute, ListException.ATRIBUTES_LIST);
        } else {
            // Yes.
            // Remove the atribute from all construct's unique keys.
            removeAtributeFromAllUniqueKeys(anAtribute);
            anAtribute.setConstruct(null);
            firePropertyChange(ATRIBUTES_PROPERTY_CHANGE, oldValue, getAtributes());
        }
    }

    /**
     * Removes <code>anAtribute</code> from all construct's unique keys.
     *
     * @param anAtribute cz.omnicom.ermodeller.conceptual.Atribute
     * @see UniqueKey#removeAtribute
     */
    private synchronized void removeAtributeFromAllUniqueKeys(Atribute anAtribute) {
        for (Enumeration<UniqueKey> elements = getUniqueKeys().elements(); elements.hasMoreElements();) {
            try {
                (elements.nextElement()).removeAtribute(anAtribute);
            }
            catch (ParameterCannotBeNullException e) {
            } // Never mind.
            catch (WasNotFoundException e) {
            } // Never mind.
        }
    }

    /**
     * Removes ISA son <code>anEntity</code>.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.WasNotFoundException
     *
     */
    private synchronized void removeISASon(Entity anEntity) throws ParameterCannotBeNullException, WasNotFoundException {
        if (anEntity == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getISASons().clone();
        if (!(getISASons().removeElement(anEntity))) { // was it removed?
            // No, it wasn't found.
            throw new WasNotFoundException(this, anEntity, ListException.ISA_SONS_LIST);
        } else {
            // Yes.
            firePropertyChange(ISASONS_PROPERTY_CHANGE, oldValue, getISASons());
        }
    }

    /**
     * Removes strong addiction parent <code>anEntity</code> from the <code>Entity</code> - disposes bidirectional
     * strong addiction connection.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.WasNotFoundException
     *
     */
    public synchronized void removeStrongAddictionParent(Entity anEntity) throws ParameterCannotBeNullException, WasNotFoundException {
        // Removes bidirectioanl connection.
        if (anEntity == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getStrongAddictionsParents().clone();
        if (!(getStrongAddictionsParents().removeElement(anEntity))) { // was it removed?
            // No, it wasn't found.
            throw new WasNotFoundException(this, anEntity, ListException.STRONG_ADDICTION_PARENTS_LIST);
        } else {
            // Yes.
            try {
                anEntity.removeStrongAddictionSon(this);
            }
            catch (ParameterCannotBeNullException e) {
            } // Cannot be thrown.
            catch (WasNotFoundException e) {
                getStrongAddictionsParents().addElement(anEntity);
                throw e;
            }
            firePropertyChange(STRONGADDICTIONSPARENTS_PROPERTY_CHANGE, oldValue, getStrongAddictionsParents());
        }
    }

    /**
     * Removes strong addiction son <code>anEntity</code> from the <code>Entity</code>.
     * Is called by <code>removeStrongAddictionParent()</code> method.
     *
     * @param anEntity cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.WasNotFoundException
     *
     * @see #removeStrongAddictionParent
     */
    private synchronized void removeStrongAddictionSon(Entity anEntity) throws ParameterCannotBeNullException, WasNotFoundException {
        if (anEntity == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getStrongAddictionsSons().clone();
        if (!(getStrongAddictionsSons().removeElement(anEntity))) { // was it removed?
            // No, it wasn't found.
            throw new WasNotFoundException(this, anEntity, ListException.STRONG_ADDICTION_SONS_LIST);
        } else {
            // Yes.
            firePropertyChange(STRONGADDICTIONSSONS_PROPERTY_CHANGE, oldValue, getStrongAddictionsSons());
        }
    }
/**
 * Removes <code>aUniqueKey</code> from the list of construct's unique keys.
 *
 * @param anUniqueKey cz.omnicom.ermodeller.conceptual.UniqueKey
 * @exception cz.felk.cvut.erm.conceptual.exception.ParameterCannotBeNullException
 * @exception cz.felk.cvut.erm.conceptual.exception.WasNotFoundException
 * @exception cz.felk.cvut.erm.conceptual.exception.IsStrongAddictedException
 * @see #addUniqueKey
 */
    /*private synchronized void removeUniqueKey(UniqueKey aUniqueKey) throws ParameterCannotBeNullException, WasNotFoundException, IsStrongAddictedException {
        if (aUniqueKey == null)
            throw new ParameterCannotBeNullException();

        Vector oldValue = (Vector) getUniqueKeys().clone();
        if (!(getUniqueKeys().removeElement(aUniqueKey))) { // was it removed?
            // No, it wasn't found.
            throw new WasNotFoundException(this, aUniqueKey, ListException.UNIQUEKEYS_LIST);
        }
        else {
            // Yes.
            // If it is a Primary key in the entity, then reset primary key.
            if (isPrimaryKey(aUniqueKey)) {
                try {
                    setPrimaryKey(null);
                }
                catch (IsStrongAddictedException e) {
                    getUniqueKeys().addElement(aUniqueKey);
                    throw e;
                }
                catch (IsISASonException e) {} // cannot be thrown
            }
            // Remove all atributes from this unique keys.
            aUniqueKey.empty();
            // Throws an exception every time - see UniqueKey::setConstruct(Construct aConstruct)
            // aUniqueKey.setConstruct(null);
            firePropertyChange(UNIQUEKEYS_PROPERTY_CHANGE, oldValue, getUniqueKeys());
        }
    }*/
/**
 * Sets the <code>Entity</code> validated property to <code>false</code>.
 * Also all unique keys.
 */
    protected void setAllUnvalidated() {
        // all objects set unvalidated
        //    - uniqueKeys
        for (Enumeration<UniqueKey> uniqueKeys = getUniqueKeys().elements(); uniqueKeys.hasMoreElements();) {
            (uniqueKeys.nextElement()).setAllUnvalidated();
        }
        //    - atributes are set here:
        super.setAllUnvalidated();
    }

    /**
     * Sets the ISA parent in the ISA hierarchy.
     *
     * @param anAncestor cz.omnicom.ermodeller.conceptual.Entity
     * @throws cz.felk.cvut.erm.conceptual.exception.CannotHavePrimaryKeyException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.WasNotFoundException
     *
     * @throws cz.felk.cvut.erm.conceptual.exception.CycleWouldAppearException
     *
     */
    public synchronized void setISAParent(Entity anISAParent) throws CannotHavePrimaryKeyException, WasNotFoundException, CycleWouldAppearException {
        // Sets bidirectional connection.
        // comment - is check in validate() before generating
        if (getPrimaryKey() != null && getPrimaryKey().size() > 0) {
            throw new CannotHavePrimaryKeyException(this);
        }

        if (anISAParent != null)
            if (anISAParent == this || /*anISAParent.haveHigherISAParent(this) || */anISAParent.haveHigherCombinedParent(this))
                // A cycle would appear in the ISA graph or in combined graph.
                throw new CycleWouldAppearException(this, anISAParent, CycleWouldAppearException.ISA_CYCLE);

        Entity oldValue = isaParent;
        if (oldValue != null) {
            try {
                oldValue.removeISASon(this);
                // Can throw WasNotFound exception.
            }
            catch (ParameterCannotBeNullException e) {
            } // Cannot be thrown.
            isaParent = null;
            firePropertyChange(ISAPARENTS_PROPERTY_CHANGE, oldValue, isaParent);
        }
        if (anISAParent != null) {
            try {
                anISAParent.addISASon(this);
            }
            catch (ParameterCannotBeNullException e) {
            } // Cannot be thrown.
        }
        isaParent = anISAParent;
        firePropertyChange(ISAPARENTS_PROPERTY_CHANGE, oldValue, isaParent);
    }

    public synchronized void addMemberOfPrimaryKey(Atribute aMemberOfPrimaryKey) {
        Vector<Atribute> oldValue = primaryKey;
        primaryKey.addElement(aMemberOfPrimaryKey);
/*	if (primaryKey != null)
		primaryKey.setAllAtributesArbitrary();
*/
        firePropertyChange(PRIMARYKEY_PROPERTY_CHANGE, oldValue, primaryKey);
    }

    /**
     */
    public synchronized void removeMemberOfPrimaryKey(Atribute aMemberOfPrimaryKey) {
        Vector<Atribute> oldValue = primaryKey;
        if (primaryKey != null && primaryKey.contains(aMemberOfPrimaryKey))
            primaryKey.removeElement(aMemberOfPrimaryKey);
/*	if (primaryKey != null)
		primaryKey.setAllAtributesArbitrary();
*/
        firePropertyChange(PRIMARYKEY_PROPERTY_CHANGE, oldValue, primaryKey);
    }

    /**
     * Checks the entity and returns list of errors.
     *
     * @return cz.omnicom.ermodeller.errorlog.ErrorLogList
     * @throws cz.felk.cvut.erm.errorlog.exception.CheckNameDuplicityValidationException
     *
     * @see ConceptualObject#validate
     * @see #checkUniqueKeyAreSubsetPrimaryKey
     * @see #checkUniqueKeyEquality
     */
    protected synchronized ErrorLogList valid() throws CheckNameDuplicityValidationException {
        ErrorLogList superErrorLogList = super.valid();
        ErrorLogList errorLogList = new ErrorLogList();
        errorLogList.concatErrorLogList(superErrorLogList);
        ValidationError error;
        if (getAtributes().isEmpty() && !isStrongAddicted() && !isISASon()) {
            error = new DoesntHaveAtributeValidationError(this);
            error.connectErrorToObject(this);
            errorLogList.addElement(error);
        }
        if (isStrongAddicted() && isISASon()) {
            error = new CannotBeISASonAndStrongAddictedValidationError(this);
            error.connectErrorToObject(this);
            errorLogList.addElement(error);
        }
        if (!isStrongAddicted()) {
            if (isISASon()) {
                // may not have primary key
                if (getPrimaryKey() != null && getPrimaryKey().size() > 0) {
                    error = new MayNotHavePrimaryKeyValidationError(this);
                    error.connectErrorToObject(this);
                    errorLogList.addElement(error);
                }
            } else {
                // must have primary key
                //System.out.println(getPrimaryKey().toString());
                if (getPrimaryKey() == null || getPrimaryKey().size() == 0) {
                    error = new MustHavePrimaryKeyValidationError(this);
                    error.connectErrorToObject(this);
                    errorLogList.addElement(error);
                }
            }
        }
        for (Enumeration<UniqueKey> elements = getUniqueKeys().elements(); elements.hasMoreElements();) {
            ErrorLogList unqErrorLogList = (elements.nextElement()).validate();
            errorLogList.concatErrorLogList(unqErrorLogList);
        }
        // unique key atributes cannot be subset of primary key atributes
//?????????????????????????????	errorLogList.concatErrorLogList(checkUniqueKeyAreSubsetPrimaryKey());
        // check for equality of unique keys
        errorLogList.concatErrorLogList(checkUniqueKeyEquality());
        return errorLogList;
    }

    /**
     * Writes data for entity model into XML file
     *
     * @param pw java.io.PrintWriter
     */
    public void write(java.io.PrintWriter pw) {
        super.write(pw);
        pw.println("\t\t<constraints>" + constraints + "</constraints>");
        Entity ent = getISAParent();
        if (ent != null)
            pw.println("\t\t<parent>" + ent.getID() + "</parent>");
    }

    /**
     * @return Returns the constraints.
     */
    public String getConstraints() {
        return constraints;
    }

    /**
     * @param constraints The constraints to set.
     */
/*	public void setConstraints(String constraints) {
		this.constraints = constraints;
	}
*/
    public synchronized void setConstraints(String constraints) {
        String oldValue = this.constraints;
        this.constraints = constraints;
        firePropertyChange(CONSTRAINTS_PROPERTY_CHANGE, oldValue, constraints);
    }
}
