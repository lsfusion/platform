package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.Dimension;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.WrapperAsyncCallbackEx;
import lsfusion.gwt.base.client.jsni.Function2;
import lsfusion.gwt.base.client.jsni.NativeHashMap;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.base.client.ui.GKeyStroke;
import lsfusion.gwt.base.client.ui.ResizableSimplePanel;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.base.shared.actions.ListResult;
import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.*;
import lsfusion.gwt.form.client.dispatch.DeferredRunner;
import lsfusion.gwt.form.client.dispatch.FormDispatchAsync;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.FormsController;
import lsfusion.gwt.form.client.form.ServerMessageProvider;
import lsfusion.gwt.form.client.form.dispatch.GFormActionDispatcher;
import lsfusion.gwt.form.client.form.dispatch.GSimpleChangePropertyDispatcher;
import lsfusion.gwt.form.client.form.ui.classes.ClassChosenHandler;
import lsfusion.gwt.form.client.form.ui.classes.GResizableClassDialog;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.client.form.ui.toolbar.preferences.GGridUserPreferences;
import lsfusion.gwt.form.shared.actions.form.*;
import lsfusion.gwt.form.shared.actions.navigator.GenerateID;
import lsfusion.gwt.form.shared.actions.navigator.GenerateIDResult;
import lsfusion.gwt.form.shared.view.*;
import lsfusion.gwt.form.shared.view.actions.GAction;
import lsfusion.gwt.form.shared.view.actions.GLogMessageAction;
import lsfusion.gwt.form.shared.view.changes.GFormChanges;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValueBuilder;
import lsfusion.gwt.form.shared.view.changes.dto.GFormChangesDTO;
import lsfusion.gwt.form.shared.view.changes.dto.GPropertyFilterDTO;
import lsfusion.gwt.form.shared.view.classes.GObjectClass;
import lsfusion.gwt.form.shared.view.filter.GPropertyFilter;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import lsfusion.gwt.form.shared.view.panel.PanelRenderer;
import lsfusion.gwt.form.shared.view.window.GModalityType;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static lsfusion.gwt.base.client.GwtClientUtils.isShowing;
import static lsfusion.gwt.base.client.GwtClientUtils.setupFillParent;
import static lsfusion.gwt.base.shared.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.base.shared.GwtSharedUtils.removeFromDoubleMap;

public class GFormController extends ResizableSimplePanel implements ServerMessageProvider {
    private static final int ASYNC_TIME_OUT = 50;

    private final FormDispatchAsync dispatcher;

    private final GFormActionDispatcher actionDispatcher;
    private final GSimpleChangePropertyDispatcher simpleDispatcher;

    private final FormsController formsController;

    private final GForm form;
    protected final GFormLayout formLayout;

    private final boolean isModal;
    private final boolean isDialog;

    private final HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects = new HashMap<>();

    private final Map<GGroupObject, List<GPropertyFilter>> currentFilters = new HashMap<>();

    private final Map<GGroupObject, GGroupObjectController> controllers = new LinkedHashMap<>();
    private final Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();

    private final Map<GGroupObject, List<Widget>> filterViews = new HashMap<>();

    private final LinkedHashMap<Integer, ModifyObject> pendingModifyObjectRequests = new LinkedHashMap<>();
    private final NativeHashMap<GGroupObject, Integer> pendingChangeCurrentObjectsRequests = new NativeHashMap<>();
    private final NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>> pendingChangePropertyRequests = new NativeHashMap<>();

    private boolean initialFormChangesReceived = false;
    private boolean defaultOrdersInitialized = false;
    private boolean hasColumnGroupObjects;

    private Timer asyncTimer;
    private PanelRenderer asyncView;

    private boolean initialResizeProcessed = false;

    private HotkeyManager hotkeyManager = new HotkeyManager();
    private LoadingManager loadingManager = MainFrame.isBusyDialog ? new GBusyDialogDisplayer(this) : new LoadingBlocker(this);

    private boolean blocked = false;
    private boolean selected = true;
    private boolean formHidden = false;

    public FormsController getFormsController() {
        return formsController;
    }

    public GFormController(FormsController formsController, GForm gForm) {
        this(formsController, gForm, false, false);
    }

