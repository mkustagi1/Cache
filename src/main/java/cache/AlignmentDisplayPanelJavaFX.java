package cache;

import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ActionMapUIResource;
import org.apache.commons.io.FilenameUtils;
import cache.dataimportes.holders.TranscriptMappingResults;
import cache.util.CursorToolkitOne;
import cache.util.EditorScene;
import cache.util.FXUtilities;
import cache.util.JavaFXBrowserChart;
import cache.util.JavaFXBrowserJXBrowser;
import cache.util.JavaFXMultimapperChart;
import cache.util.TextTransfer;
import cache.workers.AlignmentWorker;

/**
 * @author Manjunath Kustagi
 */
public class AlignmentDisplayPanelJavaFX extends JPanel {

    protected TextTransfer tt = new TextTransfer();
    protected ActionMap actionMap = new ActionMapUIResource();
    private TranscriptMappingResults tmr;
    private String authToken;
    private Long experimentId;
    private int distance;

    public AlignmentDisplayPanelJavaFX() {
        initComponents();
        javaFXBrowserChart1.setBrowser(javaFXBrowser1);
        javaFXMultimapperChart1.setBrowser(javaFXBrowser1);
        javaFXBrowser1.setChart(javaFXBrowserChart1);
        javaFXBrowser1.setMultimapperChart(javaFXMultimapperChart1);
    }

    public void setExperimentId(Long eid) {
        experimentId = eid;
    }

    public void setDistance(int d) {
        distance = d;
    }

    @Override
    public void setName(String name) {
        javaFXBrowserChart1.setName(name);
    }

    String editedName = "";
    String email = "";

    public void setEditedName(String name) {
        editedName = name;
    }

    public String getEditedName() {
        String toBeReturned = editedName;
        toBeReturned = (toBeReturned == null) ? "_" : toBeReturned;
        toBeReturned = toBeReturned.replaceAll("\'", "");
        toBeReturned = toBeReturned.replaceAll("-", "_");
        toBeReturned = toBeReturned.replaceAll(" ", "_");
        if (toBeReturned.length() >= 74) {
            toBeReturned = toBeReturned.substring(0, 74);
        } else if (toBeReturned.length() == 0) {
            toBeReturned= "_";
        }
        return toBeReturned;
    }

    public void setEmail(String e) {
        email = e;
    }

    public String getEmail() {
        return email;
    }

