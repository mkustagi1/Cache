package cache.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineBuilder;
import javafx.scene.text.Text;
import javax.swing.JPanel;

/**
 *
 * @author Manjunath Kustagi
 */
public class JavaFXBrowserChart extends JPanel {

    JavaFXBrowserJXBrowser browserPanel;
    private final JFXPanel jfxPanel = new JFXPanel();
    final NumberAxis xAxis = new NumberAxis();
    final NumberAxis yAxis = new NumberAxis();
    public final LineChart<Number, Number> lineChart
            = new LineChart<>(xAxis, yAxis);
    String name;
    private final DropShadow ds = new DropShadow();
    private Line LV1, LV2;
    private ChartLegend chartLegend;

    public JavaFXBrowserChart() {
        super();
        initComponents();
    }

    @Override
    public void setName(final String n) {
        this.name = n;
        Platform.runLater(() -> {
            lineChart.setTitle(name);
        });
    }

    public void setBrowser(JavaFXBrowserJXBrowser b) {
        browserPanel = b;
    }

    private void initComponents() {
        createScene();
        this.setLayout(new GridLayout(1, 1));
        jfxPanel.setPreferredSize(new Dimension(1100, 145));
        this.add(jfxPanel);
    }

    public void setChartLocation(Number oldValue, Number newValue) {
        System.out.println("OldLocation, newLocation: " + oldValue + ", " + newValue);
    }

    public void setLineLocation(double fraction) {
        double x = xAxis.getDisplayPosition(fraction) + xAxis.getLayoutX() + xAxis.getTranslateX();
        LV1.setStartX(x);
        LV1.setEndX(x);
    }

    private void createScene() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                xAxis.setTickLabelsVisible(true);
                yAxis.setTickLabelsVisible(true);
                yAxis.setMinWidth(Control.USE_PREF_SIZE);
                yAxis.setPrefWidth(60);
                yAxis.setMaxWidth(Control.USE_PREF_SIZE);

                lineChart.setLegendVisible(false);
                lineChart.setTitle("Coverage Plot");
                lineChart.setTitleSide(Side.TOP);
                lineChart.setCreateSymbols(false);
                lineChart.setAnimated(false);

                LV1 = LineBuilder.create()
                        .strokeWidth(1)
                        .opacity(0.6)
                        .stroke(Color.FORESTGREEN)
                        .build();
                StackPane stack = new StackPane();
                Pane glassPane = new Pane();
                glassPane.getChildren().add(LV1);
                glassPane.minWidthProperty().bind(lineChart.widthProperty());
                glassPane.minHeightProperty().bind(lineChart.heightProperty());
                glassPane.setMouseTransparent(true);
                stack.getChildren().addAll(lineChart, glassPane);
                LV1.endYProperty().bind(lineChart.heightProperty());

                lineChart.setMinSize(1100, 145);
                lineChart.setPrefSize(1100, 145);
                AnchorPane anchorPane = new AnchorPane();
                anchorPane.getChildren().add(stack);
                AnchorPane.setBottomAnchor(stack, 0.0);
                AnchorPane.setRightAnchor(stack, 0.0);
                AnchorPane.setTopAnchor(stack, 0.0);
                AnchorPane.setLeftAnchor(stack, 0.0);
                Scene scene = new Scene(anchorPane, 1100, 145);

