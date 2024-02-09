package lsfusion.client.form.controller;

import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.file.RawFileData;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.base.lambda.AsyncCallback;
import lsfusion.base.lambda.EProvider;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.TableManager;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.ItemAdapter;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.data.ClientLogicalClass;
import lsfusion.client.controller.MainController;
import lsfusion.client.controller.dispatch.DispatcherListener;
import lsfusion.client.controller.remote.AsyncListener;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.controller.remote.RmiRequest;
import lsfusion.client.form.ClientForm;
import lsfusion.client.form.ClientFormChanges;
import lsfusion.client.form.controller.dispatch.ClientFormActionDispatcher;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;
import lsfusion.client.form.design.ClientContainer;
import lsfusion.client.form.design.view.*;
import lsfusion.client.form.design.view.flex.LinearClientContainerView;
import lsfusion.client.form.design.view.widget.ComboBoxWidget;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.ClientRegularFilterWrapper;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.controller.FilterController;
import lsfusion.client.form.filter.view.SingleFilterBox;
import lsfusion.client.form.object.ClientCustomObjectValue;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.controller.GridController;
import lsfusion.client.form.object.table.grid.user.design.GridUserPreferences;
import lsfusion.client.form.object.table.tree.ClientTreeGroup;
import lsfusion.client.form.object.table.tree.controller.TreeGroupController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.*;
import lsfusion.client.form.property.cell.ClientAsync;
import lsfusion.client.form.property.cell.GetAsyncValuesProvider;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.property.cell.controller.dispatch.SimpleChangePropertyDispatcher;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.FormClientData;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.event.InputEvent;
import lsfusion.interop.form.event.*;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.order.user.Order;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.client.ClientResourceBundle.getString;

public class ClientFormController implements AsyncListener {

    private static IDGenerator idGenerator = new DefaultIDGenerator();

    private final TableManager tableManager = new TableManager();

    private final EProvider<String> serverMessageProvider = new EProvider<String>() {
        @Override
        public String getExceptionally() throws Exception {
            return remoteForm == null ? null : remoteForm.getRemoteActionMessage();
        }
        @Override
        public void interrupt(boolean cancelable) {
            try {
                remoteForm.interrupt(cancelable);
            } catch (Exception ignored) {
            }
        }
    };

    private final EProvider<List<Object>> serverMessageListProvider = new EProvider<List<Object>>() {
        @Override
        public List<Object> getExceptionally() throws Exception {
            return remoteForm == null ? null : remoteForm.getRemoteActionMessageList();
        }
        @Override
        public void interrupt(boolean cancelable) {
            try {
                remoteForm.interrupt(cancelable);
            } catch (Exception ignored) {
            }
        }
    };

    private final GetAsyncValuesProvider getAsyncValuesProvider = new GetAsyncValuesProvider() {
        @Override
        public ClientAsync[] getAsyncValues(int propertyID, byte[] columnKey, String actionSID, String value, int asyncIndex) throws RemoteException {
            return remoteForm == null ? null : ClientFormController.this.getAsyncValues(-1, 0, propertyID, columnKey, actionSID, value, asyncIndex, remoteForm);
        }
    };

