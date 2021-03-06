package cz.felk.cvut.erm.eventtool;

import cz.felk.cvut.erm.event.ResizePoint;
import cz.felk.cvut.erm.event.SelectItemEvent;
import cz.felk.cvut.erm.event.SelectItemExEvent;
import cz.felk.cvut.erm.event.exceptions.ImpossibleNegativeValueException;
import cz.felk.cvut.erm.event.exceptions.ItemNotInsideManagerException;
import cz.felk.cvut.erm.event.exceptions.ValueOutOfRangeException;
import cz.felk.cvut.erm.event.interfaces.Item;
import cz.felk.cvut.erm.event.interfaces.Manager;
import cz.felk.cvut.erm.event.interfaces.PaintableItem;
import cz.felk.cvut.erm.eventtool.interfaces.Connection;
import cz.felk.cvut.erm.eventtool.interfaces.ConnectionManager;
import cz.felk.cvut.erm.eventtool.interfaces.Printable;
import cz.felk.cvut.erm.swing.ShowException;

/**
 * This group has special functionality useful by relations and entities. It can't receive any events,
 * scales up and down to exactly fit all its childs. It means that adding items can cause increasing
 * the size and removing decreasing. Or when item is moving the size can be also changed.
 * <p/>
 * When size item (also new) require large item, then it asks its manager to can increase its size.
 * The manager can be also DGroup instance, that it can also increase and so on.
 * <p/>
 * Its interface is the same as the interface of the others groups. But as add-on it implements the
 * <code>ConnectionManager</code> iterface to can hold the connections. Therefore has also atribute
 * <code>connections</code>, where holds local connections.
 */
public class DGroupTool extends GroupTool implements ConnectionManager {
    /**
     * Can hold the local connections between items inside this group
     */
    protected ConnectionGroupTool connections = null;

    /**
     * Simply calls the inherited constructor.
     *
     * @see GroupTool#GroupTool(cz.felk.cvut.erm.event.interfaces.Manager , int, int, int, int)
     */
    public DGroupTool(Manager manager, int left, int top, int width, int height) throws NullPointerException, ImpossibleNegativeValueException {
        super(manager, left, top, width, height);
    }

    /**
     * Add new item into this group (manager). First it counts the size of all inserted items
     * (<code>itemsBounds</code>),
     * then counts the new needed size to hold all old items and the new one. Try to resize by method
     * <code>doResize</code>. When all finished alright, then invokes inherited method.
     *
     * @see cz.felk.cvut.erm.event.GroupWindowItem#addItem(cz.felk.cvut.erm.event.interfaces.Item)
     * @see #doResize(int[][])
     * @see #itemsBounds()
     */
    public void addItem(Item item) throws ItemNotInsideManagerException {
        try {
            int[][] h = itemsBounds(), r = new int[2][2];
            //to all tree pointers sets null
            item.countLinks(null);
            //get the item size - i can do this, because tree pointers are null
            r[0][0] = item.mostLeft(0);
            r[0][1] = item.mostRight(0);
            r[1][0] = item.mostLeft(1);
            r[1][1] = item.mostRight(1);
            //counts new size
            if (wins.size() != 0) {
                r[0][0] = (r[0][0] > h[0][0]) ? h[0][0] : r[0][0];
                r[0][1] = (r[0][1] < h[0][1]) ? h[0][1] : r[0][1];
                r[1][0] = (r[1][0] > h[1][0]) ? h[1][0] : r[1][0];
                r[1][1] = (r[1][1] < h[1][1]) ? h[1][1] : r[1][1];
            }
            //try resize
            doResize(r);
            super.addItem(item);
        } //this could not apper, else it is critical situation
        catch (ValueOutOfRangeException x) {
            new ShowException(null, "Error", x, true);
        }
    }

    @Override
    public Manager setManager(Manager manager) throws NullPointerException {
        final Manager m = super.setManager(manager);
        if (connections != null)
            connections.setManager(manager);//TODO hack
        return m;
    }

    /**
     * Adds connection to the local <code>ConnectionGroup</code>.
     *
     * @param conn The Adding connection.
     * @see ConnectionManager#addConnection(cz.felk.cvut.erm.ermodeller.Connection)
     * @see #connections
     */
    public void addConnection(Connection conn) {
        if (connections == null) {
            try {
                //when connection greoup doesn't exists
                connections = new ConnectionGroupTool(manager, 0, 0, 0, 0);
            } catch (ImpossibleNegativeValueException e) {
                //when execution is here -> it is very bad
                return;
            }
        }
        try {
            connections.addItem(conn);
        } catch (ItemNotInsideManagerException e) {
        }
    }

