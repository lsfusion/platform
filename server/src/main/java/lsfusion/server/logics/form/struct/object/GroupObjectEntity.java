package lsfusion.server.logics.form.struct.object;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntityInstance;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.IntFunction;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance> {

    public static int PAGE_SIZE_DEFAULT_VALUE = 50;

    public TreeGroupEntity treeGroup;

    public boolean isInTree() {
        return treeGroup != null;
    }

    public boolean isSubReport;
    public PropertyObjectEntity<?> reportPathProp;

    private DebugInfo.DebugPoint debugPoint;
    
    public UpdateType updateType;
    
    public Group propertyGroup; // used for integration (export / import)

    private Pair<Integer, Integer> scriptIndex;

    private String integrationSID;
    private boolean integrationKey; // key (key in JSON, tag in XML, fields in plain formats) or index (array in JSON, multiple object name tags in xml, order in plain formats)

    public boolean isIndex() {
        return !integrationKey;
    }

    public void setIntegrationKey(boolean integrationKey) {
        this.integrationKey = integrationKey;
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
        this.scriptIndex = Pair.create(debugPoint.getScriptLine(), debugPoint.offset);
    }

    public DebugInfo.DebugPoint getDebugPoint() {
        return debugPoint;
    }

    private static class UpStaticParamsProcessor extends GroupObjectInstance.FilterProcessor {
        public UpStaticParamsProcessor(GroupObjectInstance groupObject) {
            super(groupObject);
        }

        public ImSet<FilterInstance> getFilters() {
            return groupObject.getFixedFilters(true, true); // we can't combine filters (see check below)
        }

        public Where process(FilterInstance filt, ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
            return super.process(filt, MapFact.addExcl(mapKeys, filt.getObjects().remove(mapKeys.keys()).mapValues((ObjectInstance value) -> value.entity.getParamExpr())), modifier, reallyChanged);
        }
    }
    private static class NoUpProcessor extends GroupObjectInstance.FilterProcessor {
        public NoUpProcessor(GroupObjectInstance groupObject) {
            super(groupObject);
        }

        public ImSet<FilterInstance> getFilters() {
            return groupObject.getFixedFilters(true, true); // we can't combine filters (see check below)
        }

        public Where process(FilterInstance filt, ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
            if(!groupObject.getOrderObjects().getSet().containsAll(filt.getObjects())) // если есть "внешние" объекты исключаем
                return null;
            return super.process(filt, mapKeys, modifier, reallyChanged);
        }
    }

    // конечно не очень красивое решение с groupObject, но в противном случае пришлось бы дублировать логику определения GroupObject'ов для фильтров +
    @ManualLazy
    public UpdateType getUpdateType(GroupObjectInstance groupObject) throws SQLException, SQLHandledException {
        if(updateType == null) { // default
            if(!Settings.get().isDisableUpdateTypeHeur()) {
                // либо исключающий (для верхних групп объектов) фильтр, либо узкий (с низкой статистикой при "статичных" верхних объектах)
                // в частности в таком случае нет смысла seek делать
                Modifier modifier = Property.defaultModifier;

                ImRevMap<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();
                Where dynamicWhere = groupObject.getWhere(mapKeys, modifier, null, new UpStaticParamsProcessor(groupObject));
                Where dynamicWhereAlt = groupObject.getWhere(mapKeys, modifier, null, new UpStaticParamsProcessor(groupObject));

                boolean narrow = false;
                if (dynamicWhere.means(dynamicWhereAlt.not())) // если из одного условия следует, что при других object'ах объекты не могут повториться очевидно искать ничего не надо
                    narrow = true;
                else {
                    Where staticWhere = groupObject.getWhere(mapKeys, modifier, null, new NoUpProcessor(groupObject));

                    // сравниваем статистику фильтра со статистикой класса
                    StatType type = StatType.UPDATE;
                    Stat filterStat = dynamicWhere.and(staticWhere).getStatKeys(mapKeys.valuesSet(), type).getRows();
                    Stat classStat = staticWhere.getStatKeys(mapKeys.valuesSet(), type).getRows();

                    if (new Stat(Settings.get().getDivStatUpdateTypeHeur()).lessEquals(classStat.div(filterStat)))
                        narrow = true;
                }
                if (narrow)
                    updateType = UpdateType.FIRST;
            }

            if(updateType == null)
                updateType = UpdateType.PREV;
        }
        return updateType;

    }

    public GroupObjectEntity(int ID, TreeGroupEntity treeGroup) {
        this(ID, (String)null);
        this.treeGroup = treeGroup; // нужно чтобы IsInTree правильно определялось в addScriptingTreeGroupObject, когда идет addGroupObjectView
    }

    private Property<?> listViewTypeProp;
    public Property<?> getListViewType(ConcreteCustomClass listViewType) {
        if (listViewTypeProp == null) {
            listViewTypeProp = PropertyFact.createDataPropRev("LIST VIEW TYPE", this, listViewType);
        }
        return listViewTypeProp;
    }

    public GroupObjectEntity(int ID, String sID) {
        super(ID, sID != null ? sID : "groupObj" + ID);
    }

    public ClassViewType viewType = ClassViewType.DEFAULT;
    public ListViewType listViewType = ListViewType.DEFAULT;
    public PivotOptions pivotOptions;
    public String customRenderFunction;
    public PropertyObjectEntity<?> propertyCustomOptions;
    public String mapTileProvider;

    // for now will use async init since pivot is analytics and don't need for example focuses and can afford extra round trip
    public boolean asyncInit = true; // so far supported only for pivot

    public Integer pageSize;

    public PropertyObjectEntity<?> propertyBackground;
    public PropertyObjectEntity<?> propertyForeground;

    public boolean isFilterExplicitlyUsed; // optimization hack - there are a lot of FILTER usages by group change, but group change needs FILTER only when group (grid) is visible and refreshed, so we do filter update only if the latter condition is matched
    public boolean isOrderExplicitlyUsed; // optimization hack - there are a lot of ORDER usages by group change, but group change needs ORDER only when group (grid) is visible and refreshed, so we do filter update only if the latter condition is matched

    private boolean finalizedProps = false;
    private Object props = MapFact.mExclMap();
    @NFLazy
    public PropertyRevImplement<ClassPropertyInterface, ObjectEntity> getProperty(GroupObjectProp type) {
        if(finalizedProps)
            return getProperties().get(type);

        assert finalizedObjects;
        MExclMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectEntity>> mProps = (MExclMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectEntity>>) props;
        PropertyRevImplement<ClassPropertyInterface, ObjectEntity> prop = mProps.get(type);
        if(prop==null) { // type.getSID() + "_" + getSID() нельзя потому как надо еще SID формы подмешивать
            prop = PropertyFact.createDataPropRev(type.toString(), objects, getObjects().mapValues(new Function<ObjectEntity, ValueClass>() {
                public ValueClass apply(ObjectEntity value) {
                    return value.baseClass;
                }}), type.getValueClass(), null);
            mProps.exclAdd(type, prop);
        }
        return prop;
    }

    @NFLazy
    public ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectEntity>> getProperties() {
        if(!finalizedProps) {
            props = ((MExclMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectEntity>>) props).immutable();
            finalizedProps = true;
        }
        return (ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectEntity>>) props;
    }

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public ImMap<ObjectEntity, PropertyObjectEntity<?>> isParent = null;

    public void setIsParents(final PropertyObjectEntity... properties) {
        isParent = getOrderObjects().mapOrderValues((IntFunction<PropertyObjectEntity<?>>) i -> properties[i]);
    }

    public void setViewType(ClassViewType viewType) {
        this.viewType = viewType;
    }

    public void setListViewType(ListViewType listViewType) {
        this.listViewType = listViewType;
    }

    public void setPivotOptions(PivotOptions pivotOptions) {
        if(this.pivotOptions != null) {
            this.pivotOptions.merge(pivotOptions);
        } else {
            this.pivotOptions = pivotOptions;
        }
    }

    public void setCustomRenderFunction(String customRenderFunction) {
        this.customRenderFunction = customRenderFunction;
    }

    public void setCustomOptions(PropertyObjectEntity<?> propertyCustomOptions) {
        this.propertyCustomOptions = propertyCustomOptions;
    }

    public void setMapTileProvider(String mapTileProvider) {
        this.mapTileProvider = mapTileProvider;
    }

    public void setViewTypePanel() {
        setViewType(ClassViewType.PANEL);
    }
    
    public void setViewTypeList() {
        setViewType(ClassViewType.LIST);
    }

    public boolean isPanel() {
        return viewType.isPanel();
    }

    public boolean isCustom() {
        return !isPanel() && listViewType == ListViewType.CUSTOM;
    }

    public boolean isSimpleState() {
        return !isPanel() && (listViewType == ListViewType.CUSTOM || listViewType == ListViewType.MAP || listViewType == ListViewType.CALENDAR);
    }

    public Pair<Integer, Integer> getScriptIndex() {
        return scriptIndex;
    }

    public void setPropertyBackground(PropertyObjectEntity<?> propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(PropertyObjectEntity<?> propertyForeground) {
        this.propertyForeground = propertyForeground;
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : getSID();
    }   

    private boolean finalizedObjects;
    private Object objects = SetFact.mOrderExclSet();

    public ImSet<ObjectEntity> getObjects() {
        return getOrderObjects().getSet();
    }
    @LongMutable
    public ImOrderSet<ObjectEntity> getOrderObjects() {
        if(!finalizedObjects) {
            finalizedObjects = true;
            objects = ((MOrderExclSet<ObjectEntity>)objects).immutableOrder();
        }

        return (ImOrderSet<ObjectEntity>)objects;
    }

    public void add(ObjectEntity objectEntity) {
        assert !finalizedObjects;
        objectEntity.groupTo = this;
        ((MOrderExclSet<ObjectEntity>)objects).exclAdd(objectEntity);
    }
    
    public void setObjects(ImOrderSet<ObjectEntity> objects) {
        assert !finalizedObjects;
        finalizedObjects = true;
        this.objects = objects;
        for(ObjectEntity object : objects)
            object.groupTo = this;
    }

    public GroupObjectEntity(int ID, ImOrderSet<ObjectEntity> objects) {
        this(ID, (String)null);

        setObjects(objects);
    }


    @Override
    public String toString() {
        return getSID() + ": " + objects;
    }

    public static ImSet<ObjectEntity> getObjects(ImSet<GroupObjectEntity> groups) {
        MExclSet<ObjectEntity> mResult = SetFact.mExclSet();
        for(GroupObjectEntity group : groups)
            mResult.exclAddAll(group.getObjects());
        return mResult.immutable();
    }

    public static ImOrderSet<ObjectEntity> getOrderObjects(ImOrderSet<GroupObjectEntity> groups) {
        MOrderExclSet<ObjectEntity> mResult = SetFact.mOrderExclSet();
        for(GroupObjectEntity group : groups)
            mResult.exclAddAll(group.getOrderObjects());
        return mResult.immutableOrder();
    }
    
    private static Where getFilterWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier, ImSet<? extends FilterEntityInstance> filters) throws SQLException, SQLHandledException {
        Where where = Where.TRUE();
        for(FilterEntityInstance filt : filters)
            where = where.and(filt.getWhere(mapKeys, modifier));
        return where;
    }
    private <P extends PropertyInterface> InputFilterEntity<?, P> getFilterInputFilterEntity(ImSet<ContextFilterEntity<?, P, ObjectEntity>> filters, ImRevMap<ObjectEntity, P> mapObjects) {
        // assert single and filters objects contain this object
        InputFilterEntity<?, P> result = null;
        for(ContextFilterEntity<?, P, ObjectEntity> filt : filters)
            result = InputFilterEntity.and(result, filt.getInputFilterEntity(getObjects().single(), mapObjects));
        return result;
    }
    private <T extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, T> getFilterWhereProperty(ImSet<FilterEntity> filters, ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters, ImRevMap<P, T> mapValues, ImRevMap<ObjectEntity, T> mapObjects) {
        MList<PropertyMapImplement<?, T>> mList = ListFact.mList();
        for(FilterEntity filter : filters)
            mList.add(filter.getImplement(mapObjects));
        for(ContextFilterEntity<?, P, ObjectEntity> contextFilter : contextFilters)
            mList.add(contextFilter.getWhereProperty(mapValues, mapObjects));
        ImList<PropertyMapImplement<?, T>> list = mList.immutableList();
        if(list.isEmpty())
            return null;
        return PropertyFact.createAnd(list.getCol());
    }

    private static ImMap<ObjectEntity, ValueClass> getGridClasses(ImSet<ObjectEntity> objects) {
        return objects.filterFn(element -> !element.noClasses()).mapValues((ObjectEntity value) -> value.baseClass);
    }
    public Where getClassWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return IsClassProperty.getWhere(getGridClasses(getObjects()), mapKeys, modifier, null);
    }
    public <P extends PropertyInterface> InputFilterEntity<?, P> getClassInputFilterEntity() {
        return new InputFilterEntity<>(IsClassProperty.getProperty(getGridClasses(getObjects())).property, MapFact.EMPTYREV());
    }
    public <P extends PropertyInterface> PropertyMapImplement<?, P> getClassWhereProperty(ImRevMap<ObjectEntity, P> mapObjects) {
        return IsClassProperty.getProperty(getGridClasses(getObjects())).mapPropertyImplement(mapObjects);
    }

    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier, ImSet<? extends FilterEntityInstance> filters) throws SQLException, SQLHandledException {
        return getFilterWhere(mapKeys, modifier, filters).and(getClassWhere(mapKeys, modifier));
    }
    public <P extends PropertyInterface> InputFilterEntity<?, P> getInputFilterEntity(ImSet<ContextFilterEntity<?, P, ObjectEntity>> filters, ImRevMap<ObjectEntity, P> mapObjects) {
        return InputFilterEntity.and(getFilterInputFilterEntity(filters, mapObjects), getClassInputFilterEntity());
    }
    public <T extends PropertyInterface, P extends PropertyInterface> PropertyMapImplement<?, T> getWhereProperty(ImSet<FilterEntity> filters, ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters, ImRevMap<P, T> mapValues, ImRevMap<ObjectEntity, T> mapObjects) {
        PropertyMapImplement<?, T> classWhereProperty = getClassWhereProperty(mapObjects);
        PropertyMapImplement<?, T> filterWhereProperty = getFilterWhereProperty(filters, contextFilters, mapValues, mapObjects);
        if(filterWhereProperty == null)
            return classWhereProperty;
        return PropertyFact.createAnd(filterWhereProperty, classWhereProperty);
    }

    // hack where ImMap used (it does not support null keys)
    private GroupObjectEntity() {
    }
    public static final GroupObjectEntity NULL = new GroupObjectEntity();

    public boolean isSimpleList() {
        return getObjects().size() == 1 && viewType.isList() && !isInTree();
    }
}
