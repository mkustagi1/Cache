package cache;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import cache.dataimportes.holders.TranscriptMappingResults;
import cache.util.ButtonTabComponent;
import cache.util.CursorToolkitOne;
import cache.workers.AlignmentWorker;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
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
import java.util.HashSet;
import java.util.Set;

/**
 * @author Manjunath Kustagi
 */
public class AlignmentForm extends JPanel {

    public static PagingModel atm;
    private static PagingModel oldModel;
    static AlignmentWorker alignmentWorker;
    static AlignmentForm form;
    static Map<Long, List<Long>> mt = new HashMap<>();

    public static void setSearchTranscripts(List<Long> tids) {
        Set<Long> tSet = new HashSet<>(tids);
        tids = new ArrayList<>(tSet);
        PagingModel model = new PagingModel(tids.size(), tids.size(), null);
        model.setAlignmentSource(alignmentWorker);
        model.addTableModelListener((TableModelEvent tme) -> {
            alignmentTable.repaint();
        });
        model.setData(tids);
        oldModel = (PagingModel) alignmentTable.getModel();
        atm = model;
        alignmentTable.setModel(atm);
        atm.fireTableDataChanged();
    }

    public static void setSearchGene(int key, long experimentId, int distance, String gene) {
        if (gene != null && !gene.equals("")) {
            alignmentWorker.setExperimentID(key, experimentId, distance);
            int count = 1;
//            count = alignmentWorker.getTranscriptCountForGene(gene);
            PagingModel model = new PagingModel(count, count, null);
            model.setAlignmentSource(alignmentWorker);
            model.addTableModelListener((TableModelEvent tme) -> {
                alignmentTable.repaint();
            });
            model.setData(gene);
            oldModel = (PagingModel) alignmentTable.getModel();
            atm = model;
            alignmentTable.setModel(atm);
            atm.fireTableDataChanged();
        } else if (oldModel != null) {
            atm = oldModel;
            alignmentTable.setModel(atm);
            atm.fireTableDataChanged();
        }
    }

    public static void setPagingModel(int key, long experimentId, int distance) {
        alignmentWorker.setExperimentID(key, experimentId, distance);
        mt = alignmentWorker.loadMappedTranscripts();
        long mappedTranscripts = mt.size();
        atm = new PagingModel(mappedTranscripts, 100, mt);
        atm.setAlignmentSource(alignmentWorker);
        atm.addTableModelListener((TableModelEvent tme) -> {
            alignmentTable.repaint();
        });
        alignmentTable.setModel(atm);
        panel1.remove(scrollPane1);
        scrollPane1 = atm.createPagingScrollPaneForTable(alignmentTable);
        scrollPane1.setViewportView(alignmentTable);
        panel1.add(scrollPane1, CC.xywh(1, 2, 2, 3, CC.FILL, CC.FILL));
        atm.fireTableDataChanged();
    }

    public AlignmentForm() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        dialog = new JDialog(topFrame, false);
        initComponents();
        atm = new PagingModel();
        alignmentWorker = new AlignmentWorker();
        atm.setAlignmentSource(alignmentWorker);
        atm.addTableModelListener((TableModelEvent tme) -> {
            alignmentTable.repaint();
        });
        alignmentTable.setModel(atm);

        alignmentTable.setRowSelectionAllowed(true);
        alignmentTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        alignmentTable.setShowGrid(true);
        alignmentTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        alignmentTable.setAutoCreateRowSorter(true);

        TableColumn tcol = alignmentTable.getColumnModel().getColumn(2);
        tcol.setPreferredWidth(70);

        URLRenderer renderer = new URLRenderer();
        alignmentTable.setDefaultRenderer(URL.class, renderer);
        alignmentTable.addMouseListener(renderer);
        alignmentTable.addMouseMotionListener(renderer);

