package cache.workers;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import javax.swing.SwingWorker;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import cache.dataimportes.formbeans.DataUploadBean;
import cache.interfaces.AdminService.Status;
import java.io.File;

/**
 *
 * @author Manjunath Kustagi
 */
public class DataUploadWorker extends SwingWorker {

    private File f;
    DataUploadBean outer;
    protected HessianProxyFactory factory;
    protected cache.interfaces.ParameterService parameterService;
    protected cache.interfaces.DataUploadService dataUpload;
    protected cache.interfaces.AdminService adminService;

    public DataUploadWorker(DataUploadBean outer) {
        this.outer = outer;
        try {
            factory = new HessianProxyFactory();

            String url = System.getProperty("base.url") + "ParameterService";
            parameterService = (cache.interfaces.ParameterService) factory.create(cache.interfaces.ParameterService.class, url);

            url = System.getProperty("base.url") + "DataUploadService";
            dataUpload = (cache.interfaces.DataUploadService) factory.create(cache.interfaces.DataUploadService.class, url);

            url = System.getProperty("base.url") + "AdminService";
            adminService = (cache.interfaces.AdminService) factory.create(cache.interfaces.AdminService.class, url);
        } catch (MalformedURLException mue) {
        }
    }

    public DataUploadWorker(File f, DataUploadBean outer) {
        this.outer = outer;
        this.f = f;
        try {
            factory = new HessianProxyFactory();

            String url = System.getProperty("base.url") + "ParameterService";
            parameterService = (cache.interfaces.ParameterService) factory.create(cache.interfaces.ParameterService.class, url);

            url = System.getProperty("base.url") + "DataUploadService";
            dataUpload = (cache.interfaces.DataUploadService) factory.create(cache.interfaces.DataUploadService.class, url);

        } catch (MalformedURLException mue) {
        }
    }

    public void updateExperiment(long experimentId, DataUploadBean bean) {
        System.out.println("Updating experiment..");
        dataUpload.updateExperiment((int)experimentId, bean);
    }
    
    public boolean authenticateUser(String user, String token) {
        return adminService.authenticateUser(user, token);
    }

    public Status getHealthStatus() {
        return adminService.getServerStatus();
    }
    
    @Override
    protected Object doInBackground() throws Exception {

        int pid = parameterService.saveAdapterPruningParameters(PopulateExperimentParametersWorker.OLD_ID, outer);

        final long id = dataUpload.createExperiment(pid, outer);

        outer.setProgress(DataUploadBean.PROGRESS.STARTED);

        System.out.println("Experiment created");

        String url = System.getProperty("base.url") + "FileUploadService";

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);

        MultipartEntity entity = new MultipartEntity();
        entity.addPart("file", new FileBody(f));
        post.setEntity(entity);

        HttpResponse response = client.execute(post);

        dataUpload.uploadExperiment(id, f.getName());

        System.out.println("Data uploaded");

        outer.setProgress(DataUploadBean.PROGRESS.ALIGNING);

        return id;
    }

    @Override
    protected void done() {
        try {
        } catch (Exception e) {
        }
    }
}
