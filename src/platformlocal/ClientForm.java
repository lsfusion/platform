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
import java.text.*;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.swing.JTextField;
import javax.swing.*;
import javax.swing.text.NumberFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.tree.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.table.*;

import bibliothek.gui.dock.DefaultDockable;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

interface ClientCellViewTable {

    ClientCellView getCellView(int row, int col);

}

public class ClientForm extends JPanel {

    private final ClientFormView formView;

    public final RemoteForm remoteForm;

    // Icons - загружаем один раз, для экономии
    private final ImageIcon arrowUpIcon = new ImageIcon(getClass().getResource("images/arrowup.gif"));
    private final ImageIcon arrowDownIcon = new ImageIcon(getClass().getResource("images/arrowdown.gif"));
    private final ImageIcon filtIcon = new ImageIcon(getClass().getResource("images/filt.gif"));
    private final ImageIcon filtAddIcon = new ImageIcon(getClass().getResource("images/filtadd.gif"));
    private final ImageIcon findIcon = new ImageIcon(getClass().getResource("images/find.gif"));
    private final ImageIcon findAddIcon = new ImageIcon(getClass().getResource("images/findadd.gif"));
    private final ImageIcon deleteIcon = new ImageIcon(getClass().getResource("images/delete.gif"));
    private final ImageIcon collapseIcon = new ImageIcon(getClass().getResource("images/collapse.gif"));
    private final ImageIcon expandIcon = new ImageIcon(getClass().getResource("images/expand.gif"));

    private final static Dimension iconButtonDimension = new Dimension(22,22);


    public ClientForm(RemoteForm iremoteForm) {
//        super(app);

//        FocusOwnerTracer.installFocusTracer();

        remoteForm = iremoteForm;

//        getFrame().setTitle(caption);

        byte[] state = remoteForm.GetRichDesignByteArray();
        Log.incrementBytesReceived(state.length);
        formView = ByteArraySerializer.deserializeClientFormView(state);

        initializeForm();

        applyFormChanges();
    }

    private FormLayout formLayout;

    Map<ClientGroupObjectImplement, GroupObjectModel> models;

    private JButton buttonPrint;
    private JButton buttonApply;
    private JButton buttonCancel;
    private JButton buttonOK;
    private JButton buttonClose;

