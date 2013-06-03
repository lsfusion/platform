package lsfusion.server.form.entity;

import lsfusion.base.BaseUtils;

import java.util.*;

/**
 * User: DAle
 * Date: 20.08.2010
 * Time: 16:45:39
 */

public class GroupObjectHierarchy {
    public static final String rootNodeName = "__ROOT__";

    private List<GroupObjectEntity> groups;
    private Map<GroupObjectEntity, List<GroupObjectEntity>> dependencies;
    private Set<GroupObjectEntity> markedGroups;

    /// dependencies должны содержать зависимости, образующие лес (набор деревьев)
    public GroupObjectHierarchy(List<GroupObjectEntity> groupObjects, Map<GroupObjectEntity, List<GroupObjectEntity>> depends) {
        groups = new ArrayList<GroupObjectEntity>(groupObjects);
        dependencies = new HashMap<GroupObjectEntity, List<GroupObjectEntity>>(depends);
        markedGroups = new HashSet<GroupObjectEntity>();
        for (GroupObjectEntity group : groups) {
            if (!dependencies.containsKey(group) || dependencies.get(group) == null) {
                dependencies.put(group, new ArrayList<GroupObjectEntity>());
            }
        }
        checkValidity();
    }

    public GroupObjectHierarchy() {
        groups = new ArrayList<GroupObjectEntity>();
        dependencies = new HashMap<GroupObjectEntity, List<GroupObjectEntity>>();
        markedGroups = new HashSet<GroupObjectEntity>();
    }

    public void addDependency(GroupObjectEntity a, GroupObjectEntity b) {
        assert groups.contains(a) && groups.contains(b);
        assert !a.equals(b);
        dependencies.get(a).add(b);
        checkValidity();
    }

    public void removeDependency(GroupObjectEntity a, GroupObjectEntity b) {
        if (groups.contains(a)) {
            dependencies.get(a).remove(b);
        }
    }

    public void addGroup(GroupObjectEntity newGroupObj) {
        if (!groups.contains(newGroupObj)) {
            groups.add(newGroupObj);
            dependencies.put(newGroupObj, new ArrayList<GroupObjectEntity>());
        }
    }

    public void addGroups(Collection<GroupObjectEntity> newGroupObjects) {
        for (GroupObjectEntity group : newGroupObjects) {
            addGroup(group);
        }
    }

    public void removeGroup(GroupObjectEntity group) {
        for (Map.Entry<GroupObjectEntity, List<GroupObjectEntity>> entry : dependencies.entrySet()) {
            if (entry.getValue().contains(group)) {
                entry.getValue().addAll(dependencies.get(group));
                entry.getValue().remove(group);
                break;
            }
        }
        dependencies.remove(group);
        groups.remove(group);
        markedGroups.remove(group);
    }

    public void markGroupAsNonJoinable(GroupObjectEntity group) {
        assert groups.contains(group);
        markedGroups.add(group);
    }

    private List<GroupObjectEntity> getRootNodes() {
        List<GroupObjectEntity> roots = new ArrayList<GroupObjectEntity>();
        Map<GroupObjectEntity, Integer> inputDegree = getInputDegrees();
        for (Map.Entry<GroupObjectEntity, Integer> entry : inputDegree.entrySet()) {
            if (entry.getValue() == 0) {
                roots.add(entry.getKey());
            }
        }
        return roots;
    }

    private Map<GroupObjectEntity, Integer> getInputDegrees() {
        Map<GroupObjectEntity, Integer> degrees = new HashMap<GroupObjectEntity, Integer>();
        for (GroupObjectEntity object : groups) {
            degrees.put(object, 0);
        }
        for (GroupObjectEntity object : groups) {
            for (GroupObjectEntity dependentObj : dependencies.get(object)) {
                degrees.put(dependentObj, degrees.get(dependentObj) + 1);
            }
        }
        return degrees;
    }

