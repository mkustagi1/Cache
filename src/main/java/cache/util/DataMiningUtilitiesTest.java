package cache.util;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 *
 * @author Manjunath Kustagi
 */
public class DataMiningUtilitiesTest {

    public static void main(String[] args) {
        try {
            System.setProperty("base.url", "https://parclip.rockefeller.edu/server_3/AnvesanaWS//");

            final String authUser = "mkustagi";
            final String authPassword = "vxI2L9Qt";
            Authenticator.setDefault(new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            authUser, authPassword.toCharArray());
                }
            });

            System.setProperty("http.proxyUser", authUser);
            System.setProperty("http.proxyPassword", authPassword);

            String url = System.getProperty("base.url") + "FileUploadService";

            File f = new File(args[0]);
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(url);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            builder.addPart("file", new FileBody(f));

            post.setEntity(builder.build());

            HttpResponse response = client.execute(post);
            System.out.println("File: " + f.getName() + " was uploaded.");
        } catch (MalformedURLException ioe) {
            Logger.getLogger(DataMiningUtilitiesTest.class.getName()).log(Level.SEVERE, null, ioe);
        } catch (IOException ex) {
            Logger.getLogger(DataMiningUtilitiesTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
