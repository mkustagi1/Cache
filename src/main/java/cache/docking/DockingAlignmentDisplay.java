package cache.docking;

import bibliothek.gui.DockController;
import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.SplitDockStation;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGrid;
import bibliothek.gui.dock.common.CStation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.event.CVetoClosingEvent;
import bibliothek.gui.dock.common.event.CVetoClosingListener;
import bibliothek.gui.dock.common.intern.ui.CSingleParentRemover;
import bibliothek.gui.dock.event.DockStationAdapter;
import bibliothek.gui.dock.util.ComponentWindowProvider;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * @author Manjunath Kustagi
 */
public class DockingAlignmentDisplay extends JPanel {

    ComponentWindowProvider windows = new ComponentWindowProvider(this);
    CControl controller = new CControl(windows);
    final List<DefaultSingleCDockable> dockables = new ArrayList<>();

    public DockingAlignmentDisplay() {
        super();
        initComponents();
    }

    public void addDisplay(String title, JComponent panel) {
        DefaultSingleCDockable dockable = new DefaultSingleCDockable(title, title);
        dockable.setTitleText(title);
        dockable.setCloseable(true);
        dockable.add(panel);

        boolean contains = false;
        for (int i = 0; i < controller.getCDockableCount(); i++) {
            SingleCDockable cd = (SingleCDockable) controller.getCDockable(i);
            if (cd.getUniqueId().equals(dockable.getUniqueId())) {
                contains = true;
            }
        }
        if (!contains) {
            if (!dockables.contains(dockable)) {
                dockables.add(dockable);
            }
            controller.addDockable(dockable);
        }

        CGrid grid = new CGrid(controller);
        dockables.stream().forEach((dock) -> {
            grid.add(this.getX(), this.getY(), this.getWidth(), this.getHeight(), dock);
        });

        controller.getContentArea().deploy(grid);
    }

    protected final void initComponents() {
        try {
            this.add(controller.getContentArea());

            controller.addVetoClosingListener(new CVetoClosingListener() {

                @Override
                public void closing(CVetoClosingEvent cvce) {
                }

                @Override
                public void closed(CVetoClosingEvent cvce) {
                    for (int i = 0; i < cvce.getDockableCount(); i++) {
                        dockables.remove(cvce.getDockable(i));
                        controller.removeDockable((SingleCDockable) cvce.getDockable(i));
                    }
                }
            });

            controller.getController().getHierarchyLock().setHardExceptions(false);

            CStation screen = controller.getStation(CControl.EXTERNALIZED_STATION_ID);

            CStation stack = controller.getStation(CControl.CONTENT_AREA_STATIONS_ID);

            stack.getStation().getController().getHierarchyLock().setHardExceptions(false);

            controller.intern().getController().setSingleParentRemover(new CSingleParentRemover(controller) {

                @Override
                protected boolean shouldTest(DockStation station) {
                    if (station instanceof SplitDockStation) {
                        SplitDockStation split = (SplitDockStation) station;
                        if (split.getDockParent() instanceof ScreenDockStation) {
                            return split.getDockableCount() == 0;
                        }
                    }
                    return super.shouldTest(station);
                }
            });
        } catch (Exception e) {
        }
    }

    public static class ScreenDockStationListener extends DockStationAdapter {

        @Override
        public void dockableAdded(DockStation station, final Dockable dockable) {
            station.getController().getHierarchyLock().onRelease(() -> {
                checkAndReplace(dockable);
            });
        }
    };

    private static void checkAndReplace(Dockable dockable) {
        DockStation station = dockable.getDockParent();
        if (!(station instanceof ScreenDockStation)) {
            return;
        }

        SplitDockStation split = new SplitDockStation();

        DockController controller = station.getController();

        try {
            controller.freezeLayout();

            station.replace(dockable, split);
            split.drop(dockable);
        } finally {
            controller.meltLayout();
        }
    }
}
