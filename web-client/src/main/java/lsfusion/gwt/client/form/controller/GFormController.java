package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Event;
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
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.classes.GType;
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
import lsfusion.gwt.client.form.object.table.grid.view.GListViewType;
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;
import lsfusion.gwt.client.form.order.user.GOrder;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GPropertyGroupType;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.controller.*;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.property.panel.view.ActionPanelRenderer;
import lsfusion.gwt.client.form.property.table.view.GPropertyContextMenuPopup;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.ServerMessageProvider;
import lsfusion.gwt.client.view.StyleDefaults;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.client.base.GwtSharedUtils.removeFromDoubleMap;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.form.property.cell.GEditBindingMap.CHANGE;

public class GFormController extends ResizableSimplePanel implements ServerMessageProvider, EditManager {
    private static final int ASYNC_TIME_OUT = 50;

    private FormDispatchAsync dispatcher;

    private final GFormActionDispatcher actionDispatcher;

    private final FormsController formsController;
    private final FormContainer formContainer;

    private final GForm form;
    public final GFormLayout formLayout;

    private final boolean isDialog;

    private final HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects = new HashMap<>();

    private final Map<GGroupObject, List<GPropertyFilter>> currentFilters = new HashMap<>();

    private final Map<GGroupObject, GGridController> controllers = new LinkedHashMap<>();
    private final Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<>();

    private final LinkedHashMap<Long, ModifyObject> pendingModifyObjectRequests = new LinkedHashMap<>();
    private final NativeHashMap<GGroupObject, Long> pendingChangeCurrentObjectsRequests = new NativeHashMap<>();
    private final NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>> pendingChangePropertyRequests = new NativeHashMap<>();

    private boolean hasColumnGroupObjects;

    private Timer asyncTimer;
    private ActionPanelRenderer asyncView;

    private LoadingManager loadingManager = MainFrame.busyDialog ? new GBusyDialogDisplayer(this) : new LoadingBlocker(this);

    public FormsController getFormsController() {
        return formsController;
    }

    public GFormController(FormsController formsController, FormContainer formContainer, GForm gForm, boolean isDialog) {
        actionDispatcher = new GFormActionDispatcher(this);

        this.formsController = formsController;
        this.formContainer = formContainer;
        this.form = gForm;
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

        getElement().getStyle().setOverflow(Style.Overflow.AUTO);
        setFillWidget(formLayout);

        updateFormCaption();

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        if (form.initialFormChanges != null) {
            applyRemoteChanges(form.initialFormChanges);
            form.initialFormChanges = null;
        } else
            getRemoteChanges();

        initializeUserOrders();

        initializeAutoRefresh();

        DataGrid.initSinkMouseEvents(this);
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        if(GMouseStroke.isChangeEvent(event) || GMouseStroke.isDoubleChangeEvent(event))
            handleMouseEvent(event);
    }

