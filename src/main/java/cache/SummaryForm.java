package cache;

import java.awt.*;
import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.DefaultTableCellRenderer;
import cache.dataimportes.holders.DataStatisticsResults;
import cache.dataimportes.holders.ExperimentResult;
import cache.dataimportes.holders.SummaryResults;
import cache.docking.javadocking.DockingAlignmentDisplay;
import cache.util.CursorToolkitOne;
import cache.util.StatUtil;
import cache.util.TooltipTable;
import cache.workers.ExperimentTableWorker;
import cache.workers.SummaryWorker;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Manjunath Kustagi
 */
public class SummaryForm extends JPanel {

    ExperimentTableModel etm;

    enum Order {

        FIRST, SECOND, HIGHER
    };

    class Triplet {

        int key;
        long eid;
        int distance;

        Triplet(int k, long e, int d) {
            key = k;
            eid = e;
            distance = d;
        }

        @Override
        public String toString() {
            return "eid: " + eid + ", distance: " + distance;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 19 * hash + this.key;
            hash = 19 * hash + (int) (this.eid ^ (this.eid >>> 32));
            hash = 19 * hash + this.distance;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Triplet other = (Triplet) obj;
            if (this.key != other.key) {
                return false;
            }
            if (this.eid != other.eid) {
                return false;
            }
            if (this.distance != other.distance) {
                return false;
            }
            return true;
        }
        
    }

    final Map<Order, Triplet> selectedExperiments = new HashMap<>();
    final List<Triplet> allSelectedExperiments = new ArrayList<>();

    public SummaryForm() {
        initComponents();

        table1.setRowSelectionAllowed(true);
        table1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        ExperimentTableWorker expWorker = new ExperimentTableWorker();
        etm = new ExperimentTableModel(statusLabel);
        etm.setDataUploadSource(expWorker);
        table1.setModel(etm);
        etm.addTableModelListener(table1);
        table1.setDefaultRenderer(Object.class, new SummaryTableCellRenderer());
        table1.getRowSorter().addRowSorterListener((RowSorterEvent e) -> {
            clearSelections();
        });

        panel1.remove(scrollPane1);

        scrollPane1 = ExperimentTableModel.createPagingScrollPaneForTable(table1);
        scrollPane1.setViewportView(table1);
        panel1.add(scrollPane1, CC.xywh(1, 11, 3, 1, CC.FILL, CC.FILL));
    }

