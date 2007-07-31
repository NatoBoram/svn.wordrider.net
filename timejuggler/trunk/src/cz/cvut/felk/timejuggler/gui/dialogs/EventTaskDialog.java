package cz.cvut.felk.timejuggler.gui.dialogs;

import application.Action;
import application.ResourceMap;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import cz.cvut.felk.timejuggler.core.AppPrefs;
import cz.cvut.felk.timejuggler.swing.ComponentFactory;
import cz.cvut.felk.timejuggler.swing.Swinger;
import cz.cvut.felk.timejuggler.swing.components.EditorPaneLinkDetector;
import cz.cvut.felk.timejuggler.utilities.Browser;
import cz.cvut.felk.timejuggler.utilities.LogUtils;
import org.jdesktop.swingx.JXDatePicker;

import javax.beans.binding.Binding;
import javax.beans.binding.BindingContext;
import javax.swing.*;
import javax.swing.binding.ParameterKeys;
import javax.swing.binding.TextChangeStrategy;
import java.awt.*;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class EventTaskDialog extends AppDialog {
    private final static Logger logger = Logger.getLogger(EventTaskDialog.class.getName());

    private final boolean newEvent;
    private BindingContext context;

    public EventTaskDialog(Frame mainFrame) {
        this(mainFrame, true);
    }

    public EventTaskDialog(Frame mainFrame, boolean newEvent) {
        super(mainFrame, true);
        this.newEvent = newEvent;
        this.setName("EventTaskDialog");
        try {
            initComponents();
            build();
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
    }

    private void build() {
        inject();
        buildGUI();
        buildModels();
        setDefaultValues();
        pack();
        setResizable(true);
        locateOnOpticalScreenCenter(this);


        final ResourceMap resourceMap = getResourceMap();
        if (newEvent) {
            this.setTitle(resourceMap.getString("EventTaskDialog.newevent.title"));
        }
        final ActionMap actionMap = getActionMap();
        btnOK.setAction(actionMap.get("okBtnAction"));
        btnCancel.setAction(actionMap.get("cancelBtnAction"));
        btnVisitURL.setAction(actionMap.get("visitURLAction"));
    }

    private ActionMap getActionMap() {
        return Swinger.getActionMap(this.getClass(), this);
    }

    private void buildGUI() {
        final boolean showMore = AppPrefs.getProperty(AppPrefs.SHOW_MORE_EVENTTASKDIALOG, true);
        showMoreOrLess(showMore, false);

        context = new BindingContext();
//        Binding binding = new Binding(this.dateFromPicker, "${enabled}", checkDate, "selected");
        checkDate = new JCheckBox() {
            public Boolean getSelected() {
                return isSelected();
            }

            public void setSelected(Boolean selected) {
                setSelected(selected.booleanValue());
            }
        };
        Binding binding = new Binding(checkDate, "${selected}", this.dateFromPicker, "enabled");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ);
        context.addBinding(binding);
        binding = new Binding(this.dateToPicker, "${enabled}", checkDueDate, "selected");
        context.addBinding(binding);
        binding = new Binding(this.urlField, "${!empty text}", this.btnVisitURL, "enabled");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ);
        binding.putParameter(ParameterKeys.TEXT_CHANGE_STRATEGY, TextChangeStrategy.ON_TYPE);
        binding.bind();
        //binding.setTargetValueFromSourceValue();        

        //context.addBinding(binding);
        binding = new Binding(this.statusTypeCombo, "${selectedIndex == 4}", this.completedDatePicker, "enabled");
        //binding.setUpdateStrategy(Binding.UpdateStrategy.READ);
        context.addBinding(binding);
        context.bind();
    }

    private void setDefaultValues() {
        checkDate.setSelected(true);
    }

    private void setDefaultValuesTask() {

    }


    private void buildModels() {
        setComboModelFromResource(priorityCombo);
        setComboModelFromResource(privacyCombo);
        setComboModelFromResource(statusCombo);
        setComboModelFromResource(statusTypeCombo);
        setComboModelFromResource(alarmCombo);
        setComboModelFromResource(alarmTimeUnitCombo);
        setComboModelFromResource(alarmBeforeAfterCombo);
    }

    @Action
    public void okBtnAction() {
        doClose();
    }

    @Action
    public void cancelBtnAction() {
        doClose();
    }

    @Override
    public void doClose() {
        context.unbind();
        super.doClose();
    }

    @Action
    public void visitURLAction() {
        Browser.openBrowser(urlField.getText());
    }

    @Action
    public void lessAction() {
        showMoreOrLess(false, true);
    }

    @Action
    public void moreAction() {
        showMoreOrLess(true, true);
        Swinger.inputFocus(descriptionArea);
    }

    private void showMoreOrLess(final boolean showMore, final boolean doPackAndResize) {
        btnLessMore.setAction(getActionMap().get(showMore ? "lessAction" : "moreAction"));
        morePanel.setVisible(showMore);
        AppPrefs.storeProperty(AppPrefs.SHOW_MORE_EVENTTASKDIALOG, showMore);
        if (doPackAndResize)
            this.setSize(this.getSize().width, this.getPreferredSize().height);
    }


    @Override
    protected AbstractButton getCancelButton() {
        return btnCancel;
    }

    @Override
    protected AbstractButton getOkButton() {
        return btnOK;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Open Source Project license - unknown
        JPanel mainPanel = new JPanel();
        JLabel labelTitle = new JLabel();
        titleField = ComponentFactory.getTextField();
        JLabel labelLocation = new JLabel();
        locationField = ComponentFactory.getTextField();
        JLabel labelFrom = new JLabel();
        dateFromPicker = ComponentFactory.getDatePicker();
        timeFromSpinner = ComponentFactory.getTimeSpinner();
        JCheckBox allDayCheckbox = new JCheckBox();
        JLabel labelTo = new JLabel();
        dateToPicker = ComponentFactory.getDatePicker();
        timeToSpinner = ComponentFactory.getTimeSpinner();
        JCheckBox repeatCheckbox = new JCheckBox();
        btnSetPattern = new JButton();
        JPanel panelCalendar = new JPanel();
        labelCalendar = new JLabel();
        calendarCombo = ComponentFactory.getComboBox();
        JLabel labelCategory = new JLabel();
        categoryCombo = ComponentFactory.getComboBox();
        morePanel = new JPanel();
        JLabel labelDescription = new JLabel();
        scrollPaneDescription = new JScrollPane();
        descriptionArea = ComponentFactory.getTextArea();
        JLabel labelAttendees = new JLabel();
        scrollPaneAttendees = new JScrollPane();
        attendeesArea = ComponentFactory.getEmailsEditorPane();
        JLabel labelPrivacy = new JLabel();
        privacyCombo = ComponentFactory.getComboBox();
        JLabel labelPriority = new JLabel();
        priorityCombo = ComponentFactory.getComboBox();
        JLabel labelStatus = new JLabel();
        statusCombo = ComponentFactory.getComboBox();
        JLabel labelAlarm = new JLabel();
        alarmCombo = ComponentFactory.getComboBox();
        panelAlarm = new JPanel();
        alarmTimeSpinner = new JSpinner();
        alarmTimeUnitCombo = ComponentFactory.getComboBox();
        alarmBeforeAfterCombo = ComponentFactory.getComboBox();
        JPanel panelURL = new JPanel();
        JLabel labelURL = new JLabel();
        urlField = ComponentFactory.getTextField();
        btnVisitURL = new JButton();
        panelStatus = new JPanel();
        JLabel labelStatus2 = new JLabel();
        statusTypeCombo = ComponentFactory.getComboBox();
        completedDatePicker = ComponentFactory.getDatePicker();
        percentCompleteSpinner = new JSpinner();
        JLabel labelComplete = new JLabel();
        panelBtn = new JPanel();
        btnOK = new JButton();
        btnCancel = new JButton();
        btnLessMore = new JButton();

        checkDate = new JCheckBox();
        checkDueDate = new JCheckBox();

        CellConstraints cc = new CellConstraints();

        //======== mainPanel ========
        {
            mainPanel.setBorder(Borders.DIALOG_BORDER);
            mainPanel.setName("mainPanel");
            mainPanel.setLayout(new BorderLayout());

            //======== this ========
            {
                this.setName("this");

                //---- labelTitle ----

                labelTitle.setLabelFor(titleField);
                labelTitle.setName("labelTitle");

                //---- titleField ----
                titleField.setName("titleField");

                //---- labelLocation ----

                labelLocation.setLabelFor(locationField);
                labelLocation.setName("labelLocation");

                //---- locationField ----
                locationField.setName("locationField");

                //---- labelFrom ----

                labelFrom.setName("labelFrom");

                //---- dateFromPicker ----
                dateFromPicker.setName("dateFromPicker");

                //---- timeFromSpinner ----
                timeFromSpinner.setName("timeFromSpinner");

                //---- allDayCheckbox ----

                allDayCheckbox.setName("allDayCheckbox");

                //---- labelTo ----

                labelTo.setName("labelTo");

                //---- dateToPicker ----
                dateToPicker.setName("dateToPicker");

                //---- timeToSpinner ----
                timeToSpinner.setName("timeToSpinner");

                //---- checkDate ----
                checkDate.setName("checkDate");

                //---- checkDueDate ----
                checkDueDate.setName("checkDueDate");

                //---- repeatCheckbox ----

                repeatCheckbox.setName("repeatCheckbox");

                //---- btnSetPattern ----

                btnSetPattern.setName("btnSetPattern");

                //======== panelCalendar ========
                {
                    panelCalendar.setName("panelCalendar");

                    //---- labelCalendar ----

                    labelCalendar.setLabelFor(calendarCombo);
                    labelCalendar.setName("labelCalendar");

                    //---- calendarCombo ----
                    calendarCombo.setName("calendarCombo");

                    //---- labelCategory ----

                    labelCategory.setLabelFor(categoryCombo);
                    labelCategory.setName("labelCategory");

                    //---- categoryCombo ----
                    categoryCombo.setName("categoryCombo");

                    PanelBuilder panelCalendarBuilder = new PanelBuilder(new FormLayout(
                            new ColumnSpec[]{
                                    new ColumnSpec(Sizes.dluX(45)),
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.PREF_COLSPEC,
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                                    FormFactory.DEFAULT_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.DEFAULT_COLSPEC
                            },
                            RowSpec.decodeSpecs("default")), panelCalendar);

                    panelCalendarBuilder.add(labelCalendar, cc.xy(1, 1));
                    panelCalendarBuilder.add(calendarCombo, cc.xy(3, 1));
                    panelCalendarBuilder.add(labelCategory, cc.xy(5, 1));
                    panelCalendarBuilder.add(categoryCombo, cc.xy(7, 1));
                }

                //======== morePanel ========
                {
                    morePanel.setName("morePanel");

                    //---- labelDescription ----
                    labelDescription.setLabelFor(descriptionArea);
                    labelDescription.setName("labelDescription");

                    //======== scrollPaneDescription ========
                    {
                        scrollPaneDescription.setName("scrollPaneDescription");

                        //---- descriptionArea ----
                        descriptionArea.setRows(5);
                        descriptionArea.setName("descriptionArea");
                        scrollPaneDescription.setViewportView(descriptionArea);
                    }

                    //---- labelAttendees ----

                    labelAttendees.setLabelFor(attendeesArea);
                    labelAttendees.setName("labelAttendees");

                    //======== scrollPaneAttendees ========
                    {
                        scrollPaneAttendees.setName("scrollPaneAttendees");

                        attendeesArea.setName("attendeesArea");
                        scrollPaneAttendees.setViewportView(attendeesArea);
                    }

                    //---- labelPrivacy ----
                    labelPrivacy.setLabelFor(privacyCombo);
                    labelPrivacy.setName("labelPrivacy");

                    //---- privacyCombo ----
                    privacyCombo.setName("privacyCombo");

                    //---- labelPriority ----
                    labelPriority.setLabelFor(priorityCombo);
                    labelPriority.setName("labelPriority");

                    //---- priorityCombo ----
                    priorityCombo.setName("priorityCombo");

                    //---- labelStatus ----
                    labelStatus.setLabelFor(statusCombo);
                    labelStatus.setName("labelStatus");

                    //---- statusCombo ----
                    statusCombo.setName("statusCombo");

                    //---- labelAlarm ----
                    labelAlarm.setLabelFor(alarmCombo);
                    labelAlarm.setName("labelAlarm");

                    //---- alarmCombo ----
                    alarmCombo.setName("alarmCombo");

                    //======== panelAlarm ========
                    {
                        panelAlarm.setName("panelAlarm");

                        //---- alarmTimeSpinner ----
                        alarmTimeSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
                        alarmTimeSpinner.setName("alarmTimeSpinner");

                        //---- alarmTimeUnitCombo ----
                        alarmTimeUnitCombo.setName("alarmTimeUnitCombo");

                        //---- alarmBeforeAfterCombo ----
                        alarmBeforeAfterCombo.setName("alarmBeforeAfterCombo");

                        PanelBuilder panelAlarmBuilder = new PanelBuilder(new FormLayout(
                                new ColumnSpec[]{
                                        new ColumnSpec("max(pref;23dlu)"),
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        FormFactory.DEFAULT_COLSPEC,
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        FormFactory.DEFAULT_COLSPEC
                                },
                                RowSpec.decodeSpecs("default")), panelAlarm);

                        panelAlarmBuilder.add(alarmTimeSpinner, cc.xywh(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                        panelAlarmBuilder.add(alarmTimeUnitCombo, cc.xy(3, 1));
                        panelAlarmBuilder.add(alarmBeforeAfterCombo, cc.xy(5, 1));
                    }

                    //======== panelURL ========
                    {
                        panelURL.setName("panelURL");

                        //---- labelURL ----

                        labelURL.setLabelFor(urlField);
                        labelURL.setName("labelURL");

                        //---- urlField ----
                        urlField.setName("urlField");

                        //---- btnVisitURL ----

                        btnVisitURL.setName("btnVisitURL");

                        PanelBuilder panelURLBuilder = new PanelBuilder(new FormLayout(
                                new ColumnSpec[]{
                                        new ColumnSpec(Sizes.dluX(45)),
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        FormFactory.PREF_COLSPEC
                                },
                                RowSpec.decodeSpecs("default")), panelURL);

                        panelURLBuilder.add(labelURL, cc.xy(1, 1));
                        panelURLBuilder.add(urlField, cc.xy(3, 1));
                        panelURLBuilder.add(btnVisitURL, cc.xywh(5, 1, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
                    }

                    //======== panelStatus ========
                    {
                        panelStatus.setName("panelStatus");

                        //---- labelStatus2 ----
                        labelStatus2.setName("labelStatus2");

                        //---- statusTypeCombo ----
                        statusTypeCombo.setName("statusTypeCombo");

                        //---- completedDatePicker ----
                        completedDatePicker.setName("completedDatePicker");

                        //---- percentCompleteSpinner ----
                        percentCompleteSpinner.setModel(new SpinnerNumberModel(100, 0, 100, 25));
                        percentCompleteSpinner.setName("percentCompleteSpinner");

                        //---- labelComplete ----
                        labelComplete.setName("labelComplete");

                        PanelBuilder panelStatusBuilder = new PanelBuilder(new FormLayout(
                                new ColumnSpec[]{
                                        new ColumnSpec(Sizes.dluX(45)),
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        FormFactory.PREF_COLSPEC,
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        ComponentFactory.DATEPICKER_COLUMN_SPEC,
                                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                        new ColumnSpec("max(pref;20dlu)"),
                                        new ColumnSpec(ColumnSpec.LEFT, Sizes.DLUX4, FormSpec.NO_GROW),
                                        FormFactory.DEFAULT_COLSPEC
                                },
                                RowSpec.decodeSpecs("default")), panelStatus);

                        panelStatusBuilder.add(labelStatus2, cc.xy(1, 1));
                        panelStatusBuilder.add(statusTypeCombo, cc.xy(3, 1));
                        panelStatusBuilder.add(completedDatePicker, cc.xy(5, 1));
                        panelStatusBuilder.add(percentCompleteSpinner, cc.xywh(7, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
                        panelStatusBuilder.add(labelComplete, cc.xy(9, 1));
                    }

                    PanelBuilder morePanelBuilder = new PanelBuilder(new FormLayout(
                            new ColumnSpec[]{
                                    new ColumnSpec(Sizes.dluX(45)),
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    new ColumnSpec(Sizes.dluX(30)),
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.PREF_COLSPEC
                            },
                            new RowSpec[]{
                                    new RowSpec(RowSpec.TOP, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.PREF_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC,
                                    FormFactory.LINE_GAP_ROWSPEC,
                                    FormFactory.DEFAULT_ROWSPEC
                            }), morePanel);

                    morePanelBuilder.add(labelDescription, cc.xywh(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.TOP));
                    morePanelBuilder.add(scrollPaneDescription, cc.xywh(3, 1, 5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
                    morePanelBuilder.add(labelAttendees, cc.xy(1, 3));
                    morePanelBuilder.add(scrollPaneAttendees, cc.xywh(3, 3, 1, 9));
                    morePanelBuilder.add(labelPrivacy, cc.xy(5, 3));
                    morePanelBuilder.add(privacyCombo, cc.xy(7, 3));
                    morePanelBuilder.add(labelPriority, cc.xy(5, 5));
                    morePanelBuilder.add(priorityCombo, cc.xy(7, 5));
                    morePanelBuilder.add(labelStatus, cc.xy(5, 7));
                    morePanelBuilder.add(statusCombo, cc.xy(7, 7));
                    morePanelBuilder.add(labelAlarm, cc.xy(5, 9));
                    morePanelBuilder.add(alarmCombo, cc.xy(7, 9));
                    morePanelBuilder.add(panelAlarm, cc.xy(7, 11));
                    morePanelBuilder.add(panelURL, cc.xywh(1, 13, 7, 1));
                    morePanelBuilder.add(panelStatus, cc.xywh(1, 15, 7, 1));
                }

                //======== panelBtn ========
                {
                    panelBtn.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                    panelBtn.setName("panelBtn");

                    //---- btnOK ----
                    btnOK.setName("btnOK");

                    //---- btnCancel ----

                    btnCancel.setName("btnCancel");

                    //---- btnLessMore ----

                    btnLessMore.setName("btnLessMore");

                    PanelBuilder panelBtnBuilder = new PanelBuilder(new FormLayout(
                            new ColumnSpec[]{
                                    new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.PREF_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.PREF_COLSPEC,
                                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                    FormFactory.PREF_COLSPEC
                            },
                            RowSpec.decodeSpecs("default")), panelBtn);
                    ((FormLayout) panelBtn.getLayout()).setColumnGroups(new int[][]{{3, 5, 7}});

                    panelBtnBuilder.add(btnOK, cc.xy(3, 1));
                    panelBtnBuilder.add(btnCancel, cc.xy(5, 1));
                    panelBtnBuilder.add(btnLessMore, cc.xy(7, 1));
                }

                PanelBuilder builder = new PanelBuilder(new FormLayout(
                        new ColumnSpec[]{
                                new ColumnSpec(Sizes.dluX(45)),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.PREF_COLSPEC,
                                ComponentFactory.DATEPICKER_COLUMN_SPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec("max(pref;35dlu)"),
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.PREF_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                FormFactory.PREF_COLSPEC,
                                FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                                new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW)
                        },
                        new RowSpec[]{
                                FormFactory.PREF_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.PREF_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.PREF_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.PREF_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.LINE_GAP_ROWSPEC,
                                new RowSpec(RowSpec.FILL, Sizes.PREFERRED, FormSpec.DEFAULT_GROW),
                                FormFactory.LINE_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC
                        }), mainPanel);

                builder.add(labelTitle, cc.xy(1, 1));
                builder.add(titleField, cc.xywh(3, 1, 10, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
                builder.add(labelLocation, cc.xy(1, 3));
                builder.add(locationField, cc.xywh(3, 3, 10, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
                builder.add(labelFrom, cc.xy(1, 5));
                builder.add(checkDate, cc.xywh(3, 5, 1, 1, CellConstraints.CENTER, CellConstraints.BOTTOM));
                builder.add(dateFromPicker, cc.xy(4, 5));
                builder.add(timeFromSpinner, cc.xywh(6, 5, 1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
                builder.add(allDayCheckbox, cc.xy(8, 5));
                builder.add(labelTo, cc.xy(1, 7));
                builder.add(checkDueDate, cc.xywh(3, 7, 1, 1, CellConstraints.CENTER, CellConstraints.CENTER));
                builder.add(dateToPicker, cc.xy(4, 7));
                builder.add(timeToSpinner, cc.xywh(6, 7, 1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
                builder.add(repeatCheckbox, cc.xy(8, 7));
                builder.add(btnSetPattern, cc.xy(10, 7));
                builder.add(panelCalendar, cc.xywh(1, 9, 12, 1));
                builder.add(morePanel, cc.xywh(1, 11, 12, 1));
                builder.add(panelBtn, cc.xywh(1, 13, 12, 1));


            }
            this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Open Source Project license - unknown
    private JTextField titleField;
    private JTextField locationField;
    private JCheckBox checkDate;
    private JCheckBox checkDueDate;
    private JXDatePicker dateFromPicker;
    private JSpinner timeFromSpinner;
    private JXDatePicker dateToPicker;
    private JSpinner timeToSpinner;
    private JButton btnSetPattern;
    private JLabel labelCalendar;
    private JComboBox calendarCombo;
    private JComboBox categoryCombo;
    private JPanel morePanel;
    private JScrollPane scrollPaneDescription;
    private JTextArea descriptionArea;
    private JScrollPane scrollPaneAttendees;
    private EditorPaneLinkDetector attendeesArea;
    private JComboBox privacyCombo;
    private JComboBox priorityCombo;
    private JComboBox statusCombo;
    private JComboBox alarmCombo;
    private JPanel panelAlarm;
    private JSpinner alarmTimeSpinner;
    private JComboBox alarmTimeUnitCombo;
    private JComboBox alarmBeforeAfterCombo;
    private JTextField urlField;
    private JButton btnVisitURL;
    private JPanel panelStatus;
    private JComboBox statusTypeCombo;
    private JXDatePicker completedDatePicker;
    private JSpinner percentCompleteSpinner;
    private JPanel panelBtn;
    private JButton btnOK;
    private JButton btnCancel;
    private JButton btnLessMore;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}