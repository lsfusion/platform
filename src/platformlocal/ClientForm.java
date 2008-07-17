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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

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
            views.get(groupObject).setCurrentObject(formChanges.Objects.get(groupObject));
        }
       
        // Затем их свойства
        
        for (ClientPropertyView property : formChanges.PanelProperties.keySet()) {
            views.get(property.groupObject).setPanelPropertyValue(property, formChanges.PanelProperties.get(property));
        }
       
        for (ClientPropertyView property : formChanges.GridProperties.keySet()) {
            views.get(property.groupObject).setGridPropertyValues(property, formChanges.GridProperties.get(property));
        }
       
        mainPanel.doLayout();
        mainPanel.validate();
       
    }
    
    void changeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        views.get(groupObject).setCurrentObject(objectValue);

        applyFormChanges(clientBean.changeObject(groupObject, objectValue));
        
    }
    
    void changeProperty(ClientPropertyView property, Object value) {
        applyFormChanges(clientBean.changeProperty(property, value));
    }
    
    class View extends JPanel {
        
        ClientGroupObjectImplement groupObject;
        
        Panel panel;
        Grid grid;
        
        ClientGroupObjectValue currentObject;
        ClientPropertyView currentProperty;
 
        public View(ClientGroupObjectImplement igroupObject) {
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.gridx = 0;
            
            groupObject = igroupObject;

            grid = new Grid();
            c.weighty = 0.7;
            add(grid, c);

            panel = new Panel();
            c.weighty = 0.3;
            add(panel, c);
            
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
        
        public void setCurrentObject(ClientGroupObjectValue value) {
            
            currentObject = value;
            
            grid.selectObject(currentObject);
        }
        
        public void setPanelPropertyValue(ClientPropertyView property, Object value) {
            
            panel.setPropertyValue(property, value);
        }

        public void setGridPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue,Object> values) {
            
            grid.setPropertyValues(property, values);
        }
        
        class PropertyCellEditor extends AbstractCellEditor
                                 implements TableCellEditor {

            Object value;
            
            JTextField comp;
            
            public PropertyCellEditor() {
                
                comp = new JTextField();
                
            }

            public Object getCellEditorValue() {
                
                if (value instanceof Integer) return Integer.parseInt(comp.getText());
                return comp.getText();
            }

            public Component getTableCellEditorComponent(JTable table, 
                                                         Object ivalue, 
                                                         boolean isSelected, 
                                                         int row, 
                                                         int column) {

                value = ivalue;

                comp.setText(value.toString());
                return comp;
            }

        }
        
        class Panel extends JPanel {
            
            Map<ClientPropertyView, PropertyGrid> properties;
            
            public Panel() {
                setLayout(new FlowLayout());
                
//                setPreferredSize(new Dimension(500, 200));
                
                properties = new HashMap();
                
//                this.setBackground(new Color(100,0,231));
                
/*                final Panel pan = this;
                
                JButton test = new JButton("Test");
                test.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        System.out.println(getLayout().preferredLayoutSize(pan).height);
                    }
                    
                });
                        
                add(test); */
                
            }
            
            public void addProperty(ClientPropertyView property) {
         
                if (properties.get(property) == null)
                {
                    PropertyGrid propview = new PropertyGrid(property);

                    add(propview);
                    properties.put(property, propview);

                }
                
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                PropertyGrid propview = properties.get(property);
                if (propview != null)
                {
                    remove(propview);
                    properties.remove(property);
                }
                
            }
            
            public void setPropertyValue(ClientPropertyView property, Object value) {
                
                PropertyGrid propview = properties.get(property);
                propview.setValue(value);
                
            }
            
            class PropertyGrid extends JPanel {

                ClientPropertyView property;
                
                JLabel label;
                PropertyTable table;
                
                Object value;
 
                public PropertyGrid(ClientPropertyView iproperty) {
                    
                    property = iproperty;
                    
                    setLayout(new FlowLayout());

                    label = new JLabel(property.caption);
                    add(label);
                    
                    table = new PropertyTable();
                    add(table);
                    
                }
                
                public void setValue(Object ivalue) {
                    value = ivalue;
                    
                    repaint();
                }
                
                class PropertyTable extends JTable {

                    PropertyModel model;

                    public PropertyTable() {

                        model = new PropertyModel();
                        setModel(model);
                        
                        setPreferredSize(property.getPreferredSize());
                        
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

                        SwingUtils.addFocusTraversalKey(this, 
                                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
                        
                        SwingUtils.addFocusTraversalKey(this, 
                                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
                       
                        setDefaultEditor(Object.class, new PropertyCellEditor());
                        
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
                            return value;
                        }

                    }
                    
                }

            }            
        }
        
        class Grid extends JPanel {

            JScrollPane pane;
            Table table;

            List<ClientPropertyView> gridProperties;
            List<ClientGroupObjectValue> gridObjects;
            Map<ClientPropertyView,Map<ClientGroupObjectValue,Object>> gridValues;
            
            public Grid() {

                setLayout(new GridBagLayout());
                
                gridProperties = new ArrayList();
                gridObjects = new ArrayList();
                gridValues = new HashMap();
                
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
            
            public void addProperty(ClientPropertyView property) {
                
                if (gridProperties.indexOf(property) == -1) {
                    Iterator<ClientPropertyView> icp = gridProperties.iterator();

                    // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
                    int ind = formInit.Properties.indexOf(property), ins = 0;

                    while (icp.hasNext() && formInit.Properties.indexOf(icp.next()) < ind) { ins++; }

                    gridProperties.add(ins, property);

                }

                table.createColumnsFromModel();
                 
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                gridProperties.remove(property);
                gridValues.remove(property);
                
                table.createColumnsFromModel();

            }
            
            public void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
               
                int oldindex = gridObjects.indexOf(currentObject);
                
                gridObjects = igridObjects;
                table.validate();
                
                final int newindex = gridObjects.indexOf(currentObject);
                
                //надо сдвинуть ViewPort - иначе дергаться будет
                
                if (newindex != -1 && oldindex != -1) {
                    
                    table.getSelectionModel().setLeadSelectionIndex(newindex);
                    
                    final Point ViewPos = pane.getViewport().getViewPosition();
                    final int dltpos = (newindex-oldindex) * table.getRowHeight();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ViewPos.y += dltpos;
                            pane.getViewport().setViewPosition(ViewPos);
                            table.scrollRectToVisible(table.getCellRect(newindex, 0, true));
                        }
                    });                
                    
                }

            }

            public void selectObject(ClientGroupObjectValue value) {
                
                table.getSelectionModel().setLeadSelectionIndex(gridObjects.indexOf(value));
                
            }

            public void setPropertyValues(ClientPropertyView property, Map<ClientGroupObjectValue,Object> values) {

                gridValues.put(property, values);

            }
            
            public class Table extends JTable {
                
                Model model;
                
                public Table() {

                    model = new Model();
                    setModel(model);
                    
                    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            changeObject(groupObject, model.getSelectedObject());
                        }
                    });
                    
                    setDefaultEditor(Object.class, new PropertyCellEditor());

                }
                
                public void createColumnsFromModel () {
                    
                    createDefaultColumnsFromModel();
                    
                    for (ClientPropertyView property : gridProperties) {
                        getColumnModel().getColumn(gridProperties.indexOf(property)).setPreferredWidth(property.getPreferredWidth());
                    }
                    
                }
                
                class Model extends AbstractTableModel {

                     public java.lang.Class getColumnClass(int c) {
                        return getValueAt(0, c).getClass();
                    }

                    public String getColumnName(int col) {
                        return gridProperties.get(col).caption;
                    }

                    public int getRowCount() {
                        return gridObjects.size();
                    }

                    public int getColumnCount() {
                        return gridProperties.size();
                    }

                    public boolean isCellEditable(int row, int col) {
                        return true;
                    }

                    public Object getValueAt(int row, int col) {

                        Object val = gridValues.get(gridProperties.get(col)).get(gridObjects.get(row));
                        if (val == null)
                            return (String)"";
                        else
                            return val;
                    }
                    
                    public void setValueAt(Object value, int row, int col) {

                        changeProperty(gridProperties.get(col),value);
//                        gridValues.get(gridProperties.get(col)).put(gridObjects.get(row),value);
                    }
                    
                    public ClientGroupObjectValue getSelectedObject() {
                        return gridObjects.get(convertRowIndexToModel(getSelectedRow()));
                    }

                }
            }
            
        }
        
    }
    
}