    public void setText(final TranscriptMappingResults result) {
        tmr = result;
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        InputMap keyMap = new ComponentInputMap(this);
        keyMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                mask), "zoom_in");
        keyMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                mask), "zoom_out");
        SwingUtilities.replaceUIInputMap(this, JComponent.WHEN_IN_FOCUSED_WINDOW,
                keyMap);
        Runnable runnable = () -> {
            System.out.println("adpjfx, runLater 1: " + System.currentTimeMillis());
            javaFXBrowser1.setTranscriptId(result.transcriptID);
            javaFXBrowser1.loadContent(result.mappedAlignments, result.name);
            javaFXBrowserChart1.setCoverages(result.coverages);
            javaFXMultimapperChart1.computeMultimappers(result.multiMappers, result.rcMultiMappers);
            javaFXBrowserChart1.logTransformPlots();
            System.out.println("adpjfx, runLater 2: " + System.currentTimeMillis());
        };
        try {
            FXUtilities.runAndWait(runnable);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(AlignmentDisplayPanelJavaFX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveButtonActionPerformed(ActionEvent e) {
        FileFilter fileFilter = new FileNameExtensionFilter("HTML files", "html");
        JFileChooser fc = new JFileChooser();
        fc.setName("Save Alignment");
        fc.setFileFilter(fileFilter);
        int option = fc.showSaveDialog(this);
        if (JFileChooser.APPROVE_OPTION == option) {
            try {
                File file = fc.getSelectedFile();
                String ext = FilenameUtils.getExtension(file.getAbsolutePath());
                if (!ext.equalsIgnoreCase("") || !ext.equalsIgnoreCase("html")) {
                    file = new File(file.getAbsoluteFile() + ".html");
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(javaFXBrowser1.getContent());
                bw.flush();
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(AlignmentDisplayPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void copyButtonActionPerformed(ActionEvent e) {
        javaFXBrowser1.getRCBrowser().loadSelection();
        String selection = "EMPTY";
        while (selection.equalsIgnoreCase("EMPTY")) {
            selection = javaFXBrowser1.getBrowser().getSelection();
        }
        while (selection.equalsIgnoreCase("EMPTY")) {
            selection = javaFXBrowser1.getRCBrowser().getSelection();
        }
        tt.setClipboardContents(selection);
    }

    private void editButtonActionPerformed(ActionEvent e) {
        if (tmr != null) {
            JPanel panel = new JPanel();
            JLabel label = new JLabel("Enter password:");
            final JPasswordField pass = new JPasswordField(10);
            panel.add(label);
            panel.add(pass);
            String[] options = new String[]{"OK", "Cancel"};
            int option = JOptionPane.showOptionDialog(this, panel, "Authenticate Edit Transcript",
                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (option == 0) {
                CursorToolkitOne.startWaitCursor(javaFXBrowser1);
                SwingUtilities.invokeLater(() -> {
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
                        AlignmentWorker worker = new AlignmentWorker();
                        if (worker.authenticateUser("admin", authToken)) {
                            final JFXPanel jfxPanel = new JFXPanel();
                            Platform.runLater(() -> {
                                Scene scene = createEditor();
                                jfxPanel.setScene(scene);
                            });
                            JFrame frame = new JFrame();
                            frame.getContentPane().setLayout(new BorderLayout());
                            frame.getContentPane().add(jfxPanel, BorderLayout.CENTER);
                            frame.setPreferredSize(new Dimension(1000, 700));
                            frame.pack();
                            frame.setVisible(true);
                        }
                        CursorToolkitOne.stopWaitCursor(javaFXBrowser1);
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        }
    }

    private Scene createEditor() {
        return new EditorScene(new Group(), tmr, experimentId.intValue(), authToken);
    }

    private void zoomInButtonActionPerformed(ActionEvent e) {
        javaFXBrowser1.getBrowser().zoomIn();
        javaFXBrowser1.getRCBrowser().zoomIn();
    }

    private void zoomOutButtonActionPerformed(ActionEvent e) {
        javaFXBrowser1.getBrowser().zoomOut();
        javaFXBrowser1.getRCBrowser().zoomOut();
    }

    private void searchActionPerformed(ActionEvent e) {
        String text1 = searchTextField.getText().trim();
        text1 = text1.replaceAll("\\s", "");
        text1 = text1.replaceAll("(\\r|\\n)", "");
        text1 = text1.replaceAll("\\r\\n", "");
        text1 = text1.replaceAll("-", "");
        text1 = text1.replaceAll("_", "");
        text1 = text1.replaceAll("\\d+", "");
        text1 = text1.toUpperCase();
        text1 = text1.replaceAll("U", "T");
        final String searchText = text1;
        searchTextField.setText(searchText);
        CursorToolkitOne.startWaitCursor(this);
        javaFXBrowser1.getBrowser().clearSearchHighlights(this);
        CursorToolkitOne.startWaitCursor(this);
        javaFXBrowser1.getBrowser().highlightText(searchText, "#e0e006", this);
        javaFXBrowser1.getBrowser().refreshAnnotationsWithoutClear();
        CursorToolkitOne.startWaitCursor(this);
        javaFXBrowser1.getRCBrowser().clearSearchHighlights(this);
        CursorToolkitOne.startWaitCursor(this);
        javaFXBrowser1.getRCBrowser().highlightText(searchText, "#e0e006", this);
        javaFXBrowser1.getRCBrowser().refreshAnnotationsWithoutClear();
    }

    private void initComponents() {
        toolBar1 = new JToolBar();
        saveButton = new JButton();
        separator5 = new JSeparator();
        copyButton = new JButton();
        separator4 = new JSeparator();
        editButton = new JButton();
        separator6 = new JSeparator();
        zoomInButton = new JButton();
        separator1 = new JSeparator();
        zoomOutButton = new JButton();
        separator3 = new JSeparator();
        searchTextField = new JTextField();
        button1 = new JButton();
        javaFXBrowserChart1 = new JavaFXBrowserChart();
        javaFXMultimapperChart1 = new JavaFXMultimapperChart();
        javaFXBrowser1 = new JavaFXBrowserJXBrowser();

        //======== this ========
        setLayout(new FormLayout(
                "93dlu, 90dlu, 40dlu, 218dlu:grow, 17dlu, 18dlu, 17dlu",
                "22dlu, $lgap, 84dlu, $lgap, 84dlu, $lgap, 106dlu:grow, 59dlu:grow"));

        //======== toolBar1 ========
        {

            //---- saveButton ----
            saveButton.setMaximumSize(new Dimension(25, 25));
            saveButton.setMinimumSize(new Dimension(25, 25));
            saveButton.setPreferredSize(new Dimension(25, 25));
            saveButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/document-save22.png")));
            saveButton.setToolTipText("Save");
            saveButton.addActionListener((ActionEvent e) -> {
                saveButtonActionPerformed(e);
            });
            toolBar1.add(saveButton);
            toolBar1.add(separator5);

            //---- copyButton ----
            copyButton.setMaximumSize(new Dimension(25, 25));
            copyButton.setMinimumSize(new Dimension(25, 25));
            copyButton.setPreferredSize(new Dimension(25, 25));
            copyButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/edit-copy22.png")));
            copyButton.setToolTipText("Copy");
            copyButton.addActionListener((ActionEvent e) -> {
                copyButtonActionPerformed(e);
            });
            toolBar1.add(copyButton);
            toolBar1.add(separator4);

            //---- editButton ----
            editButton.setMaximumSize(new Dimension(25, 25));
            editButton.setMinimumSize(new Dimension(25, 25));
            editButton.setPreferredSize(new Dimension(25, 25));
            editButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/edit-pie.png")));
            editButton.setToolTipText("Edit Transcript");
            editButton.addActionListener((ActionEvent e) -> {
                editButtonActionPerformed(e);
            });
            toolBar1.add(editButton);
            toolBar1.add(separator6);

            //---- zoomInButton ----
            zoomInButton.setMaximumSize(new Dimension(25, 25));
            zoomInButton.setMinimumSize(new Dimension(25, 25));
            zoomInButton.setPreferredSize(new Dimension(25, 25));
            zoomInButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/View-zoom-in.png")));
            zoomInButton.setToolTipText("Zoom In");
            zoomInButton.addActionListener((ActionEvent e) -> {
                zoomInButtonActionPerformed(e);
            });
            toolBar1.add(zoomInButton);
            toolBar1.add(separator1);

            //---- zoomOutButton ----
            zoomOutButton.setMaximumSize(new Dimension(25, 25));
            zoomOutButton.setMinimumSize(new Dimension(25, 25));
            zoomOutButton.setPreferredSize(new Dimension(25, 25));
            zoomOutButton.setIcon(new ImageIcon(getClass().getResource("/cache/resources/View-zoom-out.png")));
            zoomOutButton.setToolTipText("Zoom Out");
            zoomOutButton.addActionListener((ActionEvent e) -> {
                zoomOutButtonActionPerformed(e);
            });
            toolBar1.add(zoomOutButton);
            toolBar1.add(separator3);
        }
        add(toolBar1, CC.xy(1, 1));

        //---- searchTextField ----
        searchTextField.setToolTipText("Enter Sequence to search for in the alignment");
        searchTextField.addActionListener((ActionEvent e) -> {
            searchActionPerformed(e);
        });
        add(searchTextField, CC.xy(2, 1));

        //---- button1 ----
        button1.setText("Search");
        button1.addActionListener((ActionEvent e) -> {
            searchActionPerformed(e);
        });
        add(button1, CC.xy(3, 1));

        javaFXBrowserChart1.setBorder(LineBorder.createBlackLineBorder());
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 1));
        panel.add(javaFXBrowserChart1);
        panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        javaFXMultimapperChart1.setBorder(LineBorder.createBlackLineBorder());
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, javaFXMultimapperChart1, javaFXBrowser1);
        splitPane.setOneTouchExpandable(true);
        final JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, splitPane);
        splitPane1.setOneTouchExpandable(true);
        add(splitPane1, CC.xywh(1, 2, 7, 7));
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JToolBar toolBar1;
    private JButton saveButton;
    private JSeparator separator5;
    private JButton copyButton;
    private JSeparator separator4;
    private JButton editButton;
    private JSeparator separator6;
    private JButton zoomInButton;
    private JSeparator separator1;
    private JButton zoomOutButton;
    private JSeparator separator3;
    private JTextField searchTextField;
    private JButton button1;
    private JavaFXBrowserChart javaFXBrowserChart1;
    private JavaFXMultimapperChart javaFXMultimapperChart1;
    private JavaFXBrowserJXBrowser javaFXBrowser1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    private class Dialog extends javafx.stage.Popup {

        private final FlowPane root;

        private Dialog() {
            root = new FlowPane();
            root.setPrefWidth(750);
            root.setPrefHeight(500);
            root.setStyle("-fx-border-width: 1; -fx-border-color: gray");
            root.getChildren().add(buildTitleBar());
            getContent().add(root);
        }

        public void setContent(Node content) {
            if (!root.getChildren().contains(content)) {
                root.getChildren().add(content);
            }
        }

        private Node buildTitleBar() {
            BorderPane pane = new BorderPane();
            pane.setStyle("-fx-background-color: #0000aa; -fx-text-fill: white; -fx-padding: 5");

            pane.setOnMouseDragged((javafx.scene.input.MouseEvent event) -> {
                // not sure why getX and getY don't work
                // double x = getX() + event.getX();
                // double y = getY() + event.getY();
                double x1 = event.getScreenX();
                double y1 = event.getScreenY();
                setX(x1);
                setY(y1);
            });

            javafx.scene.control.Label title = new javafx.scene.control.Label("Coverage Plots");
            pane.setLeft(title);

            javafx.scene.control.Button closeButton = new javafx.scene.control.Button("X");
            closeButton.setOnAction((javafx.event.ActionEvent actionEvent) -> {
                hide();
            });
            pane.setRight(closeButton);

            return pane;
        }
    }
}
