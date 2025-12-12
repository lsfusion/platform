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
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.struct.FormEntity;
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

import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance> {

    public TreeGroupEntity treeGroup;

    public boolean isInTree() {
        return treeGroup != null;
    }

    private NFProperty<Boolean> isSubReport = NFFact.property();
    private NFProperty<PropertyObjectEntity> reportPathProp = NFFact.property();

    private NFProperty<DebugInfo.DebugPoint> debugPoint = NFFact.property();
    private NFProperty<Pair<Integer, Integer>> scriptIndex = NFFact.property();

    public UpdateType updateType; //todo?

    private NFProperty<Group> propertyGroup = NFFact.property(); // used for integration (export / import)

    private NFProperty<Boolean> enableManualUpdate = NFFact.property();

    private NFProperty<String> integrationSID = NFFact.property();
    private NFProperty<Boolean> integrationKey = NFFact.property(); // key (key in JSON, tag in XML, fields in plain formats) or index (array in JSON, multiple object name tags in xml, order in plain formats)

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

    public GroupObjectEntity(int ID, TreeGroupEntity treeGroup, BaseLogicsModule LM) {
        this(ID, (String)null, LM);
        this.treeGroup = treeGroup; // нужно чтобы IsInTree правильно определялось в addScriptingTreeGroupObject, когда идет addGroupObjectView
    }

    private final FormEntity.ExProperty listViewTypeProp;
    public Property<?> getNFListViewType(Version version) {
        return listViewTypeProp.getNF(version);
    }
    public Property<?> getListViewType() {
        return listViewTypeProp.get();
    }

    public GroupObjectEntity(int ID, String sID, BaseLogicsModule LM) {
        super(ID, sID != null ? sID : "groupObj" + ID);

        ConcreteCustomClass listViewType = LM.listViewType;
        listViewTypeProp = new FormEntity.ExProperty(() -> PropertyFact.createDataPropRev("LIST VIEW TYPE", this, listViewType));
        props = MapFact.toMap(GroupObjectProp.values(), type -> new ExGroupProperty( // assert finalizedObjects
                () -> PropertyFact.createDataPropRev(type.toString(), this, getObjects().mapValues((ObjectEntity value) -> value.baseClass), type.getValueClass(), null)));
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

    private final NFProperty<Boolean> isFilterExplicitlyUsed = NFFact.property(); // optimization hack - there are a lot of FILTER usages by group change, but group change needs FILTER only when group (grid) is visible and refreshed, so we do filter update only if the latter condition is matched
    private final NFProperty<Boolean> isOrderExplicitlyUsed = NFFact.property(); // optimization hack - there are a lot of ORDER usages by group change, but group change needs ORDER only when group (grid) is visible and refreshed, so we do filter update only if the latter condition is matched

    public boolean isFilterExplicitlyUsed() {
        return isFilterExplicitlyUsed.get() != null;
    }
    public boolean isOrderExplicitlyUsed() {
        return isOrderExplicitlyUsed.get() != null;
    }

    public static class ExGroupProperty extends FormEntity.ExProp<PropertyRevImplement<ClassPropertyInterface, ObjectEntity>> {

        public ExGroupProperty(Supplier<PropertyRevImplement<ClassPropertyInterface, ObjectEntity>> supplier) {
            super(supplier);
        }

        public ExGroupProperty(FormEntity.ExProp<PropertyRevImplement<ClassPropertyInterface, ObjectEntity>> exProp, ObjectMapping mapping) {
            super(exProp, mapping::get, mapping.version);
        }
    }
    private final ImMap<GroupObjectProp, ExGroupProperty> props;
    public PropertyRevImplement<ClassPropertyInterface, ObjectEntity> getNFProperty(GroupObjectProp type, Version version) {
        if(type.equals(GroupObjectProp.FILTER))
            isFilterExplicitlyUsed.set(true, version);
        if(type.equals(GroupObjectProp.ORDER))
            isOrderExplicitlyUsed.set(true, version);
        return props.get(type).getNF(version);
    }

    public void fillGroupChanges(Version version) {
        props.get(GroupObjectProp.FILTER).getNF(version);
        props.get(GroupObjectProp.ORDER).getNF(version);
    }
    public PropertyRevImplement<ClassPropertyInterface, ObjectEntity> getGroupChange(GroupObjectProp type) {
        assert type.equals(GroupObjectProp.FILTER) || type.equals(GroupObjectProp.ORDER);
        return props.get(type).get();
    }
    public ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectEntity>> getProperties() {
        return props.mapValues(FormEntity.ExProp::get).removeNulls();
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

    public void setPropertyBackground(PropertyObjectEntity<?> propertyBackground) {
        this.propertyBackground = propertyBackground;
    }

    public void setPropertyForeground(PropertyObjectEntity<?> propertyForeground) {
        this.propertyForeground = propertyForeground;
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

    public GroupObjectEntity(int ID, ImOrderSet<ObjectEntity> objects, BaseLogicsModule LM) {
        this(ID, (String)null, LM);

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
        listViewTypeProp = null;
        props = null;
    }
    public static final GroupObjectEntity NULL = new GroupObjectEntity();

    public boolean isSimpleList() {
        return getObjects().size() == 1 && viewType.isList() && !isInTree();
    }

    public GroupObjectView view;

    public boolean isSubReport() {
        return nvl(isSubReport.get(), false);
    }
    public void setIsSubReport(Boolean value, Version version) {
        isSubReport.set(value, version);
    }

    public PropertyObjectEntity getReportPathProp() {
        return reportPathProp.get();
    }
    public void setReportPathProp(PropertyObjectEntity<?> value, Version version) {
        reportPathProp.set(value, version);
    }

    public DebugInfo.DebugPoint getNFDebugPoint(Version version) {
        return debugPoint.getNF(version);
    }
    public void setDebugPoint(DebugInfo.DebugPoint value, Version version) {
        debugPoint.set(value, version);
    }

    public Pair<Integer, Integer> getScriptIndex() {
        return scriptIndex.get();
    }
    public void setScriptIndex(Pair<Integer, Integer> value, Version version) {
        scriptIndex.set(value, version);
    }

    public Group getPropertyGroup() {
        return propertyGroup.get();
    }
    public void setPropertyGroup(Group value, Version version) {
        propertyGroup.set(value, version);
    }

    public boolean isEnableManualUpdate() {
        return nvl(enableManualUpdate.get(), false);
    }
    public void setEnableManualUpdate(Boolean value, Version version) {
        enableManualUpdate.set(value, version);
    }

    public String getIntegrationSIDValue() {
        String integrationSID = getIntegrationSID();
        return integrationSID != null ? integrationSID : getSID();
    }
    public String getIntegrationSID() {
        return integrationSID.get();
    }
    public void setIntegrationSID(String value, Version version) {
        integrationSID.set(value, version);
    }

    public boolean isIndex() {
        boolean integrationKey = nvl(getIntegrationKey(), false);
        return !integrationKey;
    }
    public Boolean getIntegrationKey() {
        return integrationKey.get();
    }
    public void setIntegrationKey(Boolean value, Version version) {
        integrationKey.set(value, version);
    }

    // copy-constructor
    public GroupObjectEntity(GroupObjectEntity src, ObjectMapping mapping) {
        super(src);

        mapping.put(src, this);

        ID = BaseLogicsModule.generateStaticNewID();

        treeGroup = src.treeGroup != null ? mapping.get(src.treeGroup) : null;

        isSubReport.set(src.isSubReport, p -> p, mapping.version);
        reportPathProp.set(src.reportPathProp, mapping::get, mapping.version);

        debugPoint.set(src.debugPoint, p -> p, mapping.version);
        scriptIndex.set(src.scriptIndex, p -> p, mapping.version);

        updateType = src.updateType;

        propertyGroup.set(src.propertyGroup, p -> p, mapping.version);

        enableManualUpdate.set(src.enableManualUpdate, p -> p, mapping.version);

        integrationSID.set(src.integrationSID, p -> p, mapping.version);
        integrationKey.set(src.integrationKey, p -> p, mapping.version);

        viewType = src.viewType;
        listViewType = src.listViewType;
        pivotOptions = src.pivotOptions;
        customRenderFunction = src.customRenderFunction;
        mapTileProvider = src.mapTileProvider;
        asyncInit =  src.asyncInit;
        pageSize = src.pageSize;

        for(ObjectEntity e : src.getOrderObjects())
            add(mapping.get(e));

        propertyCustomOptions = mapping.get(src.propertyCustomOptions);
        propertyBackground = mapping.get(src.propertyBackground);
        propertyForeground = mapping.get(src.propertyForeground);
        isParent = src.isParent != null ? src.isParent.mapKeyValues(mapping::get, (Function<PropertyObjectEntity<?>, PropertyObjectEntity<?>>) mapping::get) : null;

        props = src.props.mapItValues(mapping::get);
        listViewTypeProp = mapping.get(src.listViewTypeProp);
        isFilterExplicitlyUsed.set(src.isFilterExplicitlyUsed, p -> p, mapping.version);
        isOrderExplicitlyUsed.set(src.isOrderExplicitlyUsed, p -> p, mapping.version);
    }
}
