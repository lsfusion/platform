package lsfusion.server.logics.form.stat.integration.hierarchy;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.property.group.AbstractGroup;

import java.util.HashMap;
import java.util.Map;

public abstract class ParseNode {

    public static ParseNode getIntegrationHierarchy(StaticDataGenerator.Hierarchy hierarchy) {
        return getGroupIntegrationHierarchy(hierarchy.getRoot(), hierarchy);
    }

    private static ParseNode getGroupIntegrationHierarchy(GroupObjectEntity currentGroup, StaticDataGenerator.Hierarchy hierarchy) {

        // generating used property groups hierarchy
        Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes = new HashMap<>(); // not MMap because we need null keys in this case
        childGroupNodes.put(null, SetFact.<PGNode>mOrderExclSet());
        for(PropertyDrawEntity<?> property : hierarchy.getProperties(currentGroup))
            fillPropertyGroupIntegrationHierarchy(new PropertyPGNode(property), childGroupNodes);
        for(GroupObjectEntity group : hierarchy.getDependencies(currentGroup))
            fillPropertyGroupIntegrationHierarchy(new GroupObjectPGNode(group), childGroupNodes);
        
        // generating parse nodes recursively
        return getPropertyGroupIntegrationHierarchy(null, childGroupNodes, currentGroup, hierarchy);
    }

    private static ParseNode getPropertyGroupIntegrationHierarchy(AbstractGroup currentPropertyGroup, final Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes, final GroupObjectEntity currentGroup, final StaticDataGenerator.Hierarchy hierarchy) {
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

    private static void fillPropertyGroupIntegrationHierarchy(PGNode node, Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes) {
        AbstractGroup parentGroup = node.getParent();
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
    
    public abstract <T extends Node<T>> void exportNode(T node, ImMap<ObjectEntity, Object> upValues, ExportData exportData);

    private interface PGNode {
        AbstractGroup getParent();

        ParseNode createNode(Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive);
    }

    private static class PropertyPGNode implements PGNode {
        public final PropertyDrawEntity<?> property;

        public PropertyPGNode(PropertyDrawEntity<?> property) {
            this.property = property;
        }

        public AbstractGroup getParent() {
            return property.group;
        }

        public ParseNode createNode(Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return new PropertyParseNode(property, isExclusive);
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

        public ParseNode createNode(Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
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
        public ParseNode createNode(Map<AbstractGroup, MOrderExclSet<PGNode>> childGroupNodes, StaticDataGenerator.Hierarchy hierarchy, boolean isExclusive) {
            return getPropertyGroupIntegrationHierarchy(group, childGroupNodes, null, hierarchy);
        }
    }
}
