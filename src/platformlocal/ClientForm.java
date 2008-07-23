/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
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

interface ClientAbstractViewTable {
    
    ClientAbstractView getAbstractView(int row, int col);
    
}

public class ClientForm extends FrameView {

    String caption = "Hello World";

    ClientFormInit formInit;
    
    ClientFormBean clientBean;
            
    public ClientForm(SingleFrameApplication app) {
        super(app);
        
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

    JPanel mainPanel;

    Map<ClientGroupObjectImplement, View> views;
    
    public void initializeForm() {

        formInit = clientBean.getClientFormInit();
        
        mainPanel = new JPanel();
        BoxLayout mainLayout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
        mainPanel.setLayout(mainLayout);
        
        setComponent(mainPanel);

        views = new HashMap();
        
        for (ClientGroupObjectImplement groupObject : formInit.GroupObjects) {
            
            View view = new View(groupObject);
  
            mainPanel.add(view);
            
            views.put(groupObject, view);
            
        }
        
    }
    

    void applyFormChanges(ClientFormChanges formChanges) {
        
        // Сначала меняем виды объектов
    
        for (ClientPropertyView property : formChanges.PanelProperties.keySet()) {
            views.get(property.groupObject).addPanelProperty(property);
        }

        for (ClientPropertyView property : formChanges.GridProperties.keySet()) {
            views.get(property.groupObject).addGridProperty(property);
        }

        for (ClientPropertyView property : formChanges.DropProperties) {
            views.get(property.groupObject).dropProperty(property);
        }
       
        
        // Затем подгружаем новые данные
       
        // Сначала новые объекты
        
        for (ClientGroupObjectImplement groupObject : formChanges.GridObjects.keySet()) {
            views.get(groupObject).grid.setGridObjects(formChanges.GridObjects.get(groupObject));
        }
       
        for (ClientGroupObjectImplement groupObject : formChanges.Objects.keySet()) {
            views.get(groupObject).setCurrentObject(formChanges.Objects.get(groupObject),false);
        }
       
        // Затем их свойства
        
        for (ClientPropertyView property : formChanges.PanelProperties.keySet()) {
            views.get(property.groupObject).setPanelPropertyValue(property, formChanges.PanelProperties.get(property));
        }
       
        for (ClientPropertyView property : formChanges.GridProperties.keySet()) {
            views.get(property.groupObject).setGridPropertyValues(property, formChanges.GridProperties.get(property));
        }
       
        mainPanel.validate();
       
    }
    
    void changeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        if (!objectValue.equals(views.get(groupObject).getCurrentObject())) {
            
            System.out.println("oldval : " + views.get(groupObject).getCurrentObject().toString());
            views.get(groupObject).setCurrentObject(objectValue, true);
            System.out.println("newval : " + objectValue.toString());

            applyFormChanges(clientBean.changeObject(groupObject, objectValue));
        }
        
    }
    
    void changeProperty(ClientAbstractView property, Object value) {
        if (property instanceof ClientPropertyView) // типа только если меняется свойство
            applyFormChanges(clientBean.changeProperty((ClientPropertyView)property, value));
    }
    
    void addObject(ClientObjectImplement object) {
        applyFormChanges(clientBean.addObject(object));
    }
    
    void changeClass(ClientObjectImplement object) {
        applyFormChanges(clientBean.changeClass(object));
    }
    
    class View extends JPanel {
        
        ClientGroupObjectImplement groupObject;
        
        Panel panel;
        Grid grid;
        
        ClientGroupObjectValue currentObject;
        ClientPropertyView currentProperty;
        
        Boolean classView = false;
 
        public View(ClientGroupObjectImplement igroupObject) {
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.gridx = 0;
            c.gridwidth = 2;
            
            groupObject = igroupObject;

            grid = new Grid();
            c.weighty = 0.7;
            add(grid, c);

            panel = new Panel();
            c.weighty = 0.3;
            add(panel, c);

            setClassView(true);
            
/*            JButton test = new JButton("Test");
            test.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setClassView(!classView);
                }
                
            });
            
            add(test, c);*/
            
            for (final ClientObjectImplement object : groupObject) {
                
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
            }
            
            
        }
        
        public void setClassView(Boolean iclassView) {
            
            if (classView != iclassView) {
                
                classView = iclassView;
                if (classView) {
                    panel.removeGroupObjectID();
                    grid.addGroupObjectID();
                } else {
                    panel.addGroupObjectID();
                    grid.removeGroupObjectID();
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
                
                ClientAbstractView property = ((ClientAbstractViewTable)table).getAbstractView(row, column);
                PropertyRendererComponent currentComp = property.getRendererComponent();
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
                
                ClientAbstractView property = ((ClientAbstractViewTable)table).getAbstractView(row, column);
                currentComp = property.getEditorComponent();
                currentComp.setValue(value);
                
                return currentComp.getComponent();
            }

        }
        
        class Panel extends JPanel {
            
            Map<ClientAbstractView, PanelAbstractGrid> views;
            
            public Panel() {
                setLayout(new FlowLayout());
                
                views = new HashMap();
            }

            public void addGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelAbstractGrid idview = new PanelAbstractGrid(object.objectIDView);
                    
                    add(idview);
                    views.put(object.objectIDView, idview);
                }
                
                validate();
                
            }
            
