package lsfusion.server.logics.property.actions.integration;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.*;

import java.util.List;

public class IntegrationFormEntity<P extends PropertyInterface> extends FormEntity {

    public final GroupObjectEntity groupObject;
    public final ImRevMap<P, ObjectEntity> mapObjects;
            
    public <M extends PropertyInterface> IntegrationFormEntity(BaseLogicsModule LM, ImOrderSet<P> innerInterfaces, final ValueClass interfaceClass, final ImOrderSet<P> valueInterfaces, List<String> aliases, List<Boolean> literals, ImList<CalcPropertyInterfaceImplement<P>> properties, CalcPropertyInterfaceImplement<P> where, ImOrderMap<String, Boolean> orders, boolean attr, Version version) throws AlreadyDefined {
        super("Export.export", LocalizedString.NONAME, version);

        final ImMap<P, ValueClass> interfaceClasses;
        if(where instanceof CalcPropertyMapImplement) { // it'not clear what to do with parameter as where
            CalcPropertyMapImplement<M, P> mapWhere = (CalcPropertyMapImplement<M, P>) where;
            interfaceClasses = mapWhere.mapInterfaceClasses(ClassType.forPolicy); // need this for correct export action signature
        } else
            interfaceClasses = MapFact.EMPTY();

        mapObjects = innerInterfaces.mapOrderRevValues(new GetIndexValue<ObjectEntity, P>() {
            public ObjectEntity getMapValue(int i, P value) {
                ValueClass interfaceClass = interfaceClasses.get(value);
                return new ObjectEntity(genID(), interfaceClass, LocalizedString.NONAME);
            }});

        if(!valueInterfaces.isEmpty()) {
            GroupObjectEntity valueGroupObject = new GroupObjectEntity(genID(), innerInterfaces.subOrder(0, valueInterfaces.size()).mapOrder(mapObjects), true); // we don't know parameter classes
            addGroupObject(valueGroupObject, version);
        }

        if(valueInterfaces.size() < innerInterfaces.size()) { // extending context
            groupObject = new GroupObjectEntity(genID(), innerInterfaces.subOrder(valueInterfaces.size(), innerInterfaces.size()).mapOrder(mapObjects), interfaceClass == null); // we don't know parameter classes
            groupObject.setSID("value"); // for JSON
            addGroupObject(groupObject, version);
        } else
            groupObject = null;
        
        MAddExclMap<String, PropertyDrawEntity> mapAliases = MapFact.mAddExclMapMax(properties.size()); // for orders
        for(int i=0,size=properties.size();i<size;i++) {
            PropertyDrawEntity propertyDraw;

            CalcProperty<M> addProperty;
            ImRevMap<M, ObjectEntity> addMapping;
            CalcPropertyInterfaceImplement<P> property = properties.get(i);
            if(property instanceof CalcPropertyMapImplement) {
                CalcPropertyMapImplement<M, P> mapProperty = (CalcPropertyMapImplement<M, P>) property;
                addProperty = mapProperty.property;
                addMapping = mapProperty.mapping.join(mapObjects);
            } else {
                ObjectEntity object = mapObjects.get((P)property);
                LCP<M> objValueProp = LM.getObjValueProp(this, object);
                addProperty = objValueProp.property;
                addMapping = objValueProp.getRevMap(object);
            }

            propertyDraw = addPropertyDraw(addProperty, addMapping, version);
            propertyDraw.group = null; // without group 

            if(groupObject != null && !addMapping.valuesSet().intersect(groupObject.getObjects()))
                propertyDraw.applyObject = groupObject.getOrderObjects().get(0);

            if(attr)
                propertyDraw.attr = true;
            
            String alias = aliases.get(i);
            if(alias != null) {
                if(literals.get(i)) {
                    propertyDraw.setIntegrationSID(alias);
                    alias = null;
                }
                mapAliases.exclAdd(alias, propertyDraw);
            }
            setFinalPropertyDrawSID(propertyDraw, alias);
        }

        if(where instanceof CalcPropertyMapImplement) { // it'not clear what to do with parameter as where
            CalcPropertyMapImplement<M, P> mapWhere = (CalcPropertyMapImplement<M, P>) where;
            addFixedFilter(new FilterEntity<>(addPropertyObject(mapWhere.property, mapWhere.mapping.join(mapObjects))), version);
        }
        
        for(int i=0,size=orders.size();i<size;i++) {
            addDefaultOrder(mapAliases.get(orders.getKey(i)), orders.getValue(i), version);
        }
    }

    @Override
    public boolean noClasses() {
        return true;
    }
}
