package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.*;

public class GTreeTableTree {
    private GForm form;

    public NativeSIDMap<GGroupObject, ArrayList<GPropertyDraw>> groupProperties = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, GTreeTableNode>> groupNodes = new NativeSIDMap<>();

    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> values = new NativeSIDMap<>();
    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> loadings = new NativeSIDMap<>();
    public NativeSIDMap<GPropertyDraw, NativeHashMap<GGroupObjectValue, Object>> readOnly = new NativeSIDMap<>();

    public GTreeTableNode root;

    public GTreeTableTree(GForm iForm) {
        form = iForm;
        root = new GTreeTableNode();
    }

    public int getPropertyIndex(GPropertyDraw propertyDraw) {
        ArrayList<GPropertyDraw> properties = groupProperties.get(propertyDraw.groupObject);
        return properties.indexOf(propertyDraw);
    }

    public int updateProperty(GPropertyDraw property) {
        GGroupObject group = property.groupObject;
        ArrayList<GPropertyDraw> properties = groupProperties.computeIfAbsent(group, k -> new ArrayList<>());
        if (!properties.contains(property) && !property.hide) {
            int gins = GwtSharedUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(gins, property);
            if (group.isLastGroupInTree())
                return gins + 1;
        }
        return -1;
    }

    public int removeProperty(GPropertyDraw property) {
        values.remove(property);
        loadings.remove(property);

        GGroupObject group = property.groupObject;
        ArrayList<GPropertyDraw> properties = groupProperties.get(group);
        int ind = properties.indexOf(property);
        properties.remove(ind);
        if(group.isLastGroupInTree())
            return ind + 1;
        return -1;
    }

    private static class OptimizedIndexOfArrayList<K> {
        private final ArrayList<K> list = new ArrayList<>();
        private NativeHashMap<K, Integer> indexOf = null;

