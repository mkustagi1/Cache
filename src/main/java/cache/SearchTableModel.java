package cache;

import cache.dataimportes.holders.SearchResult;
import cache.dataimportes.holders.Strand;
import cache.dataimportes.holders.TranscriptMappingResults;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import cache.util.LazyViewPort;
import cache.workers.SearchWorker;
import java.util.UUID;

class SearchTableModel extends AbstractTableModel {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int LATENCY_MILLIS = 0;
    protected SearchWorker worker;
    protected int pageSize = 0;
    protected int pageOffset;
    protected ArrayList<STMRecord> data;
    protected long totalrecords;
    private int dataOffset = 0;
    private SortedSet<Segment> pending = new TreeSet<>();
    JLabel statusLabel;
    SearchResult.SearchType searchType = SearchResult.SearchType.READS;

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
    private static final ColumnContext[] columnArrayReads = {
        new ColumnContext("Experiment ID", Integer.class, false),
        new ColumnContext("Read ID", Long.class, false),
        new ColumnContext("Sequence", String.class, false),
        new ColumnContext("Read Count", Long.class, false),
        new ColumnContext("F/R", Integer.class, false)
    };
    private static final ColumnContext[] columnArrayTranscripts = {
        new ColumnContext("Transcript ID", Long.class, false),
        new ColumnContext("Sequence", String.class, false),
        new ColumnContext("F/R", Integer.class, false)
    };

    @Override
    public boolean isCellEditable(int row, int col) {
        if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
            return columnArrayTranscripts[col].isEditable;
        }
        return columnArrayReads[col].isEditable;
    }

    @Override
    public Class<?> getColumnClass(int modelIndex) {
        if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
            return columnArrayTranscripts[modelIndex].columnClass;
        }
        return columnArrayReads[modelIndex].columnClass;
    }

    @Override
    public int getColumnCount() {
        if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
            return columnArrayTranscripts.length;
        }
        return columnArrayReads.length;
    }

    @Override
    public String getColumnName(int modelIndex) {
        if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
            return columnArrayTranscripts[modelIndex].columnName;
        }
        return columnArrayReads[modelIndex].columnName;
    }

    public SearchTableModel(JLabel label) {
        this(0, 100, label, SearchResult.SearchType.READS);
    }

    public SearchTableModel(long numRows, int size, JLabel label, SearchResult.SearchType st) {
        statusLabel = label;
        totalrecords = numRows;
        searchType = st;
        data = new ArrayList<>();
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
            return "loading...";
        }
        STMRecord rowObject = page.get(pageIndex);
        return rowObject.getValueAt(col);
    }

    public SearchWorker getWorker() {
        return worker;
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
                statusLabel.setText(new Integer(1 + (pageOffset * pageSize)).toString()
                        + ", " + new Integer(((pageOffset + 1) * pageSize)).toString()
                        + " of " + totalrecords + " results");
            } else {
                statusLabel.setText(new Integer(1 + (pageOffset * pageSize)).toString()
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
        if (!(tmodel instanceof SearchTableModel)) {
            return jsp;
        }

        // Okay, go ahead and build the real scrollpane
        final SearchTableModel model = (SearchTableModel) tmodel;
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

    public void setSearchSourceSource(SearchWorker w) {
        worker = w;
    }

    public SearchWorker getSearchSource() {
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
        return pending.subSet(lo, hi).stream().anyMatch((seg) -> (seg.contains(offset)));
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
                Logger.getLogger(SearchTableModel.class.getName()).log(Level.SEVERE, null, ex);
                pending.remove(seg);
                return;
            }
            final ArrayList<STMRecord> page = new ArrayList<>();
            for (int j = 0; j < length; j += 1) {
                page.add(new STMRecord(j + startOffset));
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

    private void setData(int offset, ArrayList<STMRecord> newData) {
        // This method must be called from the event dispatch thread.  
        //System.out.println("setData: " + offset + " newData.size(): " + newData.size());
        int lastRow = offset + newData.size() - 1;
        dataOffset = offset;
        data = newData;
        //System.out.println("setData: " + offset + " lastRow: " + lastRow);
        fireTableDataChanged();
        //fireTableRowsUpdated(offset, lastRow);
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

        String[] headers = {"Experiment ID", "Read ID", "Sequence", "Read Count", "F/R"};
        int counter;
        Object[] data;
        String toolTipText = "";
        SearchResult result;

        public STMRecord(int i) {
            try {
                counter = i;
                result = worker.getResult(i);
                if (result == null) {
                    data = new Object[]{"", "", "", "", ""};
                    return;
                }
                Integer strand = (result.strand.equals(Strand.FORWARD)) ? 1 : 0;
                if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
                    data = new Object[]{
                        result.transcriptID,
                        result.sequence,
                        strand};
                    toolTipText = createTranscriptURL(result.name, null);
                } else if (searchType == SearchResult.SearchType.READS) {
                    data = new Object[]{
                        result.experimentID,
                        result.readID,
                        result.sequence, result.readCount,
                        strand};

                    toolTipText = composeBasicToolTipText(result.experimentID, result.readID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String composeBasicToolTipText(long e, UUID r) {
            StringBuilder html = new StringBuilder();
            List<TranscriptMappingResults> tmrs = worker.getMappedTranscripts(e, r);
            if (tmrs.size() > 0) {
                html.append("<html>Mapped Transcripts<ul>");
                for (TranscriptMappingResults tmr : tmrs) {
                    String name = tmr.name;
                    html.append("<li> ").append(name).append("</li>");
                }
                html.append("</ul></html>");
            }
            return html.toString();
        }

        private String createTranscriptURL(String name, URL _url) {
            StringBuilder html = new StringBuilder();
            html.append("<html>Mapped Transcripts<ul>");
            html.append("<li> ").append(name).append("</li>");
            html.append("</ul></html>");
            return html.toString();
        }

        public Object getValueAt(int i) {
            return data[i];
        }

        public String getColumnName(int i) {
            return headers[i];
        }

        public int getColumnCount() {
            return headers.length;
        }

        public String getToolTipText() {
            if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
                return toolTipText;
            }
            return composeBasicToolTipText(result.experimentID, result.readID);
        }
    }

//ArrowIcon.java
//A simple implementation of the Icon interface that can make
//Up and Down arrows.
//
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
