package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.*;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GLogMessageAction;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.controller.remote.action.*;
import lsfusion.gwt.client.controller.remote.action.form.*;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateID;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateIDResult;
import lsfusion.gwt.client.controller.remote.action.navigator.GainedFocus;
import lsfusion.gwt.client.form.ContainerForm;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.classes.view.ClassChosenHandler;
import lsfusion.gwt.client.form.classes.view.GClassDialog;
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
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.isChangeEvent;

public class GFormController implements EditManager {

    private static final ClientMessages messages = ClientMessages.Instance.get();

    private FormDispatchAsync dispatcher;

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

    private Set<ContainerForm> containerForms = new HashSet<>();

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
        if (filter.key != null)
            addBinding(new GKeyInputEvent(filter.key), (event) -> filterCheck.setValue(!filterCheck.getValue(), true), filterCheck, filterGroup.groupObject);
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
            if (filter.key != null)
                addBinding(new GKeyInputEvent(filter.key), (event) -> {
                    filterBox.setSelectedIndex(filterIndex + 1);
                    setRegularFilter(filterGroup, filterIndex);
                }, filterBox, filterGroup.groupObject);
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
                RendererType rendererType = RendererType.GRID;
                groupObject.columnSumWidth = groupObject.columnSumWidth.add(property.getValueWidth(font, true, true, rendererType));
                groupObject.columnCount++;
                groupObject.rowMaxHeight = groupObject.rowMaxHeight.max(property.getValueHeight(font, true, true, rendererType));
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

