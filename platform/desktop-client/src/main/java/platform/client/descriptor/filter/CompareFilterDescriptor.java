package platform.client.descriptor.filter;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.*;
import platform.client.descriptor.nodes.filters.CompareFilterNode;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.Compare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CompareFilterDescriptor extends PropertyFilterDescriptor {
    private Compare compare;

    public Compare getCompare() {
        return compare;
    }

    public void setCompare(Compare compare) {
        this.compare = compare;

        updateDependency(this, "compare");
    }

    private OrderDescriptor value;

    public OrderDescriptor getValue() {
        return value;
    }
    public void setValue(OrderDescriptor value) {
        this.value = value;

        updateDependency(this, "value");
    }

    @Override
    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (value == null) return null;
        return getDownGroup(super.getGroupObject(groupList), value.getGroupObject(groupList), groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        compare.serialize(outStream);
        pool.serializeObject(outStream, value);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        compare = Compare.deserialize(inStream);
        value = (OrderDescriptor) pool.deserializeObject(inStream);
    }

    @Override
    public CompareFilterNode createNode(Object group) {
        return new CompareFilterNode((GroupObjectDescriptor) group, this);
    }

    @Override
    public String toString() {
        String result = "";
        if (property != null)
            result += property;
        if (compare != null)
            result += " " + compare;
        if (value != null)
            result += " " + value;
        if (result.isEmpty()) result = ClientResourceBundle.getString("descriptor.filter.comparison");
        return result;
    }

    public String getCodeConstructor() {
        String code = "new CompareFilterEntity(";
        
        code += "addPropertyObject(" + property.property.code;
        for (PropertyInterfaceDescriptor pid : property.mapping.keySet()) {
            code += ", " + property.mapping.get(pid).getInstanceCode();
        }
        code += "), Compare." + compare.name() + ", ";
        code += value.getInstanceCode() + ")";
        return code;
    }
}
