package lsfusion.server.logics.property;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.interop.Compare;
import lsfusion.server.Message;
import lsfusion.server.Settings;
import lsfusion.server.ThisMessage;
import lsfusion.server.caches.*;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.ChangeEvent;
import lsfusion.server.logics.property.actions.edit.DefaultChangeActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.derived.MaxChangeProperty;
import lsfusion.server.logics.property.derived.OnChangeProperty;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.table.MapKeysTable;
import lsfusion.server.logics.table.TableFactory;
import lsfusion.server.session.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class CalcProperty<T extends PropertyInterface> extends Property<T> implements MapKeysInterface<T> {

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

    public static boolean dependsSet(CalcProperty<?> property, FunctionSet<? extends CalcProperty>... checks) {
        for(FunctionSet<? extends CalcProperty> check : checks)
            if(depends(property, check))
                return true;
        return false;
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

    public boolean isEnabledSingleApply() {
        assert isStored();
        return Settings.get().isEnabledApplySingleStored();
    }
    public boolean isSingleApplyStored() { // нужен для ClassDataProperty, для которого отдельный принцип обработки
        return isStored() && isEnabledSingleApply();
    }

    public String outputStored(boolean outputTable) {
        assert isStored() && field!=null;
        return (this instanceof DataProperty? ServerResourceBundle.getString("logics.property.primary"):ServerResourceBundle.getString("logics.property.calculated")) + " "+ServerResourceBundle.getString("logics.property")+" : " + caption+", "+mapTable.table.outputField(field, outputTable);
    }
    
    public void outClasses(DataSession session, Modifier modifier) throws SQLException, SQLHandledException {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        new Query<T, String>(mapKeys, getExpr(mapKeys, modifier), "value").outClassesSelect(session.sql, session.baseClass);
    }

    // по выражениям проверяет
    public <P extends PropertyInterface> boolean intersectFull(CalcProperty<P> property, ImMap<P, T> map) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        return !getExpr(mapKeys).getWhere().and(property.getExpr(map.join(mapKeys)).getWhere()).not().checkTrue();
    }

    protected CalcProperty(String sID, String caption, ImOrderSet<T> interfaces) {
        super(sID, caption, interfaces);
    }

    public void change(ExecutionContext context, Object value) throws SQLException, SQLHandledException {
        change(context.getEnv(), value);
    }

    public void change(ExecutionEnvironment env, Object value) throws SQLException, SQLHandledException {
        change(MapFact.<T, DataObject>EMPTY(), env, value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionContext context, Object value) throws SQLException, SQLHandledException {
        change(keys, context.getEnv(), value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, ObjectValue value) throws SQLException, SQLHandledException {
        getImplement().change(keys, env, value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException, SQLHandledException {
        getImplement().change(keys, env, value);
    }

    public Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>> splitSingleApplyClasses(SinglePropertyTableUsage<T> changeTable, SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        assert isSingleApplyStored();

        if(DataSession.notFitKeyClasses(this, changeTable)) // оптимизация
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

        OperationOwner owner = env.getOpOwner();
        try {
            SinglePropertyTableUsage<T> fit = readChangeTable(sql, change.and(classWhere), baseClass, env);
            SinglePropertyTableUsage<T> notFit;
            try {
                notFit = readChangeTable(sql, change.and(classWhere.not()), baseClass, env);
            } catch (Throwable e) {
                fit.drop(sql, owner);
                throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
            }
            assert DataSession.fitClasses(this, fit);
            assert DataSession.fitKeyClasses(this, fit);

            // это была не совсем правильная эвристика, например если изменение было таблицей с классом X, а свойство принадлежность классу Y, то X and not Y превращался назад в X (если X не равнялся / наследовался от Y)
            // для того чтобы этот assertion продолжил работать надо совершенствовать ClassWhere.andNot, что пока нецелесообразность
            // assert DataSession.notFitClasses(this, notFit);
            return new Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>>(fit,notFit);
        } finally {
            changeTable.drop(sql, owner);
        }
    }

    public boolean noOld() { // именно так, а не через getSessionCalcDepends, так как может использоваться до инициализации логики
        return getParseOldDepends().isEmpty();
    }
    public ImSet<OldProperty> getParseOldDepends() {
        MSet<OldProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : getDepends(false))
            mResult.addAll(property.getParseOldDepends());
        return mResult.immutable();
    }
    @IdentityStrongLazy // используется много где
    public OldProperty<T> getOld(PrevScope scope) {
        return new OldProperty<T>(this, scope);
    }

    @IdentityStrongLazy // используется в resolve и кое где еще
    public ChangedProperty<T> getChanged(IncrementType type, PrevScope scope) {
        return new ChangedProperty<T>(this, type, scope);
    }

    public boolean noDB() {
        return !noOld();
    }

    protected Expr getClassTableExpr(ImMap<T, ? extends Expr> joinImplement, CalcType classCalcType) {
        ClassTable<T> classTable = getClassTable(classCalcType.getPrevClasses());
        return classTable.join(classTable.mapFields.join(joinImplement)).getExpr(classTable.propValue);
    }

    @IdentityStrongLazy
    public ClassTable<T> getClassTable(PrevClasses prevSameClasses) {
        return new ClassTable<T>(this, prevSameClasses);
    }

    public static class ClassTable<P extends PropertyInterface> extends Table {

        public final ImRevMap<KeyField, P> mapFields;
        public final PropertyField propValue;

        public ClassTable(final CalcProperty<P> property, PrevClasses prevSameClasses) {
            super(property.getSID());

            ImRevMap<P, KeyField> revMapFields = property.interfaces.mapRevValues(new GetValue<KeyField, P>() {
                public KeyField getMapValue(P value) {
                    return new KeyField(value.getSID(), property.getInterfaceType(value));
                }});
            mapFields = revMapFields.reverse();
            keys = property.getOrderInterfaces().mapOrder(revMapFields);

            ValueClass valueClass = property.getValueClass(prevSameClasses);
            propValue = new PropertyField("value", valueClass.getType());
            properties = SetFact.singleton(propValue);

            classes = property.getClassWhere(ClassType.ASSERTFULL, prevSameClasses).remap(revMapFields);
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
    @LogTime
    public PropertyChange<T> getIncrementChange(Modifier modifier) throws SQLException, SQLHandledException {
        return getIncrementChange(modifier.getPropertyChanges());
    }

    public PropertyChange<T> getIncrementChange(PropertyChanges propChanges) {
        IQuery<T, String> incrementQuery = getQuery(CalcType.EXPR, propChanges, PropertyQueryType.FULLCHANGED, MapFact.<T, Expr>EMPTY());
        return new PropertyChange<T>(incrementQuery.getMapKeys(), incrementQuery.getExpr("value"), incrementQuery.getExpr("changed").getWhere());
    }

    public Expr getIncrementExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder resultChanged) {
        return getIncrementExpr(joinImplement, resultChanged, CalcType.EXPR, propChanges, IncrementType.SUSPICION, PrevScope.DB); // тут не важно какой scope
    }

    public Expr getIncrementExpr(ImMap<T, ? extends Expr> joinImplement, WhereBuilder resultChanged, CalcType calcType, PropertyChanges propChanges, IncrementType incrementType, PrevScope scope) {
        boolean isNotExpr = !calcType.isExpr();
        WhereBuilder incrementWhere = isNotExpr ? null : new WhereBuilder();
        Expr newExpr = getExpr(joinImplement, calcType, propChanges, incrementWhere);
        Expr prevExpr = getOld(scope).getExpr(joinImplement, calcType, propChanges, incrementWhere);

        Where forceWhere;
        switch(incrementType) {
            case SET:
                forceWhere = newExpr.getWhere().and(prevExpr.getWhere().not());
                break;
            case DROP:
                forceWhere = newExpr.getWhere().not().and(prevExpr.getWhere());
                break;
            case CHANGED:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere()).and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case SETCHANGED:
                forceWhere = newExpr.getWhere().and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case DROPCHANGED:
                forceWhere = prevExpr.getWhere().and(newExpr.compare(prevExpr, Compare.EQUALS).not());
                break;
            case DROPSET:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere()).and(newExpr.getWhere().and(prevExpr.getWhere()).not());
                break;
            case SUSPICION:
                forceWhere = newExpr.getWhere().or(prevExpr.getWhere());
                break;
            default:
                throw new RuntimeException("should not be");
        }
        if(!isNotExpr)
            forceWhere = forceWhere.and(incrementWhere.toWhere());
        resultChanged.add(forceWhere);
        return newExpr;
    }

    public final Type.Getter<T> interfaceTypeGetter = new Type.Getter<T>() {
        public Type getType(T key) {
            return getInterfaceType(key);
        }
    };

    @IdentityInstanceLazy
    public ImRevMap<T, KeyExpr> getMapKeys() {
//        assert isFull();
        return KeyExpr.getMapKeys(interfaces);
    }

    @IdentityInstanceLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<T>(getMapKeys(), CaseExpr.NULL);
    }

    public SinglePropertyTableUsage<T> createChangeTable() {
        return new SinglePropertyTableUsage<T>(getOrderInterfaces(), interfaceTypeGetter, getType());
    }

    @Message("message.increment.read.properties")
    @ThisMessage
    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, Modifier modifier, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        return readFixChangeTable(session, getIncrementChange(modifier), baseClass, env);
    }

    public SinglePropertyTableUsage<T> readFixChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        SinglePropertyTableUsage<T> readTable = readChangeTable(session, change, baseClass, env);

        // при вызове readChangeTable, используется assertion (см. assert fitKeyClasses) что если таблица подходит по классам для значения, то подходит по классам и для ключей
        // этот assertion может нарушаться если определилось конкретное значение и оно было null, как правило с комбинаторными event'ами (вообще может нарушиться и если не null, но так как propertyClasses просто вырезаются то не может), соответственно необходимо устранить этот случай
        readTable.fixKeyClasses(getClassWhere(ClassType.ASSERTFULL));
        readTable.checkClasses(session, null); // нужен как раз для проверки fixKeyClasses

        return readTable;
    }

    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
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

    private ImSet<CalcProperty> recDepends;
    @ManualLazy
    public ImSet<CalcProperty> getRecDepends() {
        if(recDepends == null)
            recDepends = calculateRecDepends();
        return recDepends;
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
        if(actionChangeProps!=null)
            return actionChangeProps.immutableCol();

        return SetFact.EMPTY();
    }
    public void dropActionChangeProps() {
        actionChangeProps = null;
    }

    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean calcEvents) {
        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();

        for(CalcProperty depend : getDepends(calcEvents))
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

    public boolean complex = false;
    @IdentityLazy
    public boolean isComplex() {
        if(complex)
            return true;

        for(CalcProperty property : getDepends())
            if(property.isComplex())
                return true;
        return false;
    }

    @IdentityLazy
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionCalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : getDepends(events)) // derived'ы в общем то не интересуют так как используется в singleApply
            mResult.addAll(property.getSessionCalcDepends(events));
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

    protected abstract Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere);

    public static <T extends PropertyInterface> ImMap<T, Expr> getJoinValues(ImMap<T, ? extends Expr> joinImplement) {
        return ((ImMap<T, Expr>)joinImplement).filterFnValues(new SFunctionSet<Expr>() {
            public boolean contains(Expr joinExpr) {
                return joinExpr.isValue();
            }});
    }

    public static <T extends PropertyInterface> ImMap<T, Expr> onlyComplex(ImMap<T, ? extends Expr> joinImplement) { //assert все Expr.isValue
        return ((ImMap<T, Expr>)joinImplement).filterFnValues(new SFunctionSet<Expr>() {
            public boolean contains(Expr joinExpr) {
                return !(joinExpr instanceof ValueExpr) && !joinExpr.isNull();
            }
        });
    }

    public Expr aspectGetExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert joinImplement.size() == interfaces.size();

        ModifyChange<T> modify = propChanges.getModify(this);
        if(modify!=null) {
            if(complex) { // вообще rightJoin, но вдруг случайно мимо AutoHint'а может пройти
                ImMap<T, Expr> joinValues = getJoinValues(joinImplement); Pair<ObjectValue, Boolean> row;
                if(joinValues!=null && (row = modify.preread.readValues.get(joinValues))!=null) {
                    if(changedWhere!=null) changedWhere.add(row.second ? Where.TRUE : Where.FALSE);
                    return row.first.getExpr();
                }

                joinImplement = MapFact.override(joinImplement, ObjectValue.getMapExprs(onlyComplex(joinValues).innerJoin(modify.preread.readParams)));
            }

            WhereBuilder changedExprWhere = new WhereBuilder();
            Expr changedExpr = modify.change.getExpr(joinImplement, changedExprWhere);
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement, calcType, modify.isFinal ? PropertyChanges.EMPTY : propChanges.remove(this), modify.isFinal ? null : changedWhere));
        }

        // modify == null;
        if(isStored()) {
            if(!hasChanges(propChanges)) // propChanges.isEmpty() // если нету изменений
                return getStoredExpr(joinImplement);
            if(useSimpleIncrement()) {
                WhereBuilder changedExprWhere = new WhereBuilder();
                Expr changedExpr = calculateExpr(joinImplement, calcType, propChanges, changedExprWhere);
                if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
                return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement));
            }
        }

        return calculateExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    protected Expr getStoredExpr(ImMap<T, ? extends Expr> joinImplement) {
        return mapTable.table.join(mapTable.mapKeys.crossJoin(joinImplement)).getExpr(field);
    }

    public Table.Join.Expr getInconsistentExpr(ImMap<T, ? extends Expr> joinImplement, BaseClass baseClass) {
        Table table = baseClass.getInconsistentTable(mapTable.table);
        return (Table.Join.Expr) table.join(mapTable.mapKeys.crossJoin(joinImplement)).getExpr(field);
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

        ImMap<T, ValueClass> keyClasses = getInterfaceClasses(ClassType.ASSERTFULL);
        if (table != null) {
            mapTable = table.getMapKeysTable(keyClasses);
            assert mapTable!=null;
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

    public ValueClass getValueClass(PrevClasses prevSameClasses) {
        return getClassValueWhere(ClassType.ASIS, prevSameClasses).getCommonParent(SetFact.singleton("value")).get("value");
    }
    
    public AndClassSet getValueClassSet() {
        return getClassValueWhere(ClassType.ASIS).getCommonClass("value");
    }

    public ImMap<T, ValueClass> getInterfaceClasses(ClassType type) {
        return getInterfaceClasses(type, defaultPrevSameClasses);
    }
    @IdentityLazy
    public ImMap<T, ValueClass> getInterfaceClasses(ClassType type, PrevClasses prevSameClasses) {
        return getClassWhere(type, prevSameClasses).getCommonParent(interfaces);
    }
    @IdentityLazy
    public ClassWhere<T> getClassWhere(ClassType type, PrevClasses prevSameClasses) {
        return getClassValueWhere(type, prevSameClasses).filterKeys(interfaces); // не полностью, собсно для этого и есть full
    }

    public ClassWhere<Object> getClassValueWhere(ClassType type) {
        return getClassValueWhere(type, defaultPrevSameClasses);
    }
    protected abstract ClassWhere<Object> getClassValueWhere(ClassType type, PrevClasses prevSameClasses);

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere(ClassType.ASSERTFULL).remap(MapFact.<Object, Field>addRevExcl(mapTable.mapKeys, "value", storedField)); //
    }

    public Object read(ExecutionContext context) throws SQLException, SQLHandledException {
        return read(context.getSession().sql, MapFact.<T, ObjectValue>EMPTY(), context.getModifier(), context.getQueryEnv());
    }

    public Object read(SQLSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<T, Object>(SetFact.<T>EMPTY());
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier));
        return readQuery.execute(session, env).singleValue().get(readValue);
    }

    public ObjectValue readClasses(SQLSession session, ImMap<T, Expr> keys, BaseClass baseClass, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<T, Object>(SetFact.<T>EMPTY());
        readQuery.addProperty(readValue, getExpr(keys, modifier));
        return readQuery.executeClasses(session, env, baseClass).singleValue().get(readValue);
    }

    public Pair<ObjectValue, Boolean> readClassesChanged(SQLSession session, ImMap<T, ObjectValue> keys, BaseClass baseClass, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue"; String readChanged = "readChanged";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<T, Object>(SetFact.<T>EMPTY());
        WhereBuilder changedWhere = new WhereBuilder();
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier, changedWhere));
        readQuery.addProperty(readChanged, ValueExpr.get(changedWhere.toWhere()));
        ImMap<Object, ObjectValue> result = readQuery.executeClasses(session, env, baseClass).singleValue();
        return new Pair<ObjectValue, Boolean>(result.get(readValue), !result.get(readChanged).isNull());
    }

    public Object read(FormInstance form, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return read(form.session.sql, keys, form.getModifier(), form.getQueryEnv());
    }

    public ObjectValue readClasses(FormInstance form, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return readClasses(form.session, keys, form.getModifier(), form.getQueryEnv());
    }

    public ObjectValue readClasses(DataSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        return readClasses(session.sql, ObjectValue.getMapExprs(keys), session.baseClass, modifier, env);
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
            valueImplement = new CalcPropertyImplement<D, CalcPropertyInterfaceImplement<T>>(valueImplement.property.getOld(ChangeEvent.scope), valueImplement.mapping); // вычисляемое событие, нужно значение из базы

        ImCol<CalcPropertyMapImplement<?, T>> onChangeWhereImplements = onChangeImplements.mapColValues(new GetValue<CalcPropertyMapImplement<?, T>, CalcPropertyMapImplement<?, T>>() {
                    public CalcPropertyMapImplement<?, T> getMapValue(CalcPropertyMapImplement<?, T> value) {
                        return value.mapChanged(incrementType, ChangeEvent.scope);
                    }}).mergeCol(CalcPropertyMapImplement.filter(valueImplement.mapping.values()).mapColValues(new GetValue<CalcPropertyMapImplement<?, T>, CalcPropertyMapImplement<?, T>>() {
                    public CalcPropertyMapImplement<?, T> getMapValue(CalcPropertyMapImplement<?, T> value) {
                        return value.mapChanged(IncrementType.CHANGED, ChangeEvent.scope);
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
        setEventChange(null, false, DerivedProperty.createJoin(valueImplement), where);
    }

    public <D extends PropertyInterface, W extends PropertyInterface> void setEventChange(LogicsModule lm, boolean action, CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement) {
        if(action && !Settings.get().isDisableWhenCalcDo()) {
            ActionPropertyMapImplement<?, T> setAction = DerivedProperty.createSetAction(interfaces, getImplement(), valueImplement);
            lm.addEventAction(interfaces, setAction, whereImplement, MapFact.<CalcPropertyInterfaceImplement<T>, Boolean>EMPTYORDER(), false, Event.SESSION, null, true, false);
            return;
        }

        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, ChangeEvent.scope);

        ChangeEvent<T> event = new ChangeEvent<T>(this, valueImplement, whereImplement);
        // запишем в DataProperty
        for(DataProperty dataProperty : getChangeProps()) {
            if(Settings.get().isCheckUniqueEvent() && dataProperty.event!=null)
                throw new RuntimeException(ServerResourceBundle.getString("logics.property.already.has.event", dataProperty));
            dataProperty.event = event;
        }
    }

    public void setNotNull(ImMap<T, DataObject> values, ExecutionEnvironment env, boolean notNull, boolean check) throws SQLException, SQLHandledException {
        if(!check || (read(env.getSession().sql, values, env.getModifier(), env.getQueryEnv())!=null) != notNull) {
            ActionPropertyMapImplement<?, T> action = getSetNotNullAction(notNull);
            if(action!=null)
                action.execute(new ExecutionContext<T>(values , null, null, env, null, null));
        }
    }
    public void setNotNull(ImRevMap<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull) throws SQLException, SQLHandledException {
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

    protected boolean assertPropClasses(CalcType calcType, PropertyChanges changes, WhereBuilder changedWhere) {
        return calcType.isExpr() || (changes.isEmpty() && changedWhere==null);
    }

    public CalcPropertyMapImplement<T, T> getImplement() {
        return new CalcPropertyMapImplement<T, T>(this, getIdentityInterfaces());
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<T, V> getImplement(ImOrderSet<V> list) {
        return new CalcPropertyMapImplement<T, V>(this, getMapInterfaces(list));
    }

    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        ImMap<T, ValueClass> interfaceClasses = getInterfaceClasses(ClassType.FULL); // так как в определении propertyDraw также используется FULL, а не ASSERTFULL
        if(interfaceClasses.size() < interfaces.size()) // не все классы есть
            return null;

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

    public static <T extends PropertyInterface, X> ImMap<T, ValueClass> or(ImSet<T> interfaces, ImList<CalcPropertyInterfaceImplement<T>> operands, ImList<ValueClass> operandClasses, PrevClasses prevSameClasses) {
        ImMap<T, ValueClass> result = MapFact.EMPTY();
        for(int i=0,size=operands.size();i<size;i++)
            result = or(interfaces, result, operands.get(i).mapInterfaceCommonClasses(operandClasses.get(i), prevSameClasses));
        return result;
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
    public ImMap<T, ValueClass> getInterfaceCommonClasses(ValueClass commonValue, PrevClasses prevSameClasses) { // эвристично определяет классы, для входных значений
        return getClassValueWhere(ClassType.ASIS, prevSameClasses).getCommonParent(interfaces);
    }

    // костыль для email
    public static <I extends PropertyInterface> ValueClass[] getCommonClasses(ImList<I> mapInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> props) {
        ValueClass[] result = new ValueClass[mapInterfaces.size()];
        for(PropertyInterfaceImplement<I> prop : props) {
            ImMap<I, ValueClass> propClasses;
            if(prop instanceof CalcPropertyMapImplement)
                propClasses = ((CalcPropertyMapImplement<?, I>) prop).mapInterfaceClasses(ClassType.ASSERTFULL);
            else
                propClasses = MapFact.EMPTY();

            for(int i=0;i<result.length;i++)
                result[i] = or(result[i], propClasses.get(mapInterfaces.get(i)));
        }
        return result;
    }

    public ImSet<CalcProperty> getSetUsedChanges(PropertyChanges propChanges) {
        return getUsedChanges(propChanges.getStruct());
    }

    public PropertyChanges getUsedChanges(PropertyChanges propChanges) {
        return propChanges.filter(getSetUsedChanges(propChanges));
    }

    @PackComplex
    @Message("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(calcType, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            QueryBuilder<T, String> fullQuery = new QueryBuilder<T, String>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.addProperty("value", newExpr);
            
            Expr dbExpr = getExpr(fullQuery.getMapExprs());
            Where fullWhere = newExpr.getWhere().or(dbExpr.getWhere());
            if(!DBManager.PROPERTY_REUPDATE && isStored())
                fullWhere = fullWhere.and(newExpr.compare(dbExpr, Compare.EQUALS).not());            

            fullQuery.addProperty("changed", query.getExpr("changed").and(fullWhere));
            return fullQuery.getQuery();
        }

        QueryBuilder<T, String> query = new QueryBuilder<T,String>(getMapKeys().removeRev(interfaceValues.keys()));
        ImMap<T, Expr> allKeys = query.getMapExprs().addExcl(interfaceValues);
        WhereBuilder queryWheres = queryType.needChange() ? new WhereBuilder():null;
        query.addProperty("value", aspectGetExpr(allKeys, calcType, propChanges, queryWheres));
        if(queryType.needChange())
            query.addProperty("changed", ValueExpr.get(queryWheres.toWhere()));
        return query.getQuery();
    }

    public Expr getQueryExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWheres) {

        MExclMap<T, Expr> mInterfaceValues = MapFact.mExclMap(joinImplement.size()); MExclMap<T, Expr> mInterfaceExprs = MapFact.mExclMap(joinImplement.size());
        for(int i=0,size=joinImplement.size();i<size;i++) {
            Expr expr = joinImplement.getValue(i);
            if(expr.isValue()) {
                if(expr.isNull()) // пока есть глюк с isFull
                    return Expr.NULL;
                mInterfaceValues.exclAdd(joinImplement.getKey(i), expr);
            } else
                mInterfaceExprs.exclAdd(joinImplement.getKey(i), expr);
        }

        IQuery<T, String> query = getQuery(calcType, propChanges, changedWheres!=null?PropertyQueryType.CHANGED:PropertyQueryType.NOCHANGE, mInterfaceValues.immutable());

        Join<String> queryJoin = query.join(mInterfaceExprs.immutable());
        if(changedWheres!=null)
            changedWheres.add(queryJoin.getExpr("changed").getWhere());
        return queryJoin.getExpr("value");
    }

    @Message("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, PropertyChanges.EMPTY);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier) throws SQLException, SQLHandledException {
        return getExpr(joinImplement, modifier, null);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier, WhereBuilder changedWhere) throws SQLException, SQLHandledException {
        return getExpr(joinImplement, modifier.getPropertyChanges(), changedWhere);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return getExpr(joinImplement, propChanges, null);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getExpr(joinImplement, CalcType.EXPR, propChanges, changedWhere);
    }

    // в будущем propClasses можно заменить на PropertyTables propTables
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isNotNull() && (Settings.get().isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, calcType, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    public void prereadCaches() {
        getRecDepends();
        getClassWhere(ClassType.ASIS);
        getClassWhere(ClassType.FULL);
        if(isNotNull())
            getQuery(CalcType.EXPR, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, MapFact.<T, Expr>EMPTY()).pack();
    }

    public CalcPropertyMapImplement<?, T> getClassProperty() {
        return IsClassProperty.getMapProperty(getInterfaceClasses(ClassType.ASSERTFULL));
    }

    public boolean isFull(ImCol<T> checkInterfaces) {
        return getClassValueWhere(ClassType.ASIS).isFull(checkInterfaces);
    }

    public boolean supportsDrillDown() {
        return false;
    }

    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return null;
    }

    private boolean calculateIsFull() {
        return isFull(interfaces);
    }
    private Boolean isFull;
    @ManualLazy
    public boolean isFull() { // обозначает что можно вывести классы всех параметров, используется в частности для материализации (stored, hints) чтобы знать типы колонок, или даже в subQuery (для статистики)
        if(isFull==null)
            isFull = calculateIsFull();
        return isFull;
    }
    
    public boolean isNotNull() { // обозначает что при null одном из параметров - null значение
        if(isFull())
            return true;
        
        if(this instanceof AggregateProperty)
            return ((AggregateProperty)this).checkNotNull();
        return false;
    }
    
    public boolean isDrillFull() {
        return isFull();
    }

    public boolean isEmpty() {
        return getClassValueWhere(ClassType.ASIS).isFalse();
    }

    @IdentityLazy
    public boolean allowHintIncrement() {
        assert isFull();

        if(!isEmpty())
            for(ValueClass usedClass : getInterfaceClasses(ClassType.ASSERTFULL).values().toSet().merge(getValueClass()))
                if(usedClass instanceof OrderClass)
                    return false;

        return true;
    }
    
    @IdentityLazy
    public long getComplexity() {
        try {
            return getExpr(getMapKeys(), Property.defaultModifier).getComplexity(false);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Message("logics.recalculating.data.classes")
    public void recalculateClasses(SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {
        assert isStored();
        
        ImRevMap<KeyField, KeyExpr> mapKeys = mapTable.table.getMapKeys();
        Where where = DataSession.getIncorrectWhere(this, baseClass, mapTable.mapKeys.join(mapKeys));
        Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(mapKeys, Expr.NULL, field, where);
        sql.updateRecords(new ModifyQuery(mapTable.table, query, OperationOwner.unknown, TableOwner.global));
    }
}
