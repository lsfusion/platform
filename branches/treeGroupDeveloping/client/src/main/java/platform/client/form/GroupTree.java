package platform.client.form;

import platform.base.OrderedMap;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientObject;
import platform.client.logics.ClientPropertyDraw;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeNode;
import platform.client.tree.ExpandingTreeNode;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GroupTree extends ClientTree {
    private TreeGroupNode rootNode;
    private DefaultTreeModel model;
    private final TreeGroupController treeGroupController;
    private final ClientFormController form;

    private final List<ClientPropertyDraw> properties = new ArrayList<ClientPropertyDraw>();
    private Map<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>> values = new HashMap<ClientPropertyDraw, Map<ClientGroupObjectValue, Object>>();

    public GroupTree(TreeGroupController itreeGroupController, ClientFormController iform) {
        super();

//        setPreferredSize(new Dimension(50, 50));
        setMinimumSize(new Dimension(300, 100));
        setMaximumSize(new Dimension(500, 500));

        treeGroupController = itreeGroupController;

        form = iform;

        setToggleClickCount(-1);

        model = new DefaultTreeModel(null);

        setModel(model);

        addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreeGroupNode node = (TreeGroupNode) event.getPath().getLastPathComponent();
                cleanNode(node);

                try {
                    form.expandTreeNode(treeGroupController.treeGroup, node.group, node.compositeKey);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при открытии узла дерева.");
                }
            }

            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        createRootNode();
    }

    private void cleanNode(TreeGroupNode node) {
        if (node.getChildCount() > 0 && node.getFirstChild() instanceof ExpandingTreeNode) {
            node.remove(0);
        }
    }

    public void createRootNode() {
        rootNode = new TreeGroupNode();

        model.setRoot(rootNode);

        rootNode.add(new ExpandingTreeNode());
//        expandPath(new TreePath(rootNode));
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> parentPaths) {
        Map<TreeGroupNode, List<ClientGroupObjectValue>> updatingKeys = new OrderedMap<TreeGroupNode, List<ClientGroupObjectValue>>();

        NodeEnumerator enumerator = new NodeEnumerator();
        while (enumerator.hasNext()) {
            TreeGroupNode node = enumerator.nextNode();
            for (int i = 0; i < parentPaths.size(); i++) {
                ClientGroupObjectValue key = keys.get(i);
                ClientGroupObjectValue parentKey = parentPaths.get(i);

                ClientGroupObjectValue keyTreePath = new ClientGroupObjectValue(key);
                if (!parentKey.isEmpty()) {
                    //рекурсивный случай - просто перезаписываем значения для ObjectInstance'ов
                    keyTreePath.putAll(parentKey);
                } else {
                    //удаляем значение ключа самого groupObject, чтобы получить путь к нему из "родителей"
                    for (ClientObject object : group) {
                        keyTreePath.remove(object);
                    }
                }

                //нужно просто проверить, что равны по содержимому
                //todo: проверить, одинаковый ли здесь порядок и можно ли использовать equals
                if (node.compositeKey.contentEquals(keyTreePath)) {
                    List<ClientGroupObjectValue> updatingNodesKeys = updatingKeys.get(node);
                    if (updatingNodesKeys == null) {
                        updatingNodesKeys = new ArrayList<ClientGroupObjectValue>();
                        updatingKeys.put(node, updatingNodesKeys);
                    }
                    updatingNodesKeys.add(key);
                }
            }
        }

        for (Map.Entry<TreeGroupNode, List<ClientGroupObjectValue>> entry : updatingKeys.entrySet()) {
            TreeGroupNode node = entry.getKey();
            cleanNode(node);

            List<ClientGroupObjectValue> nodeKeys = entry.getValue();

            //удаление и апдейт узлов
            int i = 0;
            while (i < node.getChildCount()) {
                TreeGroupNode child = (TreeGroupNode) node.getChildAt(i);
                //todo: нужно будет ещё поддержать порядок
                if (nodeKeys.contains(child.key)) {
                    ++i;
                    nodeKeys.remove(child.key);
                } else {
                    node.remove(i);
                }
            }

            //добавление оставшихся ключей
            for (ClientGroupObjectValue key : nodeKeys) {
                ClientGroupObjectValue compositeKey = new ClientGroupObjectValue(node.compositeKey, key);
                node.add(new TreeGroupNode(group, key, compositeKey));
            }

            if (node.getChildCount() == 0) {
                //todo: ?? что в этом случае делать и как вообще отображать листовые узлы
            }
        }
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> ivalues) {
        values.put(property, ivalues);
        updateUI();
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
            updateUI();
            return true;
        } else {
            return false;
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        if (properties.remove(property)) {
            updateUI();
        }
    }

    private class TreeGroupNode extends ClientTreeNode {
        public ClientGroupObject group;
        public ClientGroupObjectValue key;
        public ClientGroupObjectValue compositeKey;

        public TreeGroupNode() {
            this(null, new ClientGroupObjectValue(), new ClientGroupObjectValue());
        }

        public TreeGroupNode(ClientGroupObject group, ClientGroupObjectValue key, ClientGroupObjectValue compositeKey) {
            this.group = group;
            this.key = key;
            this.compositeKey = compositeKey;
        }

        @Override
        public String toString() {
            if (group == null || key == null || compositeKey == null) {
                return "RootNode: todo: delete";
            }

            String caption = "Values: ";
            for (ClientPropertyDraw property : properties) {
                Map<ClientGroupObjectValue, Object> propValues = values.get(property);
                if (propValues != null) {
                    Object value = propValues.get(compositeKey);
                    if (value != null) {
                        //todo: пока так, но надо как-то пропускать через рендереры...
                        caption += value.toString();
                    }
                }
            }

            return caption;
        }
    }

    public class NodeEnumerator {
        //depthFirstEnumeration, потому что важно, чтобы обновление происходило, начиная с нижних узлов
        private Enumeration<ClientTreeNode> nodes = ((ClientTreeNode) treeModel.getRoot()).depthFirstEnumeration();
        private TreeGroupNode nextNode;
        public NodeEnumerator() {
            findNextNode();
        }

        public TreeGroupNode nextNode() {
            TreeGroupNode result = nextNode;
            findNextNode();
            return result;
        }

        private void findNextNode() {
            nextNode = null;
            while (nodes.hasMoreElements()) {
                ClientTreeNode node = nodes.nextElement();
                if (accept(node)) {
                    nextNode = (TreeGroupNode) node;
                    break;
                }
            }
        }

        private boolean accept(ClientTreeNode node) {
            return node == rootNode || (node instanceof TreeGroupNode) && isExpanded(getPathToRoot(node));
        }

        public boolean hasNext() {
            return nextNode != null;
        }
    }
}
