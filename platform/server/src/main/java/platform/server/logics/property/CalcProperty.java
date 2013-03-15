package platform.server.logics.property;

import platform.base.*;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.Settings;
import platform.server.ThisMessage;
import platform.server.caches.*;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
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

public abstract class CalcProperty<T extends PropertyInterface> extends Property<T> {

    public static FunctionSet<CalcProperty> getDependsOnSet(final FunctionSet<CalcProperty> check) {
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

    public static FunctionSet<CalcProperty> getDependsFromSet(final ImSet<CalcProperty> check) {
        return new FunctionSet<CalcProperty>() {
            public boolean contains(CalcProperty element) {
                return depends(check, element);
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

    public static boolean depends(CalcProperty<?> property, FunctionSet<? extends CalcProperty> check) {
        return property.getRecDepends().intersect((FunctionSet<CalcProperty>)check);
    }

    public static boolean depends(Iterable<CalcProperty> properties, ImSet<CalcProperty> check) {
        for(CalcProperty property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static boolean depends(ImSet<CalcProperty> properties, CalcProperty check) {
        for(CalcProperty property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static <T extends CalcProperty> ImSet<T> used(ImSet<T> used, final ImSet<CalcProperty> usedIn) {
        return used.filterFn(new SFunctionSet<T>() {
            public boolean contains(T property) {
                return depends(usedIn, property);
            }
        });
    }

    public static <T extends PropertyInterface> boolean dependsImplement(ImCol<CalcPropertyInterfaceImplement<T>> properties, ImSet<CalcProperty> check) {
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
    public <P extends PropertyInterface> boolean intersectFull(CalcProperty<P> property, ImMap<P, T> map) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        return !getExpr(mapKeys).getWhere().and(property.getExpr(map.join(mapKeys)).getWhere()).not().checkTrue();
    }

    protected CalcProperty(String sID, String caption, ImOrderSet<T> interfaces) {
        super(sID, caption, interfaces);
    }

    public void change(ExecutionContext context, Object value) throws SQLException {
        change(context.getEnv(), value);
    }

    public void change(ExecutionEnvironment env, Object value) throws SQLException {
        change(MapFact.<T, DataObject>EMPTY(), env, value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionContext context, Object value) throws SQLException {
        change(keys, context.getEnv(), value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException {
        getImplement().change(keys, env, value);
    }

    public Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>> splitFitClasses(SinglePropertyTableUsage<T> changeTable, SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        assert isStored();

        if(!Settings.get().isEnableApplySingleStored() || DataSession.notFitKeyClasses(this, changeTable)) // оптимизация
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(createChangeTable(), changeTable);
        if(DataSession.fitClasses(this, changeTable))
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(changeTable, createChangeTable());

        PropertyChange<T> change = SinglePropertyTableUsage.getChange(changeTable);

        ImMap<KeyField, Expr> mapKeys = mapTable.mapKeys.crossJoin(change.getMapExprs());
        Where classWhere = fieldClassWhere.getWhere(MapFact.addExcl(mapKeys, field, change.expr))
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

    @IdentityStrongLazy // используется в resolve и кое где еще
    public ChangedProperty<T> getChanged(IncrementType type) {
        return new ChangedProperty<T>(this, type);
    }

    public boolean noDB() {
        return !noOld();
    }

    protected Expr getClassTableExpr(ImMap<T, ? extends Expr> joinImplement) {
        ClassTable<T> classTable = getClassTable();
        return classTable.join(classTable.mapFields.join(joinImplement)).getExpr(classTable.propValue);
    }

    @IdentityStrongLazy
    public ClassTable<T> getClassTable() {
        return new ClassTable<T>(this);
    }

    public static class ClassTable<P extends PropertyInterface> extends Table {

        public final ImRevMap<KeyField, P> mapFields;
        public final PropertyField propValue;

        public ClassTable(final CalcProperty<P> property) {
            super(property.getSID());

            ImRevMap<P, KeyField> revMapFields = property.interfaces.mapRevValues(new GetValue<KeyField, P>() {
                public KeyField getMapValue(P value) {
                    return new KeyField(value.getSID(), property.getInterfaceType(value));
                }});
            mapFields = revMapFields.reverse();
            keys = property.getOrderInterfaces().mapOrder(revMapFields);

            ValueClass valueClass = property.getValueClass();
            propValue = new PropertyField("value", valueClass.getType());
            properties = SetFact.singleton(propValue);

            classes = property.getClassWhere(true).remap(revMapFields); // true потому как может быть old не полный (в частности NewSessionAction)
            propertyClasses = MapFact.singleton(propValue, BaseUtils.<ClassWhere<Field>>immutableCast(classes).and(new ClassWhere<Field>(propValue, valueClass.getUpSet())));
        }

        public StatKeys<KeyField> getStatKeys() {
            return getStatKeys(this, 100);
        }

        public ImMap<PropertyField,PropStat> getStatProps() {
            return getStatProps(this, 100);
        }
    }

    // есть assertion, что не должен возвращать изменение null -> null, то есть или старое или новое не null, для подр. см usage
    public PropertyChange<T> getIncrementChange(Modifier modifier) {
        return getIncrementChange(modifier.getPropertyChanges());
    }

    public PropertyChange<T> getIncrementChange(PropertyChanges propChanges) {
        IQuery<T, String> incrementQuery = getQuery(propChanges, PropertyQueryType.FULLCHANGED, MapFact.<T, Expr>EMPTY());
        return new PropertyChange<T>(incrementQuery.getMapKeys(), incrementQuery.getExpr("value"), incrementQuery.getExpr("changed").getWhere());
    }

    public Expr getIncrementExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier, WhereBuilder resultChanged) {
        return getIncrementExpr(joinImplement, resultChanged, false, modifier.getPropertyChanges(), IncrementType.SUSPICION);
    }

    public Expr getIncrementExpr(ImMap<T, ? extends Expr> joinImplement, WhereBuilder resultChanged, boolean propClasses, PropertyChanges propChanges, IncrementType incrementType) {
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
        return new SinglePropertyTableUsage<T>(getOrderInterfaces(), interfaceTypeGetter, getType());
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

    @IdentityStrongLazy // just in case
    public <P extends PropertyInterface> MaxChangeProperty<T, P> getMaxChangeProperty(CalcProperty<P> change) {
        return new MaxChangeProperty<T, P>(this, change);
    }
    @IdentityStrongLazy // just in case
    public <P extends PropertyInterface> OnChangeProperty<T, P> getOnChangeProperty(CalcProperty<P> change) {
        return new OnChangeProperty<T, P>(this, change);
    }

    public enum CheckType { CHECK_NO, CHECK_ALL, CHECK_SOME }
    public CheckType checkChange = CheckType.CHECK_NO;
    public ImSet<CalcProperty<?>> checkProperties = null;

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
    public ImSet<CalcProperty> getRecDepends() {
        return calculateRecDepends();
    }

    public ImSet<CalcProperty> calculateRecDepends() {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> depend : getDepends())
            mResult.addAll(depend.getRecDepends());
        mResult.add(this);
        return mResult.immutable();
    }

    private MCol<Pair<Property<?>, LinkType>> actionChangeProps; // только у Data и IsClassProperty, чисто для лексикографики
    public void addActionChangeProp(Pair<Property<?>, LinkType> pair) {
        if(actionChangeProps==null)
            actionChangeProps = ListFact.mCol();
        actionChangeProps.add(pair);
    }
    public ImCol<Pair<Property<?>, LinkType>> getActionChangeProps() {
        if(actionChangeProps!=null) {
            ImCol<Pair<Property<?>, LinkType>> result = actionChangeProps.immutableCol();
            actionChangeProps = null;
            return result;
        }

        return SetFact.EMPTY();
    }

    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks() {
        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();

        for(CalcProperty depend : getDepends())
            mResult.add(new Pair<Property<?>, LinkType>(depend, LinkType.DEPEND));

        return mResult.immutableCol();
    }

    protected void fillDepends(MSet<CalcProperty> depends, boolean events) {
    }

    // возвращает от чего "зависят" изменения - с callback'ов, должен коррелировать с getDepends(Rec), который должен включать в себя все calculateUsedChanges
    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        return SetFact.EMPTY();
    }

    public ImSet<CalcProperty> getDepends(boolean events) {
        MSet<CalcProperty> mDepends = SetFact.mSet();
        fillDepends(mDepends, events);
        return mDepends.immutable();
    }

    public ImSet<CalcProperty> getDepends() {
        return getDepends(true);
    }

    @IdentityLazy
    public ImSet<SessionCalcProperty> getSessionCalcDepends() {
        MSet<SessionCalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : getDepends(false)) // derived'ы в общем то не интересуют так как используется в singleApply
            mResult.addAll(property.getSessionCalcDepends());
        return mResult.immutable();
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(ImMap<T, DataObject> mapValues, ImMap<T, ConcreteClass> mapClasses) {
        return (CustomClass)getValueClass();
/*        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for (Map.Entry<T, DataObject> keyField : mapColValues.entrySet())
            mapExprs.put(keyField.getKey(), new ValueExpr(keyField.getValue().object, mapClasses.get(keyField.getKey())));
        return (CustomClass) new Query<String, String>(new HashMap<String, KeyExpr>(), getClassExpr(mapExprs), "value").
                getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();*/
    }

    public boolean hasChanges(Modifier modifier) {
        return hasChanges(modifier.getPropertyChanges());
    }
    public boolean hasChanges(PropertyChanges propChanges) {
        return hasChanges(propChanges.getStruct());
    }
    public boolean hasChanges(StructChanges propChanges) {
        return propChanges.hasChanges(getUsedChanges(propChanges));
    }

    public ImSet<CalcProperty> getUsedChanges(StructChanges propChanges) {
        if(propChanges.isEmpty()) // чтобы рекурсию разбить
            return SetFact.EMPTY();

        ChangeType modifyChanges = propChanges.getUsedChange(this);
        if(modifyChanges!=null)
            return SetFact.add(SetFact.singleton((CalcProperty) this), modifyChanges.isFinal() ? SetFact.<CalcProperty>EMPTY() : getUsedChanges(propChanges.remove(this)));

        return calculateUsedChanges(propChanges);
    }

    protected abstract Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere);

    public Expr aspectGetExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert joinImplement.size() == interfaces.size();

        ModifyChange<T> modify = propChanges.getModify(this);
        if(modify!=null) {
            WhereBuilder changedExprWhere = new WhereBuilder();
            Expr changedExpr = modify.change.getExpr(joinImplement, changedExprWhere);
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement, propClasses, modify.isFinal ? PropertyChanges.EMPTY : propChanges.remove(this), modify.isFinal ? null : changedWhere));
        }

        // modify == null;
        if(isStored()) {
            if(!hasChanges(propChanges)) // propChanges.isEmpty() // если нету изменений
                return mapTable.table.join(mapTable.mapKeys.crossJoin(joinImplement)).getExpr(field);
            if(useSimpleIncrement()) {
                WhereBuilder changedExprWhere = new WhereBuilder();
                Expr changedExpr = calculateExpr(joinImplement, propClasses, propChanges, changedExprWhere);
                if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
                return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
            }
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

        ImMap<T, ValueClass> keyClasses = getInterfaceClasses();
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
        return getClassValueWhere().getCommonParent(SetFact.singleton("value")).get("value");
    }

    @IdentityLazy
    public ImMap<T, ValueClass> getInterfaceClasses(boolean full) {
        return getClassWhere(full).getCommonParent(interfaces);
    }
    @IdentityLazy
    public ClassWhere<T> getClassWhere(boolean full) {
        ClassWhere<T> result = getClassValueWhere(full).filterKeys(interfaces); // не полностью, собсно для этого и есть full
        if(full) // тут по идее assert что result => icommon, но так как и сделано для того случая когда не хватает ключей
            result = result.and(new ClassWhere<T>(getInterfaceCommonClasses(null), true));
        return result;
    }

    protected ClassWhere<Object> getClassValueWhere() {
        return getClassValueWhere(false);
    }

    protected abstract ClassWhere<Object> getClassValueWhere(boolean full);

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere().remap(MapFact.<Object, Field>addRevExcl(mapTable.mapKeys, "value", storedField)); //
    }

    public Object read(ExecutionContext context) throws SQLException {
        return read(context.getSession().sql, MapFact.<T, ObjectValue>EMPTY(), context.getModifier(), context.getQueryEnv());
    }

    public Object read(SQLSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<T, Object>(SetFact.<T>EMPTY());
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier));
        return readQuery.execute(session, env).singleValue().get(readValue);
    }

    public Object read(FormInstance form, ImMap<T, ? extends ObjectValue> keys) throws SQLException {
        return read(form.session.sql, keys, form.getModifier(), form.getQueryEnv());
    }

    public ObjectValue readClasses(DataSession session, ImMap<T, DataObject> keys, Modifier modifier, QueryEnvironment env) throws SQLException {
        return session.getObjectValue(read(session.sql, keys, modifier, env), getType());
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean useSimpleIncrement();


    public PropertyChanges getUsedDataChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedDataChanges(propChanges.getStruct()));
    }

    public ImSet<CalcProperty> getUsedDataChanges(StructChanges propChanges) {
        return calculateUsedDataChanges(propChanges);
    }

    public DataChanges getDataChanges(PropertyChange<T> change, Modifier modifier) {
        return getDataChanges(change, modifier.getPropertyChanges());
    }

    public DataChanges getDataChanges(PropertyChange<T> change, PropertyChanges propChanges) {
        return getDataChanges(change, propChanges, null);
    }

    public ImSet<DataProperty> getChangeProps() { // дублирует getDataChanges, но по сложности не вытягивает нижний механизм
//        Map<T, KeyExpr> mapKeys = getMapKeys();
//        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL : changeExpr, CompareWhere.compare(mapKeys, getChangeExprs())), changes, null);
        return SetFact.<DataProperty>EMPTY();
    }

    protected DataChanges getPullDataChanges(PropertyChanges changes, boolean toNull) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL : getChangeExpr(), CompareWhere.compare(mapKeys, getChangeExprs())), changes, null);
    }

