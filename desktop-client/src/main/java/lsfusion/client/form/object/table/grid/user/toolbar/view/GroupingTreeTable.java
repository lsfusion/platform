package lsfusion.client.form.object.table.grid.user.toolbar.view;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.file.AppFileDataImage;
import lsfusion.base.file.RawFileData;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.data.*;
import lsfusion.client.classes.data.link.ClientImageLinkClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.ImagePropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.ImageLinkPropertyRenderer;
import lsfusion.client.view.MainFrame;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.*;

import static lsfusion.base.DateConverter.instantToSqlTimestamp;
import static lsfusion.base.DateConverter.localDateTimeToSqlTimestamp;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;
import static lsfusion.client.ClientResourceBundle.getString;

public class GroupingTreeTable extends JXTreeTable {
    private final int DEFAULT_ROW_HEIGHT = SwingDefaults.getValueHeight();
    private final int EXPANDED_ROW_HEIGHT = DEFAULT_ROW_HEIGHT * 3;
    private final int MIN_COLUMN_WIDTH = 56;
    private final int MAX_COLUMN_WIDTH = 3000;
    
    GroupingTreeTableModel treeTableModel;
    
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
        
        setDefaultRenderer(LocalTime.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value != null ? MainFrame.tFormats.time.format(localTimeToSqlTime((LocalTime) value)) : null);
            }
        });

        setDefaultRenderer(LocalDateTime.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value != null ? MainFrame.tFormats.dateTime.format(localDateTimeToSqlTimestamp((LocalDateTime) value)) : null);
            }
        });

        setDefaultRenderer(Instant.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value != null ? MainFrame.tFormats.dateTime.format(instantToSqlTimestamp((Instant) value)) : null);
            }
        });
        
        setDefaultRenderer(Image.class, new ImageCellRenderer());
        
        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(header.getFont().getStyle(), MainFrame.getUIFontSize(10)));
        header.addMouseListener(new ColumnListener());  //для сортировки

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 ) {
                    int columnClicked = columnAtPoint(e.getPoint());
                    ClientPropertyDraw columnProperty = treeTableModel.getColumnProperty(columnClicked);
                    if (columnProperty != null && (columnProperty.baseType instanceof ClientImageClass || columnProperty.baseType instanceof ClientImageLinkClass)) {
                        ImagePropertyRenderer.expandImage((AppFileDataImage) getValueAt(rowAtPoint(e.getPoint()), columnClicked));
                    }
                }
            }
        });
    }
    
    public void updateModel(int keyColumnsQuantity, java.util.List<ClientPropertyDraw> columnProperties, java.util.List<String> columnNames, 
                            java.util.List<Map<java.util.List<Object>, java.util.List<Object>>> values) {
        treeTableModel = new GroupingTreeTableModel(keyColumnsQuantity, columnProperties, columnNames, values);
        setTreeTableModel(treeTableModel);
        setDefaultOrder(columnProperties);                                
        int treeColumnWidth =  55 + 19 * (values.size() - 1); //ширина колонки с деревом
        TableColumn treeColumn = getColumnModel().getColumn(0);
        treeColumn.setMinWidth(treeColumnWidth);  
        treeColumn.setPreferredWidth(treeColumnWidth);
        treeColumn.setMaxWidth(MAX_COLUMN_WIDTH);

        boolean needToExpandRows = false;
        for (int i = 1; i < treeTableModel.getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            ClientPropertyDraw property = columnProperties.get(i - 1);
            int columnMargin = getColumnModel().getColumnMargin();
            int valueWidth = property != null ? property.getValueWidth(this) + columnMargin : MIN_COLUMN_WIDTH;
            column.setPreferredWidth(valueWidth);
            column.setMinWidth(valueWidth);
            
            if (property != null && (property.baseType instanceof ClientImageClass || property.baseType instanceof ClientImageLinkClass)) {
                needToExpandRows = true;
            }
        }
        // специально для картинок увеличиваем высоту рядов
        setRowHeight(MainFrame.getIntUISize(needToExpandRows ? EXPANDED_ROW_HEIGHT : DEFAULT_ROW_HEIGHT) + getRowMargin());
    }
    
    private void setDefaultOrder(java.util.List<ClientPropertyDraw> columnProperties) {
        for (int i = 0; i < columnProperties.size(); i++) {
            ClientPropertyDraw columnProperty = columnProperties.get(i);
            if (columnProperty != null) {
                if (columnProperty.baseType instanceof ClientDateClass || columnProperty.baseType instanceof ClientTimeClass
                    || columnProperty.baseType instanceof ClientDateTimeClass) {
                    treeTableModel.addOrder(i + 1);
                }
            }
        }
    }
    
    public TreeTableNode getRoot() {
        return treeTableModel.getRoot();
    }

    public int getLastLevelRowCount() {
        return treeTableModel.lastLevelRowCount;
    }

    public int getLevelCount() {
        return treeTableModel.levelCount;
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
        Map<SortableTreeTableNode, java.util.List<Object>> values = new OrderedMap<>();
        int keyColumnsQuantity;
        int levelCount;
        int lastLevelRowCount;

        public LinkedHashMap<Integer, Boolean> sortedColumns = new LinkedHashMap<>(); //column - ascending
        
        public GroupingTreeTableModel(int keyColumnsQuantity, List<ClientPropertyDraw> columnProperties, List<String> columnNames, List<Map<List<Object>, List<Object>>> values) {
            super();
            this.keyColumnsQuantity = keyColumnsQuantity;
            this.columnProperties = columnProperties;
            this.columnNames = columnNames;
            levelCount = getLevelCount(values);
            lastLevelRowCount = getLastLevelRowCount(values);

            removeAll();

            root = new SortableTreeTableNode("Root", true);
            if (!values.isEmpty()) {
                addNodes(values, (SortableTreeTableNode) root, 0, null);
            }
        }
        
        public ClientPropertyDraw getColumnProperty(int column) {
            if (column > 0) {
                return columnProperties.get(column - 1);
            }
            return null;
        }

        private int getLevelCount(java.util.List<Map<java.util.List<Object>, java.util.List<Object>>> sources) {
            int count = 0;
            for (Map<java.util.List<Object>, java.util.List<Object>> level : sources) {
                if (level.isEmpty()) {
                    break;
                }
                count++;
            }
            return count;
        }

        private int getLastLevelRowCount(java.util.List<Map<java.util.List<Object>, java.util.List<Object>>> sources) {
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
                if (!BaseUtils.nullEquals(parent.get(i), child.get(i))) {
                    return false;
                }
            }
            return true;
        }

        private void addNodes(List<Map<List<Object>, List<Object>>> sources, SortableTreeTableNode parentNode, int index, List<Object> parentKeys) {
            Map<java.util.List<Object>, java.util.List<Object>> map = sources.get(index);
            for (java.util.List<Object> keys : map.keySet()) {
                if (parentKeys == null || containsAll(parentKeys, keys)) {
                    List<Object> row = new ArrayList<>(keys);
                    for (int i = 0; i < keyColumnsQuantity - keys.size(); i++) {
                        row.add(null);
                    }
                    row.addAll(map.get(keys));
                    SortableTreeTableNode node = new SortableTreeTableNode(index + 1, true);
                    parentNode.add(node);

                    List<Object> convertedRow = new ArrayList<>();
                    for (Object value : row) {
                        Object convertedValue = value;

                        ClientPropertyDraw columnProperty = columnProperties.get(row.indexOf(value));
                        if (columnProperty != null) {
                            if (columnProperty.baseType instanceof ClientLogicalClass) {
                                convertedValue = value != null && (Boolean) value;
                            } else if (columnProperty.baseType instanceof ClientImageLinkClass && value instanceof String) {
                                convertedValue = ImageLinkPropertyRenderer.readImage(columnProperty, (String) value);
                            }
                        }
                        if (convertedValue instanceof String) {
                            convertedValue = ((String) convertedValue).trim();
                        }

                        convertedRow.add(convertedValue);
                    }
                    
                    this.values.put(node, convertedRow);
                    if (index < sources.size() - 1) {
                        addNodes(sources, node, index + 1, keys);
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
                    } else if (columnProperty.baseType instanceof ClientImageClass || columnProperty.baseType instanceof ClientImageLinkClass) {
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
                if (sortedColumns.containsKey(index)) {
                    name += sortedColumns.get(index) ? " ↑" : " ↓";
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
        
        public void addOrder(int columnIndex) {
            if (sortedColumns.containsKey(columnIndex))
                sortedColumns.put(columnIndex, !sortedColumns.get(columnIndex));
            else {
                sortedColumns.put(columnIndex, true);
            }
            if (columnIndex < 1)
                return;

            refreshColumnsAndSorting();
        }
        
        public void removeOrder(int columnIndex) {
            if (sortedColumns.remove(columnIndex) != null) {
                refreshColumnsAndSorting();
            }
        } 
        
        private void refreshColumnsAndSorting() {
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
                int result = 0;
                for (Map.Entry<Integer, Boolean> entry : sortedColumns.entrySet()) {
                    Object value1 = GroupingTreeTableModel.this.getValueAt(o1, entry.getKey());
                    Object value2 = GroupingTreeTableModel.this.getValueAt(o2, entry.getKey());
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
                    result = entry.getValue() ? result : -result;
                    if (result != 0)
                        break;
                }
                return result;
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
            int columnIndex = getColumnModel().getColumnIndexAtX(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) {
                treeTableModel.addOrder(columnIndex);
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                treeTableModel.removeOrder(columnIndex);
            }
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
                ImageIcon icon = null;
                if (value instanceof RawFileData) {
                    icon = ((RawFileData) value).getImageIcon();
                }
                if (icon != null) {
                    Dimension scaled = ImagePropertyRenderer.getIconScale(icon, getColumn(column).getWidth(), getRowHeight());
                    if (scaled != null)
                        icon.setImage(icon.getImage().getScaledInstance(scaled.width, scaled.height, Image.SCALE_SMOOTH));
                }
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
