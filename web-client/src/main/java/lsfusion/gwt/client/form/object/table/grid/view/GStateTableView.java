package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.io.Serializable;
import java.util.*;

// view with state, without incremental updates
// flexpanel, since we need to add pagesize widget + attach it to handle events
public abstract class GStateTableView extends FlexPanel implements GTableView {

    protected final GFormController form;
    protected final GGridController grid;

    protected GFont font;

    protected GGroupObjectValue currentKey;

    private long setRequestIndex;

    protected List<GGroupObjectValue> keys;

    protected List<GPropertyDraw> properties = new ArrayList<>();
    protected List<List<GGroupObjectValue>> columnKeys = new ArrayList<>();
    protected List<Map<GGroupObjectValue, Object>> captions = new ArrayList<>();
    protected List<Map<GGroupObjectValue, Object>> values = new ArrayList<>();
    protected List<List<Map<GGroupObjectValue, Object>>> lastAggrs = new ArrayList<>();
    protected List<Map<GGroupObjectValue, Object>> readOnlys = new ArrayList<>();

    protected static class Column {
        public final GPropertyDraw property;
        public final GGroupObjectValue columnKey;

        public Column(GPropertyDraw property, GGroupObjectValue columnKey) {
            this.property = property;
            this.columnKey = columnKey;
        }
    }

    public final native JsArrayMixed clone(JsArrayMixed array) /*-{
        n = array.length;
        var clone = [];
        for(var i = 0; i < n; i++) {
            clone.push(array[i]);
        }
        return clone;
    }-*/;

    public GStateTableView(GFormController form, GGridController grid) {
        super(true);

        this.form = form;
        this.grid = grid;

//        setElement(DOM.createDiv());

        rerender = true;

        drawWidget = new DivWidget();
        addFill(drawWidget);

        initPageSizeWidget();
    }

    public void initPageSizeWidget() {
        FlexPanel messageAndButton = new FlexPanel();
        messageAndButton.addCentered(new Label(ClientMessages.Instance.get().formGridPageSizeHit(pageSize)));

        SimpleImageButton showAllButton = new SimpleImageButton(ClientMessages.Instance.get().formGridPageSizeShowAll());
        showAllButton.addClickHandler(event -> {
            pageSize = Integer.MAX_VALUE;
            this.grid.changePageSize(pageSize);
        });
        messageAndButton.addCentered(showAllButton);

        FlexPanel centeredMessageAndButton = new FlexPanel(true);
        centeredMessageAndButton.addCentered(messageAndButton);

        this.pageSizeWidget = centeredMessageAndButton;
        this.pageSizeWidget.setVisible(false);

        ResizableSimplePanel child = new ResizableSimplePanel();
        child.setWidget(this.pageSizeWidget);
        addStretched(child); // we need to attach pageSize widget to make it work
//
//        add(new ResizableSimplePanel(this.pageSizeWidget)); // we need to attach pageSize widget to make it work
    }

    private Widget drawWidget;
    protected Element getDrawElement() {
        return drawWidget.getElement();
    }

    private Widget pageSizeWidget;
    protected Widget getPageSizeWidget() {
        return pageSizeWidget;
    }

    protected Element getRecordElement() {
        Widget recordView = grid.recordView;
        if(recordView != null)
            return recordView.getElement();
        return null;
    }

    private boolean dataUpdated = false;

    @Override
    public void setCurrentKey(GGroupObjectValue currentKey) {
        this.currentKey = currentKey;

        dataUpdated = true;
    }

    private int pageSize = 1000;
    @Override
    public int getPageSize() {
        return pageSize;
    }

