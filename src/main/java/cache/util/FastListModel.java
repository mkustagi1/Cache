package cache.util;

import javax.swing.DefaultListModel;

/**
 *
 * @author Manjunath Kustagi
 */
/**

/**
 * This simple subclass of DefaultListModel allows one to disable list
 * notifications, e.g. while making a big change to the model. The
 * DefaultListModels fireXXX methods have been promoted from protected to public
 * in this class so that clients can fire an appropriate ListDataEvent after the
 * modifying the model with listeners disabled.
 */
class FastListModel extends DefaultListModel {

    private boolean listenersEnabled = true;

    public boolean getListenersEnabled() {
        return listenersEnabled;
    }

    public void setListenersEnabled(boolean enabled) {
        listenersEnabled = enabled;
    }

    @Override
    public void fireContentsChanged(Object source, int index0, int index1) {
        if (getListenersEnabled()) {
            super.fireContentsChanged(source, index0, index1);
        }
    }

    @Override
    public void fireIntervalAdded(Object source, int index0, int index1) {
        if (getListenersEnabled()) {
            super.fireIntervalAdded(source, index0, index1);
        }
    }

    @Override
    public void fireIntervalRemoved(Object source, int index0, int index1) {
        if (getListenersEnabled()) {
            super.fireIntervalAdded(source, index0, index1);
        }
    }
}