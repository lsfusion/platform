package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.*;

public interface GTableView {
    
    // components
    default Widget getThisWidget() { return (Widget)this; }

    // SETTERS
    // keys
    void setCurrentKey(GGroupObjectValue currentKey);
    void setKeys(ArrayList<GGroupObjectValue> keys);

    // columns
    void updateProperty(GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, HashMap<GGroupObjectValue, Object> values); // add or update
    void removeProperty(GPropertyDraw property);
    boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet); // assert alreadySet is true if there is no ordering in view

    // EXTRA SETTERS
    // keys
    void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(Map<GGroupObjectValue, Object> values);
    
    // columns
    void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values);
    void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values);
    void updatePropertyCaptions(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values);
    void updateShowIfValues(GPropertyDraw property, Map<GGroupObjectValue, Object> values);
    void updateReadOnlyValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values);
    void updateLastValues(GPropertyDraw property, int index, Map<GGroupObjectValue, Object> values);

    // event - FINISH SETTER
    void update(Boolean updateState);

    // GETTERS (editing / toolbar features)
    // keys
    GGroupObjectValue getCurrentKey(); // editing

    // columns
    GPropertyDraw getCurrentProperty(); // calculate sum / filtering default value
    GGroupObjectValue getCurrentColumn(); // calculate sum / filtering default value

    boolean isNoColumns(); // hide if there're no columns after update    

    // focus
    void focusProperty(GPropertyDraw propertyDraw);
    default void focus() {
        getFocusHolderElement().focus();
    }
    default Element getFocusHolderElement() { return getThisWidget().getElement(); }; // protected

    // add / delete
    int getKeyboardSelectedRow();
    void modifyGroupObject(GGroupObjectValue key, boolean add, int position);

    // toolbar features
    void groupChange();
    void runGroupReport(boolean toExcel);
    Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey); // for filter to set default value

    boolean hasUserPreferences();
    boolean containsProperty(GPropertyDraw property); // for user preferences
    LinkedHashMap<GPropertyDraw, Boolean> getUserOrders(List<GPropertyDraw> propertyDrawList);
    GGroupObjectUserPreferences getCurrentUserGridPreferences();
    GGroupObjectUserPreferences getGeneralGridPreferences();

    // events
    void beforeHiding();
    void afterShowing();

    int getPageSize();
    boolean isGroup();
    GListViewType getViewType();
    
    void afterAppliedChanges(); // after apply changed
}
