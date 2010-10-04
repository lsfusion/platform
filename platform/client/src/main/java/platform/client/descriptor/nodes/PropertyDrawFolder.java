package platform.client.descriptor.nodes;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class PropertyDrawFolder extends DefaultMutableTreeNode {

    public PropertyDrawFolder(GroupObjectDescriptor group, List<PropertyDrawDescriptor> propertyDraws) {
        super("Свойства");

        for (PropertyDrawDescriptor propertyDraw : propertyDraws)
            if (group.equals(propertyDraw.toDraw)) {
                add(new PropertyDrawNode(propertyDraw));
            }
    }
}