    public void executeNotificationAction(final Integer idNotification) throws IOException {
        syncResponseDispatch(new ExecuteNotification(idNotification));
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
                if (!formHidden) {
                    if (isShowing(getWidget()) && !MainFrame.isModalPopup()) {
                        executeFormEventAction(formScheduler, new ServerResponseCallback() {
                            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                                super.onSuccess(response, onDispatchFinished);
                                if (!formHidden && !formScheduler.fixed) {
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

    private boolean formVisible = true;

    public void gainedFocus() {
        asyncResponseDispatch(new GainedFocus());
        formVisible = true;
    }

    public void lostFocus() {
        formVisible = false;
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);

        if (hasColumnGroupObjects) // optimization
            fc.gridObjects.foreachEntry((key, value) -> currentGridObjects.put(key, value));

        int requestIndex = changesDTO.requestIndex;

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
            Widget childWidget = parentContainerView.getChildView(container);
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

    public void showClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        GClassDialog.showDialog(baseClass, defaultClass, concreate, classChosenHandler);
    }

    public long changeGroupObject(final GGroupObject group, GGroupObjectValue key) {
        long requestIndex = asyncDispatch(new ChangeGroupObject(group.ID, key), new ServerResponseCallback() {
            @Override
            public void onSuccess(ServerResponseResult response, Runnable onDispatchFinished) {
                // not sure what for it was used
                // DeferredRunner.get().commitDelayedGroupObjectChange(group);

                super.onSuccess(response, onDispatchFinished);
            }
        });
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
                valueRow.add(PValue.remapValueBack(property.parsePaste(sCell, externalType)));
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
            changeProperty(editContext, property.parsePaste(sValue, externalType), null);
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
                if (dispatcher.getLoadingManager().isVisible()) {
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

    public void executePropertyEventAction(EventHandler handler, boolean isBinding, ExecuteEditContext editContext) {
        Event event = handler.event;
        GPropertyDraw property = editContext.getProperty();
        if(property == null)  // in tree there can be no property in groups other than last
            return;

        if(BrowserEvents.CONTEXTMENU.equals(event.getType())) {
            handler.consume();
            GPropertyContextMenuPopup.show(property, event.getClientX(), event.getClientY(), actionSID -> {
                executePropertyEventAction(editContext, actionSID, handler);
            });
        } else {
            String actionSID;
            lsfusion.gwt.client.base.Result<Integer> contextAction = new lsfusion.gwt.client.base.Result<>();
            if(isBinding) {
                if(GKeyStroke.isKeyEvent(event)) // we don't want to set focus on mouse binding (it's pretty unexpected behaviour)
                    editContext.trySetFocusOnBinding(); // we want element to be focused on key binding (if it's possible)
                actionSID = GEditBindingMap.CHANGE;
            } else {
                actionSID = property.getEventSID(event, editContext, contextAction);
                if(actionSID == null)
                    return;
            }

            // hasChangeAction check is important for quickfilter not to consume event (however with propertyReadOnly, checkCanBeChanged there will be still some problems)
            if (isChangeEvent(actionSID) && (editContext.isReadOnly() != null || !property.hasUserChangeAction()))
                return;
            if(GEditBindingMap.EDIT_OBJECT.equals(actionSID) && !property.hasEditObjectAction)
                return;

            if(contextAction.result != null)
                executeContextAction(handler, editContext, contextAction.result);
            else
                executePropertyEventAction(editContext, actionSID, handler);

            if(!handler.consumed)
                handler.consume();
        }
    }

    public void executePropertyEventAction(ExecuteEditContext editContext, String actionSID, EventHandler handler) {
        GPropertyDraw property = editContext.getProperty();
        if (isChangeEvent(actionSID) && property.askConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", EscapeUtils.toHTML(property.askConfirmMessage), false, 0, 0, chosenOption -> {
                        if (chosenOption == DialogBoxHelper.OptionType.YES)
                            executePropertyEventActionConfirmed(editContext, actionSID, handler);
                    });
        } else
            executePropertyEventActionConfirmed(editContext, actionSID, handler);
    }

    public void executePropertyEventActionConfirmed(ExecuteEditContext editContext, String actionSID, EventHandler handler) {
        executePropertyEventAction(handler, editContext, editContext, actionSID, null, false, requestIndex -> setLoading(editContext, requestIndex));
    }

    public void executePropertyEventAction(GPropertyDraw property, GGroupObjectValue fullKey, String actionSID, GPushAsyncInput pushAsyncInput, boolean externalChange, Consumer<Long> onExec) {
        executePropertyEventAction(null, null, new ExecContext() {
            @Override
            public GPropertyDraw getProperty() {
                return property;
            }

            @Override
            public GGroupObjectValue getFullKey() {
                return fullKey;
            }
        }, actionSID, pushAsyncInput, externalChange, onExec);
    }

    public void executePropertyEventAction(EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncInput, boolean externalChange, Consumer<Long> onExec) {
        executePropertyEventAction(execContext.getProperty().getAsyncEventExec(actionSID), handler, editContext, execContext, actionSID, pushAsyncInput, externalChange, onExec);
    }

    private void executePropertyEventAction(GAsyncEventExec asyncEventExec, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncInput, boolean externalChange, Consumer<Long> onExec) {
        if (asyncEventExec != null)
            asyncEventExec.exec(this, handler, editContext, execContext, actionSID, pushAsyncInput, externalChange, onExec);
        else
            syncExecutePropertyEventAction(editContext, handler, execContext.getProperty(), execContext.getFullKey(), pushAsyncInput, actionSID, onExec);
    }

    public void asyncExecutePropertyEventAction(String actionSID, EditContext editContext, ExecContext execContext, EventHandler handler, GPushAsyncResult pushAsyncResult, boolean externaChange, Consumer<Long> onRequestExec, Consumer<Long> onExec) {
        long requestIndex = asyncExecutePropertyEventAction(actionSID, editContext, handler, new GPropertyDraw[]{execContext.getProperty()}, new boolean[]{externaChange}, new GGroupObjectValue[]{execContext.getFullKey()}, new GPushAsyncResult[]{pushAsyncResult});

        onExec.accept(requestIndex);

        // should be after because for example onExec can setRemoteValue, but onRequestExec also does that and should have higher priority
        onRequestExec.accept(requestIndex);
    }

    public void syncExecutePropertyEventAction(EditContext editContext, EventHandler handler, GPropertyDraw property, GGroupObjectValue fullKey, GPushAsyncInput pushAsyncResult, String actionSID, Consumer<Long> onExec) {
        long requestIndex = executePropertyEventAction(actionSID, true, editContext, handler, new GPropertyDraw[]{property}, new boolean[]{false}, new GGroupObjectValue[]{fullKey}, new GPushAsyncResult[] {pushAsyncResult});

        onExec.accept(requestIndex);
    }

    public long asyncExecutePropertyEventAction(String actionSID, EditContext editContext, EventHandler handler, GPropertyDraw[] properties, boolean[] externalChanges, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        return executePropertyEventAction(actionSID, false, editContext, handler, properties, externalChanges, fullKeys, pushAsyncResults);
    }

    private long executePropertyEventAction(String actionSID, boolean sync, EditContext editContext, EventHandler handler, GPropertyDraw[] properties, boolean[] externalChanges, GGroupObjectValue[] fullKeys, GPushAsyncResult[] pushAsyncResults) {
        int length = properties.length;
        int[] IDs = new int[length];
        GGroupObjectValue[] fullCurrentKeys = new GGroupObjectValue[length];
        for (int i = 0; i < length; i++) {
            GPropertyDraw property = properties[i];
            IDs[i] = property.ID;
            fullCurrentKeys[i] = getFullCurrentKey(property, fullKeys[i]);
        }

        ExecuteEventAction executeEventAction = new ExecuteEventAction(IDs, fullCurrentKeys, actionSID, externalChanges, pushAsyncResults);
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

    public void asyncInput(EventHandler handler, EditContext editContext, String actionSID, GAsyncInput asyncChange, Consumer<Long> onExec) {
        GInputList inputList = asyncChange.inputList;
        edit(asyncChange.changeType, handler, false, null, inputList, (value, onRequestExec) ->
            executePropertyEventAction(handler, editContext, inputList, value, actionSID, false, requestIndex -> {
                onExec.accept(requestIndex); // setLoading

                // doing that after to set the last value (onExec recursively can also set value)
                onRequestExec.accept(requestIndex); // pendingChangeProperty
        }), cancelReason -> {}, editContext, actionSID, asyncChange.customEditFunction);
    }

    private final static GAsyncNoWaitExec asyncExec = new GAsyncNoWaitExec();

    private void executePropertyEventAction(EventHandler handler, EditContext editContext, GInputList inputList, GUserInputResult value, String actionSID, boolean externalChange, Consumer<Long> onExec) {
        GInputListAction contextAction = getContextAction(inputList, value);
        executePropertyEventAction(contextAction != null ? contextAction.asyncExec : asyncExec,
                handler, editContext, editContext, actionSID, value != null ? new GPushAsyncInput(value) : null, externalChange, onExec);
    }

    private GInputListAction getContextAction(GInputList inputList, GUserInputResult value) {
        Integer contextActionIndex = value != null ? value.getContextAction() : null;
        if (contextActionIndex != null) {
            for (GInputListAction action : inputList.actions) {
                if (action.index == contextActionIndex)
                    return action;
            }
        }
        return null;
    }

    public void asyncOpenForm(GAsyncOpenForm asyncOpenForm, EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, externalChange, requestIndex -> {
            formsController.asyncOpenForm(getAsyncFormController(requestIndex), asyncOpenForm, editEvent, editContext, execContext, this);
        }, onExec);
    }

    public GAsyncFormController getAsyncFormController(long requestIndex) {
        return actionDispatcher.getAsyncFormController(requestIndex);
    }

    public void asyncCloseForm(GAsyncExecutor asyncExecutor) {
        if(needConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", messages.doYouReallyWantToCloseForm(), false, chosenOption -> {
                if(chosenOption == DialogBoxHelper.OptionType.YES) {
                    formsController.asyncCloseForm(asyncExecutor, formContainer);
                }
            });
        } else {
            formsController.asyncCloseForm(asyncExecutor, formContainer);
        }
    }

    public void asyncCloseForm(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        if(needConfirm) {
            DialogBoxHelper.showConfirmBox("lsFusion", messages.doYouReallyWantToCloseForm(), false, chosenOption -> {
                if(chosenOption == DialogBoxHelper.OptionType.YES) {
                    asyncCloseForm(editContext, execContext, handler, actionSID, externalChange, onExec);
                }
            });
        } else {
            asyncCloseForm(editContext, execContext, handler, actionSID, externalChange, onExec);
        }
    }

    private void asyncCloseForm(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, new GPushAsyncClose(), externalChange, requestIndex ->
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
    public void changeProperty(ExecuteEditContext editContext, PValue changeValue, ChangedRenderValueSupplier renderValueSupplier) {
        ChangedRenderValue changedRenderValue = changeValue != PValue.UNDEFINED ? setLocalValue(editContext, editContext.getProperty().getExternalChangeType(), changeValue, renderValueSupplier) : null;
        executePropertyEventAction(null, editContext, changeValue == PValue.UNDEFINED ? null : new GUserInputResult(changeValue), requestIndex -> {
            setRemoteValue(editContext, changedRenderValue, requestIndex);
        });
    }

    // for quick access actions (in toolbar)
    public void executeContextAction(ExecuteEditContext editContext, int contextAction) {
        executeContextAction(null, editContext, contextAction);
    }
    // for quick access actions (in toolbar and keypress)
    public void executeContextAction(EventHandler handler, ExecuteEditContext editContext, int contextAction) {
        executePropertyEventAction(handler, editContext, new GUserInputResult(null, contextAction), requestIndex -> {});
    }

    // for custom renderer, paste, quick access actions (in toolbar and keypress)
    private void executePropertyEventAction(EventHandler handler, ExecuteEditContext editContext, GUserInputResult value, Consumer<Long> onExec) {
        executePropertyEventAction(handler, editContext, editContext.getProperty().getInputList(), value, GEditBindingMap.CHANGE, true, requestIndex -> {
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

    public void asyncNoWait(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncNoWaitExec asyncNoWait, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, externalChange, requestIndex -> {}, onExec);
    }

    public void asyncChange(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncChange asyncChange, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, externalChange, requestIndex -> {
            for (int propertyID : asyncChange.propertyIDs)
                setLoadingValueAt(propertyID, editContext.getFullKey(), PValue.remapValue(asyncChange.value), requestIndex);
        }, onExec);
    }

    public void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GAsyncAddRemove asyncAddRemove, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        final GObject object = form.getObject(asyncAddRemove.object);
        final boolean add = asyncAddRemove.add;

        GGridController controller = controllers.get(object.groupObject);

        final int position = controller.getSelectedRow();

        if (add) {
            MainFrame.logicsDispatchAsync.execute(new GenerateID(), new PriorityErrorHandlingCallback<GenerateIDResult>() {
                @Override
                public void onSuccess(GenerateIDResult result) {
                    asyncAddRemove(editContext, execContext, handler, actionSID, object, add, new GPushAsyncAdd(result.ID), new GGroupObjectValue(object.ID, new GCustomObjectValue(result.ID, null)), position, externalChange, onExec);
                }
            });
        } else {
            DeferredRunner.get().commitDelayedGroupObjectChange(object.groupObject);

            final GGroupObjectValue value = controllers.get(object.groupObject).getSelectedKey();
            if(value.isEmpty())
                return;
            asyncAddRemove(editContext, execContext, handler, actionSID, object, add, pushAsyncResult, value, position, externalChange, onExec);
        }
    }

    private void asyncAddRemove(EditContext editContext, ExecContext execContext, EventHandler handler, String actionSID, GObject object, boolean add, GPushAsyncResult pushAsyncResult, GGroupObjectValue value, int position, boolean externalChange, Consumer<Long> onExec) {
        asyncExecutePropertyEventAction(actionSID, editContext, execContext, handler, pushAsyncResult, externalChange, requestIndex -> {
            pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex);
            pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));

            controllers.get(object.groupObject).modifyGroupObject(value, add, -1);
        }, onExec);
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType) {
        syncResponseDispatch(new ChangePropertyOrder(property.ID, columnKey, modiType));
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
        return applyCurrentFilters();
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

        return applyCurrentFilters();
    }

    private long applyCurrentFilters() {
        ArrayList<GPropertyFilterDTO> filters = new ArrayList<>();
        currentFilters.foreachValue(groupFilters -> groupFilters.stream().filter(filter -> !filter.property.isAction()).map(GPropertyFilter::getFilterDTO).collect(Collectors.toCollection(() -> filters)));
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

    public abstract static class CustomErrorHandlingCallback<T> extends RequestCountingErrorHandlingCallback<T> {

        protected abstract void onSuccess(T result);

        @Override
        public void onSuccess(T result, Runnable onDispatchFinished) {
            onSuccess(result);

            if(onDispatchFinished != null)
                onDispatchFinished.run();
        }
    }

    public void countRecords(final GGroupObject groupObject, int clientX, int clientY) {
        asyncDispatch(new CountRecords(groupObject.ID), new CustomErrorHandlingCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                controllers.get(groupObject).showRecordQuantity((Integer) result.value, clientX, clientY);
            }
        });
    }

