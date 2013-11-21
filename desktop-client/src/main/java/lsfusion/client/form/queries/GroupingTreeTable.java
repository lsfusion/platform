package lsfusion.client.form.queries;

import lsfusion.base.OrderedMap;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientLogicalClass;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.jdesktop.swingx.treetable.TreeTableNode;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Time;
import java.util.*;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public class GroupingTreeTable extends JXTreeTable {
    private GroupingTreeTableModel treeTableModel;
    
    public GroupingTreeTable() {
        super();
        setCellSelectionEnabled(true);
        setShowGrid(true, true);
        setTableHeader(new JTableHeader(getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int index = columnModel.getColumnIndexAtX(e.getPoint().x);
                return getTreeTableModel().getColumnName(index);
            }
        });
        
        setDefaultRenderer(Time.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value != null ? value.toString() : null);
            }
        });
        
        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(header.getFont().getStyle(), 10));
    }
    
    public void updateModel(int keyColumnsQuantity, java.util.List<ClientPropertyDraw> columnProperties, java.util.List<String> columnNames, 
                            java.util.List<Map<java.util.List<Object>, java.util.List<Object>>> values) {
        treeTableModel = new GroupingTreeTableModel(keyColumnsQuantity, columnProperties, columnNames, values);
        setTreeTableModel(treeTableModel);

        int additionalWidth = 19 * (values.size() - 1);
        getColumnModel().getColumn(0).setMinWidth(55 + additionalWidth);  //ширина колонки с деревом

        JTableHeader header = getTableHeader();
        header.addMouseListener(treeTableModel.new ColumnListener());  //для сортировки

        for (int i = 1; i < treeTableModel.getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            ClientPropertyDraw property = columnProperties.get(i - 1);
            column.setPreferredWidth(property != null ? property.getPreferredWidth(this) : 56);
            column.setMaxWidth(property != null ? property.getMaximumWidth(this) : 533900);
            column.setMinWidth(property != null ? property.getMinimumWidth(this) : 56);
        }    
    }
    
    public TreeTableNode getRoot() {
        return treeTableModel.getRoot();
    }

    public int getLastLevelRowCount() {
        return treeTableModel.getLastLevelRowCount();
    }

    public int getLevelCount() {
        return treeTableModel.getLevelCount();
    }
    
    public int getColumnCount() {
        return treeTableModel.getColumnCount();
    }
    
    public String getColumnName(int column) {
        return treeTableModel.getColumnName(column);
    }

    public Object getValueAt(DefaultMutableTreeTableNode node, int column) {
        return treeTableModel.getValueAt(node, column);
    }

    public List<Object> getRow(TreeTableNode node) {
        return treeTableModel.values.get(node);
    }

    public Set<DefaultMutableTreeTableNode> getNodes() {
        return treeTableModel.values.keySet();
    }

    //перегружаем методы для обхода бага с горизонтальным скроллбаром у JTable'а
    @Override
    public boolean getScrollableTracksViewportWidth() { 
        if (autoResizeMode != AUTO_RESIZE_OFF) {
            if (getParent() instanceof JViewport) {
                return (getParent().getWidth() > getPreferredSize().width);
            }
        }
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        if (getParent() instanceof JViewport) {
            if (getParent().getWidth() < super.getPreferredSize().width) {
                return getMinimumSize();
            }
        }
        return super.getPreferredSize();
    }

    public class GroupingTreeTableModel extends DefaultTreeTableModel {
        List<ClientPropertyDraw> columnProperties;
        java.util.List<String> columnNames;
        java.util.List<Map<java.util.List<Object>, java.util.List<Object>>> sources;
        Map<DefaultMutableTreeTableNode, java.util.List<Object>> values = new OrderedMap<DefaultMutableTreeTableNode, java.util.List<Object>>();
        int keyColumnsQuantity = 0;

        public GroupingTreeTableModel(int keyColumnsQuantity, List<ClientPropertyDraw> columnProperties, List<String> columnNames, List<Map<List<Object>, List<Object>>> values) {
            super();
            this.keyColumnsQuantity = keyColumnsQuantity;
            this.columnProperties = columnProperties;
            this.columnNames = columnNames;
            sources = values;

            DefaultMutableTreeTableNode rootNode = new DefaultMutableTreeTableNode("Root", true);
            if (!values.isEmpty()) {
                addNodes(rootNode, 0, null);
            }
            root = rootNode;
        }

        public int getLevelCount() {
            int count = 0;
            for (Map<java.util.List<Object>, java.util.List<Object>> level : sources) {
                if (level.isEmpty()) {
                    break;
                }
                count++;
            }
            return count;
        }

        public int getLastLevelRowCount() {
            int rowCount = 0;
            for (int i = sources.size() - 1; i >= 0; i--) {
                if (!sources.get(i).isEmpty()) {
                    rowCount += sources.get(i).size();
                    break;
                }
            }
            return rowCount;
        }

        private boolean containsAll(java.util.List<Object> parent, java.util.List<Object> child) {
            for (int i = 0; i < parent.size(); i++) {
                if (!parent.get(i).equals(child.get(i))) {
                    return false;
                }
            }
            return true;
        }

        private void addNodes(DefaultMutableTreeTableNode parentNode, int index, java.util.List<Object> parentKeys) {
            Map<java.util.List<Object>, java.util.List<Object>> map = sources.get(index);
            for (java.util.List<Object> keys : map.keySet()) {
                if (parentKeys == null || containsAll(parentKeys, keys)) {
                    java.util.List<Object> row = new ArrayList<Object>();
                    row.addAll(keys);
                    for (int i = 0; i < keyColumnsQuantity - keys.size(); i++) {
                        row.add(null);
                    }
                    row.addAll(map.get(keys));
                    DefaultMutableTreeTableNode node = new DefaultMutableTreeTableNode(index + 1, true);
                    parentNode.add(node);
                    values.put(node, row);
                    if (index < sources.size() - 1) {
                        addNodes(node, index + 1, keys);
                    }
                }
            }
        }

        @Override
        public Class getColumnClass(int column) {
            if (column != 0) {
                for (DefaultMutableTreeTableNode node : values.keySet()) {
                    java.util.List<Object> valueList = values.get(node);
                    Object value = valueList.get(column - 1);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return String.class;
            } else {
                return TreeTableModel.class;
            }
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column != 0 && super.isCellEditable(node, column);
        }

        public int getColumnCount() {
            return columnNames.size() + 1;
        }

        public String getColumnName(int index) {
            String name ;
            if (index == 0) {
                name = getString("form.queries.grouping.tree");
            } else {
                name = "<html>" + columnNames.get(index - 1);
                if (index == sortedCol) {
                    name += isSortAscending ? " ↑" : " ↓";
                }
                name += "</html>";
            }
            return name;
        }

        @Override
        public Object getValueAt(Object node, int column) {
            if (column == 0) {
                return node.toString();
            } else {
                java.util.List<Object> row = values.get(node);
                Object value = row.size() >= column ? row.get(column - 1) : null;
                if (value instanceof String) {
                    return value.toString().trim();
                } else if (columnProperties.get(column - 1) != null && columnProperties.get(column - 1).baseType instanceof ClientLogicalClass) {
                    return value != null && (Boolean) value;     
                }
                return value;
            }
        }

        protected boolean isSortAscending = true;
        protected int sortedCol = 0;

        class ColumnListener extends MouseAdapter {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel colModel = getColumnModel();
                int columnToSort = colModel.getColumnIndexAtX(e.getX());

                if (sortedCol == columnToSort)
                    isSortAscending = !isSortAscending;
                else {
                    isSortAscending = true;
                    sortedCol = columnToSort;
                }
                if (columnToSort < 1)
                    return;

                for (int i = 0; i < getColumnCount(); i++) {
                    TableColumn column = colModel.getColumn(i);
                    column.setHeaderValue(getColumnName(column.getModelIndex()));
                }
                getTableHeader().repaint();

                DefaultMutableTreeTableNode root = (DefaultMutableTreeTableNode) GroupingTreeTableModel.this.root;
                changeChildrenOrder(root);
                updateUI();
            }

            private void changeChildrenOrder(DefaultMutableTreeTableNode node) {
                ArrayList<DefaultMutableTreeTableNode> list = Collections.list((Enumeration<DefaultMutableTreeTableNode>) node.children());
                Collections.sort(list, new NodeComparator(isSortAscending, sortedCol));
                for (DefaultMutableTreeTableNode child : list) {
                    changeChildrenOrder(child);
                    node.remove(child);
                    node.add(child);
                }
            }
        }

        class NodeComparator implements Comparator {
            protected boolean isSortAsc;
            protected int columnIndex;

            public NodeComparator(boolean sortAsc, int columnIndex) {
                isSortAsc = sortAsc;
                this.columnIndex = columnIndex;
            }

            public int compare(Object o1, Object o2) {
                Comparable object1 = (Comparable) GroupingTreeTableModel.this.getValueAt(o1, columnIndex);
                Comparable object2 = (Comparable) GroupingTreeTableModel.this.getValueAt(o2, columnIndex);
                if (object1 == null || object2 == null) {
                    return 0;
                }
                int result = object1.compareTo(object2);
                if (!isSortAsc)
                    result = -result;
                return result;
            }
        }
    }
}