    /**
     * Simply calls the manager method <code>addCommectionToMain</code>.
     *
     * @param conn The adding connection.
     * @see ConnationManager#addConnectionToMain(cz.felk.cvut.erm.eventtool.interfaces.Connection)
     */
    public void addConnectionToMain(Connection conn) {
        ((ConnectionManager) manager).addConnectionToMain(conn);
    }

    /**
     * Recounts all links from the window item to the top of the insered items. Has the same
     * functionality as the inherited one but add only to resizing to all inserted items
     * by invoking the <code>itemsBounds</code> and then <code>doResize</code>.
     *
     * @see #doResize(int[][])
     * @see #itemsBounds()
     */
    public void countAllLinksToTop(Item item) {
        super.countAllLinksToTop(item);
        try {
            if (wins.size() != 0) {
                //resize onlky if there is some items
                doResize(itemsBounds());
            }
            //		(manager).repaintItem(this);
        } catch (ItemNotInsideManagerException e) {
            //when execution is here - very bad
        }
    }

    /**
     * Tries to resize to the specified size. Asks the manager if can expand to the new size,
     * if it can then simply resize and count all links to top.
     *
     * @param r The new size to fit all items.
     * @throws cz.felk.cvut.erm.event.exceptions.ItemNotInsideManagerException
     *          Thrown when manager give no
     *          permition to resize.
     * @see cz.felk.cvut.erm.event.GroupWindowItem#itemMoveDimension(int, int[])
     */
    protected void doResize(int[][] r) throws ItemNotInsideManagerException {
        boolean recount = false, ask0 = false, ask1 = false;
        int[][] h = hRect;
        if (hRect == null)
            h = rect;
        //construct the needed size
        if (r != null) {
            if (h[0][0] != r[0][0]) {
                if (h[0][0] > r[0][0])
                    ask0 = true;
                recount = true;
            }
            if (h[0][1] != r[0][1]) {
                if (h[0][1] < r[0][1])
                    ask0 = true;
                recount = true;
            }
            if (h[1][0] != r[1][0]) {
                if (h[1][0] > r[1][0])
                    ask1 = true;
                recount = true;
            }
            if (h[1][1] != r[1][1]) {
                if (h[1][1] < r[1][1])
                    ask1 = true;
                recount = true;
            }
        }
        if (recount) {
            //ask for permition if required
            if (ask0) {
                manager.itemMoveDimension(0, r[0]);
            }
            if (ask1) {
                manager.itemMoveDimension(1, r[1]);
            }
            //do resize
            rect = r;
            hRect = null;
            //update tree links
            manager.countAllLinksToTop(this);
            //		(manager).repaintItem(this);
        }
    }

    /**
     * Overrides this function to say that Atribute can't be resized - returns null.
     */
    public ResizePoint[] getResizePoints() {
        return null;
    }

    /**
     * This handler simply change z order to top and repaints all items of the group.
     */
    public void handleSelectItemEvent(SelectItemEvent event) {
        manager.changeZOrder(this, true);
        (manager).repaintItem(this);
    }

    /**
     * This handler do nothing.
     */
    public void handleSelectItemExEvent(SelectItemExEvent event) {
    }

    /**
     * Returns, whether this group want the event when its the potencial receiver.
     * Normal group test whether the receiver is some inserted elemented and unless
     * he receives the event itself. But this group don't receives the events. The one change from inherited
     * method is - when the receiver is set to this - no inserted item wants the event -> than return
     * <code>false</code> to not handle the event by this group.
     */
    public boolean isIn(int x, int y) {
        boolean result = super.isIn(x, y);
        if (result && (receiver == this)) {
            //this group don't wants to receive events
            receiver = null;
            return false;
        }
        return result;
    }

