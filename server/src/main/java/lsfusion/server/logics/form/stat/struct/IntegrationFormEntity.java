package lsfusion.server.logics.form.stat.struct;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.struct.AutoFormEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class IntegrationFormEntity<P extends PropertyInterface> extends AutoFormEntity {

    public final GroupObjectEntity groupObject;
    public final ImRevMap<P, ObjectEntity> mapObjects;

    private final boolean interactive;

    public <M extends PropertyInterface> IntegrationFormEntity(BaseLogicsModule LM, ImOrderSet<P> innerInterfaces, ImList<ValueClass> explicitInnerClasses, final ImOrderSet<P> valueInterfaces, ImList<PropertyInterfaceImplement<P>> properties, ImList<ScriptingLogicsModule.IntegrationPropUsage> propUsages,
                                                               PropertyInterfaceImplement<P> where, ImOrderMap<String, Boolean> orders, boolean attr, boolean interactive, Version version) {
        super(interactive, version);

        this.interactive = interactive;

        // remove null
        ImMap<P, ValueClass> interfaceClasses = Property.getExplicitCalcInterfaces(innerInterfaces.getSet(), explicitInnerClasses != null ? innerInterfaces.mapList(explicitInnerClasses) : null, () -> {
            if (where instanceof PropertyMapImplement) // it'not clear what to do with parameter as where
                return ((PropertyMapImplement<M, P>) where).mapInterfaceClasses(ClassType.forPolicy);
            return MapFact.EMPTY();
        }, this, null);

        mapObjects = innerInterfaces.mapOrderRevValues((i, value) -> {
            ValueClass interfaceClass = interfaceClasses.get(value);
            return new ObjectEntity(genID, interfaceClass);
        });

        if(!valueInterfaces.isEmpty()) {
            GroupObjectEntity valueGroupObject = addGroupObjectEntity(LM, innerInterfaces.subOrder(0, valueInterfaces.size()).mapOrder(mapObjects), version); // we don't know parameter classes

            valueGroupObject.setViewType(ClassViewType.PANEL, this, version); // for interactive view
        }

        if(valueInterfaces.size() < innerInterfaces.size()) { // extending context
            // sID - for JSON and XML
            groupObject = addGroupObjectEntity(LM, innerInterfaces.subOrder(valueInterfaces.size(), innerInterfaces.size()).mapOrder(mapObjects), version); // we don't know parameter classes
            groupObject.setSID("value");
            groupObject.setListViewType(ListViewType.CUSTOM, version);
            groupObject.setCustomRenderFunction("selectMultiInput", version);
        } else
            groupObject = null;
        
        MAddExclMap<String, PropertyDrawEntity> mapAliases = MapFact.mAddExclMapMax(properties.size()); // for orders
        for(int i=0,size=properties.size();i<size;i++) {
            PropertyDrawEntity propertyDraw;
            ScriptingLogicsModule.IntegrationPropUsage propUsage = propUsages.get(i);

            boolean isNamed = false;
            ImSet<ObjectEntity> addObjects;
            PropertyInterfaceImplement<P> property = properties.get(i);
            if(property instanceof PropertyMapImplement) {
                PropertyMapImplement<M, P> mapProperty = (PropertyMapImplement<M, P>) property;
                Property<M> addProperty = mapProperty.property;
                ImRevMap<M, ObjectEntity> addMapping = mapProperty.mapping.join(mapObjects);
                propertyDraw = addPropertyDraw(addProperty, propUsage.inherited, propUsage.listInterfaces, addMapping, version);
                addObjects = addMapping.valuesSet();
                isNamed = addProperty.isNamed();
            } else {
                ObjectEntity object = mapObjects.get((P)property);
                propertyDraw = addValuePropertyDraw(LM, object, version);
                addObjects = SetFact.singleton(object);
                propertyDraw.setSID(object.getSID()); // important for SELECT change json somewhy
            }

            String alias = propUsage.alias;
            if(alias != null) {
                if(propUsage.literal) {
                    propertyDraw.setIntegrationSID(alias, version);
                    alias = null;
                } else
                    mapAliases.exclAdd(alias, propertyDraw);
            } else {
                if(!isNamed && (properties.size() - orders.size()) == 1) // if there is only one property, without name, setting default name - value
                    alias = "value";
            }
            if(alias != null)
                propertyDraw.setSID(alias);

            propertyDraw.setGroup(propUsage.group, this, version);

            if(groupObject != null && !addObjects.intersect(groupObject.getObjects()))
                propertyDraw.setToDraw(groupObject, this, version);

            if(attr)
                propertyDraw.setAttr(true, version);
        }

        if(where instanceof PropertyMapImplement) { // it'not clear what to do with parameter as where
            PropertyMapImplement<M, P> mapWhere = (PropertyMapImplement<M, P>) where;
            addFixedFilter(new FilterEntity<>(addPropertyObject(mapWhere.property, mapWhere.mapping.join(mapObjects))), version);
        }
        
        for(int i=0,size=orders.size();i<size;i++) {
            PropertyDrawEntity property = mapAliases.get(orders.getKey(i));
            property.setIntegrationSID(PropertyDrawEntity.NOEXTID, version);
            property.setPropertyExtra(addPropertyObject(LM.vnull), PropertyDrawExtraType.SHOWIF, version); // for interactive view
            addDefaultOrder(property, orders.getValue(i), version);
        }

        finalizeInit(version);

        if(interactive) {
            // for interactive view
            DefaultFormView formView = (DefaultFormView) view;

//        OBJECTS {
//            border = FALSE;
//            class = '';
//        }
            ContainerView objectsContainer = formView.getBoxContainer(formView);
            objectsContainer.setBorder(false, version);
            objectsContainer.setElementClass(null, version);

//        BOX(i) {
//            caption = NULL;
//        }
//        REMOVE TOOLBARBOX(i);

            if (groupObject != null) {
                ContainerView boxContainer = formView.getBoxContainer(groupObject);
                boxContainer.setCaption(null, version);

                ContainerView toolbarBoxContainer = formView.getToolbarBoxContainer(groupObject);
                formView.removeComponent(toolbarBoxContainer, version);
            }
        }
    }

    @Override
    protected void finalizeDesignAroundInit() {
        if(interactive)
            super.finalizeDesignAroundInit();
    }

    @Override
    public void prereadAutoIcons(ConnectionContext context) {
        if(interactive)
            super.prereadAutoIcons(context);
    }

    @Override
    public LocalizedString getCaption() {
        if(interactive)
            return super.getCaption();
        return LocalizedString.NONAME;
    }

    @Override
    public void prereadEventActions() {
        if(interactive)
            super.prereadEventActions();
    }

    @Override
    public boolean noClasses() {
        return true;
    }
}
