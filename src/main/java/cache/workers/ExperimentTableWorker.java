package cache.workers;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import cache.dataimportes.formbeans.DataUploadBean;
import cache.dataimportes.holders.ExperimentResult;

/**
 *
 * @author Manjunath Kustagi
 */
public class ExperimentTableWorker {

    DataUploadBean outer;
    protected HessianProxyFactory factory;
    protected cache.interfaces.DataUploadService dataUpload;

    public ExperimentTableWorker(DataUploadBean outer) {
        this.outer = outer;
        try {
            factory = new HessianProxyFactory();

            String url = System.getProperty("base.url") + "DataUploadService";
            dataUpload = (cache.interfaces.DataUploadService) factory.create(cache.interfaces.DataUploadService.class, url);


        } catch (MalformedURLException mue) {
        }
    }

    public ExperimentTableWorker() {
        try {
            factory = new HessianProxyFactory();

            String url = System.getProperty("base.url") + "DataUploadService";
            dataUpload = (cache.interfaces.DataUploadService) factory.create(cache.interfaces.DataUploadService.class, url);
        } catch (MalformedURLException mue) {
        }
    }

    public ExperimentResult getExperiment(int row, int page) {
        return dataUpload.getExperiment(row, page);
    }

    public ExperimentResult getExperiment(Long eid) {
        return dataUpload.getExperimentById(eid);
    }

    public long getExperimentCount() {
        return dataUpload.getExperimentCount();
    }

    public int getPageSize() {
        return dataUpload.getPageSize();
    }
}
