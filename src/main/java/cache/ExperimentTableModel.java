package cache;

import java.awt.Color;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import cache.dataimportes.holders.ExperimentResult;
import cache.util.LazyViewPort;
import cache.workers.ExperimentTableWorker;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableModel;

/**
 * <p>
 * Title: Experiment table Model</p>
 * <p>
 * Description: The model holds the current displayed experiments in the
 * table.</p>
 * <p>
 * Copyright: Copyright (c) 2012</p>
 * <p>
 * Company: </p>
 *
 * @author manjunath@c2b2.columbia.edu
 * @version 1.0
 */
public class ExperimentTableModel extends AbstractTableModel {

    /**
     * Column definition
     */
    public static final String[] headerName = {"Experiment ID", "Name", "Description", "Method", "Mapping Parameters", "Annotation", "Investigator"};

    private static class ColumnContext {

        public final String columnName;
        public final Class columnClass;
        public final boolean isEditable;

        public ColumnContext(String columnName, Class columnClass, boolean isEditable) {
            this.columnName = columnName;
            this.columnClass = columnClass;
            this.isEditable = isEditable;
        }
    }
    private static final ColumnContext[] columnArray = {
        new ColumnContext("Experiment ID", Long.class, false),
        new ColumnContext("Name", String.class, false),
        new ColumnContext("Description", String.class, false),
        new ColumnContext("Method", String.class, false),
        new ColumnContext("Mapping Parameters", String.class, false),
        new ColumnContext("Annotation", String.class, false),
        new ColumnContext("Experimenter", String.class, false)
    };
    /**
     * Alignment array - contains the alignments.
     */
    private final Map<Integer, Map<Integer, ExperimentResult>> alignments = Collections.synchronizedMap(new HashMap<>());

    /**
     * Number of rows in the model. Note: the actual number of alignments may be
     * smaller than rowCount. This is done so we do no need to get all
     * alignments if they are not "used", i.e. requested from the model.
     */
    private int rowCount;

    private ExperimentTableWorker worker = new ExperimentTableWorker();

    protected int pageSize = 0;
    protected int pageOffset = 0;
    protected long totalrecords;
    private int dataOffset = 0;
    JLabel statusLabel;

    public ExperimentTableModel() {
    }

    public ExperimentTableModel(JLabel label) {
        statusLabel = label;
        totalrecords = worker.getExperimentCount();
        int pgSize = worker.getPageSize(); 
        if (totalrecords < pgSize)
            pgSize = (int)totalrecords;
        setPageSize(pgSize);
        statusLabel.setText(Integer.toString(1 + (pageOffset * pageSize))
                + ", " + Integer.toString(((pageOffset + 1) * pageSize))
                + " of " + totalrecords + " experiments");
    }

    /**
     * Add an alignment to the model.
     *
     * @param alignment
     */
    public synchronized void addExperiment(ExperimentResult alignment) {
        int size = alignments.size();
        this.alignments.get(pageOffset).put(size, alignment);
    }

    /**
     * Add a list of alignments to the model
     *
     * @param list ArrayList
     */
    public synchronized void addAllExperiments(ArrayList<ExperimentResult> list) {
        int i = 0;
        if (list != null) {
            Iterator<ExperimentResult> iter = list.iterator();
            while (iter.hasNext()) {
                ExperimentResult a = iter.next();
                alignments.get(pageOffset).put(i, a);
                i++;
            }
        }
    }

    /**
     * Set the alignment source for this model.
     *
     * @param source
     */
    public synchronized void setDataUploadSource(ExperimentTableWorker source) {
        worker = source;
        setRowCount(worker.getPageSize());
    }

    /**
     * See javax.swing.table.TableModel
     *
     * @return number of columns in the model
     */
    @Override
    public int getColumnCount() {
        return headerName.length;
    }

