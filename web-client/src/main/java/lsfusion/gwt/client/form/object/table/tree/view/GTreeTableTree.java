package lsfusion.gwt.client.form.object.table.tree.view;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GGroupObjectValueBuilder;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.*;

public class GTreeTableTree {
    private GForm form;

    public NativeSIDMap<GGroupObject, ArrayList<GPropertyDraw>> groupProperties = new NativeSIDMap<>();
    private final NativeSIDMap<GGroupObject, NativeHashMap<GGroupObjectValue, GTreeObjectTableNode>> groupNodes = new NativeSIDMap<>();

    public GTreeRootTableNode root;

    public GTreeTableTree(GForm iForm) {
        form = iForm;
        root = new GTreeRootTableNode();
    }

    public int getPropertyIndex(GPropertyDraw propertyDraw) {
        ArrayList<GPropertyDraw> properties = groupProperties.get(propertyDraw.groupObject);
        int index = properties.indexOf(propertyDraw);
        //first column in tree is 'tree'
        return index == -1 ? -1 : index + 1;
    }

    public int updateProperty(GPropertyDraw property) {
        GGroupObject group = property.groupObject;
        ArrayList<GPropertyDraw> properties = groupProperties.computeIfAbsent(group, k -> new ArrayList<>());
        if (!properties.contains(property) && !property.hideOrRemove()) {
            int gins = GwtSharedUtils.relativePosition(property, form.propertyDraws, properties);
            properties.add(gins, property);
            if (group.isLastGroupInTree())
                return gins + 1;
        }
        return -1;
    }

