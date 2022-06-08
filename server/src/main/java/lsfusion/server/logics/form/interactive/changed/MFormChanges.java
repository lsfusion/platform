package lsfusion.server.logics.form.interactive.changed;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;

public class MFormChanges {

    // value.keySet() из key.getUpTreeGroups
    public MExclMap<GroupObjectInstance, ImMap<ObjectInstance, ? extends ObjectValue>> objects = MapFact.mExclMap();

    // value.keySet() из key.getUpTreeGroups
    public MExclMap<GroupObjectInstance, ImOrderSet<ImMap<ObjectInstance, DataObject>>> gridObjects = MapFact.mExclMap();

    // value.keySet() из key, или пустой если root
    public MExclMap<GroupObjectInstance, ImList<ImMap<ObjectInstance, DataObject>>> parentObjects = MapFact.mExclMap();

    public MExclMap<GroupObjectInstance, ImMap<ImMap<ObjectInstance, DataObject>, Integer>> expandables = MapFact.mExclMap();

    public MExclMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties = MapFact.mExclMap();

    public MExclSet<PropertyDrawInstance> dropProperties = SetFact.mExclSet();

    public MMap<GroupObjectInstance, Boolean> updateStateObjects = MapFact.mMap(MapFact.override()); // for manual update mode, if object requires update
    
    public MList<ComponentView> activateTabs = ListFact.mList();
    public MList<PropertyDrawInstance> activateProps = ListFact.mList();
    
    public MList<ContainerView> collapseContainers = ListFact.mList();
    public MList<ContainerView> expandContainers = ListFact.mList();

    public boolean needConfirm = false;

    public FormChanges immutable() {

        return new FormChanges(
                objects.immutable(),
                gridObjects.immutable(),
                parentObjects.immutable(),
                expandables.immutable(),
                properties.immutable(),
                dropProperties.immutable(),
                updateStateObjects.immutable(),
                activateTabs.immutableList(),
                activateProps.immutableList(),
                collapseContainers.immutableList(),
                expandContainers.immutableList(),
                needConfirm
        );
    }
}
