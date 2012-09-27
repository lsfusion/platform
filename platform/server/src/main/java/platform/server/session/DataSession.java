package platform.server.session;

import platform.base.*;
import platform.interop.Compare;
import platform.server.Context;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.caches.IdentityLazy;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
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
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.table.ObjectTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.filterKeys;
import static platform.base.BaseUtils.remove;
import static platform.base.BaseUtils.removeSet;

public class DataSession extends ExecutionEnvironment implements SessionChanges {

    private Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>();
    private SingleKeyPropertyUsage news = null;

    // оптимизационные вещи
    private Set<CustomClass> add = new HashSet<CustomClass>();
    private Set<CustomClass> remove = new HashSet<CustomClass>();
    private Set<ConcreteObjectClass> usedNewClasses = new HashSet<ConcreteObjectClass>();
    private Map<CustomClass, DataObject> singleAdd = new HashMap<CustomClass, DataObject>();
    private Map<CustomClass, DataObject> singleRemove = new HashMap<CustomClass, DataObject>();
    private Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public static Where isValueClass(Expr expr, CustomClass customClass, Set<ConcreteObjectClass> usedNewClasses) {
        Where result = Where.FALSE;
        for(ConcreteObjectClass usedClass : usedNewClasses)
            if(usedClass instanceof ConcreteCustomClass) {
                ConcreteCustomClass customUsedClass = (ConcreteCustomClass) usedClass;
                if(customUsedClass.isChild(customClass)) // если изменяется на класс, у которого
                    result = result.or(expr.compare(customUsedClass.getClassObject(), Compare.EQUALS));
            }
        return result;
    }

    public QuickSet<CalcProperty> getChangedProps(Set<CustomClass> add, Set<CustomClass> remove, Set<DataProperty> data) {
        return new QuickSet<CalcProperty>(BaseUtils.mergeSet(getClassChanges(add, remove), data));
    }
    public QuickSet<CalcProperty> getChangedProps() {
        return getChangedProps(add, remove, data.keySet());
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

        protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, FunctionSet<CalcProperty> overrided) {
            PropertyChange<P> propertyChange = getPropertyChange(property);
            if(propertyChange!=null)
                return new ModifyChange<P>(propertyChange, false);
            return null;
        }

