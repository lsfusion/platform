package lsfusion.client.descriptor.editor;

import lsfusion.base.BaseUtils;
import lsfusion.base.OrderedMap;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.base.context.IncrementView;
import lsfusion.client.Main;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.PropertyDrawDescriptor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementMultipleListEditor;
import lsfusion.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import lsfusion.client.descriptor.increment.editor.IncrementSingleListSelectionModel;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public class DefaultOrdersEditor extends JPanel implements IncrementView {

    private static final ImageIcon upIcon = new ImageIcon(Main.class.getResource("/images/arrowup.png"));
    private static final ImageIcon downIcon = new ImageIcon(Main.class.getResource("/images/arrowdown.png"));

    private final FormDescriptor form;
    private DataHolder dataHolder;
    private IncrementDefaultOrdersTable table;

    public DefaultOrdersEditor(FormDescriptor iForm, final GroupObjectDescriptor group) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.form = iForm;

        dataHolder = new DataHolder(form);
        if (group != null) {
            dataHolder.setGroupObject(group);
        }

        form.addDependency(dataHolder, "defaultOrders", this);

        if (group == null) {
            add(new TitledPanel(getString("descriptor.editor.group"), new JComboBox(new IncrementSingleListSelectionModel(dataHolder, "groupObject", true) {
                public List<?> getSingleList() {
                    return form.groupObjects;
                }

                public void fillListDependencies() {
                    form.addDependency(form, "groupObjects", this);
                }
            })));
        }

        JPanel propertiesPanel = new TitledPanel(getString("descriptor.editor.pending.properties"), new JScrollPane(new IncrementMultipleListEditor(
                new IncrementMultipleListSelectionModel(dataHolder, "pendingProperties") {

            public List<?> getList() {
                return dataHolder.getAvailableProperties();
            }

            public void fillListDependencies() {
                form.addDependency(form, "propertyDraws", this);
                form.addDependency(dataHolder, "defaultOrders", this);
                form.addDependency(dataHolder, "groupObject", this);
            }
        })));

        JButton addBtn = new JButton(new AbstractAction(getString("descriptor.editor.pending.add")) {
            public void actionPerformed(ActionEvent e) {
                dataHolder.addToDefaultOrders(dataHolder.getPendingProperties());
                dataHolder.clearPendingProperties();
            }
        });
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton delBtn = new JButton(new AbstractAction(getString("descriptor.editor.pending.delete")) {
            public void actionPerformed(ActionEvent e) {
                dataHolder.removeFromDefaultOrders(table.getSelectedProperties());
            }
        });
        delBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton upBtn = new JButton(new AbstractAction("", upIcon) {
            public void actionPerformed(ActionEvent e) {
                table.moveProperties(true);
            }
        });
        upBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton downBtn = new JButton(new AbstractAction("", downIcon) {
            public void actionPerformed(ActionEvent e) {
                table.moveProperties(false);
            }
        });
        downBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel tablePanel = new TitledPanel(getString("descriptor.editor.chosen.properties"), new JScrollPane(table = new IncrementDefaultOrdersTable()));
        tablePanel.setPreferredSize(new Dimension(300, 200));

        JPanel selectPanel = new JPanel();
        selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.PAGE_AXIS));
        selectPanel.add(addBtn);
        selectPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        selectPanel.add(delBtn);

        JPanel movePanel = new JPanel();
        movePanel.setLayout(new BoxLayout(movePanel, BoxLayout.PAGE_AXIS));
        movePanel.add(upBtn);
        movePanel.add(Box.createRigidArea(new Dimension(5, 15)));
        movePanel.add(downBtn);

        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.X_AXIS));
        newPanel.add(propertiesPanel);
        newPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        newPanel.add(selectPanel);
        newPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        newPanel.add(tablePanel);
        newPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        newPanel.add(movePanel);
        newPanel.add(Box.createRigidArea(new Dimension(5, 5)));

        add(newPanel);
    }

    public void update(Object updateObject, String updateField) {
        OrderedMap<PropertyDrawDescriptor, Boolean> newOrders = new OrderedMap<PropertyDrawDescriptor, Boolean>();
        for (PropertyDrawDescriptor propertyDraw : dataHolder.defaultOrdersProps) {
            newOrders.put(propertyDraw, dataHolder.defaultOrders.get(propertyDraw));
        }
        form.setDefaultOrders(newOrders);
    }

    public class IncrementDefaultOrdersTable extends JTable {
        private final String columnNames[] = new String[]{
                getString("descriptor.editor.chosen.properties.property"),
                getString("descriptor.editor.chosen.properties.ascending")
        };

        public IncrementDefaultOrdersTable() {
            super();
            setModel(new OrdersModel());
            getColumnModel().getColumn(0).setPreferredWidth(50000);
            getColumnModel().getColumn(1).setPreferredWidth(25000);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(50, super.getPreferredSize().height);
        }

        public List<PropertyDrawDescriptor> getSelectedProperties() {
            List<PropertyDrawDescriptor> result = new ArrayList<PropertyDrawDescriptor>();
            for (int rowIndex : getSelectedRows()) {
                result.add(dataHolder.getSelectedProperties().get(rowIndex));
            }
            return result;
        }

        public void moveProperties(boolean up) {
            int[] indices = getSelectedRows();
            int[] newIndices = dataHolder.moveProperties(indices, up);

            for (int ind : newIndices) {
                getSelectionModel().addSelectionInterval(ind, ind);
            }
        }

        private class OrdersModel extends AbstractTableModel implements IncrementView {
            public OrdersModel() {
                form.addDependency(dataHolder, "groupObject", this);
                form.addDependency(dataHolder, "defaultOrders", this);
            }

            public void update(Object updateObject, String updateField) {
                fireTableDataChanged();
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex != 0;
            }

            public int getRowCount() {
                return dataHolder.getSelectedProperties().size();
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }
                return Object.class;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                PropertyDrawDescriptor propertyDraw = dataHolder.getSelectedProperties().get(rowIndex);
                switch (columnIndex) {
                    case 0:
                        return propertyDraw;
                    case 1:
                        return dataHolder.defaultOrders.get(propertyDraw);
                }
                return null;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex == 1 && aValue instanceof Boolean) {
                    boolean direction = (Boolean) aValue;
                    dataHolder.setOrderDirection(dataHolder.getSelectedProperties().get(rowIndex), direction);
                }
            }
        }
    }

    public static class DataHolder implements ApplicationContextProvider {
        private final FormDescriptor form;

        private GroupObjectDescriptor groupObject;

        private List<PropertyDrawDescriptor> pendingProperties = new ArrayList<PropertyDrawDescriptor>();

        public Map<PropertyDrawDescriptor, Boolean> defaultOrders = new HashMap<PropertyDrawDescriptor, Boolean>();
        public List<PropertyDrawDescriptor> defaultOrdersProps = new ArrayList<PropertyDrawDescriptor>();

        public DataHolder(FormDescriptor form) {
            this.form = form;
            defaultOrders.putAll(form.defaultOrders);
            defaultOrdersProps.addAll(form.defaultOrders.keySet());
            form.updateDependency(this, "defaultOrders");
        }

        public void setGroupObject(GroupObjectDescriptor groupObject) {
            this.groupObject = groupObject;
            form.updateDependency(this, "groupObject");
        }

        public GroupObjectDescriptor getGroupObject() {
            return groupObject;
        }

        public void setPendingProperties(List<PropertyDrawDescriptor> pendingProperties) {
            this.pendingProperties = pendingProperties;
            form.updateDependency(this, "pendingProperties");
        }

        public List<PropertyDrawDescriptor> getPendingProperties() {
            return pendingProperties;
        }

        public void clearPendingProperties() {
            pendingProperties.clear();
            form.updateDependency(this, "pendingProperties");
        }

        private void addToDefaultOrders(List<PropertyDrawDescriptor> newProperties) {
            for (PropertyDrawDescriptor property : newProperties) {
                defaultOrders.put(property, false);
                defaultOrdersProps.remove(property);
                defaultOrdersProps.add(property);
            }

            form.updateDependency(this, "defaultOrders");
        }

        private void removeFromDefaultOrders(List<PropertyDrawDescriptor> properties) {
            defaultOrdersProps.removeAll(properties);
            for (PropertyDrawDescriptor property : properties) {
                defaultOrders.remove(property);
            }

            form.updateDependency(this, "defaultOrders");
        }

        public List<PropertyDrawDescriptor> getSelectedProperties() {
            return BaseUtils.filterList(defaultOrdersProps, form.getGroupPropertyDraws(groupObject));
        }

        public List<PropertyDrawDescriptor> getAvailableProperties() {
            return BaseUtils.removeList(form.getGroupPropertyDraws(groupObject), defaultOrdersProps);
        }

        public int[] moveProperties(int[] indices, boolean up) {
            Arrays.sort(indices);
            int[] newIndices = new int[indices.length];

            int begi = up ? 0 : indices.length - 1;
            int endi = up ? indices.length - 1 : 0;
            int di = up ? +1 : -1;
            int firstIndex = up ? 0 : defaultOrdersProps.size() - 1;

            while (begi != endi + di && indices[begi] == firstIndex) {
                newIndices[begi] = indices[begi];
                begi += di;
                firstIndex += di;
            }

            for (int i = begi; i != endi + di; i += di) {
                int index = indices[i];

                PropertyDrawDescriptor property = defaultOrdersProps.get(index);
                defaultOrdersProps.remove(index);
                defaultOrdersProps.add(index - di, property);

                newIndices[i] = index - di;
            }
            form.updateDependency(this, "defaultOrders");

            return newIndices;
        }

        public void setOrderDirection(PropertyDrawDescriptor property, boolean isAscending) {
            defaultOrders.put(property, isAscending);
            form.updateDependency(this, "defaultOrders");
        }

        public ApplicationContext getContext() {
            return form.getContext();
        }
    }
}