    // will handle key events in upper container which will be better from UX point of view
    public static void initKeyEventHandler(Widget widget, Supplier<GFormController> currentForm) {
        widget.addDomHandler(event -> {
            GFormController form = currentForm.get();
            if(form != null)
                form.handleKeyEvent(event.getNativeEvent());
        }, KeyDownEvent.getType());
        widget.addDomHandler(event -> {
            GFormController form = currentForm.get();
            if(form != null)
                form.handleKeyEvent(event.getNativeEvent());
        }, KeyPressEvent.getType());
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
        if (filter.key != null)
            addBinding(new GKeyInputEvent(filter.key), (event) -> filterCheck.setValue(!filterCheck.getValue(), true), filterCheck, filterGroup.groupObject);
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

        filterBox.addStyleName("comboBoxFilter");
        filterBox.setHeight(StyleDefaults.COMPONENT_HEIGHT_STRING);

        addFilterView(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + 1);
        }
    }

    private void addFilterView(GRegularFilterGroup filterGroup, Widget filterWidget) {
        formLayout.addBaseComponent(filterGroup, filterWidget, null);
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
            controller.processFormChanges(changesDTO.requestIndex, fc, currentGridObjects);
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(fc);
        }

        formLayout.hideEmptyContainerViews();

        activateElements(fc);

        onResize();

        // в конце скроллим все таблицы к текущим ключам
        afterAppliedChanges();
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

    public void openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, Event initFilterEvent, final WindowHiddenHandler handler) {
        boolean isDockedModal = modalityType == GModalityType.DOCKED_MODAL;
        if (isDockedModal)
            ((FormDockable)formContainer).block();

        FormContainer blockingForm = formsController.openForm(form, modalityType, forbidDuplicate, initFilterEvent, () -> {
            if(isDockedModal) {
                ((FormDockable)formContainer).unblock();

                formsController.selectTab((FormDockable) formContainer);
            } else if(modalityType == GModalityType.DOCKED)
                formsController.ensureTabSelected();

            handler.onHidden();
        });

        if (isDockedModal)
            ((FormDockable)formContainer).setBlockingForm((FormDockable) blockingForm);
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

    public void executeEventAction(GPropertyDraw property, GGroupObjectValue columnKey, String actionSID) {
        DeferredRunner.get().commitDelayedGroupObjectChange(property.groupObject);
        dispatcher.execute(new ExecuteEventAction(property.ID, getFullCurrentKey(columnKey), actionSID), new ServerResponseCallback());
    }

    public void executePropertyEventAction(EventHandler handler, boolean isBinding, ExecuteEditContext editContext) {
        Event event = handler.event;
        if(BrowserEvents.CONTEXTMENU.equals(event.getType())) {
            handler.consume();
            GPropertyContextMenuPopup.show(editContext.getProperty(), event.getClientX(), event.getClientY(), actionSID -> {
                actionDispatcher.executePropertyActionSID(event, actionSID, editContext);
            });
        } else {
            String actionSID;
            if(isBinding) {
                if(GKeyStroke.isKeyEvent(event)) // we don't want to set focus on mouse binding (it's pretty unexpected behaviour)
                    editContext.trySetFocus(); // we want element to be focused on key binding (if it's possible)
                actionSID = CHANGE;
            } else {
                actionSID = editContext.getProperty().getEventSID(event);
                if(actionSID == null)
                    return;
            }

            if ((GEditBindingMap.CHANGE.equals(actionSID) || GEditBindingMap.CHANGE_WYS.equals(actionSID) || GEditBindingMap.GROUP_CHANGE.equals(actionSID)) &&
                    editContext.isReadOnly())
                return;

            GPropertyDraw property = editContext.getProperty();
            if(!property.hasChangeAction) // important for quickfilter not to consume event (however with propertyReadOnly, checkCanBeChanged there will be still some problems)
                return;

            handler.consume();

            if (GEditBindingMap.CHANGE.equals(actionSID)) {
                final boolean asyncModifyObject = isAsyncModifyObject(property);
                if (asyncModifyObject || property.changeType != null) {
                    if (property.askConfirm) {
                        blockingConfirm("lsFusion", property.askConfirmMessage, false, chosenOption -> {
                            if (chosenOption == DialogBoxHelper.OptionType.YES) {
                                executeSimpleChange(asyncModifyObject, event, editContext);
                            }
                        });
                    } else {
                        executeSimpleChange(asyncModifyObject, event, editContext);
                    }
                    return;
                }
            }

            actionDispatcher.executePropertyActionSID(event, actionSID, editContext);
        }
    }

    private void executeSimpleChange(boolean asyncModifyObject, Event event, ExecuteEditContext editContext) {
        GPropertyDraw property = editContext.getProperty();
        if (asyncModifyObject)
            modifyObject(editContext.getProperty(), editContext.getColumnKey());
        else {
            edit(property.changeType, event, false, null, value -> {
                Object oldValue = editContext.getValue();

                if(property.canUseChangeValueForRendering()) // changing model, to rerender new value
                    editContext.setValue(value);

                changeProperty(property, editContext.getColumnKey(), (Serializable) value, oldValue);
            }, value -> {}, () -> {}, editContext);
        }
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
        final int position = controller.getSelectedRow();

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

    public void quickFilter(Event event, int initialFilterPropertyID) {
        GPropertyDraw propertyDraw = getProperty(initialFilterPropertyID);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            focusProperty(propertyDraw);
            controllers.get(propertyDraw.groupObject).quickEditFilter(event, propertyDraw, null);
        }
    }

    public void getInitialFilterProperty(ErrorHandlingCallback<NumberResult> callback) {
        dispatcher.execute(new GetInitialFilterProperty(), callback);
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
    public long changeMode(final GGroupObject groupObject, boolean enableGroup, int pageSize, GListViewType viewType) {
        return changeMode(groupObject, true, enableGroup ? new ArrayList<>() : null, enableGroup ? new ArrayList<>() : null, 0, null, pageSize, true, null, viewType);
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
        return dispatcher.execute(new ChangeMode(groupObject.ID, setGroup, propertyIDs, columnKeys, aggrProps, aggrType, pageSize, forceRefresh, updateMode, viewType), new ServerResponseCallback());
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

    public void setAsyncView(ActionPanelRenderer asyncView) {
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
            asyncView.setImage(null);
        }
    }

    public void previewBlurEvent(Event event) {
        formsController.setLastBlurredElement(Element.as(event.getEventTarget()));
    }
    public Element getLastBlurredElement() {
        return formsController.getLastBlurredElement();
    }

    protected void onFormHidden(int closeDelay) {
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

    public boolean isModal() {
        return formContainer instanceof ModalForm;
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

    public void focusFirstWidget() {
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

    private class ServerResponseCallback extends ErrorHandlingCallback<ServerResponseResult> {

        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);
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
    public void addPropertyBindings(GPropertyDraw propertyDraw, BindingExec bindingExec, Widget widget) {
        for(GInputBindingEvent bindingEvent : propertyDraw.bindingEvents) // supplier for optimization
            addBinding(bindingEvent.inputEvent, bindingEvent.env, bindingExec, widget, propertyDraw.groupObject);
    }

    public void addBinding(GInputEvent event, BindingExec pressed, Widget component, GGroupObject groupObject) {
        addBinding(event, GBindingEnv.AUTO, pressed, component, groupObject);
    }
    public void addBinding(GInputEvent event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        addBinding(event::isEvent, env, pressed, component, groupObject);
    }
    public void addBinding(BindingCheck event, GBindingEnv env, BindingExec pressed, Widget component, GGroupObject groupObject) {
        addBinding(new GBindingEvent(event, env), new Binding(groupObject) {
            @Override
            public boolean showing() {
                return component != null ? isShowing(component) : true;
            }

            @Override
            public void exec(Event event) {
                pressed.exec(event);
            }
        });
    }
    public void addBinding(GBindingEvent event, Binding action) {
        bindingEvents.add(event);
        bindings.add(action);
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

    public boolean processBinding(Event event, GGroupObjectSupplier groupObjectSupplier) {
        TreeMap<Integer, Binding> orderedBindings = new TreeMap<>(); // descending sorting by priority

        GGroupObject groupObject = groupObjectSupplier.get();
        for (int i = 0, size = bindingEvents.size(); i < size; i++) {
            GBindingEvent bindingEvent = bindingEvents.get(i);
            if (bindingEvent.event.check(event)) {
                Binding binding = bindings.get(i);
                boolean equalGroup;
                GBindingEnv bindingEnv = bindingEvent.env;
                if(bindDialog(bindingEnv) &&
                    bindGroup(bindingEnv, equalGroup = nullEquals(groupObject, binding.groupObject)) &&
                    bindEditing(bindingEnv) &&
                    bindShowing(bindingEnv, binding.showing()))
                orderedBindings.put(-(GwtClientUtils.nvl(bindingEnv.priority, i) + (equalGroup ? 100 : 0)), binding); // increasing priority for group object
            }
        }

        for (Binding binding : orderedBindings.values()) {
            if (binding.enabled()) {
                checkCommitEditing();
                binding.exec(event);
                stopPropagation(event);
                return true;
            }
        }
        return false;
    }

    public void checkCommitEditing() {
        if(cellEditor != null)
            cellEditor.commitEditing(editContext.getRenderElement());
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
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindDialog);
        }
    }

    private boolean bindGroup(GBindingEnv bindingEvent, boolean equalGroup) {
        switch (bindingEvent.bindGroup) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return equalGroup;
            case NO:
                return !equalGroup;
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode");
        }
    }

    private boolean bindEditing(GBindingEnv binding) {
        switch (binding.bindEditing) {
            case AUTO:
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

    private boolean bindShowing(GBindingEnv binding, boolean showing) {
        switch (binding.bindShowing) {
            case ALL:
                return true;
            case AUTO:
            case ONLY:
                return showing;
            case NO:
                return !showing;
            default:
                throw new UnsupportedOperationException("Unsupported bindingMode " + binding.bindShowing);
        }
    }

    private void handleKeyEvent(NativeEvent nativeEvent) {
        final EventTarget target = nativeEvent.getEventTarget();
        if (!Element.is(target)) {
            return;
        }
        processBinding((Event) nativeEvent, () -> getGroupObject(Element.as(target)));
    }

    private void handleMouseEvent(NativeEvent nativeEvent) {
        final EventTarget target = nativeEvent.getEventTarget();
        if (!Element.is(target)) {
            return;
        }
        processBinding((Event) nativeEvent, () -> getGroupObject(Element.as(target)));
    }

    private CellEditor cellEditor;

    private EditContext editContext;

    private Consumer<Object> editBeforeCommit;
    private Consumer<Object> editAfterCommit;
    private Runnable editCancel;

    private Element focusedElement;
    private Object forceSetFocus;

    public boolean isEditing() {
        return editContext != null;
    }

    public void edit(GType type, Event event, boolean hasOldValue, Object oldValue, Consumer<Object> beforeCommit, Consumer<Object> afterCommit, Runnable cancel, EditContext editContext) {
        assert this.editContext == null;
        CellEditor cellEditor = type.createGridCellEditor(this, editContext.getProperty());
        if (cellEditor != null) {
            editBeforeCommit = beforeCommit;
            editAfterCommit = afterCommit;
            editCancel = cancel;

            this.editContext = editContext;

            Element element = editContext.getRenderElement();
            if (cellEditor instanceof ReplaceCellEditor) {
                focusedElement = GwtClientUtils.getFocusedElement();
                if(!editContext.isFocusable()) // assert that otherwise it's already has focus
                    forceSetFocus = editContext.forceSetFocus();

                removeAllChildren(element);
                ((ReplaceCellEditor)cellEditor).renderDom(element, editContext.getRenderContext(), editContext.getUpdateContext());
            }

            this.cellEditor = cellEditor; // not sure if it should before or after startEditing, but definitely after removeAllChildren, since it leads to blur for example
            cellEditor.startEditing(event, element, hasOldValue ? oldValue : editContext.getValue());
        } else
            editCancel.run();
    }

    @Override
    public void commitEditing(Object value, boolean blurred) {
        editBeforeCommit.accept(value);
        editBeforeCommit = null;

        finishEditing(blurred);

        editAfterCommit.accept(value);
        editAfterCommit = null;
    }

    @Override
    public void cancelEditing() {
        finishEditing(false);

        editCancel.run();
        editCancel = null;
    }

    private void finishEditing(boolean blurred) {
        boolean replaceEditor = cellEditor instanceof ReplaceCellEditor;
        cellEditor = null;

        EditContext editContext = this.editContext;
        this.editContext = null;

        if(replaceEditor) {
            rerender(editContext.getProperty(), editContext.getRenderElement(), editContext.getRenderContext());

            if(forceSetFocus != null) {
                editContext.restoreSetFocus(forceSetFocus);
                forceSetFocus = null;
            }

            if(blurred) // when editing is commited (thus editing element is removed), set last blurred element to main widget to keep focus there
                formsController.setLastBlurredElement(editContext.getFocusElement());
            else {
                if (focusedElement != null)
                    focusedElement.focus();
            }
        }

        update(editContext.getProperty(), editContext.getRenderElement(), editContext.getValue(), editContext.getUpdateContext());
    }

    public void render(GPropertyDraw property, Element element, RenderContext renderContext) {
        if(editContext != null && editContext.getRenderElement() == element) { // is edited
            assert false;
            return;
        }

        property.getCellRenderer().renderStatic(element, renderContext);
    }
    public void rerender(GPropertyDraw property, Element element, RenderContext renderContext) {
        removeAllChildren(element);

        render(property, element, renderContext);
    }
    public void update(GPropertyDraw property, Element element, Object value, UpdateContext updateContext) {
        if(editContext != null && editContext.getRenderElement() == element) // is edited
            return;

        property.getCellRenderer().renderDynamic(element, value, updateContext);
    }

    public static void setBackgroundColor(Element element, String color) {
        if (color != null) {
            element.getStyle().setBackgroundColor(getDisplayColor(color));
        } else {
            element.getStyle().clearBackgroundColor();
        }
    }

    public static void setForegroundColor(Element element, String color) {
        if (color != null) {
            element.getStyle().setColor(getDisplayColor(color));
        } else {
            element.getStyle().clearColor();
        }
    }

    public void onPropertyBrowserEvent(EventHandler handler, Element cellParent, Element focusElement, Runnable onOuterEditBefore, Runnable onEdit, Runnable onOuterEditAfter, Runnable onCut, Runnable onPaste) {
        boolean isPropertyEditing = cellEditor != null && editContext.getRenderElement() == cellParent;
        if(isPropertyEditing)
            cellEditor.onBrowserEvent(cellParent, handler);

        if(handler.consumed)
            return;

        if(GMouseStroke.isChangeEvent(handler.event))
            focusElement.focus(); // it should be done on CLICK, but also on MOUSEDOWN, since we want to focus even if mousedown is later consumed
        else if(GMouseStroke.isDownEvent(handler.event))
            handler.consume(); // we want to postpone focusing (and focus later on CLICK event)

        if(handler.consumed)
            return;

        onOuterEditBefore.run();

        if(handler.consumed)
            return;

        if (!isPropertyEditing) { // if editor did not consume event, we don't want it to be handled by "renderer" since it doesn't exist
            if (GKeyStroke.isCopyToClipboardEvent(handler.event)) {
                onCut.run();
            } else if (GKeyStroke.isPasteFromClipboardEvent(handler.event)) {
                onPaste.run();
            } else {
                onEdit.run();
            }
        }

        if(handler.consumed)
            return;

        onOuterEditAfter.run();
    }
}