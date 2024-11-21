package cache;

import java.awt.Component;
import java.awt.Font;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import cache.util.CursorToolkitOne;
import cache.util.TooltipTable;
import cache.workers.DataUploadWorker;
import cache.workers.ExperimentTableWorker;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.LineBorder;

/**
 * @author Manjunath Kustagi
 */
public class DataUploadForm extends JPanel {

    DataUploadWorker uploadWorker;
    ExperimentTableModel etm;
    int selectedRow;
    String selectedFile;

    public DataUploadForm() {
        initComponents();

        ExperimentTableWorker expWorker = new ExperimentTableWorker();
        etm = new ExperimentTableModel(statusLabel);
        etm.setDataUploadSource(expWorker);
        dataTable.setModel(etm);

        editButton.setSelected(false);
        enableEdit(false);
        dataTable.setDefaultRenderer(String.class, new BoldRenderer());

        dataTable.getSelectionModel().addListSelectionListener((ListSelectionEvent lse) -> {
            if (!lse.getValueIsAdjusting()) {
                listSelectionChanged(lse);
            }
        });

        etm.addTableModelListener(dataTable);

        remove(scrollPane1);

        scrollPane1 = ExperimentTableModel.createPagingScrollPaneForTable(dataTable);
        scrollPane1.setViewportView(dataTable);
        add(scrollPane1, CC.xywh(1, 2, 8, 14));
    }

    class BoldRenderer extends DefaultTableCellRenderer {

        public BoldRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            }
            return c;
        }
    }

    private void listSelectionChanged(ListSelectionEvent lse) {
        selectedRow = dataTable.getSelectedRow();
        selectedRow = dataTable.convertRowIndexToModel(selectedRow);
    }

    private void dataTableMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
            selectedRow = dataTable.getSelectedRow();
            selectedRow = dataTable.convertRowIndexToModel(selectedRow);
        } else if (e.getClickCount() == 2) {
            CursorToolkitOne.startWaitCursor(this);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            selectedRow = dataTable.getSelectedRow();
                            selectedRow = dataTable.convertRowIndexToModel(selectedRow);
                            AlignmentForm.setPagingModel(etm.pageOffset, etm.getExperiment(selectedRow).experimentID, etm.getExperiment(selectedRow).distance);
                            AlignmentForm.atm.fireTableDataChanged();
                        }
                    };
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    CursorToolkitOne.stopWaitCursor(DataUploadForm.this);
                }
            };
            thread.start();
        }
    }

    private void enableEdit(boolean edit) {
        Component[] components = this.getComponents();
        for (Component component : components) {
            if (component instanceof JComboBox) {
                ((JComboBox) component).setEditable(edit);
                ((JComboBox) component).setEnabled(edit);
            } else if (component instanceof JButton) {
                ((JButton) component).setEnabled(edit);
            } else if (component instanceof JSpinner) {
                ((JSpinner) component).setEnabled(edit);
                Component[] cs = ((JSpinner) component).getComponents();
                for (Component c : cs) {
                    c.setEnabled(edit);
                }
            } else if (component instanceof JTextField) {
                ((JTextField) component).setEditable(edit);
                ((JTextField) component).setEnabled(edit);
            } else if (component instanceof JSlider) {
                ((JSlider) component).setEnabled(edit);
            }
        }
    }

    private void editButtonActionPerformed(ActionEvent e) {
        if (editButton.isSelected()) {
            JPanel panel = new JPanel();
            JLabel label = new JLabel("Enter password:");
            final JPasswordField pass = new JPasswordField(10);
            panel.add(label);
            panel.add(pass);
            String[] options = new String[]{"OK", "Cancel"};
            int option = JOptionPane.showOptionDialog(editButton, panel, "Authenticate Edit Metadata",
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
                    if (uploadWorker.authenticateUser("admin", sb.toString())) {
                        enableEdit(true);
                    } else {
                        editButton.setSelected(false);
                    }
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(DataUploadForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            enableEdit(false);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        statusLabel = new JLabel();
        panel2 = new JPanel();
        editButton = new JToggleButton();
        scrollPane1 = new JScrollPane();
        dataTable = new TooltipTable();

        //======== this ========
        setLayout(new FormLayout(
            "84dlu:grow, 74dlu:grow, $glue, 96dlu:grow, 81dlu:grow, $glue, 91dlu:grow, 96dlu:grow",
            "19dlu:grow, 18dlu:grow, 20dlu:grow, 21dlu:grow, 3*(20dlu:grow), 24dlu:grow, 23dlu:grow, 25dlu:grow, 21dlu:grow, 22dlu, 23dlu:grow, 21dlu:grow, 26dlu:grow"));

        //---- statusLabel ----
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setFont(new Font("Heiti TC", Font.PLAIN, 13));
        add(statusLabel, CC.xywh(5, 1, 3, 1));

        //======== panel2 ========
        {
            panel2.setLayout(new FormLayout(
                "57dlu:grow, 41dlu:grow",
                "default"));

            //---- editButton ----
            editButton.setText("Edit");
            editButton.addActionListener(e -> editButtonActionPerformed(e));
            panel2.add(editButton, CC.xy(2, 1));
        }
        add(panel2, CC.xy(8, 1));

        //======== scrollPane1 ========
        {

            //---- dataTable ----
            dataTable.setAutoCreateRowSorter(true);
            dataTable.setBorder(LineBorder.createBlackLineBorder());
            dataTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    dataTableMouseClicked(e);
                }
            });
            scrollPane1.setViewportView(dataTable);
        }
        add(scrollPane1, CC.xywh(1, 2, 8, 14));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel statusLabel;
    private JPanel panel2;
    private JToggleButton editButton;
    private JScrollPane scrollPane1;
    private TooltipTable dataTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    class TableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            if (isSelected) {
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else {
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
            }
            return c;
        }
    }
}
