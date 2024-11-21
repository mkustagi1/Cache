package cache;

import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import cache.dataimportes.holders.AlignmentResult;
import cache.workers.AlignmentWorker;

/**
 * <p>Title: Alignment table Model</p> <p>Description: The model holds the
 * current displayed alignments in the table. The model gets its alignments from a
 * AlignmentWorker class.</p> <p>Copyright: Copyright (c) 2012</p> <p>Company:
 * </p>
 *
 * @author manjunath@c2b2.columbia.edu
 * @version 1.0
 */
public class AlignmentTableModel extends AbstractTableModel {

    /**
     * Column definition
     */
    public static final String[] headerName = {"Alignment ID", "Exon ID", "Read ID", "Experiment ID", "Score"};
    /**
     * Alignment array - contains the alignments.
     */
    private ArrayList<AlignmentResult> alignments = new ArrayList<AlignmentResult>();
    /**
     * The alignments for this model will come from this source.
     */
    private AlignmentWorker alignmentWorker = null;
    /**
     * Number of rows in the model. Note: the actual number of alignments may be
     * smaller than rowCount. This is done so we do no need to get all
     * alignments if they are not "used", i.e. requested from the model.
     */
    private int rowCount;
    /**
     * Will retrieve alignments from a Patterns Source.
     */
    AlignmentTableModelWorker worker = null;

    /**
     * Add an alignment to the model.
     *
     * @param alignment
     */
    public synchronized void addAlignment(AlignmentResult alignment) {
        this.alignments.add(alignment);
    }

    /**
     * Add a list of alignments to the model
     *
     * @param list ArrayList
     */
    public synchronized void addAllAlignments(ArrayList<AlignmentResult> list) {
        if (list != null) {
            Iterator<AlignmentResult> iter = list.iterator();
            while (iter.hasNext()) {
                AlignmentResult a = iter.next();
                alignments.add(a);
            }
        }
    }

    /**
     * Set the alignment source for this model.
     *
     * @param source
     */
    public synchronized void setAlignmentSource(AlignmentWorker source) {
        alignmentWorker = source;
    }

     /**
     * Get the alignment source for this model.
     *
     */
    public synchronized AlignmentWorker getAlignmentSource() {
        return alignmentWorker;
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
     * @return number of rows in the model
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
    public synchronized Object getValueAt(int row, int col) {
        AlignmentResult alignment = null;
        Object cell = null;
        alignment = getAlignmentNoBlock(row);
        if (alignment != null) {
            switch (col) {
                case 0:
                    cell = new Long(alignment.alignmentID);
                    break;
                case 1:
                    cell = new Long(alignment.exonID);
                    break;
                case 2:
                    cell = alignment.readID;
                    break;
                case 3:
                    cell = new Integer(alignment.experimentID);
                    break;
                case 4:
                    cell = new Double(alignment.score);
                    break;
            }
        } else {
            //the pattern is not in the model, yet.
            cell = (col == 0) ? "loading" : "...";
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
    public synchronized ArrayList<AlignmentResult> getAlignments() {
        ArrayList<AlignmentResult> copy = new ArrayList<AlignmentResult>();
        Iterator<AlignmentResult> iter = alignments.iterator();

        while (iter.hasNext()) {
            copy.add(iter.next());
        }

        return copy;
    }

    /**
     * Get the alignment at the index row. This method will block until the
     * alignment is retrieved from the underlying source.
     *
     * @param row
     * @return
     */
    public synchronized AlignmentResult getAlignment(int row) {
        if ((row < 0) || (row > rowCount - 1)) {
            throw new IndexOutOfBoundsException("[row=" + row + ", rowCount=" + rowCount + "]");
        }

        if ((alignments.size() <= row) || (alignments.get(row) == null)) {
            AlignmentResult p = (AlignmentResult) alignmentWorker.getAlignment(row);
            while (alignments.size() < row) {
                alignments.add(null);
            }
            alignments.add(row, p);
        }
        return (AlignmentResult) alignments.get(row);
    }

    private AlignmentResult getAlignmentNoBlock(int row) {
        AlignmentResult pat = null;
        if (row < alignments.size()) {
            pat = (AlignmentResult) alignments.get(row);
        }
        if (pat == null) {
            if ((worker == null) || worker.isDone()) {
                //if the initial condition where no alignments are available
                //create a subclass of SwingWorker and let it retrieve the alignments.
                worker = new AlignmentTableModelWorker(this, row);
                worker.execute();
            }
        }
        return pat;
    }
}

class AlignmentTableModelWorker extends SwingWorker {

    private boolean done = false;
    AlignmentTableModel model = null;
    int row;

    public AlignmentTableModelWorker(AlignmentTableModel tModel, int row) {
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
        model.getAlignment(row);
        done();
        return "done";
    }
}
