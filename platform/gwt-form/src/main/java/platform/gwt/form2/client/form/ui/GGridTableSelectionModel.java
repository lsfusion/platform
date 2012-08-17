package platform.gwt.form2.client.form.ui;

import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import platform.gwt.form2.shared.view.GridDataRecord;

public class GGridTableSelectionModel extends SelectionModel.AbstractSelectionModel<GridDataRecord> {

    private Object curKey;
    private GridDataRecord curSelection;

    // Pending selection change
    private boolean newSelected;
    private GridDataRecord newSelectedObject = null;
    private boolean newSelectedPending;

    public GGridTableSelectionModel() {
        super(null);
    }

    /**
     * Gets the currently-selected object.
     * @return the selected object
     */
    public GridDataRecord getSelectedRecord() {
        resolveChanges();
        return curSelection;
    }

    public boolean isSelected(GridDataRecord object) {
        resolveChanges();
        if (curSelection == null || curKey == null || object == null) {
            return false;
        }
        return curKey.equals(getKey(object));
    }

    public void setSelected(GridDataRecord object, boolean selected) {
        // If we are deselecting a value that isn't actually selected, ignore it.
        if (!selected) {
            Object oldKey = newSelectedPending ? getKey(newSelectedObject) : curKey;
            Object newKey = getKey(object);
            if (!equalsOrBothNull(oldKey, newKey)) {
                return;
            }
        }
        newSelectedObject = object;
        newSelected = selected;
        newSelectedPending = true;
        scheduleSelectionChangeEvent();
    }

    @Override
    protected void fireSelectionChangeEvent() {
        if (isEventScheduled()) {
            setEventCancelled(true);
        }
        resolveChanges();
    }

    private boolean equalsOrBothNull(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    private void resolveChanges() {
        if (!newSelectedPending) {
            return;
        }

        Object key = getKey(newSelectedObject);
        boolean sameKey = equalsOrBothNull(curKey, key);
        boolean changed = false;
        if (newSelected) {
            changed = !sameKey;
            curSelection = newSelectedObject;
            curKey = key;
        } else if (sameKey) {
            changed = true;
            curSelection = null;
            curKey = null;
        }

        newSelectedObject = null;
        newSelectedPending = false;

        // Fire a selection change event.
        if (changed) {
            SelectionChangeEvent.fire(this);
        }
    }
}
