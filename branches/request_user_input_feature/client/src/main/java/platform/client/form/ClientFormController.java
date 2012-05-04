package platform.client.form;

import com.google.common.base.Throwables;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IDGenerator;
import platform.client.Log;
import platform.client.Main;
import platform.client.StartupProperties;
import platform.client.SwingUtils;
import platform.client.form.queries.ToolbarGridButton;
import platform.client.form.tree.TreeGroupController;
import platform.client.logics.*;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.client.remote.proxy.RemoteObjectProxy;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.CheckFailed;
import platform.interop.action.ClientAction;
import platform.interop.form.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

import static platform.client.ClientResourceBundle.getString;
import static platform.interop.Order.*;

public class ClientFormController {
    private final TableManager tableManager = new TableManager(this);
    private final SimpleEditPropertyDispatcher simpleDispatcher = new SimpleEditPropertyDispatcher(this);

    private RemoteFormInterface remoteForm;
    private boolean busy = false;

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

    private JButton buttonApply;
    private JButton buttonCancel;

    public boolean dataChanged;

    private boolean canClose = true;

    private Color defaultApplyBackground;

    private boolean defaultOrdersInitialized = false;

    private boolean isDialog;
    private boolean isModal;
    private boolean isNewSession;

    private final Map<ClientGroupObject, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObject, List<ClientPropertyFilter>>();

    public final Map<ClientGroupObject, List<ClientGroupObjectValue>> currentGridObjects = new HashMap<ClientGroupObject, List<ClientGroupObjectValue>>();

    public ClientFormController(RemoteFormInterface remoteForm, ClientNavigator clientNavigator) throws IOException, ClassNotFoundException {
        this(remoteForm, clientNavigator, false, false, true);
    }

    public ClientFormController(RemoteFormInterface remoteForm, ClientNavigator clientNavigator, boolean isDialog, boolean isModal, boolean isNewSession) throws IOException, ClassNotFoundException {
        this.isDialog = isDialog;
        this.isModal = isModal;
        this.isNewSession = isNewSession;

        ID = idGenerator.idShift();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        this.remoteForm = remoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        this.clientNavigator = clientNavigator;

        actionDispatcher = new ClientFormActionDispatcher(this);

        form = new ClientSerializationPool().deserializeObject(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));
//        form = new ClientForm(new DataInputStream(new ByteArrayInputStream(remoteForm.getRichDesignByteArray())));

        if (remoteForm instanceof RemoteObjectProxy) {
            ((RemoteObjectProxy) remoteForm).blockedScreen = form.blockedScreen;
        }

