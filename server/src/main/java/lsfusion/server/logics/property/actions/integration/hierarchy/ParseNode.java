package lsfusion.server.logics.property.actions.integration.hierarchy;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.logics.property.group.AbstractGroup;

import java.util.HashMap;
import java.util.Map;

public abstract class ParseNode {

    public static ParseNode getIntegrationHierarchy(StaticDataGenerator.Hierarchy hierarchy) {
        return getGroupIntegrationHierarchy(hierarchy.getRoot(), hierarchy);
    }

    private static ParseNode getGroupIntegrationHierarchy(GroupObjectEntity currentGroup, StaticDataGenerator.Hierarchy hierarchy) {

        // generating used property groups hierarchy
        Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes = new HashMap<>(); // not MMap because we need null keys in this case
        for(PropertyDrawEntity<?> property : hierarchy.getProperties(currentGroup))
            fillPropertyGroupIntegrationHierarchy(new PropertyPGNode(property), childGroupNodes);
        for(GroupObjectEntity group : hierarchy.getDependencies(currentGroup))
            fillPropertyGroupIntegrationHierarchy(new GroupObjectPGNode(group), childGroupNodes);
        
        // generating parse nodes recursively
        return getPropertyGroupIntegrationHierarchy(null, childGroupNodes, currentGroup, hierarchy);
    }

    private static ParseNode getPropertyGroupIntegrationHierarchy(AbstractGroup currentPropertyGroup, final Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes, final GroupObjectEntity currentGroup, final StaticDataGenerator.Hierarchy hierarchy) {
        MExclSet<PGNode> childGroups = childGroupNodes.get(currentPropertyGroup);
        ImSet<ParseNode> childNodes = childGroups.immutable().mapSetValues(new GetValue<ParseNode, PGNode>() {
            public ParseNode getMapValue(PGNode value) {
                return value.createNode(childGroupNodes, hierarchy);
            }});
        if(currentPropertyGroup == null) {
            if(currentGroup == null)
                return new FormParseNode(childNodes);
            return new GroupObjectParseNode(childNodes, currentGroup);
        }
        return new PropertyGroupParseNode(childNodes, currentPropertyGroup);
    }

    private static void fillPropertyGroupIntegrationHierarchy(PGNode node, Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes) {
        AbstractGroup parentGroup = node.getParent();
        if(parentGroup != null && parentGroup.system)
            parentGroup = null;
        MExclSet<PGNode> childGroups = childGroupNodes.get(parentGroup);
        if(childGroups == null) {
            childGroups = SetFact.mExclSet();
            childGroupNodes.put(parentGroup, childGroups);
        
            if(parentGroup != null)
                fillPropertyGroupIntegrationHierarchy(new PropertyGroupPGNode(parentGroup), childGroupNodes);
        }        
        childGroups.exclAdd(node);
    }

    public abstract <T extends Node<T>> void importNode(T node, ImMap<ObjectEntity, Object> upValues, ImportData importData);
    
    public abstract <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData);

    private interface PGNode {
        AbstractGroup getParent();

        ParseNode createNode(Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy);
    }

    private static class PropertyPGNode implements PGNode {
        public final PropertyDrawEntity<?> property;

        public PropertyPGNode(PropertyDrawEntity<?> property) {
            this.property = property;
        }

        public AbstractGroup getParent() {
            return property.group;
        }

        public ParseNode createNode(Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy) {
            return new PropertyParseNode(property);
        }
    }

    private static class GroupObjectPGNode implements PGNode {
        public final GroupObjectEntity groupObject;

        public GroupObjectPGNode(GroupObjectEntity groupObject) {
            this.groupObject = groupObject;
        }

        public AbstractGroup getParent() {
            return groupObject.propertyGroup;
        }

        public ParseNode createNode(Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy) {
            return getGroupIntegrationHierarchy(groupObject, hierarchy);
        }
    }

    private static class PropertyGroupPGNode implements PGNode {
        public final AbstractGroup group;

        public PropertyGroupPGNode(AbstractGroup group) {
            this.group = group;
        }

        public AbstractGroup getParent() {
            return group.getParent();
        }

        @Override
        public ParseNode createNode(Map<AbstractGroup, MExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy) {
            return getPropertyGroupIntegrationHierarchy(group, childGroupNodes, null, hierarchy);
        }
    }
}
