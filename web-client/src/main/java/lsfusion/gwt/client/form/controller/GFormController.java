package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.*;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GFilterAction;
import lsfusion.gwt.client.action.GMessageAction;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.controller.remote.action.*;
import lsfusion.gwt.client.controller.remote.action.form.*;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateID;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateIDResult;
import lsfusion.gwt.client.controller.remote.action.navigator.GainedFocus;
import lsfusion.gwt.client.controller.remote.action.navigator.VoidFormAction;
import lsfusion.gwt.client.form.ContainerForm;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.dispatch.ExceptionResult;
import lsfusion.gwt.client.form.controller.dispatch.FormDispatchAsync;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.view.CustomContainerView;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.filter.user.controller.GFilterController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.*;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.controller.GPropertyController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.property.async.*;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.view.LogicalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.*;
import lsfusion.gwt.client.form.property.cell.view.*;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyContextMenuPopup;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GShowFormType;
import lsfusion.gwt.client.view.MainFrame;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.isChangeEvent;

public class GFormController implements EditManager {

    private static final ClientMessages messages = ClientMessages.Instance.get();

    private final FormDispatchAsync dispatcher;

    public int getDispatchPriority() {
        return dispatcher.dispatchPriority;
    }

    private final GFormActionDispatcher actionDispatcher;

    private final FormsController formsController;
    private final FormContainer formContainer;

    public final GForm form;
    public final GFormLayout formLayout;

    private final boolean isDialog;

    private Event editEvent;

    private final NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects = new NativeSIDMap<>();

    public NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> getCurrentGridObjects() {
        return currentGridObjects;
    }

    private final NativeSIDMap<GGroupObject, ArrayList<GPropertyFilter>> currentFilters = new NativeSIDMap<>();

    private final LinkedHashMap<GGroupObject, GGridController> controllers = new LinkedHashMap<>();
    private final LinkedHashMap<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();
    public GPanelController panelController;

    private final NativeSIDMap<GGroupObject, ArrayList<Widget>> filterViews = new NativeSIDMap<>();

    private final LinkedHashMap<Long, ModifyObject> pendingModifyObjectRequests = new LinkedHashMap<>();
    private final NativeSIDMap<GGroupObject, Long> pendingChangeCurrentObjectsRequests = new NativeSIDMap<>();
    private final NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>> pendingChangePropertyRequests = new NativeSIDMap<>(); // assert that should contain columnKeys + list keys if property is in list
    private final NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Long>> pendingLoadingPropertyRequests = new NativeSIDMap<>(); // assert that should contain columnKeys + list keys if property is in list
    private final NativeSIDMap<GFilterConditionView, Long> pendingLoadingFilterRequests = new NativeSIDMap<>();

    private boolean hasColumnGroupObjects;

    private boolean needConfirm;

    public FormsController getFormsController() {
        return formsController;
    }

    private final Set<ContainerForm> containerForms = new HashSet<>();

    public void addContainerForm(ContainerForm containerForm) {
        containerForms.add(containerForm);
    }

    public void removeContainerForm(ContainerForm containerForm) {
        containerForms.remove(containerForm);
    }

    private static int idCounter = 0;
    // we need the global id to make ids globally unique in some cases
    public String globalID;

    public GFormController(FormsController formsController, FormContainer formContainer, GForm gForm, boolean isDialog, int dispatchPriority, Event editEvent) {
        actionDispatcher = new GFormActionDispatcher(this);

        this.formsController = formsController;
        this.formContainer = formContainer;
        this.form = gForm;
        this.isDialog = isDialog;

        this.globalID = "" + (idCounter++);

        dispatcher = new FormDispatchAsync(this, dispatchPriority);

        formLayout = new GFormLayout(this, form.mainContainer);
        if (form.sID != null)
            formLayout.getElement().setAttribute("lsfusion-form", form.sID);

        this.editEvent = editEvent;

        initializeParams(); // has to be done before initializeControllers (since adding component uses getSize)

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        if (form.initialFormChanges != null) {
            applyRemoteChanges(form.initialFormChanges);
            form.initialFormChanges = null;
        } else
            getRemoteChanges();

        initializeUserOrders();

        initializeFormSchedulers();
    }

    public void checkGlobalMouseEvent(Event event) {
        checkFormEvent(event, (handler, preview) -> checkMouseEvent(handler, preview, false, false, false));
    }

    public Event popEditEvent() {
        Event result = editEvent;
        editEvent = null;
        return result;
    }

    public void checkFocusElement(boolean isFocused, Element renderElement) {
        focusedCustom = isFocused && renderElement != null && CellRenderer.isCustomElement(renderElement) ? renderElement : null;
    }

    private interface CheckEvent {
        void accept(EventHandler handler, boolean preview);
    }
    private static void checkFormEvent(Event event, CheckEvent preview) {
        EventHandler handler = new EventHandler(event);

        preview.accept(handler, true); // the problem is that now we check preview twice (however it's not that big overhead, so so far will leave it this way)
        if(handler.consumed)
            return;

        preview.accept(handler, false);
    }

    public void checkMouseEvent(EventHandler handler, boolean preview, boolean isCell, boolean panel, boolean stopPreventingDblclickEvent) {
        if(GMouseStroke.isDblDownEvent(handler.event) && !stopPreventingDblclickEvent && !isEditing())
            handler.event.preventDefault(); //need to prevent selection by double mousedown event
        else if(GMouseStroke.isChangeEvent(handler.event) || GMouseStroke.isDoubleChangeEvent(handler.event))
            processBinding(handler, preview, isCell, panel);
    }
    public void checkKeyEvent(EventHandler handler, boolean preview, boolean isCell, boolean panel) {
        if(GKeyStroke.isKeyEvent(handler.event))
            processBinding(handler, preview, isCell, panel);
    }
    private static void checkGlobalKeyEvent(DomEvent event, Supplier<GFormController> currentForm) {
        NativeEvent nativeEvent = event.getNativeEvent();
        if(nativeEvent instanceof Event) { // just in case
            GFormController form = currentForm.get();
            if(form != null)
                checkFormEvent((Event) nativeEvent, (handler, preview) -> form.checkKeyEvent(handler, preview, false, false));
        }
    }
    public void checkMouseKeyEvent(EventHandler handler, boolean preview, boolean isCell, boolean panel, boolean customRenderer) {
        if(MainFrame.isModalPopup())
            return;

        checkMouseEvent(handler, preview, isCell, panel, customRenderer);
        if(handler.consumed)
            return;

        checkKeyEvent(handler, preview, isCell, panel);
    }

    public static void checkKeyEvents(DomEvent event, FormsController formsController) {
        NativeEvent nativeEvent = event.getNativeEvent();
        formsController.checkEditModeEvents(nativeEvent);

        if(GKeyStroke.isSwitchFullScreenModeEvent(nativeEvent) && !MainFrame.mobile) {
            formsController.switchFullScreenMode();
        }
    }

    // will handle key events in upper container which will be better from UX point of view
    public static void initKeyEventHandler(Widget widget, FormsController formsController, Supplier<GFormController> currentForm) {
        widget.addDomHandler(event -> {
            checkGlobalKeyEvent(event, currentForm);
            checkKeyEvents(event, formsController);
        }, KeyDownEvent.getType());
        widget.addDomHandler(event -> checkGlobalKeyEvent(event, currentForm), KeyPressEvent.getType());
        widget.addDomHandler(event -> checkKeyEvents(event, formsController), KeyUpEvent.getType());
    }

    public GFormLayout getFormLayout() {
        return formLayout;
    }

    public boolean hasCanonicalName() {
        return form.canonicalName != null;
    }

    private void initializeRegularFilters() {
        for (final GRegularFilterGroup filterGroup : form.regularFilterGroups) {
            if (filterGroup.filters.size() == 1) {
                createSingleFilterComponent(filterGroup, filterGroup.filters.iterator().next());
            } else {
                createMultipleFilterComponent(filterGroup);
            }
        }
    }

