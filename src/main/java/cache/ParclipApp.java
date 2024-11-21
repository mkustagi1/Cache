package cache;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author manjunath@c2b2.columbia.edu
 */
public class ParclipApp {

    public static ParclipFrameForm rootFrame;

    public static void main(String[] args) {
//        System.setProperty("apple.laf.useScreenMenuBar", "true");
//        System.setProperty("apple.awt.graphics.UseQuartz", "true");
//        System.setProperty("apple.awt.rendering", "speed");
//        System.setProperty("apple.awt.interpolation", "nearestneighbor");
//        System.setProperty("apple.awt.graphics.EnableQ2DX", "true");

        if (args.length > 0 && args[0] != null && args[0].startsWith("-base.url")) {
            String baseURL = args[0].split("=")[1];
            System.setProperty("base.url", baseURL.trim());
        } else {
            String baseURL = "http://35.172.221.199:8080/CacheWS/";
            System.setProperty("base.url", baseURL.trim());
        }
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

//        System.setOut(new PrintStream(new OutputStream() {
//
//            @Override
//            public void write(int arg0) throws IOException {
//            }
//        }));
//        System.setErr(new PrintStream(new OutputStream() {
//
//            @Override
//            public void write(int arg0) throws IOException {
//            }
//        }));
//        LogManager.getLogManager().reset();
//        Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
//        globalLogger.setLevel(java.util.logging.Level.OFF);
        try {
            boolean found = false;
            for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Aqua".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    found = true;
                    break;
                }
            }
            if (!found) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

        UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        rootFrame = new ParclipFrameForm();
        rootFrame.setSize(1200, 700);
        rootFrame.pack();
        rootFrame.setVisible(true);
        rootFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rootFrame.addDragListeners();
    }
}
