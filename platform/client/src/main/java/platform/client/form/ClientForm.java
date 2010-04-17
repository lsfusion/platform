/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.client.*;
import platform.client.form.panel.PanelCellController;
import platform.client.form.grid.GridTable;
import platform.client.form.grid.GridView;
import platform.client.form.queries.FindController;
import platform.client.form.queries.FilterController;
import platform.client.logics.*;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.layout.ReportDockable;
import platform.client.navigator.ClientNavigator;
import platform.interop.CompressingInputStream;
import platform.interop.Scroll;
import platform.interop.Order;
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

import net.sf.jasperreports.engine.JRException;

public class ClientForm extends JPanel {

    private final ClientFormView formView;

    public final RemoteFormInterface remoteForm;
    public final ClientNavigator clientNavigator;

    private boolean readOnly;
    public boolean isReadOnly() {
        return readOnly;
    }

    public ClientForm(RemoteFormInterface iremoteForm, ClientNavigator iclientNavigator, boolean ireadOnly) throws IOException, ClassNotFoundException {
//        super(app);

//        FocusOwnerTracer.installFocusTracer();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

        readOnly = ireadOnly;

        formView = ClientObjectProxy.retrieveClientFormView(remoteForm);

        initializeForm();

        applyFormChanges();

    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //

    private boolean hasFocus = false;

    private ClientFormLayout formLayout;

    public Map<ClientGroupObjectImplementView, GroupObjectModel> models;

    private JButton buttonPrint;
    private JButton buttonRefresh;
    private JButton buttonApply;
    private JButton buttonCancel;
    private JButton buttonOK;
    private JButton buttonClose;

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

    }

    private void initializeGroupObjects() throws IOException {

        models = new HashMap();

        for (ClientGroupObjectImplementView groupObject : formView.groupObjects) {

            GroupObjectModel model = new GroupObjectModel(groupObject);
            models.put(groupObject, model);
        }
    }