        initializeForm();
    }

    public boolean isDialog() {
        return isDialog;
    }

    public int getID() {
        return ID;
    }

    void setBusy(boolean busy) {
        this.busy = busy;
    }

    boolean isBusy() {
        return busy;
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
    void initializeForm() throws IOException {
        formLayout = new ClientFormLayout(this, form.mainContainer);

        applyUserProperties();

        initializeControllers();

        initializeButtons();

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
        comboBox.addItem(new ClientRegularFilterWrapped(getString("form.all")));
        for (ClientRegularFilter filter : filterGroup.filters) {
            comboBox.addItem(new ClientRegularFilterWrapped(filter));
        }

        if (filterGroup.drawToToolbar()) {
            GroupObjectController controller = controllers.get(filterGroup.groupObject);
            controller.addFilterToToolbar(filterGroup, comboBox);
        }

        if (filterGroup.defaultFilter >= 0) {
            ClientRegularFilter defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
            comboBox.setSelectedItem(new ClientRegularFilterWrapped(defaultFilter));
        }
        comboBox.addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(ItemEvent e) {
                try {
                    setRegularFilter(filterGroup, ((ClientRegularFilterWrapped) e.getItem()).filter);
                } catch (IOException ioe) {
                    throw new RuntimeException(getString("form.error.changing.regular.filter"), ioe);
                }
            }
        });

        for (final ClientRegularFilter filter : filterGroup.filters) {
            formLayout.addBinding(filter.key, "regularFilter" + filterGroup.getID() + filter.getID(), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    comboBox.setSelectedItem(new ClientRegularFilterWrapped(filter));
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
                    if (ie.getStateChange() == ItemEvent.SELECTED)
                        setRegularFilter(filterGroup, singleFilter);
                    if (ie.getStateChange() == ItemEvent.DESELECTED)
                        setRegularFilter(filterGroup, null);
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

    public void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException {
        commitOrCancelCurrentEditing();
        remoteForm.saveUserPreferences(preferences, forAllUsers);
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

    public boolean isEditing() {
        return tableManager.isEditing();
    }

    public SimpleEditPropertyDispatcher getSimpleDispatcher() {
        return simpleDispatcher;
    }

    private void initializeButtons() throws RemoteException {
        KeyStroke printKeyStroke = KeyStrokes.getPrintKeyStroke();
        KeyStroke editKeyStroke = KeyStrokes.getEditKeyStroke();
        KeyStroke xlsKeyStroke = KeyStrokes.getXlsKeyStroke();
        KeyStroke nullKeyStroke = KeyStrokes.getNullKeyStroke();
        KeyStroke refreshKeyStroke = KeyStrokes.getRefreshKeyStroke();
        KeyStroke okKeyStroke = KeyStrokes.getApplyKeyStroke(isDialog() ? 0 : InputEvent.CTRL_DOWN_MASK);
        KeyStroke closeKeyStroke = KeyStrokes.getCancelKeyStroke(true);
        KeyStroke applyKeyStroke = KeyStrokes.getApplyKeyStroke(InputEvent.ALT_DOWN_MASK);
        KeyStroke cancelKeyStroke = KeyStrokes.getCancelKeyStroke(!isModal);

        // Добавляем стандартные кнопки

        if (Main.module.isFull() && !isDialog()) {
            addClientFunction(form.getPrintFunction(), printKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    printPressed();
                }
            });

            String isDebug = System.getProperty(StartupProperties.PLATFORM_CLIENT_ISDEBUG);
            if(isDebug != null && isDebug.equals("true"))
                addClientFunction(form.getEditFunction(), editKeyStroke, new AbstractAction() {
                    public void actionPerformed(ActionEvent ae) {
                        editPressed();
                    }
                });

            addClientFunction(form.getXlsFunction(), xlsKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    xlsPressed();
                }
            });
        }

        addClientFunction(form.getRefreshFunction(), refreshKeyStroke, new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                refreshPressed();
            }
        });

        if (isDialog) {
            addClientFunction(form.getNullFunction(), nullKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    nullPressed();
                }
            });
        }

        if (isNewSession) {
            buttonApply = addClientFunction(form.getApplyFunction(), applyKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    applyPressed();
                }
            });
            if (buttonApply != null) {
                buttonApply.setEnabled(false);
            }

            buttonCancel = addClientFunction(form.getCancelFunction(), cancelKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    cancelPressed();
                }
            });
            if (buttonCancel != null) {
                buttonCancel.setEnabled(false);
            }
        }

        if (isModal) {
            addClientFunction(form.getOkFunction(), okKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    okPressed();
                }
            });

            addClientFunction(form.getCloseFunction(), closeKeyStroke, new AbstractAction() {
                public void actionPerformed(ActionEvent ae) {
                    closePressed();
                }
            });
        }
    }

    private void applyUserProperties() throws RemoteException {
        commitOrCancelCurrentEditing();
        FormUserPreferences preferences = remoteForm.loadUserPreferences();

        if (preferences != null)
            for (ClientPropertyDraw property : form.getPropertyDraws()) {
                String propertySID = property.getSID();
                if (preferences.getFormColumnUserPreferences().containsKey(propertySID)) {
                    property.hideUser = preferences.getFormColumnUserPreferences().get(propertySID).isNeedToHide();
                    if (preferences.getFormColumnUserPreferences().get(propertySID).getWidthUser() != null)
                        property.widthUser = preferences.getFormColumnUserPreferences().get(propertySID).getWidthUser();
                }
            }
    }

    private JButton addClientFunction(ClientFunction function, final KeyStroke keyStroke, final AbstractAction functionAction) {
        if (function.visible) {
            JButton actionButton;
            String caption = function.caption + " (" + SwingUtils.getKeyStrokeCaption(keyStroke) + ")";
            if (function.iconPath == null) {
                functionAction.putValue(Action.NAME, caption);
                actionButton = new JButton(functionAction);
            } else {
                actionButton = new ToolbarGridButton(function.iconPath, caption) {
                    @Override
                    public void addListener() {
                        addActionListener(functionAction);
                    }
                };
                Dimension buttonSize = new Dimension(28, 28);
                actionButton.setMaximumSize(buttonSize);
                actionButton.setPreferredSize(buttonSize);
            }
            actionButton.setFocusable(false);

            formLayout.add(function, actionButton);
            formLayout.addBinding(keyStroke, function.type + "FunctionAction", functionAction);
            return actionButton;
        }
        return null;
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

    private void processRemoteChanges(RemoteChanges remoteChanges) throws IOException {
        if (remoteChanges == null) {
            //ХАК: это теоретически возможно, при реконнекте, когда RMI-поток убивается и remote-method возвращает null
            return;
        }

        do {
            if (remoteChanges.formChanges != null) {
                applyFormChanges(new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(remoteChanges.formChanges)), form, controllers));
            }

            if (clientNavigator != null) {
                clientNavigator.relevantClassNavigator.updateCurrentClass(remoteChanges.classID);
            }

            Object[] actionResults = actionDispatcher.dispatchActions(remoteChanges.actions);

            if (!remoteChanges.resumeInvocation) {
                break;
            }

            remoteChanges = remoteForm.continueRemoteChanges(actionResults);
            remoteForm.refreshData();
        } while (true);
    }

    public void applyRemoteChanges() throws IOException {
        processRemoteChanges(remoteForm.getRemoteChanges());
    }

    private void applyFormChanges(ClientFormChanges formChanges) {
        if (formChanges.dataChanged != null) {
            dataChanged = formChanges.dataChanged;
            if (buttonApply != null) {
                Color defaultBackGround = getDefaultApplyBackground();
                buttonApply.setBackground(dataChanged ? Color.green : defaultBackGround);
                buttonApply.setEnabled(dataChanged);
                if (buttonCancel != null) {
                    buttonCancel.setEnabled(dataChanged);
                }
            }
        }

        for (Map.Entry<ClientGroupObject, ClassViewType> entry : formChanges.classViews.entrySet()) {
            ClassViewType classView = entry.getValue();
            if (classView != ClassViewType.GRID) {
                currentGridObjects.remove(entry.getKey());
            }
        }
        currentGridObjects.putAll(formChanges.gridObjects);

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

    private Color getDefaultApplyBackground() {
        if (defaultApplyBackground == null) {
            defaultApplyBackground = buttonApply.getBackground();
        }
        return defaultApplyBackground;
    }

    public void changeGroupObject(ClientGroupObject group, ClientGroupObjectValue objectValue) throws IOException {
        if (objectValue == null || remoteForm == null) {
            // remoteForm может равняться null, если к моменту вызова форму уже закрыли
            return;
        }

        if (group.parent != null || !objectValue.equals(controllers.get(group).getCurrentObject())) {
            commitOrCancelCurrentEditing();
            SwingUtils.stopSingleAction(group.getActionID(), false);

            // если ClientGroupObject в дереве, то вызывать не надо изменение объекта
            if (group.parent == null) {
                controllers.get(group).setCurrentGroupObject(objectValue);
            }
            remoteForm.changeGroupObject(group.getID(), objectValue.serialize());

            applyRemoteChanges();
        }
    }

    public void changeGroupObject(ClientGroupObject groupObject, Scroll changeType) throws IOException {
        commitOrCancelCurrentEditing();
        SwingUtils.stopSingleAction(groupObject.getActionID(), false);

        remoteForm.changeGroupObject(groupObject.getID(), changeType.serialize());
        applyRemoteChanges();
    }

    public void expandGroupObject(ClientGroupObject group, ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        remoteForm.expandGroupObject(group.getID(), objectValue.serialize());

        applyRemoteChanges();
    }

    public void collapseGroupObject(ClientGroupObject group, ClientGroupObjectValue objectValue) throws IOException {
        commitOrCancelCurrentEditing();
        remoteForm.collapseGroupObject(group.getID(), objectValue.serialize());
        applyRemoteChanges();
    }

    public void changePropertyDraw(ClientPropertyDraw property, ClientGroupObjectValue columnKey, Object value, boolean all, boolean aggValue) throws IOException {
        assert false:"changePropertyDraw shouldn't be called";

        // для глобальных свойств пока не может быть отложенных действий
        if (property.getGroupObject() != null) {
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);
        }

        processRemoteChanges(
                remoteForm.changePropertyDraw(property.getID(), columnKey.serialize(), BaseUtils.serializeObject(value), all, aggValue)
        );
    }

    public EditActionResult executeEditAction(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        commitOrCancelCurrentEditing();

        // для глобальных свойств пока не может быть отложенных действий
        if (property.getGroupObject() != null) {
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);
        }
        return remoteForm.executeEditAction(property.getID(), columnKey.serialize(), actionSID);
    }

    public EditActionResult continueExecuteEditAction(UserInputResult inputResult) throws RemoteException {
        return remoteForm.continueExecuteEditAction(inputResult);
    }

    public EditActionResult continueExecuteEditAction(Object[] actionResults) throws RemoteException {
        return remoteForm.continueExecuteEditAction(actionResults);
    }

    public RemoteDialogInterface createChangeEditorDialog(ClientPropertyDraw property) throws RemoteException {
        assert false;
        return remoteForm.createChangeEditorDialog(property.getID());
    }

    public RemoteDialogInterface createObjectEditorDialog(ClientPropertyDraw property) throws RemoteException {
        assert false;
        return remoteForm.createObjectEditorDialog(property.getID());
    }

    public Object getPropertyChangeValue(ClientPropertyDraw property) throws RemoteException {
        assert false;
        return remoteForm.getPropertyChangeValue(property.getID());
    }

    public boolean[] getCompatibleProperties(ClientPropertyDraw mainProperty, ClientPropertyDraw[] otherProperties) throws RemoteException {
        assert false;
        int n = otherProperties.length;
        int propertiesIDs[] = new int[n];
        for (int i = 0; i < n; ++i) {
            propertiesIDs[i] = otherProperties[i].getID();
        }

        return remoteForm.getCompatibleProperties(mainProperty.getID(), propertiesIDs);
    }

    public void gainedFocus() {
        //remoteForm может быть == null, если сработал closed, и тогда ничего вызывать не надо
        if (!isEditing() && remoteForm != null) {
            if (remoteForm == null)
                return;

            try {
                remoteForm.gainedFocus();
                if (clientNavigator != null) {
                    clientNavigator.relevantFormNavigator.currentFormChanged();
                }

                // если вдруг изменились данные в сессии
                ClientExternalScreen.invalidate(getID());
                ClientExternalScreen.repaintAll(getID());
            } catch (IOException e) {
                throw new RuntimeException(getString("form.error.form.activation"), e);
            }
        }
    }

    public void pasteExternalTable(List<ClientPropertyDraw> propertyList, List<List<Object>> table) throws IOException {
        List<Integer> propertyIdList = new ArrayList<Integer>();
        for (ClientPropertyDraw propertyDraw : propertyList) {
            propertyIdList.add(propertyDraw.getID());
        }
        processRemoteChanges(remoteForm.pasteExternalTable(propertyIdList, table));
    }

    public void pasteMulticellValue(Map<ClientPropertyDraw, List<ClientGroupObjectValue>> cells, Object value) throws IOException {
        Map<Integer, List<Map<Integer, Object>>> reCells = new HashMap<Integer, List<Map<Integer, Object>>>();
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
        processRemoteChanges(remoteForm.pasteMulticellValue(reCells, value));
    }

    public void groupChangePropertyDraw(ClientPropertyDraw mainProperty, ClientGroupObjectValue mainColumnKey,
                                        ClientPropertyDraw getterProperty, ClientGroupObjectValue getterColumnKey) throws IOException {
        assert false;
        // для глобальных свойств пока не может быть отложенных действий
        if (mainProperty.getGroupObject() != null) {
            SwingUtils.stopSingleAction(mainProperty.getGroupObject().getActionID(), true);
        }

        processRemoteChanges(
                remoteForm.groupChangePropertyDraw(mainProperty.getID(), mainColumnKey.serialize(), getterProperty.getID(), getterColumnKey.serialize())
        );
        refresh();
    }

    public void changeGridClass(ClientObject object, ClientObjectClass cls) throws IOException {
        commitOrCancelCurrentEditing();

        remoteForm.changeGridClass(object.getID(), cls.ID);
        applyRemoteChanges();
    }

    public void switchClassView(ClientGroupObject groupObject) throws IOException {
        commitOrCancelCurrentEditing();

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.switchClassView(groupObject.getID());

        applyRemoteChanges();
    }

    public void changeClassView(ClientGroupObject groupObject, ClassViewType show) throws IOException {
        commitOrCancelCurrentEditing();

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.changeClassView(groupObject.getID(), show);

        applyRemoteChanges();
    }

    public void changeOrder(ClientPropertyDraw property, Order modiType, ClientGroupObjectValue columnKey) throws IOException {
        if (defaultOrdersInitialized) {
            commitOrCancelCurrentEditing();

            remoteForm.changePropertyOrder(property.getID(), modiType.serialize(), columnKey.serialize());
            applyRemoteChanges();
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

        remoteForm.clearUserFilters();

        for (List<ClientPropertyFilter> listFilter : currentFilters.values()) {
            for (ClientPropertyFilter filter : listFilter) {
                remoteForm.addFilter(Serializer.serializeClientFilter(filter));
            }
        }

        applyRemoteChanges();
    }

    private void setRegularFilter(ClientRegularFilterGroup filterGroup, ClientRegularFilter filter) throws IOException {
        commitOrCancelCurrentEditing();

        remoteForm.setRegularFilter(filterGroup.getID(), (filter == null) ? -1 : filter.getID());

        applyOrders(filter != null ? filter.orders : filterGroup.nullOrders);

        applyRemoteChanges();
    }

    public int countRecords(int groupObjectID) throws IOException {
        commitOrCancelCurrentEditing();

        return remoteForm.countRecords(groupObjectID);
    }

    public Object calculateSum(int propertyID, byte[] columnKeys) throws IOException {
        commitOrCancelCurrentEditing();

        return remoteForm.calculateSum(propertyID, columnKeys);
    }

    public Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap, Map<Integer,
            List<byte[]>> maxMap, boolean onlyNotNull) throws IOException {
        commitOrCancelCurrentEditing();

        return remoteForm.groupData(groupMap, sumMap, maxMap, onlyNotNull);
    }

    public void changePageSize(ClientGroupObject groupObject, Integer pageSize) throws IOException {
        if (!tableManager.isEditing()) {
            remoteForm.changePageSize(groupObject.getID(), pageSize);
        }
    }

    public void moveGroupObject(ClientGroupObject parentGroup, ClientGroupObjectValue parentKey, ClientGroupObject childGroup, ClientGroupObjectValue childKey, int index) throws IOException {
        commitOrCancelCurrentEditing();

        remoteForm.moveGroupObject(parentGroup.getID(), parentKey.serialize(), childGroup.getID(), childKey.serialize(), index);

        applyRemoteChanges();
    }

    public void runSingleGroupReport(GroupObjectController groupController) {
        commitOrCancelCurrentEditing();
        try {
            Main.frame.runSingleGroupReport(remoteForm, groupController.getGroupObject().getID(), getUserPreferences());
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public void runSingleGroupXlsExport(GroupObjectController groupController) {
        commitOrCancelCurrentEditing();
        try {
            Main.frame.runSingleGroupXlsExport(remoteForm, groupController.getGroupObject().getID(), getUserPreferences());
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    public void refresh() {
        commitOrCancelCurrentEditing();
        try {
            remoteForm.refreshData();
            applyRemoteChanges();
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.refreshing.form"), e);
        }
    }

    void apply(boolean needConfirm) {
        commitOrCancelCurrentEditing();

        try {
            if (needConfirm && !askApplyConfirmation()) {
                return;
            }

            Object clientResult = null;
            if (remoteForm.hasClientActionOnApply()) {
                ClientAction actionOnApply = remoteForm.getClientActionOnApply();
                if (actionOnApply instanceof CheckFailed) { // чтобы не делать лишний RMI вызов
                    Log.error(((CheckFailed) actionOnApply).message);
                    setCanClose(false);
                    return;
                } else {
                    try {
                        clientResult = actionOnApply.dispatch(actionDispatcher);
                    } catch (Exception e) {
                        throw new RuntimeException(getString("form.error.applying.changes"), e);
                    }
                }
            }

            processRemoteChanges(remoteForm.applyChanges(clientResult));
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.applying.changes"), e);
        }
    }

    private boolean askApplyConfirmation() {
        String confirmMsg = "";
        for (ClientGroupObject group : form.groupObjects) {
            if (controllers.containsKey(group)) {
                confirmMsg += controllers.get(group).getConfirmApplyMessage();
            }
        }

        if (!confirmMsg.isEmpty()) {
            if (!(SwingUtils.showConfirmDialog(getComponent(), confirmMsg, null, JOptionPane.QUESTION_MESSAGE, SwingUtils.YES_BUTTON) == JOptionPane.YES_OPTION)) {
                setCanClose(false);
                return false;
            }
        }

        return true;
    }

    public byte[] getPropertyChangeType(ClientPropertyDraw property, ClientGroupObjectValue key, boolean aggValue) throws IOException {
        assert false;
        return remoteForm.getPropertyChangeType(property.getID(), key.serialize(), aggValue);
    }

    public void dropLayoutCaches() {
        formLayout.dropCaches();
    }

    public ClientFormActionDispatcher getActionDispatcher() {
        return actionDispatcher;
    }

    public void closed() {
        // здесь мы сбрасываем ссылку на remoteForm для того, чтобы сборщик мусора быстрее собрал удаленные объекты
        // это нужно, чтобы connection'ы на сервере закрывались как можно быстрее
        remoteForm = null;
    }

    public Dimension calculatePreferredSize() {
        return formLayout.calculatePreferredSize();
    }

    public boolean isCanClose() {
        return canClose;
    }

    public void setCanClose(boolean canClose) {
        this.canClose = canClose;
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

    void printPressed() {
        commitOrCancelCurrentEditing();
        try {
            Main.frame.runReport(remoteForm, false, getUserPreferences());
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    void xlsPressed() {
        commitOrCancelCurrentEditing();
        Main.module.runExcel(remoteForm, getUserPreferences());
    }

    void editPressed() {
        commitOrCancelCurrentEditing();
        try {
            Map<String, String> pathMap = Main.frame.getReportPath(remoteForm, getUserPreferences());

            for (String path : pathMap.keySet()) {
                Desktop.getDesktop().open(new File(path));
            }

            // не очень хорошо оставлять живой поток, но это используется только в девелопменте, поэтому не важно
            new SavingThread(pathMap).start();
        } catch (Exception e) {
            throw new RuntimeException(getString("form.error.printing.form"), e);
        }
    }

    void cancelPressed() {
        commitOrCancelCurrentEditing();
        if (dataChanged) {
            try {
                if (SwingUtils.showConfirmDialog(getComponent(), getString("form.do.you.really.want.to.undo.changes"), null, JOptionPane.WARNING_MESSAGE, SwingUtils.NO_BUTTON) == JOptionPane.YES_OPTION) {
                    remoteForm.cancelChanges();
                    applyRemoteChanges();
                }
            } catch (IOException e) {
                throw new RuntimeException(getString("form.error.undoing.changes"), e);
            }
        }
    }

    void applyPressed() {
        commitOrCancelCurrentEditing();
        apply(true);
    }

    void refreshPressed() {
        commitOrCancelCurrentEditing();
        refresh();
    }

    public boolean okPressed() {
        if (!commitCurrentEditing()) {
             return false;
        }

        setCanClose(true);

        try {
            processRemoteChanges(remoteForm.okPressed());

            if (isNewSession) {
                apply(true);
            }
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.closing.dialog"), e);
        }

        return isCanClose();
    }

    void closePressed() {
        try {
            processRemoteChanges(remoteForm.closedPressed());
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.closing.dialog"), e);
        }
    }

    void nullPressed() {
        try {
            processRemoteChanges(remoteForm.nullPressed());
        } catch (IOException e) {
            throw new RuntimeException(getString("form.error.closing.dialog"), e);
        }
    }

    public class ClientRegularFilterWrapped {
        public ClientRegularFilter filter;
        private String caption;

        public ClientRegularFilterWrapped(String caption) {
            this(caption, null);
        }

        public ClientRegularFilterWrapped(ClientRegularFilter filter) {
            this(null, filter);
        }

        public ClientRegularFilterWrapped(String caption, ClientRegularFilter filter) {
            this.filter = filter;
            this.caption = caption;
        }

        @Override
        public boolean equals(Object wrapped) {
            return wrapped instanceof ClientRegularFilterWrapped
                    && (filter != null ? filter.equals(((ClientRegularFilterWrapped) wrapped).filter) : this == wrapped);
        }

        @Override
        public String toString() {
            return caption == null ? filter.getFullCaption() : caption;
        }
    }
}