    public GFormController(FormsController formsController, GForm gForm, final boolean isModal, boolean isDialog) {
        actionDispatcher = new GFormActionDispatcher(this);
        simpleDispatcher = new GSimpleChangePropertyDispatcher(this);

        this.formsController = formsController;
        this.form = gForm;
        this.isModal = isModal;
        this.isDialog = isDialog;

        dispatcher = new FormDispatchAsync(this);

        formLayout = new GFormLayout(this, form.mainContainer);

        asyncTimer = new Timer() {
            @Override
            public void run() {
                asyncView.setIcon("loading.gif");
            }
        };

        if (form.sID != null) {
            getElement().setAttribute("lsfusion-form", form.sID);
        }

        add(formLayout);
        setupFillParent(getElement(), formLayout.getElement());

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        if (form.initialFormChanges != null) {
            applyRemoteChanges(form.initialFormChanges);
            form.initialFormChanges = null;
        } else if (!initialFormChangesReceived) { // возможно уже получили в initializeDefaultOrders()
            getRemoteChanges();
        }

        initializeAutoRefresh();

        hotkeyManager.install(this);
    }

    protected void dropCurrentForm() {
        if(formsController != null)
        formsController.dropCurForm(this);
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
        final CheckBox filterCheck = new CheckBox(filter.getFullCaption());
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                setRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        filterCheck.addStyleName("checkBoxFilter");
        addFilterView(filterGroup, filterCheck);

        if (filterGroup.defaultFilterIndex >= 0) {
            filterCheck.setValue(true, false);
        }

        filterCheck.getElement().setPropertyObject("groupObject", filterGroup.groupObject);
        if (filter.key != null) {
            addHotkeyBinding(filterGroup.groupObject, filter.key, new HotkeyManager.Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    if (!isEditing() && isShowing(filterCheck)) {
                        filterCheck.setValue(!filterCheck.getValue(), true);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox();
        filterBox.setMultipleSelect(false);
        filterBox.addItem("(Все)", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            final GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.getFullCaption(), "" + i);

            final int filterIndex = i;
            filterBox.getElement().setPropertyObject("groupObject", filterGroup.groupObject);
            if (filter.key != null) {
                addHotkeyBinding(filterGroup.groupObject, filter.key, new HotkeyManager.Binding() {
                    @Override
                    public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                        if (!isEditing() && isShowing(filterBox)) {
                            filterBox.setSelectedIndex(filterIndex + 1);
                            setRegularFilter(filterGroup, filterIndex);
                            return true;
                        }
                        return false;
                    }
                });
            }

        }

        filterBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setRegularFilter(filterGroup, filterBox.getSelectedIndex() - 1);
            }
        });

        filterBox.addStyleName("comboBoxFilter");

        addFilterView(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + 1);
        }
    }

    private void addFilterView(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.add(filterGroup, filterWidget);

        if (filterGroup.groupObject == null) {
            return;
        }

        List<Widget> groupFilters = filterViews.get(filterGroup.groupObject);
        if (groupFilters == null) {
            groupFilters = new ArrayList<>();
            filterViews.put(filterGroup.groupObject, groupFilters);
        }
        groupFilters.add(filterWidget);
    }

    public void setFiltersVisible(GGroupObject groupObject, boolean visible) {
        List<Widget> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null) {
            for (Widget filterView : groupFilters) {
                filterView.setVisible(visible);
            }
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, int index) {
        setRemoteRegularFilter(filterGroup, index == -1 ? null : filterGroup.filters.get(index));
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form);
            treeControllers.put(treeGroup, treeController);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GGroupObjectController controller = new GGroupObjectController(this, group, form.userPreferences != null ? extractUserPreferences(form.userPreferences, group) : null);
                controllers.put(group, controller);
            }
        }

        hasColumnGroupObjects = false;
        for (GPropertyDraw property : form.propertyDraws) {
            if (property.columnGroupObjects != null && !property.columnGroupObjects.isEmpty()) {
                hasColumnGroupObjects = true;
            }
            if (property.groupObject == null) {
                controllers.put(null, new GGroupObjectController(this));
            }
        }
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
            LinkedHashMap<GPropertyDraw, Boolean> order = orders.get(groupObject);
            if(order == null) {
                order = new LinkedHashMap<>();
                orders.put(groupObject,order);
            }
            order.put(defaultOrder.getKey(), defaultOrder.getValue());
        }
        return orders;
    }

    public void initializeDefaultOrders() {
        applyOrders(form.defaultOrders, null);
        defaultOrdersInitialized = true;

        boolean hasUserOrders = false;
        Map<GGroupObject, LinkedHashMap<GPropertyDraw, Boolean>> defaultOrders = null;
        for (GGroupObjectController controller : controllers.values()) {
            LinkedHashMap<GPropertyDraw, Boolean> objectUserOrders = controller.getUserOrders();
            if (objectUserOrders != null) {
                if (defaultOrders == null)
                    defaultOrders = groupDefaultOrders();
                LinkedHashMap<GPropertyDraw, Boolean> defaultObjectOrders = defaultOrders.get(controller.groupObject);
                if (defaultObjectOrders == null)
                    defaultObjectOrders = new LinkedHashMap<>();
                if (!GwtSharedUtils.hashEquals(defaultObjectOrders, objectUserOrders)) {
                    applyOrders(objectUserOrders, controller);
                    hasUserOrders = true;
                }
            }
        }
        if (hasUserOrders)
            getRemoteChanges();
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders(GGroupObject groupObject) {
        return form.getDefaultOrders(groupObject);
    }

    public void executeNotificationAction(final Integer idNotification) throws IOException {
        syncDispatch(new ExecuteNotification(idNotification), new ServerResponseCallback());
    }

    private void initializeAutoRefresh() {
        if (form.autoRefresh > 0) {
            scheduleRefresh();
        }
    }

    private void scheduleRefresh() {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (!formHidden) {
                    if (isShowing(GFormController.this)) {
                        dispatcher.execute(new GetRemoteChanges(true), new ServerResponseCallback() {
                            @Override
                            public void success(ServerResponseResult response) {
                                super.success(response);
                                if (!formHidden) {
                                    scheduleRefresh();
                                }
                            }
                        });
                    } else {
                        scheduleRefresh();   
                    }
                }
                return false;
            }
        }, form.autoRefresh * 1000);
    }

    public void applyOrders(LinkedHashMap<GPropertyDraw, Boolean> orders, GGroupObjectController groupObjectController) {
        Set<GGroupObject> wasOrder = new HashSet<>();
        for (Map.Entry<GPropertyDraw, Boolean> entry : orders.entrySet()) {
            GPropertyDraw property = entry.getKey();
            GGroupObject groupObject = property.groupObject;
            assert groupObjectController == null || groupObject.equals(groupObjectController.groupObject);
            GGroupObjectLogicsSupplier groupObjectLogicsSupplier = getGroupObjectLogicsSupplier(groupObject);
            if (groupObjectLogicsSupplier != null) {
                groupObjectLogicsSupplier.changeOrder(property, !wasOrder.contains(groupObject) ? GOrder.REPLACE : GOrder.ADD);
                wasOrder.add(groupObject);
                if (!entry.getValue()) {
                    groupObjectLogicsSupplier.changeOrder(property, GOrder.DIR);
                }
            }
        }
        if(groupObjectController != null) {
            GGroupObject groupObject = groupObjectController.groupObject;
            if(!wasOrder.contains(groupObject)) {
                groupObjectController.clearOrders(groupObject);
            }
        }
    }

    public GPropertyDraw getProperty(int id) {
        return form.getProperty(id);
    }

    public GGroupObject getGroupObject(int groupID) {
        return form.getGroupObject(groupID);
    }

    public void getRemoteChanges() {
        dispatcher.execute(new GetRemoteChanges(), new ServerResponseCallback());
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);

        //оптимизация, т.к. на большинстве форм нет групп в колонках
        if (hasColumnGroupObjects) {
            for (Map.Entry<GGroupObject, GClassViewType> entry : fc.classViews.entrySet()) {
                GClassViewType classView = entry.getValue();
                if (classView != GClassViewType.GRID) {
                    currentGridObjects.remove(entry.getKey());
                }
            }
            currentGridObjects.putAll(fc.gridObjects);
        }

        modifyFormChangesWithModifyObjectAsyncs(changesDTO.requestIndex, fc);

        modifyFormChangesWithChangeCurrentObjectAsyncs(changesDTO.requestIndex, fc);

        modifyFormChangesWithChangePropertyAsyncs(changesDTO.requestIndex, fc);

        // затем одним скопом обновляем данные во всех таблицах
        for (GGroupObjectController controller : controllers.values()) {
            controller.processFormChanges(fc, currentGridObjects);
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(fc);
        }

        formLayout.hideEmptyContainerViews();

        onResize();

        // в конце скроллим все таблицы к текущим ключам
        applyScrollPositions();

        if (!initialFormChangesReceived) {
            onInitialFormChangesReceived();
            initialFormChangesReceived = true;
        }
    }

    private void applyScrollPositions() {
        for (GGroupObjectController controller : controllers.values()) {
            controller.restoreScrollPosition();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.restoreScrollPosition();
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(final int currentDispatchingRequestIndex, GFormChanges fc) {
        for (Iterator<Map.Entry<Integer, ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, ModifyObject> cell = iterator.next();
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

        for (Map.Entry<Integer, ModifyObject> e : pendingModifyObjectRequests.entrySet()) {
            ArrayList<GGroupObjectValue> gridObjects = fc.gridObjects.get(e.getValue().object.groupObject);
            if (gridObjects != null) {
                if (e.getValue().add) {
                    gridObjects.add(e.getValue().value);
                } else {
                    gridObjects.remove(e.getValue().value);
                }
            }
        }
    }

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangeCurrentObjectsRequests.foreachEntry(new Function2<GGroupObject, Integer>() {
            @Override
            public void apply(GGroupObject group, Integer requestIndex) {
                if (requestIndex <= currentDispatchingRequestIndex) {
                    pendingChangeCurrentObjectsRequests.remove(group);
                } else {
                    fc.objects.remove(group);
                }
            }
        });
    }

    private void modifyFormChangesWithChangePropertyAsyncs(final int currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangePropertyRequests.foreachEntry(new Function2<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>>() {
            @Override
            public void apply(final GPropertyDraw property, NativeHashMap<GGroupObjectValue, Change> values) {
                values.foreachEntry(new Function2<GGroupObjectValue, Change>() {
                    @Override
                    public void apply(GGroupObjectValue keys, Change change) {
                        int requestIndex = change.requestIndex;
                        if (requestIndex <= currentDispatchingRequestIndex) {

                            removeFromDoubleMap(pendingChangePropertyRequests, property, keys);

                            HashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(property);
                            if (propertyValues == null) {
                                // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                                propertyValues = new HashMap<>();
                                fc.properties.put(property, propertyValues);
                                fc.updateProperties.add(property);
                            }

                            if (fc.updateProperties.contains(property) && !propertyValues.containsKey(keys)) {
                                propertyValues.put(keys, change.oldValue);
                            }
                        }
                    }
                });
            }
        });

        pendingChangePropertyRequests.foreachEntry(new Function2<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>>() {
            @Override
            public void apply(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Change> values) {
                final HashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(property);
                if (propertyValues != null) {
                    values.foreachEntry(new Function2<GGroupObjectValue, Change>() {
                        @Override
                        public void apply(GGroupObjectValue group, Change change) {
                            if (change.canUseNewValueForRendering) {
                                propertyValues.put(group, change.newValue);
                            }
                        }
                    });
                }
            }
        });
    }

    public void openForm(GForm form, GModalityType modalityType, EditEvent initFilterEvent, final WindowHiddenHandler handler) {
        if (modalityType == GModalityType.DOCKED_MODAL) {
            block();
        }

        Widget blockingWidget = formsController.openForm(form, modalityType, initFilterEvent, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                unblock();
                handler.onHidden();
            }
        });

        if (modalityType == GModalityType.DOCKED_MODAL) {
            setBlockingWidget(blockingWidget);
        }
        formsController.setCurrentForm(form.sID);
    }

    public void showClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        GResizableClassDialog.showDialog(baseClass, defaultClass, concreate, classChosenHandler);
    }

    public void changeGroupObject(final GGroupObject group, GGroupObjectValue key) {
        int requestIndex = dispatcher.execute(new ChangeGroupObject(group.ID, key), new ServerResponseCallback() {
            @Override
            public void preProcess() {
                DeferredRunner.get().commitDelayedGroupObjectChange(group);
            }
        });
        pendingChangeCurrentObjectsRequests.put(group, requestIndex);
    }

    public void changeGroupObjectLater(final GGroupObject group, final GGroupObjectValue key) {
        DeferredRunner.get().scheduleDelayedGroupObjectChange(group, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changeGroupObject(group, key);
            }
        });
    }

    public void pasteExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, List<List<String>> table, int maxColumns) {
        ArrayList<ArrayList<Object>> values = new ArrayList<>();

        for (List<String> sRow : table) {
            ArrayList<Object> valueRow = new ArrayList<>();

            int rowLength = Math.min(sRow.size(), maxColumns);
            for (int i = 0; i < rowLength; i++) {
                GPropertyDraw property = propertyList.get(i);
                String sCell = sRow.get(i);
                valueRow.add(
                        property.parseChangeValueOrNull(sCell)
                );
            }
            values.add(valueRow);
        }

        final ArrayList<Integer> propertyIdList = new ArrayList<>();
        for (GPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.ID);
        }

        syncDispatch(new PasteExternalTable(propertyIdList, columnKeys, values), new ServerResponseCallback());

    }

    public void pasteSingleValue(GPropertyDraw property, GGroupObjectValue columnKey, String sValue) {
        Serializable value = (Serializable) property.parseChangeValueOrNull(sValue);
        syncDispatch(new PasteSingleCellValue(property.ID, columnKey, value), new ServerResponseCallback());
    }

    public void changePageSizeAfterUnlock(final GGroupObject groupObject, final int pageSize) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (loadingManager.isVisible()) {
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
        dispatcher.execute(new ChangePageSize(groupObject.ID, pageSize), new ServerResponseCallback());
    }

    public void scrollToEnd(GGroupObject group, boolean toEnd) {
        syncDispatch(new ScrollToEnd(group.ID, toEnd), new ServerResponseCallback());
    }

    public void executeEditAction(GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<ServerResponseResult> callback) {
        DeferredRunner.get().commitDelayedGroupObjectChange(property.groupObject);
        syncDispatch(new ExecuteEditAction(property.ID, getFullCurrentKey(columnKey), actionSID), callback);
    }

    public void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(actionResults), callback);
    }

    public void throwInServerInvocation(Throwable throwable, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ThrowInInvocation(throwable), callback);
    }

    public <T extends Result> void syncDispatch(FormBoundAction<T> action, AsyncCallback<T> callback) {
        //todo: возможно понадобится сделать чтото более сложное как в
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        loadingManager.start();
        dispatcher.execute(action, new WrapperAsyncCallbackEx<T>(callback) {
            @Override
            public void preProcess() {
                loadingManager.stop();
            }
        });
    }

    public GGroupObjectValueBuilder getFullCurrentKey() {
        GGroupObjectValueBuilder fullKey = new GGroupObjectValueBuilder();

        for (GGroupObjectController group : controllers.values()) {
            fullKey.putAll(group.getCurrentKey());
        }

        for (GTreeGroupController tree : treeControllers.values()) {
            GGroupObjectValue currentPath = tree.getCurrentKey();
            if (currentPath != null) {
                fullKey.putAll(currentPath);
            }
        }

        return fullKey;
    }

    public GGroupObjectValue getFullCurrentKey(GGroupObjectValue columnKey) {
        GGroupObjectValueBuilder fullKey = getFullCurrentKey();
        if (columnKey != null) {
            fullKey.putAll(columnKey);
        }
        return fullKey.toGroupObjectValue();
    }

    public GGroupObjectController getGroupObjectController(GGroupObject group) {
        return controllers.get(group);
    }

    private GGroupObjectLogicsSupplier getGroupObjectLogicsSupplier(GGroupObject group) {
        GGroupObjectController groupObjectController = controllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return group.parent != null
                ? treeControllers.get(group.parent)
                : null;
    }

    public void changeProperty(GPropertyDraw property, GGroupObjectValue columnKey, Serializable value, Object oldValue) {
        int requestIndex = dispatcher.execute(new ChangeProperty(property.ID, getFullCurrentKey(columnKey), value, null), new ServerResponseCallback());

        GGroupObjectLogicsSupplier controller = getGroupObjectLogicsSupplier(property.groupObject);

        GGroupObjectValue propertyKey = controller != null && !controller.hasPanelProperty(property)
                                        ? columnKey != null
                                            ? new GGroupObjectValueBuilder(controller.getCurrentKey(), columnKey).toGroupObjectValue()
                                            : controller.getCurrentKey()
                                        : columnKey;

        putToDoubleNativeMap(pendingChangePropertyRequests, property, propertyKey, new Change(requestIndex, value, oldValue, property.canUseChangeValueForRendering()));
    }

    public boolean isAsyncModifyObject(GPropertyDraw property) {
        if (property.addRemove != null) {
            GGroupObjectController controller = controllers.get(property.addRemove.object.groupObject);
            if (controller != null && controller.isInGridClassView()) {
                return true;
            }
        }
        return false;
    }

    public void modifyObject(final GPropertyDraw property, final GGroupObjectValue columnKey) {
        assert isAsyncModifyObject(property);

        final GObject object = property.addRemove.object;
        final boolean add = property.addRemove.add;

        GGroupObjectController controller = getGroupObjectController(property.addRemove.object.groupObject);
        final int position = controller.getGrid().getTable().getKeyboardSelectedRow();

        if (add) {
            NavigatorDispatchAsync.Instance.get().execute(new GenerateID(), new ErrorHandlingCallback<GenerateIDResult>() {
                @Override
                public void success(GenerateIDResult result) {
                    executeModifyObject(property, columnKey, object, add, result.ID, new GGroupObjectValue(object.ID, result.ID), position);
                }
            });
        } else {
            final GGroupObjectValue value = controllers.get(object.groupObject).getCurrentKey();
            final int ID = (Integer) value.getValue(0);
            executeModifyObject(property, columnKey, object, add, ID, value, position);
        }
    }

    private void executeModifyObject(GPropertyDraw property, GGroupObjectValue columnKey, GObject object, boolean add, int ID, GGroupObjectValue value, int position) {
        final GGroupObjectValue fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        controllers.get(object.groupObject).modifyGroupObject(value, add, -1);

        int requestIndex = dispatcher.execute(new ChangeProperty(property.ID, fullCurrentKey, null, add ? ID : null), new ServerResponseCallback());
        pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex);
        pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));
    }

    public void changeClassView(GGroupObject groupObject, GClassViewType newClassView) {
        DeferredRunner.get().commitDelayedGroupObjectChange(groupObject);
        syncDispatch(new ChangeClassView(groupObject.ID, newClassView), new ServerResponseCallback());
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType) {
        if (defaultOrdersInitialized) {
            syncDispatch(new ChangePropertyOrder(property.ID, columnKey, modiType), new ServerResponseCallback());
        }
    }

    public void clearPropertyOrders(GGroupObject groupObject) {
        if (defaultOrdersInitialized) {
            syncDispatch(new ClearPropertyOrders(groupObject.ID), new ServerResponseCallback());
        }
    }

    public void expandGroupObject(GGroupObject group, GGroupObjectValue value) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncDispatch(new ExpandGroupObject(group.ID, value), new ServerResponseCallback());
    }

    public void collapseGroupObject(GGroupObject group, GGroupObjectValue value) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncDispatch(new CollapseGroupObject(group.ID, value), new ServerResponseCallback());
    }

    public void setTabVisible(GContainer tabbedPane, GComponent visibleComponent) {
        dispatcher.execute(new SetTabVisible(tabbedPane.ID, visibleComponent.ID), new ServerResponseCallback());
        onResize();
    }

    public void closePressed() {
        syncDispatch(new ClosePressed(), new ServerResponseCallback());
    }

    public void okPressed() {
        syncDispatch(new OkPressed(), new ServerResponseCallback());
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        syncDispatch(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID), new ServerResponseCallback());
    }

    public void changeFilter(GGroupObject groupObject, List<GPropertyFilter> conditions) {
        currentFilters.put(groupObject, conditions);
        applyCurrentFilters();
    }

    public void changeFilter(GTreeGroup treeGroup, List<GPropertyFilter> conditions) {
        Map<GGroupObject, List<GPropertyFilter>> filters = GwtSharedUtils.groupList(new GwtSharedUtils.Group<GGroupObject, GPropertyFilter>() {
            public GGroupObject group(GPropertyFilter key) {
                return key.groupObject;
            }
        }, conditions);

        for (GGroupObject group : treeGroup.groups) {
            List<GPropertyFilter> groupFilters = filters.get(group);
            if (groupFilters == null) {
                groupFilters = new ArrayList<>();
            }
            currentFilters.put(group, groupFilters);
        }

        applyCurrentFilters();
    }

    private void applyCurrentFilters() {
        ArrayList<GPropertyFilterDTO> filters = new ArrayList<>();

        for (List<GPropertyFilter> groupFilters : currentFilters.values()) {
            for (GPropertyFilter filter : groupFilters) {
                filters.add(filter.getFilterDTO());
            }
        }

        dispatcher.execute(new SetUserFilters(filters), new ServerResponseCallback());
    }

    public void quickFilter(EditEvent event, int initialFilterPropertyID) {
        GPropertyDraw propertyDraw = getProperty(initialFilterPropertyID);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            selectProperty(initialFilterPropertyID);
            controllers.get(propertyDraw.groupObject).quickEditFilter(event, propertyDraw);
        }
    }

    public void getInitialFilterProperty(ErrorHandlingCallback<NumberResult> callback) {
        dispatcher.execute(new GetInitialFilterProperty(), callback);
    }

    public void selectProperty(int propertyDrawId) {
        GPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).selectProperty(propertyDraw);
        }
    }

    public void focusProperty(int propertyDrawId) {
        GPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).focusProperty(propertyDraw);
        }
    }

    public void activateTab(String formID, String tabID) {
        int a = 5;
        a++;
        formsController.selectTab(formID, tabID);
//        GPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
//        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
//            controllers.get(propertyDraw.groupObject).focusProperty(propertyDraw);
//        }
    }

    public void countRecords(final GGroupObject groupObject) {
        dispatcher.execute(new CountRecords(groupObject.ID), new ErrorHandlingCallback<NumberResult>() {
            @Override
            public void success(NumberResult result) {
                controllers.get(groupObject).showRecordQuantity((Integer) result.value);
            }
        });
    }

    public void calculateSum(final GGroupObject groupObject, final GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        dispatcher.execute(new CalculateSum(propertyDraw.ID, columnKey), new ErrorHandlingCallback<NumberResult>() {
            @Override
            public void success(NumberResult result) {
                controllers.get(groupObject).showSum(result.value, propertyDraw);
            }
        });
    }

    public GFormUserPreferences getUserPreferences() {
        List<GGroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<>();
        List<GGroupObjectUserPreferences> groupObjectGeneralPreferencesList = new ArrayList<>();
        for (GGroupObjectController controller : controllers.values()) {
            if (controller.groupObject != null) {
                groupObjectUserPreferencesList.add(controller.getUserGridPreferences());
                groupObjectGeneralPreferencesList.add(controller.getGeneralGridPreferences());
            }
        }
        return new GFormUserPreferences(groupObjectGeneralPreferencesList, groupObjectUserPreferencesList);
    }

    public void runGroupReport(Integer groupObjectID, final boolean toExcel) {
        syncDispatch(new GroupReport(groupObjectID, toExcel, getUserPreferences()), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                openReportWindow(result.get());
            }
        });
    }

    public void openReportWindow(String fileName) {
        String reportUrl = GwtClientUtils.getWebAppBaseURL() + "downloadFile?name=" + fileName;
        Window.open(reportUrl, fileName, "");
    }

    public void saveUserPreferences(GGridUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, final ErrorHandlingCallback<ServerResponseResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences.convertPreferences(), forAllUsers, completeOverride), new ServerResponseCallback() {
            @Override
            public void success(ServerResponseResult response) {
                for (GAction action : response.actions) {
                    if (action instanceof GLogMessageAction) {
                        actionDispatcher.execute((GLogMessageAction) action);
                        callback.failure(new Throwable());
                        return;
                    }
                }
                callback.success(response);
            }

            @Override
            public void failure(Throwable caught) {
                callback.failure(caught);
            }
        });
    }

    public List<GPropertyDraw> getPropertyDraws() {
        return form.propertyDraws;
    }

    public void setAsyncView(PanelRenderer asyncView) {
        this.asyncView = asyncView;
    }

    public void onAsyncStarted() {
        if (asyncView != null) {
            asyncTimer.schedule(ASYNC_TIME_OUT);
        }
    }

    public void onAsyncFinished() {
        if (asyncView != null) {
            asyncTimer.cancel();
            asyncView.setDefaultIcon();
        }
    }

    public void block() {
        //do nothing by default
    }

    public void setBlockingWidget(Widget blockingWidget) {
        //do nothing by default
    }

    public void unblock() {
        //do nothing by default
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
        if (blocked) {
            setFiltersVisible(true);
        } else {
            setFiltersVisible(selected);
        }
    }

    public void hideForm() {
        formHidden = true;
        setFiltersVisible(false);
        dispatcher.execute(new FormHidden(), new ErrorHandlingCallback<VoidResult>());
        dispatcher.close();
    }

    public void blockingConfirm(String caption, String message, boolean cancel, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showConfirmBox(caption, message, cancel, 0, 0, callback);
    }
    
    public void blockingConfirm(String caption, String message, boolean cancel, int timeout, int initialValue, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showConfirmBox(caption, message, cancel, timeout, initialValue, callback);
    }

    public void blockingMessage(String caption, String message, final DialogBoxHelper.CloseCallback callback) {
        blockingMessage(false, caption, message, callback);
    }

    public void blockingMessage(boolean isError, String caption, String message, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showMessageBox(isError, caption, message, callback);
    }

    public Dimension getPreferredSize() {
        int preferredWidth = form.mainContainer.preferredWidth;
        int preferredHeight = form.mainContainer.preferredHeight;
        if (preferredWidth == -1 || preferredHeight == -1) {
            Dimension size = formLayout.getPreferredSize();
            if (preferredWidth == -1) {
                preferredWidth = size.width;
            }
            if (preferredHeight == -1) {
                preferredHeight = size.height;
            }
        }

        return new Dimension(preferredWidth, preferredHeight);
    }

    private GPropertyTable editingTable;

    public void setCurrentEditingTable(GPropertyTable table) {
        editingTable = table;
        if (table == null) {
            dispatcher.flushCompletedRequests();
        }
    }

    public boolean isEditing() {
        return editingTable != null;
    }

    public boolean isModal() {
        return isModal;
    }

    public boolean isDialog() {
        return isDialog;
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

    public void setFiltersVisible(boolean visible) {
        for (GGroupObjectController goc : controllers.values()) {
            goc.setFilterVisible(visible);
        }
        for (GTreeGroupController tgc : treeControllers.values()) {
            tgc.setFilterVisible(visible);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setFiltersVisible(selected && !blocked);

        if (selected && blocked) { // чтобы автоматом не проставлять фокус под блокировку
            return;
        }

        if (selected) {
            scheduleFocusFirstWidget();
            restoreGridScrollPositions();
        } else {
            // обходим баг Chrome со скроллингом
            // http://code.google.com/p/chromium/issues/detail?id=36428
            storeGridScrollPositions();
        }
    }

    public void storeGridScrollPositions() {
        for (GGroupObjectController controller : controllers.values()) {
            controller.beforeHidingGrid();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.beforeHidingGrid();
        }
    }

    public void restoreGridScrollPositions() {
        for (GGroupObjectController controller : controllers.values()) {
            controller.afterShowingGrid();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.afterShowingGrid();
        }
    }

    public GSimpleChangePropertyDispatcher getSimpleDispatcher() {
        return simpleDispatcher;
    }

    @Override
    public void getServerActionMessage(ErrorHandlingCallback<StringResult> callback) {
        dispatcher.executePriorityAction(new GetRemoteActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback) {
        dispatcher.executePriorityAction(new GetRemoteActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable) {
        dispatcher.executePriorityAction(new Interrupt(cancelable), new ErrorHandlingCallback<VoidResult>());
    }

    private static final class Change {
        public final int requestIndex;
        public final Object newValue;
        public final Object oldValue;
        public final boolean canUseNewValueForRendering;

        private Change(int requestIndex, Object newValue, Object oldValue, boolean canUseNewValueForRendering) {
            this.requestIndex = requestIndex;
            this.newValue = newValue;
            this.oldValue = oldValue;
            this.canUseNewValueForRendering = canUseNewValueForRendering;
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

    protected void onInitialFormChangesReceived() {
        scheduleFocusFirstWidget();
    }

    public void scheduleFocusFirstWidget() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                onResize();
                getElement().getStyle().setOverflow(Style.Overflow.AUTO);
                focusFirstWidget();
            }
        });
    }

    private void focusFirstWidget() {
        if (formLayout.focusDefaultWidget()) {
            return;
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            if (treeController.focusFirstWidget()) {
                return;
            }
        }

        for (GGroupObjectController controller : controllers.values()) { // в конце controllers лежит нулевой groupObject. его-то и следует оставить напоследок
            if (controller.focusFirstWidget()) {
                return;
            }
        }
    }

    public void addHotkeyBinding(GGroupObject groupObjcet, GKeyStroke key, HotkeyManager.Binding binding) {
        hotkeyManager.addHotkeyBinding(groupObjcet, key, binding);
    }
    
    public void modalFormAttached() { // фильтры норовят спрятаться за диалог (например, при его перемещении). передобавляем их в конец.
        for (GGroupObjectController controller : controllers.values()) {
            controller.reattachFilter();
        }
    }

    private class ServerResponseCallback extends ErrorHandlingCallback<ServerResponseResult> {
        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);
        }
    }
}