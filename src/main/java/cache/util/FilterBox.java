package cache.util;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionListener;

/**
 * @author Manjunath Kustagi
 */
public class FilterBox extends JFrame {

    Map<String, Boolean> originalList = null;

    public FilterBox() {
        initComponents();
    }

    public FilterBox(ListSelectionListener listener, long experimentId) {
        initComponents();
        for (ListSelectionListener l : termList.getListSelectionListeners()) {
            termList.removeListSelectionListener(l);
        }
        termList.setSelectionModel(new DefaultListSelectionModel() {
            private static final long serialVersionUID = 1L;

            boolean gestureStarted = false;

            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (!gestureStarted) {
                    if (isSelectedIndex(index0)) {
                        super.removeSelectionInterval(index0, index1);
                    } else {
                        super.addSelectionInterval(index0, index1);
                    }
                }
                gestureStarted = true;
            }

            @Override
            public void setValueIsAdjusting(boolean isAdjusting) {
                if (isAdjusting == false) {
                    gestureStarted = false;
                }
            }

        });
        FastListModel model = new FastListModel();
        termList.setModel(model);
        termList.setCellRenderer(new CheckboxListCellRenderer());
        termList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        termList.addListSelectionListener(listener);
        this.setTitle("Filter Terms");
    }

    public boolean isAllExperimentsSelected() {
        return checkBox1.isSelected();
    }

    public void setTerms(final Map<String, Boolean> terms, final boolean clearSearch, boolean sortOrder) {
        final List<String> list = new ArrayList<>();
        list.addAll(terms.keySet());
        if (!sortOrder) {
            Collections.sort(list, Collections.reverseOrder());
        } else {
            Collections.sort(list);
        }
        SwingWorker<List<String>, String> worker = new SwingWorker<List<String>, String>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                ((FastListModel) termList.getModel()).setListenersEnabled(false);
                ((FastListModel) termList.getModel()).removeAllElements();
                if (clearSearch && !searchField.getText().trim().equals("")) {
                    searchField.setText("");
                }
                if (clearSearch) {
                    originalList = terms;
                }
                termList.clearSelection();
                int c = 0;
                list.stream().filter((term) -> (term != null && !term.trim().equals(""))).forEach((term) -> {
                    publish(term);
                });
                return list;
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.stream().map((term) -> {
                    ((FastListModel) termList.getModel()).addElement(term);
                    return term;
                }).filter((term) -> (terms.get(term))).map((term) -> ((FastListModel) termList.getModel()).indexOf(term)).forEach((index) -> {
                    termList.getSelectionModel().addSelectionInterval(index, index);
                });
            }

            @Override
            protected void done() {
                FastListModel model = (FastListModel) termList.getModel();
                model.setListenersEnabled(true);
                model.fireIntervalAdded(model, 0, terms.size() - 1);
            }
        };
        worker.execute();
    }

    private void downButtonActionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(() -> {
            Map<String, Boolean> values = new HashMap<>();
            for (int i = 0; i < termList.getModel().getSize(); i++) {
                String value = (String) termList.getModel().getElementAt(i);
                Boolean f = termList.getSelectionModel().isSelectedIndex(i);
                values.put(value, f);
            }
            setTerms(values, true, false);
        });
    }

    private void upButtonActionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(() -> {
            Map<String, Boolean> values = new HashMap<>();
            for (int i = 0; i < termList.getModel().getSize(); i++) {
                String value = (String) termList.getModel().getElementAt(i);
                Boolean f = termList.getSelectionModel().isSelectedIndex(i);
                values.put(value, f);
            }
            setTerms(values, true, true);
        });
    }

    private void searchFieldCaretUpdate(final CaretEvent e) {
        Map<String, Boolean> terms = new HashMap<>();
        if (e.getSource() == searchField) {
            String text = searchField.getText();
            if (!text.trim().equals("")) {
                if (text.length() >= 2 && originalList != null) {
                    originalList.keySet().stream().filter((value) -> (value.toUpperCase().contains(text.toUpperCase()))).forEach((value) -> {
                        terms.put(value, originalList.get(value));
                    });
                } else if (text.length() < 2 && originalList != null) {
                    originalList.keySet().stream().forEach((value) -> {
                        terms.put(value, originalList.get(value));
                    });
                }
                setTerms(terms, false, true);
            }
        }
    }

    private void termListMouseClicked(MouseEvent e) {
        // TODO add your code here
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        toolBar1 = new JToolBar();
        upButton = new JButton();
        downButton = new JButton();
        checkBox1 = new JCheckBox();
        searchField = new JFormattedTextField();
        scrollPane1 = new JScrollPane();
        termList = new JList();

        //======== this ========
        setTitle("Filter Terms");
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "256dlu",
            "fill:17dlu, $lgap, fill:19dlu, $lgap, fill:224dlu"));

        //======== toolBar1 ========
        {

            //---- upButton ----
            upButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/up-small.png")));
            upButton.setBackground(Color.black);
            upButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    upButtonActionPerformed(e);
                }
            });
            toolBar1.add(upButton);
            toolBar1.addSeparator();

            //---- downButton ----
            downButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/down-small.png")));
            downButton.setBackground(new Color(0, 153, 0));
            downButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    downButtonActionPerformed(e);
                }
            });
            toolBar1.add(downButton);
            toolBar1.addSeparator();

            //---- checkBox1 ----
            checkBox1.setText("All Experiments");
            toolBar1.add(checkBox1);
        }
        contentPane.add(toolBar1, CC.xy(1, 1));

        //---- searchField ----
        searchField.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                searchFieldCaretUpdate(e);
            }
        });
        contentPane.add(searchField, CC.xy(1, 3, CC.DEFAULT, CC.FILL));

        //======== scrollPane1 ========
        {
            scrollPane1.setViewportView(termList);
        }
        contentPane.add(scrollPane1, CC.xy(1, 5));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JToolBar toolBar1;
    private JButton upButton;
    private JButton downButton;
    private JCheckBox checkBox1;
    private JFormattedTextField searchField;
    private JScrollPane scrollPane1;
    private JList termList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    class CheckboxListCellRenderer extends JCheckBox implements ListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            setComponentOrientation(list.getComponentOrientation());
            setFont(list.getFont());
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setSelected(isSelected);
            setEnabled(list.isEnabled());

            setText(value == null ? "" : value.toString());

            return this;
        }
    }
}
