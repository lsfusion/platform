package platform.server.form.instance;

import java.util.List;
import java.util.Set;

public interface PropertyReadInstance {

    public PropertyObjectInstance getPropertyObject();

    public byte getTypeID();

    public int getID(); // ID в рамках Type

    public List<ObjectInstance> getSerializeList(Set<PropertyDrawInstance> panelProperties);
}
