package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.classes.data.GLogicalType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// view with state, without incremental updates
public abstract class GStateTableView extends Widget implements GTableView {

    protected final GFormController form;
    protected final GGridController grid;

    protected GGroupObjectValue currentKey;
    protected List<GGroupObjectValue> keys;
    protected List<GPropertyDraw> properties = new ArrayList<>();
    protected List<List<GGroupObjectValue>> columnKeys = new ArrayList<>();
    protected List<Map<GGroupObjectValue, Object>> captions = new ArrayList<>();
    protected List<Map<GGroupObjectValue, Object>> values = new ArrayList<>();

    public final native JsArrayString clone(JsArrayString array) /*-{
        n = array.length;
        var clone = [];
        for(var i = 0; i < n; i++) {
            clone.push(array[i]);
        }
        return clone;
    }-*/;

    public GStateTableView(GFormController form, GGridController grid) {
        this.form = form;
        this.grid = grid;

        setElement(DOM.createDiv());
    }

    private boolean dataUpdated = false;

    @Override
    public void setCurrentKey(GGroupObjectValue currentKey) {
        this.currentKey = currentKey;

        dataUpdated = true;
    }

    @Override
    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.keys = keys;

        dataUpdated = true;
    }

    @Override
    public void updateProperty(GPropertyDraw property, List<GGroupObjectValue> columnKeys, boolean updateKeys, HashMap<GGroupObjectValue, Object> values) {
        int index = properties.indexOf(property);
        if(!updateKeys) {
            if(index < 0) {
                index = properties.size();
                properties.add(property);
                this.columnKeys.add(null);
                captions.add(null);
                this.values.add(null);
            }
            this.columnKeys.set(index, columnKeys);
        } else
            assert index >= 0;
        this.values.set(index, values);

        dataUpdated = true;
    }

    @Override
    public void updatePropertyCaptions(GPropertyDraw property, Map<GGroupObjectValue, Object> values) {
        this.captions.set(properties.indexOf(property), values);

        dataUpdated = true;
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        int index = properties.indexOf(property);
        properties.remove(index);
        columnKeys.remove(index);
        captions.remove(index);
        values.remove(index);

        dataUpdated = true;
    }

    protected abstract void updateView(boolean dataUpdated, Boolean updateState);

    @Override
    public void update(Boolean updateState) {
        updateView(dataUpdated, updateState);

        dataUpdated = false;
    }

    @Override
    public boolean isNoColumns() {
        return properties.isEmpty();
    }

    // ignore for now
    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {
        
    }

    @Override
    public boolean changePropertyOrders(LinkedHashMap<GPropertyDraw, Boolean> value, boolean alreadySet) {
        return false;
    }

    @Override
    public void updateRowBackgroundValues(Map<GGroupObjectValue, Object> values) {
        
    }

    @Override
    public void updateRowForegroundValues(Map<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateShowIfValues(GPropertyDraw property, Map<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateReadOnlyValues(GPropertyDraw propertyDraw, Map<GGroupObjectValue, Object> values) {

    }

    @Override
    public GGroupObjectValue getCurrentKey() {
        return currentKey; // for executing actions used for wysiwyg
    }

    @Override
    public GPropertyDraw getCurrentProperty() {
        if(!properties.isEmpty())
            return properties.get(0);
        return null;
    }

    @Override
    public GGroupObjectValue getCurrentColumn() {
        if(!properties.isEmpty())
            return columnKeys.get(0).get(0);
        return null;
    }

    @Override
    public int getKeyboardSelectedRow() {
        return -1;
    }

    @Override
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {

    }

    @Override
    public void groupChange() {

    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return null;
    }

    @Override
    public boolean hasUserPreferences() {
        return false;
    }

    @Override
    public boolean containsProperty(GPropertyDraw property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders(List<GPropertyDraw> propertyDrawList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GGroupObjectUserPreferences getCurrentUserGridPreferences() {
        return null;
    }

    @Override
    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return null;
    }

    @Override
    public void beforeHiding() {

    }

    @Override
    public void afterShowing() {

    }

    @Override
    public void afterAppliedChanges() {

    }
}
