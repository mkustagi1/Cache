package cache;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import cache.dataimportes.holders.TranscriptMappingResults;
import cache.util.LazyViewPort;
import cache.workers.AlignmentWorker;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

class PagingModel extends AbstractTableModel {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int LATENCY_MILLIS = 1000;
    public static AlignmentWorker worker;
    protected int pageSize = 0;
    protected int pageOffset;
    protected ArrayList<Record> data;
    protected long totalrecords;
    private int dataOffset = 0;
    private SortedSet<Segment> pending = new TreeSet<>();
    Map<Long, List<Long>> mappedTranscripts;

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
        new ColumnContext("Experiment ID", Integer.class, false),
        new ColumnContext("Distance", Integer.class, false),
        new ColumnContext("Gene Symbol", String.class, false),
        new ColumnContext("Gene Biotype", String.class, false),
        new ColumnContext("Non-redundant Read Count", Long.class, false),
        new ColumnContext("Total Read Count", Long.class, false),
        new ColumnContext("Transcript Length", Integer.class, false),
        new ColumnContext("RPKM", Double.class, false)
    };

    @Override
    public boolean isCellEditable(int row, int col) {
        return columnArray[col].isEditable;
    }

    @Override
    public Class<?> getColumnClass(int modelIndex) {
        return columnArray[modelIndex].columnClass;
    }

    @Override
    public int getColumnCount() {
        return columnArray.length;
    }

    @Override
    public String getColumnName(int modelIndex) {
        return columnArray[modelIndex].columnName;
    }

    public PagingModel() {
        this(0, 100, new HashMap<>());
    }

    public PagingModel(long numRows, int size, Map<Long, List<Long>> mappedTranscripts) {
        totalrecords = numRows;
        this.mappedTranscripts = mappedTranscripts;
        data = new ArrayList<>();
        setPageSize(size);
    }

    public ArrayList<Record> getData() {
        return data;
    }

    // Return values appropriate for the visible table part.
    @Override
    public int getRowCount() {
        return pageSize;
    }

    public boolean removeRow(int row) {
        int realRow = row + (pageOffset * pageSize);
        // check if row is in current page, schedule if not  
        ArrayList<Record> page = data;
        int pageIndex = realRow - dataOffset;
        if (pageIndex < 0 || pageIndex >= page.size()) {
            return false;
        }
        totalrecords--;
        pageSize--;
        page.remove(pageIndex);
        return true;
    }

    // Work only on the visible part of the table.
    @Override
    public synchronized Object getValueAt(int row, int col) {
        int realRow = row + (pageOffset * pageSize);

        // check if row is in current page, schedule if not  
        ArrayList<Record> page = data;
        int pageIndex = realRow - dataOffset;
        if (pageIndex < 0 || pageIndex >= page.size()) {
            // not loaded  
            schedule(realRow);
            if (col == 7) {
                return Math.E;
            }
            return "";
        }
        Record rowObject = page.get(pageIndex);
        return rowObject.getValueAt(col);
    }

    // Use this method to figure out which page you are on.
    public int getPageOffset() {
        return pageOffset;
    }

    public int getPageCount() {
        return (int) Math.ceil(((double) totalrecords) / pageSize);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int s) {
        if (s == 0) {
            pageSize = 0;
            pageOffset = 0;
        } else {
            if (s == pageSize) {
                return;
            }
            int oldPageSize = pageSize;
            pageSize = s;
            pageOffset = (oldPageSize * pageOffset) / pageSize;
        }
        fireTableDataChanged();
    }

    // Update the page offset and fire a data changed (all rows).
    public void pageDown() {
        if (pageOffset < getPageCount() - 1) {
            pageOffset++;
            fireTableDataChanged();
        }
    }

    // Update the page offset and fire a data changed (all rows).
    public void pageUp() {
        if (pageOffset > 0) {
            pageOffset--;
            fireTableDataChanged();
        }
    }

    // We provide our own version of a scrollpane that includes
    // the page up and page down buttons by default.
    public static JScrollPane createPagingScrollPaneForTable(JTable jt) {
        JScrollPane jsp = LazyViewPort.createLazyScrollPaneFor(jt);
        TableModel tmodel = jt.getModel();

        // Don't choke if this is called on a regular table . . .
        if (!(tmodel instanceof PagingModel)) {
            return jsp;
        }

        // Okay, go ahead and build the real scrollpane
        final PagingModel model = (PagingModel) tmodel;
        final JButton upButton = new JButton(new ArrowIcon(ArrowIcon.UP));
        upButton.setEnabled(false); // starts off at 0, so can't go up
        final JButton downButton = new JButton(new ArrowIcon(ArrowIcon.DOWN));
        if (model.getPageCount() <= 1) {
            downButton.setEnabled(false); // One page...can't scroll down
        }

        upButton.addActionListener((ActionEvent ae) -> {
            model.pageUp();
            if (model.getPageOffset() == 0) {
                upButton.setEnabled(false);
            }
            downButton.setEnabled(true);
        });

        downButton.addActionListener((ActionEvent ae) -> {
            model.pageDown();
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

    public static void setAlignmentSource(AlignmentWorker w) {
        worker = w;
    }

    public static AlignmentWorker getAlignmentSource() {
        return worker;
    }

    private void schedule(int offset) {
        // schedule the loading of the neighborhood around offset (if not already scheduled)  
        if (isPending(offset)) {
            // already scheduled -- do nothing  
            return;
        }
        int startOffset = Math.max(0, offset - MAX_PAGE_SIZE / 2);
        int length = offset + MAX_PAGE_SIZE / 2 - startOffset;
        load(startOffset, length);
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
                Logger.getLogger(PagingModel.class.getName()).log(Level.SEVERE, null, ex);
                pending.remove(seg);
                return;
            }
            final ArrayList<Record> page = new ArrayList<>();
            for (int j = 0; j < length; j += 1) {
                page.add(new Record(j + startOffset, mappedTranscripts));
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

    private void setData(int offset, ArrayList<Record> newData) {
        if (newData.size() > 0) {
            // This method must be called from the event dispatch thread.  
            int lastRow = offset + newData.size() - 1;
            dataOffset = offset;
            data = newData;
            fireTableDataChanged();
        } else {
            totalrecords = 0l;
            data.clear();
            setPageSize(0);
        }
    }

    public void setData(List<Long> tids) {
        List<TranscriptMappingResults> results = new ArrayList<>();

        Map<Long, List<Long>> mt = new HashMap<>();
        long eid = PagingModel.worker.getExperimentId();
        mt.put(eid, tids);
        System.out.println(eid + ", " + tids.toString());
        for (int i = 0; i < tids.size(); i++) {
            TranscriptMappingResults tmr = PagingModel.worker.getTranscriptMapping(i, mt);
            results.add(tmr);
        }
        ArrayList<Record> newData = new ArrayList<>();
        results.stream().map((tmr) -> new Record(tmr)).forEach((record) -> {
            newData.add(record);
        });
        System.out.println("New data size: " + newData.size());
        setData(0, newData);
    }

    public void setData(String transcriptName) {
        if (transcriptName != null && !transcriptName.equals("")) {
            List<TranscriptMappingResults> results = new ArrayList<>();
            if (transcriptName.startsWith("ENST")) {
                results = PagingModel.worker.getTranscriptMapping(transcriptName, TranscriptMappingResults.QUERY_TYPE.TRANSCRIPT);
            } else {
                results = PagingModel.worker.getTranscriptMapping(transcriptName, TranscriptMappingResults.QUERY_TYPE.GENE);
            }
            ArrayList<Record> newData = new ArrayList<>();
            for (TranscriptMappingResults tmr : results) {
                Record record = new Record(tmr);
                newData.add(record);
            }
            System.out.println("New data size: " + newData.size());
            setData(0, newData);
        }
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
}

class Record {

    static String[] headers = {"Experiment ID", "Distance", "Gene Symbol", "Gene Biotype", "Non-redundant Read Count", "Total Read Count", "RPKM"};
    int counter;
    Object[] data;
    TranscriptMappingResults result;

    public Record(int i, Map<Long, java.util.List<Long>> mt) {
        try {
            counter = i;
            if (mt != null && mt.size() > 0) {
                result = PagingModel.worker.getTranscriptMapping(i, mt);
                if (result != null) {
                    String symbol = result.symbol;
                    String biotype = result.biotype;
                    Integer distance = PagingModel.worker.getDistance();

                    data = new Object[]{
                        new Long(PagingModel.worker.getExperimentId()),
                        distance,
                        symbol,
                        biotype,
                        result.mappedCount,
                        result.totalMappedCount,
                        result.transcriptLength,
                        result.rpkm};
                } else {
                    data = new Object[]{
                        new Long(0), 0, "",
                        "",
                        new Long(0),
                        new Long(0), 0,
                        new Double(0)};
                }
            } else {
                data = new Object[]{
                    new Long(0), 0, "",
                    "",
                    new Long(0),
                    new Long(0), 0,
                    new Double(0)};
            }
        } catch (Exception e) {
            e.printStackTrace();
            data = new Object[]{
                new Long(0), 0, "",
                "",
                new Long(0),
                new Long(0), 0,
                new Double(0)};
        }
    }

    public Record(TranscriptMappingResults tmr) {
        try {
            result = tmr;
            String symbol = result.symbol;
            String biotype = result.biotype;
            Integer distance = PagingModel.worker.getDistance();
            data = new Object[]{
                new Long(PagingModel.worker.getExperimentId()),
                distance,
                symbol,
                biotype, result.mappedCount, result.totalMappedCount, result.transcriptLength, result.rpkm};
        } catch (Exception e) {
            e.printStackTrace();
            data = new String[]{"", "", "", "", "", "", "", ""};
        }
    }

    public Object getValueAt(int i) {
        if (i >= data.length) {
            return new Object();
        }
        return data[i];
    }

    public static String getColumnName(int i) {
        return headers[i];
    }

    public static int getColumnCount() {
        return headers.length;
    }
}

//ArrowIcon.java
//A simple implementation of the Icon interface that can make
//Up and Down arrows.
//
class ArrowIcon implements Icon {

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

    public ArrowIcon(int which) {
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