    private void createSingleFilterComponent(final GRegularFilterGroup filterGroup, final GRegularFilter filter) {
        final CheckBox filterCheck = new FormCheckBox(filter.getFullCaption());
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                setRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        filterCheck.addStyleName("filter-group-check");
        addFilterView(filterGroup, filterCheck);

        if (filterGroup.defaultFilterIndex >= 0) {
            filterCheck.setValue(true, false);
        }

        setBindingGroupObject(filterCheck, filterGroup.groupObject);
        for (GInputBindingEvent bindingEvent : filter.bindingEvents) {
            addRegularFilterBinding(bindingEvent, (event) -> filterCheck.setValue(!filterCheck.getValue(), true), filterCheck, filterGroup.groupObject);
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox();
        filterBox.setMultipleSelect(false);
        filterBox.addItem("(" + messages.multipleFilterComponentAll() + ")", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            final GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.getFullCaption(), "" + i);

            final int filterIndex = i;
            GFormController.setBindingGroupObject(filterBox, filterGroup.groupObject);
            for(GInputBindingEvent bindingEvent : filter.bindingEvents) {
                addRegularFilterBinding(bindingEvent, (event) -> {
                    filterBox.setSelectedIndex(filterIndex + 1);
                    setRegularFilter(filterGroup, filterIndex);
                }, filterBox, filterGroup.groupObject);
            }
        }

        filterBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setRegularFilter(filterGroup, filterBox.getSelectedIndex() - 1);
            }
        });

        filterBox.setStyleName("filter-group-select");
        filterBox.addStyleName("form-select");

        addFilterView(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + 1);
        }
    }

    private void addFilterView(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.addBaseComponent(filterGroup, filterWidget, null);

        // need this to hide / show regular filters when group object is not visible
        if (filterGroup.groupObject != null)
            filterViews.computeIfAbsent(filterGroup.groupObject, k -> new ArrayList<>()).add(filterWidget);
    }

    public void setFiltersVisible(GGroupObject groupObject, boolean visible) {
        List<Widget> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null)
            for (Widget filterView : groupFilters)
                GwtClientUtils.setGridVisible(filterView, visible);
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, int index) {
        setRemoteRegularFilter(filterGroup, index == -1 ? null : filterGroup.filters.get(index));
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            initializeTreeController(treeGroup);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                initializeGroupController(group);
            }
        }

        panelController = new GPanelController(this);
    }

    public Pair<Widget, Boolean> getCaptionWidget() {
        return new Pair<>(formContainer.getCaptionWidget(), formContainer.async);
    }
    private void initializeParams() {
        hasColumnGroupObjects = false;
        for (GPropertyDraw property : getPropertyDraws()) {
            if (property.hasColumnGroupObjects()) {
                hasColumnGroupObjects = true;
            }

            GGroupObject groupObject = property.groupObject;
            if(groupObject != null && property.isList && !property.hide && groupObject.columnCount < 10) {
                GFont font = groupObject.grid.font;
                // in theory property renderers padding should be included, but it's hard to do that (there will be problems with the memoization)
                // plus usually there are no paddings for the property renderers in the table (td paddings are used, and they are included see the usages)
                groupObject.setColumnSumWidth(groupObject.getColumnSumWidth().add(property.getValueWidth(font, true, true)));
                groupObject.columnCount++;
                groupObject.setRowMaxHeight(groupObject.getRowMaxHeight().max(property.getValueHeight(font, true, true)));
            }
        }
    }

    private void initializeGroupController(GGroupObject group) {
        GGridController controller = new GGridController(this, group, form.userPreferences != null ? extractUserPreferences(form.userPreferences, group) : null);
        controllers.put(group, controller);
    }

    private void initializeTreeController(GTreeGroup treeGroup) {
        GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form);
        treeControllers.put(treeGroup, treeController);
    }

    private GGridUserPreferences[] extractUserPreferences(GFormUserPreferences formPreferences, GGroupObject groupObject) {
        if (formPreferences != null) {
            GGridUserPreferences[] gridPreferences = new GGridUserPreferences[2];
            gridPreferences[0] = findGridUserPreferences(formPreferences.getGroupObjectGeneralPreferencesList(), groupObject);
            gridPreferences[1] = findGridUserPreferences(formPreferences.getGroupObjectUserPreferencesList(), groupObject);
            return gridPreferences;
        }
        return null;
    }

    private GGridUserPreferences findGridUserPreferences(List<GGroupObjectUserPreferences> groupObjectUserPreferences, GGroupObject groupObject) {
        for (GGroupObjectUserPreferences groupPreferences : groupObjectUserPreferences) {
            if (groupObject.getSID().equals(groupPreferences.getGroupObjectSID())) {
                Map<GPropertyDraw, GColumnUserPreferences> columnPreferences = new HashMap<>();
                for (Map.Entry<String, GColumnUserPreferences> entry : groupPreferences.getColumnUserPreferences().entrySet()) {
                    GPropertyDraw property = form.getProperty(entry.getKey());
                    if (property != null) {
                        columnPreferences.put(property, entry.getValue());
                    }
                }
                return new GGridUserPreferences(groupObject, columnPreferences, groupPreferences.getFont(), groupPreferences.getPageSize(), groupPreferences.getHeaderHeight(), groupPreferences.hasUserPreferences());
            }
        }
        return null;
    }

    private Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> groupDefaultOrders() {
        Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> orders = new HashMap<>();
        for(Map.Entry<GPropertyDraw, Boolean> defaultOrder : form.defaultOrders.entrySet()) {
            GGroupObject groupObject = defaultOrder.getKey().groupObject;
            LinkedHashMap<GPropertyDraw, Boolean> order = orders.computeIfAbsent(groupObject, k -> new LinkedHashMap<>());
            order.put(defaultOrder.getKey(), defaultOrder.getValue());
        }
        return orders;
    }

    public void initializeDefaultOrders() {
        Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> defaultOrders = groupDefaultOrders();
        for(Map.Entry<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> entry : defaultOrders.entrySet()) {
            GGroupObject groupObject = entry.getKey();
            getGroupObjectController(groupObject).changeOrders(groupObject, entry.getValue(), true);
        }
    }

    public void initializeUserOrders() {
        boolean changed = false;
        for (GGridController controller : controllers.values()) {
            LinkedHashMap<GPropertyDraw, Boolean> objectUserOrders = controller.getUserOrders();
            if (objectUserOrders != null)
                changed = controller.changeOrders(objectUserOrders, false)  || changed;
        }
        if (changed)
            getRemoteChanges();
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders(GGroupObject groupObject) {
        return form.getDefaultOrders(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotColumns(GGroupObject groupObject) {
        return form.getPivotColumns(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotRows(GGroupObject groupObject) {
        return form.getPivotRows(groupObject);
    }

    public List<GPropertyDraw> getPivotMeasures(GGroupObject groupObject) {
        return form.getPivotMeasures(groupObject);
    }

    public void executeNotificationAction(final String notification) throws IOException {
        syncResponseDispatch(new ExecuteNotification(notification));
    }

    private void initializeFormSchedulers() {
        for(GFormScheduler formScheduler : form.formSchedulers) {
            scheduleFormScheduler(formScheduler);
        }
    }

    public Widget getWidget() {
        return formLayout;
    }

    private void scheduleFormScheduler(GFormScheduler formScheduler) {

        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (isVisible()) {
                    if (isShowing(getWidget()) && !MainFrame.isModalPopup()) {
                        executeFormEventAction(formScheduler, new ServerResponseCallback() {
                            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                                super.onSuccess(response, onDispatchFinished);
                                if (isVisible() && !formScheduler.fixed) {
                                    scheduleFormScheduler(formScheduler);
                                }
                            }
                        });

//                        if(formScheduler.fixed) {
//                            scheduleFormScheduler(formScheduler);
//                        }
                        return formScheduler.fixed;
                    } else {
                        return true;
                    }
                }
                return false;
            }
        }, formScheduler.period * 1000);
    }

    public void closePressed(EndReason reason) {
        GFormEventClose eventClose = new GFormEventClose(reason instanceof CommitReason);

        executeFormEventAction(eventClose, new ServerResponseCallback() {
            @Override
            protected Runnable getOnRequestFinished() {
                return () -> {
                    actionDispatcher.editFormCloseReason = null;
                };
            }

            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                actionDispatcher.editFormCloseReason = reason;
                super.onSuccess(response, onDispatchFinished);
            }
        });
    }

    private void executeFormEventAction(GFormEvent formEvent, ServerResponseCallback serverResponseCallback) {
        ExecuteFormEventAction executeFormEventAction = new ExecuteFormEventAction(formEvent);

        GAsyncExec asyncExec = getAsyncExec(form.asyncExecMap.get(formEvent));
        if (asyncExec != null) {
            asyncExec.exec(formsController, this, formContainer, editEvent, new GAsyncExecutor(actionDispatcher, pushAsyncResult -> {
                executeFormEventAction.pushAsyncResult = pushAsyncResult;
                return asyncDispatch(executeFormEventAction, serverResponseCallback);
            }));
        } else {
            syncDispatch(executeFormEventAction, serverResponseCallback);
        }
    }

    private GAsyncExec getAsyncExec(GAsyncEventExec asyncEventExec) {
        if(asyncEventExec instanceof GAsyncExec)
            return (GAsyncExec) asyncEventExec;
        return null;
    }

    public GPropertyDraw getProperty(int id) {
        return form.getProperty(id);
    }

    public GGroupObject getGroupObject(int groupID) {
        return form.getGroupObject(groupID);
    }

    public void getRemoteChanges() {
        getRemoteChanges(false);
    }

    public void getRemoteChanges(boolean forceLocalEvents) {
        asyncResponseDispatch(new GetRemoteChanges(forceLocalEvents));
    }

    private boolean formActive = true;

    public void gainedFocus() {
        asyncResponseDispatch(new GainedFocus());
        formActive = true;
    }

    public void lostFocus() {
        formActive = false;
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        applyRemoteChanges(GFormChanges.remap(form, changesDTO), changesDTO.requestIndex);
    }

    public void applyRemoteChanges(GFormChanges fc, int requestIndex) {
        if (hasColumnGroupObjects) // optimization
            fc.gridObjects.foreachEntry((key, value) -> currentGridObjects.put(key, value));

        modifyFormChangesWithModifyObjectAsyncs(requestIndex, fc);

        modifyFormChangesWithChangeCurrentObjectAsyncs(requestIndex, fc);

        modifyFormChangesWithChangePropertyAsyncs(requestIndex, fc);

        modifyFormChangesWithLoadingPropertyAsyncs(requestIndex, fc);

        applyLoadingFilterAsyncs(requestIndex, fc);

        applyKeyChanges(fc, requestIndex);

        applyPropertyChanges(fc);

        update(fc, requestIndex);

        expandCollapseContainers(fc);

        activateElements(fc);

        applyNeedConfirm(fc);

        formLayout.update(requestIndex);
    }

    public void applyKeyChanges(GFormChanges fc, int requestIndex) {
        fc.gridObjects.foreachEntry((key, value) ->
            getGroupObjectController(key).updateKeys(key, value, fc, requestIndex));

        fc.objects.foreachEntry((key, value) ->
            getGroupObjectController(key).updateCurrentKey(value));
    }

    private void applyPropertyChanges(GFormChanges fc) {
        fc.dropProperties.forEach(property -> {
            GPropertyController controller = getPropertyController(property);
            if (controller.isPropertyShown(property)) // drop properties sent without checking if it was sent for update at least once, so it's possible when drop is sent for property that has not been added
                controller.removeProperty(property);
        });

        // first proceed property with its values, then extra values (some views, for example GPivot use updated properties)
        updatePropertyChanges(fc, key -> !(key instanceof GExtraPropertyReader));
        updatePropertyChanges(fc, key -> key instanceof GExtraPropertyReader);
    }

    private void updatePropertyChanges(GFormChanges fc, Predicate<GPropertyReader> filter) {
        fc.properties.foreachEntry((key, value) -> {
            if(filter.test(key))
                key.update(this, value, key instanceof GPropertyDraw && fc.updateProperties.contains((GPropertyDraw)key));
        });
    }

    public void update(GFormChanges fc, int requestIndex) {
        for (GGridController controller : controllers.values())
            controller.update(requestIndex, fc);

        for (GTreeGroupController treeController : treeControllers.values())
            treeController.update();

        panelController.update();
    }

    private void activateElements(GFormChanges fc) {
        Scheduler.get().scheduleDeferred(() -> {
            for(GComponent component : fc.activateTabs)
                activateTab(component);

            for(GPropertyDraw propertyDraw : fc.activateProps)
                focusProperty(propertyDraw);
        });
    }

    private void applyNeedConfirm(GFormChanges fc) {
        needConfirm = fc.needConfirm;
    }

    private void expandCollapseContainers(GFormChanges formChanges) {
        for (GContainer container : formChanges.collapseContainers) {
            setContainerExtCollapsed(container, true);
        }

        for (GContainer container : formChanges.expandContainers) {
            setContainerExtCollapsed(container, false);
        }
    }

    private void setContainerExtCollapsed(GContainer container, boolean collapsed) {
        if (container.container != null) {
            GAbstractContainerView parentContainerView = formLayout.getContainerView(container.container);
            Widget childWidget = parentContainerView.getChildWidget(container);
            if (childWidget instanceof CollapsiblePanel) {
                ((CollapsiblePanel) childWidget).setCollapsed(collapsed);
            }
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(final int currentDispatchingRequestIndex, GFormChanges fc) {
        for (Iterator<Map.Entry<Long, ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            if (cell.getKey() <= currentDispatchingRequestIndex) {
                iterator.remove();

                ModifyObject modifyObject = cell.getValue();
                GGroupObject groupObject = modifyObject.object.groupObject;
                // делаем обратный modify, чтобы удалить/добавить ряды, асинхронно добавленные/удалённые на клиенте, если с сервера не пришло подтверждение
                // возможны скачки и путаница в строках на удалении, если до прихода ответа position утратил свою актуальность
                // по этой же причине не заморачиваемся запоминанием соседнего объекта
                if(!fc.gridObjects.containsKey(groupObject)) {
                    controllers.get(groupObject).modifyGroupObject(modifyObject.value, !modifyObject.add, modifyObject.position);
                }
            }
        }

        for (Iterator<Map.Entry<Long, ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            ModifyObject modifyObject = cell.getValue();
            ArrayList<GGroupObjectValue> gridObjects = fc.gridObjects.get(modifyObject.object.groupObject);
            if (gridObjects != null) {
                if (modifyObject.add) {
                    gridObjects.add(modifyObject.value);
                } else {
                    if(!gridObjects.remove(modifyObject.value)) { //could be removed in previous formChange (for example, two async groupChanges)
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(final long currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangeCurrentObjectsRequests.foreachEntry((group, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex)
                pendingChangeCurrentObjectsRequests.remove(group);
            else
                fc.objects.remove(group);
        });
    }

    private void modifyFormChangesWithChangePropertyAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangePropertyRequests.foreachEntry((property, values) -> values.foreachEntry((keys, change) -> {
            long requestIndex = change.requestIndex;
            if (requestIndex <= currentDispatchingRequestIndex) {

                removeFromDoubleMap(pendingChangePropertyRequests, property, keys);

                if(getPropertyController(property).isPropertyShown(property) && !fc.dropProperties.contains(property)) {
                    NativeHashMap<GGroupObjectValue, PValue> propertyValues = fc.properties.get(property);
                    if (propertyValues == null) {
                        // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                        propertyValues = new NativeHashMap<>();
                        fc.properties.put(property, propertyValues);
                        fc.updateProperties.add(property);
                    }

                    if (fc.updateProperties.contains(property) && !propertyValues.containsKey(keys)) {
                        propertyValues.put(keys, change.oldValue);
                    }
                }
            }
        }));

        pendingChangePropertyRequests.foreachEntry((property, values) -> {
            final NativeHashMap<GGroupObjectValue, PValue> propertyValues = fc.properties.get(property);
            if (propertyValues != null) {
                values.foreachEntry((key, change) -> {
                    propertyValues.put(key, change.newValue);
                });
            }
        });
    }

    private void modifyFormChangesWithLoadingPropertyAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingLoadingPropertyRequests.foreachEntry((property, values) -> values.foreachEntry((keys, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex) {

                removeFromDoubleMap(pendingLoadingPropertyRequests, property, keys);

                if(getPropertyController(property).isPropertyShown(property) && !fc.dropProperties.contains(property)) {
                    NativeHashMap<GGroupObjectValue, PValue> propertyLoadings = fc.properties.get(property.loadingReader);
                    if (propertyLoadings == null) {
                        propertyLoadings = new NativeHashMap<>();
                        fc.properties.put(property.loadingReader, propertyLoadings);
                    }
                    propertyLoadings.put(keys, null);
                }
            }
        }));
    }

    private void applyLoadingFilterAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingLoadingFilterRequests.foreachEntry((filter, requestIndex) -> {
            if (requestIndex <= currentDispatchingRequestIndex) {

                pendingLoadingFilterRequests.remove(filter);

                if(!filter.isRemoved)
                    filter.updateLoading(false);
            }
        });
    }

    public GAbstractTableController getGroupObjectController(GGroupObject group) {
        GGridController groupObjectController = controllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return treeControllers.get(group.parent);
    }

    public GPropertyController getPropertyController(GPropertyDraw property) {
        if(property.isList) {
            return getGroupObjectController(property.groupObject);
        } else
            return panelController;
    }

    public void openForm(Long requestIndex, GForm form, GShowFormType showFormType, boolean forbidDuplicate, Event editEvent, EditContext editContext, final WindowHiddenHandler handler, String formId) {
        boolean isDockedModal = showFormType.isDockedModal();
        if (isDockedModal)
            ((FormDockable)formContainer).block();

        FormContainer blockingForm = formsController.openForm(getAsyncFormController(requestIndex), form, showFormType, forbidDuplicate, editEvent, editContext, this, () -> {
            if(isDockedModal) {
                ((FormDockable)formContainer).unblock();

                formsController.selectTab((FormDockable) formContainer);
            } else if(showFormType.isDocked())
                formsController.ensureTabSelected();

            handler.onHidden();
        }, formId);

        if (isDockedModal)
            ((FormDockable)formContainer).setBlockingForm((FormDockable) blockingForm);
    }

    public void onServerInvocationResponse(ServerResponseResult response) {
        formsController.onServerInvocationResponse(response, getAsyncFormController(response.requestIndex));
    }

    public void onServerInvocationFailed(ExceptionResult exceptionResult) {
        formsController.onServerInvocationFailed(getAsyncFormController(exceptionResult.requestIndex));
        applyRemoteChanges(new GFormChanges(), (int) exceptionResult.requestIndex);
    }

    public long changeGroupObject(final GGroupObject group, GGroupObjectValue key) {
        long requestIndex = asyncResponseDispatch(new ChangeGroupObject(group.ID, key));
        pendingChangeCurrentObjectsRequests.put(group, requestIndex);
        return requestIndex;
    }

    // has to be called setCurrentKey before
    public void changeGroupObjectLater(final GGroupObject group, final GGroupObjectValue key) {
        DeferredRunner.get().scheduleGroupObjectChange(group, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changeGroupObject(group, key);
            }
        });
    }

    public void pasteExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, List<List<String>> table) {
        ArrayList<ArrayList<Object>> values = new ArrayList<>();
        ArrayList<ArrayList<String>> rawValues = new ArrayList<>();

        for (List<String> sRow : table) {
            ArrayList<Object> valueRow = new ArrayList<>();
            ArrayList<String> rawValueRow = new ArrayList<>();

            for (int i = 0, propertyColumns = propertyList.size(); i < propertyColumns; i++) {
                GPropertyDraw property = propertyList.get(i);
                String sCell = i < sRow.size() ? sRow.get(i) : null;

                GType externalType = property.getExternalChangeType();
                if(externalType == null)
                    externalType = property.getPasteType();
                valueRow.add(PValue.convertFileValueBack(property.parsePaste(sCell, externalType)));
                rawValueRow.add(sCell);
            }
            values.add(valueRow);
            rawValues.add(rawValueRow);
        }

        final ArrayList<Integer> propertyIdList = new ArrayList<>();
        for (GPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.ID);
        }

        syncResponseDispatch(new PasteExternalTable(propertyIdList, columnKeys, values, rawValues));
    }

    public void pasteValue(ExecuteEditContext editContext, String sValue) {
        GPropertyDraw property = editContext.getProperty();
        GType externalType = property.getExternalChangeType();
        if(externalType != null) {
            changeProperty(editContext, property.parsePaste(sValue, externalType), GEventSource.PASTE, null);
        } else {
            ArrayList<GPropertyDraw> propertyList = new ArrayList<>();
            propertyList.add(property);
            ArrayList<GGroupObjectValue> columnKeys = new ArrayList<>();
            columnKeys.add(editContext.getColumnKey());
            ArrayList<String> row = new ArrayList<>();
            row.add(sValue);
            List<List<String>> table = new ArrayList<>();
            table.add(row);

            pasteExternalTable(propertyList, columnKeys, table);
        }
    }

    public void changePageSizeAfterUnlock(final GGroupObject groupObject, final int pageSize) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (dispatcher.getBusyDialogDisplayer().isVisible()) {
                    return true;
                } else {
                    changePageSizeLater(groupObject, pageSize);
                    return false;
                }
            }
        }, 1000);
    }

    private void changePageSizeLater(final GGroupObject groupObject, final int pageSize) {
        DeferredRunner.get().scheduleChangePageSize(groupObject, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changePageSize(groupObject, pageSize);
            }
        });
    }

    private void changePageSize(GGroupObject groupObject, int pageSize) {
        asyncResponseDispatch(new ChangePageSize(groupObject.ID, pageSize));
    }

    public void scrollToEnd(GGroupObject group, boolean toEnd) {
        syncResponseDispatch(new ScrollToEnd(group.ID, toEnd));
    }

    public void onPropertyBinding(Event bindingEvent, ExecuteEditContext editContext) {
        if(GKeyStroke.isKeyEvent(bindingEvent) && editContext.isFocusable()) // we don't want to set focus on mouse binding (it's pretty unexpected behaviour)
            editContext.focus(FocusUtils.Reason.BINDING); // we want element to be focused on key binding (if it's possible)

        executePropertyEventAction(new EventHandler(bindingEvent), true, editContext);
    }

    public void executePropertyEventAction(EventHandler handler, ExecuteEditContext editContext) {
        executePropertyEventAction(handler, false, editContext);
    }

    public void executePropertyEventAction(EventHandler handler, boolean isBinding, ExecuteEditContext editContext) {
        Event event = handler.event;
        GPropertyDraw property = editContext.getProperty();
        if(property == null)  // in tree there can be no property in groups other than last
            return;

        GEventSource eventSource = isBinding ? GEventSource.BINDING : GEventSource.EDIT;
        if(BrowserEvents.CONTEXTMENU.equals(event.getType())) {
            handler.consume();
            GPropertyContextMenuPopup.show(new PopupOwner(editContext.getPopupOwnerWidget(), Element.as(event.getEventTarget())), property, actionSID -> {
                executePropertyEventAction(editContext, actionSID, eventSource, handler);
            });
        } else {
            lsfusion.gwt.client.base.Result<Integer> contextAction = new lsfusion.gwt.client.base.Result<>();
            String actionSID = property.getEventSID(event, isBinding, editContext, contextAction);
            if(actionSID == null)
                return;

            // hasChangeAction check is important for quickfilter not to consume event (however with propertyReadOnly, checkCanBeChanged there will be still some problems)
            if (isChangeEvent(actionSID) && (editContext.isReadOnly() != null || (contextAction.result == null && !property.hasUserChangeAction())))
                return;
            if(GEditBindingMap.EDIT_OBJECT.equals(actionSID) && !property.hasEditObjectAction)
                return;

            if(contextAction.result != null)
                executeContextAction(handler, editContext, actionSID, eventSource, contextAction.result);
            else
                executePropertyEventAction(editContext, actionSID, eventSource, handler);

            if(!handler.consumed)
                handler.consume();
        }
    }

    public void executePropertyEventAction(ExecuteEditContext editContext, String actionSID, GEventSource eventSource, EventHandler handler) {
        GPropertyDraw property = editContext.getProperty();
        if (isChangeEvent(actionSID) && property.askConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", EscapeUtils.toHTML(property.askConfirmMessage, StaticImage.MESSAGE_WARN), editContext.getPopupOwner(), chosenOption -> {
                        if (chosenOption == DialogBoxHelper.OptionType.YES)
                            executePropertyEventActionConfirmed(editContext, actionSID, eventSource, handler);
                    });
        } else
            executePropertyEventActionConfirmed(editContext, actionSID, eventSource, handler);
    }

    public void executePropertyEventActionConfirmed(ExecuteEditContext editContext, String actionSID, GEventSource eventSource, EventHandler handler) {
        executePropertyEventAction(handler, editContext, editContext, actionSID, null, eventSource, requestIndex -> setLoading(editContext, requestIndex));
    }

    public void executePropertyEventAction(GPropertyDraw property, GGroupObjectValue fullKey, String actionSID, GPushAsyncInput pushAsyncInput, GEventSource eventSource, Consumer<Long> onExec) {
        executePropertyEventAction(null, null, new ExecContext() {
            @Override
            public GPropertyDraw getProperty() {
                return property;
            }

            @Override
            public GGroupObjectValue getFullKey() {
                return fullKey;
            }
        }, actionSID, pushAsyncInput, eventSource, onExec);
    }

    public void executePropertyEventAction(EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncInput, GEventSource eventSource, Consumer<Long> onExec) {
        executePropertyEventAction(execContext.getProperty().getAsyncEventExec(actionSID), handler, editContext, execContext, actionSID, pushAsyncInput, eventSource, onExec);
    }

    private void executePropertyEventAction(GAsyncEventExec asyncEventExec, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncInput, GEventSource eventSource, Consumer<Long> onExec) {
        if (asyncEventExec != null)
            asyncEventExec.exec(this, handler, editContext, execContext, actionSID, pushAsyncInput, eventSource, onExec);
        else
            syncExecutePropertyEventAction(editContext, handler, execContext.getProperty(), execContext.getFullKey(), pushAsyncInput, actionSID, eventSource, onExec);
    }

    public void asyncExecutePropertyEventAction(String actionSID, EditContext editContext, ExecContext execContext, EventHandler handler, GPushAsyncResult pushAsyncResult, GEventSource eventSource, Consumer<Long> onRequestExec, Consumer<Long> onExec) {
        long requestIndex = asyncExecutePropertyEventAction(actionSID, editContext, handler, new GPropertyDraw[]{execContext.getProperty()}, new GEventSource[]{eventSource}, new GGroupObjectValue[]{execContext.getFullKey()}, new GPushAsyncResult[]{pushAsyncResult});

        onExec.accept(requestIndex);

        // should be after because for example onExec can setRemoteValue, but onRequestExec also does that and should have higher priority
        onRequestExec.accept(requestIndex);
    }

    public void syncExecutePropertyEventAction(EditContext editContext, EventHandler handler, GPropertyDraw property, GGroupObjectValue fullKey, GPushAsyncInput pushAsyncResult, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        long requestIndex = executePropertyEventAction(actionSID, true, editContext, handler, new GPropertyDraw[]{property}, new GEventSource[]{eventSource}, new GGroupObjectValue[]{fullKey}, new GPushAsyncResult[] {pushAsyncResult});

        onExec.accept(requestIndex);
    }

    public long asyncExecutePropertyEventAction(String actionSID, EditContext editContext, EventHandler handler, GPropertyDraw[] properties, GEventSource[] eventSources, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        return executePropertyEventAction(actionSID, false, editContext, handler, properties, eventSources, fullKeys, pushAsyncResults);
    }

    private long executePropertyEventAction(String actionSID, boolean sync, EditContext editContext, EventHandler handler, GPropertyDraw[] properties, GEventSource[] eventSources, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        int length = properties.length;
        int[] IDs = new int[length];
        GGroupObjectValue[] fullCurrentKeys = new GGroupObjectValue[length];
        for (int i = 0; i < length; i++) {
            GPropertyDraw property = properties[i];
            IDs[i] = property.ID;
            fullCurrentKeys[i] = getFullCurrentKey(property, fullKeys[i]);
        }

        ExecuteEventAction executeEventAction = new ExecuteEventAction(IDs, fullCurrentKeys, actionSID, eventSources, pushAsyncResults);
        ServerResponseCallback serverResponseCallback = new ServerResponseCallback() {
            @Override
            protected Runnable getOnRequestFinished() {
                return () -> {
                    actionDispatcher.editContext = null;
                    actionDispatcher.editEventHandler = null;
                };
            }

            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                actionDispatcher.editContext = editContext;
                actionDispatcher.editEventHandler = handler;
                super.onSuccess(response, onDispatchFinished);
            }

            @Override
            public void onFailure(ExceptionResult exceptionResult) {
                actionDispatcher.editContext = editContext;
                super.onFailure(exceptionResult);
            }
        };

        if(sync)
            return syncDispatch(executeEventAction, serverResponseCallback);
        else
            return asyncDispatch(executeEventAction, serverResponseCallback);
    }

    public long asyncResponseDispatch(final FormRequestCountingAction<ServerResponseResult> action) {
        return asyncDispatch(action, new ServerResponseCallback());
    }
    public long syncResponseDispatch(final FormRequestCountingAction<ServerResponseResult> action) {
        return syncDispatch(action, new ServerResponseCallback());
    }

    public void asyncInput(EventHandler handler, EditContext editContext, String actionSID, GAsyncInput asyncChange, GEventSource eventSource, Consumer<Long> onExec) {
        GInputList inputList = asyncChange.inputList;
        GInputListAction[] inputListActions = asyncChange.inputListActions;
        edit(asyncChange.changeType, handler, false, null, inputList, inputListActions, (value, onRequestExec) ->
            executePropertyEventAction(handler, editContext, inputListActions, value, actionSID, eventSource, requestIndex -> {
                onExec.accept(requestIndex); // setLoading

                // doing that after to set the last value (onExec recursively can also set value)
                onRequestExec.accept(requestIndex); // pendingChangeProperty
        }), cancelReason -> {}, editContext, actionSID, asyncChange.customEditFunction);
    }

    private final static GAsyncNoWaitExec asyncExec = new GAsyncNoWaitExec();

    private void executePropertyEventAction(EventHandler handler, EditContext editContext, GInputListAction[] inputListActions, GUserInputResult value, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        GInputListAction contextAction = getContextAction(inputListActions, value);
        executePropertyEventAction(contextAction != null ? contextAction.asyncExec : asyncExec,
                handler, editContext, editContext, actionSID, value != null ? new GPushAsyncInput(value) : null, eventSource, onExec);
    }

    private GInputListAction getContextAction(GInputListAction[] inputListActions, GUserInputResult value) {
        Integer contextActionIndex = value != null ? value.getContextAction() : null;
        if (contextActionIndex != null) {
            for (GInputListAction action : inputListActions) {
                if (action.index == contextActionIndex)
                    return action;
            }
        }
        return null;
    }

    public void asyncOpenForm(GAsyncOpenForm asyncOpenForm, EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {
            formsController.asyncOpenForm(getAsyncFormController(requestIndex), asyncOpenForm, editEvent, editContext, execContext, this);
        }, onExec);
    }

    public GAsyncFormController getAsyncFormController(long requestIndex) {
        return actionDispatcher.getAsyncFormController(requestIndex);
    }

    public PopupOwner getPopupOwner() {
        return new PopupOwner(getWidget());
    }
    public void asyncCloseFormConfirmed(Runnable runnable) {
        if(needConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", messages.doYouReallyWantToCloseForm(), getPopupOwner(), chosenOption -> {
                if(chosenOption == DialogBoxHelper.OptionType.YES) {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }

    public void asyncCloseForm(GAsyncExecutor asyncExecutor) {
        asyncCloseFormConfirmed(() -> formsController.asyncCloseForm(asyncExecutor, formContainer));
    }

    public void asyncCloseForm(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncCloseFormConfirmed(() -> asyncCloseForm(editContext, execContext, handler, actionSID, eventSource, onExec));
    }

    private void asyncCloseForm(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, new GPushAsyncClose(), eventSource, requestIndex ->
                formsController.asyncCloseForm(getAsyncFormController(requestIndex), formContainer), onExec);
    }

    public void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(requestIndex, actionResults, continueIndex), callback, true);
    }

    public void throwInServerInvocation(long requestIndex, Throwable throwable, int continueIndex, RequestAsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ThrowInInvocation(requestIndex, throwable, continueIndex), callback, true);
    }

    public <T extends Result> long asyncDispatch(final FormRequestCountingAction<T> action, RequestCountingAsyncCallback<T> callback) {
        return dispatcher.asyncExecute(action, callback);
    }

    public <T extends Result> long syncDispatch(final FormRequestCountingAction<T> action, RequestCountingAsyncCallback<T> callback) {
        return syncDispatch(action, callback, false);
    }

    public <T extends Result> long syncDispatch(final FormRequestAction<T> action, RequestAsyncCallback<T> callback, boolean continueInvocation) {
        return dispatcher.syncExecute(action, callback, continueInvocation);
    }

    public GGroupObjectValue getFullCurrentKey(GPropertyDraw property, GGroupObjectValue fullKey) {
        DeferredRunner.get().commitDelayedGroupObjectChange(property.groupObject);

        GGroupObjectValueBuilder fullCurrentKey = new GGroupObjectValueBuilder();

        // there was a check that if groupObject is list, then currentKey should not be empty, but I'm not sure what for this check was needed
//        property.groupObject isList isEmpty() ??

        for (GGridController group : controllers.values()) {
            fullCurrentKey.putAll(group.getSelectedKey());
        }

        for (GTreeGroupController tree : treeControllers.values()) {
            GGroupObjectValue currentPath = tree.getSelectedKey();
            if (currentPath != null) {
                fullCurrentKey.putAll(currentPath);
            }
        }

        fullCurrentKey.putAll(fullKey);

        return fullCurrentKey.toGroupObjectValue();
    }

    // for custom renderer, paste
    public void changeProperty(ExecuteEditContext editContext, PValue changeValue, GEventSource eventSource, ChangedRenderValueSupplier renderValueSupplier) {
        ChangedRenderValue changedRenderValue = changeValue != PValue.UNDEFINED ? setLocalValue(editContext, changeValue, renderValueSupplier) : null;
        // noGroupChange is needed for custom renderers that use onBlur to change values:
        // a) when ALT+TAB pressed there is no keydown previewed to disable group change mode, which is not what we want
        // b) when binding with ALT calls check commit editing, we don't want it to be treated as the group change
        String actionSID = GEditBindingMap.changeOrGroupChange(MainFrame.switchedToAnotherWindow || forcedBlurCustom);
        executePropertyEventAction(null, editContext, actionSID, eventSource, changeValue == PValue.UNDEFINED ? null : new GUserInputResult(changeValue), requestIndex -> {
            setRemoteValue(editContext, changedRenderValue, requestIndex);
        });
    }

    // for quick access actions (in toolbar and keypress)
    public void executeContextAction(EventHandler handler, ExecuteEditContext editContext, String actionSID, GEventSource eventSource, int contextAction) {
        executePropertyEventAction(handler, editContext, actionSID, eventSource, new GUserInputResult(null, contextAction), requestIndex -> {});
    }

    // for custom renderer, paste, quick access actions (in toolbar and keypress)
    private void executePropertyEventAction(EventHandler handler, ExecuteEditContext editContext, String actionSID, GEventSource eventSource, GUserInputResult value, Consumer<Long> onExec) {
        executePropertyEventAction(handler, editContext, editContext.getProperty().getInputListActions(), value, actionSID, eventSource, requestIndex -> {
            onExec.accept(requestIndex);

            setLoading(editContext, requestIndex);
        });
    }

    public interface ChangedRenderValueSupplier {
        PValue getValue(PValue oldValue, PValue changeValue);
    }
    private static class ChangedRenderValue {
        public final PValue oldValue;
        public final PValue newValue;

        public ChangedRenderValue(PValue oldValue, PValue newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    public ChangedRenderValue setLocalValue(EditContext editContext, PValue changeValue, ChangedRenderValueSupplier renderValueSupplier) {
        return setLocalValue(editContext, editContext.getProperty().getExternalChangeType(), changeValue, renderValueSupplier);
    }
    public ChangedRenderValue setLocalValue(EditContext editContext, GType changeType, PValue changeValue, ChangedRenderValueSupplier renderValueSupplier) {
        if(renderValueSupplier != null || editContext.canUseChangeValueForRendering(changeType)) {
            PValue oldValue = editContext.getValue();

            PValue newValue = renderValueSupplier != null ? renderValueSupplier.getValue(oldValue, changeValue) : changeValue;
            editContext.setValue(newValue);

            return new ChangedRenderValue(oldValue, newValue);
        }
        return null;
    }

    public void setRemoteValue(EditContext editContext, ChangedRenderValue changedRenderValue, long requestIndex) {
        if(changedRenderValue != null)
            pendingChangeProperty(editContext.getProperty(), editContext.getFullKey(), changedRenderValue.newValue, changedRenderValue.oldValue, requestIndex);
    }

    public void pendingChangeProperty(GPropertyDraw property, GGroupObjectValue fullKey, PValue value, PValue oldValue, long changeRequestIndex) {
        putToDoubleNativeMap(pendingChangePropertyRequests, property, fullKey, new Change(changeRequestIndex, value, oldValue));
    }

    public void asyncNoWait(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncNoWaitExec asyncNoWait, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {}, onExec);
    }

    public void asyncChange(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncChange asyncChange, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {
            for (int propertyID : asyncChange.propertyIDs)
                setLoadingValueAt(propertyID, editContext.getFullKey(), PValue.convertFileValue(asyncChange.value), requestIndex);
        }, onExec);
    }

    public void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncAddRemove asyncAddRemove, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        final GObject object = form.getObject(asyncAddRemove.object);
        final boolean add = asyncAddRemove.add;

        GGridController controller = controllers.get(object.groupObject);

        final int position = controller.getSelectedRow();

        if (add) {
            MainFrame.logicsDispatchAsync.executePriority(new GenerateID(), new PriorityErrorHandlingCallback<GenerateIDResult>(editContext.getPopupOwner()) {
                @Override
                public void onSuccess(GenerateIDResult result) {
                    asyncAddRemove(editContext, execContext, handler, actionSID, object, add, new GPushAsyncAdd(result.ID), new GGroupObjectValue(object.ID, new GCustomObjectValue(result.ID, null)), position, eventSource, onExec);
                }
            });
        } else {
            DeferredRunner.get().commitDelayedGroupObjectChange(object.groupObject);

            final GGroupObjectValue value = controllers.get(object.groupObject).getSelectedKey();
            if(value.isEmpty())
                return;
            asyncAddRemove(editContext, execContext, handler, actionSID, object, add, pushAsyncResult, value, position, eventSource, onExec);
        }
    }

    private void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GObject object, boolean add, GPushAsyncResult pushAsyncResult, GGroupObjectValue value, int position, GEventSource eventSource, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, eventSource, requestIndex -> {
            pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex);
            pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));

            controllers.get(object.groupObject).modifyGroupObject(value, add, -1);
        }, onExec);
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType) {
        syncResponseDispatch(new ChangePropertyOrder(property.ID, columnKey, modiType));
    }

    public void changePropertyOrder(int goID, LinkedHashMap<Integer, Boolean> orders) {
        GGroupObject groupObject = form.getGroupObject(goID);
        if (groupObject != null) {
            LinkedHashMap<GPropertyDraw, Boolean> pOrders = new LinkedHashMap<>();
            for (Integer propertyID : orders.keySet()) {
                GPropertyDraw propertyDraw = form.getProperty(propertyID);
                if (propertyDraw != null) {
                    Boolean value = orders.get(propertyID);
                    pOrders.put(propertyDraw, value);
                }
            }

            controllers.get(groupObject).changeOrders(pOrders, false);
        }
    }

    public void changePropertyFilters(int goID, List<GFilterAction.FilterItem> filters) {
        GGroupObject groupObject = form.getGroupObject(goID);
        if (groupObject != null) {
            GGridController gGridController = controllers.get(groupObject);
            List<GPropertyFilter> uFilters = new ArrayList<>();
            for (GFilterAction.FilterItem filter : filters) {
                GPropertyDraw propertyDraw = form.getProperty(filter.propertyId);
                if (propertyDraw != null) {
                    PValue value = null;
                    if (filter.value instanceof String) {
                        try {
                            value = propertyDraw.getFilterBaseType().parseString((String) filter.value, propertyDraw.getPattern());
                        } catch (ParseException ignored) {
                        }
                    } else {
                        value = PValue.convertFileValue(filter.value);
                    }
                    uFilters.add(GFilterController.createNewCondition(gGridController, new GFilter(propertyDraw), GGroupObjectValue.EMPTY, value, filter.negation, GCompare.get(filter.compare), filter.junction));
                }
            }

            gGridController.changeFilters(uFilters);
        }
    }

    public void setPropertyOrders(GGroupObject groupObject, List<Integer> propertyList, List<GGroupObjectValue> columnKeyList, List<Boolean> orderList) {
        syncResponseDispatch(new SetPropertyOrders(groupObject.ID, propertyList, columnKeyList, orderList));
    }

    public long expandGroupObjectRecursive(GGroupObject group, boolean current, boolean open) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        return asyncResponseDispatch(open ? new ExpandGroupObjectRecursive(group.ID, current) : new CollapseGroupObjectRecursive(group.ID, current));
    }

    public long expandGroupObject(GGroupObject group, GGroupObjectValue value, boolean open) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        return asyncResponseDispatch(open ? new ExpandGroupObject(group.ID, value) : new CollapseGroupObject(group.ID, value));
    }

    public void setTabActive(GContainer tabbedPane, GComponent visibleComponent) {
        asyncResponseDispatch(new SetTabActive(tabbedPane.ID, visibleComponent.ID));

        formLayout.updatePanels(); // maybe it's not needed, but we want to make it symmetrical to the container collapsed call
    }
    
    public void setContainerCollapsed(GContainer container, boolean collapsed) {
        asyncResponseDispatch(new SetContainerCollapsed(container.ID, collapsed));

        formLayout.updatePanels(); // we want to avoid blinking between setting visibility and getting response (and having updatePanels there)
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        syncResponseDispatch(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID));
    }

    public long changeFilter(GGroupObject groupObject, ArrayList<GPropertyFilter> conditions) {
        currentFilters.put(groupObject, conditions);
        return applyCurrentFilters(Collections.singletonList(groupObject));
    }

    public long changeFilter(GTreeGroup treeGroup, ArrayList<GPropertyFilter> conditions) {
        Map<GGroupObject, ArrayList<GPropertyFilter>> filters = GwtSharedUtils.groupList(new GwtSharedUtils.Group<GGroupObject, GPropertyFilter>() {
            public GGroupObject group(GPropertyFilter key) {
                return key.groupObject;
            }
        }, conditions);

        for (GGroupObject group : treeGroup.groups) {
            ArrayList<GPropertyFilter> groupFilters = filters.get(group);
            if (groupFilters == null) {
                groupFilters = new ArrayList<>();
            }
            currentFilters.put(group, groupFilters);
        }

        return applyCurrentFilters(treeGroup.groups);
    }

    private long applyCurrentFilters(List<GGroupObject> groups) {
        Map<Integer, List<GPropertyFilterDTO>> filters = new LinkedHashMap<>();
        for (GGroupObject group : groups) {
            List<GPropertyFilterDTO> groupFilters = new ArrayList<>();
            List<GPropertyFilter> gFilters = currentFilters.get(group);
            for (GPropertyFilter filter : gFilters) {
                if (!filter.property.isAction()) {
                    groupFilters.add(filter.getFilterDTO());
                }
            }
            filters.put(group.ID, groupFilters);
        }
        return asyncResponseDispatch(new SetUserFilters(filters));
    }

    public void setViewFilters(ArrayList<GPropertyFilter> conditions, int pageSize) {
        asyncResponseDispatch(new SetViewFilters(conditions.stream().map(GPropertyFilter::getFilterDTO).collect(Collectors.toCollection(ArrayList::new)), pageSize));
    }

    public void quickFilter(Event event, int initialFilterPropertyID) {
        GPropertyDraw propertyDraw = getProperty(initialFilterPropertyID);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            focusProperty(propertyDraw);
            controllers.get(propertyDraw.groupObject).quickEditFilter(event, propertyDraw, GGroupObjectValue.EMPTY);
        }
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        getPropertyController(propertyDraw).focusProperty(propertyDraw);
    }

    public void setLoadingValueAt(int propertyID, GGroupObjectValue fullKey, PValue value, long requestIndex) {
        GPropertyDraw property = getProperty(propertyID);
        GGroupObjectValue fullCurrentKey = getFullCurrentKey(property, fullKey);
        Pair<GGroupObjectValue, PValue> propertyCell = getPropertyController(property).setLoadingValueAt(property, fullCurrentKey, value);

        if(propertyCell != null) {
            pendingChangeProperty(property, propertyCell.first, value, propertyCell.second, requestIndex);
            pendingLoadingProperty(property, propertyCell.first, requestIndex);
        }
    }

    private Map<Integer, Integer> getTabMap(TabbedContainerView containerView, GContainer component) {
        Map<Integer, Integer> tabMap = new HashMap<>();
        ArrayList<GComponent> tabs = component.children;
        if (tabs != null) {
            int c = 0;
            for (int i = 0; i < tabs.size(); i++) {
                GComponent tab = tabs.get(i);
                if (containerView.isTabVisible(tab)) {
                    tabMap.put(tab.ID, c++);
                }
            }
        }
        return tabMap;
    }

    public void activateTab(GComponent component) {
        if(component.isTab())
            ((TabbedContainerView)formLayout.getContainerView(component.container)).activateTab(component);
    }

    public abstract static class CustomCallback<T> implements RequestCountingAsyncCallback<T> {

        protected abstract void onSuccess(T result);

        @Override
        public void onSuccess(T result, Runnable onDispatchFinished) {
            onSuccess(result);

            if(onDispatchFinished != null)
                onDispatchFinished.run();
        }
    }

    private abstract class SimpleRequestCallback<R> extends lsfusion.gwt.client.form.controller.SimpleRequestCallback<R> {

        public SimpleRequestCallback() {
        }

        @Override
        public PopupOwner getPopupOwner() {
            return GFormController.this.getPopupOwner();
        }
    }

    public void countRecords(final GGroupObject groupObject) {
        asyncDispatch(new CountRecords(groupObject.ID), new SimpleRequestCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                controllers.get(groupObject).showRecordQuantity((Integer) result.value);
            }
        });
    }

    public void calculateSum(final GGroupObject groupObject, final GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        asyncDispatch(new CalculateSum(propertyDraw.ID, columnKey), new SimpleRequestCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                controllers.get(groupObject).showSum(result.value, propertyDraw);
            }
        });
    }

    // change group mode with force refresh
    public long changeListViewType(final GGroupObject groupObject, int pageSize, GListViewType viewType, GUpdateMode updateMode) {
        boolean enableGroup = viewType == GListViewType.PIVOT;
        return changeMode(groupObject, true, enableGroup ? new ArrayList<>() : null, enableGroup ? new ArrayList<>() : null, 0, null, pageSize, true, updateMode, viewType);
    }
    public long changeMode(final GGroupObject groupObject, boolean setGroup, List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeysList, int aggrProps, GPropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, GUpdateMode updateMode, GListViewType viewType) {
        int[] propertyIDs = null;
        GGroupObjectValue[] columnKeys = null;
        if(properties != null) {
            propertyIDs = new int[properties.size()];
            columnKeys = new GGroupObjectValue[properties.size()];
            for(int i=0;i<propertyIDs.length;i++) {
                propertyIDs[i] = properties.get(i).ID;
                columnKeys[i] = columnKeysList.get(i);
            }
        }
        return asyncResponseDispatch(new ChangeMode(groupObject.ID, setGroup, propertyIDs, columnKeys, aggrProps, aggrType, pageSize, forceRefresh, updateMode, viewType));
    }

    public GFormUserPreferences getUserPreferences() {
        List<GGroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<>();
        List<GGroupObjectUserPreferences> groupObjectGeneralPreferencesList = new ArrayList<>();
        for (GGridController controller : controllers.values()) {
            if (controller.isList()) {
                groupObjectUserPreferencesList.add(controller.getUserGridPreferences());
                groupObjectGeneralPreferencesList.add(controller.getGeneralGridPreferences());
            }
        }
        return new GFormUserPreferences(groupObjectGeneralPreferencesList, groupObjectUserPreferencesList);
    }

    public void runGroupReport(Integer groupObjectID) {
        syncDispatch(new GroupReport(groupObjectID, getUserPreferences()), new SimpleRequestCallback<GroupReportResult>() {
            @Override
            public void onSuccess(GroupReportResult result) {
                GwtClientUtils.openFile(result.filename, false, null);
            }
        });
    }

    public void executeVoidAction() {
        syncDispatch(new VoidFormAction(), new SimpleRequestCallback<VoidResult>() {
            @Override
            public void onSuccess(VoidResult result) {
            }
        });
    }

    public void saveUserPreferences(GGridUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps, final AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences.convertPreferences(), forAllUsers, completeOverride, hiddenProps), new SimpleRequestCallback<ServerResponseResult>() {
            @Override
            public void onSuccess(ServerResponseResult response) {
                for (GAction action : response.actions) {
                    if (action instanceof GMessageAction) {
                        actionDispatcher.execute((GMessageAction) action);
                        callback.onFailure(new Throwable());
                        return;
                    }
                }
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(ExceptionResult exceptionResult) {
                callback.onFailure(exceptionResult.throwable);

                super.onFailure(exceptionResult);
            }
        });
    }

    public void refreshUPHiddenProps(String groupObjectSID, String[] propSids) {
        syncResponseDispatch(new RefreshUPHiddenPropsAction(groupObjectSID, propSids));
    }

    public List<GPropertyDraw> getPropertyDraws() {
        return form.propertyDraws;
    }

    private ActionPanelRenderer asyncView;
    public void setAsyncView(ActionPanelRenderer asyncView) {
        this.asyncView = asyncView;
    }
    public void showAsync(boolean set) {
        if(asyncView != null)
            asyncView.setForceLoading(set);
    }

    public Element getTargetAndPreview(Element element, Event event) {
        // there is a problem with non-bubbling events (there are not many such events, see CellBasedWidgetImplStandard.nonBubblingEvents, so basically focus events):
        // handleNonBubblingEvent just looks for the first event listener
        // but if there are 2 widgets listening to the focus events (for example when in ActionOrPropertyValue there is an embedded form and there is a TableContainer inside)
        // then the lower widget gets both blur events (there 2 of them with different current targets, i.e supposed to be handled by different elements) and the upper widget gets none of them (which leads to the very undesirable behaviour with the "double" finishEditing, etc.)
        // WE DON'T NEED IT NOW SINCE WE RELY ON FOCUSOUT
//        if(DataGrid.checkNonBubblingEvents(event)) { // there is a bubble field, but it does
//            EventTarget currentEventTarget = event.getCurrentEventTarget();
//            if(Element.is(currentEventTarget)) {
//                Element currentTarget = currentEventTarget.cast();
//                 maybe sinked focus events should be checked for the currentTarget
//                if(!currentTarget.equals(element) && DOM.dispatchEvent(event, currentTarget))
//                    return null;
//            }
//        }

        Element target = Element.as(event.getEventTarget());
        if(target == null)
            return null;

        if(DataGrid.FOCUSPREVIEWOUT.equals(event.getType()))
            FocusUtils.setLastBlurredElement(Element.as(event.getEventTarget()));

        formsController.checkEditModeEvents(event);

        //focus() can trigger blur event, blur finishes editing. Editing calls syncDispatch.
        //If isEditing() and busyDialogDisplayer isVisible() then flushCompletedRequests is not executed and syncDispatch is blocked.
        if(dispatcher.getBusyDialogDisplayer().isVisible() && (DataGrid.checkSinkEvents(event) || DataGrid.checkSinkFocusEvents(event)))
            return null;

        if(!DataGrid.checkSinkFocusEvents(event))
            SmartScheduler.getInstance().flush();

        if(!MainFrame.previewClickEvent(target, event))
            return null;

        // see GEditBindingMap.changeOrGroupChange
        boolean isEditing = isEditing();
        if (isEditing || focusedCustom != null) { // we need switchedToAnotherWindow to be filled - see it's usages
            boolean switched = MainFrame.previewSwitchToAnotherWindow(event);
            if(switched && isEditing) // we don't want to stop additing when switching to another window
                return null;
        }

        return target;
    }

    public boolean previewCustomEvent(Event event, Element element) {
        return getTargetAndPreview(element, event) != null;
    }

    protected void onFormHidden(GAsyncFormController asyncFormController, int closeDelay, EndReason editFormCloseReason) {
        formsController.removeFormContainer(formContainer);
        for(ContainerForm containerForm : containerForms) {
            containerForm.getForm().closePressed(editFormCloseReason);
        }

        FormDispatchAsync closeDispatcher = dispatcher;
        Scheduler.get().scheduleDeferred(() -> {
            closeDispatcher.executePriority(new Close(closeDelay), new PriorityErrorHandlingCallback<VoidResult>(getPopupOwner()) {
                @Override
                public void onFailure(Throwable caught) { // supressing errors
                }
            });
            closeDispatcher.close();
        });
//        dispatcher = null; // so far there are no null checks (for example, like in desktop-client), so changePageSize can be called after (apparently close will suppress it)
    }

    // need this because hideForm can be called twice, which will lead to several continueDispatching (and nullpointer, because currentResponse == null)
    private boolean formHidden;
    public void hideForm(GAsyncFormController asyncFormController, int closeDelay, EndReason editFormCloseReason) {
        if(!formHidden) {
            onFormHidden(asyncFormController, closeDelay, editFormCloseReason);
            formHidden = true;
        }
    }

    public Dimension getPreferredSize(GSize maxWidth, GSize maxHeight, Element element) {
        return formLayout.getPreferredSize(maxWidth, maxHeight, element);
    }

    public boolean isWindow() {
        return formContainer instanceof ModalForm;
    }

    public boolean isDialog() {
        return isDialog;
    }

    public boolean isVisible() {
        return !formHidden;
    }

    public boolean isActive() {
        return isVisible() && formActive;
    }

    public GForm getForm() {
        return form;
    }

    public List<GObject> getObjects() {
        ArrayList<GObject> objects = new ArrayList<>();
        for (GGroupObject groupObject : form.groupObjects) {
            for (GObject object : groupObject.objects) {
                objects.add(object);
            }
        }
        return objects;
    }

    public void setContainerCaption(GContainer container, String caption) {
        container.caption = caption;

        updateCaption(container);
    }

    public void setContainerImage(GContainer container, AppBaseImage image) {
        container.image = image;

        updateImage(container);
    }

    private Widget getCaptionWidget(GContainer container) {
        return formLayout.getContainerCaption(container);
    }

    public void updateCaption(GContainer container) {
        Widget captionWidget = getCaptionWidget(container);
        if(captionWidget != null)
            BaseImage.updateText(captionWidget, container.caption);
    }
    public void updateImage(GContainer container) {
        Widget captionWidget = getCaptionWidget(container);
        if(captionWidget != null)
            BaseImage.updateImage(container.image, captionWidget);
    }

    public void setContainerCustomDesign(GContainer container, String customDesign) {
        GAbstractContainerView containerView = formLayout.getContainerView(container);
        ((CustomContainerView)containerView).updateCustomDesign(customDesign);
    }

    private static final class Change {
        public final long requestIndex;
        public final PValue newValue;
        public final PValue oldValue;

        private Change(long requestIndex, PValue newValue, PValue oldValue) {
            this.requestIndex = requestIndex;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
    }

    private static class ModifyObject {
        public final GObject object;
        public final boolean add;
        public final GGroupObjectValue value;
        public final int position;

        private ModifyObject(GObject object, boolean add, GGroupObjectValue value, int position) {
            this.object = object;
            this.add = add;
            this.value = value;
            this.position = position;
        }
    }

    public boolean focusDefaultWidget() {
        FocusUtils.Reason reason = FocusUtils.Reason.SHOW;
        if (formLayout.focusDefaultWidget(reason)) {
            return true;
        }

        return focusNextElement(reason, true);
    }

    public boolean focusNextElement(FocusUtils.Reason reason, boolean forward) {
        Element nextFocusElement = FocusUtils.getNextFocusElement(formLayout.getElement(), forward);
        if(nextFocusElement != null) {
            FocusUtils.focus(nextFocusElement, reason);
            return true;
        }
        return false;
    }

    private class ServerResponseCallback extends GwtActionDispatcher.ServerResponseCallback {

        public ServerResponseCallback() {
            super(false);
        }

        @Override
        protected GwtActionDispatcher getDispatcher() {
            return actionDispatcher;
        }
    }

    public abstract static class Binding implements BindingExec {

        public GGroupObject groupObject;

        public Binding(GGroupObject groupObject) {
            this.groupObject = groupObject;
        }

        public abstract boolean showing();

        public boolean enabled() {
            return true;
        }
    }

    private final ArrayList<GBindingEvent> bindingEvents = new ArrayList<>();
    private final ArrayList<Binding> bindings = new ArrayList<>();

    public interface BindingCheck {
        boolean check(Event event);
    }
    public interface BindingExec {
        void exec(Event event);
    }
    public ArrayList<Integer> addPropertyBindings(GPropertyDraw propertyDraw, BindingExec bindingExec, Widget widget) {
        ArrayList<Integer> result = new ArrayList<>();
        for(GInputBindingEvent bindingEvent : propertyDraw.bindingEvents) // supplier for optimization
            result.add(addBinding(bindingEvent.inputEvent, bindingEvent.env, bindingExec, widget, propertyDraw.groupObject));
        return result;
    }
    public void removePropertyBindings(ArrayList<Integer> indices) {
        for(int index : indices)
            removeBinding(index);
    }

    public int addRegularFilterBinding(GInputBindingEvent event, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event.inputEvent, event.env, pressed, component, groupObject);
    }
    public int addBinding(GInputEvent event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event::isEvent, env, null, pressed, component, groupObject);
    }
    public int addBinding(BindingCheck event, GBindingEnv env, Supplier<Boolean> enabled, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(new GBindingEvent(event, env), new Binding(groupObject) {
            @Override
            public boolean showing() {
                return component == null || isShowing(component);
            }

            @Override
            public boolean enabled() {
                return enabled != null ? enabled.get() : super.enabled();
            }

            @Override
            public void exec(Event event) {
                pressed.exec(event);
            }
        });
    }
    public int addBinding(GBindingEvent event, Binding action) {
        int index = bindings.size();
        bindingEvents.add(event);
        bindings.add(action);
        return index;
    }
    public void removeBinding(int index) {
        bindingEvents.remove(index);
        bindings.remove(index);
    }

    public void addEnterBindings(GBindingMode bindGroup, Consumer<Boolean> selectNextElement, GGroupObject groupObject) {
        addEnterBinding(false, bindGroup, selectNextElement, groupObject);
        addEnterBinding(true, bindGroup, selectNextElement, groupObject);
    }

    private void addEnterBinding(boolean shiftPressed, GBindingMode bindGroup, Consumer<Boolean> selectNextElement, GGroupObject groupObject) {
        addBinding(new GKeyInputEvent(new GKeyStroke(KeyCodes.KEY_ENTER, false, false, shiftPressed)),
                new GBindingEnv(-100, GBindingMode.NO, null, bindGroup, GBindingMode.NO, null, null, null),  // bindEditing - NO, because we don't want for example when editing text in grid to catch enter
                event -> selectNextElement.accept(!shiftPressed),
                null,
                groupObject);
    }

    private static final String bindingGroupObject = "groupObject";
    public static void setBindingGroupObject(Widget widget, GGroupObject groupObject) {
        widget.getElement().setPropertyObject(bindingGroupObject, groupObject);
    }

    private static GGroupObject getBindingGroupObject(Element elementTarget) {
        while (elementTarget != null) {     // пытаемся найти GroupObject, к которому относится элемент с фокусом
            GGroupObject targetGO = (GGroupObject) elementTarget.getPropertyObject(bindingGroupObject);
            if (targetGO != null)
                return targetGO;
            elementTarget = elementTarget.getParentElement();
        }
        return null;
    }

    public interface GGroupObjectSupplier {
        GGroupObject get();
    }

    public void processBinding(EventHandler handler, boolean preview, boolean isCell, boolean panel) {
        final EventTarget target = handler.event.getEventTarget();
        if (!Element.is(target)) {
            return;
        }

        Event event = handler.event;
        boolean isMouse = GMouseStroke.isEvent(event);
        TreeMap<Integer, Binding> orderedBindings = new TreeMap<>(); // descending sorting by priority

        GGroupObject groupObject = getBindingGroupObject(Element.as(target));
        for (int i = 0, size = bindingEvents.size(); i < size; i++) {
            GBindingEvent bindingEvent = bindingEvents.get(i);
            if (bindingEvent.event.check(event)) {
                Binding binding = bindings.get(i);
                boolean equalGroup;
                GBindingEnv bindingEnv = bindingEvent.env;
                if(bindPreview(bindingEnv, preview) &&
                    bindDialog(bindingEnv) &&
                    bindGroup(bindingEnv, groupObject, equalGroup = nullEquals(groupObject, binding.groupObject)) &&
                    bindEditing(bindingEnv, event) &&
                    bindShowing(bindingEnv, binding.showing()) &&
                    bindPanel(bindingEnv, isMouse, panel) &&
                    bindCell(bindingEnv, isMouse, isCell))
                orderedBindings.put(-(GwtClientUtils.nvl(bindingEnv.priority, i) + (equalGroup ? 100 : 0)), binding); // increasing priority for group object
            }
        }

        for (Binding binding : orderedBindings.values()) {
            if (binding.enabled()) {
                checkCommitEditing();
                handler.consume();

                binding.exec(event);
                return;
            }
        }
    }

    public void checkCommitEditing() {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        if(requestCellEditor != null)
            requestCellEditor.commit(getEditElement(), CommitReason.FORCED_BLURRED);
        else
            if(focusedCustom != null) {
                Element focusedElement = FocusUtils.getFocusedChild(focusedCustom);
                if(focusedElement != null) { // we do the fake blur to call onBlur, because custom render
                    forcedBlurCustom = true;
                    try {
                        // move focus "outside" the element (to the first with tabIndex ???, that what blur actually does) and then return back to the element ???
                        FocusUtils.triggerFocus(reason -> FocusUtils.focusOut(focusedCustom, reason), focusedElement);
                    } finally {
                        forcedBlurCustom = false;
                    }
                }
            }

    }

    private boolean bindPreview(GBindingEnv binding, boolean preview) {
        switch (binding.bindPreview) {
            case AUTO:
            case ONLY:
                return preview;
            case NO:
                return !preview;
            case ALL: // actually makes no since if previewed, than will be consumed so equivalent to only
                return true;
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindDialog);
        }
    }

    private boolean bindDialog(GBindingEnv binding) {
        switch (binding.bindDialog) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return isDialog();
            case NO:
                return !isDialog();
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindDialog);
        }
    }

    private boolean bindGroup(GBindingEnv bindingEvent, GGroupObject groupObject, boolean equalGroup) {
        switch (bindingEvent.bindGroup) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return equalGroup;
            case NO:
                return !equalGroup;
            case INPUT:
                return groupObject != null && form.inputGroupObjects.contains(groupObject);
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode");
        }
    }

    private boolean bindEditing(GBindingEnv binding, Event event) {
        switch (binding.bindEditing) {
            case AUTO:
                return !(isEditing() && getEditElement().isOrHasChild(Element.as(event.getEventTarget())));
            case ALL:
                return true;
            case ONLY:
                return isEditing();
            case NO:
                return !isEditing();
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindEditing);
        }
    }

    private boolean bindShowing(GBindingEnv binding, boolean showing) {
        switch (binding.bindShowing) {
            case ALL:
                return true;
            case AUTO:
            case ONLY:
                return showing;
            case NO:
                return !showing;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindShowing);
        }
    }

    private boolean bindPanel(GBindingEnv binding, boolean isMouse, boolean panel) {
        switch (binding.bindPanel) {
            case ALL:
                return true;
            case AUTO:
                return !isMouse || !panel;
            case ONLY:
                return panel;
            case NO:
                return !panel;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindPanel);
        }
    }

    private boolean bindCell(GBindingEnv binding, boolean isMouse, boolean isCell) {
        switch (binding.bindCell) {
            case ALL:
                return true;
            case AUTO:
                return !isMouse || isCell;
            case ONLY:
                return isCell;
            case NO:
                return !isCell;
            case INPUT:
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindCell);
        }
    }

    private CellEditor cellEditor;
    private Element focusedCustom;
    private boolean forcedBlurCustom;

    public Element getEditElement() {
        return editContext.getEditElement();
    }

    private EditContext editContext;
    private long editRequestIndex = -1;

    private BiConsumer<GUserInputResult, CommitReason> editBeforeCommit;
    private BiConsumer<GUserInputResult, CommitReason> editAfterCommit;
    private Consumer<CancelReason> editCancel;

    private Element focusedElement;
    private Object forceSetFocus;

    public boolean isEditing() {
        return editContext != null;
    }

    public long getEditingRequestIndex() {
        return editRequestIndex;
    }

    private String editAsyncValuesSID;
    private boolean editAsyncUsePessimistic; // optimimization
    // shouldn't be zeroed when editing ends, since we assume that there is only one live input on the form
    private int editAsyncIndex;
    private int editLastReceivedAsyncIndex;
    // we don't want to proceed results if "later" request results where proceeded
    private AsyncCallback<GAsyncResult> checkLast(int index, AsyncCallback<GAsyncResult> callback) {
        return new AsyncCallback<GAsyncResult>() {
            @Override
            public void onFailure(Throwable caught) {
                if(index >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = index;
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(GAsyncResult result) {
                if(index >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = index;
                    if(!result.moreRequests && index < editAsyncIndex - 1)
                        result = new GAsyncResult(result.asyncs, result.needMoreSymbols, true);
                    callback.onSuccess(result);
                }
            }
        };
    }

    // synchronous call (with request indices, etc.)
    private void getPessimisticValues(int propertyID, GGroupObjectValue columnKey, String actionSID, String value, int index, AsyncCallback<GAsyncResult> callback) {
        asyncDispatch(new GetAsyncValues(propertyID, columnKey, actionSID, value, index), new CustomCallback<ListResult>() {
            @Override
            public void onFailure(ExceptionResult exceptionResult) {
                callback.onFailure(exceptionResult.throwable);
            }

            @Override
            public void onSuccess(ListResult result) {
                callback.onSuccess(convertAsyncResult(result));
            }
        });
    }

    public void getAsyncValues(String value, AsyncCallback<GAsyncResult> callback) {
        if(editContext != null) // just in case
            getAsyncValues(value, editContext, editAsyncValuesSID, callback, 0);
    }

    public void getAsyncValues(String value, EditContext editContext, String actionSID, AsyncCallback<GAsyncResult> callback, int increaseValuesNeededCount) {
        getAsyncValues(value, editContext.getProperty(), editContext.getColumnKey(), actionSID, callback, increaseValuesNeededCount);
    }

    public static class GAsyncResult {
        public final ArrayList<GAsync> asyncs;
        public final boolean needMoreSymbols;
        public final boolean moreRequests;

        public GAsyncResult(ArrayList<GAsync> asyncs, boolean needMoreSymbols, boolean moreRequests) {
            this.asyncs = asyncs;
            this.needMoreSymbols = needMoreSymbols;
            this.moreRequests = moreRequests;
        }
    }
    public void getAsyncValues(String value, GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<GAsyncResult> callback, int increaseValuesNeededCount) {
        int editIndex = editAsyncIndex++;
        AsyncCallback<GAsyncResult> fCallback = checkLast(editIndex, callback);

        GGroupObjectValue currentKey = getFullCurrentKey(property, columnKey);

        Runnable runPessimistic = () -> getPessimisticValues(property.ID, currentKey, actionSID, value, editIndex, fCallback);
        if (!editAsyncUsePessimistic)
            dispatcher.executePriority(new GetPriorityAsyncValues(property.ID, currentKey, actionSID, value, editIndex, increaseValuesNeededCount), new PriorityAsyncCallback<ListResult>() {
                @Override
                public void onFailure(Throwable caught) {
                    fCallback.onFailure(caught);
                }

                @Override
                public void onSuccess(ListResult result) {
                    if (result.value == null) { // optimistic request failed, running pessimistic one, with request indices, etc.
                        editAsyncUsePessimistic = true;
                        runPessimistic.run();
                    } else {
                        GAsyncResult asyncResult = convertAsyncResult(result);
                        if(asyncResult.moreRequests)
                            runPessimistic.run();
                        fCallback.onSuccess(asyncResult);
                    }
                }
            });
        else
            runPessimistic.run();
    }

    private static GAsyncResult convertAsyncResult(ListResult result) {
        boolean needMoreSymbols = false;
        boolean moreResults = false;
        ArrayList<GAsync> values = result.value;
        if(values.size() > 0) {
            GAsync lastResult = values.get(values.size() - 1);
            if(lastResult.equals(GAsync.RECHECK)) {
                values = removeLast(values);

                moreResults = true;
            } else if(values.size() == 1 && (lastResult.equals(GAsync.CANCELED) || lastResult.equals(GAsync.NEEDMORE))) { // ignoring CANCELED results
                needMoreSymbols = lastResult.equals(GAsync.NEEDMORE);
                values = needMoreSymbols ? new ArrayList<>() : null;
            }
        }
        return new GAsyncResult(values, needMoreSymbols, moreResults);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue setOldValue, GInputList inputList, GInputListAction[] inputListActions, BiConsumer<GUserInputResult, Consumer<Long>> afterCommit, Consumer<CancelReason> cancel, EditContext editContext, String actionSID, String customChangeFunction) {
        assert type != null;
        lsfusion.gwt.client.base.Result<ChangedRenderValue> changedRenderValue = new lsfusion.gwt.client.base.Result<>();
        edit(type, handler, hasOldValue, setOldValue, inputList, inputListActions, // actually it's assumed that actionAsyncs is used only here, in all subsequent calls it should not be referenced
                (inputResult, commitReason) -> changedRenderValue.set(setLocalValue(editContext, type, inputResult.getPValue(), null)),
                (inputResult, commitReason) -> afterCommit.accept(inputResult, requestIndex -> setRemoteValue(editContext, changedRenderValue.result, requestIndex)),
                cancel, editContext, actionSID, customChangeFunction);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue oldValue, GInputList inputList, GInputListAction[] inputListActions, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, String customChangeFunction) {
        GPropertyDraw property = editContext.getProperty();

        RequestValueCellEditor cellEditor;
        boolean hasCustomEditor = customChangeFunction != null && !customChangeFunction.equals("DEFAULT");
        if (hasCustomEditor) // see LsfLogics.g propertyCustomView rule
            cellEditor = CustomReplaceCellEditor.create(this, property, type, customChangeFunction);
        else {
            if(property.echoSymbols) // disabling dropdown if echo
                inputList = null;

            cellEditor = type.createCellEditor(this, property, inputList, inputListActions, editContext);
        }

        if (cellEditor != null) {
            boolean canUseChangeValueForRendering = editContext.canUseChangeValueForRendering(type);
            if(canUseChangeValueForRendering)
                cellEditor.setCancelTheSameValueOnBlur(editContext.getValue());

            if(!hasOldValue) { // property.baseType.equals(type) actually there should be something like compatible, but there is no such method for now, so we'll do this check in editors
                assert oldValue == null;
                oldValue = editContext.getValue();
                if(oldValue != null && !canUseChangeValueForRendering && !hasCustomEditor) {
                    try {
                        oldValue = type.parseString(PValue.getStringValue(oldValue), editContext.getRenderContext().getPattern());
                    } catch (ParseException e) {
                        oldValue = null;
                    }
                }
            }

            edit(cellEditor, handler, oldValue, beforeCommit, afterCommit, cancel, editContext, editAsyncValuesSID, -1);
        } else
            cancel.accept(CancelReason.FORCED);
    }

    public void edit(CellEditor cellEditor, EventHandler handler, PValue oldValue, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, long editRequestIndex) {
        assert this.editContext == null;
        editBeforeCommit = beforeCommit;
        editAfterCommit = afterCommit;
        editCancel = cancel;

        this.editAsyncValuesSID = editAsyncValuesSID;

        this.editContext = editContext;
        this.editRequestIndex = editRequestIndex;  // we need to force dispatch responses till this index because otherwise we won't

        focusedElement = FocusUtils.getFocusedElement();

        Element element = getEditElement();
        FocusUtils.startFocusTransaction(element);

        editContext.startEditing();

        boolean notFocusable = !editContext.isFocusable();
        if(notFocusable) // assert that otherwise it's already has focus
            forceSetFocus = editContext.forceSetFocus();

        RenderContext renderContext = editContext.getRenderContext();
        if (cellEditor instanceof ReplaceCellEditor && ((ReplaceCellEditor) cellEditor).needReplace(element, renderContext)) {
            GPropertyDraw property = editContext.getProperty();
            CellRenderer cellRenderer = property.getCellRenderer(renderContext.getRendererType());

            // we need to do it before clearRender to have actual sizes + we need to remove paddings since we're setting width for wrapped component
            Integer renderedWidth = null;
            if(property.valueWidth == -1)
                renderedWidth = GwtClientUtils.getWidth(element);
            Integer renderedHeight = null;
            if(property.valueHeight == -1)
                renderedHeight = GwtClientUtils.getHeight(element);

            cellRenderer.clearRender(element, renderContext); // dropping previous render

            ((ReplaceCellEditor)cellEditor).render(element, renderContext, oldValue, renderedWidth, renderedHeight); // rendering new one, filling inputElement
        }

        this.cellEditor = cellEditor; // not sure if it should before or after startEditing, but definitely after removeAllChildren, since it leads to blur for example
        cellEditor.start(handler, element, renderContext, notFocusable, oldValue); //need to be after this.cellEditor = cellEditor, because there is commitEditing in start in LogicalCellEditor

        FocusUtils.endFocusTransaction();
    }

    // only request cell editor can be long-living
    protected RequestCellEditor getRequestCellEditor() {
        return (RequestCellEditor)cellEditor;
    }

    @Override
    public void commitEditing(GUserInputResult result, CommitReason commitReason) {
        editBeforeCommit.accept(result, commitReason);
        editBeforeCommit = null;

        finishEditing(commitReason.isBlurred(), false);

        BiConsumer<GUserInputResult, CommitReason> editAfterCommit = this.editAfterCommit;
        this.editAfterCommit = null; // it seems this is needed because after commit another editing can be started
        editAfterCommit.accept(result, commitReason);

        onEditingFinished();
    }

    @Override
    public boolean isThisCellEditing(CellEditor cellEditor) {
        return cellEditor == this.cellEditor;
    }

    @Override
    public void cancelEditing(CancelReason cancelReason) {
        finishEditing(cancelReason.isBlurred(), true);

        editCancel.accept(cancelReason);
        editCancel = null;

        onEditingFinished();
    }

    // we have to do it after "after commit" is called because there can be for example applyRemoteChanges, and pendingChangeProperty not yet called (so there will be previous value set)
    private void onEditingFinished() {
        dispatcher.onEditingFinished();
    }

    private void finishEditing(boolean blurred, boolean cancel) {
        Element renderElement = getEditElement();
        FocusUtils.startFocusTransaction(renderElement);

        CellEditor cellEditor = this.cellEditor;
        this.cellEditor = null;

        EditContext editContext = this.editContext;
//        this.editRequestIndex = -1; //it doesn't matter since it is not used when editContext / cellEditor is null
        this.editAsyncUsePessimistic = false;
        this.editAsyncValuesSID = null;

        if(cellEditor instanceof RequestCellEditor)
            ((RequestCellEditor)cellEditor).stop(renderElement, cancel, blurred);

        boolean isReplace = cellEditor instanceof ReplaceCellEditor && ((ReplaceCellEditor) cellEditor).needReplace(renderElement, editContext.getRenderContext());
        if(isReplace)
            ((ReplaceCellEditor) cellEditor).clearRender(renderElement, editContext.getRenderContext(), cancel);

        //getAsyncValues need editContext, so it must be after clearRenderer
        this.editContext = null;

        editContext.stopEditing();

        if(isReplace)
            render(editContext);

        update(editContext);

        if(forceSetFocus != null) {
            editContext.restoreSetFocus(forceSetFocus);
            forceSetFocus = null;
        }

        if(blurred) { // when editing is commited (thus editing element is removed), set last blurred element to main widget to keep focus there
            if(editContext.isSetLastBlurred()) {
                Element focusElement = editContext.getFocusElement();
                if(focusElement != null)
                    FocusUtils.setLastBlurredElement(focusElement);
            }
        } else {
            if (focusedElement != null) {
                FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
                focusedElement = null;
            }
        }

        FocusUtils.endFocusTransaction();
    }

    public void render(EditContext editContext) {
        render(editContext.getProperty(), editContext.getEditElement(), editContext.getRenderContext());
    }
    public void render(GPropertyDraw property, Element element, RenderContext renderContext) {
        if(isEdited(element)) {
            assert false;
            return;
        }

        property.getCellRenderer(renderContext.getRendererType()).render(element, renderContext);
    }

    // setting loading
    public void setLoading(ExecuteEditContext editContext, long requestIndex) {
        // actually this part we can do before sending request
        editContext.setLoading();

        // RERENDER IF NEEDED : we have the previous state
        // but in that case we need to move GridPropertyColumn.renderDom logics to GFormController.render here (or have some callbacks)
        update(editContext);

        GPropertyDraw property = editContext.getProperty();
        GGroupObjectValue rowKey = property.isList ? editContext.getRowKey() : GGroupObjectValue.EMPTY; // because for example in custom renderer editContext can be not the currentKey

        pendingLoadingProperty(property, GGroupObjectValue.getFullKey(rowKey, editContext.getColumnKey()), requestIndex);
    }

    private void pendingLoadingProperty(GPropertyDraw property, GGroupObjectValue fullKey, long requestIndex) {
        putToDoubleNativeMap(pendingLoadingPropertyRequests, property, fullKey, requestIndex);
    }

    public void setLoading(GFilterConditionView filterView, long requestIndex) {

        // RERENDER IF NEEDED : we have the previous state

        filterView.updateLoading(true);

        pendingLoadingFilterRequests.put(filterView, requestIndex);
    }
    // "external" update - paste + server update edit value
    public void setValue(EditContext editContext, PValue value) {
        if(setLocalValue(editContext, value, null) != null)
            update(editContext);
    }
    public void update(EditContext editContext) {
        update(editContext.getProperty(), editContext.getEditElement(), editContext.getUpdateContext());
    }
    public void update(GPropertyDraw property, Element element, UpdateContext updateContext) {
        if(isEdited(element))
            return;

        property.getCellRenderer(updateContext.getRendererType()).update(element, updateContext);
    }

    public boolean isEdited(Element element) {
        return editContext != null && getEditElement() == element;
    }


    // we want to set the colors for the td (not the sized element), since we usually want the background color to include td paddings
    // it's not pretty, but other solutions also are not (for example pulling td all over the stack, or including it in the update contexts, however later this might change)
    private static Element getColorElement(Element element) {
        Element parentElement = element.getParentElement();
        if(isTDorTH(parentElement))
            return parentElement;
        return element;
    }

    public static void updateFontColors(Element element, GFont font, String backgroundColor, String foregroundColor) {
        setFont(element, font);
        setBackgroundColor(element, backgroundColor);
        setForegroundColor(element, foregroundColor);
    }

    public static void clearColors(Element element) {
        setBackgroundColor(element, null);
        setForegroundColor(element, null);
    }

    private static void setBackgroundColor(Element element, String color) {
        element = getColorElement(element);

        if(color != null) {
            element.addClassName("cell-with-background");
        } else {
            element.removeClassName("cell-with-background");
        }
        setCellBackgroundColor(element, color);
    }

    private static native void setCellBackgroundColor(Element element, String background) /*-{
        // if pass null as a value, it will be undefined in browser.
        // in this case, the sticky column will become transparent in TREE view
        if (background != null) {
            element.style.setProperty("--bs-table-bg", background);
            element.style.setProperty("--bs-body-bg", background);
        } else {
            element.style.removeProperty("--bs-table-bg");
            element.style.removeProperty("--bs-body-bg");
        }
    }-*/;

    private static void setForegroundColor(Element element, String color) {
        if(color != null)
            element.addClassName("cell-with-foreground");
        else
            element.removeClassName("cell-with-foreground");

        setCellForegroundColor(element, color);
    }

    private static native void setCellForegroundColor(Element element, String color) /*-{
        // if pass null as a value, it will be undefined in browser
        if (color != null)
            element.style.setProperty("--foreground-color", color);
        else
            element.style.removeProperty("--foreground-color");
    }-*/;

    public static void setFont(Element element, GFont font) {
        if(font != null) {
            element.addClassName("cell-with-custom-font");
            setCellFont(element, font.family, font.size, font.italic, font.bold);
        } else {
            element.removeClassName("cell-with-custom-font");
            clearCellFont(element);
        }
    }

    public static void clearFont(Element element) {
        setFont(element, null);
    }

    private static native void setCellFont(Element element, String family, int size, boolean italic, boolean bold)/*-{
        if (family != null)
            element.style.setProperty("--custom-font-family", family);
        if (size > 0)
            element.style.setProperty("--custom-font-size", size + "px");
        element.style.setProperty("--custom-font-style", italic ? "italic" : "normal");
        element.style.setProperty("--custom-font-weight", bold ? "bold" : "normal");
    }-*/;

    private static native void clearCellFont(Element element)/*-{
        element.style.removeProperty("--custom-font-family");
        element.style.removeProperty("--custom-font-size");
        element.style.removeProperty("--custom-font-style");
        element.style.removeProperty("--custom-font-weight");
    }-*/;

    public static GFont getFont(GPropertyDraw property, RenderContext renderContext) {
        return property.font != null ? property.font : renderContext.getFont();
    }

    public void onPropertyBrowserEvent(EventHandler handler, Element renderElement, boolean isCell, Element focusElement, Consumer<EventHandler> onOuterEditBefore,
                                       Consumer<EventHandler> onEdit, Consumer<EventHandler> onOuterEditAfter, Consumer<EventHandler> onCut,
                                       Consumer<EventHandler> onPaste, boolean panel, boolean customRenderer, boolean focusable) {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        boolean isPropertyEditing = requestCellEditor != null && getEditElement() == renderElement;
        if(isPropertyEditing)
            requestCellEditor.onBrowserEvent(renderElement, handler);

        if(DataGrid.getBrowserTooltipMouseEvents().contains(handler.event.getType())) // just not to have problems in debugger
            return;

        if(handler.consumed)
            return;

        if(GMouseStroke.isChangeEvent(handler.event) && focusElement != null &&
                FocusUtils.getFocusedChild(focusElement) == null) { // need to check that focus is not on the grid, otherwise when editing for example embedded form, any click will cause moving focus to grid, i.e. stopping the editing
            if(focusable)
                FocusUtils.focus(focusElement, FocusUtils.Reason.MOUSECHANGE, handler.event); // it should be done on CLICK, but also on MOUSEDOWN, since we want to focus even if mousedown is later consumed
            else
                checkCommitEditing();
        }

        /*if(!previewBusyDialogDisplayerSinkEvents(handler.event)) {
            return;
        }*/
        if (renderElement != null)
            checkChangeEvent(handler, renderElement);

        if(handler.consumed)
            return;

        checkMouseKeyEvent(handler, true, isCell, panel, customRenderer);

        if(handler.consumed)
            return;

        onOuterEditBefore.accept(handler);

        if(handler.consumed)
            return;

        if (!isPropertyEditing) { // if editor did not consume event, we don't want it to be handled by "renderer" since it doesn't exist
            onEdit.accept(handler);
            if(handler.consumed)
                return;

            if (GKeyStroke.isCopyToClipboardEvent(handler.event)) {
                onCut.accept(handler);
            } else if (GKeyStroke.isPasteFromClipboardEvent(handler.event)) {
                onPaste.accept(handler);
            }
        }

        if(handler.consumed)
            return;

        onOuterEditAfter.accept(handler);

        if(handler.consumed)
            return;

        // if we consume mouse down we disable "text selection" feature
//        if(GMouseStroke.isDownEvent(handler.event)) // we want to cancel focusing (to avoid blinking if change event IS CLICK) + native selection odd behaviour (when some events are consumed, and some - not)
//            handler.consume(false, true); // but we want to propagate event upper (to GFormController to proceed bindings)

        checkMouseKeyEvent(handler, false, isCell, panel, customRenderer);
    }

    public void checkChangeEvent(EventHandler handler, Element renderElement) {
        InputElement inputElement;
        // there can be the CHANGE event that wasn't started with regular edit mechanism (for example if !isChangeOnSingleClick and user clicks on the input)
        // in this case we have to "cancel" the change
        // probably the same should be done for SimpleTextBasedRenderer but there are no such scenarios for now
        if(GKeyStroke.isChangeEvent(handler.event) &&
                (inputElement = InputBasedCellRenderer.getInputEventTarget(renderElement, handler.event)) != null &&
                InputBasedCellRenderer.getInputElementType(inputElement).isLogical()) {
            LogicalCellRenderer.cancelChecked(inputElement);
            handler.consume();
        }
    }

    public void resetWindowsLayout() {
        formsController.resetWindowsLayout();
    }
}