/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform.client.form;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.client.*;
import platform.client.logics.*;
import platform.client.logics.filter.ClientPropertyFilter;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientConcreteClass;
import platform.client.layout.ReportDockable;
import platform.client.navigator.ClientNavigator;
import platform.interop.Compare;
import platform.interop.CompressingInputStream;
import platform.interop.Scroll;
import platform.interop.Order;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.layout.SimplexLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
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

    // Icons - загружаем один раз, для экономии
    private final ImageIcon arrowUpIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/arrowup.gif"));
    private final ImageIcon arrowDownIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/arrowdown.gif"));
    private final ImageIcon filtIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/filt.gif"));
    private final ImageIcon filtAddIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/filtadd.gif"));
    private final ImageIcon findIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/find.gif"));
    private final ImageIcon findAddIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/findadd.gif"));
    private final ImageIcon deleteIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/delete.gif"));
    private final ImageIcon collapseIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/collapse.gif"));
    private final ImageIcon expandIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/expand.gif"));

    private final static Dimension iconButtonDimension = new Dimension(22,22);

    public ClientForm(RemoteFormInterface iremoteForm, ClientNavigator iclientNavigator) throws IOException, ClassNotFoundException {
//        super(app);

//        FocusOwnerTracer.installFocusTracer();

        // Форма нужна, чтобы с ней общаться по поводу данных и прочих
        remoteForm = iremoteForm;

        // Навигатор нужен, чтобы уведомлять его об изменениях активных объектов, чтобы он мог себя переобновлять
        clientNavigator = iclientNavigator;

//        getFrame().setTitle(caption);

        formView = ClientObjectProxy.retrieveClientFormView(remoteForm);

        initializeForm();

        applyFormChanges();

    }

    // ------------------------------------------------------------------------------------ //
    // ----------------------------------- Инициализация ---------------------------------- //
    // ------------------------------------------------------------------------------------ //

    private boolean hasFocus = false;

    private FormLayout formLayout;

    public Map<ClientGroupObjectImplementView, GroupObjectModel> models;

    private JButton buttonPrint;
    private JButton buttonRefresh;
    private JButton buttonApply;
    private JButton buttonCancel;
    private JButton buttonOK;
    private JButton buttonClose;

    void initializeForm() throws IOException {

        formLayout = new FormLayout(formView.containers);

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

        formLayout.add(formView.printView, buttonPrint);

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

        formLayout.add(formView.refreshView, buttonRefresh);

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

        formLayout.add(formView.applyView, buttonApply);

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

        formLayout.add(formView.cancelView, buttonCancel);

        AbstractAction okAction = new AbstractAction("OK") {

            public void actionPerformed(ActionEvent ae) {
                try {
                    okPressed();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при применении изменений", e);
                }
            }
        };

        KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
        im.put(altEnter, "okPressed");
        am.put("okPressed", okAction);

        buttonOK = new JButton(okAction);
        formLayout.add(formView.okView, buttonOK);

        AbstractAction closeAction = new AbstractAction("Закрыть") {

            public void actionPerformed(ActionEvent ae) {
                try {
                    closePressed();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при отмене изменений", e);
                }
            }
        };

        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(escape, "closePressed");
        am.put("closePressed", closeAction);

        buttonClose = new JButton(closeAction);
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

            clientNavigator.changeCurrentClass(remoteForm.getObjectClassID(groupObject.get(0).ID));
        }

    }

    void changeGroupObject(ClientGroupObjectImplementView groupObject, Scroll changeType) throws IOException {

        remoteForm.changeGroupObject(groupObject.ID, changeType.serialize());

        applyFormChanges();

        clientNavigator.changeCurrentClass(remoteForm.getObjectClassID(groupObject.get(0).ID));
    }

    void changeProperty(ClientCellView property, Object value, boolean externalID) throws IOException {

        SwingUtils.stopSingleAction(property.getGroupObject().getActionID(), true);

        if (property instanceof ClientPropertyView) {

            // типа только если меняется свойство
            remoteForm.changePropertyView(((ClientPropertyView)property).ID, BaseUtils.serializeObject(value), externalID);
            dataChanged();
            applyFormChanges();
        } else {

            ClientObjectImplementView object = ((ClientObjectView)property).object;
            remoteForm.changeObject(object.ID, (Integer)value);

            models.get(property.getGroupObject()).setCurrentObject(object, (Integer)value);

            applyFormChanges();

            clientNavigator.changeCurrentClass(remoteForm.getObjectClassID(object.ID));
        }

    }

    void addObject(ClientObjectImplementView object, ClientConcreteClass cls) throws IOException {
        
        remoteForm.addObject(object.ID, cls.ID);
        dataChanged();

        applyFormChanges();
    }

    void changeClass(ClientObjectImplementView object, ClientConcreteClass cls) throws IOException {

        SwingUtils.stopSingleAction(object.groupObject.getActionID(), true);

        remoteForm.changeClass(object.ID, (cls == null) ? -1 : cls.ID);
        dataChanged();

        applyFormChanges();
    }

    void changeGridClass(ClientObjectImplementView object, ClientObjectClass cls) throws IOException {

        remoteForm.changeGridClass(object.ID, cls.ID);
        applyFormChanges();
    }

    public void switchClassView(ClientGroupObjectImplementView groupObject) throws IOException {

        SwingUtils.stopSingleAction(groupObject.getActionID(), true);

        remoteForm.switchClassView(groupObject.ID);

        applyFormChanges();
    }

    void changeOrder(ClientCellView property, Order modiType) throws IOException {

        if(property instanceof ClientPropertyView)
            remoteForm.changePropertyOrder(((ClientPropertyView)property).ID, modiType.serialize());
        else
            remoteForm.changeObjectOrder(((ClientObjectView)property).getID(), modiType.serialize());

        applyFormChanges();
    }

    private void changeFind(List<ClientPropertyFilter> conditions) {
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

    boolean okPressed() throws IOException {
        return saveChanges();
    }

    boolean closePressed() throws IOException {
        return cancelChanges();
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
        public final Map<ClientObjectImplementView, ObjectModel> objects = new HashMap();

        ClientGroupObjectValue currentObject;

        ClientCellView currentCell;

        Boolean classView;

        public GroupObjectModel(ClientGroupObjectImplementView igroupObject) throws IOException {

            groupObject = igroupObject;

            grid = new GridModel(groupObject.gridView);

            panel = new PanelModel();

            for (ClientObjectImplementView object : groupObject) {

                objects.put(object, new ObjectModel(object));
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
                            Component component = panel.getVisibleComponent();
                            if(component!=null) component.requestFocusInWindow();
                        }
                    });
//                    panel.requestFocusInWindow();
                }

                for (ClientObjectImplementView object : groupObject) {
                    objects.get(object).classViewChanged();
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
                    ClientObjectClass addClass = objects.get(groupObject.get(0)).classModel.getDerivedClass();
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
                    ClientObjectClass changeClass = objects.get(groupObject.get(0)).classModel.getSelectedClass();
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


        // приходится наследоваться от JComponent только для того, чтобы поддержать updateUI
        class ClientAbstractCellRenderer extends JComponent
                                         implements TableCellRenderer {


            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, 
                                                           boolean isSelected, 
                                                           boolean hasFocus, 
                                                           int row, 
                                                           int column) {
                
                ClientCellView property = ((ClientCellViewTable)table).getCellView(column);
                PropertyRendererComponent currentComp = property.getRendererComponent();
                currentComp.setValue(value, isSelected, hasFocus);

                JComponent comp = currentComp.getComponent();

                renderers.add(comp);

                return comp;
            }

            final List<JComponent> renderers = new ArrayList();
            @Override
            public void updateUI() {
                for (JComponent comp : renderers)
                    comp.updateUI();
            }
                        
        }
        
        class ClientAbstractCellEditor extends AbstractCellEditor 
                                 implements TableCellEditor {

            PropertyEditorComponent currentComp;

            boolean externalID;

            public Object getCellEditorValue() {
                try {
                    return currentComp.getCellEditorValue();
                } catch (RemoteException e) {
                    throw new RuntimeException("Ошибка при получении выбранного значения", e);
                }
            }

            public boolean isCellEditable(EventObject e) {

                if (e instanceof KeyEvent) {

                    KeyEvent event = (KeyEvent) e;

                    externalID = (event.getKeyCode() == KeyEvent.VK_F12 && event.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK);
                    if (externalID) return true;

                    if (event.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;

                    // ESC почему-то считается KEY_TYPED кнопкой, пока обрабатываем отдельно
                    if (event.getKeyCode() == KeyEvent.VK_ESCAPE) return false;

                    //будем считать, что если нажата кнопка ALT то явно пользователь не хочет вводить текст
                    if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0) return false;

                    return true;
                }

                if (e instanceof MouseEvent) {

                    MouseEvent event = (MouseEvent) e;

                    if (event.getClickCount() < 2) return false;

                    externalID = (event.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK + MouseEvent.SHIFT_DOWN_MASK);
                    if (externalID) return true;

                    return true;
                }

                return false;
            }

            public Component getTableCellEditorComponent(JTable table,
                                                         Object ivalue, 
                                                         boolean isSelected, 
                                                         int row, 
                                                         int column) {
                
                ClientCellViewTable cellTable = (ClientCellViewTable)table;

                ClientCellView property = cellTable.getCellView(column);

                try {
                    currentComp = property.getEditorComponent(ClientForm.this, ivalue, cellTable.isDataChanging(), externalID);
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при получении редактируемого значения", e);
                }

                Component comp = null;
                if (currentComp != null) {
                    comp = currentComp.getComponent();

                    if (comp == null && currentComp.valueChanged()) {

                        Object newValue = getCellEditorValue();
                        if (!BaseUtils.nullEquals(ivalue, newValue))
                            table.setValueAt(newValue, row, column);
                    }
                }

                if (comp != null) {
                    return comp;
                } else {
                    this.stopCellEditing();
                    return null;
                }
            }

        }

        class CellModel {

            ClientCellView key;
            Object value;

            final CellView view;

            public CellModel(ClientCellView ikey) {

                view = new CellView();

                setKey(ikey);
            }

            public void setKey(ClientCellView ikey) {

                key = ikey;

                view.keyChanged();

                view.repaint();
            }

            public void setValue(Object ivalue) {
                value = ivalue;

                view.repaint();
            }

            boolean isDataChanging() {
                return true;
            }

            void cellValueChanged(Object ivalue) {
                value = ivalue;
            }

            class CellView extends JPanel {

                final JLabel label;
                final CellTable table;

                int ID;

                @Override
                public int hashCode() {
                    return ID;
                }

                @Override
                public boolean equals(Object o) {
                    return o instanceof CellView && ((CellView) o).ID == this.ID;
                }

                public CellView() {

                    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

                    label = new JLabel();
                    label.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
                    add(label);

//                    add(Box.createRigidArea(new Dimension(4,0)));

                    table = new CellTable();
                    table.setBorder(BorderFactory.createLineBorder(Color.gray));

                    add(table);

                }

                public void keyChanged() {

                    ID = key.getID();

                    label.setText(key.caption);

                    table.keyChanged();
                }

                class CellTable extends SingleCellTable
                                    implements ClientCellViewTable {

                    final PropertyModel model;

                    public CellTable() {
                        super();

                        model = new PropertyModel();
                        setModel(model);

                        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
                        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

                    }

                    public void keyChanged() {

                        setMinimumSize(key.getMinimumSize());
                        setPreferredSize(key.getPreferredSize());
                        setMaximumSize(key.getMaximumSize());
                    }

                    class PropertyModel extends AbstractTableModel {

                        public int getRowCount() {
                            return 1;
                        }

                        public int getColumnCount() {
                            return 1;
                        }

                        public boolean isCellEditable(int row, int col) {
                            return true;
                        }

                        public Object getValueAt(int row, int col) {
//                            if (value != null)
                                return value;
//                            else
//                                return (String)"";
                        }

                        public void setValueAt(Object value, int row, int col) {
//                            System.out.println("setValueAt");
                            if (BaseUtils.nullEquals(value, getValueAt(row, col))) return;
                            cellValueChanged(value);
                        }

                    }

                    public boolean isDataChanging() {
                        return CellModel.this.isDataChanging();
                    }

                    public ClientCellView getCellView(int col) {
                        return key;
                    }

                }

            }

        }

        class PanelModel {
            
            final Map<ClientCellView, PanelCellModel> models;
            
            public PanelModel() {

                models = new HashMap();
            }

            public void addGroupObjectID() {
                
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {                    
                        PanelCellModel idmodel = new PanelCellModel(object.objectIDView);
                        formLayout.add(idmodel.key, idmodel.view);

                        models.put(object.objectIDView, idmodel);
                    }

                if (currentObject != null)
                    setGroupObjectIDValue(currentObject);
                
            }
            
            public void removeGroupObjectID() {
                
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) { 
                        PanelCellModel idmodel = models.get(object.objectIDView);
                        if (idmodel != null) {
                            formLayout.remove(idmodel.key, idmodel.view);
                            models.remove(object.objectIDView);
                        }
                    }
            }

            private Component getVisibleComponent() {
                for(ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                        return models.get(object.objectIDView).view.table;
                return null;
            }

            private void setGroupObjectIDValue(ClientGroupObjectValue value) {

                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show) {
                        PanelCellModel idmodel = models.get(object.objectIDView);
                        if (idmodel != null)
                            idmodel.setValue(value.get(object));
                    }
            }

            private void selectObject(ClientGroupObjectValue value) {
                
                setGroupObjectIDValue(value);
            }
            
            public void addProperty(ClientPropertyView property) {
         
                if (models.get(property) == null) {
                    
                    PanelCellModel propmodel = new PanelCellModel(property);
                    formLayout.add(propmodel.key, propmodel.view);

                    models.put(property, propmodel);
                }
                
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                PanelCellModel propmodel = models.get(property);
                if (propmodel != null) {
                    formLayout.remove(propmodel.key, propmodel.view);
                    models.remove(property);
                }
                
            }
            
            public void setPropertyValue(ClientPropertyView property, Object value) {
                
                PanelCellModel propmodel = models.get(property);
                propmodel.setValue(value);
                
            }

            class PanelCellModel extends CellModel {
                
                public PanelCellModel(ClientCellView ikey) {
                    super(ikey);

                    addGroupObjectActions(view);
                }

                protected void cellValueChanged(Object ivalue) {

                    boolean externalID = false;
                    TableCellEditor cellEditor = view.table.getCellEditor();
                    if (cellEditor instanceof ClientAbstractCellEditor && ((ClientAbstractCellEditor)cellEditor).externalID)
                        externalID = true;

                    try {
                        changeProperty(key, ivalue, externalID);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении значения свойства", e);
                    }
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
//                queriesContainer.add(table.findModel.queryView);
//                queriesContainer.add(Box.createRigidArea(new Dimension(4,0)));
                queriesContainer.add(table.filterModel.queryView);
                queriesContainer.add(Box.createHorizontalGlue());

                container.add(pane);
                container.add(queriesContainer);

                addGroupObjectActions(table);

                container.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "addFind");
                container.getActionMap().put("addFind", new AbstractAction() {

                    public void actionPerformed(ActionEvent ae) {
                        table.findModel.addCondition();
                    }
                });

                container.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "addFilter");
                container.getActionMap().put("addFilter", new AbstractAction() {

                    public void actionPerformed(ActionEvent ae) {
                        table.filterModel.addCondition();
                    }
                });

            }

            private void addGroupObjectID() {
//                System.out.println("addGroupObjectID");
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                       table.addColumn(object.objectIDView);

                // здесь еще добавить значения идентификаторов
                fillTableObjectID();
                
                updateTable();
            }

            private void removeGroupObjectID() {
//                System.out.println("removeGroupObjectID");
                for (ClientObjectImplementView object : groupObject)
                    if(object.objectIDView.show)
                        table.removeColumn(object.objectIDView);
                updateTable();
            }

            private void addProperty(ClientPropertyView property) {
//                System.out.println("addProperty " + property.toString());
                if (table.addColumn(property))
                    updateTable();
            }
            
            private void removeProperty(ClientPropertyView property) {
//                System.out.println("removeProperty " + property.toString());
                if (table.removeColumn(property))
                    updateTable();
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
            
            private void updateTable() {

//                System.out.println("CreateColumns");
                table.createDefaultColumnsFromModel();
                for (ClientCellView property : table.gridColumns) {

                    TableColumn column = table.getColumnModel().getColumn(table.gridColumns.indexOf(property));
                    column.setMinWidth(property.getMinimumWidth());
                    column.setPreferredWidth(property.getPreferredWidth());
                    column.setMaxWidth(property.getMaximumWidth());
                }

                if (table.gridColumns.size() != 0) {
                    formLayout.add(view, container);
                } else {
                    formLayout.remove(view, container);
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
                               implements ClientCellViewTable {

                final List<ClientCellView> gridColumns = new ArrayList();
                List<ClientGroupObjectValue> gridRows = new ArrayList();
                final Map<ClientCellView,Map<ClientGroupObjectValue,Object>> gridValues = new HashMap();

                final Model model;
                final JTableHeader header;

                final FindModel findModel;
                final FilterModel filterModel;

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

                    return super.processKeyBinding(ks, ae, condition, pressed);    //To change body of overridden methods use File | Settings | File Templates.
                }

                public Table() {

                    ID = groupObject.ID;

                    model = new Model();
                    setModel(model);

                    header = getTableHeader();

                    findModel = new FindModel();
                    filterModel = new FilterModel();

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

                    header.setDefaultRenderer(new GridHeaderRenderer(header.getDefaultRenderer()));
                    header.addMouseListener(new GridHeaderMouseListener());

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

                private Object getSelectedValue(ClientCellView cell) {
                    return getSelectedValue(gridColumns.indexOf(cell));
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

                private abstract class QueryModel {

                    public final QueryView queryView;

                    final List<ClientPropertyFilter> conditions;
                    final Map<ClientPropertyFilter, QueryConditionView> conditionViews;

                    boolean hasChanged = false;

                    public QueryModel() {

                        conditions = new ArrayList();
                        conditionViews = new HashMap();

                        queryView = new QueryView();
                    }

                    public void applyQuery() throws IOException {

                        hasChanged = false;

                        queryView.conditionsChanged();

                        table.requestFocusInWindow();

                    }

                    public void addCondition() {

                        queryView.collapsed = false;
                        
                        hasChanged = true;

                        ClientPropertyFilter condition = new ClientPropertyFilter();
                        conditions.add(condition);

                        QueryConditionView conditionView = new QueryConditionView(condition);
                        queryView.condviews.add(conditionView);

                        conditionViews.put(condition, conditionView);

                        queryView.conditionsChanged();

                        conditionView.valueView.requestFocusInWindow();

//                        container.validate();
                    }

                    public void removeCondition(ClientPropertyFilter condition) {

                        hasChanged = true;
                        
                        conditions.remove(condition);

                        queryView.condviews.remove(conditionViews.get(condition));
                        conditionViews.remove(condition);
                        
                        queryView.conditionsChanged();

//                        container.validate();
                    }

                    public void removeAllConditions() {

                        hasChanged = true;

                        conditions.clear();
                        conditionViews.clear();

                        queryView.condviews.removeAll();

                        queryView.conditionsChanged();
                    }

                    protected class QueryConditionView extends JPanel {

                        final ClientPropertyFilter filter;

                        final JComboBox propertyView;

                        final JComboBox compareView;

                        final JComboBox classValueLinkView;

                        ClientValueLinkView valueView;
                        final Map<ClientValueLink, ClientValueLinkView> valueViews;

                        final JButton delButton;

                        public final int PREFERRED_HEIGHT = 18;

                        class QueryConditionComboBox extends JComboBox {

                            public Dimension getPreferredSize() {
                                Dimension dim = super.getPreferredSize();
                                dim.height = PREFERRED_HEIGHT;
                                return dim;
                            }

                            public QueryConditionComboBox(Vector<?> objects) {
                                super(objects);
                            }

                            public QueryConditionComboBox(Object[] objects) {
                                super(objects);
                            }
                        }

                        public QueryConditionView(ClientPropertyFilter ifilter) {

                            filter = ifilter;

                            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

                            Vector<ClientPropertyView> sources = new Vector();
                            for (ClientPropertyView property : formView.properties)
                                if (property.groupObject == groupObject) {
                                    sources.add(property);
                                }

                            propertyView = new QueryConditionComboBox(sources);
                            add(propertyView);

                            if (currentCell instanceof ClientPropertyView)
                                propertyView.setSelectedItem(currentCell);
                            
                            filter.property = (ClientPropertyView) propertyView.getSelectedItem();

                            propertyView.addItemListener(new ItemListener() {

                                public void itemStateChanged(ItemEvent ie) {

                                    filter.property = (ClientPropertyView)ie.getItem();
                                    filterChanged();
                                }
                            });

//                            add(Box.createHorizontalStrut(4));

                            Pair<String,Compare>[] comparisons = new Pair[] {new Pair("=", Compare.EQUALS), new Pair(">", Compare.GREATER), new Pair("<", Compare.LESS),
                                                                             new Pair(">=", Compare.GREATER_EQUALS), new Pair("<=", Compare.LESS_EQUALS), new Pair("<>", Compare.NOT_EQUALS)};
                            compareView = new QueryConditionComboBox(comparisons);
                            add(compareView);

                            filter.compare = ((Pair<String,Compare>) compareView.getSelectedItem()).second;

                            compareView.addItemListener(new ItemListener() {

                                public void itemStateChanged(ItemEvent e) {
                                    filter.compare = ((Pair<String,Compare>)e.getItem()).second;

                                    hasChanged = true;
                                    queryView.dataChanged();
                                }
                            });
//                            add(Box.createHorizontalStrut(4));

                            valueViews = new HashMap<ClientValueLink, ClientValueLinkView>();
                            
                            ClientUserValueLink userValue = new ClientUserValueLink();
//                            userValue.value = table.getSelectedValue();
                            ClientUserValueLinkView userView = new ClientUserValueLinkView(userValue, filter.property);
                            valueViews.put(userValue, userView);

                            ClientObjectValueLink objectValue = new ClientObjectValueLink();
                            ClientObjectValueLinkView objectView = new ClientObjectValueLinkView(objectValue);
                            valueViews.put(objectValue, objectView);

                            ClientPropertyValueLink propertyValue = new ClientPropertyValueLink();
                            ClientPropertyValueLinkView propertyValueView = new ClientPropertyValueLinkView(propertyValue);
                            valueViews.put(propertyValue, propertyValueView);

                            ClientValueLink[] classes = new ClientValueLink[] {userValue, objectValue, propertyValue};
                            classValueLinkView = new QueryConditionComboBox(classes);
                            add(classValueLinkView);

                            filter.value = (ClientValueLink)classValueLinkView.getSelectedItem();

                            classValueLinkView.addItemListener(new ItemListener() {

                                public void itemStateChanged(ItemEvent ie) {
                                    filter.value = (ClientValueLink)classValueLinkView.getSelectedItem();
                                    filterChanged();
                                }
                            });

//                            add(Box.createHorizontalStrut(4));

                            delButton = new JButton(deleteIcon);
                            delButton.setFocusable(false);
                            delButton.setPreferredSize(new Dimension(PREFERRED_HEIGHT, PREFERRED_HEIGHT));
                            delButton.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    removeCondition(filter);
                                }
                            });

                            filterChanged();

                            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), "applyQuery");
                            getActionMap().put("applyQuery", new AbstractAction() {

                                public void actionPerformed(ActionEvent ae) {
                                    valueView.stopEditing();
                                    try {
                                        applyQuery();
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при применении фильтра", e);
                                    }
                                }
                            });

                            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK), "removeAll");
                            getActionMap().put("removeAll", new AbstractAction() {

                                public void actionPerformed(ActionEvent ae) {
                                    removeAllConditions();
                                    try {
                                        applyQuery();
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при применении фильтра", e);
                                    }
                                }
                            });

                        }

                        public void filterChanged() {

                            if (valueView != null)
                                remove(valueView);

                            valueView = valueViews.get(filter.value);

                            if (valueView != null) {
                                add(valueView);
                                valueView.propertyChanged(filter.property);
                            }
                            
                            add(delButton);

                            hasChanged = true;
                            queryView.dataChanged();

                            container.validate();

                        }

                        private abstract class ClientValueLinkView extends JPanel {

                            public ClientValueLinkView() {

                                setLayout(new BorderLayout());
                            }

                            abstract public void propertyChanged(ClientPropertyView property) ;

                            public void stopEditing() {}

                        }

                        private class ClientUserValueLinkView extends ClientValueLinkView {

                            final ClientUserValueLink valueLink;

                            final CellModel cell;

                            public ClientUserValueLinkView(ClientUserValueLink ivalueLink, ClientPropertyView iproperty) {
                                super();

                                valueLink = ivalueLink;

                                cell = new CellModel(iproperty) {

                                    protected boolean isDataChanging() {
                                        return false;
                                    }

                                    protected void cellValueChanged(Object ivalue) {
                                        super.cellValueChanged(ivalue);
                                        
                                        valueLink.value = ivalue;

                                        hasChanged = true;
                                        queryView.dataChanged();
                                    }

                                };

                                cell.setValue(valueLink.value);

                                JComboBox compBorder = new JComboBox();
                                setBorder(compBorder.getBorder());

                                cell.view.remove(cell.view.label);
                                cell.view.table.setBorder(new EmptyBorder(0,0,0,0));
                                
                                add(cell.view, BorderLayout.CENTER);
                            }

                            public boolean requestFocusInWindow() {
                                return cell.view.table.requestFocusInWindow();
                            }

                            public void propertyChanged(ClientPropertyView property) {
                                cell.setKey(property);
                                cell.cellValueChanged(table.getSelectedValue(property));
//                                cell.setValue(table.getSelectedValue(property));
                            }

                            public void stopEditing() {
                                CellEditor editor = cell.view.table.getCellEditor();
                                if (editor != null)
                                    editor.stopCellEditing();
                            }

                        }

                        private class ClientObjectValueLinkView extends ClientValueLinkView {

                            final ClientObjectValueLink valueLink;

                            final JComboBox objectView;

                            public ClientObjectValueLinkView(ClientObjectValueLink ivalueLink) {

                                valueLink = ivalueLink;

                                Vector<ClientObjectImplementView> objects = new Vector();
                                for (ClientGroupObjectImplementView groupObject : formView.groupObjects)
                                    for (ClientObjectImplementView object : groupObject)
                                        objects.add(object);

                                objectView = new QueryConditionComboBox(objects);

                                valueLink.object = (ClientObjectImplementView) objectView.getSelectedItem();

                                objectView.addItemListener(new ItemListener() {

                                    public void itemStateChanged(ItemEvent e) {
                                        valueLink.object = (ClientObjectImplementView)e.getItem();

                                        hasChanged = true;
                                        queryView.dataChanged();
                                    }
                                });

                                add(objectView);

                            }

                            public void propertyChanged(ClientPropertyView property) {
                            }
                        }

                        private class ClientPropertyValueLinkView extends ClientValueLinkView {

                            final ClientPropertyValueLink valueLink;

                            final JComboBox propertyView;

                            public ClientPropertyValueLinkView(ClientPropertyValueLink ivalueLink) {

                                valueLink = ivalueLink;

                                Vector<ClientPropertyView> properties = new Vector();
                                for (ClientPropertyView property : formView.properties)
                                    properties.add(property);

                                propertyView = new QueryConditionComboBox(properties);

                                valueLink.property = (ClientPropertyView) propertyView.getSelectedItem();

                                propertyView.addItemListener(new ItemListener() {

                                    public void itemStateChanged(ItemEvent e) {
                                        valueLink.property = (ClientPropertyView)e.getItem();

                                        hasChanged = true;
                                        queryView.dataChanged();
                                    }
                                });

                                add(propertyView);
                            }

                            public void propertyChanged(ClientPropertyView property) {
                            }
                        }

                    }

                    protected class QueryView extends JPanel {

                        final JPanel buttons;
                        final JPanel condviews;

                        boolean collapsed = false;

                        final Color defaultApplyBackground;

                        final JButton applyButton;
                        final Component centerGlue;
                        final JButton addCondition;
                        final JButton collapseButton;
                                                         
                        public QueryView() {

                            setAlignmentY(Component.TOP_ALIGNMENT);

                            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

                            buttons = new JPanel();
                            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

                            add(buttons);

                            applyButton = new JButton("");
                            applyButton.setFocusable(false);
                            applyButton.setPreferredSize(iconButtonDimension);
                            applyButton.setMaximumSize(iconButtonDimension);
                            applyButton.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent ae) {
                                    try {
                                        applyQuery();
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при применении фильтра", e);
                                    }
                                }
                            });
                            defaultApplyBackground = applyButton.getBackground();

                            centerGlue = Box.createHorizontalGlue();

                            addCondition = new JButton("");
                            addCondition.setFocusable(false);
                            addCondition.setPreferredSize(iconButtonDimension);
                            addCondition.setMaximumSize(iconButtonDimension);
                            addCondition.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent ae) {
                                    addCondition();
                                }
                            });
