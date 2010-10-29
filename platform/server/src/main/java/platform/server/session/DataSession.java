package platform.server.session;

import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.*;
import platform.server.data.KeyField;
import platform.server.data.ModifyQuery;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.type.TypeObject;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.navigator.UserController;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.table.IDTable;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

public class DataSession extends SQLSession implements ChangesSession {

    public static class SimpleChanges extends Changes<SimpleChanges> {

        private SimpleChanges() {
        }
        public static final SimpleChanges EMPTY = new SimpleChanges();

        private SimpleChanges(SimpleChanges changes, SessionChanges merge) {
            super(changes, merge);
        }
        public SimpleChanges addChanges(SessionChanges changes) {
            return new SimpleChanges(this, changes);
        }

        public SimpleChanges(Modifier<SimpleChanges> modifier) {
            super(modifier);
        }

        private SimpleChanges(SimpleChanges changes, SimpleChanges merge) {
            super(changes, merge);
        }
        public SimpleChanges add(SimpleChanges changes) {
            return new SimpleChanges(this, changes);
        }

        private SimpleChanges(Changes<SimpleChanges> changes, MapValuesTranslate mapValues) {
            super(changes, mapValues);
        }
        public SimpleChanges translate(MapValuesTranslate mapValues) {
            return new SimpleChanges(this, mapValues);
        }
    }

    public final Modifier<SimpleChanges> modifier = new Modifier<SimpleChanges>() {
        public SimpleChanges newChanges() {
            return SimpleChanges.EMPTY;
        }

        public SimpleChanges fullChanges() {
            return new SimpleChanges(this);
        }

        public SessionChanges getSession() {
            return changes;
        }

        public SimpleChanges used(Property property, SimpleChanges usedChanges) {
            return usedChanges;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof SimpleChanges;
        }
    };

    public static class UpdateChanges {

        public final Set<Property> properties;
        public final Set<CustomClass> addClasses;
        public final Set<CustomClass> removeClasses;

        public UpdateChanges() {
            properties = new HashSet<Property>();
            addClasses = new HashSet<CustomClass>();
            removeClasses = new HashSet<CustomClass>();
        }

        public UpdateChanges(SessionChanges changes, FormInstance<?> form) {
            addClasses = new HashSet<CustomClass>(changes.add.keySet());
            removeClasses = new HashSet<CustomClass>(changes.remove.keySet());
            properties = new HashSet<Property>(form.getUpdateProperties(changes));
        }

        public void add(UpdateChanges changes) {
            properties.addAll(changes.properties);
            addClasses.addAll(changes.addClasses);
            removeClasses.addAll(changes.removeClasses);
        }
    }

    // формы, для которых с момента последнего update уже был restart, соотвественно в значениях - изменения от посл. update (prev) до посл. apply
    public Map<FormInstance, UpdateChanges> appliedChanges = new HashMap<FormInstance, UpdateChanges>();

    // формы для которых с момента последнего update не было restart, соответственно в значениях - изменения от посл. update (prev) до посл. изменения
    public Map<FormInstance, UpdateChanges> incrementChanges = new HashMap<FormInstance, UpdateChanges>();

    // assert что те же формы что и в increment, соответственно в значениях - изменения от посл. apply до посл. update (prev)
    public Map<FormInstance, UpdateChanges> updateChanges = new HashMap<FormInstance, UpdateChanges>();

    public SessionChanges changes = SessionChanges.EMPTY;

    public final BaseClass baseClass;
    public final CustomClass namedObject;
    public final LP<?> name;
    public final CustomClass transaction;
    public final LP<?> date;
    public final ConcreteCustomClass sessionClass;

    // для отладки
    public static boolean reCalculateAggr = false;

    private final List<DerivedChange<?,?>> notDeterministic;

    public final UserController user;

    public DataObject applyObject = null;

    public DataSession(DataAdapter adapter, final UserController user, BaseClass baseClass, CustomClass namedObject, ConcreteCustomClass sessionClass, LP<?> name, CustomClass transaction, LP<?> date, List<DerivedChange<?,?>> notDeterministic) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        super(adapter);