            public void removeGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelAbstractGrid idview = views.get(object.objectIDView);
                    if (idview != null) {
                        remove(idview);
                        views.remove(object.objectIDView);
                    }
                }
                repaint();
                
            }

            private void selectObject(ClientGroupObjectValue value) {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelAbstractGrid idview = views.get(object.objectIDView);
                    if (idview != null)
                        idview.setValue(value.get(object));
                }
            }
            
            public void addProperty(ClientPropertyView property) {
         
                if (views.get(property) == null)
                {
                    PanelAbstractGrid propview = new PanelAbstractGrid(property);

                    add(propview);
                    views.put(property, propview);

                }
                
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                PanelAbstractGrid propview = views.get(property);
                if (propview != null)
                {
                    remove(propview);
                    views.remove(property);
                    repaint();
                }
                
            }
            
            public void setPropertyValue(ClientPropertyView property, Object value) {
                
                PanelAbstractGrid propview = views.get(property);
                propview.setValue(value);
                
            }

            class PanelAbstractGrid extends JPanel {

                ClientAbstractView view;
                
                JLabel label;
                PropertyTable table;
                
                Object value;
 
                public PanelAbstractGrid(ClientAbstractView iview) {
                    
                    view = iview;
                    
                    setLayout(new FlowLayout());

                    label = new JLabel(view.caption);
                    add(label);
                    
                    table = new PropertyTable();
                    add(table);
                    
                }
                
                public void setValue(Object ivalue) {
                    value = ivalue;
                    
                    repaint();
                }
                
                class PropertyTable extends SingleCellTable 
                                    implements ClientAbstractViewTable {

                    PropertyModel model;

                    public PropertyTable() {
                        super();

                        model = new PropertyModel();
                        setModel(model);
                        
                        setPreferredSize(view.getPreferredSize());
                        
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
                            changeProperty(view,value);
                        }
                        
                    }

                    public ClientAbstractView getAbstractView(int row, int col) {
                        return view;
                    }
                    
                }

            }            
            
        }
        
        class Grid extends JPanel {

            JScrollPane pane;
            Table table;

            public Grid() {

                setLayout(new GridBagLayout());
                
                table = new Table();
                
                pane = new JScrollPane(table);
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                
                GridBagConstraints c = new GridBagConstraints();
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                c.insets = new Insets(4,4,4,4); 
                
                add(pane, c);
            }

            private void addGroupObjectID() {
                for (ClientObjectImplement object : groupObject) {
                    table.addColumn(object.objectIDView);
                }
                
                // здесь еще добавить значения идентификаторов
                fillTableObjectID();
            }

            private void removeGroupObjectID() {
                for (ClientObjectImplement object : groupObject) {
                    table.removeColumn(object.objectIDView);
                }
            }

            private void addProperty(ClientPropertyView property) {
                table.addColumn(property);
            }
            
            private void removeProperty(ClientPropertyView property) {
                table.removeColumn(property);
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
            
            public class Table extends JTable
                               implements ClientAbstractViewTable {

                List<ClientAbstractView> gridColumns;
                List<ClientGroupObjectValue> gridRows;
                Map<ClientAbstractView,Map<ClientGroupObjectValue,Object>> gridValues;
                
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

                public void addColumn(ClientAbstractView property) {

                    if (gridColumns.indexOf(property) == -1) {
                        Iterator<ClientAbstractView> icp = gridColumns.iterator();

                        // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
                        int ind = formInit.Properties.indexOf(property), ins = 0;

                        while (icp.hasNext() && formInit.Properties.indexOf(icp.next()) < ind) { ins++; }

                        gridColumns.add(ins, property);

                        table.createColumnsFromModel();
                    }


                }

                public void removeColumn(ClientAbstractView property) {

                    if (gridColumns.remove(property)) {

                        gridValues.remove(property);

                        table.createColumnsFromModel();
                    }

                }

                public void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
               
                    int oldindex = gridRows.indexOf(currentObject);

                    gridRows = igridObjects;
                    table.validate();

                    final int newindex = gridRows.indexOf(currentObject);

                    //надо сдвинуть ViewPort - иначе дергаться будет

                    if (newindex != -1 && oldindex != -1 && newindex != oldindex) {

                        System.out.println("setgridobjects + leadselection");
                        getSelectionModel().setLeadSelectionIndex(newindex);

                        final Point ViewPos = pane.getViewport().getViewPosition();
                        final int dltpos = (newindex-oldindex) * getRowHeight();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                ViewPos.y += dltpos;
                                pane.getViewport().setViewPosition(ViewPos);
                                scrollRectToVisible(getCellRect(newindex, 0, true));
                            }
                        });                

                    }

                }

                public void selectObject(ClientGroupObjectValue value) {

                    int oldindex = getSelectionModel().getLeadSelectionIndex();
                    int newindex = gridRows.indexOf(value);
                    if (newindex != -1 && newindex != oldindex)
                        getSelectionModel().setLeadSelectionIndex(newindex);

                }

                public void setColumnValues(ClientAbstractView property, Map<ClientGroupObjectValue,Object> values) {

                    gridValues.put(property, values);
                    repaint();

                }
                
                public void createColumnsFromModel () {
                    
                    System.out.println("CreateColumns");
                    createDefaultColumnsFromModel();
                    
                    for (ClientAbstractView property : gridColumns) {
                        getColumnModel().getColumn(gridColumns.indexOf(property)).setPreferredWidth(property.getPreferredWidth());
                    }
                    
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

                        Object val;
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

                public ClientAbstractView getAbstractView(int row, int col) {
                    return gridColumns.get(col);
                }
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