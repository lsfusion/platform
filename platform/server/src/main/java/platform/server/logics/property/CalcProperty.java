package platform.server.logics.property;

import platform.base.*;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.Settings;
import platform.server.ThisMessage;
import platform.server.caches.IdentityLazy;
import platform.server.caches.PackComplex;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.actions.ChangeEvent;
import platform.server.logics.property.actions.edit.DefaultChangeActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.table.MapKeysTable;
import platform.server.logics.table.TableFactory;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.merge;

public abstract class CalcProperty<T extends PropertyInterface> extends Property<T> {

    public static FunctionSet<CalcProperty> getDependsSet(final FunctionSet<CalcProperty> check) {
        return new FunctionSet<CalcProperty>() {
            public boolean contains(CalcProperty element) {
                return depends(element, check);
            }

            public boolean isEmpty() {
                return check.isEmpty();
            }

            public boolean isFull() {
                return check.isFull();
            }
        };
    }
    public static boolean depends(CalcProperty<?> property, CalcProperty check) {
        return property.getRecDepends().contains(check);
    }

    public static boolean depends(CalcProperty<?> property, FunctionSet<CalcProperty> check) {
        return property.getRecDepends().intersect(check);
    }

    public static boolean depends(Iterable<CalcProperty> properties, QuickSet<CalcProperty> check) {
        for(CalcProperty property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static boolean depends(Iterable<CalcProperty> properties, CalcProperty check) {
        for(CalcProperty property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static <T extends CalcProperty> Set<T> used(Iterable<T> used, Iterable<CalcProperty> usedIn) {
        Set<T> result = new HashSet<T>();
        for(T property : used)
            if(depends(usedIn, property))
                result.add(property);
        return result;
    }

    public static <T extends PropertyInterface> boolean dependsImplement(Iterable<CalcPropertyInterfaceImplement<T>> properties, QuickSet<CalcProperty> check) {
        for(CalcPropertyInterfaceImplement<T> property : properties)
            if(property instanceof CalcPropertyMapImplement && depends(((CalcPropertyMapImplement)property).property, check))
                return true;
        return false;
    }

    // используется если создаваемый WhereBuilder нужен только если задан changed
    public static WhereBuilder cascadeWhere(WhereBuilder changed) {
        return changed == null ? null : new WhereBuilder();
    }

    public abstract boolean isStored();

    public String outputStored(boolean outputTable) {
        assert isStored() && field!=null;
        return (this instanceof DataProperty? ServerResourceBundle.getString("logics.property.primary"):ServerResourceBundle.getString("logics.property.calculated")) + " "+ServerResourceBundle.getString("logics.property")+" : " + caption+", "+mapTable.table.outputField(field, outputTable);
    }

    // по выражениям проверяет
    public <P extends PropertyInterface> boolean intersectFull(CalcProperty<P> property, Map<P, T> map) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return !getExpr(mapKeys).getWhere().and(property.getExpr(BaseUtils.join(map, mapKeys)).getWhere()).not().checkTrue();
    }

    protected CalcProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);

        changeExpr = new PullExpr(toString() + " value");
    }

    public void change(ExecutionContext context, Object value) throws SQLException {
        change(context.getEnv(), value);
    }

    public void change(ExecutionEnvironment env, Object value) throws SQLException {
        change(new HashMap<T, DataObject>(), env, value);
    }

    public void change(Map<T, DataObject> keys, ExecutionContext context, Object value) throws SQLException {
        change(keys, context.getEnv(), value);
    }

    public void change(Map<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException {
        getImplement().change(keys, env, value);
    }

    public Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>> splitFitClasses(SinglePropertyTableUsage<T> changeTable, SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        assert isStored();

        if(!Settings.instance.isEnableApplySingleStored() || DataSession.notFitKeyClasses(this, changeTable)) // оптимизация
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(createChangeTable(), changeTable);
        if(DataSession.fitClasses(this, changeTable))
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(changeTable, createChangeTable());

        PropertyChange<T> change = SinglePropertyTableUsage.getChange(changeTable);

        Map<KeyField, Expr> mapKeys = crossJoin(mapTable.mapKeys, change.getMapExprs());
        Where classWhere = fieldClassWhere.getWhere(merge(mapKeys, Collections.singletonMap(field, change.expr)))
                .or(mapTable.table.getClasses().getWhere(mapKeys).and(change.expr.getWhere().not())); // или если меняет на null, assert что fitKeyClasses
        
        if(classWhere.isFalse()) // оптимизация
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(createChangeTable(), changeTable);
        if(classWhere.isTrue())
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(changeTable, createChangeTable());

        SinglePropertyTableUsage<T> fit = readChangeTable(sql, change.and(classWhere), baseClass, env);
        SinglePropertyTableUsage<T> notFit = readChangeTable(sql, change.and(classWhere.not()), baseClass, env);
        assert DataSession.fitClasses(this, fit);
        assert DataSession.fitKeyClasses(this, fit);
        assert DataSession.notFitClasses(this, notFit); // из-за эвристики с not могут быть накладки
        changeTable.drop(sql);
        return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(fit,notFit);
    }

    public boolean noOld() {
        return getOldDepends().isEmpty();
    }
    private OldProperty<T> old;
    public OldProperty<T> getOld() {
        if(old==null) {
            assert noOld();
            old = new OldProperty<T>(this);
        }
        return old;
    }

    @IdentityLazy
    public ChangedProperty<T> getChanged(IncrementType type) {
        return new ChangedProperty<T>(this, type);
    }

    public boolean noDB() {
        return !noOld();
    }

    protected Expr getClassTableExpr(Map<T, ? extends Expr> joinImplement) {
        ClassTable<T> classTable = getClassTable();
        return classTable.join(join(classTable.mapFields, joinImplement)).getExpr(classTable.propValue);
    }

    @IdentityLazy
    public ClassTable<T> getClassTable() {
        return new ClassTable<T>(this);
    }

    public static class ClassTable<P extends PropertyInterface> extends Table {

        public final Map<KeyField, P> mapFields;
        public final PropertyField propValue;

        public ClassTable(CalcProperty<P> property) {
            super(property.getSID());

            mapFields = new HashMap<KeyField, P>();
            for(P propInterface : property.interfaces) {
                KeyField key = new KeyField(propInterface.getSID(), property.getInterfaceType(propInterface));
                keys.add(key); // чтобы порядок сохранить, хотя может и не критично
                mapFields.put(key, propInterface);
            }

            ValueClass valueClass = property.getValueClass();
            propValue = new PropertyField("value", valueClass.getType());
            properties.add(propValue);

            classes = property.getClassWhere(true).remap(BaseUtils.reverse(mapFields)); // true потому как может быть old не полный (в частности NewSessionAction)
            propertyClasses.put(propValue, BaseUtils.<ClassWhere<Field>>immutableCast(classes).and(new ClassWhere<Field>(propValue, valueClass.getUpSet())));
        }

        public StatKeys<KeyField> getStatKeys() {
            return getStatKeys(this, 100);
        }

        public Map<PropertyField, Stat> getStatProps() {
            return getStatProps(this, 100);
        }
    }

    // есть assertion, что не должен возвращать изменение null -> null, то есть или старое или новое не null, для подр. см usage
    public PropertyChange<T> getIncrementChange(Modifier modifier) {
        return getIncrementChange(modifier.getPropertyChanges());
    }

    public PropertyChange<T> getIncrementChange(PropertyChanges propChanges) {
        IQuery<T, String> incrementQuery = getQuery(propChanges, PropertyQueryType.FULLCHANGED, new HashMap<T, Expr>());
        return new PropertyChange<T>(incrementQuery.getMapKeys(), incrementQuery.getExpr("value"), incrementQuery.getExpr("changed").getWhere());
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier, WhereBuilder resultChanged) {
        return getIncrementExpr(joinImplement, resultChanged, false, modifier.getPropertyChanges(), IncrementType.SUSPICION);
    }

    public Expr getIncrementExpr(Map<T, ? extends Expr> joinImplement, WhereBuilder resultChanged, boolean propClasses, PropertyChanges propChanges, IncrementType incrementType) {
        WhereBuilder incrementWhere = propClasses ? null : new WhereBuilder();
        Expr newExpr = getExpr(joinImplement, propClasses, propChanges, incrementWhere);
        Expr prevExpr = getOld().getExpr(joinImplement, propClasses, propChanges, incrementWhere);

        Where forceWhere;
        switch(incrementType) {
            case CHANGESET:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere()).and(newExpr.getWhere().and(prevExpr.getWhere()).not());
                break;
            case SET:
                forceWhere = newExpr.getWhere().and(prevExpr.getWhere().not());
                break;
            case DROP:
                forceWhere = newExpr.getWhere().not().and(prevExpr.getWhere());
                break;
            case CHANGE:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere()).and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case LEFTCHANGE:
                forceWhere = newExpr.getWhere().and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case SUSPICION:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere());
                break;
            default:
                throw new RuntimeException("should not be");
        }
        if(!propClasses)
            forceWhere = forceWhere.and(incrementWhere.toWhere());
        resultChanged.add(forceWhere);
        return newExpr;
    }

    public final Type.Getter<T> interfaceTypeGetter = new Type.Getter<T>() {
        public Type getType(T key) {
            return getInterfaceType(key);
        }
    };

    public SinglePropertyTableUsage<T> createChangeTable() {
        return new SinglePropertyTableUsage<T>(new ArrayList<T>(interfaces), interfaceTypeGetter, getType());
    }

    @Message("message.increment.read.properties")
    @ThisMessage
    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, Modifier modifier, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        return readFixChangeTable(session, getIncrementChange(modifier), baseClass, env);
    }

    public SinglePropertyTableUsage<T> readFixChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        SinglePropertyTableUsage<T> readTable = readChangeTable(session, change, baseClass, env);

        // при вызове readChangeTable, используется assertion (см. assert fitKeyClasses) что если таблица подходит по классам для значения, то подходит по классам и для ключей
        // этот assertion может нарушаться если определилось конкретное значение и оно было null, как правило с комбинаторными event'ами (вообще может нарушиться и если не null, но так как propertyClasses просто вырезаются то не может), соответственно необходимо устранить этот случай
        readTable.fixKeyClasses(getClassWhere());

        return readTable;
    }

    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        SinglePropertyTableUsage<T> changeTable = createChangeTable();
        change.writeRows(changeTable, session, baseClass, env);
        return changeTable;
    }

    @IdentityLazy
    public <P extends PropertyInterface> MaxChangeProperty<T, P> getMaxChangeProperty(CalcProperty<P> change) {
        return new MaxChangeProperty<T, P>(this, change);
    }
    @IdentityLazy
    public <P extends PropertyInterface> OnChangeProperty<T, P> getOnChangeProperty(CalcProperty<P> change) {
        return new OnChangeProperty<T, P>(this, change);
    }

    public enum CheckType { CHECK_NO, CHECK_ALL, CHECK_SOME }
    public CheckType checkChange = CheckType.CHECK_NO;
    public List<CalcProperty<?>> checkProperties = null;

    public Collection<MaxChangeProperty<?, T>> getMaxChangeProperties(Collection<CalcProperty> properties) {
        Collection<MaxChangeProperty<?, T>> result = new ArrayList<MaxChangeProperty<?, T>>();
        for (CalcProperty<?> property : properties)
            if (depends(property, this))
                result.add(property.getMaxChangeProperty(this));
        return result;
    }

    public PropertyChanges getChangeModifier(PropertyChanges changes, boolean toNull) {
        // строим Where для изменения
        return getPullDataChanges(changes, toNull).add(changes);
    }

    @IdentityLazy
    public QuickSet<CalcProperty> getRecDepends() {
        return calculateRecDepends();
    }

    public QuickSet<CalcProperty> calculateRecDepends() {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CalcProperty<?> depend : getDepends())
            result.addAll(depend.getRecDepends());
        result.add(this);
        return result;
    }

    public List<Pair<Property<?>, LinkType>> actionChangeProps = new ArrayList<Pair<Property<?>, LinkType>>(); // только у Data и IsClassProperty

    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();

        for(CalcProperty depend : getDepends())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.DEPEND));

        return result;
    }

    protected void fillDepends(Set<CalcProperty> depends, boolean events) {
    }

    public Set<CalcProperty> getDepends(boolean events) {
        Set<CalcProperty> depends = new HashSet<CalcProperty>();
        fillDepends(depends, events);
        return depends;
    }

    public Set<CalcProperty> getDepends() {
        return getDepends(true);
    }

    @IdentityLazy
    public Set<SessionCalcProperty> getSessionCalcDepends() {
        Set<SessionCalcProperty> result = new HashSet<SessionCalcProperty>();
        for(CalcProperty<?> property : getDepends(false)) // derived'ы в общем то не интересуют так как используется в singleApply
            result.addAll(property.getSessionCalcDepends());
        return result;
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(Map<T, DataObject> mapValues, Map<T, ConcreteClass> mapClasses) {
        return (CustomClass)getValueClass();
/*        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for (Map.Entry<T, DataObject> keyField : mapValues.entrySet())
            mapExprs.put(keyField.getKey(), new ValueExpr(keyField.getValue().object, mapClasses.get(keyField.getKey())));
        return (CustomClass) new Query<String, String>(new HashMap<String, KeyExpr>(), getClassExpr(mapExprs), "value").
                getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();*/
    }

    public boolean hasChanges(Modifier modifier) {
        return hasChanges(modifier.getPropertyChanges());
    }
    public boolean hasChanges(PropertyChanges propChanges) {
        return hasChanges(propChanges, false);
    }
    public boolean hasChanges(PropertyChanges propChanges, boolean cascade) {
        return hasChanges(propChanges.getStruct(), cascade);
    }
    public boolean hasChanges(StructChanges propChanges, boolean cascade) {
        return propChanges.hasChanges(getUsedChanges(propChanges, cascade));
    }
    public static Set<CalcProperty> hasChanges(Collection<CalcProperty> properties, StructChanges propChanges, boolean cascade) {
        Set<CalcProperty> result = new HashSet<CalcProperty>();
        for (CalcProperty<?> updateProperty : properties)
            if (updateProperty.hasChanges(propChanges, cascade))
                result.add(updateProperty);
        return result;
    }

    // возвращает от чего "зависят" изменения - с callback'ов
    protected abstract QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade);

    public QuickSet<CalcProperty> getUsedChanges(StructChanges propChanges, boolean cascade) {
        if(propChanges.isEmpty()) // чтобы рекурсию разбить
            return QuickSet.EMPTY();

        QuickSet<CalcProperty> usedChanges;
        QuickSet<CalcProperty> modifyChanges = propChanges.getUsedChanges((CalcProperty) this, cascade);
        if(propChanges.hasChanges(modifyChanges) || (propChanges.hasChanges(usedChanges  = calculateUsedChanges(propChanges, cascade)) && !modifyChanges.isEmpty()))
            return modifyChanges;
        return usedChanges;
    }

    protected abstract Expr calculateExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere);

    public Expr aspectGetExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert joinImplement.size() == interfaces.size();

        WhereBuilder changedExprWhere = new WhereBuilder();
        Expr changedExpr = null;

        if(!propClasses) // чтобы не вызывать derived'ы
            changedExpr = propChanges.getChangeExpr(this, joinImplement, changedExprWhere);

        if (changedExpr == null && isStored()) {
            if (!hasChanges(propChanges)) // если нету изменений
                return mapTable.table.join(join(BaseUtils.reverse(mapTable.mapKeys), joinImplement)).getExpr(field);
            if (useSimpleIncrement())
                changedExpr = calculateExpr(joinImplement, propClasses, propChanges, changedExprWhere);
        }

        if (changedExpr != null) {
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
        }

        return calculateExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent
    public PropertyField field;
    public ClassWhere<Field> fieldClassWhere;

    public boolean aggProp;

    public void markStored(TableFactory tableFactory) {
        markStored(tableFactory, null);
    }

    public void markStored(TableFactory tableFactory, ImplementTable table) {
        MapKeysTable<T> mapTable = null;

        Map<T, ValueClass> keyClasses = getInterfaceClasses();
        if (table != null) {
            mapTable = table.getMapKeysTable(keyClasses);
        }
        if (mapTable == null) {
            mapTable = tableFactory.getMapTable(keyClasses);
        }

        PropertyField field = new PropertyField(getSID(), getType());
        fieldClassWhere = getClassWhere(mapTable, field);
        mapTable.table.addField(field, fieldClassWhere);

        this.mapTable = mapTable;
        this.field = field;
    }

    public ValueClass getValueClass() {
        return getClassValueWhere().getCommonParent(Collections.singleton("value")).get("value");
    }

    @IdentityLazy
    public Map<T, ValueClass> getInterfaceClasses(boolean full) {
        return getClassWhere(full).getCommonParent(interfaces);
    }
    @IdentityLazy
    public ClassWhere<T> getClassWhere(boolean full) {
        ClassWhere<T> result = getClassValueWhere().keep(interfaces);
        if(full) // тут по идее assert что result => icommon, но так как и сделано для того случая когда не хватает ключей
            result = result.and(new ClassWhere<T>(getInterfaceCommonClasses(null), true));
        return result;
    }

    protected abstract ClassWhere<Object> getClassValueWhere();

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere().remap(BaseUtils.<Object, T, String, Field>merge(mapTable.mapKeys, Collections.singletonMap("value", storedField)));
    }

    public Object read(ExecutionContext context) throws SQLException {
        return read(context.getSession().sql, new HashMap(), context.getModifier(), context.getQueryEnv());
    }

    public Object read(SQLSession session, Map<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException {
        String readValue = "readvalue";
        Query<T, Object> readQuery = new Query<T, Object>(new ArrayList<T>());
        readQuery.properties.put(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier));
        return BaseUtils.singleValue(readQuery.execute(session, env)).get(readValue);
    }

    public Object read(FormInstance form, Map<T, ? extends ObjectValue> keys) throws SQLException {
        return read(form.session.sql, keys, form.getModifier(), form.getQueryEnv());
    }

    public ObjectValue readClasses(DataSession session, Map<T, DataObject> keys, Modifier modifier, QueryEnvironment env) throws SQLException {
        return session.getObjectValue(read(session.sql, keys, modifier, env), getType());
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean useSimpleIncrement();


    public PropertyChanges getUsedDataChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedDataChanges(propChanges.getStruct()));
    }

    public QuickSet<CalcProperty> getUsedDataChanges(StructChanges propChanges) {
        return calculateUsedDataChanges(propChanges);
    }

    public DataChanges getDataChanges(PropertyChange<T> change, Modifier modifier) {
        return getDataChanges(change, modifier.getPropertyChanges());
    }

    public DataChanges getDataChanges(PropertyChange<T> change, PropertyChanges propChanges) {
        return getDataChanges(change, propChanges, null);
    }

    public Collection<DataProperty> getChangeProps() { // дублирует getDataChanges, но по сложности не вытягивает нижний механизм
//        Map<T, KeyExpr> mapKeys = getMapKeys();
//        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL : changeExpr, CompareWhere.compare(mapKeys, getChangeExprs())), changes, null);
        return new HashSet<DataProperty>();
    }

    protected DataChanges getPullDataChanges(PropertyChanges changes, boolean toNull) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL : changeExpr, CompareWhere.compare(mapKeys, getChangeExprs())), changes, null);
    }

    protected DataChanges getJoinDataChanges(Map<T, ? extends Expr> implementExprs, Expr expr, Where where, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        DataChanges result = getDataChanges(new PropertyChange<T>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, GroupType.ANY, mapKeys),
                GroupExpr.create(implementExprs, where, mapKeys).getWhere()),
                propChanges, changedImplementWhere);
        if (changedWhere != null)
            changedWhere.add(new Query<T, Object>(mapKeys, changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    @Message("message.core.property.data.changes")
    @PackComplex
    @ThisMessage
    public DataChanges getDataChanges(PropertyChange<T> change, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (!change.where.isFalse())
            return calculateDataChanges(change, changedWhere, propChanges);
        return new DataChanges();
    }

    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return QuickSet.EMPTY();
    }

    // для оболочки чтобы всем getDataChanges можно было бы timeChanges вставить
    protected DataChanges calculateDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return new DataChanges();
    }

    public Set<CalcProperty> getDataChangeProps() {
        return new HashSet<CalcProperty>();
    }

    public Map<T, Expr> getChangeExprs() {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for (T propertyInterface : interfaces)
            result.put(propertyInterface, propertyInterface.changeExpr);
        return result;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public final Expr changeExpr;

    public <D extends PropertyInterface> void setEventChange(boolean valueChanged, IncrementType incrementType, CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>> valueImplement, List<CalcPropertyMapImplement<?, T>> whereImplements, Collection<CalcPropertyMapImplement<?, T>> onChangeImplements) {
        // нужно onChange обернуть в getChange, and where, and change implement'ы
        if(!valueChanged)
            valueImplement = new CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>>(valueImplement.property.getOld(), valueImplement.mapping);

        List<CalcPropertyMapImplement<?, T>> onChangeWhereImplements = new ArrayList<CalcPropertyMapImplement<?, T>>();
        for(CalcPropertyMapImplement<?, T> onChangeImplement : onChangeImplements)
            onChangeWhereImplements.add(onChangeImplement.mapChanged(incrementType));
        for(CalcPropertyInterfaceImplement<T> mapping : valueImplement.mapping.values())
            if(mapping instanceof CalcPropertyMapImplement)
                onChangeWhereImplements.add(((CalcPropertyMapImplement<?, T>) mapping).mapChanged(IncrementType.CHANGE));

        CalcPropertyMapImplement<?, T> where;
        if(onChangeWhereImplements.size() > 0) {
            if(onChangeWhereImplements.size()==1)
                where = BaseUtils.single(onChangeWhereImplements);
            else
                where = DerivedProperty.createUnion(interfaces, onChangeWhereImplements);
            if(whereImplements.size()>0)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements);
        } else { // по сути новая ветка, assert что whereImplements > 0
            where = whereImplements.get(0);
            if(whereImplements.size() > 1)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements.subList(1, whereImplements.size()));
        }
        setEventChange(DerivedProperty.createJoin(valueImplement), where);
    }

    public <D extends PropertyInterface, W extends PropertyInterface> void setEventChange(CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement) {
        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET);

        ChangeEvent<T> event = new ChangeEvent<T>(this, valueImplement, whereImplement);
        // запишем в DataProperty
        for(DataProperty dataProperty : getChangeProps()) {
            if(Settings.instance.isCheckUniqueEvent() && dataProperty.event!=null)
                throw new RuntimeException(ServerResourceBundle.getString("logics.property.already.has.event", dataProperty));
            dataProperty.event = event;
        }
    }

    public void setNotNull(Map<T, DataObject> values, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException {
        if(!check || (read(env.getSession().sql, values, env.getModifier(), env.getQueryEnv())!=null) != notNull) {
            ActionPropertyMapImplement<?, T> action = getSetNotNullAction(notNull);
            if(action!=null)
                action.execute(new ExecutionContext<T>(values , null, null, env, null));
        }
    }
    public void setNotNull(Map<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull) throws SQLException {
        for(Map<T, DataObject> row : new Query<T, Object>(mapKeys, where).executeClasses(env).keySet())
            setNotNull(row, env, notNull, true);
    }

    protected DataObject getDefaultObjectValue() {
        Type type = getType();
        if(type instanceof DataClass)
            return ((DataClass) type).getDefaultObjectValue();
        else
            return null;
    }

    public ActionPropertyMapImplement<?, T> getSetNotNullAction(boolean notNull) {
        if(notNull) {
            DataObject defaultValue = getDefaultObjectValue();
            if(defaultValue!=null)
                return DerivedProperty.createSetAction(interfaces, getImplement(), DerivedProperty.<T>createStatic(defaultValue.object, (DataClass)defaultValue.objectClass));
            return null;
        } else
            return DerivedProperty.createSetAction(interfaces, getImplement(), DerivedProperty.<T>createNull());
    }

    public QuickSet<CalcProperty> getUsedEventChange(StructChanges propChanges, boolean cascade) {
        return QuickSet.EMPTY();
    }

    protected boolean assertPropClasses(boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return !propClasses || (changes.isEmpty() && changedWhere==null);
    }

    public CalcPropertyMapImplement<T, T> getImplement() {
        return new CalcPropertyMapImplement<T, T>(this, getIdentityInterfaces());
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<T, V> getImplement(List<V> list) {
        return new CalcPropertyMapImplement<T, V>(this, getMapInterfaces(list));
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<T, V> getImplement(V... list) {
        return getImplement(BaseUtils.toList(list));
    }


    @IdentityLazy
    public ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        List<T> listInterfaces = new ArrayList<T>();
        List<ValueClass> listValues = new ArrayList<ValueClass>();
        for(Map.Entry<T, ValueClass> mapClass : getInterfaceClasses().entrySet()) {
            listInterfaces.add(mapClass.getKey());
            listValues.add(mapClass.getValue());
        }
        DefaultChangeActionProperty<T> changeActionProperty = new DefaultChangeActionProperty<T>("DE" + getSID() + "_" + editActionSID, "sys", this, listInterfaces, listValues, editActionSID, filterProperty);
        return changeActionProperty.getImplement(listInterfaces);
    }

    public boolean setNotNull;

    protected PropertyClassImplement<T, ?> createClassImplement(List<ValueClassWrapper> classes, List<T> mapping) {
        return new CalcPropertyClassImplement<T>(this, classes, mapping);
    }

    private LCP logProperty;

    public LCP getLogProperty() {
        return logProperty;
    }

    public void setLogProperty(LCP logProperty) {
        this.logProperty = logProperty;
    }

    public boolean autoset;

    public static ValueClass or(ValueClass v1, ValueClass v2) {
        if(v1==null)
            return v2;
        if(v2==null)
            return v1;
        return v1.getUpSet().getOr().or(v2.getUpSet().getOr()).getCommonClass();
    }

    public static <T extends PropertyInterface> Map<T, ValueClass> or(Collection<T> interfaces, Map<T, ValueClass> or1, Map<T, ValueClass> or2) {
        Map<T, ValueClass> result = new HashMap<T, ValueClass>();
        for(T propInterface : interfaces) {
            ValueClass or = or(or1.get(propInterface), or2.get(propInterface));
            if(or!=null)
                result.put(propInterface, or);
        }
        return result;
    }
    public Map<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) { // эвристично определяет классы, для входных значений
        return getInterfaceClasses();
    }

    // костыль для email
    public static <I extends PropertyInterface> ValueClass[] getCommonClasses(List<I> mapInterfaces, Collection<? extends CalcPropertyInterfaceImplement<I>> props) {
        ValueClass[] result = new ValueClass[mapInterfaces.size()];
        for(PropertyInterfaceImplement<I> prop : props) {
            Map<I, ValueClass> propClasses;
            if(prop instanceof CalcPropertyMapImplement)
                propClasses = ((CalcPropertyMapImplement<?, I>) prop).mapInterfaceClasses();
            else
                propClasses = new HashMap<I, ValueClass>();

            for(int i=0;i<result.length;i++)
                result[i] = or(result[i], propClasses.get(mapInterfaces.get(i)));
        }
        return result;
    }

    public QuickSet<CalcProperty> getUsedChanges(StructChanges propChanges) {
        return getUsedChanges(propChanges, false);
    }

    public PropertyChanges getUsedChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedChanges(propChanges.getStruct()));
    }

    public IQuery<T, String> getQuery(PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        return getQuery(false, propChanges, queryType, interfaceValues);
    }

    @PackComplex
    @Message("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(propClasses, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            Query<T, String> fullQuery = new Query<T, String>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.properties.put("value", newExpr);
            fullQuery.properties.put("changed", query.getExpr("changed").and(newExpr.getWhere().or(getExpr(fullQuery.mapKeys).getWhere())));
            return fullQuery;
        }

        Query<T, String> query = new Query<T,String>(BaseUtils.filterNotKeys(getMapKeys(), interfaceValues.keySet()));
        Map<T, Expr> allKeys = BaseUtils.merge(interfaceValues, query.mapKeys);
        WhereBuilder queryWheres = queryType.needChange() ? new WhereBuilder():null;
        query.properties.put("value", aspectGetExpr(allKeys, propClasses, propChanges, queryWheres));
        if(queryType.needChange())
            query.properties.put("changed", ValueExpr.get(queryWheres.toWhere()));
        return query;
    }

    public Expr getQueryExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWheres) {

        Map<T, Expr> interfaceValues = new HashMap<T, Expr>(); Map<T, Expr> interfaceExprs = new HashMap<T, Expr>();
        for(Map.Entry<T, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey(), entry.getValue());
            else
                interfaceExprs.put(entry.getKey(), entry.getValue());

        IQuery<T, String> query = getQuery(propClasses, propChanges, changedWheres!=null?PropertyQueryType.CHANGED:PropertyQueryType.NOCHANGE, interfaceValues);

        Join<String> queryJoin = query.join(interfaceExprs);
        if(changedWheres!=null)
            changedWheres.add(queryJoin.getExpr("changed").getWhere());
        return queryJoin.getExpr("value");
    }

    @Message("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, PropertyChanges.EMPTY);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getExpr(joinImplement, modifier.getPropertyChanges());
    }
    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return getExpr(joinImplement, propChanges, null);
    }

    public Expr getExpr(Map<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getExpr(joinImplement, false, propChanges, changedWhere);
    }

    // в будущем propClasses можно заменить на PropertyTables propTables
    public Expr getExpr(Map<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isFull() && (Settings.instance.isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, propClasses, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public void prereadCaches() {
        getClassWhere();
        getClassWhere(true);
        if(isFull())
            getQuery(false, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, new HashMap<T, Expr>()).pack();
    }
}
