package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
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

public abstract class GPlatformTableController<P extends GGridProperty, V extends GTableView, B extends GToolbarController<?>> extends GPlatformPropertyController implements GTableController, GGroupController, GUserOrdersController {
    // the TABLE it draws, as its toolbar has it (GPlatformToolbarController.table): a list group's grid, or a tree, one
    // for all its groups
    protected final P table;
    // the view it draws the table's rows in - the list view a grid shows now, replaced as its switch asks, or the tree:
    // what the form sends for the table's rows and properties goes to it, each property's attribute with the property
    // (GTableView)
    protected V view;
    // ... and the table's toolbar, of whichever kind it is, which the table gives itself to as it is made, shown and
    // hidden with the rows (update)
    protected final B toolbar;
    // the input of the user filters of the table's groups - the grid's group, or the tree's, one FILTERS box for all of
    // them - the platform wires up, made over the table once it is made and bound to it then, once (bindFilter): what
    // the keys of the table and the table shown or hidden reach, and what the table tells that it has changed, once per
    // delta: the platform's over its panel, or the commands over the conditions a view draws - none where the platform
    // wires none up (GFormController.initializeTable)
    private GFilterInputController filter;

    protected TableContainer gridView;

    public void initGridView() {

        // we need to wrap into simple panel to make layout independent from property value (make flex-basis 0 for upper components)
        // plus we need this panel to change views
        this.gridView = new TableContainer(formController);

        getFormLayout().addBaseComponent(table, this.gridView, getDefaultFocusReceiver());
    }

    public void changeGridView(GTableView view) {
        gridView.changeTableComponent(view, table.isBoxed(view));
        gridView.updateElementClass(table);
    }

    public GPlatformTableController(GFormController formController, P table, B toolbar) {
        super(formController);
        this.table = table;
        this.toolbar = toolbar;
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
        view.updateOrders();
    }

    public void quickEditFilter(Event editEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.quickEditFilter(editEvent, propertyDraw, columnKey);
        }
    }

    // Alt+F3 and F3: the controls shown first, so the manual apply mode they bring holds what the key writes
    public void replaceFilter(Event event) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.setControlsVisible(true);

            filter.addCondition(event, true, false);
        }
    }

    public void addFilter(Event event) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.setControlsVisible(true);

            filter.addCondition(event, false, true);
        }
    }

    // the table's conditions replaced by these - a pivot's drill-down -, sent at once, whoever draws them
    // (GUserFiltersController.replaceConditions)
    public void replaceFilterConditions(ArrayList<GPropertyFilter> conditions) {
        formController.getUserFiltersController(table.getGroups().get(0)).replaceConditions(conditions);
    }

    public void resetFilters() {
        if (filter != null) {
            filter.resetConditions();
        }
    }

    // eventually is called either on form opening / form tab selection / filter dialog close
    public boolean focusFirstWidget(FocusUtils.Reason reason) {
        if (view != null && GwtClientUtils.isShowing(view.getWidget())) {
            view.focus(reason);
            return true;
        }

        return false;
    }

    // вызов focus() у getFocusHolderElement() грида по какой-то причине приводит к подскролливанию нашего скролла
    // (если грид заключён в скролл и не влезает по высоте) до первого ряда таблицы, скрывая заголовок (видимо вызывается scrollIntoView(), 
    // который, кстати, продолжает вызываться и при последующих изменениях фокуса в IE).
    // поэтому крутим все скроллы-предки вверх при открытии формы.
    // неоднозначное решение, т.к. вовсе необязательно фокусный компонент находится вверху скролла, но пока должно хватать. 
    public void scrollToTop() {
        scrollToTop(table.container);
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

        toolbar.setVisible(visible);

        for (GGroupObject group : table.getGroups())
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
        view.onClear();
        view.destroy();
        if (filter != null) // ... and the filter input controller given to it, let go of with it
            filter.destroy();
    }

    // ===== the platform's side of the form's controller of the table's properties (GPlatformPropertyController): each
    // into the view, a reader's values with its property
    @Override
    public boolean isPropertyShown(GPropertyDraw property) {
        return view.containsProperty(property);
    }

    @Override
    public void focusProperty(GPropertyDraw propertyDraw) {
        view.focusProperty(propertyDraw);
    }

    // its keys: the view it shows now takes them - the grid, as a column's (GTableView.getBindingWidget)
    @Override
    public Widget getBindingWidget(GPropertyDraw property) {
        return view.getBindingWidget(property);
    }
    @Override
    public void onBinding(GPropertyDraw property, Event event) {
        view.onBinding(property, event);
    }

    @Override
    public void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateProperty(property, columnKeys, updateKeys, values);
    }

    @Override
    public Pair<GGroupObjectValue, PValue> setLoadingValueAt(GPropertyDraw property, GGroupObjectValue fullCurrentKey, PValue value) {
        return view.setLoadingValueAt(property, fullCurrentKey, value);
    }

    @Override
    public void removeProperty(GPropertyDraw property) {
        view.removeProperty(property);
    }

    @Override
    public void updateCellGridElementClasses(GGridElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCellGridElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellValueElementClasses(GValueElementClassReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCellValueElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellFontValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCellFontValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCellBackgroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCellForegroundValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePlaceholderValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updatePlaceholderValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePatternValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updatePatternValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateRegexpValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateRegexpValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateRegexpMessageValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateRegexpMessageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateTooltipValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateValueTooltipValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateValueTooltipValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyCustomOptionsValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updatePropertyCustomOptionsValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateChangeKeyValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateChangeKeyValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateChangeMouseValues(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateChangeMouseValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateImageValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updatePropertyCaptions(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellCaptionElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCaptionElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateCellFooterElementClasses(GExtraPropReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateFooterElementClasses(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateLoadings(GLoadingReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateLoadings(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateShowIfValues(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updatePropertyFooters(formController.getProperty(reader.propertyID), values);
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateReadOnlyValues(formController.getProperty(reader.propertyID), values);
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
        view.updateLastValues(formController.getProperty(reader.propertyID), reader.index, values);
    }

    // ... and of the table's rows, a reader of its groups' (GGroupObjectPropertyReader)
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateRowBackgroundValues(values);
    }

    public void updateRowSelectValues(NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
        view.updateRowSelectValues(values, updateKeys);
    }

    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateRowForegroundValues(values);
    }

    public void updateCustomOptionsValues(NativeHashMap<GGroupObjectValue, PValue> values) {
        view.updateCustomOptionsValues(values);
    }

    // ===== the platform's side of the form's controller of a group whose rows it draws (GGroupController, of the grid
    // and the tree): its keys, which each kind lays out its own way, its current one and the rows added and removed into
    // the view, the selected one asked of it - none, a view's null, the empty key, as the form reads them alike -; and
    // a reader of the group by that reader's own update of this controller. A current object the view chose and a node
    // the tree opened it has shown itself (GGroupController's defaults)
    public abstract void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc, int requestIndex);
    public void updateCurrentKey(GGroupObjectValue currentKey) {
        view.setCurrentKey(currentKey);
    }
    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        view.modifyGroupObject(key, add, position);
    }
    public GGroupObjectValue getSelectedKey() {
        GGroupObjectValue result = view.getSelectedKey();
        return result == null ? GGroupObjectValue.EMPTY : result;
    }
    public int getSelectedRow() {
        return view.getSelectedRow();
    }
    public void updateAttribute(GGroupObjectPropertyReader reader, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
        reader.updatePlatform(this, values, partial);
    }
}
