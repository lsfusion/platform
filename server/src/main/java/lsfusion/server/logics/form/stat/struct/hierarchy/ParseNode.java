package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.admin.Settings;

import java.util.HashMap;
import java.util.Map;

public abstract class ParseNode {

    public static ParseNode getIntegrationHierarchy(StaticDataGenerator.Hierarchy hierarchy) {
        return getGroupIntegrationHierarchy(hierarchy.getRoot(), hierarchy);
    }

    private static ParseNode getGroupIntegrationHierarchy(GroupObjectEntity currentGroup, StaticDataGenerator.Hierarchy hierarchy) {

        // generating used property groups hierarchy
        Map<Group, MOrderExclSet<PGNode>> childGroupNodes = new HashMap<>(); // not MMap because we need null keys in this case
        childGroupNodes.put(null, SetFact.<PGNode>mOrderExclSet());

        if(Settings.get().isGroupIntegrationHierarchyOldOrder()) {

            for(PropertyDrawEntity<?> property : hierarchy.getProperties(currentGroup))
                fillPropertyGroupIntegrationHierarchy(new PropertyPGNode(property), childGroupNodes);
            for(GroupObjectEntity group : hierarchy.getDependencies(currentGroup))
                fillPropertyGroupIntegrationHierarchy(new GroupObjectPGNode(group), childGroupNodes);

        } else {

            ImOrderSet<PropertyDrawEntity> properties = hierarchy.getProperties(currentGroup);
            ImOrderSet<GroupObjectEntity> groups = hierarchy.getDependencies(currentGroup);
            int i = 0, j = 0;
            while (i < properties.size() || j < groups.size()) {
                PropertyDrawEntity property = i < properties.size() ? properties.get(i) : null;
                GroupObjectEntity group = j < groups.size() ? groups.get(j) : null;
                if ((property != null && (group == null || compareIndexes(property.getScriptIndex(), group.getScriptIndex()) <= 0))) {
                    fillPropertyGroupIntegrationHierarchy(new PropertyPGNode(property), childGroupNodes);
                    i++;
                } else {
                    fillPropertyGroupIntegrationHierarchy(new GroupObjectPGNode(group), childGroupNodes);
                    j++;
                }
            }

        }
        
        // generating parse nodes recursively
        return getPropertyGroupIntegrationHierarchy(null, childGroupNodes, currentGroup, hierarchy);
    }

    private static int compareIndexes(Pair<Integer, Integer> propertyIndex, Pair<Integer, Integer> groupIndex) {
        if(propertyIndex == null) {
            return groupIndex == null ? 0 : -1;
        } else {
            if(groupIndex == null) {
                return 1;
            } else {
                int compare = propertyIndex.first.compareTo(groupIndex.first);
                return compare == 0 ? propertyIndex.second.compareTo(groupIndex.second) : compare;
            }
        }
    }

    private static ParseNode getPropertyGroupIntegrationHierarchy(Group currentPropertyGroup, final Map<Group, MOrderExclSet<PGNode>> childGroupNodes, final GroupObjectEntity currentGroup, final StaticDataGenerator.Hierarchy hierarchy) {
        MOrderExclSet<PGNode> childGroups = childGroupNodes.get(currentPropertyGroup);
        ImOrderSet<ParseNode> childNodes = childGroups.immutableOrder().mapOrderSetValues(new GetValue<ParseNode, PGNode>() {
            public ParseNode getMapValue(PGNode value) {
                return value.createNode(childGroupNodes, hierarchy, currentGroup == null || currentGroup.isIndex());
            }});
        if(currentPropertyGroup == null) {
            if(currentGroup == null)
                return new FormParseNode(childNodes);
            return new GroupObjectParseNode(childNodes, currentGroup);
        }
        return new PropertyGroupParseNode(childNodes, currentPropertyGroup);
    }

    private static void fillPropertyGroupIntegrationHierarchy(PGNode node, Map<Group, MOrderExclSet<PGNode>> childGroupNodes) {
        Group parentGroup = node.getParent();
        if(parentGroup != null && parentGroup.system)
            parentGroup = null;
        MOrderExclSet<PGNode> childGroups = childGroupNodes.get(parentGroup);
        if(childGroups == null) {
            childGroups = SetFact.mOrderExclSet();
            childGroupNodes.put(parentGroup, childGroups);
        
            if(parentGroup != null)
                fillPropertyGroupIntegrationHierarchy(new PropertyGroupPGNode(parentGroup), childGroupNodes);
        }        
        childGroups.exclAdd(node);
    }

    public abstract <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData);
    
    public abstract <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData);

    private interface PGNode {
        Group getParent();

        ParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive);
    }

    private static class PropertyPGNode implements PGNode {
        public final PropertyDrawEntity<?> property;

        public PropertyPGNode(PropertyDrawEntity<?> property) {
            this.property = property;
        }

        public Group getParent() {
            return property.getGroup();
        }

        public ParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return new PropertyParseNode(property, isExclusive);
        }
    }

    private static class GroupObjectPGNode implements PGNode {
        public final GroupObjectEntity groupObject;

        public GroupObjectPGNode(GroupObjectEntity groupObject) {
            this.groupObject = groupObject;
        }

        public Group getParent() {
            return groupObject.propertyGroup;
        }

        public ParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return getGroupIntegrationHierarchy(groupObject, hierarchy);
        }
    }

    private static class PropertyGroupPGNode implements PGNode {
        public final Group group;

        public PropertyGroupPGNode(Group group) {
            this.group = group;
        }

        public Group getParent() {
            return group.getParent();
        }

        @Override
        public ParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return getPropertyGroupIntegrationHierarchy(group, childGroupNodes, null, hierarchy);
        }
    }
}
