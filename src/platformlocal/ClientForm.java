/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JRViewer;

import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.DateFormat;
import javax.swing.JTextField;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.table.*;

import bibliothek.gui.dock.DefaultDockable;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

interface ClientCellViewTable {

    ClientCellView getCellView(int row, int col);
}

class SingleViewable<ViewClass> {
    ViewClass view;
}

public class ClientForm extends Container {

    String caption = "Hello World";

    ClientForm thisForm;

    ClientFormView formView;

    RemoteForm remoteForm;

    // Icons - загружаем один раз, для экономии
    final ImageIcon arrowUpIcon = new ImageIcon(getClass().getResource("images/arrowup.gif"));
    final ImageIcon arrowDownIcon = new ImageIcon(getClass().getResource("images/arrowdown.gif"));
    final ImageIcon filtIcon = new ImageIcon(getClass().getResource("images/filt.gif"));
    final ImageIcon filtAddIcon = new ImageIcon(getClass().getResource("images/filtadd.gif"));
    final ImageIcon findIcon = new ImageIcon(getClass().getResource("images/find.gif"));
    final ImageIcon findAddIcon = new ImageIcon(getClass().getResource("images/findadd.gif"));
    final ImageIcon deleteIcon = new ImageIcon(getClass().getResource("images/delete.gif"));
    final ImageIcon collapseIcon = new ImageIcon(getClass().getResource("images/collapse.gif"));
    final ImageIcon expandIcon = new ImageIcon(getClass().getResource("images/expand.gif"));

    public final static Dimension iconButtonDimension = new Dimension(22,22);


    public ClientForm(RemoteForm iremoteForm) {
//        super(app);

//        FocusOwnerTracer.installFocusTracer();

        remoteForm = iremoteForm;

        thisForm = this;

//        getFrame().setTitle(caption);

        formView = remoteForm.GetRichDesign();
        initializeForm();

        applyFormChanges();
    }

    FormLayout formLayout;

    Map<ClientGroupObjectImplement, GroupObjectModel> models;

    public void initializeForm() {

        formLayout = new FormLayout(formView.containers);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(formLayout.getComponent());
//        setContentPane(formLayout.getComponent());
//        setComponent(formLayout.getComponent());

        models = new HashMap();

        for (ClientGroupObjectImplement groupObject : formView.groupObjects) {

            GroupObjectModel model = new GroupObjectModel(groupObject);

            models.put(groupObject, model);

        }

        JButton buttonPrint = new JButton("Печать");
        buttonPrint.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                print();
            }

        });

        formLayout.add(formView.printView, buttonPrint);

        JButton buttonApply = new JButton("Применить");
        buttonApply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }

        });

        formLayout.add(formView.applyView, buttonApply);

        JButton buttonCancel = new JButton("Отменить");
        buttonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelChanges();
            }

        });

        formLayout.add(formView.cancelView, buttonCancel);

