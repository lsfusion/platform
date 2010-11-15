package platform.client.form;

import platform.base.OrderedMap;
import platform.base.BaseUtils;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeNode;
import platform.client.tree.ExpandingTreeNode;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GroupTree extends ClientTree {
    private final TreeGroupNode rootNode;
    private final ClientFormController form;

    private final List<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    boolean synchronize = false;

    public GroupTree(ClientFormController iform) {
        super();

//        setPreferredSize(new Dimension(50, 50));
//        setMinimumSize(new Dimension(300, 100));
//        setMaximumSize(new Dimension(500, 500));

        form = iform;

        setToggleClickCount(-1);

        DefaultTreeModel model = new DefaultTreeModel(null);

        setModel(model);

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if(synchronize) return;

                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();

                try {
                    form.expandGroupObject(node.group, node.key);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при открытии узла дерева.");
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                try {
                    form.changeGroupObject(node.group, node.key);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при выборе узла.");
                }
            }
        });

        rootNode = new TreeGroupNode();
        model.setRoot(rootNode);
    }

    private Map<ClientGroupObject, Set<TreeGroupNode>> groupNodes = new HashMap<ClientGroupObject, Set<TreeGroupNode>>();
    private Set<TreeGroupNode> getGroupNodes(ClientGroupObject group) { // так как mutable надо аккуратно пользоваться а то можно на concurrent нарваться
        if(group==null) return Collections.singleton(rootNode);

        Set<TreeGroupNode> nodes = groupNodes.get(group);
        if(nodes==null) {
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
            parentPath.removeAll(group); // удаляем значение ключа самого groupObject, чтобы получить путь к нему из "родителей"
            parentPath.putAll(parents.get(i)); //рекурсивный случай - просто перезаписываем значения для ObjectInstance'ов
            parentTree.put(key, parentPath);
        }

        synchronize = true; // никаких запросов к серверу идет синхронизация

        Map<ClientGroupObjectValue, List<ClientGroupObjectValue>> childTree = BaseUtils.groupList(parentTree);
        for(TreeGroupNode groupNode : getGroupNodes(group.getUpTreeGroup()))
            groupNode.synchronize(group, childTree);

        synchronize = false;
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues) {
        values.put(property, ivalues);
    }

    public boolean addDrawProperty(ClientPropertyDraw property) {
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
            List<ClientGroupObjectValue> syncChilds = tree.get(key);
            if(syncChilds==null) syncChilds = new ArrayList<ClientGroupObjectValue>();

            if (getChildCount() == 1 && getFirstChild() instanceof ExpandingTreeNode) // убираем +
                remove(0);

            Set<ClientGroupObjectValue> existChilds = new HashSet<ClientGroupObjectValue>();
            for(TreeGroupNode child : BaseUtils.<TreeGroupNode>copyTreeChildren(children)) // бежим по node'ам
                if(child.group.equals(syncGroup))
                    if(!syncChilds.contains(child.key)) {
                        remove(child); getGroupNodes(syncGroup).remove(child);
                    } else { // помечаем что был, и рекурсивно синхронизируем child
                        existChilds.add(child.key);
                        child.synchronize(syncGroup, tree);
                    }

            for(ClientGroupObjectValue newChild : BaseUtils.filterNotList(syncChilds, existChilds)) { // добавляем те которых не было
                TreeGroupNode addNode = new TreeGroupNode(syncGroup, newChild);
                add(addNode); getGroupNodes(syncGroup).add(addNode);
                addNode.synchronize(syncGroup, tree);
            }

            if(getChildCount()==0) {// пока не знаем нету внизу child'ов или просто не expand'ут, добавляем '+'
                add(new ExpandingTreeNode());
                collapsePath(GroupTree.this.getPathToRoot(this));
            } else
                expandPath(GroupTree.this.getPathToRoot(this));
        }

        @Override
        public String toString() {
            String caption = "";
            for (ClientPropertyDraw property : properties) {
                Map<ClientGroupObjectValue, Object> propValues = values.get(property);
                if (propValues != null)
                    caption += BaseUtils.nullToString(propValues.get(key));
            }

            return caption;
        }
    }
}
