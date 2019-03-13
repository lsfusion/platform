package lsfusion.server.logics.form.struct.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.Settings;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.StaticParamNullableExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.struct.property.CalcPropertyObjectEntity;
import lsfusion.server.logics.form.interactive.GroupObjectProp;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.InstanceFactory;
import lsfusion.server.logics.form.interactive.Instantiable;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcPropertyRevImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.action.session.change.modifier.Modifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.interop.form.property.ClassViewType.DEFAULT;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance> {

    public static int PAGE_SIZE_DEFAULT_VALUE = 50;

    public TreeGroupEntity treeGroup;

    public boolean isInTree() {
        return treeGroup != null;
    }

    public boolean isSubReport;
    public CalcPropertyObjectEntity<?> reportPathProp;
    
    public UpdateType updateType;
    
    public AbstractGroup propertyGroup; // used for integration (export / import)

    private String integrationSID;
    private boolean integrationKey; // key (key in JSON, tag in XML, fields in plain formats) or index (array in JSON, multiple object name tags in xml, order in plain formats)

    public boolean isIndex() {
        return !integrationKey;
    }

    public void setIntegrationKey(boolean integrationKey) {
        this.integrationKey = integrationKey;
    }

    private static class UpStaticParamsProcessor implements GroupObjectInstance.FilterProcessor {
        private final GroupObjectInstance groupObject;

        public UpStaticParamsProcessor(GroupObjectInstance groupObject) {
            this.groupObject = groupObject;
        }

        public ImSet<FilterInstance> getFilters() {
            return groupObject.fixedFilters;
        }

        public ImMap<ObjectInstance, ? extends Expr> process(FilterInstance filt, ImMap<ObjectInstance, ? extends Expr> mapKeys) {
            return MapFact.addExcl(mapKeys, filt.getObjects().remove(mapKeys.keys()).mapValues(new GetValue<Expr, ObjectInstance>() {
                public Expr getMapValue(ObjectInstance value) {
                    return new StaticParamNullableExpr(value.getBaseClass().getUpSet(), value.toString());
                }
            }));
        }
    }
    private static class NoUpProcessor implements GroupObjectInstance.FilterProcessor {
        private final GroupObjectInstance groupObject;

        public NoUpProcessor(GroupObjectInstance groupObject) {
            this.groupObject = groupObject;
        }

        public ImSet<FilterInstance> getFilters() {
            return groupObject.fixedFilters;
        }

        public ImMap<ObjectInstance, ? extends Expr> process(FilterInstance filt, ImMap<ObjectInstance, ? extends Expr> mapKeys) {
            if(!groupObject.getOrderObjects().getSet().containsAll(filt.getObjects())) // если есть "внешние" объекты исключаем
                return null;
            return mapKeys;
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


    public GroupObjectEntity() {
    }

    public GroupObjectEntity(int ID, TreeGroupEntity treeGroup) {
        this(ID, (String)null);
        this.treeGroup = treeGroup; // нужно чтобы IsInTree правильно определялось в addScriptingTreeGroupObject, когда идет addGroupObjectView
    }

    public GroupObjectEntity(int ID, String sID) {
        super(ID, sID != null ? sID : "groupObj" + ID);
    }

    public ClassViewType initClassView = DEFAULT;
    public List<ClassViewType> banClassView = new ArrayList<>();
    public Integer pageSize;

    public CalcPropertyObjectEntity<?> propertyBackground;
    public CalcPropertyObjectEntity<?> propertyForeground;

    private boolean finalizedProps = false;
    private Object props = MapFact.mExclMap();
    public CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> getProperty(GroupObjectProp type) {
        assert finalizedObjects && !finalizedProps;
        MExclMap<GroupObjectProp, CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity>> mProps = (MExclMap<GroupObjectProp, CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity>>) props;
        CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> prop = mProps.get(type);
        if(prop==null) { // type.getSID() + "_" + getSID() нельзя потому как надо еще SID формы подмешивать
            prop = DerivedProperty.createDataPropRev(LocalizedString.create(type.toString() + " (" + objects.toString() + ")", false), getObjects().mapValues(new GetValue<ValueClass, ObjectEntity>() {
                public ValueClass getMapValue(ObjectEntity value) {
                    return value.baseClass;
                }}), type.getValueClass(), null);
            mProps.exclAdd(type, prop);
        }
        return prop;
    }

    public ImMap<GroupObjectProp, CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity>> getProperties() {
        if(!finalizedProps) {
            props = ((MExclMap<GroupObjectProp, CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity>>) props).immutable();
            finalizedProps = true;
        }
        return (ImMap<GroupObjectProp, CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity>>) props;
    }

    public GroupObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public ImMap<ObjectEntity, CalcPropertyObjectEntity<?>> isParent = null;

    public void setIsParents(final CalcPropertyObjectEntity... properties) {
        isParent = getOrderObjects().mapOrderValues(new GetIndex<CalcPropertyObjectEntity<?>>() {
            public CalcPropertyObjectEntity<?> getMapValue(int i) {
                return properties[i];
            }});
    }

    public void setInitClassView(ClassViewType type) {
        initClassView = type;
    }

    public void setPanelClassView() {
        setInitClassView(ClassViewType.PANEL);
        setSingleClassView();
    }
    
    public void setGridClassView() {
        setInitClassView(ClassViewType.GRID);
        setSingleClassView();
    }

    public void setSingleClassView() {
        banClassView.addAll(BaseUtils.toList(ClassViewType.getAllTypes()));
        banClassView.remove(initClassView);
    }

    public boolean isAllowedClassView(ClassViewType type) {
        return !banClassView.contains(type);
    }

    public boolean isPanel() {
        return initClassView.isPanel();
    }
    public boolean isForcedPanel() {
        if(!isPanel())
            return false;
        for(ClassViewType type : ClassViewType.getAllTypes())
            if(!type.isPanel() && isAllowedClassView(type))
                return false;
        return true;
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
    
    private static Where getFilterWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier, ImSet<FilterEntity> filters) throws SQLException, SQLHandledException {
        Where where = Where.TRUE;
        for(FilterEntity filt : filters)
            where = where.and(filt.getWhere(mapKeys, modifier));
        return where;
    }

    private static ImMap<ObjectEntity, ValueClass> getGridClasses(ImSet<ObjectEntity> objects) {
        return objects.filterFn(new SFunctionSet<ObjectEntity>() {
            public boolean contains(ObjectEntity element) {
                return !element.noClasses();
            }
        }).mapValues(new GetValue<ValueClass, ObjectEntity>() {
            public ValueClass getMapValue(ObjectEntity value) {
                return value.baseClass;
            }});
    }
    public Where getClassWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return IsClassProperty.getWhere(getGridClasses(getObjects()), mapKeys, modifier, null);
    }

    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier, ImSet<FilterEntity> filters) throws SQLException, SQLHandledException {
        return getFilterWhere(mapKeys, modifier, filters).and(getClassWhere(mapKeys, modifier));
    }

    // hack where ImMap used (it does not support null keys)
    public static final GroupObjectEntity NULL = new GroupObjectEntity();
}