        form = this;
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
        dialog.setFocusableWindowState(false);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);
    }
    JDialog dialog;
    PopUpJList ppjl;

    private void searchBoxDocumentChanged() {
        String text = searchText.getText();
        if (text != null && text.length() >= 2) {
            List<String> symbols = alignmentWorker.getGenesByPartialSymbol(text);
            ppjl = new PopUpJList(searchText, symbols);
            dialog.getContentPane().removeAll();
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
        } else if (text != null && text.length() < 2) {
            dialog.setVisible(false);
        }
    }

    private void alignmentTableMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            final int srow = alignmentTable.getSelectedRow();
            final int row = alignmentTable.convertRowIndexToModel(srow);
            CursorToolkitOne.startWaitCursor(alignmentTable);
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        TranscriptMappingResults result = atm.getAlignmentSource().populateAlignmentDisplay(atm.getData().get(row).result);
                        final AlignmentDisplayPanelJavaFX display = new AlignmentDisplayPanelJavaFX();
                        display.setExperimentId((Long) atm.getData().get(row).data[0]);
                        display.setName(result.name);
                        display.setText(result);
                        Long expId = 0l;
                        Object o = atm.getValueAt(row, 0);
                        if (o instanceof Long) {
                            expId = (Long) o;
                        }
                        final String label = result.name + "_" + expId.toString();
                        JScrollPane scrollPane = new JScrollPane(display);
                        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                        ButtonTabComponent tabComponent = new ButtonTabComponent(tabbedPane1);
                        tabbedPane1.add(scrollPane, label);
                        tabbedPane1.setTabComponentAt(tabbedPane1.getTabCount() - 1, tabComponent);
                        tabbedPane1.setSelectedComponent(scrollPane);
                        CursorToolkitOne.stopWaitCursor(alignmentTable);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            t.start();
        }
    }

    private void runAlignmentActionPerformed(ActionEvent e) {
    }

    private void comboBox1ActionPerformed(ActionEvent e) {
        // TODO add your code here
    }

    private void searchActionPerformed(ActionEvent e) {
        final String name = (ppjl.list.getSelectedValue() == null) ? searchText.getText().trim() : (String) ppjl.list.getSelectedValue();
        searchText.setText(name);
        dialog.getContentPane().removeAll();
        dialog.setVisible(false);
        if (name != null && !name.equals("")) {
            int count = 1;
            count = alignmentWorker.getTranscriptCountForGene(name);
            PagingModel model = new PagingModel(count, count, mt);
            model.setAlignmentSource(alignmentWorker);
            model.addTableModelListener((TableModelEvent tme) -> {
                alignmentTable.repaint();
            });
            model.setData(name);
            oldModel = (PagingModel) alignmentTable.getModel();
            atm = model;
            alignmentTable.setModel(atm);
            atm.fireTableDataChanged();
        } else if (oldModel != null) {
            atm = oldModel;
            alignmentTable.setModel(atm);
            atm.fireTableDataChanged();
        }
    }

    private void searchTextActionPerformed(ActionEvent e) {
        searchActionPerformed(e);
    }

    private void searchTextFocusGained(FocusEvent e) {
        int caretPosition = searchText.getCaretPosition();
        searchText.setSelectionStart(0);
        searchText.setSelectionEnd(0);
        searchText.setCaretPosition(caretPosition);
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

    private void thisComponentHidden(ComponentEvent e) {
        dialog.setVisible(false);
    }

    private void editName() {
        final int srow = alignmentTable.getSelectedRow();
        final int row = alignmentTable.convertRowIndexToModel(srow);
        TranscriptMappingResults tmr = atm.getData().get(row).result;
        JPanel panel = new JPanel();
        JPanel panel0 = new JPanel();
        JPanel panel2 = new JPanel();
        JLabel label1 = new JLabel("Enter Name:");

        final JTextField nName = new JTextField(25);
        nName.setText(tmr.name);
        nName.setCaretPosition(0);
        JScrollBar scrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
        BoundedRangeModel brm = nName.getHorizontalVisibility();
        scrollBar.setModel(brm);
        panel3.add(nName);
        panel3.add(scrollBar);

        JLabel label = new JLabel("Enter password:");
        final JPasswordField pass = new JPasswordField(10);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel0.add(label1);
        panel0.add(panel3);
        panel2.add(label);
        panel2.add(pass);
        panel.add(panel0);
        panel.add(panel2);
        String[] options = new String[]{"OK", "Cancel"};
        int option = JOptionPane.showOptionDialog(alignmentTable, panel, "Authenticate and Enter New Name",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (option == 0) {

            try {
                final String newName = nName.getText();
                CursorToolkitOne.startWaitCursor(alignmentTable);
                ExecutorService ex = Executors.newFixedThreadPool(1, (Runnable r) -> {
                    Thread t = new Thread(r);
                    t.setPriority(Thread.MAX_PRIORITY);
                    return t;
                });
                Task task = new Task() {

                    @Override
                    protected Object call() throws Exception {
                        try {
                            char[] password = pass.getPassword();
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            md.update((new String(password)).getBytes());
                            byte[] digest = md.digest();
                            StringBuilder sb = new StringBuilder();
                            for (byte b : digest) {
                                sb.append(String.format("%02x", b & 0xff));
                            }
                            final int srow = alignmentTable.getSelectedRow();
                            final int row = alignmentTable.convertRowIndexToModel(srow);
                            TranscriptMappingResults tmr = atm.getData().get(row).result;
                            AlignmentWorker worker = new AlignmentWorker();
                            worker.editTranscriptName(tmr.transcriptID, newName, sb.toString());
                            atm.fireTableDataChanged();
                            CursorToolkitOne.stopWaitCursor(alignmentTable);
                        } catch (NoSuchAlgorithmException ex) {
                            Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        return Boolean.TRUE;
                    }
                };

                Future f = ex.submit(task);
                f.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void deleteTranscript() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter password:");
        final JPasswordField pass = new JPasswordField(10);
        panel.add(label);
        panel.add(pass);
        String[] options = new String[]{"OK", "Cancel"};
        int option = JOptionPane.showOptionDialog(alignmentTable, panel, "Authenticate Transcript Removal",
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        if (option == 0) {
            final int srow = alignmentTable.getSelectedRow();
            final int row = alignmentTable.convertRowIndexToModel(srow);

            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        char[] password = pass.getPassword();
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        md.update((new String(password)).getBytes());
                        byte[] digest = md.digest();
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) {
                            sb.append(String.format("%02x", b & 0xff));
                        }
                        String authToken = sb.toString();
                        if (alignmentWorker.authenticateUser("admin", authToken)) {

                            TranscriptMappingResults tmr = atm.getData().get(row).result;
                            atm.removeRow(srow);
                            AlignmentWorker worker = new AlignmentWorker();
                            worker.deleteTranscript(tmr.transcriptID, sb.toString());
                        }
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            t.start();
        }
    }

    private JPopupMenu createPopup() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem menuItem1 = new JMenuItem("Delete Transcript");
        menuItem1.addActionListener((ActionEvent e) -> {
            deleteTranscript();
        });
        menuItem1.setToolTipText("Deletes Transcript from DB");
        menu.add(menuItem1);

        JMenuItem menuItem2 = new JMenuItem("Edit Transcript Name");
        menuItem2.addActionListener((ActionEvent e) -> {
            editName();
        });
        menuItem2.setToolTipText("Change Transcript Name");
        menu.add(menuItem2);

        return menu;
    }

    private void alignmentTableMouseReleased(MouseEvent e) {
        final int srow = alignmentTable.getSelectedRow();
        final int row = alignmentTable.convertRowIndexToModel(srow);
        if (row < 0) {
            return;
        }
        if (e.isMetaDown() && e.getComponent() instanceof JTable) {
            JPopupMenu popup = createPopup();
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        splitPane1 = new JSplitPane();
        panel1 = new JPanel();
        searchText = new JTextField();
        button1 = new JButton();
        scrollPane1 = new JScrollPane();
        alignmentTable = new JTable();
        tabbedPane1 = new JTabbedPane();

        //======== this ========
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                thisComponentHidden(e);
            }
        });
        setLayout(new FormLayout(
                "111dlu:grow, 48dlu:grow, 71dlu:grow",
                "18dlu, $lgap, fill:342dlu:grow"));

        //======== splitPane1 ========
        {
            splitPane1.setDividerLocation(400);
            splitPane1.setOneTouchExpandable(true);

            //======== panel1 ========
            {
                panel1.setLayout(new FormLayout(
                        "111dlu:grow, default",
                        "18dlu, 133dlu:grow, 146dlu, 30dlu:grow"));

                //---- searchText ----
                searchText.setToolTipText("Enter Ensembl ID to search for");
                searchText.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        searchTextActionPerformed(e);
                    }
                });
                searchText.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        searchTextFocusGained(e);
                    }
                });
                searchText.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        searchTextKeyReleased(e);
                    }
                });
                panel1.add(searchText, CC.xy(1, 1));

                //---- button1 ----
                button1.setText("Search");
                button1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        searchActionPerformed(e);
                    }
                });
                panel1.add(button1, CC.xy(2, 1));

                //======== scrollPane1 ========
                {

                    //---- alignmentTable ----
                    alignmentTable.setFillsViewportHeight(true);
                    alignmentTable.setCellSelectionEnabled(true);
                    alignmentTable.setSurrendersFocusOnKeystroke(true);
                    alignmentTable.setAutoCreateRowSorter(true);
                    alignmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    alignmentTable.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            alignmentTableMouseClicked(e);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            alignmentTableMouseReleased(e);
                        }
                    });
                    scrollPane1.setViewportView(alignmentTable);
                }
                panel1.add(scrollPane1, CC.xywh(1, 2, 2, 3, CC.FILL, CC.FILL));
            }
            splitPane1.setLeftComponent(panel1);
            splitPane1.setRightComponent(tabbedPane1);
        }
        add(splitPane1, CC.xywh(1, 3, 3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JSplitPane splitPane1;
    private static JPanel panel1;
    private JTextField searchText;
    private JButton button1;
    private static JScrollPane scrollPane1;
    private static JTable alignmentTable;
    private JTabbedPane tabbedPane1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

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
