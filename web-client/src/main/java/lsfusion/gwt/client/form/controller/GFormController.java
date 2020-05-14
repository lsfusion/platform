package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.GFormChangesDTO;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GLogMessageAction;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.WrapperAsyncCallbackEx;
import lsfusion.gwt.client.base.busy.GBusyDialogDisplayer;
import lsfusion.gwt.client.base.busy.LoadingBlocker;
import lsfusion.gwt.client.base.busy.LoadingManager;
import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.jsni.Function2;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.controller.remote.DeferredRunner;
import lsfusion.gwt.client.controller.remote.action.form.*;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateID;
import lsfusion.gwt.client.controller.remote.action.logics.GenerateIDResult;
import lsfusion.gwt.client.controller.remote.action.navigator.GainedFocus;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.classes.view.ClassChosenHandler;
import lsfusion.gwt.client.form.classes.view.GClassDialog;
import lsfusion.gwt.client.form.controller.dispatch.FormDispatchAsync;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.GRegularFilter;
import lsfusion.gwt.client.form.filter.GRegularFilterGroup;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilterDTO;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GColumnUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GFormUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.view.GGridViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyTable;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.ServerMessageProvider;
import lsfusion.gwt.client.view.StyleDefaults;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.gwt.client.base.GwtClientUtils.isShowing;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.form.event.GKeyStroke.isPossibleEditKeyEvent;

public class GFormController extends ResizableSimplePanel implements ServerMessageProvider {
    private static final int ASYNC_TIME_OUT = 50;

    private FormDispatchAsync dispatcher;

    private final GFormActionDispatcher actionDispatcher;

    private final FormsController formsController;

    private final GForm form;
    public final GFormLayout formLayout;

    private final boolean isModal;
    private final boolean isDialog;

    private final HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects = new HashMap<>();

    private final Map<GGroupObject, List<GPropertyFilter>> currentFilters = new HashMap<>();

    private final Map<GGroupObject, GGridController> controllers = new LinkedHashMap<>();
    private final Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();

    private final Map<GGroupObject, List<Widget>> filterViews = new HashMap<>();

    private final LinkedHashMap<Long, ModifyObject> pendingModifyObjectRequests = new LinkedHashMap<>();
    private final NativeHashMap<GGroupObject, Long> pendingChangeCurrentObjectsRequests = new NativeHashMap<>();
    private final NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>> pendingChangePropertyRequests = new NativeHashMap<>();

    private boolean initialFormChangesReceived = false;
    private boolean hasColumnGroupObjects;

    private Timer asyncTimer;
    private PanelRenderer asyncView;

    private LoadingManager loadingManager = MainFrame.busyDialog ? new GBusyDialogDisplayer(this) : new LoadingBlocker(this);

    private boolean blocked = false;
    private boolean selected = true;

    public FormsController getFormsController() {
        return formsController;
    }

    public GFormController(FormsController formsController, GForm gForm, final boolean isModal, boolean isDialog) {
        actionDispatcher = new GFormActionDispatcher(this);

        this.formsController = formsController;
        this.form = gForm;
        this.isModal = isModal;
        this.isDialog = isDialog;

        dispatcher = new FormDispatchAsync(this);

        formLayout = new GFormLayout(this, form.mainContainer);

        asyncTimer = new Timer() {
            @Override
            public void run() {
                asyncView.setImage("loading.gif");
            }
        };

        if (form.sID != null) {
            getElement().setAttribute("lsfusion-form", form.sID);
        }

        setFillWidget(formLayout);

        updateFormCaption();

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        if (form.initialFormChanges != null) {
            applyRemoteChanges(form.initialFormChanges);
            form.initialFormChanges = null;
        } else if (!initialFormChangesReceived) { // возможно уже получили в initializeDefaultOrders()
            getRemoteChanges();
        }

        initializeUserOrders();

        initializeAutoRefresh();

        install(this);
    }

    protected void unregisterForm() {
        if(formsController != null)
            formsController.unregisterForm(this);
    }

