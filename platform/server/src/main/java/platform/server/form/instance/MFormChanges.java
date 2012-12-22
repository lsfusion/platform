package platform.server.form.instance;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.interop.ClassViewType;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

public class MFormChanges {

    public MExclMap<GroupObjectInstance, ClassViewType> classViews = MapFact.mExclMap();

    // value.keySet() из key.getUpTreeGroups
    public MExclMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects = MapFact.mExclMap();

    // value.keySet() из key.getUpTreeGroups
    public MExclMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects = MapFact.mExclMap();

    // value.keySet() из key, или пустой если root
    public MExclMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects = MapFact.mExclMap();

    public MExclMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties = MapFact.mExclMap();

    public MExclSet<PropertyDrawInstance> panelProperties = SetFact.mExclSet();
    public MExclSet<PropertyDrawInstance> dropProperties = SetFact.mExclSet();

    public FormChanges immutable() {

        return new FormChanges(
                classViews.immutable(),
                objects.immutable(),
                gridObjects.immutable(),
                parentObjects.immutable(),
                properties.immutable(),
                panelProperties.immutable(),
                dropProperties.immutable()
        );
    }
}