//                            buttons.add(addCondition);

                            collapseButton = new JButton();
                            collapseButton.setFocusable(false);
                            collapseButton.setPreferredSize(iconButtonDimension);
                            collapseButton.setMaximumSize(iconButtonDimension);
                            collapseButton.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    collapsed = !collapsed;
                                    conditionsChanged();
                                }
                            });
//                            buttons.add(collapseButton);

                            condviews = new JPanel();
                            condviews.setLayout(new BoxLayout(condviews, BoxLayout.Y_AXIS));

//                            add(condviews);

                            conditionsChanged();

                        }

                        public Dimension getMaximumSize() {
                            return getPreferredSize();
                        }

                        public void updateUI() {

                            if (condviews != null)
                                condviews.updateUI();

                            if (applyButton != null)
                                applyButton.updateUI();

                            if (addCondition != null)
                                addCondition.updateUI();

                            if (collapseButton != null)
                                collapseButton.updateUI();
                        }

                        public void conditionsChanged() {

                            if (!conditions.isEmpty() || hasChanged) {
                                buttons.add(applyButton);
                            } else {
                                buttons.remove(applyButton);
                            }

                            buttons.add(centerGlue);

                            buttons.add(addCondition);

                            if (!conditions.isEmpty()) {
                                buttons.add(collapseButton);
                            } else {
                                buttons.remove(collapseButton);
                            }

                            if (!collapsed) {
                                collapseButton.setIcon(collapseIcon);
                                add(condviews);
                            } else {
                                collapseButton.setIcon(expandIcon);
                                remove(condviews);
                            }

                            dataChanged();

                            container.validate();
                        }

                        public void dataChanged() {

                            if (hasChanged)
                                applyButton.setBackground(Color.green);
                            else
                                applyButton.setBackground(defaultApplyBackground);
                            
                        }
                    }

                }

                private class FindModel extends QueryModel {

                    public FindModel() {
                        super();

                        queryView.applyButton.setIcon(findIcon);
                        queryView.addCondition.setIcon(findAddIcon);
                    }

                    public void applyQuery() throws IOException {
                        changeFind(conditions);
                        super.applyQuery();
                    }

                }


                private class FilterModel extends QueryModel {

                    public FilterModel() {
                        super();
                                         
                        queryView.applyButton.setIcon(filtIcon);
                        queryView.addCondition.setIcon(filtAddIcon);

                    }

                    public void applyQuery() throws IOException {
                        changeFilter(groupObject, conditions);
                        super.applyQuery();
                    }

                }


                // ---------------------------------------------------------------------------------------------- //
                // -------------------------------------- Сортировка -------------------------------------------- //
                // ---------------------------------------------------------------------------------------------- //

                private class GridHeaderRenderer implements TableCellRenderer {

                    private final TableCellRenderer tableCellRenderer;

                    public GridHeaderRenderer(TableCellRenderer tableCellRenderer) {
                        this.tableCellRenderer = tableCellRenderer;
                    }

                    public Component getTableCellRendererComponent(JTable itable,
                                                                   Object value,
                                                                   boolean isSelected,
                                                                   boolean hasFocus,
                                                                   int row,
                                                                   int column) {

                        if (value instanceof String)
                            value = "<html>" + value + "</html>";

                        Component comp = tableCellRenderer.getTableCellRendererComponent(itable,
                                value, isSelected, hasFocus, row, column);
                        if (comp instanceof JLabel) {

                            JLabel label = (JLabel) comp;
                            label.setHorizontalAlignment(JLabel.CENTER);
                            label.setVerticalAlignment(JLabel.TOP);

                            ClientCellView property = table.getCellView(column);
                            if (property != null) {

                                int ordNum = orders.indexOf(property);
                                if (ordNum != -1) {

                                    label.setIcon((orderDirections.get(ordNum)) ? arrowUpIcon : arrowDownIcon);
                                }

                            }

                       }
                        return comp;
                    }
                }

                private class GridHeaderMouseListener extends MouseAdapter {
                    
                    public void mouseClicked(MouseEvent me) {

                        if (me.getClickCount() != 2) return;
                        if (!(me.getButton() == MouseEvent.BUTTON1 || me.getButton() == MouseEvent.BUTTON3)) return;

                        TableColumnModel columnModel = table.getColumnModel();
                        int viewColumn = columnModel.getColumnIndexAtX(me.getX());
                        int column = columnModel.getColumn(viewColumn).getModelIndex();

                        if (column != -1) {

                            ClientCellView property = table.getCellView(column);
                            if (property != null) {

                                try {
                                    int ordNum = orders.indexOf(property);
                                    if (ordNum == -1) {
                                        if (me.getButton() == MouseEvent.BUTTON1)
                                            changeGridOrder(property, Order.REPLACE);
                                         else
                                            changeGridOrder(property, Order.ADD);
                                    } else {
                                        if (me.getButton() == MouseEvent.BUTTON1) {
                                            changeGridOrder(property, Order.DIR);
                                        } else {
                                            changeGridOrder(property, Order.REMOVE);
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException("Ошибка при изменении сортировки", e); 
                                }

                                header.repaint();
                            }
                        }
                    }
                }


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

                        boolean externalID = false;
                        TableCellEditor cellEditor = getCellEditor();
                        if (cellEditor instanceof ClientAbstractCellEditor && ((ClientAbstractCellEditor)cellEditor).externalID)
                            externalID = true;
                        try {
                            changeProperty(gridColumns.get(col), value, externalID);
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
            }
            
        }

        public class ObjectModel {

            final ClientObjectImplementView object;

            JButton buttonAdd;
            JButton buttonChangeClass;
            JButton buttonDel;

            public ClassModel classModel;

            public ObjectModel(ClientObjectImplementView iobject) throws IOException {

                object = iobject;

                classModel = new ClassModel(object.classView);

                if (classModel.rootClass instanceof ClientObjectClass && object.objectIDView.show) {

                    String extraCaption = ((groupObject.size() > 1) ? ("(" + object.objectIDView.caption + ")") : "");

                    buttonAdd = new JButton("Добавить" + extraCaption);
                    buttonAdd.setFocusable(false);
                    buttonAdd.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            ClientObjectClass derivedClass = classModel.getDerivedClass();
                            if(derivedClass instanceof ClientConcreteClass) {
                                try {
                                    addObject(object, (ClientConcreteClass)derivedClass);
                                } catch (IOException e) {
                                    throw new RuntimeException("Ошибка при добавлении объекта", e);
                                }
                            }
                        }

                    });

                    formLayout.add(groupObject.addView, buttonAdd);

                    buttonDel = new JButton("Удалить" + extraCaption);
                    buttonDel.setFocusable(false);
                    buttonDel.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            try {
                                changeClass(object, null);
                            } catch (IOException e) {
                                throw new RuntimeException("Ошибка при удалении объекта", e);
                            }
                        }

                    });

                    formLayout.add(groupObject.delView, buttonDel);

                    if (classModel.rootClass.hasChilds()) {
                        buttonChangeClass = new JButton("Изменить класс" + extraCaption);
                        buttonChangeClass.setFocusable(false);
                        buttonChangeClass.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent ae) {
                                ClientObjectClass selectedClass = classModel.getSelectedClass();
                                if(selectedClass instanceof ClientConcreteClass) {
                                    try {
                                        changeClass(object, (ClientConcreteClass)selectedClass);
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при изменении класса объекта", e);
                                    }
                                }
                            }

                        });

                        formLayout.add(groupObject.changeClassView, buttonChangeClass);
                    }

                }

            }

            public void classViewChanged() {

                if (classView) {

                    classModel.addClassTree();
                    if (buttonChangeClass != null)
                        formLayout.add(groupObject.changeClassView, buttonChangeClass);
                } else {

                    classModel.removeClassTree();
                    if (buttonChangeClass != null)
                        formLayout.remove(groupObject.changeClassView, buttonChangeClass);
                }

            }

            public class ClassModel {

                final ClientClassView key;

                DefaultMutableTreeNode rootNode;
                ClientClass rootClass;

                DefaultMutableTreeNode currentNode;
                public ClientClass currentClass;

                JScrollPane pane;
                ClassTree view;

                public ClassModel(ClientClassView ikey) throws IOException {

                    key = ikey;

                    rootClass = ClientClass.deserialize(new DataInputStream(new ByteArrayInputStream(remoteForm.getBaseClassByteArray(object.ID))));
                    currentClass = rootClass;

                    rootNode = new DefaultMutableTreeNode(rootClass);
                    currentNode = rootNode;

                    view = new ClassTree();
                    pane = new JScrollPane(view);
                }

                public void addClassTree() {
                    if (rootClass.hasChilds())
                        formLayout.add(key, pane);
                }

                public void removeClassTree() {
                    formLayout.remove(key, pane);
                }

                private DefaultMutableTreeNode getSelectedNode() {

                    TreePath path = view.getSelectionModel().getLeadSelectionPath();
                    if (path == null) return null;

                    return (DefaultMutableTreeNode) path.getLastPathComponent();
                }

                public ClientObjectClass getDerivedClass() {

                    DefaultMutableTreeNode selNode = getSelectedNode();
                    if (selNode == null || !currentNode.isNodeChild(selNode)) return (ClientObjectClass) currentClass;

                    return (ClientObjectClass) selNode.getUserObject();
                }

                public ClientObjectClass getSelectedClass() {

                    DefaultMutableTreeNode selNode = getSelectedNode();
                    if (selNode == null) return (ClientObjectClass) currentClass;

                    return (ClientObjectClass) selNode.getUserObject();
                }

                public void changeCurrentClass(ClientObjectClass cls, DefaultMutableTreeNode clsNode) throws IOException {

                    if (cls == null) return;

                    changeGridClass(object, cls);
                    currentClass = cls;
                    currentNode = clsNode;

                    //запускаем событие изменение фонта, чтобы сбросить кэш в дереве, который расчитывает ширину поля
                    //собственно оно само вызывает перерисовку
                    view.firePropertyChange("font", false, true);

                }

                class ClassTree extends JTree {

                    final DefaultTreeModel model;

                    final int ID;

                    @Override
                    public int hashCode() {
                        return ID;
                    }

                    @Override
                    public boolean equals(Object o) {
                        return o instanceof ClassTree && ((ClassTree) o).ID == this.ID;
                    }

                    public ClassTree() {

                        ID = object.ID;

                        setToggleClickCount(-1);
                        setBorder(new EtchedBorder(EtchedBorder.LOWERED));

                        model = new DefaultTreeModel(rootNode);

                        setModel(model);

                        addTreeExpansionListener(new TreeExpansionListener() {

                            public void treeExpanded(TreeExpansionEvent event) {
                                try {
                                    addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                                } catch (IOException e) {
                                    throw new RuntimeException("Ошибка при получении потомков класса", e);
                                }
                            }

                            public void treeCollapsed(TreeExpansionEvent event) {}

                        });

                        addMouseListener(new MouseAdapter() {

                            public void mouseReleased(MouseEvent me) {
                                if (me.getClickCount() == 2) {
                                    try {
                                        changeCurrentClass(getSelectedClass(), getSelectedNode());
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при изменении текущего класса", e);
                                    }
                                }
                            }
                        });

                        addKeyListener(new KeyAdapter() {

                            public void keyPressed(KeyEvent ke) {
                                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                                    try {
                                        changeCurrentClass(getSelectedClass(), getSelectedNode());
                                    } catch (IOException e) {
                                        throw new RuntimeException("Ошибка при изменении текущего класса", e);
                                    }
                                    grid.table.requestFocusInWindow();
                                }
                            }
                        });

                        if (rootClass.hasChilds()) {
                            rootNode.add(new ExpandingTreeNode());
                            expandPath(new TreePath(rootNode));
                        }

                        this.setSelectionRow(0);
                        
                    }

                    @Override
                    public void updateUI() {
                        super.updateUI();

                        //приходится в updateUI это засовывать, иначе при изменении UI - нифига не перерисовывает
                        setCellRenderer(new DefaultTreeCellRenderer() {

                            Font defaultFont;

                            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                                          boolean expanded, boolean leaf, int row,
                                                                          boolean hasFocus) {
                                if (defaultFont == null) {
                                    Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                                    defaultFont = comp.getFont();
                                }

                                setFont(defaultFont);

                                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                                if (node != null) {

                                    if (node == currentNode)
                                        setFont(getFont().deriveFont(Font.BOLD));

                                }

                                return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                            }

                        });
                    }

                    private void addNodeElements(DefaultMutableTreeNode parent) throws IOException {

                        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

                        if (! (firstChild instanceof ExpandingTreeNode)) return;
                        parent.removeAllChildren();

                        Object nodeObject = parent.getUserObject();
                        if (nodeObject == null || ! (nodeObject instanceof ClientClass) ) return;

                        ClientObjectClass parentClass = (ClientObjectClass) nodeObject;

                        List<ClientClass> classes = DeSerializer.deserializeListClientClass(
                                                                        remoteForm.getChildClassesByteArray(object.ID,parentClass.ID));

                        for (ClientClass cls : classes) {

                            DefaultMutableTreeNode node;
                            node = new DefaultMutableTreeNode(cls, cls.hasChilds());
                            parent.add(node);

                            if (cls.hasChilds())
                                node.add(new ExpandingTreeNode());

                        }

                        model.reload(parent);
                    }

                    public DefaultMutableTreeNode getSelectedNode() {

                        TreePath path = getSelectionPath();
                        if (path == null) return null;

                        return (DefaultMutableTreeNode) path.getLastPathComponent();
                    }

                    public ClientObjectClass getSelectedClass() {

                        DefaultMutableTreeNode node = getSelectedNode();
                        if (node == null) return null;
                        
                        Object nodeObject = node.getUserObject();
                        if (! (nodeObject instanceof ClientClass)) return null;
                        return (ClientObjectClass) nodeObject;
                    }

                }

            }

        }
        
    }

    class FormLayout {

        ContainerView mainContainer;
        
        SimplexLayout globalLayout;
        
        final Map<ClientContainerView, ContainerView> contviews;
        
        public FormLayout(List<ClientContainerView> containers) {
        
            contviews = new HashMap();
            
//            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            
            while (true) {
                
                boolean found = false;
                for (ClientContainerView container : containers) {
                    if ((container.container == null || contviews.containsKey(container.container)) && !contviews.containsKey(container)) {
                        
                        ContainerView contview = new ContainerView(container);
                        if (container.container == null) {
                            
                            mainContainer = contview;
                            
                            globalLayout = new SimplexLayout(mainContainer, container.constraints);
                            mainContainer.setLayout(globalLayout);
                        }
                        else {
                            contviews.get(container.container).add(contview, container.constraints);
                        }
                        contviews.put(container, contview);
                        found = true;
                    }
                }
                
                if (!found) break;
                
            }

        }
        
        public JComponent getComponent() {
            return mainContainer;
        }

        private boolean add(ClientComponentView component, Component view) {
            if (!contviews.get(component.container).isAncestorOf(view)) {
                contviews.get(component.container).addComponent(view, component.constraints);
                contviews.get(component.container).repaint();
                return true;
            } else
                return false;
        }

        private boolean remove(ClientComponentView component, Component view) {
           if (contviews.get(component.container).isAncestorOf(view)) {
                contviews.get(component.container).removeComponent(view);
                contviews.get(component.container).repaint();
                return true;
           } else
                return false;
        }
        
        class ContainerView extends JPanel {
            
            final ClientContainerView view;
            
            public ContainerView(ClientContainerView iview) {
                
                view = iview;
                
                setLayout(globalLayout);

                if (view.title != null) {
                    TitledBorder border = BorderFactory.createTitledBorder(view.title);
                    setBorder(border);
                }


//                Random rnd = new Random();
//                this.setBackground(new Color(rnd.nextInt(255),rnd.nextInt(255),rnd.nextInt(255)));

                setPreferredSize(new Dimension(10000, 10000));

                setVisible(false);

            }

            public void addComponent(Component comp, Object constraints) {

                incrementComponentCount();
                add(comp, constraints);
            }

            public void removeComponent(Component comp) {

                remove(comp);
                decrementComponentCount();
            }

            int compCount = 0;
            private void incrementComponentCount() {

                if (compCount == 0)
                    setVisible(true);

                compCount++;

                Container parent = getParent();
                if (parent instanceof ContainerView)
                    ((ContainerView)parent).incrementComponentCount();
            }

            private void decrementComponentCount() {

                compCount--;
                if (compCount == 0)
                    setVisible(false);

                Container parent = getParent();
                if (parent instanceof ContainerView)
                    ((ContainerView)parent).decrementComponentCount();
            }

        }
    }
}