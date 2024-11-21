package cache;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import cache.dataimportes.holders.SummaryResults;
import cache.util.LazyViewPort;
import cache.workers.SummaryWorker;
import java.io.IOException;
import java.io.StringReader;
import com.opencsv.CSVReader;

/**
 *
 * @author Manjunath Kustagi
 */
public class SummaryTableModel extends AbstractTableModel {

    private static final int MAX_PAGE_SIZE = 30000;
    private static final int LATENCY_MILLIS = 0;
    protected SummaryWorker worker;
    protected int pageSize = 0;
    protected int pageOffset;
    protected ArrayList<STMRecord> data;
    protected long totalrecords;
    private int dataOffset = 0;
    private SortedSet<Segment> pending = new TreeSet<>();
    JLabel statusLabel;
    boolean summary = true;
    List<SummaryResults> comparisons = null;
    String authToken = null;
    boolean editable = false;

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
    private static final ColumnContext[] columnArraySummary = {
        new ColumnContext("Gene ID", String.class, false),
        new ColumnContext("Gene Symbol", String.class, false),
        new ColumnContext("Start Codon", Integer.class, false),
        new ColumnContext("Stop Codon", Integer.class, false),
        new ColumnContext("Start Site", Integer.class, false),
        new ColumnContext("PolyA Signal", Integer.class, false),
        new ColumnContext("Cleavage Site", Integer.class, false),
        new ColumnContext("Substitution", Integer.class, false),
        new ColumnContext("siRNA", Integer.class, false),
        new ColumnContext("Alternative Cassette Exon", Integer.class, false),
        new ColumnContext("Alternative 5’ Splice Site Exon", Integer.class, false),
        new ColumnContext("Alternative 3’ Splice Site Exon", Integer.class, false),
        new ColumnContext("1st Shared Exon", Integer.class, false),
        new ColumnContext("Last Shared Exon", Integer.class, false),
        new ColumnContext("Mutually Exclusive Exon", Integer.class, false),
        new ColumnContext("Retained Intron 3’ UTR", Integer.class, false),
        new ColumnContext("Retained Intron", Integer.class, false),
        new ColumnContext("Insertion", Integer.class, false),
        new ColumnContext("Deletion", Integer.class, false),
        new ColumnContext("Transposition", Integer.class, false),
        new ColumnContext("Freeform", Integer.class, false),
        new ColumnContext("Other", Integer.class, false),
        new ColumnContext("EPD", Integer.class, false),
        new ColumnContext("EPD antisense", Integer.class, false),
        new ColumnContext("Start Site diff", Integer.class, false),
        new ColumnContext("Start Codon diff", Integer.class, false),
        new ColumnContext("Synonyms", String.class, true),
        new ColumnContext("Level I classification", String.class, true),
        new ColumnContext("Level II classification", String.class, true),
        new ColumnContext("Level III classification", String.class, true),
        new ColumnContext("Level IV classification", String.class, true),
        new ColumnContext("Hierarchy +1", String.class, true),
        new ColumnContext("Hierarchy -1", String.class, true),
        new ColumnContext("Hierarchy 0 (isoform)", String.class, true),
        new ColumnContext("Hierarchy 0 (mutation / allelic variation)", String.class, true),
        new ColumnContext("Hierarchy 0 (other)", String.class, true),
        new ColumnContext("Function level I", String.class, true),
        new ColumnContext("Gene structure level I", String.class, true),
        new ColumnContext("Disease", String.class, true),
        new ColumnContext("Role in cancers", String.class, true),
        new ColumnContext("Non-redundant Read Count Reference Transcript", Long.class, false),
        new ColumnContext("Total Read Count Reference Transcript", Long.class, false),
        new ColumnContext("Transcripts mapped by Unique Reads", Long.class, false),
        new ColumnContext("Reference Transcript length", Long.class, false),
        new ColumnContext("Number of Isoforms", Integer.class, false),
        new ColumnContext("NR reads multimapping", Long.class, false),
        new ColumnContext("Total Read Count / Reference Transcript Length", Double.class, false),
        new ColumnContext("RPKM", Double.class, false),
        new ColumnContext("RPKM rank", Integer.class, false)
    };
    private static final ColumnContext[] columnArrayComparison = {
        new ColumnContext("Gene ID", String.class, false),
        new ColumnContext("Gene Symbol", String.class, false),
        new ColumnContext("Level II classification", String.class, false),
        new ColumnContext("Level I function", String.class, false),
        new ColumnContext("RPKM 1", Double.class, false),
        new ColumnContext("RPKM 2", Double.class, false),
        new ColumnContext("RPKM rank 1", Integer.class, false),
        new ColumnContext("RPKM rank 2", Integer.class, false),
        new ColumnContext("Ratio of RPKM", Double.class, false)
    };

