package cache.workers;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;
import cache.dataimportes.holders.AlignmentResult;
import cache.dataimportes.holders.AnnotationResults;
import cache.dataimportes.holders.TranscriptMappingResults;

/**
 *
 * @author Manjunath Kustagi
 */
public class AlignmentWorker extends SwingWorker {

    long experimentID;
    int distance;
    int key;
    protected cache.interfaces.AdminService adminService;
    protected cache.interfaces.AlignmentService alignmentService;
    int alignmentCount;

    public AlignmentWorker() {
        try {
            String url = System.getProperty("base.url") + "AlignmentService";
            String adminUrl = System.getProperty("base.url") + "AdminService";
            HessianProxyFactory factory = new HessianProxyFactory();
            factory.setConnectTimeout(6000000);
            factory.setReadTimeout(6000000);
            adminService = (cache.interfaces.AdminService) factory.create(cache.interfaces.AdminService.class, adminUrl);
            alignmentService = (cache.interfaces.AlignmentService) factory.create(cache.interfaces.AlignmentService.class, url);
        } catch (MalformedURLException mue) {
        }

    }

    @Override
    protected Object doInBackground() throws Exception {
        return alignmentService.runAlignment(key, experimentID, distance);
    }

    public void setExperimentID(int k, long id, int d) {
        experimentID = id;
        distance = d;
        key = k;
//        alignmentCount = (int) alignmentService.getTranscriptMappingCount(experimentID, distance);
    }

    public int getExperimentId() {
        return (int) experimentID;
    }

    public int getDistance() {
        return distance;
    }
    
    public void setDistance(int d) {
        distance = d;
    }

    public int getKey() {
        return key;
    }
    
    public void setKey(int k) {
        key = k;
    }

    public int getAlignmentCount() {
        return alignmentCount;
    }

    public AlignmentResult getAlignment(int row) {
        return alignmentService.getAlignment(row, key, experimentID, distance);
    }

    public String getAlignedSequences(int row) {
        return alignmentService.getAlignedSequences((int) alignmentService.getAlignment(row, key, experimentID, distance).alignmentID);
    }

    public Map<Long, List<Long>> loadMappedTranscripts() {
        return alignmentService.loadMappedTranscripts(key, experimentID, distance);
    }

    public long getMappedTranscriptCount() {
        return alignmentService.getTranscriptMappingCount(key, experimentID, distance);
    }

    public TranscriptMappingResults getTranscriptMapping(int index, Map<Long, List<Long>> mappedTranscripts) {
        System.out.println(experimentID + ", " + mappedTranscripts.toString());
        return alignmentService.getTranscriptMapping(index, key, experimentID, distance, mappedTranscripts);
    }

    public List<TranscriptMappingResults> getTranscriptMapping(String name, TranscriptMappingResults.QUERY_TYPE queryType) {
        System.out.println(experimentID + ", " + name);
        return alignmentService.getTranscriptMappingByName(name, key, experimentID, distance);
    }

    public TranscriptMappingResults populateAlignmentDisplay(TranscriptMappingResults tmr) {
        return alignmentService.populateAlignmentDisplay(tmr, key, experimentID, distance);
    }

    public int getTranscriptCountForGene(String gene) {
        return alignmentService.getTranscriptCountForGene(gene);
    }

    public List<String> getGenesByPartialSymbol(String partialGene) {
        return alignmentService.getGenesByPartialSymbol(partialGene);
    }

    public List<String> getDistinctBiotypes() {
        return alignmentService.getDistinctBiotypes();
    }

    public List<String> getGenesForBiotypes(List<String> biotypes) {
        return alignmentService.getGenesForBiotypes(biotypes);
    }

    public void createOrDeleteAnnotation(List<AnnotationResults> ar, boolean create) {
        alignmentService.createOrDeleteAnnotation(ar, create);
    }

    public List<AnnotationResults> getAnnotationsForTranscript(long transcriptId) {
        return alignmentService.getAnnotationsForTranscript(transcriptId);
    }

    public void deleteTranscript(long transcriptId, String authToken) {
        alignmentService.deleteTranscript(transcriptId, authToken);
    }

    public void editTranscriptName(long transcriptId, String newName, String authToken) {
        alignmentService.editTranscriptName(transcriptId, newName, authToken);
    }

    public void beginEditTranscript(int transcriptId, String authToken) {
        alignmentService.beginEditTranscript(transcriptId, authToken);
    }

    public TranscriptMappingResults previewEditTranscript(TranscriptMappingResults tmr, long experimentId, String authToken) {
        return alignmentService.previewEditTranscript(tmr, key, experimentId, distance, authToken);
    }

    public void persistEditTranscript(TranscriptMappingResults tmr, List<AnnotationResults> arList, long experimentId, boolean all, String authToken, String email) {
        alignmentService.persistEditTranscript(tmr, arList, key, experimentId, distance, all, authToken, email);
    }

    public boolean authenticateUser(String user, String token) {
        return adminService.authenticateUser(user, token);
    }

}