    protected boolean isPageSizeHit() {
        return keys != null && keys.size() == getPageSize();
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
                index = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), this.properties);

                this.captions.add(index, null);
                this.properties.add(index, property);
                this.columnKeys.add(index, null);
                this.values.add(index, null);
                this.readOnlys.add(index, null);

                List<Map<GGroupObjectValue, Object>> list = new ArrayList<>();
                for (int i = 0; i < property.lastReaders.size(); i++)
                    list.add(null);
                lastAggrs.add(index, list);
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
    public void updateLastValues(GPropertyDraw property, int index, Map<GGroupObjectValue, Object> values) {
        this.lastAggrs.get(properties.indexOf(property)).set(index, values);

        dataUpdated = true;
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        int index = properties.indexOf(property);
        properties.remove(index);
        columnKeys.remove(index);
        captions.remove(index);
        lastAggrs.remove(index);
        values.remove(index);
        readOnlys.remove(index);

        dataUpdated = true;
    }

    private boolean rerender;
    protected void rerender() {
        rerender = true;
    }
    private boolean updateState;

    protected void updateView(boolean dataUpdated, Boolean updateState) {
        if(updateState != null)
            this.updateState = updateState;

        if(dataUpdated || rerender) {
            updatePageSizeState(isPageSizeHit());

            updateView();
            rerender = false;
        }

        updateRendererState(this.updateState); // update state with server response
    }
    protected abstract void updateView();
    protected void updatePageSizeState(boolean hit) {
        getPageSizeWidget().setVisible(hit);
    }
    protected abstract void updateRendererState(boolean set);

    @Override
    public void update(Boolean updateState) {
        updateView(dataUpdated, updateState);

        dataUpdated = false;
    }

    @Override
    public boolean isNoColumns() {
        return properties.isEmpty();
    }

    @Override
    public long getSetRequestIndex() {
        return setRequestIndex;
    }

    @Override
    public void setSetRequestIndex(long index) {
        setRequestIndex = index;
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
        this.readOnlys.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
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
    public void runGroupReport(boolean toExcel) {
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
        return properties.indexOf(property) >= 0;
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

    protected void changeGroupObject(GGroupObjectValue value) {
        setCurrentKey(value);
        form.changeGroupObjectLater(grid.groupObject, value);
    }

    protected Object getValue(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        return values.get(properties.indexOf(property)).get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected void changeProperty(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey, Serializable newValue) {
        form.changeProperty(property, rowKey, columnKey, newValue, getValue(property, rowKey, columnKey));
    }

    protected boolean isReadOnly(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        if(property.isReadOnly())
            return true;

        Map<GGroupObjectValue, Object> readOnlyValues = readOnlys.get(properties.indexOf(property));
        if(readOnlyValues == null)
            return false;

        return readOnlyValues.get(GGroupObjectValue.getFullKey(rowKey, columnKey)) != null;
    }

    // utils

    protected JsArray<JavaScriptObject> convertToObjectsString(JsArray<JsArrayString> array) {
        JsArrayString columns = array.get(0);
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArrayString values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(columns.get(j), fromString(values.get(j)));
            }
            convert.push(object);
        }
        return convert;
    }

    protected JsArray<JavaScriptObject> convertToObjectsMixed(JsArray<JsArray<JavaScriptObject>> array) {
        JsArray<JavaScriptObject> columns = array.get(0); // actually strings
        JsArray<JavaScriptObject> convert = JavaScriptObject.createArray().cast();
        for(int i=1;i<array.length();i++) {
            WrapperObject object = JavaScriptObject.createObject().cast();
            JsArray<JavaScriptObject> values = array.get(i);
            for(int j=0;j<columns.length();j++) {
                object.putValue(toString(columns.get(j)), values.get(j));
            }
            convert.push(object);
        }
        return convert;
    }

    static class WrapperObject extends JavaScriptObject {
        protected WrapperObject() {
        }

        protected native final JsArrayString getKeys() /*-{
            return Object.keys(this);
        }-*/;
        protected native final JsArrayString getArrayString(String string) /*-{
            return this[string];
        }-*/;
        protected native final JsArrayMixed getArrayMixed(String string) /*-{
            return this[string];
        }-*/;
        protected native final void putValue(String key, JavaScriptObject object) /*-{
            this[key] = object;
        }-*/;
        protected native final JavaScriptObject getValue(String key) /*-{
            return this[key];
        }-*/;
    }

    protected native final JavaScriptObject getValue(JavaScriptObject object, String key) /*-{
            return object[key];
    }-*/;

    protected native final JavaScriptObject fromString(String string) /*-{
        return string;
    }-*/;
    protected native final String toString(JavaScriptObject string) /*-{
        return string;
    }-*/;
    protected native final JavaScriptObject fromNumber(double d) /*-{
        return d;
    }-*/;
    protected native final double toNumber(JavaScriptObject d) /*-{
        return d;
    }-*/;
    protected native final JavaScriptObject fromBoolean(boolean b) /*-{
        return b;
    }-*/;
    protected native final boolean toBoolean(JavaScriptObject b) /*-{
        return b;
    }-*/;
    protected native final <T> JavaScriptObject fromObject(T object) /*-{
        return object;
    }-*/;
    protected native final <T> T toObject(JavaScriptObject object) /*-{
        return object;
    }-*/;
}