        public QuickSet<CalcProperty> calculateProperties() {
            return getChangedProps();
        }
    }
    private final DataModifier dataModifier = new DataModifier();

    protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        if(property instanceof ObjectClassProperty)
            return (PropertyChange<P>) getObjectClassChange((ObjectClassProperty) property);

        if(property instanceof IsClassProperty)
            return (PropertyChange<P>) getClassChange((IsClassProperty) property);

        if(property instanceof DataProperty)
            return (PropertyChange<P>) getDataChange((DataProperty) property);
        return null;
    }

    private class Transaction {
        private final Set<CustomClass> add;
        private final Set<CustomClass> remove;
        private final Set<ConcreteObjectClass> usedNewClases;
        private final Map<CustomClass, DataObject> singleAdd;
        private final Map<CustomClass, DataObject> singleRemove;
        private final Map<DataObject, ConcreteObjectClass> newClasses;

        private final SessionData news;
        private final Map<DataProperty, SessionData> data;

        private Transaction() {
            assert sessionEventChangedOld.isEmpty(); // в транзакции никаких сессионных event'ов быть не может
//            assert applyModifier.getHintProps().isEmpty(); // равно как и хинт'ов, не факт, потому как транзакция не сразу создается

            add = new HashSet<CustomClass>(DataSession.this.add);
            remove = new HashSet<CustomClass>(DataSession.this.remove);
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
            Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> rollData = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>();
            for(Map.Entry<DataProperty, SessionData> entry : data.entrySet()) {
                SinglePropertyTableUsage<ClassPropertyInterface> table = DataSession.this.data.get(entry.getKey());
                if(table==null) {
                    table = entry.getKey().createChangeTable();
                    table.drop(sql);
                }
                table.rollData(sql, entry.getValue());
                rollData.put(entry.getKey(), table);
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

            dropTables(new HashSet<SessionDataProperty>()); // старые вернем, таблицу удалятся (но если нужны будут, rollback откатит эти изменения)

            // assert что новые включают старые
            DataSession.this.add = add;
            DataSession.this.remove = remove;
            DataSession.this.usedNewClasses = usedNewClases;
            DataSession.this.singleAdd = singleAdd;
            DataSession.this.singleRemove = singleRemove;
            DataSession.this.newClasses = newClasses;

            rollData();
            rollNews();
            
            dataModifier.eventDataChanges(getChangedProps(add, remove, data.keySet()));
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

    private Set<AggregateProperty<ClassPropertyInterface>> getClassChanges(Set<CustomClass> addClasses, Set<CustomClass> removeClasses) {
        return BaseUtils.addSet(CustomClass.getProperties(addClasses, removeClasses), baseClass.getObjectClassProperty());
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
            return SingleKeyPropertyUsage.getChange(news, BaseUtils.single(property.interfaces));
        return null;
    }

    private PropertyChange<ClassPropertyInterface> getClassChange(IsClassProperty property) {
        ValueClass isClass = property.getInterfaceClass();
        if(isClass instanceof CustomClass) {
            CustomClass customClass = (CustomClass) isClass;
            boolean added = add.contains(customClass);
            boolean removed = remove.contains(customClass);
            if(added || removed) { // оптимизация в том числе
                Map<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
                KeyExpr key = BaseUtils.singleValue(mapKeys);

                Join<String> join = news.join(key);
                Expr newClassExpr = join.getExpr("value");

                Where hasClass = isValueClass(newClassExpr, customClass, usedNewClasses);
                Where hadClass = key.isClass(isClass.getUpSet());

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

        public final QuickSet<CalcProperty> properties;

        public UpdateChanges() {
            properties = new QuickSet<CalcProperty>();
        }

        public UpdateChanges(DataSession session) {
            properties = session.getChangedProps();
        }

        public void add(UpdateChanges changes) {
            properties.addAll(changes.properties);
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
    
    private final List<ActionProperty> sessionEvents;

    public DataSession(SQLSession sql, final UserController user, final ComputerController computer, IsServerRestartingController isServerRestarting, BaseClass baseClass, ConcreteCustomClass sessionClass, LCP currentSession, SQLSession idSession, List<ActionProperty> sessionEvents) throws SQLException {
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

    public void restart(boolean cancel, Set<SessionDataProperty> keep) throws SQLException {

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

    public <T extends PropertyInterface> SinglePropertyTableUsage<T> addObjects(ConcreteCustomClass cls, PropertySet<T> set) throws SQLException {
        final Query<T, String> query = set.getAddQuery(baseClass); // query, который генерит номера записей (one-based)

        // сначала закидываем в таблицу set с номерами рядов (!!! нужно гарантировать однозначность)
        SinglePropertyTableUsage<T> table = new SinglePropertyTableUsage<T>(new ArrayList<T>(query.getMapKeys().keySet()), new Type.Getter<T>() {
            public Type getType(T key) {
                return query.getKeyType(key);
            }
        }, ObjectType.instance);
        table.modifyRows(sql, query, baseClass, Modify.ADD, env);

        if(table.isEmpty()) // оптимизация, не зачем генерить id и все такое
            return table;

        // берем количество рядов - резервируем ID'ки
        int startFrom = IDTable.instance.reserveIDs(table.getCount(), idSession, IDTable.OBJECT);

        // update'им на эту разницу ключи, чтобы сгенерить объекты
        table.updateAdded(sql, baseClass, startFrom-1); // так как не zero-based отнимаем 1

        // вообще избыточно, если compile'ить отдельно в for() + changeClass, который сам сгруппирует, но тогда currentClass будет unknown в свойстве что вообщем то не возможно
        KeyExpr keyExpr = new KeyExpr("keyExpr");
        changeClass(new ClassChange(keyExpr, GroupExpr.create(Collections.singletonMap("key", table.join(query.mapKeys).getExpr("value")),
                Where.TRUE, Collections.singletonMap("key", keyExpr)).getWhere(), cls));
        
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

    public <K, T extends ObjectValue> Map<K, T> updateCurrentClasses(Map<K, T> objectValues) throws SQLException {
        Map<K, T> result = new HashMap<K, T>();
        for(Map.Entry<K, T> entry : objectValues.entrySet())
            result.put(entry.getKey(), (T) updateCurrentClass(entry.getValue()));
        return result;
    }

    public ObjectValue updateCurrentClass(ObjectValue value) throws SQLException {
        if(value instanceof NullValue)
            return value;
        else {
            DataObject dataObject = (DataObject)value;
            return new DataObject(dataObject.object, getCurrentClass(dataObject));
        }
    }

    public <K> List<Map<K, ConcreteObjectClass>> readDiffClasses(Where where, Map<K, ? extends Expr> classExprs, Map<K, ? extends Expr> objectExprs) throws SQLException {
        Map<K, KeyExpr> keys = new HashMap<K, KeyExpr>();

        ValueExpr unknownExpr = new ValueExpr(-1, baseClass.unknown);
        Map<K, Expr> group = new HashMap<K, Expr>();

        for(Map.Entry<K, ? extends Expr> classExpr : classExprs.entrySet()) {
            keys.put(classExpr.getKey(), new KeyExpr("key" + keys.size()));
            group.put(classExpr.getKey(), classExpr.getValue().nvl(unknownExpr));
        }
        for(Map.Entry<K, ? extends Expr> classExpr : objectExprs.entrySet()) {
            keys.put(classExpr.getKey(), new KeyExpr("key" + keys.size()));
            group.put(classExpr.getKey(), baseClass.getObjectClassProperty().getExpr(classExpr.getValue(), getModifier()).nvl(unknownExpr));
        }

        List<Map<K, ConcreteObjectClass>> result = new ArrayList<Map<K, ConcreteObjectClass>>();
        for(Map<K, Object> readClasses : new Query<K, String>(keys, GroupExpr.create(group, where, keys).getWhere()).execute(this).keySet()) {
            Map<K, ConcreteObjectClass> readObjectClasses = new HashMap<K, ConcreteObjectClass>();
            for(Map.Entry<K, Object> readClass : readClasses.entrySet()) {
                Integer id = (Integer) readClass.getValue();
                readObjectClasses.put(readClass.getKey(), baseClass.findConcreteClassID(id != -1 ? id : null));
            }
            result.add(readObjectClasses);
        }
        return result;
    }

    public void changeClass(ClassChange change) throws SQLException {
        if(change.isEmpty()) // оптимизация, важна так как во многих event'ах может учавствовать
            return;
        
        boolean hadStoredChanges = hasStoredChanges();

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        if(change.keyValue !=null) { // оптимизация
            ConcreteObjectClass newcl = baseClass.findConcreteClassID((Integer) change.propValue.getValue());
            newcl.getDiffSet((ConcreteObjectClass) getCurrentClass(change.keyValue), addClasses, removeClasses);
            usedNewClasses.add(newcl);
        } else {
            change = change.materialize(sql, baseClass, env); // materialize'им изменение

            // читаем варианты изменения классов
            for(Map<String, ConcreteObjectClass> diffClasses : readDiffClasses(change.where, Collections.singletonMap("newcl", change.expr), Collections.singletonMap("prevcl", change.key))) {
                ConcreteObjectClass newcl = diffClasses.get("newcl");
                newcl.getDiffSet(diffClasses.get("prevcl"), addClasses, removeClasses);
                usedNewClasses.add(newcl);
            }
        }

        assert Collections.disjoint(addClasses,removeClasses);

        Collection<AggregateProperty<ClassPropertyInterface>> updateChanges = getClassChanges(addClasses, removeClasses);

        updateSessionEvents(updateChanges);

        aspectChangeClass(addClasses, removeClasses, change);

        // так как таблица news используется при определении изменений всех классов, то нужно обновить и их "источники"
        dataModifier.eventSourceChanges(CustomClass.getProperties(removeSet(add, addClasses), removeSet(remove, removeClasses)));
        updateProperties(updateChanges);

        aspectAfterChange(hadStoredChanges);
    }
    
    public void dropChanges(SessionDataProperty property) throws SQLException {
        aspectDropChanges(property);

        updateProperties(Collections.singleton(property));
    }

    public void changeProperty(DataProperty property, PropertyChange<ClassPropertyInterface> change) throws SQLException {
        boolean hadStoredChanges = hasStoredChanges();

        if(neededProps!=null && property.isStored() && property.event==null) { // если транзакция, нет change event'а, singleApply'им
            assert isInTransaction();

            SinglePropertyTableUsage<ClassPropertyInterface> changeTable = property.readFixChangeTable(sql, change, baseClass, getQueryEnv());

            Pair<SinglePropertyTableUsage<ClassPropertyInterface>, SinglePropertyTableUsage<ClassPropertyInterface>> split = property.splitFitClasses(changeTable, sql, baseClass, env);

            applySingleStored(property, split.first, Context.context.get().getBL());
            change = SinglePropertyTableUsage.getChange(split.second);
        }

        Set<DataProperty> updateChanges = Collections.singleton(property);

        updateSessionEvents(updateChanges);

        aspectChangeProperty(property, change);

        updateProperties(updateChanges);

        aspectAfterChange(hadStoredChanges);
    }

    public static final SessionDataProperty isDataChanged = new SessionDataProperty("isDataChanged", "Is data changed", LogicalClass.instance);
    private void aspectAfterChange(boolean hadStoredChanges) throws SQLException {
        if(!hadStoredChanges && hasStoredChanges()) {
            Set<SessionDataProperty> updateChanges = Collections.singleton(isDataChanged);
            updateSessionEvents(updateChanges);

            aspectChangeProperty(isDataChanged, new PropertyChange<ClassPropertyInterface>(new DataObject(true, LogicalClass.instance)));

            updateProperties(updateChanges);
        }
    }

    public void updateProperties(Collection<? extends CalcProperty> changes) throws SQLException {
        dataModifier.eventDataChanges(changes);

        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet()) {
            FormInstance<?> formInstance = (FormInstance<?>) incrementChange.getKey();
            incrementChange.getValue().properties.addAll(changes);
            formInstance.dataChanged = true;
        }
    }

    // для OldProperty хранит изменения с предыдущего execute'а
    private IncrementTableProps sessionEventChangedOld = new IncrementTableProps();
    private IncrementChangeProps sessionEventNotChangedOld = new IncrementChangeProps();
    private OverrideSessionModifier sessionEventModifier = new OverrideSessionModifier(new OverrideIncrementProps(sessionEventChangedOld, sessionEventNotChangedOld), dataModifier);

    @IdentityLazy
    private Set<OldProperty> getSessionEventOldDepends() {
        Set<OldProperty> result = new HashSet<OldProperty>();
        for(ActionProperty action : sessionEvents)
            result.addAll(action.getOldDepends());
        return result;
    }

    public <P extends PropertyInterface> void updateSessionEvents(Collection<? extends CalcProperty> changes) throws SQLException {
        if(!isInTransaction()) {
            StructChanges structChanges = new StructChanges(changes);
            for(OldProperty<PropertyInterface> old : getSessionEventOldDepends())
                if(!sessionEventChangedOld.contains(old) && old.property.hasChanges(structChanges, true)) // если влияет на old из сессионного event'а и еще не читалось
                    sessionEventChangedOld.add(old, old.property.readChangeTable(sql, getModifier(), baseClass, getQueryEnv()));
        }
    }

    private boolean inSessionEvent;
    private boolean isInSessionEvent() {
        return inSessionEvent;
    }

    public <T extends PropertyInterface> void executeSessionEvents() throws SQLException {

        if(sessionEventChangedOld.getProperties().size > 0) { // оптимизационная проверка
            inSessionEvent = true;
            for(ActionProperty<?> action : sessionEvents) {
                action.execute(this);
                if(!isInSessionEvent())
                    return;
            }
            inSessionEvent = false;

            // закидываем старые изменения
            for(CalcProperty changedOld : sessionEventChangedOld.getProperties()) // assert что только old'ы
                sessionEventNotChangedOld.add(changedOld, ((OldProperty<PropertyInterface>)changedOld).property.getIncrementChange(getModifier()));
            sessionEventChangedOld.clear(sql);
        }
    }


    private OverrideSessionModifier resolveModifier = null;

    public <T extends PropertyInterface> void resolve(ActionProperty<?> action) throws SQLException {
        IncrementChangeProps changes = new IncrementChangeProps();
        for(SessionCalcProperty sessionCalcProperty : action.getSessionCalcDepends())
            if(sessionCalcProperty instanceof ChangedProperty) // именно так, OldProperty нельзя подменять, так как предполагается что SET и DROPPED требуют разные значения PREV
                changes.add(sessionCalcProperty, ((ChangedProperty)sessionCalcProperty).getFullChange(getModifier()));
        resolveModifier = new OverrideSessionModifier(changes, FullFunctionSet.<CalcProperty>instance(), dataModifier);

        action.execute(this);

        resolveModifier.clean(sql);
        resolveModifier = null;
    }

    // для оптимизации
    public DataChanges getUserDataChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) throws SQLException {
        Pair<Map<ClassPropertyInterface, DataObject>, ObjectValue> simple;
        if((simple = change.getSimple())!=null) {
            if(IsClassProperty.fitClasses(getCurrentClasses(simple.first), property.value,
                    simple.second instanceof DataObject ? getCurrentClass((DataObject) simple.second) : null))
                return new DataChanges(property, change);
            else
                return new DataChanges();
        }
        return null;
    }

    public ConcreteClass getCurrentClass(DataObject value) throws SQLException {
        ConcreteObjectClass newClass = null;
        if(news!=null && value.objectClass instanceof ConcreteObjectClass) {
            if(newClasses.containsKey(value))
                newClass = newClasses.get(value);
            else {
                Collection<Map<String, Object>> read = news.read(this, value);
                if(read.isEmpty())
                    newClass = null;
                else
                    newClass = baseClass.findConcreteClassID((Integer) BaseUtils.single(read).get("value"));
                newClasses.put(value, newClass);
            }
        }

        if(newClass==null)
            return value.objectClass;
        else
            return newClass;
    }

    public <K> Map<K, ConcreteClass> getCurrentClasses(Map<K, DataObject> map) throws SQLException {
        Map<K, ConcreteClass> result = new HashMap<K, ConcreteClass>();
        for(Map.Entry<K, DataObject> entry : map.entrySet())
            result.put(entry.getKey(), getCurrentClass(entry.getValue()));
        return result;
    }

    public ObjectValue getCurrentValue(ObjectValue value) throws SQLException {
        if(value instanceof NullValue)
            return value;
        else {
            DataObject dataObject = (DataObject)value;
            return new DataObject(dataObject.object, getCurrentClass(dataObject));
        }
    }

    public <K, V extends ObjectValue> Map<K, V> getCurrentObjects(Map<K, V> map) throws SQLException {
        Map<K, V> result = new HashMap<K, V>();
        for(Map.Entry<K, V> entry : map.entrySet())
            result.put(entry.getKey(), (V) getCurrentValue(entry.getValue()));
        return result;
    }

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        return baseClass.getDataObject(sql, value, type);
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        return baseClass.getObjectValue(sql, value, type);
    }

    // узнает список изменений произошедших без него
    public QuickSet<CalcProperty> update(FormInstance<?> form) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)

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
            return Context.context.get().getLogMessage();
//            return ((LogMessageClientAction)BaseUtils.single(actions)).message;
    }

    public boolean apply(BusinessLogics BL) throws SQLException {
        return apply(BL, false);
    }

    public boolean check(BusinessLogics BL) throws SQLException {
        return apply(BL, true);
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

        public QuickSet<CalcProperty> calculateProperties() {
            return new QuickSet<CalcProperty>();
        }

        protected <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, FunctionSet<CalcProperty> overrided) {
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

    private <T extends PropertyInterface, D extends PropertyInterface> void applySingleStored(CalcProperty<T> property, SinglePropertyTableUsage<T> change, BusinessLogics<?> BL) throws SQLException {
        assert isInTransaction();

        // assert что у change классы совпадают с property
        assert property.isStored();
        assert fitClasses(property, change); // проверяет гипотезу
        assert fitKeyClasses(property, change); // дополнительная проверка, она должна обеспечиваться тем что в change не должно быть замен null на null

        if(change.isEmpty())
            return;

        // тут есть assert что в increment+noUpdate не будет noDB, то есть не пересекется с NoEventModifier, то есть можно в любом порядке increment'ить
        IncrementTableProps increment = new IncrementTableProps(property, change);
        IncrementChangeProps noUpdate = new IncrementChangeProps();
        for(CalcProperty event : BL.getDataChangeEvents())
            noUpdate.addNoChange(event);

        OverrideSessionModifier modifier = new OverrideSessionModifier(new OverrideIncrementProps(noUpdate, increment), emptyModifier);

        List<CalcProperty> dependProps = BL.getAppliedDependFrom(property); // !!! важно в лексикографическом порядке должно быть

        if(neededProps!=null && !flush) { // придется отдельным прогоном чтобы правильную лексикографику сохранить
            for(CalcProperty<D> depend : dependProps)
                if(!neededProps.contains(depend)) {
                    updatePendingApplyStart(property, change);
                    break;
                }
        }

        for(CalcProperty<D> depend : dependProps) {
            assert depend.isStored() || depend instanceof OldProperty;

            if(neededProps!=null) { // управление pending'ом
                assert !flush || !pendingSingleTables.containsKey(depend); // assert что если flush то уже обработано (так как в обратном лексикографике идет)
                if(!neededProps.contains(depend)) { // если не нужная связь не обновляем
                    if(!flush)
                        continue;
                } else { // если нужная то уже обновили
                    if(flush) {
                        if(depend.isStored())
                            noUpdate.addNoChange(depend);
                        continue;
                    }
                }
            }

            if(depend.isStored()) { // читаем новое значение, запускаем рекурсию
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
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
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

            Map<T, KeyExpr> mapKeys = property.getMapKeys();
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
        savePropertyChanges(implementTable, BaseUtils.toMap(changeTable.getValues()), BaseUtils.toMap(changeTable.getKeys()), changeTable);
    }

    private <T extends PropertyInterface> void savePropertyChanges(CalcProperty<T> property, SinglePropertyTableUsage<T> change) throws SQLException {
        savePropertyChanges(property.mapTable.table, Collections.singletonMap("value", (CalcProperty) property), property.mapTable.mapKeys, change);
    }

    private <K,V> void savePropertyChanges(Table implementTable, Map<V, CalcProperty> props, Map<K, KeyField> mapKeys, SessionTableUsage<K, V> changeTable) throws SQLException {
        Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(implementTable);
        Join<V> join = changeTable.join(BaseUtils.join(mapKeys, modifyQuery.mapKeys));
        for (Map.Entry<V, CalcProperty> property : props.entrySet())
            modifyQuery.properties.put(property.getValue().field, join.getExpr(property.getKey()));
        modifyQuery.and(join.getWhere());
        sql.modifyRecords(new ModifyQuery(implementTable, modifyQuery, env));
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

    public Set<SessionDataProperty> recursiveUsed = new HashSet<SessionDataProperty>();
    public List<ActionPropertyValueImplement> recursiveActions = new ArrayList<ActionPropertyValueImplement>();
    public void addRecursion(ActionPropertyValueImplement action, Set<SessionDataProperty> sessionUsed, boolean singleApply) {
        action.property.singleApply = singleApply; // жестко конечно, но пока так
        recursiveActions.add(action);
        recursiveUsed.addAll(sessionUsed);
    }

    public boolean apply(final BusinessLogics<?> BL, boolean onlyCheck) throws SQLException {
        if(!hasChanges())
            return true;

        // до чтения persistent свойств в сессию
        if (applyObject == null) {
            applyObject = addObject(sessionClass);
            currentSession.change(applyObject.object, DataSession.this);
        }

        executeSessionEvents();

        // очистим, так как в транзакции уже другой механизм используется, и старые increment'ы будут мешать
        dataModifier.clearHints(sql);

//        assert !isInTransaction();
        startTransaction();

        return recursiveApply(new ArrayList<ActionPropertyValueImplement>(), BL, onlyCheck);
    }
    
    private boolean recursiveApply(List<ActionPropertyValueImplement> actions, BusinessLogics BL, boolean onlyCheck) throws SQLException {
        // тоже нужен посередине, чтобы он успел dataproperty изменить до того как они обработаны
        for (Object property : BaseUtils.mergeList(actions, BL.getAppliedProperties(onlyCheck))) {
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

        // записываем в базу, то что туда еще не сохранено, приходится сохранять группами, так могут не подходить по классам
        packRemoveClasses(BL); // нужно делать до, так как классы должны быть актуальными, иначе спакует свои же изменения
        for (Map.Entry<ImplementTable, Collection<CalcProperty>> groupTable : groupPropertiesByTables().entrySet())
            savePropertyChanges(groupTable.getKey(), readSave(groupTable.getKey(), groupTable.getValue()));

        List<ActionPropertyValueImplement> updatedRecursiveActions = null;
        if(recursiveActions.size()>0) {
            recursiveUsed.add((SessionDataProperty) currentSession.property);
        
            updateCurrentClasses(filterKeys(data, recursiveUsed).values()); // обновить классы sessionDataProperty, которые остались
            
            updatedRecursiveActions = new ArrayList<ActionPropertyValueImplement>();
            for(ActionPropertyValueImplement recursiveAction : recursiveActions)
                updatedRecursiveActions.add(recursiveAction.updateCurrentClasses(this));
        }

        saveClassChanges(BL);

        apply.clear(sql); // все сохраненные хинты обнуляем

        restart(false, recursiveUsed); // оставляем usedSessiona

        if(recursiveActions.size() > 0) {
            recursiveUsed.clear(); recursiveActions.clear();
            return recursiveApply(updatedRecursiveActions, BL, onlyCheck);
        }

        commitTransaction();

        return true;
    }

    private void saveClassChanges(BusinessLogics BL) throws SQLException {
        // сохраняем изменения по классам
        if(news==null)
            return;

        ObjectTable classTable = baseClass.table;

        KeyExpr keyExpr = new KeyExpr("object");
        Join<String> join = news.join(keyExpr);
        Expr changeExpr = join.getExpr("value");

        boolean hasDelete = usedNewClasses.contains(baseClass.unknown);
        if(!(usedNewClasses.size()==1 && hasDelete))
            sql.modifyRecords(new ModifyQuery(classTable, new Query<KeyField, PropertyField>(Collections.singletonMap(classTable.key, keyExpr),
                    changeExpr, classTable.objectClass, changeExpr.getWhere()), getQueryEnv()));
        if(hasDelete)
            sql.deleteRecords(new ModifyQuery(classTable, new Query<KeyField, PropertyField>(Collections.singletonMap(classTable.key, keyExpr),
                    join.getWhere().and(changeExpr.getWhere().not())), getQueryEnv()));
    }

    private void packRemoveClasses(BusinessLogics BL) throws SQLException {
        if(news==null)
            return;

        // проводим "мини-паковку", то есть удаляем все записи, у которых ключем является удаляемый объект
        for(ImplementTable table : BL.LM.tableFactory.getImplementTables(remove)) {
            Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(table);
            Where removeWhere = Where.FALSE;
            for (Map.Entry<KeyField, ValueClass> field : table.mapFields.entrySet())
                if (remove.contains(field.getValue())) {
                    Join<String> newJoin = news.join(query.mapKeys.get(field.getKey()));
                    removeWhere = removeWhere.or(newJoin.getWhere().and(isValueClass(newJoin.getExpr("value"), (CustomClass) field.getValue(), usedNewClasses).not()));
                }
            query.and(table.join(query.mapKeys).getWhere().and(removeWhere));
            sql.deleteRecords(new ModifyQuery(table, query, getQueryEnv()));
        }
    }

    @Message("message.session.apply.write")
    private <P extends PropertyInterface> void readStored(@ParamMessage CalcProperty<P> property, BusinessLogics<?> BL) throws SQLException {
        assert isInTransaction();
        assert property.isStored();
        if(property.hasChanges(getModifier())) {
            SinglePropertyTableUsage<P> changeTable = property.readChangeTable(sql, getModifier(), baseClass, env);

            Pair<SinglePropertyTableUsage<P>,SinglePropertyTableUsage<P>> result = property.splitFitClasses(changeTable, sql, baseClass, env);

            applySingleStored(property, result.first, BL);
            apply.add(property, result.second);
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

    private void aspectChangeClass(Set<CustomClass> addClasses, Set<CustomClass> removeClasses, ClassChange change) throws SQLException {
        checkTransaction(); // важно что, вначале

        // оптимизация
        changeSingle(addClasses, change, singleAdd, add, singleRemove, remove);
        changeSingle(removeClasses, change, singleRemove, remove, singleAdd, add);

        if(news ==null)
            news = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
        change.modifyRows(news, sql, baseClass, Modify.MODIFY, env);
        newClasses.clear();

        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> dataChange : data.entrySet()) { // удаляем существующие изменения
            DataProperty property = dataChange.getKey();
            if(property.depends(removeClasses)) { // оптимизация
                SinglePropertyTableUsage<ClassPropertyInterface> table = dataChange.getValue();

                // кейс с удалением, похож на getEventChange и в saveClassChanges - "мини паковка"
                Where removeWhere = Where.FALSE;
                Map<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
                for(ClassPropertyInterface propertyInterface : property.interfaces)
                    if(removeClasses.contains(propertyInterface.interfaceClass)) {
                        Join<String> newJoin = change.join(mapKeys.get(propertyInterface));
                        removeWhere = removeWhere.or(newJoin.getWhere().and(isValueClass(newJoin.getExpr("value"), (CustomClass) propertyInterface.interfaceClass, usedNewClasses).not()));
                        dataModifier.eventDataChange(property);
                    }
                Join<String> join = table.join(mapKeys);
                removeWhere = removeWhere.and(join.getWhere());

                if(removeClasses.contains(property.value)) {
                    Join<String> newJoin = change.join(join.getExpr("value"));
                    removeWhere = removeWhere.or(newJoin.getWhere().and(isValueClass(newJoin.getExpr("value"), (CustomClass) property.value, usedNewClasses).not()));
                    dataModifier.eventDataChange(property);
                }
                table.modifyRows(sql, new Query<ClassPropertyInterface, String>(mapKeys, removeWhere), baseClass, Modify.DELETE, getQueryEnv());
            }
        }
    }

    private static void changeSingle(Set<CustomClass> thisChanged, ClassChange thisChange, Map<CustomClass, DataObject> single, Set<CustomClass> changed, Map<CustomClass, DataObject> singleBack, Set<CustomClass> changedBack) {
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

    private void aspectDropChanges(final SessionDataProperty property) throws SQLException {
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

    public void dropTables(Set<SessionDataProperty> keep) throws SQLException {
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
            inSessionEvent = false;
        }

        if(isInTransaction()) {
            if(neededProps!=null) {
                for(SinglePropertyTableUsage table : pendingSingleTables.values())
                    table.drop(sql);
                pendingSingleTables.clear();
                neededProps = null;
                assert !flush;
            }
                
            // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы
            apply.clear(sql);
            dataModifier.clearHints(sql); // drop'ем hint'ы (можно и без sql но пока не важно)
            rollbackTransaction();
            return;
        }

        restart(true, new HashSet<SessionDataProperty>());
    }

    private <P extends PropertyInterface> void updateApplyStart(OldProperty<P> property, SinglePropertyTableUsage<P> tableUsage) throws SQLException { // изврат конечно
        assert isInTransaction();

        SinglePropertyTableUsage<P> prevTable = apply.getTable(property);
        if(prevTable==null) {
            prevTable = property.createChangeTable();
            apply.add(property, prevTable);
        }
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        prevTable.modifyRows(sql, mapKeys, property.getExpr(mapKeys), tableUsage.join(mapKeys).getWhere(), baseClass, Modify.LEFT, env); // если он уже был в базе он не заместится
        apply.eventChange(property);
        tableUsage.drop(sql);
    }

    public Map<ImplementTable, Collection<CalcProperty>> groupPropertiesByTables() {
        return BaseUtils.group(
                new BaseUtils.Group<ImplementTable, CalcProperty>() {
                    public ImplementTable group(CalcProperty key) {
                        if (key.isStored())
                            return key.mapTable.table;
                        assert key instanceof OldProperty;
                        return null;
                    }
                }, apply.getProperties());
    }

    @Message("message.increment.read.properties")
    public <P extends PropertyInterface> SessionTableUsage<KeyField, CalcProperty> readSave(ImplementTable table, @ParamMessage Collection<CalcProperty> properties) throws SQLException {
        assert isInTransaction();

        // подготавливаем запрос
        Query<KeyField, CalcProperty> changesQuery = new Query<KeyField, CalcProperty>(table.keys);
        WhereBuilder changedWhere = new WhereBuilder();
        for (CalcProperty<P> property : properties)
            changesQuery.properties.put(property, property.getIncrementExpr(BaseUtils.join(property.mapTable.mapKeys, changesQuery.mapKeys), getModifier(), changedWhere));
        changesQuery.and(changedWhere.toWhere());

        // подготовили - теперь надо сохранить в курсор и записать классы
        SessionTableUsage<KeyField, CalcProperty> changeTable =
                new SessionTableUsage<KeyField, CalcProperty>(table.keys, new ArrayList<CalcProperty>(properties), Field.<KeyField>typeGetter(),
                        new Type.Getter<CalcProperty>() {
                            public Type getType(CalcProperty key) {
                                return key.getType();
                            }
                        });
        changeTable.writeRows(sql, changesQuery, baseClass, env);
        return changeTable;
    }

}
