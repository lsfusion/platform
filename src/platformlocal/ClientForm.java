/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

public class ClientForm extends FrameView {

    String caption = "Hello World";

    ClientFormInit formInit;
    
    FormBeanView formBean;
            
    public ClientForm(SingleFrameApplication app) {
        super(app);
        
        getFrame().setTitle(caption);

        Test t = new Test();
        try {
//            t.SimpleTest(this);
        } catch(Exception e) {
            
        }
        
    }

    JPanel mainPanel;
    GridBagLayout mainLayout;

    Map<ClientGroupObjectImplement, View> views;
    
    public void InitializeForm(ClientFormInit formInit) {

        mainPanel = new JPanel();
        mainLayout = new GridBagLayout();
        mainPanel.setLayout(mainLayout);

        setComponent(mainPanel);

        views = new HashMap();
        
        Iterator<ClientGroupObjectImplement> ig = formInit.GroupObjects.iterator();

        while (ig.hasNext()) {
            
            ClientGroupObjectImplement groupObject = ig.next();
            
            View view = new View(groupObject);
            
            mainPanel.add(view, groupObject.GridConstraint);
            
            views.put(groupObject, view);
            
        }
        
    }
    

    void ApplyFormChanges(ClientFormChanges formChanges) {
        
        // Сначала меняем виды объектов
    
        Iterator<ClientPropertyView> ip = formChanges.PanelProperties.keySet().iterator();
        while (ip.hasNext())
        {
            ClientPropertyView property = ip.next();
            views.get(property.GroupObject).addPanelProperty(property);
        }

       ip = formChanges.GridProperties.keySet().iterator();
       while (ip.hasNext())
       {
            ClientPropertyView property = ip.next();
            views.get(property.GroupObject).addGridProperty(property);
       }

       ip = formChanges.DropProperties.iterator();
       while (ip.hasNext())
       {
           ClientPropertyView property = ip.next();
           views.get(property.GroupObject).dropProperty(property);
       }
       
        
       // Затем подгружаем новые данные
       
       // Сначала новые объекты

       Iterator<ClientGroupObjectImplement> ig = formChanges.GridObjects.keySet().iterator();
       while (ig.hasNext())
       {
           ClientGroupObjectImplement groupObject = ig.next();
           views.get(groupObject).grid.setGridObjects(formChanges.GridObjects.get(groupObject));

       }
       
       ig = formChanges.Objects.keySet().iterator();
       while (ig.hasNext()) {

           ClientGroupObjectImplement groupObject = ig.next();
           views.get(groupObject).setCurrentObject(formChanges.Objects.get(groupObject));
           
       }
       
       // Затем их свойства
       
       ip = formChanges.PanelProperties.keySet().iterator();
       while (ip.hasNext())
       {
           ClientPropertyView property = ip.next();
           views.get(property.GroupObject).setPanelPropertyValue(property, formChanges.PanelProperties.get(property));
       }
       
       ip = formChanges.GridProperties.keySet().iterator();
       while (ip.hasNext())
       {
           ClientPropertyView property = ip.next();
           views.get(property.GroupObject).setGridPropertyValues(property, formChanges.GridProperties.get(property));
           
       }
       
       mainPanel.doLayout();
       mainPanel.validate();
       
    }
    
    void ChangeObject(ClientGroupObjectImplement groupObject, ClientGroupObjectValue objectValue) {
        
        MapUtils<GroupObjectImplement, ClientGroupObjectImplement> mgu = new MapUtils();
        GroupObjectImplement GroupObject = mgu.getKey(grs, groupObject);

        MapUtils<GroupObjectValue, ClientGroupObjectValue> mvu = new MapUtils();
        GroupObjectValue GroupVal = mvu.getKey(gos, objectValue);
        
        try {
            formBean.ChangeObject(GroupObject, GroupVal);
        } catch(Exception e) {
            
        }
        
        FormChanges fc = null;
        try {
            fc = formBean.EndApply();
        } catch(SQLException e) {
            
        }
        
        fc.Out(formBean);
        
        if (fc != null)
        {
            ClientFormChanges cfc = ConvertFormChangesToClient(fc);
            ApplyFormChanges(cfc);
        }
        
    }
    
