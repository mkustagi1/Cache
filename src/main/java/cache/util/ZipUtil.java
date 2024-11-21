package cache.util;

/**
 *
 * @author Manjunath Kustagi
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import org.apache.commons.io.IOUtils;

public class ZipUtil {

    public static byte[] compressBytes(String data) throws UnsupportedEncodingException, IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
        DataOutputStream dataStream = new DataOutputStream(gzipOut);
        dataStream.writeBytes(data);
        dataStream.close();
        byte[] bytes = baos.toByteArray();
        return bytes;
//        byte[] input = data.getBytes("UTF-8");
//        Deflater df = new Deflater();
//        df.setInput(input);
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
//        df.finish();
//        byte[] buff = new byte[1024];
//        while (!df.finished()) {
//            int count = df.deflate(buff);
//            baos.write(buff, 0, count);
//        }
//        baos.close();
//        byte[] output = baos.toByteArray();
//
//        return output;
    }

    public static String extractBytes(byte[] input) throws UnsupportedEncodingException, IOException, DataFormatException {

        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        GZIPInputStream gis = new GZIPInputStream(bais);
//        BufferedReader br
//                = new BufferedReader(new InputStreamReader(gis));

        byte[] bytes = IOUtils.toByteArray(gis);

//        StringBuilder sb = new StringBuilder();
//        String line;
//        while((line = br.readLine()) != null) {
//            sb.append(line);
//        }
//        br.close();
        gis.close();
        bais.close();

        return new String(bytes, "UTF-8");
//        return sb.toString();
//        Inflater ifl = new Inflater();
//        ifl.setInput(input);
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
//        byte[] buff = new byte[1024];
//        while (!ifl.finished()) {
//            int count = ifl.inflate(buff);
//            baos.write(buff, 0, count);
//        }
//        baos.close();
//        byte[] output = baos.toByteArray();
//
//        return new String(output);
    }

    public static byte[] compressBytesList(List<Integer> data) throws UnsupportedEncodingException, IOException {

        StringBuilder sb = new StringBuilder();
        data.stream().map((s) -> {
            sb.append(s.toString());
            return s;
        }).forEach((_item) -> {
            sb.append("\t");
        });

        byte[] input = sb.toString().getBytes("UTF-8");
        Deflater df = new Deflater();
        df.setInput(input);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
        df.finish();
        byte[] buff = new byte[1024];
        while (!df.finished()) {
            int count = df.deflate(buff);
            baos.write(buff, 0, count);
        }
        baos.close();
        byte[] output = baos.toByteArray();

        return output;
    }

    public static List<Integer> extractBytesList(byte[] input) throws UnsupportedEncodingException, IOException, DataFormatException {
        Inflater ifl = new Inflater();
        ifl.setInput(input);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
        byte[] buff = new byte[1024];
        while (!ifl.finished()) {
            int count = ifl.inflate(buff);
            baos.write(buff, 0, count);
        }
        baos.close();
        byte[] output = baos.toByteArray();

        String dataString = new String(output);
        String[] tokens = dataString.split("\t");
        List<Integer> data = new ArrayList<>();
        for (String tok : tokens) {
            data.add(Integer.parseInt(tok));
        }
        return data;
    }

}
