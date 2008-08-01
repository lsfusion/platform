/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

interface ClientCellViewTable {
    
    ClientCellView getCellView(int row, int col);
    
}

class SingleViewable<ViewClass> {
    ViewClass view;
}

public class ClientForm extends FrameView {

    String caption = "Hello World";

    ClientForm thisForm;
    ClientFormInit formInit;
    
    ClientFormBean clientBean;
            
    public ClientForm(SingleFrameApplication app) {
        super(app);
        
        thisForm = this;
        
        getFrame().setTitle(caption);

        Test t = new Test();
        try {
            t.SimpleTest(this);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    FormLayout formLayout;

    Map<ClientGroupObjectImplement, GroupObjectModel> models;
    
    public void initializeForm() {

        formInit = clientBean.getClientFormInit();
        
        formLayout = new FormLayout(formInit.containers);
        
        setComponent(formLayout.getComponent());

        models = new HashMap();
        
        for (ClientGroupObjectImplement groupObject : formInit.groupObjects) {
            
            GroupObjectModel model = new GroupObjectModel(groupObject);
  
            models.put(groupObject, model);
            
        }
        
        JButton buttonSave = new JButton("Применить");
        buttonSave.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }
            
        });

        JButton buttonCancel = new JButton("Отменить");
        buttonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelChanges();
            }
            
        });
        
        SimplexConstraints c = new SimplexConstraints();
        c.order = 100001;
        formLayout.getComponent().add(buttonSave, c);
        
        c = new SimplexConstraints();
        c.order = 100002;
        formLayout.getComponent().add(buttonCancel, c);
    }
    

    void applyFormChanges(ClientFormChanges formChanges) {
        
        // Сначала меняем виды объектов
    
        for (ClientPropertyView property : formChanges.PanelProperties.keySet()) {
            models.get(property.groupObject).addPanelProperty(property);
        }

        for (ClientPropertyView property : formChanges.GridProperties.keySet()) {
            models.get(property.groupObject).addGridProperty(property);
        }

        for (ClientPropertyView property : formChanges.DropProperties) {
            models.get(property.groupObject).dropProperty(property);
        }
       
        
        // Затем подгружаем новые данные
       
        // Сначала новые объекты
        
        for (ClientGroupObjectImplement groupObject : formChanges.GridObjects.keySet()) {
            models.get(groupObject).grid.setGridObjects(formChanges.GridObjects.get(groupObject));
        }
       
        for (ClientGroupObjectImplement groupObject : formChanges.Objects.keySet()) {
            models.get(groupObject).setCurrentObject(formChanges.Objects.get(groupObject),false);
        }
       
        // Затем их свойства
        
        for (ClientPropertyView property : formChanges.PanelProperties.keySet()) {
            models.get(property.groupObject).setPanelPropertyValue(property, formChanges.PanelProperties.get(property));
        }
       
        for (ClientPropertyView property : formChanges.GridProperties.keySet()) {
            models.get(property.groupObject).setGridPropertyValues(property, formChanges.GridProperties.get(property));
        }
       
        formLayout.getComponent().validate();
       
    }
    
    void changeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        if (!objectValue.equals(models.get(groupObject).getCurrentObject())) {
            
            System.out.println("oldval : " + models.get(groupObject).getCurrentObject().toString());
            models.get(groupObject).setCurrentObject(objectValue, true);
            System.out.println("newval : " + objectValue.toString());

            applyFormChanges(clientBean.changeObject(groupObject, objectValue));
        }
        
    }
    
    void changeProperty(ClientCellView property, Object value) {
        if (property instanceof ClientPropertyView) // типа только если меняется свойство
            applyFormChanges(clientBean.changeProperty((ClientPropertyView)property, value));
    }
    
    void addObject(ClientObjectImplement object) {
        applyFormChanges(clientBean.addObject(object));
    }
    
    void changeClass(ClientObjectImplement object) {
        applyFormChanges(clientBean.changeClass(object));
    }
    
    void switchClassView(ClientGroupObjectImplement groupObject) {
        Boolean classView;
        classView = !models.get(groupObject).classView;
        applyFormChanges(clientBean.changeClassView(groupObject,classView));
        models.get(groupObject).setClassView(classView);
    }
    
    void saveChanges() {
        applyFormChanges(clientBean.saveChanges());
    }

    void cancelChanges() {
        applyFormChanges(clientBean.cancelChanges());
    }
    
    class GroupObjectModel {
        
        ClientGroupObjectImplement groupObject;
        
        PanelModel panel;
        GridModel grid;
        
        ClientGroupObjectValue currentObject;
        ClientPropertyView currentProperty;
        
        Boolean classView = false;
 
        public GroupObjectModel(ClientGroupObjectImplement igroupObject) {
            
/*            setLayout(new GridBagLayout());
            
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.gridx = 0;
            c.gridwidth = 2; */
            
            groupObject = igroupObject;

            grid = new GridModel(groupObject.gridView);
            
/*            c.weighty = 0.7;
            add(grid, c);*/

            panel = new PanelModel();
/*            c.weighty = 0.3;
            add(panel, c);*/

            setClassView(true);
            
/*            JButton test = new JButton("Test");
            test.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setClassView(!classView);
                }
                
            });
            
            add(test, c);*/
            
/*            for (final ClientObjectImplement object : groupObject) {
                
                c.gridwidth = 1;
                c.weighty = 0;
                c.gridy = 2;
                
                c.gridx = 0;
                JButton buttonAdd = new JButton("Добавить(" + object.caption + ")");
                buttonAdd.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        addObject(object);
                    }
                    
                });
                
                add(buttonAdd, c);
                
                c.gridx = 1;
                JButton buttonDel = new JButton("Удалить(" + object.caption + ")");
                buttonDel.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        changeClass(object);
                    }
                    
                });
                
                add(buttonDel, c);
            }*/
            
            
        }
        
        public void setClassView(Boolean iclassView) {
            
            if (classView != iclassView) {
                
                classView = iclassView;
                if (classView) {
                    panel.removeGroupObjectID();
                    grid.addGroupObjectID();
                    grid.table.requestFocusInWindow();
                } else {
                    panel.addGroupObjectID();
                    grid.removeGroupObjectID();
                    panel.getObjectIDView(0).requestFocusInWindow();
//                    panel.requestFocusInWindow();
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
        
        public void setCurrentObject(ClientGroupObjectValue value, Boolean userChange) {
    
            boolean realChange = !value.equals(currentObject);

            if (currentObject != null)
                System.out.println("view - oldval : " + currentObject.toString() + " ; userChange " + userChange.toString() );
            if (value != null)
                System.out.println("view - newval : " + value.toString() + " ; userChange " + userChange.toString());
            
            currentObject = value;
            
            if (realChange) {
                
                panel.selectObject(currentObject);
                if (!userChange) // если не сам изменил, а то пойдет по кругу
                    grid.selectObject(currentObject);
            }
            
        }
        
        public void setPanelPropertyValue(ClientPropertyView property, Object value) {
            
            panel.setPropertyValue(property, value);
        }

        public void setGridPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue,Object> values) {
            
            grid.setPropertyValues(property, values);
        }
        
        class ClientAbstractCellRenderer implements TableCellRenderer {

            public Component getTableCellRendererComponent(JTable table, 
                                                           Object value, 
                                                           boolean isSelected, 
                                                           boolean hasFocus, 
                                                           int row, 
                                                           int column) {
                
                ClientCellView property = ((ClientCellViewTable)table).getCellView(row, column);
                PropertyRendererComponent currentComp = property.getRendererComponent(thisForm);
                currentComp.setValue(value, isSelected, hasFocus);

                return currentComp.getComponent();
            }
            
        }
        
        class ClientAbstractCellEditor extends AbstractCellEditor 
                                 implements TableCellEditor {

            PropertyEditorComponent currentComp;
            Object value;
            
            public Object getCellEditorValue() {
                
                return currentComp.getValue();
                
            }

            public boolean isCellEditable(EventObject e) {
                return !(e instanceof MouseEvent)
                        || ((MouseEvent)e).getClickCount()>=2;
            }
            
            public Component getTableCellEditorComponent(JTable table, 
                                                         Object ivalue, 
                                                         boolean isSelected, 
                                                         int row, 
                                                         int column) {
                
                value = ivalue;
                
                ClientCellView property = ((ClientCellViewTable)table).getCellView(row, column);
                currentComp = property.getEditorComponent(thisForm);
                
                if (currentComp != null) {
                    currentComp.setValue(value);
                
                    return currentComp.getComponent();
                } else {                   
                    this.stopCellEditing();
                    return null;
                }
            }

        }
        
        class PanelModel {
            
            Map<ClientCellView, CellModel> models;
            
            public PanelModel() {
//                setLayout(new FlowLayout());

                models = new HashMap();
            }

            public void addGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    CellModel idmodel = new CellModel(object.objectIDView);
                    
/*                    int ord = formInit.order.indexOf(idview), ind = 0;
                    for (ClientCellView view : views.keySet())
                        if (formInit.order.indexOf(view) < ord) ind++;
                    
                    add(idview, ind); */
                    
                    models.put(object.objectIDView, idmodel);
                }
                setGroupObjectIDValue(currentObject);
                
//                validate();
                
            }
            
            public void removeGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    CellModel idmodel = models.get(object.objectIDView);
                    if (idmodel != null) {
                        idmodel.removeView();
                        models.remove(object.objectIDView);
                    }
                }
//                repaint();
                
            }

            private Component getObjectIDView(int ind) {
                return models.get(groupObject.get(ind).objectIDView).view;
            }

            private void setGroupObjectIDValue(ClientGroupObjectValue value) {

                for (ClientObjectImplement object : groupObject) {
                    
                    CellModel idmodel = models.get(object.objectIDView);
                    if (idmodel != null)
                        idmodel.setValue(value.get(object));
                }
                
            }

            private void selectObject(ClientGroupObjectValue value) {
                
                setGroupObjectIDValue(value);
            }
            
            public void addProperty(ClientPropertyView property) {
         
                if (models.get(property) == null) {
                    
                    CellModel propmodel = new CellModel(property);

/*                    int ord = formInit.order.indexOf(property), ind = 0;
                    for (ClientCellView view : views.keySet())
                        if (formInit.order.indexOf(view) < ord) ind++;
                    add(propview, ind);*/
                    models.put(property, propmodel);
                }
                
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                CellModel propmodel = models.get(property);
                if (propmodel != null)
                {
//                    remove(propview);
                    propmodel.removeView();
                    models.remove(property);
//                    repaint();
                }
                
            }
            
            public void setPropertyValue(ClientPropertyView property, Object value) {
                
                CellModel propmodel = models.get(property);
                propmodel.setValue(value);
                
            }
            
            class CellModel {
                
                ClientCellView key;
                Object value;
                
                CellView view;
                
                public CellModel(ClientCellView ikey) {
                    
                    key = ikey;
                    
                    view = new CellView();
                    
                    SimplexConstraints c = new SimplexConstraints();
                    c.order = formInit.order.indexOf(key);
                    
                    formLayout.add(key.container, view, c);
                    
                }

                public void setValue(Object ivalue) {
                    value = ivalue;

                    view.repaint();
                }

                private void removeView() {
                    formLayout.remove(key.container, view);
                }

                class CellView extends JPanel {

                    JLabel label;
                    CellTable table;

                    public CellView() {

//                        setLayout(new FlowLayout());
                        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

                        Random rnd = new Random();
//                        this.setBackground(new Color(rnd.nextInt(255),rnd.nextInt(255),rnd.nextInt(255)));
                        
                        label = new JLabel(key.caption);
                        add(label);

                        table = new CellTable();
                        add(table);

                    }

                    class CellTable extends SingleCellTable 
                                        implements ClientCellViewTable {

                        PropertyModel model;

                        public CellTable() {
                            super();

                            model = new PropertyModel();
                            setModel(model);

                            setPreferredSize(key.getPreferredSize());

                            setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
                            setDefaultEditor(Object.class, new ClientAbstractCellEditor());

                        }

                        class PropertyModel extends AbstractTableModel {

                            public int getRowCount() {
                                return 1;
                            }

                            public int getColumnCount() {
                                return 1;
                            }

    /*                        public java.lang.Class getColumnClass(int c) {
                                if (value != null)
                                    return value.getClass(); else
                                        return Object.class;
                            }*/

                            public boolean isCellEditable(int row, int col) {
                                return true;
                            }

                            public Object getValueAt(int row, int col) {
                                if (value != null)
                                    return value;
                                else
                                    return (String)"";
                            }

                            public void setValueAt(Object value, int row, int col) {
                                System.out.println("setValueAt");
                                changeProperty(key,value);
                            }

                        }

                        public ClientCellView getCellView(int row, int col) {
                            return key;
                        }

                    }

                }            
                
            }
            
        }
        
        class GridModel {

            ClientGridView view;
            
            JScrollPane pane;
            GridBagConstraints paneConstraints;
            Table table;

            public GridModel(ClientGridView iview) {

                view = iview;
                
//                setLayout(new GridBagLayout());

//                setFocusable(false);
                
                table = new Table();
                
                pane = new JScrollPane(table);
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                
                paneConstraints = new GridBagConstraints();
                paneConstraints.fill = GridBagConstraints.BOTH;
                paneConstraints.weightx = 1;
                paneConstraints.weighty = 1;
                paneConstraints.insets = new Insets(4,4,4,4); 
                
//                add(pane, paneConstraints);
            }

            private void addGroupObjectID() {
                System.out.println("addGroupObjectID");
                for (ClientObjectImplement object : groupObject) {
                    table.addColumn(object.objectIDView);
                }
                
                // здесь еще добавить значения идентификаторов
                fillTableObjectID();
                
                updateTable();
            }

            private void removeGroupObjectID() {
                System.out.println("removeGroupObjectID");
                for (ClientObjectImplement object : groupObject) {
                    table.removeColumn(object.objectIDView);
                }
                updateTable();
            }

            private void addProperty(ClientPropertyView property) {
                System.out.println("addProperty " + property.toString());
                table.addColumn(property);
                updateTable();
            }
            
            private void removeProperty(ClientPropertyView property) {
                System.out.println("removeProperty " + property.toString());
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
                for (ClientObjectImplement object : groupObject) {
                    Map<ClientGroupObjectValue, Object> values = new HashMap();
                    for (ClientGroupObjectValue value : table.gridRows)
                        values.put(value, value.get(object));
                    table.setColumnValues(object.objectIDView, values);
                }
            }
            
            private void updateTable() {

                System.out.println("CreateColumns");
                table.createDefaultColumnsFromModel();
                for (ClientCellView property : table.gridColumns) {
                    table.getColumnModel().getColumn(table.gridColumns.indexOf(property)).setPreferredWidth(property.getPreferredWidth());
                }

                if (table.gridColumns.size() != 0) {
                    
                    SimplexConstraints c = new SimplexConstraints();
                    c.order = formInit.groupObjects.indexOf(groupObject);
                    c.fillVertical = SimplexConstraints.MAXIMUM;
                    c.fillHorizontal = SimplexConstraints.MAXIMUM;

                    formLayout.add(view.container, pane, c);
//                    if (!isAncestorOf(pane))
//                        add(pane, paneConstraints);

                    formLayout.getComponent().validate();
//                    validate();

                } else {
                    formLayout.remove(view.container, pane);
//                    remove(pane);
//                    table = new Table();
//                    pane = new JScrollPane(table);
//                    validate();
                    formLayout.getComponent().validate();
                }
                
            }
            
            public class Table extends JTable
                               implements ClientCellViewTable {

                List<ClientCellView> gridColumns;
                List<ClientGroupObjectValue> gridRows;
                Map<ClientCellView,Map<ClientGroupObjectValue,Object>> gridValues;
                
                Model model;
                
                public Table() {

                    gridColumns = new ArrayList();
                    gridRows = new ArrayList();
                    gridValues = new HashMap();
                    
                    model = new Model();
                    setModel(model);
                    
                    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            System.out.println("changeSel");
                            changeObject(groupObject, model.getSelectedObject());
                        }
                    });
                    
                    setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
                    setDefaultEditor(Object.class, new ClientAbstractCellEditor());

                }

                public void addColumn(ClientCellView property) {

                    if (gridColumns.indexOf(property) == -1) {
                        Iterator<ClientCellView> icp = gridColumns.iterator();

                        // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
                        int ind = formInit.order.indexOf(property), ins = 0;

                        while (icp.hasNext() && formInit.properties.indexOf(icp.next()) < ind) { ins++; }

                        gridColumns.add(ins, property);

                    }


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
                    table.validate();

                    final int newindex = gridRows.indexOf(currentObject);

                    //надо сдвинуть ViewPort - иначе дергаться будет

                    System.out.println("setGridObjects" + oldindex + " - " + newindex);
                    if (newindex != -1) {

                        System.out.println("setgridobjects + leadselection");
                        getSelectionModel().setLeadSelectionIndex(newindex);

                        if (oldindex != -1 && newindex != oldindex) {
                        
                            final Point ViewPos = pane.getViewport().getViewPosition();
                            final int dltpos = (newindex-oldindex) * getRowHeight();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    ViewPos.y += dltpos;
                                    pane.getViewport().setViewPosition(ViewPos);
                                    scrollRectToVisible(getCellRect(newindex, 0, true));
                                    pane.validate();
                                }
                            });
                        } //else
