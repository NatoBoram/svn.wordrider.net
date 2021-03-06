package cz.cvut.felk.erm.gui.plugintools;

import cz.cvut.felk.erm.gui.managers.interfaces.IFileInstance;
import cz.cvut.felk.erm.gui.managers.interfaces.IInformedTab;

import javax.swing.*;
import java.awt.*;


/**
 * @author Vity
 */
public abstract class PluginTool implements IInformedTab {
    private JPanel tool = new JPanel(new BorderLayout());
    private JPanel contentPane = new JPanel();

    public PluginTool() {
        super();
        //  tool.add(Swinger.getTitleComponent(this.getName()), BorderLayout.NORTH);
        tool.add(contentPane, BorderLayout.CENTER);
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
    }

    Container getContentPane() {
        return contentPane;
    }

    public final JComponent getComponent() {
        return tool;
    }

    abstract public void updateData();

    public void setFileInstance(final IFileInstance instance) {
    }
}
