package cache;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;

/**
 * A JavaFX smoke test that can be used to check the embedding, rendering and other functionality
 * right in this project. This test is for internal usage only and should not be distributed.
 *
 * <p>Important: please feel free to modify it in order to check the different functionality, but
 * do not commit the changes to this test.
 */
public final class SmokeTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Creating and running Chromium engine.
        Engine engine = Engine.newInstance(EngineOptions.newBuilder(HARDWARE_ACCELERATED)
                        .licenseKey("OK6AX35CPFMQ4QSQP0114A8L972E053Y104S25JFV8S9R7FKTKAWTWTDNAEI5VAFZFJSSGZBGGFEEOUC74Z3538R2E86B22FQU8D2OY0HFNYTM3FQLBS6SNPQNU4RDPQXJY9GO19WEJ0VZN1Q")
                .build());
        Browser browser = engine.newBrowser();

        // Creating UI component for rendering web content
        // loaded in the given Browser instance.
        BrowserView view = BrowserView.newInstance(browser);

        Button button = new Button("Print");
        button.setOnAction(event -> browser.mainFrame().ifPresent(Frame::print));

        BorderPane root = new BorderPane(view);
        root.setBottom(button);
        Scene scene = new Scene(root, 1280, 900);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();

        browser.navigation().loadUrl("google.com");

        // Close the engine when stage is about to close.
        primaryStage.setOnCloseRequest(event -> engine.close());
    }
}