//                            getSelectionModel().clearSelection();

                    }

                }

                public void selectObject(ClientGroupObjectValue value) {

                    int oldindex = getSelectionModel().getLeadSelectionIndex();
                    int newindex = gridRows.indexOf(value);
                    if (newindex != -1 && newindex != oldindex)
                        getSelectionModel().setLeadSelectionIndex(newindex);

                }

                public void setColumnValues(ClientCellView property, Map<ClientGroupObjectValue,Object> values) {

                    gridValues.put(property, values);
                    repaint();

                }
                
                class Model extends AbstractTableModel {

/*                    public java.lang.Class getColumnClass(int c) {
                        if (c < gridColumns.size())
                            return Object.class;
                        else
                            return Integer.class;
                    }*/

                    public String getColumnName(int col) {
//                        if (col < gridColumns.size())
                            return gridColumns.get(col).caption;
//                        else
//                            return groupObject.get(col-gridColumns.size()).caption;
                    }

                    public int getRowCount() {
                        return gridRows.size();
                    }

                    public int getColumnCount() {
                        return gridColumns.size(); // + (classView ? groupObject.size() : 0);
                    }

                    public boolean isCellEditable(int row, int col) {
                        return true;
                    }

                    public Object getValueAt(int row, int col) {

                        Object val = null;
//                        if (col < gridColumns.size())
                          val = gridValues.get(gridColumns.get(col)).get(gridRows.get(row));
//                        else
//                            val = gridRows.get(row).get(groupObject.get(col-gridColumns.size()));
                            
                        if (val == null)
                            return (String)"";
                        else
                            return val;
                    }
                    
                    public void setValueAt(Object value, int row, int col) {
                        System.out.println("setValueAt");
//                        if (col < gridColumns.size())
                            changeProperty(gridColumns.get(col),value);
                    }
                    
                    public ClientGroupObjectValue getSelectedObject() {
                        return gridRows.get(convertRowIndexToModel(getSelectedRow()));
                    }

                }

                public ClientCellView getCellView(int row, int col) {
                    return gridColumns.get(col);
                }
            }
            
        }
        
    }
    
    class ComponentView<ComponentState extends ClientComponentView, DrawComponent extends Component> {
        
        ComponentState state;
        DrawComponent component;
        
        public ComponentView(ComponentState istate) {
            state = istate;
        }
    }
    
    class FormLayout {

        ContainerView mainContainer;
        
        SimplexLayout globalLayout;
        
        Map<ClientContainerView, ContainerView> contviews;
        
        public FormLayout(List<ClientContainerView> containers) {
        
            contviews = new HashMap();
            
//            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            
            while (true) {
                
                boolean found = false;
                for (ClientContainerView container : containers) {
                    if ((container.container == null || contviews.containsKey(container.container)) && !contviews.containsKey(container)) {
                        
                        ContainerView contview = new ContainerView();
                        if (container.container == null) {
                            
                            mainContainer = contview;
                            
                            SimplexConstraints c = new SimplexConstraints();
                            c.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
                            
                            globalLayout = new SimplexLayout(mainContainer, c);
                            mainContainer.setLayout(globalLayout);
                        }
                        else {
                            SimplexConstraints c = new SimplexConstraints();
                            c.order = containers.indexOf(container);
                            if (contviews.get(container.container) == mainContainer)
                                c.childConstraints = SingleSimplexConstraint.TOTHE_BOTTOM;
                            else
                                c.childConstraints = SingleSimplexConstraint.TOTHE_RIGHTBOTTOM;
                            c.fillHorizontal = SimplexConstraints.MAXIMUM;
                            
                            contviews.get(container.container).add(contview, c);
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

        private void add(ClientContainerView container, Component view, SimplexConstraints constraint) {
/*            if (container == null)
                add(view);
            else*/
            contviews.get(container).add(view, constraint);
        }

        private void remove(ClientContainerView container, Component view) {
/*            if (container == null)
                remove(view);
            else*/
            contviews.get(container).remove(view);
        }
        
        class ContainerView extends JPanel {
            
            public ContainerView() {
                
                setLayout(globalLayout);
                
                Random rnd = new Random();
//                this.setBackground(new Color(rnd.nextInt(255),rnd.nextInt(255),rnd.nextInt(255)));
//                setLayout(new SimplexLayout());
//                setLayout(new FlowLayout());
            }
        }
    }
}


interface PropertyRendererComponent {

    Component getComponent();
    
    void setValue(Object value, boolean isSelected, boolean hasFocus);
    
}

class LabelPropertyRenderer extends JLabel {

    public LabelPropertyRenderer() {
        setBorder(new EmptyBorder(1, 1, 2, 2));
        setOpaque(true);
    }

    public void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255)); 
            else
                setBackground(new Color(192,192,255));
           
        } else
            setBackground(Color.white);
    }
    
}