    /**
     * See javax.swing.table.TableModel
     *
     * @return number of columns in the model
     */
    @Override
    public synchronized int getRowCount() {
        return rowCount;
    }

    /**
     * Removes all existing alignments from the model
     */
    public synchronized void clear() {
        alignments.clear();
    }

    /**
     * See javax.swing.table.TableModel
     *
     * @param columnIndex the index of the column
     * @return the name of the column
     */
    @Override
    public String getColumnName(int columnIndex) {
        return headerName[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int modelIndex) {
        return columnArray[modelIndex].columnClass;
    }

    @Override
    public synchronized Object getValueAt(int row, int col) {
        ExperimentResult alignment = null;
        Object cell = null;
        alignment = getAlignmentNoBlock(row);
        if (alignment != null) {
            switch (col) {
                case 0:
                    cell = alignment.experimentID;
                    break;
                case 1:
                    cell = alignment.library;
                    break;
                case 2:
                    cell = alignment.protein;
                    break;
                case 3:
                    cell = alignment.method;
                    break;
                case 4:
                    cell = alignment.mappingParameters;
                    break;
                case 5:
                    cell = alignment.annotation;
                    break;
                case 6:
                    cell = alignment.investigator;
                    break;
            }
        } else {
            //the pattern is not in the model, yet.
            //cell = (col == 0) ? new Long(0) : "loading...";
            cell = "";
        }
        return cell;
    }

    /**
     * Set the number of rows for the model.
     *
     * @param rowNum number of rows.
     */
    public synchronized void setRowCount(int rowNum) {
        rowCount = rowNum;
    }

    /**
     * Return a copy of all stored patterns
     *
     * @return pattern
     */
    public synchronized ArrayList<ExperimentResult> getExperiments() {
        ArrayList<ExperimentResult> copy = new ArrayList<>();
        Iterator<ExperimentResult> iter = alignments.get(pageOffset).values().iterator();

        while (iter.hasNext()) {
            copy.add(iter.next());
        }

        return copy;
    }

    /**
     * Get the pattern at the index row. This method will block until the
     * pattern is retrieved from the underline source.
     *
     * @param row
     * @return
     */
    public synchronized ExperimentResult getExperiment(int row) {
        //get the pattern from a Source if needed.
        if ((row < 0) || (row > rowCount - 1)) {
            throw new IndexOutOfBoundsException("[row=" + row + ", rowCount=" + rowCount + "]");
        }

        if (alignments.get(row) == null) {
            ExperimentResult p = (ExperimentResult) worker.getExperiment(row, pageOffset);
            alignments.get(pageOffset).put(row, p);
        }
        ExperimentResult result = (ExperimentResult) alignments.get(pageOffset).get(row);
        return result;
    }

    private ExperimentResult getAlignmentNoBlock(int row) {
        ExperimentResult pat = null;
        if (alignments.get(pageOffset) == null) {
            alignments.put(pageOffset, new HashMap<>());
        }
        if (row < alignments.get(pageOffset).size()) {
            pat = (ExperimentResult) alignments.get(pageOffset).get(row);
        }

        if (pat == null) {
            pat = worker.getExperiment(row, pageOffset);
            alignments.get(pageOffset).put(row, pat);
        }
        return pat;
    }
    Map<Integer, Map<Integer, Color>> rowColors = new HashMap<>();

    public void setRowColour(int row, Color c) {
        if (rowColors.get(pageOffset) == null) {
            rowColors.put(pageOffset, new HashMap<>());
        }
        rowColors.get(pageOffset).put(row, c);
        fireTableRowsUpdated(row, row);
    }

    public Color getRowColour(int row) {
        Color c = null;
        if (rowColors.get(pageOffset) == null) {
            rowColors.put(pageOffset, new HashMap<>());
        }
        c = rowColors.get(pageOffset).get(row);

        if (c == null) {
            c = Color.white;
        }

        return c;
    }

    public void clearColorSelections() {
        rowColors.clear();
    }

    public int getPageOffset() {
        return pageOffset;
    }

    public int getPageCount() {
        return (int) Math.ceil(((double) totalrecords) / pageSize);
    }

    public int getPageSize() {
        return pageSize;
    }

    final public void setPageSize(int s) {
        if (s == pageSize) {
            return;
        }
        int oldPageSize = pageSize;
        pageSize = s;
        pageOffset = (oldPageSize * pageOffset) / pageSize;
        fireTableDataChanged();
    }

    // Update the page offset and fire a data changed (all rows).
    public void pageDown() {
        if (pageOffset < getPageCount() - 1) {
            pageOffset++;
            if (((pageOffset + 1) * pageSize) <= totalrecords) {
                statusLabel.setText(Integer.toString(1 + (pageOffset * pageSize))
                        + ", " + Integer.toString(((pageOffset + 1) * pageSize))
                        + " of " + totalrecords + " experiments");
            } else {
                statusLabel.setText(Integer.toString(1 + (pageOffset * pageSize))
                        + ", " + totalrecords
                        + " of " + totalrecords + " experiments");
                rowCount = ((int) totalrecords - (pageOffset * pageSize));
            }
            fireTableDataChanged();
        }
    }

    // Update the page offset and fire a data changed (all rows).
    public void pageUp() {
        rowCount = pageSize;
        if (pageOffset > 0) {
            pageOffset--;
            statusLabel.setText(Integer.toString(1 + (pageOffset * pageSize))
                    + ", " + Integer.toString(((pageOffset + 1) * pageSize))
                    + " of " + totalrecords + " experiments");
            fireTableDataChanged();
        }
    }

    // We provide our own version of a scrollpane that includes
    // the page up and page down buttons by default.
    public static JScrollPane createPagingScrollPaneForTable(JTable jt) {
        JScrollPane jsp = LazyViewPort.createLazyScrollPaneFor(jt);
        TableModel tmodel = jt.getModel();

        // Don't choke if this is called on a regular table . . .
        if (!(tmodel instanceof ExperimentTableModel)) {
            return jsp;
        }

        // Okay, go ahead and build the real scrollpane
        final ExperimentTableModel model = (ExperimentTableModel) tmodel;
        final JButton upButton = new JButton(new ArrowIcon(ArrowIcon.UP));
        upButton.setEnabled(false); // starts off at 0, so can't go up
        final JButton downButton = new JButton(new ArrowIcon(ArrowIcon.DOWN));
        if (model.getPageCount() <= 1) {
            downButton.setEnabled(false); // One page...can't scroll down
        }

        upButton.addActionListener((ActionEvent ae) -> {
            model.pageUp();

            // If we hit the top of the data, disable the up button.
            if (model.getPageOffset() == 0) {
                upButton.setEnabled(false);
            }
            downButton.setEnabled(true);
        });

        downButton.addActionListener((ActionEvent ae) -> {
            model.pageDown();

            // If we hit the bottom of the data, disable the down button.
            if (model.getPageOffset() == (model.getPageCount() - 1)) {
                downButton.setEnabled(false);
            }
            upButton.setEnabled(true);
        });

        // Turn on the scrollbars; otherwise we won't get our corners.
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        // Add in the corners (page up/down).
        jsp.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, upButton);
        jsp.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, downButton);

        return jsp;
    }

}

class ExperimentTableModelWorker extends SwingWorker {

    private boolean done = false;
    ExperimentTableModel model = null;
    int row;

    public ExperimentTableModelWorker(ExperimentTableModel tModel, int row) {
        super();
        model = tModel;
        this.row = row;
    }

    @Override
    public void done() {
        done = true;
    }

    /**
     * Main work of the SwingWorker is in this method SwingWorker retrieves a
     * new Alignment from the server.
     */
    @Override
    protected Object doInBackground() throws Exception {
        model.getExperiment(row);
        done();
        return "done";
    }
}
