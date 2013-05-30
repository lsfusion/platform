package platform.server.session;

import platform.base.*;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;
import platform.interop.Compare;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.Settings;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.context.ThreadLocalContext;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.data.type.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.navigator.ComputerController;
import platform.server.form.navigator.IsServerRestartingController;
import platform.server.form.navigator.UserController;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.SessionEnvEvent;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.filterKeys;

public class DataSession extends ExecutionEnvironment implements SessionChanges {

    private Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data = MapFact.mAddRemoveMap();
    private SingleKeyPropertyUsage news = null;

    // оптимизационные вещи
    private Set<CustomClass> add = SetFact.mAddRemoveSet();
    private Set<CustomClass> remove = SetFact.mAddRemoveSet();
    private Set<ConcreteObjectClass> usedOldClasses = SetFact.mAddRemoveSet();
    private Set<ConcreteObjectClass> usedNewClasses = SetFact.mAddRemoveSet();
    private Map<CustomClass, DataObject> singleAdd = MapFact.mAddRemoveMap();
    private Map<CustomClass, DataObject> singleRemove = MapFact.mAddRemoveMap();
    private Map<DataObject, ConcreteObjectClass> newClasses = MapFact.mAddRemoveMap(); // просто lazy кэш для getCurrentClass

    public static Where isValueClass(Expr expr, CustomClass customClass, Set<ConcreteObjectClass> usedNewClasses) {
        return isValueClass(expr, customClass.getUpSet(), usedNewClasses);
    }

    public static Where isValueClass(Expr expr, ObjectValueClassSet classSet, Set<ConcreteObjectClass> usedNewClasses) {
        Where result = Where.FALSE;
        for(ConcreteObjectClass usedClass : usedNewClasses)
            if(usedClass instanceof ConcreteCustomClass) {
                ConcreteCustomClass customUsedClass = (ConcreteCustomClass) usedClass;
                if(classSet.containsAll(customUsedClass)) // если изменяется на класс, у которого
                    result = result.or(expr.compare(customUsedClass.getClassObject(), Compare.EQUALS));
            }
        return result;
    }

    public ImSet<CalcProperty> getChangedProps(ImSet<CustomClass> add, ImSet<CustomClass> remove, ImSet<ConcreteObjectClass> old, ImSet<ConcreteObjectClass> newc, ImSet<DataProperty> data) {
        return SetFact.<CalcProperty>addExcl(getClassChanges(add, remove, old, newc), data);
    }
    public ImSet<CalcProperty> getChangedProps() {
        return getChangedProps(SetFact.fromJavaSet(add), SetFact.fromJavaSet(remove), SetFact.fromJavaSet(usedOldClasses), SetFact.fromJavaSet(usedNewClasses), SetFact.fromJavaSet(data.keySet()));
    }

    private class DataModifier extends SessionModifier {

        public SQLSession getSQL() {
            return sql;
        }

        public BaseClass getBaseClass() {
            return baseClass;
        }

        public QueryEnvironment getQueryEnv() {
            return env;
        }

        protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> overrided) {
            PropertyChange<P> propertyChange = getPropertyChange(property);
            if(propertyChange!=null)
                return new ModifyChange<P>(propertyChange, false);
            if(!preread.isEmpty())
                return new ModifyChange<P>(property.getNoChange(), preread, false);
            return null;
        }

        public ImSet<CalcProperty> calculateProperties() {
            return getChangedProps();
        }
    }
    private final DataModifier dataModifier = new DataModifier();

    protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        if(property instanceof ObjectClassProperty)
            return (PropertyChange<P>) getObjectClassChange((ObjectClassProperty) property);

        if(property instanceof ClassDataProperty)
            return (PropertyChange<P>) getClassDataChange((ClassDataProperty) property);

        if(property instanceof IsClassProperty)
            return (PropertyChange<P>) getClassChange((IsClassProperty) property);

        if(property instanceof DataProperty)
            return (PropertyChange<P>) getDataChange((DataProperty) property);
        return null;
    }

    private class Transaction {
        private final Set<CustomClass> add;
        private final Set<CustomClass> remove;
        private final Set<ConcreteObjectClass> usedOldClases;
        private final Set<ConcreteObjectClass> usedNewClases;
        private final Map<CustomClass, DataObject> singleAdd;
        private final Map<CustomClass, DataObject> singleRemove;
        private final Map<DataObject, ConcreteObjectClass> newClasses;

        private final SessionData news;
        private final ImMap<DataProperty, SessionData> data;

        private Transaction() {
            assert sessionEventChangedOld.isEmpty(); // в транзакции никаких сессионных event'ов быть не может
//            assert applyModifier.getHintProps().isEmpty(); // равно как и хинт'ов, не факт, потому как транзакция не сразу создается

            add = new HashSet<CustomClass>(DataSession.this.add);
            remove = new HashSet<CustomClass>(DataSession.this.remove);
            usedOldClases = new HashSet<ConcreteObjectClass>(DataSession.this.usedOldClasses);
            usedNewClases = new HashSet<ConcreteObjectClass>(DataSession.this.usedNewClasses);
            singleAdd = new HashMap<CustomClass, DataObject>(DataSession.this.singleAdd);
            singleRemove = new HashMap<CustomClass, DataObject>(DataSession.this.singleRemove);
            newClasses = new HashMap<DataObject, ConcreteObjectClass>(DataSession.this.newClasses);

            data = SessionTableUsage.saveData(DataSession.this.data);
            if(DataSession.this.news!=null)
                news = DataSession.this.news.saveData();
            else
                news = null;
        }
        
        private void rollData() throws SQLException {
            Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> rollData = MapFact.mAddRemoveMap();
            for(int i=0,size=data.size();i<size;i++) {
                DataProperty prop = data.getKey(i);

                SinglePropertyTableUsage<ClassPropertyInterface> table = DataSession.this.data.get(prop);
                if(table==null) {
                    table = prop.createChangeTable();
                    table.drop(sql);
                }

                table.rollData(sql, data.getValue(i));
                rollData.put(prop, table);
            }
            DataSession.this.data = rollData;
        }

        private void rollNews() throws SQLException {
            if(news!=null) {
                if(DataSession.this.news==null) {
                    DataSession.this.news = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
                    DataSession.this.news.drop(sql);
                }
                DataSession.this.news.rollData(sql, news);
            } else
                DataSession.this.news = null;
        }

        private void rollback() throws SQLException {
            assert sessionEventChangedOld.isEmpty(); // в транзакции никаких сессионных event'ов быть не может
            assert applyModifier.getHintProps().isEmpty(); // равно как и хинт'ов

            dropTables(SetFact.<SessionDataProperty>EMPTY()); // старые вернем, таблицу удалятся (но если нужны будут, rollback откатит эти изменения)

            // assert что новые включают старые
            DataSession.this.add = add;
            DataSession.this.remove = remove;
            DataSession.this.usedOldClasses = usedOldClases;
            DataSession.this.usedNewClasses = usedNewClases;
            DataSession.this.singleAdd = singleAdd;
            DataSession.this.singleRemove = singleRemove;
            DataSession.this.newClasses = newClasses;

            rollData();
            rollNews();
            
            dataModifier.eventDataChanges(getChangedProps(SetFact.fromJavaSet(add), SetFact.fromJavaSet(remove), SetFact.fromJavaSet(usedOldClasses), SetFact.fromJavaSet(usedNewClasses), data.keys()));
        }
    }
    private Transaction applyTransaction; // restore point

    private void startTransaction() throws SQLException {
        sql.startTransaction();
    }
    private void checkTransaction() {
        if(sql.isInTransaction() && applyTransaction==null)
            applyTransaction = new Transaction();
    }
    public void rollbackTransaction() throws SQLException {
        if(applyTransaction!=null) {
            applyTransaction.rollback();
            applyTransaction = null;
        }
        sql.rollbackTransaction();
//        checkSessionTableMap();
    }
