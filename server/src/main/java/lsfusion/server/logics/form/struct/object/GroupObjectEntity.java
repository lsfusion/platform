package lsfusion.server.logics.form.struct.object;

import lsfusion.base.Pair;
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
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.controller.init.Instantiable;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.struct.filter.FilterEntityInstance;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.IntFunction;

import static lsfusion.interop.form.property.ClassViewType.DEFAULT;

public class GroupObjectEntity extends IdentityObject implements Instantiable<GroupObjectInstance> {

    public static int PAGE_SIZE_DEFAULT_VALUE = 50;

    public TreeGroupEntity treeGroup;

    public boolean isInTree() {
        return treeGroup != null;
    }

    public boolean isSubReport;
    public PropertyObjectEntity<?> reportPathProp;
    
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

    private static class UpStaticParamsProcessor implements GroupObjectInstance.FilterProcessor {
        private final GroupObjectInstance groupObject;

        public UpStaticParamsProcessor(GroupObjectInstance groupObject) {
            this.groupObject = groupObject;
        }

        public ImSet<FilterInstance> getFilters() {
            return groupObject.fixedFilters;
        }

        public ImMap<ObjectInstance, ? extends Expr> process(FilterInstance filt, ImMap<ObjectInstance, ? extends Expr> mapKeys) {
            return MapFact.addExcl(mapKeys, filt.getObjects().remove(mapKeys.keys()).mapValues(new Function<ObjectInstance, Expr>() {
                public Expr apply(ObjectInstance value) {
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

    public GroupObjectEntity(int ID, TreeGroupEntity treeGroup) {
        this(ID, (String)null);
        this.treeGroup = treeGroup; // нужно чтобы IsInTree правильно определялось в addScriptingTreeGroupObject, когда идет addGroupObjectView
    }

    private PropertyRevImplement<ClassPropertyInterface, ObjectEntity> gridViewTypeProp;
    public PropertyRevImplement<ClassPropertyInterface, ObjectEntity> getGridViewType(ConcreteCustomClass gridViewType) {
        if (gridViewTypeProp == null) {
            gridViewTypeProp = PropertyFact.createDataPropRev(LocalizedString.create(this.toString()), MapFact.EMPTY(), gridViewType, LocalNestedType.ALL);
        }
        return gridViewTypeProp;
    }

    public GroupObjectEntity(int ID, String sID) {
        super(ID, sID != null ? sID : "groupObj" + ID);
    }

    public ClassViewType viewType = DEFAULT;
    public Integer pageSize;

    public int getDefaultPageSize() {
        return pageSize != null ? pageSize : Settings.get().getPageSizeDefaultValue();
    }

    public PropertyObjectEntity<?> propertyBackground;
    public PropertyObjectEntity<?> propertyForeground;

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
            prop = PropertyFact.createDataPropRev(LocalizedString.create(type.toString() + " (" + objects.toString() + ")", false), getObjects().mapValues(new Function<ObjectEntity, ValueClass>() {
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

    public void setViewType(ClassViewType type) {
        viewType = type;
    }

    public void setPanelViewType() {
        setViewType(ClassViewType.PANEL);
    }
    
    public void setGridViewType() {
        setViewType(ClassViewType.GRID);
    }

    public boolean isPanel() {
        return viewType.isPanel();
    }

    public Pair<Integer, Integer> getScriptIndex() {
        return scriptIndex;
    }

    public void setScriptIndex(Pair<Integer, Integer> scriptIndex) {
        this.scriptIndex = scriptIndex;
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

    private static ImMap<ObjectEntity, ValueClass> getGridClasses(ImSet<ObjectEntity> objects) {
        return objects.filterFn(element -> !element.noClasses()).mapValues((ObjectEntity value) -> value.baseClass);
    }
    public Where getClassWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return IsClassProperty.getWhere(getGridClasses(getObjects()), mapKeys, modifier, null);
    }

    public Where getWhere(ImMap<ObjectEntity, ? extends Expr> mapKeys, Modifier modifier, ImSet<? extends FilterEntityInstance> filters) throws SQLException, SQLHandledException {
        return getFilterWhere(mapKeys, modifier, filters).and(getClassWhere(mapKeys, modifier));
    }

    // hack where ImMap used (it does not support null keys)
    private GroupObjectEntity() {
    }
    public static final GroupObjectEntity NULL = new GroupObjectEntity();
}
