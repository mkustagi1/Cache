package cache;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowFilter.Entry;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import cache.SpecificityTableModel.STMRecord;
import cache.SummaryForm.Triplet;
import cache.dataimportes.holders.SummaryResults;
import cache.util.TextTransfer;
import cache.workers.AlignmentWorker;
import cache.workers.SummaryWorker;
import java.net.URISyntaxException;

/**
 *
 * @author mk2432
 */
public class SpecificityPanel extends javax.swing.JPanel {

    protected TextTransfer tt = new TextTransfer();
    protected String searchQuery = "";
    SpecificityTableModel oldModel = null;
    String oldLabel = null;
    final TableRowSorter<SpecificityTableModel> sorter = new TableRowSorter<>();
    List<Triplet> experiments = null;
    String label = null;

    static AlignmentWorker alignmentWorker;

    /**
     * Creates new form SummaryResultsPanel
     * @param experiments
     * @param l
     * @param f
     */
    public SpecificityPanel(List<Triplet> experiments, String l, SummaryForm f) {
        initComponents();
        alignmentWorker = new AlignmentWorker();
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        dialog = new JDialog(topFrame, false);
        dialog.setResizable(true);
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
        searchText.getDocument().addDocumentListener(new DocumentListener() {
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

        this.experiments = experiments;
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
        label = l;
        form = f;
    }

    public void setSearchWorker(SummaryWorker worker, List<SummaryResults> comparisons) {
        SpecificityTableModel model = null;
        model = new SpecificityTableModel(comparisons.size(), comparisons.size(), statusLabel, comparisons, experiments);
        sorter.setModel(model);
        for (int i = 0; i < model.getColumnCount(); i++) {
            Class klass = model.getColumnClass(i);
            sorter.setComparator(i, createComparator(klass));
        }
        sorter.setRowFilter(makeRowFilter(model.comparisons.size(), 0));

        model.setSearchSourceSource(worker);
        model.addTableModelListener((TableModelEvent tme) -> {
            resultsTable.repaint();
        });

        if (comparisons.isEmpty()) {
            statusLabel.setText("No results found...");
        } else {
            statusLabel.setText("1, " + comparisons.size() + " of " + comparisons.size() + " results");
        }

        resultsTable.setModel(model);
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.setRowSorter(sorter);

        int w = 0;
        int count = resultsTable.getModel().getColumnCount();
        for (int i = 0; i < count; i++) {
            String name = resultsTable.getModel().getColumnName(i);
            Font f = resultsTable.getFont();
            FontMetrics fm = resultsTable.getFontMetrics(f);
            int width = fm.stringWidth(name);
            w += width;
            resultsTable.getColumnModel().getColumn(i).setWidth(width);
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(width);
        }
        resultsTable.setPreferredSize(new Dimension(w, resultsTable.getHeight()));

        this.remove(jScrollPane1);
        jScrollPane1 = SpecificityTableModel.createPagingScrollPaneForTable(resultsTable);
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        saveButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        copyButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        searchText = new javax.swing.JTextField();
        copyButton1 = new javax.swing.JButton();
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

        searchText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchActionPerformed(e);
            }
        });
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchTextKeyReleased(e);
            }
        });
        searchText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                searchTextFocusGained(e);
            }
        });

        searchText.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        searchText.setMaximumSize(new java.awt.Dimension(150, 28));
        jToolBar1.add(searchText);

        copyButton1.setText("Search");
        copyButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copyButton1.setMaximumSize(new java.awt.Dimension(50, 25));
        copyButton1.setMinimumSize(new java.awt.Dimension(25, 25));
        copyButton1.setPreferredSize(new java.awt.Dimension(25, 25));
        copyButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        copyButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });
        jToolBar1.add(copyButton1);
        jToolBar1.add(jSeparator3);
        jToolBar1.add(statusLabel);

        resultsTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null}
                },
                new String[]{
                    "Title 1", "Title 2", "Title 3", "Title 4"
                }));
        jScrollPane1.setViewportView(resultsTable);

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
    }// </editor-fold>                        

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        FileFilter fileFilter = new FileNameExtensionFilter("Table as TXT Files", "txt");
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(label + ".txt"));
        fc.setName("Save Specificity Results");
        fc.addChoosableFileFilter(fileFilter);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(fileFilter);
        int option = fc.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION == option) {
            if (fc.getFileFilter() == fileFilter) {
                try {
                    File file = fc.getSelectedFile();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    SpecificityTableModel model = (SpecificityTableModel) resultsTable.getModel();
                    List<SummaryResults> results = model.comparisons;
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
                        String geneURL = result.geneId;
                        bw.write(geneURL);
                        bw.write("\t");
                        String symbol = result.geneSymbol;
                        bw.write(symbol);
                        bw.write("\t");
                        String biotype = result.biotype;
                        bw.write(biotype);
                        bw.write("\t");
                        String classification = result.levelIIclassification;
                        bw.write(classification);
                        bw.write("\t");
                        String finction = result.levelIfunction;
                        bw.write(finction);
                        bw.write("\t");
                        for (Integer rpkmRank : result.otherRanks) {
                            bw.write(rpkmRank.toString());
                            bw.write("\t");
                        }
                        Double variance = result.rankVariance;
                        bw.write(variance.toString());
                        bw.write("\t");
                        Double entropy = result.rankEntropy;
                        if (entropy == null || entropy.isNaN()) {
                            entropy = 0d;
                        }
                        bw.write(entropy.toString());
                        bw.write("\t");
                        for (Double rpkm : result.otherRpkms) {
                            bw.write(rpkm.toString());
                            bw.write("\t");
                        }
                        bw.newLine();
                    }

                    bw.flush();
                    bw.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            SpecificityTableModel model = (SpecificityTableModel) resultsTable.getModel();
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
    }

    PopUpJList ppjl;
    int oldX = 0;
    int oldY = 0;
    SummaryForm form;

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
                String text = searchText.getText();
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
                String text = searchText.getText();
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
            String text = searchText.getText();
            if (frame.getTabbedPane1().getSelectedComponent() ==  form && (text != null && text.length() > 2)) {
                dialog.setVisible(true);
            } else {
                dialog.setVisible(false);
            }
        });
    }
    
    
    private void searchBoxDocumentChanged() {
        String text = searchText.getText();
        if (text != null && text.length() >= 2) {
            List<String> symbols = alignmentWorker.getGenesByPartialSymbol(text);
            dialog.getContentPane().removeAll();
            ppjl = new PopUpJList(searchText, symbols);
            dialog.getContentPane().add(ppjl);
            dialog.setMinimumSize(ppjl.getPreferredSize());
            dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
            if (symbols.size() > 0) {
                dialog.setVisible(true);
            } else {
                dialog.setVisible(false);
            }
            searchText.requestFocus();
            int cPos = searchText.getCaretPosition();
            int ht = searchText.getHeight();
            Point pt = searchText.getLocationOnScreen();
            int x = pt.x + cPos;
            int y = pt.y + ht;
            dialog.setLocation(x, y);
        } else {
            dialog.setVisible(false);
        }
    }

    private void searchTextKeyReleased(KeyEvent ke) {
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

    private void searchTextFocusGained(FocusEvent e) {
        int caretPosition = searchText.getCaretPosition();
        searchText.setSelectionStart(0);
        searchText.setSelectionEnd(0);
        searchText.setCaretPosition(caretPosition);
        String text = searchText.getText();
        if (text.length() >= 2) {
            dialog.getContentPane().removeAll();
            List<String> symbols = alignmentWorker.getGenesByPartialSymbol(text);
            ppjl = new PopUpJList(searchText, symbols);
            dialog.getContentPane().add(ppjl);
            dialog.setMinimumSize(ppjl.getPreferredSize());
            dialog.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
            if (symbols.size() > 0) {
                dialog.setVisible(true);
            } else {
                dialog.setVisible(false);
            }
            searchText.requestFocus();
            int cPos = searchText.getCaretPosition();
            int ht = searchText.getHeight();
            Point pt = searchText.getLocationOnScreen();
            int x = pt.x + cPos;
            int y = pt.y + ht;
            dialog.setLocation(x, y);
        }
    }

    private void thisComponentHidden(ComponentEvent e) {
        dialog.setVisible(false);
    }

    private void thisComponentShown(ComponentEvent e) {
        dialog.setVisible(false);
    }

    private void searchActionPerformed(java.awt.event.ActionEvent evt) {
        final String name = (ppjl.list.getSelectedValue() == null) ? searchText.getText().trim() : (String) ppjl.list.getSelectedValue();
        searchText.setText(name);
        dialog.getContentPane().removeAll();
        dialog.setVisible(false);
        if (name != null && !name.equals("")) {
            int count = 1;
            SpecificityTableModel model = (SpecificityTableModel) resultsTable.getModel();
            SummaryWorker worker = model.getWorker();
            List<SummaryResults> result = null;
            List<SummaryResults> r = model.comparisons;
            result = new ArrayList<>();
            for (SummaryResults _sr : r) {
                if (_sr.geneSymbol.equalsIgnoreCase(name)) {
                    result.add(_sr);
                }
            }
            if (result != null && result.size() > 0) {
                SpecificityTableModel newModel = null;
                newModel = new SpecificityTableModel(result.size(), result.size(), statusLabel, result, experiments);
                newModel.addTableModelListener((TableModelEvent tme) -> {
                    resultsTable.repaint();
                });

                oldModel = model;
                oldLabel = statusLabel.getText();
                statusLabel.setText(result.size() + " results found");
                resultsTable.setModel(newModel);
                newModel.fireTableDataChanged();
            } else {
                oldModel = model;
                oldLabel = statusLabel.getText();
                statusLabel.setText("No results found");
            }
        } else if (oldModel != null) {
            resultsTable.setModel(oldModel);
            oldModel.fireTableDataChanged();
            statusLabel.setText(oldLabel);
            repaint();
        }
    }
    // Variables declaration - do not modify                     
    private javax.swing.JButton copyButton;
    private javax.swing.JButton copyButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTable resultsTable;
    private javax.swing.JButton saveButton;
    private javax.swing.JTextField searchText;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration                   

    class ComparisonFileFilter extends FileFilter {

        String description;
        String extensions[];

        public ComparisonFileFilter(String description, String extension) {
            this(description, new String[]{extension});
        }

        public ComparisonFileFilter(String description, String extensions[]) {
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

    private RowFilter< SpecificityTableModel, Integer> makeRowFilter(final int itemsPerPage, final int target) {
        return new RowFilter< SpecificityTableModel, Integer>() {
            @Override
            public boolean include(Entry< ? extends SpecificityTableModel, ? extends Integer> entry) {
                int ei = entry.getIdentifier();
                return (target * itemsPerPage <= ei && ei < target * itemsPerPage + itemsPerPage);
            }
        };
    }

    private Comparator createComparator(final Class klass) {
        return (Comparator) (Object t, Object t1) -> ((Comparable) klass.cast(t)).compareTo((Comparable) klass.cast(t1));
    }

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
                        searchText.grabFocus();
                        searchText.requestFocus();
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
                        searchText.requestFocus();
                        int caretPosition = searchText.getCaretPosition();
                        try {
                            searchText.getDocument().remove(caretPosition - 1, 1);
                        } catch (BadLocationException ex) {
                            Logger.getLogger(GeneSearchForm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (ke.getKeyCode() != KeyEvent.VK_UP && ke.getKeyCode() != KeyEvent.VK_DOWN) {
                        searchText.requestFocus();
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
