package platform.client.form;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.*;
import platform.base.BaseUtils;
import platform.base.Callback;
import platform.base.EProvider;
import platform.base.OrderedMap;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.form.dispatch.ClientFormActionDispatcher;
import platform.client.form.dispatch.SimpleChangePropertyDispatcher;
import platform.client.form.tree.TreeGroupController;
import platform.client.logics.*;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.form.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
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

public class ClientFormController {

    private final TableManager tableManager = new TableManager(this);
    private final RmiQueue rmiQueue = new RmiQueue(tableManager, new EProvider<String>() {
        @Override
        public String getExceptionally() throws Exception {
            return remoteForm.getRemoteActionMessage();
        }
    });
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

        applyRemoteChanges();
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

        rmiQueue.syncRequest(new RmiRequest<FormUserPreferences>() {
            @Override
            protected FormUserPreferences doRequest(long requestIndex) throws Exception {
                return remoteForm.getUserPreferences();
            }

            @Override
            public void onResponse(long requestIndex, FormUserPreferences preferences) throws Exception {
                if (preferences != null) {
                    for (ClientPropertyDraw property : form.getPropertyDraws()) {
                        String propertySID = property.getSID();
                        if (preferences.getFormColumnUserPreferences().containsKey(propertySID)) {
                            property.hideUser = preferences.getFormColumnUserPreferences().get(propertySID).isNeedToHide();
                            if (preferences.getFormColumnUserPreferences().get(propertySID).getWidthUser() != null) {
                                property.widthUser = preferences.getFormColumnUserPreferences().get(propertySID).getWidthUser();
                            }
                        }
                    }
                }
            }
        });
    }

    private void initializeDefaultOrders() throws IOException {
        //сначала получаем изменения, чтобы был первоначальный список свойств в таблице
        applyRemoteChanges();
        try {
            // Применяем порядки по умолчанию
            applyOrders(form.defaultOrders);
            defaultOrdersInitialized = true;
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

    public void applyRemoteChanges() throws IOException {
        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.getRemoteChanges(requestIndex);
            }
        });
    }

    private final Map<ClientGroupObject, Long> lastChangeCurrentObjectsRequestIndices = Maps.newHashMap();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, Long> lastChangePropertyRequestIndices = HashBasedTable.create();
    private final Table<ClientPropertyDraw, ClientGroupObjectValue, Optional<Object>> lastChangePropertyRequestValues = HashBasedTable.create();
    public void applyFormChanges(byte[] bFormChanges) throws IOException {
        if (bFormChanges == null) {
            return;
        }

        ClientFormChanges formChanges = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(bFormChanges)), form, controllers);

        for (Map.Entry<ClientGroupObject, ClassViewType> entry : formChanges.classViews.entrySet()) {
            ClassViewType classView = entry.getValue();
            if (classView != ClassViewType.GRID) {
                currentGridObjects.remove(entry.getKey());
            }
        }
        currentGridObjects.putAll(formChanges.gridObjects);

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
        for (Iterator<Map.Entry<ClientGroupObject, Long>> iterator = lastChangeCurrentObjectsRequestIndices.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<ClientGroupObject, Long> entry = iterator.next();

            if (entry.getValue() > rmiQueue.getCurrentDispatchingRequestIndex()) {
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
                lastChangePropertyRequestValues.remove(cell.getRowKey(), cell.getColumnKey());
            }
        }

        for (Map.Entry<ClientPropertyDraw, Map<ClientGroupObjectValue, Optional<Object>>> e : lastChangePropertyRequestValues.rowMap().entrySet()) {
            Map<ClientGroupObjectValue, Object> propertyValues = formChanges.properties.get(e.getKey());
            if (propertyValues != null) {
                for (Map.Entry<ClientGroupObjectValue, Optional<Object>> keyValue : e.getValue().entrySet()) {
                    propertyValues.put(keyValue.getKey(), keyValue.getValue().orNull());
                }
            }
        }
    }

    public void expandGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.expandGroupObject(requestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void collapseGroupObject(final ClientGroupObject group, final ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.collapseGroupObject(requestIndex, group.getID(), objectValue.serialize());
            }
        });
    }

    public void changeGroupObject(final ClientGroupObject groupObject, final Scroll changeType) throws IOException {
        commitOrCancelCurrentEditing();

        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
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
                    public void preRequest(long requestIndex) {
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

    public void changeProperty(final ClientPropertyDraw property, final ClientGroupObjectValue columnKey, final Object value) throws IOException {
        assert !isEditing();

        final ClientGroupObjectValue fullCurrentKey = getFullCurrentKey();
        fullCurrentKey.putAll(columnKey);

        commitOrCancelCurrentEditing();

        rmiQueue.asyncRequest(new RmiRequest<ServerResponse>() {
            ClientGroupObjectValue propertyKey = null;

            @Override
            protected void preRequest(long requestIndex) {
                GroupObjectController controller = controllers.get(property.groupObject);

                propertyKey = controller != null ? new ClientGroupObjectValue(controller.getCurrentObject(), columnKey) : columnKey;

                lastChangePropertyRequestIndices.put(property, propertyKey, requestIndex);
                lastChangePropertyRequestValues.put(property, propertyKey, Optional.fromNullable(value));
            }

            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeProperty(requestIndex, property.getID(), fullCurrentKey.serialize(), serializeObject(value));
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

        // для глобальных свойств пока не может быть отложенных действий
        if (property.getGroupObject() != null) {
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);
        }

        return syncGetServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.executeEditAction(requestIndex, property.getID(), columnKey.serialize(), actionSID);
            }
        });
    }

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        return remoteForm.continueServerInvocation(actionResults);
    }

    public ServerResponse throwInServerInvocation(Exception ex) throws RemoteException {
        return remoteForm.throwInServerInvocation(ex);
    }

    public void gainedFocus() {
        //remoteForm может быть == null, если сработал closed, и тогда ничего вызывать не надо
        if (!isEditing() && remoteForm != null) {
            if (remoteForm == null) {
                return;
            }

            try {
                rmiQueue.syncRequest(new RmiVoidRequest() {
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
        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
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
        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
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
        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.pasteMulticellValue(requestIndex, reCells, value);
            }
        });
    }

    public void changeGridClass(final ClientObject object, final ClientObjectClass cls) throws IOException {
        commitOrCancelCurrentEditing();

        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeGridClass(requestIndex, object.getID(), cls.ID);
            }
        });
    }

    public void switchClassView(ClientGroupObject groupObject) throws IOException {
        ClassViewType newClassView = ClassViewType.switchView(controllers.get(groupObject).classView);
        changeClassView(groupObject, newClassView);
    }

    public void changeClassView(final ClientGroupObject groupObject, final ClassViewType show) throws IOException {
        commitOrCancelCurrentEditing();

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.changeClassView(requestIndex, groupObject.getID(), show);
            }
        });
    }

    public void changePropertyOrder(final ClientPropertyDraw property, final Order modiType, final ClientGroupObjectValue columnKey) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            syncProcessServerResponse(new RmiRequest<ServerResponse>() {
                @Override
                protected ServerResponse doRequest(long requestIndex) throws Exception {
                    return remoteForm.changePropertyOrder(requestIndex, property.getID(), modiType.serialize(), columnKey.serialize());
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

        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
            @Override
            protected ServerResponse doRequest(long requestIndex) throws Exception {
                return remoteForm.setUserFilters(requestIndex, filters.toArray(new byte[filters.size()][]));
            }
        });
    }

    private void setRegularFilter(final ClientRegularFilterGroup filterGroup, final ClientRegularFilter filter) throws IOException {
        commitOrCancelCurrentEditing();

        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
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
            syncProcessServerResponse(new RmiRequest<ServerResponse>() {
                @Override
                protected ServerResponse doRequest(long requestIndex) throws Exception {
                    return remoteForm.changePageSize(requestIndex, groupObject.getID(), pageSize);
                }
            });
        }
    }

    public void moveGroupObject(final ClientGroupObject parentGroup, final ClientGroupObjectValue parentKey, final ClientGroupObject childGroup, final ClientGroupObjectValue childKey, final int index) throws IOException {
        commitOrCancelCurrentEditing();

        syncProcessServerResponse(new RmiRequest<ServerResponse>() {
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
        Map<String, FormColumnUserPreferences> columnPreferences = new HashMap<String, FormColumnUserPreferences>();
        for (GroupObjectController controller : controllers.values()) {
            for (ClientPropertyDraw property : controller.getPropertyDraws()) {
                columnPreferences.put(property.getSID(), new FormColumnUserPreferences(needToHideProperty(property), property.widthUser));
            }
        }
        return new FormUserPreferences(columnPreferences);
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
        try {
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
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public void okPressed() {
        commitOrCancelCurrentEditing();
        try {
            syncProcessServerResponse(new RmiRequest<ServerResponse>() {
                @Override
                protected ServerResponse doRequest(long requestIndex) throws Exception {
                    return remoteForm.okPressed(requestIndex);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.closing.dialog"), e);
        }
    }

    void closePressed() {
        commitOrCancelCurrentEditing();
        try {
            syncProcessServerResponse(new RmiRequest<ServerResponse>() {
                @Override
                protected ServerResponse doRequest(long requestIndex) throws Exception {
                    return remoteForm.closedPressed(requestIndex);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.closing.dialog"), e);
        }
    }

    private ServerResponse syncGetServerResponse(final RmiRequest<ServerResponse> request) {
        return rmiQueue.syncRequest(request);
    }

    private void syncProcessServerResponse(final RmiRequest<ServerResponse> request) throws IOException {
        processServerResponse(syncGetServerResponse(request));
    }

    private void asyncProcessServerResponse(final RmiRequest<ServerResponse> request) throws IOException {
        rmiQueue.asyncRequest(request, new Callback<ServerResponse>() {
            @Override
            public void done(ServerResponse result) throws Exception {
                processServerResponse(result);
            }
        });
    }
}
