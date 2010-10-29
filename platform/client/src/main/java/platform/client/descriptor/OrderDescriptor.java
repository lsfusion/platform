package platform.client.descriptor;

import platform.client.descriptor.filter.*;
import platform.client.serialization.ClientCustomSerializable;

import java.util.List;

public interface OrderDescriptor extends ClientCustomSerializable {

    GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groups);

    public static String[] derivedNames = new String[]{"Сравнение", "Определено", "Класс", "Отрицание", "Или"};
    public static Class[] derivedClasses = new Class[]{CompareFilterDescriptor.class, NotNullFilterDescriptor.class, IsClassFilterDescriptor.class, NotFilterDescriptor.class, OrFilterDescriptor.class};
}