        this.user = user;
        this.baseClass = baseClass;
        this.namedObject = namedObject;
        this.name = name;
        this.transaction = transaction;
        this.date = date;
        this.notDeterministic = notDeterministic;
        this.sessionClass = sessionClass;
    }

    public TypeObject getSQLUser() {
        return new TypeObject(user.getCurrentUser().object, ObjectType.instance);
    }

    protected TypeObject getID() {
        return applyObject ==null?new TypeObject(0, ObjectType.instance):new TypeObject(applyObject);
    }

    public void restart(boolean cancel) throws SQLException {

        // apply
        //      по кому был restart : добавляем changes -> applied
        //      по кому не было restart : to -> applied (помечая что был restart)

        // cancel
        //    по кому не было restart :  from -> в applied (помечая что был restart)

        if(!cancel)
            for(Map.Entry<FormInstance,UpdateChanges> appliedChange : appliedChanges.entrySet())
                appliedChange.getValue().add(new UpdateChanges(changes, (FormInstance<?>) appliedChange.getKey()));

        assert Collections.disjoint(appliedChanges.keySet(),(cancel?updateChanges:incrementChanges).keySet());
        appliedChanges.putAll(cancel?updateChanges:incrementChanges);
        incrementChanges = new HashMap<FormInstance, UpdateChanges>();
        updateChanges = new HashMap<FormInstance, UpdateChanges>();

        newClasses = new HashMap<DataObject, ConcreteObjectClass>();

        changes.dropTables(this);
        changes = SessionChanges.EMPTY;

        applyObject = null; // сбрасываем в том числе когда cancel потому как cancel drop'ает в том числе и добавление объекта
    }

    public DataObject addObject(ConcreteCustomClass customClass, Modifier<? extends Changes> modifier) throws SQLException {

        DataObject object = new DataObject(IDTable.instance.generateID(this, IDTable.OBJECT),baseClass.unknown);

        // запишем объекты, которые надо будет сохранять
        changeClass(object, customClass);

        if(customClass.isChild(namedObject))
            name.execute(customClass.caption+" "+object.object, this, modifier, object);

        if(customClass.isChild(transaction))
            date.execute(DateConverter.dateToSql(new Date()), this, modifier, object);

        return object;
    }

    Map<DataObject, ConcreteObjectClass> newClasses = new HashMap<DataObject, ConcreteObjectClass>();

    public void changeClass(DataObject change, ConcreteObjectClass toClass) throws SQLException {
        if(toClass==null) toClass = baseClass.unknown;

        Set<CustomClass> addClasses = new HashSet<CustomClass>();
        Set<CustomClass> removeClasses = new HashSet<CustomClass>();
        ConcreteObjectClass prevClass = (ConcreteObjectClass) getCurrentClass(change);
        toClass.getDiffSet(prevClass,addClasses,removeClasses);

        assert Collections.disjoint(addClasses,removeClasses);

        changes = new SessionChanges(changes, addClasses, removeClasses, toClass, change, this);

        newClasses.put(change,toClass);

        // по тем по кому не было restart'а new -> to
        updateProperties(changes.getSessionChanges(addClasses, removeClasses));
        for(UpdateChanges incrementChange : incrementChanges.values()) {
            incrementChange.addClasses.addAll(addClasses);
            incrementChange.removeClasses.addAll(removeClasses);
        }
    }

    public void changeProperty(DataProperty property, Map<ClassPropertyInterface, DataObject> keys, ObjectValue newValue) throws SQLException {
        changes = new SessionChanges(changes, property, keys, newValue, this);

        // по тем по кому не было restart'а new -> to
        updateProperties(changes.getSessionChanges(property));
    }

    private void updateProperties(SessionChanges changes) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(changes));
    }

    public void updateProperties(Modifier<? extends Changes> modifier) {
        for(Map.Entry<FormInstance,UpdateChanges> incrementChange : incrementChanges.entrySet())
            incrementChange.getValue().properties.addAll(((FormInstance<?>) incrementChange.getKey()).getUpdateProperties(modifier));
    }

    public <P extends PropertyInterface> List<ClientAction> execute(Property<P> property, PropertyChange<P> change, Modifier<? extends Changes> modifier, RemoteForm executeForm, Map<P, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
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
            propRows.put(dataChanges.getKey(i), dataChanges.getValue(i).getQuery("value").executeClasses(this, baseClass));

        // потом изменяем
        List<ClientAction> actions = new ArrayList<ClientAction>();
        for(Map.Entry<UserProperty,Map<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>>> propRow : propRows.entrySet()) 
            for(Map.Entry<Map<ClassPropertyInterface,DataObject>,Map<String,ObjectValue>> row : propRow.getValue().entrySet()) {
                UserProperty property = propRow.getKey();
                Map<ClassPropertyInterface, P> mapInterfaces = mapChanges.map.get(property);
                property.execute(row.getKey(), row.getValue().get("value"), this, actions, executeForm, mapInterfaces==null?null:BaseUtils.nullJoin(mapInterfaces, mapObjects));
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

    public DataObject getDataObject(Object value, Type type) throws SQLException {
        return new DataObject(value,type.getDataClass(value, this, baseClass));
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
            UpdateChanges formChanges = new UpdateChanges(changes, form);
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

    private static class Increment extends Modifier<Increment.UsedChanges> {

        Map<Property, IncrementChangeTable> tables = new HashMap<Property, IncrementChangeTable>(); 

        public final DataSession session;

        public SessionChanges getSession() {
            return session.changes;
        }

        private Increment(DataSession session) {
            this.session = session;
        }

        static class UsedChanges extends Changes<UsedChanges> {
            final Map<Property, IncrementChangeTable> increment;

            private UsedChanges() {
                 increment = new HashMap<Property, IncrementChangeTable>();
            }
            private final static UsedChanges EMPTY = new UsedChanges();

            public UsedChanges(Increment modifier) {
                 super(modifier);
                 increment = new HashMap<Property, IncrementChangeTable>(modifier.tables);
            }

            @Override
            public boolean modifyUsed() {
                return !increment.isEmpty();
            }

            @Override
            public boolean hasChanges() {
                return super.hasChanges() || modifyUsed();
            }

            private UsedChanges(UsedChanges changes, SessionChanges merge) {
                super(changes, merge);
                increment = changes.increment;
            }
            public UsedChanges addChanges(SessionChanges changes) {
                return new UsedChanges(this, changes);
            }

            private UsedChanges(UsedChanges changes, UsedChanges merge) {
                super(changes, merge);
                increment = BaseUtils.merge(changes.increment, merge.increment);
            }
            public UsedChanges add(UsedChanges changes) {
                return new UsedChanges(this, changes);
            }

            @Override
            protected boolean modifyEquals(UsedChanges changes) {
                return increment.equals(changes.increment);
            }

            @Override
            @IdentityLazy
            public int hashValues(HashValues hashValues) {
                return super.hashValues(hashValues) * 31 + MapValuesIterable.hash(increment,hashValues);
            }

            @Override
            @IdentityLazy
            public Set<ValueExpr> getValues() {
                Set<ValueExpr> result = new HashSet<ValueExpr>();
                result.addAll(super.getValues());
                MapValuesIterable.enumValues(result, increment);
                return result;
            }

            public UsedChanges(Property property, IncrementChangeTable table) {
                increment = Collections.singletonMap(property, table);
            }

            private UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
                super(usedChanges, mapValues);
                increment = mapValues.translateValues(usedChanges.increment);
            }

            public UsedChanges translate(MapValuesTranslate mapValues) {
                return new UsedChanges(this, mapValues);
            }


        }

        public UsedChanges fullChanges() {
            return new UsedChanges(this);
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            IncrementChangeTable incrementTable = tables.get(property);
            if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
                Join<PropertyField> incrementJoin = incrementTable.join(BaseUtils.join(BaseUtils.reverse(BaseUtils.join(property.mapTable.mapKeys, incrementTable.mapKeys)), joinImplement));
                changedWhere.add(incrementJoin.getWhere());
                return incrementJoin.getExpr(incrementTable.changes.get(property));
            } else
                return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof UsedChanges;
        }

        public UsedChanges used(Property property, UsedChanges usedChanges) {
            IncrementChangeTable incrementTable = tables.get(property);
            if(incrementTable!=null)
                return new UsedChanges(property, incrementTable);
            else
                return usedChanges;
        }

        public UsedChanges newChanges() {
            return UsedChanges.EMPTY;
        }

        public IncrementChangeTable read(Collection<Property> properties,BaseClass baseClass) throws SQLException {
            // создаем таблицу
            IncrementChangeTable changeTable = new IncrementChangeTable(properties);
            session.createTemporaryTable(changeTable);

            // подготавливаем запрос
            Query<KeyField,PropertyField> changesQuery = new Query<KeyField, PropertyField>(changeTable);
            WhereBuilder changedWhere = new WhereBuilder();
            for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                changesQuery.properties.put(change.getValue(),
                        change.getKey().getIncrementExpr(BaseUtils.join(changeTable.mapKeys, changesQuery.mapKeys), this, changedWhere));
            changesQuery.and(changedWhere.toWhere());

            // подготовили - теперь надо сохранить в курсор и записать классы
            changeTable = changeTable.writeRows(session, changesQuery, baseClass);

            for(Property property : properties)
                tables.put(property,changeTable);

            return changeTable;
        }

        public <T extends PropertyInterface> String check(Property<T> property) throws SQLException {
            if(property.isFalse) {
                Query<T,String> changed = new Query<T,String>(property);

                WhereBuilder changedWhere = new WhereBuilder();
                Expr valueExpr = property.getExpr(changed.mapKeys,this,changedWhere);
                changed.and(valueExpr.getWhere());
                changed.and(changedWhere.toWhere()); // только на измененные смотрим

                // сюда надо name'ы вставить
                for(T propertyInterface : property.interfaces) {
                    Expr nameExpr;
                    if(property.getInterfaceType(propertyInterface) instanceof ObjectType) // иначе assert'ионы с compatible'ами нарушатся, если ключ скажем число
                        nameExpr = session.name.getExpr(this, changed.mapKeys.get(propertyInterface));
                    else
                        nameExpr = CaseExpr.NULL;
                    changed.properties.put("int"+propertyInterface.ID, nameExpr);
                }

                OrderedMap<Map<T, Object>, Map<String, Object>> result = changed.execute(session);
                if(result.size()>0) {
                    String resultString = property.toString() + '\n';
                    for(Map.Entry<Map<T,Object>,Map<String,Object>> row : result.entrySet()) {
                        String objects = "";
                        for(T propertyInterface : property.interfaces)
                            objects = (objects.length()==0?"":objects+", ") + BaseUtils.nvl((String)row.getValue().get("int"+propertyInterface.ID),row.getKey().get(propertyInterface).toString()).trim();
                        resultString += "    " + objects + '\n';
                    }

                    return resultString;
                }
            }

            return null;
        }
    }

    public String apply(final BusinessLogics<?> BL) throws SQLException {
        return apply(BL, false);
    }

    public String apply(final BusinessLogics<?> BL, boolean noCommit) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        if(applyObject==null)
            applyObject = addObject(sessionClass, modifier);
        
        startTransaction();

        Collection<IncrementChangeTable> temporary = new ArrayList<IncrementChangeTable>();

        Increment increment = new Increment(this);

        // сохранить св-ва которые Persistent, те что входят в Persistents и DataProperty
        for(Property<?> property : BL.getAppliedProperties())
            if(property.hasChanges(increment)) {
                String constraintResult = increment.check(property);
                if(constraintResult!=null) {
                    // не надо DROP'ать так как Rollback автоматически drop'ает все temporary таблицы 
                    rollbackTransaction();
                    return constraintResult;
                }
                if(property.isStored()) // сохраним изменения в таблицы
                    temporary.add(increment.read(Collections.<Property>singleton(property),baseClass));
            }

        // записываем в базу
        for(Collection<Property> groupTable : BaseUtils.group(new BaseUtils.Group<ImplementTable,Property>(){public ImplementTable group(Property key) {return key.mapTable.table;}},
                increment.tables.keySet()).values()) {
            IncrementChangeTable changeTable;
            if(groupTable.size()==1) // временно так - если одна берем старую иначе группой
                changeTable = increment.tables.get(groupTable.iterator().next());
            else {
                changeTable = increment.read(groupTable,baseClass);
                temporary.add(changeTable);
            }

            Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(changeTable.table);
            Join<PropertyField> join = changeTable.join(BaseUtils.join(BaseUtils.reverse(changeTable.mapKeys), modifyQuery.mapKeys));
            for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
                modifyQuery.properties.put(change.getKey().field,join.getExpr(change.getValue()));
            modifyQuery.and(join.getWhere());
            modifyRecords(new ModifyQuery(changeTable.table, modifyQuery));
        }

        for(Map.Entry<DataObject, ConcreteObjectClass> newClass : newClasses.entrySet())
            newClass.getValue().saveClassChanges(this,newClass.getKey());

        for(IncrementChangeTable addTable : temporary)
            dropTemporaryTable(addTable);
        
        if(!noCommit)
            commitApply();

        return null;
    }

    // для noCommit - распределенных транзакций
    public void rollbackApply() throws SQLException {
        rollbackTransaction();
    }

    // для noCommit - распределенных транзакций
    public void commitApply() throws SQLException {
        commitTransaction();

        restart(false);
    }

    private final Map<Integer,Integer> viewIDs = new HashMap<Integer, Integer>();
    public int generateID(int ID) {
        Integer idCounter;
        synchronized(viewIDs) {
            idCounter = viewIDs.get(ID);
            if(idCounter==null) idCounter = 0;
            viewIDs.put(ID,idCounter+1);
        }
        return (ID << 6) + idCounter;
    }
}
