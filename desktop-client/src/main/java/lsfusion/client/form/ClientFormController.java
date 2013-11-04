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
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.form.dispatch.ClientFormActionDispatcher;
import lsfusion.client.form.dispatch.SimpleChangePropertyDispatcher;
import lsfusion.client.form.grid.GridUserPreferences;
import lsfusion.client.form.layout.ClientFormLayout;
import lsfusion.client.form.tree.TreeGroupController;
import lsfusion.client.logics.*;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.logics.filter.ClientPropertyFilter;
import lsfusion.client.navigator.ClientNavigator;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.Order;
import lsfusion.interop.Scroll;
import lsfusion.interop.form.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
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
    private static final String FORM_REFRESH_PROPERTY_SID = "formRefresh";

    private final TableManager tableManager = new TableManager(this);

    private final EProvider<String> serverMessageProvider = new EProvider<String>() {
        @Override
        public String getExceptionally() throws Exception {
            return remoteForm == null ? null : remoteForm.getRemoteActionMessage();
        }
    };

    private final RmiQueue rmiQueue = new RmiQueue(tableManager, serverMessageProvider, this);
    private final SimpleChangePropertyDispatcher simpleDispatcher = new SimpleChangePropertyDispatcher(this);

    private volatile RemoteFormInterface remoteForm;

    private final ClientForm form;
    private final ClientNavigator clientNavigator;
    private final ClientFormActionDispatcher actionDispatcher;

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private int ID;

    private ClientFormLayout formLayout;

    private final Map<ClientGroupObject, GroupObjectController> controllers = new HashMap<ClientGroupObject, GroupObjectController>();
    private final Map<ClientTreeGroup, TreeGroupController> treeControllers = new HashMap<ClientTreeGroup, TreeGroupController>();

    private final Map<ClientGroupObject, List<JComponent>> filterViews = new HashMap<ClientGroupObject, List<JComponent>>();

    private boolean defaultOrdersInitialized = false;

    private final boolean isDialog;
    private final boolean isModal;

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObject, List<ClientPropertyFilter>>();

    private final Map<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();

    private final OrderedMap<Long, ModifyObject> pendingModifyObjectRequests = new OrderedMap<Long, ModifyObject>();
    private final Map<ClientGroupObject, Long> pendingChangeCurrentObjectsRequests = Maps.newHashMap();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange> pendingChangePropertyRequests = HashBasedTable.create();

    private Timer asyncTimer;
    private PanelView asyncView;
    private Icon asyncPrevIcon;

    private boolean blocked = false;

    private boolean showing = true;

    private EditReportInvoker editReportInvoker = new EditReportInvoker() {
        @Override
        public void invokeEditReport() {
            runEditReport();
        }
    };

    private ScheduledExecutorService autoRefreshScheduler;

    public ClientFormController(RemoteFormInterface remoteForm, ClientNavigator clientNavigator) {
        this(remoteForm, clientNavigator, false, false);
    }

    public ClientFormController(RemoteFormInterface iremoteForm, ClientNavigator iclientNavigator, boolean iisModal, boolean iisDialog) {
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
            initializeForm();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
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

    public ClientFormLayout getLayout() {
        return formLayout;
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //
    private void initializeForm() throws Exception {
        formLayout = new ClientFormLayout(this, form.mainContainer);

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        getRemoteChanges(false);

        initializeAutoRefresh();
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
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
                Map<ClientPropertyDraw, ColumnUserPreferences> columnPreferences = new HashMap<ClientPropertyDraw, ColumnUserPreferences>();
                for (Map.Entry<String, ColumnUserPreferences> entry : groupPreferences.getColumnUserPreferences().entrySet()) {
                    ClientPropertyDraw property = form.getProperty(entry.getKey());
                    if (property != null) {
                        columnPreferences.put(property, entry.getValue());
                    }
                }
                return new GridUserPreferences(groupObject, columnPreferences, groupPreferences.fontInfo, groupPreferences.hasUserPreferences);
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
        final ClientPropertyDraw property = form.getProperty(FORM_REFRESH_PROPERTY_SID);
        if (property != null && form.autoRefresh > 0) {
            SwingUtils.assertDispatchThread();

            // т.к. модальные диалоги запускают новый EDT для обработки событий, то возможен случай,
            // когда авторефреш для этой формы попробует выполниться, когда форма заблокирована, что есс-но приведёт к dead-lock,
            // поэтому добавляем проверку на корректный поток
            final Thread executingEdtThread = Thread.currentThread();
            autoRefreshScheduler = Executors.newScheduledThreadPool(1);
            autoRefreshScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    SwingUtils.invokeLater(new ERunnable() {
                        @Override
                        public void run() throws Exception {
                            if (remoteForm != null &&
                                !blocked &&
                                showing &&
                                !isModal &&
                                !isEditing() &&
                                !rmiQueue.isSyncStarted()
                                && Thread.currentThread() == executingEdtThread) {

                                simpleDispatcher.executeAction(property, ClientGroupObjectValue.EMPTY);
                            }
                        }
                    });
                }

            }, form.autoRefresh, form.autoRefresh, TimeUnit.SECONDS);
        }
    }

    private void createMultipleFilterComponent(final ClientRegularFilterGroup filterGroup) {
        final JComboBox comboBox = new JComboBox();
        comboBox.addItem(new ClientRegularFilterWrapper(getString("form.all")));
        for (final ClientRegularFilter filter : filterGroup.filters) {
            comboBox.addItem(new ClientRegularFilterWrapper(filter));
            formLayout.addBinding(filter.key, "regularFilter" + filterGroup.getID() + filter.getID(), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    comboBox.setSelectedItem(new ClientRegularFilterWrapper(filter));
                }
            });
        }

        if (filterGroup.defaultFilterIndex >= 0) {
            ClientRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilterIndex);
            comboBox.setSelectedItem(new ClientRegularFilterWrapper(defaultFilter));
        }
        comboBox.addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(ItemEvent e) {
                try {
                    setRegularFilter(filterGroup, ((ClientRegularFilterWrapper) e.getItem()).filter);
                } catch (IOException ioe) {
                    throw new RuntimeException(getString("form.error.changing.regular.filter"), ioe);
                }
            }
        });

        addFilterView(filterGroup, comboBox);
    }

    private void createSingleFilterComponent(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter singleFilter) {
        final JCheckBox checkBox = new JCheckBox(singleFilter.getFullCaption());

        if (filterGroup.defaultFilterIndex >= 0) {
            checkBox.setSelected(true);
        }

        checkBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie) {
                try {
                    if (ie.getStateChange() == ItemEvent.SELECTED) {
                        setRegularFilter(filterGroup, singleFilter);
                    }
                    if (ie.getStateChange() == ItemEvent.DESELECTED) {
                        setRegularFilter(filterGroup, null);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(getString("form.error.changing.regular.filter"), e);
                }
            }
        });

        addFilterView(filterGroup, checkBox);

        formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.getID() + singleFilter.getID(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                checkBox.setSelected(!checkBox.isSelected());
            }
        });
    }

    private void addFilterView(ClientRegularFilterGroup filterGroup, JComponent filterView) {
        formLayout.add(filterGroup, filterView);

        if (filterGroup.groupObject == null) {
            return;
        }

        List<JComponent> groupFilters = filterViews.get(filterGroup.groupObject);
        if (groupFilters == null) {
            groupFilters = new ArrayList<JComponent>();
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

    public GroupObjectLogicsSupplier getGroupObjectLogicsSupplier(ClientGroupObject group) {
        GroupObjectController groupObjectController = controllers.get(group);
        if (groupObjectController != null) {
            return groupObjectController;
        }

        return group.parent != null
                ? treeControllers.get(group.parent)
                : null;
    }

    public void saveUserPreferences(final GridUserPreferences gridPreferences, final boolean forAllUsers) throws RemoteException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new RmiVoidRequest() {
            @Override
            protected void doExecute(long requestIndex, RemoteFormInterface remoteForm) throws RemoteException {
                remoteForm.saveUserPreferences(requestIndex, gridPreferences.convertPreferences(), forAllUsers);
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

    public void initializeDefaultOrders() throws IOException {
        getRemoteChanges(false);
        try {
            //применяем все свойства по умолчанию
            applyOrders(form.defaultOrders);
            defaultOrdersInitialized = true;

            //применяем пользовательские свойства
            OrderedMap<ClientPropertyDraw, Boolean> userOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
            for (GroupObjectController controller : controllers.values()) {
                userOrders.putAll(controller.getUserOrders());
            }
            applyOrders(userOrders);
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.cant.initialize.default.orders"));
        }
    }

    public void applyOrders(OrderedMap<ClientPropertyDraw, Boolean> orders) throws IOException {
        Set<ClientGroupObject> wasOrder = new HashSet<ClientGroupObject>();
        for (Map.Entry<ClientPropertyDraw, Boolean> entry : orders.entrySet()) {
            ClientPropertyDraw property = entry.getKey();
            ClientGroupObject groupObject = property.getGroupObject();
            GroupObjectLogicsSupplier groupObjectLogicsSupplier = getGroupObjectLogicsSupplier(groupObject);
            if (groupObjectLogicsSupplier != null) {
                groupObjectLogicsSupplier.changeOrder(property, !wasOrder.contains(groupObject) ? REPLACE : ADD);
                wasOrder.add(groupObject);
                if (!entry.getValue()) {
                    groupObjectLogicsSupplier.changeOrder(property, DIR);
                }
            }
        }
    }

    private void processServerResponse(ServerResponse serverResponse) throws IOException {
        //ХАК: serverResponse == null теоретически может быть при реконнекте, когда RMI-поток убивается и remote-method возвращает null
        if (serverResponse != null) {
            actionDispatcher.dispatchResponse(serverResponse);
        }
    }

    public void getRemoteChanges(boolean async) throws IOException {
        rmiQueue.syncRequestWithTimeOut(async ? 0 : RmiQueue.FOREVER, new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.getRemoteChanges(requestIndex);
            }
        });
    }

    public void applyFormChanges(byte[] bFormChanges) throws IOException {
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

        modifyFormChangesWithModifyObjectAsyncs(formChanges);

        modifyFormChangesWithChangeCurrentObjectAsyncs(formChanges);

        modifyFormChangesWithChangePropertyAsyncs(formChanges);

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

        // выдадим сообщение если было от сервера
        if (formChanges.message.length() > 0) {
            Log.error(formChanges.message);
        }
    }

    private void modifyFormChangesWithChangeCurrentObjectAsyncs(ClientFormChanges formChanges) {
        long currentDispatchingRequestIndex = rmiQueue.getCurrentDispatchingRequestIndex();

        for (Iterator<Map.Entry<ClientGroupObject, Long>> iterator = pendingChangeCurrentObjectsRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ClientGroupObject, Long> entry = iterator.next();

            if (entry.getValue() <= currentDispatchingRequestIndex) {
                iterator.remove();
            } else {
                formChanges.objects.remove(entry.getKey());
            }
        }
    }

    private void modifyFormChangesWithChangePropertyAsyncs(ClientFormChanges formChanges) {
        long currentDispatchingRequestIndex = rmiQueue.getCurrentDispatchingRequestIndex();

        for (Iterator<Table.Cell<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange>> iterator = pendingChangePropertyRequests.cellSet().iterator(); iterator.hasNext(); ) {
            Table.Cell<ClientPropertyDraw, ClientGroupObjectValue, PropertyChange> cell = iterator.next();
            PropertyChange change = cell.getValue();
            if (change.requestIndex <= currentDispatchingRequestIndex) {
                iterator.remove();

                ClientPropertyDraw propertyDraw = cell.getRowKey();
                ClientGroupObjectValue keys = cell.getColumnKey();

                Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(propertyDraw);
                if (propertyValues == null) { // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                    propertyValues = new HashMap<ClientGroupObjectValue, Object>();
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

    private void modifyFormChangesWithModifyObjectAsyncs(ClientFormChanges formChanges) {
        long currentDispatchingRequestIndex = rmiQueue.getCurrentDispatchingRequestIndex();

        for (Iterator<Map.Entry<Long,ModifyObject>> iterator = pendingModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            if(cell.getKey() <= currentDispatchingRequestIndex)
                iterator.remove();
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
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.expandGroupObject(requestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void collapseGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.collapseGroupObject(requestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject groupObject, final Scroll changeType) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changeGroupObject(requestIndex, groupObject.getID(), changeType.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        if (objectValue == null) {
            return;
        }

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected void onAsyncRequest(long requestIndex) {
//                        System.out.println("!!Async changing group object with req#: " + requestIndex + " on " + objectValue);
                pendingChangeCurrentObjectsRequests.put(group, requestIndex);
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changeGroupObject(requestIndex, group.getID(), objectValue.serialize());
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

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
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
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changeProperty(requestIndex, property.getID(), fullCurrentKey, serializeObject(newValue), null);
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
            ID = Main.remoteLogics.generateID();
            value = new ClientGroupObjectValue(object, ID);
        } else {
            value = controller.getCurrentObject();
            ID = (Integer) BaseUtils.singleValue(value);
        }

        final byte[] fullCurrentKey = getFullCurrentKey(columnKey); // чтобы не изменился

        rmiQueue.syncRequestWithTimeOut(Main.asyncTimeOut, new ProcessServerResponseRmiRequest() {
            @Override
            protected void onAsyncRequest(long requestIndex) {
                controller.modifyGroupObject(value, add); // сначала посылаем запрос, так как getFullCurrentKey может измениться

                pendingChangeCurrentObjectsRequests.put(object.groupObject, requestIndex); // так как по сути такой execute сам меняет groupObject
                pendingModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value));
            }

            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changeProperty(requestIndex, property.getID(), fullCurrentKey, null, add ? ID : null);
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
        // а форма не может отработать, т.к. EDT заблокирован. Модальные диалоги отрабатывают нормально, т.к. Swing специально создаёт для них новый поток.
        // Поэтому применяется двойной хак: если DockedModal-FormAction пришёл после начала синхронного редактирования, то она показывается в модальном диалоге,
        // а если сначала была показана форма, а затем на текущей форме сработало редактирование - то мы его отменяем

        if (blocked) {
            return ServerResponse.EMPTY;
        }

        commitOrCancelCurrentEditing();

        SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());

        return rmiQueue.syncRequest(new RmiCheckNullFormRequest<ServerResponse>() {
            @Override
            protected ServerResponse defaultValue() { return ServerResponse.EMPTY; }

            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.executeEditAction(requestIndex, property.getID(), columnKey.serialize(), actionSID);
            }
        });
    }

    public ServerResponse continueServerInvocation(final Object[] actionResults) throws RemoteException {
        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();
        try {
            return remoteForm.continueServerInvocation(actionResults);
        } finally {
            busyDisplayer.stop();
        }
    }

    public ServerResponse throwInServerInvocation(Throwable t) throws RemoteException {
        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();
        try {
            return remoteForm.throwInServerInvocation(t);
        } finally {
            busyDisplayer.stop();
        }
    }

    public void gainedFocus() {
        try {
            rmiQueue.asyncRequest(new RmiVoidRequest() {
                @Override
                protected void doExecute(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                    remoteForm.gainedFocus(requestIndex);
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
        rmiQueue.syncRequestWithTimeOut(Main.asyncTimeOut, new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.setTabVisible(requestIndex, container.getID(), component.getID());
            }
        });
    }

    public void pasteExternalTable(List<ClientPropertyDraw> propertyList, List<ClientGroupObjectValue> columnKeys, final List<List<String>> table, int maxColumns) throws IOException {
        final List<List<byte[]>> values = new ArrayList<List<byte[]>>();
        for (List<String> sRow : table) {
            List<byte[]> valueRow = new ArrayList<byte[]>();

            int rowLength = Math.min(sRow.size(), maxColumns);
            for (int i = 0; i < rowLength; i++) {
                ClientPropertyDraw property = propertyList.get(i);
                String sCell = sRow.get(i);
                Object oCell = sCell == null ? null : property.parseChangeValueOrNull(sCell);
                valueRow.add(serializeObject(oCell));
            }
            values.add(valueRow);
        }

        final List<Integer> propertyIdList = new ArrayList<Integer>();
        for (ClientPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.getID());
        }
        final List<byte[]> serializedColumnKeys = new ArrayList<byte[]>();
        for (ClientGroupObjectValue key : columnKeys) {
            serializedColumnKeys.add(key.serialize());
        }
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.pasteExternalTable(requestIndex, propertyIdList, serializedColumnKeys, values);
            }
        });
    }

    public void pasteMulticellValue(final Map<ClientPropertyDraw, PasteData> paste) throws IOException {
        if (paste.isEmpty()) {
            return;
        }

        final Map<Integer, List<byte[]>> mKeys = new HashMap<Integer, List<byte[]>>();
        final Map<Integer, byte[]> mValues = new HashMap<Integer, byte[]>();

        for (Map.Entry<ClientPropertyDraw, PasteData> keysEntry : paste.entrySet()) {
            ClientPropertyDraw property = keysEntry.getKey();
            PasteData pasteData = keysEntry.getValue();

            List<byte[]> propMKeys = new ArrayList<byte[]>();
            for (int i = 0; i < pasteData.keys.size(); ++i) {
                propMKeys.add(getFullCurrentKey(pasteData.keys.get(i)));
            }

            mKeys.put(property.getID(), propMKeys);
            mValues.put(property.getID(), serializeObject(pasteData.newValue));
        }

        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
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
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.pasteMulticellValue(requestIndex, mKeys, mValues);
            }
        });
    }

    public void changeGridClass(final ClientObject object, final ClientObjectClass cls) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changeGridClass(requestIndex, object.getID(), cls.getID());
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

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changeClassView(requestIndex, groupObject.getID(), show);
            }
        });
    }

    public void changePropertyOrder(final ClientPropertyDraw property, final Order modiType, final ClientGroupObjectValue columnKey) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
                @Override
                protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                    return remoteForm.changePropertyOrder(requestIndex, property.getID(), modiType.serialize(), columnKey.serialize());
                }
            });
        }
    }

    public void clearPropertyOrders(final ClientGroupObject groupObject) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
                @Override
                protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                    return remoteForm.clearPropertyOrders(requestIndex, groupObject.getID());
                }
            });
        }
    }

    public void changeFilter(ClientGroupObject groupObject, List<ClientPropertyFilter> conditions) throws IOException {
        currentFilters.put(groupObject, new ArrayList<ClientPropertyFilter>(conditions));
        applyCurrentFilters();
    }

    public void changeFilter(ClientTreeGroup treeGroup, List<ClientPropertyFilter> conditions) throws IOException {
        Map<ClientGroupObject, List<ClientPropertyFilter>> filters = BaseUtils.groupList(new BaseUtils.Group<ClientGroupObject, ClientPropertyFilter>() {
            public ClientGroupObject group(ClientPropertyFilter key) {
                return key.groupObject;
            }
        }, new ArrayList<ClientPropertyFilter>(conditions));

        for (ClientGroupObject group : treeGroup.groups) {
            List<ClientPropertyFilter> groupFilters = filters.get(group);
            if (groupFilters == null) {
                groupFilters = new ArrayList<ClientPropertyFilter>();
            }

            currentFilters.put(group, groupFilters);
        }

        applyCurrentFilters();
    }

    private void applyCurrentFilters() throws IOException {
        commitOrCancelCurrentEditing();

        final List<byte[]> filters = new ArrayList<byte[]>();

        for (List<ClientPropertyFilter> groupFilters : currentFilters.values()) {
            for (ClientPropertyFilter filter : groupFilters) {
                filters.add(Serializer.serializeClientFilter(filter));
            }
        }

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.setUserFilters(requestIndex, filters.toArray(new byte[filters.size()][]));
            }
        });
    }

    private void setRegularFilter(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter filter) throws IOException {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.setRegularFilter(requestIndex, filterGroup.getID(), (filter == null) ? -1 : filter.getID());
            }
        });
    }

    public int countRecords(final int groupObjectID) throws Exception {
        commitOrCancelCurrentEditing();

        return rmiQueue.syncRequest(new RmiRequest<Integer>() {
            @Override
            public Integer doRequest(long requestIndex) throws Exception {
                return remoteForm.countRecords(requestIndex, groupObjectID);
            }
        });
    }

    public Object calculateSum(final int propertyID, final byte[] columnKeys) throws Exception {
        commitOrCancelCurrentEditing();

        return rmiQueue.syncRequest(new RmiRequest<Object>() {
            @Override
            public Object doRequest(long requestIndex) throws Exception {
                return remoteForm.calculateSum(requestIndex, propertyID, columnKeys);
            }
        });
    }

    public Map<List<Object>, List<Object>> groupData(final Map<Integer, List<byte[]>> groupMap, final Map<Integer, List<byte[]>> sumMap, final Map<Integer,
            List<byte[]>> maxMap, final boolean onlyNotNull) throws IOException {
        commitOrCancelCurrentEditing();

        return rmiQueue.syncRequest(new RmiRequest<Map<List<Object>, List<Object>>>() {
            @Override
            public Map<List<Object>, List<Object>> doRequest(long requestIndex) throws Exception {
                return remoteForm.groupData(requestIndex, groupMap, sumMap, maxMap, onlyNotNull);
            }
        });
    }

    public void changePageSize(final ClientGroupObject groupObject, final Integer pageSize) throws IOException {
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.changePageSize(requestIndex, groupObject.getID(), pageSize);
            }
        });
    }

    public void moveGroupObject(final ClientGroupObject parentGroup, final ClientGroupObjectValue parentKey, final ClientGroupObject childGroup, final ClientGroupObjectValue childKey, final int index) throws IOException {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.moveGroupObject(requestIndex, parentGroup.getID(), parentKey.serialize(), childGroup.getID(), childKey.serialize(), index);
            }
        });
    }

    public void revalidate() {
        formLayout.revalidate();
    }

    public void closed() {
    }

    public FormUserPreferences getUserPreferences() {
        List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GroupObjectUserPreferences>();
        for (GroupObjectController controller : controllers.values()) {
            if (controller.getGroupObject() != null) {
                groupObjectUserPreferencesList.add(controller.grid.table.getCurrentPreferences().convertPreferences());
            }
        }
        return new FormUserPreferences(groupObjectUserPreferencesList, null);
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
            rmiQueue.syncRequest(new RmiRequest<ReportGenerationData>() {
                @Override
                protected ReportGenerationData doRequest(long requestIndex) throws Exception {
                    return remoteForm.getReportData(requestIndex, null, false, getUserPreferences());
                }

                @Override
                public void onResponse(long requestIndex, ReportGenerationData generationData) throws Exception {
                    Main.frame.runReport(remoteForm.getSID(), false, generationData, isDebug ? editReportInvoker : null);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    public void runOpenInExcel() {
        assert Main.module.isFull();

        try {
            rmiQueue.syncRequest(new RmiRequest<ReportGenerationData>() {
                @Override
                protected ReportGenerationData doRequest(long requestIndex) throws Exception {
                    return remoteForm.getReportData(requestIndex, null, true, getUserPreferences());
                }

                @Override
                public void onResponse(long requstIndex, ReportGenerationData generationData) throws Exception {
                    Main.module.openInExcel(generationData);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.running.excel"), e);
        }
    }

    public void runEditReport() {
        assert Main.module.isFull();

        try {
            rmiQueue.syncRequest(new RmiRequest<Map<String, String>>() {
                @Override
                protected Map<String, String> doRequest(long requestIndex) throws Exception {
                    return remoteForm.getReportPath(requestIndex, false, null, getUserPreferences());
                }

                @Override
                public void onResponse(long requstIndex, Map<String, String> pathMap) throws Exception {
                    for (String path : pathMap.keySet()) {
                        Desktop.getDesktop().open(new File(path));
                    }

                    // не очень хорошо оставлять живой поток, но это используется только в девелопменте, поэтому не важно
                    new SavingThread(pathMap).start();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    public void runSingleGroupReport(final GroupObjectController groupController) {
        commitOrCancelCurrentEditing();
        try {
            rmiQueue.syncRequest(new RmiRequest<ReportGenerationData>() {
                @Override
                protected ReportGenerationData doRequest(long requestIndex) throws Exception {
                    return remoteForm.getReportData(requestIndex, groupController.getGroupObject().getID(), false, getUserPreferences());
                }

                @Override
                public void onResponse(long requstIndex, ReportGenerationData generationData) throws Exception {
                    Main.frame.runReport("SingleGroupReport_" + remoteForm.getSID(), false, generationData, null);
                }
            });
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public void runSingleGroupXlsExport(final GroupObjectController groupController) {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new RmiRequest<ReportGenerationData>() {
            @Override
            protected ReportGenerationData doRequest(long requestIndex) throws Exception {
                return remoteForm.getReportData(requestIndex, groupController.getGroupObject().getID(), true, getUserPreferences());
            }

            @Override
            public void onResponse(long requstIndex, ReportGenerationData generationData) throws Exception {
                Main.module.openInExcel(generationData);
            }
        });
    }

    public void okPressed() {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.okPressed(requestIndex);
            }
        });
    }

    public void closePressed() {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
                return remoteForm.closedPressed(requestIndex);
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

    public void block() {
        blocked = true;
    }

    public void unblock() {
        blocked = false;
    }

    public void changeShowing(boolean newShowing) {
        showing = newShowing;
    }

    private abstract class RmiCheckNullFormRequest<T> extends RmiRequest<T> {
        protected final T doRequest(long requestIndex) throws Exception {
            RemoteFormInterface form = remoteForm;
            if (form != null) {
                return doRequest(requestIndex, form);
            }
            return defaultValue();
        }

        protected abstract T defaultValue();

        protected abstract T doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception;
    }

    private abstract class RmiVoidRequest extends RmiCheckNullFormRequest<Void> {
        @Override
        protected Void defaultValue() { return null; }

        @Override
        protected Void doRequest(long requestIndex, RemoteFormInterface remoteForm) throws Exception {
            doExecute(requestIndex, remoteForm);
            return null;
        }

        protected abstract void doExecute(long requestIndex, RemoteFormInterface remoteForm) throws Exception;
    }

    private abstract class ProcessServerResponseRmiRequest extends RmiCheckNullFormRequest<ServerResponse> {
        @Override
        protected ServerResponse defaultValue() { return ServerResponse.EMPTY; }

        @Override
        protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
            processServerResponse(result);
        }
    }

    private static class ModifyObject {
        public final ClientObject object;
        public final boolean add;
        public final ClientGroupObjectValue value;

        private ModifyObject(ClientObject object, boolean add, ClientGroupObjectValue value) {
            this.object = object;
            this.add = add;
            this.value = value;
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
