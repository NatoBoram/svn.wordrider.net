package cz.cvut.felk.erm.swing;

import com.jgoodies.forms.layout.ColumnSpec;
import cz.cvut.felk.erm.swing.components.ColorComboBox;
import cz.cvut.felk.erm.swing.components.EditorPaneLinkDetector;
import cz.cvut.felk.erm.swing.models.NaiiveComboModel;
import cz.cvut.felk.erm.swing.renderers.ComboBoxRenderer;
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Trida slouzici k instanciovani upravenych zakladnich komponent
 *
 * @author Ladislav Vitasek
 */

public class ComponentFactory {
    private FocusListener focusListener;

    private static ComponentFactory instance;
    public final static ColumnSpec DATEPICKER_COLUMN_SPEC = new ColumnSpec("max(pref;65dlu)");
    public static final ColumnSpec BUTTON_COLSPEC = new ColumnSpec("max(pref;42dlu)");

    private synchronized static ComponentFactory getInstance() {
        if (instance == null) {
            instance = new ComponentFactory();
        }
        return instance;
    }

    private ComponentFactory() {
        focusListener = new SelectAllOnFocusListener();
    }

    private FocusListener getFocusListener() {
        return focusListener;
    }

    public static JSpinner getTimeSpinner() {
        final JSpinner spinner = new JSpinner(new SpinnerDateModel());
        ((JSpinner.DateEditor) spinner.getEditor()).getTextField().setFormatterFactory(new DefaultFormatterFactory(new DateFormatter(DateFormat.getTimeInstance(DateFormat.SHORT))));
        return spinner;
    }

    public static JXDatePicker getDatePicker() {
        final JXDatePicker picker = new JXDatePicker();
        picker.setFormats(new SimpleDateFormat(Swinger.getResourceMap().getString("shortDateFormat")));
        return picker;
    }

    public static JComboBox getComboBox() {
        JComboBox combo = new JComboBox(new NaiiveComboModel());
        combo.setRenderer(new ComboBoxRenderer());
        return combo;
    }

    public static ColorComboBox getColorComboBox() {
        return new ColorComboBox();
    }

    public static JTextArea getTextArea() {
        final JTextArea textArea = new JTextArea();
        textArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return textArea;
    }

    public static JTextField getTextField() {
        final JTextField field = new JTextField();
        field.addFocusListener(ComponentFactory.getInstance().getFocusListener());
        return field;
    }


    public static EditorPaneLinkDetector getEmailsEditorPane() {
        return new EditorPaneLinkDetector();
    }


    /**
     * Focus listener pouzivany v textovych komponentach. Na vstup do komponenty vybere celej text.
     */
    public static final class SelectAllOnFocusListener implements FocusListener {
        public final void focusGained(final FocusEvent e) {
            if (!e.isTemporary()) {
                //final Component component = ;
                ((JTextComponent) e.getComponent()).selectAll();
            }
        }

        public final void focusLost(final FocusEvent e) {
        }
    }
}