    /// Проверка на отсутствие циклов с помощью breadth-first search
    private boolean isValidForest(Map<GroupObjectEntity, List<GroupObjectEntity>> dependencies) {
        Set<GroupObjectEntity> was = new HashSet<GroupObjectEntity>();
        Queue<GroupObjectEntity> queue = new ArrayDeque<GroupObjectEntity>();
        List<GroupObjectEntity> roots = getRootNodes();
        for (GroupObjectEntity obj : roots) {
            queue.add(obj);
            was.add(obj);
        }

        while (!queue.isEmpty()) {
            GroupObjectEntity cur = queue.poll();
            for (GroupObjectEntity dependentObj : dependencies.get(cur)) {
                if (was.contains(dependentObj)) {
                    return false;
                } else {
                    was.add(dependentObj);
                    queue.add(dependentObj);
                }
            }
        }
        return true;
    }

    private void checkValidity() {
        // Проверка на петли + assertы на принадлежность списку групп
        for (Map.Entry<GroupObjectEntity, List<GroupObjectEntity>> entry : dependencies.entrySet()) {
            GroupObjectEntity obj = entry.getKey();
            assert groups.contains(obj);
            for (GroupObjectEntity dependentObj : entry.getValue()) {
                assert groups.contains(dependentObj);
                assert !obj.equals(dependentObj);
            }
        }
        assert isValidForest(dependencies);
    }

    public static final class ReportNode {
        private List<GroupObjectEntity> groups;
        /// Равен max(groupLevel) потомков + groups.size()
        private int groupLevel;

        private ReportNode(GroupObjectEntity group) {
            this(Collections.singletonList(group));
        }

        private ReportNode(List<GroupObjectEntity> groups) {
            this.groups = new ArrayList<GroupObjectEntity>(groups);
        }

        public String getID() {
            assert groups.size() > 0;
            return groups.iterator().next().getSID();
        }

        public List<GroupObjectEntity> getGroupList() {
            return new ArrayList<GroupObjectEntity>(groups);
        }

        private void merge(ReportNode obj) {
            groups.addAll(obj.groups);
        }

        private boolean isNonJoinable = false;

        public boolean isNonJoinable() {
            return isNonJoinable;
        }

        public void setNonJoinable(boolean nonJoinable) {
            this.isNonJoinable = nonJoinable;
        }

        public int getGroupLevel() {
            return groupLevel;
        }

        void setGroupLevel(int groupLevel) {
            this.groupLevel = groupLevel;
        }
    }

    public ReportHierarchy createReportHierarchy() {
        return new ReportHierarchy(groups, dependencies, markedGroups);
    }

    /// Для отчета по одной группе оставляем от всей иерархии только путь от нужной нам группы до корневой вершины
    public ReportHierarchy createSingleGroupReportHierarchy(int groupId) {
        Map<GroupObjectEntity, GroupObjectEntity> parents = new HashMap<GroupObjectEntity, GroupObjectEntity>();
        GroupObjectEntity targetGroup = null;
        for (GroupObjectEntity parentGroup : groups) {
            for (GroupObjectEntity childGroup : dependencies.get(parentGroup)) {
                parents.put(childGroup, parentGroup);
            }
            if (parentGroup.getID() == groupId) {
                targetGroup = parentGroup;
            }
        }

        List<GroupObjectEntity> remainGroups = new ArrayList<GroupObjectEntity>();
        while (targetGroup != null) {
            remainGroups.add(targetGroup);
            targetGroup = parents.get(targetGroup);
        }
        Map<GroupObjectEntity, List<GroupObjectEntity>> remainDependencies = new HashMap<GroupObjectEntity, List<GroupObjectEntity>>();
        for (GroupObjectEntity group : remainGroups) {
            remainDependencies.put(group, BaseUtils.filterList(dependencies.get(group), remainGroups));
        }
        return new ReportHierarchy(remainGroups, remainDependencies, new HashSet<GroupObjectEntity>());
    }

    public static class ReportHierarchy {
        private List<ReportNode> reportNodes = new ArrayList<ReportNode>();
        private Map<ReportNode, List<ReportNode>> dependencies = new HashMap<ReportNode, List<ReportNode>>();
        private Map<GroupObjectEntity, ReportNode> groupToReportNode = new HashMap<GroupObjectEntity, ReportNode>();

