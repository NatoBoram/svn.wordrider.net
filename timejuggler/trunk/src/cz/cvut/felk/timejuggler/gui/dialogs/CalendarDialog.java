package cz.cvut.felk.timejuggler.gui.dialogs;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyConnector;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import cz.cvut.felk.timejuggler.db.entity.interfaces.VCalendarEntity;
import cz.cvut.felk.timejuggler.swing.ComponentFactory;
import cz.cvut.felk.timejuggler.swing.Swinger;
import cz.cvut.felk.timejuggler.utilities.LogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class CalendarDialog extends AppDialog {
    private final static Logger logger = Logger.getLogger(CalendarDialog.class.getName());
    private VCalendarEntity calendar;
    private boolean newCalendar;
    private PresentationModel model;
    // private static final String PROPERTY_COLOR = "color";
    private static final String PROPERTY_NAME = "name";

    //TODO pridat ikonu pro dialog
    public CalendarDialog(Frame owner, VCalendarEntity calendarEntity, final boolean isNew) throws HeadlessException {
        super(owner, true);
        this.newCalendar = isNew;
        this.calendar = calendarEntity;
        this.setName("CalendarDialog");

        try {
            initComponents();
            build();
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            doClose(); // pri otevirani vyjimce se dialog neotevre = fatalni chyba
        }
    }


    @Override
    protected AbstractButton getBtnCancel() {
        return btnCancel;
    }

    @Override
    protected AbstractButton getBtnOK() {
        return btnOK;
    }

    private void build() {
        inject();
        buildGUI();
        buildModels();

        final ActionMap actionMap = getActionMap();
        btnOK.setAction(actionMap.get("okBtnAction"));
        btnCancel.setAction(actionMap.get("cancelBtnAction"));

        setDefaultValues();

        pack();
        setResizable(false);
        locateOnOpticalScreenCenter(this);
    }

    private void buildGUI() {
    }

//    private void updateCombo() {
//        comboColor.setEnabled(checkUseColor.isSelected());
//    }

    private void setDefaultValues() {
        //final Color activeColor = (Color) model.getBufferedValue(PROPERTY_COLOR);
        if (!newCalendar) {
            this.setTitle(getResourceMap().getString("CalendarDialog_edit_title"));
        }
        //checkUseColor.setSelected(activeColor != null);
        model.triggerFlush();
        //updateCombo();
    }

    private void buildModels() {
        model = new PresentationModel(calendar, new Trigger());
        Bindings.bind(fieldName, model.getBufferedModel(PROPERTY_NAME), false);
//        final BufferedValueModel valueColorModel = model.getBufferedModel(PROPERTY_COLOR);

//        checkUseColor.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                updateCombo();
//                if (!checkUseColor.isSelected())
//                    category.setColor(null);//nutne zlo
//            }
//        });

        final Action actionOK = getActionMap().get("okBtnAction");
//        PropertyConnector.connectAndUpdate(valueColorModel, comboColor, PROPERTY_COLOR);

        final PropertyConnector connector1 = PropertyConnector.connect(model, PresentationModel.PROPERTYNAME_BUFFERING, actionOK, "enabled");
        connector1.updateProperty2();
    }

    @application.Action
    public void okBtnAction() {
        if (!validateForm()) {
            return;
        }
        model.triggerCommit();
        //workaround
//        if (!checkUseColor.isSelected())
//            category.setColor(null);
        //   DataProvider.getInstance().addCategory(category);
        calendar.setChanged(true);
        setResult(RESULT_OK);
        doClose();
    }

    @application.Action
    public void cancelBtnAction() {
        model.triggerFlush();
        doClose();
    }

    @Override
    public void doClose() {
        if (model != null)
            model.release();
        super.doClose();
    }

    private void initComponents() {
        JPanel dialogPane = new JPanel();
        JPanel contentPanel = new JPanel();
        JLabel labelName = new JLabel();
        fieldName = ComponentFactory.getTextField();
        //checkUseColor = new JCheckBox();
        //comboColor = ComponentFactory.getColorComboBox();
        JPanel buttonBar = new JPanel();
        btnOK = new JButton();
        btnCancel = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== CalendarDialog ========
        {
            Container contentPane = this.getContentPane();
            contentPane.setLayout(new BorderLayout());

            //======== dialogPane ========
            {
                dialogPane.setBorder(Borders.DIALOG_BORDER);
                dialogPane.setName("dialogPane");
                dialogPane.setLayout(new BorderLayout());

                //======== contentPanel ========
                {
                    contentPanel.setName("contentPanel");

                    //---- labelName ----

                    labelName.setLabelFor(fieldName);
                    labelName.setName("labelName");

                    //---- fieldName ----
                    fieldName.setName("fieldName");

                    //---- checkUseColor ----

                    //checkUseColor.setName("checkUseColor");

                    //---- comboColor ----
                    // comboColor.setName("comboColor");

                    PanelBuilder contentPanelBuilder = new PanelBuilder(new FormLayout(
                            new ColumnSpec[]{
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    new ColumnSpec("max(pref;80dlu)")
                            },
                            new RowSpec[]{
                                    FormFactory.PREF_ROWSPEC,
                                    FormFactory.UNRELATED_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC
                            }), contentPanel);

                    contentPanelBuilder.add(labelName, cc.xywh(1, 1, 1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
                    contentPanelBuilder.add(fieldName, cc.xy(3, 1));
//                    contentPanelBuilder.add(checkUseColor, cc.xy(1, 3));
//                    contentPanelBuilder.add(comboColor, cc.xy(3, 3));
                }
                dialogPane.add(contentPanel, BorderLayout.CENTER);

                //======== buttonBar ========
                {
                    buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                    buttonBar.setName("buttonBar");

                    //---- okButton ----

                    btnOK.setName("okButton");

                    //---- cancelButton ----
                    btnCancel.setName("cancelButton");

                    PanelBuilder buttonBarBuilder = new PanelBuilder(new FormLayout(
                            new ColumnSpec[]{
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                                    new ColumnSpec("max(pref;42dlu)"),
                                    FormFactory.RELATED_GAP_COLSPEC,
                                    FormFactory.PREF_COLSPEC
                            },
                            RowSpec.decodeSpecs("pref")), buttonBar);
                    ((FormLayout) buttonBar.getLayout()).setColumnGroups(new int[][]{{2, 4}});

                    buttonBarBuilder.add(btnOK, cc.xy(2, 1));
                    buttonBarBuilder.add(btnCancel, cc.xy(4, 1));
                }
                dialogPane.add(buttonBar, BorderLayout.SOUTH);
            }
            contentPane.add(dialogPane);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    private ActionMap getActionMap() {
        return Swinger.getActionMap(this.getClass(), this);
    }

    private boolean validateForm() {
        final String value = (String) model.getBufferedValue(PROPERTY_NAME);
        return validateNonEmpty(fieldName, value);
    }


    private JTextField fieldName;
    //    private JCheckBox checkUseColor;
    //    private ColorComboBox comboColor;
    private JButton btnOK;
    private JButton btnCancel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

}