    public GGridController getController(GGroupObject groupObject) {
        return controllers.get(groupObject);
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
            addBinding(new GKeyInputEvent(filter.key, null), new GFormController.Binding(filterGroup.groupObject) {
                @Override
                public void pressed(EventTarget eventTarget) {
                    filterCheck.setValue(!filterCheck.getValue(), true);
                }
                @Override
                public boolean showing() {
                    return isShowing(filterCheck);
                }
            });
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox();
        filterBox.setMultipleSelect(false);
        filterBox.addItem("(" + ClientMessages.Instance.get().multipleFilterComponentAll() + ")", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            final GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.getFullCaption(), "" + i);

            final int filterIndex = i;
            filterBox.getElement().setPropertyObject("groupObject", filterGroup.groupObject);
            if (filter.key != null) {
                addBinding(new GKeyInputEvent(filter.key, null), new GFormController.Binding(filterGroup.groupObject) {
                    @Override
                    public void pressed(EventTarget eventTarget) {
                        filterBox.setSelectedIndex(filterIndex + 1);
                        setRegularFilter(filterGroup, filterIndex);
                    }
                    @Override
                    public boolean showing() {
                        return isShowing(filterBox);
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
        filterBox.setHeight(StyleDefaults.COMPONENT_HEIGHT_STRING);

        addFilterView(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + 1);
        }
    }

    private void addFilterView(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.addBaseComponent(filterGroup, filterWidget, null);

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
            initializeTreeController(treeGroup);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                initializeGroupController(group);
            }
        }
        initializeGroupController(null);

        hasColumnGroupObjects = false;
        for (GPropertyDraw property : form.propertyDraws) {
            if (property.columnGroupObjects != null && !property.columnGroupObjects.isEmpty()) {
                hasColumnGroupObjects = true;
            }
        }
    }

    private void initializeGroupController(GGroupObject group) {
        GGridController controller = new GGridController(this, group, group != null && form.userPreferences != null ? extractUserPreferences(form.userPreferences, group) : null);
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
            getGroupObjectLogicsSupplier(groupObject).changeOrders(groupObject, entry.getValue(), true);
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

    public GPropertyDraw getProperty(int id) {
        return form.getProperty(id);
    }

    public GGroupObject getGroupObject(int groupID) {
        return form.getGroupObject(groupID);
    }

    public void getRemoteChanges() {
        dispatcher.execute(new GetRemoteChanges(), new ServerResponseCallback());
    }

    public void gainedFocus() {
        dispatcher.execute(new GainedFocus(), new ServerResponseCallback());
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);

        if (hasColumnGroupObjects) // optimization
            currentGridObjects.putAll(fc.gridObjects);

        modifyFormChangesWithModifyObjectAsyncs(changesDTO.requestIndex, fc);

        modifyFormChangesWithChangeCurrentObjectAsyncs(changesDTO.requestIndex, fc);

        modifyFormChangesWithChangePropertyAsyncs(changesDTO.requestIndex, fc);

        // затем одним скопом обновляем данные во всех таблицах
        for (GGridController controller : controllers.values()) {
            controller.processFormChanges(fc, currentGridObjects);
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(fc);
        }

        formLayout.hideEmptyContainerViews();

        activateElements(fc);

        onResize();

        // в конце скроллим все таблицы к текущим ключам
        afterAppliedChanges();

        if (!initialFormChangesReceived) {
            onInitialFormChangesReceived();
            initialFormChangesReceived = true;
        }
    }

    private void activateElements(GFormChanges fc) {
        for(GComponent component : fc.activateTabs)
            activateTab(component);

        for(GPropertyDraw propertyDraw : fc.activateProps)
            focusProperty(propertyDraw);            
    }

    private void afterAppliedChanges() {
        for (GGridController controller : controllers.values()) {
            controller.afterAppliedChanges();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.afterAppliedChanges();
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

        for (Map.Entry<Long, ModifyObject> e : pendingModifyObjectRequests.entrySet()) {
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

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(final long currentDispatchingRequestIndex, final GFormChanges fc) {
        pendingChangeCurrentObjectsRequests.foreachEntry(new Function2<GGroupObject, Long>() {
            @Override
            public void apply(GGroupObject group, Long requestIndex) {
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
                        long requestIndex = change.requestIndex;
                        if (requestIndex <= currentDispatchingRequestIndex) {

                            removeFromDoubleMap(pendingChangePropertyRequests, property, keys);

                            if(isPropertyShown(property) && !fc.dropProperties.contains(property)) {
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

    private boolean isPropertyShown(GPropertyDraw property) {
        if(property != null) {
            GGridController controller = controllers.get(property.groupObject);
            return controller != null && controller.isPropertyShown(property);
        }
        return false;
    }

    public void openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, EditEvent initFilterEvent, final WindowHiddenHandler handler) {
        if (modalityType == GModalityType.DOCKED_MODAL) {
            block();
        }

        Widget blockingWidget = formsController.openForm(form, modalityType, forbidDuplicate, initFilterEvent, () -> {
            unblock();
            handler.onHidden();
        });

        if (modalityType == GModalityType.DOCKED_MODAL) {
            setBlockingWidget(blockingWidget);
        }
    }

    public void showClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        GClassDialog.showDialog(baseClass, defaultClass, concreate, classChosenHandler);
    }

    public void changeGroupObject(final GGroupObject group, GGroupObjectValue key) {
        long requestIndex = dispatcher.execute(new ChangeGroupObject(group.ID, key), new ServerResponseCallback() {
            @Override
            public void preProcess() {
                DeferredRunner.get().commitDelayedGroupObjectChange(group);
            }
        });
        pendingChangeCurrentObjectsRequests.put(group, requestIndex);
    }

    // has to be called setCurrentKey before
    public void changeGroupObjectLater(final GGroupObject group, final GGroupObjectValue key) {
        DeferredRunner.get().scheduleDelayedGroupObjectChange(group, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changeGroupObject(group, key);
            }
        });
    }

    public void pasteExternalTable(ArrayList<GPropertyDraw> propertyList, ArrayList<GGroupObjectValue> columnKeys, List<List<String>> table) {
        ArrayList<ArrayList<Object>> values = new ArrayList<>();

        int propertyColumns = propertyList.size();
        for (List<String> sRow : table) {
            ArrayList<Object> valueRow = new ArrayList<>();

            for (int i = 0; i < propertyColumns; i++) {
                GPropertyDraw property = propertyList.get(i);
                String sCell = i < sRow.size() ? sRow.get(i) : null;
                valueRow.add(property.parseChangeValueOrNull(sCell));
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

    public void executeEventAction(GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<ServerResponseResult> callback) {
        DeferredRunner.get().commitDelayedGroupObjectChange(property.groupObject);
        syncDispatch(new ExecuteEventAction(property.ID, getFullCurrentKey(columnKey), actionSID), callback);
    }

    public void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(requestIndex, actionResults, continueIndex), callback, true);
    }

    public void throwInServerInvocation(long requestIndex, Throwable throwable, int continueIndex, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ThrowInInvocation(requestIndex, throwable, continueIndex), callback, true);
    }

    public <T extends Result> void syncDispatch(final FormAction<T> action, AsyncCallback<T> callback) {
        syncDispatch(action, callback, false);
    }

    public <T extends Result> void syncDispatch(final FormAction<T> action, AsyncCallback<T> callback, boolean direct) {
        //todo: возможно понадобится сделать чтото более сложное как в
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        loadingManager.start();
        dispatcher.execute(action, new WrapperAsyncCallbackEx<T>(callback) {
            @Override
            public void preProcess() {
                loadingManager.stop();
            }
        }, direct);
    }

    public GGroupObjectValue getFullCurrentKey(GGroupObjectValue propertyKey) {
        GGroupObjectValueBuilder fullKey = new GGroupObjectValueBuilder();

        for (GGridController group : controllers.values()) {
            fullKey.putAll(group.getCurrentKey());
        }

        for (GTreeGroupController tree : treeControllers.values()) {
            GGroupObjectValue currentPath = tree.getCurrentKey();
            if (currentPath != null) {
                fullKey.putAll(currentPath);
            }
        }

        fullKey.putAll(propertyKey);

        return fullKey.toGroupObjectValue();
    }

    public GGridController getGroupObjectController(GGroupObject group) {
        return controllers.get(group);
    }

    private GTableController getGroupObjectLogicsSupplier(GGroupObject group) {
        GGridController groupObjectController = controllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return treeControllers.get(group.parent);
    }

    public void changeProperty(GPropertyDraw property, GGroupObjectValue columnKey, Serializable value, Object oldValue) {
        GGroupObjectValue rowKey = GGroupObjectValue.EMPTY;
        if(property.grid) {
            rowKey = getGroupObjectLogicsSupplier(property.groupObject).getCurrentKey();
            if(rowKey.isEmpty())
                return;
        }
        changeProperty(property, rowKey, columnKey, value, oldValue);
    }

    public void changeProperty(GPropertyDraw property, GGroupObjectValue rowKey, GGroupObjectValue columnKey, Serializable value, Object oldValue) {
        GGroupObjectValue fullKey = GGroupObjectValue.getFullKey(rowKey, columnKey);

        long requestIndex = dispatcher.execute(new ChangeProperty(property.ID, getFullCurrentKey(fullKey), value, null), new ServerResponseCallback());

        putToDoubleNativeMap(pendingChangePropertyRequests, property, fullKey, new Change(requestIndex, value, oldValue, property.canUseChangeValueForRendering()));
    }

    public boolean isAsyncModifyObject(GPropertyDraw property) {
        if (property.addRemove != null) {
            GGridController controller = controllers.get(property.addRemove.object.groupObject);
            if (controller != null && controller.isList()) {
                return true;
            }
        }
        return false;
    }

    public void modifyObject(final GPropertyDraw property, final GGroupObjectValue columnKey) {
        assert isAsyncModifyObject(property);

        final GObject object = property.addRemove.object;
        final boolean add = property.addRemove.add;

        GGridController controller = getGroupObjectController(property.addRemove.object.groupObject);
        final int position = controller.getKeyboardSelectedRow();

        if (add) {
            MainFrame.logicsDispatchAsync.execute(new GenerateID(), new ErrorHandlingCallback<GenerateIDResult>() {
                @Override
                public void success(GenerateIDResult result) {
                    executeModifyObject(property, columnKey, object, add, result.ID, new GGroupObjectValue(object.ID, result.ID), position);
                }
            });
        } else {
            final GGroupObjectValue value = controllers.get(object.groupObject).getCurrentKey();
            if(value.isEmpty())
                return;
            final long ID = (Long) value.getValue(0);
            executeModifyObject(property, columnKey, object, add, ID, value, position);
        }
    }

    private void executeModifyObject(GPropertyDraw property, GGroupObjectValue columnKey, GObject object, boolean add, long ID, GGroupObjectValue value, int position) {
        final GGroupObjectValue fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        controllers.get(object.groupObject).modifyGroupObject(value, add, -1);

        long requestIndex = dispatcher.execute(new ChangeProperty(property.ID, fullCurrentKey, null, add ? ID : null), new ServerResponseCallback());
        pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex);
        pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType, boolean alreadySet) {
        if (!alreadySet) {
            syncDispatch(new ChangePropertyOrder(property.ID, columnKey, modiType), new ServerResponseCallback());
        }
    }

    public void clearPropertyOrders(GGroupObject groupObject) {
        syncDispatch(new ClearPropertyOrders(groupObject.ID), new ServerResponseCallback());
    }

    public void expandGroupObjectRecursive(GGroupObject group, boolean current) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncDispatch(new ExpandGroupObjectRecursive(group.ID, current), new ServerResponseCallback());
    }

    public void expandGroupObject(GGroupObject group, GGroupObjectValue value) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncDispatch(new ExpandGroupObject(group.ID, value), new ServerResponseCallback());
    }

    public void collapseGroupObjectRecursive(GGroupObject group, boolean current) {
        DeferredRunner.get().commitDelayedGroupObjectChange(group);
        syncDispatch(new CollapseGroupObjectRecursive(group.ID, current), new ServerResponseCallback());
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
            controllers.get(propertyDraw.groupObject).quickEditFilter(event, propertyDraw, null);
        }
    }

    public void getInitialFilterProperty(ErrorHandlingCallback<NumberResult> callback) {
        dispatcher.execute(new GetInitialFilterProperty(), callback);
    }

    public void selectProperty(int propertyDrawId) {
        GPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).focusProperty(propertyDraw);
        }
    }

