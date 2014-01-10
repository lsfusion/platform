package lsfusion.client.form.queries;

import lsfusion.base.OrderedMap;
import lsfusion.client.Main;
import lsfusion.client.form.renderer.ImagePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientDateClass;
import lsfusion.client.logics.classes.ClientDateTimeClass;
import lsfusion.client.logics.classes.ClientImageClass;
import lsfusion.client.logics.classes.ClientLogicalClass;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.*;

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
    private final int DEFAULT_ROW_HEIGHT = 16;
    
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
        
        setDefaultRenderer(Image.class, new ImageCellRenderer());
        
        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(header.getFont().getStyle(), 10));
        header.addMouseListener(new ColumnListener());  //для сортировки

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 ) {
                    int columnClicked = columnAtPoint(e.getPoint());
                    ClientPropertyDraw columnProperty = treeTableModel.getColumnProperty(columnClicked);
                    if (columnProperty != null && columnProperty.baseType instanceof ClientImageClass) {
                        ImagePropertyRenderer.expandImage((byte[]) getValueAt(rowAtPoint(e.getPoint()), columnClicked));
                    }
                }
            }
        });
    }
    
    public void updateModel(int keyColumnsQuantity, java.util.List<ClientPropertyDraw> columnProperties, java.util.List<String> columnNames, 
                            java.util.List<Map<java.util.List<Object>, java.util.List<Object>>> values) {
        treeTableModel = new GroupingTreeTableModel(keyColumnsQuantity, columnProperties, columnNames, values);
        setTreeTableModel(treeTableModel);

        int treeColumnWidth =  55 + 19 * (values.size() - 1); //ширина колонки с деревом
        TableColumn treeColumn = getColumnModel().getColumn(0);
        treeColumn.setMinWidth(treeColumnWidth);  
        treeColumn.setPreferredWidth(treeColumnWidth);
        treeColumn.setMaxWidth(3000);

        boolean needToExpandRows = false;
        for (int i = 1; i < treeTableModel.getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            ClientPropertyDraw property = columnProperties.get(i - 1);
            column.setPreferredWidth(property != null ? property.getPreferredWidth(this) : 56);
            column.setMaxWidth(property != null ? property.getMaximumWidth(this) : 3000);
            column.setMinWidth(property != null ? property.getMinimumWidth(this) : 56);
            
            if (property != null && property.baseType instanceof ClientImageClass) {
                needToExpandRows = true;
            }
        }    
        if (needToExpandRows) { // специально для картинок увеличиваем высоту рядов
            setRowHeight(48);
        } else {
            setRowHeight(DEFAULT_ROW_HEIGHT);
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
    
    public String getColumnName(int column) {
        return treeTableModel.getColumnName(column);
    }

    public Object getValueAt(SortableTreeTableNode node, int column) {
        return treeTableModel.getValueAt(node, column);
    }

    public List<Object> getRow(TreeTableNode node) {
        return treeTableModel.values.get(node);
    }

    public Set<SortableTreeTableNode> getNodes() {
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
        Map<SortableTreeTableNode, java.util.List<Object>> values = new OrderedMap<SortableTreeTableNode, java.util.List<Object>>();
        int keyColumnsQuantity = 0;

        public boolean isSortAscending = true;
        public int sortedCol = 0;

        public GroupingTreeTableModel(int keyColumnsQuantity, List<ClientPropertyDraw> columnProperties, List<String> columnNames, List<Map<List<Object>, List<Object>>> values) {
            super();
            this.keyColumnsQuantity = keyColumnsQuantity;
            this.columnProperties = columnProperties;
            this.columnNames = columnNames;
            sources = values;

            removeAll();

            root = new SortableTreeTableNode("Root", true);
            if (!values.isEmpty()) {
                addNodes((SortableTreeTableNode) root, 0, null);
            }
        }
        
        public ClientPropertyDraw getColumnProperty(int column) {
            if (column > 0) {
                return columnProperties.get(column - 1);
            }
            return null;
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

        private void addNodes(SortableTreeTableNode parentNode, int index, java.util.List<Object> parentKeys) {
            Map<java.util.List<Object>, java.util.List<Object>> map = sources.get(index);
            for (java.util.List<Object> keys : map.keySet()) {
                if (parentKeys == null || containsAll(parentKeys, keys)) {
                    java.util.List<Object> row = new ArrayList<Object>();
                    row.addAll(keys);
                    for (int i = 0; i < keyColumnsQuantity - keys.size(); i++) {
                        row.add(null);
                    }
                    row.addAll(map.get(keys));
                    SortableTreeTableNode node = new SortableTreeTableNode(index + 1, true);
                    parentNode.add(node);
                    
                    List<Object> convertedRow = new ArrayList<Object>();
                    for (Object value : row) {
                        Object convertedValue = value;

                        if (value instanceof String) {
                            convertedValue = value.toString().trim();
                        } else {
                            ClientPropertyDraw columnProperty = columnProperties.get(row.indexOf(value));
                            if (columnProperty != null) {
                                if (columnProperty.baseType instanceof ClientDateClass) {
                                    convertedValue = value == null ? null : Main.dateFormat.format(value);
                                } else if (columnProperty.baseType instanceof ClientDateTimeClass) {
                                    convertedValue = value == null ? null : Main.dateTimeFormat.format(value);
                                } else if (columnProperty.baseType instanceof ClientLogicalClass) {
                                    convertedValue = value != null && (Boolean) value;
                                }
                            }
                        }
                        
                        convertedRow.add(convertedValue);
                    }
                    
                    values.put(node, convertedRow);
                    if (index < sources.size() - 1) {
                        addNodes(node, index + 1, keys);
                    }
                }
            }
        }

        @Override
        public Class getColumnClass(int column) {
            if (column != 0) {
                ClientPropertyDraw columnProperty = getColumnProperty(column);
                if (columnProperty != null) {
                    if (columnProperty.baseType instanceof ClientLogicalClass) {
                        return Boolean.class;
                    } else if (columnProperty.baseType instanceof ClientImageClass) {
                        return Image.class;
                    }
                }
                
                for (SortableTreeTableNode node : values.keySet()) {
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
                return row.size() >= column ? row.get(column - 1) : null;
            }
        }
        
        public void changeOrder(int columnIndex) {
            if (sortedCol == columnIndex)
                isSortAscending = !isSortAscending;
            else {
                isSortAscending = true;
                sortedCol = columnIndex;
            }
            if (columnIndex < 1)
                return;

            TableColumnModel colModel = getColumnModel();
            for (int i = 0; i < getColumnCount(); i++) {
                TableColumn column = colModel.getColumn(i);
                column.setHeaderValue(getColumnName(column.getModelIndex()));
            }
            getTableHeader().repaint();
            
            changeChildrenOrder((SortableTreeTableNode) root);
            updateUI();
        }

        private void changeChildrenOrder(SortableTreeTableNode node) {
            node.sortChildren(new NodeComparator());
            
            Enumeration<? extends MutableTreeTableNode> children = node.children();
            while (children.hasMoreElements()) {
                SortableTreeTableNode child = (SortableTreeTableNode) children.nextElement();
                changeChildrenOrder(child);
            }
        }

        class NodeComparator implements Comparator<TreeTableNode> {
            public int compare(TreeTableNode o1, TreeTableNode o2) {
                int result;
                Object value1 = GroupingTreeTableModel.this.getValueAt(o1, sortedCol);
                Object value2 = GroupingTreeTableModel.this.getValueAt(o2, sortedCol);
                
                if (value1 == null) {
                    result = value2 == null ? 0 : -1;
                } else {
                    if (value2 == null) {
                        result = 1;
                    } else {
                        if (value1 instanceof Comparable) {
                            result = value2 instanceof Comparable ? ((Comparable) value1).compareTo(value2) : 1;    
                        } else {
                            result = value2 instanceof Comparable ? -1 : 0;
                        }
                    }
                }

                return isSortAscending ? result : -result;
            }
        }
    }
    
    public class SortableTreeTableNode extends DefaultMutableTreeTableNode {
        public SortableTreeTableNode(String root, boolean b) {
            super(root, b);
        }

        public SortableTreeTableNode(int i, boolean b) {
            super(i, b);
        }

        protected void sortChildren(Comparator<TreeTableNode> comparator) {
            Collections.sort(children, comparator);    
        } 
    }

    private class ColumnListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            treeTableModel.changeOrder(getColumnModel().getColumnIndexAtX(e.getX()));
        }
    }
    
    private class ImageCellRenderer extends DefaultTableCellRenderer {
        private int column;
        
        public ImageCellRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
        }
        
        @Override
        protected void setValue(Object value) {
            if (value != null) {
                ImageIcon icon = new ImageIcon((byte[]) value);
                Dimension scaled = ImagePropertyRenderer.scaleIcon(icon, getColumn(column).getWidth(), getRowHeight());
                icon.setImage(icon.getImage().getScaledInstance(scaled.width, scaled.height, Image.SCALE_SMOOTH));
                setIcon(icon);
            } else {
                setIcon(null);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.column = column;
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
