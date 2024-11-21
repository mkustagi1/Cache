package cache.util;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

/**
 * @author Manjunath Kustagi
 */
public class SearchBox extends JFrame {

    List<String> originalList = null;

    public SearchBox() {
        initComponents();
    }

    public SearchBox(MouseListener listener, long experimentId) {
        initComponents();
        termList.addMouseListener(listener);
        FastListModel model = new FastListModel();
        termList.setModel(model);
        setTitle("Available Terms, Experiment: " + experimentId);
    }

    public void setTerms(final List<String> terms) {
        SwingWorker<List<String>, String> worker = new SwingWorker<List<String>, String>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                ((FastListModel) termList.getModel()).setListenersEnabled(false);
                ((FastListModel) termList.getModel()).removeAllElements();
                if (!searchField.getText().trim().equals("")) {
                    searchField.setText("");
                }
                originalList = terms;
                termList.clearSelection();
                terms.stream().filter((term) -> (term != null && !term.trim().equals(""))).forEach((term) -> {
                    publish(term);
                });
                return terms;
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.stream().forEach((term) -> {
                    ((FastListModel) termList.getModel()).addElement(term);
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
            List<String> values = new ArrayList<>();
            for (int i = 0; i < termList.getModel().getSize(); i++) {
                String value = (String) termList.getModel().getElementAt(i);
                values.add(value);
            }
            Collections.sort(values, Collections.reverseOrder());
            ((DefaultListModel) termList.getModel()).removeAllElements();
            values.stream().forEach((value) -> {
                ((DefaultListModel) termList.getModel()).addElement(value);
            });
            Collections.sort(originalList, Collections.reverseOrder());
        });
    }

    private void upButtonActionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(() -> {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < termList.getModel().getSize(); i++) {
                String value = (String) termList.getModel().getElementAt(i);
                values.add(value);
            }
            Collections.sort(values);
            ((DefaultListModel) termList.getModel()).removeAllElements();
            values.stream().forEach((value) -> {
                ((DefaultListModel) termList.getModel()).addElement(value);
            });
            Collections.sort(originalList);
        });
    }

    private void searchFieldCaretUpdate(final CaretEvent e) {
        SwingUtilities.invokeLater(() -> {
            if (e.getSource() == searchField) {
                String text = searchField.getText();
                if (text.length() >= 2 && originalList != null) {
                    ((DefaultListModel) termList.getModel()).removeAllElements();
                    for (int i = 0; i < originalList.size(); i++) {
                        String value = originalList.get(i);
                        if (value.toUpperCase().contains(text.toUpperCase())) {
                            ((DefaultListModel) termList.getModel()).addElement(value);
                        }
                    }
                } else if (text.length() < 2 && originalList != null) {
                    ((DefaultListModel) termList.getModel()).removeAllElements();
                    for (int i = 0; i < originalList.size(); i++) {
                        String value = originalList.get(i);
                        ((DefaultListModel) termList.getModel()).addElement(value);
                    }
                }
            }
        });
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
        searchField = new JFormattedTextField();
        scrollPane1 = new JScrollPane();
        termList = new JList();

        //======== this ========
        setTitle("Available Terms");
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "256dlu",
            "fill:17dlu, $lgap, fill:19dlu, $lgap, fill:224dlu"));

        //======== toolBar1 ========
        {
            toolBar1.setBackground(new Color(0, 153, 0));

            //---- upButton ----
            upButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/up-small.png")));
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
            downButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    downButtonActionPerformed(e);
                }
            });
            toolBar1.add(downButton);
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

            //---- termList ----
            termList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            termList.setVisibleRowCount(24);
            termList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    termListMouseClicked(e);
                }
            });
            scrollPane1.setViewportView(termList);
        }
        contentPane.add(scrollPane1, CC.xy(1, 5, CC.FILL, CC.FILL));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JToolBar toolBar1;
    private JButton upButton;
    private JButton downButton;
    private JFormattedTextField searchField;
    private JScrollPane scrollPane1;
    private JList termList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
