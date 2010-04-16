/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.client.*;
import platform.client.form.sort.GridHeaderRenderer;
import platform.client.form.sort.GridHeaderMouseListener;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.form.cell.CellController;
import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.client.form.cell.ClientCellViewTable;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
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

    void changeGroupObject(ClientGroupObjectImplementView groupObject, ClientGroupObjectValue objectValue) throws IOException {

        ClientGroupObjectValue curObjectValue = models.get(groupObject).getCurrentObject();

        if (!objectValue.equals(curObjectValue)) {

            remoteForm.changeGroupObject(groupObject.ID, Serializer.serializeClientGroupObjectValue(objectValue));

            models.get(groupObject).setCurrentGroupObject(objectValue,true);

            applyFormChanges();

            clientNavigator.changeCurrentClass(remoteForm,groupObject.get(0));
        }

    }

    void changeGroupObject(ClientGroupObjectImplementView groupObject, Scroll changeType) throws IOException {

        remoteForm.changeGroupObject(groupObject.ID, changeType.serialize());

        applyFormChanges();

        clientNavigator.changeCurrentClass(remoteForm,groupObject.get(0));
    }

    void changeProperty(ClientCellView property, Object value) throws IOException {

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

        remoteForm.switchClassView(groupObject.ID);

        applyFormChanges();

        return true;
    }

    void changeOrder(ClientCellView property, Order modiType) throws IOException {

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

    void changePageSize(ClientGroupObjectImplementView groupObject, int pageSize) throws IOException {

        remoteForm.changePageSize(groupObject.ID, pageSize);

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

    boolean okPressed() {
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
        final GridModel grid;
        public final Map<ClientObjectImplementView, ObjectController> objects = new HashMap();

        ClientGroupObjectValue currentObject;

        ClientCellView currentCell;

        Boolean classView;

        public GroupObjectModel(ClientGroupObjectImplementView igroupObject) throws IOException {

            groupObject = igroupObject;

            grid = new GridModel(groupObject.gridView);

            panel = new PanelModel();

            for (ClientObjectImplementView object : groupObject) {

                objects.put(object, new ObjectController(object, ClientForm.this) {

                    @Override
                    protected void currentClassChanged() {
                        grid.table.requestFocusInWindow(); // перейдем сразу на Grid
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
                            grid.table.requestFocusInWindow();
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

                        PanelCellController idController = new PanelCellController(object.objectIDView);
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
                    
                    PanelCellController propController = new PanelCellController(property);
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

            class PanelCellController extends CellController {
                
                public PanelCellController(ClientCellView ikey) {
                    super(ikey, ClientForm.this);

                    if (!readOnly) addGroupObjectActions(getView());
                }

                protected boolean cellValueChanged(Object ivalue) {

                    try {
                        changeProperty(getKey(), ivalue);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении значения свойства", e);
                    }

                    return true;
                }

            }
            
        }
        
        class GridModel {

            final ClientGridView view;

            final JPanel container;

            final JPanel queriesContainer;

            final JScrollPane pane;
            final GridBagConstraints paneConstraints;
            final Table table;

            public GridModel(ClientGridView iview) {

                view = iview;

                container = new JPanel();
                container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

                table = new Table();
                table.getTableHeader().setPreferredSize(new Dimension(1000, 34));
//                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//                table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

                pane = new JScrollPane(table);
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                table.setFillsViewportHeight(true);

                paneConstraints = new GridBagConstraints();
                paneConstraints.fill = GridBagConstraints.BOTH;
                paneConstraints.weightx = 1;
                paneConstraints.weighty = 1;
                paneConstraints.insets = new Insets(4,4,4,4); 

                queriesContainer = new JPanel();
                queriesContainer.setLayout(new BoxLayout(queriesContainer, BoxLayout.X_AXIS));

//              отключим поиски пока они не работают                
//                queriesContainer.add(table.findController.view);
//                queriesContainer.add(Box.createRigidArea(new Dimension(4,0)));
                queriesContainer.add(table.filterController.getView());
                queriesContainer.add(Box.createHorizontalGlue());

                container.add(pane);
                container.add(queriesContainer);

                if (!readOnly) addGroupObjectActions(table);

                table.findController.getView().addActions(table);
                table.filterController.getView().addActions(table);

            }

            private void addGroupObjectID() {
//                System.out.println("addGroupObjectID");
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                       table.addColumn(object.objectIDView);

                // здесь еще добавить значения идентификаторов
                fillTableObjectID();
                
                table.updateTable();
            }

            private void removeGroupObjectID() {
//                System.out.println("removeGroupObjectID");
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                        table.removeColumn(object.objectIDView);
                table.updateTable();
            }

            private void addProperty(ClientPropertyView property) {
//                System.out.println("addProperty " + property.toString());
                if (table.addColumn(property))
                    table.updateTable();
            }
            
            private void removeProperty(ClientPropertyView property) {
//                System.out.println("removeProperty " + property.toString());
                if (table.removeColumn(property))
                    table.updateTable();
            }

            private void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
                table.setGridObjects(igridObjects);
                
                //здесь еще добавить значения идентификаторов
                fillTableObjectID();
            }
            
            private void selectObject(ClientGroupObjectValue currentObject) {
                table.selectObject(currentObject);
            }

            private void setPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue, Object> values) {
                table.setColumnValues(property, values);
            }

            private void fillTableObjectID() {
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {
                        Map<ClientGroupObjectValue, Object> values = new HashMap<ClientGroupObjectValue, Object>();
                        for (ClientGroupObjectValue value : table.gridRows)
                            values.put(value, value.get(object));
                        table.setColumnValues(object.objectIDView, values);
                    }
            }

            final List<ClientCellView> orders = new ArrayList<ClientCellView>();
            final List<Boolean> orderDirections = new ArrayList<Boolean>();

            void changeGridOrder(ClientCellView property, Order modiType) throws IOException {

                changeOrder(property, modiType);

                int ordNum;
                switch(modiType) {
                    case REPLACE:
                        orders.clear();
                        orderDirections.clear();

                        orders.add(property);
                        orderDirections.add(true);
                        break;
                    case ADD:
                        orders.add(property);
                        orderDirections.add(true);
                        break;
                    case DIR:
                        ordNum = orders.indexOf(property);
                        orderDirections.set(ordNum, !orderDirections.get(ordNum));
                        break;
                    case REMOVE:
                        ordNum = orders.indexOf(property);
                        orders.remove(ordNum);
                        orderDirections.remove(ordNum);
                        break;
                }
            }

            public class Table extends ClientFormTable
                               implements ClientCellViewTable, LogicsSupplier {

                final List<ClientCellView> gridColumns = new ArrayList();
                List<ClientGroupObjectValue> gridRows = new ArrayList();
                final Map<ClientCellView,Map<ClientGroupObjectValue,Object>> gridValues = new HashMap();

                final Model model;
                final JTableHeader header;

                final FindController findController;
                final FilterController filterController;

                final int ID;

                @Override
                public int hashCode() {
                    return ID;
                }

                @Override
                public boolean equals(Object o) {
                    if (!(o instanceof Table))
                        return false;
                    return ((Table)o).ID == this.ID;
                }

                private void updateTable() {

                    createDefaultColumnsFromModel();
                    for (ClientCellView property : gridColumns) {

                        TableColumn column = getColumnModel().getColumn(gridColumns.indexOf(property));
                        column.setMinWidth(property.getMinimumWidth());
                        column.setPreferredWidth(property.getPreferredWidth());
                        column.setMaxWidth(property.getMaximumWidth());
                    }

                    if (gridColumns.size() != 0) {
                        formLayout.add(view, container);
                    } else {
                        formLayout.remove(view, container);
                    }

                }

                public List<ClientObjectImplementView> getObjects() {

                    ArrayList<ClientObjectImplementView> objects = new ArrayList<ClientObjectImplementView> ();
                    for (ClientGroupObjectImplementView groupObject : formView.groupObjects)
                        for (ClientObjectImplementView object : groupObject)
                            objects.add(object);

                    return objects;
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

                public ClientPropertyView getDefaultProperty() {

                    if (currentCell instanceof ClientPropertyView)
                        return (ClientPropertyView) currentCell; 
                    else
                        return null;
                }

                public Object getSelectedValue(ClientPropertyView cell) {
                    return getSelectedValue(gridColumns.indexOf(cell));
                }

                private boolean fitWidth() {

                    int minWidth = 0;
                    int columnCount = getColumnCount();
                    TableColumnModel columnModel = getColumnModel();

                    for (int i = 0; i < columnCount; i++)
                        minWidth += columnModel.getColumn(i).getMinWidth();

//                    System.out.println(this + " ~ " + groupObject.toString() + " : " + minWidth + " - " + pane.getWidth());

                    return (minWidth < pane.getWidth());
                }

                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return fitWidth();
                }

                private int pageSize = 50;

                @Override
                public void doLayout() {

//                    System.out.println(this + " ~ " + groupObject.toString() + " : " + minWidth + " - " + pane.getWidth());

                    if (fitWidth()) {
                        autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
                    } else {
                        autoResizeMode = JTable.AUTO_RESIZE_OFF;
                    }
                    super.doLayout();
                }

                protected boolean processKeyBinding(KeyStroke ks, KeyEvent ae, int condition, boolean pressed) {

                    try {
                        // Отдельно обработаем CTRL + HOME и CTRL + END
                        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK))) {
                            changeGroupObject(groupObject, Scroll.HOME);
                            return true;
                        }

                        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK))) {
                            changeGroupObject(groupObject, Scroll.END);
                            return true;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при переходе на запись", e);
                    }

                    if (readOnly && ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))) return false;

                    return super.processKeyBinding(ks, ae, condition, pressed);    //To change body of overridden methods use File | Settings | File Templates.
                }

                public Table() {

                    ID = groupObject.ID;

                    model = new Model();
                    setModel(model);

                    header = getTableHeader();

                    findController = new FindController(this) {

                        protected boolean queryChanged() {

                            changeFind(getConditions());

                            table.requestFocusInWindow();
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

                            table.requestFocusInWindow();
                            return true;
                        }
                    };

                    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
//                            System.out.println("changeSel");
                            final ClientGroupObjectValue changeObject = model.getSelectedObject();
                            assert changeObject!=null;
                            SwingUtils.invokeLaterSingleAction(groupObject.getActionID()
                                    , new ActionListener() {
                                public void actionPerformed(ActionEvent ae) {
                                    try {
                                        if(changeObject.equals(model.getSelectedObject()))
                                            changeGroupObject(groupObject, model.getSelectedObject());
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при изменении текущего объекта", e);
                                    }
                                }
                            }, 50);
                        }
                    });

                    getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            currentCell = model.getSelectedCell();
                        }
                    });

                    header.setDefaultRenderer(new GridHeaderRenderer(header.getDefaultRenderer()) {

                        protected Boolean getSortDirection(int column) {
                            return Table.this.getSortDirection(getCellView(column));
                        }
                    });

                    header.addMouseListener(new GridHeaderMouseListener() {

                        protected Boolean getSortDirection(int column) {
                            return Table.this.getSortDirection(getCellView(column));
                        }

                        protected TableColumnModel getColumnModel() {
                            return Table.this.getColumnModel();
                        }

                        protected void changeOrder(int column, Order modiType) {

                            try {
                                changeGridOrder(getCellView(column), modiType);
                            } catch (IOException e) {
                                throw new RuntimeException("Ошибка изменении сортировки", e);
                            }

                            header.repaint();
                        }
                    });

                    setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
                    setDefaultEditor(Object.class, new ClientAbstractCellEditor());

                    addComponentListener(new ComponentAdapter() {
                        public void componentResized(ComponentEvent ce) {
                            int newPageSize = pane.getViewport().getHeight() / getRowHeight() + 1;
//                            System.out.println(groupObject.toString() + pane.getViewport().getHeight() + " - " + getRowHeight() + " ; " + pageSize + " : " + newPageSize);
                            if (newPageSize != pageSize) {
                                try {
                                    changePageSize(groupObject, newPageSize);
                                    pageSize = newPageSize;
                                } catch (IOException e) {
                                    throw new RuntimeException("Ошибка при изменении размера страницы", e);
                                }
                            }
                        }
                    });

                    addMouseListener(new MouseAdapter() {

                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (readOnly && e.getClickCount() > 1) okPressed();
                        }
                    });

                }

                public boolean addColumn(ClientCellView property) {

                    if (gridColumns.indexOf(property) == -1) {
                        Iterator<ClientCellView> icp = gridColumns.iterator();

                        // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
                        int ind = formView.order.indexOf(property), ins = 0;

                        while (icp.hasNext() && formView.order.indexOf(icp.next()) < ind) { ins++; }

                        gridColumns.add(ins, property);

                        return true;
                        
                    } else
                        return false;


                }

                public boolean removeColumn(ClientCellView property) {

                    if (gridColumns.remove(property)) {

                        gridValues.remove(property);
                        return true;
                    }
                    
                    return false;

                }

                public void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
               
                    int oldindex = gridRows.indexOf(currentObject);

                    gridRows = igridObjects;

                    // так делается, потому что почему-то сам JTable ну ни в какую не хочет изменять свою высоту (getHeight())
                    // приходится это делать за него, а то JViewPort смотрит именно на getHeight()
                    table.setSize(table.getWidth(), table.getRowHeight() * table.getRowCount());

                    final int newindex = gridRows.indexOf(currentObject);

                    //надо сдвинуть ViewPort - иначе дергаться будет

                    if (newindex != -1) {

                        if (oldindex != -1 && newindex != oldindex) {

                            final Point ViewPos = pane.getViewport().getViewPosition();
                            final int dltpos = (newindex-oldindex) * getRowHeight();
                            ViewPos.y += dltpos;
                            if (ViewPos.y < 0) ViewPos.y = 0;
                            pane.getViewport().setViewPosition(ViewPos);
                        }

                        selectRow(newindex);
                    }

                }

                public void selectObject(ClientGroupObjectValue value) {

                    int oldindex = getSelectionModel().getLeadSelectionIndex();
                    int newindex = gridRows.indexOf(value);
                    if (newindex != -1 && newindex != oldindex) {
                        //Выставляем именно первую активную колонку, иначе фокус на таблице - вообще нереально увидеть
                        selectRow(newindex);
                    }
                }

                public void setColumnValues(ClientCellView property, Map<ClientGroupObjectValue,Object> values) {

                    gridValues.put(property, values);
                    repaint();

                }


                private Object getSelectedValue(int col) {

                    int row = getSelectedRow();
                    if (row != -1 && row < getRowCount() && col != -1 && col < getColumnCount())
                        return getValueAt(row, col);
                    else
                        return null;
                }

                // ---------------------------------------------------------------------------------------------- //
                // -------------------------------------- Поиски и отборы --------------------------------------- //
                // ---------------------------------------------------------------------------------------------- //

                // ---------------------------------------------------------------------------------------------- //
                // ------------------------------------------- Модель данных ------------------------------------ //
                // ---------------------------------------------------------------------------------------------- //

                class Model extends AbstractTableModel {

                    public String getColumnName(int col) {
                          return gridColumns.get(col).caption;
                    }

                    public int getRowCount() {
                        return gridRows.size();
                    }

                    public int getColumnCount() {
                        return gridColumns.size();
                    }

                    public boolean isCellEditable(int row, int col) {
                        return true;
                    }

                    public Object getValueAt(int row, int col) {

                        return gridValues.get(gridColumns.get(col)).get(gridRows.get(row));
                    }
                    
                    public void setValueAt(Object value, int row, int col) {

                        // частный случай - не работает если меняется не само это свойство, а какое-то связанное
                        if (BaseUtils.nullEquals(value, getValueAt(row, col))) return;

                        try {
                            changeProperty(gridColumns.get(col), value);
                        } catch (IOException e) {
                            throw new RuntimeException("Ошибка при изменении значения свойства", e);
                        }
                    }
                    
                    public ClientGroupObjectValue getSelectedObject() {
                        int rowModel = convertRowIndexToModel(getSelectedRow());
                        if (rowModel < 0 || rowModel >= getRowCount())
                            return null;

                        return gridRows.get(rowModel);
                    }

                    public ClientCellView getSelectedCell() {

                        int colView = getSelectedColumn();
                        if (colView < 0 || colView >= getColumnCount())
                            return null;

                        int colModel = convertColumnIndexToModel(colView);
                        if (colModel < 0)
                            return null;

                        return gridColumns.get(colModel);
                    }
                }

                public boolean isDataChanging() {
                    return true;
                }

                public ClientCellView getCellView(int col) {
                    return gridColumns.get(col);
                }

                public ClientForm getForm() {
                    return ClientForm.this;
                }

                private Boolean getSortDirection(ClientCellView property) {
                    int ordNum = orders.indexOf(property);
                    return (ordNum != -1) ? orderDirections.get(ordNum) : null;
                }
            }
            
        }
        
    }
}