    class View extends JPanel {
        
        ClientGroupObjectImplement groupObject;
        
        Panel panel;
        Grid grid;
        
        ClientGroupObjectValue currentObject;
        ClientPropertyView currentProperty;
 
        public View(ClientGroupObjectImplement igroupObject) {
            
            groupObject = igroupObject;

            panel = new Panel();
            add(panel, groupObject.PanelConstraint);
            
            grid = new Grid();
            add(grid, groupObject.GridConstraint);
            
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
        
        
        class Panel extends JPanel {
            
            Map<ClientPropertyView, PropertyGrid> properties;
            
            public void Panel() {
                setLayout(new FlowLayout());
                
                properties = new HashMap();
            }
            
            public void addProperty(ClientPropertyView property) {
         
                if (properties.get(property) == null)
                {
                    PropertyGrid propview = new PropertyGrid();
                    propview.setCaption(property.Caption);

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
                
                properties.get(property).setValue(value);
                
            }
            
            class PropertyGrid extends JTable {

                PropertyModel model;

                String caption = "";
                Object value = null;
                
                public void setCaption(String icaption) {
                    caption = icaption;
                }

                public void setValue(Object ivalue) {
                    value = ivalue;
                }

                public PropertyGrid() {

                    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                    model = new PropertyModel();
                    setModel(model);
                }
                
                class PropertyModel extends AbstractTableModel {

                    public int getRowCount() {
                        return 1;
                    }

                    public int getColumnCount() {
                        return 2;
                    }

                    public Object getValueAt(int row, int col) {
                        if (col == 0)
                            return caption;
                        else
                            return value;

                    }

                }

            }            
        }
        
        class Grid extends JScrollPane {

            Table table;

            List<ClientPropertyView> gridProperties;
            List<ClientGroupObjectValue> gridObjects;
            Map<ClientPropertyView,Map<ClientGroupObjectValue,Object>> gridValues;
            
            public void Grid() {

                gridProperties = new ArrayList();
                gridObjects = new ArrayList();
                gridValues = new HashMap();
                
                setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                
                table = new Table();
                add(table);
                
            }
            
            public void addProperty(ClientPropertyView property) {
                
                
                if (gridProperties.indexOf(property) == -1) {
                    Iterator<ClientPropertyView> icp = gridProperties.iterator();

                    // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
                    int ind = formInit.Properties.indexOf(property), ins = 0;

                    while (icp.hasNext() && formInit.Properties.indexOf(icp.next()) < ind) { ins++; }

                    gridProperties.add(ins, property);

                }

                table.createDefaultColumnsFromModel();
                 
            }
            
            public void removeProperty(ClientPropertyView property) {
                
                gridProperties.remove(property);
                gridValues.remove(property);
                table.createDefaultColumnsFromModel();

            }
            
            public void setGridObjects(List<ClientGroupObjectValue> igridObjects) {
               
                int oldindex = gridObjects.indexOf(currentObject);
                
                gridObjects = igridObjects;
                table.validate();
                
                final int newindex = gridObjects.indexOf(currentObject);
                
                //надо сдвинуть ViewPort - иначе дергаться будет
                
                if (newindex != -1 && oldindex != -1) {
                    
                    table.getSelectionModel().setLeadSelectionIndex(newindex);
                    
                    final Point ViewPos = getViewport().getViewPosition();
                    final int dltpos = (newindex-oldindex) * table.getRowHeight();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ViewPos.y += dltpos;
                            getViewport().setViewPosition(ViewPos);
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
                
                public void Table() {

                    model = new Model();
                    table.setModel(model);
                    
                    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            ChangeObject(groupObject, model.getSelectedObject());
        //                    System.out.println("valuechanged");
                        }
                    });

                }
                
                class Model extends AbstractTableModel {

                     public java.lang.Class getColumnClass(int c) {
                        return getValueAt(0, c).getClass();
                    }

                    public String getColumnName(int col) {
                        return gridProperties.get(col).Caption;
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
                    
                    public ClientGroupObjectValue getSelectedObject() {
                        return gridObjects.get(convertRowIndexToModel(getSelectedRow()));
                    }

                }
            }
            
        }
        
    }
    

