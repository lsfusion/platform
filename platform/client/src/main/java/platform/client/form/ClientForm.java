/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.client.Log;
import platform.client.Main;
import platform.client.SwingUtils;
import platform.client.logics.*;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.navigator.ClientNavigator;
import platform.interop.CompressingInputStream;
import platform.interop.Order;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionResult;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.response.ChangeGroupObjectResponse;
import platform.interop.form.response.ChangePropertyViewResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

public class ClientForm extends JPanel {

    private final ClientFormView formView;

    public final RemoteFormInterface remoteForm;
    public final ClientNavigator clientNavigator;
    public final ClientFormActionDispatcher actionDispatcher;

    public boolean isDialogMode() {
        return false;
    }

    public boolean isReadOnlyMode() {
        return formView.readOnly;
    }

    private int ID;
    public int getID() {
        return ID;
    }

    private static final Map<Integer, ClientFormView> cacheClientFormView = new HashMap<Integer, ClientFormView>();

    private static ClientFormView cacheClientFormView(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {

        int ID = remoteForm.getID();

        if (!cacheClientFormView.containsKey(ID)) {

            byte[] state = remoteForm.getRichDesignByteArray();
            Log.incrementBytesReceived(state.length);

            cacheClientFormView.put(ID, new ClientFormView(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(state)))));
        }

