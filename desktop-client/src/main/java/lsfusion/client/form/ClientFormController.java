package lsfusion.client.form;

import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import lsfusion.base.BaseUtils;
import lsfusion.base.EProvider;
import lsfusion.base.ERunnable;
import lsfusion.base.OrderedMap;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.client.EditReportInvoker;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.dock.ClientFormDockable;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.form.dispatch.ClientFormActionDispatcher;
import lsfusion.client.form.dispatch.SimpleChangePropertyDispatcher;
import lsfusion.client.form.grid.GridUserPreferences;
import lsfusion.client.form.layout.ClientContainerView;
import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.form.layout.TabbedClientContainerView;
import lsfusion.client.form.tree.TreeGroupController;
import lsfusion.client.logics.*;
import lsfusion.client.logics.classes.ClientActionClass;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.filter.ClientPropertyFilter;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FormGrouping;
import lsfusion.interop.Order;
import lsfusion.interop.Scroll;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.form.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.interop.Order.*;

public class ClientFormController implements AsyncListener {

    private static final ImageIcon loadingIcon = new ImageIcon(Main.class.getResource("/images/loading.gif"));

    private static IDGenerator idGenerator = new DefaultIDGenerator();

    private final TableManager tableManager = new TableManager(this);

    private final EProvider<String> serverMessageProvider = new EProvider<String>() {
        @Override
        public String getExceptionally() throws Exception {
            return remoteForm == null ? null : remoteForm.getRemoteActionMessage();
        }
        @Override
        public void interrupt(boolean cancelable) {
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
    private final SimpleChangePropertyDispatcher simpleDispatcher = new SimpleChangePropertyDispatcher(this);

    private volatile RemoteFormInterface remoteForm;

    private final ClientForm form;
    private final ClientNavigator clientNavigator;
    private final ClientFormActionDispatcher actionDispatcher;

    private final String formSID;
    private final String canonicalName;

    private ColorPreferences colorPreferences;

    private final int ID;

    private final ClientFormLayout formLayout;

    private final Map<ClientGroupObject, GroupObjectController> controllers = new HashMap<>();
    private final Map<ClientTreeGroup, TreeGroupController> treeControllers = new HashMap<>();

    private final Map<ClientGroupObject, List<JComponent>> filterViews = new HashMap<>();

    private boolean defaultOrdersInitialized = false;

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

    private EditReportInvoker editReportInvoker = new EditReportInvoker() {
        @Override
        public void invokeEditReport() {
            RmiQueue.runAction(new Runnable() {
                @Override
                public void run() {
                    runEditReport();
                }
            });
        }
    };

    private ScheduledExecutorService autoRefreshScheduler;

    public ClientFormController(String canonicalName, String formSID, RemoteFormInterface remoteForm, byte[] firstChanges, ClientNavigator clientNavigator) {
        this(canonicalName, formSID, remoteForm, firstChanges, clientNavigator, false, false);
    }

    public ClientFormController(String icanonicalName, String iformSID, RemoteFormInterface iremoteForm, byte[] firstChanges, ClientNavigator iclientNavigator, boolean iisModal, boolean iisDialog) {
        formSID = iformSID + (iisModal ? "(modal)" : "") + "(" + System.identityHashCode(this) + ")";
        canonicalName = icanonicalName;
        isDialog = iisDialog;
        isModal = iisModal;

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

        actionDispatcher = new ClientFormActionDispatcher() {
            @Override
            public ClientFormController getFormController() {
                return ClientFormController.this;
            }
        };

        try {
            form = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

            rmiQueue = new RmiQueue(tableManager, serverMessageProvider, serverMessageListProvider, this);

            formLayout = new ClientFormLayout(this, form.mainContainer);

            initializeForm(firstChanges);
        } catch (Exception e) {
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

    public KeyStroke getKeyStroke() {
        return form.keyStroke;
    }

    public String getCaption() {
        return form.caption;
    }

    public String getFullCaption() {
        return form.getFullCaption();
    }

    public String getTooltip() {
        return form.getTooltip();
    }

    public ClientFormLayout getLayout() {
        return formLayout;
    }

    public ColorPreferences getColorPreferences() {
        return colorPreferences;
    }

    public void activateTab(String tabSID) {
        ClientContainer parentContainer = form.findParentContainerBySID(tabSID);
        if(parentContainer != null && parentContainer.isTabbed()) {
            Map<String, Integer> tabMap = getTabMap(parentContainer);
            ClientContainerView containerView = getLayout().getContainerView(parentContainer);
            if(containerView instanceof TabbedClientContainerView)
                ((TabbedClientContainerView)containerView).activateTab(tabMap.get(tabSID));
        }
    }

    private Map<String, Integer> getTabMap(ClientContainer component) {
        Map<String, Integer> tabMap = new HashMap<>();
        List<ClientComponent> tabs = component.getChildren();
        if (tabs != null)
            for (int i = 0; i < tabs.size(); i++)
                tabMap.put(tabs.get(i).getSID(), i);
        return tabMap;
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //
    private void initializeForm(byte[] firstChanges) throws Exception {
        initializeColorPreferences();
        
        initializeControllers();

        initializeRegularFilters();

        if(firstChanges != null) {
            applyFormChanges(-1, firstChanges);
        } else {
            getRemoteChanges(false);
        }

        initializeDefaultOrders();

        initializeAutoRefresh();
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }

    private void initializeColorPreferences() throws RemoteException {
        colorPreferences = remoteForm.getColorPreferences();
        for (ClientPropertyDraw properties : form.getPropertyDraws()) {
                properties.colorPreferences = colorPreferences;
        }
    }
    
    private void initializeControllers() throws IOException {
        FormUserPreferences preferences = remoteForm.getUserPreferences();
        
        for (ClientTreeGroup treeGroup : form.treeGroups) {
            TreeGroupController controller = new TreeGroupController(treeGroup, form, this, formLayout);
            treeControllers.put(treeGroup, controller);
        }

        for (ClientGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GroupObjectController controller = new GroupObjectController(group, form, this, formLayout, extractGridUserPreferences(preferences, group));
                controllers.put(group, controller);
            }
        }

        for (ClientPropertyDraw properties : form.getPropertyDraws()) {
            if (properties.groupObject == null) {
                GroupObjectController controller = new GroupObjectController(form, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
    }
    
    private GridUserPreferences[] extractGridUserPreferences(FormUserPreferences formPreferences, ClientGroupObject groupObject) {
        if (formPreferences != null) {
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
                                        return remoteForm.getRemoteChanges(requestIndex, lastReceivedRequestIndex, true);
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
                formLayout.addBinding(filter.key, "regularFilter" + filterGroup.getID() + filter.getID(), new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        comboBox.setSelectedItem(new ClientRegularFilterWrapper(filter));
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
            formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.getID() + singleFilter.getID(), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    checkBox.setSelected(!checkBox.isSelected());
                }
            });
        }
    }

    private void addFilterView(ClientRegularFilterGroup filterGroup, JComponent filterView) {
        formLayout.add(filterGroup, filterView);

        if (filterGroup.groupObject == null) {
            return;
        }

        List<JComponent> groupFilters = filterViews.get(filterGroup.groupObject);
        if (groupFilters == null) {
            groupFilters = new ArrayList<>();
            filterViews.put(filterGroup.groupObject, groupFilters);
        }
        groupFilters.add(filterView);
    }

    public void setFiltersVisible(ClientGroupObject groupObject, boolean visible) {
        List<JComponent> groupFilters = filterViews.get(groupObject);
        if (groupFilters != null) {
            for (JComponent filterView : groupFilters) {
                filterView.setVisible(visible);
            }
        }
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, int initialFilterPropertyDrawID) {
        ClientPropertyDraw propertyDraw = form.getProperty(initialFilterPropertyDrawID);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).quickEditFilter(initFilterKeyEvent, propertyDraw);
        }
    }

    public void selectProperty(int propertyDrawId) {
        ClientPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).selectProperty(propertyDraw);
        }
    }

    public void focusProperty(int propertyDrawId) {
        ClientPropertyDraw propertyDraw = form.getProperty(propertyDrawId);
        if (propertyDraw != null && controllers.containsKey(propertyDraw.groupObject)) {
            controllers.get(propertyDraw.groupObject).focusProperty(propertyDraw);
        }
    }

    public GroupObjectLogicsSupplier getGroupObjectLogicsSupplier(ClientGroupObject group) {
        GroupObjectController groupObjectController = controllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return group.parent != null
                ? treeControllers.get(group.parent)
                : null;
    }

    public void saveUserPreferences(final GridUserPreferences gridPreferences, final boolean forAllUsers, final boolean completeOverride, final Runnable successCallback, final Runnable failureCallback) throws RemoteException {
        commitOrCancelCurrentEditing();

        ServerResponse result = rmiQueue.syncRequest(new RmiCheckNullFormRequest<ServerResponse>("saveUserPreferences") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.saveUserPreferences(requestIndex, lastReceivedRequestIndex, gridPreferences.convertPreferences(), forAllUsers, completeOverride);
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

    public void initializeDefaultOrders() throws IOException {
        try {
            //применяем все свойства по умолчанию
            applyOrders(form.defaultOrders, null);
            defaultOrdersInitialized = true;

            //применяем пользовательские свойства
            boolean hasUserOrders = false;
            Map<ClientGroupObject, OrderedMap<ClientPropertyDraw, Boolean>> defaultOrders = null;
            for (GroupObjectController controller : controllers.values()) {
                OrderedMap<ClientPropertyDraw, Boolean> objectUserOrders = controller.getUserOrders();
                if(objectUserOrders != null) {
                    if(defaultOrders == null)
                        defaultOrders = groupDefaultOrders();
                    OrderedMap<ClientPropertyDraw, Boolean> defaultObjectOrders = defaultOrders.get(controller.getGroupObject());
                    if(defaultObjectOrders == null)
                        defaultObjectOrders = new OrderedMap<>();
                    if(!BaseUtils.hashEquals(defaultObjectOrders, objectUserOrders)) {
                        applyOrders(objectUserOrders, controller);
                        hasUserOrders = true;
                    }
                }
            }
            if(hasUserOrders)
                getRemoteChanges(false);
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.cant.initialize.default.orders"));
        }
    }

    public OrderedMap<ClientPropertyDraw, Boolean> getDefaultOrders(ClientGroupObject groupObject) {
        return form.getDefaultOrders(groupObject);
    }

    public void applyOrders(OrderedMap<ClientPropertyDraw, Boolean> orders, GroupObjectController groupObjectController) throws IOException {
        Set<ClientGroupObject> wasOrder = new HashSet<>();
        for (Map.Entry<ClientPropertyDraw, Boolean> entry : orders.entrySet()) {
            ClientPropertyDraw property = entry.getKey();
            ClientGroupObject groupObject = property.getGroupObject();
            assert groupObjectController == null || groupObject.equals(groupObjectController.getGroupObject());
            GroupObjectLogicsSupplier groupObjectLogicsSupplier = getGroupObjectLogicsSupplier(groupObject);
            if (groupObjectLogicsSupplier != null) {
                groupObjectLogicsSupplier.changeOrder(property, !wasOrder.contains(groupObject) ? REPLACE : ADD);
                wasOrder.add(groupObject);
                if (!entry.getValue()) {
                    groupObjectLogicsSupplier.changeOrder(property, DIR);
                }
            }
        }
        if(groupObjectController != null) {
            ClientGroupObject groupObject = groupObjectController.getGroupObject();
            if(!wasOrder.contains(groupObject)) {
                groupObjectController.clearOrders();
            }
        }
    }

    private void processServerResponse(ServerResponse serverResponse) throws IOException {
        //ХАК: serverResponse == null теоретически может быть при реконнекте, когда RMI-поток убивается и remote-method возвращает null
        if (serverResponse != null) {
            actionDispatcher.dispatchResponse(serverResponse);
        }
    }

    public void getRemoteChanges(boolean async) {
        ProcessServerResponseRmiRequest request = new ProcessServerResponseRmiRequest("getRemoteChanges") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.getRemoteChanges(requestIndex, lastReceivedRequestIndex, false);
            }
        };
        if (async) {
            rmiQueue.asyncRequest(request);
        } else {
            rmiQueue.syncRequest(request);
        }
    }

    public void applyFormChanges(long requestIndex, byte[] bFormChanges) throws IOException {
        if (bFormChanges == null) {
            return;
        }

        ClientFormChanges formChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(bFormChanges)), form);

