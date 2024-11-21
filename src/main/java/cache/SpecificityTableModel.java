package cache;

import cache.SummaryForm.Triplet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

/**
 *
 * @author mk2432
 */
public class SpecificityTableModel extends AbstractTableModel {

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
    List<SummaryResults> comparisons = null;
    List<Triplet> experiments = null;

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int modelIndex) {
        if (modelIndex == 0) {
            return URL.class;
        } else if (modelIndex == 1) {
            return String.class;
        } else if (modelIndex == 2) {
            return String.class;
        } else if (modelIndex == 3) {
            return String.class;
        } else if (modelIndex == 4) {
            return String.class;
        } else if (modelIndex > 4 && modelIndex < (experiments.size() + 5)) {
            return Integer.class;
        } else if (modelIndex >= (experiments.size() + 5)) {
            return Double.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount() {
        return (2 * experiments.size()) + 7;
    }

    @Override
    public String getColumnName(int modelIndex) {
        if (modelIndex == 0) {
            return "Gene Ensembl ID";
        } else if (modelIndex == 1) {
            return "Gene Symbol";
        } else if (modelIndex == 2) {
            return "Biotype";
        } else if (modelIndex == 3) {
            return "Level II classification";
        } else if (modelIndex == 4) {
            return "Level I function";
        } else if (modelIndex > 4 && modelIndex < (experiments.size() + 5)) {
            return "Rank_" + experiments.get(modelIndex - 5).eid + ":" + experiments.get(modelIndex - 5).distance;
        } else if (modelIndex == (experiments.size() + 5)) {
            return "Variance";
        } else if (modelIndex == (experiments.size() + 6)) {
            return "Entropy";
        } else if (modelIndex > (experiments.size() + 6)) {
            return "RPKM_" + experiments.get(modelIndex - (experiments.size() + 7)).eid + ":" + experiments.get(modelIndex - (experiments.size() + 7)).distance;
        }
        return "";
    }

    public SpecificityTableModel(JLabel label) {
        this(0, 100, label);
    }

    public SpecificityTableModel(long numRows, int size, JLabel label) {
        statusLabel = label;
        totalrecords = numRows;
        data = new ArrayList<>();
        setPageSize(size);
    }

    public SpecificityTableModel(long numRows, int size, JLabel label, List<SummaryResults> comparisons, List<Triplet> experiments) {
        statusLabel = label;
        totalrecords = numRows;
        data = new ArrayList<>();
        this.comparisons = comparisons;
        this.experiments = experiments;
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
            if (col == 5 || col == 6 || col == 9) {
                return Math.E;
            }
            if (col > 4 && col < (experiments.size() + 5)) {
                return 0;
            } else if (col >= (experiments.size() + 5)) {
                return 0d;
            } else {
                return "loading...";
            }
        }
        STMRecord rowObject = page.get(pageIndex);
        return rowObject.getValueAt(col);
    }

    public SummaryWorker getWorker() {
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

    public void setSearchSourceSource(SummaryWorker w) {
        worker = w;
    }

    public SummaryWorker getSearchSource() {
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

    final class STMRecord {

        int counter;
        Object[] data;
        SummaryResults result;

        public STMRecord(int i, SummaryResults sr) {
            try {
                counter = i;
                result = comparisons.get(i);

                List<Object> values = new ArrayList<>();
                for (int j = 0; j < getColumnCount(); j++) {
                    if (j == 0) {
                        URL geneURL = createGeneURL(result.geneId);
                        values.add(j, geneURL);
                    } else if (j == 1) {
                        String symbol = (result.geneSymbol == null) ? " " : result.geneSymbol;
                        values.add(j, symbol);
                    } else if (j == 2) {
                        String biotype = (result.biotype == null) ? " " : result.biotype;
                        values.add(j, biotype);
                    } else if (j == 3) {
                        String classification = (result.levelIIclassification == null) ? " " : result.levelIIclassification;
                        values.add(j, classification);
                    } else if (j == 4) {
                        String function = (result.levelIfunction == null) ? " " : result.levelIfunction;
                        values.add(j, function);
                    } else if (j > 4 && j < (experiments.size() + 5)) {
                        Integer rank = result.otherRanks.get(j - 5);
                        values.add(j, rank);
                    } else if (j == (experiments.size() + 5)) {
                        Double variance = result.rankVariance;
                        values.add(j, variance);
                    } else if (j == (experiments.size() + 6)) {
                        Double entropy = result.rankEntropy;
                        if (entropy == null || entropy.isNaN()) {
                            entropy = 0d;
                        }
                        values.add(j, entropy);
                    } else if (j > (experiments.size() + 6)) {
                        Double rpkm = result.otherRpkms.get(j - (experiments.size() + 7));
                        values.add(j, rpkm);
                    }
                }
                data = values.toArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private URL createTranscriptURL(String name) {
            URL url = null;
            if (name != null) {
                try {
                    String base = "species=Homo_sapiens;idx=Transcript;end=1;q=";
                    String id = name.split(" ")[0];
                    URI uri = new URI(
                            "http",
                            "useast.ensembl.org",
                            "/Homo_sapiens/Search/Details",
                            base + id,
                            null);
                    url = uri.toURL();
                } catch (URISyntaxException | MalformedURLException ex) {
                    Logger.getLogger(SummaryTableModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return url;
        }

        private URL createGeneURL(String name) {
            URL url = null;
            try {
                URI uri = new URI(
                        "http",
                        "useast.ensembl.org",
                        "/Homo_sapiens/Gene/Summary",
                        "g=" + name,
                        null);
                url = uri.toURL();
            } catch (URISyntaxException | MalformedURLException ex) {
                Logger.getLogger(SummaryTableModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return url;
        }

        public Object getValueAt(int i) {
            return data[i];
        }

        public int getColumnCount() {
            return (2 * experiments.size()) + 7;
        }

        public String getColumnName(int modelIndex) {
            if (modelIndex == 0) {
                return "Gene Ensembl ID";
            } else if (modelIndex == 1) {
                return "Gene Symbol";
            } else if (modelIndex == 2) {
                return "Biotype";
            } else if (modelIndex == 3) {
                return "Level II classification";
            } else if (modelIndex == 4) {
                return "Level I function";
            } else if (modelIndex > 4 && modelIndex < (experiments.size() + 5)) {
                return "Rank_" + experiments.get(modelIndex - 1);
            } else if (modelIndex == (experiments.size() + 5)) {
                return "Variance";
            } else if (modelIndex == (experiments.size() + 6)) {
                return "Entropy";
            } else if (modelIndex > (experiments.size() + 6)) {
                return "RPKM_" + experiments.get(modelIndex - (experiments.size() + 7));
            }
            return "";
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
