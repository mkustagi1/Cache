package cache.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Control;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineBuilder;
import javax.swing.JPanel;
import cache.AlignmentForm;
import java.awt.GridLayout;

/**
 *
 * @author Manjunath Kustagi
 */
public class JavaFXMultimapperChart extends JPanel {
    
    JavaFXBrowserJXBrowser browserPanel;
    private final JFXPanel jfxPanel = new JFXPanel();
    final NumberAxis xAxis = new NumberAxis();
    final NumberAxis yAxis = new NumberAxis();
    public final AreaChart<Number, Number> areaChart
            = new AreaChart<>(xAxis, yAxis);
    private ChartLegend chartLegend;
    private Line LV1;
    
    public JavaFXMultimapperChart() {
        super();
        initComponents();
    }
    
    private void initComponents() {
        createScene();
        this.setLayout(new GridLayout(1, 1));
        jfxPanel.setPreferredSize(new Dimension(1100, 145));
        this.add(jfxPanel);
    }
    
    public void setBrowser(JavaFXBrowserJXBrowser b) {
        browserPanel = b;
    }
    
    public void setLineLocation(double fraction) {
        double x = xAxis.getDisplayPosition(fraction) + xAxis.getLayoutX() + xAxis.getTranslateX();
        LV1.setStartX(x);
        LV1.setEndX(x);
    }
    