        for (Map.Entry<ClientGroupObject, ClassViewType> entry : formChanges.classViews.entrySet()) {
            ClassViewType classView = entry.getValue();
            if (classView != ClassViewType.GRID) {
                currentGridObjects.remove(entry.getKey());
            }
        }
        currentGridObjects.putAll(formChanges.gridObjects);

        modifyFormChangesWithModifyObjectAsyncs(requestIndex, formChanges);

        modifyFormChangesWithChangeCurrentObjectAsyncs(requestIndex, formChanges);

        modifyFormChangesWithChangePropertyAsyncs(requestIndex, formChanges);

        for (GroupObjectController controller : controllers.values()) {
            controller.processFormChanges(formChanges, currentGridObjects);
        }

        for (TreeGroupController treeController : treeControllers.values()) {
            treeController.processFormChanges(formChanges, currentGridObjects);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                formLayout.revalidate();
                ClientExternalScreen.repaintAll(getID());
            }
        });
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

                ClientPropertyDraw propertyDraw = cell.getRowKey();
                ClientGroupObjectValue keys = cell.getColumnKey();

                Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(propertyDraw);
                if (propertyValues == null) { // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                    propertyValues = new HashMap<>();
                    formChanges.properties.put(propertyDraw, propertyValues);
                    formChanges.updateProperties.add(propertyDraw);
                }

                if (formChanges.updateProperties.contains(propertyDraw) && !propertyValues.containsKey(keys)) {
                    propertyValues.put(keys, change.oldValue);
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

    public void expandGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("expandGroupObject") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.expandGroupObject(requestIndex, lastReceivedRequestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void collapseGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
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
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changeGroupObject.end") {
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

    public void changeProperty(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey,
                               final Object newValue, final Object oldValue) throws IOException {
        assert !isEditing();

        commitOrCancelCurrentEditing();

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился
        final byte[] newValueBytes = serializeObject(newValue);

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest("changeProperty") {
            @Override
            protected void onAsyncRequest(long requestIndex) {
//                System.out.println("!!Async changing property with req#: " + requestIndex);
//                ExceptionUtils.dumpStack();
//                System.out.println("------------------------");

                GroupObjectController controller = controllers.get(property.groupObject);

                ClientGroupObjectValue propertyKey = controller != null && !controller.hasPanelProperty(property)
                                                     ? new ClientGroupObjectValue(controller.getCurrentObject(), columnKey)
                                                     : columnKey;

                pendingChangePropertyRequests.put(property, propertyKey,
                                                    new PropertyChange(requestIndex, newValue, oldValue, property.canUseChangeValueForRendering())
                );
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changeProperty(requestIndex, lastReceivedRequestIndex, property.getID(), fullCurrentKey, newValueBytes, null);
            }

            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());
                processServerResponse(result);
            }
        });
    }

    public boolean isAsyncModifyObject(ClientPropertyDraw property) {
        if (property.addRemove != null) {
            GroupObjectController controller = controllers.get(property.addRemove.first.groupObject);
            if (controller != null && controller.classView == ClassViewType.GRID) {
                return true;
            }
        }
        return false;
    }

    public void modifyObject(final ClientPropertyDraw property, ClientGroupObjectValue columnKey) throws IOException {
        assert isAsyncModifyObject(property);

        commitOrCancelCurrentEditing();

        final ClientObject object = property.addRemove.first;
        final boolean add = property.addRemove.second;

        final GroupObjectController controller = controllers.get(object.groupObject);

        final int ID;
        final ClientGroupObjectValue value;
        if(add) {
            ID = Main.generateID();
            value = new ClientGroupObjectValue(object, ID);
        } else {
            value = controller.getCurrentObject();
            ID = (Integer) BaseUtils.singleValue(value);
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
                return remoteForm.changeProperty(requestIndex, lastReceivedRequestIndex, property.getID(), fullCurrentKey, null, add ? ID : null);
            }
        });
    }

    public ClientGroupObjectValue getFullCurrentKey() {
        ClientGroupObjectValue fullKey = new ClientGroupObjectValue();

        for (GroupObjectController group : controllers.values()) {
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

    public boolean hasVisibleGrid() {
        for (GroupObjectController group : controllers.values()) {
            if (group.grid != null && group.grid.isVisible()) {
                return true;
            }
        }
        return false;
    }

    public ServerResponse executeEditAction(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, final String actionSID) throws IOException {
        // При выполнение синхронных запросов, EDT блокируется. Если перед этим синхр. запросом был послан асинхронный, который возвращает DockedModal-FormAction,
        // то получается dead-lock: executeEditAction ждёт окончания предыдущего async-запроса и значит закрытия DockedModal формы,
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
                rmiQueue.syncRequest(new RmiCheckNullFormRequest<ServerResponse>("executeEditAction") {
                    @Override
                    protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                        return remoteForm.executeEditAction(requestIndex, lastReceivedRequestIndex, property.getID(), fullCurrentKey, actionSID);
                    }

                    @Override
                    protected void onResponseGetFailed(long requestIndex) throws Exception {
                        processServerResponse(new ServerResponse(requestIndex, new ClientAction[0], true));
                    }
                });
        return result == null ? ServerResponse.EMPTY : result;
    }

    public ServerResponse continueServerInvocation(final long requestIndex, final int continueIndex, final Object[] actionResults) throws RemoteException {
        ServerResponse result =
                rmiQueue.directRequest(requestIndex, new RmiCheckNullFormRequest<ServerResponse>("continueServerInvocation") {
                    @Override
                    protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                        return remoteForm.continueServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
                    }
                });
        return result == null ? ServerResponse.EMPTY : result;
    }

    public ServerResponse throwInServerInvocation(final long requestIndex, final int continueIndex, final Throwable t) throws RemoteException {
        ServerResponse result =
                rmiQueue.directRequest(requestIndex, new RmiCheckNullFormRequest<ServerResponse>("throwInServerInvocation") {
                    @Override
                    protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                        return remoteForm.throwInServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, t);
                    }
                });
        return result == null ? ServerResponse.EMPTY : result;
    }

    public void gainedFocus() {
        try {
            rmiQueue.asyncRequest(new RmiVoidRequest("gainedFocus") {
                @Override
                protected void doExecute(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    remoteForm.gainedFocus(requestIndex, lastReceivedRequestIndex);
                }
            });

            // если вдруг изменились данные в сессии
            ClientExternalScreen.invalidate(getID());
            ClientExternalScreen.repaintAll(getID());
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

    public void pasteExternalTable(List<ClientPropertyDraw> propertyList, List<ClientGroupObjectValue> columnKeys, final List<List<String>> table, int maxColumns) throws IOException {
        final List<List<byte[]>> values = new ArrayList<>();
        for (List<String> sRow : table) {
            List<byte[]> valueRow = new ArrayList<>();

            int rowLength = Math.min(sRow.size(), maxColumns);
            for (int i = 0; i < rowLength; i++) {
                ClientPropertyDraw property = propertyList.get(i);
                String sCell = sRow.get(i);
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

    public void changeGridClass(final ClientObject object, final ClientObjectClass cls) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changeGridClass") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changeGridClass(requestIndex, lastReceivedRequestIndex, object.getID(), cls.getID());
            }
        });
    }

    public void switchClassView(ClientGroupObject groupObject) throws IOException {
        ClassViewType newClassView = ClassViewType.switchView(controllers.get(groupObject).classView);
        changeClassView(groupObject, newClassView);
    }

    public void changeClassView(final ClientGroupObject groupObject, final ClassViewType show) throws IOException {
        commitOrCancelCurrentEditing();

        SwingUtils.commitDelayedGroupObjectChange(groupObject);

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changeClassView") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.changeClassView(requestIndex, lastReceivedRequestIndex, groupObject.getID(), show);
            }
        });
    }

    public void changePropertyOrder(final ClientPropertyDraw property, final Order modiType, final ClientGroupObjectValue columnKey) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("changePropertyOrder") {
                @Override
                protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.changePropertyOrder(requestIndex, lastReceivedRequestIndex, property.getID(), modiType.serialize(), columnKey.serialize());
                }
            });
        }
    }

    public void clearPropertyOrders(final ClientGroupObject groupObject) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("clearPropertyOrders") {
                @Override
                protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.clearPropertyOrders(requestIndex, lastReceivedRequestIndex, groupObject.getID());
                }
            });
        }
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

    private void applyCurrentFilters() throws IOException {
        commitOrCancelCurrentEditing();

        final List<byte[]> filters = new ArrayList<>();

        for (List<ClientPropertyFilter> groupFilters : currentFilters.values()) {
            for (ClientPropertyFilter filter : groupFilters) {
                if (!(filter.property.baseType instanceof ClientActionClass))
                    filters.add(Serializer.serializeClientFilter(filter));
            }
        }

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("applyCurrentFilters") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setUserFilters(requestIndex, lastReceivedRequestIndex, filters.toArray(new byte[filters.size()][]));
            }
        });
    }

    private void setRegularFilter(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter filter) throws IOException {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("setRegularFilter") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.setRegularFilter(requestIndex, lastReceivedRequestIndex, filterGroup.getID(), (filter == null) ? -1 : filter.getID());
            }
        });
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
    
    public List<FormGrouping> readGroupings(final String groupObjectSID) {
        List<FormGrouping> result = rmiQueue.syncRequest(new RmiCheckNullFormRequest<List<FormGrouping>>("readGroupings") {
            @Override
            protected List<FormGrouping> doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.readGroupings(requestIndex, lastReceivedRequestIndex, groupObjectSID);
            }
        });
        return result == null ? new ArrayList<FormGrouping>() : result;
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
        for (GroupObjectController controller : controllers.values()) {
            if (controller.getGroupObject() != null) {
                groupObjectUserPreferencesList.add(controller.getUserGridPreferences());
                groupObjectGeneralPreferencesList.add(controller.getGeneralGridPreferences());
            }
        }
        return new FormUserPreferences(groupObjectGeneralPreferencesList, groupObjectUserPreferencesList);
    }

    public void hideForm() {
        if (autoRefreshScheduler != null) {
            autoRefreshScheduler.shutdown();
        }
        // здесь мы сбрасываем ссылку на remoteForm для того, чтобы сборщик мусора быстрее собрал удаленные объекты
        // это нужно, чтобы connection'ы на сервере закрывались как можно быстрее
        remoteForm = null;
    }

    public void runPrintReport(final boolean isDebug) {
        assert Main.module.isFull();

        try {
            rmiQueue.syncRequest(new RmiCheckNullFormRequest<ReportGenerationData>("runPrintReport") {
                @Override
                protected ReportGenerationData doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.getReportData(requestIndex, lastReceivedRequestIndex, null, false, getUserPreferences());
                }

                @Override
                public void onResponse(long requestIndex, final ReportGenerationData generationData) throws Exception {
                    if (generationData != null) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                Main.frame.runReport(false, generationData, isDebug ? editReportInvoker : null);
                                } catch (Exception e) {
                                    throw new RuntimeException(getString("form.error.printing.form"), e);
                                }
                            }
                        });

                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    public void runOpenInExcel() {
        assert Main.module.isFull();

        try {
            rmiQueue.syncRequest(new RmiCheckNullFormRequest<ReportGenerationData>("runOpenInExcel") {
                @Override
                protected ReportGenerationData doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.getReportData(requestIndex, lastReceivedRequestIndex, null, true, getUserPreferences());
                }

                @Override
                public void onResponse(long requstIndex, final ReportGenerationData generationData) throws Exception {
                    if (generationData != null) {
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                Main.module.openInExcel(generationData);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.running.excel"), e);
        }
    }

    public void runEditReport() {
        assert Main.module.isFull();

        try {
            rmiQueue.syncRequest(new RmiCheckNullFormRequest<Map<String, String>>("runEditReport") {
                @Override
                protected Map<String, String> doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.getReportPath(requestIndex, lastReceivedRequestIndex, false, null, getUserPreferences());
                }

                @Override
                public void onResponse(long requstIndex, Map<String, String> pathMap) throws Exception {
                    if (pathMap != null) {
                        for (String path : pathMap.keySet()) {
                            Desktop.getDesktop().open(new File(path));
                        }

                        // не очень хорошо оставлять живой поток, но это используется только в девелопменте, поэтому не важно
                        new SavingThread(pathMap).start();
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    public void runSingleGroupReport(final GroupObjectController groupController) {
        commitOrCancelCurrentEditing();
        try {
            rmiQueue.syncRequest(new RmiCheckNullFormRequest<ReportGenerationData>("runSingleGroupReport") {
                @Override
                protected ReportGenerationData doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                    return remoteForm.getReportData(requestIndex, lastReceivedRequestIndex, groupController.getGroupObject().getID(), false, getUserPreferences());
                }

                @Override
                public void onResponse(long requstIndex, ReportGenerationData generationData) throws Exception {
                    if (generationData != null) {
                        Main.frame.runReport(false, generationData, null);
                    }
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void runSingleGroupXlsExport(final GroupObjectController groupController) {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new RmiCheckNullFormRequest<ReportGenerationData>("runSingleGroupXlsExport") {
            @Override
            protected ReportGenerationData doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.getReportData(requestIndex, lastReceivedRequestIndex, groupController.getGroupObject().getID(), true, getUserPreferences());
            }

            @Override
            public void onResponse(long requstIndex, ReportGenerationData generationData) throws Exception {
                if (generationData != null) {
                    Main.module.openInExcel(generationData);
                }
            }
        });
    }

    public void okPressed() {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest("okPressed") {
            @Override
            protected ServerResponse doRequest(long requestIndex, long lastReceivedRequestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                return remoteForm.okPressed(requestIndex, lastReceivedRequestIndex);
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
            asyncTimer = new Timer(Main.asyncTimeOut, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    asyncPrevIcon = asyncView.getIcon();
                    asyncView.setIcon(loadingIcon);
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
}