        return cacheClientFormView.get(ID);
    }

    public ClientForm(RemoteFormInterface iremoteForm, ClientNavigator iclientNavigator) throws IOException, ClassNotFoundException {

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

        actionDispatcher = new ClientFormActionDispatcher(clientNavigator);

        formView = cacheClientFormView(remoteForm);

        // так неправильно делать по двум причинам :
        // 1. лишний ping
        // 2. compID могут совпадать. Пока это используется только в диалогах, поэтому не столь критично
        ID = remoteForm.getID();

        initializeForm();

        applyFormChanges();

    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //

    private boolean hasFocus = false;

    private ClientFormLayout formLayout;

    private Map<ClientGroupObjectImplementView, GroupObjectController> controllers;

    private JButton buttonApply;
    private JButton buttonCancel;

    void initializeForm() throws IOException {

        formLayout = new ClientFormLayout(formView.containers);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(formLayout.getComponent());
//        setContentPane(formLayout.getComponent());
//        setComponent(formLayout.getComponent());

        initializeGroupObjects();

        initializeRegularFilters();

        initializeButtons();

        initializeOrders();

        dataChanged();

        // следим за тем, когда форма становится активной
        final String FOCUS_OWNER_PROPERTY = "focusOwner";

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(FOCUS_OWNER_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                Component focusComponent = (Component)evt.getNewValue();
                if (focusComponent != null) {
                    boolean newHasFocus = ClientForm.this.isAncestorOf(focusComponent);
                    if (hasFocus != newHasFocus) {
                        hasFocus = newHasFocus;
                        if (hasFocus) {

                            try {
                                remoteForm.gainedFocus();
                                clientNavigator.currentFormChanged();

                                // если вдруг изменились данные в сессии
                                applyFormChanges();
                                dataChanged();
                            } catch (IOException e) {
                                throw new RuntimeException("Ошибка при активации формы", e);
                            }
                        }
                    }
                }

            }
        });

        setFocusCycleRoot(true);

        // вот таким вот маразматичным способом делается, чтобы при нажатии мышкой в ClientForm фокус оставался на ней, а не уходил куда-то еще
        // теоретически можно найти способ как это сделать не так извращенно, но копаться в исходниках Swing'а очень долго
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    // здесь хранится список всех GroupObjects плюс при необходимости null
    private List<ClientGroupObjectImplementView> groupObjects;
    public List<ClientGroupObjectImplementView> getGroupObjects() {
        return groupObjects;
    }


    private void initializeGroupObjects() throws IOException {

        controllers = new HashMap<ClientGroupObjectImplementView, GroupObjectController>();
        groupObjects = new ArrayList<ClientGroupObjectImplementView>();

        for (ClientGroupObjectImplementView groupObject : formView.groupObjects) {
            groupObjects.add(groupObject);
            GroupObjectController controller = new GroupObjectController(groupObject, formView, this, formLayout);
            controllers.put(groupObject, controller);
        }

        for (ClientPropertyView properties : formView.getProperties()) {
            if (properties.groupObject == null) {
                groupObjects.add(null);
                GroupObjectController controller = new GroupObjectController(null, formView, this, formLayout);
                controllers.put(null, controller);
                break;
            }
        }
    }

    // реализуем "обратную" обработку нажатий кнопок
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        // делаем так, чтобы первым нажатия клавиш обрабатывал GroupObject, у которого стоит фокус
        // хотя конечно идиотизм это делать таким образом
        Component comp = e.getComponent(); //KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        while (comp != null && !(comp instanceof Window) && comp != this) {
            if (comp instanceof JComponent) {
                ClientGroupObjectImplementView groupObject = (ClientGroupObjectImplementView)((JComponent)comp).getClientProperty("groupObject");
                if (groupObject != null) {
                    Map<ClientGroupObjectImplementView, Runnable> keyBinding = bindings.get(ks);
                    if (keyBinding != null && keyBinding.containsKey(groupObject)) {
                        keyBinding.get(groupObject).run();
                        return true;
                    }
                    break;
                }
            }
            comp = comp.getParent();
        }

        Map<ClientGroupObjectImplementView, Runnable> keyBinding = bindings.get(ks);
        if (keyBinding != null && !keyBinding.isEmpty())
            keyBinding.values().iterator().next().run();

        if (super.processKeyBinding(ks, e, condition, pressed)) return true;

        return true;
    }

    private Map<KeyStroke, Map<ClientGroupObjectImplementView, Runnable>> bindings = new HashMap<KeyStroke, Map<ClientGroupObjectImplementView, Runnable>>();

    public void addKeyBinding(KeyStroke ks, ClientGroupObjectImplementView groupObject, Runnable run) {
        if (!bindings.containsKey(ks))
            bindings.put(ks, new HashMap<ClientGroupObjectImplementView, Runnable>());
        bindings.get(ks).put(groupObject, run);
    }

    private void initializeRegularFilters() {

        InputMap im = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        // Проинициализируем регулярные фильтры

        for (final ClientRegularFilterGroupView filterGroup : formView.regularFilters) {

            if (filterGroup.filters.size() == 1) {

                final ClientRegularFilterView singleFilter = filterGroup.filters.get(0);

                final JCheckBox checkBox = new JCheckBox(singleFilter.toString());
                checkBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            if (ie.getStateChange() == ItemEvent.SELECTED)
                                setRegularFilter(filterGroup, singleFilter);
                            else
                                setRegularFilter(filterGroup, null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, checkBox);

                String filterID = "regularFilter" + filterGroup.ID + singleFilter.ID;
                im.put(singleFilter.key, filterID);
                am.put(filterID, new AbstractAction() {

                    public void actionPerformed(ActionEvent e) {
                        checkBox.setSelected(!checkBox.isSelected());
                    }
                });

                if(filterGroup.defaultFilter >= 0) {
                    checkBox.setSelected(true);
                    try {
                        setRegularFilter(filterGroup, singleFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }
            } else {

                final JComboBox comboBox = new JComboBox(
                        BaseUtils.mergeList(Collections.singletonList("(Все)"),filterGroup.filters).toArray());
                comboBox.addItemListener(new ItemListener() {

                    public void itemStateChanged(ItemEvent ie) {
                        try {
                            setRegularFilter(filterGroup,
                                    ie.getItem() instanceof ClientRegularFilterView?(ClientRegularFilterView)ie.getItem():null);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении регулярного фильтра", e);
                        }
                    }
                });
                formLayout.add(filterGroup, comboBox);

                for (final ClientRegularFilterView singleFilter : filterGroup.filters) {
                    String filterID = "regularFilter" + filterGroup.ID + singleFilter.ID;
                    im.put(singleFilter.key, filterID);
                    am.put(filterID, new AbstractAction() {

                        public void actionPerformed(ActionEvent e) {
                            comboBox.setSelectedItem(singleFilter);
                        }
                    });
                }

                if(filterGroup.defaultFilter >= 0) {
                    ClientRegularFilterView defaultFilter = filterGroup.filters.get(filterGroup.defaultFilter);
                    comboBox.setSelectedItem(defaultFilter);
                    try {
                        setRegularFilter(filterGroup, defaultFilter);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при инициализации регулярного фильтра", e);
                    }
                }
            }

        }
    }

    private void initializeButtons() {

        InputMap im = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        KeyStroke altP = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK);
        im.put(altP, "altPPressed");

        KeyStroke altX = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK);
        im.put(altX, "altXPressed");

        KeyStroke altDel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
        im.put(altDel, "altDelPressed");

        KeyStroke keyF5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
        im.put(keyF5, "f5Pressed");

        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (isDialogMode() && isReadOnlyMode()) ? 0 : InputEvent.ALT_DOWN_MASK);
        im.put(altEnter, "enterPressed");

        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(escape, "escPressed");

        // Добавляем стандартные кнопки

        if(Main.module.isFull()) {
            AbstractAction printAction = new AbstractAction("Печать (" + SwingUtils.getKeyStrokeCaption(altP) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    print();
                }
            };
            am.put("altPPressed", printAction);

            JButton buttonPrint = new JButton(printAction);
            buttonPrint.setFocusable(false);

            AbstractAction xlsAction = new AbstractAction("Excel (" + SwingUtils.getKeyStrokeCaption(altX) + ")") {

                public void actionPerformed(ActionEvent ae) {
                    Main.module.runExcel(remoteForm);
                }
            };
            am.put("altXPressed", xlsAction);

            JButton buttonXls = new JButton(xlsAction);
            buttonXls.setFocusable(false);

            if (!isDialogMode()) {
                formLayout.add(formView.printView, buttonPrint);
                formLayout.add(formView.xlsView, buttonXls);
            }            
        }

        AbstractAction nullAction = new AbstractAction("Сбросить (" + SwingUtils.getKeyStrokeCaption(altDel) + ")") {

            public void actionPerformed(ActionEvent ae) {
                nullPressed();
            }
        };
        JButton buttonNull = new JButton(nullAction);
        buttonNull.setFocusable(false);

        AbstractAction refreshAction = new AbstractAction("Обновить (" + SwingUtils.getKeyStrokeCaption(keyF5) + ")") {

            public void actionPerformed(ActionEvent ae) {
                refreshData();
            }
        };
        JButton buttonRefresh = new JButton(refreshAction);
        buttonRefresh.setFocusable(false);

        AbstractAction applyAction = new AbstractAction("Применить (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                applyChanges();
            }
        };
        buttonApply = new JButton(applyAction);
        buttonApply.setFocusable(false);

        AbstractAction cancelAction = new AbstractAction("Отменить (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                cancelChanges();
            }
        };
        buttonCancel = new JButton(cancelAction);
        buttonCancel.setFocusable(false);

        AbstractAction okAction = new AbstractAction("OK (" + SwingUtils.getKeyStrokeCaption(altEnter) + ")") {

            public void actionPerformed(ActionEvent ae) {
                okPressed();
            }
        };
        JButton buttonOK = new JButton(okAction);
        buttonOK.setFocusable(false);

        AbstractAction closeAction = new AbstractAction("Закрыть (" + SwingUtils.getKeyStrokeCaption(escape) + ")") {

            public void actionPerformed(ActionEvent ae) {
                closePressed();
            }
        };
        JButton buttonClose = new JButton(closeAction);
        buttonClose.setFocusable(false);

        am.put("f5Pressed", refreshAction);

        formLayout.add(formView.refreshView, buttonRefresh);
        
        if (!isDialogMode()) {

            am.put("enterPressed", applyAction);
            am.put("escPressed", cancelAction);

            formLayout.add(formView.applyView, buttonApply);
            formLayout.add(formView.cancelView, buttonCancel);

        } else {

            am.put("altDelPressed", nullAction);

            am.put("enterPressed", okAction);
            am.put("escPressed", closeAction);

            formLayout.add(formView.nullView, buttonNull);
            formLayout.add(formView.okView, buttonOK);
            formLayout.add(formView.closeView, buttonClose);

        }
    }

    private void initializeOrders() throws IOException {
        // Применяем порядки по умолчанию
        for (Map.Entry<ClientCellView, Boolean> entry : formView.defaultOrders.entrySet()) {
            controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.ADD);
            if (!entry.getValue()) {
                controllers.get(entry.getKey().getGroupObject()).changeGridOrder(entry.getKey(), Order.DIR);
            }
        }
    }

    void applyFormChanges() throws IOException {
        applyFormChanges(remoteForm.getFormChangesByteArray());
    }

    void applyFormChanges(byte[] changes) throws IOException {
        Log.incrementBytesReceived(changes.length);
        applyFormChanges(new ClientFormChanges(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(changes))), formView));
    }

    void applyFormChanges(ClientFormChanges formChanges) {

        // Сначала меняем виды объектов

        for (ClientPropertyView property : formChanges.panelProperties.keySet()) {
            controllers.get(property.groupObject).addPanelProperty(property);
        }

        for (ClientPropertyView property : formChanges.gridProperties.keySet()) {
            controllers.get(property.groupObject).addGridProperty(property);
        }

        for (ClientPropertyView property : formChanges.dropProperties) {
            controllers.get(property.groupObject).dropProperty(property);
        }


        // Затем подгружаем новые данные

        // Сначала новые объекты

        for (ClientGroupObjectImplementView groupObject : formChanges.gridObjects.keySet()) {
            controllers.get(groupObject).setGridObjects(formChanges.gridObjects.get(groupObject));
        }

        for (ClientGroupObjectImplementView groupObject : formChanges.gridClasses.keySet()) {
            controllers.get(groupObject).setGridClasses(formChanges.gridClasses.get(groupObject));
        }

        for (Map.Entry<ClientGroupObjectImplementView,ClientGroupObjectValue> groupObject : formChanges.objects.entrySet())
            controllers.get(groupObject.getKey()).setCurrentGroupObject(groupObject.getValue(),false);

        for (ClientGroupObjectImplementView groupObject : formChanges.classViews.keySet())
            controllers.get(groupObject).setClassView(formChanges.classViews.get(groupObject));

        for (Map.Entry<ClientGroupObjectImplementView,ClientGroupObjectClass> groupObject : formChanges.classes.entrySet())
            controllers.get(groupObject.getKey()).setCurrentGroupObjectClass(groupObject.getValue());

        // Затем их свойства

        for (ClientPropertyView property : formChanges.panelProperties.keySet()) {
            controllers.get(property.groupObject).setPanelPropertyValue(property, formChanges.panelProperties.get(property));
        }

        for (ClientPropertyView property : formChanges.gridProperties.keySet()) {
            controllers.get(property.groupObject).setGridPropertyValues(property, formChanges.gridProperties.get(property));
        }

        formLayout.getComponent().validate();

        // выдадим сообщение если было от сервера
        if(formChanges.message.length()>0)
            Log.printFailedMessage(formChanges.message);        
    }

    public void changeGroupObject(ClientGroupObjectImplementView groupObject, ClientGroupObjectValue objectValue) throws IOException {

        ClientGroupObjectValue curObjectValue = controllers.get(groupObject).getCurrentObject();

        if (!objectValue.equals(curObjectValue)) {

            // приходится вот так возвращать класс, чтобы не было лишних запросов
            ChangeGroupObjectResponse response = remoteForm.changeGroupObject(groupObject.getID(), Serializer.serializeClientGroupObjectValue(objectValue));

            controllers.get(groupObject).setCurrentGroupObject(objectValue,true);

            applyFormChanges(response.formChanges);

            clientNavigator.changeCurrentClass(remoteForm,response.classID);
        }

    }

    public void changeGroupObject(ClientGroupObjectImplementView groupObject, Scroll changeType) throws IOException {

        remoteForm.changeGroupObject(groupObject.getID(), changeType.serialize());

        applyFormChanges();

        clientNavigator.changeCurrentClass(remoteForm,groupObject.get(0));
    }

    public void changeProperty(ClientCellView property, Object value, boolean all) throws IOException {

        if (property.getGroupObject() != null) // для глобальных свойств пока не может быть отложенных действий
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);

        if (property instanceof ClientPropertyView) {

            ChangePropertyViewResponse response = remoteForm.changePropertyView(property.getID(), BaseUtils.serializeObject(value), all); 
            dispatchActions(response.actions);
            dataChanged();
            applyFormChanges(response.formChanges);

        } else {

            if (property instanceof ClientClassCellView) {
                changeClass(((ClientClassCellView)property).object, (ClientConcreteClass)value);
            } else {

                ClientObjectImplementView object = ((ClientObjectCellView)property).object;
                dispatchActions(remoteForm.changeObject(object.getID(), value));

                controllers.get(property.getGroupObject()).setCurrentObject(object, value);

                applyFormChanges();

                clientNavigator.changeCurrentClass(remoteForm,object);
            }
        }

    }

    private List<ClientActionResult> dispatchActions(List<? extends ClientAction> actions) throws IOException {
        
        if (actions == null) return null;

        List<ClientActionResult> result = new ArrayList<ClientActionResult>();

        for(ClientAction action : actions)
            result.add(action.dispatch(actionDispatcher));

        return result;
    }

    void addObject(ClientObjectImplementView object, ClientConcreteClass cls) throws IOException {
        
        remoteForm.addObject(object.getID(), cls.ID);
        dataChanged();

        applyFormChanges();
    }

    public ClientClass getBaseClass(ClientObjectImplementView object) throws IOException {
        return ClientClass.deserialize(new DataInputStream(new ByteArrayInputStream(remoteForm.getBaseClassByteArray(object.getID()))));
    }

    public List<ClientClass> getChildClasses(ClientObjectImplementView object, ClientObjectClass parentClass) throws IOException {
        return DeSerializer.deserializeListClientClass(remoteForm.getChildClassesByteArray(object.getID(),parentClass.ID));
    }

    public void changeClass(ClientObjectImplementView object, ClientConcreteClass cls) throws IOException {

        SwingUtils.stopSingleAction(object.groupObject.getActionID(), true);

        remoteForm.changeClass(object.getID(), (cls == null) ? -1 : cls.ID);
        dataChanged();

        applyFormChanges();

        clientNavigator.changeCurrentClass(remoteForm, object);
    }

    public void changeGridClass(ClientObjectImplementView object, ClientObjectClass cls) throws IOException {

        remoteForm.changeGridClass(object.getID(), cls.ID);
        applyFormChanges();
    }

    public boolean switchClassView(ClientGroupObjectImplementView groupObject) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        if (remoteForm.switchClassView(groupObject.getID())) {
            applyFormChanges();
            return true;
        } else
            return false;
    }

    public boolean changeClassView(ClientGroupObjectImplementView groupObject, byte show) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        if (remoteForm.changeClassView(groupObject.getID(), show)) {
            applyFormChanges();
            return true;
        } else
            return false;
    }

    public void changeOrder(ClientCellView property, Order modiType) throws IOException {

        if(property instanceof ClientPropertyView)
            remoteForm.changePropertyOrder(property.getID(), modiType.serialize());
        else
            remoteForm.changeObjectOrder(property.getID(), modiType.serialize());

        applyFormChanges();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void changeFind(List<ClientPropertyFilter> conditions) {
    }

    private final Map<ClientGroupObjectImplementView, List<ClientPropertyFilter>> currentFilters = new HashMap<ClientGroupObjectImplementView, List<ClientPropertyFilter>>();
    
    public void changeFilter(ClientGroupObjectImplementView groupObject, List<ClientPropertyFilter> conditions) throws IOException {

        currentFilters.put(groupObject, conditions);

        remoteForm.clearUserFilters();

        for (List<ClientPropertyFilter> listFilter : currentFilters.values())
            for (ClientPropertyFilter filter : listFilter) {
                remoteForm.addFilter(Serializer.serializeClientFilter(filter));
            }

        applyFormChanges();
    }

    private void setRegularFilter(ClientRegularFilterGroupView filterGroup, ClientRegularFilterView filter) throws IOException {

        remoteForm.setRegularFilter(filterGroup.ID, (filter == null) ? -1 : filter.ID);

        applyFormChanges();
    }

    public void changePageSize(ClientGroupObjectImplementView groupObject, int pageSize) throws IOException {

        remoteForm.changePageSize(groupObject.getID(), pageSize);

//        applyFormChanges();
    }

    void print() {

        try {
            Main.frame.runReport(clientNavigator, remoteForm);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при печати формы", e);
        }
    }

    void refreshData() {

        try {

            remoteForm.refreshData();

            applyFormChanges();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при обновлении формы", e);
        }
    }

    boolean applyChanges() {

        try {

            if (remoteForm.hasSessionChanges()) {

                String okMessage = "";
                for (ClientGroupObjectImplementView groupObject : formView.groupObjects) {
                    okMessage += controllers.get(groupObject).getSaveMessage();
                }

                if (!okMessage.isEmpty()) {
                    if (!(SwingUtils.showConfirmDialog(this, okMessage, null, JOptionPane.QUESTION_MESSAGE, SwingUtils.YES_BUTTON) == JOptionPane.YES_OPTION)) {
                        return false;
                    }
                }

                List<? extends ClientAction> actions = remoteForm.getApplyActions();
                if (actions != null) {

                    String checkMessage = remoteForm.checkChanges();
                    if (checkMessage != null) {
                        Log.printFailedMessage(checkMessage);
                        return false;
                    }

                    for (ClientAction action : actions) {
                        String message = remoteForm.checkApplyActions(action.ID, action.dispatch(actionDispatcher));
                        if (message != null) {
                            Log.printFailedMessage(message);
                            return false;
                        }
                    }
                }

                String message = remoteForm.applyChanges();

                if (message == null) {
                    Log.printSuccessMessage("Изменения были удачно записаны...");
                    dataChanged();
                } else {
                    Log.printFailedMessage(message);
                    return false;
                }

                applyFormChanges();
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при применении изменений", e);
        }

        
        return true;
    }

    boolean cancelChanges() {

        try {

            if (remoteForm.hasSessionChanges()) {

                if (SwingUtils.showConfirmDialog(this, "Вы действительно хотите отменить сделанные изменения ?", null, JOptionPane.WARNING_MESSAGE, SwingUtils.NO_BUTTON) == JOptionPane.YES_OPTION) {
                    remoteForm.cancelChanges();
                    dataChanged();
                    applyFormChanges();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при отмене изменений", e);
        }

        return true;
    }

    public boolean okPressed() {
        return applyChanges();
    }

    boolean closePressed() {
        return cancelChanges();
    }

    boolean nullPressed() {
        return true;
    }

    private Color defaultApplyBackground;

    private void dataChanged() throws RemoteException {

        if (defaultApplyBackground == null)
            defaultApplyBackground = buttonApply.getBackground();

        boolean formHasChanged = remoteForm.hasSessionChanges();
        
        if (formHasChanged) {

            buttonApply.setBackground(Color.green);
            buttonApply.setEnabled(true);
            buttonCancel.setEnabled(true);
        } else {

            buttonApply.setBackground(defaultApplyBackground);
            buttonApply.setEnabled(false);
            buttonCancel.setEnabled(false);
        }

    }

    public void dropLayoutCaches() {
        formLayout.dropCaches();
    }
}