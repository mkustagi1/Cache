package cache;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import cache.docking.javadocking.DockingAlignmentDisplay;
import cache.util.CursorToolkitOne;
import cache.workers.AlignmentWorker;
import cache.workers.SummaryWorker;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Manjunath Kustagi
 */
public class GeneSearchForm extends JPanel {

    static AlignmentWorker alignmentWorker;

    public GeneSearchForm() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        dialog = new JDialog(topFrame, false);
//        dialog = new JDialog();
        dialog.setResizable(true);
        initComponents();

        alignmentWorker = new AlignmentWorker();

        searchBox.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent de) {
                searchBoxDocumentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                searchBoxDocumentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                searchBoxDocumentChanged();
            }
        });
        dialog.setFocusableWindowState(false);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);
        dialog.setLocationRelativeTo(this);
        Action actionListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.setVisible(false);
            }
        };
        this.getActionMap().put("ESCAPE", actionListener);

        DefaultListModel model = new DefaultListModel();

        List<String> biotypes = alignmentWorker.getDistinctBiotypes();
        Collections.sort(biotypes);
        biotypes.remove("-");
        model.addElement("All");

        biotypes.stream().forEach((biotype) -> {
            model.addElement(biotype);
        });

        biotypesList.setModel(model);
        form = this;
    }

    PopUpJList ppjl;
    int oldX = 0;
    int oldY = 0;
    GeneSearchForm form;

    public void addDragListeners(final ParclipFrameForm frame) {
        oldX = frame.getX();
        oldY = frame.getY();

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                int deltaX = e.getComponent().getX() - oldX;
                int deltaY = e.getComponent().getY() - oldY;

                dialog.setLocation(dialog.getLocation().x + deltaX, dialog.getLocation().y + deltaY);
                oldX = e.getComponent().getX();
                oldY = e.getComponent().getY();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                dialog.setVisible(false);
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent we) {
                dialog.setVisible(false);
            }

            @Override
            public void windowDeiconified(WindowEvent we) {
                String text = searchBox.getText();
                if (form.isVisible() && (text != null && text.length() > 2)) {
                    dialog.setVisible(true);
                } else {
                    dialog.setVisible(false);
                }
            }
        });

        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent fe) {
                String text = searchBox.getText();
                if (form.isVisible() && (text != null && text.length() > 2)) {
                    dialog.setVisible(true);
                } else {
                    dialog.setVisible(false);
                }
            }

            @Override
            public void windowLostFocus(WindowEvent fe) {
                dialog.setVisible(false);
            }
        });

        frame.getTabbedPane1().addChangeListener((ChangeEvent e) -> {
            String text = searchBox.getText();
            if (frame.getTabbedPane1().getSelectedComponent() == form && (text != null && text.length() > 2)) {
                dialog.setVisible(true);
            } else {
                dialog.setVisible(false);
            }
        });
    }

    private void searchBoxDocumentChanged() {
        String text = searchBox.getText();
        if (text != null && text.length() >= 2) {
            if (text.length() > 100) {
                text = text.substring(0, 100);
            }
            List<String> symbols = alignmentWorker.getGenesByPartialSymbol(text);
            dialog.getContentPane().removeAll();
            ppjl = new PopUpJList(searchBox, symbols);
            dialog.getContentPane().add(ppjl);
            dialog.setMinimumSize(ppjl.getPreferredSize());
            dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
            if (symbols.size() > 0) {
                dialog.setVisible(true);
            } else {
                dialog.setVisible(false);
            }
            searchBox.requestFocus();
            int cPos = searchBox.getCaretPosition();
            int ht = searchBox.getHeight();
            Point pt = searchBox.getLocationOnScreen();
            int x = pt.x + cPos;
            int y = pt.y + ht;
            dialog.setLocation(x, y);
        } else {
            dialog.setVisible(false);
        }
    }

    private void searchBoxActionPerformed(ActionEvent e) {
        searchButtonActionPerformed(e);
    }

    private void searchButtonActionPerformed(ActionEvent e) {
        final String text = (ppjl.list.getSelectedValue() == null) ? searchBox.getText().trim() : (String) ppjl.list.getSelectedValue();
        searchBox.setText(text);
        dialog.getContentPane().removeAll();
        dialog.setVisible(false);
        final List<String> types = new ArrayList<>();
        String extension = "";
        if (polyA101Box.isSelected()) {
            types.add("PolyA101");
            extension += "PolyA101";
        }
        if (total101Box.isSelected()) {
            types.add("Total101");
            extension += "_Total101";
        }

        if (hydro1924Box.isSelected()) {
            types.add("Hydro19-24");
            extension += "_Hydro19-24";
        }
        if (totalZero101.isSelected()) {
            types.add("TotalZero101");
            extension += "_TotalZero101";
        }
        if (riboPCheckBox.isSelected()) {
            types.add("RiboP50");
            extension += "_RiboP50";
        }
        if (cacheCheckBox.isSelected()) {
            types.add("PAR-CLIP");
            extension += "_PAR-CLIP";
        }
        if (otherBox.isSelected()) {
            types.add("Other");
            extension += "_Other";
        }
        if (exactMatchBox.isSelected()) {
            types.add("_Exact_Match");
            extension += "_Exact_Match";
        }
        if (oneMismatchBox.isSelected()) {
            types.add("_1_Mismatch");
            extension += "_1_Mismatch";
        }
        if (twoMismatchBox.isSelected()) {
            types.add("_2_Mismatch");
            extension += "_2_Mismatch";
        }
        if (singleCellBox.isSelected()) {
            types.add("SingleCell");
            extension += "_SingleCell";            
        }
        if (allDistanceBox.isSelected()) {
            types.add("_All_Distances");
            extension += "_All_Distances";
        }
        String directionality = (String)directionalityBox.getSelectedItem();
        switch (directionality) {
            case "Both":
                types.add("_Directionality-all");
                extension += "_Directionality-all";
                break;
            case "True":
                types.add("_Directional");
                extension += "_Directional";
                break;                        
            default:
                types.add("_Non-Directional");
                extension += "_Non-Directional";
                break;
        }
 
        final String ext = extension;
        CursorToolkitOne.startWaitCursor(dockingAlignmentDisplay1);
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                SummaryWorker worker = new SummaryWorker();
                GeneSearchResultsPanel srp = new GeneSearchResultsPanel();
                System.out.println("types: " + types.toString());
                worker.getSummariesForGenes(text, types);
                srp.setSearchWorker(worker, null);
                String label = "GeneSummary_" + text + "_" + ext;
                srp.setPreferredSize(dockingAlignmentDisplay1.getSize());
                dockingAlignmentDisplay1.addDisplay(label, srp);
                srp.setVisible(true);
                dockingAlignmentDisplay1.repaint();

                return true;
            }

            @Override
            protected void done() {
                CursorToolkitOne.stopWaitCursor(dockingAlignmentDisplay1);
            }

        };
        worker.execute();
    }

    private void searchBoxKeyReleased(KeyEvent ke) {
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                dialog.setVisible(false);
                break;
            case KeyEvent.VK_DOWN:
                if (ppjl != null) {
                    int si = ppjl.list.getSelectedIndex();
                    si = (si < 0) ? 0 : si + 1;
                    ppjl.list.setSelectedIndex(si);
                    ppjl.list.ensureIndexIsVisible(ppjl.list.getSelectedIndex());
                    ppjl.list.requestFocus();
                }   break;
            case KeyEvent.VK_UP:
                if (ppjl != null) {
                    int si = ppjl.list.getSelectedIndex();
                    si -= 1;
                    si = (si < 0) ? 0 : si;
                    ppjl.list.setSelectedIndex(si);
                    ppjl.list.ensureIndexIsVisible(ppjl.list.getSelectedIndex());
                    ppjl.list.requestFocus();
                }   break;
            default:
                break;
        }
        ke.consume();
    }

    private void searchBoxFocusGained(FocusEvent e) {
        int caretPosition = searchBox.getCaretPosition();
        searchBox.setSelectionStart(0);
        searchBox.setSelectionEnd(0);
        searchBox.setCaretPosition(caretPosition);
        String text = searchBox.getText();
        if (text.length() < 2) {
            if (biotypesList.getSelectedValuesList().size() > 0) {
                List b = biotypesList.getSelectedValuesList();
                List<String> biotypes = new ArrayList<>();
                b.stream().forEach((_b) -> {
                    biotypes.add((String) _b);
                });
                List<String> symbols = alignmentWorker.getGenesForBiotypes(biotypes);
                dialog.getContentPane().removeAll();
                ppjl = new PopUpJList(searchBox, symbols);
                dialog.getContentPane().add(ppjl);
                dialog.setMinimumSize(ppjl.getPreferredSize());
                dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
                if (symbols.size() > 0) {
                    dialog.setVisible(true);
                } else {
                    dialog.setVisible(false);
                }
                searchBox.requestFocus();
                int cPos = searchBox.getCaretPosition();
                int ht = searchBox.getHeight();
                Point pt = searchBox.getLocationOnScreen();
                int x = pt.x + cPos;
                int y = pt.y + ht;
                dialog.setLocation(x, y);
            }
        }
    }

    private void thisComponentHidden(ComponentEvent e) {
        dialog.setVisible(false);
    }

    private void thisComponentShown(ComponentEvent e) {
        dialog.setVisible(false);
    }

    private void biotypesListMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            biotypesList.clearSelection();
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        label1 = new JLabel();
        searchBox = new JTextField();
        searchButton = new JButton();
        polyA101Box = new JCheckBox();
        total101Box = new JCheckBox();
        hydro1924Box = new JCheckBox();
        totalZero101 = new JCheckBox();
        riboPCheckBox = new JCheckBox();
        cacheCheckBox = new JCheckBox();
        singleCellBox = new JCheckBox();
        otherBox = new JCheckBox();
        label2 = new JLabel();
        scrollPane2 = new JScrollPane();
        biotypesList = new JList();
        exactMatchBox = new JCheckBox();
        oneMismatchBox = new JCheckBox();
        twoMismatchBox = new JCheckBox();
        allDistanceBox = new JCheckBox();
        panel1 = new JPanel();
        label3 = new JLabel();
        directionalityBox = new JComboBox<>();
        scrollPane1 = new JScrollPane();
        dockingAlignmentDisplay1 = new DockingAlignmentDisplay();

        //======== this ========
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                thisComponentHidden(e);
            }
            @Override
            public void componentShown(ComponentEvent e) {
                thisComponentShown(e);
            }
        });
        setLayout(new FormLayout(
            "57dlu, $lcgap, 119dlu, $lcgap, 46dlu, $lcgap, 56dlu, $lcgap, 60dlu, $lcgap, 62dlu, 4*($lcgap, default), $lcgap, 97dlu:grow",
            "2*(20dlu, $lgap), 26dlu, $lgap, default, $lgap, 310dlu:grow"));

        //---- label1 ----
        label1.setText("Gene Symbol:");
        add(label1, CC.xy(1, 1));

        //---- searchBox ----
        searchBox.addActionListener(e -> searchBoxActionPerformed(e));
        searchBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchBoxKeyReleased(e);
            }
        });
        searchBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                searchBoxFocusGained(e);
            }
        });
        add(searchBox, CC.xy(3, 1));

        //---- searchButton ----
        searchButton.setText("Search");
        searchButton.addActionListener(e -> searchButtonActionPerformed(e));
        add(searchButton, CC.xy(5, 1));

        //---- polyA101Box ----
        polyA101Box.setText("PolyA101");
        polyA101Box.setSelected(true);
        add(polyA101Box, CC.xy(7, 1));

        //---- total101Box ----
        total101Box.setText("Total101");
        add(total101Box, CC.xy(9, 1));

        //---- hydro1924Box ----
        hydro1924Box.setText("small RNA");
        add(hydro1924Box, CC.xy(11, 1));

        //---- totalZero101 ----
        totalZero101.setText("TotalZero101");
        add(totalZero101, CC.xy(13, 1));

        //---- riboPCheckBox ----
        riboPCheckBox.setText("RiboP50");
        add(riboPCheckBox, CC.xy(15, 1));

        //---- cacheCheckBox ----
        cacheCheckBox.setText("PAR-CLIP");
        add(cacheCheckBox, CC.xy(17, 1));

        //---- singleCellBox ----
        singleCellBox.setText("Single Cell");
        add(singleCellBox, CC.xy(19, 1));

        //---- otherBox ----
        otherBox.setText("Other");
        add(otherBox, CC.xy(21, 1));

        //---- label2 ----
        label2.setText("Biotypes:");
        add(label2, CC.xy(1, 3));

        //======== scrollPane2 ========
        {

            //---- biotypesList ----
            biotypesList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    biotypesListMouseClicked(e);
                }
            });
            scrollPane2.setViewportView(biotypesList);
        }
        add(scrollPane2, CC.xywh(3, 3, 3, 4));

        //---- exactMatchBox ----
        exactMatchBox.setText("Exact Match");
        exactMatchBox.setSelected(true);
        add(exactMatchBox, CC.xy(7, 3));

        //---- oneMismatchBox ----
        oneMismatchBox.setText("1 Mismatch");
        add(oneMismatchBox, CC.xy(9, 3));

        //---- twoMismatchBox ----
        twoMismatchBox.setText("2 Mismatches");
        add(twoMismatchBox, CC.xy(11, 3));

        //---- allDistanceBox ----
        allDistanceBox.setText("All distances");
        add(allDistanceBox, CC.xy(13, 3));

        //======== panel1 ========
        {
            panel1.setBorder(LineBorder.createBlackLineBorder());
            panel1.setLayout(new FormLayout(
                "56dlu, $lcgap, 60dlu",
                "26dlu"));

            //---- label3 ----
            label3.setText("Directional:");
            label3.setHorizontalAlignment(SwingConstants.CENTER);
            panel1.add(label3, CC.xy(1, 1));

            //---- directionalityBox ----
            directionalityBox.setModel(new DefaultComboBoxModel<>(new String[] {
                "Both",
                "True",
                "False"
            }));
            panel1.add(directionalityBox, CC.xy(3, 1));
        }
        add(panel1, CC.xywh(7, 5, 3, 1));

        //======== scrollPane1 ========
        {
            scrollPane1.setPreferredSize(new Dimension(1185, 503));

            //---- dockingAlignmentDisplay1 ----
            dockingAlignmentDisplay1.setPreferredSize(new Dimension(1185, 500));
            scrollPane1.setViewportView(dockingAlignmentDisplay1);
        }
        add(scrollPane1, CC.xywh(1, 7, 21, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel label1;
    private JTextField searchBox;
    private JButton searchButton;
    private JCheckBox polyA101Box;
    private JCheckBox total101Box;
    private JCheckBox hydro1924Box;
    private JCheckBox totalZero101;
    private JCheckBox riboPCheckBox;
    private JCheckBox cacheCheckBox;
    private JCheckBox singleCellBox;
    private JCheckBox otherBox;
    private JLabel label2;
    private JScrollPane scrollPane2;
    private JList biotypesList;
    private JCheckBox exactMatchBox;
    private JCheckBox oneMismatchBox;
    private JCheckBox twoMismatchBox;
    private JCheckBox allDistanceBox;
    private JPanel panel1;
    private JLabel label3;
    private JComboBox<String> directionalityBox;
    private JScrollPane scrollPane1;
    private DockingAlignmentDisplay dockingAlignmentDisplay1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    JDialog dialog;

    public class PopUpJList extends JPanel {

        final List<String> data = new ArrayList<>();
        JList list;
        JTextField textField = null;

        public PopUpJList(JTextField tf, List<String> d) {
            textField = tf;
            setLayout(new BorderLayout());
            data.clear();
            data.addAll(d);
            DefaultListModel listModel = new DefaultListModel();
            for (String element : data) {
                listModel.addElement(element);
            }
            list = new JList(listModel);
            add(new JScrollPane(list), "Center");

            list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    if (!list.isSelectionEmpty()
                            && list.locationToIndex(me.getPoint())
                            == list.getSelectedIndex()) {
                        textField.setText(data.get(list.getSelectedIndex()));
                        dialog.setVisible(false);
                    }
                }
            });

            list.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent ke) {
                    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (!list.isSelectionEmpty()) {
                            textField.setText(data.get(list.getSelectedIndex()));
                            dialog.setVisible(false);
                        }
                        searchBox.grabFocus();
                        searchBox.requestFocus();
                        ke.consume();
                    }
                }
            });

            list.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent ke) {
                    if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        ((DefaultListModel) list.getModel()).clear();
                        dialog.setVisible(false);
                    } else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        searchBox.requestFocus();
                        int caretPosition = searchBox.getCaretPosition();
                        try {
                            searchBox.getDocument().remove(caretPosition - 1, 1);
                        } catch (BadLocationException ex) {
                            Logger.getLogger(GeneSearchForm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (ke.getKeyCode() != KeyEvent.VK_UP && ke.getKeyCode() != KeyEvent.VK_DOWN) {
                        searchBox.requestFocus();
                    }

                    ke.consume();
                }
            });

        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(300, 100);
        }
    }
}