    private void createScene() {
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            xAxis.setTickLabelsVisible(true);
            yAxis.setTickLabelsVisible(true);
            yAxis.setMinWidth(Control.USE_PREF_SIZE);
            yAxis.setPrefWidth(60);
            yAxis.setMaxWidth(Control.USE_PREF_SIZE);
            
            areaChart.setLegendVisible(false);
            areaChart.setTitle("Multimappers");
            areaChart.setTitleSide(Side.TOP);
            
            LV1 = LineBuilder.create()
                    .strokeWidth(1)
                    .opacity(0.6)
                    .stroke(Color.FORESTGREEN)
                    .build();
            StackPane stack = new StackPane();
            Pane glassPane = new Pane();
            glassPane.getChildren().add(LV1);
            glassPane.minWidthProperty().bind(areaChart.widthProperty());
            glassPane.minHeightProperty().bind(areaChart.heightProperty());
            glassPane.setMouseTransparent(true);
            LV1.endYProperty().bind(areaChart.heightProperty());
            stack.getChildren().addAll(areaChart, glassPane);
            
            areaChart.setMinSize(1100, 145);
            areaChart.setPrefSize(1100, 145);
            AnchorPane anchorPane = new AnchorPane();
            anchorPane.getChildren().add(stack);
            AnchorPane.setBottomAnchor(stack, 0.0);
            AnchorPane.setRightAnchor(stack, 0.0);
            AnchorPane.setTopAnchor(stack, 0.0);
            AnchorPane.setLeftAnchor(stack, 0.0);
            Scene scene = new Scene(anchorPane, 1100, 145);
            
            areaChart.getStylesheets().add("cache/util/multimapperChart.css");
            jfxPanel.setScene(scene);
            EventHandler<MouseEvent> mouseHandler = (MouseEvent mouseEvent) -> {
                if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                    NumberAxis xAxis1 = (NumberAxis) areaChart.getXAxis();
                    Number xValue = xAxis1.getValueForDisplay(mouseEvent.getX() - xAxis1.getLayoutX() - xAxis1.getTranslateX());
                    setAlignmentChartLocation(xValue);
                } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                    double x1 = mouseEvent.getX();
                    Axis xAxis2 = areaChart.getXAxis();
                    Axis yAxis1 = areaChart.getYAxis();
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
                        areaChart.setCursor(Cursor.CROSSHAIR);
                    } else {
                        areaChart.setCursor(Cursor.DEFAULT);
                    }
                }
            };
            areaChart.setOnMouseClicked(mouseHandler);
            areaChart.setOnMouseMoved(mouseHandler);
        });
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
    
    List<Integer> coverageMultimappers;
    List<Integer> coverageRCMultimappers;
    Map<Integer, List<Long>> multiMappers;
    Map<Integer, List<Long>> rcMultiMappers;
    
    public List<Integer> getMultimapperCoverage() {
        return coverageMultimappers;
    }
    
    public List<Integer> getRCMultimapperCoverage() {
        return coverageRCMultimappers;
    }
    
    public void computeMultimappers(Map<Integer, List<Long>> mmm, Map<Integer, List<Long>> rmm) {
        System.out.println("jfxmc1: " + System.currentTimeMillis());
        boolean logTransform = false;
        coverageMultimappers = new ArrayList<>(mmm.size());
        XYChart.Series<Number, Number> series0 = new XYChart.Series();
        multiMappers = mmm;
        SortedSet<Integer> keys = new TreeSet<>(mmm.keySet());
        for (Integer key : keys) {
            int value = mmm.get(key).size();
            value = (value < 2) ? 0 : mmm.get(key).size();
            if (!logTransform) {
                logTransform = value > 128;
            }
            coverageMultimappers.add(key, value);
        }
        System.out.println("jfxmc2: " + System.currentTimeMillis());
        coverageRCMultimappers = new ArrayList<>(rmm.size());
        XYChart.Series<Number, Number> series1 = new XYChart.Series();
        rcMultiMappers = rmm;
        keys = new TreeSet<>(rmm.keySet());
        for (Integer key : keys) {
            int value = rmm.get(key).size();
            value = (value < 2) ? 0 : rmm.get(key).size();
            if (!logTransform) {
                logTransform = value > 128;
            }
            coverageRCMultimappers.add(key, -value);
        }
        System.out.println("jfxmc3: " + System.currentTimeMillis());
        
        GeometryFactory gf = new GeometryFactory();
        
        Coordinate[] coordinates1 = new Coordinate[mmm.size()];
        for (int i = 0; i < coverageMultimappers.size(); i++) {
            double value = coverageMultimappers.get(i).doubleValue();
            if (logTransform) {
                if (value < 2) {
                    value = 0;
                } else {
                    value = Math.log(value) / Math.log(2);
                }
            }
            coordinates1[i] = new Coordinate(i, value);
        }
        Geometry geom = new LineString(new CoordinateArraySequence(coordinates1), gf);
        Geometry simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        List<Data<Number, Number>> update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<Data<Number, Number>> list1 = FXCollections.observableArrayList(update);
        
        Coordinate[] coordinates2 = new Coordinate[mmm.size()];
        for (int i = 0; i < coverageRCMultimappers.size(); i++) {
            double value = coverageRCMultimappers.get(i).doubleValue();
            if (logTransform) {
                if ((-value) < 2) {
                    value = 0;
                } else {
                    value = -Math.log(-value) / Math.log(2);
                }
            }
            coordinates2[i] = new Coordinate(i, value);
        }
        geom = new LineString(new CoordinateArraySequence(coordinates2), gf);
        simplified = DouglasPeuckerSimplifier.simplify(geom, 0.00001);
        update = new ArrayList<>();
        for (Coordinate each : simplified.getCoordinates()) {
            update.add(new Data<>((Number) each.x, (Number) each.y));
        }
        ObservableList<Data<Number, Number>> list2 = FXCollections.observableArrayList(update);
        
        System.out.println("jfxmc4: " + System.currentTimeMillis());
        series0.getData().addAll(list1);
        System.out.println("jfxmc5: " + System.currentTimeMillis());
        series1.getData().addAll(list2);
        System.out.println("jfxmc6: " + System.currentTimeMillis());
        series0.setName("Multimappers");
        System.out.println("jfxmc7: " + System.currentTimeMillis());
        series1.setName("Multimappers RC");
        System.out.println("jfxmc8: " + System.currentTimeMillis());
        areaChart.getData().add(series0);
        System.out.println("jfxmc9: " + System.currentTimeMillis());
        areaChart.getData().add(series1);
        System.out.println("jfxmc10: " + System.currentTimeMillis());
        applyMouseEvents(series0);
        System.out.println("jfxmc11: " + System.currentTimeMillis());
        applyMouseEvents(series1);
        System.out.println("jfxmc12: " + System.currentTimeMillis());
    }
    
    private void applyMouseEvents(final XYChart.Series<Number, Number> series) {
        
        final Node node = series.getNode();
        
        node.setOnMouseEntered((MouseEvent arg0) -> {
            node.setCursor(Cursor.HAND);
        });
        
        node.setOnMouseExited((MouseEvent arg0) -> {
            node.setCursor(Cursor.DEFAULT);
        });
        
        node.setOnMouseClicked((MouseEvent mouseEvent) -> {
            Number x1 = xAxis.getValueForDisplay(mouseEvent.getX());
            Number y1 = yAxis.getValueForDisplay(mouseEvent.getY());
            System.out.println("x: " + x1);
            System.out.println("y: " + y1);
            final List<Long> tids = new ArrayList<>();
            if (y1.intValue() >= 0) {
                tids.addAll((List<Long>) multiMappers.get(x1.intValue()));
            } else {
                tids.addAll((List<Long>) rcMultiMappers.get(x1.intValue()));
            }
            System.out.println("tids: " + tids);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            AlignmentForm.setSearchTranscripts(tids);
                        }
                    };
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AlignmentForm.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            thread.start();
        });
    }
}
