/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.client.*;
import platform.client.logics.*;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.layout.ReportDockable;
import platform.client.layout.ClientFormDockable;
import platform.client.navigator.ClientNavigator;
import platform.interop.CompressingInputStream;
import platform.interop.Scroll;
import platform.interop.Order;
import platform.interop.action.*;
import platform.interop.form.RemoteFormInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.rmi.RemoteException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;

import net.sf.jasperreports.engine.JRException;

public class ClientForm extends JPanel {

    private final ClientFormView formView;

    public final RemoteFormInterface remoteForm;
    public final ClientNavigator clientNavigator;
    public final ClientFormActionDispatcher actionDispatcher;

    public boolean isDialogMode() {
        return false;
    }

    public boolean isReadOnlyMode() {
        return false;
    }

    private int ID;
    public int getID() {
        return ID;
    }

    public ClientForm(RemoteFormInterface iremoteForm, ClientNavigator iclientNavigator) throws IOException, ClassNotFoundException {

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

        actionDispatcher = new ClientFormActionDispatcher(clientNavigator);

        formView = ClientObjectProxy.retrieveClientFormView(remoteForm);

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

        for (ClientPropertyView properties : formView.properties) {
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
        Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        while (comp != null && !(comp instanceof Window) && comp != this) {
            if (comp instanceof JComponent) {
                ClientGroupObjectImplementView groupObject = (ClientGroupObjectImplementView)((JComponent)comp).getClientProperty("groupObject");
                if (groupObject != null && controllers.containsKey(groupObject) && controllers.get(groupObject).processKeyEvent(ks, e)) return true;
            }
            comp = comp.getParent();
        }

        for (ClientGroupObjectImplementView groupObject : groupObjects) {
            if (controllers.get(groupObject).processKeyEvent(ks, e)) return true;
        }

        return super.processKeyBinding(ks, e, condition, pressed);
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

        // Добавляем стандартные кнопки
        JButton buttonPrint = new JButton("Печать");
        buttonPrint.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    print();
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при печати формы", e);
                }
            }
        });

        JButton buttonXls = new JButton("Excel");
        buttonXls.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    ReportDockable.exportToExcel(remoteForm);
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при экспорте в Excel", e);
                }
            }
        });

        AbstractAction nullAction = new AbstractAction("Сбросить") {

            public void actionPerformed(ActionEvent ae) {
                nullPressed();
            }
        };
        JButton buttonNull = new JButton(nullAction);

        AbstractAction refreshAction = new AbstractAction("Обновить") {

            public void actionPerformed(ActionEvent ae) {
                refreshData();
            }
        };
        JButton buttonRefresh = new JButton(refreshAction);

        AbstractAction applyAction = new AbstractAction("Применить") {

            public void actionPerformed(ActionEvent ae) {
                applyChanges();
            }
        };
        buttonApply = new JButton(applyAction);

        AbstractAction cancelAction = new AbstractAction("Отменить") {

            public void actionPerformed(ActionEvent ae) {
                cancelChanges();
            }
        };
        buttonCancel = new JButton(cancelAction);

        AbstractAction okAction = new AbstractAction("OK") {

            public void actionPerformed(ActionEvent ae) {
                okPressed();
            }
        };
        JButton buttonOK = new JButton(okAction);

        AbstractAction closeAction = new AbstractAction("Закрыть") {

            public void actionPerformed(ActionEvent ae) {
                closePressed();
            }
        };
        JButton buttonClose = new JButton(closeAction);

        KeyStroke altDel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK);
        im.put(altDel, "altDelPressed");

        KeyStroke keyF5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
        im.put(keyF5, "f5Pressed");

        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (isDialogMode() && isReadOnlyMode()) ? 0 : InputEvent.ALT_DOWN_MASK);
        im.put(altEnter, "enterPressed");

        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(escape, "escPressed");

        am.put("f5Pressed", refreshAction);

        formLayout.add(formView.refreshView, buttonRefresh);
        
        if (!isDialogMode()) {

            am.put("enterPressed", applyAction);
            am.put("escPressed", cancelAction);

            formLayout.add(formView.printView, buttonPrint);
            formLayout.add(formView.xlsView, buttonXls);
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

        byte[] state = remoteForm.getFormChangesByteArray();
        Log.incrementBytesReceived(state.length);

        applyFormChanges(new ClientFormChanges(new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(state))), formView));

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

            remoteForm.changeGroupObject(groupObject.getID(), Serializer.serializeClientGroupObjectValue(objectValue));

            controllers.get(groupObject).setCurrentGroupObject(objectValue,true);

            applyFormChanges();

            clientNavigator.changeCurrentClass(remoteForm,groupObject.get(0));
        }

    }

    public void changeGroupObject(ClientGroupObjectImplementView groupObject, Scroll changeType) throws IOException {

        remoteForm.changeGroupObject(groupObject.getID(), changeType.serialize());

        applyFormChanges();

        clientNavigator.changeCurrentClass(remoteForm,groupObject.get(0));
    }

    public void changeProperty(ClientCellView property, Object value) throws IOException {

        if (property.getGroupObject() != null) // для глобальных свойств пока не может быть отложенных действий
            SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);

        if (property instanceof ClientPropertyView) {

            // типа только если меняется свойство
            List<ClientAction> actions = remoteForm.changePropertyView(property.getID(), BaseUtils.serializeObject(value));

            dispatchActions(actions);

            dataChanged();
            applyFormChanges();

        } else {

            if (property instanceof ClientClassCellView) {
                changeClass(((ClientClassCellView)property).object, (ClientConcreteClass)value);
            } else {

                ClientObjectImplementView object = ((ClientObjectCellView)property).object;
                remoteForm.changeObject(object.getID(), value);

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

    void print() throws IOException, ClassNotFoundException, JRException {

        Main.layout.defaultStation.drop(new ReportDockable(clientNavigator, remoteForm));

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