    void initializeForm() {

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

        buttonPrint = new JButton("Печать");
        buttonPrint.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                print();
            }
        });

        formLayout.add(formView.printView, buttonPrint);

        buttonApply = new JButton("Применить");
        buttonApply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveChanges();
            }
        });

        formLayout.add(formView.applyView, buttonApply);

        buttonCancel = new JButton("Отменить");
        buttonCancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelChanges();
            }
        });

        formLayout.add(formView.cancelView, buttonCancel);

        AbstractAction okAction = new AbstractAction("OK") {

            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        };

        buttonOK = new JButton(okAction);
        formLayout.add(formView.okView, buttonOK);

        buttonClose = new JButton("Закрыть");
        buttonClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closePressed();
            }
        });

        formLayout.add(formView.closeView, buttonClose);

        dataReset();

        //  Have the enter key work the same as the tab key
		InputMap im = getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
        im.put(altEnter, "okPressed");

        ActionMap am = getActionMap();
        am.put("okPressed", okAction);

    }

    void applyFormChanges() {

        try {
            byte[] state = remoteForm.getFormChangesByteArray();
            Log.incrementBytesReceived(state.length);
            applyFormChanges(ByteArraySerializer.deserializeClientFormChanges(state, formView));
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
                dataChanged();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            applyFormChanges();
        }
    }

    void addObject(ClientObjectImplement object, ClientClass cls) {
        try {
            remoteForm.AddObject(object.ID, cls.ID);
            dataChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        applyFormChanges();
    }

    void changeClass(ClientObjectImplement object, ClientClass cls) {
        try {
            remoteForm.ChangeClass(object.ID, (cls == null) ? -1 : cls.ID);
            dataChanged();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        applyFormChanges();
    }

    void changeGridClass(ClientObjectImplement object, ClientClass cls) {

        try {
            remoteForm.ChangeGridClass(object.ID, cls.ID);
        } catch (SQLException e) {
            e.printStackTrace();
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

        models.get(groupObject).setClassView(classView, true);

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
        JasperReport Report;
        try {
            Report = JasperCompileManager.compileReport(Design);
            JasperPrint Print = JasperFillManager.fillReport(Report,new HashMap(),remoteForm.ReadData());
            Main.Layout.DefaultStation.drop(new ReportDockable(Print));
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
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
//        JasperCompileManager.writeReportToXmlFile(Report,"report.xml");
//        JasperExportManager.exportReportToPdfFile(Print, "report.pdf");
    }

    boolean saveChanges() {

        if (formHasChanged) {

            try {
                String message = remoteForm.SaveChanges();
                if (message==null) {
                    Log.printSuccessMessage("Изменения были удачно записаны...");
                    dataReset();
                }
                else {
                    Log.printFailedMessage(message);
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            applyFormChanges();
        }
        
        return true;
    }

    boolean cancelChanges() {

        if (formHasChanged) {

            remoteForm.CancelChanges();
            dataReset();
            applyFormChanges();
        }

        return true;
    }

    boolean okPressed() {
        return saveChanges();
    }

    boolean closePressed() {
        return cancelChanges();
    }


    private boolean formHasChanged;
    private Color defaultApplyBackground;

    private void dataChanged() {

        formHasChanged = true;

        buttonApply.setBackground(Color.green);
        buttonApply.setEnabled(true);
        buttonCancel.setEnabled(true);

    }

    private void dataReset() {

        formHasChanged = false;

        if (defaultApplyBackground != null)
            buttonApply.setBackground(defaultApplyBackground);
        else
            defaultApplyBackground = buttonApply.getBackground();

        buttonApply.setEnabled(false);
        buttonCancel.setEnabled(false);
    }

    ClientCellView editingCell;

    class GroupObjectModel {

        final ClientGroupObjectImplement groupObject;

        final PanelModel panel;
        final GridModel grid;
        final Map<ClientObjectImplement, ObjectModel> objects = new HashMap();

        ClientGroupObjectValue currentObject;

        ClientCellView currentCell;

        Boolean classView = false;

        public GroupObjectModel(ClientGroupObjectImplement igroupObject) {

            groupObject = igroupObject;

            grid = new GridModel(groupObject.gridView);

            panel = new PanelModel();

            for (ClientObjectImplement object : groupObject) {

                objects.put(object, new ObjectModel(object));
            }
            
            setClassView(true, false);
        }
        
        public void setClassView(Boolean iclassView, boolean userChange) {
            
            if (classView != iclassView) {
                
                classView = iclassView;
                if (classView) {
                    panel.removeGroupObjectID();
                    grid.addGroupObjectID();
                    if (userChange)
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                grid.table.requestFocusInWindow();
                            }
                        });
                } else {
                    panel.addGroupObjectID();
                    grid.removeGroupObjectID();
                    if (userChange)
                        panel.getObjectIDView(0).requestFocusInWindow();
//                    panel.requestFocusInWindow();
                }

                for (ClientObjectImplement object : groupObject) {
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

        // приходится делать именно так, так как логика отображения одного GroupObject може не совпадать с логикой Container-Component
        public void addGroupObjectActions(JComponent comp) {

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "switchClassView");
            comp.getActionMap().put("switchClassView", new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    switchClassView(groupObject);
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_DOWN_MASK), "addObject");
            comp.getActionMap().put("addObject", new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    addObject(groupObject.get(0), objects.get(groupObject.get(0)).classModel.getDerivedClass());
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK), "removeObject");
            comp.getActionMap().put("removeObject", new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    changeClass(groupObject.get(0), null);
                }
            });

            comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_DOWN_MASK), "changeObjectClass");
            comp.getActionMap().put("changeObjectClass", new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    changeClass(groupObject.get(0), objects.get(groupObject.get(0)).classModel.getSelectedClass());
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
                
                ClientCellView property = ((ClientCellViewTable)table).getCellView(row, column);
                PropertyRendererComponent currentComp = property.getRendererComponent(ClientForm.this);
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
            Object value;
            
            public Object getCellEditorValue() {
                
                return currentComp.getCellEditorValue();
                
            }

            public boolean isCellEditable(EventObject e) {

                if (e instanceof KeyEvent) {

                    KeyEvent event = (KeyEvent) e;

                    if (event.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;

                    //будем считать, что если нажата кнопка ALT то явно пользователь не хочет вводить текст
                    if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0) return false;

                    return true;
                }

                if (e instanceof MouseEvent) {

                    MouseEvent event = (MouseEvent) e;

                    return event.getClickCount() >= 2;
                }

                return false;
            }

            public Component getTableCellEditorComponent(JTable table,
                                                         Object ivalue, 
                                                         boolean isSelected, 
                                                         int row, 
                                                         int column) {
                
                value = ivalue;
                
                ClientCellView property = ((ClientCellViewTable)table).getCellView(row, column);
                editingCell = property;
                currentComp = property.getEditorComponent(ClientForm.this);
                
                if (currentComp != null) {

                    currentComp.setCellEditorValue(value);

                    Component comp = currentComp.getComponent();
                    if (comp == null) {
                        Object newValue = getCellEditorValue();
                        if ((value == null && newValue != null) || ! value.equals(newValue))
                            table.setValueAt(newValue, row, column);
                    }
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

                    ID = key.ID;

                    label.setText(key.caption);

                    table.keyChanged();
                }

                class CellTable extends SingleCellTable
                                    implements ClientCellViewTable {

                    PropertyModel model;

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

                models = new HashMap();
            }

            public void addGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelCellModel idmodel = new PanelCellModel(object.objectIDView);
                    formLayout.add(idmodel.key, idmodel.view);

                    models.put(object.objectIDView, idmodel);

                }
                setGroupObjectIDValue(currentObject);
                
            }
            
            public void removeGroupObjectID() {
                
                for (ClientObjectImplement object : groupObject) {
                    
                    PanelCellModel idmodel = models.get(object.objectIDView);
                    if (idmodel != null) {
                        formLayout.remove(idmodel.key, idmodel.view);
                        models.remove(object.objectIDView);
                    }
                }
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

                    addGroupObjectActions(view);
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

                queriesContainer.add(table.findModel.queryView);
                queriesContainer.add(Box.createRigidArea(new Dimension(4,0)));
                queriesContainer.add(table.filterModel.queryView);
                queriesContainer.add(Box.createHorizontalGlue());

                container.add(pane);
                container.add(queriesContainer);

                addGroupObjectActions(table);

                container.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "addFind");
                container.getActionMap().put("addFind", new AbstractAction() {

                    public void actionPerformed(ActionEvent e) {
                        table.findModel.addCondition();
                    }
                });

                container.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "addFilter");
                container.getActionMap().put("addFilter", new AbstractAction() {

                    public void actionPerformed(ActionEvent e) {
                        table.filterModel.addCondition();
                    }
                });

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

                    TableColumn column = table.getColumnModel().getColumn(table.gridColumns.indexOf(property));
                    column.setPreferredWidth(property.getPreferredWidth());
                    column.setMinWidth(property.getMinimumWidth());
                }

                if (table.gridColumns.size() != 0) {
                    formLayout.add(view, container);
                } else {
                    formLayout.remove(view, container);
                }
                
            }

            public class Table extends ClientFormTable
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

                public Table() {

                    ID = groupObject.GID;

                    model = new Model();
                    setModel(model);

                    header = getTableHeader();

                    findModel = new FindModel();
                    filterModel = new FilterModel();

                    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
//                            System.out.println("changeSel");
                            changeObject(groupObject, model.getSelectedObject());
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
                            ViewPos.y += dltpos;
                            if (ViewPos.y < 0) ViewPos.y = 0;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    pane.getViewport().setViewPosition(ViewPos);
//                                        System.out.println("Viewpos : " + ViewPos);
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
                    if (newindex != -1 && newindex != oldindex) {
                        //Выставляем именно первую активную колонку, иначе фокус на таблице - вообще нереально увидеть
                        changeSelection(newindex, 0, false, false);
//                        getSelectionModel().setLeadSelectionIndex(newindex);
                    }

                }

                public void setColumnValues(ClientCellView property, Map<ClientGroupObjectValue,Object> values) {

                    gridValues.put(property, values);
                    repaint();

                }

                private Object getSelectedValue() {

                    return getSelectedValue(getSelectedColumn());
                }

                private Object getSelectedValue(ClientCellView cell) {
                    return getSelectedValue(gridColumns.indexOf(cell));
                }


                private Object getSelectedValue(int col) {

                    int row = getSelectedRow();
                    if (row != -1 && col != -1)
                        return getValueAt(row, col);
                    else
                        return null;
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

                        table.requestFocusInWindow();

                    }

                    public void addCondition() {

                        queryView.collapsed = false;
                        
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

                    public void removeAllConditions() {

                        hasChanged = true;

                        conditions.clear();
                        conditionViews.clear();

                        queryView.condviews.removeAll();

                        queryView.conditionsChanged();
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
                                propertyView.setSelectedItem(currentCell);
                            
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

                            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), "applyQuery");
                            getActionMap().put("applyQuery", new AbstractAction() {

                                public void actionPerformed(ActionEvent e) {
                                    valueView.stopEditing();
                                    applyQuery();
                                }
                            });

                            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.ALT_DOWN_MASK), "removeAll");
                            getActionMap().put("removeAll", new AbstractAction() {

                                public void actionPerformed(ActionEvent e) {
                                    removeAllConditions();
                                    applyQuery();
                                }
                            });

                        }

                        public void filterChanged() {

                            if (valueView != null)
                                remove(valueView);

                            valueView = valueViews.get(filter.value);
                            if (valueView != null) {
                                add(valueView);
                            }

                            valueView.propertyChanged(filter.property);
                            
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

                            public void stopEditing() {}

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
                                cell.setValue(table.getSelectedValue(property));
                            }

                            public void stopEditing() {
                                CellEditor editor = cell.view.table.getCellEditor();
                                if (editor != null)
                                    editor.stopCellEditing();
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

                        JPanel buttons;
                        protected JPanel condviews;

                        boolean collapsed = false;

                        Color defaultApplyBackground;

                        JButton applyButton;
                        Component centerGlue;
                        JButton addCondition;
                        JButton collapseButton;
                                                         
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
                                        changeOrder(property, RemoteForm.ORDER_DIR);
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

                        return gridValues.get(gridColumns.get(col)).get(gridRows.get(row));
                    }
                    
                    public void setValueAt(Object value, int row, int col) {
                        changeProperty(gridColumns.get(col),value);
                    }
                    
                    public ClientGroupObjectValue getSelectedObject() {
                        int rowModel = convertRowIndexToModel(getSelectedRow());
                        if (rowModel < 0)
                            return null;

                        return gridRows.get(rowModel);
                    }

                    public ClientCellView getSelectedCell() {
                        int colView = getSelectedColumn();
                        int colModel = convertColumnIndexToModel(colView);
                        if (colModel < 0)
                            return null;

                        return gridColumns.get(colModel);
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

                String extraCaption = ((groupObject.size() > 1) ? ("(" + object.caption + ")") : "");

                buttonAdd = new JButton("Добавить" + extraCaption);
                buttonAdd.setFocusable(false);
                buttonAdd.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        addObject(object, classModel.getDerivedClass());
                    }

                });

                formLayout.add(groupObject.addView, buttonAdd);

                buttonDel = new JButton("Удалить" + extraCaption);
                buttonDel.setFocusable(false);
                buttonDel.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        changeClass(object, null);
                    }

                });

                formLayout.add(groupObject.delView, buttonDel);

                classModel = new ClassModel(object.classView);

                if (classModel.rootClass.hasChilds) {
                    buttonChangeClass = new JButton("Изменить класс" + extraCaption);
                    buttonChangeClass.setFocusable(false);
                    buttonChangeClass.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            changeClass(object, classModel.getSelectedClass());
                        }

                    });

                    formLayout.add(groupObject.changeClassView, buttonChangeClass);
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

                private DefaultMutableTreeNode getSelectedNode() {

                    TreePath path = view.getSelectionModel().getLeadSelectionPath();
                    if (path == null) return null;

                    return (DefaultMutableTreeNode) path.getLastPathComponent();
                }

                public ClientClass getDerivedClass() {

                    DefaultMutableTreeNode selNode = getSelectedNode();
                    if (selNode == null || !currentNode.isNodeChild(selNode)) return currentClass;

                    return (ClientClass) selNode.getUserObject();
                }

                public ClientClass getSelectedClass() {

                    DefaultMutableTreeNode selNode = getSelectedNode();
                    if (selNode == null) return currentClass;

                    return (ClientClass) selNode.getUserObject();
                }

                public void changeCurrentClass(ClientClass cls, DefaultMutableTreeNode clsNode) {

                    if (cls == null) return;

                    changeGridClass(object, cls);
                    currentClass = cls;
                    currentNode = clsNode;
                    view.updateUI();

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
                                addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                            }

                            public void treeCollapsed(TreeExpansionEvent event) {}

                        });

                        addMouseListener(new MouseAdapter() {

                            public void mouseReleased(MouseEvent e) {
                                if (e.getClickCount() == 2) {
                                    changeCurrentClass(getSelectedClass(), getSelectedNode());
                                }
                            }
                        });

                        addKeyListener(new KeyAdapter() {

                            public void keyPressed(KeyEvent e) {
                                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                                    changeCurrentClass(getSelectedClass(), getSelectedNode());
                                }
                            }
                        });

                        if (rootClass.hasChilds) {
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

//                                    Object nodeObject = node.getUserObject();
//                                    if (nodeObject != null && nodeObject instanceof ClientClass && ((ClientClass)nodeObject == currentClass))
                                    if (node == currentNode)
                                        setFont(getFont().deriveFont(Font.BOLD));
                                }

                                return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                            }

                        });
                    }

                    private void addNodeElements(DefaultMutableTreeNode parent) {

                        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

                        if (! (firstChild instanceof ExpandingTreeNode)) return;
                        parent.removeAllChildren();

                        Object nodeObject = parent.getUserObject();
                        if (nodeObject == null || ! (nodeObject instanceof ClientClass) ) return;

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

                    public DefaultMutableTreeNode getSelectedNode() {

                        TreePath path = getSelectionPath();
                        if (path == null) return null;

                        return (DefaultMutableTreeNode) path.getLastPathComponent();
                    }

                    public ClientClass getSelectedClass() {

                        DefaultMutableTreeNode node = getSelectedNode();
                        if (node == null) return null;
                        
                        Object nodeObject = node.getUserObject();
                        if (! (nodeObject instanceof ClientClass)) return null;
                        return (ClientClass) nodeObject;
                    }

                }

            }

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
                contviews.get(component.container).repaint();
                return true;
            } else
                return false;
        }

        private boolean remove(ClientComponentView component, Component view) {
           if (contviews.get(component.container).isAncestorOf(view)) {
                contviews.get(component.container).remove(view);
                contviews.get(component.container).repaint();
                return true;
           } else
                return false;
        }
        
        class ContainerView extends JPanel {
            
            ClientContainerView view;
            
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

            }
        }
    }
}