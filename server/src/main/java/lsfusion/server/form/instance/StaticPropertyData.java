package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.*;

// in static presentation column-based (property -> keys) map is used (instead of row-based (keys -> properties) in interactive)  
public class StaticPropertyData<Prop extends PropertyReaderEntity> {
    public final MAddExclMap<Prop, ImSet<ObjectEntity>> objects = MapFact.mAddExclMap();
    public final MAddExclMap<Prop, ImMap<ImMap<ObjectEntity, Object>, Object>> data = MapFact.mAddExclMap();
    public final MAddExclMap<Prop, Type> types = MapFact.mAddExclMap();
    
    public final MAddExclMap<PropertyDrawEntity, Pair<ImSet<ObjectEntity>, ImSet<ObjectEntity>>> columnObjects = MapFact.mAddExclMap();
    public final MAddExclMap<PropertyDrawEntity, ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>>> columnData = MapFact.mAddExclMap();

    public static Object getProperty(StaticPropertyData<PropertyDrawEntity> propData, PropertyDrawEntity<?> entity, ImMap<ObjectEntity, Object> keys) {
        return propData.data.get(entity).get(keys.filterIncl(propData.objects.get(entity)));
    }

    public void add(ImSet<ObjectEntity> objects, ImMap<Prop, ImMap<ImMap<ObjectEntity, Object>, Object>> data, ImMap<Prop, Type> propTypes, ImSet<ObjectEntity> parentColumnObjects, ImSet<ObjectEntity> thisColumnObjects, final ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>> columnData) {
        this.objects.exclAddAll(data.keys().toMap(objects));
        this.data.exclAddAll(data);
        this.types.exclAddAll(propTypes);

        // filtering only main properties (without extra info)
        ImSet<PropertyDrawEntity> propertyDraws = BaseUtils.immutableCast(data.keys().filterFn(new SFunctionSet<Prop>() {
                               public boolean contains(Prop element) {
                                   return element instanceof PropertyDrawEntity;
                               }
                           }));
        this.columnObjects.exclAddAll(propertyDraws.toMap(new Pair<>(parentColumnObjects, thisColumnObjects)));
        this.columnData.exclAddAll(propertyDraws.toMap(columnData));
    }
}
