package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.*;

public class GTreeTableTree {
    private GForm form;

    public HashMap<GGroupObject, List<GPropertyDraw>> groupProperties = new HashMap<>();
    private Map<GGroupObject, Set<GTreeTableNode>> groupNodes = new HashMap<>();

    public NativeHashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> values = new NativeHashMap<>();
    public HashMap<GPropertyDraw, Map<GGroupObjectValue, Object>> readOnly = new HashMap<>();

    public GTreeTableNode root;

    public GTreeTableTree(GForm iForm) {
        form = iForm;
        root = new GTreeTableNode();
    }

    public int updateProperty(GGroupObject group, GPropertyDraw property) {
        List<GPropertyDraw> properties = groupProperties.computeIfAbsent(group, k -> new ArrayList<>());
        if (!properties.contains(property) && !property.hide) {
            int gins = GwtSharedUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(gins, property);
            if (group.isLastGroupInTree())
                return gins + 1;
        }
        return -1;
    }

    public int removeProperty(GGroupObject group, GPropertyDraw property) {
        values.remove(property);

        List<GPropertyDraw> properties = groupProperties.get(group);
        int ind = properties.indexOf(property);
        properties.remove(property);
        if(group.isLastGroupInTree())
            return ind + 1;
        return -1;
    }

    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, Map<GGroupObjectValue, Boolean> expandable) {
        Map<GGroupObjectValue, List<GGroupObjectValue>> childTree = new HashMap<>();

        for (int i = 0; i < keys.size(); i++) {
            GGroupObjectValue key = keys.get(i);

            GGroupObjectValueBuilder parentPathBuilder =
                    new GGroupObjectValueBuilder(key)
                            .removeAll(group.objects)
                            .putAll(parents.get(i));

            GGroupObjectValue parentPath = parentPathBuilder.toGroupObjectValue();

            List<GGroupObjectValue> children = childTree.get(parentPath);
            if (children == null) {
                children = new ArrayList<>();
                childTree.put(parentPath, children);
            }
            children.add(key);
        }

        for (GTreeTableNode groupNode : getGroupNodes(group.getUpTreeGroup())) {
            synchronize(groupNode, group, childTree, expandable);
        }
    }

    void synchronize(GTreeTableNode parent, GGroupObject syncGroup, Map<GGroupObjectValue, List<GGroupObjectValue>> tree, Map<GGroupObjectValue, Boolean> expandables) {
        List<GGroupObjectValue> syncChilds = tree.get(parent.getKey());
        if (syncChilds == null) {
            syncChilds = new ArrayList<>();
        }

        if (hasOnlyExpandingNodeAsChild(parent)) {
            parent.getChild(0).removeFromParent();
        }

        List<GTreeTableNode> allChildren = new ArrayList<>();
        GTreeTableNode[] thisGroupChildren = new GTreeTableNode[syncChilds.size()];

        for (GTreeTableNode child : new ArrayList<>(parent.getChildren())) {

            if (child.getGroup().equals(syncGroup)) {
                int index = syncChilds.indexOf(child.getKey());
                if (index == -1) {
                    child.removeFromParent();
                    removeFromGroupNodes(child);
                } else {
                    thisGroupChildren[index] = child;
                }
            } else {
                allChildren.add(child);
            }
        }

        for (int i = 0; i < syncChilds.size(); ++i) {
            GGroupObjectValue key = syncChilds.get(i);
            GTreeTableNode child = thisGroupChildren[i];

            if (child == null) {
                thisGroupChildren[i] = child = new GTreeTableNode(syncGroup, key);

                parent.addNode(child);

                getGroupNodes(syncGroup).add(child);
            }

            boolean expandable = false;
            if (syncGroup.mayHaveChildren()) {
                Boolean e = expandables.get(key);
                expandable = e == null || e;
            }
            child.setExpandable(expandable);

            synchronize(child, syncGroup, tree, expandables);
        }

        if (parent.getGroup() == syncGroup) {
            allChildren.addAll(0, Arrays.asList(thisGroupChildren));
        } else {
            allChildren.addAll(Arrays.asList(thisGroupChildren));
        }

        removeList(parent.getChildren());

        for (GTreeTableNode child : allChildren) {
            parent.addNode(child);
        }

        if (parent.getChildren().isEmpty() && parent.isExpandable()) {
            parent.addNode(new ExpandingTreeTableNode());
            parent.setOpen(false);
        }
    }

    private void removeList(List<GTreeTableNode> children) {
        for (GTreeTableNode child : new ArrayList<>(children)) {
            child.removeFromParent();
        }
    }

    public boolean hasOnlyExpandingNodeAsChild(GTreeTableNode node) {
        return node.getChildren().size() == 1 && node.getChild(0) instanceof ExpandingTreeTableNode;
    }

    private void removeFromGroupNodes(GTreeTableNode node) {
        getGroupNodes(node.getGroup()).remove(node);

        for (GTreeTableNode child : node.getChildren()) {
            if (!(child instanceof ExpandingTreeTableNode)) {
                removeFromGroupNodes(child);
            }
        }
    }

    public Set<GTreeTableNode> getGroupNodes(GGroupObject group) {
        if (group == null) {
            return Collections.singleton(root);
        }

        return groupNodes.computeIfAbsent(group, k -> new HashSet<>());
    }


    public void setPropertyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);
    }

    public void setReadOnlyValues(GPropertyDraw property, Map<GGroupObjectValue, Object> readOnlyValues) {
        GwtSharedUtils.putUpdate(readOnly, property, readOnlyValues, false);
    }

    private int nodeCounter;

    public ArrayList<GTreeGridRecord> updateRows(int columnCount) {
        nodeCounter = 0;
        ArrayList<GTreeGridRecord> result = new ArrayList<>();
        if (!hasOnlyExpandingNodeAsChild(root)) {
            result.addAll(getNodeChildrenRecords(columnCount, root, 0, null));
        }
        return result;
    }

    private List<GTreeGridRecord> getNodeChildrenRecords(int columnCount, GTreeTableNode node, int level, GTreeColumnValue parentValue) {
        List<GTreeGridRecord> result = new ArrayList<>();
        for (GTreeTableNode child : node.getChildren()) {
            HashMap<GPropertyDraw, Object> valueMap = new HashMap<>();
            for (int i = 1; i < columnCount; i++) {
                GPropertyDraw property = getProperty(child.getGroup(), i);
                if (property != null) {
                    Object value = values.get(property).get(child.getKey());
                    valueMap.put(property, value);
                }
            }
            GTreeGridRecord record = new GTreeGridRecord(child.getGroup(), child.getKey(), valueMap);

            if (child.isExpandable() && (child.getChildren().size() > 1 || !hasOnlyExpandingNodeAsChild(child))) {
                child.setOpen(true);
            }

            GTreeColumnValue treeValue = generateTreeCellValue(child, parentValue, level);
            treeValue.addLastInLevel(level - 1, node.getChildren().indexOf(child) == node.getChildren().size() - 1);
            record.setTreeValue(treeValue);
            result.add(record);
            if (child.isOpen()) {
                result.addAll(getNodeChildrenRecords(columnCount, child, level + 1, treeValue));
            }
        }
        return result;
    }

    private GTreeColumnValue generateTreeCellValue(GTreeTableNode node, GTreeColumnValue parentValue, int level) {
        GTreeColumnValue value = new GTreeColumnValue(level, objectsToString(node.getGroup()) + nodeCounter);
        if (node.isOpen()) {
            value.setOpen(true);
        } else {
            value.setOpen(!node.getChildren().isEmpty() ? false : null);
        }
        if (parentValue != null) {
            value.setLastInLevelMap(parentValue.getLastInLevelMap());
        }
        nodeCounter++;
        return value;
    }

    private String objectsToString(GGroupObject groupObject) {
        String result = "";
        for (GObject object : groupObject.objects) {
            result += object.sID;
        }
        return result;
    }

    public List<GPropertyDraw> getProperties(GGroupObject group) {
        return groupProperties.get(group);
    }
    public GPropertyDraw getProperty(GGroupObject group, int column) {
        column = column - 1;

        List<GPropertyDraw> groupProperties = this.groupProperties.get(group);
        if (groupProperties == null || column < 0 || column >= groupProperties.size()) {
            return null;
        }

        return groupProperties.get(column);
    }

    public Object getValue(GGroupObject group, int column, GGroupObjectValue key) {
        GPropertyDraw property = getProperty(group, column);
        if (property == null) {
            return null;
        }
        return values.get(property).get(key);
    }

    public boolean isReadOnly(GGroupObject group, int column, GGroupObjectValue key) {
        if (column >= 1) {
            GPropertyDraw property = getProperty(group, column);
            if (property != null && !property.isReadOnly()) {
                Map<GGroupObjectValue, Object> propReadOnly = readOnly.get(property);
                return propReadOnly != null && propReadOnly.get(key) != null;
            }
        }
        return true;
    }

    public GTreeTableNode getNodeByRecord(GTreeGridRecord record) {
        if (record != null)
            for (GTreeTableNode node : getGroupNodes(record.getGroup()))
                if (record.getKey().equals(node.getKey())) {
                    return node;
                }
        return null;
    }

    private class ExpandingTreeTableNode extends GTreeTableNode {
        public ExpandingTreeTableNode() {
            super();
        }
    }
}
