package cache.workers;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingWorker;
import cache.dataimportes.holders.SearchResult;
import cache.dataimportes.holders.Strand;
import cache.dataimportes.holders.TranscriptMappingResults;
import java.util.UUID;

/**
 *
 * @author Manjunath Kustagi
 */
public class SearchWorker extends SwingWorker {

    long experimentID;
    int distance;
    int key;
    public int searchLength;
    public SearchResult.SearchType searchType;
    public Strand strand;
    String searchSequence = "";
    protected cache.interfaces.AdminService adminService;
    protected cache.interfaces.AlignmentService alignmentService;
    List<SearchResult> results = null;
    public int maxCount = 0;

    public SearchWorker(int k, long eid, int d, String s, int mc, int sl, SearchResult.SearchType st, Strand str) {
        key = k;
        experimentID = eid;
        distance = d;
        searchLength = sl;
        searchType = st;
        searchSequence = s;
        strand = str;
        maxCount = mc;
        try {
            String url = System.getProperty("base.url") + "AdminService";
            String urla = System.getProperty("base.url") + "AlignmentService";
            HessianProxyFactory factory = new HessianProxyFactory();
            adminService = (cache.interfaces.AdminService) factory.create(cache.interfaces.AdminService.class, url);
            alignmentService = (cache.interfaces.AlignmentService) factory.create(cache.interfaces.AlignmentService.class, urla);
        } catch (MalformedURLException mue) {
        }

    }

    @Override
    protected Object doInBackground() throws Exception {
        results = adminService.searchReads(key, experimentID, distance, searchSequence, maxCount, searchLength, searchType, strand);
        Collections.sort(results);
        return results;
    }

    public void loadSearchResults() {
        if (searchType == SearchResult.SearchType.READS) {
            results = adminService.searchReads(key, experimentID, distance, searchSequence, maxCount, searchLength, searchType, strand);
        } else  if (searchType == SearchResult.SearchType.TRANSCRIPTS) {
            results = adminService.searchDatabase(searchSequence, key, distance, maxCount, searchLength, searchType, strand);
        }
        Collections.sort(results);
    }

    public void setExperimentID(int id) {
        experimentID = id;
    }

    public int getExperimentId() {
        return (int) experimentID;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public List<TranscriptMappingResults> getMappedTranscripts(long experimentId, UUID readId) {
        return alignmentService.getMappedTranscripts(key, experimentId, distance, readId);
    }

    public int getResultSize() {
        if (results != null) {
            return results.size();
        }
        return maxCount;
    }

    public SearchResult getResult(int i) {
        if (results != null && i < results.size()) {
            return results.get(i);
        }
        return null;
    }
}
