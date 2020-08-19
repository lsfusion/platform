package lsfusion.server.logics.form.stat.struct;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.AutoFormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;

public class IntegrationFormEntity<P extends PropertyInterface> extends AutoFormEntity {

    public final GroupObjectEntity groupObject;
    public final ImRevMap<P, ObjectEntity> mapObjects;
            
    public <M extends PropertyInterface> IntegrationFormEntity(BaseLogicsModule LM, ImOrderSet<P> innerInterfaces, ImList<ValueClass> innerClasses, final ImOrderSet<P> valueInterfaces, List<String> aliases, List<Boolean> literals, ImList<PropertyInterfaceImplement<P>> properties, PropertyInterfaceImplement<P> where, ImOrderMap<String, Boolean> orders, boolean attr, Version version) throws AlreadyDefined {
        super(LocalizedString.NONAME, version);

        final ImMap<P, ValueClass> interfaceClasses;
        if(innerClasses == null) { // export
            if (where instanceof PropertyMapImplement) { // it'not clear what to do with parameter as where
                PropertyMapImplement<M, P> mapWhere = (PropertyMapImplement<M, P>) where;
                interfaceClasses = mapWhere.mapInterfaceClasses(ClassType.forPolicy); // need this for correct export action signature
            } else 
                interfaceClasses = MapFact.EMPTY();
        } else
            interfaceClasses = innerInterfaces.mapList(innerClasses);

        mapObjects = innerInterfaces.mapOrderRevValues((i, value) -> {
            ValueClass interfaceClass = interfaceClasses.get(value);
            return new ObjectEntity(genID(), interfaceClass, LocalizedString.NONAME, interfaceClass == null);
        });

        if(!valueInterfaces.isEmpty()) {
            GroupObjectEntity valueGroupObject = new GroupObjectEntity(genID(), innerInterfaces.subOrder(0, valueInterfaces.size()).mapOrder(mapObjects)); // we don't know parameter classes
            addGroupObject(valueGroupObject, version);
        }

        if(valueInterfaces.size() < innerInterfaces.size()) { // extending context
            groupObject = new GroupObjectEntity(genID(), innerInterfaces.subOrder(valueInterfaces.size(), innerInterfaces.size()).mapOrder(mapObjects)); // we don't know parameter classes
            groupObject.setSID("value"); // for JSON and XML
            addGroupObject(groupObject, version);
        } else
            groupObject = null;
        
        MAddExclMap<String, PropertyDrawEntity> mapAliases = MapFact.mAddExclMapMax(properties.size()); // for orders
        for(int i=0,size=properties.size();i<size;i++) {
            PropertyDrawEntity propertyDraw;

            Property<M> addProperty;
            ImRevMap<M, ObjectEntity> addMapping;
            PropertyInterfaceImplement<P> property = properties.get(i);
            if(property instanceof PropertyMapImplement) {
                PropertyMapImplement<M, P> mapProperty = (PropertyMapImplement<M, P>) property;
                addProperty = mapProperty.property;
                addMapping = mapProperty.mapping.join(mapObjects);
            } else {
                ObjectEntity object = mapObjects.get((P)property);
                LP<M> objValueProp = LM.getObjValueProp(this, object);
                addProperty = objValueProp.property;
                addMapping = objValueProp.getRevMap(object);
            }

            propertyDraw = addPropertyDraw(addProperty, addMapping, version);
            
            String alias = aliases.get(i);
            if(alias != null) {
                if(literals.get(i)) {
                    propertyDraw.setIntegrationSID(alias);
                    alias = null;
                } else
                    mapAliases.exclAdd(alias, propertyDraw);
            } else {
                if(!addProperty.isNamed() && (properties.size() - orders.size()) == 1) // if there is only one property, without name, setting default name - value
                    alias = "value";
            }
            setFinalPropertyDrawSID(propertyDraw, alias);

            propertyDraw.group = Group.NOGROUP; // without group

            if(groupObject != null && !addMapping.valuesSet().intersect(groupObject.getObjects()))
                propertyDraw.toDraw = groupObject;

            if(attr)
                propertyDraw.attr = true;
        }

        if(where instanceof PropertyMapImplement) { // it'not clear what to do with parameter as where
            PropertyMapImplement<M, P> mapWhere = (PropertyMapImplement<M, P>) where;
            addFixedFilter(new FilterEntity<>(addPropertyObject(mapWhere.property, mapWhere.mapping.join(mapObjects))), version);
        }
        
        for(int i=0,size=orders.size();i<size;i++) {
            PropertyDrawEntity property = mapAliases.get(orders.getKey(i));
            property.setIntegrationSID(null);
            addDefaultOrder(property, orders.getValue(i), version);
        }

        finalizeInit(version);
    }

    @Override
    public boolean noClasses() {
        return true;
    }
}