    private ClientAsync[] getAsyncValues(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] columnKey, String actionSID, String value, int asyncIndex, RemoteFormInterface remoteForm) throws RemoteException {
        return ClientAsync.deserialize(remoteForm.getAsyncValues(requestIndex, lastReceivedRequestIndex, propertyID, columnKey, actionSID, value, asyncIndex), form);
    }

    private final RmiQueue rmiQueue;
    private final SimpleChangePropertyDispatcher simpleDispatcher;

    private volatile RemoteFormInterface remoteForm;

    public final FormsController formsController;
    public final ClientForm form;
    private final ClientNavigator clientNavigator;
    private final ClientFormActionDispatcher actionDispatcher;

    private final String formSID;
    private final String canonicalName;

    private final int ID;

    private final ClientFormLayout formLayout;

    private final Map<ClientGroupObject, GridController> controllers = new LinkedHashMap<>();
    private final Map<ClientTreeGroup, TreeGroupController> treeControllers = new LinkedHashMap<>();

    private final Map<ClientGroupObject, List<FlexPanel>> filterViews = new HashMap<>();

    private final boolean isDialog;
    private final boolean isWindow;

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<>();

    private final Map<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjects = new HashMap<>();

    private final OrderedMap<Long, ModifyObject> pendingModifyObjectRequests = new OrderedMap<>();
    private final Map<ClientGroupObject, Long> pendingChangeCurrentObjectsRequests = Maps.newHashMap();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange> pendingChangePropertyRequests = HashBasedTable.create();

    private boolean hasColumnGroupObjects;

    private Timer asyncTimer;
    private PanelView asyncView;
    private Icon asyncPrevIcon;

    private boolean blocked = false;

    private boolean selected = true;

    private List<ScheduledExecutorService> formSchedulers;

    private List<ClientComponent> firstTabsToActivate;
    private List<ClientPropertyDraw> firstPropsToActivate;

    private Set<Integer> inputGroupObjects;

    public ClientFormController(RemoteFormInterface iremoteForm, FormsController iformsController, ClientForm iform, FormClientData clientData, ClientNavigator iclientNavigator, boolean iisModal, boolean iisDialog) {
        formSID = clientData.formSID + (iisModal ? "(modal)" : "") + "(" + System.identityHashCode(this) + ")";
        canonicalName = clientData.canonicalName;
        inputGroupObjects = clientData.inputGroupObjects;

        isDialog = iisDialog;
        isWindow = iisModal;

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

        try {
            formsController = iformsController;
            form = iform;

            rmiQueue = new RmiQueue(tableManager, serverMessageProvider, serverMessageListProvider, this, true);

            actionDispatcher = new ClientFormActionDispatcher(rmiQueue) {
                @Override
                public ClientFormController getFormController() {
                    return ClientFormController.this;
                }
            };
            
            rmiQueue.setDispatcher(actionDispatcher);

            simpleDispatcher = new SimpleChangePropertyDispatcher(this);

            formLayout = new ClientFormLayout(this, form.mainContainer);

            updateFormCaption();

            initializeForm(clientData);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static ClientForm deserializeClientForm(RemoteFormInterface remoteForm, FormClientData clientData) throws IOException {
        return new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(clientData.richDesign)));
    }

    public void checkMouseEvent(MouseEvent e, boolean preview, ClientPropertyDraw property, Supplier<ClientGroupObject> groupObjectSupplier, boolean panel) {
        boolean ignore = property != null && property.baseType instanceof ClientLogicalClass && !property.isReadOnly();
        if (!ignore && !e.isConsumed()) {
            boolean doubleChangeEvent = MouseStrokes.isDoubleChangeEvent(e);
            if (MouseStrokes.isChangeEvent(e) || doubleChangeEvent) {
                processBinding(new MouseInputEvent(e, doubleChangeEvent), preview, e, groupObjectSupplier, panel);
            }
        }
    }
    public void checkKeyEvent(KeyStroke ks, KeyEvent e, boolean preview, ClientPropertyDraw property, Supplier<ClientGroupObject> groupObjectSupplier, boolean panel, int condition, boolean pressed) {
        if(condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT && pressed && !e.isConsumed())
            processBinding(new KeyInputEvent(ks), preview, e, groupObjectSupplier, panel);
    }

    public boolean hasCanonicalName() {
        return canonicalName != null;
    }

    public boolean isWindow() {
        return isWindow;
    }

    public boolean isDialog() {
        return isDialog;
    }

    public int getID() {
        return ID;
    }

    public ClientFormLayout getLayout() {
        return formLayout;
    }
    
    public DispatcherListener getDispatcherListener() { 
        return rmiQueue;
    }

    public RemoteFormInterface getRemoteForm() {
        return remoteForm;
    }
    
    public void activateTab(ClientComponent component) {
       if(component.isTab())
            ((TabbedClientContainerView)getLayout().getContainerView(component.container)).activateTab(component);
    }

    private Map<Integer, Integer> getTabMap(TabbedClientContainerView containerView, ClientContainer component) {
        Map<Integer, Integer> tabMap = new HashMap<>();
        List<ClientComponent> tabs = component.getChildren();
        if (tabs != null) {
            int c = 0;
            for (int i = 0; i < tabs.size(); i++) {
                ClientComponent tab = tabs.get(i);
                if (containerView.isTabVisible(tab)) {
                    tabMap.put(tab.getID(), c++);
                }
            }
        }
        return tabMap;
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //
    private void initializeForm(FormClientData clientData) throws Exception {

        initializeParams(); // has to be done before initializeControllers (since adding component uses getSize)

        initializeControllers(clientData);

        initializeDefaultOrders(); // now it doesn't matter, because NavigatorForm will be removed, and first changes will always be not null, but still

        byte[] firstChanges = clientData.firstChanges;
        if(firstChanges != null) {
            applyFormChanges(-1, firstChanges, true);
        } else {
            getRemoteChanges(false);
        }

        //has to be after firstChanges because can eventually invoke remote call with own changes and form changes applies immediately before first changes
        initializeRegularFilters();

        initializeUserOrders();

        initializeFormSchedulers();
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }

    private void initializeParams() {
        hasColumnGroupObjects = false;
        for (ClientPropertyDraw property : getPropertyDraws()) {
            if (property.hasColumnGroupObjects()) {
                hasColumnGroupObjects = true;
            }

            ClientGroupObject groupObject = property.groupObject;
            if(groupObject != null && property.isList && !property.hide && groupObject.columnCount < 10) {
                groupObject.columnSumWidth += property.getValueWidthWithPadding(formLayout);
                groupObject.columnCount++;
                groupObject.rowMaxHeight = Math.max(groupObject.rowMaxHeight, property.getValueHeightWithPadding(formLayout));
            }
        }
    }

    private void initializeControllers(FormClientData clientData) throws IOException {
        FormUserPreferences preferences = clientData.userPreferences;
        
        for (ClientTreeGroup treeGroup : form.treeGroups) {
            initializeTreeController(treeGroup);
        }

        for (ClientGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                initializeGroupController(group, preferences);
            }
        }
        initializeGroupController(null, preferences);
    }

    private void initializeGroupController(ClientGroupObject group, FormUserPreferences preferences) throws IOException {
        GridController controller = new GridController(group, this, formLayout, extractGridUserPreferences(preferences, group));
        controllers.put(group, controller);
    }

    private void initializeTreeController(ClientTreeGroup treeGroup) throws IOException {
        TreeGroupController controller = new TreeGroupController(treeGroup, this, formLayout);
        treeControllers.put(treeGroup, controller);
    }

    private GridUserPreferences[] extractGridUserPreferences(FormUserPreferences formPreferences, ClientGroupObject groupObject) {
        if (groupObject != null && formPreferences != null) {
            GridUserPreferences[] gridPreferences = new GridUserPreferences[2];
            gridPreferences[0] = findGridUserPreferences(formPreferences.getGroupObjectGeneralPreferencesList(), groupObject);
            gridPreferences[1] = findGridUserPreferences(formPreferences.getGroupObjectUserPreferencesList(), groupObject);
            return gridPreferences;
        }
        return null;
    }
    
    private GridUserPreferences findGridUserPreferences(List<GroupObjectUserPreferences> groupObjectUserPreferences, ClientGroupObject groupObject) {
        for (GroupObjectUserPreferences groupPreferences : groupObjectUserPreferences) {
            if (groupObject.getSID().equals(groupPreferences.groupObjectSID)) {
                Map<ClientPropertyDraw, ColumnUserPreferences> columnPreferences = new HashMap<>();
                for (Map.Entry<String, ColumnUserPreferences> entry : groupPreferences.getColumnUserPreferences().entrySet()) {
                    ClientPropertyDraw property = form.getProperty(entry.getKey());
                    if (property != null) {
                        columnPreferences.put(property, entry.getValue());
                    }
                }
                return new GridUserPreferences(groupObject, columnPreferences, groupPreferences.fontInfo, groupPreferences.pageSize, groupPreferences.headerHeight, groupPreferences.hasUserPreferences);
            }
        }
        return null;
    }

    private void initializeRegularFilters() {
        // Проинициализируем регулярные фильтры
        for (final ClientRegularFilterGroup filterGroup : form.regularFilterGroups) {
            if (filterGroup.filters.size() == 1) {
                createSingleFilterComponent(filterGroup, BaseUtils.single(filterGroup.filters));
            } else {
                createMultipleFilterComponent(filterGroup);
            }
        }
    }

    private void initializeFormSchedulers() {
        formSchedulers = new ArrayList<>();
        for(int i = 0; i < form.formSchedulers.size(); i++) {
            FormScheduler formScheduler = form.formSchedulers.get(i);
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            formSchedulers.add(scheduler);
            scheduleFormScheduler(scheduler, formScheduler);
        }
    }

    private void scheduleFormScheduler(ScheduledExecutorService scheduler, FormScheduler formScheduler) {
        if (remoteForm != null) {
            scheduler.schedule(() -> {
                if (formLayout.isShowing()) {
                    SwingUtils.invokeLater(() -> rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("executeFormSchedulerAction") {
                        @Override
                        protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                            if (formScheduler.fixed) {
                                scheduleFormScheduler(scheduler, formScheduler);
                            }
                            return remoteForm.executeEventAction(requestIndex, lastReceivedRequestIndex, formScheduler, null);
                        }

                        @Override
                        protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                            super.onResponse(requestIndex, result);
                            if (!formScheduler.fixed) {
                                scheduleFormScheduler(scheduler, formScheduler);
                            }
                        }
                    }));
                } else {
                    scheduleFormScheduler(scheduler, formScheduler);
                }
            }, formScheduler.period, TimeUnit.SECONDS);
        }
    }

    private void createMultipleFilterComponent(final ClientRegularFilterGroup filterGroup) {
        final ComboBoxWidget comboBox = new ComboBoxWidget();
        comboBox.addItem(new ClientRegularFilterWrapper(getString("form.all")));
        for (final ClientRegularFilter filter : filterGroup.filters) {
            comboBox.addItem(new ClientRegularFilterWrapper(filter));
            if(filter.key != null) {
                addBinding(new KeyInputEvent(filter.key), new Binding(filterGroup.groupObject, 0) {
                    @Override
                    public boolean pressed(java.awt.event.InputEvent ke) {
                        comboBox.setSelectedItem(new ClientRegularFilterWrapper(filter));
                        return true;
                    }
                    @Override
                    public boolean showing() {
                        return true;
                    }
                });
            }
        }

        if (filterGroup.defaultFilterIndex >= 0) {
            ClientRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilterIndex);
            comboBox.setSelectedItem(new ClientRegularFilterWrapper(defaultFilter));
        }
        comboBox.addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(final ItemEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setRegularFilter(filterGroup, ((ClientRegularFilterWrapper) e.getItem()).filter);
                        } catch (IOException ioe) {
                            throw new RuntimeException(getString("form.error.changing.regular.filter"), ioe);
                        }
                    }
                });
            }
        });

        comboBox.setPreferredSize(new Dimension(comboBox.getPreferredSize().width, SwingDefaults.getComponentHeight()));

        addFilterView(filterGroup, comboBox);
    }

    private void createSingleFilterComponent(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter singleFilter) {
        final SingleFilterBox checkBox = new SingleFilterBox(filterGroup, singleFilter) {
            @Override
            public void selected() throws IOException {
                setRegularFilter(filterGroup, singleFilter);
            }

            @Override
            public void deselected() throws IOException {
                setRegularFilter(filterGroup, null);
            }
        };

        addFilterView(filterGroup, checkBox);

        if(singleFilter.key != null) {
            addBinding(new KeyInputEvent(singleFilter.key), new Binding(filterGroup.groupObject, 0) {
                @Override
                public boolean pressed(java.awt.event.InputEvent ke) {
                    checkBox.setSelected(!checkBox.isSelected());
                    return true;
                }
                @Override
                public boolean showing() {
                    return true;
                }
            });
        }
    }

    private void addFilterView(ClientRegularFilterGroup filterGroup, Widget filterView) {
        FlexPanel filterPanel = new FlexPanel(false);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0 , 2));
        filterPanel.add(filterView);
        
        formLayout.addBaseComponent(filterGroup, filterPanel);

        if (filterGroup.groupObject == null) {
            return;
        }

        List<FlexPanel> groupFilters = filterViews.get(filterGroup.groupObject);
        if (groupFilters == null) {
            groupFilters = new ArrayList<>();
            filterViews.put(filterGroup.groupObject, groupFilters);
        }
        groupFilters.add(filterPanel);
    }

    public void setFiltersVisible(ClientGroupObject groupObject, boolean visible) {
        List<FlexPanel> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null) {
            for (FlexPanel filterView : groupFilters) {
                SwingUtils.setGridVisible(filterView, visible);
            }
        }
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, int initialFilterPropertyDrawID) {
        ClientPropertyDraw propertyDraw = form.getProperty(initialFilterPropertyDrawID);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).quickEditFilter(initFilterKeyEvent, propertyDraw, null);
        }
    }

    public void selectProperty(int propertyDrawId) {
        ClientPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).selectProperty(propertyDraw);
        }
    }

    public boolean focusProperty(ClientPropertyDraw propertyDraw) {
        if (controllers.containsKey(propertyDraw.groupObject)) {
            return controllers.get(propertyDraw.groupObject).focusProperty(propertyDraw);
        }
        return false;
    }

    public TableController getGroupObjectLogicsSupplier(ClientGroupObject group) {
        GridController gridController = controllers.get(group);
        if (gridController != null) {
            return gridController;
        }

        return group.parent != null
                ? treeControllers.get(group.parent)
                : null;
    }

    public void saveUserPreferences(final GridUserPreferences gridPreferences, final boolean forAllUsers, final boolean completeOverride, final Runnable successCallback, final Runnable failureCallback, final String[] hiddenProps) {
        commitOrCancelCurrentEditing();

        ServerResponse result = rmiQueue.syncRequest(new RmiCheckNullFormRequest<ServerResponse>("saveUserPreferences") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.saveUserPreferences(requestIndex, lastReceivedRequestIndex, gridPreferences.convertPreferences(), forAllUsers, completeOverride, hiddenProps);
            }
        });

        for (ClientAction action : result.actions) {
            if (action instanceof LogMessageClientAction) {
                actionDispatcher.execute((LogMessageClientAction) action);
                failureCallback.run();
                return;
            }
        }
        successCallback.run();
    }
    
    public void refreshUPHiddenProperties(final String groupObjectSID, final String[] sids) {
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("refreshUPHiddenProperties") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.refreshUPHiddenProperties(requestIndex, lastReceivedRequestIndex, groupObjectSID, sids);   
            }
        });
    }

    public void commitOrCancelCurrentEditing() {
        editAsyncUsePessimistic = false;
        tableManager.commitOrCancelCurrentEditing();
    }

    public boolean commitCurrentEditing() {
        return tableManager.commitCurrentEditing();
    }

    public void setCurrentEditingTable(JTable currentTable) {
        tableManager.setCurrentEditingTable(currentTable);
        if (currentTable == null) {
            rmiQueue.editingStopped();
        }
    }

    public void clearCurrentEditingTable(JTable currentTable) {
        if (tableManager.getCurrentTable() == currentTable) {
            setCurrentEditingTable(null);
        }
    }

    public JTable getCurrentEditingTable() {
        return tableManager.getCurrentTable();
    }

    public boolean isEditing() {
        return tableManager.isEditing();
    }

    public SimpleChangePropertyDispatcher getSimpleChangePropertyDispatcher() {
        return simpleDispatcher;
    }

    private Map<ClientGroupObject, OrderedMap<ClientPropertyDraw, Boolean>> groupDefaultOrders() {
        Map<ClientGroupObject, OrderedMap<ClientPropertyDraw, Boolean>> orders = new HashMap<>();
        for(Map.Entry<ClientPropertyDraw, Boolean> defaultOrder : form.defaultOrders.entrySet()) {
            ClientGroupObject groupObject = defaultOrder.getKey().getGroupObject();
            OrderedMap<ClientPropertyDraw, Boolean> order = orders.get(groupObject);
            if(order == null) {
                order = new OrderedMap<>();
                orders.put(groupObject,order);
            }
            order.put(defaultOrder.getKey(), defaultOrder.getValue());
        }
        return orders;
    }

    public void initializeDefaultOrders() {
        Map<ClientGroupObject, OrderedMap<ClientPropertyDraw, Boolean>> defaultOrders = groupDefaultOrders();
        for (Map.Entry<ClientGroupObject, OrderedMap<ClientPropertyDraw, Boolean>> entry : defaultOrders.entrySet()) {
            ClientGroupObject groupObject = entry.getKey();
            TableController groupObjectLogicsSupplier = getGroupObjectLogicsSupplier(groupObject);
            if (groupObjectLogicsSupplier != null)
                groupObjectLogicsSupplier.changeOrders(groupObject, entry.getValue(), true);
        }
    }

    public void initializeUserOrders() {
        boolean changed = false;
        for (GridController controller : controllers.values()) {
            LinkedHashMap<ClientPropertyDraw, Boolean> objectUserOrders = controller.getUserOrders();
            if (objectUserOrders != null)
                changed = controller.changeOrders(objectUserOrders, false)  || changed;
        }
        if (changed)
            getRemoteChanges(true);
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getDefaultOrders(ClientGroupObject groupObject) {
        return form.getDefaultOrders(groupObject);
    }

    private void processServerResponse(ServerResponse serverResponse, EditPropertyDispatcher editDispatcher) throws IOException {
        //ХАК: serverResponse == null теоретически может быть при реконнекте, когда RMI-поток убивается и remote-method возвращает null
        if (serverResponse != null) {
            (editDispatcher != null ? editDispatcher : actionDispatcher).dispatchServerResponse(serverResponse);
        }
    }

    public void getRemoteChanges(boolean async) {
        getRemoteChanges(async, false);
    }

    public void getRemoteChanges(boolean async, boolean forceLocalEvents) {
        ProcessServerResponseRmiRequest request = new ProcessServerResponseRmiRequest("getRemoteChanges") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.getRemoteChanges(requestIndex, lastReceivedRequestIndex, false, forceLocalEvents);
            }
        };
        if (async) {
            rmiQueue.adaptiveSyncRequest(request);
        } else {
            rmiQueue.syncRequest(request);
        }
    }

    public void applyFormChanges(long requestIndex, byte[] bFormChanges, boolean firstChanges) throws IOException {
        if (bFormChanges == null) {
            return;
        }

        ClientFormChanges formChanges = new ClientFormChanges(bFormChanges, form);

        if(hasColumnGroupObjects) // optimization
            currentGridObjects.putAll(formChanges.gridObjects);

        modifyFormChangesWithModifyObjectAsyncs(requestIndex, formChanges);

        modifyFormChangesWithChangeCurrentObjectAsyncs(requestIndex, formChanges);

        modifyFormChangesWithChangePropertyAsyncs(requestIndex, formChanges);

        for (GridController controller : controllers.values()) {
            controller.processFormChanges(formChanges, currentGridObjects);
        }

        for (TreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(formChanges, currentGridObjects);
        }
        
        expandCollapseContainers(formChanges);
        
        formLayout.autoShowHideContainers();
        
        activateElements(formChanges, firstChanges);

        SwingUtilities.invokeLater(() -> formLayout.revalidate());
    }
    
    private void activateElements(ClientFormChanges formChanges, boolean firstChanges) {
        if (firstChanges) {
            firstTabsToActivate = formChanges.activateTabs;
            firstPropsToActivate = formChanges.activateProps;
        } else {
            activateTabs(formChanges.activateTabs);
            activateProperties(formChanges.activateProps);
        }
    }
    
    private void expandCollapseContainers(ClientFormChanges formChanges) {
        for (ClientContainer container : formChanges.collapseContainers) {
            setContainerExtCollapsed(container, true);
        }
        
        for (ClientContainer container : formChanges.expandContainers) {
            setContainerExtCollapsed(container, false);
        }
    }
    
    private void setContainerExtCollapsed(ClientContainer container, boolean collapsed) {
        if (container.container != null) {
            ClientContainerView parentContainerView = formLayout.getContainerView(container.container);
            if (parentContainerView instanceof LinearClientContainerView) {
                Widget childWidget = ((LinearClientContainerView) parentContainerView).getChildView(container);
                if (childWidget instanceof CollapsiblePanel) {
                    ((CollapsiblePanel) childWidget).setExtCollapsed(collapsed);
                }
            }
        }
    }

    public boolean activateFirstComponents() {
        if (firstTabsToActivate != null) {
            activateTabs(firstTabsToActivate);
            firstTabsToActivate = null;
        }

        boolean focused = false;
        if (firstPropsToActivate != null) {
            focused = activateProperties(firstPropsToActivate);
            firstPropsToActivate = null;
        }
        return focused;
    }

    private void activateTabs(List<ClientComponent> tabs) {
        for (ClientComponent tab : tabs) {
            activateTab(tab);
        }
    }

    private boolean activateProperties(List<ClientPropertyDraw> properties) {
        boolean focused = false;
        for (ClientPropertyDraw prop : properties) {
            focused = focused || focusProperty(prop);
        }
        return focused;
    }

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(long currentDispatchingRequestIndex, ClientFormChanges formChanges) {
        assert currentDispatchingRequestIndex >= 0 || pendingChangeCurrentObjectsRequests.isEmpty();

        for (Iterator<Map.Entry<ClientGroupObject, Long>> iterator = pendingChangeCurrentObjectsRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ClientGroupObject, Long> entry = iterator.next();

            if (entry.getValue() <= currentDispatchingRequestIndex) {
                iterator.remove();
            } else {
                formChanges.objects.remove(entry.getKey());
            }
        }
    }

    private void modifyFormChangesWithChangePropertyAsyncs(long currentDispatchingRequestIndex, ClientFormChanges formChanges) {
        assert currentDispatchingRequestIndex >= 0 || pendingChangePropertyRequests.isEmpty();

        for (Iterator<Table.Cell<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange>> iterator = pendingChangePropertyRequests.cellSet().iterator(); iterator.hasNext(); ) {
            Table.Cell<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange> cell = iterator.next();
            PropertyChange change = cell.getValue();
            if (change.requestIndex <= currentDispatchingRequestIndex) {
                iterator.remove();

                ClientPropertyDraw property = cell.getRowKey();
                ClientGroupObjectValue keys = cell.getColumnKey();

                if(isPropertyShown(property) && !formChanges.dropProperties.contains(property)) {
                    Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(property);
                    if (propertyValues == null) { // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                        propertyValues = new HashMap<>();
                        formChanges.properties.put(property, propertyValues);
                        formChanges.updateProperties.add(property);
                    }

                    if (formChanges.updateProperties.contains(property) && !propertyValues.containsKey(keys)) {
                        propertyValues.put(keys, change.oldValue);
                    }
                }
            }
        }

        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, PropertyChange>> e : pendingChangePropertyRequests.rowMap().entrySet()) {
            Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(e.getKey());
            if (propertyValues != null) {
                for (Map.Entry<ClientGroupObjectValue, PropertyChange> keyValue : e.getValue().entrySet()) {
                    PropertyChange change = keyValue.getValue();
                    if (change.canUseNewValueForRendering) {
                        propertyValues.put(keyValue.getKey(), change.newValue);
                    }
                }
            }
        }
    }

    private boolean isPropertyShown(ClientPropertyDraw property) {
        if(property != null) {
            GridController controller = controllers.get(property.groupObject);
            return controller != null && controller.isPropertyShown(property);
        }
        return false;
    }

    private void modifyFormChangesWithModifyObjectAsyncs(long currentDispatchingRequestIndex, ClientFormChanges formChanges) {
        assert currentDispatchingRequestIndex >= 0 || pendingModifyObjectRequests.isEmpty();

        for (Iterator<Map.Entry<Long,ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            if (cell.getKey() <= currentDispatchingRequestIndex) {
                iterator.remove();

                ModifyObject modifyObject = cell.getValue();
                ClientGroupObject groupObject = modifyObject.object.groupObject;
                // делаем обратный modify, чтобы удалить/добавить ряды, асинхронно добавленные/удалённые на клиенте, если с сервера не пришло подтверждение
                // возможны скачки и путаница в строках на удалении, если до прихода ответа position утратил свою актуальность
                // по этой же причине не заморачиваемся запоминанием соседнего объекта
                if(!formChanges.gridObjects.containsKey(groupObject)) {
                    controllers.get(groupObject).modifyGroupObject(modifyObject.value, !modifyObject.add, modifyObject.position);
                }
            }
        }

        for (Iterator<Map.Entry<Long, ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            ModifyObject modifyObject = cell.getValue();
            List<ClientGroupObjectValue> gridObjects = formChanges.gridObjects.get(modifyObject.object.groupObject);
            if (gridObjects != null) {
                if (modifyObject.add) {
                    gridObjects.add(modifyObject.value);
                } else {
                    if (!gridObjects.remove(modifyObject.value)) { //could be removed in previous formChange (for example, two async groupChanges)
                        iterator.remove();
                    }
                }
            }
        }

    }

    public void expandGroupObjectRecursive(ClientGroupObject group, boolean current) {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("expandGroupObjectRecursive - " + group.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.expandGroupObjectRecursive(requestIndex, lastReceivedRequestIndex, group.getID(), current);
            }
        });
    }

    public void expandGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("expandGroupObject - " + group.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.expandGroupObject(requestIndex, lastReceivedRequestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void collapseGroupObjectRecursive(ClientGroupObject group, boolean current) {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("collapseGroupObjectRecursive - " + group.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.collapseGroupObjectRecursive(requestIndex, lastReceivedRequestIndex, group.getID(), current);
            }
        });
    }

    public void collapseGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) {
        commitOrCancelCurrentEditing();
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("collapseGroupObject") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.collapseGroupObject(requestIndex, lastReceivedRequestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject groupObject, final Scroll changeType) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changeGroupObject.end - " + groupObject.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changeGroupObject(requestIndex, lastReceivedRequestIndex, groupObject.getID(), changeType.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        if (objectValue == null) {
            return;
        }

        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("changeGroupObject") {
            @Override
            protected void onAsyncRequest(long requestIndex) {
//                        System.out.println("!!Async changing group object with req#: " + requestIndex + " on " + objectValue);
                pendingChangeCurrentObjectsRequests.put(group, requestIndex);
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changeGroupObject(requestIndex, lastReceivedRequestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    private byte[] getFullCurrentKey(ClientGroupObjectValue columnKey) {
        List<ClientGroupObjectValue> values = new ArrayList<>();
        for (GridController group : controllers.values())
            values.add(group.getCurrentKey());

        for (TreeGroupController tree : treeControllers.values()) {
            ClientGroupObjectValue currentPath = tree.getCurrentPath();
            if (currentPath != null)
                values.add(currentPath);
        }

        values.add(columnKey);

        return new ClientGroupObjectValue(values.toArray(new ClientGroupObjectValue[0])).serialize();
    }

    public void changeProperty(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, String actionSID,
                               final Object newValue, Integer contextAction, final Object oldValue, EditPropertyDispatcher editDispatcher) throws IOException {
        assert !isEditing();

        commitOrCancelCurrentEditing();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey);

        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("changeProperty", editDispatcher) {
            @Override
            protected void onAsyncRequest(long requestIndex) {
//                System.out.println("!!Async changing property with req#: " + requestIndex);
//                ExceptionUtils.dumpStack();
//                System.out.println("------------------------");

                GridController controller = controllers.get(property.groupObject);

                ClientGroupObjectValue propertyKey;
                if (controller != null && property.isList) {
                    ClientGroupObjectValue currentObject = controller.getCurrentKey();
                    if(currentObject.isEmpty())
                        return;
                    propertyKey = new ClientGroupObjectValue(currentObject, columnKey);
                } else 
                    propertyKey = columnKey;

                pendingChangePropertyRequests.put(property, propertyKey,
                                                    new PropertyChange(requestIndex, newValue, oldValue, property.canUseChangeValueForRendering())
                );
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID, new ClientPushAsyncInput(newValue, contextAction));
            }

            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());
                super.onResponse(requestIndex, result);
            }
        });
    }

    public void asyncAddRemove(ClientPropertyDraw property, EditPropertyDispatcher dispatcher, ClientGroupObjectValue columnKey, String actionSID, ClientAsyncAddRemove addRemove) throws IOException {
        commitOrCancelCurrentEditing();

        final ClientObject object = form.getObject(addRemove.object);
        final boolean add = addRemove.add;

        final GridController controller = controllers.get(object.groupObject);

        final ClientPushAsyncResult pushAsyncResult;
        final ClientGroupObjectValue value;
        if(add) {
            long ID;
            try {
                ID = rmiQueue.runRetryableRequest(() -> MainController.generateID());
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            pushAsyncResult = new ClientPushAsyncAdd(ID);
            value = new ClientGroupObjectValue(object, new ClientCustomObjectValue(ID, null));
        } else {
            value = controller.getCurrentKey();
            if(value.isEmpty())
                return;
            pushAsyncResult = null;
        }
        
        final int position = controller.getCurrentRow();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("modifyObject", dispatcher) {
            @Override
            protected void onAsyncRequest(long requestIndex) {
                controller.modifyGroupObject(value, add, -1); // сначала посылаем запрос, так как getFullCurrentKey может измениться

                pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex); // так как по сути такой execute сам меняет groupObject
                pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey,  actionSID, pushAsyncResult);
            }
        });
    }

    public ClientGroupObjectValue getFullCurrentKey() {
        List<ClientGroupObjectValue> values = new ArrayList<>();
        for (GridController group : controllers.values())
            values.add(group.getCurrentKey());

        for (TreeGroupController tree : treeControllers.values()) {
            ClientGroupObjectValue currentPath = tree.getCurrentPath();
            if (currentPath != null)
                values.add(currentPath);
        }

        return new ClientGroupObjectValue(values.toArray(new ClientGroupObjectValue[0]));
    }

    public void asyncOpenForm(ClientPropertyDraw property, EditPropertyDispatcher dispatcher, ClientGroupObjectValue columnKey, String actionSID, ClientAsyncOpenForm asyncOpenForm) throws IOException {
        commitOrCancelCurrentEditing();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey);

        long requestIndex = rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("openForm", dispatcher) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID);
            }
            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                super.onResponse(requestIndex, result);
                if(formsController != null) {
                    formsController.setLastCompletedRequest(requestIndex);
                }
            }
        });

        ((DockableMainFrame) MainFrame.instance).asyncOpenForm(dispatcher.getAsyncFormController(requestIndex), asyncOpenForm);
    }

    private ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm, ClientPropertyDraw property, byte[] fullCurrentKey, String actionSID) throws RemoteException {
        return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID, null);
    }
    private ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm, ClientPropertyDraw property, byte[] fullCurrentKey, String actionSID, ClientPushAsyncResult asyncResult) throws RemoteException {
        return remoteForm.executeEventAction(requestIndex, lastReceivedRequestIndex, actionSID, new int[]{property.getID()}, new byte[][]{fullCurrentKey}, new boolean[] {false}, new byte[][]{asyncResult != null ? asyncResult.serialize() : null});
    }

    public ServerResponse executeEventAction(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, final String actionSID, ClientPushAsyncResult asyncResult) throws IOException {
        // При выполнение синхронных запросов, EDT блокируется. Если перед этим синхр. запросом был послан асинхронный, который возвращает DockedModal-FormAction,
        // то получается dead-lock: executeEventAction ждёт окончания предыдущего async-запроса и значит закрытия DockedModal формы,
        // а форма не может отработать, т.к. EDT заблокирован. Модальные диалоги отрабатывают нормально, т.к. Swing специально создаёт для них новую очередь событий.
        // Поэтому применяется двойной хак:
        //  если DockedModal-FormAction пришёл после начала синхронного редактирования [canShowDockedModal()], то она показывается в модальном диалоге,
        //  а если сначала была показана форма, а затем на текущей форме сработало редактирование - то мы его отменяем
        if (blocked) {
            return ServerResponse.EMPTY;
        }

        // при показе модального диалога (при создании новой очереди событий), Swing додиспатчивает некоторые события из старой очереди в нижнюю форму,
        // это иногда приводит к случаю, когда додиспатчивается KeyPress, который вызывает повторное редактирование. В этом случае возвращаем пустой ServerResponse.
        if (rmiQueue.isSyncStarted()) {
            return ServerResponse.EMPTY;
        }

        commitOrCancelCurrentEditing();

        SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey);
        
        ServerResponse result =
                rmiQueue.syncRequest(new RmiCheckNullFormRequest<ServerResponse>("executeEventAction - " + property.getLogName()) {
                    @Override
                    protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                        return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID, asyncResult);
                    }

                    @Override
                    protected void onResponseGetFailed(long requestIndex, Exception e) throws Exception {
                        processServerResponse(new ServerResponse(requestIndex, new ClientAction[] {new ExceptionClientAction(e)}, isInServerInvocation(requestIndex)), null);
                    }

                    @Override
                    protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
