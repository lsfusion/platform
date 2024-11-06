package lsfusion.client.form.object.table.tree;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.tree.view.TreeGroupTable;
import lsfusion.client.form.property.ClientPropertyDraw;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public class GroupTreeTableModel extends DefaultTreeTableModel {
    private final Map<ClientGroupObject, Set<TreeGroupNode>> groupNodes = new HashMap<>();
    public final List<ClientPropertyDraw> properties = new ArrayList<>();
    public final List<ClientPropertyDraw> columnProperties = new ArrayList<>();
    public final Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<>();
    public final Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> readOnly = new HashMap<>();
    public final Map<ClientGroupObject, List<ClientPropertyDraw>> groupPropsMap = new HashMap<>();
    private Map<ClientGroupObjectValue, Object> rowBackground = new HashMap<>();
    private Map<ClientGroupObjectValue, Object> rowForeground = new HashMap<>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellBackgroundValues = new HashMap<>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> cellForegroundValues = new HashMap<>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> imageValues = new HashMap<>();

    private final ClientFormController form;
    private final boolean plainTreeMode;

    public boolean synchronize;

    public GroupTreeTableModel(ClientFormController form, boolean plainTreeMode) {
        super();
        this.form = form;
        this.plainTreeMode = plainTreeMode;
        root = new TreeGroupNode(this);
    }

    @Override
    public TreeGroupNode getRoot() {
        return (TreeGroupNode) super.getRoot();
    }

    @Override
    public int getColumnCount() {
        return plainTreeMode ? 1 : 1 + columnProperties.size();
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return ClientResourceBundle.getString("form.tree");
        }

        return getColumnProperty(column).getChangeCaption();
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if (column == 0) {
            return plainTreeMode ? node.toString() : "\u200b"; // zero width space. in GTK LAF tree controls are not shown when value is null or empty. 
        }

        if (node instanceof TreeGroupNode) {
            ClientPropertyDraw property = getProperty(node, column);

            if (property == null) {
                return null;
            }

            return values.get(property).get(((TreeGroupNode) node).key);
        }
        return node.toString();
    }

    public Color getBackgroundColor(Object node, int column) {
        if (column > 0 && node instanceof TreeGroupNode) {
            ClientPropertyDraw property = getProperty(node, column);
            if (property != null) {
                ClientGroupObjectValue key = ((TreeGroupNode) node).key;
                Color color = (Color) rowBackground.get(key);
                if (color == null) {
                    Map<ClientGroupObjectValue, Object> backgroundValues = cellBackgroundValues.get(property);
                    if (backgroundValues != null) {
                        color = (Color) backgroundValues.get(key);
                    }
                }
                return color;
            }
        }
        return null;
    }

    public Color getForegroundColor(Object node, int column) {
        if (column > 0 && node instanceof TreeGroupNode) {
            ClientPropertyDraw property = getProperty(node, column);
            if (property != null) {
                ClientGroupObjectValue key = ((TreeGroupNode) node).key;
                Color color = (Color) rowForeground.get(key);
                if (color == null) {
                    Map<ClientGroupObjectValue, Object> foregroundValues = cellForegroundValues.get(property);
                    if (foregroundValues != null) {
                        color = (Color) foregroundValues.get(key);
                    }
                }
                return color;
            }
        }
        return null;
    }


    public Image getImage(Object node, int column) {
        if (column > 0 && node instanceof TreeGroupNode) {
            ClientPropertyDraw property = getProperty(node, column);
            if (property != null) {
                ClientGroupObjectValue key = ((TreeGroupNode) node).key;
                Image image = null;
                Map<ClientGroupObjectValue, Object> imageValues = this.imageValues.get(property);
                if (imageValues != null) {
                    image = (Image) imageValues.get(key);
                }
                return image;
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, Object node, int column) {
        if (column != 0 && node instanceof TreeGroupNode) {
            ClientPropertyDraw property = getProperty(node, column);

            if (property != null) {
                values.get(property).put(((TreeGroupNode) node).key, value);
            }
        }
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        if (column != 0) {
            ClientPropertyDraw property = getProperty(node, column);
            if (property != null && !property.isReadOnly()) {
                Map<ClientGroupObjectValue, Object> propReadOnly = readOnly.get(property);
                return propReadOnly == null || propReadOnly.get(((TreeGroupNode) node).key) == null;
            }
        }
        return true;
    }

    public Object getPropertyValue(Object node, ClientPropertyDraw property) {
        if (node instanceof TreeGroupNode) {
            Map<ClientGroupObjectValue, Object> properties = values.get(property);
            if (properties != null) {
                return properties.get(((TreeGroupNode) node).key);
            }
        }
        return null;
    }

    public boolean isCellFocusable(Object node, int column) {
        if (column == 0) {
            return true;
        }
        ClientPropertyDraw property = getProperty(node, column);
        if (property == null) {
            return false;
        }

        Boolean focusable = property.focusable;
        return focusable == null || focusable;
    }

    public ClientPropertyDraw getColumnProperty(int col) {
        return col > 0
               ? columnProperties.get(col - 1)
               : null;
    }
    
    public List<ClientPropertyDraw> getProperties(ClientGroupObject group) {
        return groupPropsMap.get(group);
    }

    public ClientPropertyDraw getProperty(Object node, int column) {
        if (node instanceof TreeGroupNode) {
            List<ClientPropertyDraw> groupProperties = groupPropsMap.get(((TreeGroupNode) node).group);
            if (groupProperties == null || column <= 0 || column > groupProperties.size()) {
                return null;
            }

            return groupProperties.get(column - 1);
        }
        return null;
    }

    public Set<TreeGroupNode> getGroupNodes(ClientGroupObject group) { // так как mutable надо аккуратно пользоваться а то можно на concurrent нарваться
        if (group == null) {
            return Collections.singleton(getRoot());
        }

        Set<TreeGroupNode> nodes = groupNodes.get(group);
        if (nodes == null) {
            nodes = new HashSet<>();
            groupNodes.put(group, nodes);
        }
        return nodes;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents, Map<ClientGroupObjectValue, Integer> expandables) {
        // приводим переданную структуру в нормальную - child -> parent
        OrderedMap<ClientGroupObjectValue, ClientGroupObjectValue> parentTree = new OrderedMap<>();
        for (int i = 0; i < keys.size(); i++) {
            ClientGroupObjectValue key = keys.get(i);

            // 1 param - значение для непосредственного родителя
            // 2 param - удаляем значение ключа самого groupObject, чтобы получить путь к нему из "родителей"
            // 3 param - рекурсивный случай - просто перезаписываем значения для ObjectInstance'ов
            parentTree.put(key, new ClientGroupObjectValue(key, group.objects, parents.get(i)));
        }

        Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> childTree = BaseUtils.groupList(parentTree);
        synchronize = true;
        for (TreeGroupNode groupNode : getGroupNodes(group.getUpTreeGroup())) {
            synchronize(groupNode, group, childTree, expandables);
        }
        synchronize = false;
    }

    void synchronize(TreeGroupNode parent,
                     ClientGroupObject syncGroup,
                     Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> tree,
                     Map<ClientGroupObjectValue, Integer> expandables) {
        List<ClientGroupObjectValue> syncChilds = tree.get(parent.key);
        if (syncChilds == null) {
            syncChilds = new ArrayList<>();
        }

        if (parent.hasOnlyExpandningNodeAsChild()) {
            // убираем +
            parent.removeFirstChild();
        }

        List<TreeGroupNode> allChildren = new ArrayList<>();
        TreeGroupNode[] thisGroupChildren = new TreeGroupNode[syncChilds.size()];

        for (TreeGroupNode child : BaseUtils.<TreeGroupNode>copyTreeChildren(parent.getChildren())) {
            // бежим по node'ам
            if (child.group.equals(syncGroup)) {
                int index = syncChilds.indexOf(child.key);
                if (index == -1) {
                    parent.removeChild(child);
                    removeFromGroupNodes(syncGroup, child);
                } else {
                    // помечаем что был
                    thisGroupChildren[index] = child;
                }
            } else {
                allChildren.add(child);
            }
        }

        for (int i = 0; i < syncChilds.size(); ++i) {
            ClientGroupObjectValue key = syncChilds.get(i);
            TreeGroupNode child = thisGroupChildren[i];

            if (child == null) {
                thisGroupChildren[i] = child = new TreeGroupNode(this, syncGroup, key);

                parent.addChild(child);

                getGroupNodes(syncGroup).add(child);
            }

            boolean expandable = false;
            if (syncGroup.mayHaveChildren()) {
                Integer e = expandables.get(key);
                expandable = e == null || e > 0;
            }
            child.setExpandable(expandable);

            synchronize(child, syncGroup, tree, expandables);
        }

        if (parent.group == syncGroup) {
            allChildren.addAll(0, Arrays.asList(thisGroupChildren));
        } else {
            allChildren.addAll(Arrays.asList(thisGroupChildren));
        }

        parent.removeAllChildren();

        for (TreeGroupNode child : allChildren) {
            parent.addChild(child);
        }

        if (parent.getChildCount() == 0 && parent.isExpandable()) {
            parent.addChild(new ExpandingTreeTableNode());
        }
    }

    private void removeFromGroupNodes(ClientGroupObject syncGroup, TreeGroupNode node) {
        getGroupNodes(syncGroup).remove(node);

        for (MutableTreeTableNode child : Collections.list(node.children())) {
            if (child instanceof TreeGroupNode) {
                TreeGroupNode groupNode = (TreeGroupNode) child;
                removeFromGroupNodes(groupNode.group, groupNode);
            }
        }
    }

    public void updateReadOnlyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues) {
        BaseUtils.putUpdate(readOnly, property, ivalues, false);
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues, boolean update) {
        BaseUtils.putUpdate(values, property, ivalues, update);
    }

    public void updateCellBackgroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellBackgroundValues) {
        BaseUtils.putUpdate(this.cellBackgroundValues, property, cellBackgroundValues, false);
    }

    public void updateCellForegroundValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellForegroundValues) {
        BaseUtils.putUpdate(this.cellForegroundValues, property, cellForegroundValues, false);
    }

    public void updateImageValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> imageValues) {
        BaseUtils.putUpdate(this.imageValues, property, imageValues, false);
    }

    public void updateRowBackgroundValues(Map<ClientGroupObjectValue, Object> rowBackground) {
        this.rowBackground = rowBackground;
    }

    public void updateRowForegroundValues(Map<ClientGroupObjectValue, Object> rowForeground) {
        this.rowForeground = rowForeground;
    }

    public int addDrawProperty(ClientFormController form, ClientGroupObject group, ClientPropertyDraw property) {
        if (properties.indexOf(property) == -1 && !property.hideOrRemove()) {
            int ins = BaseUtils.relativePosition(property, form.getPropertyDraws(), properties);
            properties.add(ins, property);

            List<ClientPropertyDraw> groupProperties = groupPropsMap.get(group);
            if (groupProperties == null) {
                groupProperties = new ArrayList<>();
                groupPropsMap.put(group, groupProperties);
            }
            int gins = BaseUtils.relativePosition(property, properties, groupProperties);
            groupProperties.add(gins, property);

            if (group.isLastGroupInTree()) {
                int tins = BaseUtils.relativePosition(property, properties, columnProperties);
                columnProperties.add(tins, property);
                return tins + 1;
            }
        }
        return -1;
    }

    public int removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        properties.remove(property);
        if (groupPropsMap.containsKey(group))
            groupPropsMap.get(group).remove(property);

        int ind = columnProperties.indexOf(property);
        if (ind != -1) {
            columnProperties.remove(property);
        }
        return ind + 1;
    }

    public void firePathChanged(TreePath nodePath) {
        modelSupport.firePathChanged(nodePath);
    }

    public int getPropertyColumnIndex(ClientPropertyDraw property) {
        return columnProperties.indexOf(property) + 1;
    }

    public int getColumnIndexAtX(int xPosition, TreeGroupTable treeGroupTable) {
        if (xPosition < 0)
            return -1;

        for(int column = 0; column < getColumnCount(); column++) {
            xPosition = xPosition - treeGroupTable.getColumn(column).getWidth();
            if (xPosition < 0)
                return getColumnProperty(column) != null ? column : -1;
        }
        return -1;
    }
}