    protected DataChanges getJoinDataChanges(ImMap<T, ? extends Expr> implementExprs, Expr expr, Where where, PropertyChanges propChanges, WhereBuilder changedWhere) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
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
        if (change.where.isFalse()) // оптимизация
            return DataChanges.EMPTY;

        return calculateDataChanges(change, changedWhere, propChanges);
    }

    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return SetFact.EMPTY();
    }

    // для оболочки чтобы всем getDataChanges можно было бы timeChanges вставить
    protected DataChanges calculateDataChanges(PropertyChange<T> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return DataChanges.EMPTY;
    }

    public ImMap<T, Expr> getChangeExprs() {
        return interfaces.mapValues(new GetValue<Expr, T>() {
            public Expr getMapValue(T value) {
                return value.getChangeExpr();
            }});
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    @LazyInit
    public Expr getChangeExpr() {
        if(changeExpr == null)
            changeExpr = new PullExpr(-128);
        return changeExpr;
    }
    public Expr changeExpr;

    public <D extends PropertyInterface> void setEventChange(boolean valueChanged, final IncrementType incrementType, CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>> valueImplement, ImList<CalcPropertyMapImplement<?, T>> whereImplements, ImCol<CalcPropertyMapImplement<?, T>> onChangeImplements) {
        // нужно onChange обернуть в getChange, and where, and change implement'ы
        if(!valueChanged)
            valueImplement = new CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>>(valueImplement.property.getOld(), valueImplement.mapping);

        ImCol<CalcPropertyMapImplement<?, T>> onChangeWhereImplements = onChangeImplements.mapColValues(new GetValue<CalcPropertyMapImplement<?, T>, CalcPropertyMapImplement<?, T>>() {
                    public CalcPropertyMapImplement<?, T> getMapValue(CalcPropertyMapImplement<?, T> value) {
                        return value.mapChanged(incrementType);
                    }}).mergeCol(CalcPropertyMapImplement.filter(valueImplement.mapping.values()).mapColValues(new GetValue<CalcPropertyMapImplement<?, T>, CalcPropertyMapImplement<?, T>>() {
                    public CalcPropertyMapImplement<?, T> getMapValue(CalcPropertyMapImplement<?, T> value) {
                        return value.mapChanged(IncrementType.CHANGE);
                    }}));

        CalcPropertyMapImplement<?, T> where;
        if(onChangeWhereImplements.size() > 0) {
            if(onChangeWhereImplements.size()==1)
                where = onChangeWhereImplements.single();
            else
                where = DerivedProperty.createUnion(interfaces, onChangeWhereImplements.toList());
            if(whereImplements.size()>0)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements.getCol());
        } else { // по сути новая ветка, assert что whereImplements > 0
            where = whereImplements.get(0);
            if(whereImplements.size() > 1)
                where = DerivedProperty.createAnd(interfaces, where, whereImplements.subList(1, whereImplements.size()).getCol());
        }
        setEventChange(DerivedProperty.createJoin(valueImplement), where);
    }

    public <D extends PropertyInterface, W extends PropertyInterface> void setEventChange(CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement) {
        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET);

        ChangeEvent<T> event = new ChangeEvent<T>(this, valueImplement, whereImplement);
        // запишем в DataProperty
        for(DataProperty dataProperty : getChangeProps()) {
            if(Settings.get().isCheckUniqueEvent() && dataProperty.event!=null)
                throw new RuntimeException(ServerResourceBundle.getString("logics.property.already.has.event", dataProperty));
            dataProperty.event = event;
        }
    }

    public void setNotNull(ImMap<T, DataObject> values, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException {
        if(!check || (read(env.getSession().sql, values, env.getModifier(), env.getQueryEnv())!=null) != notNull) {
            ActionPropertyMapImplement<?, T> action = getSetNotNullAction(notNull);
            if(action!=null)
                action.execute(new ExecutionContext<T>(values , null, null, env, null));
        }
    }
    public void setNotNull(ImRevMap<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull) throws SQLException {
        for(ImMap<T, DataObject> row : new Query<T, Object>(mapKeys, where).executeClasses(env).keys())
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

    protected boolean assertPropClasses(boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return !propClasses || (changes.isEmpty() && changedWhere==null);
    }

    public CalcPropertyMapImplement<T, T> getImplement() {
        return new CalcPropertyMapImplement<T, T>(this, getIdentityInterfaces());
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<T, V> getImplement(ImOrderSet<V> list) {
        return new CalcPropertyMapImplement<T, V>(this, getMapInterfaces(list));
    }

    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        ImMap<T, ValueClass> interfaceClasses = getInterfaceClasses();

        ImOrderSet<T> listInterfaces = interfaceClasses.keys().toOrderSet();
        ImList<ValueClass> listValues = listInterfaces.mapList(interfaceClasses);
        DefaultChangeActionProperty<T> changeActionProperty = new DefaultChangeActionProperty<T>("DE" + getSID() + "_" + editActionSID, "sys", this, listInterfaces, listValues, editActionSID, filterProperty);
        return changeActionProperty.getImplement(listInterfaces);
    }

    public boolean setNotNull;

    protected PropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping) {
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

    public static <T extends PropertyInterface> ImMap<T, ValueClass> or(ImSet<T> interfaces, ImMap<T, ValueClass> or1, ImMap<T, ValueClass> or2) {
        ImFilterValueMap<T, ValueClass> mvResult = interfaces.mapFilterValues();
        for(int i=0,size=interfaces.size();i<size;i++) {
            T propInterface = interfaces.get(i);
            ValueClass or = or(or1.get(propInterface), or2.get(propInterface));
            if(or!=null)
                mvResult.mapValue(i, or);
        }
        return mvResult.immutableValue();
    }
    public ImMap<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) { // эвристично определяет классы, для входных значений
        return getClassValueWhere(true).getCommonParent(interfaces);
    }

    // костыль для email
    public static <I extends PropertyInterface> ValueClass[] getCommonClasses(ImList<I> mapInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> props) {
        ValueClass[] result = new ValueClass[mapInterfaces.size()];
        for(PropertyInterfaceImplement<I> prop : props) {
            ImMap<I, ValueClass> propClasses;
            if(prop instanceof CalcPropertyMapImplement)
                propClasses = ((CalcPropertyMapImplement<?, I>) prop).mapInterfaceClasses();
            else
                propClasses = MapFact.EMPTY();

            for(int i=0;i<result.length;i++)
                result[i] = or(result[i], propClasses.get(mapInterfaces.get(i)));
        }
        return result;
    }

    public PropertyChanges getUsedChanges(PropertyChanges propChanges) {
        return propChanges.filter(getUsedChanges(propChanges.getStruct()));
    }

    public IQuery<T, String> getQuery(PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) {
        return getQuery(false, propChanges, queryType, interfaceValues);
    }

    @PackComplex
    @Message("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(boolean propClasses, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(propClasses, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            QueryBuilder<T, String> fullQuery = new QueryBuilder<T, String>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.addProperty("value", newExpr);
            fullQuery.addProperty("changed", query.getExpr("changed").and(newExpr.getWhere().or(getExpr(fullQuery.getMapExprs()).getWhere())));
            return fullQuery.getQuery();
        }

        QueryBuilder<T, String> query = new QueryBuilder<T,String>(getMapKeys().removeRev(interfaceValues.keys()));
        ImMap<T, Expr> allKeys = query.getMapExprs().addExcl(interfaceValues);
        WhereBuilder queryWheres = queryType.needChange() ? new WhereBuilder():null;
        query.addProperty("value", aspectGetExpr(allKeys, propClasses, propChanges, queryWheres));
        if(queryType.needChange())
            query.addProperty("changed", ValueExpr.get(queryWheres.toWhere()));
        return query.getQuery();
    }

    public Expr getQueryExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWheres) {

        MExclMap<T, Expr> mInterfaceValues = MapFact.mExclMap(joinImplement.size()); MExclMap<T, Expr> mInterfaceExprs = MapFact.mExclMap(joinImplement.size());
        for(int i=0,size=joinImplement.size();i<size;i++) {
            Expr expr = joinImplement.getValue(i);
            if(expr.isValue())
                mInterfaceValues.exclAdd(joinImplement.getKey(i), expr);
            else
                mInterfaceExprs.exclAdd(joinImplement.getKey(i), expr);
        }

        IQuery<T, String> query = getQuery(propClasses, propChanges, changedWheres!=null?PropertyQueryType.CHANGED:PropertyQueryType.NOCHANGE, mInterfaceValues.immutable());

        Join<String> queryJoin = query.join(mInterfaceExprs.immutable());
        if(changedWheres!=null)
            changedWheres.add(queryJoin.getExpr("changed").getWhere());
        return queryJoin.getExpr("value");
    }

    @Message("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, PropertyChanges.EMPTY);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier) {
        return getExpr(joinImplement, modifier.getPropertyChanges());
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return getExpr(joinImplement, propChanges, null);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getExpr(joinImplement, false, propChanges, changedWhere);
    }

    // в будущем propClasses можно заменить на PropertyTables propTables
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isFull() && (Settings.get().isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, propClasses, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, propClasses, propChanges, changedWhere);
    }

    public void prereadCaches() {
        getRecDepends();
        getClassWhere();
        getClassWhere(true);
        if(isFull())
            getQuery(false, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, MapFact.<T, Expr>EMPTY()).pack();
    }

    public CalcPropertyMapImplement<?, T> getClassProperty() {
        ClassWhere<T> classWhere = getClassWhere();
        return IsClassProperty.getMapProperty(classWhere.getCommonParent(interfaces));
    }
}
