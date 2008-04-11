package cz.green.ermodeller;

import cz.green.event.ResizePoint;
import cz.green.event.exceptions.ImpossibleNegativeValueException;
import cz.green.event.exceptions.ItemNotInsideManagerException;
import cz.green.event.interfaces.Item;
import cz.green.event.interfaces.Manager;
import cz.green.event.interfaces.PaintableManager;
import cz.green.eventtool.Connection;
import cz.green.eventtool.ConnectionArrow;
import cz.green.eventtool.ConnectionManager;
import cz.green.eventtool.Printable;
import cz.green.swing.ShowException;

import javax.swing.*;
import java.awt.*;

/**
 * This type was created in VisualAge.
 */
public class StrongAddiction extends ConceptualObject {
    public static final int SIZE = 6;
    cz.green.ermodeller.Entity parent;
    cz.green.ermodeller.Entity child;

    /**
     * StrongAddiction constructor comment.
     *
     * @param manager cz.green.event.Manager
     * @param left    int
     * @param top     int
     * @param width   int
     * @param height  int
     * @throws java.lang.NullPointerException The exception description.
     * @throws cz.green.event.exceptions.ImpossibleNegativeValueException
     *                                        The exception description.
     */
    public StrongAddiction(Entity parent, Entity son, Manager manager, int left, int top) throws NullPointerException, ImpossibleNegativeValueException {
        super(manager, left - (SIZE / 2), top - (SIZE / 2), SIZE, SIZE);
        this.parent = parent;
        this.child = son;
        if (ACTUAL_NOTATION == BINARY) {
            java.awt.FontMetrics fm = ((FontManager) manager).getReferentFontMetrics();
            Dimension dim = new java.awt.Dimension(fm.getAscent() + fm.stringWidth("N:N"), (int) (2.25 * fm.getAscent()));
            rect[0][1] = rect[0][0] + dim.width;
            rect[1][1] = rect[1][0] + dim.height;
        }
    }

    protected java.awt.Dimension countSize() {
        java.awt.FontMetrics fm;
        fm = ((FontManager) manager).getReferentFontMetrics();
        int w2 = fm.stringWidth("N:N"), height = fm.getAscent();
        try {
            switch (ACTUAL_NOTATION) {
                case (CHEN):
                    break;
                case (BINARY):
                    return new java.awt.Dimension(height + w2, (int) (2.25 * height));
                case (UML):
                    return new java.awt.Dimension(2 * SIZE, SIZE);
            }
            return new java.awt.Dimension(SIZE, SIZE);
        } catch (ClassCastException e) {
            return new java.awt.Dimension(10, 10);
        }
    }

    /**
     * This method adds items to the context menu, which are specific to the atribute.
     * If the childs wants to display other items in the context menu the best way is to
     * override this methods. Creates only one item - Properties, common to all objects.
     *
     * @param menu  The popup menu where to add the new items.
     * @param event The event, which caused the context menu displaying. Is useful for determing targets of the
     *              methods call.
     * @return The filled menu.
     */
    protected JPopupMenu createMenu(JPopupMenu menu, PopupMenuEvent event) {
        menu.removeAll();
        return menu;
    }

