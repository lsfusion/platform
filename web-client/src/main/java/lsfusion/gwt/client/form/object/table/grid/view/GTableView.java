package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;

import java.util.*;

public interface GTableView {
    
    // components
    default Widget getThisWidget() { return (Widget)this; }

    // SETTERS
    // keys
    void setCurrentKey(GGroupObjectValue currentKey);
    void setKeys(ArrayList<GGroupObjectValue> keys);

    // columns
    void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values); // add or update
    void removeProperty(GPropertyDraw property);
    boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet); // assert alreadySet is true if there is no ordering in view

    // EXTRA SETTERS
    // keys
    void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values);
    void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values);
    
    // columns
    void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updatePropertyCaptions(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updateLoadings(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updatePropertyFooters(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updateShowIfValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values);
    void updateReadOnlyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values);
    void updateLastValues(GPropertyDraw property, int index, NativeHashMap<GGroupObjectValue, Object> values);

    // event - FINISH SETTER
    void update(Boolean updateState);

    // GETTERS (editing / toolbar features)
    // keys
    GGroupObjectValue getSelectedKey(); // editing

    // columns
    GPropertyDraw getCurrentProperty(); // calculate sum / filtering default value
    GGroupObjectValue getCurrentColumnKey(); // calculate sum / filtering default value

    boolean isNoColumns(); // hide if there're no columns after update
    long getSetRequestIndex(); // we need to understand that view was already updated, to avoid unnecessary effects (for example making grid invisible)
    void setSetRequestIndex(long index); // we need to understand that view was already updated, to avoid unnecessary effects (for example making grid invisible)

    // focus
    void focusProperty(GPropertyDraw propertyDraw);
    default void focus() {
        getFocusHolderElement().focus();
    }
    default Element getFocusHolderElement() { return getThisWidget().getElement(); }; // protected

    // add / delete
    int getSelectedRow();
    void modifyGroupObject(GGroupObjectValue key, boolean add, int position);

    // toolbar features
    void runGroupReport();
    List<Pair<Column, String>> getSelectedColumns(); // for filter to get all columns with keys and captions
    Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey); // for filter to set default value

    boolean hasUserPreferences();
    boolean containsProperty(GPropertyDraw property); // for user preferences
    LinkedHashMap<GPropertyDraw, Boolean> getUserOrders(List<GPropertyDraw> propertyDrawList);
    GGroupObjectUserPreferences getCurrentUserGridPreferences();
    GGroupObjectUserPreferences getGeneralGridPreferences();

    int getPageSize();

    default void updateRecordLayout(long requestIndex) {}

    default void onRender(Event editEvent){}
    default void onClear(){}

    default boolean isDefaultBoxed() { return true; }
}
