package cz.green.eventtool;

import cz.green.event.GroupWindow;
import cz.green.event.exceptions.ImpossibleNegativeValueException;
import cz.green.event.interfaces.Manager;
import cz.green.eventtool.interfaces.Printable;

/**
 * This class has the same functionality as predecessor. Adds only one methods, which is caused by implementing the
 * interface <code>Printable</code>. Implementing this interface caused the possibility to be printed.
 */
public class GroupTool extends GroupWindow implements Printable {
    /**
     * Is needful for creating desktop.
     *
     * @see cz.green.event.GroupWindow#GroupWindow()
     */
    protected GroupTool() {
        super();
    }

    /**
     * Calls the derived constructor.
     *
     * @see cz.green.event.GroupWindow#GroupWindow(cz.green.event.interfaces.Manager , int, int, int, int)
     */
    public GroupTool(Manager manager, int left, int top, int width, int height) throws NullPointerException, ImpossibleNegativeValueException {
        super(manager, left, top, width, height);
    }

    /**
     * Returns rectangle which specifies the size of this window.
     *
     * @see PaintableItem#getBounds()
     */
    public java.awt.Rectangle getPrintBounds() {
        return getBounds();
    }

    /**
     * Prints the group and all inserted groups and windows. Exists because of implementing the interface Printable.
     *
     * @see Printable#print(java.awt.Graphics)
     */
    public void print(java.awt.Graphics g) {
        //paint group
        java.awt.Rectangle r = getBounds();
        g.drawRect(r.x, r.y, r.width, r.height);
        r = null;
        //paint all included element
        java.util.Enumeration e = wins.elements();
        while (e.hasMoreElements()) {
            try {
                ((Printable) e.nextElement()).print(g);
            } catch (Exception ex) {
            }
        }
    }
}
