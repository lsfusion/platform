package lsfusion.server.form.instance;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.ClassViewType;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

public class MFormChanges {

    public MExclMap<GroupObjectInstance, ClassViewType> classViews = MapFact.mExclMap();

    // value.keySet() из key.getUpTreeGroups
    public MExclMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects = MapFact.mExclMap();

    // value.keySet() из key.getUpTreeGroups
    public MExclMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects = MapFact.mExclMap();

    // value.keySet() из key, или пустой если root
    public MExclMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects = MapFact.mExclMap();

    public MExclMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Boolean>> expandables = MapFact.mExclMap();

    public MExclMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties = MapFact.mExclMap();

    public MExclSet<PropertyDrawInstance> panelProperties = SetFact.mExclSet();
    public MExclSet<PropertyDrawInstance> dropProperties = SetFact.mExclSet();

    public FormChanges immutable() {

        return new FormChanges(
                classViews.immutable(),
                objects.immutable(),
                gridObjects.immutable(),
                parentObjects.immutable(),
                expandables.immutable(),
                properties.immutable(),
                panelProperties.immutable(),
                dropProperties.immutable()
        );
    }
}
