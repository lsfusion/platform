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
import lsfusion.base.lambda.EProvider;
import lsfusion.base.lambda.ERunnable;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.TableManager;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.ItemAdapter;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.ClientActionClass;
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
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.design.view.JComponentPanel;
import lsfusion.client.form.design.view.TabbedClientContainerView;
import lsfusion.client.form.filter.ClientRegularFilter;
import lsfusion.client.form.filter.ClientRegularFilterGroup;
import lsfusion.client.form.filter.ClientRegularFilterWrapper;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.view.SingleFilterBox;
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
import lsfusion.client.form.property.cell.controller.dispatch.SimpleChangePropertyDispatcher;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.client.form.view.ClientFormDockable;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.view.DockableMainFrame;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.action.*;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.InputEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.order.user.Order;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.print.ReportGenerator;
import lsfusion.interop.form.remote.RemoteFormInterface;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
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

    private final Map<ClientGroupObject, List<JComponentPanel>> filterViews = new HashMap<>();

    private final boolean isDialog;
    private final boolean isModal;

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<>();

    private final Map<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjects = new HashMap<>();

    private final OrderedMap<Long, ModifyObject> pendingModifyObjectRequests = new OrderedMap<>();
    private final Map<ClientGroupObject, Long> pendingChangeCurrentObjectsRequests = Maps.newHashMap();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange> pendingChangePropertyRequests = HashBasedTable.create();

    private Timer asyncTimer;
    private PanelView asyncView;
    private Icon asyncPrevIcon;

    private boolean blocked = false;

    private boolean selected = true;

    private ScheduledExecutorService autoRefreshScheduler;

    private List<ClientComponent> firstTabsToActivate;
    private List<ClientPropertyDraw> firstPropsToActivate;

    public ClientFormController(String icanonicalName, String iformSID, RemoteFormInterface iremoteForm, FormsController iformsController, ClientForm iform, byte[] firstChanges, ClientNavigator iclientNavigator, boolean iisModal, boolean iisDialog) {
        formSID = iformSID + (iisModal ? "(modal)" : "") + "(" + System.identityHashCode(this) + ")";
        canonicalName = icanonicalName;
        isDialog = iisDialog;
        isModal = iisModal;

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

        try {
            formsController = iformsController;
            form = iform;

            rmiQueue = new RmiQueue(tableManager, serverMessageProvider, serverMessageListProvider, this);

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

            initializeForm(firstChanges);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static ClientForm deserializeClientForm(RemoteFormInterface remoteForm) {
        try {
            return new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean hasCanonicalName() {
        return canonicalName != null;
    }

    public boolean isModal() {
        return isModal;
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
    private void initializeForm(byte[] firstChanges) throws Exception {
        initializeControllers();

        initializeDefaultOrders(); // now it doesn't matter, because NavigatorForm will be removed, and first changes will always be not null, but still

        if(firstChanges != null) {
            applyFormChanges(-1, firstChanges, true);
        } else {
            getRemoteChanges(false);
        }

        //has to be after firstChanges because can eventually invoke remote call with own changes and form changes applies immediately before first changes
        initializeRegularFilters();

        initializeUserOrders();

        initializeAutoRefresh();
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }
    
    private void initializeControllers() throws IOException {
        FormUserPreferences preferences = remoteForm.getUserPreferences();
        
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

    private void initializeAutoRefresh() {
        if (form.autoRefresh > 0) {
            autoRefreshScheduler = Executors.newScheduledThreadPool(1);
            scheduleRefresh();
        }
    }

    private void scheduleRefresh() {
        if (remoteForm != null) {
            autoRefreshScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    if (formLayout.isShowing()) {
                        SwingUtils.invokeLater(new ERunnable() {
                            @Override
                            public void run() throws Exception {
                                rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("autoRefresh.getRemoteChanges") {
                                    @Override
                                    protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                                        return remoteForm.getRemoteChanges(requestIndex, lastReceivedRequestIndex, true, false);
                                    }

                                    @Override
                                    protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                                        super.onResponse(requestIndex, result);
                                        scheduleRefresh();
                                    }
                                });
                            }
                        });
                    } else {
                        scheduleRefresh();
                    }
                }
            }, form.autoRefresh, TimeUnit.SECONDS);
        }
    }

    private void createMultipleFilterComponent(final ClientRegularFilterGroup filterGroup) {
        final JComboBox comboBox = new JComboBox();
        comboBox.addItem(new ClientRegularFilterWrapper(getString("form.all")));
        for (final ClientRegularFilter filter : filterGroup.filters) {
            comboBox.addItem(new ClientRegularFilterWrapper(filter));
            if(filter.key != null) {
                addBinding(new KeyInputEvent(filter.key), new Binding(filterGroup.groupObject, 0) {
                    @Override
                    public boolean pressed(KeyEvent ke) {
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
                public boolean pressed(KeyEvent ke) {
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

    private void addFilterView(ClientRegularFilterGroup filterGroup, JComponent filterView) {
        JComponentPanel filterPanel = new JComponentPanel();
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0 , 2));
        filterPanel.add(filterView, BorderLayout.CENTER);
        
        formLayout.add(filterGroup, filterPanel);

        if (filterGroup.groupObject == null) {
            return;
        }

        List<JComponentPanel> groupFilters = filterViews.get(filterGroup.groupObject);
        if (groupFilters == null) {
            groupFilters = new ArrayList<>();
            filterViews.put(filterGroup.groupObject, groupFilters);
        }
        groupFilters.add(filterPanel);
    }

    public void setFiltersVisible(ClientGroupObject groupObject, boolean visible) {
        List<JComponentPanel> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null) {
            for (JComponent filterView : groupFilters) {
                filterView.setVisible(visible);
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
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("refreshUPHiddenProperties") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.refreshUPHiddenProperties(requestIndex, lastReceivedRequestIndex, groupObjectSID, sids);   
            }
        });
    }

    public void commitOrCancelCurrentEditing() {
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

    private void processServerResponse(ServerResponse serverResponse) throws IOException {
        //ХАК: serverResponse == null теоретически может быть при реконнекте, когда RMI-поток убивается и remote-method возвращает null
        if (serverResponse != null) {
            actionDispatcher.dispatchResponse(serverResponse);
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
            rmiQueue.asyncRequest(request);
        } else {
            rmiQueue.syncRequest(request);
        }
    }

    public void applyFormChanges(long requestIndex, byte[] bFormChanges, boolean firstChanges) throws IOException {
        if (bFormChanges == null) {
            return;
        }

        ClientFormChanges formChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(bFormChanges)), form);

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
        
        formLayout.preValidateMainContainer();
        
        activateElements(formChanges, firstChanges);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                formLayout.revalidate();
            }
        });
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

        for (Map.Entry<Long, ModifyObject> e : pendingModifyObjectRequests.entrySet()) {
            List<ClientGroupObjectValue> gridObjects = formChanges.gridObjects.get(e.getValue().object.groupObject);
            if(gridObjects!=null) {
                if(e.getValue().add)
                    gridObjects.add(e.getValue().value);
                else
                    gridObjects.remove(e.getValue().value);
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
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("collapseGroupObject") {
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

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("changeGroupObject") {
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

    private byte[] getFullCurrentKey(ClientGroupObjectValue columnKey) throws IOException {
        final ClientGroupObjectValue fullCurrentKey = getFullCurrentKey();
        fullCurrentKey.putAll(columnKey);

        return fullCurrentKey.serialize();
    }

    public void changeProperty(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, String actionSID,
                               final Object newValue, final Object oldValue) throws IOException {
        assert !isEditing();

        commitOrCancelCurrentEditing();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey);

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("changeProperty") {
            @Override
            protected void onAsyncRequest(long requestIndex) {
//                System.out.println("!!Async changing property with req#: " + requestIndex);
//                ExceptionUtils.dumpStack();
//                System.out.println("------------------------");

                GridController controller = controllers.get(property.groupObject);

                ClientGroupObjectValue propertyKey;
                if (controller != null && property.isList) {
                    ClientGroupObjectValue currentObject = controller.getCurrentObject();
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
                return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID, new ClientPushAsyncChange(newValue));
            }

            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());
                super.onResponse(requestIndex, result);
            }
        });
    }

    public void asyncAddRemove(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID, ClientAsyncAddRemove addRemove) throws IOException {
        commitOrCancelCurrentEditing();

        final ClientObject object = form.getObject(addRemove.object);
        final boolean add = addRemove.add;

        final GridController controller = controllers.get(object.groupObject);

        final long ID;
        final ClientGroupObjectValue value;
        if(add) {
            try {
                ID = rmiQueue.runRetryableRequest(new Callable<Long>() {
                    public Long call() throws Exception {
                        return MainController.generateID();
                    }
                });
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            value = new ClientGroupObjectValue(object, ID);
        } else {
            value = controller.getCurrentObject();
            if(value.isEmpty())
                return;
            ID = (Long) BaseUtils.singleValue(value);
        }
        
        final int position = controller.getCurrentRow();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("modifyObject") {
            @Override
            protected void onAsyncRequest(long requestIndex) {
                controller.modifyGroupObject(value, add, -1); // сначала посылаем запрос, так как getFullCurrentKey может измениться

                pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex); // так как по сути такой execute сам меняет groupObject
                pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value, position));
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey,  actionSID, add ? new ClientPushAsyncAdd(ID) : null);
            }
        });
    }

    public ClientGroupObjectValue getFullCurrentKey() {
        ClientGroupObjectValue fullKey = new ClientGroupObjectValue();

        for (GridController group : controllers.values()) {
            fullKey.putAll(group.getCurrentObject());
        }

        for (TreeGroupController tree : treeControllers.values()) {
            ClientGroupObjectValue currentPath = tree.getCurrentPath();
            if (currentPath != null) {
                fullKey.putAll(currentPath);
            }
        }

        return fullKey;
    }

    public void asyncOpenForm(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID, ClientAsyncOpenForm asyncOpenForm) throws IOException {
        if(!asyncOpenForm.isModal()) { //ignore async modal windows in desktop
            ((DockableMainFrame) MainFrame.instance).asyncOpenForm(rmiQueue.getNextRmiRequestIndex(), asyncOpenForm);
        }

        commitOrCancelCurrentEditing();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey);

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("openForm") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID);
            }
            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                super.onResponse(requestIndex, result);
                //FormClientAction closes asyncForm, if there is no GFormAction in response,
                //we should close this erroneous asyncForm
                DockableRepository forms = ((DockableMainFrame) MainFrame.instance).getForms();
                if (forms.hasAsyncForm(requestIndex)) {
                    if (Arrays.stream(result.actions).noneMatch(a -> a instanceof FormClientAction)) {
                        ClientFormDockable formContainer = forms.removeAsyncForm(requestIndex);
                        formContainer.onClosing();
                    }
                }
                if(formsController != null) {
                    formsController.setLastCompletedRequest(requestIndex);
                }
            }
        });
    }

    private ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm, ClientPropertyDraw property, byte[] fullCurrentKey, String actionSID) throws RemoteException {
        return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID, null);
    }
    private ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm, ClientPropertyDraw property, byte[] fullCurrentKey, String actionSID, ClientPushAsyncResult asyncResult) throws RemoteException {
        return remoteForm.executeEventAction(requestIndex, lastReceivedRequestIndex, actionSID, new int[]{property.getID()}, new byte[][]{fullCurrentKey}, new byte[][]{asyncResult != null ? asyncResult.serialize() : null});
    }

    public ServerResponse executeEventAction(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, final String actionSID) throws IOException {
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
                        return executeEventAction(requestIndex, lastReceivedRequestIndex, remoteForm, property, fullCurrentKey, actionSID);
                    }

                    @Override
                    protected void onResponseGetFailed(long requestIndex, Exception e) throws Exception {
                        processServerResponse(new ServerResponse(requestIndex, new ClientAction[] {new ExceptionClientAction(e)}, isInServerInvocation(requestIndex)));
                    }

                    @Override
                    protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