    private void table1MouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1 && !e.isPopupTrigger() && !e.isMetaDown()) {
            int row = table1.rowAtPoint(e.getPoint());
            int row1 = table1.convertRowIndexToModel(row);
            final ExperimentResult er = etm.getExperiment(row1);
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        final SummaryWorker worker = new SummaryWorker();
                        DataStatisticsResults stats = worker.getDataStatistics(er.experimentID, er.distance);

                        DecimalFormat formatter = new DecimalFormat();
                        totalReadsNonRedundant.setText(formatter.format(stats.totalReadsNonRedundant));
                        totalReads.setText(formatter.format(stats.totalReads));
                        totalMappedReadsNonRedundant.setText(formatter.format(stats.totalMappedReadsNonRedundant));
                        totalMappedReads.setText(formatter.format(stats.totalMappedReads));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            t.start();

            if (selectedExperiments.isEmpty()) {
                Triplet p = new Triplet(er.key, er.experimentID, er.distance);
                selectedExperiments.put(Order.FIRST, p);
                ((ExperimentTableModel) table1.getModel()).setRowColour(row, Color.red);
                allSelectedExperiments.add(new Triplet(er.key, er.experimentID, er.distance));
            } else if (selectedExperiments.size() == 1 && !selectedExperiments.get(Order.FIRST).equals(new Triplet(er.key, er.experimentID, er.distance))) {
                Triplet p = new Triplet(er.key, er.experimentID, er.distance);
                selectedExperiments.put(Order.SECOND, p);
                ((ExperimentTableModel) table1.getModel()).setRowColour(row, Color.green);
                allSelectedExperiments.add(new Triplet(er.key, er.experimentID, er.distance));
            } else if (selectedExperiments.size() >= 2) {
                Triplet p = new Triplet(er.key, er.experimentID, er.distance);
                if (!allSelectedExperiments.contains(p)) {
                    allSelectedExperiments.add(p);
                    ((ExperimentTableModel) table1.getModel()).setRowColour(row, Color.lightGray);
                }
            }
            table1.repaint();
        } else if (e.getClickCount() == 2) {
            int r = table1.rowAtPoint(e.getPoint());
            final int row1 = table1.convertRowIndexToModel(r);
            final ExperimentResult er = etm.getExperiment(row1);
            final int key = etm.pageOffset;
            CursorToolkitOne.startWaitCursor(table1);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            final SummaryWorker worker = new SummaryWorker();
                            String label = "Summary_" + er.experimentID;
                            worker.loadResults(key, er.experimentID, er.distance);
                            SummaryResultsPanel srp = new SummaryResultsPanel(label, key, er.experimentID, er.distance);
                            srp.setSearchWorker(worker, true, null);
                            srp.setPreferredSize(dockingAlignmentDisplay1.getSize());
                            dockingAlignmentDisplay1.addDisplay(label, srp);
                            srp.setVisible(true);
                            dockingAlignmentDisplay1.repaint();
                        }
                    };
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    CursorToolkitOne.stopWaitCursor(table1);
                }
            };
            thread.start();
        }
    }

    private void table1MouseReleased(MouseEvent e) {
        int rowIndex = table1.rowAtPoint(e.getPoint());
        rowIndex = table1.convertRowIndexToModel(rowIndex);
        if (rowIndex < 0) {
            return;
        }
        if (e.isMetaDown() && e.getComponent() instanceof JTable) {
            JPopupMenu popup = createPopup();
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void showSampleComparisons() {
        if (selectedExperiments.size() == 2) {
            CursorToolkitOne.startWaitCursor(table1);
            final int key = etm.pageOffset;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            final SummaryWorker worker = new SummaryWorker();
                            long exp0 = selectedExperiments.get(Order.FIRST).eid;
                            long exp1 = selectedExperiments.get(Order.SECOND).eid;
                            int d0 = selectedExperiments.get(Order.FIRST).distance;
                            int d1 = selectedExperiments.get(Order.SECOND).distance;
                            String label = "Comparison" + "_" + exp0 + "_" + d0 + "__" + exp1 + "_" + d1;

                            List<SummaryResults> list0 = worker.getSummary(key, exp0, d0);
                            List<SummaryResults> list1 = worker.getSummary(key, exp1, d1);
                            List<SummaryResults> comparisons = new ArrayList<>();

                            Comparator<SummaryResults> comparator1 = (SummaryResults t1, SummaryResults t2) -> t2.geneId.compareTo(t1.geneId);
                            Collections.sort(list0, comparator1);
                            Collections.sort(list1, comparator1);
                            for (SummaryResults sr : list0) {
                                int index = Collections.binarySearch(list1, sr, comparator1);
                                if (index >= 0) {
                                    SummaryResults sr1 = list1.get(index);
                                    SummaryResults comparison = new SummaryResults();
                                    comparison.geneId = sr.geneId;
                                    comparison.geneSymbol = sr.geneSymbol;
                                    comparison.synonymousNames = sr.synonymousNames;
                                    comparison.levelIclassification = sr.levelIclassification;
                                    comparison.levelIIclassification = sr.levelIIclassification;
                                    comparison.levelIIIclassification = sr.levelIIclassification;
                                    comparison.levelIVclassification = sr.levelIVclassification;
                                    comparison.hierarchyup = sr.hierarchyup;
                                    comparison.hierarchydown = sr.hierarchydown;
                                    comparison.hierarchy0isoform = sr.hierarchy0isoform;
                                    comparison.hierarchy0mutation = sr.hierarchy0mutation;
                                    comparison.levelIfunction = sr.levelIfunction;
                                    comparison.levelIgenestructure = sr.levelIgenestructure;
                                    comparison.disease = sr.disease;
                                    comparison.roleincancers = sr.roleincancers;

                                    comparison.rpkm = sr.rpkm;
                                    comparison.rpkm1 = (sr1.rpkm < 0.001) ? 0.001 : sr1.rpkm;
                                    comparison.rpkmRank = sr.rpkmRank;
                                    comparison.rpkmRank1 = sr1.rpkmRank;
                                    comparison.rpkmDiff = (comparison.rpkm / comparison.rpkm1);
                                    if (new Double(comparison.rpkmDiff).isNaN()) {
                                        comparison.rpkmDiff = -1d;
                                    }
                                    comparison.biotype = sr.biotype;
                                    comparisons.add(comparison);
                                }
                            }

                            comparator1 = (SummaryResults t1, SummaryResults t2) -> new Double(t2.rpkmDiff).compareTo(t1.rpkmDiff);

                            Collections.sort(comparisons, comparator1);

                            SummaryResultsPanel srp = new SummaryResultsPanel(label, -1, -1l, -1);
                            srp.setSearchWorker(worker, false, comparisons);
                            srp.setPreferredSize(dockingAlignmentDisplay1.getSize());
                            dockingAlignmentDisplay1.addDisplay(label, srp);
                            srp.setVisible(true);
                            dockingAlignmentDisplay1.repaint();
                        }
                    };
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    CursorToolkitOne.stopWaitCursor(table1);
                }
            };
            thread.start();
        } else {
            // Complain about less than 2 rows selected
        }
    }

    final SummaryForm form = this;

    private void specificityAnalysis() {
        if (allSelectedExperiments.size() >= 2) {
            System.out.println("allSelectedExperiments: " + allSelectedExperiments.toString());
            CursorToolkitOne.startWaitCursor(table1);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            final SummaryWorker worker = new SummaryWorker();
                            String label = "Specificity";
                            List<List<SummaryResults>> allSummaries = new ArrayList<>();

                            for (Triplet sr : allSelectedExperiments) {
                                long exp = sr.eid;
                                int d = sr.distance;
                                label += "_" + exp + ":" + d;
                                List<SummaryResults> sResults = worker.getSummary(sr.key, exp, d);
                                allSummaries.add(sResults);
                            }

                            List<SummaryResults> comparisons = new ArrayList<>();

                            Comparator<SummaryResults> comparator1 = (SummaryResults t1, SummaryResults t2) -> t2.geneId.compareTo(t1.geneId);

                            for (List<SummaryResults> list : allSummaries) {
                                Collections.sort(list, comparator1);
                            }

                            for (SummaryResults sr : allSummaries.get(0)) {
                                SummaryResults comparison = new SummaryResults();
                                comparison.geneId = sr.geneId;
                                comparison.geneSymbol = sr.geneSymbol;
                                for (List<SummaryResults> l : allSummaries) {
                                    int index = Collections.binarySearch(l, sr, comparator1);
                                    if (index >= 0) {
                                        SummaryResults sr1 = l.get(index);
                                        comparison.otherRpkms.add(sr1.rpkm);
                                        comparison.otherRanks.add(sr1.rpkmRank);
                                    } else {
                                        comparison.otherRpkms.add(0d);
                                        comparison.otherRanks.add(l.size() + 1);
                                    }
                                }
                                double variance = StatUtil.computeVariance(comparison.otherRpkms);
                                double entropy = StatUtil.computeShannonEntropy(comparison.otherRpkms);
                                comparison.rankVariance = variance;
                                comparison.rankEntropy = entropy;
                                comparison.biotype = sr.biotype;
                                comparison.levelIIclassification = sr.levelIIclassification;
                                comparison.levelIfunction = sr.levelIfunction;
                                comparisons.add(comparison);
                            }

                            comparator1 = (SummaryResults t1, SummaryResults t2) -> new Double(t2.rankEntropy).compareTo(t1.rankEntropy);

                            Collections.sort(comparisons, comparator1);

                            SpecificityPanel srp = new SpecificityPanel(allSelectedExperiments, label, form);
                            srp.addDragListeners(frame);
                            srp.setSearchWorker(worker, comparisons);
                            srp.setPreferredSize(dockingAlignmentDisplay1.getSize());
                            dockingAlignmentDisplay1.addDisplay(label, srp);
                            srp.setVisible(true);
                            dockingAlignmentDisplay1.repaint();
                        }
                    };
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    CursorToolkitOne.stopWaitCursor(table1);
                }
            };
            thread.start();
        } else {
            // Complain about 1 row selected
        }
    }

    private void clearSelections() {
        table1.clearSelection();
        selectedExperiments.clear();
        ((ExperimentTableModel) table1.getModel()).clearColorSelections();
        allSelectedExperiments.clear();
    }

    ParclipFrameForm frame;

    public void setFrame(ParclipFrameForm f) {
        frame = f;
    }

    private JPopupMenu createPopup() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem menuItem1 = new JMenuItem("Compare Selections");
        menuItem1.addActionListener((ActionEvent e) -> {
            showSampleComparisons();
        });
        menuItem1.setToolTipText("Select at-least 2 experiments");

        JMenuItem menuItem2 = new JMenuItem("Clear Table Selections");
        menuItem2.addActionListener((ActionEvent e) -> {
            clearSelections();
        });

        JMenuItem menuItem3 = new JMenuItem("Specificity Analysis");
        menuItem3.addActionListener((ActionEvent e) -> {
            specificityAnalysis();
        });

        menu.add(menuItem1);
        menu.add(menuItem3);
        menu.add(menuItem2);
        return menu;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        splitPane1 = new JSplitPane();
        panel1 = new JPanel();
        statusLabel = new JLabel();
        totalMappedReads = new JTextField();
        label4 = new JLabel();
        totalMappedReadsNonRedundant = new JTextField();
        label3 = new JLabel();
        totalReads = new JTextField();
        label2 = new JLabel();
        scrollPane1 = new JScrollPane();
        table1 = new TooltipTable();
        totalReadsNonRedundant = new JTextField();
        label1 = new JLabel();
        scrollPane2 = new JScrollPane();
        dockingAlignmentDisplay1 = new DockingAlignmentDisplay();

        //======== this ========
        setLayout(new FormLayout(
            "515dlu:grow",
            "fill:default:grow"));

        //======== splitPane1 ========
        {
            splitPane1.setDividerLocation(400);
            splitPane1.setMinimumSize(new Dimension(990, 604));
            splitPane1.setPreferredSize(new Dimension(990, 604));
            splitPane1.setOneTouchExpandable(true);

            //======== panel1 ========
            {
                panel1.setMinimumSize(new Dimension(400, 604));
                panel1.setPreferredSize(new Dimension(400, 604));
                panel1.setLayout(new FormLayout(
                    "104dlu, $lcgap, 51dlu:grow",
                    "5*(default, $lgap), fill:277dlu:grow"));

                //---- statusLabel ----
                statusLabel.setFont(new Font("Heiti TC", Font.PLAIN, 12));
                statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                panel1.add(statusLabel, CC.xywh(1, 9, 3, 1));

                //---- totalMappedReads ----
                totalMappedReads.setEditable(false);
                totalMappedReads.setHorizontalAlignment(SwingConstants.RIGHT);
                panel1.add(totalMappedReads, CC.xy(3, 7));

                //---- label4 ----
                label4.setText("Total Mapped Reads");
                label4.setFont(label4.getFont().deriveFont(label4.getFont().getSize() - 5f));
                panel1.add(label4, CC.xy(1, 7));

                //---- totalMappedReadsNonRedundant ----
                totalMappedReadsNonRedundant.setEditable(false);
                totalMappedReadsNonRedundant.setHorizontalAlignment(SwingConstants.RIGHT);
                panel1.add(totalMappedReadsNonRedundant, CC.xy(3, 5));

                //---- label3 ----
                label3.setText("Total Mapped Reads Non Redundant");
                label3.setFont(label3.getFont().deriveFont(label3.getFont().getSize() - 5f));
                panel1.add(label3, CC.xy(1, 5));

                //---- totalReads ----
                totalReads.setEditable(false);
                totalReads.setHorizontalAlignment(SwingConstants.RIGHT);
                panel1.add(totalReads, CC.xy(3, 3));

                //---- label2 ----
                label2.setText("Total Reads");
                label2.setFont(label2.getFont().deriveFont(label2.getFont().getSize() - 5f));
                panel1.add(label2, CC.xy(1, 3));

                //======== scrollPane1 ========
                {
                    scrollPane1.setMinimumSize(new Dimension(400, 404));
                    scrollPane1.setPreferredSize(new Dimension(400, 404));

                    //---- table1 ----
                    table1.setAutoCreateRowSorter(true);
                    table1.setPreferredScrollableViewportSize(new Dimension(400, 404));
                    table1.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            table1MouseClicked(e);
                        }
                        @Override
                        public void mouseReleased(MouseEvent e) {
                            table1MouseReleased(e);
                        }
                    });
                    scrollPane1.setViewportView(table1);
                }
                panel1.add(scrollPane1, CC.xywh(1, 11, 3, 1, CC.FILL, CC.FILL));

                //---- totalReadsNonRedundant ----
                totalReadsNonRedundant.setEditable(false);
                totalReadsNonRedundant.setHorizontalAlignment(SwingConstants.RIGHT);
                panel1.add(totalReadsNonRedundant, CC.xy(3, 1));

                //---- label1 ----
                label1.setText("Total Reads Non Redundant");
                label1.setFont(label1.getFont().deriveFont(label1.getFont().getSize() - 5f));
                panel1.add(label1, CC.xy(1, 1));
            }
            splitPane1.setLeftComponent(panel1);

            //======== scrollPane2 ========
            {
                scrollPane2.setMinimumSize(new Dimension(600, 604));
                scrollPane2.setPreferredSize(new Dimension(600, 604));
                scrollPane2.setViewportView(dockingAlignmentDisplay1);
            }
            splitPane1.setRightComponent(scrollPane2);
        }
        add(splitPane1, CC.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JSplitPane splitPane1;
    private JPanel panel1;
    private JLabel statusLabel;
    private JTextField totalMappedReads;
    private JLabel label4;
    private JTextField totalMappedReadsNonRedundant;
    private JLabel label3;
    private JTextField totalReads;
    private JLabel label2;
    private JScrollPane scrollPane1;
    private TooltipTable table1;
    private JTextField totalReadsNonRedundant;
    private JLabel label1;
    private JScrollPane scrollPane2;
    private DockingAlignmentDisplay dockingAlignmentDisplay1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    class SummaryTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (allSelectedExperiments.size() > 0) {
                ExperimentTableModel model = (ExperimentTableModel) table.getModel();
                Color color = model.getRowColour(row);
                c.setBackground(color);
            } else {
                c.setBackground(Color.white);
                c.setForeground(Color.black);
            }
            return c;
        }
    }
}
