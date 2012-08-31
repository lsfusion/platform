package platform.client.form;

import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import platform.base.*;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.cell.PanelView;
import platform.client.form.dispatch.ClientFormActionDispatcher;
import platform.client.form.dispatch.SimpleChangePropertyDispatcher;
import platform.client.form.tree.TreeGroupController;
import platform.client.logics.*;
import platform.client.logics.classes.ClientActionClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.form.*;

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

import static platform.base.BaseUtils.serializeObject;
import static platform.client.ClientResourceBundle.getString;
import static platform.interop.Order.*;

public class ClientFormController implements AsyncView {

    private final TableManager tableManager = new TableManager(this);

    private final EProvider<String> serverMessageProvider = new EProvider<String>() {
        @Override
        public String getExceptionally() throws Exception {
            return remoteForm.getRemoteActionMessage();
        }
    };

    private final RmiQueue rmiQueue = new RmiQueue(tableManager, serverMessageProvider, this);
    private final SimpleChangePropertyDispatcher simpleDispatcher = new SimpleChangePropertyDispatcher(this);

//    private RemoteFormInterface remoteForm;
    public RemoteFormInterface remoteForm;

    private final ClientForm form;
    private final ClientNavigator clientNavigator;
    private final ClientFormActionDispatcher actionDispatcher;

    // здесь хранится список всех GroupObjects плюс при необходимости null
//    private List<ClientGroupObject> groupObjects;

    private static IDGenerator idGenerator = new DefaultIDGenerator();
    private int ID;

    private ClientFormLayout formLayout;

    public Map<ClientGroupObject, GroupObjectController> controllers;
    public Map<ClientTreeGroup, TreeGroupController> treeControllers;

    public boolean dataChanged;

    private boolean defaultOrdersInitialized = false;

    private boolean isDialog;

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObject, List<ClientPropertyFilter>>();

    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();

    private static final ImageIcon loadingIcon = new ImageIcon(Main.class.getResource("/images/loading.gif"));

