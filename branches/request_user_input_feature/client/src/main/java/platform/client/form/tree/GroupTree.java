package platform.client.form.tree;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeNode;
import platform.client.tree.ExpandingTreeNode;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

//todo: delete later
@Deprecated()
public class GroupTree extends ClientTree {
    private final TreeGroupNode rootNode;
    private final ClientFormController form;

    private final List<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    boolean synchronize = false;
    private ClientGroupObjectValue currentPath;

    public GroupTree(ClientFormController iform) {
        super();

        form = iform;

        setToggleClickCount(-1);
        setDropMode(DropMode.ON_OR_INSERT);
        setDragEnabled(true);
        setCellRenderer(new GroupTreeCellRenderer());

        DefaultTreeModel model = new DefaultTreeModel(null);

        setModel(model);

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if (synchronize) {
                    return;
                }

                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                if (node.group != null) {
                    try {
                        form.expandGroupObject(node.group, node.key);
                    } catch (IOException e) {
                        throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.opening.treenode"));
                    }
                    if (hasOnlyExpandningNodeAsChild(node)) {
                        throw new ExpandVetoException(event);
                    }
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (synchronize) {
                    return;
                }

                if (event.getClickCount() == 2) {
                    int x = event.getX();
                    int y = event.getY();
                    final TreePath path = getPathForLocation(x, y);
                    if (path != null) {
                        TreeGroupNode node = (TreeGroupNode) path.getLastPathComponent();
                        if (node.group != null) {
                            try {
                                currentPath = node.key;
                                form.changeGroupObject(node.group, node.key);
                            } catch (IOException e) {
                                throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.selecting.node"));
                            }
                        }
                    }
                }
            }
        });

        rootNode = new TreeGroupNode();
        model.setRoot(rootNode);
    }

    private Map<ClientGroupObject, Set<TreeGroupNode>> groupNodes = new HashMap<ClientGroupObject, Set<TreeGroupNode>>();

    private Set<TreeGroupNode> getGroupNodes(ClientGroupObject group) { // так как mutable надо аккуратно пользоваться а то можно на concurrent нарваться
        if (group == null) {
            return Collections.singleton(rootNode);
        }

        Set<TreeGroupNode> nodes = groupNodes.get(group);
        if (nodes == null) {
            nodes = new HashSet<TreeGroupNode>();
            groupNodes.put(group, nodes);
        }
        return nodes;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parents) {

        // приводим переданную структуры в нормальную - child -> parent
        OrderedMap<ClientGroupObjectValue, ClientGroupObjectValue> parentTree = new OrderedMap<ClientGroupObjectValue, ClientGroupObjectValue>();
        for (int i = 0; i < keys.size(); i++) {
            ClientGroupObjectValue key = keys.get(i);

            ClientGroupObjectValue parentPath = new ClientGroupObjectValue(key); // значение для непосредственного родителя
            parentPath.removeAll(group.objects); // удаляем значение ключа самого groupObject, чтобы получить путь к нему из "родителей"
            parentPath.putAll(parents.get(i)); //рекурсивный случай - просто перезаписываем значения для ObjectInstance'ов
            parentTree.put(key, parentPath);
        }

        synchronize = true; // никаких запросов к серверу - идет синхронизация

        Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> childTree = BaseUtils.groupList(parentTree);
        for (TreeGroupNode groupNode : getGroupNodes(group.getUpTreeGroup())) {
            groupNode.synchronize(group, childTree);
        }

        synchronize = false;
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues) {
        values.put(property, ivalues);
    }

    public boolean addDrawProperty(ClientGroupObject group, ClientPropertyDraw property) {
        if (properties.indexOf(property) == -1) {
            List<ClientPropertyDraw> cells = form.getPropertyDraws();

            // конечно кривовато определять порядок по номеру в листе, но потом надо будет сделать по другому
            int ind = cells.indexOf(property), ins = 0;

            Iterator<ClientPropertyDraw> icp = properties.iterator();
            while (icp.hasNext() && cells.indexOf(icp.next()) < ind) {
                ins++;
            }

            properties.add(ins, property);
            return true;
        } else {
            return false;
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        properties.remove(property);
    }

    private boolean hasOnlyExpandningNodeAsChild(TreeGroupNode node) {
        return node.getChildCount() == 1 && node.getFirstChild() instanceof ExpandingTreeNode;
    }

    public void setCurrentObjects(ClientGroupObjectValue objects) {
        Enumeration nodes = rootNode.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            Object node = nodes.nextElement();
            if (node instanceof TreeGroupNode) {
                TreeGroupNode groupNode = (TreeGroupNode) node;
                if (groupNode.key.equals(objects)) {
                    synchronize = true;
                    currentPath = objects;
                    synchronize = false;
                    break;
                }
            }
        }
    }

    private class TreeGroupNode extends ClientTreeNode {
        public ClientGroupObject group;

        public ClientGroupObjectValue key;

        public TreeGroupNode() {
            this(null, new ClientGroupObjectValue());
        }

        public TreeGroupNode(ClientGroupObject group, ClientGroupObjectValue key) {
            this.group = group;
            this.key = key;
        }

        void synchronize(ClientGroupObject syncGroup, Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> tree) {
            final List<ClientGroupObjectValue> syncChilds = tree.containsKey(key)
                                                            ? tree.get(key)
                                                            : new ArrayList<ClientGroupObjectValue>();

            if (hasOnlyExpandningNodeAsChild(this)) {
                // убираем +
                remove(0);
            }

            List<TreeGroupNode> allChildren = new ArrayList<TreeGroupNode>();
            TreeGroupNode[] thisGroupChildren = new TreeGroupNode[syncChilds.size()];

            for (TreeGroupNode child : BaseUtils.<TreeGroupNode>copyTreeChildren(children)) {
                // бежим по node'ам
                if (child.group.equals(syncGroup)) {
                    int index = syncChilds.indexOf(child.key);
                    if (index == -1) {
                        remove(child);
                        getGroupNodes(syncGroup).remove(child);
                    } else { // помечаем что был, и рекурсивно синхронизируем child
                        thisGroupChildren[index] = child;
                        child.synchronize(syncGroup, tree);
                    }
                } else {
                    allChildren.add(child);
                }
            }

            for (int i = 0; i < syncChilds.size(); ++i) {
                if (thisGroupChildren[i] == null) {
                    TreeGroupNode newNode = new TreeGroupNode(syncGroup, syncChilds.get(i));
                    if (syncGroup.mayHaveChildren()) {
                        newNode.add(new ExpandingTreeNode());
                    }

                    thisGroupChildren[i] = newNode;
                    getGroupNodes(syncGroup).add(newNode);
                }
            }

            if (group == syncGroup) {
                allChildren.addAll(0, Arrays.asList(thisGroupChildren));
            } else {
                allChildren.addAll(Arrays.asList(thisGroupChildren));
            }

            removeAllChildren();

            for (TreeGroupNode child : allChildren) {
                add(child);
            }

            if (getChildCount() == 0) {
                if (group != null && group.mayHaveChildren()) {
                    add(new ExpandingTreeNode());
                }
            }
        }

        @Override
        public String toString() {
            String caption = "";
            for (ClientPropertyDraw property : properties) {
                Map<ClientGroupObjectValue, Object> propValues = values.get(property);
                if (propValues != null) {
                    String value = BaseUtils.toCaption(propValues.get(key));
                    if (value.length() != 0 && caption.length() != 0) {
                        value = " " + value;
                    }
                    caption += value;
                }
            }

            return caption;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            ClientTreeNode node = ClientTree.getNode(info);
            if (node instanceof TreeGroupNode && !node.isNodeDescendant(this)) {
                TreeGroupNode treeGroupNode = (TreeGroupNode) node;
                return treeGroupNode.group.getUpTreeGroup() == group
                        || (group == treeGroupNode.group && group.isRecursive);
            }
            return false;
        }

        @Override
        public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
            ClientTreeNode node = ClientTree.getNode(info);
            if (node instanceof TreeGroupNode && !node.isNodeDescendant(this)) {
                TreeGroupNode treeGroupNode = (TreeGroupNode) node;

                int index = ClientTree.getChildIndex(info);
                if (index == -1) {
                    index = getChildCount();
                }

                try {
                    form.moveGroupObject(group, key, treeGroupNode.group, treeGroupNode.key, index);
                } catch (IOException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("form.tree.error.moving.node.in.tree"));
                }

                return true;
            } else {
                return false;
            }
        }
    }

    class GroupTreeCellRenderer extends ClientTree.ClientTreeCellRenderer {
        private Color backgroundSelectionColor;
        private Color backgroundNonSelectionColor;

        public GroupTreeCellRenderer() {
            super();

            backgroundSelectionColor = getBackgroundSelectionColor();
            backgroundNonSelectionColor = getBackgroundNonSelectionColor();
        }

        @Override
        public Component getTreeCellRendererComponent(JTree iTree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            setBackgroundSelectionColor(backgroundSelectionColor);
            setBackgroundNonSelectionColor(backgroundNonSelectionColor);

            if (value instanceof TreeGroupNode) {
                TreeGroupNode groupNode = (TreeGroupNode) value;
                if (currentPath != null && currentPath.contains(groupNode.key)) {
                    setBackgroundSelectionColor(Color.YELLOW.darker().darker());
                    setBackgroundNonSelectionColor(Color.YELLOW);
                }
            }

            return super.getTreeCellRendererComponent(iTree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
}