        public ReportHierarchy(List<GroupObjectEntity> groupObjects, Map<GroupObjectEntity, List<GroupObjectEntity>> depends,
                               Set<GroupObjectEntity> markedGroups) {

            for (GroupObjectEntity group : groupObjects) {
                ReportNode newReportNode = new ReportNode(group);
                newReportNode.setNonJoinable(markedGroups.contains(group));
                reportNodes.add(newReportNode);
                groupToReportNode.put(group, newReportNode);
                dependencies.put(newReportNode, new ArrayList<ReportNode>());
            }

            for (Map.Entry<GroupObjectEntity, List<GroupObjectEntity>> entry : depends.entrySet()) {
                ReportNode reportNode = groupToReportNode.get(entry.getKey());
                for (GroupObjectEntity dependentGroup : entry.getValue()) {
                    ReportNode dependentReportNode = groupToReportNode.get(dependentGroup);
                    dependencies.get(reportNode).add(dependentReportNode);
                }
            }

            squeezeAll();
            countGroupLevels(null);
        }

        public List<ReportNode> getRootNodes() {
            List<ReportNode> roots = new ArrayList<ReportNode>();
            Set<ReportNode> dependentNodes = new HashSet<ReportNode>();
                
            for (Map.Entry<ReportNode, List<ReportNode>> entry : dependencies.entrySet()) {
                for (ReportNode node : entry.getValue()) {
                    dependentNodes.add(node);
                }
            }

            for (ReportNode node : reportNodes) {
                if (!dependentNodes.contains(node)) {
                    roots.add(node);
                }
            }
            return roots;
        }

        public Collection<ReportNode> getAllNodes() {
            return new ArrayList<ReportNode>(reportNodes);
        }

        public List<ReportNode> getChildNodes(ReportNode parent) {
            return new ArrayList<ReportNode>(dependencies.get(parent));
        }

        public ReportNode getParentNode(ReportNode node) {
            for (Map.Entry<ReportNode, List<ReportNode>> entry : dependencies.entrySet()) {
                if (entry.getValue().contains(node)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public ReportNode getReportNode(GroupObjectEntity group) {
            return groupToReportNode.get(group);
        }

        public boolean isLeaf(ReportNode node) {
            Collection<ReportNode> children = dependencies.get(node);
            assert(children != null);
            return children.size() == 0;
        }

        private void squeezeAll() {
            for (ReportNode treeRoot : getRootNodes()) {
                squeeze(treeRoot);
            }
        }

        private void squeeze(ReportNode reportNode) {
            Collection<ReportNode> children = dependencies.get(reportNode);
            for (ReportNode child : children) {
                squeeze(child);
            }
            if (children.size() == 1 && !reportNode.isNonJoinable()) {
                ReportNode child = BaseUtils.single(children);
                dependencies.put(reportNode, dependencies.get(child));
                dependencies.remove(child);
                reportNode.merge(child);
                for (GroupObjectEntity childGroup : child.getGroupList()) {
                    groupToReportNode.put(childGroup, reportNode);
                }
                reportNodes.remove(child);
            }
        }

        private int countGroupLevels(ReportNode parent) {
            int maxChildLevel = 0;
            List<ReportNode> children = (parent == null ? getRootNodes() : dependencies.get(parent));
            final int selfChildCount = (parent == null ? 0 : parent.getGroupList().size());

            for (ReportNode child : children) {
                maxChildLevel = Math.max(maxChildLevel, countGroupLevels(child));
            }
            if (parent != null) {
                parent.setGroupLevel(maxChildLevel + selfChildCount);
            }
            return maxChildLevel + selfChildCount;
        }

        public Map<String, List<String>> getReportHierarchyMap() {
            Map<String, List<String>> res = new HashMap<String, List<String>>();
            for (Map.Entry<ReportNode, List<ReportNode>> parentNode : dependencies.entrySet()) {
                String parentID = parentNode.getKey().getID();
                List<String> childIDs = new ArrayList<String>();
                for (ReportNode child : parentNode.getValue()) {
                    childIDs.add(child.getID());
                }
                res.put(parentID, childIDs);
            }
            List<String> rootIDs = new ArrayList<String>();
            for (ReportNode node : getRootNodes()) {
                rootIDs.add(node.getID());
            }
            res.put(rootNodeName, rootIDs);
            return res;
        }
    }
}

