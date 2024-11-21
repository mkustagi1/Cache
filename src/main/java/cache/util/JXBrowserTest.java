/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cache.util;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 *
 * @author mk2432
 */
public class JXBrowserTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Creating and running Chromium engine.
        Engine engine = Engine.newInstance(OFF_SCREEN);
        Browser browser = engine.newBrowser();

        // Creating UI component for rendering web content
        // loaded in the given Browser instance.
        BrowserView view = BrowserView.newInstance(browser);

        Button button = new Button("Print");
        button.setOnAction(event -> browser.mainFrame().ifPresent(Frame::print));

        BorderPane root = new BorderPane(view);
        TextField addressBar = new TextField("https://google.com");
        addressBar.setOnAction(event -> browser.navigation().loadUrl(addressBar.getText()));
        root.setBottom(button);
        Scene scene = new Scene(root, 200, 900);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();

        browser.navigation().loadUrl(addressBar.getText());

        // Close the engine when stage is about to close.
        primaryStage.setOnCloseRequest(event -> engine.close());
    }
}