//                        if(remoteForm != null) // when there is hide in changeProperty and some button is clicked - breaks assertion in dispatchingEnded  
                        rmiQueue.postponeDispatchingEnded();
                    }
                });
        return result == null ? ServerResponse.EMPTY : result;
    }

    public RmiQueue getRmiQueue() {
        return rmiQueue;
    }

    public RemoteRequestInterface getRemoteRequestInterface() {
        return remoteForm;
    }

    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        return rmiQueue.directRequest(requestIndex, new RmiCheckNullFormRequest<Boolean>("isInServerInvocation") {
            @Override
            protected Boolean doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.isInServerInvocation(requestIndex);
            }
        });
    }

    public void gainedFocus() {
        try {
            rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("gainedFocus") {
                @Override
                protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.gainedFocus(requestIndex, lastReceivedRequestIndex);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.form.activation"), e);
        }
    }

    public void setTabActive(final ClientContainer container, final ClientComponent component) {
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("setTabVisible") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setTabActive(requestIndex, lastReceivedRequestIndex, container.getID(), component.getID());
            }
        });
    }

    public void setContainerCollapsed(ClientContainer container, boolean collapsed) {
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("setTabVisible") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setContainerCollapsed(requestIndex, lastReceivedRequestIndex, container.getID(), collapsed);
            }
        });

        formLayout.updatePanels(); // we want to avoid blinking between setting visibility and getting response (and having updatePanels there)
    }

    public void executeNotificationAction(final Integer idNotification) throws IOException {
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("executeNotificationAction") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.executeNotificationAction(requestIndex, lastReceivedRequestIndex, idNotification);
            }
        });
    }

    public void pasteExternalTable(List<ClientPropertyDraw> propertyList, List<ClientGroupObjectValue> columnKeys, final List<List<String>> table) throws IOException {
        int propertyColumns = propertyList.size();

        List<List<byte[]>> values = new ArrayList<>();
        List<ArrayList<String>> rawValues = new ArrayList<>();
        for (List<String> sRow : table) {
            List<byte[]> valueRow = new ArrayList<>();
            ArrayList<String> rawValueRow = new ArrayList<>();

            for (int i = 0; i < propertyColumns; i++) {
                ClientPropertyDraw property = propertyList.get(i);
                String sCell = i < sRow.size() ? sRow.get(i) : null;
                Object oCell = property.parsePaste(sCell);
                valueRow.add(serializeObject(oCell));
                rawValueRow.add(sCell);
            }
            values.add(valueRow);
            rawValues.add(rawValueRow);
        }

        final List<Integer> propertyIdList = new ArrayList<>();
        for (ClientPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.getID());
        }
        final List<byte[]> serializedColumnKeys = new ArrayList<>();
        for (ClientGroupObjectValue key : columnKeys) {
            serializedColumnKeys.add(key.serialize());
        }
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("pasteExternalTable") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.pasteExternalTable(requestIndex, lastReceivedRequestIndex, propertyIdList, serializedColumnKeys, values, rawValues);
            }
        });
    }

    public void pasteMulticellValue(final Map<ClientPropertyDraw, PasteData> paste) throws IOException {
        if (paste.isEmpty()) {
            return;
        }

        final Map<Integer, List<byte[]>> mKeys = new HashMap<>();
        final Map<Integer, byte[]> mValues = new HashMap<>();
        final Map<Integer, String> mRawValues = new HashMap<>();

        for (Map.Entry<ClientPropertyDraw, PasteData> keysEntry : paste.entrySet()) {
            ClientPropertyDraw property = keysEntry.getKey();
            PasteData pasteData = keysEntry.getValue();

            List<byte[]> propMKeys = new ArrayList<>();
            for (int i = 0; i < pasteData.keys.size(); ++i) {
                propMKeys.add(getFullCurrentKey(pasteData.keys.get(i)));
            }

            int propertyID = property.getID();
            mKeys.put(propertyID, propMKeys);
            mValues.put(propertyID, serializeObject(pasteData.value));
            mRawValues.put(propertyID, pasteData.rawValue);
        }

        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("pasteMulticellValue") {
            @Override
            protected void onAsyncRequest(long requestIndex) {
                for (Map.Entry<ClientPropertyDraw, PasteData> e : paste.entrySet()) {
                    ClientPropertyDraw property = e.getKey();
                    PasteData pasteData = e.getValue();
                    Object newValue = pasteData.value;
                    boolean canUseNewValueForRendering = property.canUsePasteValueForRendering();

                    for (int i = 0; i < pasteData.keys.size(); ++i) {
                        ClientGroupObjectValue key = pasteData.keys.get(i);
                        Object oldValue = pasteData.oldValues.get(i);
                        pendingChangePropertyRequests.put(property,
                                                          key,
                                                          new PropertyChange(requestIndex, newValue, oldValue, canUseNewValueForRendering)
                        );
                    }
                }
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.pasteMulticellValue(requestIndex, lastReceivedRequestIndex, mKeys, mValues, mRawValues);
            }
        });
    }

    private boolean editAsyncUsePessimistic; // optimimization
    // shouldn't be zeroed when editing ends, since we assume that there is only one live input on the form
    private int editAsyncIndex;
    private int editLastReceivedAsyncIndex;
    // we don't want to proceed results if "later" request results where proceeded
    private AsyncCallback<ClientAsyncResult> checkLast(int editAsyncIndex, AsyncCallback<ClientAsyncResult> callback) {
        return new AsyncCallback<ClientAsyncResult>() {
            @Override
            public void failure(Throwable t) {
                if(editAsyncIndex >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = editAsyncIndex;
                    callback.failure(t);
                }
            }

            @Override
            public void done(ClientAsyncResult result) {
                if(editAsyncIndex >= editLastReceivedAsyncIndex) {
                    editLastReceivedAsyncIndex = editAsyncIndex;
                    if(!result.moreRequests && editAsyncIndex < editAsyncIndex - 1)
                        result = new ClientAsyncResult(result.asyncs, result.needMoreSymbols, true);
                    callback.done(result);
                }
            }
        };
    }

    // synchronous call (with request indices, etc.)
    private void getPessimisticValues(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String value, String actionSID, AsyncCallback<ClientAsyncResult> callback) {
        rmiQueue.asyncRequest(new RmiCheckNullFormRequest<ClientAsync[]>("getAsyncValues - " + property.getLogName()) {
            @Override
            protected ClientAsync[] doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return ClientFormController.this.getAsyncValues(requestIndex, lastReceivedRequestIndex, property.getID(), getFullCurrentKey(columnKey), actionSID, value, editAsyncIndex, remoteForm);
            }

            @Override
            protected void onResponse(long requestIndex, ClientAsync[] result) throws Exception {
                super.onResponse(requestIndex, result);
                callback.done(convertAsyncResult(result));
            }

            @Override
            protected void onResponseGetFailed(long requestIndex, Exception e) throws Exception {
                super.onResponseGetFailed(requestIndex, e);
                callback.failure(e);
            }
        }, true);
    }

    public static class ClientAsyncResult {
        public final List<ClientAsync> asyncs;
        public final boolean needMoreSymbols;
        public final boolean moreRequests;

        public ClientAsyncResult(List<ClientAsync> asyncs, boolean needMoreSymbols, boolean moreRequests) {
            this.asyncs = asyncs;
            this.needMoreSymbols = needMoreSymbols;
            this.moreRequests = moreRequests;
        }
    }
    public void getAsyncValues(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String value, String actionSID, AsyncCallback<ClientAsyncResult> callback) {
        AsyncCallback<ClientAsyncResult> fCallback = checkLast(editAsyncIndex++, callback);

        new SwingWorker<ClientAsyncResult, Void>() {
            @Override
            protected ClientAsyncResult doInBackground() {
                boolean runPessimistic;

                if (!editAsyncUsePessimistic) {
                    try {
                        ClientAsync[] result = getAsyncValuesProvider.getAsyncValues(property.getID(), getFullCurrentKey(columnKey), actionSID, value, editAsyncIndex);
                        if (result == null) { // optimistic request failed, running pessimistic one, with request indices, etc.
                            editAsyncUsePessimistic = true;
                            runPessimistic = true;
                        } else {
                            return convertAsyncResult(result);
                        }

                    } catch (RemoteException e) {
                        throw Throwables.propagate(e);
                    }
                } else {
                    runPessimistic = true;
                }

                return new ClientAsyncResult(Collections.emptyList(), false, runPessimistic);
            }

            @Override
            protected void done() {
                ClientAsyncResult result;
                try {
                    result = get();
                } catch (Throwable t) {
                    fCallback.failure(t);
                    return;
                }

                fCallback.done(result);
                if(result.moreRequests)
                    getPessimisticValues(property, columnKey, value, actionSID, fCallback);
            }
        }.execute();
    }

    private static ClientAsyncResult convertAsyncResult(ClientAsync[] result) {
        boolean needMoreSymbols = false;
        boolean moreResults = false;
        List<ClientAsync> values = Arrays.asList(result);
        if (values.size() > 0) {
            ClientAsync lastResult = values.get(values.size() - 1);
            if (lastResult.equals(ClientAsync.RECHECK)) {
                values = values.subList(0, values.size() - 1);

                moreResults = true;
            } else if (values.size() == 1 && (lastResult.equals(ClientAsync.CANCELED) || lastResult.equals(ClientAsync.NEEDMORE))) {// ignoring CANCELED results
                needMoreSymbols = lastResult.equals(ClientAsync.NEEDMORE);
                values = needMoreSymbols ? Collections.emptyList() : null;
            }
        }
        return new ClientAsyncResult(values, needMoreSymbols, moreResults);
    }

    public void changePropertyOrder(final ClientPropertyDraw property, final Order modiType, final ClientGroupObjectValue columnKey) {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changePropertyOrder - " + property.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changePropertyOrder(requestIndex, lastReceivedRequestIndex, property.getID(), modiType.serialize(), columnKey.serialize());
            }
        });
    }
    
    public void changePropertyOrders(int goID, LinkedHashMap<Integer, Boolean> orders) {
        ClientGroupObject groupObject = form.getGroupObject(goID);
        if (groupObject != null) {
            LinkedHashMap<ClientPropertyDraw, Boolean> pOrders = new LinkedHashMap<>();
            for (Integer propertyID : orders.keySet()) {
                ClientPropertyDraw propertyDraw = form.getProperty(propertyID);
                if (propertyDraw != null) {
                    pOrders.put(propertyDraw, orders.get(propertyID));
                }
            }

            controllers.get(groupObject).changeOrders(pOrders, false);
        }
    }

    public void setPropertyOrders(final ClientGroupObject groupObject, List<Integer> propertyList, List<byte[]> columnKeyList, List<Boolean> orderList) {
        commitOrCancelCurrentEditing();

        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("setPropertyOrders - " + groupObject.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setPropertyOrders(requestIndex, lastReceivedRequestIndex, groupObject.getID(), propertyList, columnKeyList, orderList);
            }
        });
    }

    public void changeFilter(ClientGroupObject groupObject, List<ClientPropertyFilter> conditions) throws IOException {
        currentFilters.put(groupObject, new ArrayList<>(conditions));
        applyCurrentFilters(Collections.singletonList(groupObject));
    }

    public void changeFilter(ClientTreeGroup treeGroup, List<ClientPropertyFilter> conditions) throws IOException {
        Map<ClientGroupObject, List<ClientPropertyFilter>> filters = BaseUtils.groupList(new BaseUtils.Group<ClientGroupObject, ClientPropertyFilter>() {
            public ClientGroupObject group(ClientPropertyFilter key) {
                return key.groupObject;
            }
        }, new ArrayList<>(conditions));

        for (ClientGroupObject group : treeGroup.groups) {
            List<ClientPropertyFilter> groupFilters = filters.get(group);
            if (groupFilters == null) {
                groupFilters = new ArrayList<>();
            }

            currentFilters.put(group, groupFilters);
        }

        applyCurrentFilters(treeGroup.groups);
    }

    public static byte[] serializeClientFilter(ClientPropertyFilter filter) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        filter.serialize(new DataOutputStream(outStream));
        return outStream.toByteArray();
    }

    private void applyCurrentFilters(Collection<ClientGroupObject> groups) throws IOException {
        Map<Integer, byte[][]> filters = new LinkedHashMap<>(); 
        for (ClientGroupObject group : groups) {
            final List<byte[]> groupFilters = new ArrayList<>();
            List<ClientPropertyFilter> gFilters = currentFilters.get(group);
            for (ClientPropertyFilter filter : gFilters) {
                if (!filter.property.isAction())
                    groupFilters.add(serializeClientFilter(filter));
            }
            filters.put(group.ID, groupFilters.toArray(new byte[filters.size()][]));
        }

        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("applyCurrentFilters") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setUserFilters(requestIndex, lastReceivedRequestIndex, filters);
            }
        });
    }

    public void changePropertyFilters(int goID, List<FilterClientAction.FilterItem> filters) {
        ClientGroupObject groupObject = form.getGroupObject(goID);
        if (groupObject != null) {
            GridController gridController = controllers.get(groupObject);
            List<ClientPropertyFilter> props = new ArrayList<>();
            for (FilterClientAction.FilterItem filter : filters) {
                ClientPropertyDraw propertyDraw = form.getProperty(filter.propertyId);
                if (propertyDraw != null) {
                    Compare compare = null;
                    try {
                        compare = Compare.deserialize(filter.compare);
                    } catch (IOException ignored) {}

                    Object value = filter.value;
                    if (filter.value instanceof String) {
                        try {
                            value = propertyDraw.baseType.parseString((String) filter.value);
                        } catch (ParseException ignored) {
                        }
                    }
                    props.add(FilterController.createNewCondition(gridController, new ClientFilter(propertyDraw), ClientGroupObjectValue.EMPTY, value, filter.negation, compare, filter.junction));
                }
            }
            
            gridController.changeFilters(props);
        }
    }    

    // setRegularFilter is synchronous, that's why busy dialog filter can be set visible, which will lead to another itemStateChanged and setRegularFilter call (with nested sync exception)
    // so we just suppress that call
    private final ThreadLocal<Boolean> threadSettingRegularFilter = new ThreadLocal<>();

    private void setRegularFilter(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter filter) throws IOException {
        commitOrCancelCurrentEditing();

        Boolean settingRegularFilter = threadSettingRegularFilter.get();
        if(settingRegularFilter != null && settingRegularFilter)
            return;
        threadSettingRegularFilter.set(true);
        try {
            rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("setRegularFilter - " + filterGroup.getLogName()) {
                @Override
                protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.setRegularFilter(requestIndex, lastReceivedRequestIndex, filterGroup.getID(), (filter == null) ? -1 : filter.getID());
                }
            });
        } finally {
            threadSettingRegularFilter.set(null);
        }
    }

    public Integer countRecords(final int groupObjectID) throws Exception {
        commitOrCancelCurrentEditing();

        return rmiQueue.syncRequest(new RmiCheckNullFormRequest<Integer>("countRecords") {
            @Override
            protected Integer doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.countRecords(requestIndex, lastReceivedRequestIndex, groupObjectID);
            }
        });
    }

    public Object calculateSum(final int propertyID, final byte[] columnKeys) throws Exception {
        commitOrCancelCurrentEditing();

        return rmiQueue.syncRequest(new RmiCheckNullFormRequest<Object>("calculateSum") {
            @Override
            protected Object doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.calculateSum(requestIndex, lastReceivedRequestIndex, propertyID, columnKeys);
            }
        });
    }

    public void changeMode(final ClientGroupObject groupObject, UpdateMode updateMode) {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changeMode") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changeMode(requestIndex, lastReceivedRequestIndex, groupObject.ID, false, null, null, 0, null, null, false, updateMode, null);
            }
        });
    }

    public List<FormGrouping> readGroupings(final String groupObjectSID) {
        commitOrCancelCurrentEditing();
        
        List<FormGrouping> result = rmiQueue.syncRequest(new RmiCheckNullFormRequest<List<FormGrouping>>("readGroupings") {
            @Override
            protected List<FormGrouping> doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.readGroupings(requestIndex, lastReceivedRequestIndex, groupObjectSID);
            }
        });
        return result == null ? new ArrayList<>() : result;
    }

    public Map<List<Object>, List<Object>> groupData(final Map<Integer, List<byte[]>> groupMap, final Map<Integer, List<byte[]>> sumMap, final Map<Integer,
            List<byte[]>> maxMap, final boolean onlyNotNull) {
        commitOrCancelCurrentEditing();

        return rmiQueue.syncRequest(new RmiCheckNullFormRequest<Map<List<Object>,List<Object>>>("groupData") {
            @Override
            protected Map<List<Object>, List<Object>> doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                byte[] groupedData = remoteForm.groupData(requestIndex, lastReceivedRequestIndex, groupMap, sumMap, maxMap, onlyNotNull);
                
                Map<List<Object>, List<Object>> result = new OrderedMap<>();
                DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(groupedData));
                try {
                    int resultSize = inputStream.readInt();
                    for (int i = 0; i < resultSize; i++) {
                        List<Object> key = new ArrayList<>();
                        int keySize = inputStream.readInt();
                        for (int k = 0; k < keySize; k++) {
                            key.add(BaseUtils.deserializeObject(inputStream));
                        }
                        
                        List<Object> value = new ArrayList<>();
                        int valueSize = inputStream.readInt();
                        for (int v = 0; v < valueSize; v++) {
                            value.add(BaseUtils.deserializeObject(inputStream));
                        }
                        
                        result.put(key, value);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return result;
            }
        });
    }
    
    public void saveGrouping(final FormGrouping grouping) {
        rmiQueue.adaptiveSyncRequest(new RmiVoidRequest("saveGrouping") {
            @Override
            protected void doExecute(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                remoteForm.saveGrouping(requestIndex, lastReceivedRequestIndex, grouping);
            }
        });    
    }

    public void changePageSize(final ClientGroupObject groupObject, final Integer pageSize) throws IOException {
        rmiQueue.adaptiveSyncRequest(new ProcessServerResponseRmiRequest("changePageSize") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changePageSize(requestIndex, lastReceivedRequestIndex, groupObject.getID(), pageSize);
            }
        });
    }

    public void revalidate() {
        formLayout.revalidate();
    }

    public void closed() {
    }

    public FormUserPreferences getUserPreferences() {
        List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<>();
        List<GroupObjectUserPreferences> groupObjectGeneralPreferencesList = new ArrayList<>();
        for (GridController controller : controllers.values()) {
            if (controller.getGroupObject() != null) {
                groupObjectUserPreferencesList.add(controller.getUserGridPreferences());
                groupObjectGeneralPreferencesList.add(controller.getGeneralGridPreferences());
            }
        }
        return new FormUserPreferences(groupObjectGeneralPreferencesList, groupObjectUserPreferencesList);
    }

    private static ExecutorService closeService = Executors.newCachedThreadPool();

    protected void onFormHidden() {
        if(formSchedulers != null) {
            for (ScheduledExecutorService formScheduler : formSchedulers) {
                formScheduler.shutdown();
            }
        }
        RemoteFormInterface closeRemoteForm = remoteForm;
        closeService.submit(() -> {
            try {
                closeRemoteForm.close();
            } catch (RemoteException ignored) {
            }
        });
        remoteForm = null;
    }

    public void updateFormCaption() {
        String caption = form.getCaption();
        setFormCaption(caption, form.getTooltip(caption));
    }

    public void setFormCaption(String caption, String tooltip) {
        throw new UnsupportedOperationException();
    }

    // need this because hideForm can be called twice, which will lead to several continueDispatching (and nullpointer, because currentResponse == null)
    private boolean formHidden;
    public void hideForm() {
        if(!formHidden) {
            onFormHidden();
            formHidden = true;
        }
    }

    public void runEditReport(List<String> customReportPathList) {
        try {
            MainController.editReportPathList(customReportPathList);
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    public void runSingleGroupXlsExport(final GridController groupController) {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new RmiCheckNullFormRequest<Object>("runSingleGroupXlsExport") {
            @Override
            protected Object doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.getGroupReportData(requestIndex, lastReceivedRequestIndex, groupController.getGroupObject().getID(), FormPrintType.XLSX, getUserPreferences());
            }

            @Override
            public void onResponse(long requestIndex, Object reportData) throws Exception {
                if (reportData != null) {
                    if (reportData instanceof RawFileData) {
                        BaseUtils.openFile((RawFileData) reportData, "report", "csv");
                    } else {
                        //assert generationData instanceof ReportGenerationData
                        ReportGenerator.exportAndOpen((ReportGenerationData) reportData, FormPrintType.XLSX, true, MainController.remoteLogics);
                    }
                }
            }
        });
    }

    public void closePressed() {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("closePressed") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.executeEventAction(requestIndex, lastReceivedRequestIndex, new FormEventClose(false), null);
            }
        });
    }

    public void setAsyncView(PanelView asyncView) {
        this.asyncView = asyncView;
    }

    public void onAsyncStarted() {
        if (asyncView != null) {
            asyncTimer = new Timer(MainController.asyncTimeOut, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    asyncPrevIcon = asyncView.getIcon();
                    asyncView.setIcon(ClientImages.get("loading.gif"));
                    asyncTimer = null;
                }
            });
            asyncTimer.setRepeats(false);
            asyncTimer.start();
        }
    }

    public void onAsyncFinished() {
        if (asyncView != null) {
            if (asyncTimer != null) {
                asyncTimer.stop();
            } else {
                asyncView.setIcon(asyncPrevIcon);
            }
        }
    }

    public boolean canShowDockedModal() {
        return !isWindow && !rmiQueue.isSyncStarted();
    }

    public void block(boolean blockView) {
        blocked = true;
        if (blockView) {
            blockView();
        }
    }

    public void unblock(boolean viewBlocked) {
        blocked = false;
        if (viewBlocked) {
            unblockView();
        }
    }

    public void setBlockingForm(ClientFormDockable blockingForm) {
    }
    
    public void blockView() {
    }
    
    public void unblockView() {
    }

    public void setSelected(boolean newSelected) {
        selected = newSelected;
    }

    public void setContainerCaption(ClientContainer clientContainer, String caption) {
        clientContainer.caption = caption;

        // update captions (actually we could've set them directly to the containers, but tabbed pane physically adds / removes that views, so the check if there is a tab is required there)
        ClientFormLayout layout = getLayout();
        if(clientContainer.main)
            updateFormCaption();
        else
            layout.getContainerView(clientContainer.container).updateCaption(clientContainer);
    }

    private abstract class RmiCheckNullFormRequest<T> extends RmiRequest<T> {
        protected RmiCheckNullFormRequest(String name) {
            super(formSID + ":" + name);
        }

        @Override
        protected T doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
            RemoteFormInterface form = remoteForm;
            if (form != null) {
                return doRequest(requestIndex, lastReceivedRequestIndex, form);
            }
            return null;
        }

        protected abstract T doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException;
    }

    private abstract class RmiVoidRequest extends RmiCheckNullFormRequest<Void> {
        protected RmiVoidRequest(String name) {
            super(name);
        }

        @Override
        protected Void doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
            doExecute(requestIndex, lastReceivedRequestIndex, remoteForm);
            return null;
        }

        protected abstract void doExecute(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException;
    }

    private abstract class ProcessServerResponseRmiRequest extends RmiCheckNullFormRequest<ServerResponse> {
        protected ProcessServerResponseRmiRequest(String name) {
            this(name, null);
        }

        private final EditPropertyDispatcher editDispatcher;

        protected ProcessServerResponseRmiRequest(String name, EditPropertyDispatcher editDispatcher) {
            super(name);

            this.editDispatcher = editDispatcher;
        }

        @Override
        protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
            processServerResponse(result, editDispatcher);
        }
    }

    private static class ModifyObject {
        public final ClientObject object;
        public final boolean add;
        public final ClientGroupObjectValue value;
        public final int position;

        private ModifyObject(ClientObject object, boolean add, ClientGroupObjectValue value, int position) {
            this.object = object;
            this.add = add;
            this.value = value;
            this.position = position;
        }
    }

    private static class PropertyChange {
        final long requestIndex;

        final Object newValue;
        final Object oldValue;
        final boolean canUseNewValueForRendering;

        private PropertyChange(long requestIndex, Object newValue, Object oldValue, boolean canUseNewValueForRendering) {
            this.requestIndex = requestIndex;
            this.newValue = canUseNewValueForRendering ? newValue : null;
            this.oldValue = oldValue;
            this.canUseNewValueForRendering = canUseNewValueForRendering;
        }
    }

    public final static class PasteData {
        public final Object value;
        public final String rawValue;

        public final List<ClientGroupObjectValue> keys;
        public final List<Object> oldValues;

        public PasteData(Object value, String rawValue, List<ClientGroupObjectValue> keys, List<Object> oldValues) {
            this.value = value;
            this.rawValue = rawValue;
            this.keys = keys;
            this.oldValues = oldValues;
        }
    }

    private final Map<InputEvent, List<Binding>> bindings = new HashMap<>();
    private final List<Binding> keySetBindings = new ArrayList<>();

    public static abstract class Binding {
        public final ClientGroupObject groupObject;
        public int priority;
        public BindingMode bindPreview;
        public BindingMode bindDialog;
        public BindingMode bindGroup;
        public BindingMode bindEditing;
        public BindingMode bindShowing;
        public BindingMode bindPanel;
        public Function<EventObject, Boolean> isSuitable;

        public Binding(ClientGroupObject groupObject, int priority) {
            this(groupObject, priority, null);
        }

        public Binding(ClientGroupObject groupObject, int priority, Function<EventObject, Boolean> isSuitable) {
            this.groupObject = groupObject;
            this.priority = priority;
            this.isSuitable = isSuitable;
        }

        public abstract boolean pressed(java.awt.event.InputEvent ke);
        public abstract boolean showing();
    }

    public void addBinding(InputEvent ks, Binding binding) {
        List<Binding> groupBindings = bindings.computeIfAbsent(ks, k1 -> new ArrayList<>());
        if(binding.priority == 0)
            binding.priority = groupBindings.size();
        if(binding.bindPreview == null)
            binding.bindPreview = ks.bindingModes != null ? ks.bindingModes.getOrDefault("preview", BindingMode.AUTO) : BindingMode.AUTO;
        if(binding.bindDialog == null)
            binding.bindDialog = ks.bindingModes != null ? ks.bindingModes.getOrDefault("dialog", BindingMode.AUTO) : BindingMode.AUTO;
        if(binding.bindGroup == null)
            binding.bindGroup = ks.bindingModes != null ? ks.bindingModes.getOrDefault("group", BindingMode.AUTO) : BindingMode.AUTO;
        if(binding.bindEditing == null)
            binding.bindEditing = ks.bindingModes != null ? ks.bindingModes.getOrDefault("editing", BindingMode.AUTO) : BindingMode.AUTO;
        if(binding.bindShowing == null)
            binding.bindShowing = ks.bindingModes != null ? ks.bindingModes.getOrDefault("showing", BindingMode.AUTO) : BindingMode.AUTO;
        if(binding.bindPanel == null)
            binding.bindPanel = ks.bindingModes != null ? ks.bindingModes.getOrDefault("panel", BindingMode.AUTO) : BindingMode.AUTO;
        groupBindings.add(binding);
    }

    public void addKeySetBinding(Binding binding) {
        binding.bindPreview = BindingMode.AUTO;
        binding.bindDialog = BindingMode.AUTO;
        binding.bindGroup = BindingMode.AUTO;
        binding.bindEditing = BindingMode.NO;
        binding.bindShowing = BindingMode.AUTO;
        binding.bindPanel = BindingMode.AUTO;
        keySetBindings.add(binding);
    }

    public void addPropertyBindings(ClientPropertyDraw propertyDraw, Supplier<Binding> bindingSupplier) {
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

    public boolean processBinding(InputEvent ks, boolean preview, java.awt.event.InputEvent ke, Supplier<ClientGroupObject> groupObjectSupplier, boolean panel) {
        List<Binding> keyBinding = bindings.getOrDefault(ks, ks instanceof MouseInputEvent ? null : keySetBindings);
        if(keyBinding != null && !keyBinding.isEmpty()) { // optimization
            TreeMap<Integer, Binding> orderedBindings = new TreeMap<>();

            // increasing priority for group object
            ClientGroupObject groupObject = groupObjectSupplier.get();
            for(Binding binding : keyBinding) // descending sorting by priority
                if((binding.isSuitable == null || binding.isSuitable.apply(ke)) && bindPreview(binding, preview) && bindDialog(binding) && bindGroup(groupObject, binding)
                        && bindEditing(binding, ke) && bindShowing(binding) && bindPanel(binding, panel))
                        orderedBindings.put(-(binding.priority + (equalGroup(groupObject, binding) ? 100 : 0)), binding);

            if(!orderedBindings.isEmpty())
                commitOrCancelCurrentEditing();

            for(Binding binding : orderedBindings.values()) {
                if (binding.pressed(ke)) {
                    ke.consume();

                    return true;
                }
            }
        }
        return false;
    }

    private boolean bindPreview(Binding binding, boolean preview) {
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

    private boolean bindDialog(Binding binding) {
        switch (binding.bindDialog) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return isDialog();
            case NO:
                return !isDialog();
        }
        return true;
    }

    private boolean bindGroup(ClientGroupObject groupObject, Binding binding) {
        switch (binding.bindGroup) {
            case AUTO:
            case ALL:
                return true;
            case ONLY:
                return equalGroup(groupObject, binding);
            case NO:
                return !equalGroup(groupObject, binding);
            case INPUT:
                Set<ClientGroupObject> inputGroupObjects = getInputGroupObjects();
                return groupObject != null && inputGroupObjects.contains(groupObject);
        }
        return true;
    }

    private Set<ClientGroupObject> getInputGroupObjects() {
        Set<ClientGroupObject> inputGroupObjects = new HashSet<>();
        for(Integer inputGroupObject : this.inputGroupObjects)
            inputGroupObjects.add(form.getGroupObject(inputGroupObject));
        return inputGroupObjects;
    }

    private boolean equalGroup(ClientGroupObject groupObject, Binding binding) {
        return Objects.equals(groupObject, binding.groupObject);
    }

    private boolean bindEditing(Binding binding, java.awt.event.InputEvent ke) {
        switch (binding.bindEditing) {
            case AUTO:
                return !isEditing() || !targetElementIsEditing(ke) || ke instanceof MouseEvent || (ke instanceof KeyEvent && notTextCharEvent((KeyEvent) ke));
            case ALL:
                return true;
            case ONLY:
                return isEditing();
            case NO:
                return !isEditing();
        }
        return true;
    }

    private boolean targetElementIsEditing(java.awt.event.InputEvent event) {
        return getCurrentEditingTable().getEditorComponent().equals(event.getSource());
    }

    private static List<Character> textChars = Arrays.asList(new Character[]{KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_ENTER,
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT});
    private boolean notTextCharEvent(KeyEvent event) {
        char c = (char) event.getKeyCode();
        return event.isControlDown() || (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && !textChars.contains(c));
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
        }
        return true;
    }

    private boolean bindPanel(Binding binding, boolean panel) {
        switch (binding.bindPanel) {
            case ALL:
            case AUTO:
                return true;
            case ONLY:
                return panel;
            case NO:
                return !panel;
        }
        return true;
    }

    public boolean focusFirstComponent() {
        for (TreeGroupController treeController : treeControllers.values()) {
            if (treeController.focusFirstComponent()) {
                return true;
            }
        }

        for (GridController controller : controllers.values()) {
            if (controller.focusFirstComponent()) {
                return true;
            }
        }

        for (TreeGroupController treeController : treeControllers.values()) {
            if (treeController.getPanelController().focusFirstComponent()) {
                return true;
            }
        }

        for (GridController controller : controllers.values()) {
            if (controller.getPanelController().focusFirstComponent()) {
                return true;
            }
        }
        return false;
    }
}