class IntegerPropertyRenderer extends LabelPropertyRenderer
                              implements PropertyRendererComponent {

    public IntegerPropertyRenderer() {
        super();

        setHorizontalAlignment(JLabel.RIGHT);
        
    }
    
    public Component getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(value.toString());
        setSelected(isSelected, hasFocus);
    }
    
    
}
class StringPropertyRenderer extends LabelPropertyRenderer 
                             implements PropertyRendererComponent {

    public StringPropertyRenderer() {
        super();

//        setHorizontalAlignment(JLabel.LEFT);
        
    }
    
    public Component getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(value.toString());
        setSelected(isSelected, hasFocus);
    }
    
}

interface PropertyEditorComponent {

    Component getComponent();
    
    void setValue(Object value);
    Object getValue();
    
}


class TextFieldPropertyEditor extends JTextField {

    public TextFieldPropertyEditor() {
        setBorder(new EmptyBorder(0, 1, 0, 0));
        setOpaque(true);
        setBackground(new Color(128,128,255));
    }
    
}

class IntegerPropertyEditor extends TextFieldPropertyEditor 
                            implements PropertyEditorComponent {

    public IntegerPropertyEditor() {
        this.setHorizontalAlignment(JTextField.RIGHT);
    }
    
    public Component getComponent() {
        return this;
    }

    public void setValue(Object value) {
        if (value != null)
            setText(value.toString());
        selectAll();
    }

    public Object getValue() {
        try {
            return Integer.parseInt(this.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
}

class StringPropertyEditor extends TextFieldPropertyEditor 
                           implements PropertyEditorComponent {

    public Component getComponent() {
        return this;
    }

    public void setValue(Object value) {
        if (value != null)
            setText(value.toString());
        selectAll();
    }

    public Object getValue() {
        return (String)getText();
    }
    
}


class SingleCellTable extends JTable {
    
    public SingleCellTable() {
       
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                requestFocusInWindow();
                changeSelection(0, 0, false, false);
//                                getSelectionModel().setLeadSelectionIndex(0);
            }

            public void focusLost(FocusEvent e) {
                getSelectionModel().clearSelection();
            }

        });

        SwingUtils.addFocusTraversalKey(this, 
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));

/*                        SwingUtils.addFocusTraversalKey(this, 
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));*/

        SwingUtils.addFocusTraversalKey(this, 
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
                        
   }
    
}