    private void initializeRegularFilters() {
        InputMap im = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        // Проинициализируем регулярные фильтры

        for (final ClientRegularFilterGroupView filterGroup : formView.regularFilters) {

            if (filterGroup.filters.size() == 1) {

                final ClientRegularFilterView singleFilter = filterGroup.filters.get(0);

                final JCheckBox checkBox = new JCheckBox(singleFilter.name);
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
        buttonPrint = new JButton("Печать");
        buttonPrint.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    print();
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при печати формы", e);
                }
            }
        });

        buttonRefresh = new JButton("Обновить");
        buttonRefresh.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    refreshData();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при обновлении формы", e);
                }
            }
        });

        buttonApply = new JButton("Применить");
        buttonApply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    saveChanges();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при применении изменений", e);
                }
            }
        });

        buttonCancel = new JButton("Отменить");
        buttonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                try {
                    cancelChanges();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при отмене изменений", e);
                }
            }
        });

        AbstractAction okAction = new AbstractAction("OK") {

            public void actionPerformed(ActionEvent ae) {
                okPressed();
            }
        };

        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (readOnly) ? 0 : InputEvent.ALT_DOWN_MASK);
        im.put(altEnter, "okPressed");
        am.put("okPressed", okAction);

        buttonOK = new JButton(okAction);

        AbstractAction closeAction = new AbstractAction("Закрыть") {

            public void actionPerformed(ActionEvent ae) {
                closePressed();
            }
        };

        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(escape, "closePressed");
        am.put("closePressed", closeAction);

        buttonClose = new JButton(closeAction);

        if (!readOnly) {
            formLayout.add(formView.printView, buttonPrint);
            formLayout.add(formView.refreshView, buttonRefresh);
            formLayout.add(formView.applyView, buttonApply);
            formLayout.add(formView.cancelView, buttonCancel);
        }

        formLayout.add(formView.okView, buttonOK);
        formLayout.add(formView.closeView, buttonClose);
    }

    private void initializeOrders() throws IOException {
        // Применяем порядки по умолчанию
        for (Map.Entry<ClientCellView, Boolean> entry : formView.defaultOrders.entrySet()) {
            models.get(entry.getKey().getGroupObject()).grid.changeGridOrder(entry.getKey(), Order.ADD);
            if (!entry.getValue()) {
                models.get(entry.getKey().getGroupObject()).grid.changeGridOrder(entry.getKey(), Order.DIR);
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
            models.get(property.groupObject).addPanelProperty(property);
        }

        for (ClientPropertyView property : formChanges.gridProperties.keySet()) {
            models.get(property.groupObject).addGridProperty(property);
        }

        for (ClientPropertyView property : formChanges.dropProperties) {
            models.get(property.groupObject).dropProperty(property);
        }


        // Затем подгружаем новые данные

        // Сначала новые объекты

        for (ClientGroupObjectImplementView groupObject : formChanges.gridObjects.keySet()) {
            models.get(groupObject).grid.setGridObjects(formChanges.gridObjects.get(groupObject));
        }

        for (Map.Entry<ClientGroupObjectImplementView,ClientGroupObjectValue> groupObject : formChanges.objects.entrySet())
            models.get(groupObject.getKey()).setCurrentGroupObject(groupObject.getValue(),false);

        for (ClientGroupObjectImplementView groupObject : formChanges.classViews.keySet())
            models.get(groupObject).setClassView(formChanges.classViews.get(groupObject));

        // Затем их свойства

        for (ClientPropertyView property : formChanges.panelProperties.keySet()) {
            models.get(property.groupObject).setPanelPropertyValue(property, formChanges.panelProperties.get(property));
        }

        for (ClientPropertyView property : formChanges.gridProperties.keySet()) {
            models.get(property.groupObject).setGridPropertyValues(property, formChanges.gridProperties.get(property));
        }

        formLayout.getComponent().validate();

        // выдадим сообщение если было от сервера
        if(formChanges.message.length()>0)
            Log.printFailedMessage(formChanges.message);        
    }

    public void changeGroupObject(ClientGroupObjectImplementView groupObject, ClientGroupObjectValue objectValue) throws IOException {

        ClientGroupObjectValue curObjectValue = models.get(groupObject).getCurrentObject();

        if (!objectValue.equals(curObjectValue)) {

            remoteForm.changeGroupObject(groupObject.getID(), Serializer.serializeClientGroupObjectValue(objectValue));

            models.get(groupObject).setCurrentGroupObject(objectValue,true);

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

        SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);

        if (property instanceof ClientPropertyView) {

            // типа только если меняется свойство
            remoteForm.changePropertyView(((ClientPropertyView)property).ID, BaseUtils.serializeObject(value));
            dataChanged();
            applyFormChanges();
        } else {

            ClientObjectImplementView object = ((ClientObjectView)property).object;
            remoteForm.changeObject(object.getID(), (Integer)value);

            models.get(property.getGroupObject()).setCurrentObject(object, (Integer)value);

            applyFormChanges();

            clientNavigator.changeCurrentClass(remoteForm,object);
        }

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

        if(groupObject.fixedClassView) return false;

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.switchClassView(groupObject.getID());

        applyFormChanges();

        return true;
    }

    public void changeOrder(ClientCellView property, Order modiType) throws IOException {

        if(property instanceof ClientPropertyView)
            remoteForm.changePropertyOrder(((ClientPropertyView)property).ID, modiType.serialize());
        else
            remoteForm.changeObjectOrder(((ClientObjectView)property).getID(), modiType.serialize());

        applyFormChanges();
    }

    public void changeFind(List<ClientPropertyFilter> conditions) {
    }

    private final Map<ClientGroupObjectImplementView, List<ClientPropertyFilter>> currentFilters = new HashMap();
    
    private void changeFilter(ClientGroupObjectImplementView groupObject, List<ClientPropertyFilter> conditions) throws IOException {

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

        Main.layout.defaultStation.drop(new ReportDockable(remoteForm.getID(), clientNavigator, remoteForm));

    }

    void refreshData() throws IOException {

        remoteForm.refreshData();

        applyFormChanges();
    }

    boolean saveChanges() throws IOException {

        if (remoteForm.hasSessionChanges()) {

            String message = remoteForm.saveChanges();
            if (message==null) {
                Log.printSuccessMessage("Изменения были удачно записаны...");
                dataChanged();
            }
            else {
                Log.printFailedMessage(message);
                return false;
            }

            applyFormChanges();
        }
        
        return true;
    }

    boolean cancelChanges() throws IOException {

        if (remoteForm.hasSessionChanges()) {

            remoteForm.cancelChanges();
            dataChanged();
            applyFormChanges();
        }

        return true;
    }

    public boolean okPressed() {
        try {
            return saveChanges();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при применении изменений", e);
        }
    }

    boolean closePressed() {
        try {
            return cancelChanges();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при отмене изменений", e);
        }
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

    public class GroupObjectModel {

        final ClientGroupObjectImplementView groupObject;

        final PanelModel panel;
        final GridController grid;
        public final Map<ClientObjectImplementView, ObjectController> objects = new HashMap();

        ClientGroupObjectValue currentObject;

        Boolean classView;

        public GroupObjectModel(ClientGroupObjectImplementView igroupObject) throws IOException {

            groupObject = igroupObject;

            grid = new GridController(groupObject.gridView);
            grid.addView(formLayout);

            panel = new PanelModel();

            for (ClientObjectImplementView object : groupObject) {

                objects.put(object, new ObjectController(object, ClientForm.this) {

                    @Override
                    protected void currentClassChanged() {
                        grid.gridTable.requestFocusInWindow(); // перейдем сразу на Grid
                    }
                });
                objects.get(object).addView(formLayout);
            }

        }
        
        public void setClassView(Boolean setClassView) {
            
            if (classView == null || classView != setClassView) {
                
                classView = setClassView;
                if (classView) {
                    panel.removeGroupObjectID();
                    grid.addGroupObjectID();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            grid.gridTable.requestFocusInWindow();
                        }
                    });
                } else {
                    panel.addGroupObjectID();
                    grid.removeGroupObjectID();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            panel.requestFocusInWindow();
                        }
                    });
