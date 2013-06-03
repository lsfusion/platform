package lsfusion.client.descriptor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.filter.*;
import lsfusion.client.serialization.ClientCustomSerializable;

import java.util.List;

public interface OrderDescriptor extends ClientCustomSerializable {

    GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups);

    public static String[] derivedNames = new String[]{ClientResourceBundle.getString("descriptor.filter.comparison.comparison"), ClientResourceBundle.getString("descriptor.filter.defined.defined"), ClientResourceBundle.getString("descriptor.filter.class.class"), ClientResourceBundle.getString("descriptor.filter.negation"), ClientResourceBundle.getString("descriptor.filter.or.or")};
    public static Class[] derivedClasses = new Class[]{CompareFilterDescriptor.class, NotNullFilterDescriptor.class, IsClassFilterDescriptor.class, NotFilterDescriptor.class, OrFilterDescriptor.class};
}
