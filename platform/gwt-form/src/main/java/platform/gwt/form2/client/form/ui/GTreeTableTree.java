package platform.gwt.form2.client.form.ui;

import platform.gwt.utils.GwtSharedUtils;
import platform.gwt.form2.shared.view.GForm;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;

import java.util.*;

public class GTreeTableTree {
    private GForm form;

    private final Map<GGroupObject, Set<GTreeTableNode>> groupNodes = new HashMap<GGroupObject, Set<GTreeTableNode>>();
    public ArrayList<GPropertyDraw> properties = new ArrayList<GPropertyDraw>();
    public final List<GPropertyDraw> columnProperties = new ArrayList<GPropertyDraw>();

    public HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>>();
    public HashMap<GGroupObject, List<GPropertyDraw>> groupPropsMap = new HashMap<GGroupObject, List<GPropertyDraw>>();

    public GTreeTableTree(GForm iForm) {
        form = iForm;
    }

    public int addProperty(GGroupObject group, GPropertyDraw property) {

        if (properties.indexOf(property) == -1) {
            int ins = GwtSharedUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(ins, property);

            List<GPropertyDraw> groupProperties = groupPropsMap.get(group);
            if (groupProperties == null) {
                groupProperties = new ArrayList<GPropertyDraw>();
                groupPropsMap.put(group, groupProperties);
            }
            int gins = GwtSharedUtils.relativePosition(property, properties, groupProperties);
            groupProperties.add(gins, property);

            if (group.isLastGroupInTree()) {
                int tins = GwtSharedUtils.relativePosition(property, properties, columnProperties);
                columnProperties.add(tins, property);
                return tins + 1;
            }
        }
        return -1;
    }

    public int removeProperty(GGroupObject group, GPropertyDraw property) {
        values.remove(property);

        properties.remove(property);
        if (groupPropsMap.containsKey(group))
            groupPropsMap.get(group).remove(property);

        int ind = columnProperties.indexOf(property);
        if (ind != -1) {
            columnProperties.remove(property);
        }
        return ind + 1;
    }

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents) {
        Map<GGroupObjectValue, List<GGroupObjectValue>> childTree = new HashMap<GGroupObjectValue, List<GGroupObjectValue>>();

        for (int i = 0; i < keys.size(); i++) {
            GGroupObjectValue key = keys.get(i);

            GGroupObjectValue parentPath = new GGroupObjectValue(key);
            parentPath.removeAll(group.objects);
            parentPath.putAll(parents.get(i));

            if (childTree.containsKey(parentPath)) {
                childTree.get(parentPath).add(key);
            } else {

                List<GGroupObjectValue> children = new ArrayList<GGroupObjectValue>();
                children.add(key);
                childTree.put(parentPath, children);
            }
        }

        for (GTreeTableNode groupNode : getGroupNodes(group.getUpTreeGroup())) {
            synchronize(groupNode, group, childTree);
        }
    }

    void synchronize(GTreeTableNode parent, GGroupObject syncGroup, Map<GGroupObjectValue, List<GGroupObjectValue>> tree) {
//        final List<GGroupObjectValue> syncChilds = tree.containsKey(parent.key)
//                                                   ? tree.get(parent.key)
//                                                   : new ArrayList<GGroupObjectValue>();
//
//        if (hasOnlyExpandningNodeAsChild(parent)) {
//            remove(getChildren(parent)[0]);
//        }
//
//        List<GTreeTableNode> allChildren = new ArrayList<GTreeTableNode>();
//        GTreeTableNode[] thisGroupChildren = new GTreeTableNode[syncChilds.size()];
//
//        for (TreeNode child : getChildren(parent)) {
//            GTreeTableNode node = (GTreeTableNode) child;
//            if (node.group.equals(syncGroup)) {
//                int index = syncChilds.indexOf(node.key);
//                if (index == -1) {
//                    remove(node);
//                    removeFromGroupNodes(syncGroup, node);
//                } else {
//                    thisGroupChildren[index] = node;
//                    synchronize(node, syncGroup, tree);
//                }
//            } else {
//                allChildren.add(node);
//            }
//        }
//
//        for (int i = 0; i < syncChilds.size(); ++i) {
//            if (thisGroupChildren[i] == null) {
//                GTreeTableNode newNode = new GTreeTableNode(syncGroup, syncChilds.get(i));
//                thisGroupChildren[i] = newNode;
//                add(newNode, parent);
//
//                getGroupNodes(syncGroup).add(newNode);
//
//                if (syncGroup.mayHaveChildren()) {
//                    add(new ExpandingTreeTableNode(), newNode);
//                }
//            }
//        }
//
//        if (parent.group == syncGroup) {
//            allChildren.addAll(0, Arrays.asList(thisGroupChildren));
//        } else {
//            allChildren.addAll(Arrays.asList(thisGroupChildren));
//        }
//
//        removeList(getChildren(parent));
//
//        for (GTreeTableNode child : allChildren) {
//            add(child, parent);
//        }
//
//        if (getChildren(parent).length == 0) {
//            if (parent.group != null && parent.group.mayHaveChildren()) {
//                add(new ExpandingTreeTableNode(), parent);
//            }
//        }
    }

    public boolean hasOnlyExpandningNodeAsChild(GTreeTableNode node) {
//        return getChildren(node).length == 1 && getChildren(node)[0] instanceof ExpandingTreeTableNode;
        return false;
    }

    private void removeFromGroupNodes(GGroupObject syncGroup, GTreeTableNode node) {
        getGroupNodes(syncGroup).remove(node);

//        for (TreeNode child : getChildren(node)) {
//            if (!(child instanceof ExpandingTreeTableNode))
//                removeFromGroupNodes(((GTreeTableNode) child).group, (GTreeTableNode) child);
//        }
    }

    public Set<GTreeTableNode> getGroupNodes(GGroupObject group) {
//        if (group == null) {
//            return Collections.singleton(getRoot());
//        }

        Set<GTreeTableNode> nodes = groupNodes.get(group);
//        if (nodes == null) {
//            nodes = new HashSet<GTreeTableNode>();
//            groupNodes.put(group, nodes);
//        }
        return nodes;
    }

    public void setPropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        if (propValues != null) {
            GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);
        }
    }

    public void updateValues() {
        for (GGroupObject group : groupNodes.keySet()) {
            for (GTreeTableNode node : groupNodes.get(group)) {
                for (int i = 0; i < columnProperties.size(); i++) {
                    GPropertyDraw property = getProperty(group, i);
                    if (property != null) {
//                        node.setAttribute(columnProperties.get(i).sID, values.get(property).get(node.key));
                    }
                }
            }
        }
    }

    public GPropertyDraw getProperty(GGroupObject group, int column) {
        List<GPropertyDraw> groupProperties = groupPropsMap.get(group);
        if (groupProperties == null || column > groupProperties.size()) {
            return null;
        }

        return groupProperties.get(column);
    }

    private class ExpandingTreeTableNode {
        public ExpandingTreeTableNode() {
            super();
//            setAttribute("nodeTitle", "Retrieving table...");
        }
    }
}