    public PanelView drawAsync;
    private Timer timer;
    private Icon prevIcon;
    public void onAsyncStarted() {
        if(drawAsync!=null) {
            timer = new Timer(Main.asyncTimeOut, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    prevIcon = drawAsync.getIcon();
                    drawAsync.setIcon(loadingIcon);
                    timer = null;
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
    public void onAsyncFinished() {
        if(drawAsync!=null) {
            if(timer!=null)
                timer.stop();
            else
                drawAsync.setIcon(prevIcon);
        }
    }

    public ClientFormController(RemoteFormInterface remoteForm, ClientNavigator clientNavigator) {
        this(remoteForm, clientNavigator, false);
    }

    public ClientFormController(RemoteFormInterface iremoteForm, ClientNavigator iclientNavigator, boolean iisDialog) {
        isDialog = iisDialog;

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

    public ClientFormLayout getComponent() {
        return formLayout;
    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //
    void initializeForm() throws Exception {
        formLayout = new ClientFormLayout(this, form.mainContainer);

        applyUserProperties();

        initializeControllers();

        initializeRegularFilters();

        initializeDefaultOrders();

        processRemoteChanges(false);
    }

    public List<ClientPropertyDraw> getPropertyDraws() {
        return form.getPropertyDraws();
    }

    private void initializeControllers() throws IOException {
        treeControllers = new HashMap<ClientTreeGroup, TreeGroupController>();
        for (ClientTreeGroup treeGroup : form.treeGroups) {
            TreeGroupController controller = new TreeGroupController(treeGroup, form, this, formLayout);
            treeControllers.put(treeGroup, controller);
        }

        controllers = new HashMap<ClientGroupObject, GroupObjectController>();

        for (ClientGroupObject group : form.groupObjects) {
            if (group.parent == null) {
                GroupObjectController controller = new GroupObjectController(group, form, this, formLayout);
                controllers.put(group, controller);
            }
        }

        for (ClientPropertyDraw properties : form.getPropertyDraws()) {
            if (properties.groupObject == null) {
                GroupObjectController controller = new GroupObjectController(null, form, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
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

    private void createMultipleFilterComponent(final ClientRegularFilterGroup filterGroup) {
        final JComboBox comboBox = new JComboBox();
        comboBox.addItem(new ClientRegularFilterWrapper(getString("form.all")));
        for (ClientRegularFilter filter : filterGroup.filters) {
            comboBox.addItem(new ClientRegularFilterWrapper(filter));
        }

        if (filterGroup.drawToToolbar()) {
            GroupObjectController controller = controllers.get(filterGroup.groupObject);
            controller.addFilterToToolbar(filterGroup, comboBox);
        }

        if (filterGroup.defaultFilter >= 0) {
            ClientRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
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

        for (final ClientRegularFilter filter : filterGroup.filters) {
            formLayout.addBinding(filter.key, "regularFilter" + filterGroup.getID() + filter.getID(), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    comboBox.setSelectedItem(new ClientRegularFilterWrapper(filter));
                }
            });
        }

        formLayout.add(filterGroup, comboBox);
    }

    private void createSingleFilterComponent(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter singleFilter) {
        final JCheckBox checkBox = new JCheckBox(singleFilter.getFullCaption());

        if (filterGroup.defaultFilter >= 0) {
            checkBox.setSelected(true);
        }

        if (filterGroup.drawToToolbar()) {
            GroupObjectController controller = controllers.get(filterGroup.groupObject);
            controller.addFilterToToolbar(filterGroup, checkBox);
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
        formLayout.add(filterGroup, checkBox);
        formLayout.addBinding(singleFilter.key, "regularFilter" + filterGroup.getID() + singleFilter.getID(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                checkBox.setSelected(!checkBox.isSelected());
            }
        });
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

    public void saveUserPreferences(final FormUserPreferences preferences, final Boolean forAllUsers) throws RemoteException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new RmiVoidRequest() {
            @Override
            protected void doExecute(long requestIndex) throws RemoteException {
                remoteForm.saveUserPreferences(requestIndex, preferences, forAllUsers);
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

    private void applyUserProperties() throws Exception {
        commitOrCancelCurrentEditing();

        //не посылаем в rmiQueue, т.к. ImmutableMethod и значит не будет rmi-call'а
        FormUserPreferences preferences = remoteForm.getUserPreferences();
        if (preferences != null) {
            for (ClientPropertyDraw property : form.getPropertyDraws()) {
                String propertySID = property.getSID();
                for (GroupObjectUserPreferences groupObjectPreferences : preferences.getGroupObjectUserPreferencesList()) {
                    Map<String, ColumnUserPreferences> columnUserPreferences = groupObjectPreferences.getColumnUserPreferences();
                    if (columnUserPreferences.containsKey(propertySID)) {
                        property.hideUser = columnUserPreferences.get(propertySID).isNeedToHide();
                        if (columnUserPreferences.get(propertySID).getWidthUser() != null) {
                            property.widthUser = columnUserPreferences.get(propertySID).getWidthUser();
                        }
                        if (columnUserPreferences.get(propertySID).getOrderUser() != null) {
                            property.orderUser = columnUserPreferences.get(propertySID).getOrderUser();
                        }
                        if (columnUserPreferences.get(propertySID).getSortUser() != null) {
                            property.sortUser = columnUserPreferences.get(propertySID).getSortUser();
                            property.ascendingSortUser = columnUserPreferences.get(propertySID).getAscendingSortUser();
                        }
                    }
                }
            }
            for (ClientGroupObject groupObject : form.groupObjects) {
                for (GroupObjectUserPreferences groupObjectPreferences : preferences.getGroupObjectUserPreferencesList()) {
                    if (groupObject.getSID().equals(groupObjectPreferences.groupObjectSID))
                        groupObject.hasUserPreferences = groupObjectPreferences.hasUserPreferences;
                }
            }
        }
    }

    private void initializeDefaultOrders() throws IOException {
        processRemoteChanges(false);
        try {
            //применяем все свойства по умолчанию
            applyOrders(form.defaultOrders);
            defaultOrdersInitialized = true;

            //применяем пользовательские свойства
            OrderedMap<ClientPropertyDraw, Boolean> userOrders = new OrderedMap<ClientPropertyDraw, Boolean>();
            for (GroupObjectController controller : controllers.values()) {
                boolean userPreferencesEmpty = true;
                if (controller.getGroupObject() != null && controller.getGroupObject().hasUserPreferences) {
                    List<ClientPropertyDraw> clientPropertyDrawList = controller.getPropertyDraws();
                    Collections.sort(clientPropertyDrawList, COMPARATOR_USERSORT);
                    for (ClientPropertyDraw property : controller.getPropertyDraws()) {
                        if (property.sortUser != null && property.ascendingSortUser != null) {
                            userOrders.put(property, property.ascendingSortUser);
                            userPreferencesEmpty = false;
                        }
                    }
                }
                if (userPreferencesEmpty)
                    controller.clearOrders();
            }
            applyOrders(userOrders);
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.cant.initialize.default.orders"));
        }
    }

    private void applyOrders(OrderedMap<ClientPropertyDraw, Boolean> orders) throws IOException {
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

    public void updateCurrentClass(int currentClassId) {
        if (clientNavigator != null) {
            clientNavigator.relevantClassNavigator.updateCurrentClass(currentClassId);
        }
    }

    public void processRemoteChanges(boolean async) throws IOException {
        rmiQueue.syncRequestWithTimeOut(async ? 0 : RmiQueue.FOREVER, new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.getRemoteChanges(requestIndex);
            }
        });
    }

    private final Map<ClientGroupObject, Long> lastChangeCurrentObjectsRequestIndices = Maps.newHashMap();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, Long> lastChangePropertyRequestIndices = HashBasedTable.create();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, Pair<Object, Object>> lastChangePropertyRequestValues = HashBasedTable.create();

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

    private final OrderedMap<Long, ModifyObject> lastModifyObjectRequests = new OrderedMap<Long, ModifyObject>();

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
                formLayout.getComponent().revalidate();
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

        for (Iterator<Map.Entry<ClientGroupObject, Long>> iterator = lastChangeCurrentObjectsRequestIndices.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ClientGroupObject, Long> entry = iterator.next();

            if (entry.getValue() > currentDispatchingRequestIndex) {
                formChanges.objects.remove(entry.getKey());
            } else {
                iterator.remove();
            }
        }
    }

    private void modifyFormChangesWithChangePropertyAsyncs(ClientFormChanges formChanges) {
        long currentDispatchingRequestIndex = rmiQueue.getCurrentDispatchingRequestIndex();

        for (Iterator<Table.Cell<ClientPropertyDraw, ClientGroupObjectValue, Long>> iterator = lastChangePropertyRequestIndices.cellSet().iterator(); iterator.hasNext(); ) {
            Table.Cell<ClientPropertyDraw, ClientGroupObjectValue, Long> cell = iterator.next();
            if (cell.getValue() <= currentDispatchingRequestIndex) {
                iterator.remove();

                ClientPropertyDraw propertyDraw = cell.getRowKey();
                ClientGroupObjectValue keys = cell.getColumnKey();
                Pair<Object, Object> change = lastChangePropertyRequestValues.remove(propertyDraw, keys);

                Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(propertyDraw);
                if(propertyValues==null) { // включаем изменение на старое значение, если ответ с сервера пришел, а новое значение нет
                    propertyValues = new HashMap<ClientGroupObjectValue, Object>();
                    formChanges.properties.put(propertyDraw, propertyValues);
                    formChanges.updateProperties.add(propertyDraw);
                }

                if(formChanges.updateProperties.contains(propertyDraw) && !propertyValues.containsKey(keys))
                    propertyValues.put(keys, change.second);
            }
        }

        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Pair<Object, Object>>> e : lastChangePropertyRequestValues.rowMap().entrySet()) {
            Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(e.getKey());
            if (propertyValues != null) {
                for (Map.Entry<ClientGroupObjectValue, Pair<Object, Object>> keyValue : e.getValue().entrySet()) {
                    propertyValues.put(keyValue.getKey(), keyValue.getValue().first);
                }
            }
        }
    }

    private void modifyFormChangesWithModifyObjectAsyncs(ClientFormChanges formChanges) {
        long currentDispatchingRequestIndex = rmiQueue.getCurrentDispatchingRequestIndex();

        for (Iterator<Map.Entry<Long,ModifyObject>> iterator = lastModifyObjectRequests.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, ModifyObject> cell = iterator.next();
            if(cell.getKey() <= currentDispatchingRequestIndex)
                iterator.remove();
        }

        for (Map.Entry<Long, ModifyObject> e : lastModifyObjectRequests.entrySet()) {
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
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.expandGroupObject(requestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void collapseGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.collapseGroupObject(requestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject groupObject, final Scroll changeType) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeGroupObject(requestIndex, groupObject.getID(), changeType.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        if (objectValue == null || remoteForm == null) {
            // remoteForm может равняться null, если к моменту вызова форму уже закрыли
            return;
        }

        rmiQueue.asyncRequest(
                new RmiRequest<ServerResponse>() {
                    @Override
                    public void onAsyncRequest(long requestIndex) {
//                        System.out.println("!!Async changing group object with req#: " + requestIndex + " on " + objectValue);
                        lastChangeCurrentObjectsRequestIndices.put(group, requestIndex);
                    }

                    @Override
                    protected ServerResponse doRequest(long requestIndex) throws Exception {
                        return remoteForm.changeGroupObject(requestIndex, group.getID(), objectValue.serialize());
                    }

                    @Override
                    protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                        processServerResponse(result);
                    }
                }
        );
    }

    private byte[] getFullCurrentKey(ClientPropertyDraw property, ClientGroupObjectValue columnKey) throws IOException {
        final ClientGroupObjectValue fullCurrentKey = getFullCurrentKey();
        fullCurrentKey.putAll(columnKey);

        return fullCurrentKey.serialize();
    }

    public void changeProperty(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, final Object value, final Object oldValue) throws IOException {
        assert !isEditing();

        commitOrCancelCurrentEditing();

        final byte[] fullCurrentKey = getFullCurrentKey(property, columnKey); // чтобы не изменился

        rmiQueue.syncRequestWithTimeOut(0, new RmiRequest<ServerResponse>() {
            ClientGroupObjectValue propertyKey = null;

            @Override
            protected void onAsyncRequest(long requestIndex) {
//                System.out.println("!!Async changing property with req#: " + requestIndex);
//                ExceptionUtils.dumpStack();
//                System.out.println("------------------------");
                GroupObjectController controller = controllers.get(property.groupObject);

                propertyKey = controller != null && !controller.panelProperties.contains(property) ? new ClientGroupObjectValue(controller.getCurrentObject(), columnKey) : columnKey;

                lastChangePropertyRequestIndices.put(property, propertyKey, requestIndex);
                lastChangePropertyRequestValues.put(property, propertyKey, new Pair<Object, Object>(value, oldValue));
            }

            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeProperty(requestIndex, property.getID(), fullCurrentKey, serializeObject(value), null);
            }

            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());
                processServerResponse(result);
            }
        });
    }

    public boolean isAsyncModifyObject(ClientPropertyDraw property) {
        return property.addRemove!=null && controllers.get(property.addRemove.first.groupObject).classView==ClassViewType.GRID;
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

        final byte[] fullCurrentKey = getFullCurrentKey(property, columnKey); // чтобы не изменился

        rmiQueue.syncRequestWithTimeOut(Main.asyncTimeOut, new RmiRequest<ServerResponse>() {
            @Override
            protected void onAsyncRequest(long requestIndex) {
                controller.modifyGroupObject(value, add); // сначала посылаем запрос, так как getFullCurrentKey может измениться

                lastChangeCurrentObjectsRequestIndices.put(object.groupObject, requestIndex); // так как по сути такой execute сам меняет groupObject
                lastModifyObjectRequests.put(requestIndex, new ModifyObject(object, add, value));
            }

            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeProperty(requestIndex, property.getID(), fullCurrentKey, null, add ? serializeObject(ID) : null);
            }

            @Override
            protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
                processServerResponse(result);
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

    public ServerResponse executeEditAction(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, final String actionSID) throws IOException {
        commitOrCancelCurrentEditing();

        SwingUtils.commitDelayedGroupObjectChange(property.getGroupObject());

        return rmiQueue.syncRequest(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
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

    public ServerResponse throwInServerInvocation(Exception ex) throws RemoteException {
        BusyDisplayer busyDisplayer = new BusyDisplayer(serverMessageProvider);
        busyDisplayer.start();
        try {
            return remoteForm.throwInServerInvocation(ex);
        } finally {
            busyDisplayer.stop();
        }
    }

    public void gainedFocus() {
        //remoteForm может быть == null, если сработал closed, и тогда ничего вызывать не надо
        if (!isEditing() && remoteForm != null) {
            try {
                rmiQueue.asyncRequest(new RmiVoidRequest() {
                    @Override
                    protected void doExecute(long requestIndex) throws Exception {
                        remoteForm.gainedFocus(requestIndex);
                    }
                });

                if (clientNavigator != null) {
                    clientNavigator.relevantFormNavigator.currentFormChanged();
                }

                // если вдруг изменились данные в сессии
                ClientExternalScreen.invalidate(getID());
                ClientExternalScreen.repaintAll(getID());
            } catch (Exception e) {
                throw new RuntimeException(getString("form.error.form.activation"), e);
            }
        }
    }

    public void setTabVisible(final ClientContainer container, final ClientComponent component) throws IOException {
        rmiQueue.syncRequestWithTimeOut(Main.asyncTimeOut, new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.setTabVisible(requestIndex, container.getID(), component.getID());
            }
        });
    }

    public void pasteExternalTable(List<ClientPropertyDraw> propertyList, final List<List<Object>> table) throws IOException {
        final List<Integer> propertyIdList = new ArrayList<Integer>();
        for (ClientPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.getID());
        }
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.pasteExternalTable(requestIndex, propertyIdList, table);
            }
        });
    }

    public void pasteMulticellValue(Map<ClientPropertyDraw, List<ClientGroupObjectValue>> cells, final Object value) throws IOException {
        final Map<Integer, List<Map<Integer, Object>>> reCells = new HashMap<Integer, List<Map<Integer, Object>>>();
        for (ClientPropertyDraw property : cells.keySet()) {
            List<Map<Integer, Object>> keys = new ArrayList<Map<Integer, Object>>();
            for (ClientGroupObjectValue groupObjectValue : cells.get(property)) {
                Map<Integer, Object> key = new HashMap<Integer, Object>();
                for (ClientObject object : groupObjectValue.keySet()) {
                    key.put(object.getID(), groupObjectValue.get(object));
                }
                keys.add(key);
            }
            reCells.put(property.getID(), keys);
        }
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.pasteMulticellValue(requestIndex, reCells, value);
            }
        });
    }

    public void changeGridClass(final ClientObject object, final ClientObjectClass cls) throws IOException {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
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
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeClassView(requestIndex, groupObject.getID(), show);
            }
        });
    }

    public void changePropertyOrder(final ClientPropertyDraw property, final Order modiType, final ClientGroupObjectValue columnKey) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
                @Override
                protected ServerResponse doRequest(long requestIndex) throws Exception {
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
                protected ServerResponse doRequest(long requestIndex) throws Exception {
                    return remoteForm.clearPropertyOrders(requestIndex, groupObject.getID());
                }
            });
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void changeFind(List<ClientPropertyFilter> conditions) {
    }

    public void changeFilter(ClientGroupObject groupObject, List<ClientPropertyFilter> conditions) throws IOException {
        currentFilters.put(groupObject, conditions);
        applyCurrentFilters();
    }

    public void changeFilter(ClientTreeGroup treeGroup, List<ClientPropertyFilter> conditions) throws IOException {
        Map<ClientGroupObject, List<ClientPropertyFilter>> filters = BaseUtils.groupList(new BaseUtils.Group<ClientGroupObject, ClientPropertyFilter>() {
            public ClientGroupObject group(ClientPropertyFilter key) {
                return key.groupObject;
            }
        }, conditions);

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
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.setUserFilters(requestIndex, filters.toArray(new byte[filters.size()][]));
            }
        });
    }

    private void setRegularFilter(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter filter) throws IOException {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
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
        if (!tableManager.isEditing()) {
            rmiQueue.asyncRequest(new ProcessServerResponseRmiRequest() {
                @Override
                protected ServerResponse doRequest(long requestIndex) throws Exception {
                    return remoteForm.changePageSize(requestIndex, groupObject.getID(), pageSize);
                }
            });
        }
    }

    public void moveGroupObject(final ClientGroupObject parentGroup, final ClientGroupObjectValue parentKey, final ClientGroupObject childGroup, final ClientGroupObjectValue childKey, final int index) throws IOException {
        commitOrCancelCurrentEditing();

        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.moveGroupObject(requestIndex, parentGroup.getID(), parentKey.serialize(), childGroup.getID(), childKey.serialize(), index);
            }
        });
    }

    public void dropLayoutCaches() {
        formLayout.dropCaches();
    }

    public void closed() {
        // здесь мы сбрасываем ссылку на remoteForm для того, чтобы сборщик мусора быстрее собрал удаленные объекты
        // это нужно, чтобы connection'ы на сервере закрывались как можно быстрее
        remoteForm = null;
    }

    public Dimension calculatePreferredSize() {
        return formLayout.calculatePreferredSize();
    }

    public Boolean needToHideProperty(ClientPropertyDraw property){
        return property.hideUser == null ? property.hide : property.hideUser;
    }

    public FormUserPreferences getUserPreferences() {
        List<GroupObjectUserPreferences> groupObjectUserPreferencesList = new ArrayList<GroupObjectUserPreferences>();
        for (GroupObjectController controller : controllers.values()) {
            Map<String, ColumnUserPreferences> columnPreferences = new HashMap<String, ColumnUserPreferences>();
            for (ClientPropertyDraw property : controller.getPropertyDraws()) {
                columnPreferences.put(property.getSID(), new ColumnUserPreferences(needToHideProperty(property), property.widthUser, property.orderUser, property.sortUser, property.ascendingSortUser));
            }
            groupObjectUserPreferencesList.add(new GroupObjectUserPreferences(columnPreferences, controller.getGroupObject().getSID(), controller.getGroupObject().hasUserPreferences));
        }
        return new FormUserPreferences(groupObjectUserPreferencesList);
    }

    public void hideForm() {
        //do nothing by default
    }

    public void runPrintReport() {
        assert Main.module.isFull();

        try {
            rmiQueue.syncRequest(new RmiRequest<ReportGenerationData>() {
                @Override
                protected ReportGenerationData doRequest(long requestIndex) throws Exception {
                    return remoteForm.getReportData(requestIndex, null, false, getUserPreferences());
                }

                @Override
                public void onResponse(long requestIndex, ReportGenerationData generationData) throws Exception {
                    Main.frame.runReport(remoteForm.getSID(), false, generationData);
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
                    Main.frame.runReport("SingleGroupReport_" + remoteForm.getSID(), false, generationData);
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
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.okPressed(requestIndex);
            }
        });
    }

    void closePressed() {
        commitOrCancelCurrentEditing();
        rmiQueue.syncRequest(new ProcessServerResponseRmiRequest() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.closedPressed(requestIndex);
            }
        });
    }

    private abstract class ProcessServerResponseRmiRequest extends RmiRequest<ServerResponse> {
        @Override
        protected void onResponse(long requestIndex, ServerResponse result) throws Exception {
            processServerResponse(result);
        }
    }

    private static Comparator<ClientPropertyDraw> COMPARATOR_USERSORT = new Comparator<ClientPropertyDraw>() {
        public int compare(ClientPropertyDraw c1, ClientPropertyDraw c2) {
            if (c1.ascendingSortUser != null && c2.ascendingSortUser != null)
                return c1.sortUser - c2.sortUser;
            else return 0;
        }
    };
}
