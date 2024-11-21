package cache.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * @author Manjunath Kustagi
 */
public class ScaledTextPane extends JEditorPane {

    ActionListener zoomInListener;
    ActionListener zoomOutListener;
    final SimpleAttributeSet attrs;
    
    public ScaledTextPane() {
        super();
        setEditorKit(new ScaledEditorKit());
        attrs = new SimpleAttributeSet();
        StyleConstants.setFontSize(attrs, 10);
        recreateListeners();
    }

    public final void recreateListeners() {
        getDocument().putProperty("i18n", Boolean.FALSE);
        getDocument().putProperty("ZOOM_FACTOR", 1.0);
        StyledDocument doc = (StyledDocument) getDocument();
        doc.setCharacterAttributes(0, 1, attrs, true);
        zoomInListener = new ZoomInListener(attrs);
        zoomOutListener = new ZoomOutListener(attrs);
    }

    public ActionListener getZoomInListener() {
        return zoomInListener;
    }

    public ActionListener getZoomOutListener() {
        return zoomOutListener;
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
        super.repaint(0, 0, getWidth(), getHeight());
    }

    private class ZoomInListener implements ActionListener {

        private final SimpleAttributeSet attrs;

        public ZoomInListener(SimpleAttributeSet attrs) {
            this.attrs = attrs;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Double s = (Double) ScaledTextPane.this.getDocument().getProperty("ZOOM_FACTOR");
                double scale = s + 0.1;
                ScaledTextPane.this.getDocument().putProperty("ZOOM_FACTOR", scale);

                StyledDocument doc = (StyledDocument) ScaledTextPane.this.getDocument();
                doc.setCharacterAttributes(0, 1, attrs, true);
                ScaledTextPane.this.repaint();
                doc.insertString(0, "", null);
            } catch (BadLocationException ex) {
                Logger.getLogger(ScaledTextPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ZoomOutListener implements ActionListener {

        private final SimpleAttributeSet attrs;

        public ZoomOutListener(SimpleAttributeSet attrs) {
            this.attrs = attrs;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Double s = (Double) ScaledTextPane.this.getDocument().getProperty("ZOOM_FACTOR");
                double scale = (s > 0.21) ? s - 0.1 : 0.2;
                ScaledTextPane.this.getDocument().putProperty("ZOOM_FACTOR", scale);

                StyledDocument doc = (StyledDocument) ScaledTextPane.this.getDocument();
                doc.setCharacterAttributes(0, 1, attrs, true);
                ScaledTextPane.this.repaint();
                doc.insertString(0, "", null);
            } catch (BadLocationException ex) {
                Logger.getLogger(ScaledTextPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
