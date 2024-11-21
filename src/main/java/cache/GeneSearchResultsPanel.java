package cache;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import cache.GeneSearchTableModel.STMRecord;
import cache.dataimportes.holders.SummaryResults;
import cache.util.CursorToolkitOne;
import cache.util.TextTransfer;
import cache.workers.SummaryWorker;
import java.net.URISyntaxException;

/**
 *
 * @author mk2432
 */
public class GeneSearchResultsPanel extends javax.swing.JPanel {

    protected TextTransfer tt = new TextTransfer();
    SummaryTableModel oldModel = null;
    String oldLabel = null;
    final TableRowSorter< GeneSearchTableModel> sorter = new TableRowSorter< >();

    /**
     * Creates new form GeneSearchResultsPanel
     */
    public GeneSearchResultsPanel() {
        initComponents();
        resultsTable.setFillsViewportHeight(Boolean.TRUE);
        resultsTable.setRowSelectionAllowed(true);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        resultsTable.setShowGrid(true);
        resultsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        resultsTable.setAutoCreateRowSorter(true);
        URLRenderer renderer = new URLRenderer();
        resultsTable.setDefaultRenderer(URL.class, renderer);
        resultsTable.addMouseListener(renderer);
        resultsTable.addMouseMotionListener(renderer);
    }

