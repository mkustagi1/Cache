package cache.util;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.RootPaneContainer;

/** Basic CursorToolkit that still allows mouseclicks */
public class CursorToolkitOne implements Cursors {
  private CursorToolkitOne() { }

  /** Sets cursor for specified component to Wait cursor
     * @param component */
  public static void startWaitCursor(JComponent component) {
    RootPaneContainer root = 
      (RootPaneContainer)component.getTopLevelAncestor();
    root.getGlassPane().setCursor(WAIT_CURSOR);
    root.getGlassPane().setVisible(true);
  }

  /** Sets cursor for specified component to normal cursor
     * @param component */
  public static void stopWaitCursor(JComponent component) {
    RootPaneContainer root = 
      (RootPaneContainer)component.getTopLevelAncestor();
    root.getGlassPane().setCursor(DEFAULT_CURSOR);
    root.getGlassPane().setVisible(false);
  }

  public static void main(String[] args) {
    final JFrame frame = new JFrame("Test App");
    frame.getContentPane().add(
      new JLabel("I'm a Frame"), BorderLayout.NORTH);
    frame.getContentPane().add(
      new JButton(new AbstractAction("Wait Cursor") {
          @Override
        public void actionPerformed(ActionEvent event) {
          System.out.println("Setting Wait cursor on frame");
          startWaitCursor(frame.getRootPane());
        }
      }));
    frame.setSize(800, 600);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.show();
  }
}
