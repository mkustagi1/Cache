package cache.util;

import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import cache.dataimportes.holders.AnnotationResults;
import cache.dataimportes.holders.Strand;
import cache.workers.AlignmentWorker;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.InjectCssCallback;
import com.teamdev.jxbrowser.browser.callback.ShowContextMenuCallback;
import com.teamdev.jxbrowser.browser.callback.input.MoveMouseCallback;
import com.teamdev.jxbrowser.browser.callback.input.PressMouseCallback;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.net.HttpHeader;
import com.teamdev.jxbrowser.net.HttpStatus;
import com.teamdev.jxbrowser.net.Scheme;
import com.teamdev.jxbrowser.net.UrlRequestJob;
import com.teamdev.jxbrowser.net.callback.InterceptUrlRequestCallback;
import com.teamdev.jxbrowser.ui.Point;
import com.teamdev.jxbrowser.ui.event.MouseMoved;
import com.teamdev.jxbrowser.ui.event.MousePressed;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;

public class JavaFXBrowserJXBrowser extends JPanel {

    private final JFXPanel jfxPanel = new JFXPanel();
    private final JFXPanel rcJfxPanel = new JFXPanel();
    private final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jfxPanel, rcJfxPanel);
    private BrowserPane browserPane;
    private BrowserPane rcBrowserPane;
    private String content;
    protected JavaFXBrowserChart chart;
    protected JavaFXMultimapperChart mmpChart;

    public JavaFXBrowserJXBrowser() {
        super();
        initComponents();
    }

    @Override
    public void repaint() {
        super.repaint();
        Platform.runLater(() -> {
            splitPane.setDividerLocation(0.3);
        });
    }

    public void setAlignmentChartLocation(final double xx) {
        browserPane.centerTo(xx);
        rcBrowserPane.centerTo(xx);
        this.repaint();
    }

    public void setChart(JavaFXBrowserChart c) {
        chart = c;
    }

    public void setMultimapperChart(JavaFXMultimapperChart c) {
        mmpChart = c;
    }

    private void initComponents() {
        createScene();
    }

    private void createScene() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                browserPane = new BrowserPane(false);
                Scene scene = new Scene(browserPane, 1100, 250, javafx.scene.paint.Color.web("#666970"));
                jfxPanel.setScene(scene);
                rcBrowserPane = new BrowserPane(true);
                browserPane.setOtherBrowser(rcBrowserPane);
                rcBrowserPane.setOtherBrowser(browserPane);
                scene = new Scene(rcBrowserPane, 1100, 250, javafx.scene.paint.Color.web("#666970"));
                rcJfxPanel.setScene(scene);
                splitPane.setTopComponent(jfxPanel);
                splitPane.setBottomComponent(rcJfxPanel);
                splitPane.setDividerLocation(0.3);
                splitPane.setOneTouchExpandable(true);
                this.setLayout(new BorderLayout());
                this.add(splitPane, BorderLayout.CENTER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String getContent() {
        String fc = browserPane.getContent();
        String rc = rcBrowserPane.getContent();
        String[] tokens = fc.split("</font></html>");
        return tokens[0] + rc;
    }

    int transcriptLength = 0;
    long transcriptId = 0;

    public void setTranscriptId(long tid) {
        transcriptId = tid;
    }

    public BrowserPane getRCBrowser() {
        return rcBrowserPane;
    }

    public BrowserPane getBrowser() {
        return browserPane;
    }

    public void loadContent(final String content, final String name) {
        try {
            this.content = content;

            Task task = new Task<Void>() {
                @Override
                public Void call() {
                    final String senseContent = getReverseComplement(content, Boolean.FALSE, name);
                    browserPane.loadContent(senseContent);
                    return null;
                }
            };

            new Thread(task).start();

            task = new Task<Void>() {
                @Override
                public Void call() {
                    final String rcContent = getReverseComplement(content, Boolean.TRUE, name);
                    rcBrowserPane.loadContent(rcContent);
                    return null;
                }
            };

            new Thread(task).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getReverseComplement(String html, Boolean rc, String name) {
        StringBuilder buffer = new StringBuilder(html.length());
        name = name.replace("-", "_");
        String[] tokens = html.split(name);
        if (!rc) {
            System.out.println("name, tokens.length, html.length: " + name + ", " + tokens.length + ", " + html.length());
            buffer.append(tokens[0]);
            buffer.append(name);
            String[] tokens2 = tokens[1].split("<br/>");
            transcriptLength = tokens2[0].length() + 75;
            buffer.append(tokens[1]);
            buffer.append("</font></html>");
        } else {
            buffer.append(tokens[0]);
            buffer.append(name);
            String[] tokens2 = tokens[2].split("<br/>");
            transcriptLength = tokens2[0].length() + 75;
            buffer.append(tokens[2]);
        }
        return buffer.toString();
    }

    public class BrowserPane extends StackPane {

        Browser b;
        BrowserView browserView;
        private final Line LV1;
        boolean rc;
        BrowserPane otherBrowser;
        String html = "";
        Map<Coordinates, String> annotations = new HashMap<>();

        final AlignmentWorker worker = new AlignmentWorker();

        public BrowserPane(boolean rc) {
            super();

            TempDirectory tmpDir = new TempDirectory();
            tmpDir.deleteOnExit();

            InterceptUrlRequestCallback interceptCallback = params -> {
                if (params.urlRequest().url().endsWith("?load-html")) {
                    byte[] bytes = html.getBytes();
                    UrlRequestJob job = params.newUrlRequestJob(
                            UrlRequestJob.Options
                                    .newBuilder(HttpStatus.OK)
                                    .addHttpHeader(HttpHeader.of("Content-Type", "text/html"))
                                    .build());
                    job.write(bytes);
                    job.complete();
                    return InterceptUrlRequestCallback.Response.intercept(job);
                }
                return InterceptUrlRequestCallback.Response.proceed();
            };

            Engine engine = Engine.newInstance(
                    EngineOptions.newBuilder(OFF_SCREEN)
                            .addScheme(Scheme.HTTP, interceptCallback)
                            .build());

            this.b = engine.newBrowser();
            this.browserView = BrowserView.newInstance(b);
//            b.resize(6144, 768);

            this.rc = rc;

            LV1 = new Line();
            LV1.setStrokeWidth(1);
            LV1.setOpacity(0.6);
            LV1.setStroke(javafx.scene.paint.Color.FORESTGREEN);

            initComponents();
            addListeners();
        }

        final void initComponents() {
            //apply the styles
            getStyleClass().add("browser");
            //add the web view to the scene

            Pane glassPane = new Pane();
            glassPane.getChildren().add(LV1);
            glassPane.minWidthProperty().bind(browserView.widthProperty());
            glassPane.minHeightProperty().bind(browserView.heightProperty());
            glassPane.setMouseTransparent(true);
            LV1.endYProperty().bind(browserView.heightProperty());
            browserView.setVisible(true);
            getChildren().add(browserView);
            getChildren().add(glassPane);

            b.navigation().on(FrameLoadFinished.class, event -> {
                System.out.println("Document loaded");
                System.out.println("Size: " + b.size().toString());
                refreshAnnotations();
            });

            browserView.getStylesheets().add("cache/util/webView.css");

            b.set(InjectCssCallback.class, params
                    -> InjectCssCallback.Response.inject("::selection {\n"
                            + "    color: white; \n"
                            + "    background: lightblue;\n"
                            + "}"
                            + ""
                            + "span.highlight {\n"
                            + "    opacity: 0.5;\n"
                            + "}"
                            + ""
                            + "font {\n"
                            + "    overflow-x: scroll \n"
                            + "}"
                            + "body {\n"
                            + "    overflow-x: scroll \n"
                            + "}"));

            b.settings().hideScrollbars();
        }

        public Browser getBrowser() {
            return b;
        }

        public void setOtherBrowser(BrowserPane ob) {
            this.otherBrowser = ob;
        }

        private void addListeners() {
            Set<Node> nodes = browserView.lookupAll(".scroll-bar");
            nodes.stream().filter((node) -> (node instanceof ScrollBar)).map((node) -> (ScrollBar) node).filter((scroll) -> (scroll.getOrientation() == Orientation.HORIZONTAL)).forEach((scroll) -> {
                scroll.valueProperty().addListener((ObservableValue<? extends Number> ov, final Number old_val, final Number new_val) -> {
                    Platform.runLater(() -> {
                        double diff = new_val.doubleValue() - old_val.doubleValue();
                        LV1.setStartX(new_val.doubleValue());
                        LV1.setEndX(new_val.doubleValue());
                    });
                });
            });

            EventHandler<MouseEvent> mouseHandler = (MouseEvent mouseEvent) -> {
                if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                    int position = getCaretPositionX();
                    System.out.println("x, transcriptLength: " + position + ", " + transcriptLength);
                    if (position < transcriptLength) {
                        chart.setLineLocation(position - 75);
                        mmpChart.setLineLocation(position - 75);
                    }
                } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                    double x1 = mouseEvent.getX();
                    LV1.setStartX(x1);
                    LV1.setEndX(x1);
                } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                    Platform.runLater(() -> {
                        setAlignmentChartLocation(mouseEvent.getScreenX());
                    });
                }
            };

            browserView.setOnMouseClicked(mouseHandler);
            browserView.setOnMouseMoved(mouseHandler);

            b.set(PressMouseCallback.class, params -> {
                MousePressed event = params.event();
                if (event.clickCount() == 1) {
                    int position = getCaretPositionX();
                    System.out.println("x, transcriptLength: " + position + ", " + transcriptLength);
                    if (position < transcriptLength) {
                        chart.setLineLocation(position - 75);
                        mmpChart.setLineLocation(position - 75);
                        chart.setLineLocation(position - 75);
                        mmpChart.setLineLocation(position - 75);
                    }
                }
                return PressMouseCallback.Response.proceed();
            });

            b.set(MoveMouseCallback.class, params -> {
                MouseMoved event = params.event();
                double x1 = event.location().x();
                LV1.setStartX(x1);
                LV1.setEndX(x1);
                return MoveMouseCallback.Response.proceed();
            });

            final ContextMenu cm = new ContextMenu();
            MenuItem cmItem1 = new MenuItem("Annotate");
            MenuItem cmItem2 = new MenuItem("Clear Annotation");
            cm.getItems().add(cmItem1);
            cm.getItems().add(cmItem2);

            b.set(ShowContextMenuCallback.class, (params, tell)
                    -> Platform.runLater(() -> {
                        cm.setAutoHide(true);
                        cm.setOnHidden(event -> {
                            if (!tell.isClosed()) {
                                tell.close();
                            }
                        });
                        browserView.setOnMousePressed(event -> {
                            if (cm.isShowing()) {
                                cm.hide();
                            }
                        });
                        Point location = params.location();
                        javafx.geometry.Point2D locationOnScreen = browserView.localToScreen(location.x(), location.y());
                        cm.show(browserView, locationOnScreen.getX(), locationOnScreen.getY());
                    }));

            cmItem1.setOnAction((javafx.event.ActionEvent actionEvent) -> {
                handleAnnotations();
            });

            cmItem2.setOnAction((javafx.event.ActionEvent actionEvent) -> {
                clearSelectedAnnotation();
            });
        }

        public void refreshAnnotations() {
            Runnable runnable = () -> {
                clearAnnotationHighlights();
                annotations.clear();
                List<AnnotationResults> arList = worker.getAnnotationsForTranscript(transcriptId);
                if (arList != null) {
                    Collections.sort(arList);
                    arList.stream().forEach((ar) -> {
                        int frequency = Collections.frequency(arList, ar);
                        boolean dcm = false;
                        Coordinates c = new Coordinates(ar.startCoordinate, ar.stopCoordinate);
                        annotations.put(c, ar.annotation);
                        if (ar.annotation.contains("__disease_causing_mutation")) {
                            ar.annotation = ar.annotation.split("__")[0];
                            dcm = true;
                        }
                        String tip = ar.annotation;
                        tip = tip.trim();
                        tip = tip.replaceAll("\"", "");
                        if (!(ar.variant.equals("_") || ar.variant.equals("N"))) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                String variant = "";
                                for (AnnotationResults _ar : dups) {
                                    if (_ar.predicted) {
                                        variant += "_" + _ar.variant + ", ";
                                    } else {
                                        variant += _ar.variant + ", ";
                                    }
                                }
                                variant = variant.substring(0, variant.length() - 2);
                                tip += ": " + variant;
                            } else {
                                tip += ": " + ar.variant;
                            }
                        }
                        if (dcm) {
                            tip += " : Disease Causing Mutation";
                        }
                        if (ar.annotation.equals("Start Codon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            }
                        } else if (ar.annotation.equals("Stop Codon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            }
                        } else if (ar.annotation.equals("PolyA Signal")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            }
                        } else if (ar.annotation.equals("Cleavage Site")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            }
                        } else if (ar.annotation.equals("Alternative Cassette Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            }
                        } else if (ar.annotation.equals("Alternative 5’ Splice Site Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            }
                        } else if (ar.annotation.equals("Alternative 3’ Splice Site Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            }
                        } else if (ar.annotation.equals("1st Shared Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            }
                        } else if (ar.annotation.equals("Last Shared Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            }
                        } else if (ar.annotation.equals("Mutually Exclusive Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            }
                        } else if (ar.annotation.equals("Retained Intron 3’ UTR")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            }
                        } else if (ar.annotation.equals("Retained Intron")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            }
                        } else if (ar.annotation.equals("Transcript Specific Priority Region")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            }
                        } else if (ar.annotation.equals("Start Site")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            }
                        } else if (ar.annotation.equals("EPD_NEW")) {
                            if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 86, "#00BFFF", tip);
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 86, (int) ar.stopCoordinate + 75, "#ADD8E6", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 124, "#ADD8E6", tip);
                                selectAndHighlightRange((int) ar.startCoordinate + 124, (int) ar.stopCoordinate + 75, "#00BFFF", tip);
                            }
                        } else if (ar.annotation.equals("Substitution")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            }
                        } else if (ar.annotation.equals("Transposition")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            }
                        } else if (ar.annotation.equals("Deletion")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            }
                        } else if (ar.annotation.equals("Insertion")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Ambion_SilencerSelect")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_ON-TARGETplus")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_siGENOME")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip);
                            }
                        } else if (ar.annotation.startsWith("Affymetrix:")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip);
                            }
                        } else if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else if (dcm) {
                            selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        }
                    });
                }
                cleanFontTags();
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void refreshAnnotationsWithoutClear() {
            Runnable runnable = () -> {
                List<AnnotationResults> arList = worker.getAnnotationsForTranscript(transcriptId);
                if (arList != null) {
                    Collections.sort(arList);
                    arList.stream().forEach((ar) -> {
                        int frequency = Collections.frequency(arList, ar);
                        boolean dcm = false;
                        Coordinates c = new Coordinates(ar.startCoordinate, ar.stopCoordinate);
                        annotations.put(c, ar.annotation);
                        if (ar.annotation.contains("__disease_causing_mutation")) {
                            ar.annotation = ar.annotation.split("__")[0];
                            dcm = true;
                        }
                        String tip = ar.annotation;
                        tip = tip.trim();
                        tip = tip.replaceAll("\"", "");
                        if (!(ar.variant.equals("_") || ar.variant.equals("N"))) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                String variant = "";
                                for (AnnotationResults _ar : dups) {
                                    if (_ar.predicted) {
                                        variant += "_" + _ar.variant + ", ";
                                    } else {
                                        variant += _ar.variant + ", ";
                                    }
                                }
                                variant = variant.substring(0, variant.length() - 2);
                                tip += ": " + variant;
                            } else {
                                tip += ": " + ar.variant;
                            }
                        }
                        if (dcm) {
                            tip += " : Disease Causing Mutation";
                        }
                        if (ar.annotation.equals("Start Codon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            }
                        } else if (ar.annotation.equals("Stop Codon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            }
                        } else if (ar.annotation.equals("PolyA Signal")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            }
                        } else if (ar.annotation.equals("Cleavage Site")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            }
                        } else if (ar.annotation.equals("Alternative Cassette Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip);
                            }
                        } else if (ar.annotation.equals("Alternative 5’ Splice Site Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip);
                            }
                        } else if (ar.annotation.equals("Alternative 3’ Splice Site Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip);
                            }
                        } else if (ar.annotation.equals("1st Shared Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip);
                            }
                        } else if (ar.annotation.equals("Last Shared Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip);
                            }
                        } else if (ar.annotation.equals("Mutually Exclusive Exon")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip);
                            }
                        } else if (ar.annotation.equals("Retained Intron 3’ UTR")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip);
                            }
                        } else if (ar.annotation.equals("Retained Intron")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip);
                            }
                        } else if (ar.annotation.equals("Transcript Specific Priority Region")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgb(243, 218, 190)", tip);
                            }
                        } else if (ar.annotation.equals("Start Site")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            }
                        } else if (ar.annotation.equals("EPD_NEW")) {
                            if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 86, "#00BFFF", tip);
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 86, (int) ar.stopCoordinate + 75, "#ADD8E6", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 124, "#ADD8E6", tip);
                                selectAndHighlightRange((int) ar.startCoordinate + 124, (int) ar.stopCoordinate + 75, "#00BFFF", tip);
                            }
                        } else if (ar.annotation.equals("Substitution")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            }
                        } else if (ar.annotation.equals("Transposition")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            }
                        } else if (ar.annotation.equals("Deletion")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            }
                        } else if (ar.annotation.equals("Insertion")) {
                            if (dcm) {
                                selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Ambion_SilencerSelect")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_ON-TARGETplus")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_siGENOME")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip);
                            }
                        } else if (ar.annotation.startsWith("Affymetrix:")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip);
                            }
                        } else if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else if (dcm) {
                            selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        }
                    });
                }
                cleanFontTags();
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void handleAnnotations() {
            loadSelection();
            loadSelectionCoordinates();

            String variant = "_";
            String[] annotationTypes = new String[]{"Start Codon", "Stop Codon", "PolyA Signal",
                "Cleavage Site", "Start Site", "Substitution", "Transposition", "Insertion", "Deletion",
                "Alternative Cassette Exon", "Alternative 5’ Splice Site Exon",
                "Alternative 3’ Splice Site Exon", "1st Shared Exon", "Last Shared Exon", "Mutually Exclusive Exon",
                "Retained Intron 3’ UTR", "Retained Intron", "Transcript Specific Priority Region", "Freeform annotation"};

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Select Annotation Type");
            dialog.setHeaderText("Available Annotations");

            dialog.initModality(Modality.APPLICATION_MODAL);

            ButtonType okButtonType = new ButtonType("OK", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            CheckBox diseaseMutation = new CheckBox("Disease causing mutation");
            diseaseMutation.setIndeterminate(false);

            ToggleGroup group = new ToggleGroup();
            RadioButton button0 = new RadioButton(annotationTypes[0]);
            RadioButton button1 = new RadioButton(annotationTypes[1]);
            RadioButton button2 = new RadioButton(annotationTypes[2]);
            RadioButton button3 = new RadioButton(annotationTypes[3]);
            RadioButton button4 = new RadioButton(annotationTypes[4]);
            RadioButton button5 = new RadioButton(annotationTypes[5]);
            RadioButton button6 = new RadioButton(annotationTypes[6]);
            RadioButton button7 = new RadioButton(annotationTypes[7]);
            RadioButton button8 = new RadioButton(annotationTypes[8]);
            RadioButton button9 = new RadioButton(annotationTypes[9]);
            RadioButton button10 = new RadioButton(annotationTypes[10]);
            RadioButton button11 = new RadioButton(annotationTypes[11]);
            RadioButton button12 = new RadioButton(annotationTypes[12]);
            RadioButton button13 = new RadioButton(annotationTypes[13]);
            RadioButton button14 = new RadioButton(annotationTypes[14]);
            RadioButton button15 = new RadioButton(annotationTypes[15]);
            RadioButton button16 = new RadioButton(annotationTypes[16]);
            RadioButton button17 = new RadioButton(annotationTypes[17]);
            RadioButton button18 = new RadioButton(annotationTypes[18]);

            button0.setToggleGroup(group);
            button0.setSelected(true);
            button1.setToggleGroup(group);
            button2.setToggleGroup(group);
            button3.setToggleGroup(group);
            button4.setToggleGroup(group);
            button5.setToggleGroup(group);
            button6.setToggleGroup(group);
            button7.setToggleGroup(group);
            button8.setToggleGroup(group);
            button9.setToggleGroup(group);
            button10.setToggleGroup(group);
            button11.setToggleGroup(group);
            button12.setToggleGroup(group);
            button13.setToggleGroup(group);
            button14.setToggleGroup(group);
            button15.setToggleGroup(group);
            button16.setToggleGroup(group);
            button17.setToggleGroup(group);
            button18.setToggleGroup(group);

            grid.add(diseaseMutation, 0, 0);
            grid.add(button0, 0, 1);
            grid.add(button1, 0, 2);
            grid.add(button2, 0, 3);
            grid.add(button3, 0, 4);
            grid.add(button4, 0, 5);
            grid.add(button5, 0, 6);
            grid.add(button6, 0, 7);
            grid.add(button7, 0, 8);
            grid.add(button8, 0, 9);
            grid.add(button9, 0, 10);
            grid.add(button10, 0, 11);
            grid.add(button11, 0, 12);
            grid.add(button12, 0, 13);
            grid.add(button13, 0, 14);
            grid.add(button14, 0, 15);
            grid.add(button15, 0, 16);
            grid.add(button16, 0, 17);
            grid.add(button17, 0, 18);
            grid.add(button18, 0, 19);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    return getSelectedButtonText(group);
                }
                return null;
            });

            dialog.getDialogPane().setContent(grid);

            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            stage.toFront();

            Optional<String> result = dialog.showAndWait();

            String at = null;
            if (result.isPresent()) {
                at = result.get();
                System.out.println("Your choice: " + at);
            }

            if (at == null) {
                return;
            }

            if (at.equals("Substitution")) {
                List<String> choices = new ArrayList<>();
                choices.add("A");
                choices.add("C");
                choices.add("T");
                choices.add("G");

                ChoiceDialog<String> vDialog = new ChoiceDialog<>("A", choices);
                vDialog.setTitle("Substitution");
                vDialog.setHeaderText("Variants");
                vDialog.setContentText("Select Variant:");

                vDialog.initModality(Modality.APPLICATION_MODAL);
                Stage vStage = (Stage) vDialog.getDialogPane().getScene().getWindow();
                vStage.setAlwaysOnTop(true);
                vStage.toFront();

                Optional<String> vResult = vDialog.showAndWait();

                if (vResult.isPresent()) {
                    variant = vResult.get();
                    System.out.println("Your choice: " + vResult.get());
                } else {
                    return;
                }
                if (variant == null) {
                    return;
                }
            }
            if (at.equals("Insertion")) {

                TextInputDialog iDialog = new TextInputDialog("Inserted Nucleotides..");
                iDialog.setTitle("Insertion");
                iDialog.setHeaderText("Insertion");
                iDialog.setContentText("Specify Insertion:");
                Stage iStage = (Stage) iDialog.getDialogPane().getScene().getWindow();
                iStage.setAlwaysOnTop(true);
                iStage.toFront();

                iDialog.initModality(Modality.APPLICATION_MODAL);

                Optional<String> iResult = iDialog.showAndWait();
                if (iResult.isPresent()) {
                    variant = iResult.get();
                    System.out.println("Your choice: " + iResult.get());
                } else {
                    return;
                }
                if (variant == null) {
                    return;
                }
            }
            if (at.equals("Freeform annotation")) {
                TextInputDialog iDialog = new TextInputDialog("Your annotation here..");
                iDialog.setTitle("Freeform annotation");
                iDialog.setHeaderText("Freeform annotation");
                iDialog.setContentText("Specify annotation:");
                Stage iStage = (Stage) iDialog.getDialogPane().getScene().getWindow();
                iStage.setAlwaysOnTop(true);
                iStage.toFront();

                iDialog.initModality(Modality.APPLICATION_MODAL);

                Optional<String> iResult = iDialog.showAndWait();

                if (iResult.isPresent()) {
                    at = iResult.get();
                    System.out.println("Your : " + iResult.get());
                } else {
                    return;
                }
                if (at == null) {
                    return;
                }
                at = at.trim();
                at = at.replaceAll("\"", "");
            }
            String selection1 = getSelection();
            Coordinates c = getSelectionCoordinate();
            if (c.x < 0 || c.y < 0 || c.x > transcriptLength || c.y > transcriptLength) {
                return;
            }
            if ((at.equals("Insertion") || at.equals("Transposition")) && (c.y - c.x) != 2) {
                return;
            }
            if (at.equals("Substitution") && (c.y - c.x) != 1) {
                return;
            }
            switch (at) {
                case "Start Codon":
                    highlightCurrentSelection("#ff00ff", at);
                    break;
                case "Stop Codon":
                    highlightCurrentSelection("#e6e6fa", at);
                    break;
                case "PolyA Signal":
                    highlightCurrentSelection("#00ff00", at);
                    break;
                case "Cleavage Site":
                    highlightCurrentSelection("#e9967a", at);
                    break;
                case "Start Site":
                    highlightCurrentSelection("#F4D03F", at);
                    break;
                case "Substitution":
                    highlightCurrentSelection("#a3c2db", at);
                    break;
                case "Transposition":
                    highlightCurrentSelection("#ffcccc", at);
                    break;
                case "Deletion":
                    highlightCurrentSelection("#ff0000", at);
                    break;
                case "Insertion":
                    highlightCurrentSelection("#ff7f00", at);
                    break;
                case "Alternative Cassette Exon":
                    highlightCurrentSelection("#b4efb8", at);
                    break;
                case "Alternative 5’ Splice Site Exon":
                    highlightCurrentSelection("#c9f4cc", at);
                    break;
                case "Alternative 3’ Splice Site Exon":
                    highlightCurrentSelection("#def8e0", at);
                    break;
                case "1st Shared Exon":
                    highlightCurrentSelection("#9feba4", at);
                    break;
                case "Last Shared Exon":
                    highlightCurrentSelection("#8ae790", at);
                    break;
                case "Mutually Exclusive Exon":
                    highlightCurrentSelection("rgba(165,112,255,0.5)", at);
                    break;
                case "Retained Intron 3’ UTR":
                    highlightCurrentSelection("#D2B4DE", at);
                    break;
                case "Retained Intron":
                    highlightCurrentSelection("rgba(153,25,145,0.5)", at);
                    break;
                case "Transcript Specific Priority Region":
                    highlightCurrentSelection("rgb(243, 218, 190)", at);
                    break;
                default:
                    highlightCurrentSelection("#d3d3d3", at);
                    break;
            }
            List<AnnotationResults> arList = new ArrayList<>();
            Strand strand = (!rc) ? Strand.FORWARD : Strand.REVERSECOMPLEMENT;
            if (diseaseMutation.isSelected()) {
                at += "__disease_causing_mutation";
            }
            AnnotationResults ar = new AnnotationResults(UUID.randomUUID(), transcriptId, selection1, at, variant, (int) c.x, (int) c.y, strand, false);
            arList.add(ar);
            worker.createOrDeleteAnnotation(arList, true);
            refreshAnnotations();
            otherBrowser.refreshAnnotations();
        }

        public void zoomIn() {
            Runnable runnable = () -> {
                b.zoom().in();
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void zoomOut() {
            Runnable runnable = () -> {
                b.zoom().out();
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void centerTo(final double xx) {
            Runnable runnable = () -> {

                Double maxScroll = b.mainFrame().get().executeJavaScript("Math.max( document.body.scrollWidth, "
                        + "document.body.offsetWidth, document.documentElement.clientWidth, "
                        + "document.documentElement.scrollWidth, document.documentElement.offsetWidth); ");

                Double maxScrollY = b.mainFrame().get().executeJavaScript("Math.max( document.body.scrollHeight, "
                        + "document.body.offsetHeight, document.documentElement.clientHeight, "
                        + "document.documentElement.scrollHeight, document.documentElement.offsetHeight); ");

                double bvw = browserView.widthProperty().get();
                double bvh = browserView.heightProperty().get();

                double fraction = (xx) / transcriptLength;

                Double verticalValue = b.mainFrame().get().executeJavaScript("window.pageYOffset");
                Double horizontalValue = b.mainFrame().get().executeJavaScript("window.pageXOffset");

                b.mainFrame().get().executeJavaScript("window.scrollTo(" + fraction * maxScroll + ", " + verticalValue + ");");
                double x1 = fraction * maxScroll;
                LV1.setStartX(x1);
                LV1.setEndX(x1);
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        ChangeListener ocl = null;

        public void clearAnnotationHighlights() {
            Platform.runLater(() -> {
                String script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "sel.collapse(document.body, 0);"
                        + "var spans = document.getElementsByTagName(\"SPAN\"), i;"
                        + "for( i=0; i<spans.length; i++) {"
                        + "if (spans[i].hasAttribute(\"title\") && !spans[i].hasAttribute(\"class\")) {"
                        + "spans[i].style.border = \"none\";"
                        + "spans[i].style.backgroundColor = \"transparent\";"
                        + "spans[i].style.fontWeight = \"normal\";"
                        + "spans[i].style.textDecoration = \"none\";"
                        + "spans[i].title = \"\";"
                        //                        + "var container = spans[i].parentNode;\n"
                        //                        + "     var text = spans[i].innerText;\n"
                        //                        + "     container.innerText += text;\n"
                        //                        + "     container.removeChild(spans[i]);"
                        //                    + "while(spans[i].firstChild)\n"
                        //                    + "{\n"
                        //                    + "spans[i].parentElement.insertBefore(spans[i].firstChild, spans[i]);\n"
                        //                    + "}\n"
                        //                    + "spans[i].parentElement.removeChild(spans[i]);"
                        + "}"
                        + "}"
                        + "sel.collapseToEnd();"
                        + "document.designMode = \"off\";";

                b.mainFrame().get().executeJavaScript(script);
            });
        }

        public void cleanFontTags() {
            Runnable runnable = () -> {
                String script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "sel.collapse(document.body, 0);"
                        + "var font = document.getElementsByTagName(\"font\")[0], i;"
                        + "font.style.textDecoration = \"none\";"
                        + "font.border = \"none\";"
                        + "font.backgroundColor = \"transparent\";"
                        + "font.style.fontWeight = \"normal\";"
                        + "font.style.fontStyle = \"normal\";"
                        + "font.title = \"\";"
                        + "sel.collapseToEnd();"
                        + "document.designMode = \"off\";";

                b.mainFrame().get().executeJavaScript(script);
            };
            Platform.runLater(runnable);
        }

        public void clearSearchHighlights(final JPanel source) {
            Runnable runnable = () -> {
                String script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "sel.collapse(document.body, 0);"
                        + "var spans = document.getElementsByTagName(\"SPAN\"), i;"
                        + "for( i=0; i<spans.length; i++) {"
                        + "if(spans[i].style.backgroundColor == \"rgb(224, 224, 6)\") {"
                        + "spans[i].style.backgroundColor = \"transparent\";"
                        + "}"
                        + "}"
                        + "sel.collapseToEnd();"
                        + "document.designMode = \"off\";";

                b.mainFrame().get().executeJavaScript(script);
                CursorToolkitOne.stopWaitCursor(source);
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void highlightText(final String text, final String hexColor, final JPanel source) {
            Runnable runnable = () -> {

                String script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "sel.collapse(document.body, 0);"
                        + "while (window.find(\"" + text + "\")) {"
                        + "document.execCommand(\"HiliteColor\", false, \"" + hexColor + "\");"
                        + "sel.collapseToEnd();"
                        + "}"
                        + "document.designMode = \"off\";";

                b.mainFrame().get().executeJavaScript(script);
                CursorToolkitOne.stopWaitCursor(source);
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void clearSpanAroundSelection() {
            Platform.runLater(() -> {
                String script
                        = "document.designMode = \"on\";"
                        + "var selection = window.getSelection();\n"
                        + "for (var i = 0; i < selection.rangeCount; i++) {"
                        + "var range = selection.getRangeAt(i);\n"
                        + "var span = range.startContainer.parentNode;"
                        + "if (span.hasAttribute(\"title\") && span.nodeName == \"span\") {"
                        + "var container = span.parentNode;\n"
                        + "     var text = span.innerText;\n"
                        + "     container.innerText += text;\n"
                        + "     container.removeChild(span);"
                        //                    + "while(span.firstChild)\n"
                        //                    + "{\n"
                        //                    + "span.parentElement.insertBefore(span.firstChild, span);\n"
                        //                    + "}\n"
                        //                    + "span.parentElement.removeChild(span);"
                        + "}"
                        + "}"
                        + "selection.collapseToEnd();"
                        + "document.designMode = \"off\";";
                b.mainFrame().get().executeJavaScript(script);
            });
        }

        public void clearSelectedAnnotation() {
            loadSelection();
            loadSelectionCoordinates();

            Platform.runLater(() -> {
                String selection1 = getSelection();
                Coordinates c = getSelectionCoordinate();
                System.out.println("selection: " + c.toString());
                Strand strand = (!rc) ? Strand.FORWARD : Strand.REVERSECOMPLEMENT;
                System.out.println("coordinate: " + c.toString());
                System.out.println("annotation: " + annotations.get(c));
                if (annotations.get(c) != null) {
                    AnnotationResults ar = new AnnotationResults(UUID.randomUUID(), transcriptId, selection1, annotations.get(c), "", (int) c.x, (int) c.y, strand, false);
                    List<AnnotationResults> list = new ArrayList<>();
                    list.add(ar);
                    worker.createOrDeleteAnnotation(list, false);
                    refreshAnnotations();
                    otherBrowser.refreshAnnotations();
                    refreshAnnotations();
                    otherBrowser.refreshAnnotations();
//                    clearSpanAroundSelection();
//                    System.out.println("c.x, c.y: " + c.x + ", " + c.y);
//                    otherBrowser.selectRangeAndClear((int) c.x + 75, (int) c.y + 75);
                }
            });
        }

        public void selectAndHighlightRange(final int start, final int stop, final String hexColor, final String tip) {
            if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim().equals("")) {
                Platform.runLater(() -> {
                    String script = "function getTextNodesIn(node) {\n"
                            + "    var textNodes = [];\n"
                            + "    if (node.nodeType == 3) {\n"
                            + "        textNodes.push(node);\n"
                            + "    } else {\n"
                            + "        var children = node.childNodes;\n"
                            + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                            + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                            + "        }\n"
                            + "    }\n"
                            + "    return textNodes;\n"
                            + "}\n"
                            + "\n"
                            + "function setSelectionRange(el, start, end) {\n"
                            + "    if (document.createRange && window.getSelection) {\n"
                            + "        var range = document.createRange();\n"
                            + "        range.selectNodeContents(el);\n"
                            + "        var textNodes = getTextNodesIn(el);\n"
                            + "        var foundStart = false;\n"
                            + "        var charCount = 0, endCharCount;\n"
                            + "\n"
                            + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                            + "            endCharCount = charCount + textNode.length;\n"
                            + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                            + "                range.setStart(textNode, start - charCount);\n"
                            + "                foundStart = true;\n"
                            + "            }\n"
                            + "            if (foundStart && end <= endCharCount) {\n"
                            + "                range.setEnd(textNode, end - charCount);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "            charCount = endCharCount;\n"
                            + "        }\n"
                            + "\n"
                            + "        var sel = window.getSelection();\n"
                            + "        sel.removeAllRanges();\n"
                            + "        sel.addRange(range);\n"
                            + "    } else if (document.selection && document.body.createTextRange) {\n"
                            + "        var textRange = document.body.createTextRange();\n"
                            + "        textRange.moveToElementText(el);\n"
                            + "        textRange.collapse(true);\n"
                            + "        textRange.moveEnd(\"character\", end);\n"
                            + "        textRange.moveStart(\"character\", start);\n"
                            + "        textRange.select();\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "function makeEditableAndHighlight(color, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    document.execCommand('styleWithCSS', false, true);"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                            + "        document.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;\n"
                            + "    sel.collapseToEnd();\n"
                            + "    document.designMode = \"off\";\n"
                            + "}\n"
                            + "\n"
                            + "function highlightAndToolTip(color, ttext) {\n"
                            + "    var range, sel;\n"
                            + "    if (window.getSelection) {\n"
                            + "        // IE9 and non-IE\n"
                            + "        try {\n"
                            + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                            + "                makeEditableAndHighlight(color, ttext);\n"
                            + "            }\n"
                            + "        } catch (ex) {\n"
                            + "            makeEditableAndHighlight(color, ttext);\n"
                            + "        }\n"
                            + "    } else if (document.selection && document.selection.createRange) {\n"
                            + "        // IE <= 8 case\n"
                            + "        range = document.selection.createRange();\n"
                            + "        range.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "}\n"
                            + "\n"
                            + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                            + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\");\n";
                    b.mainFrame().get().executeJavaScript(script);
                });
            }
        }

        public void selectAndHighlightItalicizeRange(final int start, final int stop, final String hexColor, final String tip) {
            if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim().equals("")) {
                Platform.runLater(() -> {
                    String script = "function getTextNodesIn(node) {\n"
                            + "    var textNodes = [];\n"
                            + "    if (node.nodeType == 3) {\n"
                            + "        textNodes.push(node);\n"
                            + "    } else {\n"
                            + "        var children = node.childNodes;\n"
                            + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                            + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                            + "        }\n"
                            + "    }\n"
                            + "    return textNodes;\n"
                            + "}\n"
                            + "\n"
                            + "function setSelectionRange(el, start, end) {\n"
                            + "    if (document.createRange && window.getSelection) {\n"
                            + "        var range = document.createRange();\n"
                            + "        range.selectNodeContents(el);\n"
                            + "        var textNodes = getTextNodesIn(el);\n"
                            + "        var foundStart = false;\n"
                            + "        var charCount = 0, endCharCount;\n"
                            + "\n"
                            + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                            + "            endCharCount = charCount + textNode.length;\n"
                            + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                            + "                range.setStart(textNode, start - charCount);\n"
                            + "                foundStart = true;\n"
                            + "            }\n"
                            + "            if (foundStart && end <= endCharCount) {\n"
                            + "                range.setEnd(textNode, end - charCount);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "            charCount = endCharCount;\n"
                            + "        }\n"
                            + "\n"
                            + "        var sel = window.getSelection();\n"
                            + "        sel.removeAllRanges();\n"
                            + "        sel.addRange(range);\n"
                            + "    } else if (document.selection && document.body.createTextRange) {\n"
                            + "        var textRange = document.body.createTextRange();\n"
                            + "        textRange.moveToElementText(el);\n"
                            + "        textRange.collapse(true);\n"
                            + "        textRange.moveEnd(\"character\", end);\n"
                            + "        textRange.moveStart(\"character\", start);\n"
                            + "        textRange.select();\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "function makeEditableAndHighlight(colour, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    document.execCommand('styleWithCSS', false, true);"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                            + "        document.execCommand(\"BackColor\", false, colour);\n"
                            + "    }\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.fontStyle = \"italic\";"
                            + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;"
                            + "    sel.collapseToEnd();"
                            + "    document.designMode = \"off\";\n"
                            + "}\n"
                            + "\n"
                            + "function highlightAndToolTip(color, ttext) {\n"
                            + "    var range, sel;\n"
                            + "    if (window.getSelection) {\n"
                            + "        // IE9 and non-IE\n"
                            + "        try {\n"
                            + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                            + "                makeEditableAndHighlight(color, ttext);\n"
                            + "            }\n"
                            + "        } catch (ex) {\n"
                            + "            makeEditableAndHighlight(color, ttext)\n"
                            + "        }\n"
                            + "    } else if (document.selection && document.selection.createRange) {\n"
                            + "        // IE <= 8 case\n"
                            + "        range = document.selection.createRange();\n"
                            + "        range.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                            + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\");";

                    b.mainFrame().get().executeJavaScript(script);
                });
            }
        }

        public void selectAndHighlightUnderlineRange(final int start, final int stop, final String hexColor, final String tip) {
            if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim().equals("")) {
                Platform.runLater(() -> {
                    String script = "function getTextNodesIn(node) {\n"
                            + "    var textNodes = [];\n"
                            + "    if (node.nodeType == 3) {\n"
                            + "        textNodes.push(node);\n"
                            + "    } else {\n"
                            + "        var children = node.childNodes;\n"
                            + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                            + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                            + "        }\n"
                            + "    }\n"
                            + "    return textNodes;\n"
                            + "}\n"
                            + "\n"
                            + "function setSelectionRange(el, start, end) {\n"
                            + "    if (document.createRange && window.getSelection) {\n"
                            + "        var range = document.createRange();\n"
                            + "        range.selectNodeContents(el);\n"
                            + "        var textNodes = getTextNodesIn(el);\n"
                            + "        var foundStart = false;\n"
                            + "        var charCount = 0, endCharCount;\n"
                            + "\n"
                            + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                            + "            endCharCount = charCount + textNode.length;\n"
                            + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                            + "                range.setStart(textNode, start - charCount);\n"
                            + "                foundStart = true;\n"
                            + "            }\n"
                            + "            if (foundStart && end <= endCharCount) {\n"
                            + "                range.setEnd(textNode, end - charCount);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "            charCount = endCharCount;\n"
                            + "        }\n"
                            + "\n"
                            + "        var sel = window.getSelection();\n"
                            + "        sel.removeAllRanges();\n"
                            + "        sel.addRange(range);\n"
                            + "    } else if (document.selection && document.body.createTextRange) {\n"
                            + "        var textRange = document.body.createTextRange();\n"
                            + "        textRange.moveToElementText(el);\n"
                            + "        textRange.collapse(true);\n"
                            + "        textRange.moveEnd(\"character\", end);\n"
                            + "        textRange.moveStart(\"character\", start);\n"
                            + "        textRange.select();\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "function makeEditableAndHighlight(color, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    document.execCommand('styleWithCSS', false, true);"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                            + "        document.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.textDecoration = \"underline\";"
                            + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;"
                            + "    sel.collapseToEnd();"
                            + "    document.designMode = \"off\";\n"
                            + "}\n"
                            + "\n"
                            + "function highlightAndToolTip(color, ttext) {\n"
                            + "    var range, sel;\n"
                            + "    if (window.getSelection) {\n"
                            + "        // IE9 and non-IE\n"
                            + "        try {\n"
                            + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                            + "                makeEditableAndHighlight(color, ttext);\n"
                            + "            }\n"
                            + "        } catch (ex) {\n"
                            + "            makeEditableAndHighlight(color, ttext)\n"
                            + "        }\n"
                            + "    } else if (document.selection && document.selection.createRange) {\n"
                            + "        // IE <= 8 case\n"
                            + "        range = document.selection.createRange();\n"
                            + "        range.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                            + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\");";

                    b.mainFrame().get().executeJavaScript(script);
                });
            }
        }

        public void selectAndHighlightItalicizeUnderlineRange(final int start, final int stop, final String hexColor, final String tip) {
            if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim().equals("")) {
                Platform.runLater(() -> {
                    String script = "function getTextNodesIn(node) {\n"
                            + "    var textNodes = [];\n"
                            + "    if (node.nodeType == 3) {\n"
                            + "        textNodes.push(node);\n"
                            + "    } else {\n"
                            + "        var children = node.childNodes;\n"
                            + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                            + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                            + "        }\n"
                            + "    }\n"
                            + "    return textNodes;\n"
                            + "}\n"
                            + "\n"
                            + "function setSelectionRange(el, start, end) {\n"
                            + "    if (document.createRange && window.getSelection) {\n"
                            + "        var range = document.createRange();\n"
                            + "        range.selectNodeContents(el);\n"
                            + "        var textNodes = getTextNodesIn(el);\n"
                            + "        var foundStart = false;\n"
                            + "        var charCount = 0, endCharCount;\n"
                            + "\n"
                            + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                            + "            endCharCount = charCount + textNode.length;\n"
                            + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                            + "                range.setStart(textNode, start - charCount);\n"
                            + "                foundStart = true;\n"
                            + "            }\n"
                            + "            if (foundStart && end <= endCharCount) {\n"
                            + "                range.setEnd(textNode, end - charCount);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "            charCount = endCharCount;\n"
                            + "        }\n"
                            + "\n"
                            + "        var sel = window.getSelection();\n"
                            + "        sel.removeAllRanges();\n"
                            + "        sel.addRange(range);\n"
                            + "    } else if (document.selection && document.body.createTextRange) {\n"
                            + "        var textRange = document.body.createTextRange();\n"
                            + "        textRange.moveToElementText(el);\n"
                            + "        textRange.collapse(true);\n"
                            + "        textRange.moveEnd(\"character\", end);\n"
                            + "        textRange.moveStart(\"character\", start);\n"
                            + "        textRange.select();\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "function makeEditableAndHighlight(color, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    document.execCommand('styleWithCSS', false, true);"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                            + "        document.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.fontStyle = \"italic\";"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.textDecoration = \"underline\";"
                            + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;"
                            + "    sel.collapseToEnd();"
                            + "    document.designMode = \"off\";\n"
                            + "}\n"
                            + "\n"
                            + "function highlightAndToolTip(color, ttext) {\n"
                            + "    var range, sel;\n"
                            + "    if (window.getSelection) {\n"
                            + "        // IE9 and non-IE\n"
                            + "        try {\n"
                            + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                            + "                makeEditableAndHighlight(color, ttext);\n"
                            + "            }\n"
                            + "        } catch (ex) {\n"
                            + "            makeEditableAndHighlight(color, ttext)\n"
                            + "        }\n"
                            + "    } else if (document.selection && document.selection.createRange) {\n"
                            + "        // IE <= 8 case\n"
                            + "        range = document.selection.createRange();\n"
                            + "        range.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "}"
                            + "\n"
                            + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                            + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\");";

                    b.mainFrame().get().executeJavaScript(script);
                });
            }
        }

        public void selectAndHighlightBoldRange(final int start, final int stop, final String hexColor, final String tip) {
            if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim().equals("")) {
                Platform.runLater(() -> {
                    String script = "function getTextNodesIn(node) {\n"
                            + "    var textNodes = [];\n"
                            + "    if (node.nodeType == 3) {\n"
                            + "        textNodes.push(node);\n"
                            + "    } else {\n"
                            + "        var children = node.childNodes;\n"
                            + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                            + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                            + "        }\n"
                            + "    }\n"
                            + "    return textNodes;\n"
                            + "}\n"
                            + "\n"
                            + "function setSelectionRange(el, start, end) {\n"
                            + "    if (document.createRange && window.getSelection) {\n"
                            + "        var range = document.createRange();\n"
                            + "        range.selectNodeContents(el);\n"
                            + "        var textNodes = getTextNodesIn(el);\n"
                            + "        var foundStart = false;\n"
                            + "        var charCount = 0, endCharCount;\n"
                            + "\n"
                            + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                            + "            endCharCount = charCount + textNode.length;\n"
                            + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                            + "                range.setStart(textNode, start - charCount);\n"
                            + "                foundStart = true;\n"
                            + "            }\n"
                            + "            if (foundStart && end <= endCharCount) {\n"
                            + "                range.setEnd(textNode, end - charCount);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "            charCount = endCharCount;\n"
                            + "        }\n"
                            + "\n"
                            + "        var sel = window.getSelection();\n"
                            + "        sel.removeAllRanges();\n"
                            + "        sel.addRange(range);\n"
                            + "    } else if (document.selection && document.body.createTextRange) {\n"
                            + "        var textRange = document.body.createTextRange();\n"
                            + "        textRange.moveToElementText(el);\n"
                            + "        textRange.collapse(true);\n"
                            + "        textRange.moveEnd(\"character\", end);\n"
                            + "        textRange.moveStart(\"character\", start);\n"
                            + "        textRange.select();\n"
                            + "    }\n"
                            + "}\n"
                            + "\n"
                            + "function makeEditableAndHighlight(color, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    document.execCommand('styleWithCSS', false, true);"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                            + "        document.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.fontWeight = \"bold\";\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;\n"
                            + "    sel.collapseToEnd();\n"
                            + "    document.designMode = \"off\";\n"
                            + "}\n"
                            + "\n"
                            + "function highlightAndToolTip(color, ttext) {\n"
                            + "    var range, sel;\n"
                            + "    if (window.getSelection) {\n"
                            + "        // IE9 and non-IE\n"
                            + "        try {\n"
                            + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                            + "                makeEditableAndHighlight(color, ttext);\n"
                            + "            }\n"
                            + "        } catch (ex) {\n"
                            + "            makeEditableAndHighlight(color, ttext);\n"
                            + "        }\n"
                            + "    } else if (document.selection && document.selection.createRange) {\n"
                            + "        // IE <= 8 case\n"
                            + "        range = document.selection.createRange();\n"
                            + "        range.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "}\n"
                            + "\n"
                            + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                            + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\");\n";

                    b.mainFrame().get().executeJavaScript(script);
                });
            }
        }

        public void selectAndHighlightBorderRange(final int start, final int stop, final String hexColor, final String tip) {
            if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim().equals("")) {
                Platform.runLater(() -> {
                    String script = "function getTextNodesIn(node) {\n"
                            + "    var textNodes = [];\n"
                            + "    if (node.nodeType == 3) {\n"
                            + "        textNodes.push(node);\n"
                            + "    } else {\n"
                            + "        var children = node.childNodes;\n"
                            + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                            + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                            + "        }\n"
                            + "    }\n"
                            + "    return textNodes;\n"
                            + "}\n"
                            + "\n"
                            + "function setSelectionRange(el, start, end) {\n"
                            + "    if (document.createRange && window.getSelection) {\n"
                            + "        var range = document.createRange();\n"
                            + "        range.selectNodeContents(el);\n"
                            + "        var textNodes = getTextNodesIn(el);\n"
                            + "        var foundStart = false;\n"
                            + "        var charCount = 0, endCharCount;\n"
                            + "\n"
                            + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                            + "            endCharCount = charCount + textNode.length;\n"
                            + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                            + "                range.setStart(textNode, start - charCount);\n"
                            + "                foundStart = true;\n"
                            + "            }\n"
                            + "            if (foundStart && end <= endCharCount) {\n"
                            + "                range.setEnd(textNode, end - charCount);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "            charCount = endCharCount;\n"
                            + "        }\n"
                            + "\n"
                            + "        var sel = window.getSelection();\n"
                            + "        sel.removeAllRanges();\n"
                            + "        sel.addRange(range);\n"
                            + "    } else if (document.selection && document.body.createTextRange) {\n"
                            + "        var textRange = document.body.createTextRange();\n"
                            + "        textRange.moveToElementText(el);\n"
                            + "        textRange.collapse(true);\n"
                            + "        textRange.moveEnd(\"character\", end);\n"
                            + "        textRange.moveStart(\"character\", start);\n"
                            + "        textRange.select();\n"
                            + "    }\n"
                            + "}\n"
                            + "\n"
                            + "function makeEditableAndHighlight(color, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    document.execCommand('styleWithCSS', false, true);"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                            + "        document.execCommand(\"BackColor\", false, color);\n"
                            + "    }\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.borderStyle = \"solid\";\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.borderWidth = \"thin\";\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.borderColor = \"black\";\n"
                            + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;\n"
                            + "    sel.collapseToEnd();\n"
                            + "    document.designMode = \"off\";\n"
                            + "}\n"
                            + "\n"
                            + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                            + "makeEditableAndHighlight(\"" + hexColor + "\", \"" + tip + "\");\n";

                    b.mainFrame().get().executeJavaScript(script);
                });
            }
        }

        public void highlightCurrentSelection(final String hexColor, final String type) {
            Platform.runLater(() -> {
                String script
                        = "function getTextNodesIn(node) {\n"
                        + "    var textNodes = [];\n"
                        + "    if (node.nodeType == 3) {\n"
                        + "        textNodes.push(node);\n"
                        + "    } else {\n"
                        + "        var children = node.childNodes;\n"
                        + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                        + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                        + "        }\n"
                        + "    }\n"
                        + "    return textNodes;\n"
                        + "}\n"
                        + "\n"
                        + "function setSelectionRange(el, start, end) {\n"
                        + "    if (document.createRange && window.getSelection) {\n"
                        + "        var range = document.createRange();\n"
                        + "        range.selectNodeContents(el);\n"
                        + "        var textNodes = getTextNodesIn(el);\n"
                        + "        var foundStart = false;\n"
                        + "        var charCount = 0, endCharCount;\n"
                        + "\n"
                        + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                        + "            endCharCount = charCount + textNode.length;\n"
                        + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                        + "                range.setStart(textNode, start - charCount);\n"
                        + "                foundStart = true;\n"
                        + "            }\n"
                        + "            if (foundStart && end <= endCharCount) {\n"
                        + "                range.setEnd(textNode, end - charCount);\n"
                        + "                break;\n"
                        + "            }\n"
                        + "            charCount = endCharCount;\n"
                        + "        }\n"
                        + "\n"
                        + "        var sel = window.getSelection();\n"
                        + "        sel.removeAllRanges();\n"
                        + "        sel.addRange(range);\n"
                        + "    } else if (document.selection && document.body.createTextRange) {\n"
                        + "        var textRange = document.body.createTextRange();\n"
                        + "        textRange.moveToElementText(el);\n"
                        + "        textRange.collapse(true);\n"
                        + "        textRange.moveEnd(\"character\", end);\n"
                        + "        textRange.moveStart(\"character\", start);\n"
                        + "        textRange.select();\n"
                        + "    }\n"
                        + "}"
                        + "\n"
                        + "function makeEditableAndHighlight(color, ttext) {\n"
                        + "    sel = window.getSelection();\n"
                        + "    document.designMode = \"on\";\n"
                        + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                        + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                        + "        document.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;\n"
                        + "    sel.collapseToEnd();\n"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext);\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + type + "\");\n";

                b.mainFrame().get().executeJavaScript(script);
            });
        }

        public String getContent() {
            return b.mainFrame().get().html();
        }

        public void loadContent(String html) {
            this.html = html;
            b.navigation().loadUrl("http://localhost?load-html");
        }

        private Node createSpacer() {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            return spacer;
        }

        String selection = "EMPTY";
        final Coordinates coordinate = new Coordinates(0, 0);

        public void loadSelection() {
            Runnable runnable = () -> {
                selection = b.mainFrame().get().executeJavaScript("window.getSelection().toString()");
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public String getSelection() {
            return selection;
        }

        Coordinates getSelectionCoordinate() {
            return coordinate;
        }

        public void loadSelectionCoordinates() {
            Runnable runnable = () -> {
                String positionX = getRangePosition(true);
                String positionY = getRangePosition(false);
                coordinate.x = Float.parseFloat(positionX) - 75.0f;
                coordinate.y = Float.parseFloat(positionY) - 75.0f;
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        protected int getCaretPositionX() {
            String position = "function getCaretCharacterOffsetWithin(element) {\n"
                    + "    var caretOffset = 0;\n"
                    + "    var doc = element.ownerDocument || element.document;\n"
                    + "    var win = doc.defaultView || doc.parentWindow;\n"
                    + "    var sel;\n"
                    + "    if (typeof win.getSelection != \"undefined\") {\n"
                    + "        sel = win.getSelection();\n"
                    + "        if (sel.rangeCount > 0) {\n"
                    + "            var range = win.getSelection().getRangeAt(0);\n"
                    + "            var preCaretRange = range.cloneRange();\n"
                    + "            preCaretRange.selectNodeContents(element);\n"
                    + "            preCaretRange.setEnd(range.endContainer, range.endOffset);\n"
                    + "            caretOffset = preCaretRange.toString().length;\n"
                    + "        }\n"
                    + "    } else if ( (sel = doc.selection) && sel.type != \"Control\") {\n"
                    + "        var textRange = sel.createRange();\n"
                    + "        var preCaretTextRange = doc.body.createTextRange();\n"
                    + "        preCaretTextRange.moveToElementText(element);\n"
                    + "        preCaretTextRange.setEndPoint(\"EndToEnd\", textRange);\n"
                    + "        caretOffset = preCaretTextRange.text.length;\n"
                    + "    }\n"
                    + "    return caretOffset;\n"
                    + "}"
                    + "getCaretCharacterOffsetWithin(document.documentElement)";

            Double offSet = b.mainFrame().get().executeJavaScript(position);
            return offSet.intValue();
        }

        public void selectRangeAndClear(final int start, final int stop) {
            Platform.runLater(() -> {
                String script = "function getTextNodesIn(node) {\n"
                        + "    var textNodes = [];\n"
                        + "    if (node.nodeType == 3) {\n"
                        + "        textNodes.push(node);\n"
                        + "    } else {\n"
                        + "        var children = node.childNodes;\n"
                        + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                        + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                        + "        }\n"
                        + "    }\n"
                        + "    return textNodes;\n"
                        + "}\n"
                        + "\n"
                        + "function setSelectionRangeAndClear(el, start, end) {\n"
                        + "        var range = document.createRange();\n"
                        + "        range.selectNodeContents(el);\n"
                        + "        var textNodes = getTextNodesIn(el);\n"
                        + "        var foundStart = false;\n"
                        + "        var charCount = 0, endCharCount;\n"
                        + "\n"
                        + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                        + "            endCharCount = charCount + textNode.length;\n"
                        + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                        + "                range.setStart(textNode, start - charCount);\n"
                        + "                foundStart = true;\n"
                        + "            }\n"
                        + "            if (foundStart && end <= endCharCount) {\n"
                        + "                range.setEnd(textNode, end - charCount);\n"
                        + "                break;\n"
                        + "            }\n"
                        + "            charCount = endCharCount;\n"
                        + "        }\n"
                        + "\n"
                        + "        var sel = window.getSelection();\n"
                        + "        sel.removeAllRanges();\n"
                        + "        sel.addRange(range);\n"
                        + "        document.designMode = \"on\";"
                        + "        var span = sel.getRangeAt(0).endContainer.parentNode;"
                        + "        while(span.firstChild)\n"
                        + "        {\n"
                        + "            span.parentElement.insertBefore(span.firstChild, span);\n"
                        + "        }\n"
                        + "        span.parentElement.removeChild(span);"
                        + "        sel.collapseToEnd();"
                        + "        document.designMode = \"off\";"
                        + "}"
                        + "setSelectionRangeAndClear(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n";
                b.mainFrame().get().executeJavaScript(script);
            });
        }

        protected String getRangePosition(boolean start) {
            String container = "", offset = "";
            if (start) {
                container = "startContainer";
                offset = "startOffset";
            } else {
                container = "endContainer";
                offset = "endOffset";
            }
            String ranges = "function getCaretOffset(element) {\n"
                    + "    var caretOffset = 0;\n"
                    + "    var doc = element.ownerDocument || element.document;\n"
                    + "    var win = doc.defaultView || doc.parentWindow;\n"
                    + "    var sel;\n"
                    + "    if (typeof win.getSelection != \"undefined\") {\n"
                    + "        sel = win.getSelection();\n"
                    + "        if (sel.rangeCount > 0) {\n"
                    + "            var range = win.getSelection().getRangeAt(0);\n"
                    + "            var preCaretRange = range.cloneRange();\n"
                    + "            preCaretRange.selectNodeContents(element);\n"
                    + "            preCaretRange.setEnd(range." + container + ", range." + offset + ");\n"
                    + "            caretOffset = preCaretRange.toString().length;\n"
                    + "        }\n"
                    + "    } else if ( (sel = doc.selection) && sel.type != \"Control\") {\n"
                    + "        var textRange = sel.createRange();\n"
                    + "        var preCaretTextRange = doc.body.createTextRange();\n"
                    + "        preCaretTextRange.moveToElementText(element);\n"
                    + "        preCaretTextRange.setEndPoint(\"EndToEnd\", textRange);\n"
                    + "        caretOffset = preCaretTextRange.text.length;\n"
                    + "    }\n"
                    + "    return caretOffset;\n"
                    + "}"
                    + "getCaretOffset(document.getElementsByTagName(\"font\")[0])";

            Double offSet = b.mainFrame().get().executeJavaScript(ranges);
            System.out.println("range: " + offSet);
            return offSet.toString();
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            layoutInArea(browserView, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
        }

        @Override
        protected double computePrefWidth(double height) {
            return 1100;
        }

        @Override
        protected double computePrefHeight(double width) {
            return 500;
        }

        private <T> Set<T> findDuplicates(Collection<T> list, T object) {
            Set<T> duplicates = new LinkedHashSet<>();
            list.stream().filter((t) -> (t.equals(object))).forEach((t) -> {
                duplicates.add(t);
            });
            return duplicates;
        }
    }

    class Coordinates extends Point2D.Float implements Comparable {

        public Coordinates(float x, float y) {
            super(x, y);
        }

        @Override
        public int compareTo(Object t) {
            if (t instanceof Coordinates) {
                Coordinates po = (Coordinates) t;
                return (new java.lang.Float(this.x)).compareTo(po.x);
            }
            return 0;
        }

        @Override
        public String toString() {
            String string = "";
            string += "[" + this.x + ", " + this.y + "]";
            return string;
        }
    }

    private String getSelectedButtonText(ToggleGroup buttonGroup) {

        for (Toggle toggle : buttonGroup.getToggles()) {
            if (toggle.isSelected()) {
                return ((RadioButton) toggle).getText();
            }
        }

        return null;
    }
}
