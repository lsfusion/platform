package platform.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.base.client.WrapperAsyncCallbackEx;
import platform.gwt.form.client.ErrorHandlingCallback;
import platform.gwt.form.client.LoadingBlocker;
import platform.gwt.form.client.dispatch.DeferredRunner;
import platform.gwt.form.client.dispatch.FormDispatchAsync;
import platform.gwt.form.client.dispatch.NavigatorDispatchAsync;
import platform.gwt.form.client.form.FormsController;
import platform.gwt.form.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form.client.form.dispatch.GFormActionDispatcher;
import platform.gwt.form.client.form.ui.classes.ClassChosenHandler;
import platform.gwt.form.client.form.ui.classes.GResizableClassDialog;
import platform.gwt.form.client.form.ui.dialog.DialogBoxHelper;
import platform.gwt.form.client.form.ui.dialog.GResizableModalDialog;
import platform.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form.shared.actions.form.*;
import platform.gwt.form.shared.actions.navigator.GenerateID;
import platform.gwt.form.shared.actions.navigator.GenerateIDResult;
import platform.gwt.form.shared.view.*;
import platform.gwt.form.shared.view.changes.GFormChanges;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;
import platform.gwt.form.shared.view.classes.GObjectClass;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.window.GModalityType;

import java.io.Serializable;
import java.util.*;

import static platform.gwt.base.shared.GwtSharedUtils.putToDoubleMap;
import static platform.gwt.base.shared.GwtSharedUtils.removeFromDoubleMap;

public class GFormController extends SimplePanel {

    private final FormDispatchAsync dispatcher;

    private final GFormActionDispatcher actionDispatcher;

    private final FormsController formsController;

    private final GForm form;
    protected final GFormLayout formLayout;
    private final boolean isDialog;

    private final HashMap<GGroupObject, List<GGroupObjectValue>> currentGridObjects = new HashMap<GGroupObject, List<GGroupObjectValue>>();

    private final Map<GGroupObject, GGroupObjectController> controllers = new LinkedHashMap<GGroupObject, GGroupObjectController>();
    private final Map<GTreeGroup, GTreeGroupController> treeControllers = new LinkedHashMap<GTreeGroup, GTreeGroupController>();
    private final LinkedHashMap<Long, ModifyObject> lastModifyObjectRequests = new LinkedHashMap<Long, ModifyObject>();
    private final HashMap<GGroupObject, Long> lastChangeCurrentObjectsRequestIndices = new HashMap<GGroupObject, Long>();
    private final HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Long>> lastChangePropertyRequestIndices = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Long>>();
    private final HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Change>> lastChangePropertyRequestValues = new HashMap<GPropertyDraw, HashMap<GGroupObjectValue, Change>>();

    private boolean defaultOrdersInitialized = false;
    private boolean initialFormChangesReceived = false;
    private boolean hasColumnGroupObjects;

