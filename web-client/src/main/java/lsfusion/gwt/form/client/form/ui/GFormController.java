package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
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
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.HotkeyManager;
import lsfusion.gwt.form.client.LoadingBlocker;
import lsfusion.gwt.form.client.dispatch.DeferredRunner;
import lsfusion.gwt.form.client.dispatch.FormDispatchAsync;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.FormsController;
import lsfusion.gwt.form.client.form.ServerMessageProvider;
import lsfusion.gwt.form.client.form.dispatch.GEditPropertyHandler;
import lsfusion.gwt.form.client.form.dispatch.GFormActionDispatcher;
import lsfusion.gwt.form.client.form.dispatch.GSimpleChangePropertyDispatcher;
import lsfusion.gwt.form.client.form.ui.classes.ClassChosenHandler;
import lsfusion.gwt.form.client.form.ui.classes.GResizableClassDialog;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalDialog;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.shared.actions.form.*;
import lsfusion.gwt.form.shared.actions.navigator.GenerateID;
import lsfusion.gwt.form.shared.actions.navigator.GenerateIDResult;
import lsfusion.gwt.form.shared.view.*;
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

import java.io.Serializable;
import java.util.*;

import static lsfusion.gwt.base.shared.GwtSharedUtils.putToDoubleNativeMap;
import static lsfusion.gwt.base.shared.GwtSharedUtils.removeFromDoubleMap;

//public class GFormController extends ResizableSimplePanel implements ServerMessageProvider {
public class GFormController extends FlexPanel implements ServerMessageProvider {

    private final FormDispatchAsync dispatcher;

    private final GFormActionDispatcher actionDispatcher;
    private final GSimpleChangePropertyDispatcher simpleDispatcher;

    private final FormsController formsController;

    private final GForm form;
    protected final GFormLayout formLayout;
    private final boolean isDialog;

    private final HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects = new HashMap<GGroupObject, List<GGroupObjectValue>>();

    private final Map<GGroupObject, List<GPropertyFilter>> currentFilters = new HashMap<GGroupObject, List<GPropertyFilter>>();

    private final Map<GGroupObject, GGroupObjectController> controllers = new LinkedHashMap<GGroupObject, GGroupObjectController>();
    private final Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<GTreeGroup, GTreeGroupController>();

    private final LinkedHashMap<Integer, ModifyObject> lastModifyObjectRequests = new LinkedHashMap<Integer, ModifyObject>();
    private final NativeHashMap<GGroupObject, Integer> lastChangeCurrentObjectsRequestIndices = new NativeHashMap<GGroupObject, Integer>();
    private final NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Integer>> lastChangePropertyRequestIndices = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Integer>>();
    private final NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>> lastChangePropertyRequestValues = new NativeHashMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>>();

    private boolean defaultOrdersInitialized = false;
    private boolean hasColumnGroupObjects;

    private Timer asyncTimer;
    private PanelRenderer asyncView;
    private final int ASYNC_TIME_OUT = 50;
    private boolean needToResize = false;

    private boolean initialResizeProcessed = false;

    private HotkeyManager hotkeyManager = new HotkeyManager();

    private LoadingBlocker blocker = new LoadingBlocker(this);

    private boolean blocked = false;
    private boolean selected = true;
    private boolean formHidden = false;

    private static final Comparator<GPropertyDraw> COMPARATOR_USERSORT = new Comparator<GPropertyDraw>() {
        @Override
        public int compare(GPropertyDraw c1, GPropertyDraw c2) {
            if (c1.ascendingSortUser != null && c2.ascendingSortUser != null) {
                return c1.sortUser - c2.sortUser;
            } else {
                return 0;
            }
        }
    };