    /**
     * Adds new strong addiction parent. We should create new arrow connection to the netity.
     *
     * @param ent Strong addiction Parent.
     */
    static public StrongAddiction createStrongAddiction(Entity parent, Entity child, Manager man, int left, int top) {
        try {
            if (ACTUAL_NOTATION == UML)
                man = parent.getManager();
            cz.omnicom.ermodeller.conceptual.Entity cPar = (cz.omnicom.ermodeller.conceptual.Entity) parent.getModel();
            cz.omnicom.ermodeller.conceptual.Entity cChild = (cz.omnicom.ermodeller.conceptual.Entity) child.getModel();
            cChild.addStrongAddictionParent(cPar);
            StrongAddiction sa = new StrongAddiction(parent, child, man, left, top);
            man.add(sa);
            ((PaintableManager) man).repaintItem(sa);
            //create connection to unique key
            Connection conn = new ConnectionArrow(man, sa, child);
            ((ConnectionArrow) conn).setStrongAddicted(true);
            ((ConnectionArrow) conn).setStrongAddictionChild(true);
            ((ConnectionManager) man).addConnectionToMain(conn);
            ((PaintableManager) man).repaintItem(conn);
            //create connection to entity
            conn = new ConnectionArrow(man, sa, parent);
            ((ConnectionArrow) conn).setStrongAddicted(true);
            ((ConnectionArrow) conn).setStrongAddictionChild(false);
            ((ConnectionManager) man).addConnectionToMain(conn);
            ((PaintableManager) man).repaintItem(conn);
            sa.moveStrongAddiction(new ExMovingEvent(sa.getBounds().x, sa.getBounds().x, 0, 0, null, false));
            return sa;
        } catch (Throwable x) {
            ShowException d = new ShowException(null, "Error", x, true);
        }
        return null;
    }

    /**
     * Get the entity that participation this object represents.
     */
    public Entity getEntity() {
        java.util.Enumeration e = connections.elements();
        while (e.hasMoreElements()) {
            Connection c = ((Connection) e.nextElement());
            if (c.getOne() instanceof Entity)
                return (Entity) (c.getOne());
            if (c.getTwo() instanceof Entity)
                return (Entity) (c.getTwo());
        }
        return null;
    }

    /**
     * This method return all points where should have be placed resize point.
     * The position of all resize points is specified by relative coordinates
     * according to the bounds of this item.
     *
     * @see ResizePoint
     * @see cz.green.event.interfaces.PaintableItem#getResizePoints()
     */
    public ResizePoint[] getResizePoints() {
        return null;
    }

    /**
     * Get the entity that participation this object represents.
     */
    public UniqueKey getUniqueKey() {
        java.util.Enumeration e = connections.elements();
        while (e.hasMoreElements()) {
            Connection c = ((Connection) e.nextElement());
            if (c.getOne() instanceof UniqueKey)
                return (UniqueKey) (c.getOne());
            if (c.getTwo() instanceof UniqueKey)
                return (UniqueKey) (c.getTwo());
        }
        return null;
    }