//                        if(remoteForm != null) // when there is hide in changeProperty and some button is clicked - breaks assertion in dispatchingEnded  
                        rmiQueue.postponeDispatchingEnded();

                        //FormClientAction closes asyncForm, if there is no GFormAction in response,
                        //we should close this erroneous asyncForm
                        DockableRepository forms = ((DockableMainFrame) MainFrame.instance).getForms();
                        if (forms.hasAsyncForm(requestIndex)) {
                            if (Arrays.stream(result.actions).noneMatch(a -> a instanceof FormClientAction)) {
                                ClientFormDockable formContainer = forms.removeAsyncForm(requestIndex);
                                formContainer.onClosing();
                            }
                        }
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
            rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("gainedFocus") {
                @Override
                protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.gainedFocus(requestIndex, lastReceivedRequestIndex);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.form.activation"), e);
        }
    }

    public void setTabVisible(final ClientContainer container, final ClientComponent component) throws IOException {
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("setTabVisible") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setTabVisible(requestIndex, lastReceivedRequestIndex, container.getID(), component.getID());
            }
        });
    }

    public void executeNotificationAction(final Integer idNotification) throws IOException {
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("executeNotificationAction") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.executeNotificationAction(requestIndex, lastReceivedRequestIndex, idNotification);
            }
        });
    }

    public void pasteExternalTable(List<ClientPropertyDraw> propertyList, List<ClientGroupObjectValue> columnKeys, final List<List<String>> table) throws IOException {
        int propertyColumns = propertyList.size();

        final List<List<byte[]>> values = new ArrayList<>();
        for (List<String> sRow : table) {
            List<byte[]> valueRow = new ArrayList<>();

            for (int i = 0; i < propertyColumns; i++) {
                ClientPropertyDraw property = propertyList.get(i);
                String sCell = i < sRow.size() ? sRow.get(i) : null;
                Object oCell = sCell == null ? null : property.parseChangeValueOrNull(sCell);
                valueRow.add(serializeObject(oCell));
            }
            values.add(valueRow);
        }

        final List<Integer> propertyIdList = new ArrayList<>();
        for (ClientPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.getID());
        }
        final List<byte[]> serializedColumnKeys = new ArrayList<>();
        for (ClientGroupObjectValue key : columnKeys) {
            serializedColumnKeys.add(key.serialize());
        }
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("pasteExternalTable") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.pasteExternalTable(requestIndex, lastReceivedRequestIndex, propertyIdList, serializedColumnKeys, values);
            }
        });
    }

    public void pasteMulticellValue(final Map<ClientPropertyDraw, PasteData> paste) throws IOException {
        if (paste.isEmpty()) {
            return;
        }

        final Map<Integer, List<byte[]>> mKeys = new HashMap<>();
        final Map<Integer, byte[]> mValues = new HashMap<>();

        for (Map.Entry<ClientPropertyDraw, PasteData> keysEntry : paste.entrySet()) {
            ClientPropertyDraw property = keysEntry.getKey();
            PasteData pasteData = keysEntry.getValue();

            List<byte[]> propMKeys = new ArrayList<>();
            for (int i = 0; i < pasteData.keys.size(); ++i) {
                propMKeys.add(getFullCurrentKey(pasteData.keys.get(i)));
            }

            mKeys.put(property.getID(), propMKeys);
            mValues.put(property.getID(), serializeObject(pasteData.newValue));
        }

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("pasteMulticellValue") {
            @Override
            protected void onAsyncRequest(long requestIndex) {
                for (Map.Entry<ClientPropertyDraw, PasteData> e : paste.entrySet()) {
                    ClientPropertyDraw property = e.getKey();
                    PasteData pasteData = e.getValue();
                    Object newValue = pasteData.newValue;
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
                return remoteForm.pasteMulticellValue(requestIndex, lastReceivedRequestIndex, mKeys, mValues);
            }
        });
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

    public void setPropertyOrders(final ClientGroupObject groupObject, List<Integer> propertyList, List<byte[]> columnKeyList, List<Boolean> orderList) {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("setPropertyOrders - " + groupObject.getLogName()) {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setPropertyOrders(requestIndex, lastReceivedRequestIndex, groupObject.getID(), propertyList, columnKeyList, orderList);
            }
        });
    }

    public void changeFilter(ClientGroupObject groupObject, List<ClientPropertyFilter> conditions) throws IOException {
        currentFilters.put(groupObject, new ArrayList<>(conditions));
        applyCurrentFilters();
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

        applyCurrentFilters();
    }

    public static byte[] serializeClientFilter(ClientPropertyFilter filter) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        filter.serialize(new DataOutputStream(outStream));
        return outStream.toByteArray();
    }

    private void applyCurrentFilters() throws IOException {
        commitOrCancelCurrentEditing();

        final List<byte[]> filters = new ArrayList<>();

        for (List<ClientPropertyFilter> groupFilters : currentFilters.values()) {
            for (ClientPropertyFilter filter : groupFilters) {
                if (!(filter.property.baseType instanceof ClientActionClass))
                    filters.add(serializeClientFilter(filter));
            }
        }

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("applyCurrentFilters") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setUserFilters(requestIndex, lastReceivedRequestIndex, filters.toArray(new byte[filters.size()][]));
            }
        });
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
        rmiQueue.asyncRequest(new RmiVoidRequest("saveGrouping") {
            @Override
            protected void doExecute(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                remoteForm.saveGrouping(requestIndex, lastReceivedRequestIndex, grouping);
            }
        });    
    }

    public void changePageSize(final ClientGroupObject groupObject, final Integer pageSize) throws IOException {
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("changePageSize") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changePageSize(requestIndex, lastReceivedRequestIndex, groupObject.getID(), pageSize);
            }
        });
    }

    public void moveGroupObject(final ClientGroupObject parentGroup, final ClientGroupObjectValue parentKey, final ClientGroupObject childGroup, final ClientGroupObjectValue childKey, final int index) throws IOException {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("moveGroupObject") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.moveGroupObject(requestIndex, lastReceivedRequestIndex, parentGroup.getID(), parentKey.serialize(), childGroup.getID(), childKey.serialize(), index);
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
        if (autoRefreshScheduler != null) {
            autoRefreshScheduler.shutdown();
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

    public void runEditReport(List<ReportPath> customReportPathList) {
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
                return remoteForm.closedPressed(requestIndex, lastReceivedRequestIndex);
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
        return !isModal && !rmiQueue.isSyncStarted();
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
        if(clientContainer.isTab())
            ((TabbedClientContainerView)layout.getContainerView(clientContainer.container)).updateTabCaption(clientContainer);
        else if(clientContainer.main)
            updateFormCaption();
        else
            layout.getContainerView(clientContainer).updateCaption();
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
            super(name);
        }

        @Override
        protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
            processServerResponse(result);
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
        public final Object newValue;

        public final List<ClientGroupObjectValue> keys;
        public final List<Object> oldValues;

        public PasteData(Object newValue, List<ClientGroupObjectValue> keys, List<Object> oldValues) {
            this.newValue = newValue;
            this.keys = keys;
            this.oldValues = oldValues;
        }
    }

    private final Map<InputEvent, List<Binding>> bindings = new HashMap<>();
    private final List<Binding> keySetBindings = new ArrayList<>();

    public static abstract class Binding {
        public final ClientGroupObject groupObject;
        public int priority;
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

        public abstract boolean pressed(KeyEvent ke);
        public abstract boolean showing();
    }

    public void addBinding(InputEvent ks, Binding binding) {
        List<Binding> groupBindings = bindings.computeIfAbsent(ks, k1 -> new ArrayList<>());
        if(binding.priority == 0)
            binding.priority = groupBindings.size();
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

    public boolean processBinding(InputEvent ks, KeyEvent ke, Supplier<ClientGroupObject> groupObjectSupplier, boolean panel) {
        List<Binding> keyBinding = bindings.getOrDefault(ks, keySetBindings);
        if(keyBinding != null && !keyBinding.isEmpty()) { // optimization
            if(ks instanceof MouseInputEvent) // not sure that it should be done only for mouse events, but it's been working like this for a long time
                commitOrCancelCurrentEditing();

            TreeMap<Integer, Binding> orderedBindings = new TreeMap<>();

            // increasing priority for group object
            ClientGroupObject groupObject = groupObjectSupplier.get();
            for(Binding binding : keyBinding) // descending sorting by priority
                if((binding.isSuitable == null || binding.isSuitable.apply(ke)) && bindDialog(binding) && bindGroup(groupObject, binding)
                        && bindEditing(binding, ke) && bindShowing(binding) && bindPanel(binding, panel))
                        orderedBindings.put(-(binding.priority + (equalGroup(groupObject, binding) ? 100 : 0)), binding);

            for(Binding binding : orderedBindings.values())
                if(binding.pressed(ke))
                    return true;
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
        try {
            Set<ClientGroupObject> inputGroupObjects = new HashSet<>();
            for(Integer inputGroupObject : remoteForm.getInputGroupObjects()) {
                inputGroupObjects.add(form.getGroupObject(inputGroupObject));
            }
            return inputGroupObjects;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean equalGroup(ClientGroupObject groupObject, Binding binding) {
        return Objects.equals(groupObject, binding.groupObject);
    }

    private boolean bindEditing(Binding binding, KeyEvent ke) {
        switch (binding.bindEditing) {
            case AUTO:
                return ke == null || (!isEditing() || notTextCharEvent(ke));
            case ALL:
                return true;
            case ONLY:
                return isEditing();
            case NO:
                return !isEditing();
        }
        return true;
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