        public void add(K element) {
            list.add(element);
            int listSize = list.size();
            if(listSize > 5) {
                if(indexOf == null) {
                    indexOf = new NativeHashMap<>();
                    for(int i=0;i<listSize-1;i++)
                        indexOf.put(list.get(i), i);
                }
                indexOf.put(list.get(listSize - 1), listSize - 1);
            }
        }
        public int indexOf(K element) {
            if(indexOf != null) {
                Integer index = indexOf.get(element);
                return index != null ? index : -1;
            }
            return list.indexOf(element);
        }
        public ArrayList<K> getList() {
            return list;
        }
    }
    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, NativeHashMap<GGroupObjectValue, Boolean> expandable) {
        NativeHashMap<GGroupObjectValue, OptimizedIndexOfArrayList<GGroupObjectValue>> childTree = new NativeHashMap<>();

        for (int i = 0; i < keys.size(); i++) {
            GGroupObjectValue key = keys.get(i);
            GGroupObjectValue parent = parents.get(i);

            GGroupObjectValue parentPath = getParentPath(group, key, parent);
            OptimizedIndexOfArrayList<GGroupObjectValue> list = childTree.get(parentPath);
            if(list == null) {
                list = new OptimizedIndexOfArrayList<>();
                childTree.put(parentPath, list);
            }
            list.add(key);
        }

        GGroupObject upGroup = group.getUpTreeGroup();
        if(upGroup == null)
            synchronize(root, group, childTree, expandable);
        else
            getGroupNodes(upGroup).foreachValue(groupNode ->
                    synchronize(groupNode, group, childTree, expandable));
    }

    public GGroupObjectValue getParentPath(GGroupObject group, GGroupObjectValue key, GGroupObjectValue parent) {
        if(key.size() == group.objects.size()) {
//            assert new GGroupObjectValueBuilder()
//                    .putAll(key)
//                    .removeAll(group.objects).toGroupObjectValue().isEmpty();// to remove
            return parent;
        }

        return GGroupObjectValue.checkTwins(new GGroupObjectValueBuilder()
                        .putAll(key)
                        .removeAll(group.objects)
                        .putAll(parent).toGroupObjectValue());
    }

    // we're assuming that recursive "this" groups goes first (before down groups)
    private void synchronize(GTreeTableNode node, GGroupObject syncGroup, NativeHashMap<GGroupObjectValue, OptimizedIndexOfArrayList<GGroupObjectValue>> tree, NativeHashMap<GGroupObjectValue, Boolean> expandables) {
        if (hasOnlyExpandingNodeAsChild(node)) {
            node.removeNode(0);
        }

        OptimizedIndexOfArrayList<GGroupObjectValue> syncChilds = tree.get(node.getKey());
        if (syncChilds == null)
            syncChilds = new OptimizedIndexOfArrayList<>();

        ArrayList<GTreeTableNode> children = node.getChildren();

        int downGroups = 0;
        ArrayList<GGroupObjectValue> thisGroupsList = syncChilds.getList();
        int thisGroups = thisGroupsList.size();
        int childrenSize = children.size();
        GTreeTableNode[] downGroupChildren = new GTreeTableNode[childrenSize]; // we need to preserve the order
        GTreeTableNode[] thisGroupChildren = new GTreeTableNode[thisGroups];

        boolean isUpperGroup = !GwtClientUtils.nullEquals(node.getGroup(), syncGroup);

        // removing "obsolete" nodes
        // if it is an upper group, down groups are at the list beginning, otherwise at the end
        for (int i = childrenSize - 1; i >= 0; i--) {
            GTreeTableNode child = children.get(i);

            if (child.getGroup().equals(syncGroup)) {
                GGroupObjectValue childKey = child.getKey();
                int index = syncChilds.indexOf(childKey);
                if (index == -1) {
                    node.removeNode(i);
                    getGroupNodes(syncGroup).remove(childKey);

                    removeChildrenFromGroupNodes(child);
                } else
                    thisGroupChildren[index] = child;
            } else {
                downGroups++;
                downGroupChildren[i] = child;
            }
        }

        // adding missing nodes, recursive call
        for (int i = 0; i < thisGroups; ++i) {
            GGroupObjectValue key = thisGroupsList.get(i);
            GTreeTableNode child = thisGroupChildren[i];

            if (child == null) {
                thisGroupChildren[i] = child = new GTreeTableNode(syncGroup, key);

                // if it is an upper group, there are down groups at the beginning
                int offset = (isUpperGroup ? downGroups : 0) + i;
                node.addNode(offset, child);
                GTreeTableNode result = getGroupNodes(syncGroup).put(child.getKey(), child);
                assert result == null;
            }

            updateExpandable(syncGroup, expandables, key, child);

            synchronize(child, syncGroup, tree, expandables);
        }

        GTreeTableNode[] allChildren = new GTreeTableNode[downGroups + thisGroups];
        // if we're synchronizing "upper" group, then we should put downGroups up, otherwise down
        if (isUpperGroup) {
            System.arraycopy(downGroupChildren, 0, allChildren, 0, downGroups);
            System.arraycopy(thisGroupChildren, 0, allChildren, downGroups, thisGroups);
        } else {
            System.arraycopy(thisGroupChildren, 0, allChildren, 0, thisGroups);
            System.arraycopy(downGroupChildren, childrenSize - downGroups, allChildren, thisGroups, downGroups);
        }

        // adding virtual "expandable" node
        if (allChildren.length == 0 && node.isExpandable()) {
            allChildren = new GTreeTableNode[] { new ExpandingTreeTableNode() };
            node.setOpen(false);
        }

        node.setChildren(GwtClientUtils.newArrayList(allChildren));
    }

    private void updateExpandable(GGroupObject syncGroup, NativeHashMap<GGroupObjectValue, Boolean> expandables, GGroupObjectValue key, GTreeTableNode child) {
        boolean expandable = false;
        if (syncGroup.mayHaveChildren()) {
            Boolean e = expandables.get(key);
            expandable = e == null || e;
        }
        child.setExpandable(expandable);
    }

    public boolean hasOnlyExpandingNodeAsChild(GTreeTableNode node) {
        return node.getChildren().size() == 1 && node.getChild(0) instanceof ExpandingTreeTableNode;
    }

    private void removeChildrenFromGroupNodes(GTreeTableNode node) {
        for (GTreeTableNode child : node.getChildren()) {
            if (!(child instanceof ExpandingTreeTableNode)) {
                getGroupNodes(child.getGroup()).remove(child.getKey());

                removeChildrenFromGroupNodes(child);
            }
        }
    }

    public NativeHashMap<GGroupObjectValue, GTreeTableNode> getGroupNodes(GGroupObject group) {
        return groupNodes.computeIfAbsent(group, k -> new NativeHashMap<>());
    }

    public void setLoadings(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> propLoadings) {
        // here can be leaks because of nulls (so in theory nulls better to be removed)
        GwtSharedUtils.putUpdate(loadings, property, propLoadings, true);
    }

    public void setPropertyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> propValues, boolean updateKeys) {
        GwtSharedUtils.putUpdate(values, property, propValues, updateKeys);
    }

    public void setReadOnlyValues(GPropertyDraw property, NativeHashMap<GGroupObjectValue, Object> readOnlyValues) {
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

    private List<GTreeGridRecord> getNodeChildrenRecords(int columnCount, GTreeTableNode node, int level, boolean[] parentLastInLevelMap) {
        List<GTreeGridRecord> result = new ArrayList<>();
        for (GTreeTableNode child : node.getChildren()) {
            HashMap<GPropertyDraw, Pair<Object, Boolean>> valueMap = new HashMap<>();
            for (int i = 1; i < columnCount; i++) {
                GPropertyDraw property = getProperty(child.getGroup(), i);
                if (property != null) {
                    GGroupObjectValue key = child.getKey();
                    Object value = values.get(property).get(key);
                    NativeHashMap<GGroupObjectValue, Object> loadingMap = loadings.get(property);
                    boolean loading = loadingMap != null && loadingMap.get(key) != null;

                    valueMap.put(property, new Pair<>(value, loading));
                }
            }
            GTreeGridRecord record = new GTreeGridRecord(child.getGroup(), child.getKey(), valueMap);

            if (child.isExpandable() && (child.getChildren().size() > 1 || !hasOnlyExpandingNodeAsChild(child))) {
                child.setOpen(true);
            }

            boolean[] lastInLevelMap = new boolean[level];
            assert level > 0 == (parentLastInLevelMap != null);
            if(parentLastInLevelMap != null) {
                System.arraycopy(parentLastInLevelMap, 0, lastInLevelMap, 0, level - 1);
                lastInLevelMap[level - 1] = node.isLast(child);
            }
            record.setTreeValue(new GTreeColumnValue(level, lastInLevelMap, child.isOpen() ? (Boolean)true : (child.getChildren().isEmpty() ? null : false),
                    objectsToString(child.getGroup()) + (nodeCounter++), true, false));
            result.add(record);
            if (child.isOpen())
                result.addAll(getNodeChildrenRecords(columnCount, child, level + 1, lastInLevelMap));
        }
        return result;
    }

    private String objectsToString(GGroupObject groupObject) {
        String result = "";
        for (GObject object : groupObject.objects) {
            result += object.sID;
        }
        return result;
    }

    public ArrayList<GPropertyDraw> getProperties(GGroupObject group) {
        return groupProperties.get(group);
    }
    public GPropertyDraw getProperty(GGroupObject group, int column) {
        column = column - 1;

        ArrayList<GPropertyDraw> groupProperties = this.groupProperties.get(group);
        if (groupProperties == null || column < 0 || column >= groupProperties.size()) {
            return null;
        }

        return groupProperties.get(column);
    }

    public boolean isReadOnly(GGroupObject group, int column, GGroupObjectValue key) {
        if (column >= 1) {
            GPropertyDraw property = getProperty(group, column);
            if (property != null && !property.isReadOnly()) {
                NativeHashMap<GGroupObjectValue, Object> propReadOnly = readOnly.get(property);
                return propReadOnly != null && propReadOnly.get(key) != null;
            }
        }
        return true;
    }

    public GTreeTableNode getNodeByRecord(GTreeGridRecord record) {
        if (record != null)
            return getGroupNodes(record.getGroup()).get(record.getKey());
        return null;
    }

    private class ExpandingTreeTableNode extends GTreeTableNode {
        public ExpandingTreeTableNode() {
            super();
        }
    }
}