    public GFormController(FormsController formsController, GForm gForm, final boolean isDialog) {
        actionDispatcher = new GFormActionDispatcher(this);

        this.formsController = formsController;
        this.form = gForm;
        this.isDialog = isDialog;

        dispatcher = new FormDispatchAsync(this);

        formLayout = new GFormLayout(this, gForm.mainContainer);

        addStyleName("formController");

        add(formLayout);

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        formLayout.hideEmptyContainerViews();
        formLayout.totalResize();

        processRemoteChanges();
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
        final CheckBox filterCheck = new CheckBox(filter.caption);
        filterCheck.setValue(false);
        filterCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> e) {
                setRegularFilter(filterGroup, e.getValue() != null && e.getValue() ? filter : null);
            }
        });
        filterCheck.addStyleName("checkBoxFilter");
        addFilterComponent(filterGroup, filterCheck);

        if (filterGroup.defaultFilter >= 0) {
            filterCheck.setValue(true);
        }
    }

    private void createMultipleFilterComponent(final GRegularFilterGroup filterGroup) {
        final ListBox filterBox = new ListBox(false);
        filterBox.addItem("(Все)", "-1");

        ArrayList<GRegularFilter> filters = filterGroup.filters;
        for (int i = 0; i < filters.size(); i++) {
            GRegularFilter filter = filters.get(i);
            filterBox.addItem(filter.caption, "" + i);
        }

        filterBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int ind = Integer.parseInt(filterBox.getValue(filterBox.getSelectedIndex()));
                setRegularFilter(filterGroup, ind == -1 ? null : filterGroup.filters.get(ind));
            }
        });

        filterBox.addStyleName("comboBoxFilter");

        addFilterComponent(filterGroup, filterBox);
        if (filterGroup.defaultFilter >= 0) {
            //todo: check if this is correct
            filterBox.setSelectedIndex(filterGroup.defaultFilter + 1);
        }
    }

    private void setRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        setRemoteRegularFilter(filterGroup, filter);
    }

    private void addFilterComponent(GRegularFilterGroup filterGroup, Widget filterWidget) {
        controllers.get(filterGroup.groupObject).addFilterComponent(filterGroup, filterWidget);
    }

    private void initializeControllers() {
        for (GTreeGroup treeGroup : form.treeGroups) {
            GTreeGroupController treeController = new GTreeGroupController(treeGroup, this, form, formLayout);
            treeControllers.put(treeGroup, treeController);
        }

        for (GGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GGroupObjectController controller = new GGroupObjectController(this, group, formLayout);
                controllers.put(group, controller);
            }
        }

        hasColumnGroupObjects = false;
        for (GPropertyDraw property : form.propertyDraws) {
            if (property.columnGroupObjects != null && !property.columnGroupObjects.isEmpty()) {
                hasColumnGroupObjects = true;
            }
            if (property.groupObject == null) {
                controllers.put(null, new GGroupObjectController(this, null, formLayout));
            }
        }
    }

    private void initializeDefaultOrders() {
        applyOrders(form.defaultOrders);
        defaultOrdersInitialized = true;
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

    public void processRemoteChanges() {
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

        // для уменьшения количества вызовов flush() при скроллинге сначала запоминаем текущее положение скролла во всех таблицах
        rememberScrollPositions();

        // затем одним скопом обновляем данные во всех таблицах
        for (GGroupObjectController controller : controllers.values()) {
            controller.processFormChanges(fc, currentGridObjects, changedGroups);
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(fc);
        }

        formLayout.hideEmptyContainerViews();
        if (!fc.classViews.isEmpty()) {
            formLayout.totalResize();
        }

        // в конце скроллим все таблицы к текущим ключам
        applyScrollPositions();
    }

    private void rememberScrollPositions() {
        for (GGroupObjectController controller : controllers.values()) {
            controller.rememberScrollPosition();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.rememberScrollPosition();
        }
    }

    private void applyScrollPositions() {
        for (GGroupObjectController controller : controllers.values()) {
            controller.preparePendingState();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.preparePendingState();
        }

        for (GGroupObjectController controller : controllers.values()) {
            controller.applyPendingState();
        }
        for (GTreeGroupController treeController : treeControllers.values()) {
            treeController.applyPendingState();
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(GFormChanges fc) {
        long currentDispatchingRequestIndex = dispatcher.getCurrentDispatchingRequestIndex();

        for (Iterator<Map.Entry<Long, ModifyObject>> iterator = lastModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            if (cell.getKey() <= currentDispatchingRequestIndex) {
                iterator.remove();
            }
        }

        for (Map.Entry<Long, ModifyObject> e : lastModifyObjectRequests.entrySet()) {
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

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(GFormChanges fc) {
        long currentDispatchingRequestIndex = dispatcher.getCurrentDispatchingRequestIndex();

        for (Iterator<Map.Entry<GGroupObject, Long>> iterator = lastChangeCurrentObjectsRequestIndices.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<GGroupObject, Long> entry = iterator.next();

            if (entry.getValue() <= currentDispatchingRequestIndex) {
                iterator.remove();
            } else {
                fc.objects.remove(entry.getKey());
            }
        }
    }

    private void modifyFormChangesWithChangePropertyAsyncs(GFormChanges fc) {
        long currentDispatchingRequestIndex = dispatcher.getCurrentDispatchingRequestIndex();

        for (Map.Entry<GPropertyDraw, HashMap<GGroupObjectValue, Long>> entry : lastChangePropertyRequestIndices.entrySet()) {
            GPropertyDraw property = entry.getKey();
            for (Map.Entry<GGroupObjectValue, Long> e : entry.getValue().entrySet()) {
                long requestIndex = e.getValue();
                if (requestIndex <= currentDispatchingRequestIndex) {
                    GGroupObjectValue keys = e.getKey();

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
        }

        for (Map.Entry<GPropertyDraw, HashMap<GGroupObjectValue, Change>> e : lastChangePropertyRequestValues.entrySet()) {
            HashMap<GGroupObjectValue, Object> propertyValues = fc.properties.get(e.getKey());
            if (propertyValues != null) {
                for (Map.Entry<GGroupObjectValue, Change> keyValue : e.getValue().entrySet()) {
                    propertyValues.put(keyValue.getKey(), keyValue.getValue().newValue);
                }
            }
        }
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

    public void showModalDialog(GForm form, final WindowHiddenHandler handler) {
        GResizableModalDialog.showDialog(formsController, form, handler);
    }

    public void showClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        GResizableClassDialog.showDialog(baseClass, defaultClass, concreate, classChosenHandler);
    }

    public void changeGroupObject(final GGroupObject group, GGroupObjectValue key) {
        long requestIndex = dispatcher.execute(new ChangeGroupObject(group.ID, key.getValueDTO()), new ServerResponseCallback() {
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

    public void scrollToEnd(GGroupObject group, boolean toEnd) {
        syncDispatch(new ScrollToEnd(group.ID, toEnd), new ServerResponseCallback());
    }

    public void executeEditAction(GPropertyDraw property, GGroupObjectValue columnKey, String actionSID, AsyncCallback<ServerResponseResult> callback) {
        DeferredRunner.get().commitDelayedGroupObjectChange(property.groupObject);
        syncDispatch(new ExecuteEditAction(property.ID, columnKey == null ? null : columnKey.getValueDTO(), actionSID), callback);
    }

    public void continueServerInvocation(Object[] actionResults, AsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ContinueInvocation(actionResults), callback);
    }

    public void throwInServerInvocation(Exception exception) {
        syncDispatch(new ThrowInInvocation(exception), new ErrorAsyncCallback<ServerResponseResult>());
    }

    public <T extends Result> void syncDispatch(FormBoundAction<T> action, AsyncCallback<T> callback) {
        //todo: возможно понадобится сделать чтото более сложное как в
        //todo: http://stackoverflow.com/questions/2061699/disable-user-interaction-in-a-gwt-container
        LoadingBlocker.getInstance().start();
        dispatcher.execute(action, new WrapperAsyncCallbackEx<T>(callback) {
            @Override
            public void preProcess() {
                LoadingBlocker.getInstance().stop();
            }
        });
    }

    public GGroupObjectValue getFullCurrentKey() {
        GGroupObjectValue fullKey = new GGroupObjectValue();

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
        GGroupObjectValue fullKey = getFullCurrentKey();
        if (columnKey != null) {
            fullKey.putAll(columnKey);
        }
        return fullKey;
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

        long requestIndex = dispatcher.execute(new ChangeProperty(property.ID, getFullCurrentKey(columnKey).getValueDTO(), value, null), new ServerResponseCallback());

        GGroupObjectLogicsSupplier controller = getGroupObjectLogicsSupplier(property.groupObject);

        GGroupObjectValue propertyKey = controller != null && !controller.hasPanelProperty(property)
                                        ? columnKey != null ? new GGroupObjectValue(controller.getCurrentKey(), columnKey) : controller.getCurrentKey()
                                        : columnKey;

        putToDoubleMap(lastChangePropertyRequestIndices, property, propertyKey, requestIndex);
        putToDoubleMap(lastChangePropertyRequestValues, property, propertyKey, new Change(value, oldValue));
    }

    public boolean isAsyncModifyObject(GPropertyDraw property) {
        return property.addRemove != null && controllers.get(property.addRemove.object.groupObject).isInGridClassView();
    }

    public void modifyObject(final GPropertyDraw property, final GGroupObjectValue columnKey) {
        assert isAsyncModifyObject(property);

        final GObject object = property.addRemove.object;
        final boolean add = property.addRemove.add;

        if (add) {
            NavigatorDispatchAsync.Instance.get().execute(new GenerateID(), new ErrorHandlingCallback<GenerateIDResult>() {
                @Override
                public void success(GenerateIDResult result) {
                    executeModifyObject(property, columnKey, object, add, result.ID, new GGroupObjectValue(object, result.ID));
                }
            });
        } else {
            final GGroupObjectValue value = controllers.get(object.groupObject).getCurrentKey();
            final int ID = (Integer) value.values().iterator().next();
            executeModifyObject(property, columnKey, object, add, ID, value);
        }
    }

    private void executeModifyObject(GPropertyDraw property, GGroupObjectValue columnKey, GObject object, boolean add, int ID, GGroupObjectValue value) {
        final GGroupObjectValue fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        controllers.get(object.groupObject).modifyGroupObject(value, add);

        long requestIndex = dispatcher.execute(new ChangeProperty(property.ID, fullCurrentKey.getValueDTO(), null, add ? ID : null), new ServerResponseCallback());
        lastChangeCurrentObjectsRequestIndices.put(object.groupObject, requestIndex);
        lastModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value));
    }

    public void changeClassView(GGroupObject groupObject, GClassViewType newClassView) {
        DeferredRunner.get().commitDelayedGroupObjectChange(groupObject);
        syncDispatch(new ChangeClassView(groupObject.ID, newClassView), new ServerResponseCallback());
    }

    public void changePropertyOrder(GPropertyDraw property, GGroupObjectValue columnKey, GOrder modiType) {
        if (defaultOrdersInitialized) {
            syncDispatch(new ChangePropertyOrder(property.ID, columnKey.getValueDTO(), modiType), new ServerResponseCallback());
        }
    }

    public void expandGroupObject(GGroupObject group, GGroupObjectValue value) {
        syncDispatch(new ExpandGroupObject(group.ID, value.getValueDTO()), new ServerResponseCallback());
    }

    public void collapseGroupObject(GGroupObject group, GGroupObjectValue value) {
        syncDispatch(new CollapseGroupObject(group.ID, value.getValueDTO()), new ServerResponseCallback());
    }

    public void setTabVisible(GContainer tabbedPane, GComponent visibleComponent) {
        syncDispatch(new SetTabVisible(tabbedPane.ID, visibleComponent.ID), new ServerResponseCallback());
        if (formLayout != null && visibleComponent instanceof GContainer) {
            formLayout.adjustContainerSizes((GContainer) visibleComponent);
        }
        redrawHeaders(visibleComponent);
    }

    public void closePressed() {
        syncDispatch(new ClosePressed(), new ServerResponseCallback());
    }

    public void okPressed() {
        syncDispatch(new OkPressed(), new ServerResponseCallback());
    }

    // судя по документации, TabPanel криво работает в StandardsMode. в данном случае неправильно отображает
    // таблицы, кроме тех, что в первой вкладке. поэтому вынуждены сами вызывать onResize() для заголовков таблиц
    private void redrawHeaders(GComponent component) {
        if (controllers.isEmpty() && treeControllers.isEmpty()) {
            return;
        }
        List<GGrid> grids = new ArrayList<GGrid>();
        List<GTreeGroup> treeGrids = new ArrayList<GTreeGroup>();
        if (component instanceof GGrid) {
            grids.add((GGrid) component);
        } else if (component instanceof GTreeGroup) {
            treeGrids.add((GTreeGroup) component);
        } else if (component instanceof GContainer) {
            grids.addAll(((GContainer) component).getAllGrids());
            treeGrids.addAll(((GContainer) component).getAllTreeGrids());
        }
        for (GGrid grid : grids) {
            if (controllers.get(grid.groupObject) != null) {
                controllers.get(grid.groupObject).relayoutTable();
            }
        }
        for (GTreeGroup treeGroup : treeGrids) {
            if (treeControllers.get(treeGroup) != null) {
                treeControllers.get(treeGroup).relayoutTable();
            }
        }
    }

    private void setRemoteRegularFilter(GRegularFilterGroup filterGroup, GRegularFilter filter) {
        syncDispatch(new SetRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID), new ServerResponseCallback());
    }

    public List<GPropertyDraw> getPropertyDraws() {
        return form.propertyDraws;
    }

    public void block() {
        //do nothing by default
    }

    public void unblock() {
        //do nothing by default
    }

    public void hideForm() {
        //do nothing by default
    }

    public void runPrintReport(String reportSID) {
        openReportWindow(reportSID, "pdf");
    }

    public void runOpenInExcel(String reportSID) {
        openReportWindow(reportSID, "xls");
    }

    private void openReportWindow(String reportSID, String type) {
        String reportUrl = GWT.getHostPageBaseURL() + "report?file=" + reportSID + "&type=" + type;
        Window.open(reportUrl, "Report", "");
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

    public int getPreferredWidth() {
        return formLayout.getMainKey().preferredWidth;
    }

    public int getPreferredHeight() {
        return formLayout.getMainKey().preferredHeight;
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
        focusFirstWidget();
    }

    private void focusFirstWidget() {
        for (GGroupObjectController controller : controllers.values()) {
            if (controller.focusFirstWidget()) {
                return;
            }
        }

        for (GTreeGroupController treeController : treeControllers.values()) {
            if (treeController.focusFirstWidget()) {
                return;
            }
        }
    }

    private class ServerResponseCallback extends ErrorHandlingCallback<ServerResponseResult> {
        @Override
        public void success(ServerResponseResult response) {
            actionDispatcher.dispatchResponse(response);

            if (!initialFormChangesReceived) {
                onInitialFormChangesReceived();
                initialFormChangesReceived = true;
            }
        }
    }
}