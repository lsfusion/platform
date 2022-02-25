package lsfusion.server.logics.form.stat.struct.hierarchy;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.stat.struct.export.hierarchy.json.FormPropertyDataInterface;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.util.HashMap;
import java.util.Map;

public interface ParseNode {

    static ParseNode getIntegrationHierarchy(StaticDataGenerator.Hierarchy hierarchy) {
        return getGroupIntegrationHierarchy(hierarchy.getRoot(), hierarchy);
    }

    static ParseNode getGroupIntegrationHierarchy(GroupObjectEntity currentGroup, StaticDataGenerator.Hierarchy hierarchy) {

        // generating used property groups hierarchy
        Map<Group, MOrderExclSet<PGNode>> childGroupNodes = new HashMap<>(); // not MMap because we need null keys in this case
        childGroupNodes.put(null, SetFact.mOrderExclSet());

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

    static int compareIndexes(Pair<Integer, Integer> propertyIndex, Pair<Integer, Integer> groupIndex) {
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

    static ParseNode getPropertyGroupIntegrationHierarchy(Group currentPropertyGroup, final Map<Group, MOrderExclSet<PGNode>> childGroupNodes, final GroupObjectEntity currentGroup, final StaticDataGenerator.Hierarchy hierarchy) {
        MOrderExclSet<PGNode> childGroups = childGroupNodes.get(currentPropertyGroup);
        ImOrderSet<ChildParseNode> childNodes = childGroups.immutableOrder().mapOrderSetValues(value -> value.createNode(childGroupNodes, hierarchy, currentGroup == null || currentGroup.isIndex()));
        if(currentPropertyGroup == null) {
            if(currentGroup == null)
                return new FormParseNode(childNodes);
            return new GroupObjectParseNode(childNodes, currentGroup);
        }
        return new PropertyGroupParseNode(childNodes, currentPropertyGroup);
    }

    static void fillPropertyGroupIntegrationHierarchy(PGNode node, Map<Group, MOrderExclSet<PGNode>> childGroupNodes) {
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

    <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData);
    
    <T extends Node<T>> boolean exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData);

    <X extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, X> getJSONProperty(FormPropertyDataInterface<P> form, ImRevMap<P, X> mapValues, ImRevMap<ObjectEntity, X> mapObjects);

    interface PGNode {
        Group getParent();

        ChildParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive);
    }

    class PropertyPGNode implements PGNode {
        public final PropertyDrawEntity<?> property;

        public PropertyPGNode(PropertyDrawEntity<?> property) {
            this.property = property;
        }

        public Group getParent() {
            return property.getGroup();
        }

        public ChildParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return new PropertyParseNode(property, isExclusive);
        }
    }

    class GroupObjectPGNode implements PGNode {
        public final GroupObjectEntity groupObject;

        public GroupObjectPGNode(GroupObjectEntity groupObject) {
            this.groupObject = groupObject;
        }

        public Group getParent() {
            return groupObject.propertyGroup;
        }

        public ChildParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return (ChildParseNode) getGroupIntegrationHierarchy(groupObject, hierarchy);
        }
    }

    class PropertyGroupPGNode implements PGNode {
        public final Group group;

        public PropertyGroupPGNode(Group group) {
            this.group = group;
        }

        public Group getParent() {
            return group.getParent();
        }

        @Override
        public ChildParseNode createNode(Map<Group, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return (ChildParseNode) getPropertyGroupIntegrationHierarchy(group, childGroupNodes, null, hierarchy);
        }
    }
}
