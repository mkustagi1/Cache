package cache.docking.javadocking;

import com.javadocking.DockingManager;
import com.javadocking.dock.Position;
import com.javadocking.dock.TabDock;
import com.javadocking.dockable.DefaultDockable;
import com.javadocking.dockable.Dockable;
import com.javadocking.dockable.DockableState;
import com.javadocking.dockable.StateActionDockable;
import com.javadocking.dockable.action.DefaultDockableStateActionFactory;
import com.javadocking.drag.DragListener;
import com.javadocking.model.DefaultDockingPath;
import com.javadocking.model.DockingPath;
import com.javadocking.model.FloatDockModel;
import com.javadocking.visualizer.FloatExternalizer;
import com.javadocking.visualizer.LineMinimizer;
import com.javadocking.visualizer.SingleMaximizer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Manjunath Kustagi
 */
public class DockingAlignmentDisplay extends JPanel {

    TabDock dock = new TabDock();
    FloatDockModel dockModel;
    public static final int FRAME_WIDTH = 900;
    public static final int FRAME_HEIGHT = 650;

    public DockingAlignmentDisplay() {
        super(new BorderLayout());
//        this.add(dock, BorderLayout.CENTER);
    }

    public void addDisplay(String title, JComponent panel) {

        if (dockModel == null) {
            JFrame frame = new JFrame();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setPreferredSize(screenSize);
            frame.setSize(screenSize);
            frame.pack();

            Dimension pSize = panel.getPreferredSize();
            panel.setSize(pSize.width, screenSize.height);
            Dimension alias = new Dimension(pSize.width, (int) (0.8 * screenSize.height));
            panel.setPreferredSize(alias);
            panel.setSize(alias);

            dockModel = new FloatDockModel("workspace.dck");
            dockModel.addOwner("frame0", frame);
            DockingManager.setDockModel(dockModel);
            dockModel.addRootDock("totalDock", dock, frame);

            // Add an externalizer to the dock model.
            dockModel.addVisualizer("externalizer", new FloatExternalizer(frame), frame);

            // Create a minimizer.
            LineMinimizer minimizer = new LineMinimizer(dock);
            dockModel.addVisualizer("minimizer", minimizer, frame);

            // Create a maximizer.
            SingleMaximizer maximizer = new SingleMaximizer(minimizer);
            dockModel.addVisualizer("maximizer", maximizer, frame);

            this.add(maximizer, BorderLayout.CENTER);
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        Dimension pSize = panel.getPreferredSize();
        panel.setSize(pSize.width, screenSize.height);
        Dimension alias = new Dimension(pSize.width, (int) (0.9 * screenSize.height));
        panel.setPreferredSize(alias);
        panel.setSize(alias);

        Dockable dockable = createDockable(title, (Component) panel, title, new ImageIcon(getClass().getResource("/com/javadocking/resources/images/text12.gif")), title);
        ((DefaultDockable) dockable).setPossibleStates(DockableState.CLOSED | DockableState.NORMAL | DockableState.MINIMIZED | DockableState.EXTERNALIZED);
        dockable = addAllActions(dockable);
        createDockableDragger(dockable);

        int count = dock.getDockableCount();
        dock.addDockable(dockable, new Position(count));
        addDockingPath(dockable);
    }

    /**
     * Creates a dockable for a given content component.
     *
     * @param id The ID of the dockable. The IDs of all dockables should be
     * different.
     * @param content The content of the dockable.
     * @param title The title of the dockable.
     * @param icon The icon of the dockable.
     * @return The created dockable.
     * @throws IllegalArgumentException If the given ID is null.
     */
    private Dockable createDockable(String id, Component content, String title, Icon icon, String description) {

        // Create the dockable.
        DefaultDockable dockable = new DefaultDockable(id, content, title, icon);

        // Add a description to the dockable. It will be displayed in the tool tip.
        dockable.setDescription(description);

        return dockable;

    }

    /**
     * Decorates the given dockable with all state actions.
     *
     * @param dockable The dockable to decorate.
     * @return The wrapper around the given dockable, with actions.
     */
    private Dockable addAllActions(Dockable dockable) {

        Dockable wrapper = new StateActionDockable(dockable, new DefaultDockableStateActionFactory(), DockableState.statesClosed());
        wrapper = new StateActionDockable(wrapper, new DefaultDockableStateActionFactory(), DockableState.statesAllExceptClosed());
        return wrapper;
    }

    /**
     * Decorates the given dockable with some state actions (not maximized).
     *
     * @param dockable The dockable to decorate.
     * @return The wrapper around the given dockable, with actions.
     */
    private Dockable addLimitedActions(Dockable dockable) {

        Dockable wrapper = new StateActionDockable(dockable, new DefaultDockableStateActionFactory(), DockableState.statesClosed());
        int[] limitStates = {DockableState.NORMAL, DockableState.MINIMIZED, DockableState.EXTERNALIZED};
        wrapper = new StateActionDockable(wrapper, new DefaultDockableStateActionFactory(), limitStates);
        return wrapper;

    }

    /**
     * Adds a drag listener on the content component of a dockable.
     */
    private void createDockableDragger(Dockable dockable) {

        // Create the dragger for the dockable.
        DragListener dragListener = DockingManager.getDockableDragListenerFactory().createDragListener(dockable);
        dockable.getContent().addMouseListener(dragListener);
        dockable.getContent().addMouseMotionListener(dragListener);

    }

    /**
     * Creates a docking path for the given dockable. It contains the
     * information how the dockable is docked now. The docking path is added to
     * the docking path model of the docking manager.
     *
     * @param dockable The dockable for which a docking path has to be created.
     * @return The docking path model. Null if the dockable is not docked.
     */
    private DockingPath addDockingPath(Dockable dockable) {

        if (dockable.getDock() != null) {
            // Create the docking path of the dockable.
            DockingPath dockingPath = DefaultDockingPath.createDockingPath(dockable);
            DockingManager.getDockingPathModel().add(dockingPath);
            return dockingPath;
        }
        return null;
    }
}