    /**
     * Givesthe answer into inserted item - can I move to the new postion. It means when theitem will be also
     * after moving (resizing) inside this group it gives positiove answer. But when it moves otside this group,
     * this groups ask its manager  - can I resise to fit all my items. When manager gives possitive answer, then
     * also gild gets possitive answer and vice versa.
     */
    public void itemMoveDimension(int dimension, int[] interval) throws ItemNotInsideManagerException {
        int[][] r = rect;
        int where;
        try {
            if ((where = cz.felk.cvut.erm.util.IntervalMethods.whereIs(r[dimension], interval)) != cz.felk.cvut.erm.util.IntervalMethods.IN) {
                int[] h = new int[2];
                System.arraycopy(interval, 0, h, 0, 2);
                //count the new size
                if ((where & cz.felk.cvut.erm.util.IntervalMethods.LEFT) == 0)
                    h[0] = r[dimension][0];
                if ((where & cz.felk.cvut.erm.util.IntervalMethods.RIGHT) == 0)
                    h[1] = r[dimension][1];
                //asks for permition to resize
                manager.itemMoveDimension(dimension, h);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ItemNotInsideManagerException();
        } catch (cz.felk.cvut.erm.util.BadDimensionException e) {
            throw new ItemNotInsideManagerException();
        }
    }

    /**
     * Detects the size of all inserted items. Its makes this work by using the methods
     * <code>mostLeft</code> and <code>mostRight</code> methods.
     *
     * @return The size necessary to hold all items. When the group contain no elements, then
     *         returns <code>null</code>.
     * @see cz.felk.cvut.erm.event.WindowItem#mostLeft(int)
     * @see cz.felk.cvut.erm.event.WindowItem#mostRight(int)
     */
    protected int[][] itemsBounds() {
        int[][] r = new int[2][2];
        try {
            Item i = ((Item) wins.lastElement());
            r[0][0] = i.mostLeft(0);
            r[0][1] = i.mostRight(0);
            r[1][0] = i.mostLeft(1);
            r[1][1] = i.mostRight(1);
            return r;
        } catch (ValueOutOfRangeException e) {
            //This is bad, it shoud not appear
            int[][] h = rect;
            r[0][0] = h[0][0];
            r[0][1] = h[0][1];
            r[1][0] = h[1][0];
            r[1][1] = h[1][1];
        } catch (java.util.NoSuchElementException e) {
            //contains no windows
        }
        return r;
    }

    /**
     * Simply paints all inserted elements and connections from connection manager
     * <code>connections</code>..
     *
     * @see #connections
     */
    public void paint(java.awt.Graphics g) {
        //paint all included element
        java.util.Enumeration e = wins.elements();
        while (e.hasMoreElements()) {
            ((PaintableItem) e.nextElement()).paint(g);
        }
        //paint connections
        if (!selected) {
            if (connections != null)
                connections.paint(g);
        }
    }

    /**
     * Paints fast all inserted elements except connections from connection
     * manager <code>connection</code>.
     *
     * @see #connections
     */
    public void paintFast(java.awt.Graphics g) {
        //paint items
        java.util.Enumeration e = wins.elements();
        while (e.hasMoreElements()) {
            ((PaintableItem) e.nextElement()).paintFast(g);
        }
    }

    /**
     * Prints all inserted elements and also all connections from the
     * connection manager <code>connections</code>.
     *
     * @see #connections
     */
    public void print(java.awt.Graphics g) {
        //paint all included element
        java.util.Enumeration e = wins.elements();
        while (e.hasMoreElements()) {
            ((Printable) e.nextElement()).print(g);
        }
        //paint connections
        if (connections != null)
            connections.print(g);
    }

    /**
     * Removes the item from this group. Call the inherited <code>remove</code> and the resize to
     * fit exactly all inserted items. When removing the last element, than the size isn't changed.
     *
     * @see #itemsBounds()
     * @see #doResize(int[][])
     */
    public void remove(Item item) {
        super.remove(item);
        if (wins.size() == 0)
            return;
        int[][] r = itemsBounds();
        try {
            doResize(r);
        } catch (ItemNotInsideManagerException e) {
            //when execution is here - it is bad
        }
    }

    /**
     * Removes connection <code>conn</code> from the local <code>ConnectionGroup</code>.
     *
     * @param conn The removing connection.
     * @see ConnectionManager#removeConnection(cz.felk.cvut.erm.ermodeller.Connection)
     * @see #connections
     */
    public void removeConnection(Connection conn) {
        if (connections != null) {
            connections.remove(conn);
        }
    }

    /**
     * Simply calls the manager method <code>removeCommectionFromMain</code>.
     *
     * @param conn The removing connection.
     * @see ConnationManager#removeConnectionFromMain(cz.felk.cvut.erm.ermodeller.Connection)
     */
    public void removeConnectionFromMain(Connection conn) {
        ((ConnectionManager) manager).removeConnectionFromMain(conn);
    }
}