    public void setSearchWorker(SummaryWorker worker, List<SummaryResults> comparisons) {
        GeneSearchTableModel model = null;
        model = new GeneSearchTableModel(worker.getResults().size(), worker.getResults().size(), statusLabel);
        sorter.setModel(model);
        for (int i = 0; i < model.getColumnCount(); i++) {
            Class klass = model.getColumnClass(i);
            sorter.setComparator(i, createComparator(klass));
        }
        sorter.setRowFilter(makeRowFilter(model.getPageSize(), 0));
        model.setSearchSourceSource(worker);
        model.addTableModelListener((TableModelEvent tme) -> {
            resultsTable.repaint();
        });

        if (worker.getResults().isEmpty()) {
            statusLabel.setText("No results found...");
        } else {
            statusLabel.setText("1, " + worker.getResults().size() + " of " + worker.getResults().size() + " results");
        }

        resultsTable.setModel(model);
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.setRowSorter(sorter);

        int count = resultsTable.getModel().getColumnCount();
        for (int i = 0; i < count; i++) {
            String name = resultsTable.getModel().getColumnName(i);
            Font f = resultsTable.getFont();
            FontMetrics fm = resultsTable.getFontMetrics(f);
            int width = fm.stringWidth(name);
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(width);
        }

        this.remove(jScrollPane1);
        jScrollPane1 = GeneSearchTableModel.createPagingScrollPaneForTable(resultsTable);
        jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setViewportView(resultsTable);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 712, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                                .add(0, 0, 0)
                                .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 712, Short.MAX_VALUE)
                                .add(0, 0, 0))));
        layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(29, 29, 29)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE))
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                                .add(0, 0, 0)
                                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(0, 0, 0))));

        model.fireTableDataChanged();
        model.fireTableStructureChanged();
        resultsTable.revalidate();
        resultsTable.repaint();
        this.revalidate();
        this.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        saveButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        copyButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        statusLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultsTable = new javax.swing.JTable();

        jToolBar1.setRollover(true);

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cache/resources/document-save22.png"))); // NOI18N
        saveButton.setFocusable(false);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setMaximumSize(new java.awt.Dimension(25, 25));
        saveButton.setMinimumSize(new java.awt.Dimension(25, 25));
        saveButton.setPreferredSize(new java.awt.Dimension(25, 25));
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(saveButton);
        jToolBar1.add(jSeparator1);

        copyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cache/resources/edit-copy22.png"))); // NOI18N
        copyButton.setFocusable(false);
        copyButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copyButton.setMaximumSize(new java.awt.Dimension(25, 25));
        copyButton.setMinimumSize(new java.awt.Dimension(25, 25));
        copyButton.setPreferredSize(new java.awt.Dimension(25, 25));
        copyButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(copyButton);
        jToolBar1.add(jSeparator2);
        jToolBar1.add(jSeparator3);
        jToolBar1.add(statusLabel);

        resultsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultsTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(resultsTable);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 712, Short.MAX_VALUE)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 712, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 532, Short.MAX_VALUE))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                    .add(0, 33, Short.MAX_VALUE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 528, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        FileFilter fileFilter = new FileNameExtensionFilter("Table as TXT Files", "txt");
        JFileChooser fc = new JFileChooser();
        fc.setName("Save Summary Results");
        fc.addChoosableFileFilter(fileFilter);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(fileFilter);
        int option = fc.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION == option) {
            if (fc.getFileFilter() == fileFilter) {
                try {
                    File file = fc.getSelectedFile();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    GeneSearchTableModel model = (GeneSearchTableModel) resultsTable.getModel();
                    SummaryWorker worker = model.getWorker();
                    List<SummaryResults> results = worker.getResults();
                    int rows = results.size();
                    System.out.println("Rows size: " + rows);
                    int columns = model.getColumnCount();
                    for (int i = 0; i < columns; i++) {
                        bw.write(model.getColumnName(i));
                        bw.write("\t");
                    }
                    bw.newLine();
                    for (int i = 0; i < rows; i++) {
                        SummaryResults result = results.get(i);
                        Long experimentId = result.experimentId;
                        bw.write(experimentId.toString());
                        bw.write("\t");
                        Integer distance = result.distance;
                        bw.write(distance.toString());
                        bw.write("\t");
                        String en = result.experimentName;
                        bw.write(en);
                        bw.write("\t");
                        String geneURL = result.geneId;
                        bw.write(geneURL);
                        bw.write("\t");

                        String symbol = result.geneSymbol;
                        bw.write(symbol);
                        bw.write("\t");
                        String transcriptURL = result.masterTranscript;
                        bw.write(transcriptURL);
                        bw.write("\t");
                        Long nrrcrt = result.masterReadsNonRedundant;
                        bw.write(nrrcrt.toString());
                        bw.write("\t");
                        Long trcrt = result.masterReadsTotal;
                        bw.write(trcrt.toString());
                        bw.write("\t");
                        Long tmbur = result.mappedTranscriptsByMasterReads;
                        bw.write(tmbur.toString());
                        bw.write("\t");
                        Long rtl = result.masterTranscriptLength;
                        bw.write(rtl.toString());
                        bw.write("\t");
                        Integer noi = result.isoforms;
                        bw.write(noi.toString());
                        bw.write("\t");
                        Long rcutoi = result.otherReadsCount;
                        bw.write(rcutoi.toString());
                        bw.write("\t");
                        Double trcbrtl = result.mappedReadByLength;
                        bw.write(trcbrtl.toString());
                        bw.write("\t");
                        Double rpkm = result.rpkm;
                        bw.write(rpkm.toString());
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();
                } catch (IOException ioe) {
                }
            }
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
        try {
            GeneSearchTableModel model = (GeneSearchTableModel) resultsTable.getModel();
            int[] rows = resultsTable.getSelectedRows();
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            for (int row : rows) {
                STMRecord record = model.data.get(row);
                Object[] data = record.data;
                for (Object o : data) {
                    bw.write(o.toString());
                    bw.write("\t");
                }
                bw.newLine();
            }
            bw.flush();
            tt.setClipboardContents(sw.getBuffer().toString());
            sw.close();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(SummaryResultsPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_copyButtonActionPerformed

    private void resultsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultsTableMouseClicked
        if (evt.getClickCount() == 2) {
            int selectedRow = resultsTable.getSelectedRow();
            final int selectedRowModel = resultsTable.convertRowIndexToModel(selectedRow);
            final GeneSearchTableModel model = (GeneSearchTableModel) resultsTable.getModel();
            final Long eid = (Long) model.getValueAt(selectedRowModel, 0);
            final int distance = (Integer) model.getValueAt(selectedRowModel, 1);
            final int key = model.pageOffset;
            CursorToolkitOne.startWaitCursor(resultsTable);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            AlignmentForm.setSearchGene(key, eid, distance, (String) model.getValueAt(selectedRowModel, 4));
                            AlignmentForm.atm.fireTableDataChanged();
                        }
                    };
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    CursorToolkitOne.stopWaitCursor(resultsTable);
                }
            };
            thread.start();
        }
    }//GEN-LAST:event_resultsTableMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton copyButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTable resultsTable;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables

    class URLRenderer extends DefaultTableCellRenderer implements MouseListener, MouseMotionListener {

        private int row = -1;
        private int col = -1;
        private boolean isRollover = false;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            if (value != null && value instanceof URL) {
                URL url = (URL) value;
                String str = value != null ? value.toString() : "";
                String[] tokens = url.getFile().split("=");
                String name = tokens[tokens.length - 1];
                String _url = url.getProtocol() + "://" + url.getHost() + url.getFile();
                String uFormattedName = "<html><u><font color='blue'><a href=\"" + url + "\">" + name + "</a></font></u></html>";
                String formattedName = "<html><font color='blue'><a href=\"" + url + "\">" + name + "</a></font></html>";

                if (!table.isEditing() && this.row == row && this.col == column && this.isRollover) {
                    setText(uFormattedName);
                } else if (hasFocus) {
                    setText(formattedName);
                } else {
                    setText(name);
                }
            }
            return this;
        }

        private boolean isURLColumn(JTable table, int column) {
            return column >= 0 && table.getColumnClass(column).equals(URL.class);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            Point pt = e.getPoint();
            int prev_row = row;
            int prev_col = col;
            boolean prev_ro = isRollover;
            row = table.rowAtPoint(pt);
            col = table.columnAtPoint(pt);
            isRollover = isURLColumn(table, col); // && pointInsidePrefSize(table, pt);
            if (row == prev_row && col == prev_col && isRollover == prev_ro || !isRollover && !prev_ro) {
                return;
            }

            Rectangle repaintRect;
            if (isRollover) {
                Rectangle r = table.getCellRect(row, col, false);
                repaintRect = prev_ro ? r.union(table.getCellRect(prev_row, prev_col, false)) : r;
            } else { //if(prev_ro) {
                repaintRect = table.getCellRect(prev_row, prev_col, false);
            }
            table.repaint(repaintRect);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            if (isURLColumn(table, col)) {
                table.repaint(table.getCellRect(row, col, false));
                row = -1;
                col = -1;
                isRollover = false;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            Point pt = e.getPoint();
            int ccol = table.columnAtPoint(pt);

            if (isURLColumn(table, ccol)) { // && pointInsidePrefSize(table, pt)) {
                int crow = table.rowAtPoint(pt);
                Object selected = table.getValueAt(crow, ccol);
                if (selected instanceof URL) {
                    URL url = (URL) selected;
                    System.out.println(url);
                    try {
                        if (Desktop.isDesktopSupported()) { // JDK 1.6.0
                            Desktop.getDesktop().browse(url.toURI());
                        }
                    } catch (URISyntaxException | IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }
    }

    private RowFilter< GeneSearchTableModel, Integer> makeRowFilter(final int itemsPerPage, final int target) {
        return new RowFilter< GeneSearchTableModel, Integer>() {
            @Override
            public boolean include(RowFilter.Entry< ? extends GeneSearchTableModel, ? extends Integer> entry) {
                int ei = entry.getIdentifier();
                return (target * itemsPerPage <= ei && ei < target * itemsPerPage + itemsPerPage);
            }
        };
    }

    private Comparator createComparator(final Class klass) {
        return (Comparator) (Object t, Object t1) -> ((Comparable) klass.cast(t)).compareTo((Comparable) klass.cast(t1));
    }
}
