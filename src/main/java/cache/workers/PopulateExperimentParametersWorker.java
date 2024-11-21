package cache.workers;

import com.caucho.hessian.client.HessianProxyFactory;
import java.net.MalformedURLException;
import javax.swing.SwingWorker;
import cache.dataimportes.formbeans.DataUploadBean;

/**
 *
 * @author Manjunath Kustagi
 */
public class PopulateExperimentParametersWorker extends SwingWorker {

    public static int OLD_ID = 1;
    public DataUploadBean outer;
    protected cache.interfaces.ParameterService parameterService;

    public PopulateExperimentParametersWorker(DataUploadBean outer) {
        this.outer = outer;
        try {
            String url = System.getProperty("base.url") + "ParameterService";
            HessianProxyFactory factory = new HessianProxyFactory();
            parameterService = (cache.interfaces.ParameterService) factory.create(cache.interfaces.ParameterService.class, url);
        } catch (MalformedURLException mue) {
        }
    }

    @Override
    protected Object doInBackground() throws Exception {
        outer = parameterService.getAdapterPruningParameters(OLD_ID);
        return outer;
    }

    @Override
    protected void done() {
        try {
//            outer.progressBar.setValue(0);
//            outer.statusLabel.setText("Idle");
        } catch (Exception e) {
        }
    }
}