    public void focusProperty(GPropertyDraw propertyDraw) {
        if (controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).focusProperty(propertyDraw);
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

    // change group mode with force refresh
    public void changeMode(final GGroupObject groupObject, boolean enableGroup, int pageSize, GGridViewType viewType) {
        changeMode(groupObject, true, enableGroup ? new ArrayList<>() : null, enableGroup ? new ArrayList<>() : null, 0, null, pageSize, true, null, viewType);
    }
    public void changeMode(final GGroupObject groupObject, boolean setGroup, List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeysList, int aggrProps, GPropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, GUpdateMode updateMode, GGridViewType viewType) {
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
        dispatcher.execute(new ChangeMode(groupObject.ID, setGroup, propertyIDs, columnKeys, aggrProps, aggrType, pageSize, forceRefresh, updateMode, viewType), new ServerResponseCallback());
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

    public void runGroupReport(Integer groupObjectID, final boolean toExcel) {
        syncDispatch(new GroupReport(groupObjectID, toExcel, getUserPreferences()), new ErrorHandlingCallback<GroupReportResult>() {
            @Override
            public void success(GroupReportResult result) {
                GwtClientUtils.downloadFile(result.filename, "lsfReport", result.extension);
            }
        });
    }

    public void saveUserPreferences(GGridUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps, final ErrorHandlingCallback<ServerResponseResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences.convertPreferences(), forAllUsers, completeOverride, hiddenProps), new ServerResponseCallback() {
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
    
    public void refreshUPHiddenProps(String groupObjectSID, String[] propSids) {
        syncDispatch(new RefreshUPHiddenPropsAction(groupObjectSID, propSids), new ServerResponseCallback());
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

    protected void onFormHidden(int closeDelay) {
        setFiltersVisible(false);
        FormDispatchAsync closeDispatcher = dispatcher;
        Scheduler.get().scheduleDeferred(() -> {
            closeDispatcher.execute(new Close(closeDelay), new ErrorHandlingCallback<VoidResult>() {
                @Override
                public void failure(Throwable caught) { // supressing errors
                }
            });
            closeDispatcher.close();
        });
//        dispatcher = null; // so far there are no null checks (for example, like in desktop-client), so changePageSize can be called after (apparently close will suppress it)
    }

    public void updateFormCaption() {
        String caption = form.mainContainer.caption;
        setFormCaption(caption, form.getTooltip(caption));
    }

    public void setFormCaption(String caption, String tooltip) {
        throw new UnsupportedOperationException();
    }

    // need this because hideForm can be called twice, which will lead to several continueDispatching (and nullpointer, because currentResponse == null)
    private boolean formHidden;
    public void hideForm(int closeDelay) {
        if(!formHidden) {
            onFormHidden(closeDelay);
            formHidden = true;
        }
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

    public Dimension getMaxPreferredSize() {
        return formLayout.getMaxPreferredSize();
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

    public void commitEditingTable() {
        editingTable.validateAndCommit();
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
        for (GGridController goc : controllers.values()) {
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
        for (GGridController controller : controllers.values()) {
            controller.beforeHidingGrid();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.beforeHidingGrid();
        }
    }

    public void restoreGridScrollPositions() {
        for (GGridController controller : controllers.values()) {
            controller.afterShowingGrid();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.afterShowingGrid();
        }
    }

    @Override
    public void getServerActionMessage(ErrorHandlingCallback<StringResult> callback) {
        dispatcher.executePriority(new GetRemoteActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback) {
        dispatcher.executePriority(new GetRemoteActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable) {
        dispatcher.executePriority(new Interrupt(cancelable), new ErrorHandlingCallback<VoidResult>());
    }

    public void setContainerCaption(GContainer container, String caption) {
        container.caption = caption;

        // update captions (actually we could've set them directly to the containers, but tabbed pane physically adds / removes that views, so the check if there is a tab is required there)
        GFormLayout layout = formLayout;
        if(container.isTab())
            ((TabbedContainerView)layout.getContainerView(container.container)).updateTabCaption(container);
        else if(container.main)
            updateFormCaption();
        else
            layout.getContainerView(container).updateCaption();
    }

    private static final class Change {
        public final long requestIndex;
        public final Object newValue;
        public final Object oldValue;
        public final boolean canUseNewValueForRendering;

        private Change(long requestIndex, Object newValue, Object oldValue, boolean canUseNewValueForRendering) {
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
                treeController.scrollToTop();
                return;
            }
        }

        for (GGridController controller : controllers.values()) { // в конце controllers лежит нулевой groupObject. его-то и следует оставить напоследок
            if (controller.focusFirstWidget()) {
                controller.scrollToTop();
                return;
            }
        }
    }

    public void modalFormAttached() { // фильтры норовят спрятаться за диалог (например, при его перемещении). передобавляем их в конец.
        for (GGridController controller : controllers.values()) {
            controller.reattachFilter();
        }
    }

    private class ServerResponseCallback extends ErrorHandlingCallback<ServerResponseResult> {

        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);
        }
    }

    public abstract static class Binding {
        public final GGroupObject groupObject;
        public int priority;
        public GBindingMode bindDialog;
        public GBindingMode bindGroup;
        public GBindingMode bindEditing;
        public GBindingMode bindShowing;
        Function<NativeEvent, Boolean> isSuitable;

        public Binding(GGroupObject groupObject) {
            this(groupObject, 0, null);
        }

        public Binding(GGroupObject groupObject, int priority, Function<NativeEvent, Boolean> isSuitable) {
            this.groupObject = groupObject;
            this.priority = priority;
            this.isSuitable = isSuitable;
        }

        public abstract void pressed(EventTarget eventTarget);
        public abstract boolean showing();

        public boolean enabled() {
            return true;
        }
    }

    private final HashMap<GInputEvent, ArrayList<Binding>> bindings = new HashMap<>();

    public void install(Widget rootWidget) {
        rootWidget.addDomHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                handleKeyEvent(event.getNativeEvent());
            }
        }, KeyDownEvent.getType());
        rootWidget.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handleMouseEvent(event.getNativeEvent());
            }
        }, ClickEvent.getType());
        rootWidget.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                handleMouseEvent(event.getNativeEvent());
            }
        }, DoubleClickEvent.getType());
    }
    
    public void addPropertyBindings(GPropertyDraw propertyDraw, Supplier<Binding> bindingSupplier) {
        if(propertyDraw.changeKey != null) {
            Binding binding = bindingSupplier.get();
            if(propertyDraw.changeKeyPriority != null)
                binding.priority = propertyDraw.changeKeyPriority;
            addBinding(propertyDraw.changeKey, binding);
        }
        if(propertyDraw.changeMouse != null) {
            Binding binding = bindingSupplier.get();
            if(propertyDraw.changeMousePriority != null)
                binding.priority = propertyDraw.changeMousePriority;
            addBinding(propertyDraw.changeMouse, binding);
        }
    }

    public void addBinding(GInputEvent event, Binding binding) {
        assert event != null && binding != null;

        ArrayList<Binding> groupBindings = bindings.computeIfAbsent(event, k -> new ArrayList<>());
        if(binding.priority == 0)
            binding.priority = groupBindings.size();
        if(binding.bindDialog == null)
            binding.bindDialog = event.bindingModes != null ? event.bindingModes.getOrDefault("dialog", GBindingMode.AUTO) : GBindingMode.AUTO;
        if(binding.bindGroup == null)
            binding.bindGroup = event.bindingModes != null ? event.bindingModes.getOrDefault("group", GBindingMode.AUTO) : GBindingMode.AUTO;
        if(binding.bindEditing == null)
            binding.bindEditing = event.bindingModes != null ? event.bindingModes.getOrDefault("editing", GBindingMode.AUTO) : GBindingMode.AUTO;
        if(binding.bindShowing == null)
            binding.bindShowing = event.bindingModes != null ? event.bindingModes.getOrDefault("showing", GBindingMode.AUTO) : GBindingMode.AUTO;
        groupBindings.add(binding);
    }
    
    private GGroupObject getGroupObject(Element elementTarget) {
        while (elementTarget != null) {     // пытаемся найти GroupObject, к которому относится элемент с фокусом
            GGroupObject targetGO = (GGroupObject) elementTarget.getPropertyObject("groupObject");
            if (targetGO != null)
                return targetGO;
            elementTarget = elementTarget.getParentElement();
        }
        return null;
    }
    
    public interface GGroupObjectSupplier {
        GGroupObject get();
    }

    public boolean processBinding(GInputEvent ks, NativeEvent event, GGroupObjectSupplier groupObjectSupplier) {
        ArrayList<Binding> keyBinding = bindings.get(ks);
        if(keyBinding != null && !keyBinding.isEmpty()) { // optimization

            TreeMap<Integer, Binding> orderedBindings = new TreeMap<>();

            // increasing priority for group object
            GGroupObject groupObject = groupObjectSupplier.get();
            for(Binding binding : keyBinding) // descending sorting by priority
                if((binding.isSuitable == null || binding.isSuitable.apply(event)) && bindDialog(binding) && bindGroup(groupObject, binding) && bindEditing(binding, event) && bindShowing(binding))
                    orderedBindings.put(-(binding.priority + (equalGroup(groupObject, binding) ? 100 : 0)), binding);

            for (Binding binding : orderedBindings.values()) {
                if (binding.enabled()) {
                    if (isEditing()) {
                        commitEditingTable();
                    }
                    binding.pressed(event.getEventTarget());
                    stopPropagation(event);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean bindDialog(Binding binding) {
        switch (binding.bindDialog) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return isDialog();
            case NO:
                return !isDialog();
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindDialog);
        }
    }

    private boolean bindGroup(GGroupObject groupObject, Binding binding) {
        switch (binding.bindGroup) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return equalGroup(groupObject, binding);
            case NO:
                return !equalGroup(groupObject, binding);
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindGroup);
        }
    }

    private boolean equalGroup(GGroupObject groupObject, Binding binding) {
        return groupObject != null && groupObject.equals(binding.groupObject);
    }

    private boolean bindEditing(Binding binding, NativeEvent event) {
        switch (binding.bindEditing) {
            case AUTO:
                return !isEditing() || notTextCharEvent(event);
            case ALL:
                return true;
            case ONLY:
                return isEditing();
            case NO:
                return !isEditing();
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindEditing);
        }
    }

    private static List<Character> textChars = Arrays.asList(new Character[]{KeyCodes.KEY_DELETE, KeyCodes.KEY_BACKSPACE, KeyCodes.KEY_ENTER,
            KeyCodes.KEY_UP, KeyCodes.KEY_DOWN, KeyCodes.KEY_LEFT, KeyCodes.KEY_RIGHT});
    private boolean notTextCharEvent(NativeEvent event) {
        char c = (char) event.getKeyCode();
        return event.getCtrlKey() || (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && !textChars.contains(c));
    }

    private boolean bindShowing(Binding binding) {
        switch (binding.bindShowing) {
            case ALL:
                return true;
            case AUTO:
            case ONLY:
                return binding.showing();
            case NO:
                return !binding.showing();
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindShowing);
        }
    }


    private void handleKeyEvent(NativeEvent nativeEvent) {
        assert BrowserEvents.KEYDOWN.equals(nativeEvent.getType());

        final EventTarget target = nativeEvent.getEventTarget();
        if (!Element.is(target)) {
            return;
        }

        if (isPossibleEditKeyEvent(nativeEvent)) {
            GKeyStroke key = GKeyStroke.getKeyStroke(nativeEvent);
            
            processBinding(new GKeyInputEvent(key, null), nativeEvent, new GGroupObjectSupplier() {
                public GGroupObject get() {
                    return getGroupObject(Element.as(target));
                }
            });
        }
    }

    private void handleMouseEvent(NativeEvent nativeEvent) {
        final EventTarget target = nativeEvent.getEventTarget();
        if (!Element.is(target)) {
            return;
        }

        processBinding(new GMouseInputEvent(nativeEvent), nativeEvent, new GGroupObjectSupplier() {
            public GGroupObject get() {
                return getGroupObject(Element.as(target));
            }
        });
    }
}