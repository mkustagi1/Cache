package cache;

import cache.interfaces.AdminService.Status;
import com.caucho.hessian.client.HessianProxyFactory;
import java.awt.*;
import javax.swing.*;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * @author Manjunath Kustagi
 */
public class ParclipFrameForm extends JFrame {
    
    public ParclipFrameForm() {
        initComponents();
        this.setTitle("RNA Seq, Genome Sequence Aligner and Search");
        TimerTask timerTask = new StatusTask();
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 60000);
    }
    
    public DataUploadForm getDataUploadForm() {
        return dataUploadForm1;
    }
    
    public void addDragListeners() {
        geneSearchForm1.addDragListeners(this);
        summaryForm1.setFrame(this);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ParclipFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ParclipFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ParclipFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ParclipFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(() -> {
            new ParclipFrameForm().setVisible(true);
        });
    }
    
    private void thisWindowClosed(WindowEvent e) {
        System.exit(0);
    }
    
    private void exitMenuItemActionPerformed(ActionEvent e) {
        System.exit(0);
    }
    
    private void deleteMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    private void pasteMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    private void copyMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    private void cutMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    private void openMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    private void saveMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    private void saveAsMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
    }
    
    public JTabbedPane getTabbedPane1() {
        return tabbedPane1;
    }
    
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        menuBar1 = new JMenuBar();
        menu1 = new JMenu();
        openMenuItem = new JMenuItem();
        saveMenuItem = new JMenuItem();
        saveAsMenuItem = new JMenuItem();
        exitMenuItem = new JMenuItem();
        menu2 = new JMenu();
        cutMenuItem = new JMenuItem();
        copyMenuItem = new JMenuItem();
        pasteMenuItem = new JMenuItem();
        deleteMenuItem = new JMenuItem();
        menu3 = new JMenu();
        hSpacer1 = new JPanel(null);
        status = new JLabel();
        hSpacer2 = new JPanel(null);
        panel1 = new JPanel();
        tabbedPane1 = new JTabbedPane();
        dataUploadForm1 = new DataUploadForm();
        alignmentForm1 = new AlignmentForm();
        sequenceSearchForm1 = new SequenceSearchElasticsearchForm();
        summaryForm1 = new SummaryForm();
        geneSearchForm1 = new GeneSearchForm();
        sequenceSearchWithAlignmentsForm1 = new SequenceSearchWithAlignmentsForm();

        //======== this ========
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
            "700dlu:grow",
            "fill:425dlu:grow"));

        //======== menuBar1 ========
        {
            menuBar1.setMinimumSize(new Dimension(39, 17));

            //======== menu1 ========
            {
                menu1.setText("File");

                //---- openMenuItem ----
                openMenuItem.setText("Open");
                openMenuItem.addActionListener(e -> openMenuItemActionPerformed(e));
                menu1.add(openMenuItem);

                //---- saveMenuItem ----
                saveMenuItem.setText("Save");
                saveMenuItem.addActionListener(e -> saveMenuItemActionPerformed(e));
                menu1.add(saveMenuItem);

                //---- saveAsMenuItem ----
                saveAsMenuItem.setText("Save As..");
                saveAsMenuItem.addActionListener(e -> saveAsMenuItemActionPerformed(e));
                menu1.add(saveAsMenuItem);

                //---- exitMenuItem ----
                exitMenuItem.setText("Exit");
                exitMenuItem.addActionListener(e -> exitMenuItemActionPerformed(e));
                menu1.add(exitMenuItem);
            }
            menuBar1.add(menu1);

            //======== menu2 ========
            {
                menu2.setText("Edit");

                //---- cutMenuItem ----
                cutMenuItem.setText("Cut");
                cutMenuItem.addActionListener(e -> cutMenuItemActionPerformed(e));
                menu2.add(cutMenuItem);

                //---- copyMenuItem ----
                copyMenuItem.setText("Copy");
                copyMenuItem.addActionListener(e -> copyMenuItemActionPerformed(e));
                menu2.add(copyMenuItem);

                //---- pasteMenuItem ----
                pasteMenuItem.setText("Paste");
                pasteMenuItem.addActionListener(e -> pasteMenuItemActionPerformed(e));
                menu2.add(pasteMenuItem);

                //---- deleteMenuItem ----
                deleteMenuItem.setText("Delete");
                deleteMenuItem.addActionListener(e -> deleteMenuItemActionPerformed(e));
                menu2.add(deleteMenuItem);
            }
            menuBar1.add(menu2);

            //======== menu3 ========
            {
                menu3.setText("Help");
            }
            menuBar1.add(menu3);
            menuBar1.add(hSpacer1);

            //---- status ----
            status.setIcon(new ImageIcon(getClass().getResource("/cache/resources/green.png")));
            status.setMaximumSize(new Dimension(22, 22));
            status.setMinimumSize(new Dimension(22, 22));
            status.setPreferredSize(new Dimension(22, 22));
            status.setVerticalTextPosition(SwingConstants.TOP);
            status.setHorizontalTextPosition(SwingConstants.LEADING);
            status.setIconTextGap(0);
            status.setHorizontalAlignment(SwingConstants.CENTER);
            status.setBackground(UIManager.getColor("Button.background"));
            status.setForeground(UIManager.getColor("Button.background"));
            status.setOpaque(true);
            status.setToolTipText("Server Status: GREEN");
            menuBar1.add(status);

            //---- hSpacer2 ----
            hSpacer2.setMaximumSize(new Dimension(10, 22));
            hSpacer2.setMinimumSize(new Dimension(10, 22));
            hSpacer2.setPreferredSize(new Dimension(10, 22));
            menuBar1.add(hSpacer2);
        }
        setJMenuBar(menuBar1);

        //======== panel1 ========
        {
            panel1.setLayout(new FormLayout(
                "600dlu:grow",
                "fill:426dlu:grow"));

            //======== tabbedPane1 ========
            {
                tabbedPane1.addTab("Data Preprocessing and Upload", dataUploadForm1);
                tabbedPane1.addTab("Alignments", alignmentForm1);
                tabbedPane1.addTab("Search Databases", sequenceSearchForm1);
                tabbedPane1.addTab("Experiment Summary", summaryForm1);
                tabbedPane1.addTab("Gene Search", geneSearchForm1);
                tabbedPane1.addTab("Sequence Search", sequenceSearchWithAlignmentsForm1);
            }
            panel1.add(tabbedPane1, CC.xy(1, 1));
        }
        contentPane.add(panel1, CC.xy(1, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JMenuBar menuBar1;
    private JMenu menu1;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem saveAsMenuItem;
    private JMenuItem exitMenuItem;
    private JMenu menu2;
    private JMenuItem cutMenuItem;
    private JMenuItem copyMenuItem;
    private JMenuItem pasteMenuItem;
    private JMenuItem deleteMenuItem;
    private JMenu menu3;
    private JPanel hSpacer1;
    private JLabel status;
    private JPanel hSpacer2;
    private JPanel panel1;
    private JTabbedPane tabbedPane1;
    private DataUploadForm dataUploadForm1;
    private AlignmentForm alignmentForm1;
    private SequenceSearchElasticsearchForm sequenceSearchForm1;
    private SummaryForm summaryForm1;
    private GeneSearchForm geneSearchForm1;
    private SequenceSearchWithAlignmentsForm sequenceSearchWithAlignmentsForm1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    class StatusTask extends TimerTask {
        
        cache.interfaces.AdminService adminService;
        
        public StatusTask() {
            try {
                HessianProxyFactory factory = new HessianProxyFactory();
                String url = System.getProperty("base.url") + "AdminService";
                adminService = (cache.interfaces.AdminService) factory.create(cache.interfaces.AdminService.class, url);
            } catch (MalformedURLException ex) {
                Logger.getLogger(ParclipFrameForm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void run() {
            Status health = adminService.getServerStatus();
            switch (health) {
                case GREEN:
                    status.setIcon(new ImageIcon(getClass().getResource("/cache/resources/green.png")));
                    status.setToolTipText("Server Status: GREEN");
                    break;
                case YELLOW:
                    status.setIcon(new ImageIcon(getClass().getResource("/cache/resources/yellow.png")));
                    status.setToolTipText("Server Status: YELLOW");
                    break;
                case RED:
                    status.setIcon(new ImageIcon(getClass().getResource("/cache/resources/red.png")));
                    status.setToolTipText("Server Status: RED");
                    break;
                default:
                    status.setIcon(new ImageIcon(getClass().getResource("/cache/resources/green.png")));
                    status.setToolTipText("Server Status: GREEN");
                    break;
            }
        }
    }
}
