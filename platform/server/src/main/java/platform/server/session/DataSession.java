package platform.server.session;

import platform.base.BaseUtils;
import platform.base.MutableObject;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.Settings;
import platform.server.caches.MapValues;
import platform.server.classes.*;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.type.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.ComputerController;
import platform.server.form.navigator.IsServerRestartingController;
import platform.server.form.navigator.UserController;
import platform.server.logics.*;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends MutableObject implements SessionChanges, ExprChanges {

    public final Modifier<SimpleChanges> modifier = new Modifier<SimpleChanges>() {
        public SimpleChanges newChanges() {
            return SimpleChanges.EMPTY;
        }

        public SimpleChanges newFullChanges() {
            return new SimpleChanges(this);
        }

        public ExprChanges getSession() {
            return DataSession.this;
        }

        public SimpleChanges preUsed(Property property) {
            return null;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof SimpleChanges;
        }
    };
    public final SQLSession sql;
    public final SQLSession idSession;

    public void close() throws SQLException {
    }

    public static class UpdateChanges {

        public final Set<Property> properties;
        public final Set<CustomClass> addClasses;
        public final Set<CustomClass> removeClasses;

        public UpdateChanges() {
            properties = new HashSet<Property>();
            addClasses = new HashSet<CustomClass>();
            removeClasses = new HashSet<CustomClass>();
        }

        public UpdateChanges(DataSession session, FormInstance<?> form) {
            assert form.session == session;
            addClasses = new HashSet<CustomClass>(session.add.keySet());
            removeClasses = new HashSet<CustomClass>(session.remove.keySet());
            properties = new HashSet<Property>(form.getUpdateProperties(form));
        }

        public void add(UpdateChanges changes) {
            properties.addAll(changes.properties);
            addClasses.addAll(changes.addClasses);
            removeClasses.addAll(changes.removeClasses);
        }
    }

    // формы, для которых с момента последнего update уже был restart, соотвественно в значениях - изменения от посл. update (prev) до посл. apply
    public IdentityHashMap<FormInstance, UpdateChanges> appliedChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    // формы для которых с момента последнего update не было restart, соответственно в значениях - изменения от посл. update (prev) до посл. изменения
    public IdentityHashMap<FormInstance, UpdateChanges> incrementChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    // assert что те же формы что и в increment, соответственно в значениях - изменения от посл. apply до посл. update (prev)
    public IdentityHashMap<FormInstance, UpdateChanges> updateChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

    public final BaseClass baseClass;
    public final CustomClass namedObject;
    public final LP<?> name;
    public final AbstractGroup recognizeGroup;
    public final CustomClass transaction;
    public final LP<?> date;
    public final ConcreteCustomClass sessionClass;
    public final LP<?> currentDate;

    // для отладки
    public static boolean reCalculateAggr = false;

    private final List<DerivedChange<?,?>> notDeterministic;

    private final IsServerRestartingController isServerRestarting;
    public final UserController user;
    public final ComputerController computer;

    public DataObject applyObject = null;

    public DataSession(SQLSession sql, final UserController user, final ComputerController computer, IsServerRestartingController isServerRestarting, BaseClass baseClass, CustomClass namedObject, ConcreteCustomClass sessionClass, LP<?> name, AbstractGroup recognizeGroup, CustomClass transaction, LP<?> date, LP<?> currentDate, List<DerivedChange<?,?>> notDeterministic, SQLSession idSession) throws SQLException {
        this.sql = sql;
        this.isServerRestarting = isServerRestarting;

        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.recognizeGroup = recognizeGroup;
        this.transaction = transaction;
        this.date = date;
        this.notDeterministic = notDeterministic;
        this.sessionClass = sessionClass;
        this.currentDate = currentDate;

        this.user = user;
        this.computer = computer;

        this.idSession = idSession;
    }

    public DataSession createSession() throws SQLException {
        return new DataSession(sql, user, computer, isServerRestarting, baseClass, namedObject, sessionClass, name, recognizeGroup, transaction, date, currentDate, notDeterministic, idSession);
    }

    public void restart(boolean cancel) throws SQLException {

        // apply
        //      по кому был restart : добавляем changes -> applied
        //      по кому не было restart : to -> applied (помечая что был restart)

        // cancel
        //    по кому не было restart :  from -> в applied (помечая что был restart)

        if(!cancel)
            for(Map.Entry<FormInstance,UpdateChanges> appliedChange : appliedChanges.entrySet())
                appliedChange.getValue().add(new UpdateChanges(this, (FormInstance<?>) appliedChange.getKey()));

        assert Collections.disjoint(appliedChanges.keySet(),(cancel?updateChanges:incrementChanges).keySet());
        appliedChanges.putAll(cancel?updateChanges:incrementChanges);
        incrementChanges = new IdentityHashMap<FormInstance, UpdateChanges>();
        updateChanges = new IdentityHashMap<FormInstance, UpdateChanges>();

        newClasses = new HashMap<DataObject, ConcreteObjectClass>();

        dropTables(sql);

        applyObject = null; // сбрасываем в том числе когда cancel потому как cancel drop'ает в том числе и добавление объекта
    }

    public DataObject addObject(ConcreteCustomClass customClass, Modifier<? extends Changes> modifier) throws SQLException {
        return addObject(customClass, modifier, true, true);
    }

    public DataObject addObject() throws SQLException {
        return new DataObject(IDTable.instance.generateID(idSession, IDTable.OBJECT),baseClass.unknown);
    }

    public DataObject addObject(ConcreteCustomClass customClass, Modifier<? extends Changes> modifier, boolean fillDefault, boolean groupLast) throws SQLException {

        DataObject object = addObject();

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass, groupLast);

        if(fillDefault) {
            if(customClass.isChild(namedObject))
                name.execute(customClass.caption+" "+object.object, this, modifier, object);

            if(customClass.isChild(transaction))
                date.execute(currentDate.read(sql, modifier, env), DataSession.this, modifier, object);
        }

        return object;
    }

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        changeClass(change, toClass, true);
    }

    public void changeClass(DataObject change, ConcreteObjectClass toClass, boolean groupLast) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        assert Collections.disjoint(addClasses,removeClasses);

        changeClass(addClasses, removeClasses, toClass, change, sql, groupLast);

        newClasses.put(change,toClass);

        if(groupLast) {
            // по тем по кому не было restart'а new -> to
            updateProperties(new SimpleChanges(getUsedChanges(), addClasses, removeClasses, true));
            for(UpdateChanges incrementChange : incrementChanges.values()) {
                incrementChange.addClasses.addAll(addClasses);
                incrementChange.removeClasses.addAll(removeClasses);
            }
        }
    }

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, boolean groupLast) throws SQLException {
        changeProperty(property, keys, newValue, sql, groupLast);

        if(groupLast) // по тем по кому не было restart'а new -> to
            updateProperties(new SimpleChanges(getUsedChanges(), property));
    }

    private void updateProperties(ExprChanges changes) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(changes));
    }

    public void updateProperties(SimpleChanges changes) {
        updateProperties(new UpdateExprChanges(changes));
    }

    public void updateProperties(Modifier<? extends Changes> modifier) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(modifier));
    }

    public <P extends PropertyInterface> List<ClientAction> execute(Property<P> property, PropertyChange<P> change, Modifier<? extends Changes> modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        if(executeForm!=null) // assert что сама форма modifier и session ее же
            executeForm.form.fireChange(property, change);
        return execute(property.getDataChanges(change, null, modifier), executeForm, mapObjects);
    }


    public <P extends PropertyInterface> List<ClientAction> execute(MapDataChanges<P> mapChanges, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {

        DataChanges dataChanges = mapChanges.changes;

        // если идет изменение и есть недетерменированное производное изменение зависищее от него, то придется его "выполнить"
        for(DerivedChange<?,?> derivedChange : notDeterministic) {
            DataChanges derivedChanges = derivedChange.getDataChanges(new DataChangesModifier(modifier, dataChanges));
            if(!derivedChanges.isEmpty())
                mapChanges = mapChanges.add(new MapDataChanges<P>(derivedChanges));
        }

        // сначала читаем изменения, чтобы не было каскадных непредсказуемых эффектов
        Map<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> propRows = new HashMap<UserProperty, Map<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>>();
        for(int i=0;i<dataChanges.size;i++)
            propRows.put(dataChanges.getKey(i), dataChanges.getValue(i).getQuery().executeClasses(sql, env, baseClass));

        // потом изменяем
        List<ClientAction> actions = new ArrayList<ClientAction>();
        for(Map.Entry<UserProperty,Map<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>>> propRow : propRows.entrySet()) {
            for (Iterator<Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> iterator = propRow.getValue().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>> row = iterator.next();
                UserProperty property = propRow.getKey();
                Map<ClassPropertyInterface, P> mapInterfaces = mapChanges.map.get(property);
                property.execute(new ExecutionContext(row.getKey(), row.getValue().get("value"), this, (executeForm == null ? modifier : executeForm.form), actions, executeForm, mapInterfaces == null ? null : BaseUtils.nullJoin(mapInterfaces, mapObjects), !iterator.hasNext()));
            }
        }
        return actions;
    }

    public ConcreteClass getCurrentClass(DataObject value) {
        ConcreteClass newClass;
        if((newClass = newClasses.get(value))==null)
            return value.objectClass;
        else
            return newClass;
    }

    public ObjectValue getCurrentValue(ObjectValue value) {
        if(value instanceof NullValue)
            return value;
        else {
            DataObject dataObject = (DataObject)value;
            return new DataObject(dataObject.object, getCurrentClass(dataObject));
        }
    }

    public <K, V extends ObjectValue> Map<K, V> getCurrentObjects(Map<K, V> map) {
        Map<K, V> result = new HashMap<K, V>();
        for(Map.Entry<K, V> entry : map.entrySet())
            result.put(entry.getKey(), (V) getCurrentValue(entry.getValue()));
        return result;
    }

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        return new DataObject(value,type.getDataClass(value, sql, baseClass));
    }

    public ObjectValue getObjectValue(Object value, Type type) throws SQLException {
        if(value==null)
            return NullValue.instance;
        else
            return getDataObject(value, type);
    }

    // узнает список изменений произошедших без него
    public Collection<Property> update(FormInstance<?> form, Collection<CustomClass> updateClasses) throws SQLException {
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
            UpdateChanges formChanges = new UpdateChanges(this, form);
            // from = changes (сбрасываем пометку что не было restart'а)
            updateChanges.put(form, formChanges);
            // возвращаем applied + changes
            incrementChange.add(formChanges);
        }
        incrementChanges.put(form,new UpdateChanges());

        updateClasses.addAll(incrementChange.addClasses);
        updateClasses.addAll(incrementChange.removeClasses);
        return incrementChange.properties;
    }

    public String apply(BusinessLogics<?> BL) throws SQLException {
        return apply(BL, new ArrayList<ClientAction>());
    }

    public String apply(BusinessLogics<?> BL, List<ClientAction> actions) throws SQLException {
        return apply(BL, actions, false);
    }

    public String check(BusinessLogics BL) throws SQLException {
        return apply(BL, new ArrayList<ClientAction>(), true);
    }

    private <T extends PropertyInterface> boolean fitClasses(Property<T> property, SessionTableUsage<KeyField, Property> change) {
        return change.<Field>getClassWhere(property, property.field).means(property.getClassWhere(property.field));
    }

    private static boolean needPrevious(Property<?> property, BusinessLogics<?> BL) {
        return BL.getConstraintDerivedDependProperties().contains(property);
    }
    private <T extends PropertyInterface> void applySingleStored(Property<T> property, SessionTableUsage<KeyField, Property> change, BusinessLogics<?> BL, IncrementApply incrementApply) throws SQLException {
        // assert что у change классы совпадают с property
        assert property.isStored();
        assert fitClasses(property, change); // проверяет гипотезу

        if(change.isEmpty())
            return;

        if(needPrevious(property, BL)) // запоминаем предыдущее значение
            incrementApply.readApplyStart(property, change);

        IncrementProps<KeyField> increment = new IncrementProps<KeyField>() {
            public ExprChanges getSession() {
                return ExprChanges.EMPTY;
            }
            public SQLSession getSql() {
                return sql;
            }
            public QueryEnvironment getEnv() {
                return env;
            }
        };
        increment.add(property, change);

        // true нужно предыдущее значение сохранить
        for(Property<?> depend : BL.getAppliedDependFrom(property)) { // !!! важно в лексикографическом порядке должно быть
            if(depend.isStored()) { // читаем новое значение, запускаем рекурсию
                applySingleStored(depend, increment.readTable(depend, baseClass), BL, incrementApply);
                increment.noUpdate.add(depend); // докидываем noUpdate чтобы по нескольку раз одну ветку не отрабатывать
            } else {
                assert needPrevious(depend, BL);
                incrementApply.readApplyStart(depend, increment);
            }
        }
        savePropertyChanges(property.mapTable.table, Collections.singleton((Property) property), change);
    }

    private void savePropertyChanges(Table implementTable, Collection<Property> props, SessionTableUsage<KeyField, Property> changeTable) throws SQLException {
        Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(implementTable);
        Join<Property> join = changeTable.join(modifyQuery.mapKeys);
        for (Property property : props)
            modifyQuery.properties.put(property.field, join.getExpr(property));
        modifyQuery.and(join.getWhere());
        sql.modifyRecords(new ModifyQuery(implementTable, modifyQuery, env));
    }

    public String apply(final BusinessLogics<?> BL, List<ClientAction> actions, boolean onlyCheck) throws SQLException {
        resolveFollows(BL, false);

        // до чтения persistent свойств в сессию
        if (applyObject == null) {
            applyObject = addObject(sessionClass, modifier);
        }

        sql.startTransaction();

        Map<ExecuteProperty, List<ExecutionContext>> pendingExecutes = new HashMap<ExecuteProperty, List<ExecutionContext>>();
        IncrementApply incrementApply = new IncrementApply(this);

        // тоже нужен посередине, чтобы он успел dataproperty изменить до того как они обработаны
        for (Property<?> property : BL.getAppliedProperties(onlyCheck)) {
            if(property.isFalse) { // ограничения
                String constraintResult = check(property, incrementApply);
                if (constraintResult != null) {
                    // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы
                    incrementApply.cleanIncrementTables();
                    sql.rollbackTransaction();
                    return constraintResult;
                }
            }
            if(property.isExecuteDerived()) // действия
                execute((ExecuteProperty) property, incrementApply, actions, pendingExecutes);
            if(property.isStored()) // постоянно-хранимые свойства
                readStored(property, incrementApply, BL);
        }

        // записываем в базу, то что туда еще не сохранено, приходится сохранять группами, так могут не подходить по классам
        for (Map.Entry<ImplementTable, Collection<Property>> groupTable : incrementApply.groupPropertiesByTables().entrySet())
            savePropertyChanges(groupTable.getKey(), groupTable.getValue(), incrementApply.read(groupTable.getKey(), groupTable.getValue(), baseClass));

        for (Map.Entry<DataObject, ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(sql, newClass.getKey());

        incrementApply.cleanIncrementTables();
        sql.commitTransaction();
        restart(false);

        for(Map.Entry<ExecuteProperty, List<ExecutionContext>> pendingExecute : pendingExecutes.entrySet())
            for(ExecutionContext context : pendingExecute.getValue())
                executePending(pendingExecute.getKey(), context);

        return null;

    }

    @Message("message.session.apply.write")
    private void readStored(@ParamMessage Property<?> property, IncrementApply incrementApply, BusinessLogics<?> BL) throws SQLException {
        if(property.hasChanges(incrementApply)) {
            SessionTableUsage<KeyField, Property> readTable = incrementApply.read(property.mapTable.table, Collections.<Property>singleton(property), baseClass);

            if(Settings.instance.isEnableApplySingleStored() && fitClasses(property, readTable)) {
                applySingleStored(property, readTable, BL, incrementApply);
                incrementApply.remove(property);
                incrementApply.noUpdate.add(property);
            }
        }
    }

    @Message("message.session.apply.check")
    public <T extends PropertyInterface> String check(@ParamMessage Property<T> property, IncrementApply incrementApply) throws SQLException {
        if(property.hasChanges(incrementApply) || property.hasChanges(incrementApply.getApplyStart())) {
            Query<T,String> changed = new Query<T,String>(property);

            WhereBuilder changedWhere = new WhereBuilder();
            Expr newExpr = property.getExpr(changed.mapKeys, incrementApply, changedWhere);
            Expr prevExpr = property.getExpr(changed.mapKeys, incrementApply.getApplyStart(), changedWhere);
            changed.and(newExpr.getWhere().and(prevExpr.getWhere().not()).and(changedWhere.toWhere())); // только на измененные смотрим

            // сюда надо name'ы вставить
            List<List<String>> propCaptions = new ArrayList<List<String>>();
            for (int i = 0; i < property.interfaces.size(); i++) {
                propCaptions.add(new ArrayList<String>());
            }

            Property.CommonClasses<?> classes = property.getCommonClasses();
            int interfaceIndex = 0;
            for(T propertyInterface : property.interfaces) {
                ValueClass valueClass = classes.interfaces.get(propertyInterface);
                for (Property nameProp : recognizeGroup.getProperties()) {
                    List<ValueClassWrapper> wrapper = Arrays.asList(new ValueClassWrapper(valueClass));
                    if (!nameProp.getProperties(Arrays.asList(wrapper), true).isEmpty()) {
                        Expr nameExpr = nameProp.getExpr(Collections.singletonMap(BaseUtils.single(nameProp.interfaces), changed.mapKeys.get(propertyInterface)), incrementApply);
                        changed.properties.put("int" + propertyInterface.ID + "_" + propCaptions.get(interfaceIndex).size(), nameExpr);
                        propCaptions.get(interfaceIndex).add(nameProp.caption);
                    }
                }
                interfaceIndex++;
            }

            OrderedMap<Map<T, Object>, Map<String, Object>> result = changed.execute(sql, new OrderedMap<String, Boolean>(), 30, env);
            if (result.size() > 0) {
                String resultString = property.toString() + '\n';
                for (Map.Entry<Map<T,Object>, Map<String,Object>> row : result.entrySet()) {
                    String infoStr = "";
                    int ifaceIndex = 0;
                    for (T propertyInterface : property.interfaces) {
                        if (ifaceIndex > 0) {
                            infoStr += ", ";
                        }
                        infoStr += "[id=" + row.getKey().get(propertyInterface).toString().trim();
                        for (int i = 0; i < propCaptions.get(ifaceIndex).size(); i++) {
                            Object value = row.getValue().get("int" + propertyInterface.ID + "_" + i);
                            if (value != null) {
                                infoStr += ", " + propCaptions.get(ifaceIndex).get(i) + '=';
                                infoStr += value.toString().trim();
                            }
                        }
                        infoStr += "]";
                        ifaceIndex++;
                    }
                    resultString += "    " + infoStr + '\n';
                }

                return resultString;
            }
        }

        return null;
    }

    @Message("message.session.apply.auto.execute")
    public void execute(@ParamMessage ExecuteProperty property, IncrementApply incrementApply, List<ClientAction> actions, Map<ExecuteProperty, List<ExecutionContext>> pendingExecute) throws SQLException {
        if(property.derivedChange.hasDerivedChange(incrementApply)) {
            PropertyChange<ClassPropertyInterface> propertyChange = property.derivedChange.getDataChanges(incrementApply).get(property);
            if(propertyChange!=null) {
                List<ExecutionContext> pendingPropExecute = null;
                if(property.pendingDerivedExecute()) {
                    pendingPropExecute = new ArrayList<ExecutionContext>();
                    pendingExecute.put(property, pendingPropExecute);
                }

                for (Iterator<Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>>> iterator = propertyChange.getQuery().executeClasses(sql, env, baseClass).entrySet().iterator(); iterator.hasNext();) {
                    Map.Entry<Map<ClassPropertyInterface, DataObject>, Map<String, ObjectValue>> executeRow = iterator.next();
                    Map<ClassPropertyInterface, DataObject> executeKeys = executeRow.getKey();
                    ObjectValue executeValue = executeRow.getValue().get("value");

                    if(pendingPropExecute!=null)
                        // иначе "pend'им" выполнение
                        pendingPropExecute.add(new ExecutionContext(getCurrentObjects(executeKeys), getCurrentValue(executeValue), this, modifier, actions, null, null, !iterator.hasNext()));
                    else
                        property.execute(new ExecutionContext(executeKeys, executeValue, this, incrementApply, actions, null, null, !iterator.hasNext()));
                }
            }
        }
    }

    @Message("message.session.apply.auto.execute")
    public void executePending(@ParamMessage ExecuteProperty property, ExecutionContext context) throws SQLException {
        property.execute(context);
    }

    @Message("message.session.apply.resolve.follows")
    public void resolveFollows(final BusinessLogics<?> BL, boolean recalculate) throws SQLException {

        for(Property<?> property : BL.getFollowProperties()) {
            for(PropertyFollows<?, ?> follow : property.follows)
                follow.resolveFalse(this, BL, recalculate);
            for(PropertyFollows<?, ?> follow : property.followed)
                follow.resolveTrue(this, BL, recalculate);
        }
    }

    public final QueryEnvironment env = new QueryEnvironment() {
        public ParseInterface getSQLUser() {
            return new TypeObject(user.getCurrentUser().object, ObjectType.instance);
        }

        public ParseInterface getID() {
            return applyObject ==null?new TypeObject(0, ObjectType.instance):new TypeObject(applyObject);
        }

        public ParseInterface getSQLComputer() {
            return new TypeObject(computer.getCurrentComputer().object, ObjectType.instance);
        }

        public ParseInterface getIsServerRestarting() {
            return new StringParseInterface() {
                public String getString(SQLSyntax syntax) {
                    return isServerRestarting.isServerRestarting()
                           ? LogicalClass.instance.getString(true, syntax)
                           : SQLSyntax.NULL;
                }
            };
        }
    };

    public Map<CustomClass, SingleKeyNoPropertyUsage> add = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    public Map<CustomClass, SingleKeyNoPropertyUsage> remove = new HashMap<CustomClass, SingleKeyNoPropertyUsage>();
    public Map<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> data = new HashMap<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>>();

    public SingleKeyPropertyUsage news = null;

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty() || news!=null;
    }

    public boolean hasStoredChanges() {
        if (!add.isEmpty() || !remove.isEmpty() || news != null)
            return true;

        for (DataProperty property : data.keySet())
            if (property.isStored())
                return true;

        return false;
    }

    public void changeClass(Set<CustomClass> addClasses, Set<CustomClass> removeClasses, ConcreteObjectClass toClass, DataObject change, SQLSession session, boolean groupLast) throws SQLException {
        for(CustomClass addClass : addClasses) {
            SingleKeyNoPropertyUsage addTable = add.get(addClass);
            if(addTable==null) { // если нету таблицы создаем
                addTable = new SingleKeyNoPropertyUsage(ObjectType.instance);
                add.put(addClass, addTable);
            }
            addTable.insertRecord(session, change, false, groupLast);

            SingleKeyNoPropertyUsage removeTable = remove.get(addClass);
            if(removeTable!=null)
                removeTable.deleteRecords(session,change);
        }
        for(CustomClass removeClass : removeClasses) {
            SingleKeyNoPropertyUsage removeTable = remove.get(removeClass);
            if(removeTable==null) { // если нету таблицы создаем
                removeTable = new SingleKeyNoPropertyUsage(ObjectType.instance);
                remove.put(removeClass, removeTable);
            }
            removeTable.insertRecord(session, change, false, groupLast);

            SingleKeyNoPropertyUsage addTable = add.get(removeClass);
            if(addTable!=null)
                addTable.deleteRecords(session,change);
        }

        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> dataChange : data.entrySet()) { // удаляем существующие изменения
            DataProperty property = dataChange.getKey();
            for(ClassPropertyInterface propertyInterface : property.interfaces)
                if(propertyInterface.interfaceClass instanceof CustomClass && removeClasses.contains((CustomClass)propertyInterface.interfaceClass))
                    dataChange.getValue().deleteKey(session, propertyInterface, change);
            if(property.value instanceof CustomClass && removeClasses.contains((CustomClass) property.value))
                dataChange.getValue().deleteProperty(session, "value", change);
        }

        if(news ==null)
            news = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
        news.insertRecord(session, change, toClass.getClassObject(), true, groupLast);
    }

    public void changeProperty(final DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue, SQLSession session, boolean groupLast) throws SQLException {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange == null) { // создадим таблицу, если не было
            dataChange = property.createChangeTable();
            data.put(property, dataChange);
        }
        dataChange.insertRecord(session, keys, newValue, true, groupLast);
    }

    public void dropTables(SQLSession session) throws SQLException {
        for(SingleKeyNoPropertyUsage addTable : add.values())
            addTable.drop(session);
        for(SingleKeyNoPropertyUsage removeTable : remove.values())
            removeTable.drop(session);
        for(SinglePropertyTableUsage<ClassPropertyInterface> dataTable : data.values())
            dataTable.drop(session);
        if(news !=null)
            news.drop(session);

        add.clear();
        remove.clear();
        data.clear();
        news = null;
    }

    public Where getIsClassWhere(Expr expr, ValueClass isClass, WhereBuilder changedWheres) {
        Where isClassWhere = expr.isClass(isClass.getUpSet());

        if(isClass instanceof CustomClass) {
            SingleKeyNoPropertyUsage removeTable = remove.get((CustomClass) isClass);
            if(removeTable!=null) {
                Where removeWhere = removeTable.getWhere(expr);
                isClassWhere = isClassWhere.and(removeWhere.not());
                if(changedWheres!=null) changedWheres.add(removeWhere);
            }

            SingleKeyNoPropertyUsage addTable = add.get((CustomClass)isClass);
            if(addTable!=null) {
                Where addWhere = addTable.getWhere(expr);
                isClassWhere = isClassWhere.or(addWhere);
                if(changedWheres!=null) changedWheres.add(addWhere);
            }
        }
        return isClassWhere;
    }

    public Expr getIsClassExpr(Expr expr, BaseClass baseClass, WhereBuilder changedWheres) {
        Expr isClassExpr = expr.classExpr(baseClass);

        if(news !=null) {
            Join<String> newJoin = news.join(expr);
            Where newWhere = newJoin.getWhere();
            isClassExpr = newJoin.getExpr("value").ifElse(newWhere, isClassExpr);
            if(changedWheres!=null) changedWheres.add(newWhere);
        }

        return isClassExpr;
    }

    public Join<String> getDataChange(DataProperty property, Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        SinglePropertyTableUsage<ClassPropertyInterface> dataChange = data.get(property);
        if(dataChange!=null)
            return dataChange.join(joinImplement);
        else
            return null;
    }

    public Where getRemoveWhere(ValueClass valueClass, Expr expr) {
        SingleKeyNoPropertyUsage removeTable;
        if(valueClass instanceof CustomClass && ((removeTable = remove.get((CustomClass)valueClass))!=null))
            return removeTable.getWhere(expr);
        else
            return Where.FALSE;
    }

    // IMMUTABLE
    public SimpleChanges getUsedChanges() {
        Map<CustomClass, MapValues> usedAdd = new HashMap<CustomClass, MapValues>();
        for(Map.Entry<CustomClass, SingleKeyNoPropertyUsage> change : add.entrySet())
            usedAdd.put(change.getKey(), change.getValue().getUsage());
        Map<CustomClass, MapValues> usedRemove = new HashMap<CustomClass, MapValues>();
        for(Map.Entry<CustomClass, SingleKeyNoPropertyUsage> change : remove.entrySet())
            usedRemove.put(change.getKey(), change.getValue().getUsage());
        Map<DataProperty, MapValues> usedData = new HashMap<DataProperty, MapValues>();
        for(Map.Entry<DataProperty, SinglePropertyTableUsage<ClassPropertyInterface>> change : data.entrySet())
            usedData.put(change.getKey(), change.getValue().getUsage());
        return new SimpleChanges(usedAdd, usedRemove, usedData, news != null ? news.getUsage() : null);
    }
}
