package cache.util;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 *
 * @author Manjunath Kustagi
 */
public class FileUpload {

    public static void main(String[] args) {
        try {
            System.setProperty("base.url", "");

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

            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials
                    = new UsernamePasswordCredentials(authUser, authPassword);
            provider.setCredentials(AuthScope.ANY, credentials);

            HttpClient client = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();

            String url = System.getProperty("base.url") + "FileUploadService";

            File f = new File(args[0]);
            HttpPost post = new HttpPost(url);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            String boundary = "---------------" + UUID.randomUUID().toString();
            builder.setBoundary(boundary);
            post.setHeader("Content-Type", ContentType.MULTIPART_FORM_DATA.getMimeType() + ";boundary=" + boundary);

            String name = f.getName();
            builder.addBinaryBody("file", f, ContentType.APPLICATION_OCTET_STREAM, name);

            boundary = "---------------" + UUID.randomUUID().toString();
            builder.setBoundary(boundary);
            
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
