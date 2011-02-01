package platform.server.form.instance;

import java.util.List;
import java.util.Set;

public interface PropertyReaderInstance {

    public PropertyObjectInstance getPropertyObjectInstance();

    public byte getTypeID();

    public int getID(); // ID в рамках Type

    public List<ObjectInstance> getKeysObjectsList(Set<PropertyReaderInstance> panelProperties);
}
