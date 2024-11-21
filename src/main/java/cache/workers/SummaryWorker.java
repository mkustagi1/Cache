package cache.workers;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;
import cache.dataimportes.holders.DataStatisticsResults;
import cache.dataimportes.holders.SummaryResults;

/**
 *
 * @author Manjunath Kustagi
 */
public class SummaryWorker extends SwingWorker {

    protected cache.interfaces.AdminService adminService;
    protected cache.interfaces.AlignmentService alignmentService;
    List<SummaryResults> results = new ArrayList<>();
    long experimentId = 0;
    Map<Long, List<SummaryResults>> map = new HashMap<>();

    @Override
    protected Object doInBackground() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SummaryWorker() {
        try {
            String url = System.getProperty("base.url") + "AdminService";
            String urla = System.getProperty("base.url") + "AlignmentService";
            HessianProxyFactory factory = new HessianProxyFactory();
            adminService = (cache.interfaces.AdminService) factory.create(cache.interfaces.AdminService.class, url);
            alignmentService = (cache.interfaces.AlignmentService) factory.create(cache.interfaces.AlignmentService.class, urla);
        } catch (MalformedURLException mue) {
        }
    }

    public void getSummariesForGenes(String geneSymbol, List<String> types) {
        results.clear();
        results.addAll(adminService.getSummariesForGenes(geneSymbol, types));
    }

    public List<SummaryResults> getSummary(int key, long experimentId, int distance) {
        return adminService.getSummary(key, experimentId, distance);
    }

    public void loadResults(int key, long experimentId, int distance) {
        this.experimentId = experimentId;
        if (!map.containsKey(experimentId)) {
            results = getSummary(key, experimentId, distance);
            map.put(experimentId, results);
        } else {
            results = map.get(experimentId);
        }
    }

    public void setResults(List<SummaryResults> sr) {
        results = sr;
    }
    
    public SummaryResults getResult(int i) {
        return results.get(i);
    }

    public List<SummaryResults> getResults() {
        return results;
    }

    public List<SummaryResults> getResultsForGene(String gene) {
        List<SummaryResults> r = new ArrayList<>();
        if (gene != null && !gene.equals("")) {
            results.stream().filter((result) -> (result.geneSymbol.equalsIgnoreCase(gene) || result.biotype.contains(gene))).map((result) -> {
                System.out.println(result.toString());
                return result;
            }).forEach((result) -> {
                r.add(result);
            });
        }
        return r;
    }

    public long getExperimentId() {
        return experimentId;
    }

    public DataStatisticsResults getDataStatistics(long experimentId, int distance) {
        return adminService.getDataStatistics(experimentId, distance);
    }

    public boolean authenticateUser(String user, String token) {
        return adminService.authenticateUser(user, token);
    }

    public void persistBiotypesFromSummary(SummaryResults sr, String user, String authToken) {
        adminService.persistBiotypesFromSummary(sr, user, authToken);
    }
    
    public void editBiotypes(String search, String replace, String user, String authToken){
        adminService.editBiotypes(search, replace, user, authToken);
    }

    public void editSummary(SummaryResults sr, boolean allExperiments, String user, String authToken){
        adminService.editSummary(sr, allExperiments, user, authToken);
    }
}
