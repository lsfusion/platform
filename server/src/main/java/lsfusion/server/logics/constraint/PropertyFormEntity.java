package lsfusion.server.logics.constraint;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.AutoFinalFormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyFormEntity extends AutoFinalFormEntity {

    public <P extends PropertyInterface, X extends PropertyInterface> PropertyFormEntity(BaseLogicsModule LM, Property<P> property, Property<X> messageProperty, ImList<PropertyMapImplement<?,P>> properties) {
        super(property.caption, LM);

        addPropertyDraw(messageProperty, MapFact.EMPTYREV());

        ImMap<P,ValueClass> interfaceClasses = property.getInterfaceClasses(ClassType.logPolicy);

        ImRevMap<P, ObjectEntity> mapObjects = interfaceClasses.mapRevValues((ValueClass value) -> {
            // need to specify baseClass anyway, because we need it when adding recognizeGroup
            return new ObjectEntity(genID(), value, value.getCaption(), true); // because heuristics can be incorrect, but we don't need classes (to be more specific, when there is DROPPED operator)
        });

        ImOrderSet<ObjectEntity> objects;
        if(mapObjects.isEmpty()) // not to create GroupObjectEntity with no objects
            objects = SetFact.EMPTYORDER();
        else {
            GroupObjectEntity groupObject = new GroupObjectEntity(genID(), mapObjects.valuesSet().toOrderSet());
            addGroupObject(groupObject);

            objects = groupObject.getOrderObjects();
        }

        if(properties.isEmpty()) {
            for(ObjectEntity object : objects)
                addValuePropertyDraw(LM, object);
            addPropertyDraw(LM.getRecognizeGroup(), property.usePrevHeur(), objects);
        } else {
            for (PropertyMapImplement prop : properties) {
                addPropertyDraw(prop.property, prop.mapping.join(mapObjects));
            }
        }

        addFixedFilter(new FilterEntity<>(new PropertyObjectEntity<>(property, mapObjects)));

        finalizeInit();
    }
}
