package cache;

import cache.docking.DockingAlignmentDisplay;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import org.biojava3.core.sequence.DNASequence;
import org.biojava3.core.sequence.compound.NucleotideCompound;
import org.biojava3.core.sequence.views.ComplementSequenceView;
import org.biojava3.core.sequence.views.ReversedSequenceView;
import cache.util.CursorToolkitOne;
import cache.workers.AlignmentWorker;
import cache.workers.SummaryWorker;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * @author Manjunath Kustagi
 */
public class SequenceSearchWithAlignmentsForm extends JPanel {

    static AlignmentWorker alignmentWorker;

    public SequenceSearchWithAlignmentsForm() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        dialog = new JDialog(topFrame, false);
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
    }

    private void searchBoxDocumentChanged() {
    }

    private void searchBoxActionPerformed(ActionEvent e) {
        searchButtonActionPerformed(e);
    }

    private void searchButtonActionPerformed(ActionEvent e) {
        String text1 = searchBox.getText();
        text1 = text1.replaceAll("\\s", "");
        text1 = text1.replaceAll("(\\r|\\n)", "");
        text1 = text1.replaceAll("\\r\\n", "");
        text1 = text1.toUpperCase();
        text1 = text1.replaceAll("U", "T");
        final String text = text1;
        dialog.getContentPane().removeAll();
        dialog.setVisible(false);
        final List<String> types = new ArrayList<>();
        String extension = "";
        types.add("sequence_search");
        if (polyA101Box.isSelected()) {
            types.add("PolyA101");
            extension += "_PolyA101";
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
        if (allDistanceBox.isSelected()) {
            types.add("_All_Distances");
            extension += "_All_Distances";
        }
        final String ext = extension;
        CursorToolkitOne.startWaitCursor(dockingAlignmentDisplay1);

        SwingWorker<Boolean, Void> sWorker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                SummaryWorker worker = new SummaryWorker();
                GeneSearchResultsPanel srp = new GeneSearchResultsPanel();
                worker.getSummariesForGenes(text, types);
                srp.setSearchWorker(worker, null);
                String substr = text.length() > 10 ? text.substring(0, 10) : text;
                String label = "Matches_" + substr + "_" + ext;
                srp.setPreferredSize(dockingAlignmentDisplay1.getSize());
                dockingAlignmentDisplay1.addDisplay(label, srp);
                srp.setVisible(true);
                dockingAlignmentDisplay1.repaint();
                return true;
            }

            @Override
            protected void done() {
                dockingAlignmentDisplay1.repaint();
                CursorToolkitOne.stopWaitCursor(dockingAlignmentDisplay1);
            }

        };
        sWorker.execute();

    }

    private void searchBoxKeyReleased(KeyEvent ke) {
    }

    private void searchBoxFocusGained(FocusEvent e) {
        int caretPosition = searchBox.getCaretPosition();
        searchBox.setSelectionStart(0);
        searchBox.setSelectionEnd(0);
        searchBox.setCaretPosition(caretPosition);
    }

    private void thisComponentHidden(ComponentEvent e) {
        dialog.setVisible(false);
    }

    private void reverseComplementBoxActionPerformed(ActionEvent e) {
        String sequence = searchBox.getText();
        if (sequence != null) {
            try {
                sequence = sequence.replaceAll("\\s", "");
                DNASequence seq = new DNASequence(sequence);
                org.biojava3.core.sequence.template.Sequence<NucleotideCompound> rc
                        = new ReversedSequenceView<>(
                                new ComplementSequenceView<>(seq));

                String revComp = rc.getSequenceAsString();
                searchBox.setText(revComp);
            } catch (Error er) {
                searchBox.setText("Sequence contains non Nucleotide characters");
            }
        }        // TODO add your code here
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        label1 = new JLabel();
        reverseComplementBox = new JCheckBox();
        scrollPane2 = new JScrollPane();
        searchBox = new JTextArea();
        searchButton = new JButton();
        polyA101Box = new JCheckBox();
        total101Box = new JCheckBox();
        hydro1924Box = new JCheckBox();
        totalZero101 = new JCheckBox();
        exactMatchBox = new JCheckBox();
        oneMismatchBox = new JCheckBox();
        twoMismatchBox = new JCheckBox();
        allDistanceBox = new JCheckBox();
        riboPCheckBox = new JCheckBox();
        cacheCheckBox = new JCheckBox();
        otherBox = new JCheckBox();
        scrollPane1 = new JScrollPane();
        dockingAlignmentDisplay1 = new DockingAlignmentDisplay();

        //======== this ========
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                thisComponentHidden(e);
            }
        });
        setLayout(new FormLayout(
            "57dlu, 2*($lcgap, 46dlu), $lcgap, 56dlu, $lcgap, 60dlu, $lcgap, 67dlu, $lcgap, default, $lcgap, 73dlu, $lcgap, 67dlu:grow",
            "23dlu, 2*($lgap, 20dlu), 3*($lgap, default), $lgap, 310dlu:grow, $lgap, default"));

        //---- label1 ----
        label1.setText("Search Sequence:");
        add(label1, CC.xy(1, 1));

        //---- reverseComplementBox ----
        reverseComplementBox.setText("Transform to Reverse Complement");
        reverseComplementBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reverseComplementBoxActionPerformed(e);
            }
        });
        add(reverseComplementBox, CC.xywh(5, 1, 5, 1));

        //======== scrollPane2 ========
        {
            scrollPane2.setViewportView(searchBox);
        }
        add(scrollPane2, CC.xywh(1, 3, 13, 3));

        //---- searchButton ----
        searchButton.setText("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchButtonActionPerformed(e);
            }
        });
        add(searchButton, CC.xy(15, 5));

        //---- polyA101Box ----
        polyA101Box.setText("PolyA101");
        polyA101Box.setSelected(true);
        add(polyA101Box, CC.xy(1, 7));

        //---- total101Box ----
        total101Box.setText("Total101");
        add(total101Box, CC.xy(3, 7));

        //---- hydro1924Box ----
        hydro1924Box.setText("Hydro19-24");
        add(hydro1924Box, CC.xy(5, 7));

        //---- totalZero101 ----
        totalZero101.setText("TotalZero101");
        add(totalZero101, CC.xy(7, 7));

        //---- exactMatchBox ----
        exactMatchBox.setText("Exact Match");
        exactMatchBox.setSelected(true);
        add(exactMatchBox, CC.xy(9, 7));

        //---- oneMismatchBox ----
        oneMismatchBox.setText("1 Mismatch");
        add(oneMismatchBox, CC.xy(11, 7));

        //---- twoMismatchBox ----
        twoMismatchBox.setText("2 Mismatches");
        add(twoMismatchBox, CC.xy(13, 7));

        //---- allDistanceBox ----
        allDistanceBox.setText("All Distances");
        add(allDistanceBox, CC.xy(15, 7));

        //---- riboPCheckBox ----
        riboPCheckBox.setText("Ribo50");
        add(riboPCheckBox, CC.xy(1, 9));

        //---- cacheCheckBox ----
        cacheCheckBox.setText("PAR-CLIP");
        add(cacheCheckBox, CC.xy(3, 9));

        //---- otherBox ----
        otherBox.setText("Other");
        add(otherBox, CC.xy(5, 9));

        //======== scrollPane1 ========
        {
            scrollPane1.setViewportView(dockingAlignmentDisplay1);
        }
        add(scrollPane1, CC.xywh(1, 11, 17, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel label1;
    private JCheckBox reverseComplementBox;
    private JScrollPane scrollPane2;
    private JTextArea searchBox;
    private JButton searchButton;
    private JCheckBox polyA101Box;
    private JCheckBox total101Box;
    private JCheckBox hydro1924Box;
    private JCheckBox totalZero101;
    private JCheckBox exactMatchBox;
    private JCheckBox oneMismatchBox;
    private JCheckBox twoMismatchBox;
    private JCheckBox allDistanceBox;
    private JCheckBox riboPCheckBox;
    private JCheckBox cacheCheckBox;
    private JCheckBox otherBox;
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
            data.stream().forEach((element) -> {
                listModel.addElement(element);
            });
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
            return new Dimension(100, 100);
        }
    }
}
