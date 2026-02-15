package lsfusion.server.logics.form.struct.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
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
import lsfusion.server.logics.classes.data.LogicalClass;
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
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.FilterEntityInstance;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class GroupObjectEntity extends IdentityEntity<GroupObjectEntity, ObjectEntity> implements Instantiable<GroupObjectInstance> {

    public TreeGroupEntity treeGroup;

    public PropertyDrawEntity count;

    public boolean isInTree() {
        return treeGroup != null;
    }

    private NFProperty<Boolean> isSubReport = NFFact.property();
    private NFProperty<PropertyObjectEntity> reportPathProp = NFFact.property();


    private final NFProperty<UpdateType> updateType = NFFact.property();

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
            if(!groupObject.objects.containsAll(filt.getObjects())) // если есть "внешние" объекты исключаем
                return null;
            return super.process(filt, mapKeys, modifier, reallyChanged);
        }
    }

    private UpdateType lazyUpdateType;
    @ManualLazy
    public UpdateType getUpdateType(GroupObjectInstance groupObject) throws SQLException, SQLHandledException {
        if(lazyUpdateType == null) {
            UpdateType updateTypeValue = updateType.get();
            if (updateTypeValue == null) { // default
                if (!Settings.get().isDisableUpdateTypeHeur()) {
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
                        updateTypeValue = UpdateType.FIRST;
                }

                if (updateTypeValue == null)
                    updateTypeValue = UpdateType.PREV;
            }
            lazyUpdateType = updateTypeValue;
        }
        return lazyUpdateType;
    }

    private final FormEntity.ExProperty listViewTypeProp;
    public Property<?> getNFListViewType(Version version) {
        return listViewTypeProp.getNF(version);
    }
    public Property<?> getListViewType() {
        return listViewTypeProp.get();
    }

    private final NFProperty<ClassViewType> viewType = NFFact.property();
    private final NFProperty<ListViewType> listViewType = NFFact.property();

    private final NFProperty<String> pivotType = NFFact.property();
    private final NFProperty<PropertyGroupType> pivotAggregation = NFFact.property();
    private final NFProperty<Boolean> pivotShowSettings = NFFact.property();
    private final NFProperty<String> pivotConfigFunction = NFFact.property();

    private final NFProperty<String> customRenderFunction = NFFact.property();
    private final NFProperty<PropertyObjectEntity> propertyCustomOptions = NFFact.property();
    private final NFProperty<String> mapTileProvider = NFFact.property();

    // for now will use async init since pivot is analytics and don't need for example focuses and can afford extra round trip
    private final NFProperty<Boolean> asyncInit = NFFact.property(); // default true

    private final NFProperty<Integer> pageSize = NFFact.property();

    private final NFProperty<PropertyObjectEntity> propertyBackground = NFFact.property();
    private final NFProperty<PropertyObjectEntity> propertyForeground = NFFact.property();

    public boolean isFilterExplicitlyUsed() {
        return isExplicitlyUsed(GroupObjectProp.FILTER);
    }
    public boolean isOrderExplicitlyUsed() {
        return isExplicitlyUsed(GroupObjectProp.ORDER);
    }
    public boolean isSelectExplicitlyUsed() {
        return isExplicitlyUsed(GroupObjectProp.SELECT);
    }

    public static class ExGroupProperty extends FormEntity.ExMapProp<PropertyObjectEntity<ClassPropertyInterface>, ExGroupProperty> {

        public ExGroupProperty(Supplier<PropertyObjectEntity<ClassPropertyInterface>> supplier) {
            super(supplier);
        }

        public ExGroupProperty(ExGroupProperty exProp, ObjectMapping mapping) {
            super(exProp, mapping);
        }

        @Override
        public ExGroupProperty get(ObjectMapping mapping) {
            return new ExGroupProperty(this, mapping);
        }
    }

    private final FormEntity.ExProperty isSelectProperty;
    @NFLazy
    public Property<?> getNFIsSelectProperty(Version version) {
        return isSelectProperty.getNF(version);
    }

    public Property<?> getIsSelectProperty() {
        return isSelectProperty.get();
    }

    private final ImMap<GroupObjectProp, ExGroupProperty> props;
    private final ImMap<GroupObjectProp, NFProperty<Boolean>> isExplicitlyUsed = MapFact.toMap(GroupObjectProp.values(), type -> NFFact.property()); // optimization hack - there are a lot of FILTER usages by group change, but group change needs FILTER, etc. only when group (grid) is visible and refreshed, so we do filter update only if the latter condition is matched
    public PropertyObjectEntity<ClassPropertyInterface> getNFProperty(GroupObjectProp type, Version version) {
        isExplicitlyUsed.get(type).set(true, version);
        return props.get(type).getNF(version);
    }
    public PropertyObjectEntity<ClassPropertyInterface> getGroupProp(GroupObjectProp type) {
        assert type.equals(GroupObjectProp.FILTER) || type.equals(GroupObjectProp.ORDER) || type.equals(GroupObjectProp.SELECT);
        return props.get(type).get();
    }
    public ImMap<GroupObjectProp, PropertyObjectEntity<ClassPropertyInterface>> getProperties() {
        return props.mapValues(p -> p.get()).removeNulls();
    }
    public boolean isExplicitlyUsed(GroupObjectProp type) {
        return isExplicitlyUsed.get(type).get() != null;
    }

    public LocalizedString getContainerCaption() {
        if (!objects.isEmpty())
            return objects.get(0).getCaption();
        else
            return null;
    }

    public void fillGroupProps(Version version) {
        // we're prereading:
        // order for using in the group change
        // filter, select, isSelect for using in the group change / ctrl+c
        // value for using in the ctrl + c
        props.get(GroupObjectProp.FILTER).getNF(version);
        props.get(GroupObjectProp.ORDER).getNF(version);
        // selection
        props.get(GroupObjectProp.SELECT).getNF(version);
        getObjects().mapItRevValues(object -> object.getNFValueProperty(version));
        isSelectProperty.getNF(version);
    }

    public static PropertyObjectEntity<?> getFullSelectProperty(Property<?> isSelectProperty, PropertyObjectEntity<ClassPropertyInterface> selectProp, PropertyObjectEntity<ClassPropertyInterface> filterProp, ImRevMap<ObjectEntity, Property<?>> objectValues) {
        ImRevMap<ObjectEntity, PropertyInterface> mapObjects = BaseUtils.immutableCast(selectProp.mapping.reverse());

        ImOrderMap<ObjectEntity, Property<?>> orderMap = objectValues.toOrderMap();
        ImOrderSet<PropertyInterfaceImplement<PropertyInterface>> mapInterfaces = BaseUtils.immutableCast(orderMap.keyOrderSet().mapOrder(mapObjects));

        // IF ISSELECT() THEN SELECT(p1, p2, .., pN) AND FILTER ELSE p1 = value o1 AND p2 = value o2 AND ... AND pN = value oN
        PropertyMapImplement<?, PropertyInterface> fullSelectImpl = PropertyFact.createIfElseUProp(mapObjects.valuesSet(),
                isSelectProperty.getImplement(SetFact.EMPTYORDER()), PropertyFact.createAnd(selectProp.getImplement(mapObjects), filterProp.getImplement(mapObjects)),
                PropertyFact.createCompare(mapInterfaces, orderMap.mapListValues(prop -> prop.getImplement(SetFact.EMPTYORDER())), Compare.EQUALS));

        return new PropertyObjectEntity(fullSelectImpl.property, fullSelectImpl.mapping.join(mapObjects.reverse()));
    }

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    private final NFProperty<ImMap<ObjectEntity, PropertyObjectEntity>> isParent = NFFact.property();

    public UpdateType getUpdateType() {
        return updateType.get();
    }

    public void setUpdateType(UpdateType value, Version version) {
        updateType.set(value, version);
    }

    public ClassViewType getViewType() {
        ClassViewType value = viewType.get();
        return value != null ? value : ClassViewType.DEFAULT;
    }

    public ClassViewType getNFViewType(Version version) {
        ClassViewType value = viewType.getNF(version);
        return value != null ? value : ClassViewType.DEFAULT;
    }

    public void setViewType(ClassViewType viewType, FormEntity form, Version version) {
        this.viewType.set(viewType, version);

        form.updatePropertyDraws(version);
    }

    public ListViewType getListViewTypeValue() {
        ListViewType value = listViewType.get();
        return value != null ? value : ListViewType.DEFAULT;
    }

    public void setListViewType(ListViewType listViewType, Version version) {
        this.listViewType.set(listViewType, version);
    }

    public PivotOptions getPivotOptions() {
        return new PivotOptions(pivotType.get(), pivotAggregation.get(), pivotShowSettings.get(), pivotConfigFunction.get());
    }

    public void setPivotOptions(PivotOptions pivotOptions, Version version) {
        pivotType.set(pivotOptions.getType(), version);
        pivotAggregation.set(pivotOptions.getAggregation(), version);
        pivotShowSettings.set(pivotOptions.getShowSettings(), version);
        pivotConfigFunction.set(pivotOptions.getConfigFunction(), version);
    }

    public String getCustomRenderFunction() {
        return customRenderFunction.get();
    }

    public void setCustomRenderFunction(String customRenderFunction, Version version) {
        this.customRenderFunction.set(customRenderFunction, version);
    }

    public void setPropertyCustomOptions(PropertyObjectEntity<?> propertyCustomOptions, Version version) {
        this.propertyCustomOptions.set(propertyCustomOptions, version);
    }

    public PropertyObjectEntity<?> getPropertyCustomOptions() {
        return propertyCustomOptions.get();
    }

    public String getMapTileProvider() {
        return mapTileProvider.get();
    }

    public void setMapTileProvider(String mapTileProvider, Version version) {
        this.mapTileProvider.set(mapTileProvider, version);
    }

    public boolean isAsyncInit() {
        Boolean value = asyncInit.get();
        return value == null || value;
    }

    public void setAsyncInit(boolean asyncInit, Version version) {
        this.asyncInit.set(asyncInit, version);
    }

    public Integer getPageSize() {
        return pageSize.get();
    }

    public void setPageSize(Integer pageSize, Version version) {
        this.pageSize.set(pageSize, version);
    }

    public PropertyObjectEntity<?> getPropertyBackground() {
        return propertyBackground.get();
    }

    public void setPropertyBackground(PropertyObjectEntity<?> propertyBackground, Version version) {
        this.propertyBackground.set(propertyBackground, version);
    }

    public PropertyObjectEntity<?> getPropertyForeground() {
        return propertyForeground.get();
    }

    public void setPropertyForeground(PropertyObjectEntity<?> propertyForeground, Version version) {
        this.propertyForeground.set(propertyForeground, version);
    }

    public ImMap<ObjectEntity, PropertyObjectEntity> getIsParent() {
        return isParent.get();
    }

    public void setIsParents(Version version, final PropertyObjectEntity... properties) {
        isParent.set(getOrderObjects().mapOrderValues((IntFunction<PropertyObjectEntity>) i -> properties[i]), version);
    }

    public void setViewTypePanel(FormEntity form, Version version) {
        setViewType(ClassViewType.PANEL, form, version);
    }
    
    public void setViewTypeList(FormEntity form, Version version) {
        setViewType(ClassViewType.LIST, form, version);
    }

    public boolean isPanel() {
        return getViewType().isPanel();
    }

    public boolean isCustom() {
        return !isPanel() && getListViewTypeValue() == ListViewType.CUSTOM;
    }

    public boolean isSimpleState() {
        ListViewType listViewType = getListViewTypeValue();
        return !isPanel() && (listViewType == ListViewType.CUSTOM || listViewType == ListViewType.MAP || listViewType == ListViewType.CALENDAR);
    }

    public ObjectEntity getObject(String sid) {
        for (ObjectEntity object : getObjects()) {
            if (object.getSID().equals(sid)) {
                return object;
            }
        }
        return null;
    }

    private final ImOrderSet<ObjectEntity> objects;
    public ImSet<ObjectEntity> getObjects() {
        return objects.getSet();
    }
    @LongMutable
    public ImOrderSet<ObjectEntity> getOrderObjects() {
        return objects;
    }

    @Override
    protected String getDefaultSIDPrefix() {
        return "groupObj";
    }

    public GroupObjectEntity(IDGenerator ID, String sID, ImOrderSet<ObjectEntity> objects, BaseLogicsModule LM, DebugInfo.DebugPoint debugPoint) {
        super(ID, sID, debugPoint);

        ConcreteCustomClass listViewType = LM.listViewType;
        listViewTypeProp = new FormEntity.ExProperty(() -> PropertyFact.createDataPropRev("LIST VIEW TYPE", this, listViewType));
        isSelectProperty = new FormEntity.ExProperty(() -> PropertyFact.createDataPropRev("IS SELECT", this, LogicalClass.instance));
        props = MapFact.toMap(GroupObjectProp.values(), type -> new ExGroupProperty( // assert finalizedObjects
                () -> PropertyFact.createDataPropRev(type.toString(), this, getOrderObjects(), type.getValueClass(), null)));

        this.objects = objects;
        for(ObjectEntity object : objects)
            object.groupTo = this;
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
        super(() -> -1, null, null);
        listViewTypeProp = null;
        isSelectProperty = null;
        props = null;
        objects = null;
    }
    public static final GroupObjectEntity NULL = new GroupObjectEntity();

    public boolean isSimpleList() {
        return getObjects().size() == 1 && getViewType().isList() && !isInTree();
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

    public Pair<Integer, Integer> getScriptIndex() {
        return debugPoint != null ? Pair.create(debugPoint.getScriptLine(), debugPoint.offset) : null;
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
        if (integrationSID != null)
            return integrationSID;

        if(sID != null)
            return sID;

        integrationSID = "";
        for (ObjectEntity obj : getOrderObjects()) {
            integrationSID = (integrationSID.length() == 0 ? "" : integrationSID + ".") + obj.getIntegrationSID();
        }

        return integrationSID;
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
    protected GroupObjectEntity(GroupObjectEntity src, ObjectMapping mapping) {
        super(src, mapping);

        objects = mapping.get(src.objects);

        treeGroup = mapping.get(src.treeGroup); // nullable
        view = mapping.get(src.view);
        count = mapping.get(src.count); // nullable

        props = mapping.gets(src.props);
        listViewTypeProp = mapping.get(src.listViewTypeProp);
        isSelectProperty = mapping.get(src.isSelectProperty);
    }

    @Override
    public void extend(GroupObjectEntity src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(isSubReport, src.isSubReport);
        mapping.sets(propertyGroup, src.propertyGroup);
        mapping.sets(enableManualUpdate, src.enableManualUpdate);
        mapping.sets(integrationSID, src.integrationSID);
        mapping.sets(integrationKey, src.integrationKey);

        mapping.sets(isExplicitlyUsed, src.isExplicitlyUsed);

        mapping.sets(updateType, src.updateType);
        mapping.sets(viewType, src.viewType);
        mapping.sets(listViewType, src.listViewType);

        mapping.sets(pivotType, src.pivotType);
        mapping.sets(pivotAggregation, src.pivotAggregation);
        mapping.sets(pivotShowSettings, src.pivotShowSettings);
        mapping.sets(pivotConfigFunction, src.pivotConfigFunction);

        mapping.sets(customRenderFunction, src.customRenderFunction);
        mapping.sets(mapTileProvider, src.mapTileProvider);
        mapping.sets(asyncInit, src.asyncInit);
        mapping.sets(pageSize, src.pageSize);

        mapping.set(propertyCustomOptions, src.propertyCustomOptions);
        mapping.set(propertyBackground, src.propertyBackground);
        mapping.set(propertyForeground, src.propertyForeground);
        mapping.setm(isParent, src.isParent);

        mapping.set(reportPathProp, src.reportPathProp);
    }

    @Override
    public ObjectEntity getAddParent(ObjectMapping mapping) {
        return getOrderObjects().get(0);
    }

    @Override
    public GroupObjectEntity getAddChild(ObjectEntity parent, ObjectMapping mapping) {
        return parent.groupTo;
    }
//    @Override
//    public GroupObjectEntity getAdd(ObjectMapping mapping) {
//        if(mapping.extend) // in theory it makes sense to check neighbours and tree that they perfectly match
//            return mapping.addForm.getNFGroupObject(getSID(), mapping.getFindVersion());
//        return null;
//    }
    @Override
    public GroupObjectEntity copy(ObjectMapping mapping) {
        return new GroupObjectEntity(this, mapping);
    }
}