    public int removeProperty(GPropertyDraw property) {
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
    public void setKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, ArrayList<GGroupObjectValue> parents, NativeHashMap<GGroupObjectValue, Integer> expandable, int requestIndex) {
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
            synchronize(root, group, childTree, expandable, requestIndex);
        else
            getGroupNodes(upGroup).foreachValue(groupNode ->
                    synchronize(groupNode, group, childTree, expandable, requestIndex));
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
    private void synchronize(GTreeContainerTableNode node, GGroupObject syncGroup, NativeHashMap<GGroupObjectValue, OptimizedIndexOfArrayList<GGroupObjectValue>> tree, NativeHashMap<GGroupObjectValue, Integer> expandables, int requestIndex) {
        OptimizedIndexOfArrayList<GGroupObjectValue> syncChilds = tree.get(node.getKey());

        if (node.pendingExpanding != null) {
            if (node.pendingExpanding) {
                assert node.hasOnlyExpandingTreeTableNodes();
            } else { // collapsing
                assert !node.hasExpandableChildren();
            }

            if (node.pendingExpandingRequestIndex > requestIndex)
                return;

            if (node.pendingExpanding) // removing expanding nodes
                node.removeNodes();

            node.setPendingExpanding(null, -1);
        }

        ArrayList<GTreeChildTableNode> children = node.getChildren();

        if (syncChilds == null) // collapsed (considering empty)
            syncChilds = new OptimizedIndexOfArrayList<>();

        ArrayList<GGroupObjectValue> thisGroupsList = syncChilds.getList();

        int downGroups = 0;
        int thisGroups = thisGroupsList.size();
        int childrenSize = children.size();

        GTreeObjectTableNode[] downGroupChildren = new GTreeObjectTableNode[childrenSize]; // we need to preserve the order
        GTreeObjectTableNode[] thisGroupChildren = new GTreeObjectTableNode[thisGroups];

        boolean isUpperGroup = !GwtClientUtils.nullEquals(node.getGroup(), syncGroup);

        // removing "obsolete" nodes
        // if it is an upper group, down groups are at the list beginning, otherwise at the end
        for (int i = childrenSize - 1; i >= 0; i--) {
            GTreeObjectTableNode child = (GTreeObjectTableNode) children.get(i); // since expandingTableNode already removed

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
            GTreeObjectTableNode child = thisGroupChildren[i];

            if (child == null) {
                thisGroupChildren[i] = child = new GTreeObjectTableNode(syncGroup, key);

                // if it is an upper group, there are down groups at the beginning
                int offset = (isUpperGroup ? downGroups : 0) + i;
                node.addNode(offset, child);
                GTreeObjectTableNode result = getGroupNodes(syncGroup).put(child.getKey(), child);
                assert result == null;
            }

            updateExpandable(syncGroup, expandables, key, child);

            synchronize(child, syncGroup, tree, expandables, requestIndex);
        }

        GTreeObjectTableNode[] allChildren = new GTreeObjectTableNode[downGroups + thisGroups];
        // if we're synchronizing "upper" group, then we should put downGroups up, otherwise down
        if (isUpperGroup) {
            System.arraycopy(downGroupChildren, 0, allChildren, 0, downGroups);
            System.arraycopy(thisGroupChildren, 0, allChildren, downGroups, thisGroups);
        } else {
            System.arraycopy(thisGroupChildren, 0, allChildren, 0, thisGroups);
            System.arraycopy(downGroupChildren, childrenSize - downGroups, allChildren, thisGroups, downGroups);
        }

        node.setChildren(GwtClientUtils.newArrayList(allChildren));
    }

    private void updateExpandable(GGroupObject syncGroup, NativeHashMap<GGroupObjectValue, Integer> expandables, GGroupObjectValue key, GTreeObjectTableNode child) {
        int expandable = 0;
        if (syncGroup.mayHaveChildren()) {
            Integer e = expandables.get(key);
            if(e != null)
                expandable = e;
        }
        child.setExpandable(expandable);
    }

    public void removeChildrenFromGroupNodes(GTreeContainerTableNode node) {
        for (GTreeChildTableNode child : node.getChildren()) {
            if(child instanceof GTreeObjectTableNode) {
                GTreeObjectTableNode childObject = (GTreeObjectTableNode) child;
                getGroupNodes(childObject.getGroup()).remove(childObject.getKey());

                removeChildrenFromGroupNodes(childObject);
            }
        }
    }

    public NativeHashMap<GGroupObjectValue, GTreeObjectTableNode> getGroupNodes(GGroupObject group) {
        return groupNodes.computeIfAbsent(group, k -> new NativeHashMap<>());
    }

    // should correspond getIndexRange
    public void updateRows(ArrayList<GTreeGridRecord> rows) {
        updateRows(rows, root, null);
    }

    private void updateRows(ArrayList<GTreeGridRecord> rows, GTreeContainerTableNode node, GTreeObjectGridRecord record) {
        for (GTreeChildTableNode child : node.getChildren()) {
            GTreeColumnValue treeValue = createTreeColumnValue(child, node, record);

            if (child instanceof GTreeObjectTableNode) {
                GTreeObjectTableNode childObject = (GTreeObjectTableNode) child;

                GTreeObjectGridRecord treeRecord = new GTreeObjectGridRecord(rows.size(), childObject, treeValue);

                rows.add(treeRecord);

                updateRows(rows, childObject, treeRecord);
            } else {
                assert node.pendingExpanding;
                assert node.hasOnlyExpandingTreeTableNodes();
                assert node instanceof GTreeObjectTableNode;

                rows.add(new GTreeExpandingGridRecord(rows.size(), node, treeValue, ((GTreeExpandingTableNode)child)));
            }
        }
    }

    // should correspond updateRows
    // index from first child (if any) till next sibling
    public Pair<Integer, Integer> incGetIndexRange(GTreeContainerTableNode findNode) {
        return incGetIndexRange(new Result<>(0), root, findNode);
    }
    private Pair<Integer, Integer> incGetIndexRange(Result<Integer> counter, GTreeContainerTableNode node, GTreeContainerTableNode findNode) {
        for (GTreeChildTableNode child : node.getChildren()) {
            if(child instanceof GTreeObjectTableNode) {
                GTreeObjectTableNode childObject = (GTreeObjectTableNode) child;

                counter.set(counter.result + 1);

                Integer startIndex = null;
                if(childObject.equals(findNode))
                    startIndex = counter.result;

                Pair<Integer, Integer> result = incGetIndexRange(counter, childObject, findNode);

                if(startIndex != null) {
                    assert result == null;
                    return new Pair<>(startIndex, counter.result);
                } else {
                    if(result != null)
                        return result;
                }
            } else
                counter.set(counter.result + 1);
        }
        return null;
    }

    public GTreeColumnValue createTreeColumnValue(GTreeChildTableNode child, GTreeContainerTableNode parent, GTreeObjectGridRecord parentRecord) {
        GTreeColumnValue parentTreeValue = parentRecord != null ? parentRecord.getTreeValue() : null;
        int level = parentTreeValue != null ? parentTreeValue.level + 1 : 0;
        boolean[] parentLastInLevelMap = parentTreeValue != null ? parentTreeValue.lastInLevelMap : null;

        boolean[] lastInLevelMap = new boolean[level];
        assert level > 0 == (parentLastInLevelMap != null);
        if(parentLastInLevelMap != null) {
            System.arraycopy(parentLastInLevelMap, 0, lastInLevelMap, 0, level - 1);
            lastInLevelMap[level - 1] = parent.isLast(child);
        }
        return new GTreeColumnValue(level, lastInLevelMap, child.getColumnValueType(), true, false);
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

    // needed for expand functionality, so assert that after there is an isExpandableCheck
    public GTreeObjectTableNode getExpandNodeByRecord(GTreeGridRecord record) {
        if (record instanceof GTreeObjectGridRecord)
            return getGroupNodes(record.getGroup()).get(record.getKey());
        return null;
    }

}
