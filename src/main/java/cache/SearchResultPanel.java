package cache;

import java.awt.Color;
import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import cache.dataimportes.holders.SearchResult;
import cache.dataimportes.holders.Strand;
import cache.util.TextTransfer;
import cache.workers.SearchWorker;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author mk2432
 */
public class SearchResultPanel extends javax.swing.JPanel {

    protected TextTransfer tt = new TextTransfer();
    protected String searchQuery = "";

    /**
     * Creates new form SearchResultPanel
     */
    public SearchResultPanel() {
        initComponents();

        resultsTable.setFillsViewportHeight(Boolean.TRUE);
        resultsTable.setRowSelectionAllowed(true);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        resultsTable.setShowGrid(true);
        resultsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        resultsTable.setAutoCreateRowSorter(true);

        mappedTranscriptsPane.setContentType("text/html");
        mappedTranscriptsPane.setEditable(false);
        StyleSheet css = ((HTMLEditorKit) mappedTranscriptsPane.getEditorKit()).getStyleSheet();
        Style style = css.getStyle("body");
        mappedTranscriptsPane.addHyperlinkListener((HyperlinkEvent e) -> {
            if (e.getEventType()
                    == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) {
                    Logger.getLogger(SearchResultPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public void setSearchWorker(SearchWorker worker) {
        SearchTableModel model = new SearchTableModel(worker.getResultSize(), 50, statusLabel, worker.searchType);
        model.setSearchSourceSource(worker);
        model.addTableModelListener((TableModelEvent tme) -> {
            resultsTable.repaint();
        });

        if (worker.getResultSize() == 0) {
            statusLabel.setText("No results found...");
        } else if (worker.getResultSize() >= 100) {
            statusLabel.setText("1, 50 of " + worker.getResultSize() + " results");
        } else {
            statusLabel.setText("1, " + worker.getResultSize() + " of " + worker.getResultSize() + " results");
        }

        resultsTable.setModel(model);
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        jPanel1.remove(jScrollPane3);
        jScrollPane3 = SearchTableModel.createPagingScrollPaneForTable(resultsTable);
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane3.setViewportView(resultsTable);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(0, 0, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(0, 379, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)));

        model.fireTableDataChanged();
        model.fireTableStructureChanged();
        resultsTable.revalidate();
        resultsTable.repaint();
        jPanel1.revalidate();
        jPanel1.repaint();
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
        showMappedTranscripts = new javax.swing.JCheckBox();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        statusLabel = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        resultsTable = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        mappedTranscriptsPane = new javax.swing.JEditorPane();

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

        showMappedTranscripts.setSelected(true);
        showMappedTranscripts.setText("Show Mapped Transcripts");
        showMappedTranscripts.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        showMappedTranscripts.setFocusable(false);
        showMappedTranscripts.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        showMappedTranscripts.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showMappedTranscripts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showMappedTranscriptsActionPerformed(evt);
            }
        });
        jToolBar1.add(showMappedTranscripts);
        jToolBar1.add(jSeparator3);
        jToolBar1.add(statusLabel);

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setOneTouchExpandable(true);

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
        resultsTable.setMaximumSize(new java.awt.Dimension(2147483647, 805));
        resultsTable.setMinimumSize(new java.awt.Dimension(60, 805));
        resultsTable.setPreferredSize(new java.awt.Dimension(805, 805));
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultsTableMouseClicked(evt);
            }
        });
        resultsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                resultsTableMouseMoved(evt);
            }
        });
        jScrollPane3.setViewportView(resultsTable);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jScrollPane1.setViewportView(mappedTranscriptsPane);

        jSplitPane1.setRightComponent(jScrollPane1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        FileFilter fileFilter = new FileNameExtensionFilter("Table as TXT Files", "txt");
        JFileChooser fc = new JFileChooser();
        fc.setName("Save Search Results");
        FileFilter fastaFilter = new FastaFileFilter("FASTA files", "fa");
        FileFilter fastaTotalFilter = new FastaFileFilter("FASTA files - Redundant reads", "fa");
        fc.addChoosableFileFilter(fastaFilter);
        fc.addChoosableFileFilter(fastaTotalFilter);
        fc.addChoosableFileFilter(fileFilter);
        fc.setFileFilter(fastaFilter);
        fc.setAcceptAllFileFilterUsed(false);
        int option = fc.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION == option) {
            if (fc.getFileFilter() == fileFilter) {
                try {
                    File file = fc.getSelectedFile();
                    if (!fc.getSelectedFile().getAbsolutePath().endsWith(".txt")) {
                        file = new File(file.toString() + ".txt");
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    SearchTableModel model = (SearchTableModel) resultsTable.getModel();
                    SearchWorker worker = model.getWorker();
                    List<SearchResult> results = worker.getResults();
                    int rows = results.size();
                    int columns = model.getColumnCount();
                    bw.write(model.getColumnName(0));
                    for (int i = 0; i < columns; i++) {
                        bw.write("\t");
                        bw.write(model.getColumnName(i));
                    }
                    bw.newLine();
                    for (int i = 0; i < rows; i++) {
                        SearchResult sr = results.get(i);
                        if (sr.type.equals(SearchResult.SearchType.READS)) {
                            bw.write(Long.toString(sr.experimentID));
                            bw.write("\t");
                            bw.write(sr.readID.toString());
                            bw.write("\t");
                            bw.write(sr.sequence);
                            bw.write("\t");
                            bw.write(Long.toString(sr.readCount));
                            bw.write("\t");
                            Integer strand = (sr.strand.equals(Strand.FORWARD)) ? 1 : 0;
                            bw.write(strand.toString());
                            bw.newLine();
                        } else if (sr.type.equals(SearchResult.SearchType.TRANSCRIPTS)) {
                            bw.write(sr.readID.toString());
                            bw.write("\t");
                            bw.write(sr.sequence);
                            bw.write("\t");
                            Integer strand = (sr.strand.equals(Strand.FORWARD)) ? 1 : 0;
                            bw.write(strand.toString());
                            bw.newLine();
                        }
                    }
                    bw.flush();
                    bw.close();
                } catch (IOException ioe) {
                }
            } else if (fc.getFileFilter() == fastaFilter) {
                try {
                    File file = fc.getSelectedFile();
                    if (!(fc.getSelectedFile().getAbsolutePath().endsWith(".fa") || fc.getSelectedFile().getAbsolutePath().endsWith(".fasta"))) {
                        file = new File(file.toString() + ".fa");
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    SearchTableModel model = (SearchTableModel) resultsTable.getModel();
                    SearchWorker worker = model.getWorker();
                    List<SearchResult> results = worker.getResults();
                    int rows = results.size();
                    bw.write(">query");
                    bw.newLine();
                    bw.write(searchQuery);
                    bw.newLine();
                    for (int i = 0; i < rows; i++) {
                        SearchResult sr = results.get(i);
                        bw.write(">");
                        bw.write(Long.toString(sr.readCount)
                                + "_" + Long.toString(sr.experimentID)
                                + "_" + sr.readID.toString()
                                + "_" + sr.strand.toString());
                        bw.newLine();
                        bw.write(sr.sequence);
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();
                } catch (IOException ioe) {
                }
            } else if (fc.getFileFilter() == fastaTotalFilter) {
                try {
                    File file = fc.getSelectedFile();
                    if (!(fc.getSelectedFile().getAbsolutePath().endsWith(".fa") || fc.getSelectedFile().getAbsolutePath().endsWith(".fasta"))) {
                        file = new File(file.toString() + ".fa");
                    }
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    SearchTableModel model = (SearchTableModel) resultsTable.getModel();
                    SearchWorker worker = model.getWorker();
                    List<SearchResult> results = worker.getResults();
                    int rows = results.size();
                    bw.write(">query");
                    bw.newLine();
                    bw.write(searchQuery);
                    bw.newLine();
                    for (int i = 0; i < rows; i++) {
                        SearchResult sr = results.get(i);
                        if (!showMappedTranscripts.isSelected()) {
                            String text = model.getToolTipText(i);
                            if (text == null || text.trim().equals("")) {
                                for (long c = 0; c < sr.readCount; c++) {
                                    bw.write(">");
                                    bw.write(Long.toString(c)
                                            + "_" + Long.toString(sr.experimentID)
                                            + "_" + sr.readID.toString()
                                            + "_" + sr.strand.toString());
                                    bw.newLine();
                                    bw.write(sr.sequence);
                                    bw.newLine();
                                }
                            }
                        } else {
                            for (long c = 0; c < sr.readCount; c++) {
                                bw.write(">");
                                bw.write(Long.toString(c)
                                        + "_" + Long.toString(sr.experimentID)
                                        + "_" + sr.readID.toString()
                                        + "_" + sr.strand.toString());
                                bw.newLine();
                                bw.write(sr.sequence);
                                bw.newLine();
                            }
                        }
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
            SearchTableModel model = (SearchTableModel) resultsTable.getModel();
            SearchWorker worker = model.getWorker();
            int[] rows = resultsTable.getSelectedRows();
            List<SearchResult> results = worker.getResults();
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);
            for (int row : rows) {
                SearchResult sr = results.get(row);
                bw.write(Long.toString(sr.experimentID));
                bw.write("\t");
                bw.write(sr.readID.toString());
                bw.write("\t");
                bw.write(sr.sequence);
                bw.write("\t");
                bw.write(Long.toString(sr.readCount));
                bw.newLine();
            }
            bw.flush();
            tt.setClipboardContents(sw.getBuffer().toString());
            sw.close();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(SearchResultPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_copyButtonActionPerformed

    private void resultsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultsTableMouseMoved
    }//GEN-LAST:event_resultsTableMouseMoved

    private void showMappedTranscriptsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showMappedTranscriptsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showMappedTranscriptsActionPerformed

    private void resultsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultsTableMouseClicked
        if (!showMappedTranscripts.isSelected()) {
            int start = mappedTranscriptsPane.getDocument().getStartPosition().getOffset();
            int end = mappedTranscriptsPane.getDocument().getEndPosition().getOffset();
            try {
                mappedTranscriptsPane.getDocument().remove(start, end - start);
            } catch (BadLocationException ex) {
                Logger.getLogger(SearchResultPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            mappedTranscriptsPane.setText("<html></html>");
        }
        if (evt.getSource() == resultsTable && showMappedTranscripts.isSelected()) {
            int row = resultsTable.rowAtPoint(evt.getPoint());
            SearchTableModel model = (SearchTableModel) resultsTable.getModel();
            if (model.getData().isEmpty()) {
                mappedTranscriptsPane.setText("<html>No transcripts mapped</html>");
            }
            mappedTranscriptsPane.setText(model.getToolTipText(row));
        } else {
        }
    }//GEN-LAST:event_resultsTableMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton copyButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JEditorPane mappedTranscriptsPane;
    private javax.swing.JTable resultsTable;
    private javax.swing.JButton saveButton;
    private javax.swing.JCheckBox showMappedTranscripts;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables

    public void setQuerySequence(String s) {
        searchQuery = s;
    }

    class FastaFileFilter extends FileFilter {

        String description;
        String extensions[];

        public FastaFileFilter(String description, String extension) {
            this(description, new String[]{extension});
        }

        public FastaFileFilter(String description, String extensions[]) {
            if (description == null) {
                this.description = extensions[0];
            } else {
                this.description = description;
            }
            this.extensions = (String[]) extensions.clone();
            toLower(this.extensions);
        }

        private void toLower(String array[]) {
            for (int i = 0, n = array.length; i < n; i++) {
                array[i] = array[i].toLowerCase();
            }
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            } else {
                String path = file.getAbsolutePath().toLowerCase();
                for (int i = 0, n = extensions.length; i < n; i++) {
                    String extension = extensions[i];
                    if ((path.endsWith(extension) && (path.charAt(path.length() - extension.length() - 1)) == '.')) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
