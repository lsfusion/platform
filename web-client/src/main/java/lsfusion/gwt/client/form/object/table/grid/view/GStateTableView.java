package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.DivWidget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.SimpleImageButton;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;

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
    protected List<NativeHashMap<GGroupObjectValue, Object>> captions = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> values = new ArrayList<>();
    protected List<List<NativeHashMap<GGroupObjectValue, Object>>> lastAggrs = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> readOnlys = new ArrayList<>();
    protected List<NativeHashMap<GGroupObjectValue, Object>> showIfs = new ArrayList<>();

    protected boolean checkShowIf(int property, GGroupObjectValue columnKey) {
        NativeHashMap<GGroupObjectValue, Object> propertyShowIfs = showIfs.get(property);
        return propertyShowIfs != null && propertyShowIfs.get(columnKey) == null;
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
        Label messageLabel = new Label(ClientMessages.Instance.get().formGridPageSizeHit(pageSize));
        messageLabel.getElement().getStyle().setPaddingRight(4, Style.Unit.PX);
        messageAndButton.addCentered(messageLabel);

        SimpleImageButton showAllButton = new SimpleImageButton(ClientMessages.Instance.get().formGridPageSizeShowAll());
        showAllButton.addClickHandler(event -> {
            pageSize = Integer.MAX_VALUE / 10; // /10 to prevent Integer overflow because in GroupObjectInstance we use "pageSize * 2"
            this.grid.changePageSize(pageSize);
        });
        messageAndButton.addCentered(showAllButton);

        FlexPanel centeredMessageAndButton = new FlexPanel(true);
        centeredMessageAndButton.addCentered(messageAndButton);
        centeredMessageAndButton.getElement().getStyle().setPadding(2, Style.Unit.PX);

        this.pageSizeWidget = centeredMessageAndButton;
        this.pageSizeWidget.setVisible(false);

        ResizableSimplePanel child = new ResizableSimplePanel();
        child.setWidget(this.pageSizeWidget);
        addStretched(child); // we need to attach pageSize widget to make it work
//
//        add(new ResizableSimplePanel(this.pageSizeWidget)); // we need to attach pageSize widget to make it work
    }

    private final Widget drawWidget;
    protected Element getDrawElement() {
        return drawWidget.getElement();
    }

    private Widget pageSizeWidget;
    protected Widget getPageSizeWidget() {
        return pageSizeWidget;
    }

    private boolean dataUpdated = false;

    @Override
    public void setCurrentKey(GGroupObjectValue currentKey) {
        setCurrentKey(currentKey, true);
    }

    private void setCurrentKey(GGroupObjectValue currentKey, boolean rendered) {
        this.currentKey = currentKey;
        if (!rendered)
            dataUpdated = true;
    }

    // should correspond FormInstance.constructor - changePageSize method
    private int pageSize = 1000;
    public int getPageSize() {
        return pageSize;
    }

    protected boolean isPageSizeHit() {
        return keys != null && keys.size() >= getPageSize();
    }

    @Override
    public void setKeys(ArrayList<GGroupObjectValue> keys) {
        this.keys = keys;

        dataUpdated = true;
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        int index = properties.indexOf(property);
        if(!updateKeys) {
            if(index < 0) {
                index = GwtSharedUtils.relativePosition(property, form.getPropertyDraws(), this.properties);

                this.captions.add(index, null);
                this.properties.add(index, property);
                this.columnKeys.add(index, null);
                this.values.add(index, null);
                this.readOnlys.add(index, null);
                this.showIfs.add(index, null);

                List<NativeHashMap<GGroupObjectValue, Object>> list = new ArrayList<>();
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
    public void updatePropertyCaptions(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values) {
        this.captions.set(properties.indexOf(property), values);

        dataUpdated = true;
    }

    @Override
    public void updatePropertyFooters(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    @Override
    public void updateLastValues(GPropertyDraw property, int index, NativeHashMap<GGroupObjectValue, Object> values) {
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
        showIfs.remove(index);

        dataUpdated = true;
    }

    private boolean rerender;
    protected void rerender() {
        rerender = true;
    }
    private boolean updateState;

    public void updateView(boolean dataUpdated, Boolean updateState) {
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
    protected abstract Element getRendererAreaElement();

    private long lastRendererDropped = 0;
    protected void updateRendererState(boolean set) {
        Runnable setFilter = () -> setOpacity(set, getRendererAreaElement());

        if(set) {
            long wasRendererDropped = lastRendererDropped;
            Scheduler.get().scheduleFixedDelay(() -> {
                if(wasRendererDropped == lastRendererDropped) // since set and drop has different timeouts
                    setFilter.run();
                return false;
            }, (int) MainFrame.updateRendererStateSetTimeout);
        } else {
            lastRendererDropped++;
            setFilter.run();
        }
    }

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
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateCellBackgroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateCellForegroundValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateImageValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {

    }

    @Override
    public void updateShowIfValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> values) {
        this.showIfs.set(properties.indexOf(property), values);

        this.dataUpdated = true;
    }

    @Override
    public void updateReadOnlyValues(GPropertyDraw propertyDraw, NativeHashMap<GGroupObjectValue, Object> values) {
        this.readOnlys.set(properties.indexOf(propertyDraw), values);

        this.dataUpdated = true;
    }

    @Override
    public GGroupObjectValue getCurrentKey() {
        return currentKey; // for executing actions used for wysiwyg
    }

    protected boolean isCurrentKey(GGroupObjectValue object){
        return Objects.equals(object, getCurrentKey());
    }


    @Override
    public GPropertyDraw getCurrentProperty() {
        if(!properties.isEmpty())
            return properties.get(0);
        return null;
    }

    @Override
    public GGroupObjectValue getCurrentColumnKey() {
        if(!properties.isEmpty())
            return columnKeys.get(0).get(0);
        return null;
    }

    @Override
    public int getSelectedRow() {
        return -1;
    }

    @Override
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {

    }

    @Override
    public void runGroupReport() {
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return null;
    }

    @Override
    public List<Pair<Column, String>> getSelectedColumns() {
        List<Pair<Column, String>> result = new ArrayList<>();
        for(int i=0,size=properties.size();i<size;i++) {
            GPropertyDraw property = properties.get(i);
            NativeHashMap<GGroupObjectValue, Object> propertyCaptions = captions.get(i);
            List<GGroupObjectValue> columns = columnKeys.get(i);
            for (GGroupObjectValue column : columns)
                result.add(GGridPropertyTable.getSelectedColumn(propertyCaptions, property, column));
        }
        return result;
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

    protected long changeGroupObject(GGroupObjectValue value, boolean rendered) {
        setCurrentKey(value, rendered);
        return form.changeGroupObject(grid.groupObject, value);
    }

    protected Object getValue(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        return values.get(properties.indexOf(property)).get(GGroupObjectValue.getFullKey(rowKey, columnKey));
    }

    protected void changeProperty(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey, Serializable newValue) {
        form.changeProperty(property, rowKey, columnKey, newValue, getValue(property, rowKey, columnKey), null);
    }

    protected boolean isReadOnly(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey) {
        if(property.isReadOnly())
            return true;

        NativeHashMap<GGroupObjectValue, Object> readOnlyValues = readOnlys.get(properties.indexOf(property));
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

    protected static void setOpacity(boolean updateState, Element element) {
        if (updateState) {
            element.getStyle().setProperty("filter", "opacity(0.5)");
        } else {
            //there is a bug with position:fixed and opacity parameter
            element.getStyle().setProperty("filter", "");
        }
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
        protected native final JsArrayInteger getArrayInteger(String string) /*-{
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