    public void calculateSum(final GGroupObject groupObject, final GPropertyDraw propertyDraw, GGroupObjectValue columnKey, int clientX, int clientY) {
        asyncDispatch(new CalculateSum(propertyDraw.ID, columnKey), new CustomErrorHandlingCallback<NumberResult>() {
            @Override
            public void onSuccess(NumberResult result) {
                controllers.get(groupObject).showSum(result.value, propertyDraw, clientX, clientY);
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
        syncDispatch(new GroupReport(groupObjectID, getUserPreferences()), new CustomErrorHandlingCallback<GroupReportResult>() {
            @Override
            public void onSuccess(GroupReportResult result) {
                GwtClientUtils.openFile(result.filename, false, null);
            }
        });
    }

    public void saveUserPreferences(GGridUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps, final AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences.convertPreferences(), forAllUsers, completeOverride, hiddenProps), new CustomErrorHandlingCallback<ServerResponseResult>() {
            @Override
            public void onSuccess(ServerResponseResult response) {
                for (GAction action : response.actions) {
                    if (action instanceof GLogMessageAction) {
                        actionDispatcher.execute((GLogMessageAction) action);
                        callback.onFailure(new Throwable());
                        return;
                    }
                }
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);

                super.onFailure(caught);
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

    public boolean previewEvent(Element target, Event event) {
        if(BrowserEvents.BLUR.equals(event.getType()))
            MainFrame.setLastBlurredElement(Element.as(event.getEventTarget()));
        formsController.checkEditModeEvents(event);
        return previewLoadingManagerSinkEvents(event) && MainFrame.previewEvent(target, event, isEditing());
    }

    public GFormController contextEditForm;
    public void propagateFocusEvent(Event event) {
//        it seems that now we don't need it, because now we have getBrowserTargetAndCheck fix
//        if(BrowserEvents.BLUR.equals(event.getType()) && contextEditForm != null)
//            contextEditForm.getRequestCellEditor().onBrowserEvent(contextEditForm.getEditElement(), new EventHandler(event));
    }

    private boolean previewLoadingManagerSinkEvents(Event event) {
        //focus() can trigger blur event, blur finishes editing. Editing calls syncDispatch.
        //If isEditing() and loadingManager isVisible() then flushCompletedRequests is not executed and syncDispatch is blocked.
        return !(dispatcher.getLoadingManager().isVisible() && (DataGrid.checkSinkEvents(event) || DataGrid.checkSinkFocusEvents(event)));
    }

    protected void onFormHidden(GAsyncFormController asyncFormController, int closeDelay, EndReason editFormCloseReason) {
        formsController.removeFormContainer(formContainer);
        for(ContainerForm containerForm : containerForms) {
            containerForm.getForm().closePressed(editFormCloseReason);
        }

        FormDispatchAsync closeDispatcher = dispatcher;
        Scheduler.get().scheduleDeferred(() -> {
            closeDispatcher.executePriority(new Close(closeDelay), new PriorityErrorHandlingCallback<VoidResult>() {
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
        return !formHidden && formVisible;
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
            BaseImage.updateText(captionWidget, container.caption, false);
    }
    public void updateImage(GContainer container) {
        Widget captionWidget = getCaptionWidget(container);
        if(captionWidget != null)
            BaseImage.updateImage(container.image, captionWidget, false);
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

    public void focusFirstWidget(FocusUtils.Reason reason) {
        if (formLayout.focusDefaultWidget(reason)) {
            return;
        }

        Element nextFocusElement = GPanelController.getNextFocusElement(getWidget().getElement(), true);
        if(nextFocusElement != null) {
            FocusUtils.focus(nextFocusElement, reason);
        } else {
            //focus form container if no one element is focusable
            FocusUtils.focus(formContainer.getFocusedElement(), reason);
        }
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

    public int addBinding(GInputEvent event, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event, GBindingEnv.AUTO, pressed, component, groupObject);
    }
    public int addBinding(GInputEvent event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event::isEvent, env, pressed, component, groupObject);
    }
    public int addBinding(BindingCheck event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        return addBinding(event, env, null, pressed, component, groupObject);
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
                new GBindingEnv(-100, null, null, bindGroup, GBindingMode.NO, null, null, null),  // bindEditing - NO, because we don't want for example when editing text in grid to catch enter
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
                if(bindPreview(bindingEnv, isMouse, preview) &&
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
    }

    private boolean bindPreview(GBindingEnv binding, boolean isMouse, boolean preview) {
        switch (binding.bindPreview) {
            case AUTO:
                return isMouse || !preview;
            case NO:
                return !preview;
            case ALL: // actually makes no since if previewed, than will be consumed so equivalent to only
                return true;
            case ONLY:
                return preview;
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
                return !isEditing() || !targetElementIsEditing(event);
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

    private boolean targetElementIsEditing(Event event) {
        return editContext.getEditElement().isOrHasChild(Element.as(event.getEventTarget()));
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
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(ListResult result) {
                callback.onSuccess(convertAsyncResult(result));
            }
        });
    }

    public void getAsyncValues(String value, AsyncCallback<GAsyncResult> callback) {
        if(editContext != null) // just in case
            getAsyncValues(value, editContext, editAsyncValuesSID, callback);
    }

    public void getAsyncValues(String value, EditContext editContext, String actionSID, AsyncCallback<GAsyncResult> callback) {
        getAsyncValues(value, editContext.getProperty(), editContext.getColumnKey(), actionSID, callback);
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
    public void getAsyncValues(String value, GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<GAsyncResult> callback) {
        int editIndex = editAsyncIndex++;
        AsyncCallback<GAsyncResult> fCallback = checkLast(editIndex, callback);

        GGroupObjectValue currentKey = getFullCurrentKey(property, columnKey);

        Runnable runPessimistic = () -> getPessimisticValues(property.ID, currentKey, actionSID, value, editIndex, fCallback);
        if (!editAsyncUsePessimistic)
            dispatcher.executePriority(new GetPriorityAsyncValues(property.ID, currentKey, actionSID, value, editIndex), new PriorityAsyncCallback<ListResult>() {
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

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue setOldValue, GInputList inputList, BiConsumer<GUserInputResult, Consumer<Long>> afterCommit, Consumer<CancelReason> cancel, EditContext editContext, String actionSID, String customChangeFunction) {
        assert type != null;
        lsfusion.gwt.client.base.Result<ChangedRenderValue> changedRenderValue = new lsfusion.gwt.client.base.Result<>();
        edit(type, handler, hasOldValue, setOldValue, inputList, // actually it's assumed that actionAsyncs is used only here, in all subsequent calls it should not be referenced
                (inputResult, commitReason) -> changedRenderValue.set(setLocalValue(editContext, type, inputResult.getPValue(), null)),
                (inputResult, commitReason) -> afterCommit.accept(inputResult, requestIndex -> setRemoteValue(editContext, changedRenderValue.result, requestIndex)),
                cancel, editContext, actionSID, customChangeFunction);
    }

    public void edit(GType type, EventHandler handler, boolean hasOldValue, PValue oldValue, GInputList inputList, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, String customChangeFunction) {
        GPropertyDraw property = editContext.getProperty();

        RequestValueCellEditor cellEditor;
        boolean hasCustomEditor = customChangeFunction != null && !customChangeFunction.equals("DEFAULT");
        if (hasCustomEditor) // see LsfLogics.g propertyCustomView rule
            cellEditor = CustomReplaceCellEditor.create(this, property, type, customChangeFunction);
        else {
            if(property.echoSymbols) // disabling dropdown if echo
                inputList = null;

            cellEditor = type.createCellEditor(this, property, inputList, editContext);
        }

        if (cellEditor != null) {
            boolean canUseChangeValueForRendering = editContext.canUseChangeValueForRendering(type);
            if(canUseChangeValueForRendering)
                cellEditor.setCancelTheSameValueOnBlur(editContext.getValue());

            if(!hasOldValue) { // property.baseType.equals(type) actually there should be something like compatible, but there is no such method for now, so we'll do this check in editors
                oldValue = editContext.getValue();
                if(oldValue == null)
                    oldValue = cellEditor.getDefaultNullValue();
                else if(!canUseChangeValueForRendering && !hasCustomEditor) {
                    try {
                        oldValue = type.parseString(PValue.getStringValue(oldValue), property.pattern);
                    } catch (ParseException e) {
                        oldValue = null;
                    }
                }
            }

            edit(cellEditor, handler, oldValue, beforeCommit, afterCommit, cancel, editContext, editAsyncValuesSID, -1);
        } else
            cancel.accept(CancelReason.OTHER);
    }

    public void edit(CellEditor cellEditor, EventHandler handler, PValue oldValue, BiConsumer<GUserInputResult, CommitReason> beforeCommit, BiConsumer<GUserInputResult, CommitReason> afterCommit,
                     Consumer<CancelReason> cancel, EditContext editContext, String editAsyncValuesSID, long editRequestIndex) {
        // because for example embedded / popup (that use edit) forms are opened with a timer, there can be some pending edit calls so we need to avoid this
        checkCommitEditing();

        assert this.editContext == null;
        editBeforeCommit = beforeCommit;
        editAfterCommit = afterCommit;
        editCancel = cancel;

        this.editAsyncValuesSID = editAsyncValuesSID;

        this.editContext = editContext;
        this.editRequestIndex = editRequestIndex;  // we need to force dispatch responses till this index because otherwise we won't

        Element element = getEditElement();

        editContext.startEditing();

        if(!editContext.isFocusable()) // assert that otherwise it's already has focus
            forceSetFocus = editContext.forceSetFocus();

        RenderContext renderContext;
        if (cellEditor instanceof ReplaceCellEditor && ((ReplaceCellEditor) cellEditor).needReplace(element, renderContext = editContext.getRenderContext())) {
            focusedElement = GwtClientUtils.getFocusedElement();

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
        cellEditor.start(handler, element, oldValue); //need to be after this.cellEditor = cellEditor, because there is commitEditing in start in LogicalCellEditor
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
        finishEditing(false, true);

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

        if(isReplace) {

            if(blurred) { // when editing is commited (thus editing element is removed), set last blurred element to main widget to keep focus there
                if(editContext.isSetLastBlurred()) {
                    Element focusElement = editContext.getFocusElement();
                    if(focusElement != null)
                        MainFrame.setLastBlurredElement(focusElement);
                }
            } else {
                if (focusedElement != null) {
                    FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
                    focusedElement = null;
                }
            }
        }
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
        editContext.setValue(value);

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
        return editContext != null && editContext.getEditElement() == element;
    }

    public static void setBackgroundColor(Element element, String color) {
        if(color != null) {
            element.addClassName("cell-with-background");
        } else {
            element.removeClassName("cell-with-background");
        }
        setCellBackgroundColor(element, color);
    }

    private static native void setCellBackgroundColor(Element element, String background) /*-{
        element.style.setProperty("--bs-table-bg", background);
        element.style.setProperty("--bs-body-bg", background);
    }-*/;

    public static void setForegroundColor(Element element, String color) {
        if (color != null) {
            element.getStyle().setColor(color);
        } else {
            element.getStyle().clearColor();
        }
    }

    public void onPropertyBrowserEvent(EventHandler handler, Element renderElement, boolean isCell, Element focusElement, Consumer<EventHandler> onOuterEditBefore,
                                       Consumer<EventHandler> onEdit, Consumer<EventHandler> onOuterEditAfter, Consumer<EventHandler> onCut,
                                       Consumer<EventHandler> onPaste, boolean panel, boolean customRenderer) {
        RequestCellEditor requestCellEditor = getRequestCellEditor();
        boolean isPropertyEditing = requestCellEditor != null && getEditElement() == renderElement;
        if(isPropertyEditing)
            requestCellEditor.onBrowserEvent(renderElement, handler);

        if(DataGrid.getBrowserTooltipMouseEvents().contains(handler.event.getType())) // just not to have problems in debugger
            return;

        if(handler.consumed)
            return;

        if(GMouseStroke.isChangeEvent(handler.event) && focusElement != null &&
                GwtClientUtils.getFocusedChild(focusElement) == null) // need to check that focus is not on the grid, otherwise when editing for example embedded form, any click will cause moving focus to grid, i.e. stopping the editing
            FocusUtils.focus(focusElement, FocusUtils.Reason.MOUSECHANGE); // it should be done on CLICK, but also on MOUSEDOWN, since we want to focus even if mousedown is later consumed

        /*if(!previewLoadingManagerSinkEvents(handler.event)) {
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
        if(BrowserEvents.CHANGE.equals(handler.event.getType()) && (inputElement = LogicalCellRenderer.getInputEventTarget(renderElement, handler.event)) != null) {
            LogicalCellRenderer.cancelChecked(inputElement);
            handler.consume();
        }
    }

    public void resetWindowsLayout() {
        formsController.resetWindowsLayout();
    }
}