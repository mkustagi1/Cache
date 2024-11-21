package cache.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cache.dataimportes.holders.Strand;
import cache.dataimportes.holders.AnnotationResults;
import cache.dataimportes.holders.TranscriptMappingResults;
import cache.workers.AlignmentWorker;
import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.InjectCssCallback;
import com.teamdev.jxbrowser.engine.Engine;
import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 *
 * @author Manjunath Kustagi
 */
public class EditorScene extends Scene {

    private final int experimentId;
    private final String authToken;
    final Browser b;
    final BrowserView browserView;
    long transcriptId;
    private boolean allExperiments = false;
    final AlignmentWorker worker = new AlignmentWorker();
    TranscriptMappingResults currentTmr;
    private static final int NAME_WIDTH = 75;
    private static final String DOT = ".";
    final Button refreshButton = new Button();
    final TextField nameBox = new TextField();
    final TextField emailBox = new TextField();

    public EditorScene(Parent p, final TranscriptMappingResults _tmr, int eid, String at) {
        super(p);

        TempDirectory tmpDir = new TempDirectory();
        tmpDir.deleteOnExit();

        Engine engine = Engine.newInstance(OFF_SCREEN);

        this.b = engine.newBrowser();
        this.browserView = BrowserView.newInstance(b);

        transcriptId = _tmr.transcriptID;
        experimentId = eid;
        authToken = at;
        setTmr(_tmr);

        Document doc1 = Jsoup.parse(_tmr.mappedAlignments);
        org.jsoup.nodes.Element element = doc1.body();
        org.jsoup.nodes.Element font = element.child(0);
        org.jsoup.nodes.Document doc = Jsoup.parse("<html></html>");
        org.jsoup.nodes.Element html = doc.html("<html></html>");
        org.jsoup.nodes.Element head = html.appendElement("head");
        org.jsoup.nodes.Element style = head.appendElement("style");
        style.attr("type", "text/css");
        style.text("font { white-space: nowrap; }");
        org.jsoup.nodes.Element e = html.appendElement("body");
        e.appendChild(font);

        getStylesheets().add("cache/util/webView.css");

        ToolBar toolBar = new ToolBar();
        Separator separator = new Separator();
        refreshButton.setMaxSize(25, 25);
        refreshButton.setMinSize(25, 25);
        refreshButton.setPrefSize(25, 25);
        refreshButton.setTooltip(new Tooltip("Refresh"));
        Image img = new Image(getClass().getResourceAsStream("/cache/resources/view-refresh-3.png"));
        refreshButton.setGraphic(new ImageView(img));
        refreshButton.setOnAction((javafx.event.ActionEvent event) -> {
            Image img1 = new Image(getClass().getResourceAsStream("/cache/resources/wait.gif"));
            setCursor(Cursor.WAIT);
            refreshButton.disableProperty().set(true);
            Runnable runnable = () -> {
                refreshWebView(getTmr());
            };
            try {
                FXUtilities.runAndWait(runnable);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        final Button persistButton = new Button();
        Separator separator1 = new Separator();
        persistButton.setMaxSize(25, 25);
        persistButton.setMinSize(25, 25);
        persistButton.setPrefSize(25, 25);
        persistButton.setTooltip(new Tooltip("Persist"));
        img = new Image(getClass().getResourceAsStream("/cache/resources/development-database.png"));
        persistButton.setGraphic(new ImageView(img));
        persistButton.setOnAction((javafx.event.ActionEvent event) -> {
            if (getEditedName().length() < 2) {
                nameBox.requestFocus();
                return;
            }
            persistEditedTranscript(b, getTmr(), getEmail());
            persistButton.disableProperty().set(true);
        });
        nameBox.setTooltip(new Tooltip("Transcript Name"));
        nameBox.setMinWidth(200);
        nameBox.setPrefWidth(200);

        nameBox.textProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                setEditedName((String) newValue);
            }

        });

        nameBox.setOnAction((javafx.event.ActionEvent event) -> {
            String text = nameBox.getText();
            setEditedName(text);
        });
        emailBox.setTooltip(new Tooltip("Enter your email"));
        emailBox.setMinWidth(200);
        emailBox.setPrefWidth(200);

        emailBox.setOnAction((javafx.event.ActionEvent event) -> {
            String text = emailBox.getText();
            setEmail(text);
        });

        emailBox.textProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                setEmail((String) newValue);
            }
        });

        final TextField searchBox = new TextField();
        searchBox.setTooltip(new Tooltip("Enter Sequence to search for in the alignment"));
        searchBox.setMinWidth(200);
        searchBox.setPrefWidth(200);
        Separator separator2 = new Separator();
        Button search = new Button();
        search.setText("Search");
        Separator separator3 = new Separator();
        search.setOnAction((javafx.event.ActionEvent event) -> {
            String text1 = searchBox.getText().trim();
            text1 = text1.replaceAll("\\s", "");
            text1 = text1.replaceAll("(\\r|\\n)", "");
            text1 = text1.replaceAll("\\r\\n", "");
            text1 = text1.replaceAll("-", "");
            text1 = text1.replaceAll("_", "");
            text1 = text1.replaceAll("\\d+", "");
            text1 = text1.toUpperCase();
            text1 = text1.replaceAll("U", "T");
            final String searchText = text1;

            Platform.runLater(() -> {
                searchBox.setText(searchText);
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
                script
                        = "document.designMode = \"on\";"
                        + "var sel = window.getSelection();"
                        + "sel.collapse(document.body, 0);"
                        + "while (window.find(\"" + searchText + "\")) {"
                        + "document.execCommand(\"HiliteColor\", false, \"" + "#E0E006" + "\");"
                        + "sel.collapseToEnd();"
                        + "}"
                        + "document.designMode = \"off\";";

                b.mainFrame().get().executeJavaScript(script);
            });
        });

        Separator separator4 = new Separator();
        Separator separator5 = new Separator();
        final CheckBox allExperimentsBox = new CheckBox();
        allExperimentsBox.setSelected(false);
        allExperimentsBox.setText("All Experiments");

        allExperimentsBox.selectedProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                allExperiments = allExperimentsBox.isSelected();
            }
        });

        toolBar.getItems().addAll(refreshButton, separator, persistButton, separator5, allExperimentsBox, separator1, nameBox, separator2, emailBox, separator3, searchBox, search, separator4);

        b.zoom().disable();

        b.mainFrame().get().loadHtml(doc.toString());

        b.navigation().on(FrameLoadFinished.class, event -> {
            refreshAnnotations();
            String htmlText = doc.body().getElementsByTag("font").text();
            htmlText = htmlText.replaceAll("\\s", "");
            String name = htmlText.substring(0, 75);
            name = name.replaceAll("\\.", "");
            String pattern = "MM.dd.yy.HH:mm";
            SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
            Date date = new Date();
            String _date = formatter.format(date);
            nameBox.setText(name + "_" + _date);
            setEditedName(name + "_" + _date);
        });

        browserView.setVisible(true);
        b.set(InjectCssCallback.class, params
                -> InjectCssCallback.Response.inject("span.highlight {opacity: 0.5;} body {font-size: 12px;}"));

        browserView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if ((event.isShortcutDown() || event.isMetaDown()) && event.getCode() == KeyCode.V) {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                String _html = clipboard.getString();
                Document _doc = Jsoup.parse(_html);
                String text = _doc.text();
                text = text.replaceAll("(\\r|\\n)", "");
                clipboard.clear();
                final ClipboardContent content = new ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            }
        });

        BorderPane border = new BorderPane();
        border.setTop(toolBar);
        border.setCenter(browserView);
        border.getStyleClass().add("browser");
        setRoot(border);

        String userHome = System.getProperty("user.home");
        try {
            File cache = new File(userHome + File.separator + ".cache.conf");
            if (cache.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(cache));
                String line = "";
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("user.email")) {
                        String _email = line.split("=")[1];
                        emailBox.setText(_email);
                        setEmail(emailBox.getText());
                    }
                }
            } else {
                emailBox.setText("yourname@domain.com");
                setEmail(emailBox.getText());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void loadWebView(TranscriptMappingResults tmr, Boolean editable) {
    }

    String editedName = "";
    String email = "";

    private void setEditedName(String name) {
        editedName = name;
    }

    public String getEditedName() {
        String toBeReturned = editedName;
        toBeReturned = (toBeReturned == null) ? "_" : toBeReturned;
        toBeReturned = toBeReturned.replaceAll("\'", "");
        toBeReturned = toBeReturned.replaceAll("-", "_");
        toBeReturned = toBeReturned.replaceAll(" ", "_");
        if (toBeReturned.length() >= 74) {
            toBeReturned = toBeReturned.substring(0, 74);
        } else if (toBeReturned.length() == 0) {
            toBeReturned = "_";
        }
        return toBeReturned;
    }

    private void setEmail(String e) {
        email = e;
        String userHome = System.getProperty("user.home");
        try {
            File cache = new File(userHome + File.separator + ".cache.conf");
            BufferedWriter bw = new BufferedWriter(new FileWriter(cache));
            String line = "user.email=" + email;
            bw.append(line);
            bw.newLine();
            bw.flush();
            bw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getEmail() {
        return email;
    }

    private void setTmr(TranscriptMappingResults t) {
        currentTmr = t;
    }

    public TranscriptMappingResults getTmr() {
        return currentTmr;
    }

    final AlignmentWorker worker1 = new AlignmentWorker();

    public void clearSearchHighlights() {
        Platform.runLater(() -> {
            String script
                    = "document.designMode = \"on\";"
                    + "var sel = window.getSelection();"
                    + "sel.collapse(document.body, 0);"
                    + "var spans = document.getElementsByTagName(\"SPAN\"), i;"
                    + "for( i=0; i<spans.length; i++) {"
                    + "var style = window.getComputedStyle(spans[i], null);"
                    + "if(spans[i].style.backgroundColor == \"rgb(224, 224, 6)\") {"
                    + "spans[i].style.backgroundColor = \"transparent\";"
                    + "}"
                    + "}"
                    + "sel.collapseToEnd();"
                    + "document.designMode = \"off\";";

            b.mainFrame().get().executeJavaScript(script);
        });
    }

    private void refreshWebView(TranscriptMappingResults tmr) {
        String html1 = b.mainFrame().get().executeJavaScript("document.documentElement.outerHTML");
        String htmlText = Jsoup.parse(html1).text();
        htmlText = htmlText.replaceAll("\\s", "");
        htmlText = htmlText.substring(76, htmlText.length() - 1);
        String delim = "\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.";
        String[] tokens = htmlText.split(delim);
        String transcript = tokens[0];
        if (transcript != null) {
            transcript = transcript.toUpperCase();
            transcript = transcript.replaceAll("[^\\w\\s]", "").trim();
            transcript = transcript.replaceAll("[^\\p{L}\\p{Z}]", "").trim();
        }
        TranscriptMappingResults tmr1 = new TranscriptMappingResults();
        tmr1.transcriptID = tmr.transcriptID;
        tmr1.mappedAlignments = transcript;
        tmr1.transcriptLength = transcript.length();
        tmr1.name = getEditedName();
        tmr1.symbol = getEditedName();
        final List<AnnotationResults> arList1 = getAnnotations();
        TranscriptMappingResults tmr2 = worker1.previewEditTranscript(tmr1, experimentId, authToken);
        setTmr(tmr2);

        if (tmr2 != null) {
            Document _doc1 = Jsoup.parse(tmr2.mappedAlignments);
            org.jsoup.nodes.Element _element = _doc1.body();
            org.jsoup.nodes.Element _font = _element.child(0);

            org.jsoup.nodes.Document doc2 = Jsoup.parse("<html></html>");
            org.jsoup.nodes.Element html2 = doc2.html("<html></html>");
            org.jsoup.nodes.Element head2 = html2.appendElement("head");
            org.jsoup.nodes.Element style2 = head2.appendElement("style");
            style2.attr("type", "text/css");
            style2.text("font { white-space: nowrap; }");
            org.jsoup.nodes.Element e = html2.appendElement("body");
            e.appendChild(_font);

            b.mainFrame().get().executeJavaScript("document.documentElement.outerHTML=''");

            b.navigation().on(FrameLoadFinished.class, event -> {
                refreshAnnotationsWithList(arList1);
                refreshButton.disableProperty().set(false);
                setCursor(Cursor.DEFAULT);
            });

            b.mainFrame().get().loadHtml(doc2.toString());
        } else {
        }
    }

    private void persistEditedTranscript(Browser b, TranscriptMappingResults tmr, String email) {
        String html1 = b.mainFrame().get().executeJavaScript("document.documentElement.outerHTML");
        html1 = html1.replaceAll("<\\/?\\s*span.*?>", "");
        Element font = Jsoup.parse(html1).getElementsByTag("body").first().getElementsByTag("font").first();
        String htmlText = font.textNodes().get(0).text();
        htmlText = htmlText.replaceAll("\\s", "");
        htmlText = htmlText.substring(76, htmlText.length());
        String delim = "\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.\\.";
        String[] tokens = htmlText.split(delim);
        String transcript = tokens[0];
        if (transcript != null) {
            transcript = transcript.toUpperCase();
            transcript = transcript.replaceAll("[^\\w\\s]", "").trim();
            transcript = transcript.replaceAll("[^\\p{L}\\p{Z}]", "").trim();
        }
        TranscriptMappingResults tmr1 = new TranscriptMappingResults();
        tmr1.transcriptID = tmr.transcriptID;
        tmr1.mappedAlignments = transcript;
        tmr1.transcriptLength = transcript.length();
        tmr1.name = getEditedName();
        tmr1.symbol = getEditedName();
        List<AnnotationResults> arList = getAnnotations();
        worker1.persistEditTranscript(tmr1, arList, experimentId, allExperiments, authToken, email);
    }

    private List<AnnotationResults> getAnnotations() {
        clearSearchHighlights();
        List<AnnotationResults> cList = new ArrayList<>();
        String html1 = b.mainFrame().get().executeJavaScript("document.documentElement.outerHTML");
        Document doc = Jsoup.parse(html1);
        Element font = doc.getElementsByTag("font").first();
        List<String> ids = new ArrayList<>();
        Elements annotations = doc.select("span");
        for (Element span : annotations.subList(0, annotations.size())) {
            String id = span.attr("id");
            if (id.contains("::")) {
                final String id1 = id;
                String[] _tokens = id.split("::");
                for (String _id : _tokens) {
                    if (_id != null && !_id.trim().equals("")) {
                        _id = _id.replace("siRNA: ", "siRNA_ ");
                        String[] tokens = _id.split(":");
                        if (tokens.length == 9) {
                            final AnnotationResults ar = new AnnotationResults();
                            ar.sequence = tokens[0];
                            ar.annotation = tokens[1];
                            if (ar.annotation.startsWith("siRNA")) {
                                ar.annotation = ar.annotation.replace("siRNA_ ", "siRNA: ");
                            }
                            ar.strand = (tokens[7].equals("FORWARD")) ? Strand.FORWARD : Strand.REVERSECOMPLEMENT;
                            ar.variant = tokens[2];
                            ar.predicted = (tokens[8].equals("true"));
                            ar.id = UUID.fromString(tokens[3].trim());
                            ar.transcriptId = Long.parseLong(tokens[4].trim());
                            int length = 0;

                            traversal:
                            for (Node node : font.childNodes()) {
                                if (node instanceof TextNode) {
                                    length += ((TextNode) node).text().length();
                                } else if ((node instanceof Element && ((Element) node).equals(span)) || (node.nodeName().equals("br"))) {
                                    break;
                                } else if (node.nodeName().equals("span") && node.childNodeSize() <= 1) {
                                    length += ((Element) node).text().length();
                                } else if (node.childNodeSize() > 1) {
                                    for (Node n : node.childNodes()) {
                                        if (n instanceof TextNode) {
                                            length += ((TextNode) n).text().length();
                                        } else if ((n instanceof Element && ((Element) n).equals(span)) || (n.nodeName().equals("br"))) {
                                            break traversal;
                                        } else if (n.nodeName().equals("span")) {
                                            length += ((Element) n).text().length();
                                        }
                                    }
                                }
                            }
                            ar.startCoordinate = (int) (Math.ceil(length)) - NAME_WIDTH;
                            if (ar.annotation.startsWith("Substitution")) {
                                ar.stopCoordinate = ar.startCoordinate + 1;
                            } else {
                                ar.stopCoordinate = ar.startCoordinate + ar.sequence.length();
                            }
                            if (!ids.contains(_id)) {
                                ids.add(_id);
                                cList.add(ar);
                            }
                        }
                    }
                }
            } else if (id != null && !id.trim().equals("")) {
                final String id1 = id;
                id = id.replace("siRNA: ", "siRNA_ ");
                String[] tokens = id.split(":");
                if (tokens.length == 9) {
                    final AnnotationResults ar = new AnnotationResults();
                    ar.sequence = tokens[0];
                    ar.annotation = tokens[1];
                    if (ar.annotation.startsWith("siRNA")) {
                        ar.annotation = ar.annotation.replace("siRNA_ ", "siRNA: ");
                    }
                    ar.strand = (tokens[7].equals("FORWARD")) ? Strand.FORWARD : Strand.REVERSECOMPLEMENT;
                    ar.variant = tokens[2];
                    ar.predicted = (tokens[8].equals("true"));
                    ar.id = UUID.fromString(tokens[3].trim());
                    ar.transcriptId = Long.parseLong(tokens[4].trim());
                    int length = 0;

                    traversal:
                    for (Node node : font.childNodes()) {
                        if (node instanceof TextNode) {
                            length += ((TextNode) node).text().length();
                        } else if ((node instanceof Element && ((Element) node).equals(span)) || (node.nodeName().equals("br"))) {
                            break;
                        } else if (node.nodeName().equals("span") && node.childNodeSize() <= 1) {
                            length += ((Element) node).text().length();
                        } else if (node.childNodeSize() > 1) {
                            for (Node n : node.childNodes()) {
                                if (n instanceof TextNode) {
                                    length += ((TextNode) n).text().length();
                                } else if ((n instanceof Element && ((Element) n).equals(span)) || (n.nodeName().equals("br"))) {
                                    break traversal;
                                } else if (n.nodeName().equals("span")) {
                                    length += ((Element) n).text().length();
                                }
                            }
                        }
                    }
                    ar.startCoordinate = (int) (Math.ceil(length)) - NAME_WIDTH;
                    if (ar.annotation.startsWith("Substitution")) {
                        ar.stopCoordinate = ar.startCoordinate + 1;
                    } else {
                        ar.stopCoordinate = ar.startCoordinate + ar.sequence.length();
                    }
                    if (!ids.contains(id)) {
                        ids.add(id);
                        cList.add(ar);
                    }
                }
            }
        }
        return cList;
    }

    private void refreshAnnotationsWithList(List<AnnotationResults> arList) {
        if (arList != null) {
            Collections.sort(arList);
            arList.stream().forEach((ar) -> {
                boolean dcm = false;
                if (ar.annotation.contains("__disease_causing_mutation")) {
                    ar.annotation = ar.annotation.split("__")[0];
                    dcm = true;
                }
                String tip = ar.annotation;
                tip = tip.trim();
                tip = tip.replaceAll("\"", "");
                String id = ar.toString();
                int frequency = Collections.frequency(arList, ar);
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
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    }
                } else if (ar.annotation.equals("Stop Codon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    }
                } else if (ar.annotation.equals("PolyA Signal")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    }
                } else if (ar.annotation.equals("Cleavage Site")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    }
                } else if (ar.annotation.equals("Alternative Cassette Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    }
                } else if (ar.annotation.equals("Alternative 5’ Splice Site Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    }
                } else if (ar.annotation.equals("Alternative 3’ Splice Site Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    }
                } else if (ar.annotation.equals("1st Shared Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    }
                } else if (ar.annotation.equals("Last Shared Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    }
                } else if (ar.annotation.equals("Mutually Exclusive Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    }
                } else if (ar.annotation.equals("Retained Intron 3’ UTR")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    }
                } else if (ar.annotation.equals("Retained Intron")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    }
                } else if (ar.annotation.equals("Start Site")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    }
                } else if (ar.annotation.equals("EPD_NEW")) {
                    if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 86, "#00BFFF", tip, id);
                        selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 86, (int) ar.stopCoordinate + 75, "#ADD8E6", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 124, "#ADD8E6", tip, id);
                        selectAndHighlightRange((int) ar.startCoordinate + 124, (int) ar.stopCoordinate + 75, "#00BFFF", tip, id);
                    }
                } else if (ar.annotation.equals("Substitution")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    }
                } else if (ar.annotation.equals("Transposition")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    }
                } else if (ar.annotation.equals("Deletion")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    }
                } else if (ar.annotation.equals("Insertion")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    }
                } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Ambion_SilencerSelect")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "::").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip, id);
                    }
                } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_ON-TARGETplus")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "::").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip, id);
                    }
                } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_siGENOME")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "::").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip, id);
                    }
                } else if (ar.annotation.startsWith("Affymetrix:")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip, id);
                    }
                } else if (frequency > 1) {
                    selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                } else if (ar.predicted) {
                    selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                } else if (dcm) {
                    selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                } else {
                    selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                }
            });
            cleanFontTags();
            setContentEditableFalse();
        }
    }

    private void refreshAnnotations() {
        try {
            clearAnnotationHighlights();
            List<AnnotationResults> arList = worker.getAnnotationsForTranscript(transcriptId);
            List<Long> singles = new ArrayList<>();
            arList.stream().filter((ar) -> (ar.stopCoordinate - ar.startCoordinate == 1)).forEach((ar) -> {
                singles.add(ar.startCoordinate);
            });
            singles.stream().forEach((l) -> {
                arList.stream().filter((ar) -> (ar.stopCoordinate - ar.startCoordinate > 1 && ar.startCoordinate == l)).map((ar) -> {
                    ar.startCoordinate++;
                    return ar;
                }).forEach((ar) -> {
                    ar.sequence = ar.sequence.substring(1, ar.sequence.length());
                });
            });
            Collections.sort(arList);
            arList.stream().forEach((ar) -> {
                boolean dcm = false;
                if (ar.annotation.contains("__disease_causing_mutation")) {
                    ar.annotation = ar.annotation.split("__")[0];
                    dcm = true;
                }
                String tip = ar.annotation;
                tip = tip.trim();
                tip = tip.replaceAll("\"", "");

                String id = ar.toString();
                int frequency = Collections.frequency(arList, ar);
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
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff00ff", tip, id);
                    }
                } else if (ar.annotation.equals("Stop Codon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#A1A1FF", tip, id);
                    }
                } else if (ar.annotation.equals("PolyA Signal")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#00ff00", tip, id);
                    }
                } else if (ar.annotation.equals("Cleavage Site")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e9967a", tip, id);
                    }
                } else if (ar.annotation.equals("Alternative Cassette Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#bcd9d3", tip, id);
                    }
                } else if (ar.annotation.equals("Alternative 5’ Splice Site Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d1dcf2", tip, id);
                    }
                } else if (ar.annotation.equals("Alternative 3’ Splice Site Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d5d1f2", tip, id);
                    }
                } else if (ar.annotation.equals("1st Shared Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#e6d1f2", tip, id);
                    }
                } else if (ar.annotation.equals("Last Shared Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#f2d1d8", tip, id);
                    }
                } else if (ar.annotation.equals("Mutually Exclusive Exon")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(165,112,255,0.5)", tip, id);
                    }
                } else if (ar.annotation.equals("Retained Intron 3’ UTR")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#D2B4DE", tip, id);
                    }
                } else if (ar.annotation.equals("Retained Intron")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(153,25,145,0.5)", tip, id);
                    }
                } else if (ar.annotation.equals("Start Site")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#F4D03F", tip, id);
                    }
                } else if (ar.annotation.equals("EPD_NEW")) {
                    if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 86, "#00BFFF", tip, id);
                        selectAndHighlightItalicizeUnderlineRange((int) ar.startCoordinate + 86, (int) ar.stopCoordinate + 75, "#ADD8E6", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.startCoordinate + 124, "#ADD8E6", tip, id);
                        selectAndHighlightRange((int) ar.startCoordinate + 124, (int) ar.stopCoordinate + 75, "#00BFFF", tip, id);
                    }
                } else if (ar.annotation.equals("Substitution")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#a3c2db", tip, id);
                    }
                } else if (ar.annotation.equals("Transposition")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ffcccc", tip, id);
                    }
                } else if (ar.annotation.equals("Deletion")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff0000", tip, id);
                    }
                } else if (ar.annotation.equals("Insertion")) {
                    if (dcm) {
                        selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    } else if (frequency > 1) {
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    } else if (ar.predicted) {
                        selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#ff7f00", tip, id);
                    }
                } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Ambion_SilencerSelect")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "::").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(211,221,189,0.8)", tip, id);
                    }
                } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_ON-TARGETplus")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "::").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(196,210,167,0.5)", tip, id);
                    }
                } else if (ar.annotation.startsWith("siRNA:") && ar.annotation.endsWith("Dharmacon_siGENOME")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "::").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "rgba(181,198,145,0.5)", tip, id);
                    }
                } else if (ar.annotation.startsWith("Affymetrix:")) {
                    if (frequency > 1) {
                        Set<AnnotationResults> dups = findDuplicates(arList, ar);
                        tip = "";
                        tip = dups.stream().map((_ar) -> _ar.annotation + "\\n").reduce(tip, String::concat);
                        id = "";
                        id = dups.stream().map((_ar) -> _ar.toString() + "::").reduce(id, String::concat);
                        selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip, id);
                    } else if (ar.strand.equals(Strand.REVERSECOMPLEMENT)) {
                        selectAndHighlightUnderlineRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip, id);
                    } else {
                        selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#DCEEFF", tip, id);
                    }
                } else if (frequency > 1) {
                    selectAndHighlightBoldRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                } else if (ar.predicted) {
                    selectAndHighlightItalicizeRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                } else if (dcm) {
                    selectAndHighlightBorderRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                } else {
                    selectAndHighlightRange((int) ar.startCoordinate + 75, (int) ar.stopCoordinate + 75, "#d3d3d3", tip, id);
                }
            });
            cleanFontTags();
            setContentEditableFalse();
        } catch (Exception e) {
            Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private <T> Set<T> findDuplicates(Collection<T> list, T object) {
        Set<T> duplicates = new LinkedHashSet<>();
        list.stream().filter((t) -> (t.equals(object))).forEach((t) -> {
            duplicates.add(t);
        });
        return duplicates;
    }

    private List<AnnotationResults> mergeSpans(List<AnnotationResults> arList) {
        Map<UUID, AnnotationResults> arMap = new HashMap<>();
        arList.stream().forEach((ar) -> {
            UUID id = ar.id;
            if (arMap.containsKey(id)) {
                AnnotationResults _ar = arMap.get(id);
                _ar.startCoordinate = Math.min(_ar.startCoordinate, ar.startCoordinate);
                _ar.stopCoordinate = Math.max(_ar.stopCoordinate, ar.stopCoordinate);
                arMap.put(id, _ar);
            } else {
                arMap.put(id, ar);
            }
        });
        return new ArrayList<>(arMap.values());
    }

    private <T> List<T> removeDuplicates(Collection<T> list) {
        List<T> unique = new ArrayList<>();
        list.stream().filter((t) -> (!unique.contains(t))).forEach((t) -> {
            unique.add(t);
        });
        return unique;
    }

    private void clearAnnotationHighlights() {
        Runnable runnable = () -> {
            String script
                    = "document.designMode = \"on\";"
                    + "var sel = window.getSelection();"
                    + "sel.collapse(document.body, 0);"
                    + "var spans = document.getElementsByTagName(\"SPAN\"), i;"
                    + "for( i=0; i<spans.length; i++) {"
                    + "var container = spans[i].parentNode;\n"
                    + "     var text = spans[i].innerText;\n"
                    + "     container.innerText += text;\n"
                    + "     container.removeChild(spans[i]);"
                    + "}"
                    + "sel.collapseToEnd();"
                    + "document.designMode = \"off\";";

            b.mainFrame().get().executeJavaScript(script);
        };

        try {
            FXUtilities.runAndWait(runnable);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EditorScene.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void selectAndHighlightRange(final int start, final int stop, final String hexColor, final String tip, final String id) {
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
                        + "function makeEditableAndHighlight(color, ttext, id) {\n"
                        + "    sel = window.getSelection();\n"
                        + "    document.designMode = \"on\";\n"
                        + "    document.execCommand('styleWithCSS', false, true);"
                        + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                        + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                        + "        document.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.id=id;\n"
                        + "    sel.collapseToEnd();\n"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext, id) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext, id);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext, id);\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\",\"" + id + "\");\n";
                
                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    public void selectAndHighlightItalicizeRange(final int start, final int stop, final String hexColor, final String tip, final String id) {
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
                        + "function makeEditableAndHighlight(colour, ttext, id) {\n"
                        + "    sel = window.getSelection();\n"
                        + "    document.designMode = \"on\";\n"
                        + "    document.execCommand('styleWithCSS', false, true);"
                        + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                        + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                        + "        document.execCommand(\"BackColor\", false, colour);\n"
                        + "    }\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.style.fontStyle = \"italic\";"
                        + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;"
                        + "    sel.getRangeAt(0).startContainer.parentNode.id=id;\n"
                        + "    sel.collapseToEnd();"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext, id) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext, id);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext, id)\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\",\"" + id + "\");\n";

                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    public void selectAndHighlightUnderlineRange(final int start, final int stop, final String hexColor, final String tip, final String id) {
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
                        + "function makeEditableAndHighlight(color, ttext, id) {\n"
                        + "    sel = window.getSelection();\n"
                        + "    document.designMode = \"on\";\n"
                        + "    document.execCommand('styleWithCSS', false, true);"
                        + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                        + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                        + "        document.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.style.textDecoration = \"underline\";"
                        + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;"
                        + "    sel.getRangeAt(0).startContainer.parentNode.id=id;\n"
                        + "    sel.collapseToEnd();"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext, id) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext, id);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext, id)\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\",\"" + id + "\");\n";

                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    public void selectAndHighlightItalicizeUnderlineRange(final int start, final int stop, final String hexColor, final String tip, final String id) {
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
                        + "function makeEditableAndHighlight(color, ttext, id) {\n"
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
                        + "    sel.getRangeAt(0).startContainer.parentNode.id=id;\n"
                        + "    sel.collapseToEnd();"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext, id) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext, id);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext, id)\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\",\"" + id + "\");\n";

                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    public void selectAndHighlightBoldRange(final int start, final int stop, final String hexColor, final String tip, final String id) {
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
                        + "function makeEditableAndHighlight(color, ttext, id) {\n"
                        + "    sel = window.getSelection();\n"
                        + "    document.designMode = \"on\";\n"
                        + "    document.execCommand('styleWithCSS', false, true);"
                        + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                        + "    if (!document.execCommand(\"HiliteColor\", false, color)) {\n"
                        + "        document.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.style.fontWeight = \"bold\";\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.id=id;\n"
                        + "    sel.collapseToEnd();\n"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext, id) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext, id);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext, id);\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\",\"" + id + "\");\n";

                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    public void selectAndHighlightBorderRange(final int start, final int stop, final String hexColor, final String tip, final String id) {
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
                        + "function makeEditableAndHighlight(color, ttext, id) {\n"
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
                        + "    sel.getRangeAt(0).startContainer.parentNode.id=id;\n"
                        + "    sel.collapseToEnd();\n"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext, id) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext, id);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext, id);\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}\n"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\",\"" + id + "\");\n";

                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    public void cleanFontTags() {
        Runnable runnable = () -> {
            String script
                    = "document.designMode = \"on\";"
                    + "var sel = window.getSelection();"
                    + "sel.collapse(document.body, 0);"
                    + "var font = document.getElementsByTagName(\"font\")[0], i;"
                    + "font.style.textDecoration = \"none\";"
                    + "font.title = \"\";"
                    + "font.id = \"\";"
                    + "font.border = \"none\";"
                    + "font.backgroundColor = \"transparent\";"
                    + "font.style.fontWeight = \"normal\";"
                    + "font.style.fontStyle = \"normal\";"
                    + "sel.collapseToEnd();"
                    + "document.designMode = \"off\";";

            b.mainFrame().get().executeJavaScript(script);
        };
        Platform.runLater(runnable);
    }

    private void setContentEditableFalse() {
        Platform.runLater(() -> {
            String script
                    = "document.designMode = \"on\";"
                    + "var sel = window.getSelection();"
                    + "sel.collapse(document.body, 0);"
                    + "var spans = document.getElementsByTagName(\"SPAN\"), i;"
                    + "for( i=0; i<spans.length; i++) {"
                    + "    spans[i].contentEditable = \"false\";\n"
                    + "}"
                    + "document.body.contentEditable = \"true\";\n"
                    + "sel.collapseToEnd();"
                    + "document.designMode = \"off\";";
            
            b.mainFrame().get().executeJavaScript(script);
        });
    }

    private void setContentEditableFalse(final String tip) {
        if (!tip.trim().equals("")) {
            Platform.runLater(() -> {
                String script
                        = "function setContentEditableFalse(el) {\n"
                        + "    document.designMode = \"on\";\n"
                        + "    el.contentEditable = \"false\";\n"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "setContentEditableFalse(document.getElementById(\"" + tip + "\"));\n";
                
                b.mainFrame().get().executeJavaScript(script);
            });
        }
    }

    private void selectAndSetContentEditableFalse(final int start, final int stop) {
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
                    + "}\n"
                    + "function setContentEditableFalse() {\n"
                    + "    var range, sel;\n"
                    + "    sel = window.getSelection();\n"
                    + "    document.designMode = \"on\";\n"
                    + "    var selectedText = sel.getRangeAt(0).extractContents();\n"
                    + "    var span= document.createElement(\"span\");\n"
                    + "    span.contentEditable = \"false\";\n"
                    + "    span.appendChild(selectedText);\n"
                    + "    sel.getRangeAt(0).insertNode(span);"
                    + "    sel.collapseToEnd();\n"
                    + "    document.designMode = \"off\";\n"
                    + "}\n"
                    + "\n"
                    + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start + ", " + stop + ");\n"
                    + "setContentEditableFalse();\n";
            
            b.mainFrame().get().executeJavaScript(script);
        });
    }

    /**
     *
     * @param name name to adjusted
     * @return adjusted name
     */
    private String adjustName(String name) {
        StringBuilder buffer = new StringBuilder();

        if (name.length() > NAME_WIDTH) {
            buffer.append(name.substring(0, NAME_WIDTH));
        } else {
            buffer.append(name);
            for (int j = buffer.length(); j < NAME_WIDTH; j++) {
                buffer.append(DOT);
            }
        }
        return buffer.toString();
    }

    private List<Interval> merge(List<Interval> intervals) {

        if (intervals.isEmpty() || intervals.size() == 1) {
            return intervals;
        }

        Collections.sort(intervals, new IntervalComparator());

        Interval first = intervals.get(0);
        int start = first.getStart();
        int end = first.getEnd();

        ArrayList<Interval> result = new ArrayList<>();

        for (int i = 1; i < intervals.size(); i++) {
            Interval current = intervals.get(i);
            if (current.getStart() <= end) {
                end = Math.max(current.getEnd(), end);
            } else {
                result.add(new Interval(start, end));
                start = current.getStart();
                end = current.getEnd();
            }
        }

        result.add(new Interval(start, end));
        return result;
    }

    class Interval {

        private int start;
        private int end;

        Interval() {
            start = 0;
            end = 0;
        }

        Interval(int s, int e) {
            start = s;
            end = e;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    class IntervalComparator implements Comparator<Interval> {

        public int compare(Interval i1, Interval i2) {
            return i1.getStart() - i2.getStart();
        }
    }
}