/*    private void checkSessionTableMap() {
        checkSessionTableMap(add);
        checkSessionTableMap(remove);
        checkSessionTableMap(data);
        checkSessionTableMap(news);
    }
    private void checkSessionTableMap(Map<?, ? extends SessionTableUsage> usages) {
        for(SessionTableUsage usage : usages.values())
            checkSessionTableMap(usage);
    }
    private void checkSessionTableMap(SessionTableUsage usage) {
        if(usage!=null && usage.table instanceof SessionDataTable)
            sql.checkSessionTableMap(((SessionDataTable)usage.table).getTable(), usage);
    }*/
    

    private void commitTransaction() throws SQLException {
        applyTransaction = null;
        sql.commitTransaction();
    }

    private ImSet<CalcProperty<ClassPropertyInterface>> getClassChanges(ImSet<CustomClass> addClasses, ImSet<CustomClass> removeClasses, ImSet<ConcreteObjectClass> oldClasses, ImSet<ConcreteObjectClass> newClasses) {
        return SetFact.addExcl(CustomClass.getProperties(addClasses, removeClasses, oldClasses, newClasses), baseClass.getObjectClassProperty());
    }

    public boolean hasChanges() {
        return !data.isEmpty() || news!=null;
    }

    public boolean hasStoredChanges() {
        if (news != null)
            return true;

        for (DataProperty property : data.keySet())
            if (property.isStored())
                return true;

        return false;
    }

    private PropertyChange<ClassPropertyInterface> getObjectClassChange(ObjectClassProperty property) {
        if(news!=null)
            return SingleKeyPropertyUsage.getChange(news, property.interfaces.single());
        return null;
    }

    private boolean containsClassDataChange(ClassDataProperty property) {
        for(ConcreteCustomClass child : property.set.getSetConcreteChildren())
            if(usedOldClasses.contains(child) || usedNewClasses.contains(child))
                return true;
        return false;
    }
    private PropertyChange<ClassPropertyInterface> getClassDataChange(ClassDataProperty property) {
        if(news!=null && containsClassDataChange(property)) {
            ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
            KeyExpr keyExpr = mapKeys.singleValue();

            Join<String> join = news.join(keyExpr);
            Expr newClassExpr = join.getExpr("value");
            Where where = join.getWhere();

            Where newClass = isValueClass(newClassExpr, property.set, usedNewClasses);
/*            Where oldClass = keyExpr.isClass(property.set); // в общем то оптимизация (чтобы например не делать usedOldClasses)
            where = where.and(oldClass.or(newClass));*/

            return new PropertyChange<ClassPropertyInterface>(mapKeys, // на не null меняем только тех кто подходит по классу
                    newClassExpr.and(newClass), where);
        }
        return null;
    }

    private PropertyChange<ClassPropertyInterface> getClassChange(IsClassProperty property) {
        ValueClass isClass = property.getInterfaceClass();
        if(isClass instanceof CustomClass) {
            CustomClass customClass = (CustomClass) isClass;
            boolean added = add.contains(customClass);
            boolean removed = remove.contains(customClass);
            if(added || removed) { // оптимизация в том числе
                ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
                KeyExpr key = mapKeys.singleValue();

                Join<String> join = news.join(key);
                Expr newClassExpr = join.getExpr("value");

                Where hasClass = isValueClass(newClassExpr, customClass, usedNewClasses);
                Where hadClass = key.isUpClass(isClass);

                Where changedWhere = Where.FALSE;
                Expr changeExpr;
                if(added) {
                    Where addWhere;
                    DataObject dataObject = singleAdd.get(customClass);
                    if(dataObject!=null) // оптимизация
                        addWhere = key.compare(dataObject, Compare.EQUALS);
                    else
                        addWhere = hasClass.and(hadClass.not());
                    changeExpr = ValueExpr.get(addWhere);
                    changedWhere = changedWhere.or(addWhere);
                } else
                    changeExpr = Expr.NULL;

                if(removed) {
                    Where removeWhere;
                    DataObject dataObject = singleRemove.get(customClass);
                    if(dataObject!=null)
                        removeWhere = key.compare(dataObject, Compare.EQUALS);
                    else
                        removeWhere = hadClass.and(join.getWhere().and(hasClass.not())); 
                    changedWhere = changedWhere.or(removeWhere); // был класс и изменился, но не на новый
                }
                return new PropertyChange<ClassPropertyInterface>(mapKeys, changeExpr, changedWhere);
            }
        }
        return null;
    }

    public PropertyChange<ClassPropertyInterface> getDataChange(DataProperty property) {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange!=null)
            return SinglePropertyTableUsage.getChange(dataChange);
        return null;
    }

    public final SQLSession sql;
    public final SQLSession idSession;

    public void close() throws SQLException {
    }

    public static class UpdateChanges {

        public ImSet<CalcProperty> properties;

        public UpdateChanges() {
            properties = SetFact.EMPTY();
        }

        public UpdateChanges(DataSession session) {
            properties = session.getChangedProps();
        }

        public void add(ImSet<? extends CalcProperty> set) {
            properties = properties.merge(set);
        }
        public void add(UpdateChanges changes) {
            add(changes.properties);
        }
    }

    // формы, для которых с момента последнего update уже был restart, соотвественно в значениях - изменения от посл. update (prev) до посл. apply
    public IdentityHashMap<FormInstance, UpdateChanges> appliedChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    // формы для которых с момента последнего update не было restart, соответственно в значениях - изменения от посл. update (prev) до посл. изменения
    public IdentityHashMap<FormInstance, UpdateChanges> incrementChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    // assert что те же формы что и в increment, соответственно в значениях - изменения от посл. apply до посл. update (prev)
    public IdentityHashMap<FormInstance, UpdateChanges> updateChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    public final BaseClass baseClass;
    public final ConcreteCustomClass sessionClass;
    public final LCP<?> currentSession;

    // для отладки
    public static boolean reCalculateAggr = false;

    private final IsServerRestartingController isServerRestarting;
    public final UserController user;
    public final ComputerController computer;

    public DataObject applyObject = null;
    
    private final ImOrderMap<ActionProperty, SessionEnvEvent> sessionEvents;

    private ImOrderSet<ActionProperty> activeSessionEvents;
    @ManualLazy
    private ImOrderSet<ActionProperty> getActiveSessionEvents() {
        if(activeSessionEvents == null)
            activeSessionEvents = filterEnv(sessionEvents);
        return activeSessionEvents;
    }

    private ImSet<OldProperty> sessionEventOldDepends;
    @ManualLazy
    private ImSet<OldProperty> getSessionEventOldDepends() {
        if(sessionEventOldDepends==null) {
            MSet<OldProperty> mResult = SetFact.mSet();
            for(ActionProperty<?> action : getActiveSessionEvents())
                mResult.addAll(action.getSessionEventOldDepends());
            sessionEventOldDepends = mResult.immutable();
        }
        return sessionEventOldDepends;
    }

    public DataSession(SQLSession sql, final UserController user, final ComputerController computer, IsServerRestartingController isServerRestarting, BaseClass baseClass, ConcreteCustomClass sessionClass, LCP currentSession, SQLSession idSession, ImOrderMap<ActionProperty, SessionEnvEvent> sessionEvents) throws SQLException {
        this.sql = sql;
        this.isServerRestarting = isServerRestarting;

        this.baseClass = baseClass;
        this.sessionClass = sessionClass;
        this.currentSession = currentSession;

        this.user = user;
        this.computer = computer;

        this.sessionEvents = sessionEvents;

        this.idSession = idSession;
    }

    public DataSession createSession() throws SQLException {
        return new DataSession(sql, user, computer, isServerRestarting, baseClass, sessionClass, currentSession, idSession, sessionEvents);
    }

    public void restart(boolean cancel, ImSet<SessionDataProperty> keep) throws SQLException {

        // apply
        //      по кому был restart : добавляем changes -> applied
        //      по кому не было restart : to -> applied (помечая что был restart)

        // cancel
        //    по кому не было restart :  from -> в applied (помечая что был restart)

        if(!cancel)
            for(Map.Entry<FormInstance,UpdateChanges> appliedChange : appliedChanges.entrySet())
                appliedChange.getValue().add(new UpdateChanges(this));

        assert Collections.disjoint(appliedChanges.keySet(),(cancel?updateChanges:incrementChanges).keySet());
        appliedChanges.putAll(cancel?updateChanges:incrementChanges);
        incrementChanges = new IdentityHashMap<FormInstance, UpdateChanges>();
        updateChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

        dropTables(keep);
        add.clear();
        remove.clear();
        usedOldClasses.clear();
        usedNewClasses.clear();
        singleAdd.clear();
        singleRemove.clear();
        newClasses.clear();

        BaseUtils.clearNotKeys(data, keep);
        news = null;

        assert dataModifier.getHintProps().isEmpty(); // hint'ы все должны также уйти

        if(cancel) {
            sessionEventChangedOld.clear(sql);
        } else
            assert sessionEventChangedOld.isEmpty();
        sessionEventNotChangedOld.clear();

        applyObject = null; // сбрасываем в том числе когда cancel потому как cancel drop'ает в том числе и добавление объекта
    }

    public DataObject addObject() throws SQLException {
        return new DataObject(IDTable.instance.generateID(idSession, IDTable.OBJECT),baseClass.unknown);
    }

    // с fill'ами addObject'ы
    public DataObject addObject(ConcreteCustomClass customClass, DataObject object) throws SQLException {
        if(object==null)
            object = addObject();

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass);

        return object;
    }

    private static Pair<Integer, Integer>[] toZeroBased(Pair<Integer, Integer>[] shifts) {
        Pair<Integer, Integer>[] result = new Pair[shifts.length];
        for(int i=0;i<shifts.length;i++)
            result[i] = new Pair<Integer, Integer>(shifts[i].first - 1, shifts[i].second);
        return result;
    }

    public <T extends PropertyInterface> SinglePropertyTableUsage<T> addObjects(ConcreteCustomClass cls, PropertySet<T> set) throws SQLException {
        final Query<T, String> query = set.getAddQuery(baseClass); // query, который генерит номера записей (one-based)

        // сначала закидываем в таблицу set с номерами рядов (!!! нужно гарантировать однозначность)
        SinglePropertyTableUsage<T> table = new SinglePropertyTableUsage<T>(query.getMapKeys().keys().toOrderSet(), new Type.Getter<T>() {
            public Type getType(T key) {
                return query.getKeyType(key);
            }
        }, ObjectType.instance);
        table.modifyRows(sql, query, baseClass, Modify.ADD, env);

        if(table.isEmpty()) // оптимизация, не зачем генерить id и все такое
            return table;

        // берем количество рядов - резервируем ID'ки
        Pair<Integer, Integer>[] startFrom = IDTable.instance.generateIDs(table.getCount(), idSession, IDTable.OBJECT);

        // update'им на эту разницу ключи, чтобы сгенерить объекты
        table.updateAdded(sql, baseClass, toZeroBased(startFrom)); // так как не zero-based отнимаем 1

        // вообще избыточно, если compile'ить отдельно в for() + changeClass, который сам сгруппирует, но тогда currentClass будет unknown в свойстве что вообщем то не возможно
        KeyExpr keyExpr = new KeyExpr("keyExpr");
        changeClass(new ClassChange(keyExpr, GroupExpr.create(MapFact.singleton("key", table.join(query.mapKeys).getExpr("value")),
                Where.TRUE, MapFact.singleton("key", keyExpr)).getWhere(), cls));
        
        // возвращаем таблицу
        return table;
    }

    public DataObject addObject(ConcreteCustomClass customClass) throws SQLException {
        return addObject(customClass, null);
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException {
        changeClass(dataObject, cls);
    }

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        changeClass(new ClassChange(change, toClass));
    }

    public <K> void updateCurrentClasses(Collection<SinglePropertyTableUsage<K>> tables) throws SQLException {
        for(SinglePropertyTableUsage<K> table : tables)
            table.updateCurrentClasses(this);
    }

    public <K, T extends ObjectValue> ImMap<K, T> updateCurrentClasses(ImMap<K, T> objectValues) throws SQLException {
        ImValueMap<K, T> mvResult = objectValues.mapItValues(); // exception кидает
        for(int i=0,size=objectValues.size();i<size;i++)
            mvResult.mapValue(i, (T) updateCurrentClass(objectValues.getValue(i)));
        return mvResult.immutableValue();
    }

    public ObjectValue updateCurrentClass(ObjectValue value) throws SQLException {
        if(value instanceof NullValue)
            return value;
        else {
            DataObject dataObject = (DataObject)value;
            return new DataObject(dataObject.object, getCurrentClass(dataObject));
        }
    }

    public <K> ImOrderSet<ImMap<K, ConcreteObjectClass>> readDiffClasses(Where where, ImMap<K, ? extends Expr> classExprs, ImMap<K, ? extends Expr> objectExprs) throws SQLException {

        final ValueExpr unknownExpr = new ValueExpr(-1, baseClass.unknown);

        ImRevMap<K,KeyExpr> keys = KeyExpr.getMapKeys(classExprs.keys().addExcl(objectExprs.keys()));
        ImMap<K, Expr> group = ((ImMap<K, Expr>)classExprs).mapValues(new GetValue<Expr, Expr>() {
                    public Expr getMapValue(Expr value) {
                        return value.nvl(unknownExpr);
            }}).addExcl(((ImMap<K, Expr>)objectExprs).mapValues(new GetValue<Expr, Expr>() {
                public Expr getMapValue(Expr value) {
                    return baseClass.getObjectClassProperty().getExpr(value, getModifier()).nvl(unknownExpr);
                }
            }));

        return new Query<K, String>(keys, GroupExpr.create(group, where, keys).getWhere()).execute(this).keyOrderSet().mapMergeOrderSetValues(new GetValue<ImMap<K, ConcreteObjectClass>, ImMap<K, Object>>() {
            public ImMap<K, ConcreteObjectClass> getMapValue(ImMap<K, Object> readClasses) {
                return readClasses.mapValues(new GetValue<ConcreteObjectClass, Object>() {
                    public ConcreteObjectClass getMapValue(Object id) {
                        return baseClass.findConcreteClassID(((Integer) id) != -1 ? (Integer) id : null);
                    }
                });
            }
        });
    }

    public void changeClass(ClassChange change) throws SQLException {
        if(change.isEmpty()) // оптимизация, важна так как во многих event'ах может учавствовать
            return;
        
        boolean hadStoredChanges = hasStoredChanges();

        SingleKeyPropertyUsage changeTable = null;
        MSet<CustomClass> mAddClasses = SetFact.mSet(); MSet<CustomClass> mRemoveClasses = SetFact.mSet();
        MSet<ConcreteObjectClass> mChangeOldClasses = SetFact.mSet(); MSet<ConcreteObjectClass> mChangeNewClasses = SetFact.mSet();
        if(change.keyValue !=null) { // оптимизация
            ConcreteObjectClass prevcl = (ConcreteObjectClass) getCurrentClass(change.keyValue);
            ConcreteObjectClass newcl = baseClass.findConcreteClassID((Integer) change.propValue.getValue());
            newcl.getDiffSet(prevcl, mAddClasses, mRemoveClasses);
            mChangeOldClasses.add(prevcl);
            mChangeNewClasses.add(newcl);
        } else {
            if(change.needMaterialize()) {
                changeTable = change.materialize(sql, baseClass, env); // materialize'им изменение
                change = changeTable.getChange();
            }

            if(change.isEmpty()) // оптимизация, важна так как во многих event'ах может учавствовать
                return;

            // читаем варианты изменения классов
            for(ImMap<String, ConcreteObjectClass> diffClasses : readDiffClasses(change.where, MapFact.singleton("newcl", change.expr), MapFact.singleton("prevcl", change.key))) {
                ConcreteObjectClass newcl = diffClasses.get("newcl");
                ConcreteObjectClass prevcl = diffClasses.get("prevcl");
                newcl.getDiffSet(prevcl, mAddClasses, mRemoveClasses);
                mChangeOldClasses.add(prevcl);
                mChangeNewClasses.add(newcl);
            }
        }
        ImSet<CustomClass> addClasses = mAddClasses.immutable(); ImSet<CustomClass> removeClasses = mRemoveClasses.immutable();
        ImSet<ConcreteObjectClass> changedOldClasses = mChangeOldClasses.immutable(); ImSet<ConcreteObjectClass> changedNewClasses = mChangeNewClasses.immutable();

        ImSet<CalcProperty<ClassPropertyInterface>> updateChanges = getClassChanges(addClasses, removeClasses, changedOldClasses, changedNewClasses);

        updateSessionEvents(updateChanges);

        aspectChangeClass(addClasses, removeClasses, changedOldClasses, changedNewClasses, change);

        if(changeTable!=null)
            changeTable.drop(sql);

        // так как таблица news используется при определении изменений всех классов, то нужно обновить и их "источники"
        dataModifier.eventSourceChanges(CustomClass.getProperties(
                                SetFact.fromJavaSet(add).remove(addClasses), SetFact.fromJavaSet(remove).remove(removeClasses),
                                SetFact.fromJavaSet(usedOldClasses).remove(changedOldClasses), SetFact.fromJavaSet(usedNewClasses).remove(changedNewClasses)));
        updateProperties(updateChanges);

        aspectAfterChange(hadStoredChanges);
    }
    
    public void dropChanges(DataProperty property) throws SQLException {
        if(!data.containsKey(property)) // оптимизация, см. использование
            return;

        aspectDropChanges(property);

        updateProperties(SetFact.singleton(property));
    }

    public void changeProperty(DataProperty property, PropertyChange<ClassPropertyInterface> change) throws SQLException {
        boolean hadStoredChanges = hasStoredChanges();

        SinglePropertyTableUsage<ClassPropertyInterface> changeTable = null;
        if(neededProps!=null && property.isStored() && property.event==null) { // если транзакция, нет change event'а, singleApply'им
            assert isInTransaction();

            change = SinglePropertyTableUsage.getChange(splitApplySingleStored(property,
                    property.readFixChangeTable(sql, change, baseClass, getQueryEnv()), ThreadLocalContext.getBusinessLogics()));
        } else {
            if(change.needMaterialize()) {
                changeTable = change.materialize(property, sql, baseClass, getQueryEnv());
                change = SinglePropertyTableUsage.getChange(changeTable);
            }

            if(change.isEmpty()) // оптимизация по аналогии с changeClass
                return;
        }

        ImSet<DataProperty> updateChanges = SetFact.singleton(property);

        updateSessionEvents(updateChanges);

        aspectChangeProperty(property, change);

        if(changeTable!=null)
            changeTable.drop(sql);

        updateProperties(updateChanges);

        aspectAfterChange(hadStoredChanges);
    }

    public static final SessionDataProperty isDataChanged = new SessionDataProperty("isDataChanged", "Is data changed", LogicalClass.instance);
    private void aspectAfterChange(boolean hadStoredChanges) throws SQLException {
        if(!hadStoredChanges && hasStoredChanges()) {
            ImSet<SessionDataProperty> updateChanges = SetFact.singleton(isDataChanged);
            updateSessionEvents(updateChanges);

            aspectChangeProperty(isDataChanged, new PropertyChange<ClassPropertyInterface>(new DataObject(true, LogicalClass.instance)));

            updateProperties(updateChanges);
        }
    }

    public void updateProperties(ImSet<? extends CalcProperty> changes) throws SQLException {
        dataModifier.eventDataChanges(changes);

        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet()) {
            incrementChange.getValue().add(changes);
        }

        for (FormInstance form : activeForms.keySet()) {
            form.dataChanged = true;
        }
    }

    // для OldProperty хранит изменения с предыдущего execute'а
    private IncrementTableProps sessionEventChangedOld = new IncrementTableProps();
    private IncrementChangeProps sessionEventNotChangedOld = new IncrementChangeProps();
    private OverrideSessionModifier sessionEventModifier;

    // потом можно было бы оптимизировать создание OverrideSessionModifier'а (в рамках getPropertyChanges) и тогда можно создавать modifier'ы непосредственно при запуске
    private OverrideSessionModifier commonSessionEventModifier = new OverrideSessionModifier(new OverrideIncrementProps(sessionEventChangedOld, sessionEventNotChangedOld), false, dataModifier);
    private Set<OverrideSessionModifier> prevStartSessionEventModifiers = new HashSet<OverrideSessionModifier>();
    private Map<ActionProperty, OverrideSessionModifier> mapPrevStartSessionEventModifiers = new HashMap<ActionProperty, OverrideSessionModifier>();

    public <P extends PropertyInterface> void updateSessionEvents(ImSet<? extends CalcProperty> changes) throws SQLException {
        if(!isInTransaction())
            for(OldProperty<PropertyInterface> old : getSessionEventOldDepends())
                if(!sessionEventChangedOld.contains(old) && CalcProperty.depends(old.property, changes)) // если влияет на old из сессионного event'а и еще не читалось
                    sessionEventChangedOld.add(old, old.property.readChangeTable(sql, getModifier(), baseClass, getQueryEnv()));
    }

    private boolean isInSessionEvent() {
        return sessionEventModifier!=null;
    }

    public <T extends PropertyInterface> void executeSessionEvents(FormInstance form) throws SQLException {

        if(sessionEventChangedOld.getProperties().size() > 0) { // оптимизационная проверка

            ExecutionEnvironment env = (form != null ? form : this);

            for(ActionProperty<?> action : getActiveSessionEvents()) {
                if(sessionEventChangedOld.getProperties().intersect(action.getSessionEventOldDepends())) { // оптимизация аналогичная верхней
                    executeSessionEvent(env, action);
                    if(!isInSessionEvent())
                        return;
                }
            }
            sessionEventModifier = null;

            // закидываем старые изменения
            for(CalcProperty changedOld : sessionEventChangedOld.getProperties()) // assert что только old'ы
                sessionEventNotChangedOld.add(changedOld, ((OldProperty<PropertyInterface>)changedOld).property.getIncrementChange(env.getModifier()));
            sessionEventChangedOld.clear(sql);
        }
    }

    @LogTime
    private void executeSessionEvent(ExecutionEnvironment env, ActionProperty<?> action) throws SQLException {
        sessionEventModifier = mapPrevStartSessionEventModifiers.get(action); // перегружаем modifier своим если он есть
        if(sessionEventModifier==null) sessionEventModifier = commonSessionEventModifier;

        action.execute(env);
    }

    private OverrideSessionModifier resolveModifier = null;

    public <T extends PropertyInterface> void resolve(ActionProperty<?> action) throws SQLException {
        IncrementChangeProps changes = new IncrementChangeProps();
        for(SessionCalcProperty sessionCalcProperty : action.getSessionCalcDepends(false))
            if(sessionCalcProperty instanceof ChangedProperty) // именно так, OldProperty нельзя подменять, так как предполагается что SET и DROPPED требуют разные значения PREV
                changes.add(sessionCalcProperty, ((ChangedProperty)sessionCalcProperty).getFullChange(getModifier()));
        resolveModifier = new OverrideSessionModifier(changes, true, dataModifier);

        action.execute(this);

        resolveModifier.clean(sql);
        resolveModifier = null;
    }

    // для оптимизации
    public DataChanges getUserDataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) throws SQLException {
        Pair<ImMap<ClassPropertyInterface, DataObject>, ObjectValue> simple;
        if((simple = change.getSimple())!=null) {
            if(IsClassProperty.fitClasses(getCurrentClasses(simple.first), property.value,
                    simple.second instanceof DataObject ? getCurrentClass((DataObject) simple.second) : null))
                return new DataChanges(property, change);
            else
                return DataChanges.EMPTY;
        }
        return null;
    }

    public ConcreteClass getCurrentClass(DataObject value) throws SQLException {
        ConcreteObjectClass newClass = null;
        if(news!=null && value.objectClass instanceof ConcreteObjectClass) {
            if(newClasses.containsKey(value))
                newClass = newClasses.get(value);
            else {
                ImCol<ImMap<String, Object>> read = news.read(this, value);
                if(read.size()==0)
                    newClass = null;
                else
                    newClass = baseClass.findConcreteClassID((Integer) read.single().get("value"));
                newClasses.put(value, newClass);
            }
        }

        if(newClass==null)
            return value.objectClass;
        else
            return newClass;
    }

    public <K> ImMap<K, ConcreteClass> getCurrentClasses(ImMap<K, DataObject> map) throws SQLException {
        ImValueMap<K, ConcreteClass> mvResult = map.mapItValues(); // exception
        for(int i=0,size=map.size();i<size;i++)
            mvResult.mapValue(i, getCurrentClass(map.getValue(i)));
        return mvResult.immutableValue();
    }

    public ObjectValue getCurrentValue(ObjectValue value) throws SQLException {
        if(value instanceof NullValue)
            return value;
        else {
            DataObject dataObject = (DataObject)value;
            return new DataObject(dataObject.object, getCurrentClass(dataObject));
        }
    }

    public <K, V extends ObjectValue> ImMap<K, V> getCurrentObjects(ImMap<K, V> map) throws SQLException {
        ImValueMap<K, V> mvResult = map.mapItValues(); // exception
        for(int i=0,size=map.size();i<size;i++)
            mvResult.mapValue(i, (V) getCurrentValue(map.getValue(i)));
        return mvResult.immutableValue();
    }

    public DataObject getDataObject(ValueClass valueClass, Object value) throws SQLException {
        return baseClass.getDataObject(sql, value, valueClass.getUpSet());
    }

    public ObjectValue getObjectValue(ValueClass valueClass, Object value) throws SQLException {
        return baseClass.getObjectValue(sql, value, valueClass.getUpSet());
    }

    // узнает список изменений произошедших без него
    public ImSet<CalcProperty> update(FormInstance<?> form) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)
        assert activeForms.containsKey(form);

        UpdateChanges incrementChange = incrementChanges.get(form);
        if(incrementChange!=null) // если не было restart
            //    to -> from или from = changes, to = пустому
            updateChanges.get(form).add(incrementChange);
            //    возвращаем to
        else { // иначе
            incrementChange = appliedChanges.remove(form);
            if(incrementChange==null) // совсем не было
                incrementChange = new UpdateChanges();
            UpdateChanges formChanges = new UpdateChanges(this);
            // from = changes (сбрасываем пометку что не было restart'а)
            updateChanges.put(form, formChanges);
            // возвращаем applied + changes
            incrementChange.add(formChanges);
        }
        incrementChanges.put(form,new UpdateChanges());

        return incrementChange.properties;
    }

    public String applyMessage(BusinessLogics<?> BL) throws SQLException {
        if(apply(BL))
            return null;
        else
            return ThreadLocalContext.getLogMessage();
//            return ((LogMessageClientAction)BaseUtils.single(actions)).message;
    }

    public boolean apply(BusinessLogics BL) throws SQLException {
        return apply(BL, null);
    }

    public boolean apply(BusinessLogics BL, FormInstance form) throws SQLException {
        return apply(BL, false, form);
    }

    public boolean check(BusinessLogics BL, FormInstance form) throws SQLException {
        return apply(BL, true, form);
    }

    public static <T extends PropertyInterface> boolean fitKeyClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys).means(property.mapTable.table.getClasses());
    }

    public static <T extends PropertyInterface> boolean fitClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys, property.field).means(property.fieldClassWhere);
    }

    public static <T extends PropertyInterface> boolean notFitKeyClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys).and(property.mapTable.table.getClasses()).isFalse();
    }

    public static <T extends PropertyInterface> boolean notFitClasses(CalcProperty<T> property, SinglePropertyTableUsage<T> change) {
        return change.getClassWhere(property.mapTable.mapKeys, property.field).and(property.fieldClassWhere).isFalse();
    }

    // для Single Apply
    private class EmptyModifier extends SessionModifier {

        private EmptyModifier() {
        }

        @Override
        public void addHintIncrement(CalcProperty property) {
            throw new RuntimeException("should not be"); // так как нет изменений то и hint не может придти
        }

        public ImSet<CalcProperty> calculateProperties() {
            return SetFact.EMPTY();
        }

        protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> overrided) {
            if(!preread.isEmpty())
                return new ModifyChange<P>(property.getNoChange(), preread, false);
            return null;
        }

        public SQLSession getSQL() {
            return sql;
        }

        public BaseClass getBaseClass() {
            return baseClass;
        }

        public QueryEnvironment getQueryEnv() {
            return env;
        }
    }
    public final EmptyModifier emptyModifier = new EmptyModifier();

    private <T extends PropertyInterface, D extends PropertyInterface> SinglePropertyTableUsage<T> splitApplySingleStored(CalcProperty<T> property, SinglePropertyTableUsage<T> changeTable, BusinessLogics<?> BL) throws SQLException {
        if(property.isEnabledSingleApply()) {
            Pair<SinglePropertyTableUsage<T>, SinglePropertyTableUsage<T>> split = property.splitSingleApplyClasses(changeTable, sql, baseClass, env);
            applySingleStored(property, split.first, BL);
            return split.second;
        }
        return changeTable;
    }

    private <T extends PropertyInterface, D extends PropertyInterface> void applySingleStored(CalcProperty<T> property, SinglePropertyTableUsage<T> change, BusinessLogics<?> BL) throws SQLException {
        assert isInTransaction();

        // assert что у change классы совпадают с property
        assert property.isSingleApplyStored();
        assert fitClasses(property, change); // проверяет гипотезу
        assert fitKeyClasses(property, change); // дополнительная проверка, она должна обеспечиваться тем что в change не должно быть замен null на null

        if(change.isEmpty())
            return;

        // тут есть assert что в increment+noUpdate не будет noDB, то есть не пересекется с NoEventModifier, то есть можно в любом порядке increment'ить
        IncrementTableProps increment = new IncrementTableProps(property, change);
        IncrementChangeProps noUpdate = new IncrementChangeProps(BL.getDataChangeEvents());

        OverrideSessionModifier modifier = new OverrideSessionModifier(new OverrideIncrementProps(noUpdate, increment), emptyModifier);

        ImOrderSet<CalcProperty> dependProps = filterEnv(BL.getSingleApplyDependFrom(property)); // !!! важно в лексикографическом порядке должно быть

        if(neededProps!=null && !flush) { // придется отдельным прогоном чтобы правильную лексикографику сохранить
            for(CalcProperty<D> depend : dependProps)
                if(!neededProps.contains(depend)) {
                    updatePendingApplyStart(property, change);
                    break;
                }
        }

        for(CalcProperty<D> depend : dependProps) {
            assert depend.isSingleApplyStored() || depend instanceof OldProperty;

            if(neededProps!=null) { // управление pending'ом
                assert !flush || !pendingSingleTables.containsKey(depend); // assert что если flush то уже обработано (так как в обратном лексикографике идет)
                if(!neededProps.contains(depend)) { // если не нужная связь не обновляем
                    if(!flush)
                        continue;
                } else { // если нужная то уже обновили
                    if(flush) {
                        if(depend.isSingleApplyStored())
                            noUpdate.addNoChange(depend);
                        continue;
                    }
                }
            }

            if(depend.isSingleApplyStored()) { // читаем новое значение, запускаем рекурсию
                SinglePropertyTableUsage<D> dependChange = depend.readChangeTable(sql, modifier, baseClass, env);
                applySingleStored((CalcProperty)depend, (SinglePropertyTableUsage)dependChange, BL);
                noUpdate.addNoChange(depend); // докидываем noUpdate чтобы по нескольку раз одну ветку не отрабатывать
            } else {
                SinglePropertyTableUsage<D> dependChange = ((OldProperty<D>) depend).property.readChangeTable(sql, modifier, baseClass, env);
                updateApplyStart((OldProperty<D>) depend, dependChange);
            }
        }
        savePropertyChanges(property, change);
        
        modifier.clean(sql); // hint'ы и ссылки почистить
    }

    private OrderedMap<CalcProperty, SinglePropertyTableUsage> pendingSingleTables = new OrderedMap<CalcProperty, SinglePropertyTableUsage>();
    boolean flush = false;

    private FunctionSet<CalcProperty> neededProps = null;
    private void startPendingSingles(ActionProperty action) throws SQLException {
        assert isInTransaction();

        if(!action.singleApply)
            return;

        neededProps = action.usedProps;
    }

    private <P extends PropertyInterface> void updatePendingApplyStart(CalcProperty<P> property, SinglePropertyTableUsage<P> tableUsage) throws SQLException { // изврат конечно
        assert isInTransaction();

        SinglePropertyTableUsage<P> prevTable = pendingSingleTables.get(property);
        if(prevTable==null) {
            prevTable = property.createChangeTable();
            pendingSingleTables.put(property, prevTable);
        }
        ImRevMap<P, KeyExpr> mapKeys = property.getMapKeys();
        prevTable.modifyRows(sql, mapKeys, property.getExpr(mapKeys), tableUsage.join(mapKeys).getWhere(), baseClass, Modify.LEFT, env); // если он уже был в базе он не заместится
    }

    // assert что в pendingSingleTables в обратном лексикографике
    private <T extends PropertyInterface> void flushPendingSingles(BusinessLogics BL) throws SQLException {
        assert isInTransaction();

        if(neededProps==null)
            return;

        flush = true;

        // сначала "возвращаем" изменения в базе на предыдущее
        for(Map.Entry<CalcProperty, SinglePropertyTableUsage> pendingSingle : pendingSingleTables.entrySet()) {
            CalcProperty<T> property = pendingSingle.getKey();
            SinglePropertyTableUsage<T> prevTable = pendingSingle.getValue();

            ImRevMap<T, KeyExpr> mapKeys = property.getMapKeys();
            SinglePropertyTableUsage<T> newTable = property.readChangeTable(sql, new PropertyChange<T>(mapKeys, property.getExpr(mapKeys), prevTable.join(mapKeys).getWhere()), baseClass, env);
            savePropertyChanges(property, prevTable); // записываем старые изменения
            pendingSingle.setValue(newTable); // сохраняем новые изменения
        }

        for (Map.Entry<CalcProperty, SinglePropertyTableUsage> pendingSingle : pendingSingleTables.reverse().entrySet()) {
            applySingleStored(pendingSingle.getKey(), pendingSingle.getValue(), BL);
            pendingSingleTables.remove(pendingSingle.getKey());
        }

        neededProps = null;
        flush = false;
    }

    private void savePropertyChanges(Table implementTable, SessionTableUsage<KeyField, CalcProperty> changeTable) throws SQLException {
        savePropertyChanges(implementTable, changeTable.getValues().toMap(), changeTable.getKeys().toRevMap(), changeTable, true);
    }

    private <T extends PropertyInterface> void savePropertyChanges(CalcProperty<T> property, SinglePropertyTableUsage<T> change) throws SQLException {
        savePropertyChanges(property.mapTable.table, MapFact.singleton("value", (CalcProperty) property), property.mapTable.mapKeys, change, false);
    }

    private <K,V> void savePropertyChanges(Table implementTable, ImMap<V, CalcProperty> props, ImRevMap<K, KeyField> mapKeys, SessionTableUsage<K, V> changeTable, boolean onlyNotNull) throws SQLException {
        QueryBuilder<KeyField, PropertyField> modifyQuery = new QueryBuilder<KeyField, PropertyField>(implementTable);
        Join<V> join = changeTable.join(mapKeys.join(modifyQuery.getMapExprs()));
        for (int i=0,size=props.size();i<size;i++)
            modifyQuery.addProperty(props.getValue(i).field, join.getExpr(props.getKey(i)));
        modifyQuery.and(join.getWhere());
        sql.modifyRecords(new ModifyQuery(implementTable, modifyQuery.getQuery(), env));
        changeTable.drop(sql);
    }

    // хранит агрегированные изменения для уменьшения сложности (в транзакции очищает ветки от single applied)
    private IncrementTableProps apply = new IncrementTableProps();
    private OverrideSessionModifier applyModifier = new OverrideSessionModifier(apply, dataModifier);

    @Override
    public SessionModifier getModifier() {
        if(resolveModifier != null)
            return resolveModifier;

        if(isInSessionEvent())
            return sessionEventModifier;

        if(isInTransaction())
            return applyModifier;

        return dataModifier;
    }

    public Set<SessionDataProperty> recursiveUsed = SetFact.mAddRemoveSet();
    public List<ActionPropertyValueImplement> recursiveActions = ListFact.mAddRemoveList();
    public void addRecursion(ActionPropertyValueImplement action, ImSet<SessionDataProperty> sessionUsed, boolean singleApply) {
        action.property.singleApply = singleApply; // жестко конечно, но пока так
        recursiveActions.add(action);
        recursiveUsed.addAll(sessionUsed.toJavaSet());
    }

    public boolean apply(final BusinessLogics<?> BL, boolean onlyCheck, FormInstance form) throws SQLException {
        if(!hasChanges())
            return true;

        // до чтения persistent свойств в сессию
        if (applyObject == null) {
            applyObject = addObject(sessionClass);
            Integer changed = data.size();
            String dataChanged = "";
            for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> entry : data.entrySet()){
                dataChanged+=entry.getKey().getSID() + ": " + entry.getValue().getCount() + "\n";
            }
            BL.systemEventsLM.changesSession.change(dataChanged, DataSession.this, applyObject);
            currentSession.change(applyObject.object, DataSession.this);
            if (form != null){
                BL.systemEventsLM.connectionSession.change(form.instanceFactory.connection, (ExecutionEnvironment)DataSession.this, applyObject);
                Object ne = BL.reflectionLM.navigatorElementSID.read(form, new DataObject(form.entity.getSID(), StringClass.get(50)));
                if(ne!=null) 
                    BL.systemEventsLM.navigatorElementSession.change(new DataObject(ne, BL.reflectionLM.navigatorElement), (ExecutionEnvironment)DataSession.this, applyObject);
                BL.systemEventsLM.quantityAddedClassesSession.change(add.size(), DataSession.this, applyObject);
                BL.systemEventsLM.quantityRemovedClassesSession.change(remove.size(), DataSession.this, applyObject);
                BL.systemEventsLM.quantityChangedClassesSession.change(changed, DataSession.this, applyObject);
            }
        }

        executeSessionEvents(form);

        // очистим, так как в транзакции уже другой механизм используется, и старые increment'ы будут мешать
        dataModifier.clearHints(sql);

