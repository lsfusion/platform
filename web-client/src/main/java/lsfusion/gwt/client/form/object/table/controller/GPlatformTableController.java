package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GFilterInputController;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.object.table.view.GTableView;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public abstract class GPlatformTableController extends GPlatformPropertyController implements GTableController, GGroupController, GUserOrdersController {
    // the controller of the FILTERS box of the table's groups - the grid's group, or the tree's, one box for all of
    // them - the platform wires up, made over the table once it is made and bound to it then, once (bindFilter): what
    // the keys of the table, a pivot's drill-down and the table shown or hidden reach, and what the table tells that it
    // has changed, once per delta: the platform's panel, or the commands over the conditions a view draws - none where
    // the platform wires none up (GFormController.initializeTable)
    private GFilterInputController filter;

    protected TableContainer gridView;

    public void initGridView() {

        // we need to wrap into simple panel to make layout independent from property value (make flex-basis 0 for upper components)
        // plus we need this panel to change views
        this.gridView = new TableContainer(formController);

        getFormLayout().addBaseComponent(getGridComponent(), this.gridView, getDefaultFocusReceiver());
    }

    public void changeGridView(GTableView view) {
        GGridProperty table = getGridComponent();
        gridView.changeTableComponent(view, table.isBoxed(view));
        gridView.updateElementClass(table);
    }

    public GPlatformTableController(GFormController formController) {
        super(formController);
    }

    protected DefaultFocusReceiver getDefaultFocusReceiver() {
        return (FocusUtils.Reason reason) -> {
            boolean focused = focusFirstWidget(reason);
            if (focused) {
                scrollToTop();
            }
            return focused;
        };
    }

    public void bindFilter(GFilterInputController filter) {
        assert this.filter == null;
        this.filter = filter;
    }
    // what it is to the form for the table's groups (GTableController): the controller of each group, and what shows
    // their user orders - itself
    public GGroupController getGroupController(GGroupObject group) {
        return this;
    }
    public GUserOrdersController getUserOrdersController() {
        return this;
    }
    public GFilterInputController getFilter() {
        return filter;
    }

    // ===== what shows the group's user orders, on the platform's side (GUserOrdersController): a header only asks for
    // orders, and shows the ones the form changes as the reported ones (updateOrders, the view's)
    public void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
        updateOrders(group, orders);
    }
    public void updateOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders) {
        getView().updateOrders();
    }

    public void quickEditFilter(Event editEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.quickEditFilter(editEvent, propertyDraw, columnKey);
        }
    }

    public void replaceFilter(Event event) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.addCondition(event, true, false);

            filter.setControlsVisible(true);
        }
    }

    public void addFilter(Event event) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.addCondition(event, false, true);
            
            filter.setControlsVisible(true);
        }
    }

    // the group's conditions replaced by these - a pivot's drill-down: handed to its filter input controller - the
    // panel sends them as its rows, the commands over a view's conditions as they are -, or, with none, sent as they
    // are, which what shows the group's filters is told of (changeFilter)
    public void replaceFilterConditions(ArrayList<GPropertyFilter> conditions) {
        if (filter != null)
            filter.replaceConditions(conditions);
        else
            changeFilter(conditions);
    }

    public void resetFilters() {
        if (filter != null) {
            filter.resetConditions();
        }
    }

    // the conditions sent for the table's groups - the grid's group, or the tree's, one list for all of them
    public long changeFilter(ArrayList<GPropertyFilter> conditions) {
        return formController.changeFilters(getGridComponent().getGroups(), conditions);
    }

    // eventually is called either on form opening / form tab selection / filter dialog close
    public boolean focusFirstWidget(FocusUtils.Reason reason) {
        GTableView view = getView();
        if (view != null && GwtClientUtils.isShowing(view.getWidget())) {
            view.focus(reason);
            return true;
        }

        return false;
    }
    public abstract GGridProperty getGridComponent();
    // the view it draws the table's rows in - the list view a grid shows now, or the tree: what the form sends for
    // the table's rows and properties goes to it, each property's attribute with the property (GTableView)
    protected abstract GTableView getView();
    // ... and the table's toolbar, of whichever kind it is, shown and hidden with the rows (update)
    protected abstract GToolbarController<?> getToolbar();

    // вызов focus() у getFocusHolderElement() грида по какой-то причине приводит к подскролливанию нашего скролла
    // (если грид заключён в скролл и не влезает по высоте) до первого ряда таблицы, скрывая заголовок (видимо вызывается scrollIntoView(), 
    // который, кстати, продолжает вызываться и при последующих изменениях фокуса в IE).
    // поэтому крутим все скроллы-предки вверх при открытии формы.
    // неоднозначное решение, т.к. вовсе необязательно фокусный компонент находится вверху скролла, но пока должно хватать. 
    public void scrollToTop() {
        GComponent gridComponent = getGridComponent();
        if (gridComponent != null) {
            scrollToTop(gridComponent.container);
        }
    }

    private void scrollToTop(GContainer container) {
        if (container != null) {
            Element childElement = getFormLayout().getContainerView(container).getView().getElement().getFirstChildElement();
            if (childElement != null && childElement.getScrollTop() != 0) {
                childElement.setScrollTop(0);
            }
            scrollToTop(container.container);
        }
    }

    // ===== what the form asks of every table's rows (GTableController): the delta applied, its index and its changes
    // given - the view, then what is shown with it: the toolbar, the filters of the table's groups and its filter input
    // controller -; the form closing - what the view keeps registered let go of, and its filter input controller; and
    // the platform's controller of the table's list properties - itself, which draws their columns
    public void update(long requestIndex, GFormChanges fc) {
        boolean visible = updateView(requestIndex, fc);

        GwtClientUtils.setGridVisible(gridView, visible);

        getToolbar().setVisible(visible);

        for (GGroupObject group : getGridComponent().getGroups())
            formController.setFiltersVisible(group, visible);

        if (filter != null) {
            filter.update();
            filter.setVisible(visible);
        }
    }
    // ... the view updated by a delta, and whether it is shown then
    protected abstract boolean updateView(long requestIndex, GFormChanges fc);
    // the form is closing: the view is registered in MainFrame's static color-theme listener list, and from there it
    // keeps the whole form (the view holds this controller, and the controller holds the form controller). onClear is
    // where a custom view lets go of what its own render built - a third-party widget, a listener of its own - and
    // closing the form is the far more common way a view is dropped, so leaving it to the view switch meant the
    // application never got the call at all. Before the platform's own teardown, so what it sees is the state the view
    // switch shows it
    public void destroy() {
        GTableView view = getView();
        view.onClear();
        view.destroy();
        if (filter != null) // ... and the filter input controller given to it, let go of with it
            filter.destroy();
    }
    public GPlatformPropertyController getPlatformPropertyController() {
        return this;
    }

    // ===== the platform's side of the form's controller of the table's properties (GPlatformPropertyController): each
    // into the view, a reader's values with its property
    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return getView().containsProperty(property);
    }

    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {
        getView().focusProperty(propertyDraw);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateProperty(property, columnKeys, updateKeys, values);
    }

    @Override
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        return getView().setLoadingValueAt(property, fullCurrentKey, value);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        getView().removeProperty(property);
    }

    @Override
    public void updateCellGridElementClasses(GGridElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCellGridElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCellValueElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellFontValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCellFontValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCellBackgroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCellForegroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updatePlaceholderValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updatePatternValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateRegexpValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateRegexpMessageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateTooltipValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateValueTooltipValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyCustomOptionsValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updatePropertyCustomOptionsValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateChangeKeyValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateChangeKeyValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateChangeMouseValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateChangeMouseValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateImageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updatePropertyCaptions(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellCaptionElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCaptionElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellFooterElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateFooterElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateLoadings(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateShowIfValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updatePropertyFooters(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateReadOnlyValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyComments(GCommentReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateCellCommentElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateDefaultValueValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateLastValues(formController.getProperty(reader.propertyID), reader.index, values);
    }

    // ... and of the table's rows, a reader of its groups' (GGroupObjectPropertyReader)
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateRowBackgroundValues(values);
    }

    public void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        getView().updateRowSelectValues(values, updateKeys);
    }

    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateRowForegroundValues(values);
    }

    public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        getView().updateCustomOptionsValues(values);
    }

    // ===== the platform's side of the form's controller of a group whose rows it draws (GGroupController, of the grid
    // and the tree): its keys, which each kind lays out its own way, its current one and the rows added and removed into
    // the view, the selected one asked of it - none, a view's null, the empty key, as the form reads them alike -; a
    // current object the view chose itself and a node the tree opened itself; and a reader of the group by that
    // reader's own update of this controller
    public abstract void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex);
    public void updateCurrentKey(GGroupObjectValue currentKey) {
        getView().setCurrentKey(currentKey);
    }
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        getView().modifyGroupObject(key, add, position);
    }
    public GGroupObjectValue getSelectedKey() {
        GGroupObjectValue result = getView().getSelectedKey();
        return result == null ? GGroupObjectValue.EMPTY : result;
    }
    public int getSelectedRow() {
        return getView().getSelectedRow();
    }
    public void changeCurrentKey(GGroupObjectValue currentKey) {
    }
    public void changeExpanded(GGroupObjectValue key, boolean open, long requestIndex) {
    }
    public void changeExpandedAll(boolean open, long requestIndex) {
    }
    public void changeExpandedUnder(GGroupObjectValue key, long requestIndex) {
    }
    public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        reader.updatePlatform(this, values, partial);
    }
}