                lineChart.getStylesheets().add("cache/util/lineChart.css");
                jfxPanel.setScene(scene);
                EventHandler<MouseEvent> mouseHandler = (MouseEvent mouseEvent) -> {
                    if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                        NumberAxis xAxis1 = (NumberAxis) lineChart.getXAxis();
                        Number xValue = xAxis1.getValueForDisplay(mouseEvent.getX() - xAxis1.getLayoutX() - xAxis1.getTranslateX());
                        setAlignmentChartLocation(xValue);
                    } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                        double x1 = mouseEvent.getX();
                        Axis xAxis2 = lineChart.getXAxis();
                        Axis yAxis1 = lineChart.getYAxis();
                        final double min = getSceneShift(xAxis2);
                        final double max = min + xAxis2.getWidth();
                        boolean setCrosshair = false;
                        if (x1 > min && x1 < min + xAxis2.getWidth()) {
                            LV1.setStartX(x1);
                            LV1.setEndX(x1);
                            setCrosshair = true;
                        } else if (x1 <= min) {
                            LV1.setStartX(min);
                            LV1.setEndX(min);
                        } else if (x1 >= max) {
                            LV1.setStartX(max);
                            LV1.setEndX(max);
                        }
                        if (setCrosshair) {
                            lineChart.setCursor(Cursor.CROSSHAIR);
                        } else {
                            lineChart.setCursor(Cursor.DEFAULT);
                        }
                    }
                };
                lineChart.setOnMouseClicked(mouseHandler);
                lineChart.setOnMouseMoved(mouseHandler);
            } catch (Exception ex) {
                Logger.getLogger(JavaFXBrowserChart.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    public void logTransformPlots() {
        System.out.println("jfxbc10: " + System.currentTimeMillis());
        seriesNameMap.keySet().stream().forEach((key) -> {
            XYChart.Series<Number, Number> series = seriesNameMap.get(key);
            if (key.contains("RC")) {
                series.getData().stream().forEach((d) -> {
                    double value = -d.getYValue().doubleValue();
                    if (value <= Math.E) {
                        value = Math.E;
                    }
                    d.setYValue(-Math.log(value));
                });
                logMap.put(series, Boolean.TRUE);

            } else {
                series.getData().stream().forEach((d) -> {
                    double value = d.getYValue().doubleValue();
                    if (value <= Math.E) {
                        value = Math.E;
                    }
                    d.setYValue(Math.log(value));
                });
                logMap.put(series, Boolean.TRUE);
            }
        });
        System.out.println("jfxbc11: " + System.currentTimeMillis());
    }

    private double getSceneShift(Node node) {
        double shift = 0;
        do {
            shift += node.getLayoutX();
            node = node.getParent();
        } while (node != null);
        return shift;
    }

    void setAlignmentChartLocation(Number xx) {
        browserPanel.setAlignmentChartLocation(xx.doubleValue());
        browserPanel.repaint();
    }

    int transcriptLength = 0;
    List<Integer> coverageNonRedundant;
    List<Integer> coverageTotal;
    List<Integer> coverageMagentaTotal;
    List<Integer> coverageGreenTotal;
    List<Integer> coverageBlueTotal;
    List<Integer> coveragePurpleTotal;

    List<Integer> coverageNonRedundantRC;
    List<Integer> coverageTotalRC;
    List<Integer> coverageMagentaTotalRC;
    List<Integer> coverageGreenTotalRC;
    List<Integer> coverageBlueTotalRC;
    List<Integer> coveragePurpleTotalRC;

    public List<Integer> getNonRedundantCoverage() {
        return coverageNonRedundant;
    }

    public List<Integer> getTotalCoverage() {
        return coverageTotal;
    }

    public List<Integer> getMagentaTotalCoverage() {
        return coverageMagentaTotal;
    }

    public List<Integer> getGreenTotalCoverage() {
        return coverageGreenTotal;
    }

    public List<Integer> getBlueTotalCoverage() {
        return coverageBlueTotal;
    }

    public List<Integer> getPurpleTotalCoverage() {
        return coveragePurpleTotal;
    }

    public List<Integer> getNonRedundantCoverageRC() {
        return coverageNonRedundantRC;
    }

    public List<Integer> getTotalCoverageRC() {
        return coverageTotalRC;
    }

    public List<Integer> getMagentaTotalCoverageRC() {
        return coverageMagentaTotalRC;
    }

    public List<Integer> getGreenTotalCoverageRC() {
        return coverageGreenTotalRC;
    }

    public List<Integer> getBlueTotalCoverageRC() {
        return coverageBlueTotalRC;
    }

    public List<Integer> getPurpleTotalCoverageRC() {
        return coveragePurpleTotalRC;
    }

    public void setCoverages(Map<String, List<Integer>> coverages) {
        System.out.println("jfxbc 1: " + System.currentTimeMillis());
        coverageNonRedundant = coverages.get("coverageNonRedundant");
        XYChart.Series<Number, Number> series0 = new XYChart.Series();
        coverageTotal = coverages.get("coverageTotal");
        XYChart.Series<Number, Number> series1 = new XYChart.Series();
        coverageMagentaTotal = coverages.get("coverageMagentaTotal");
        XYChart.Series<Number, Number> series2 = new XYChart.Series();
        coverageGreenTotal = coverages.get("coverageGreenTotal");
        XYChart.Series<Number, Number> series3 = new XYChart.Series();
        coverageBlueTotal = coverages.get("coverageBlueTotal");
        XYChart.Series<Number, Number> series4 = new XYChart.Series();
        coverageNonRedundantRC = coverages.get("coverageNonRedundantRC");
        XYChart.Series<Number, Number> series5 = new XYChart.Series();
        coverageTotalRC = coverages.get("coverageTotalRC");
        XYChart.Series<Number, Number> series6 = new XYChart.Series();
        coverageMagentaTotalRC = coverages.get("coverageMagentaTotalRC");
        XYChart.Series<Number, Number> series7 = new XYChart.Series();
        coverageGreenTotalRC = coverages.get("coverageGreenTotalRC");
        XYChart.Series<Number, Number> series8 = new XYChart.Series();
        coverageBlueTotalRC = coverages.get("coverageBlueTotalRC");
        XYChart.Series<Number, Number> series9 = new XYChart.Series();
        coveragePurpleTotal = coverages.get("coveragePurpleTotal");
        XYChart.Series<Number, Number> series10 = new XYChart.Series();
        coveragePurpleTotalRC = coverages.get("coveragePurpleTotalRC");
        XYChart.Series<Number, Number> series11 = new XYChart.Series();
        transcriptLength = coverageTotal.size();
        System.out.println("jfxbc 2: " + System.currentTimeMillis());

        GeometryFactory gf = new GeometryFactory();

        Coordinate[] coordinates0 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates0.length; i++) {
            coordinates0[i] = new Coordinate(i, coverageNonRedundant.get(i));
        }
        Geometry geom = new LineString(new CoordinateArraySequence(coordinates0), gf);
        Geometry simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        List<XYChart.Data<Number, Number>> update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list0 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates1 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates1.length; i++) {
            coordinates1[i] = new Coordinate(i, coverageTotal.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates1), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list1 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates2 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates2.length; i++) {
            coordinates2[i] = new Coordinate(i, coverageMagentaTotal.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates2), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list2 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates3 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates3.length; i++) {
            coordinates3[i] = new Coordinate(i, coverageGreenTotal.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates3), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list3 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates4 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates4.length; i++) {
            coordinates4[i] = new Coordinate(i, coverageBlueTotal.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates4), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list4 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates5 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates5.length; i++) {
            coordinates5[i] = new Coordinate(i, coverageNonRedundantRC.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates5), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list5 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates6 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates6.length; i++) {
            coordinates6[i] = new Coordinate(i, coverageTotalRC.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates6), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list6 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates7 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates7.length; i++) {
            coordinates7[i] = new Coordinate(i, coverageMagentaTotalRC.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates7), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list7 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates8 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates8.length; i++) {
            coordinates8[i] = new Coordinate(i, coverageGreenTotalRC.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates8), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list8 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates9 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates9.length; i++) {
            coordinates9[i] = new Coordinate(i, coverageBlueTotalRC.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates9), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list9 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates10 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates10.length; i++) {
            coordinates10[i] = new Coordinate(i, coveragePurpleTotal.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates10), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list10 = FXCollections.observableArrayList(update);

        Coordinate[] coordinates11 = new Coordinate[transcriptLength];
        for (int i = 0; i < coordinates11.length; i++) {
            coordinates11[i] = new Coordinate(i, coveragePurpleTotalRC.get(i));
        }
        geom = new LineString(new CoordinateArraySequence(coordinates11), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new XYChart.Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<XYChart.Data<Number, Number>> list11 = FXCollections.observableArrayList(update);

        System.out.println("jfxbc 3: " + System.currentTimeMillis());
        series0.getData().addAll(list0);
        series1.getData().addAll(list1);
        series2.getData().addAll(list2);
        series3.getData().addAll(list3);
        series4.getData().addAll(list4);
        series5.getData().addAll(list5);
        series6.getData().addAll(list6);
        series7.getData().addAll(list7);
        series8.getData().addAll(list8);
        series9.getData().addAll(list9);
        series10.getData().addAll(list10);
        series11.getData().addAll(list11);
        System.out.println("jfxbc 4: " + System.currentTimeMillis());
        series0.setName("NR");
        series1.setName("Total");
        series2.setName("CAGE");
        series3.setName("Bartel");
        series4.setName("Mayr");
        series5.setName("NR RC");
        series6.setName("Total RC");
        series7.setName("CAGE RC");
        series8.setName("Bartel RC");
        series9.setName("Mayr RC");
        series10.setName("TT-seq");
        series11.setName("TT-seq RC");
        lineChart.getData().add(series0);
        lineChart.getData().add(series5);
        lineChart.getData().add(series1);
        lineChart.getData().add(series6);
        lineChart.getData().add(series4);
        lineChart.getData().add(series9);
        lineChart.getData().add(series3);
        lineChart.getData().add(series8);
        lineChart.getData().add(series2);
        lineChart.getData().add(series7);
        lineChart.getData().add(series10);
        lineChart.getData().add(series11);
        logMap.put(series0, Boolean.FALSE);
        logMap.put(series1, Boolean.FALSE);
        logMap.put(series2, Boolean.FALSE);
        logMap.put(series3, Boolean.FALSE);
        logMap.put(series4, Boolean.FALSE);
        logMap.put(series5, Boolean.FALSE);
        logMap.put(series6, Boolean.FALSE);
        logMap.put(series7, Boolean.FALSE);
        logMap.put(series8, Boolean.FALSE);
        logMap.put(series9, Boolean.FALSE);
        logMap.put(series10, Boolean.FALSE);
        logMap.put(series11, Boolean.FALSE);
        seriesNameMap.put("NR", series0);
        seriesNameMap.put("Total", series1);
        seriesNameMap.put("CAGE", series2);
        seriesNameMap.put("Bartel", series3);
        seriesNameMap.put("Mayr", series4);
        seriesNameMap.put("MR RC", series5);
        seriesNameMap.put("Total RC", series6);
        seriesNameMap.put("CAGE RC", series7);
        seriesNameMap.put("Bartel RC", series8);
        seriesNameMap.put("Mayr RC", series9);
        seriesNameMap.put("TT-seq", series10);
        seriesNameMap.put("TT-seq RC", series11);
        System.out.println("jfxbc 5: " + System.currentTimeMillis());

        for (int i = 0; i < lineChart.getData().get(0).getData().size(); i++) {
            XYChart.Data<Number, Number> point = lineChart.getData().get(0).getData().get(i);
//            Text t = new Text();
//            t.setText(Double.toString(coverageNonRedundant.get(i)));
//            point.setNode(t);
            Tooltip.install(point.getNode(), new Tooltip(Double.toString(coverageNonRedundant.get(i))));
            System.out.println("Point: " + point.getNode());
        }
        for (int i = 0; i < series1.getData().size(); i++) {
            XYChart.Data<Number, Number> point = series1.getData().get(i);
            Tooltip.install(point.getNode(), new Tooltip(Double.toString(coverageTotal.get(i))));
        }
        for (int i = 0; i < series5.getData().size(); i++) {
            XYChart.Data<Number, Number> point = series5.getData().get(i);
            Tooltip.install(point.getNode(), new Tooltip(Double.toString(coverageNonRedundantRC.get(i))));
        }
        for (int i = 0; i < series6.getData().size(); i++) {
            XYChart.Data<Number, Number> point = series6.getData().get(i);
            Tooltip.install(point.getNode(), new Tooltip(Double.toString(coverageTotalRC.get(i))));
        }
    }

    Map<XYChart.Series<Number, Number>, Boolean> logMap = new HashMap<>();
    Map<String, XYChart.Series<Number, Number>> seriesNameMap = new HashMap<>();

    public boolean coverageNormalization = false;
}
