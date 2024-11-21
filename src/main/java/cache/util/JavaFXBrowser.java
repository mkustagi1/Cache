package cache.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.geom.Point2D;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.web.WebEvent;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import cache.dataimportes.holders.AnnotationResults;
import cache.dataimportes.holders.Strand;
import cache.workers.AlignmentWorker;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JavaFXBrowser extends JPanel {

    private final JFXPanel jfxPanel = new JFXPanel();
    private final JFXPanel rcJfxPanel = new JFXPanel();
    private final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jfxPanel, rcJfxPanel);
    private Browser browser;
    private Browser rcBrowser;
    private String content;
    protected JavaFXBrowserChart chart;
    protected JavaFXMultimapperChart mmpChart;

    public JavaFXBrowser() {
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
        browser.centerTo(xx);
        rcBrowser.centerTo(xx);
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
        this.setLayout(new BorderLayout());
        this.add(splitPane, BorderLayout.CENTER);
    }

    private void createScene() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            browser = new Browser(false);
            Scene scene = new Scene(browser, 1100, 250, javafx.scene.paint.Color.web("#666970"));
            jfxPanel.setScene(scene);
            splitPane.setTopComponent(jfxPanel);
            rcBrowser = new Browser(true);
            browser.setOtherBrowser(rcBrowser);
            rcBrowser.setOtherBrowser(browser);
            scene = new Scene(rcBrowser, 1100, 250, javafx.scene.paint.Color.web("#666970"));
            rcJfxPanel.setScene(scene);
            splitPane.setTopComponent(jfxPanel);
            splitPane.setBottomComponent(rcJfxPanel);
            splitPane.setDividerLocation(0.3);
            splitPane.setOneTouchExpandable(true);
        });
    }

    public String getContent() {
        String fc = browser.getContent();
        String rc = rcBrowser.getContent();
        String[] tokens = fc.split("</font></html>");
        return tokens[0] + rc;
    }

    int transcriptLength = 0;
    long transcriptId = 0;

    public void setTranscriptId(long tid) {
        transcriptId = tid;
    }

    public Browser getRCBrowser() {
        return rcBrowser;
    }

    public Browser getBrowser() {
        return browser;
    }

    public void loadContent(final String content, final String name) {
        this.content = content;
        final String senseContent = getReverseComplement(content, Boolean.FALSE, name);
        Platform.runLater(() -> {
            browser.loadContent(senseContent);
        });
        final String rcContent = getReverseComplement(content, Boolean.TRUE, name);
        Platform.runLater(() -> {
            rcBrowser.loadContent(rcContent);
        });
    }

    private String getReverseComplement1(String html, Boolean rc, String name) {
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

    private String getReverseComplement(String html, Boolean rc, String name) {
        String htmlSection = "";
        Document doc = Jsoup.parse(html);
        Element root = doc.body();
        Element font = root.child(0);

        java.util.List<org.jsoup.nodes.Node> toBeRemoved = new ArrayList<>();

        if (rc) {
            int tEntry = 0;

            for (org.jsoup.nodes.Node child : font.childNodes()) {
                if (child instanceof TextNode) {
                    String text = ((TextNode) child).text();
                    int index = name.indexOf("_");
                    int index1 = name.indexOf("-");
                    index = (index1 >= 0 && index1 < index) ? index1 : index;
                    index = (index <= 0) ? name.length() : index;
                    if (!text.startsWith(".") && (text.startsWith(name.substring(0, index)))) {
                        transcriptLength = ((TextNode) child).text().length();
                        tEntry++;
                    }
                }

                if (tEntry == 1) {
                    toBeRemoved.add(child);
                }
            }
        } else {
            int tEntry = 0;
            for (org.jsoup.nodes.Node child : font.childNodes()) {
                if (child instanceof TextNode) {
                    String text = ((TextNode) child).text();
                    int index = name.indexOf("_");
                    int index1 = name.indexOf("-");
                    index = (index1 >= 0 && index1 < index) ? index1 : index;
                    index = (index <= 0) ? name.length() : index;
                    if (!text.startsWith(".") && (text.startsWith(name.substring(0, index)))) {
                        transcriptLength = ((TextNode) child).text().length();
                        tEntry++;
                    }
                }
                if (tEntry == 2) {
                    toBeRemoved.add(child);
                }
            }
        }
        toBeRemoved.stream().forEach((child) -> {
            child.remove();
        });
        htmlSection = doc.html();
        return htmlSection;
    }

    public class Browser extends Pane {

        final WebView browser = new WebView();
        final WebEngine webEngine = browser.getEngine();
        private final Line LV1;
        boolean rc;
        Browser otherBrowser;
        Map<Coordinates, String> annotations = new HashMap<>();

        final AlignmentWorker worker = new AlignmentWorker();

        public Browser(boolean rc) {
            this.rc = rc;
            //apply the styles
            getStyleClass().add("browser");
            //add the web view to the scene

            LV1 = new Line();
            LV1.setStrokeWidth(1);
            LV1.setOpacity(0.6);
            LV1.setStroke(javafx.scene.paint.Color.FORESTGREEN);

            Pane glassPane = new Pane();
            glassPane.getChildren().add(LV1);
            glassPane.minWidthProperty().bind(browser.widthProperty());
            glassPane.minHeightProperty().bind(browser.heightProperty());
            glassPane.setMouseTransparent(true);
            LV1.endYProperty().bind(browser.heightProperty());
            getChildren().add(browser);
            getChildren().add(glassPane);
            webEngine.setOnAlert((WebEvent<String> t) -> {
                System.out.println("WebEngine alert: " + t.getData());
            });

            webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

                @Override
                public void changed(ObservableValue ov, State oldState, State newState) {
                    if (newState == Worker.State.SUCCEEDED) {
                        refreshAnnotations();
                    }
                }
            });

            webEngine.setJavaScriptEnabled(true);

            browser.getStylesheets().add("cache/util/webView.css");

            addListeners();
        }

        public void setOtherBrowser(Browser ob) {
            this.otherBrowser = ob;
        }

        private void addListeners() {
            Set<Node> nodes = browser.lookupAll(".scroll-bar");
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
                }
            };
            browser.setOnMouseClicked(mouseHandler);
            browser.setOnMouseMoved(mouseHandler);

            browser.setContextMenuEnabled(false);
            final ContextMenu cm = new ContextMenu();
            MenuItem cmItem1 = new MenuItem("Annotate");
            MenuItem cmItem2 = new MenuItem("Clear Annotation");
            cm.getItems().add(cmItem1);
            cm.getItems().add(cmItem2);
            browser.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    cm.show(browser, e.getScreenX(), e.getScreenY());
                }
            });

            cmItem1.setOnAction((javafx.event.ActionEvent actionEvent) -> {
                handleAnnotations();
            });

            cmItem2.setOnAction((javafx.event.ActionEvent actionEvent) -> {
                clearSelectedAnnotation();
            });
        }

        public void refreshAnnotations() {
            clearAnnotationHighlights();
            annotations.clear();
            List<AnnotationResults> arList = worker.getAnnotationsForTranscript(transcriptId);
            if (arList != null) {
                Collections.sort(arList);
                arList.stream().forEach((ar) -> {
                    int frequency = Collections.frequency(arList, ar);
                    String tip = ar.annotation;
                    tip = tip.trim();
                    tip = tip.replaceAll("\"", "");

                    Coordinates c = new Coordinates(ar.startCoordinate, ar.stopCoordinate);
                    annotations.put(c, ar.annotation);
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
                    if (ar.annotation.equals("Start Codon")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                        }
                    } else if (ar.annotation.equals("Stop Codon")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                        }
                    } else if (ar.annotation.equals("PolyA Signal")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                        }
                    } else if (ar.annotation.equals("Cleavage Site")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                        }
                    } else if (ar.annotation.equals("Alternative Cassette Exon")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#b4efb8", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#b4efb8", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#b4efb8", tip);
                        }
                    } else if (ar.annotation.equals("Alternative 5’ Splice Site Exon")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#c9f4cc", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#c9f4cc", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#c9f4cc", tip);
                        }
                    } else if (ar.annotation.equals("Alternative 3’ Splice Site Exon")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#def8e0", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#def8e0", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#def8e0", tip);
                        }
                    } else if (ar.annotation.equals("Start Site")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
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
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                        }
                    } else if (ar.annotation.equals("Transposition")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                        }
                    } else if (ar.annotation.equals("Deletion")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                        }
                    } else if (ar.annotation.equals("Insertion")) {
                        if (frequency > 1) {
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "##ff7f00", tip);
                        } else if (ar.predicted) {
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                        }
                    } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Ambion_SilencerSelect")) {
                        if (frequency > 1) {
                            Set<AnnotationResults> dups = findDuplicates(arList, ar);
                            tip = "";
                            tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D3DDBD", tip);
                        } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                            selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D3DDBD", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D3DDBD", tip);
                        }
                    } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_ON-TARGETplus")) {
                        if (frequency > 1) {
                            Set<AnnotationResults> dups = findDuplicates(arList, ar);
                            tip = "";
                            tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#C4D2A7", tip);
                        } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                            selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#C4D2A7", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#C4D2A7", tip);
                        }
                    } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_siGENOME")) {
                        if (frequency > 1) {
                            Set<AnnotationResults> dups = findDuplicates(arList, ar);
                            tip = "";
                            tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                            selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#B5C691", tip);
                        } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                            selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#B5C691", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#B5C691", tip);
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
                        selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                    }
                });
            }
        }

        public void refreshAnnotationsWithoutClear() {
            Runnable runnable = () -> {
                List<AnnotationResults> arList = worker.getAnnotationsForTranscript(transcriptId);
                if (arList != null) {
                    Collections.sort(arList);
                    arList.stream().forEach((ar) -> {
                        int frequency = Collections.frequency(arList, ar);
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
                        if (ar.annotation.equals("Start Codon")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip);
                            }
                        } else if (ar.annotation.equals("Stop Codon")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip);
                            }
                        } else if (ar.annotation.equals("PolyA Signal")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip);
                            }
                        } else if (ar.annotation.equals("Cleavage Site")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip);
                            }
                        } else if (ar.annotation.equals("Alternative Cassette Exon")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#b4efb8", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#b4efb8", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#b4efb8", tip);
                            }
                        } else if (ar.annotation.equals("Alternative 5’ Splice Site Exon")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#c9f4cc", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#c9f4cc", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#c9f4cc", tip);
                            }
                        } else if (ar.annotation.equals("Alternative 3’ Splice Site Exon")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#def8e0", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#def8e0", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#def8e0", tip);
                            }
                        } else if (ar.annotation.equals("Start Site")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip);
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
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip);
                            }
                        } else if (ar.annotation.equals("Transposition")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip);
                            }
                        } else if (ar.annotation.equals("Deletion")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip);
                            }
                        } else if (ar.annotation.equals("Insertion")) {
                            if (frequency > 1) {
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else if (ar.predicted) {
                                selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Ambion_SilencerSelect")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D3DDBD", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D3DDBD", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D3DDBD", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_ON-TARGETplus")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#C4D2A7", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#C4D2A7", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#C4D2A7", tip);
                            }
                        } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_siGENOME")) {
                            if (frequency > 1) {
                                Set<AnnotationResults> dups = findDuplicates(arList, ar);
                                tip = "";
                                tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                                selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#B5C691", tip);
                            } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                                selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#B5C691", tip);
                            } else {
                                selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#B5C691", tip);
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
                            selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        } else {
                            selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip);
                        }
                    });
                }
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

            SwingUtilities.invokeLater(() -> {
                String variant = "_";
                String[] annotationTypes = new String[]{"Start Codon", "Stop Codon", "PolyA Signal",
                    "Cleavage Site", "Start Site", "Substitution", "Transposition", "Insertion", "Deletion",
                    "Alternative Cassette Exon", "Alternative 5’ Splice Site Exon",
                    "Alternative 3’ Splice Site Exon", "Freeform annotation"};
                final JPanel panel = new JPanel(new GridLayout(11, 1));
                final JRadioButton button0 = new JRadioButton(annotationTypes[0]);
                final JRadioButton button1 = new JRadioButton(annotationTypes[1]);
                final JRadioButton button2 = new JRadioButton(annotationTypes[2]);
                final JRadioButton button3 = new JRadioButton(annotationTypes[3]);
                final JRadioButton button4 = new JRadioButton(annotationTypes[4]);
                final JRadioButton button5 = new JRadioButton(annotationTypes[5]);
                final JRadioButton button6 = new JRadioButton(annotationTypes[6]);
                final JRadioButton button7 = new JRadioButton(annotationTypes[7]);
                final JRadioButton button8 = new JRadioButton(annotationTypes[8]);
                final JRadioButton button9 = new JRadioButton(annotationTypes[9]);
                final JRadioButton button10 = new JRadioButton(annotationTypes[10]);
                final JRadioButton button11 = new JRadioButton(annotationTypes[11]);
                final JRadioButton button12 = new JRadioButton(annotationTypes[12]);
                panel.add(button0);
                panel.add(button1);
                panel.add(button2);
                panel.add(button3);
                panel.add(button4);
                panel.add(button5);
                panel.add(button6);
                panel.add(button7);
                panel.add(button8);
                panel.add(button9);
                panel.add(button10);
                panel.add(button11);
                panel.add(button12);
                ButtonGroup group = new ButtonGroup();
                group.add(button0);
                group.add(button1);
                group.add(button2);
                group.add(button3);
                group.add(button4);
                group.add(button5);
                group.add(button6);
                group.add(button7);
                group.add(button8);
                group.add(button9);
                group.add(button10);
                group.add(button11);
                group.add(button12);
                button0.setSelected(true);
                panel.setSize(200, 430);
                panel.setPreferredSize(new Dimension(200, 430));
                JScrollPane pane = new JScrollPane(panel);
                int result = JOptionPane.showConfirmDialog(null, pane, "Select Annotation Type", JOptionPane.OK_CANCEL_OPTION);
                String at = getSelectedButtonText(group);
                if (at == null || result != JOptionPane.OK_OPTION) {
                    return;
                }
                if (at.equals("Substitution")) {
                    String[] variants = new String[]{"A", "C", "T", "G"};
                    variant = (String) JOptionPane.showInputDialog(null,
                            "Select Variant",
                            "Variant",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            variants,
                            variants[0]);
                    if (variant == null) {
                        return;
                    }
                }
                if (at.equals("Insertion")) {
                    variant = (String) JOptionPane.showInputDialog(null,
                            "Specify Insertion",
                            "",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            "Inserted Nucleotides..");
                    if (variant == null) {
                        return;
                    }
                }
                if (at.equals("Freeform annotation")) {
                    at = (String) JOptionPane.showInputDialog(null,
                            "Specify annotation",
                            "",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            "Your annotation here..");
                    if (at == null) {
                        return;
                    }
                }

                at = at.trim();
                at = at.replaceAll("\"", "");

                String selection1 = getSelection();
                Coordinates c = getSelectionCoordinate();
                if (c.x < 0 || c.y < 0 || c.x > transcriptLength || c.y > transcriptLength) {
                    return;
                }
                if (at.equals("Insertion") && (c.y - c.x) != 2) {
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
                    default:
                        highlightCurrentSelection("#d3d3d3", at);
                        break;
                }
                List<AnnotationResults> arList = new ArrayList<>();
                Strand strand = (!rc) ? Strand.FORWARD : Strand.REVERSECOMPLEMENT;
                AnnotationResults ar = new AnnotationResults(UUID.randomUUID(), transcriptId, selection1, at, variant, (int) c.x, (int) c.y, strand, false);
                arList.add(ar);
                worker.createOrDeleteAnnotation(arList, true);
                refreshAnnotations();
                otherBrowser.refreshAnnotations();
            });
        }

        public void zoomIn() {
            Runnable runnable = () -> {
                double fs = browser.getFontScale();
                browser.setFontScale(fs + 0.1);
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void zoomOut() {
            Runnable runnable = () -> {
                double fs = browser.getFontScale();
                if (fs > 0.1) {
                    browser.setFontScale(fs - 0.1);
                }
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        double verticalValue = 0;

        public void centerTo(final double xx) {
            Runnable runnable = () -> {
                Set<Node> nodes = browser.lookupAll(".scroll-bar");
                nodes.stream().filter((node) -> (ScrollBar.class.isInstance(node))).map((node) -> (ScrollBar) node).filter((scroll) -> (scroll.getOrientation() == Orientation.VERTICAL)).forEach((scroll) -> {
                    verticalValue = scroll.getValue();
                });
                for (Node node : nodes) {
                    if (ScrollBar.class.isInstance(node)) {
                        ScrollBar scroll = (ScrollBar) node;
                        if (scroll.getOrientation() == Orientation.HORIZONTAL) {
                            String docWidth = webEngine.executeScript(
                                    "window.getComputedStyle(document.body, null).getPropertyValue('width')"
                            ).toString();
                            final double width1 = Double.valueOf(docWidth.replace("px", ""));
                            final double maxScroll = scroll.getMax();
                            double fraction = (xx) / transcriptLength;
                            String script = "window.scrollTo(" + fraction * maxScroll + ", " + verticalValue + ");";
                            webEngine.executeScript(script);
                            double x1 = fraction * maxScroll;
                            LV1.setStartX(x1);
                            LV1.setEndX(x1);
                        }
                    }
                }
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        ChangeListener ocl = null;

        public void clearAnnotationHighlights() {
            Runnable runnable = () -> {
                String script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "sel.collapse(document.body, 0);"
                        + "var spans = document.getElementsByTagName(\"SPAN\"), i;"
                        + "for( i=0; i<spans.length; i++) {"
                        + "if((spans[i].style.backgroundColor != \"rgb(255, 127, 0)\") && (spans[i].style.backgroundColor != \"rgb(163, 194, 219)\") "
                        + "&& (spans[i].style.backgroundColor != \"rgb(255, 0, 0)\") && (spans[i].style.backgroundColor != \"rgb(255, 204, 204)\")) {"
                        + "spans[i].style.border = \"none\";"
                        + "spans[i].style.backgroundColor = \"transparent\";"
                        + "spans[i].style.fontWeight = \"normal\";"
                        + "spans[i].style.textDecoration = \"none\";"
                        + "spans[i].title = \"\";"
                        + "}"
                        + "}"
                        + "sel.collapseToEnd();"
                        + "document.designMode = \"off\";";

                webEngine.executeScript(script);
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

                webEngine.executeScript(script);
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
                        //                            + "document.execCommand(\"underline\", true, \"\");"
                        + "sel.collapseToEnd();"
                        + "}"
                        + "document.designMode = \"off\";";

                webEngine.executeScript(script);
                CursorToolkitOne.stopWaitCursor(source);
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(JavaFXBrowser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void clearSelectedAnnotation() {
            loadSelection();
            loadSelectionCoordinates();

            SwingUtilities.invokeLater(() -> {
                String selection1 = getSelection();
                Coordinates c = getSelectionCoordinate();
                System.out.println("Coordinates: " + c.toString());
                Strand strand = (!rc) ? Strand.FORWARD : Strand.REVERSECOMPLEMENT;
                if (annotations.get(c) != null) {
                    AnnotationResults ar = new AnnotationResults(UUID.randomUUID(), transcriptId, selection1, annotations.get(c), "", (int) c.x, (int) c.y, strand, false);
                    List<AnnotationResults> list = new ArrayList<>();
                    list.add(ar);
                    worker.createOrDeleteAnnotation(list, false);
                    refreshAnnotations();
                    otherBrowser.refreshAnnotations();
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
                            + "function makeEditableAndHighlight(colour, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                            + "        document.execCommand(\"BackColor\", false, colour);\n"
                            + "    }\n"
                            //                                + "    sel.getRangeAt(0).startContainer.parentNode.style.border = \"thin dotted black\";"
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
                    webEngine.executeScript(script);
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
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                            + "        document.execCommand(\"BackColor\", false, colour);\n"
                            + "    }\n"
                            //                                + "    sel.getRangeAt(0).startContainer.parentNode.style.border = \"thin dotted black\";"
                            + "    sel.getRangeAt(0).startContainer.parentNode.style.fontStyle = \"italic\";"
                            //                                + "    sel.getRangeAt(0).startContainer.parentNode.style.textDecoration = \"underline\";"
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

                    webEngine.executeScript(script);
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
                            + "function makeEditableAndHighlight(colour, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                            + "        document.execCommand(\"BackColor\", false, colour);\n"
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

                    webEngine.executeScript(script);
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
                            + "function makeEditableAndHighlight(colour, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                            + "        document.execCommand(\"BackColor\", false, colour);\n"
                            + "    }\n"
                            //                                + "    sel.getRangeAt(0).startContainer.parentNode.style.border = \"thin dotted black\";"
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

                    webEngine.executeScript(script);
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
                            + "function makeEditableAndHighlight(colour, ttext) {\n"
                            + "    sel = window.getSelection();\n"
                            + "    document.designMode = \"on\";\n"
                            + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                            + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                            + "        document.execCommand(\"BackColor\", false, colour);\n"
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
                    webEngine.executeScript(script);
                });
            }
        }

        public void highlightCurrentSelection(final String hexColor, final String type) {
            Platform.runLater(() -> {
                String script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "document.execCommand(\"HiliteColor\", true, \"" + hexColor + "\");"
                        + "sel.collapseToEnd();"
                        + "document.designMode = \"off\";";

                webEngine.executeScript(script);
            });
        }

        public String getContent() {
            String content = "";
            try {
                DOMSource domSource = new DOMSource(webEngine.getDocument());
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(domSource, result);
                content = writer.toString();
            } catch (TransformerException ex) {
                ex.printStackTrace();
            }
            return content;
        }

        public void loadContent(String html) {
            webEngine.loadContent(html);
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
                selection = (String) webEngine
                        .executeScript("window.getSelection().toString()");
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

        public Coordinates getSelectionCoordinate() {
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
            String position = "var element = document.body.childNodes[1].childNodes[0];"
                    + "var range = window.getSelection().getRangeAt(0);\n"
                    + "var preCaretRange = range.cloneRange();\n"
                    + "preCaretRange.selectNodeContents(element);\n"
                    + "preCaretRange.setEnd(range.endContainer, range.endOffset);\n"
                    + "caretOffset = preCaretRange.toString().length;"
                    + "caretOffset;";

            Number offSet = (Number) webEngine.executeScript(position);
            return offSet.intValue();
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
//            String ranges = "var element = document.body.childNodes[1].childNodes[0];"
//                    + "var range = window.getSelection().getRangeAt(0);\n"
//                    + "var preCaretRange = range.cloneRange();\n"
//                    + "preCaretRange.selectNodeContents(element);\n"
//                    + "preCaretRange.setEnd(range." + container + ", range." + offset + ");\n"
//                    + "caretOffset = preCaretRange.toString().length;"
//                    + "caretOffset;";

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

            Number offSet = (Number) webEngine.executeScript(ranges);
            System.out.println("range: " + offSet.toString());
            return offSet.toString();
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            layoutInArea(browser, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
        }

        @Override
        protected double computePrefWidth(double height) {
            return 750;
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

    private String getSelectedButtonText(ButtonGroup buttonGroup) {
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }

        return null;
    }
}
