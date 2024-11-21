package cache;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowFilter.Entry;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import cache.SummaryTableModel.STMRecord;
import cache.dataimportes.holders.SummaryResults;
import cache.util.CursorToolkitOne;
import cache.util.FilterBox;
import cache.util.FindBox;
import cache.util.SearchBox;
import cache.util.TextTransfer;
import cache.workers.AlignmentWorker;
import cache.workers.SummaryWorker;

/**
 *
 * @author Manjunath Kustagi
 */
public class SummaryResultsPanel extends javax.swing.JPanel {

    protected TextTransfer tt = new TextTransfer();
    protected String searchQuery = "";
    boolean summary = true;
    SummaryTableModel oldModel = null;
    private String authToken;
    String oldLabel = null;
    final TableRowSorter< SummaryTableModel> sorter = new TableRowSorter<>();
    AlignmentWorker alignmentWorker;
    String label = null;
    boolean editable = false;
    final long experimentId;
    final int distance;
    final int key;

    /**
     * Creates new form SummaryResultsPanel
     *
     * @param l
     * @param experimentId
     * @param distance
     */
    public SummaryResultsPanel(String l, int key, long experimentId, int distance) {
        this.experimentId = experimentId;
        this.distance = distance;
        this.key = key;
        initComponents();
        createFilterBox();
        alignmentWorker = new AlignmentWorker();

        resultsTable.setFillsViewportHeight(Boolean.TRUE);
        resultsTable.setRowSelectionAllowed(true);
        resultsTable.setShowGrid(true);
        resultsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        label = l;

        JMenuItem copyItem = new JMenuItem("Copy");
        Action copyAction = new CopyAction(resultsTable);
        copyItem.addActionListener(copyAction);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        pm.add(copyItem);
        resultsTable.getActionMap().put("copy", copyAction);
        JMenuItem pasteItem = new JMenuItem("Paste");
        Action pasteAction = new PasteAction(resultsTable);
        pasteItem.addActionListener(pasteAction);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        pm.add(pasteItem);
        resultsTable.getActionMap().put("paste", pasteAction);

        resultsTable.addPropertyChangeListener((final PropertyChangeEvent e) -> {
            if ("tableCellEditor".equals(e.getPropertyName())) {
                if (resultsTable.isEditing()) {
                    SwingUtilities.invokeLater(() -> {
                        int col = resultsTable.convertColumnIndexToModel(resultsTable.getEditingColumn());
                        if (col >= 0) {
                            Map<String, Boolean> matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(col);
                            List<String> values = new ArrayList<>();
                            values.addAll(matches.keySet());
                            Collections.sort(values);
                            if (searchBox != null && values.size() > 0) {
                                searchBox.setTerms(values);
                            }
                        }
                    });
                } else {
                }
            }
        });
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
                    if (summary && !editable && e.getClickCount() == 2) {
                        showAlignmentDisplay();
                    } else if (summary && editable && e.getClickCount() == 1 && col > 1) {
                        Map<String, Boolean> matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(col);
                        List<String> values = new ArrayList<>();
                        values.addAll(matches.keySet());
                        Collections.sort(values);
                        if (searchBox != null && values.size() > 0) {
                            searchBox.setTerms(values);
                        }
                    }
                    if (e.getClickCount() == 1) {
                        List<String> values = new ArrayList<>();
                        Map<String, Boolean> matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(col);
                        if (col == 2) {
                            values = allNames;
                        } else {
                            values.addAll(matches.keySet());
                            Collections.sort(values);
                        }
                        if (findBox != null) {
                            findBox.setTerms(values, true);
                        } else {
                            createFindBox();
                            findBox.setTerms(values, true);
                        }
                        if (filterBox != null) {
                            filterBox.setTerms(matches, true, true);
                        } else {
                            createFilterBox();
                            filterBox.setTerms(matches, true, true);
                        }
                    }
                    if (e.isMetaDown() || e.isPopupTrigger()) {
                        doPopup(e);
                    }
                });
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isMetaDown() || e.isPopupTrigger()) {
                    doPopup(e);
                }
            }

            protected void doPopup(final MouseEvent e) {
                if (editable) {
                    pm.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        findButton.setMnemonic(KeyEvent.VK_F);
        enableEdit(false);
    }

    int oldX = 0;
    int oldY = 0;
    final JPopupMenu pm = new JPopupMenu();

    private List<TableColumn> nameColumns = new ArrayList<>();
    private List<TableColumn> countColumns = new ArrayList<>();
    private List<TableColumn> annotationColumns = new ArrayList<>();
    private List<TableColumn> biotypeColumns = new ArrayList<>();
    private Map<TableColumn, Integer> nameColumnsMap = new HashMap<>();
    private Map<TableColumn, Integer> countColumnsMap = new HashMap<>();
    private Map<TableColumn, Integer> annotationColumnsMap = new HashMap<>();
    private Map<TableColumn, Integer> biotypeColumnsMap = new HashMap<>();
    private final List<String> allNames = new ArrayList<>();

    private void createFindBox() {

        MouseAdapter mListener = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    JList source = (JList) e.getSource();
                    int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
                    col = (col < 0) ? 1 : col;
                    if (col >= 0 && ((SummaryTableModel) resultsTable.getModel()).getColumnClass(col).equals(String.class)) {
                        List<String> selectedValues = (List<String>) source.getSelectedValuesList();
                        if (!selectedValues.isEmpty()) {
                            List<Integer> rows = ((SummaryTableModel) resultsTable.getModel()).getRowsMatchingTerms(selectedValues, col);
                            resultsTable.getSelectionModel().clearSelection();
                            rows.stream().map((row) -> resultsTable.convertRowIndexToView(row)).forEach((r) -> {
                                resultsTable.getSelectionModel().addSelectionInterval(r, r);
                            });
                            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
                            int columnIndexToSort = col;
                            RowSorter.SortKey key = new RowSorter.SortKey(columnIndexToSort, SortOrder.ASCENDING);
                            sortKeys.add(key);
                            sorter.setSortKeys(sortKeys);
                            sorter.sort();
                            if (rows.size() > 0) {
                                int r = resultsTable.convertRowIndexToView(rows.get(rows.size() - 1));
                                int c = resultsTable.convertColumnIndexToView(col);
                                Rectangle rect = resultsTable.getCellRect(r, c, true);
                                resultsTable.scrollRectToVisible(rect);
                            }
                        }
                    }
                }
            }
        };

        findBox = new FindBox(mListener, experimentId);
    }

    private void createFilterBox() {
        ListSelectionListener selectionListener = (ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                JList source = (JList) e.getSource();
                ListModel model = (ListModel) source.getModel();
                ListSelectionModel lsm = source.getSelectionModel();
                int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
                col = (col < 0) ? 1 : col;
                if (col >= 0 && ((SummaryTableModel) resultsTable.getModel()).getColumnClass(col).equals(String.class)) {
                    int firstIndex = e.getFirstIndex();
                    int lastIndex = e.getLastIndex();
                    for (int index = firstIndex; index <= lastIndex; index++) {
                        String v = (String) model.getElementAt(index);
                        if (v != null) {
                            List<String> values = new ArrayList<>();
                            values.add(v);
                            List<Integer> rows = ((SummaryTableModel) resultsTable.getModel()).getRowsMatchingTerms(values, col);
                            SummaryWorker worker = ((SummaryTableModel) resultsTable.getModel()).getWorker();
                            if (filterBox != null) {
                                if (lsm.isSelectedIndex(index)) {
                                    rows.stream().map((i) -> ((SummaryTableModel) resultsTable.getModel()).getData().get(i).result).map((sr) -> {
                                        sr.filtered = true;
                                        return sr;
                                    }).forEach((sr) -> {
                                        worker.editSummary(sr, filterBox.isAllExperimentsSelected(), "admin", authToken);
                                    });
                                    ((SummaryTableModel) resultsTable.getModel()).removeRows(rows);
                                } else {
                                    rows.stream().map((i) -> ((SummaryTableModel) resultsTable.getModel()).getData().get(i).result).map((sr) -> {
                                        sr.filtered = false;
                                        return sr;
                                    }).forEach((sr) -> {
                                        worker.editSummary(sr, filterBox.isAllExperimentsSelected(), "admin", authToken);
                                    });
                                }
                            }
                        }
                    }
                }
            }
        };

        filterBox = new FilterBox(selectionListener, experimentId);
    }

    private Map<Integer, Integer> mappedColumns = new HashMap<>();

    private void refreshColumns() {
        int count = resultsTable.getColumnModel().getColumnCount();
        mappedColumns.clear();
        for (int c = count - 1; c >= 0; c--) {
            resultsTable.removeColumn(resultsTable.getColumnModel().getColumn(c));
        }
        int c = 0;
        for (TableColumn tc : nameColumns) {
            resultsTable.addColumn(tc);
            mappedColumns.put(c, nameColumnsMap.get(tc));
            c++;
        }
        if (showAnnotationsBox.isSelected()) {
            for (TableColumn tc : annotationColumns) {
                resultsTable.addColumn(tc);
                mappedColumns.put(c, annotationColumnsMap.get(tc));
                c++;
            }
        }
        if (showBiotypesBox.isSelected()) {
            for (TableColumn tc : biotypeColumns) {
                resultsTable.addColumn(tc);
                mappedColumns.put(c, biotypeColumnsMap.get(tc));
                c++;
            }
        }
        for (TableColumn tc : countColumns) {
            resultsTable.addColumn(tc);
            mappedColumns.put(c, countColumnsMap.get(tc));
            c++;
        }
    }

    private void showBiotypesBoxStateChanged(ChangeEvent ce) {
        if (ce.getSource() == showBiotypesBox) {
            refreshColumns();
        }
    }

    private void showAlignmentDisplay() {
        int selectedRow = resultsTable.getSelectedRow();
        final int selectedRowModel = resultsTable.convertRowIndexToModel(selectedRow);
        final SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();
        final Long eid = experimentId;
        final int key = model.pageOffset;
        CursorToolkitOne.startWaitCursor(resultsTable);
        Thread thread = new Thread() {
            @Override
            public void run() {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        AlignmentForm.setSearchGene(key, eid, distance, (String) model.getValueAt(selectedRowModel, 1));
                        AlignmentForm.atm.fireTableDataChanged();
                    }
                };
                t.start();
                try {
                    t.join();

                } catch (InterruptedException ex) {
                    Logger.getLogger(AlignmentForm.class
                            .getName()).log(Level.SEVERE, null, ex);
                }

                CursorToolkitOne.stopWaitCursor(resultsTable);
            }
        };
        thread.start();
    }

    private void editButtonActionPerformed(ActionEvent e) {
        if (editButton.isSelected()) {
            JPanel panel = new JPanel();
            JLabel label1 = new JLabel("Enter password:");
            final JPasswordField pass = new JPasswordField(10);
            panel.add(label1);
            panel.add(pass);
            String[] options = new String[]{"OK", "Cancel"};
            int option = JOptionPane.showOptionDialog(editButton, panel, "Authenticate Edit",
                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (option == 0) {
                try {
                    char[] password = pass.getPassword();
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update((new String(password)).getBytes());
                    byte[] digest = md.digest();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) {
                        sb.append(String.format("%02x", b & 0xff));
                    }
                    authToken = sb.toString();
                    if (alignmentWorker.authenticateUser("admin", authToken)) {
                        ((SummaryTableModel) resultsTable.getModel()).setAuthToken(authToken);
                        enableEdit(true);
                    } else {
                        editButton.setSelected(false);

                    }
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(DataUploadForm.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                editButton.setSelected(false);
                enableEdit(false);
            }
        } else {
            enableEdit(false);
        }
    }

    private void enableEdit(boolean edit) {
        for (int i = 0; i < resultsTable.getColumnCount(); i++) {
            TableColumn col = resultsTable.getColumnModel().getColumn(i);
            col.setCellEditor(new ResultsTableCellEditor(edit));
        }
        if (resultsTable.getModel() instanceof SummaryTableModel) {
            SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();
            model.setEditable(edit);
        }
        editable = edit;
        if (editable) {
            resultsTable.setCellSelectionEnabled(true);
            resultsTable.setColumnSelectionAllowed(true);
            ListSelectionModel selectionModel = resultsTable.getColumnModel().getSelectionModel();
            selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            resultsTable.setRowSelectionAllowed(true);
            termsButton.setEnabled(true);
            MouseListener listener = new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        if (e.getClickCount() == 2) {
                            JList source = (JList) e.getSource();
                            String value = (String) source.getSelectedValue();
                            if (value != null && value.contains(",")) {
                                value = "\"" + value + "\"";
                            }
                            int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
                            int[] rows = resultsTable.getSelectedRows();
                            SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();

                            if (col >= 0 && model.getColumnClass(col).equals(String.class)) {
                                for (int row : rows) {
                                    int r = resultsTable.convertRowIndexToModel(row);
                                    String v = (String) model.getValueAt(r, col);
                                    if (v.trim().endsWith(",")) {
                                        v = v.trim() + " " + value;
                                    } else if (v.trim().equals("-") || v.trim().equals("")) {
                                        v = value;
                                    } else {
                                        v = v.trim() + ", " + value;
                                    }
                                    model.setValueAt(v, r, col);
                                }

                                model.fireTableDataChanged();
                            }
                        }
                    });
                }
            };
            searchBox = new SearchBox(listener, experimentId);
            int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
            List<String> values = null;
            if (col > 0) {
                Map<String, Boolean> matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(col);
                if (col == 2) {
                    values = allNames;
                } else if (col > 2) {
                    if (values == null) {
                        values = new ArrayList<>();
                    }
                    values.addAll(matches.keySet());
                    Collections.sort(values);
                }
                if (searchBox != null) {
                    if (values != null && values.size() > 0) {
                        searchBox.setTerms(values);
                    }
                }
                if (findBox != null) {
                    if (values != null && values.size() > 0) {
                        findBox.setTerms(values, true);
                    }
                } else {
                    createFindBox();
                    if (values != null && values.size() > 0) {
                        findBox.setTerms(values, true);
                    }
                }
                if (filterBox != null) {
                    if (values != null && values.size() > 0) {
                        filterBox.setTerms(matches, true, true);
                    }
                } else {
                    createFilterBox();
                    if (values != null && values.size() > 0) {
                        filterBox.setTerms(matches, true, true);
                    }
                }
            }
        } else {
            resultsTable.setCellSelectionEnabled(false);
            resultsTable.setColumnSelectionAllowed(false);
            resultsTable.setRowSelectionAllowed(true);
            termsButton.setEnabled(false);
            if (searchBox != null) {
                searchBox.setVisible(false);
            }
            if (findBox != null) {
                findBox.setVisible(false);

            }
        }
        resultsTable.setDefaultRenderer(URL.class, new URLRenderer());
    }

    SearchBox searchBox = null;
    FindBox findBox = null;
    FilterBox filterBox = null;

    private void termsButtonActionPerformed(ActionEvent e) {
        if (searchBox != null) {
            searchBox.pack();
            searchBox.setAlwaysOnTop(true);
            searchBox.setVisible(true);
        }
    }

    private void findButtonActionPerformed(ActionEvent e) {
        List<String> values = allNames;
        if (resultsTable.getSelectedColumn() >= 0) {
            int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
            if (col == 2) {
                values = allNames;
            } else {
                Map<String, Boolean> matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(col);
                values.addAll(matches.keySet());
                Collections.sort(values);
            }
        } else {
            values = allNames;
        }
        if (findBox == null) {
            createFindBox();
        }
        if (findBox != null && values.size() > 0) {
            findBox.setTerms(values, true);
            findBox.setAlwaysOnTop(true);
            findBox.setLocation(this.getLocation());
            findBox.setVisible(true);
        }
    }

    private void showAnnotationsBoxStateChanged(ChangeEvent ce) {
        if (ce.getSource() == showAnnotationsBox) {
            refreshColumns();
        }
    }

    private void filterButtonActionPerformed(ActionEvent e) {
        if (filterButton.isSelected()) {
            JPanel panel = new JPanel();
            JLabel label1 = new JLabel("Enter password:");
            final JPasswordField pass = new JPasswordField(10);
            panel.add(label1);
            panel.add(pass);
            String[] options = new String[]{"OK", "Cancel"};
            int option = JOptionPane.showOptionDialog(filterButton, panel, "Authenticate Edit",
                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (option == 0) {
                try {
                    char[] password = pass.getPassword();
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update((new String(password)).getBytes());
                    byte[] digest = md.digest();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest) {
                        sb.append(String.format("%02x", b & 0xff));
                    }
                    authToken = sb.toString();
                    if (alignmentWorker.authenticateUser("admin", authToken)) {
                        ((SummaryTableModel) resultsTable.getModel()).setAuthToken(authToken);
                        List<String> values = allNames;
                        Map<String, Boolean> matches;
                        if (resultsTable.getSelectedColumn() >= 0) {
                            int col = resultsTable.convertColumnIndexToModel(resultsTable.getSelectedColumn());
                            matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(col);
                            if (col == 2) {
                                values = allNames;
                            } else {
                                values.addAll(matches.keySet());
                                Collections.sort(values);
                            }
                        } else {
                            values = allNames;
                            matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(0);
                        }
                        if (filterBox == null) {
                            createFindBox();
                        }
                        if (filterBox != null) {
                            filterBox.setTerms(matches, true, true);
                            filterBox.pack();
                            filterBox.setAlwaysOnTop(true);
                            filterBox.setLocation(this.getLocation());
                            filterBox.setVisible(true);
                        }
                    } else {
                        filterButton.setSelected(false);
                    }
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(DataUploadForm.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                filterButton.setSelected(false);
            }
        }
    }

    public void setSearchWorker(final SummaryWorker worker, boolean summary, List<SummaryResults> comparisons) {
        this.summary = summary;
        if (!summary) {
            showAnnotationsBox.setEnabled(false);
        }
        SummaryTableModel model = null;
        if (summary) {
            model = new SummaryTableModel(worker.getResults().size(), worker.getResults().size(), statusLabel, summary);
            sorter.setModel(model);
            for (int i = 0; i < model.getColumnCount(); i++) {
                Class klass = model.getColumnClass(i);
                sorter.setComparator(i, createComparator(klass));
            }
            sorter.setRowFilter(makeRowFilter(model.getPageSize(), 0));
        } else {
            model = new SummaryTableModel(comparisons.size(), comparisons.size(), statusLabel, summary, comparisons);
            sorter.setModel(model);
            for (int i = 0; i < model.getColumnCount(); i++) {
                Class klass = model.getColumnClass(i);
                sorter.setComparator(i, createComparator(klass));
            }
            sorter.setRowFilter(makeRowFilter(model.comparisons.size(), 0));
            showBiotypesBox.setEnabled(false);
        }
        model.setWorker(worker);
        model.addTableModelListener((TableModelEvent tme) -> {
            SwingUtilities.invokeLater(() -> {
                Map<String, Boolean> matches = ((SummaryTableModel) resultsTable.getModel()).getUniqueValuesInColumn(2);
                allNames.addAll(matches.keySet());
                Collections.sort(allNames);
                refreshColumns();
                resultsTable.repaint();
            });
        });

        if (summary) {
            if (worker.getResults().isEmpty()) {
                statusLabel.setText("No results found...");
            } else {
                statusLabel.setText("1, " + worker.getResults().size() + " of " + worker.getResults().size() + " results");
            }
        } else if (comparisons.isEmpty()) {
            statusLabel.setText("No results found...");
        } else {
            statusLabel.setText("1, " + comparisons.size() + " of " + comparisons.size() + " results");
        }

        resultsTable.setModel(model);
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.setRowSorter(sorter);

        nameColumns.clear();
        countColumns.clear();
        annotationColumns.clear();
        biotypeColumns.clear();
        int count = resultsTable.getModel().getColumnCount();
        for (int i = 0; i < count; i++) {
            String name = resultsTable.getModel().getColumnName(i);
            Font f = resultsTable.getFont();
            FontMetrics fm = resultsTable.getFontMetrics(f);
            int width = fm.stringWidth(name);
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(width);
            if (summary) {
                if (i >= 26 && i < 40) {
                    biotypeColumns.add(resultsTable.getColumnModel().getColumn(i));
                    biotypeColumnsMap.put(resultsTable.getColumnModel().getColumn(i), i);
                } else if (i >= 2 && i < 26) {
                    annotationColumns.add(resultsTable.getColumnModel().getColumn(i));
                    annotationColumnsMap.put(resultsTable.getColumnModel().getColumn(i), i);
                } else if (i >= 0 && i < 2) {
                    nameColumns.add(resultsTable.getColumnModel().getColumn(i));
                    nameColumnsMap.put(resultsTable.getColumnModel().getColumn(i), i);
                } else if (i >= 40 && i < 49) {
                    countColumns.add(resultsTable.getColumnModel().getColumn(i));
                    countColumnsMap.put(resultsTable.getColumnModel().getColumn(i), i);
                }
            } else if (i >= 0 && i < 4) {
                nameColumns.add(resultsTable.getColumnModel().getColumn(i));
                nameColumnsMap.put(resultsTable.getColumnModel().getColumn(i), i);
            } else if (i >= 4) {
                countColumns.add(resultsTable.getColumnModel().getColumn(i));
                countColumnsMap.put(resultsTable.getColumnModel().getColumn(i), i);
            }
        }

        this.remove(jScrollPane1);
        jScrollPane1 = SummaryTableModel.createPagingScrollPaneForTable(resultsTable);
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
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        jToolBar1 = new JToolBar();
        saveButton = new JButton();
        copyButton = new JButton();
        termsButton = new JButton();
        findButton = new JButton();
        showBiotypesBox = new JCheckBox();
        showAnnotationsBox = new JCheckBox();
        statusLabel = new JLabel();
        editButton = new JToggleButton();
        filterButton = new JToggleButton();
        jScrollPane1 = new JScrollPane();
        resultsTable = new JTable();

        //======== this ========

        //======== jToolBar1 ========
        {
            jToolBar1.setRollover(true);

            //---- saveButton ----
            saveButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/document-save22.png")));
            saveButton.setFocusable(false);
            saveButton.setHorizontalTextPosition(SwingConstants.CENTER);
            saveButton.setMaximumSize(new Dimension(25, 25));
            saveButton.setMinimumSize(new Dimension(25, 25));
            saveButton.setPreferredSize(new Dimension(25, 25));
            saveButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            saveButton.setToolTipText("Save");
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveButtonActionPerformed(e);
                }
            });
            jToolBar1.add(saveButton);
            jToolBar1.addSeparator();

            //---- copyButton ----
            copyButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/edit-copy22.png")));
            copyButton.setFocusable(false);
            copyButton.setHorizontalTextPosition(SwingConstants.CENTER);
            copyButton.setMaximumSize(new Dimension(25, 25));
            copyButton.setMinimumSize(new Dimension(25, 25));
            copyButton.setPreferredSize(new Dimension(25, 25));
            copyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            copyButton.setToolTipText("Copy");
            copyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyButtonActionPerformed(e);
                }
            });
            jToolBar1.add(copyButton);
            jToolBar1.addSeparator();

            //---- termsButton ----
            termsButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/gear-2.0.png")));
            termsButton.setToolTipText("Terms");
            termsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    termsButtonActionPerformed(e);
                }
            });
            jToolBar1.add(termsButton);
            jToolBar1.addSeparator();

            //---- findButton ----
            findButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/find.png")));
            findButton.setToolTipText("Find");
            findButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    findButtonActionPerformed(e);
                }
            });
            jToolBar1.add(findButton);
            jToolBar1.addSeparator();

            //---- showBiotypesBox ----
            showBiotypesBox.setText("Show Biotypes");
            showBiotypesBox.setSelected(true);
            showBiotypesBox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    showBiotypesBoxStateChanged(e);
                }
            });
            jToolBar1.add(showBiotypesBox);
            jToolBar1.addSeparator();

            //---- showAnnotationsBox ----
            showAnnotationsBox.setText("Show Annotations");
            showAnnotationsBox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    showAnnotationsBoxStateChanged(e);
                }
            });
            jToolBar1.add(showAnnotationsBox);
            jToolBar1.addSeparator();
            jToolBar1.add(statusLabel);
            jToolBar1.addSeparator();

            //---- editButton ----
            editButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/edit-pie.png")));
            editButton.setToolTipText("Edit");
            editButton.setPreferredSize(new Dimension(32, 26));
            editButton.setMaximumSize(new Dimension(32, 26));
            editButton.setMinimumSize(new Dimension(32, 26));
            editButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editButtonActionPerformed(e);
                }
            });
            jToolBar1.add(editButton);
            jToolBar1.addSeparator();

            //---- filterButton ----
            filterButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/filter.png")));
            filterButton.setPreferredSize(new Dimension(32, 26));
            filterButton.setMaximumSize(new Dimension(32, 26));
            filterButton.setMinimumSize(new Dimension(32, 26));
            filterButton.setToolTipText("Filter");
            filterButton.setSelectedIcon(null);
            filterButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filterButtonActionPerformed(e);
                }
            });
            jToolBar1.add(filterButton);
        }

        //======== jScrollPane1 ========
        {

            //---- resultsTable ----
            resultsTable.setModel(new DefaultTableModel(
                new Object[][] {
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null},
                },
                new String[] {
                    "Title 1", "Title 2", "Title 3", "Title 4"
                }
            ));
            resultsTable.setCellSelectionEnabled(true);
            jScrollPane1.setViewportView(resultsTable);
        }

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup()
                .addGroup(layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addComponent(jToolBar1, GroupLayout.DEFAULT_SIZE, 670, Short.MAX_VALUE)
                        .addGap(0, 0, 0)))
                .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 670, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup()
                .addGroup(layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, 0)
                        .addComponent(jToolBar1, GroupLayout.PREFERRED_SIZE, 27, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)))
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(29, 29, 29)
                    .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE))
        );
    }// </editor-fold>                        

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        FileFilter fileFilter = new FileNameExtensionFilter("Table as TXT Files", "txt");
        JFileChooser fc = new JFileChooser();
        fc.setName("Save Summary Results");
        fc.setSelectedFile(new File(label + ".txt"));
        fc.addChoosableFileFilter(fileFilter);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(fileFilter);
        int option = fc.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION == option) {
            if (fc.getFileFilter() == fileFilter) {
                try {
                    File file = fc.getSelectedFile();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                        if (summary) {
                            SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();
                            SummaryWorker worker = model.getWorker();
                            List<SummaryResults> results = worker.getResults();
                            int rows = results.size();
                            int columns = model.getColumnCount();
                            for (int i = 0; i < columns; i++) {
                                bw.write(model.getColumnName(i));
                                bw.write("\t");
                            }
                            bw.newLine();
                            for (int i = 0; i < rows; i++) {
                                SummaryResults result = results.get(i);
                                bw.write(result.geneId);
                                bw.write("\t");
                                String symbol = (result.geneSymbol == null) ? " " : result.geneSymbol;
                                bw.write(symbol);
                                bw.write("\t");
                                List<Integer> annotations = result.annotations;
                                Integer sc = 0;
                                Integer stc = 0;
                                Integer ss = 0;
                                Integer polya = 0;
                                Integer cs = 0;
                                Integer variants = 0;
                                Integer sirna = 0;
                                Integer acs = 0;
                                Integer afsse = 0;
                                Integer atsse = 0;
                                Integer fse = 0;
                                Integer lse = 0;
                                Integer mee = 0;
                                Integer ritu = 0;
                                Integer ri = 0;
                                Integer ins = 0;
                                Integer del = 0;
                                Integer tra = 0;
                                Integer ff = 0;
                                Integer other = 0;
                                Integer promoter = 0;
                                Integer antiSense = 0;
                                Integer ssDiff = 0;
                                Integer scDiff = 0;
                                if (annotations.size() == 24) {
                                    sc = annotations.get(0);
                                    stc = annotations.get(1);
                                    ss = annotations.get(2);
                                    polya = annotations.get(3);
                                    cs = annotations.get(4);
                                    variants = annotations.get(5);
                                    sirna = annotations.get(6);
                                    acs = annotations.get(7);
                                    afsse = annotations.get(8);
                                    atsse = annotations.get(9);
                                    fse = annotations.get(10);
                                    lse = annotations.get(11);
                                    mee = annotations.get(12);
                                    ritu = annotations.get(13);
                                    ri = annotations.get(14);
                                    ins = annotations.get(15);
                                    del = annotations.get(16);
                                    tra = annotations.get(17);
                                    ff = annotations.get(18);
                                    other = annotations.get(19);
                                    promoter = annotations.get(20);
                                    antiSense = annotations.get(21);
                                    ssDiff = annotations.get(22);
                                    scDiff = annotations.get(23);
                                }
                                bw.write(sc.toString());
                                bw.write("\t");
                                bw.write(stc.toString());
                                bw.write("\t");
                                bw.write(ss.toString());
                                bw.write("\t");
                                bw.write(polya.toString());
                                bw.write("\t");
                                bw.write(cs.toString());
                                bw.write("\t");
                                bw.write(variants.toString());
                                bw.write("\t");
                                bw.write(sirna.toString());
                                bw.write("\t");
                                bw.write(acs.toString());
                                bw.write("\t");
                                bw.write(afsse.toString());
                                bw.write("\t");
                                bw.write(atsse.toString());
                                bw.write("\t");
                                bw.write(fse.toString());
                                bw.write("\t");
                                bw.write(lse.toString());
                                bw.write("\t");
                                bw.write(mee.toString());
                                bw.write("\t");
                                bw.write(ritu.toString());
                                bw.write("\t");
                                bw.write(ri.toString());
                                bw.write("\t");
                                bw.write(ins.toString());
                                bw.write("\t");
                                bw.write(del.toString());
                                bw.write("\t");
                                bw.write(tra.toString());
                                bw.write("\t");
                                bw.write(ff.toString());
                                bw.write("\t");
                                bw.write(other.toString());
                                bw.write("\t");
                                bw.write(promoter.toString());
                                bw.write("\t");
                                bw.write(antiSense.toString());
                                bw.write("\t");
                                bw.write(ssDiff.toString());
                                bw.write("\t");
                                bw.write(scDiff.toString());
                                bw.write("\t");
                                String synonyms = result.synonymousNames;
                                bw.write(synonyms);
                                bw.write("\t");
                                String levelIC = result.levelIclassification;
                                bw.write(levelIC);
                                bw.write("\t");
                                String levelIIC = result.levelIIclassification;
                                bw.write(levelIIC);
                                bw.write("\t");
                                String levelIIIC = result.levelIIIclassification;
                                bw.write(levelIIIC);
                                bw.write("\t");
                                String levelIVC = result.levelIVclassification;
                                bw.write(levelIVC);
                                bw.write("\t");
                                String hierarchy1 = result.hierarchyup;
                                bw.write(hierarchy1);
                                bw.write("\t");
                                String hierarchy2 = result.hierarchydown;
                                bw.write(hierarchy2);
                                bw.write("\t");
                                String hierarchy3 = result.hierarchy0isoform;
                                bw.write(hierarchy3);
                                bw.write("\t");
                                String hierarchy4 = result.hierarchy0mutation;
                                bw.write(hierarchy4);
                                bw.write("\t");
                                String hierarchy5 = result.hierarchy0other;
                                bw.write(hierarchy5);
                                bw.write("\t");
                                String functionLevelI = result.levelIfunction;
                                bw.write(functionLevelI);
                                bw.write("\t");
                                String geneStructureLevelI = result.levelIgenestructure;
                                bw.write(geneStructureLevelI);
                                bw.write("\t");
                                String disease = result.disease;
                                bw.write(disease);
                                bw.write("\t");
                                String roleInCancer = result.roleincancers;
                                bw.write(roleInCancer);
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
                                bw.write("\t");
                                Integer rpkm_rnk = result.rpkmRank;
                                bw.write(rpkm_rnk.toString());

                                bw.newLine();
                            }
                        } else {
                            SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();
                            List<SummaryResults> results = model.comparisons;
                            int rows = results.size();
                            int columns = model.getColumnCount();
                            for (int i = 0; i < columns; i++) {
                                bw.write(model.getColumnName(i));
                                bw.write("\t");
                            }
                            bw.newLine();

                            for (int i = 0; i < rows; i++) {
                                SummaryResults result = results.get(i);
                                bw.write(result.geneId);
                                bw.write("\t");
                                String symbol = (result.geneSymbol == null) ? " " : result.geneSymbol;
                                bw.write(symbol);
                                bw.write("\t");
                                String levelIIC = result.levelIIclassification;
                                bw.write(levelIIC);
                                bw.write("\t");
                                String levelIF = result.levelIfunction;
                                bw.write(levelIF);
                                bw.write("\t");

                                Double rpkm = result.rpkm;
                                bw.write(rpkm.toString());
                                bw.write("\t");
                                Double rpkm1 = result.rpkm1;
                                bw.write(rpkm1.toString());
                                bw.write("\t");
                                Integer rpkm_rnk = result.rpkmRank;
                                bw.write(rpkm_rnk.toString());
                                bw.write("\t");
                                Integer rpkm_rnk1 = result.rpkmRank1;
                                bw.write(rpkm_rnk1.toString());
                                bw.write("\t");
                                Double rpkm_ratio = result.rpkmDiff;
                                bw.write(rpkm_ratio.toString());

                                bw.newLine();
                            }
                        }
                        bw.flush();
                    }
                } catch (IOException ioe) {
                }
            }
        }
    }                                          

    private void replaceAllAction(ActionEvent e) {
        if (summary && editButton.isSelected()) {
            JTextField xField = new JTextField(15);
            JTextField yField = new JTextField(15);

            Object[] message = {
                "Search:", xField,
                "Replace:", yField
            };

            int result = JOptionPane.showConfirmDialog(null, message,
                    "Enter Search and Replace Values", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String search = xField.getText();
                String replace = yField.getText();
                SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();
                model.getWorker().editBiotypes(search, replace, "admin", authToken);
                model.fireTableDataChanged();
                model.getWorker().loadResults(key, experimentId, distance);
                setSearchWorker(model.worker, true, null);
            }
        }
    }

    private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        try {
            SummaryTableModel model = (SummaryTableModel) resultsTable.getModel();
            int[] rows = resultsTable.getSelectedRows();
            BufferedWriter bw;
            try (StringWriter sw = new StringWriter()) {
                bw = new BufferedWriter(sw);
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
            }
            bw.close();

        } catch (IOException ex) {
            Logger.getLogger(SummaryResultsPanel.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }                                          

    // Variables declaration - do not modify                     
    // Generated using JFormDesigner non-commercial license
    private JToolBar jToolBar1;
    private JButton saveButton;
    private JButton copyButton;
    private JButton termsButton;
    private JButton findButton;
    private JCheckBox showBiotypesBox;
    private JCheckBox showAnnotationsBox;
    private JLabel statusLabel;
    private JToggleButton editButton;
    private JToggleButton filterButton;
    private JScrollPane jScrollPane1;
    private JTable resultsTable;
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
        @SuppressWarnings("CallToPrintStackTrace")
        public void mouseClicked(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            Point pt = e.getPoint();
            int ccol = table.columnAtPoint(pt);

            if (isURLColumn(table, ccol)) { // && pointInsidePrefSize(table, pt)) {
                int crow = table.rowAtPoint(pt);
                Object selected = table.getValueAt(crow, ccol);
                if (selected instanceof URL) {
                    URL url = (URL) selected;
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

    private RowFilter< SummaryTableModel, Integer> makeRowFilter(final int itemsPerPage, final int target) {
        return new RowFilter< SummaryTableModel, Integer>() {
            @Override
            public boolean include(Entry< ? extends SummaryTableModel, ? extends Integer> entry) {
                int ei = entry.getIdentifier();
                return (target * itemsPerPage <= ei && ei < target * itemsPerPage + itemsPerPage);
            }
        };
    }

    private Comparator createComparator(final Class klass) {
        return (Comparator) (Object t, Object t1) -> ((Comparable) klass.cast(t)).compareTo((Comparable) klass.cast(t1));
    }

    class CopyAction extends AbstractAction {

        private final JTable table;

        public CopyAction(JTable table) {
            this.table = table;
            putValue(NAME, "Copy");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();

            tt.setClipboardContents(table.getValueAt(row, col).toString());
            table.repaint();
        }

    }

    class PasteAction extends AbstractAction {

        private final JTable table;

        public PasteAction(JTable tbl) {
            putValue(NAME, "Paste");
            table = tbl;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = table.getSelectedRows();
            int[] columns = table.getSelectedColumns();

            Object value = tt.getClipboardContents();
            for (int row : rows) {
                for (int col : columns) {
                    if (mappedColumns.containsKey(col)) {
                        int c = mappedColumns.get(col);
                        if (((SummaryTableModel) table.getModel()).isCellEditable(row, c)) {
                            ((SummaryTableModel) table.getModel()).setValueAt(value, row, c);
                        }
                    }
                }
            }
            table.repaint();
        }
    }

    static class CellTransferable implements Transferable {

        public static final DataFlavor CELL_DATA_FLAVOR = new DataFlavor(Object.class, "application/x-cell-value");

        private final Object cellValue;

        public CellTransferable(Object cellValue) {
            this.cellValue = cellValue;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{CELL_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return CELL_DATA_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return cellValue;
        }
    }

    class ResultsTableCellEditor extends AbstractCellEditor implements TableCellEditor {

        JComponent component = new JTextField();
        boolean editable;

        public ResultsTableCellEditor(boolean e) {
            editable = e;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                int rowIndex, int vColIndex) {

            ((JTextField) component).setText((String) value);

            return component;
        }

        @Override
        public Object getCellEditorValue() {
            return ((JTextField) component).getText();
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                return ((MouseEvent) e).getClickCount() >= 2;
            } else if (e instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) e;
                return ke.getKeyCode() == KeyEvent.VK_F2;
            }
            return editable;
        }
    }
}