//        assert !isInTransaction();
        startTransaction();

        try {
            return recursiveApply(SetFact.<ActionPropertyValueImplement>EMPTYORDER(), BL, onlyCheck);
        } catch (SQLException e) { // assert'им что последняя SQL комманда, работа с транзакцией
            apply.clear(sql);
            dataModifier.clearHints(sql); // drop'ем hint'ы (можно и без sql но пока не важно)
            rollbackTransaction();
            throw e;
        }
    }

    private IdentityHashMap<FormInstance, Object> activeForms = new IdentityHashMap<FormInstance, Object>();
    public void registerForm(FormInstance form) throws SQLException {
        activeForms.put(form, true);

        dropFormCaches();
    }
    public void unregisterForm(FormInstance<?> form) throws SQLException {
        for(SessionModifier modifier : form.modifiers.values())
            modifier.clean(sql);

        activeForms.remove(form);
        incrementChanges.remove(form);
        appliedChanges.remove(form);
        updateChanges.remove(form);

        dropFormCaches();
    }
    private void dropFormCaches() throws SQLException {
        // убираем все prevStart
        for(OverrideSessionModifier modifier : prevStartSessionEventModifiers) {
            for(FormInstance<?> activeForm : activeForms.keySet()) {
                SessionModifier formModifier = activeForm.modifiers.remove(modifier);
                if(formModifier!=null)
                    formModifier.clean(sql);
            }
            modifier.clean(sql);
        }
        prevStartSessionEventModifiers.clear();
        mapPrevStartSessionEventModifiers.clear();

        activeSessionEvents = null;
        sessionEventOldDepends = null;

        final ImSet<OldProperty> eventOlds = getSessionEventOldDepends();
        ImMap<ImSet<OldProperty>, ImSet<ActionProperty>> conflictActions = getActiveSessionEvents().getSet().mapValues(new GetValue<ImSet<OldProperty>, ActionProperty>() {
            public ImSet<OldProperty> getMapValue(ActionProperty eventAction) {
                return eventAction.getSessionEventOldStartDepends().filter(eventOlds);
            }
        }).groupValues();
        for(int i=0,size=conflictActions.size();i<size;i++) {
            ImSet<OldProperty> conflictPrevs = conflictActions.getKey(i);
            if(!conflictPrevs.isEmpty()) {
                OverrideSessionModifier conflictModifier = new OverrideSessionModifier(new IncrementChangeProps(conflictPrevs), commonSessionEventModifier);
                prevStartSessionEventModifiers.add(conflictModifier);
                MapFact.addJavaAll(mapPrevStartSessionEventModifiers, conflictActions.getValue(i).toMap(conflictModifier));
            }
        }
    }
    public Set<FormInstance> getActiveForms() {
        return activeForms.keySet();
    }
    private <K> ImOrderSet<K> filterEnv(ImOrderMap<K, SessionEnvEvent> elements) {
        return elements.filterOrderValues(new SFunctionSet<SessionEnvEvent>() {
            public boolean contains(SessionEnvEvent elements) {
                return elements.contains(DataSession.this);
            }});
    }
    
    private boolean recursiveApply(ImOrderSet<ActionPropertyValueImplement> actions, BusinessLogics BL, boolean onlyCheck) throws SQLException {
        // тоже нужен посередине, чтобы он успел dataproperty изменить до того как они обработаны
        ImOrderMap<Object, SessionEnvEvent> execActions = MapFact.addOrderExcl(actions.toOrderMap(SessionEnvEvent.ALWAYS), BL.getAppliedProperties(onlyCheck));
        for (Object property : filterEnv(execActions)) {
            if(property instanceof ActionPropertyValueImplement) {
                startPendingSingles(((ActionPropertyValueImplement) property).property);
                ((ActionPropertyValueImplement)property).execute(this);
                if(!isInTransaction()) // если ушли из транзакции вываливаемся
                    return false;
                flushPendingSingles(BL);
            }
            if(property instanceof CalcProperty) // постоянно-хранимые свойства
                readStored((CalcProperty<PropertyInterface>) property, BL);
        }

        if (onlyCheck) {
            cancel();
            return true;
        }

        ImOrderSet<ActionPropertyValueImplement> updatedRecursiveActions = updateRecursiveActions();

        // записываем в базу, то что туда еще не сохранено, приходится сохранять группами, так могут не подходить по классам
        packRemoveClasses(BL); // нужно делать до, так как классы должны быть актуальными, иначе спакует свои же изменения
        ImMap<ImplementTable, ImSet<CalcProperty>> groupTables = groupPropertiesByTables();
        for (int i=0,size=groupTables.size();i<size;i++) {
            ImplementTable table = groupTables.getKey(i);
            savePropertyChanges(table, readSave(table, groupTables.getValue(i)));
        }

        apply.clear(sql); // все сохраненные хинты обнуляем
        restart(false, SetFact.fromJavaSet(recursiveUsed)); // оставляем usedSessiona

        if(recursiveActions.size() > 0) {
            recursiveUsed.clear(); recursiveActions.clear();
            return recursiveApply(updatedRecursiveActions, BL, onlyCheck);
        }

        commitTransaction();

        return true;
    }

    private ImOrderSet<ActionPropertyValueImplement> updateRecursiveActions() throws SQLException {
        ImOrderSet<ActionPropertyValueImplement> updatedRecursiveActions = null;
        if(recursiveActions.size()>0) {
            recursiveUsed.add((SessionDataProperty) currentSession.property);

            updateCurrentClasses(filterKeys(data, recursiveUsed).values()); // обновить классы sessionDataProperty, которые остались

            MOrderExclSet<ActionPropertyValueImplement> mUpdatedRecursiveActions = SetFact.mOrderExclSet();
            for(ActionPropertyValueImplement recursiveAction : recursiveActions)
                mUpdatedRecursiveActions.exclAdd(recursiveAction.updateCurrentClasses(this));
            updatedRecursiveActions = mUpdatedRecursiveActions.immutableOrder();
        }
        return updatedRecursiveActions;
    }

    private void packRemoveClasses(BusinessLogics BL) throws SQLException {
        if(news==null)
            return;

        // проводим "мини-паковку", то есть удаляем все записи, у которых ключем является удаляемый объект
        for(ImplementTable table : BL.LM.tableFactory.getImplementTables(SetFact.fromJavaSet(remove))) {
            QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(table);
            Where removeWhere = Where.FALSE;
            for (int i=0,size=table.mapFields.size();i<size;i++) {
                ValueClass value = table.mapFields.getValue(i);
                if (remove.contains(value)) {
                    Join<String> newJoin = news.join(query.getMapExprs().get(table.mapFields.getKey(i)));
                    removeWhere = removeWhere.or(newJoin.getWhere().and(isValueClass(newJoin.getExpr("value"), (CustomClass) value, usedNewClasses).not()));
                }
            }
            query.and(table.join(query.getMapExprs()).getWhere().and(removeWhere));
            sql.deleteRecords(new ModifyQuery(table, query.getQuery(), getQueryEnv()));
        }
    }

    @Message("message.session.apply.write")
    private <P extends PropertyInterface> void readStored(@ParamMessage CalcProperty<P> property, BusinessLogics<?> BL) throws SQLException {
        assert isInTransaction();
        assert property.isStored();
        if(property.hasChanges(getModifier())) {
            apply.add(property, splitApplySingleStored(property,
                    property.readChangeTable(sql, getModifier(), baseClass, env), BL));
        }
    }

    protected SQLSession getSQL() {
        return sql;
    }

    protected BaseClass getBaseClass() {
        return baseClass;
    }

    public QueryEnvironment getQueryEnv() {
        return env;
    }

    public final QueryEnvironment env = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return new TypeObject(user.getCurrentUser().object, ObjectType.instance);
        }

        public ParseInterface getIsFullClient() {
            return new LogicalParseInterface() {
                public boolean isTrue() {
                    return computer.isFullClient();
                }
            };
        }

        public ParseInterface getSQLComputer() {
            return new TypeObject(computer.getCurrentComputer().object, ObjectType.instance);
        }

        public ParseInterface getIsServerRestarting() {
            return new LogicalParseInterface() {
                public boolean isTrue() {
                    return isServerRestarting.isServerRestarting();
                }
            };
        }
    };

    private void aspectChangeClass(ImSet<CustomClass> addClasses, ImSet<CustomClass> removeClasses, ImSet<ConcreteObjectClass> oldClasses, ImSet<ConcreteObjectClass> newClasses, ClassChange change) throws SQLException {
        checkTransaction(); // важно что, вначале

        SetFact.addJavaAll(usedOldClasses, oldClasses);
        SetFact.addJavaAll(usedNewClasses, newClasses);

        // оптимизация
        changeSingle(addClasses, change, singleAdd, add, singleRemove, remove);
        changeSingle(removeClasses, change, singleRemove, remove, singleAdd, add);

        if(news ==null)
            news = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
        change.modifyRows(news, sql, baseClass, Modify.MODIFY, env);
        this.newClasses.clear();

        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> dataChange : data.entrySet()) { // удаляем существующие изменения
            DataProperty property = dataChange.getKey();
            if(property.depends(removeClasses)) { // оптимизация
                SinglePropertyTableUsage<ClassPropertyInterface> table = dataChange.getValue();

                // кейс с удалением, похож на getEventChange и в saveClassChanges - "мини паковка"
                Where removeWhere = Where.FALSE;
                ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
                for(ClassPropertyInterface propertyInterface : property.interfaces)
                    if(SetFact.contains(propertyInterface.interfaceClass, removeClasses)) {
                        Join<String> newJoin = change.join(mapKeys.get(propertyInterface));
                        removeWhere = removeWhere.or(newJoin.getWhere().and(isValueClass(newJoin.getExpr("value"), (CustomClass) propertyInterface.interfaceClass, usedNewClasses).not()));
                        dataModifier.eventDataChange(property);
                    }
                Join<String> join = table.join(mapKeys);
                removeWhere = removeWhere.and(join.getWhere());

                if(SetFact.contains(property.value, removeClasses)) {
                    Join<String> newJoin = change.join(join.getExpr("value"));
                    removeWhere = removeWhere.or(newJoin.getWhere().and(isValueClass(newJoin.getExpr("value"), (CustomClass) property.value, usedNewClasses).not()));
                    dataModifier.eventDataChange(property);
                }
                table.modifyRows(sql, new Query<ClassPropertyInterface, String>(mapKeys, removeWhere), baseClass, Modify.DELETE, getQueryEnv());
            }
        }
    }

    private static void changeSingle(ImSet<CustomClass> thisChanged, ClassChange thisChange, Map<CustomClass, DataObject> single, Set<CustomClass> changed, Map<CustomClass, DataObject> singleBack, Set<CustomClass> changedBack) {
        for(CustomClass changeClass : thisChanged) {
            if(changed.contains(changeClass))
                single.remove(changeClass);
            else {
                if(thisChange.keyValue !=null)
                    single.put(changeClass, thisChange.keyValue);
                changed.add(changeClass);
            }

            DataObject removeObject = singleBack.get(changeClass);
            if(removeObject!=null && thisChange.keyValue !=null && removeObject.equals(thisChange.keyValue)) {
                singleBack.remove(changeClass);
                changedBack.remove(changeClass);
            }
        }
    }

    private void aspectDropChanges(final DataProperty property) throws SQLException {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.remove(property);
        if(dataChange!=null)
            dataChange.drop(sql);
    }

    private void aspectChangeProperty(final DataProperty property, PropertyChange<ClassPropertyInterface> change) throws SQLException {
        checkTransaction();

        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange == null) { // создадим таблицу, если не было
            dataChange = property.createChangeTable();
            data.put(property, dataChange);
        }
        change.modifyRows(dataChange, sql, baseClass, Modify.MODIFY, getQueryEnv());
    }

    public void dropTables(ImSet<SessionDataProperty> keep) throws SQLException {
        for(SinglePropertyTableUsage<ClassPropertyInterface> dataTable : BaseUtils.filterNotKeys(data, keep).values())
            dataTable.drop(sql);
        if(news !=null)
            news.drop(sql);

        dataModifier.eventDataChanges(getChangedProps());
    }

    public DataSession getSession() {
        return this;
    }

    public FormInstance getFormInstance() {
        return null;
    }

    public boolean isInTransaction() {
        return sql.isInTransaction();
    }

    public void cancel() throws SQLException {
        if(isInSessionEvent()) {
            sessionEventModifier = null;
        }

        if(isInTransaction()) {
            if(neededProps!=null) {
                for(SinglePropertyTableUsage table : pendingSingleTables.values())
                    table.drop(sql);
                pendingSingleTables.clear();
                neededProps = null;
                assert !flush;
            }

            recursiveUsed.clear();
            recursiveActions.clear();

            // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы
            apply.clear(sql);
            dataModifier.clearHints(sql); // drop'ем hint'ы (можно и без sql но пока не важно)
            rollbackTransaction();
            return;
        }

        restart(true, SetFact.<SessionDataProperty>EMPTY());
    }

    private <P extends PropertyInterface> void updateApplyStart(OldProperty<P> property, SinglePropertyTableUsage<P> tableUsage) throws SQLException { // изврат конечно
        assert isInTransaction();

        SinglePropertyTableUsage<P> prevTable = apply.getTable(property);
        if(prevTable==null) {
            prevTable = property.createChangeTable();
            apply.add(property, prevTable);
        }
        ImRevMap<P, KeyExpr> mapKeys = property.getMapKeys();
        prevTable.modifyRows(sql, mapKeys, property.getExpr(mapKeys), tableUsage.join(mapKeys).getWhere(), baseClass, Modify.LEFT, env); // если он уже был в базе он не заместится
        apply.eventChange(property);
        tableUsage.drop(sql);
    }

    public ImMap<ImplementTable, ImSet<CalcProperty>> groupPropertiesByTables() {
        return apply.getProperties().group(
                new BaseUtils.Group<ImplementTable, CalcProperty>() {
                    public ImplementTable group(CalcProperty key) {
                        if (key.isStored())
                            return key.mapTable.table;
                        assert key instanceof OldProperty;
                        return null;
                    }
                });
    }

    public <P extends PropertyInterface> SessionTableUsage<KeyField, CalcProperty> splitReadSave(ImplementTable table, ImSet<CalcProperty> properties) throws SQLException {
        IncrementChangeProps increment = new IncrementChangeProps();
        MAddSet<SessionTableUsage<KeyField, CalcProperty>> splitTables = SetFact.mAddSet();

        final int split = (int) Math.sqrt(properties.size());
        final ImOrderSet<CalcProperty> propertyOrder = properties.toOrderSet();
        for(ImSet<CalcProperty> splitProps : properties.group(new BaseUtils.Group<Integer, CalcProperty>() {
                    public Integer group(CalcProperty key) {
                        return propertyOrder.indexOf(key) / split;
                    }}).valueIt()) {
            SessionTableUsage<KeyField, CalcProperty> splitChangesTable = readSave(table, splitProps, getModifier());
            for(CalcProperty<P> splitProp : splitProps)
                increment.add(splitProp, SessionTableUsage.getChange(splitChangesTable, splitProp.mapTable.mapKeys, splitProp));
            splitTables.add(splitChangesTable);
        }

        OverrideSessionModifier modifier = new OverrideSessionModifier(increment, emptyModifier);
        SessionTableUsage<KeyField, CalcProperty> result = readSave(table, properties, modifier);

        modifier.clean(sql);
        for(SessionTableUsage<KeyField, CalcProperty> splitTable : splitTables)
            splitTable.drop(sql);

        return result;
    }

    @Message("message.increment.read.properties")
    public SessionTableUsage<KeyField, CalcProperty> readSave(ImplementTable table, @ParamMessage ImSet<CalcProperty> properties) throws SQLException {
        assert isInTransaction();

        final int split = Settings.get().getSplitIncrementApply();
        if(properties.size() > split) // если слишком много групп, разделим на несколько read'ов
            return splitReadSave(table, properties);

        return readSave(table, properties, getModifier());
    }

    public <P extends PropertyInterface> SessionTableUsage<KeyField, CalcProperty> readSave(ImplementTable table, ImSet<CalcProperty> properties, Modifier modifier) throws SQLException {
        // подготавливаем запрос
        QueryBuilder<KeyField, CalcProperty> changesQuery = new QueryBuilder<KeyField, CalcProperty>(table);
        WhereBuilder changedWhere = new WhereBuilder();
        for (CalcProperty<P> property : properties)
            changesQuery.addProperty(property, property.getIncrementExpr(property.mapTable.mapKeys.join(changesQuery.getMapExprs()), modifier, changedWhere));
        changesQuery.and(changedWhere.toWhere());

        // подготовили - теперь надо сохранить в курсор и записать классы
        SessionTableUsage<KeyField, CalcProperty> changeTable =
                new SessionTableUsage<KeyField, CalcProperty>(table.keys, properties.toOrderSet(), Field.<KeyField>typeGetter(),
                        new Type.Getter<CalcProperty>() {
                            public Type getType(CalcProperty key) {
                                return key.getType();
                            }
                        });
        changeTable.writeRows(sql, changesQuery.getQuery(), baseClass, env);
        return changeTable;
    }

    public void pushVolatileStats() throws SQLException {
        sql.pushVolatileStats(null);
    }

    public void popVolatileStats() throws SQLException {
        sql.popVolatileStats(null);
    }
}