    @Override
    public boolean isCellEditable(int row, int col) {
        if (!editable) {
            return false;
        } else {
            if (!summary) {
                return columnArrayComparison[col].isEditable;
            } else if (authToken != null && columnArraySummary[col].isEditable) {
                if (getWorker().authenticateUser("admin", authToken)) {
                    return columnArraySummary[col].isEditable;
                } else {
                    return false;
                }
            }
            return columnArraySummary[col].isEditable;
        }
    }

    @Override
    public Class<?> getColumnClass(int modelIndex) {
        if (!summary) {
            return columnArrayComparison[modelIndex].columnClass;
        }
        return columnArraySummary[modelIndex].columnClass;
    }

    @Override
    public int getColumnCount() {
        if (!summary) {
            return columnArrayComparison.length;
        }
        return columnArraySummary.length;
    }

    @Override
    public String getColumnName(int modelIndex) {
        if (!summary) {
            return columnArrayComparison[modelIndex].columnName;
        }
        return columnArraySummary[modelIndex].columnName;
    }

    public SummaryTableModel(JLabel label) {
        this(0, 100, label, true);
    }

    public SummaryTableModel(long numRows, int size, JLabel label, boolean smry) {
        statusLabel = label;
        totalrecords = numRows;
        summary = smry;
        data = new ArrayList<>();
        setPageSize(size);
    }

    public SummaryTableModel(long numRows, int size, JLabel label, boolean smry, List<SummaryResults> comparisons) {
        statusLabel = label;
        totalrecords = numRows;
        summary = smry;
        data = new ArrayList<>();
        this.comparisons = comparisons;
        setPageSize(size);
    }

    public ArrayList<STMRecord> getData() {
        return data;
    }

    // Return values appropriate for the visible table part.
    @Override
    public int getRowCount() {
        return pageSize;
    }

    public String getToolTipText(int row) {
        int realRow = row + (pageOffset * pageSize);

        // check if row is in current page, schedule if not  
        ArrayList<STMRecord> page = data;
        int pageIndex = realRow - dataOffset;
        if (pageIndex < 0 || pageIndex >= page.size()) {
            return "loading...";
        }
        STMRecord rowObject = page.get(pageIndex);
        return rowObject.getToolTipText();
    }

    public void removeRows(List<Integer> indices) {
        Collections.sort(indices);
        for (int i = indices.size() - 1; i >= 0; i--) {
            this.data.remove(indices.get(i).intValue());
            fireTableRowsDeleted(indices.get(i), indices.get(i));
            totalrecords--;
        }
    }

