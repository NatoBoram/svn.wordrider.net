package cz.felk.cvut.erm.dialogs;

import cz.felk.cvut.erm.app.ERModeller;
import cz.felk.cvut.erm.conceptual.NotationType;
import cz.felk.cvut.erm.conceptual.beans.Entity;
import cz.felk.cvut.erm.ermodeller.WorkingDesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Property editor for the <code>DataType</code> beans.
 */
public class ChangeNotationDialog extends JDialog implements java.awt.event.ActionListener {
    /**
     * Datatype to be edited.
     * @see cz.felk.cvut.erm.datatype.DataType
     */
    /**
     * Panel which customizes the <code>dataType</code>.
     *
     * @see cz.felk.cvut.erm.datatype.editor.DataTypePanel
     */
    private JTextArea DescriptionTA = null;
    private final Entity Cent = null;
    final JButton OKbutton = new JButton();
    final JButton CancelButton = new JButton();
    final JLabel DecomposeLabel = new JLabel();
    final JLabel DecomposeLabel2 = new JLabel();
    final JLabel DeleteLabel = new JLabel();
    final JList AtrRList = new JList();
    final JList DelRList = new JList();
    final JLabel ChangeLabel = new JLabel();
    JLabel ChangeFromLabel = new JLabel();
    final JLabel ChangeToLabel = new JLabel();
    final JList TernaryRList = new JList();
    final ERModeller Erm;
    private final NotationType currentNotation;
    private final NotationType nextNotation;

    /**
     * Constructor
     */
/* WARNING: THIS METHOD WILL BE REGENERATED. */
    public ChangeNotationDialog(JFrame owner, ERModeller Erm, NotationType nextNotation, NotationType currentNotation) {
        super(owner, "Changing notation dialog ");
        this.Erm = Erm;
        this.currentNotation = currentNotation;
        this.nextNotation = nextNotation;
        initialize();

    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
    }

    /**
     * Called whenever the part throws an exception.
     *
     * @param exception java.lang.Throwable
     */
    private void handleException(Throwable exception) {

        /* Uncomment the following lines to print uncaught exceptions to stdout */
        System.out.println("--------- UNCAUGHT EXCEPTION ---------");
        exception.printStackTrace(System.out);
    }

    /**
     * Initialize the class.
     */
/* WARNING: THIS METHOD WILL BE REGENERATED. */
    private void initialize() {
        try {
            setName("ChangeNotationDialog");
//		setPreferredSize(new java.awt.Dimension(390, 310));
//		setMinimumSize(new java.awt.Dimension(390, 310));
            setLayout(null);
            setSize(new Dimension(355, 230));

            ChangeLabel.setFont(new java.awt.Font("Dialog", 1, 14));
            ChangeLabel.setText("Change notation from");
            ChangeLabel.setBounds(new Rectangle(15, 9, 160, 20));

            ChangeToLabel.setFont(new java.awt.Font("Dialog", 1, 14));
//      ChangeToLabel.setBounds(new Rectangle(170, 7, 180, 24));
            ChangeToLabel.setBounds(new Rectangle(15, 9, 260, 20));
            ChangeToLabel.setText(getFromToString());

            CancelButton.setBounds(new Rectangle(173, 165, 73, 25));
            CancelButton.setText("Cancel");
            CancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            OKbutton.setBounds(new Rectangle(13, 165, 130, 25));
            OKbutton.setText("Change Notation");
            OKbutton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    switch (nextNotation) {
                        case CHEN:
                            Erm.setChen();
                            break;
                        case BINARY:
                            Erm.setBinary();
                            break;
                        case UML:
                            Erm.setUML();
                            break;
                    }
                    setVisible(false);
                }
            });