    /**
     * Handle event when soma element is dragging over. Can work only with <code>ConceptualConstruct</code>
     * instances.
     */
    public void handleDragOverEvent(DragOverEvent event) {
        if (selected && event.getAdd())
            return;
        Item item = event.getItem();
        if (item instanceof Entity) {
            if (event.getAdd()) {
                if (this.connectionTo(item) == null) {
                    event.getComponent().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    return;
                }
            }
        }
        event.getComponent().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Handle event when soma element is dropping above. Can work only with <code>ConceptualConstruct</code>
     * instances and that action caused the reconnection to other entity or relation.
     */
    public void handleDropAboveEvent(DropAboveEvent event) {
        if (selected && event.getAdd())
            return;
        Item item = event.getItem();
        if (item instanceof Entity) {
            if (event.getAdd()) {
                if (this.connectionTo(item) == null) {
                    reconnectStrongAddictionParent((Entity) item);
                    event.setDropped(true);
                }
            }
        }
        event.getComponent().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * When removing unique key, then we have to remove all strong addictions
     * (method <code>disposeStrongAddiction</code>), dispose unique key int model objects
     * and call inherited handling method.
     *
     * @see #disposeStrongAddiction()
     */
    public void handleRemoveEvent(cz.green.event.RemoveEvent event) {
        try {
            cz.omnicom.ermodeller.conceptual.Entity Cparent = (cz.omnicom.ermodeller.conceptual.Entity) parent.getModel();
            cz.omnicom.ermodeller.conceptual.Entity Cchild = (cz.omnicom.ermodeller.conceptual.Entity) child.getModel();
            Cchild.removeStrongAddictionParent(Cparent);
            super.handleRemoveEvent(event);
        } catch (Throwable x) {
            ShowException d = new ShowException(null, "Error", x, true);
        }
    }

    /**
     * Handle moving event and adds restrictions to BIN and UML notation
     */
    public void handleExMovingEvent(ExMovingEvent event) {
        int dx, dy = 0;
        java.awt.Point cardinalityCenter = getCenter();
        java.awt.Rectangle er;
        er = (ACTUAL_NOTATION == UML) ? getParent().getBounds() : getChild().getBounds();
        java.awt.Rectangle r = getBounds();

/*	if (ACTUAL_NOTATION != UML)*/
        {
            if (event.getX() < er.x) {
                dx = (er.x - r.width / 2) - cardinalityCenter.x;
                if (cardinalityCenter.y + r.height / 2 + event.getDy() >= er.y
                        && cardinalityCenter.y - r.height / 2 + event.getDy() <= er.y + er.height)
                    dy = event.getDy();
                else dy = 0;
                //|| cardinalityCenter.y > er.y + er.height)?0:event.getDy();
                //dy = event.getDy();
            } else if (event.getX() > (er.x + er.width)) {
                dx = (er.x + er.width + r.width / 2) - cardinalityCenter.x;
                if (cardinalityCenter.y + r.height / 2 + event.getDy() >= er.y
                        && cardinalityCenter.y - r.height / 2 + event.getDy() <= er.y + er.height)
                    dy = event.getDy();
                else dy = 0;
            } else if (event.getY() < er.y) {
                dx = event.getDx();
                dy = (er.y - r.height / 2) - cardinalityCenter.y;
            } else if (cardinalityCenter.y > (er.y + er.height)) {
                dx = event.getDx();
                dy = (er.y + er.height + r.height / 2) - cardinalityCenter.y;
            }/* Cardinality is inside Enity */ else {
                if (cardinalityCenter.x < er.x + er.width / 2)
                    dx = er.x - r.width / 2 - cardinalityCenter.x;
                else
                    dx = er.x + er.width + r.width / 2 - cardinalityCenter.x;
            }
        }
/*	else {
		dx = event.getDx();
		dy = event.getDy();
	}
*/
        if (paintedFast) {
            ((PaintableManager) manager).repaintItemFast(this);
        } else {
            paintedFast = true;
            rectangle = getBounds();
        }
        try {
            move(dx, dy, false);
        } catch (ItemNotInsideManagerException e) {
        } finally {
            ((PaintableManager) manager).repaintItemFast(this);
        }
    }

    /**
     * Handle moving event and adds restrictions to BIN and UML notation
     */
    public void handleExMoveEvent(ExMoveEvent event) {
        int dx = 0, dy = 0;
        java.awt.Point cardinalityCenter = getCenter();
        java.awt.Rectangle er;
        er = (ACTUAL_NOTATION == UML) ? getParent().getBounds() : getChild().getBounds();
        java.awt.Rectangle r = getBounds();

//	if (ConceptualConstruct.ACTUAL_NOTATION != ConceptualConstruct.UML )
        {
            if (cardinalityCenter.x < er.x && cardinalityCenter.y < er.y) {
                dx = er.x - cardinalityCenter.x + r.height / 5;
                dy = er.y - cardinalityCenter.y - r.height / 2;
            } else if (cardinalityCenter.x > er.x + er.width && cardinalityCenter.y < er.y) {
                dx = er.x + er.width - cardinalityCenter.x - r.height / 5;
                dy = er.y - cardinalityCenter.y - r.height / 2;
            } else if (cardinalityCenter.x < er.x && cardinalityCenter.y > er.y + er.height) {
                dx = er.x - cardinalityCenter.x + r.height / 5;
                dy = er.y + er.height - cardinalityCenter.y + r.height / 2;
            } else if (cardinalityCenter.x > er.x + er.width && cardinalityCenter.y > er.y + er.height) {
                dx = er.x + er.width - cardinalityCenter.x - r.height / 5;
                dy = er.y + er.height - cardinalityCenter.y + r.height / 2;
            } else {
            }
        }
/*	else {
		dx = event.getDx();
		dy = event.getDy();
	}*/
        if (paintedFast) {
            ((PaintableManager) manager).repaintItemFast(this);
            paintedFast = false;
        } else {
            rectangle = getBounds();
        }
        try {
            move(dx, dy, true);
            move(event.getDx(), event.getDy(), true);
            if (rectangle != null) {
                r = rectangle;
                rectangle = null;
                ((PaintableManager) manager).repaintRectangle(r.x, r.y, r.width, r.height);
            }
            ((PaintableManager) manager).repaintItem(this);
        } catch (ItemNotInsideManagerException e) {
        }
    }

    public void moveStrongAddiction(ExMovingEvent event) {
        this.handleExMovingEvent(event);
        this.handleExMoveEvent((ExMoveEvent) event);
    }

    /**
     * This method paints this window.
     *
     * @see cz.green.event.interfaces.PaintableItem#paint(java.awt.Graphics)
     */
    public void paint(java.awt.Graphics g) {
        //paint item
        final Stroke stroke = updateStrokeWithAliasing(g);
        java.awt.Rectangle r = getBounds();
        if (selected) {
            g.setColor(getSelectedBackgroundColor());
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        switch (ACTUAL_NOTATION) {
            case (CHEN):
                g.setColor(getForegroundColor());
                g.fillRect(r.x, r.y, r.width, r.height);
                break;
            case (BINARY):
                if (selected) {
                    g.setColor(getSelectedBackgroundColor());
                    g.fillRect(r.x, r.y, r.width, r.height);
                }
                g.setColor(getForegroundColor());
                java.awt.Point cardinalityCenter = getCenter();
                java.awt.Rectangle er = child.getBounds();

                if (cardinalityCenter.x < er.x && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width, r.y + r.height / 2);
                    g.drawLine(r.x + r.width, r.y + r.height / 2 - r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.width, r.y + r.height / 2 + r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.width - r.height / 2, r.y + r.height / 2 - r.height / 6, r.x + r.width - r.height / 2, r.y + r.height / 2 + r.height / 6);
//					g.drawString(name, r.x, r.y + r.height);
                    paintLineToCardinality(g, false);
                } else
                if (cardinalityCenter.x > (er.x + er.width) && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height / 2);
                    g.drawLine(r.x, r.y + r.height / 2 - r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x, r.y + r.height / 2 + r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.height / 2, r.y + r.height / 2 - r.height / 6, r.x + r.height / 2, r.y + r.height / 2 + r.height / 6);
//						g.drawString(name, r.x + (int) ((r.width - fm.stringWidth(name))), r.y + r.height);
                    paintLineToCardinality(g, false);
                } else if (cardinalityCenter.y < er.y) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height);
                    g.drawLine(r.x + r.width / 2 - r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                    g.drawLine(r.x + r.width / 2 + r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                    g.drawLine(r.x + r.width / 2 - r.height / 6, r.y + r.height / 2 + 1, r.x + r.width / 2 + r.height / 6, r.y + r.height / 2 + 1);
//							g.drawString(name, r.x + (int) ((r.width - fm.stringWidth(name)) / 2), r.y + fm.getAscent());
                    paintLineToCardinality(g, true);
                } else if (cardinalityCenter.y > (er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y, r.x + r.width / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.width / 2 - r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                    g.drawLine(r.x + r.width / 2 + r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                    g.drawLine(r.x + r.width / 2 - r.height / 6, r.y + r.height / 2, r.x + r.width / 2 + r.height / 6, r.y + r.height / 2);
//								g.drawString(name, r.x + (int) ((r.width - fm.stringWidth(name)) / 2), r.y + r.height);
                    paintLineToCardinality(g, false);
                } else {
                    g.drawRect(r.x, r.y, r.width, r.height);
                }
                break;
            case (UML):
                g.setColor(getForegroundColor());
                g.fillRect(r.x, r.y, r.width, r.height);
                break;
        }
        updateBackupStroke(g, stroke);
    }

    private void paintLineToCardinality(java.awt.Graphics g, boolean UP) {
        java.awt.Rectangle r = getBounds();
        Point rcenter = parent.getRealCenter();
        Point ccenter = getRealCenter();
        int dx = rcenter.x - ccenter.x;
        int dy = ccenter.y - rcenter.y;
        float konst = (float) dy / (float) dx;
        // na horni stenu
        if (dy > 0 && (((r.width / 2) * konst) > r.height / 2 || ((r.width / 2) * konst) < -r.height / 2)) {
            g.drawLine(r.x + r.width / 2, r.y + r.height / 2, (int) (r.x + r.width / 2 + (r.height / 2) / konst), r.y);
        } else if (dy < 0 && (((r.width / 2) * konst) > r.height / 2 || ((r.width / 2) * konst) < -r.height / 2)) {
            g.drawLine(r.x + r.width / 2, r.y + r.height / 2, (int) (r.x + r.width / 2 - (r.height / 2) / konst), r.y + r.height);
        } else {
            if (dx > 0)
                g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width, (int) (r.y + r.height / 2 - (r.width / 2) * konst));
            if (dx < 0)
                g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x, (int) (r.y + r.height / 2 + (r.width / 2) * konst));
        }
    }
/**
 * Paints window bud only board. Don't fills the entire window area.
 *
 * @see cz.green.event.interfaces.PaintableItem#paintFast(java.awt.Graphics)
 */
//public void paintFast(java.awt.Graphics g) {

    //}

    /**
     * Prints the window. Exists because of implementing the interface Printable.
     *
     * @see Printable#print(java.awt.Graphics)
     */
    public void print(java.awt.Graphics g) {
        java.awt.Rectangle r = getBounds();
        switch (ACTUAL_NOTATION) {
            case (CHEN):
                g.setColor(getForegroundColor());
                g.fillRect(r.x, r.y, r.width, r.height);
                break;
            case (BINARY):
                g.setColor(getForegroundColor());
                java.awt.Point cardinalityCenter = getCenter();
                java.awt.Rectangle er = child.getBounds();

                if (cardinalityCenter.x < er.x && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width, r.y + r.height / 2);
                    g.drawLine(r.x + r.width, r.y + r.height / 2 - r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.width, r.y + r.height / 2 + r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.width - r.height / 2, r.y + r.height / 2 - r.height / 6, r.x + r.width - r.height / 2, r.y + r.height / 2 + r.height / 6);
//				g.drawString(name, r.x, r.y + r.height);
                    paintLineToCardinality(g, false);
                } else
                if (cardinalityCenter.x > (er.x + er.width) && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height / 2);
                    g.drawLine(r.x, r.y + r.height / 2 - r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x, r.y + r.height / 2 + r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.height / 2, r.y + r.height / 2 - r.height / 6, r.x + r.height / 2, r.y + r.height / 2 + r.height / 6);
//					g.drawString(name, r.x + (int) ((r.width - fm.stringWidth(name))), r.y + r.height);
                    paintLineToCardinality(g, false);
                } else if (cardinalityCenter.y < er.y) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height);
                    g.drawLine(r.x + r.width / 2 - r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                    g.drawLine(r.x + r.width / 2 + r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                    g.drawLine(r.x + r.width / 2 - r.height / 6, r.y + r.height / 2 + 1, r.x + r.width / 2 + r.height / 6, r.y + r.height / 2 + 1);
//						g.drawString(name, r.x + (int) ((r.width - fm.stringWidth(name)) / 2), r.y + fm.getAscent());
                    paintLineToCardinality(g, true);
                } else if (cardinalityCenter.y > (er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y, r.x + r.width / 2, r.y + r.height / 2);
                    g.drawLine(r.x + r.width / 2 - r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                    g.drawLine(r.x + r.width / 2 + r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                    g.drawLine(r.x + r.width / 2 - r.height / 6, r.y + r.height / 2, r.x + r.width / 2 + r.height / 6, r.y + r.height / 2);
//							g.drawString(name, r.x + (int) ((r.width - fm.stringWidth(name)) / 2), r.y + r.height);
                    paintLineToCardinality(g, false);
                } else {
                    g.drawRect(r.x, r.y, r.width, r.height);
                }
                break;
            case (UML):
                g.setColor(getForegroundColor());
                g.fillRect(r.x, r.y, r.width, r.height);
                break;
        }
        r = null;
    }

    /**
     * Removes strong addiction parent. It means removing connection between the parent and the primary key.
     *
     * @param ent The removing strong addiction parent.
     */
    public void reconnectStrongAddictionChild(UniqueKey uk) {
        try {
            cz.omnicom.ermodeller.conceptual.Entity parent = (cz.omnicom.ermodeller.conceptual.Entity) getEntity().getModel();
            cz.omnicom.ermodeller.conceptual.Entity oldChild = (cz.omnicom.ermodeller.conceptual.Entity) getUniqueKey().getOwner().getModel();
            cz.omnicom.ermodeller.conceptual.Entity newChild = (cz.omnicom.ermodeller.conceptual.Entity) uk.getOwner().getModel();
            oldChild.removeStrongAddictionParent(parent);
            newChild.addStrongAddictionParent(parent);
            Connection c = connectionTo(getUniqueKey());
            if (c.getOne() == getUniqueKey()) {
                c.setOne(uk);
                return;
            }
            if (c.getTwo() == getUniqueKey()) {
                c.setTwo(uk);
            }
        } catch (Throwable x) {
            ShowException d = new ShowException(null, "Error", x, true);
        }
    }

    /**
     * Removes strong addiction parent. It means removing connection between the parent and the primary key.
     *
     * @param ent The removing strong addiction parent.
     */
    public void reconnectStrongAddictionParent(Entity ent) {
        try {
            cz.omnicom.ermodeller.conceptual.Entity oldParent = (cz.omnicom.ermodeller.conceptual.Entity) getEntity().getModel();
            cz.omnicom.ermodeller.conceptual.Entity newParent = (cz.omnicom.ermodeller.conceptual.Entity) ent.getModel();
            cz.omnicom.ermodeller.conceptual.Entity child = (cz.omnicom.ermodeller.conceptual.Entity) getUniqueKey().getOwner().getModel();
            child.removeStrongAddictionParent(oldParent);
            child.addStrongAddictionParent(newParent);
            Connection c = connectionTo(getEntity());
            if (c.getOne() == getEntity()) {
                c.setOne(ent);
                return;
            }
            if (c.getTwo() == getEntity()) {
                c.setTwo(ent);
            }
        } catch (Throwable x) {
            ShowException d = new ShowException(null, "Error", x, true);
        }
    }

    /**
     * Writes data for stromg addiction into XML file
     *
     * @param pw java.io.PrintWriter
     */
    public void write(java.io.PrintWriter pw) {
        pw.println("\t<strong>");
        pw.print("\t\t<left>");
        pw.print(rect[0][0]);
        pw.println("</left>");
        pw.print("\t\t<top>");
        pw.print(rect[1][0]);
        pw.println("</top>");
        pw.print("\t\t<width>");
        pw.print(rect[0][1] - rect[0][0]);
        pw.println("</width>");
        pw.print("\t\t<height>");
        pw.print(rect[1][1] - rect[1][0]);
        pw.println("</height>");
        cz.omnicom.ermodeller.conceptual.Entity entPar = (cz.omnicom.ermodeller.conceptual.Entity) parent.getModel();
        cz.omnicom.ermodeller.conceptual.Entity entChild = (cz.omnicom.ermodeller.conceptual.Entity) child.getModel();
        pw.println("\t\t<ent>" + entPar.getID() + "</ent>");
        pw.println("\t\t<child>" + entChild.getID() + "</child>");
        pw.println("\t</strong>");
    }

    public cz.green.ermodeller.Entity getParent() {
        return parent;
    }

    public cz.green.ermodeller.Entity getChild() {
        return child;
    }
}
