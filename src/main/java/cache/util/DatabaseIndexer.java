package cache.util;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import cache.interfaces.AdminService;

/**
 *
 * @author Manjunath Kustagi
 */
public class DatabaseIndexer {

    public static void main(String[] args) {
        try {
            System.setProperty("base.url", "");
            String url = System.getProperty("base.url") + "AdminService";
            HessianProxyFactory factory = new HessianProxyFactory();
            AdminService adminService = (AdminService) factory.create(AdminService.class, url);
            for (long experimentId = 29l; experimentId <= 32l; experimentId++) {
                adminService.indexDatabaseReadsForExperiment(experimentId);
                adminService.indexDatabaseReadsForExperiment_20(experimentId);
                adminService.indexDatabaseReadsForExperiment_40(experimentId);
            }
//            for (long l = 1l; l < 60l; l++){
//            String sequence = "AAACTTTTTGGAAATTTCAATAACAATAACAATAATGGGCTCAAAAACAGCACAGAACCCATTTATGCTAAAGTTAATAAAAAGAAAACAGGACAAGTAGCTAGCCCTGAAGAACCCATTTATACTCAAGTTGCTAAAAA";
//            List<SearchResult> results = adminService.searchReads(1l, sequence, 100, SearchResult.SearchLength.NINETY_EIGHT, SearchResult.SearchType.READS, SearchResult.Strand.BOTH);
//                System.out.println("experiment: " + l + " matches: " + results.size());
//            }
            //            adminService.searchDatabase();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        }

    }
}