//      this.getContentPane().add(ChangeLabel, null);
            this.getContentPane().add(ChangeToLabel, null);

            this.add(getTextArea(), getTextArea().getName());

            if (currentNotation == nextNotation)
                InitializeNoChange();
            else {

                switch (currentNotation) {
                    case CHEN:
                        InitializeChen();
                        break;
                    case BINARY:
                        InitializeBinary();
                        break;
                    case UML:
                        InitializeUML();
                        break;

                }
                this.getContentPane().add(OKbutton, null);
                this.getContentPane().add(CancelButton, null);


            }
        } catch (java.lang.Throwable ivjExc) {
            handleException(ivjExc);
        }
    }

    private void InitializeChen() {
        setSize(new Dimension(399, 335));
        DescriptionTA.setSize(365, 112);
        OKbutton.setBounds(new Rectangle(13, 270, 130, 25));
        CancelButton.setBounds(new Rectangle(173, 270, 73, 25));

        switch (nextNotation) {
            case BINARY:
                DescriptionTA.setText(
                        "If you change the notation to BINARY, all ternary relationships and \n" +
                                "relationships with attributes will be decomposed. All relationships \n" +
                                "without any connection to entity will be deleted!\n\n" +
                                "Press Change notation to switch or Cancel to return back.");
                break;
            case UML:
                DescriptionTA.setText(
                        "If you change the notation to UML, all ternary relationships will be\n" +
                                "decomposed. Relationships with attributes will be also decom-\n" +
                                "posed, because this construct is not implemented in UML in this \n" +
                                "ERM version. All Relationships without any connection to Entity will \n" +
                                "be deleted!\n\n" +
                                "Press Change notation to switch or Cancel to return back.");
                break;
        }

        DecomposeLabel2.setText("Ternary Rels :");
        DecomposeLabel2.setBounds(new Rectangle(5, 158, 157, 20));
        Vector RT = ((WorkingDesktop) Erm.getPlace().getDesktop()).getTernaryRelations(true);
        TernaryRList.setListData(RT);
        TernaryRList.setBounds(new Rectangle(5, 179, 120, 80));
        JScrollPane TernaryRListScroller = new JScrollPane(TernaryRList);
        TernaryRListScroller.setBounds(new Rectangle(5, 179, 120, 80));

        DecomposeLabel.setText("Rels with attributes:");
        DecomposeLabel.setBounds(new Rectangle(135, 158, 157, 20));
        Vector AR = ((WorkingDesktop) Erm.getPlace().getDesktop()).getRelationsWithAttribute(true);
        AtrRList.setListData(AR);
        AtrRList.setBounds(new Rectangle(135, 175, 120, 80));
        JScrollPane AtrRListScroller = new JScrollPane(AtrRList);
        AtrRListScroller.setBounds(new Rectangle(135, 179, 120, 80));

        DeleteLabel.setBounds(new Rectangle(265, 158, 120, 20));
        DeleteLabel.setText("Rels without 2 conns:");
        Vector DR = ((WorkingDesktop) Erm.getPlace().getDesktop()).getRelationsWithoutConnection(true);
        DelRList.setListData(DR);
        DelRList.setBounds(new Rectangle(265, 189, 120, 80));
        JScrollPane DelRListScroller = new JScrollPane(DelRList);
        DelRListScroller.setBounds(new Rectangle(265, 179, 120, 80));

        this.getContentPane().add(DecomposeLabel, null);
        this.getContentPane().add(DecomposeLabel2, null);
        this.getContentPane().add(DeleteLabel, null);

        this.getContentPane().add(TernaryRListScroller, null);
        this.getContentPane().add(AtrRListScroller, null);
        this.getContentPane().add(DelRListScroller, null);
    }

    private void InitializeBinary() {
        switch (nextNotation) {
            case CHEN:
                DescriptionTA.setText("There are no restrictions for changing notation\n from BINARY to CHAN.\n\n" +
                        "Press Change notation to switch or Cancel to return back.");
                break;
            case UML:
                DescriptionTA.setText("There are no restrictions for changing notation\n from BINARY to UML.\n\n" +
                        "Press Change notation to switch or Cancel to return back.");
                break;
        }
    }

    private void InitializeUML() {
        switch (nextNotation) {
            case CHEN:
                DescriptionTA.setText("There are no restrictions for changing notation\n from UML to CHAN.\n\n" +
                        "Press Change notation to switch or Cancel to return back.");
                break;
            case BINARY:
                DescriptionTA.setText("There are no restrictions for changing notation \n " +
                        "from UML to BINARY.\n" +
                        "(because Relationships with atributes are not implemented yet)\n\n" +
                        "Press Change notation to switch or Cancel to return back.");
                break;
        }
    }

    private void InitializeNoChange() {
        CancelButton.setBounds(new Rectangle(143, 165, 73, 25));
        CancelButton.setText("Close");
        DescriptionTA.setText("You are already using this graphic notation. \n\nPress Close to return.");
        this.getContentPane().add(CancelButton, null);
    }

    private String getFromToString() {
        String fromToString;
        fromToString = "From ";
        switch (this.currentNotation) {
            case CHEN:
                fromToString += "CHEN";
                break;
            case BINARY:
                fromToString += "BINARY";
                break;
            case UML:
                fromToString += "UML";
                break;
        }
        fromToString += " to ";
        switch (nextNotation) {
            case CHEN:
                fromToString += "CHEN";
                break;
            case BINARY:
                fromToString += "BINARY";
                break;
            case UML:
                fromToString += "UML";
                break;
        }
        fromToString += " switch";
        return fromToString;
    }

    /**
     * Return the DataTypePanel property value.
     *
     * @return javax.swing.JPanel
     */
    private javax.swing.JTextArea getTextArea() {
        if (DescriptionTA == null) {
            try {
                DescriptionTA = new javax.swing.JTextArea();
                DescriptionTA.setName("Description of notation change");
                DescriptionTA.setOpaque(true);
                DescriptionTA.setEditable(false);
                DescriptionTA.setLayout(null);
                DescriptionTA.setBounds(new Rectangle(12, 40, 326, 112));
                DescriptionTA.setBackground(SystemColor.text);
                DescriptionTA.setEnabled(true);
                DescriptionTA.setText("If you will change the notation ...");
            } catch (java.lang.Throwable ivjExc) {
                handleException(ivjExc);
            }
        }
        return DescriptionTA;
    }

    /**
     * @return false.
     */
    public boolean isPaintable() {
        return false;
    }


    /**
     * Sets the datatype to be edited.
     *
     * @param java.lang.Object value
     */
    public synchronized void setValue(Object value) {
        Cent.setConstraints((String) value);
    }

    public String getValue() {

        //value = getValue();
        return Cent.getConstraints();
    }

}