//                    panel.requestFocusInWindow();
                }

                for (ClientObjectImplementView object : groupObject) {
                    objects.get(object).changeClassView(classView);
                }

            }
            
        }
        
        public void addPanelProperty(ClientPropertyView property) {
            
            grid.removeProperty(property);
            panel.addProperty(property);
            
        }
        
        public void addGridProperty(ClientPropertyView property) {
            
            panel.removeProperty(property);
            grid.addProperty(property);
            
        }
        
        public void dropProperty(ClientPropertyView property) {
            
            panel.removeProperty(property);
            grid.removeProperty(property);
            
        }
        
        public ClientGroupObjectValue getCurrentObject() {
            return currentObject;
        }
        
        public void setCurrentGroupObject(ClientGroupObjectValue value, Boolean userChange) {
    
            boolean realChange = !value.equals(currentObject);

/*            if (currentObject != null)
                System.out.println("view - oldval : " + currentObject.toString() + " ; userChange " + userChange.toString() );
            if (value != null)
                System.out.println("view - newval : " + value.toString() + " ; userChange " + userChange.toString());*/
            
            currentObject = value;
            
            if (realChange) {
                
                panel.selectObject(currentObject);
                if (!userChange) // если не сам изменил, а то пойдет по кругу
                    grid.selectObject(currentObject);
            }
            
        }

        public void setCurrentObject(ClientObjectImplementView object, Integer value) {

            if (currentObject == null) return;

            ClientGroupObjectValue curValue = (ClientGroupObjectValue) currentObject.clone();

            curValue.put(object, value);
            setCurrentGroupObject(curValue, false);
        }

        public void setPanelPropertyValue(ClientPropertyView property, Object value) {
            
            panel.setPropertyValue(property, value);
        }

        public void setGridPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue,Object> values) {
            
            grid.setPropertyValues(property, values);
        }

        // приходится делать именно так, так как логика отображения одного GroupObject може не совпадать с логикой Container-Component
        public void addGroupObjectActions(JComponent comp) {

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "switchClassView");
            comp.getActionMap().put("switchClassView", new AbstractAction() {

                public void actionPerformed(ActionEvent ae) {
                    try {
                        switchClassView(groupObject);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении вида", e);
                    }
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_DOWN_MASK), "addObject");
            comp.getActionMap().put("addObject", new AbstractAction() {

                public void actionPerformed(ActionEvent ae) {
                    ClientObjectClass addClass = objects.get(groupObject.get(0)).classController.getDerivedClass();
                    if(addClass instanceof ClientConcreteClass) {
                        try {
                            addObject(groupObject.get(0),(ClientConcreteClass)addClass);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при добавлении объекта", e);
                        }
                    }
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK), "removeObject");
            comp.getActionMap().put("removeObject", new AbstractAction() {

                public void actionPerformed(ActionEvent ae) {
                    try {
                        changeClass(groupObject.get(0), null);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при удалении объекта", e);
                    }
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_DOWN_MASK), "changeObjectClass");
            comp.getActionMap().put("changeObjectClass", new AbstractAction() {

                public void actionPerformed(ActionEvent ae) {
                    ClientObjectClass changeClass = objects.get(groupObject.get(0)).classController.getSelectedClass();
                    if(changeClass instanceof ClientConcreteClass) {
                        try {
                            changeClass(groupObject.get(0), (ClientConcreteClass) changeClass);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении класса объекта", e);
                        }
                    }
                }
            });

        }

        class PanelModel {

            final Map<ClientCellView, PanelCellController> controllers;

            public PanelModel() {

                controllers = new HashMap();
            }

            public void addGroupObjectID() {
                
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {

                        PanelCellController idController = new PanelCellController(object.objectIDView, ClientForm.this);
                        addGroupObjectActions(idController.getView());
                        idController.addView(formLayout);

                        controllers.put(object.objectIDView, idController);
                    }

                if (currentObject != null)
                    setGroupObjectIDValue(currentObject);
                
            }
            
            public void removeGroupObjectID() {
                
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) { 
                        PanelCellController idController = controllers.get(object.objectIDView);
                        if (idController != null) {
                            idController.removeView(formLayout);
                            controllers.remove(object.objectIDView);
                        }
                    }
            }

            public void requestFocusInWindow() {

                // так делать конечно немного неправильно, так как теоретически objectID может вообще не быть в панели
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {
                        PanelCellController idController = controllers.get(object.objectIDView);
                        if (idController != null) {
                            idController.getView().requestFocusInWindow();
                            return;
                        }
                    }
            }

            private void setGroupObjectIDValue(ClientGroupObjectValue value) {

                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {
                        PanelCellController idmodel = controllers.get(object.objectIDView);
                        if (idmodel != null)
                            idmodel.setValue(value.get(object));
                    }
            }

            private void selectObject(ClientGroupObjectValue value) {
                
                setGroupObjectIDValue(value);
            }
            
            public void addProperty(ClientPropertyView property) {
         
                if (controllers.get(property) == null) {
                    
                    PanelCellController propController = new PanelCellController(property, ClientForm.this);
                    addGroupObjectActions(propController.getView());
                    propController.addView(formLayout);
                    
                    controllers.put(property, propController);
                }
                
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                PanelCellController propController = controllers.get(property);
                if (propController != null) {
                    propController.removeView(formLayout);
                    controllers.remove(property);
                }
                
            }
            
            public void setPropertyValue(ClientPropertyView property, Object value) {
                
                PanelCellController propmodel = controllers.get(property);
                propmodel.setValue(value);
                
            }

        }

        class GridController implements LogicsSupplier {

            private final ClientGridView view;

            private final GridView gridView;

            private final FindController findController;
            private final FilterController filterController;

            private final GridTable gridTable;

            public GridController(ClientGridView iview) {

                view = iview;

                findController = new FindController(this) {

                    protected boolean queryChanged() {

                        changeFind(getConditions());

                        gridView.requestFocusInWindow();
                        return true;
                    }
                };

                filterController = new FilterController(this) {

                    protected boolean queryChanged() {

                        try {
                            changeFilter(groupObject, getConditions());
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при применении фильтра", e);
                        }

                        gridView.requestFocusInWindow();
                        return true;
                    }
                };

                gridView = new GridView(this, ClientForm.this, findController.getView(), filterController.getView());
                gridTable = gridView.getTable();

                if (!isReadOnly()) addGroupObjectActions(gridTable);

            }

            public void addView(ClientFormLayout formLayout) {
                formLayout.add(view, gridView);
            }

            private void addGroupObjectID() {
//                System.out.println("addGroupObjectID");
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                       gridTable.addColumn(object.objectIDView);

                // здесь еще добавить значения идентификаторов
                fillTableObjectID();

                gridTable.updateTable();
            }

            private void removeGroupObjectID() {
//                System.out.println("removeGroupObjectID");
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                        gridTable.removeColumn(object.objectIDView);
                gridTable.updateTable();
            }

            private void addProperty(ClientPropertyView property) {
//                System.out.println("addProperty " + property.toString());
                if (gridTable.addColumn(property))
                    gridTable.updateTable();
            }

            private void removeProperty(ClientPropertyView property) {
//                System.out.println("removeProperty " + property.toString());
                if (gridTable.removeColumn(property))
                    gridTable.updateTable();
            }

            private void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
                gridTable.setGridObjects(igridObjects);

                //здесь еще добавить значения идентификаторов
                fillTableObjectID();
            }

            private void selectObject(ClientGroupObjectValue currentObject) {
                gridTable.selectObject(currentObject);
            }

            private void setPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue, Object> values) {
                gridTable.setColumnValues(property, values);
            }

            private void fillTableObjectID() {
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {
                        Map<ClientGroupObjectValue, Object> values = new HashMap<ClientGroupObjectValue, Object>();
                        for (ClientGroupObjectValue value : gridTable.getGridRows())
                            values.put(value, value.get(object));
                        gridTable.setColumnValues(object.objectIDView, values);
                    }
            }

            public void changeGridOrder(ClientCellView property, Order modiType) throws IOException {
                gridTable.changeGridOrder(property, modiType);
            }

            // реализация LogicsSupplier

            public List<ClientObjectImplementView> getObjects() {

                ArrayList<ClientObjectImplementView> objects = new ArrayList<ClientObjectImplementView> ();
                for (ClientGroupObjectImplementView groupObject : formView.groupObjects)
                    for (ClientObjectImplementView object : groupObject)
                        objects.add(object);

                return objects;
            }

            public ClientGroupObjectImplementView getGroupObject() {
                return groupObject;
            }

            public List<ClientPropertyView> getGroupObjectProperties() {

                ArrayList<ClientPropertyView> properties = new ArrayList<ClientPropertyView>();
                for (ClientPropertyView property : formView.properties) {
                    if (groupObject.equals(property.groupObject))
                        properties.add(property);
                }

                return properties;
            }

            public List<ClientPropertyView> getProperties() {
                return formView.properties;
            }

            public List<ClientCellView> getCells() {
                return formView.order;
            }

            public ClientPropertyView getDefaultProperty() {

                ClientCellView currentCell = gridTable.getCurrentCell();
                if (currentCell instanceof ClientPropertyView)
                    return (ClientPropertyView) currentCell;
                else
                    return null;
            }

            public Object getSelectedValue(ClientPropertyView cell) {
                return gridTable.getSelectedValue(cell);
            }

            public ClientForm getForm() {
                return ClientForm.this;
            }

        }
    }
}