    public GFormController(FormsController formsController, GForm gForm, final boolean isDialog) {
        super(true);

        actionDispatcher = new GFormActionDispatcher(this);
        simpleDispatcher = new GSimpleChangePropertyDispatcher(this);

        this.formsController = formsController;
        this.form = gForm;
        this.isDialog = isDialog;

        dispatcher = new FormDispatchAsync(this);

        formLayout = new GFormLayout(this, form.mainContainer);

        asyncTimer = new Timer() {
            @Override
            public void run() {
                asyncView.setIcon("loading.gif");
            }
        };

        addStyleName("formController");

        if (form.sID != null) {
            getElement().setAttribute("lsfusion-form", form.sID);
        }

        addFill(formLayout);

        initializeUserPreferences();

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        needToResize = true;
        applyRemoteChanges(form.initialFormChanges);
        form.initialFormChanges = null;

        initializeAutoRefresh();

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                onInitialFormChangesReceived();
            }
        });

        hotkeyManager.install(this);
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
        addFilterComponent(filterGroup, filterCheck);

        if (filterGroup.defaultFilterIndex >= 0) {
            filterCheck.setValue(true, false);
        }

        filterCheck.getElement().setPropertyObject("groupObject", filterGroup.groupObject);
        if (filter.key != null) {
            addHotkeyBinding(filterGroup.groupObject, filter.key, new HotkeyManager.Binding() {
                @Override
                public boolean onKeyPress(NativeEvent event, GKeyStroke key) {
                    filterCheck.setValue(!filterCheck.getValue(), true);
                    return true;
                }
            });
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox(false);
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
                        filterBox.setSelectedIndex(filterIndex + 1);
                        setRegularFilter(filterGroup, filterIndex);
                        return true;
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

        addFilterComponent(filterGroup, filterBox);
        if (filterGroup.defaultFilterIndex >= 0) {
            filterBox.setSelectedIndex(filterGroup.defaultFilterIndex + 1);
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, int index) {
        setRemoteRegularFilter(filterGroup, index == -1 ? null : filterGroup.filters.get(index));
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void addFilterComponent(GRegularFilterGroup filterGroup, Widget filterWidget) {
        GGroupObjectLogicsSupplier logicsSupplier = getGroupObjectLogicsSupplier(filterGroup.groupObject);
        if (logicsSupplier != null) {
            logicsSupplier.addFilterComponent(filterGroup, filterWidget);
        }
    }

    private void initializeUserPreferences() {
        if (form.userPreferences != null) {
            for (GPropertyDraw property : getPropertyDraws()) {
                for (GGroupObjectUserPreferences groupObjectUP : form.userPreferences.getGroupObjectUserPreferencesList()) {
                    GColumnUserPreferences columnUP = groupObjectUP.getColumnUserPreferences().get(property.sID);
                    if (columnUP != null) {
                        property.hideUser = columnUP.isNeedToHide();
                        if (columnUP.getWidthUser() != null) {
                            property.widthUser = columnUP.getWidthUser();
                        }
                        if (columnUP.getOrderUser() != null) {
                            property.orderUser = columnUP.getOrderUser();
                        }
                        if (columnUP.getSortUser() != null) {
                            property.sortUser = columnUP.getSortUser();
                            property.ascendingSortUser = columnUP.getAscendingSortUser();
                        }
                    }
                }
            }
            for (GGroupObjectUserPreferences groupObjectUP : form.userPreferences.getGroupObjectUserPreferencesList()) {
                GGroupObject groupObject = form.getGroupObject(groupObjectUP.getGroupObjectSID());
                if (groupObject != null) {
                    groupObject.hasUserPreferences = groupObjectUP.hasUserPreferences();
                }

            }
        }
        form.userPreferences = null;
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form);
            treeControllers.put(treeGroup, treeController);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GGroupObjectController controller = new GGroupObjectController(this, group);
                controllers.put(group, controller);
            }
        }

        hasColumnGroupObjects = false;
        for (GPropertyDraw property : form.propertyDraws) {
            if (property.columnGroupObjects != null && !property.columnGroupObjects.isEmpty()) {
                hasColumnGroupObjects = true;
            }
            if (property.groupObject == null) {
                controllers.put(null, new GGroupObjectController(this, null));
            }
        }
    }

    private void initializeDefaultOrders() {
        applyOrders(form.defaultOrders);
        defaultOrdersInitialized = true;

        LinkedHashMap<GPropertyDraw, Boolean> userOrders = new LinkedHashMap<GPropertyDraw, Boolean>();
        for (GGroupObjectController controller : controllers.values()) {
            boolean userPreferencesEmpty = true;
            boolean hasUserPreferences = controller.groupObject != null && controller.groupObject.hasUserPreferences;
            if (hasUserPreferences) {
                List<GPropertyDraw> propertyDrawList = controller.getPropertyDraws();
                Collections.sort(propertyDrawList, COMPARATOR_USERSORT);
                for (GPropertyDraw property : controller.getPropertyDraws()) {
                    if (property.sortUser != null && property.ascendingSortUser != null) {
                        userOrders.put(property, property.ascendingSortUser);
                        userPreferencesEmpty = false;
                    }
                }
            }
            if (userPreferencesEmpty && hasUserPreferences) {
                controller.clearOrders(controller.groupObject);
            }
        }
        applyOrders(userOrders);
    }

    private void initializeAutoRefresh() {
        if (form.autoRefresh > 0) {
            final String FORM_REFRESH_PROPERTY_SID = "formRefresh";

            Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
                @Override
                public boolean execute() {
                    if (!isEditing() && selected && !blocked && !formHidden && !isDialog) {
                        GPropertyDraw refreshProperty = form.getProperty(FORM_REFRESH_PROPERTY_SID);
                        if (refreshProperty != null) {
                            executeEditAction(refreshProperty, GGroupObjectValue.EMPTY, GEditBindingMap.CHANGE, new ServerResponseCallback());
                        }
                    }
                    return !formHidden;
                }
            }, form.autoRefresh * 1000);
        }
    }

    public void totalResize() {
        formLayout.totalResize();
        needToResize = false;
    }

    public void setNeedToResize(boolean needToResize) {
        this.needToResize = needToResize;
    }

    private void applyOrders(LinkedHashMap<GPropertyDraw, Boolean> orders) {
        Set<GGroupObject> wasOrder = new HashSet<GGroupObject>();
        for (Map.Entry<GPropertyDraw, Boolean> entry : orders.entrySet()) {
            GPropertyDraw property = entry.getKey();
            GGroupObject groupObject = property.groupObject;
            GGroupObjectLogicsSupplier groupObjectLogicsSupplier = getGroupObjectLogicsSupplier(groupObject);
            if (groupObjectLogicsSupplier != null) {
                groupObjectLogicsSupplier.changeOrder(property, !wasOrder.contains(groupObject) ? GOrder.REPLACE : GOrder.ADD);
                wasOrder.add(groupObject);
                if (!entry.getValue()) {
                    groupObjectLogicsSupplier.changeOrder(property, GOrder.DIR);
                }
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
        HashSet<GGroupObject> changedGroups = null;
        if (hasColumnGroupObjects) {
            changedGroups = new HashSet<GGroupObject>();
            for (Map.Entry<GGroupObject, GClassViewType> entry : fc.classViews.entrySet()) {
                GClassViewType classView = entry.getValue();
                if (classView != GClassViewType.GRID) {
                    changedGroups.add(entry.getKey());
                    currentGridObjects.remove(entry.getKey());
                }
            }
            currentGridObjects.putAll(fc.gridObjects);
            changedGroups.addAll(fc.gridObjects.keySet());
        }

        modifyFormChangesWithModifyObjectAsyncs(fc);

        modifyFormChangesWithChangeCurrentObjectAsyncs(fc);

        modifyFormChangesWithChangePropertyAsyncs(fc);

        // затем одним скопом обновляем данные во всех таблицах
        for (GGroupObjectController controller : controllers.values()) {
            controller.processFormChanges(fc, currentGridObjects, changedGroups);
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(fc);
        }

        formLayout.hideEmptyContainerViews();
        if (!fc.classViews.isEmpty() || needToResize) {
            totalResize();
        }

        // в конце скроллим все таблицы к текущим ключам
        applyScrollPositions();
    }

    private void applyScrollPositions() {
        for (GGroupObjectController controller : controllers.values()) {
            controller.restoreScrollPosition();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.restoreScrollPosition();
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(GFormChanges fc) {
        int currentDispatchingRequestIndex = dispatcher.getCurrentDispatchingRequestIndex();

        for (Iterator<Map.Entry<Integer, ModifyObject>> iterator = lastModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, ModifyObject> cell = iterator.next();
            if (cell.getKey() <= currentDispatchingRequestIndex) {
                iterator.remove();
            }
        }

        for (Map.Entry<Integer, ModifyObject> e : lastModifyObjectRequests.entrySet()) {
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

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(final GFormChanges fc) {
        final int currentDispatchingRequestIndex = dispatcher.getCurrentDispatchingRequestIndex();

        lastChangeCurrentObjectsRequestIndices.foreachEntry(new Function2<GGroupObject, Integer>() {
            @Override
            public void apply(GGroupObject group, Integer requestIndex) {
                if (requestIndex <= currentDispatchingRequestIndex) {
                    lastChangeCurrentObjectsRequestIndices.remove(group);
                } else {
                    fc.objects.remove(group);
                }
            }
        });
    }

    private void modifyFormChangesWithChangePropertyAsyncs(final GFormChanges fc) {
        final int currentDispatchingRequestIndex = dispatcher.getCurrentDispatchingRequestIndex();

        lastChangePropertyRequestIndices.foreachEntry(new Function2<GPropertyDraw, NativeHashMap<GGroupObjectValue, Integer>>() {
            @Override
            public void apply(final GPropertyDraw property, NativeHashMap<GGroupObjectValue, Integer> values) {
                values.foreachEntry(new Function2<GGroupObjectValue, Integer>() {
                    @Override
                    public void apply(GGroupObjectValue keys, Integer requestIndex) {
                        if (requestIndex <= currentDispatchingRequestIndex) {

                            removeFromDoubleMap(lastChangePropertyRequestIndices, property, keys);
                            Change change = removeFromDoubleMap(lastChangePropertyRequestValues, property, keys);

                            HashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(property);
                            if (propertyValues == null) {
                                // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                                propertyValues = new HashMap<GGroupObjectValue, Object>();
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

        lastChangePropertyRequestValues.foreachEntry(new Function2<GPropertyDraw, NativeHashMap<GGroupObjectValue, Change>>() {
            @Override
            public void apply(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Change> values) {
                final HashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(property);
                if (propertyValues != null) {
                    values.foreachEntry(new Function2<GGroupObjectValue, Change>() {
                        @Override
                        public void apply(GGroupObjectValue group, Change change) {
                            propertyValues.put(group, change.newValue);
                        }
                    });
                }
            }
        });
    }

    public void openForm(GForm form, GModalityType modalityType, final WindowHiddenHandler handler) {
        if (isDialog && !modalityType.isDialog()) {
            modalityType = GModalityType.MODAL;
        } else if (modalityType == GModalityType.DOCKED_MODAL) {
            block();
        }

        formsController.openForm(form, modalityType, new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                unblock();
                handler.onHidden();
            }
        });
    }

    public void showModalDialog(GForm form, EditEvent initFilterEvent, final WindowHiddenHandler handler) {
        WindowHiddenHandler dialogHiddenHandler = new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                blocked = false;
                handler.onHidden();
            }
        };
        GResizableModalDialog.showDialog(formsController, form, initFilterEvent, dialogHiddenHandler);
        blocked = true;
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
        lastChangeCurrentObjectsRequestIndices.put(group, requestIndex);
    }

    public void changeGroupObjectLater(final GGroupObject group, final GGroupObjectValue key) {
        DeferredRunner.get().scheduleDelayedGroupObjectChange(group, new DeferredRunner.AbstractCommand() {
            @Override
            public void execute() {
                changeGroupObject(group, key);
            }
        });
    }

    public void pasteExternalTable(List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, String dataLine) {
        syncDispatch(new PasteExternalTable(properties, columnKeys, dataLine), new ServerResponseCallback());
    }

    public void changePageSizeAfterUnlock(final GGroupObject groupObject, final int pageSize) {
        Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (blocker.isVisible()) {
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
        syncDispatch(new ExecuteEditAction(property.ID, columnKey == null ? null : columnKey, actionSID), callback);
    }

    public void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(actionResults), callback);
    }

    public void throwInServerInvocation(Exception exception) {
        syncDispatch(new ThrowInInvocation(exception), new ErrorHandlingCallback<ServerResponseResult>());
    }

    public <T extends Result> void syncDispatch(FormBoundAction<T> action, AsyncCallback<T> callback) {
        //todo: возможно понадобится сделать чтото более сложное как в
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        blocker.start();
        dispatcher.execute(action, new WrapperAsyncCallbackEx<T>(callback) {
            @Override
            public void preProcess() {
                blocker.stop();
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

    public void changeProperty(GEditPropertyHandler editHandler, GPropertyDraw property, GGroupObjectValue columnKey, Serializable value, Object oldValue) {
        editHandler.updateEditValue(value);

        int requestIndex = dispatcher.execute(new ChangeProperty(property.ID, getFullCurrentKey(columnKey), value, null), new ServerResponseCallback());

        GGroupObjectLogicsSupplier controller = getGroupObjectLogicsSupplier(property.groupObject);

        GGroupObjectValue propertyKey = controller != null && !controller.hasPanelProperty(property)
                                        ? columnKey != null
                                            ? new GGroupObjectValueBuilder(controller.getCurrentKey(), columnKey).toGroupObjectValue()
                                            : controller.getCurrentKey()
                                        : columnKey;

        putToDoubleNativeMap(lastChangePropertyRequestIndices, property, propertyKey, requestIndex);
        putToDoubleNativeMap(lastChangePropertyRequestValues, property, propertyKey, new Change(value, oldValue));
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

        if (add) {
            NavigatorDispatchAsync.Instance.get().execute(new GenerateID(), new ErrorHandlingCallback<GenerateIDResult>() {
                @Override
                public void success(GenerateIDResult result) {
                    executeModifyObject(property, columnKey, object, add, result.ID, new GGroupObjectValue(object.ID, result.ID));
                }
            });
        } else {
            final GGroupObjectValue value = controllers.get(object.groupObject).getCurrentKey();
            final int ID = (Integer) value.getValue(0);
            executeModifyObject(property, columnKey, object, add, ID, value);
        }
    }

    private void executeModifyObject(GPropertyDraw property, GGroupObjectValue columnKey, GObject object, boolean add, int ID, GGroupObjectValue value) {
        final GGroupObjectValue fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        controllers.get(object.groupObject).modifyGroupObject(value, add);

        int requestIndex = dispatcher.execute(new ChangeProperty(property.ID, fullCurrentKey, null, add ? ID : null), new ServerResponseCallback());
        lastChangeCurrentObjectsRequestIndices.put(object.groupObject, requestIndex);
        lastModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value));
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
        syncDispatch(new ExpandGroupObject(group.ID, value), new ServerResponseCallback());
    }

    public void collapseGroupObject(GGroupObject group, GGroupObjectValue value) {
        syncDispatch(new CollapseGroupObject(group.ID, value), new ServerResponseCallback());
    }

    public void setTabVisible(GContainer tabbedPane, GComponent visibleComponent) {
        dispatcher.execute(new SetTabVisible(tabbedPane.ID, visibleComponent.ID), new ServerResponseCallback());
        relayoutTables(visibleComponent);
    }

    public void closePressed() {
        syncDispatch(new ClosePressed(), new ServerResponseCallback());
    }

    public void okPressed() {
        syncDispatch(new OkPressed(), new ServerResponseCallback());
    }

    // судя по документации, TabPanel криво работает в StandardsMode. в данном случае неправильно отображает
    // таблицы, кроме тех, что в первой вкладке. поэтому вынуждены сами вызывать onResize() для заголовков таблиц
    private void relayoutTables(GComponent component) {
        //TODO: ПРОТЕСТИРОВАТЬ: после переписывания TabPanel на div'ы этот хак возможно не нужен...
        if (controllers.isEmpty() && treeControllers.isEmpty()) {
            return;
        }
        GContainer container = component == null || component instanceof GContainer ? (GContainer) component : component.container;
        if (container != null) {
            formLayout.getFormContainer(container).onResize();
        }
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
                groupFilters = new ArrayList<GPropertyFilter>();
            }
            currentFilters.put(group, groupFilters);
        }

        applyCurrentFilters();
    }

    private void applyCurrentFilters() {
        ArrayList<GPropertyFilterDTO> filters = new ArrayList<GPropertyFilterDTO>();

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

    public void runSingleGroupReport(int groupObjectID, final boolean toExcel) {
        syncDispatch(new SingleGroupReport(groupObjectID, toExcel), new ErrorHandlingCallback<StringResult>() {
            @Override
            public void success(StringResult result) {
                openReportWindow(result.get());
            }
        });
    }

    public void openReportWindow(String fileName) {
        String reportUrl = GwtClientUtils.getWebAppBaseURL() + "downloadFile?name=" + fileName;
        Window.open(reportUrl, "Report", "");
    }

    public void saveUserPreferences(GFormUserPreferences userPreferences, boolean forAllUsers, ErrorHandlingCallback<VoidResult> callback) {
        syncDispatch(new SaveUserPreferencesAction(userPreferences, forAllUsers), callback);
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

    public void blockingConfirm(String caption, String message, final DialogBoxHelper.CloseCallback callback) {
        DialogBoxHelper.showConfirmBox(caption, message, callback);
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

    public boolean isDialog() {
        return isDialog;
    }

    public GForm getForm() {
        return form;
    }

    public List<GObject> getObjects() {
        ArrayList<GObject> objects = new ArrayList<GObject>();
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
            if (!initialResizeProcessed) { // до чего-нибудь мог не успеть дойти onResize() при открытии (открытие сразу нескольких форм)
                relayoutTables(form.mainContainer);
                initialResizeProcessed = true;
            }
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

    private static final class Change {
        public Object newValue;
        public Object oldValue;

        private Change(Object newValue, Object oldValue) {
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
    }

    private static class ModifyObject {
        public final GObject object;
        public final boolean add;
        public final GGroupObjectValue value;

        private ModifyObject(GObject object, boolean add, GGroupObjectValue value) {
            this.object = object;
            this.add = add;
            this.value = value;
        }
    }

    protected void onInitialFormChangesReceived() {
        scheduleFocusFirstWidget();
    }

    public void scheduleFocusFirstWidget() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
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

    private class ServerResponseCallback extends ErrorHandlingCallback<ServerResponseResult> {
        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);
        }
    }
}