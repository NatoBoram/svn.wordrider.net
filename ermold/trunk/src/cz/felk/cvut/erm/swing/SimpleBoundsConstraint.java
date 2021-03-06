package cz.felk.cvut.erm.swing;

import java.awt.*;

public class SimpleBoundsConstraint implements BoundsConstraint {
    protected Constraint x = null;
    protected Constraint y = null;

    public SimpleBoundsConstraint(Constraint x, Constraint y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds(Dimension parent) {
        return new Rectangle(x.getStart(parent.width), y.getStart(parent.height),
                x.getSize(parent.width), y.getSize(parent.height));
    }

    public Dimension getParentSize(Dimension compSize) {
        return new Dimension(x.getParentSize(compSize.width),
                y.getParentSize(compSize.height));
    }
}