/*        JButton test = new JButton("Test");

        test.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                formLayout.globalLayout.disableLayout = true;
            }
        });

        add(test);*/
    }

    void applyFormChanges() {

        try {
            applyFormChanges(ByteArraySerializer.deserializeClientFormChanges(remoteForm.EndApply().serialize(), formView));
        } catch (SQLException e) {
            e.printStackTrace();
        }

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

        long st = System.currentTimeMillis();

        if (!objectValue.equals(models.get(groupObject).getCurrentObject())) {

//            System.out.println("oldval : " + models.get(groupObject).getCurrentObject().toString());
            models.get(groupObject).setCurrentObject(objectValue, true);
            System.out.println("Change Object - setCurrentObject : " + (System.currentTimeMillis()-st));
//            System.out.println("newval : " + objectValue.toString());

            try {
                remoteForm.ChangeObject(groupObject.GID, ByteArraySerializer.serializeClientGroupObjectValue(objectValue));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            applyFormChanges();
        }
        System.out.println("Whole Change Object : " + (System.currentTimeMillis()-st));

    }

    void changeProperty(ClientCellView property, Object value) {
        if (property instanceof ClientPropertyView) {
            // типа только если меняется свойство
            try {
                remoteForm.ChangePropertyView(property.ID, ByteArraySerializer.serializeObjectValue(value));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            applyFormChanges();
        }
    }

    void addObject(ClientObjectImplement object, ClientClass cls) {
        try {
            remoteForm.AddObject(object.ID, cls.ID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        applyFormChanges();
    }

    void changeClass(ClientObjectImplement object, ClientClass cls) {
        try {
            remoteForm.ChangeClass(object.ID, (cls == null) ? -1 : cls.ID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        applyFormChanges();
    }

    void changeGridClass(ClientObjectImplement object, ClientClass cls) {

        try {
            remoteForm.ChangeGridClass(object.ID, cls.ID);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        applyFormChanges();
    }

    void switchClassView(ClientGroupObjectImplement groupObject) {

        Boolean classView;
        classView = !models.get(groupObject).classView;

        try {
            remoteForm.ChangeClassView(groupObject.GID, classView);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        models.get(groupObject).setClassView(classView);

        applyFormChanges();
    }

    void changeOrder(ClientPropertyView property, int modiType) {

        remoteForm.ChangeOrder(property.ID, modiType);
        
        applyFormChanges();
    }

    private void changeFind(List<ClientFilter> conditions) {
    }


    private void changeFilter(List<ClientFilter> conditions) {

        remoteForm.clearFilter();
        for (ClientFilter filter : conditions) {
            remoteForm.addFilter(ByteArraySerializer.serializeClientFilter(filter));
        }

        applyFormChanges();
    }

    void print() {


        JasperDesign Design = remoteForm.GetReportDesign();
        JasperReport Report = null;
        try {
            Report = JasperCompileManager.compileReport(Design);
            JasperPrint Print = JasperFillManager.fillReport(Report,new HashMap(),remoteForm.ReadData());

            JRViewer Viewer = new JRViewer(Print);
            Main.Layout.DefaultStation.drop(new DefaultDockable(Viewer,"Report"));
/*            JFrame DrawPrint = new JFrame("Report");
            DrawPrint.getContentPane().add(Viewer);
            DrawPrint.setSize(800,600);
            DrawPrint.setVisible(true);
            DrawPrint.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);*/

/*
            JRXlsExporter ToExcel = new JRXlsExporter();
            ToExcel.setParameter(JRExporterParameter.JASPER_PRINT, Print);
            ToExcel.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, "report.xls");
            ToExcel.exportReport();
*/
        } catch (JRException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
//        JasperCompileManager.writeReportToXmlFile(Report,"report.xml");
//        JasperExportManager.exportReportToPdfFile(Print, "report.pdf");
    }

    void saveChanges() {
        try {
            remoteForm.SaveChanges();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        applyFormChanges();
    }

    void cancelChanges() {
        remoteForm.CancelChanges();
        applyFormChanges();
    }

    class GroupObjectModel {

        ClientGroupObjectImplement groupObject;

        PanelModel panel;
        GridModel grid;
        Map<ClientObjectImplement, ObjectModel> objects = new HashMap();

        ClientGroupObjectValue currentObject;

        ClientCellView currentCell;
        Object currentValue;

        Boolean classView = false;

        public GroupObjectModel(ClientGroupObjectImplement igroupObject) {

            groupObject = igroupObject;

            grid = new GridModel(groupObject.gridView);

            panel = new PanelModel();

            for (ClientObjectImplement object : groupObject) {

                objects.put(object, new ObjectModel(object));

            }
            
            setClassView(true);

        }
        
        public void setClassView(Boolean iclassView) {
            
            if (classView != iclassView) {
                
                classView = iclassView;
                if (classView) {
                    panel.removeGroupObjectID();
                    grid.addGroupObjectID();
                    for (ClientObjectImplement object : groupObject)
                        objects.get(object).classModel.addClassTree();
                    grid.table.requestFocusInWindow();
                } else {
                    panel.addGroupObjectID();
                    grid.removeGroupObjectID();
                    for (ClientObjectImplement object : groupObject)
                        objects.get(object).classModel.removeClassTree();
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

        class CellModel {

            ClientCellView key;
            Object value;

            CellView view;

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

            protected void cellValueChanged(Object ivalue) {
                value = ivalue;
            }

            class CellView extends JPanel {

                JLabel label;
                CellTable table;

                int ID;

                @Override
                public int hashCode() {
                    return ID;
                }

                @Override
                public boolean equals(Object o) {
                    if (!(o instanceof CellView))
                        return false;
                    return ((CellView)o).ID == this.ID;
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

                    ID = key.ID;

                    label.setText(key.caption);

                    table.keyChanged();
                }

                class CellTable extends SingleCellTable
                                    implements ClientCellViewTable {

                    PropertyModel model;

                    public CellTable() {
                        super();

                        setSurrendersFocusOnKeystroke(true);
                        
                        model = new PropertyModel();
                        setModel(model);

                        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
                        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

                    }

                    public void keyChanged() {

                        setPreferredSize(key.getPreferredSize());
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
                            cellValueChanged(value);
                        }

                    }

                    public ClientCellView getCellView(int row, int col) {
                        return key;
                    }

                }

            }

        }

        class PanelModel {
            
            Map<ClientCellView, PanelCellModel> models;
            
            public PanelModel() {
//                setLayout(new FlowLayout());

                models = new HashMap();
            }

            public void addGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelCellModel idmodel = new PanelCellModel(object.objectIDView);
                    formLayout.add(idmodel.key, idmodel.view);

                    models.put(object.objectIDView, idmodel);

                }
                setGroupObjectIDValue(currentObject);
                
//                validate();
                
            }
            
            public void removeGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelCellModel idmodel = models.get(object.objectIDView);
                    if (idmodel != null) {
                        formLayout.remove(idmodel.key, idmodel.view);
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
                }

                protected void cellValueChanged(Object ivalue) {
                    changeProperty(key,ivalue);
                }

            }
            
        }
        
        class GridModel {

            ClientGridView view;

            JPanel container;

            JPanel queriesContainer;

            JScrollPane pane;
            GridBagConstraints paneConstraints;
            Table table;

            public GridModel(ClientGridView iview) {

                view = iview;

                container = new JPanel();
                container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

                table = new Table();
                
                pane = new JScrollPane(table);
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

                table.setFillsViewportHeight(true);

                paneConstraints = new GridBagConstraints();
                paneConstraints.fill = GridBagConstraints.BOTH;
                paneConstraints.weightx = 1;
                paneConstraints.weighty = 1;
                paneConstraints.insets = new Insets(4,4,4,4); 

                queriesContainer = new JPanel();
//                queriesContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
//                queriesContainer.setLayout(new FlowLayout());
                queriesContainer.setLayout(new BoxLayout(queriesContainer, BoxLayout.X_AXIS));

                queriesContainer.add(table.findModel.queryView);
                queriesContainer.add(Box.createRigidArea(new Dimension(4,0)));
                queriesContainer.add(table.filterModel.queryView);
                queriesContainer.add(Box.createHorizontalGlue());

                container.add(pane);
                container.add(queriesContainer);
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
                    
                    formLayout.add(view, container);

                } else {
                    formLayout.remove(view, container);
                }
                
            }

            public class Table extends JTable
                               implements ClientCellViewTable {

                List<ClientCellView> gridColumns = new ArrayList();
                List<ClientGroupObjectValue> gridRows = new ArrayList();
                Map<ClientCellView,Map<ClientGroupObjectValue,Object>> gridValues = new HashMap();

                List<ClientPropertyView> orders = new ArrayList();
                List<Boolean> orderDirections = new ArrayList();
                
                Model model;
                JTableHeader header;

                FindModel findModel;
                FilterModel filterModel;

                int ID;

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

                public Table() {

                    ID = groupObject.GID;

                    setSurrendersFocusOnKeystroke(true);
                    
                    model = new Model();
                    setModel(model);

                    header = getTableHeader();

                    findModel = new FindModel();
                    filterModel = new FilterModel();
                    
                    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            System.out.println("changeSel");
                            changeObject(groupObject, model.getSelectedObject());
                            currentCell = model.getSelectedCell();
                        }
                    });

                    header.setDefaultRenderer(new GridHeaderRenderer(header.getDefaultRenderer()));
                    header.addMouseListener(new GridHeaderMouseListener());

                    setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
                    setDefaultEditor(Object.class, new ClientAbstractCellEditor());

                }

                public boolean addColumn(ClientCellView property) {

                    if (gridColumns.indexOf(property) == -1) {
                        Iterator<ClientCellView> icp = gridColumns.iterator();

                        // конечно кривова-то определять порядок по номеру в листе, но потом надо будет сделать по другому
                        int ind = formView.order.indexOf(property), ins = 0;

                        while (icp.hasNext() && formView.properties.indexOf(icp.next()) < ind) { ins++; }

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
//                    table.validate();

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

                // ---------------------------------------------------------------------------------------------- //
                // -------------------------------------- Поиски и отборы --------------------------------------- //
                // ---------------------------------------------------------------------------------------------- //

                private abstract class QueryModel {

                    public QueryView queryView;

                    List<ClientFilter> conditions;
                    Map<ClientFilter, QueryConditionView> conditionViews;

                    boolean hasChanged = false;

                    public QueryModel() {

                        conditions = new ArrayList();
                        conditionViews = new HashMap();

                        queryView = new QueryView();
                    }

                    public void applyQuery() {
                        hasChanged = false;

                        queryView.conditionsChanged();
                    }

                    public void addCondition() {

                        hasChanged = true;

                        ClientFilter condition = new ClientFilter();
                        conditions.add(condition);

                        QueryConditionView conditionView = new QueryConditionView(condition);
                        queryView.condviews.add(conditionView);

                        conditionViews.put(condition, conditionView);

                        queryView.conditionsChanged();

                        conditionView.valueView.requestFocusInWindow();

//                        container.validate();
                    }

                    public void removeCondition(ClientFilter condition) {

                        hasChanged = true;
                        
                        conditions.remove(condition);

                        queryView.condviews.remove(conditionViews.get(condition));
                        conditionViews.remove(condition);
                        
                        queryView.conditionsChanged();

//                        container.validate();
                    }

                    protected class QueryConditionView extends JPanel {

                        ClientFilter filter;

                        JComboBox propertyView;

                        JComboBox compareView;

                        JComboBox classValueLinkView;

                        ClientValueLinkView valueView;
                        Map<ClientValueLink, ClientValueLinkView> valueViews;

                        JButton delButton;

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

                        public QueryConditionView(ClientFilter ifilter) {

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
                                propertyView.setSelectedItem((ClientPropertyView)currentCell);
                            
                            filter.property = (ClientPropertyView) propertyView.getSelectedItem();

                            propertyView.addItemListener(new ItemListener() {

                                public void itemStateChanged(ItemEvent e) {

                                    filter.property = (ClientPropertyView)e.getItem();
                                    filterChanged();
                                }
                            });

//                            add(Box.createHorizontalStrut(4));

                            Pair<String,Integer>[] comparisons = new Pair[] {new Pair("=",0), new Pair(">",1), new Pair("<",2),
                                                                             new Pair(">=",3), new Pair("<=",4), new Pair("<>",5)};
                            compareView = new QueryConditionComboBox(comparisons);
                            add(compareView);

                            filter.compare = ((Pair<String,Integer>)compareView.getSelectedItem()).second; 

                            compareView.addItemListener(new ItemListener() {

                                public void itemStateChanged(ItemEvent e) {
                                    filter.compare = ((Pair<String,Integer>)e.getItem()).second;

                                    hasChanged = true;
                                    queryView.dataChanged();
                                }
                            });
//                            add(Box.createHorizontalStrut(4));

                            valueViews = new HashMap();
                            
                            ClientUserValueLink userValue = new ClientUserValueLink();
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

                                public void itemStateChanged(ItemEvent e) {
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

/*                            JButton test = new JButton("Test");
                            test.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    System.out.println(propertyView.getSize());
                                    System.out.println(compareView.getSize());
                                    System.out.println(classValueLinkView.getSize());
                                    System.out.println(valueView.getBounds());
                                }
                            });
                            add(test);*/
                        }

                        public void filterChanged() {

                            if (valueView != null)
                                remove(valueView);

                            valueView = valueViews.get(filter.value);
                            valueView.propertyChanged(filter.property);

                            if (valueView != null)
                                add(valueView);

                            add(delButton);

                            hasChanged = true;
                            queryView.dataChanged();

                            container.validate();

                        }

                        private abstract class ClientValueLinkView extends JPanel {

                            public ClientValueLinkView() {

                                setLayout(new BorderLayout());
                            }

                            abstract public void propertyChanged(ClientPropertyView property);

                        }

                        private class ClientUserValueLinkView extends ClientValueLinkView {

                            ClientUserValueLink valueLink;

                            CellModel cell;

                            public ClientUserValueLinkView(ClientUserValueLink ivalueLink, ClientPropertyView iproperty) {
                                super();

                                valueLink = ivalueLink;

                                cell = new CellModel(iproperty) {

                                    protected void cellValueChanged(Object ivalue) {
                                        super.cellValueChanged(ivalue);
                                        
                                        valueLink.value = ivalue;

                                        hasChanged = true;
                                        queryView.dataChanged();
                                    }

                                };
                                cell.view.remove(cell.view.label);

                                add(cell.view, BorderLayout.CENTER);
                            }

                            public boolean requestFocusInWindow() {
                                return cell.view.table.requestFocusInWindow();
                            }



                            public void propertyChanged(ClientPropertyView property) {
                                cell.setKey(property);
                            }
                        }

                        private class ClientObjectValueLinkView extends ClientValueLinkView {

                            ClientObjectValueLink valueLink;

                            JComboBox objectView;

                            public ClientObjectValueLinkView(ClientObjectValueLink ivalueLink) {

                                valueLink = ivalueLink;

                                Vector<ClientObjectImplement> objects = new Vector();
                                for (ClientObjectImplement object : formView.objects)
                                    objects.add(object);

                                objectView = new QueryConditionComboBox(objects);

                                valueLink.object = (ClientObjectImplement) objectView.getSelectedItem();

                                objectView.addItemListener(new ItemListener() {

                                    public void itemStateChanged(ItemEvent e) {
                                        valueLink.object = (ClientObjectImplement)e.getItem();

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

                            ClientPropertyValueLink valueLink;

                            JComboBox propertyView;

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

                        protected JPanel buttons;
                        protected JPanel condviews;

                        boolean collapsed = false;

                        Color defaultApplyBackground;

                        protected JButton applyButton;
                        protected Component centerGlue;
                        protected JButton addCondition;
                        protected JButton collapseButton;
                                                         
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

                                public void actionPerformed(ActionEvent e) {
                                    applyQuery();
                                }
                            });
                            defaultApplyBackground = applyButton.getBackground();

                            centerGlue = Box.createHorizontalGlue();

                            addCondition = new JButton("");
                            addCondition.setFocusable(false);
                            addCondition.setPreferredSize(iconButtonDimension);
                            addCondition.setMaximumSize(iconButtonDimension);
                            addCondition.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    collapsed = false;
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

                    public void applyQuery() {
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

                    public void applyQuery() {
                        changeFilter(conditions);
                        super.applyQuery();
                    }

                }


                // ---------------------------------------------------------------------------------------------- //
                // -------------------------------------- Сортировка -------------------------------------------- //
                // ---------------------------------------------------------------------------------------------- //

                private class GridHeaderRenderer implements TableCellRenderer {

                    private TableCellRenderer tableCellRenderer;

                    public GridHeaderRenderer(TableCellRenderer tableCellRenderer) {
                        this.tableCellRenderer = tableCellRenderer;
                    }

                    public Component getTableCellRendererComponent(JTable itable,
                                                                   Object value,
                                                                   boolean isSelected,
                                                                   boolean hasFocus,
                                                                   int row,
                                                                   int column) {

                        Component comp = tableCellRenderer.getTableCellRendererComponent(itable,
                                value, isSelected, hasFocus, row, column);
                        if (comp instanceof JLabel) {

                            JLabel label = (JLabel) comp;
                            label.setHorizontalAlignment(JLabel.CENTER);

                            ClientPropertyView property = table.getPropertyView(row, column);
                            if (property != null) {

                                int ordNum = orders.indexOf(property);
                                if (ordNum != -1) {

                                    label.setIcon((orderDirections.get(ordNum)) ? arrowUpIcon : arrowDownIcon);
//                                    label.setFont(label.getFont().deriveFont(Font.BOLD));
//                                    label.setHorizontalAlignment();
                                }

                            }

                       }
                        return comp;
                    }
                }

                private class GridHeaderMouseListener extends MouseAdapter {
                    
                    public void mouseClicked(MouseEvent e) {

                        if (e.getClickCount() != 2) return;
                        if (!(e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3)) return;

                        TableColumnModel columnModel = table.getColumnModel();
                        int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                        int column = columnModel.getColumn(viewColumn).getModelIndex();

                        if (column != -1) {

                            ClientPropertyView property = table.getPropertyView(0, column);
                            if (property != null) {

                                int ordNum = orders.indexOf(property);
                                if (ordNum == -1) {
                                    if (e.getButton() == MouseEvent.BUTTON1) {
                                        changeOrder(property, RemoteForm.ORDER_REPLACE);
                                        orders.clear();
                                    } else
                                        changeOrder(property, RemoteForm.ORDER_ADD);

                                    orders.add(property);
                                    orderDirections.add(true);

                                } else {
                                    if (e.getButton() == MouseEvent.BUTTON1) {
                                        orderDirections.set(ordNum, !orderDirections.get(ordNum));
                                    } else {
                                        changeOrder(property, RemoteForm.ORDER_REMOVE);
                                        orders.remove(ordNum);
                                        orderDirections.remove(ordNum);
                                    }
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

                        Object val = null;
                        val = gridValues.get(gridColumns.get(col)).get(gridRows.get(row));
                            
//                        if (val == null)
//                            return (String)"";
//                        else
                            return val;
                    }
                    
                    public void setValueAt(Object value, int row, int col) {
//                        System.out.println("setValueAt");
                        changeProperty(gridColumns.get(col),value);
                    }
                    
                    public ClientGroupObjectValue getSelectedObject() {
                        return gridRows.get(convertRowIndexToModel(getSelectedRow()));
                    }

                    public ClientCellView getSelectedCell() {
                        return gridColumns.get(convertColumnIndexToModel(getSelectedColumn()));
                    }
                }

                public ClientCellView getCellView(int row, int col) {
                    return gridColumns.get(col);
                }

                public ClientPropertyView getPropertyView(int row, int col) {
                    ClientCellView cell = getCellView(row, col);
                    if (cell instanceof ClientPropertyView)
                        return (ClientPropertyView) cell;
                    else
                        return null;
                }

            }
            
        }

        class ObjectModel {

            ClientObjectImplement object;

            JButton buttonAdd;
            JButton buttonChangeClass;
            JButton buttonDel;

            ClassModel classModel;

            public ObjectModel(ClientObjectImplement iobject) {

                object = iobject;

                buttonAdd = new JButton("Добавить(" + object.caption + ")");
                buttonAdd.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        addObject(object, classModel.getDerivedClass());
                    }

                });

                formLayout.add(groupObject.addView, buttonAdd);

                buttonDel = new JButton("Удалить(" + object.caption + ")");
                buttonDel.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        changeClass(object, null);
                    }

                });

                formLayout.add(groupObject.delView, buttonDel);

                classModel = new ClassModel(object.classView);

                if (classModel.rootClass.hasChilds) {
                    buttonChangeClass = new JButton("Изменить класс(" + object.caption + ")");
                    buttonChangeClass.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            changeClass(object, classModel.getSelectedClass());
                        }

                    });

                    formLayout.add(groupObject.changeClassView, buttonChangeClass);
                }

            }

            class ClassModel {

                ClientClassView key;

                DefaultMutableTreeNode rootNode;
                ClientClass rootClass;

                DefaultMutableTreeNode currentNode;
                ClientClass currentClass;

                JScrollPane pane;
                ClassTree view;

                public ClassModel(ClientClassView ikey) {

                    key = ikey;

                    rootClass = ByteArraySerializer.deserializeClientClass(remoteForm.getBaseClassByteArray(object.ID));
                    currentClass = rootClass;

                    rootNode = new DefaultMutableTreeNode(rootClass);
                    currentNode = rootNode;

                    view = new ClassTree();
                    pane = new JScrollPane(view);
                }

                public void addClassTree() {
                    if (rootClass.hasChilds)
                        formLayout.add(key, pane);
                }

                public void removeClassTree() {
                    formLayout.remove(key, pane);
                }

                public ClientClass getDerivedClass() {

                    TreePath path = view.getSelectionModel().getLeadSelectionPath();
                    if (path == null) return currentClass;

                    DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (selNode == null || !currentNode.isNodeChild(selNode)) return currentClass;

                    return (ClientClass) selNode.getUserObject();
                }

                public ClientClass getSelectedClass() {

                    TreePath path = view.getSelectionModel().getLeadSelectionPath();
                    if (path == null) return currentClass;

                    DefaultMutableTreeNode selNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (selNode == null) return currentClass;

                    return (ClientClass) selNode.getUserObject();
                }

                class ClassTree extends JTree {

                    DefaultTreeModel model;

                    int ID;

                    @Override
                    public int hashCode() {
                        return ID;
                    }

                    @Override
                    public boolean equals(Object o) {
                        if (!(o instanceof ClassTree))
                            return false;
                        return ((ClassTree)o).ID == this.ID;
                    }

                    public ClassTree() {

                        ID = object.ID;

                        setBorder(new EtchedBorder(EtchedBorder.LOWERED));

                        model = new DefaultTreeModel(rootNode);

                        setModel(model);

                        addTreeExpansionListener(new TreeExpansionListener() {

                            public void treeExpanded(TreeExpansionEvent event) {
                                addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                            }

                            public void treeCollapsed(TreeExpansionEvent event) {};

                        });

                        addMouseListener(new MouseAdapter() {

                            public void mouseReleased(MouseEvent e) {
                                if (e.getClickCount() == 2) {

                                    TreePath path = getSelectionPath();
                                    if (path == null) return;

                                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                                    if (node == null) return;

                                    Object nodeObject = node.getUserObject();
                                    if (! (nodeObject instanceof ClientClass)) return;

                                    changeGridClass(object, (ClientClass) nodeObject);
                                    currentNode = node;
                                    currentClass = (ClientClass) nodeObject;
                                    view.updateUI();
                                }
                            }
                        });

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

//                                    Object nodeObject = node.getUserObject();
//                                    if (nodeObject != null && nodeObject instanceof ClientClass && ((ClientClass)nodeObject == currentClass))
                                    if (node == currentNode)
                                        setFont(getFont().deriveFont(Font.BOLD));
                                }

                                Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                                return comp;

                            }
                        });

                        if (rootClass.hasChilds) {
                            rootNode.add(new ExpandingTreeNode());
                            expandPath(new TreePath(rootNode));
                        }


                    }

                    private void addNodeElements(DefaultMutableTreeNode parent) {

                        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

                        if (! (firstChild instanceof ExpandingTreeNode)) return;
                        parent.removeAllChildren();

                        Object nodeObject = parent.getUserObject();
                        if (nodeObject != null && ! (nodeObject instanceof ClientClass) ) return;

                        ClientClass parentClass = (ClientClass) nodeObject;

                        List<ClientClass> classes = ByteArraySerializer.deserializeListClientClass(
                                                                        remoteForm.getChildClassesByteArray(object.ID,parentClass.ID));

                        for (ClientClass cls : classes) {

                            DefaultMutableTreeNode node;
                            node = new DefaultMutableTreeNode(cls, cls.hasChilds);
                            parent.add(node);

                            if (cls.hasChilds)
                                node.add(new ExpandingTreeNode());

                        }

                        model.reload(parent);
                    }

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
                contviews.get(component.container).add(view, component.constraints);
                return true;
            } else
                return false;
        }

        private boolean remove(ClientComponentView component, Component view) {
           if (contviews.get(component.container).isAncestorOf(view)) {
                contviews.get(component.container).remove(view);
                return true;
           } else
                return false;
        }
        
        class ContainerView extends JPanel {
            
            ClientContainerView view;
            
            public ContainerView(ClientContainerView iview) {
                
                view = iview;
                
                setLayout(globalLayout);
                
                Random rnd = new Random();
//                this.setBackground(new Color(rnd.nextInt(255),rnd.nextInt(255),rnd.nextInt(255)));
//                setLayout(new SimplexLayout());
//                setLayout(new FlowLayout());
                setPreferredSize(new Dimension(10000, 10000));
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
        else
            setText("");
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
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }
    
}

class DatePropertyRenderer extends LabelPropertyRenderer
                           implements PropertyRendererComponent {

    public static final DateFormat dateFormat = DateFormat.getDateInstance();

    public DatePropertyRenderer() {
        super();

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public Component getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(dateFormat.format(DateConverter.intToDate((Integer)value)));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}

class BitPropertyRenderer extends JCheckBox
                          implements PropertyRendererComponent {

    public BitPropertyRenderer() {
        super();

        setHorizontalAlignment(JCheckBox.CENTER);

        setBorder(new EmptyBorder(1, 1, 2, 2));
        setOpaque(true);
    }

    public Component getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setSelected((Integer)value != 0);
        else
            setSelected(false);

        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else
            setBackground(Color.white);
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
//        setBackground(new Color(128,128,255));
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

class DatePropertyEditor extends JDateChooser
                           implements PropertyEditorComponent {

    public DatePropertyEditor() {
        super(null, null, "dd.MM.yy", new DatePropertyEditorComponent("dd.MM.yy","##.##.##",' '));

    }

/*    @Override
    public void requestFocus() {
        super.requestFocus();
    }*/

    @Override
    public boolean processKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        return ((DatePropertyEditorComponent)dateEditor).publicProcessKeyBinding(ks, ke, condition, pressed);
    }

/*    @Override
    public void setNextFocusableComponent(Component comp) {
        ((JComponent)dateEditor).setNextFocusableComponent(comp);
    }*/


    public Component getComponent() {
        return this;
    }

    public void setValue(Object value) {
        if (value != null)
            setDate(DateConverter.intToDate((Integer)value));
        ((JFormattedTextField)dateEditor).selectAll();
    }

    public Object getValue() {
        return DateConverter.dateToInt(getDate());
    }

}

class DatePropertyEditorComponent extends JTextFieldDateEditor {

    public DatePropertyEditorComponent(String datePattern, String maskPattern, char placeholder) {
        super(datePattern, maskPattern, placeholder);

        setBorder(new EmptyBorder(0, 1, 0, 0));

        SwingUtils.addFocusTraversalKey(this,
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

    }

    //а вот так будем дурить их protected метод
    public boolean publicProcessKeyBinding(KeyStroke ks, KeyEvent ke, int condition, boolean pressed) {
        return processKeyBinding(ks, ke, condition, pressed);
    }

    @Override
    public void focusLost(FocusEvent focusEvent) {
        super.focusLost(focusEvent);
    }

}

class BitPropertyEditor extends JCheckBox
                        implements PropertyEditorComponent {

    public BitPropertyEditor() {

        setHorizontalAlignment(JCheckBox.CENTER);

        setBorder(new EmptyBorder(0, 1, 0, 0));
        setOpaque(true);

        setBackground(Color.white);
    }

    public Component getComponent() {
        return this;
    }

    public void setValue(Object value) {
        if (value != null)
            setSelected((Integer) value != 0);
    }

    public Object getValue() {
        return (isSelected()) ? 1 : 0;
    }
}

class SingleCellTable extends JTable {
    
    public SingleCellTable() {
       
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                requestFocusInWindow();
                changeSelection(0, 0, false, false);
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