    public Map<String, Boolean> getUniqueValuesInColumn(int col) {
        Map<String, Boolean> values = new HashMap<>();
        for (int i = 0; i < totalrecords; i++) {
            Object[] rowValues = getValueAndFilteredStatusAt(i, col);
            Object value = rowValues[0];
            Boolean f = (Boolean) rowValues[1];
            if (value instanceof String) {
                try {
                    CSVReader reader = new CSVReader(new StringReader((String) value));
                    String[] tokens = reader.readNext();
                    for (String tok : tokens) {
                        if (tok != null && !tok.trim().equals("") && !values.containsKey(tok.trim())) {
                            values.put(tok.trim(), f);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SummaryTableModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        while (values.containsKey("loading...")) {
            values.remove("loading...");
        }
        return values;
    }

    public List<Integer> getRowsMatchingTerms(List<String> terms, int col) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < totalrecords; i++) {
            Object value = getValueAt(i, col);
            if (value instanceof String) {
                try {
                    CSVReader reader = new CSVReader(new StringReader((String) value));
                    String[] tokens = reader.readNext();
                    for (String tok : tokens) {
                        if (terms.contains(tok.trim())) {
                            rows.add(i);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SummaryTableModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        Collections.sort(rows);
        return rows;
    }

    public synchronized Object[] getValueAndFilteredStatusAt(int row, int col) {
        int realRow = row + (pageOffset * pageSize);
        Object[] o = new Object[2];

        // check if row is in current page, schedule if not
        ArrayList<STMRecord> page = data;
        int pageIndex = realRow - dataOffset;
        if (pageIndex < 0 || pageIndex >= page.size()) {
            // not loaded
            schedule(realRow);
            if (!summary) {
                if (col == 4 || col == 5 || col == 8) {
                    o[0] = Math.E;
                    o[1] = false;
                    return o;
                }
            } else if (col == 47 || col == 48) {
                o[0] = Math.E;
                o[1] = false;
                return o;
            }
            o[0] = "loading...";
            o[1] = false;
            return o;
        }
        STMRecord rowObject = page.get(pageIndex);
        o[0] = rowObject.getValueAt(col);
        o[1] = rowObject.result.filtered;
        return o;
    }

    // Work only on the visible part of the table.
    @Override
    public synchronized Object getValueAt(int row, int col) {
        int realRow = row + (pageOffset * pageSize);

        // check if row is in current page, schedule if not  
        ArrayList<STMRecord> page = data;
        int pageIndex = realRow - dataOffset;
        if (pageIndex < 0 || pageIndex >= page.size()) {
            // not loaded  
            schedule(realRow);
            if (!summary) {
                if (col == 4 || col == 5 || col == 8) {
                    return Math.E;
                }
            } else if (col == 47 || col == 48) {
                return Math.E;
            }
            return "loading...";
        }
        STMRecord rowObject = page.get(pageIndex);
        return rowObject.getValueAt(col);
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (aValue == null || (aValue instanceof String && ((String) aValue).trim().equals(""))) {
            aValue = "-";
        }
        int realRow = row + (pageOffset * pageSize);

        ArrayList<STMRecord> page = data;
        int pageIndex = realRow - dataOffset;
        STMRecord rowObject = page.get(pageIndex);
        rowObject.setValueAt(col, aValue);
        worker.persistBiotypesFromSummary(rowObject.result, "admin", authToken);
    }

    public SummaryWorker getWorker() {
        return worker;
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
                        + " of " + totalrecords + " results");
            } else {
                statusLabel.setText(Integer.toString(1 + (pageOffset * pageSize))
                        + ", " + totalrecords
                        + " of " + totalrecords + " results");
            }
            fireTableDataChanged();
        }
    }

    // Update the page offset and fire a data changed (all rows).
    public void pageUp() {
        if (pageOffset > 0) {
            pageOffset--;
            statusLabel.setText(Integer.toString(1 + (pageOffset * pageSize))
                    + ", " + Integer.toString(((pageOffset + 1) * pageSize))
                    + " of " + totalrecords + " results");
            fireTableDataChanged();
        }
    }

    // We provide our own version of a scrollpane that includes
    // the page up and page down buttons by default.
    public static JScrollPane createPagingScrollPaneForTable(JTable jt) {
        JScrollPane jsp = LazyViewPort.createLazyScrollPaneFor(jt);
        TableModel tmodel = jt.getModel();

        // Don't choke if this is called on a regular table . . .
        if (!(tmodel instanceof SummaryTableModel)) {
            return jsp;
        }

        // Okay, go ahead and build the real scrollpane
        final SummaryTableModel model = (SummaryTableModel) tmodel;
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

    public void setEditable(boolean e) {
        editable = e;
    }

    public void setAuthToken(String at) {
        authToken = at;
    }

    public void setWorker(SummaryWorker w) {
        worker = w;
    }

    private void schedule(int offset) {
        // schedule the loading of the neighborhood around offset (if not already scheduled)  
        if (isPending(offset)) {
            // already scheduled -- do nothing  
            return;
        }
        int startOffset = Math.max(0, offset - MAX_PAGE_SIZE / 2);
        int length = offset + MAX_PAGE_SIZE / 2 - startOffset;
        load(offset, getPageSize());
    }

    private boolean isPending(int offset) {
        int sz = pending.size();
        if (sz == 0) {
            return false;
        }
        if (sz == 1) {
            // special case (for speed)  
            Segment seg = pending.first();
            return seg.contains(offset);
        }
        Segment lo = new Segment(offset - MAX_PAGE_SIZE, 0);
        Segment hi = new Segment(offset + 1, 0);
        // search pending segments that may contain offset
        if (pending.subSet(lo, hi).stream().anyMatch((seg) -> (seg.contains(offset)))) {
            return true;
        }
        return false;
    }

    private void load(final int startOffset, final int length) {
        // simulate something slow like loading from a database  
        final Segment seg = new Segment(startOffset, length);
        pending.add(seg);
        // set up code to run in another thread  
        Runnable fetch;
        fetch = () -> {
            try {
                // simulate network
                Thread.sleep(LATENCY_MILLIS);
            } catch (InterruptedException ex) {
                Logger.getLogger(SummaryTableModel.class.getName()).log(Level.SEVERE, null, ex);
                pending.remove(seg);
                return;
            }
            final ArrayList<STMRecord> page = new ArrayList<>();
            for (int j = 0; j < length; j += 1) {
                page.add(new STMRecord(j + startOffset, null));
            }
            // done loading -- make available on the event dispatch thread
            SwingUtilities.invokeLater(() -> {
                setData(startOffset, page);
                pending.remove(seg);
            });
        };
        // run on another thread  
        new Thread(fetch).start();
    }

    public void setData(List<SummaryResults> sr) {
        ArrayList<STMRecord> newData = new ArrayList<>();
        for (int i = 0; i < sr.size(); i++) {
            STMRecord stRecord = new STMRecord(i, sr.get(i));
            newData.add(stRecord);
        }
        setData(0, newData);
    }

    private void setData(int offset, ArrayList<STMRecord> newData) {
        // This method must be called from the event dispatch thread.  
        int lastRow = offset + newData.size() - 1;
        dataOffset = offset;
        data = newData;
        fireTableDataChanged();
    }

    // ---------------- begin static nested class ----------------  
    /**
     * This class is used to keep track of which rows have been scheduled for
     * loading, so that rows don't get scheduled twice concurrently. The idea is
     * to store Segments in a sorted data structure for fast searching.
     *
     * The compareTo() method sorts first by base position, then by length.
     */
    static final class Segment implements Comparable<Segment> {

        private int base = 0, length = 1;

        public Segment(int base, int length) {
            this.base = base;
            this.length = length;
        }

        public boolean contains(int pos) {
            return (base <= pos && pos < base + length);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Segment && base == ((Segment) o).base && length == ((Segment) o).length;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + this.base;
            hash = 97 * hash + this.length;
            return hash;
        }

        @Override
        public int compareTo(Segment other) {
            //return negative/zero/positive as this object is less-than/equal-to/greater-than other  
            int d = base - other.base;
            if (d != 0) {
                return d;
            }
            return length - other.length;
        }
    }

    class STMRecord {

        int counter;
        Object[] data;
        SummaryResults result;

        public STMRecord(int i, SummaryResults sr) {
            try {
                counter = i;
                if (!summary) {
                    result = comparisons.get(i);
                    String geneId = result.geneId;
                    String symbol = (result.geneSymbol == null) ? " " : result.geneSymbol;
                    String synonyms = result.synonymousNames;
                    String levelIC = result.levelIclassification;
                    String levelIIC = result.levelIIclassification;
                    String levelIIIC = result.levelIIIclassification;
                    String levelIVC = result.levelIVclassification;
                    String hierarchy1 = result.hierarchyup;
                    String hierarchy2 = result.hierarchydown;
                    String hierarchy3 = result.hierarchy0isoform;
                    String hierarchy4 = result.hierarchy0mutation;
                    String functionLevelI = result.levelIfunction;
                    String geneStructureLevelI = result.levelIgenestructure;
                    String disease = result.disease;
                    String roleInCancer = result.roleincancers;
                    Double rpkm = result.rpkm;
                    Double rpkm1 = result.rpkm1;
                    Integer rpkm_rnk = result.rpkmRank;
                    Integer rpkm_rnk1 = result.rpkmRank1;
                    Double rpkm_diff = result.rpkmDiff;
                    if (rpkm_diff == null || rpkm_diff.isNaN()) {
                        rpkm_diff = -1d;
                    }
                    data = new Object[]{
                        geneId, symbol, levelIIC, functionLevelI, rpkm, rpkm1, rpkm_rnk, rpkm_rnk1, rpkm_diff
                    };
                } else {
                    if (worker != null) {
                        result = worker.getResult(i);
                    } else if (sr != null) {
                        result = sr;
                    }

                    String geneId = result.geneId;
                    String symbol = (result.geneSymbol == null) ? " " : result.geneSymbol;

                    List<Integer> annotations = result.annotations;
                    Integer sc = 0;
                    Integer stc = 0;
                    Integer ss = 0;
                    Integer polya = 0;
                    Integer cs = 0;
                    Integer variants = 0;
                    Integer sirna = 0;
                    Integer acs = 0;
                    Integer afsse = 0;
                    Integer atsse = 0;
                    Integer fse = 0;
                    Integer lse = 0;
                    Integer mee = 0;
                    Integer ritu = 0;
                    Integer ri = 0;
                    Integer ins = 0;
                    Integer del = 0;
                    Integer tra = 0;
                    Integer ff = 0;
                    Integer other = 0;
                    Integer promoter = 0;
                    Integer antiSense = 0;
                    Integer ssDiff = 0;
                    Integer scDiff = 0;
                    if (annotations.size() == 24) {
                        sc = annotations.get(0);
                        stc = annotations.get(1);
                        ss = annotations.get(2);
                        polya = annotations.get(3);
                        cs = annotations.get(4);
                        variants = annotations.get(5);
                        sirna = annotations.get(6);
                        acs = annotations.get(7);
                        afsse = annotations.get(8);
                        atsse = annotations.get(9);
                        fse = annotations.get(10);
                        lse = annotations.get(11);
                        mee = annotations.get(12);
                        ritu = annotations.get(13);
                        ri = annotations.get(14);
                        ins = annotations.get(15);
                        del = annotations.get(16);
                        tra = annotations.get(17);
                        ff = annotations.get(18);
                        other = annotations.get(10);
                        promoter = annotations.get(20);
                        antiSense = annotations.get(21);
                        ssDiff = annotations.get(22);
                        scDiff = annotations.get(23);
                    }

                    String synonyms = result.synonymousNames;
                    String levelIC = result.levelIclassification;
                    String levelIIC = result.levelIIclassification;
                    String levelIIIC = result.levelIIIclassification;
                    String levelIVC = result.levelIVclassification;
                    String hierarchy1 = result.hierarchyup;
                    String hierarchy2 = result.hierarchydown;
                    String hierarchy3 = result.hierarchy0isoform;
                    String hierarchy4 = result.hierarchy0mutation;
                    String hierarchy5 = result.hierarchy0other;
                    String functionLevelI = result.levelIfunction;
                    String geneStructureLevelI = result.levelIgenestructure;
                    String disease = result.disease;
                    String roleInCancer = result.roleincancers;

                    Long nrrcrt = result.masterReadsNonRedundant;
                    Long trcrt = result.masterReadsTotal;
                    Long tmbur = result.mappedTranscriptsByMasterReads;
                    Long rtl = result.masterTranscriptLength;
                    Integer noi = result.isoforms;
                    Long rcutoi = result.otherReadsCount;
                    Double trcbrtl = result.mappedReadByLength;
                    Double rpkm = result.rpkm;
                    Integer rpkm_rnk = result.rpkmRank;
                    data = new Object[]{
                        geneId, symbol, sc, stc, ss, polya, cs, variants, sirna,
                        acs, afsse, atsse, fse, lse, mee, ritu, ri, ins, del, tra,
                        ff, other, promoter, antiSense, ssDiff, scDiff,
                        synonyms, levelIC, levelIIC, levelIIIC, levelIVC,
                        hierarchy1, hierarchy2, hierarchy3, hierarchy4, hierarchy5,
                        functionLevelI, geneStructureLevelI, disease, roleInCancer, nrrcrt, trcrt, tmbur,
                        rtl, noi, rcutoi, trcbrtl, rpkm, rpkm_rnk
                    };
                }
            } catch (Exception e) {
                e.printStackTrace();
                data = new Object[]{"", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            }
        }

        public Object getValueAt(int i) {
            return data[i];
        }

        public void setValueAt(int i, Object o) {
            data[i] = o;
            if (summary) {
                switch (i) {
                    case 26:
                        result.synonymousNames = (String) o;
                        break;
                    case 27:
                        result.levelIclassification = (String) o;
                        break;
                    case 28:
                        result.levelIIclassification = (String) o;
                        break;
                    case 29:
                        result.levelIIIclassification = (String) o;
                        break;
                    case 30:
                        result.levelIVclassification = (String) o;
                        break;
                    case 31:
                        result.hierarchyup = (String) o;
                        break;
                    case 32:
                        result.hierarchydown = (String) o;
                        break;
                    case 33:
                        result.hierarchy0isoform = (String) o;
                        break;
                    case 34:
                        result.hierarchy0mutation = (String) o;
                        break;
                    case 35:
                        result.hierarchy0other = (String) o;
                        break;
                    case 36:
                        result.levelIfunction = (String) o;
                        break;
                    case 37:
                        result.levelIgenestructure = (String) o;
                        break;
                    case 38:
                        result.disease = (String) o;
                        break;
                    case 39:
                        result.roleincancers = (String) o;
                        break;
                    default:
                        break;
                }
            }
        }

        public Class<?> getColumnClass(int modelIndex) {
            if (!summary) {
                return columnArrayComparison[modelIndex].columnClass;
            }
            return columnArraySummary[modelIndex].columnClass;
        }

        public int getColumnCount() {
            if (!summary) {
                return columnArrayComparison.length;
            }
            return columnArraySummary.length;
        }

        public String getColumnName(int modelIndex) {
            if (!summary) {
                return columnArrayComparison[modelIndex].columnName;
            }
            return columnArraySummary[modelIndex].columnName;
        }

        public String getToolTipText() {
            return "";
        }
    }

    class STMArrowIcon implements Icon {

        public static final int UP = 0;
        public static final int DOWN = 1;
        private final int direction;
        private final Polygon pagePolygon = new Polygon(new int[]{2, 4, 4, 10, 10, 2},
                new int[]{4, 4, 2, 2, 12, 12}, 6);
        private final int[] arrowX = {4, 9, 6};
        private final Polygon arrowUpPolygon = new Polygon(arrowX,
                new int[]{10, 10, 4}, 3);
        private final Polygon arrowDownPolygon = new Polygon(arrowX,
                new int[]{6, 6, 11}, 3);

        public STMArrowIcon(int which) {
            direction = which;
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            pagePolygon.translate(x, y);
            g.drawPolygon(pagePolygon);
            pagePolygon.translate(-x, -y);
            if (direction == UP) {
                arrowUpPolygon.translate(x, y);
                g.fillPolygon(arrowUpPolygon);
                arrowUpPolygon.translate(-x, -y);
            } else {
                arrowDownPolygon.translate(x, y);
                g.fillPolygon(arrowDownPolygon);
                arrowDownPolygon.translate(-x, -y);
            }
        }
    }
}
