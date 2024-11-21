package cache.util;

import java.awt.event.MouseEvent;
import javax.swing.JTable;

/**
 *
 * @author Manjunath Kustagi
 */
public class TooltipTable extends JTable {

    @Override
    public String getToolTipText(MouseEvent e) {
        String tip = null;
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        try {
            tip = getValueAt(rowIndex, colIndex).toString();
        } catch (RuntimeException e1) {
        }

        return tip;
    }
}