    // Здесь тестинг, заглушка на простую перекачку FormChanges в ClientFormChanges
    Map<GroupObjectImplement, ClientGroupObjectImplement> grs;
    Map<ObjectImplement, ClientObjectImplement> ors;
    Map<PropertyView, ClientPropertyView> prs;
    Map<GroupObjectValue, ClientGroupObjectValue> gos;
    
    ClientFormChanges ConvertFormChangesToClient (FormChanges fc)
    {
        ClientFormChanges cfc = new ClientFormChanges();
        
        Iterator<GroupObjectImplement> igo;
        Iterator<GroupObjectValue> igv;
        Iterator<ObjectImplement> ioi;
        Iterator<PropertyView> ipv;
                
        igo = fc.Objects.keySet().iterator();
        while (igo.hasNext())
        {
            GroupObjectImplement Group = igo.next();
            ClientGroupObjectImplement ClientGroup = grs.get(Group);
            
            GroupObjectValue ObjectValue = fc.Objects.get(Group);
            ClientGroupObjectValue ClientObjectValue = new ClientGroupObjectValue();
            
            ioi = ObjectValue.keySet().iterator();
            while (ioi.hasNext())
            {
                ObjectImplement Object = ioi.next();
                ClientObjectImplement ClientObject = ors.get(Object);
                
                ClientObjectValue.put(ClientObject, ObjectValue.get(Object));
            }
            
            gos.put(ObjectValue, ClientObjectValue);
            cfc.Objects.put(ClientGroup, ClientObjectValue);
        }

        igo = fc.GridObjects.keySet().iterator();
        while (igo.hasNext())
        {
            GroupObjectImplement Group = igo.next();
            ClientGroupObjectImplement ClientGroup = grs.get(Group);
            
            List<GroupObjectValue> ListObjectValue = fc.GridObjects.get(Group);
            List<ClientGroupObjectValue> ListClientObjectValue = new ArrayList();
            
            igv = ListObjectValue.iterator();
            while (igv.hasNext())
            {
                GroupObjectValue ObjectValue = igv.next();
                ClientGroupObjectValue ClientObjectValue = new ClientGroupObjectValue();
            
                ioi = ObjectValue.keySet().iterator();
                while (ioi.hasNext())
                {
                    ObjectImplement Object = ioi.next();
                    ClientObjectImplement ClientObject = ors.get(Object);

                    ClientObjectValue.put(ClientObject, ObjectValue.get(Object));
                }
                
                gos.put(ObjectValue, ClientObjectValue);
                ListClientObjectValue.add(ClientObjectValue);
            }
            
            cfc.GridObjects.put(ClientGroup, ListClientObjectValue);
        }
        
        ipv = fc.PanelProperties.keySet().iterator();
        while (ipv.hasNext())
        {
            PropertyView Property = ipv.next();
            ClientPropertyView ClientProp = prs.get(Property);
            
            cfc.PanelProperties.put(ClientProp, fc.PanelProperties.get(Property));
        }

        ipv = fc.GridProperties.keySet().iterator();
        while (ipv.hasNext())
        {
            PropertyView Property = ipv.next();
            ClientPropertyView ClientProp = prs.get(Property);
            
            Map<GroupObjectValue, Object> MapObjectValue = fc.GridProperties.get(Property);
            Map<ClientGroupObjectValue, Object> ClientMapObjectValue = new HashMap();
            
            igv = MapObjectValue.keySet().iterator();
            while (igv.hasNext())
            {
                GroupObjectValue ObjectValue = igv.next();
                ClientGroupObjectValue ClientObjectValue = gos.get(ObjectValue);
                ClientMapObjectValue.put(ClientObjectValue, MapObjectValue.get(ObjectValue));
            }
            cfc.GridProperties.put(ClientProp, ClientMapObjectValue);
        }
        
        ipv = fc.DropProperties.iterator();
        while (ipv.hasNext())
        {
            PropertyView Property = ipv.next();
            ClientPropertyView ClientProp = prs.get(Property);
            
            cfc.DropProperties.add(ClientProp);
        }
        
        return cfc;
    
    }
    
}
