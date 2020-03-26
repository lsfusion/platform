package lsfusion.server.logics.action.session.classes.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.controller.stack.ParamMessage;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.StaticClassExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.ModifyResult;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.UpdateResult;
import lsfusion.server.logics.action.session.classes.changed.ChangedClasses;
import lsfusion.server.logics.action.session.classes.changed.ChangedDataClasses;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.*;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.classes.user.ObjectClassProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.monitor.StatusMessage;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClassChanges {

//    public static Where isStaticValueClass(Expr expr, ObjectValueClassSet classSet, ObjectValueClassSet usedClasses) {
//        if(classSet.containsAll(usedClasses, false))
//            return expr.getWhere();
//        
//        Where result = Where.FALSE();
//        for(ConcreteCustomClass usedClass : ((ObjectValueClassSet)classSet.and(usedClasses)).getSetConcreteChildren())
//            result = result.or(expr.compare(StaticClassExpr.getClassExpr(usedClass), Compare.EQUALS));
//        return result;
//    }
    
    private static Where isValueClass(Expr expr, ObjectValueClassSet classSet, ImSet<ConcreteObjectClass> usedClasses) {
        Where result = Where.FALSE();
        for(ConcreteObjectClass usedClass : usedClasses)
            if(usedClass instanceof ConcreteCustomClass) {
                ConcreteCustomClass customUsedClass = (ConcreteCustomClass) usedClass;
                if(customUsedClass.inSet(classSet)) // если изменяется на класс, у которого
                    result = result.or(expr.compare(StaticClassExpr.getClassExpr(customUsedClass), Compare.EQUALS)); // тут можно и не static, но логически нет смысла сравнивать, маппить такие значения
            }
        return result;
    }

    public static Where isValueClass(Expr expr, ObjectValueClassSet classSet, ImSet<ConcreteObjectClass> neededClasses, boolean exprOnlyFromNeeded, Where exprJoinWhere, BaseClass baseClass) {
        if(exprOnlyFromNeeded) {
            ImSet<ConcreteCustomClass> neededCustomClasses = BaseUtils.immutableCast(neededClasses.remove(SetFact.singleton(baseClass.unknown)));
            if(classSet.containsAll(new OrObjectClassSet(neededCustomClasses), false)) { // оптимизация (зная что newClassExpr может состоять только из dataUsedNewClasses) 
                if(neededCustomClasses.size() < neededClasses.size()) { // есть удаление
                    if(neededCustomClasses.isEmpty()) {
                        assert expr.getWhere().isFalse();
                        return Where.FALSE();
                    } else
                        return expr.getWhere();
                } else
                    return exprJoinWhere; // просто join возвращаем
            }
        } 
        return isValueClass(expr, classSet, neededClasses);
    }

    private static boolean isValueClass(ObjectValue expr, ObjectValueClassSet classSet, ConcreteObjectClass usedClass) {
        if(usedClass instanceof ConcreteCustomClass) {
            ConcreteCustomClass customUsedClass = (ConcreteCustomClass) usedClass;
            if (customUsedClass.inSet(classSet)) // если изменяется на класс, у которого
                return BaseUtils.hashEquals(expr, customUsedClass.getClassObject()); 
        }
        return false;
    }

    public final static KeyField classField = new KeyField("key0", ObjectType.instance);
    public static Where isStaticValueClass(Expr expr, ObjectValueClassSet classSet) {
        Where result;
        ImSet<ConcreteCustomClass> concreteChildren = classSet.getSetConcreteChildren();
        if(concreteChildren.size() > Settings.get().getSessionRowsToTable()) {
            SessionRows rows = new SessionRows(SetFact.singletonOrder(classField), SetFact.<PropertyField>EMPTY(), concreteChildren.mapSetValues(value -> MapFact.singleton(classField, value.getClassObject())).toMap(MapFact.<PropertyField, ObjectValue>EMPTY()));
            return new ValuesTable(rows).join(MapFact.singleton(classField, expr)).getWhere();
        } else {
            result = Where.FALSE();
            for (ConcreteCustomClass customUsedClass : concreteChildren)
                result = result.or(expr.compare(StaticClassExpr.getClassExpr(customUsedClass), Compare.EQUALS));
        }
        return result;
    }

    // читаем текущие классы в сессии
    public static <K> ImOrderSet<ImMap<K, ConcreteObjectClass>> readChangedCurrentObjectClasses(Where where, ImMap<K, ? extends Expr> classExprs, ImMap<K, ? extends Expr> objectExprs, SQLSession sql, final Modifier modifier, QueryEnvironment env, final BaseClass baseClass) throws SQLException, SQLHandledException {

        final ValueExpr unknownExpr = new ValueExpr(-1L, baseClass.unknown);

        ImRevMap<K,KeyExpr> keys = KeyExpr.getMapKeys(classExprs.keys().addExcl(objectExprs.keys()));
        ImMap<K, Expr> group = ((ImMap<K, Expr>)classExprs).mapValues(value -> value.nvl(unknownExpr)).addExcl(((ImMap<K, Expr>)objectExprs).mapValuesEx((ThrowingFunction<Expr, Expr, SQLException, SQLHandledException>) value -> baseClass.getObjectClassProperty().getExpr(value, modifier).nvl(unknownExpr)));

        return new Query<K, String>(keys, GroupExpr.create(group, where, keys).getWhere()).execute(sql, env).keyOrderSet().mapMergeOrderSetValues(readClasses -> readClasses.mapValues(id -> baseClass.findConcreteClassID((Long) id, -1)));
    }

    public String logSession(Result<Integer> rAddedCount, Result<Integer> rRemovedCount) {
        String result = "";

        int addedCount = 0;
        int removedCount = 0;
        StringBuilder added = new StringBuilder();
        StringBuilder removed = new StringBuilder();
        for(ChangedDataClasses dataNewsInfo : changedClasses.values()) {
            for (CustomClass addEntry : dataNewsInfo.add)
                added.append((added.length() == 0) ? "" : ", ").append(addEntry.getCanonicalName());
            addedCount += dataNewsInfo.add.size();
            for (CustomClass removeEntry : dataNewsInfo.remove)
                removed.append((removed.length() == 0) ? "" : ", ").append(removeEntry.getCanonicalName());
            removedCount += dataNewsInfo.remove.size();
        }

        if(added.length() > 0)
            result += "Added objects of classes: " + added + "\n";

        if(removed.length() > 0)
            result += "Removed objects of classes: " + removed + "\n";

        rAddedCount.set(addedCount);
        rRemovedCount.set(removedCount);
        return result;
    }

    private static void addChanged(MMap<ClassDataProperty, ChangedDataClasses> mChangedClasses, ConcreteObjectClass newcl, ConcreteObjectClass prevcl) {
        MSet<CustomClass> mAddClasses = SetFact.mSet();
        MSet<CustomClass> mRemoveClasses = SetFact.mSet();
        newcl.getDiffSet(prevcl, mAddClasses, mRemoveClasses);
        
        ChangedDataClasses changedDataClasses = new ChangedDataClasses(mAddClasses.immutable(), mRemoveClasses.immutable(), SetFact.singleton(prevcl), SetFact.singleton(newcl));
        if(newcl instanceof ConcreteCustomClass)
            mChangedClasses.add(((ConcreteCustomClass) newcl).dataProperty, changedDataClasses);
        if(prevcl instanceof ConcreteCustomClass)
            mChangedClasses.add(((ConcreteCustomClass) prevcl).dataProperty, changedDataClasses);
    }

    public ChangedClasses readChangedClasses(MaterializableClassChange matChange, Modifier classModifier, SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {

        MMap<ClassDataProperty, ChangedDataClasses> mChangedClasses = MapFact.mMap(ChangedDataClasses.mergeAdd);
        
        if(matChange.change.keyValue !=null) { // оптимизация
            addChanged(mChangedClasses, baseClass.findConcreteClassID((Long) matChange.change.propValue.getValue()),
                                        (ConcreteObjectClass) getCurrentClass(sql, env, baseClass, matChange.change.keyValue));
        } else {
            matChange.materializeIfNeeded("ccltable", sql, baseClass, env, ClassChange::needMaterialize);

            if(matChange.change.isEmpty()) // оптимизация, важна так как во многих event'ах может участвовать
                return null;

            // читаем варианты изменения классов
            for(ImMap<String, ConcreteObjectClass> diffClasses : readChangedCurrentObjectClasses(matChange.change.where, MapFact.singleton("newcl", matChange.change.expr), MapFact.singleton("prevcl", matChange.change.key), sql, classModifier, env, baseClass))
                addChanged(mChangedClasses, diffClasses.get("newcl"), diffClasses.get("prevcl"));
        }
        return new ChangedClasses(mChangedClasses.immutable());
    }
    
    private boolean checkOldChangeClasses(DataObject keyValue, ConcreteObjectClass oldClassObject, boolean isOldClasses, SingleKeyPropertyUsage dataNews, SQLSession sql, QueryEnvironment env) throws SQLException, SQLHandledException {
        ImCol<ImMap<String, Object>> prevValue = dataNews.read(sql, env, keyValue);
        if(prevValue.isEmpty())
            assert oldClassObject instanceof UnknownClass || isOldClasses; // а значит не важно true или false isOldChangeClasses, так как isOldClasses - true и delete все равно false
        else // изменение считалось, но оно собственно и должно быть в oldClassObject
            assert BaseUtils.nullEquals(oldClassObject.getClassObject().getValue(), prevValue.single().singleValue());
        return true;
    }
    
    private Pair<ClassChange, ClassChange> getUpdateNews(ClassChange change, ClassDataProperty dataProperty, SingleKeyPropertyUsage dataNews, ImSet<ConcreteObjectClass> dataUsedNewClasses, ChangedDataClasses dataChangedClasses, BaseClass baseClass, boolean onlyOneChange, SQLSession sql, QueryEnvironment env) throws SQLException, SQLHandledException {
        ClassChange delete;
        ClassChange modify;
        ImSet<ConcreteObjectClass> usedOld = dataChangedClasses.old.filter(dataUsedNewClasses);
        if (change.keyValue != null) { // оптимизация
            boolean isNewChangeClasses = isValueClass(change.propValue, dataProperty.set, dataChangedClasses.newc.single());
            boolean isOldClasses = change.keyValue.objectClass.inSet(dataProperty.set);
            boolean isOldOrNewClasses = isNewChangeClasses || isOldClasses;

            if(!usedOld.isEmpty()) { // оптимизация
                boolean isOldChangeClasses = isValueClass(usedOld.single().getClassObject(), dataProperty.set, usedOld.single()); // предполагается что в usedOld или новый или старый класс (но если там старый класс, то isOldClasses)
                assert checkOldChangeClasses(change.keyValue, usedOld.single(), isOldClasses, dataNews, sql, env);
                delete = isOldChangeClasses && !isOldOrNewClasses ? new ClassChange(change.keyValue, baseClass.unknown) : ClassChange.EMPTY_DELETE;
            } else
                delete = ClassChange.EMPTY_DELETE;

            modify = isOldOrNewClasses ? change : ClassChange.EMPTY;
        } else {
            Where isNewChangeClasses = isValueClass(change.expr, dataProperty.set, dataChangedClasses.newc, onlyOneChange, change.where, baseClass); // если изменяются на классы этого ClassDataProperty
            Where isOldClasses = change.key.isClass(dataProperty.set); // если старый класс был этого ClassDataProperty
            Where isOldOrNewClasses = isNewChangeClasses.or(isOldClasses);

            if(!usedOld.isEmpty()) { // оптимизация
                Where isOldChangeClasses = isValueClass(dataNews.getExpr(change.key), dataProperty.set, usedOld, usedOld.size() == dataUsedNewClasses.size(), dataNews.getWhere(change.key), baseClass); // если были изменения на классы этого ClassDataProperty
                delete = new ClassChange(change.key, change.where.and(isOldChangeClasses.and(isOldOrNewClasses.not())));
            } else
                delete = ClassChange.EMPTY_DELETE;
            
            modify = new ClassChange(change.key, change.where.and(isOldOrNewClasses), change.expr);
        }
        return new Pair<>(delete, modify);
    }
    
    public static SingleKeyPropertyUsage createChangeTable(String debugInfo) {
        return new SingleKeyPropertyUsage(debugInfo, ObjectType.instance, ObjectType.instance);
    }

    public ImMap<Property, UpdateResult> changeClass(MaterializableClassChange matChange, SQLSession sql, BaseClass baseClass, QueryEnvironment env, ChangedClasses changedClasses) throws SQLException, SQLHandledException {
        // если старые классы
        ImMap<ClassDataProperty, ChangedDataClasses> dataProperties = changedClasses.data;
        ImFilterValueMap<ClassDataProperty, ModifyResult> mDataChanges = dataProperties.mapFilterValues();
        MMap<Property, UpdateResult> mIsClassChanges = MapFact.mMap(new SymmAddValue<Property, UpdateResult>() {
            public UpdateResult addValue(Property key, UpdateResult prevValue, UpdateResult newValue) {
                return prevValue.or(newValue);
            }});
        for(int i=0,size=dataProperties.size();i<size;i++) {
            ClassDataProperty dataProperty = dataProperties.getKey(i);
            ChangedDataClasses dataChangedClasses = dataProperties.getValue(i);
            SingleKeyPropertyUsage dataNews = news.get(dataProperty);
            ChangedDataClasses oldChangedClasses = this.changedClasses.get(dataProperty);

            if (dataNews == null) {
                dataNews = createChangeTable("chcl:news");
                news.put(dataProperty, dataNews);
                assert oldChangedClasses == null;
                oldChangedClasses = ChangedDataClasses.EMPTY;
                this.changedClasses.put(dataProperty, oldChangedClasses);
            }

            final SingleKeyPropertyUsage fDataNews = dataNews; // материализуем так как change несколько раз будет использоваться и по сути изменится при изменении dataNews (а не из-за того что нельзя modify'ить таблицу используя ее)
            matChange.materializeIfNeeded("chcl:nmn", sql, baseClass, env, value -> {
                return value.needMaterialize(fDataNews); // return true всегда materializ'овать чтобы не переписывала таблицу в rewrite при чтении isOldChangeClasses ????
            });
            
            Pair<ClassChange, ClassChange> update = getUpdateNews(matChange.change, dataProperty, dataNews, oldChangedClasses.newc, dataChangedClasses, baseClass, dataProperties.size() == 1, sql, env);
            ClassChange deleteChange = update.first; ClassChange modifyChange = update.second;

//            // нижний assertion по идее только при удалении добавленного нарушается, но как этим воспользоваться пока непонятно
//            assert dataProperties.size() != 1 || BaseUtils.hashEquals(modifyChange.getQuery(), matChange.change.getQuery()) && deleteChange.isEmpty());

            ModifyResult deleteChanged = deleteChange.modifyRows(dataNews, sql, baseClass, Modify.DELETE, env, env.getOpOwner(), SessionTable.matGlobalQuery);
            ModifyResult modifyChanged = modifyChange.modifyRows(dataNews, sql, baseClass, Modify.MODIFY, env, env.getOpOwner(), SessionTable.matGlobalQuery);

            ModifyResult tableChanged = deleteChanged.or(modifyChanged);

            ChangedDataClasses newChangedClasses = oldChangedClasses.merge(dataChangedClasses);
            if(newChangedClasses != oldChangedClasses) // оптимизация - если реально изменились классы, а не только удалились
                this.changedClasses.put(dataProperty, newChangedClasses);

            if(tableChanged.sourceChanged() || (dataChangedClasses != newChangedClasses && oldChangedClasses != newChangedClasses && !oldChangedClasses.newc.containsAll(newChangedClasses.newc))) // если изменились источник (а newc - тоже источник), добавляем всем изменение SOURCE
                mIsClassChanges.addAll(getChangedIsClassProperties(newChangedClasses, baseClass).toMap(UpdateResult.SOURCE));

            aspectChangeClass(mDataChanges, mIsClassChanges, i, dataProperty, dataNews, tableChanged, dataChangedClasses, baseClass);
        }
        
        this.newClasses.clear();
        
        return MapFact.addExcl(mDataChanges.immutableValue(), mIsClassChanges.immutable());
    }

    public void aspectChangeClass(ImFilterValueMap<ClassDataProperty, ModifyResult> mDataChanges, MMap<Property, UpdateResult> mIsClassChanges, int i, ClassDataProperty dataProperty, SingleKeyPropertyUsage dataNews, ModifyResult tableChanged, ChangedDataClasses dataChangedClasses, BaseClass baseClass) {
        if(dataNews.isEmpty()) { // есть удаление
            news.remove(dataProperty);
            this.changedClasses.remove(dataProperty);
        }

        mDataChanges.mapValue(i, tableChanged);

        // надо еще increment'ить getIsClassChange, чтобы не сильно усложнять, просто при изменении source или changedClasses, обновляем все add и remove классы которые в getClassDataProps содержат это dataProperty (они по определению в качестве )
        mIsClassChanges.addAll(getChangedIsClassProperties(dataChangedClasses, baseClass).toMap(tableChanged));
    }

    public void copyDataTo(DataSession other) throws SQLException, SQLHandledException {
        for(Map.Entry<ClassDataProperty, SingleKeyPropertyUsage> entry : news.entrySet())
            other.changeClass(entry.getValue().getChange());
    }

    public long getMaxDataUsed(Property prop) {
        if(prop instanceof IsClassProperty || prop instanceof ClassDataProperty || prop instanceof ObjectClassProperty) {
            ImSet<ClassDataProperty> classDataProps;
            if(prop instanceof IsClassProperty)
                classDataProps = ((IsClassProperty) prop).getClassDataProps(); 
            else if(prop instanceof ObjectClassProperty)
                classDataProps = ((ObjectClassProperty) prop).getClassDataProps();
            else
                classDataProps = SetFact.singleton((ClassDataProperty) prop);
                
            long count = 0;
            for(ClassDataProperty classDataProp : classDataProps) {
                SingleKeyPropertyUsage dataNews = news.get(classDataProp);
                if(dataNews != null)
                    count += dataNews.getCount();
            }
            return count;
        }
        return 0;
    }

    public class Transaction {
        private final ImMap<ClassDataProperty, SessionData> news;
        private final Map<ClassDataProperty, ChangedDataClasses> changedClasses;

        private final Map<DataObject, ConcreteObjectClass> newClasses;

        public Transaction() {
            changedClasses = new HashMap<>(ClassChanges.this.changedClasses);
            newClasses = new HashMap<>(ClassChanges.this.newClasses);

            news = SessionTableUsage.saveData(ClassChanges.this.news);
        }
        
        public void rollback(SQLSession sql, OperationOwner owner) throws SQLException {
            ClassChanges.this.changedClasses = changedClasses;
            ClassChanges.this.newClasses = newClasses;

            rollNews(sql, owner);
        }

        private void rollNews(SQLSession sql, OperationOwner owner) throws SQLException {
            Map<ClassDataProperty, SingleKeyPropertyUsage> rollData = MapFact.mAddRemoveMap();
            for(int i=0,size=news.size();i<size;i++) {
                ClassDataProperty prop = news.getKey(i);

                SingleKeyPropertyUsage table = ClassChanges.this.news.get(prop);
                if(table==null) {
                    table = createChangeTable("rllnews");
                    table.drop(sql, owner);
                }

                table.rollData(sql, news.getValue(i), owner);
                rollData.put(prop, table);
            }
            ClassChanges.this.news = rollData;
        }

    }
    
    public Transaction startTransaction() {
        return new Transaction();
    }
    
    // предполагается хранит изменение ClassDataProperty как если бы это было DataProperty (за исключением того что при изменении на другое ClassDataProperty, хранит новый класс, в getClassDataChange заменяется на null)
    private Map<ClassDataProperty, SingleKeyPropertyUsage> news;
    // оптимизационные вещи
    private Map<ClassDataProperty, ChangedDataClasses> changedClasses;
    public ImSet<CustomClass> getAllRemoveClasses() {
        return ChangedClasses.getAllRemoveClasses(changedClasses);
    }

    // просто lazy кэш для getCurrentClass
    private Map<DataObject, ConcreteObjectClass> newClasses = MapFact.mAddRemoveMap();
    
    public ClassChanges() { // mutable конструктор
        news = MapFact.mAddRemoveMap();
        changedClasses = MapFact.mAddRemoveMap();
    }

    public ClassChanges(ImMap<ClassDataProperty, SingleKeyPropertyUsage> news, ImMap<ClassDataProperty, ChangedDataClasses> changedClasses) {
        this.news = Collections.unmodifiableMap(news.toJavaMap()); // immutable
        this.changedClasses = Collections.unmodifiableMap(changedClasses.toJavaMap()); // immutable;
    }

    public boolean hasChanges() {
        return !news.isEmpty();
    }

    public boolean hasChanges(AndClassSet classSet) {
        boolean result = false;
        if(classSet instanceof ObjectClassSet) {
            ObjectClassSet objectClassSet = (ObjectClassSet) classSet;
            ObjectValueClassSet valueClassSet = objectClassSet.getValueClassSet();
            if(!BaseUtils.hashEquals(classSet, valueClassSet)) { // если есть unknown все равно проверяем все классы
                result = hasObjectChanges(objectClassSet);
                assert hasObjectValueChanges(valueClassSet) == hasObjectChanges(valueClassSet);
            } else {
                result = hasObjectValueChanges(valueClassSet);
                assert result == hasObjectChanges(valueClassSet);
            }
                
        }
        return result; // concatenate будем считать что нет изменений
    }

    public boolean hasObjectChanges(ObjectClassSet classSet) {
        return hasObjectChanges(classSet, null);
    }

    public boolean hasObjectChanges(ObjectClassSet classSet, ImSet<ClassDataProperty> filter) {
        for (Map.Entry<ClassDataProperty, SingleKeyPropertyUsage> dataNews : news.entrySet())
            if(filter == null || filter.contains(dataNews.getKey()))
                if (!dataNews.getValue().getClasses().and(classSet).isEmpty())
                    return true;
        return false;
    }

    public boolean hasObjectValueChanges(ObjectValueClassSet valueClassSet) {
        return hasObjectChanges(valueClassSet, BaseUtils.<ImSet<ClassDataProperty>>immutableCast(valueClassSet.getObjectClassFields().keys()));
    }

    public void drop(SQLSession sql, OperationOwner owner) throws SQLException {
        for(SingleKeyPropertyUsage dataNews : news.values())
            dataNews.drop(sql, owner);
    }
    
    public void clear() {
        news.clear();

        changedClasses.clear();
        
        newClasses.clear();
    }
    
    public ImSet<Property> getChangedProps(BaseClass baseClass) {
        return getChangedProps(news.keySet(), changedClasses, baseClass);
    }
    
    public static ImSet<Property> getChangedProps(Iterable<ClassDataProperty> news, Map<ClassDataProperty, ChangedDataClasses> changedClasses, BaseClass baseClass) {
        MSet<Property> mResult = SetFact.mSet();
        for(ClassDataProperty dataProperty : news) {
            mResult.add(dataProperty);
            mResult.addAll(getChangedIsClassProperties(changedClasses.get(dataProperty), baseClass));
        }
        return mResult.immutable();
    }
    public static ImSet<Property> getChangedProps(ImMap<ClassDataProperty, ChangedDataClasses> news, BaseClass baseClass) {
        MSet<Property> mResult = SetFact.mSet();
        for(int i=0,size=news.size();i<size;i++) {
            mResult.add(news.getKey(i));
            mResult.addAll(getChangedIsClassProperties(news.getValue(i), baseClass));
        }
        return mResult.immutable();
    }

    private static ImSet<Property> getChangedIsClassProperties(ChangedDataClasses dataChangedClasses, BaseClass baseClass) {
        MSet<Property> mResult = SetFact.mSet();
        for(CustomClass customClass : dataChangedClasses.add)
            mResult.add(customClass.getProperty());
        for(CustomClass customClass : dataChangedClasses.remove)
            mResult.add(customClass.getProperty());
        if(!dataChangedClasses.add.isEmpty() || !dataChangedClasses.remove.isEmpty())
            mResult.add(baseClass.getObjectClassProperty());
        return mResult.immutable();
    }
    private static ImSet<IsClassProperty> fillChangedIsClassProperties(ChangedDataClasses dataChangedClasses) {
        ImSet<CustomClass> merged = dataChangedClasses.add.merge(dataChangedClasses.remove);
        return merged.mapSetValues(ValueClass::getProperty);
    }

    public PropertyChange<ClassPropertyInterface> getIsClassChange(IsClassProperty property, BaseClass baseClass) { // важно чтобы совпадало с инкрементальным алгритмом в changeClass
        ValueClass isClass = property.getInterfaceClass();
        if(isClass instanceof CustomClass) {
            CustomClass customClass = (CustomClass) isClass;
            ImMap<ClassDataProperty, ObjectValueClassSet> classDataProps = BaseUtils.immutableCast(customClass.getUpObjectClassFields());
            if(!SetFact.intersectJava(news.keySet(), classDataProps.keys())) // оптимизация
                return null;

            ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
            KeyExpr key = mapKeys.singleValue();

            // has : (W1E1 OR .. WnEn)
            // changed : (W1 OR .. Wn)  
            // new is : has OR !changed*had что эквивалентно IF (!had*has OR had*!has*changed) THEN has ELSE had  
            // changed is : !had*has OR had*!has*changed

            Where had = null;

            Where has = Where.FALSE(); 
            Where changed = Where.FALSE(); 
            for(int i=0,size=classDataProps.size();i<size;i++) {
                ClassDataProperty dataProperty = classDataProps.getKey(i);
                ObjectValueClassSet classSet = classDataProps.getValue(i);
                
                SingleKeyPropertyUsage dataNews = news.get(dataProperty);
                if(dataNews != null) {
                    ChangedDataClasses dataChangedClasses = changedClasses.get(dataProperty);
                    
                    boolean added = dataChangedClasses.add.contains(customClass);
                    boolean removed = dataChangedClasses.remove.contains(customClass);
                    if(added || removed) {
                        if(had == null) // оптимизация
                            had = key.isUpClass(isClass);

                        Join<String> join = dataNews.join(key);
                        Expr newClassExpr = join.getExpr("value");
                        Where where = join.getWhere();                    

                        Where dataChanged = null;
                        if(added) {
                            dataChanged = isValueClass(newClassExpr, classSet, dataChangedClasses.newc, true, where, baseClass);
                            has = has.or(dataChanged);
                        }
                        if(removed)
                            dataChanged = where;
                        changed = changed.or(dataChanged);
                    }
                }
            }
            if(had != null) {
                Where changedWhere = has.and(had.not()).or(had.and(changed.and(has.not())));
                if(!changedWhere.isFalse()) // вообще оптимизация, есть некритичный assertion местами, что должен быть !empty (так как в PropertyChanges.replace есть фильтрация emptyChanges)
                    return new PropertyChange<>(mapKeys, ValueExpr.get(has), changedWhere);
            }
        }
        return null;
    }

    public PropertyChange<ClassPropertyInterface> getObjectClassChange(ObjectClassProperty property, BaseClass baseClass) {
        if(news.isEmpty()) // оптимизация
            return null;

        ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
        KeyExpr key = mapKeys.singleValue();

        Where changeWhere = Where.FALSE();
        Expr changeExpr = Expr.NULL();
        for(SingleKeyPropertyUsage dataNews : news.values()) {
            Join<String> join = dataNews.join(key);
            Expr newClassExpr = join.getExpr("value");
            Where where = join.getWhere();

            changeWhere = changeWhere.or(where);
            changeExpr = changeExpr.nvl(newClassExpr);            
        }
        return new PropertyChange<>(mapKeys, changeExpr, changeWhere);
    }

    public PropertyChange<ClassPropertyInterface> getClassDataChange(ClassDataProperty property, BaseClass baseClass) {
        SingleKeyPropertyUsage dataNews = news.get(property);
        if(dataNews != null) {
            ChangedDataClasses dataInfo = changedClasses.get(property);

            ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = property.getMapKeys();
            KeyExpr keyExpr = mapKeys.singleValue();

            Join<String> join = dataNews.join(keyExpr);
            Expr newClassExpr = join.getExpr("value");
            Where where = join.getWhere();
            
            Where newClass = isValueClass(newClassExpr, property.set, dataInfo.newc, true, where, baseClass);

            return new PropertyChange<>(mapKeys, // на не null меняем только тех кто подходит по классу
                    newClassExpr.and(newClass), where);
        }
        return null;
    }

    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property, BaseClass baseClass) {
        if(property instanceof ObjectClassProperty)
            return (PropertyChange<P>) getObjectClassChange((ObjectClassProperty) property, baseClass);

        if(property instanceof ClassDataProperty)
            return (PropertyChange<P>) getClassDataChange(((ClassDataProperty) property), baseClass);

        if(property instanceof IsClassProperty)
            return (PropertyChange<P>) getIsClassChange((IsClassProperty) property, baseClass);
        
        return null;
    }

    private ConcreteObjectClass readCurrentClass(SingleKeyPropertyUsage dataNews, DataObject value, SQLSession sql, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        ImCol<ImMap<String, Object>> read = dataNews.read(sql, env, value);
        if(read.size()==0)
            return null;
        else
            return baseClass.findConcreteClassID((Long) read.single().singleValue());        
    }
    private ConcreteObjectClass getObjectCurrentClass(DataObject object, SQLSession sql, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        for(SingleKeyPropertyUsage dataNews : news.values()) {
            ConcreteObjectClass newClass = readCurrentClass(dataNews, object, sql, env, baseClass);
            if(newClass != null)
                return newClass;
        }
        return null;
    }
    private ConcreteObjectClass getObjectValueCurrentClass(ConcreteCustomClass customClass, DataObject object, SQLSession sql, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        SingleKeyPropertyUsage dataNews = news.get(customClass.dataProperty);
        if(dataNews != null)
            return readCurrentClass(dataNews, object, sql, env, baseClass);
        return null;
    }

    public ConcreteClass getCurrentClass(SQLSession sql, QueryEnvironment env, BaseClass baseClass, DataObject value) throws SQLException, SQLHandledException {
        ConcreteObjectClass newClass = null;
        if(value.objectClass instanceof ConcreteObjectClass) {
            if(newClasses.containsKey(value))
                newClass = newClasses.get(value);
            else {
                if(value.objectClass instanceof UnknownClass)
                    newClass = getObjectCurrentClass(value, sql, env, baseClass);
                else {
                    newClass = getObjectValueCurrentClass((ConcreteCustomClass) value.objectClass, value, sql, env, baseClass);
                    assert BaseUtils.nullHashEquals(newClass, getObjectCurrentClass(value, sql, env, baseClass));
                }
                newClasses.put(value, newClass);
            }
        }

        if(newClass==null)
            return value.objectClass;
        else
            return newClass;
    }

    public ObjectValue updateCurrentClass(SQLSession sql, QueryEnvironment env, BaseClass baseClass, ObjectValue value) throws SQLException, SQLHandledException {
        if(value instanceof DataObject) {
            DataObject dataObject = (DataObject)value;
            ConcreteClass currentClass = getCurrentClass(sql, env, baseClass, dataObject);
            if(currentClass != dataObject.objectClass) // optimization
                return new DataObject(dataObject.object, currentClass);
        }
        return value;
    }

    public <K, T extends ObjectValue> ImMap<K, T> updateCurrentClasses(SQLSession sql, QueryEnvironment env, BaseClass baseClass, ImMap<K, T> objectValues) throws SQLException, SQLHandledException {
        return objectValues.<SQLException, SQLHandledException>mapItIdentityValuesEx(value -> (T) updateCurrentClass(sql, env, baseClass, value));
    }
    
    public ImSet<CustomClass> packRemoveClasses(Modifier classModifier, BusinessLogics BL, SQLSession sql, QueryEnvironment queryEnv) throws SQLException, SQLHandledException {
        if(news.isEmpty()) // оптимизация
            return SetFact.EMPTY();

        ImSet<CustomClass> remove = getAllRemoveClasses();
        // проводим "мини-паковку", то есть удаляем все записи, у которых ключем является удаляемый объект
        for(ImplementTable table : BL.LM.tableFactory.getImplementTables(remove)) {
            QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<>(table);
            Where removeWhere = Where.FALSE();
            ImMap<KeyField, ValueClass> mapFields = table.getMapFields();
            ImMap<KeyField, Expr> mapExprs = query.getMapExprs();
            for (int i = 0, size = mapFields.size(); i < size; i++) {
                try {
                    KeyField key = mapFields.getKey(i);
                    sql.statusMessage = new StatusMessage("delete", key, i, size);
                    ValueClass value = mapFields.getValue(i);
                    if (value instanceof CustomClass && remove.contains((CustomClass) value))
                        removeWhere = removeWhere.or(value.getProperty().getDroppedWhere(mapExprs.get(key), classModifier));
                } finally {
                    sql.statusMessage = null;
                }
            }
            query.and(table.join(mapExprs).getWhere().and(removeWhere));
            sql.deleteRecords(new ModifyQuery(table, query.getQuery(), queryEnv, TableOwner.global));
        }
        return remove;
    }

    private final Pair<Pair<ImMap<ClassDataProperty, SingleKeyPropertyUsage>, ImMap<ClassDataProperty, ChangedDataClasses>>, ImMap<Property, UpdateResult>> EMPTY_SPLIT = new Pair<>(new Pair<>(MapFact.<ClassDataProperty, SingleKeyPropertyUsage>EMPTY(), MapFact.<ClassDataProperty, ChangedDataClasses>EMPTY()), MapFact.<Property, UpdateResult>EMPTY());
    public Pair<Pair<ImMap<ClassDataProperty, SingleKeyPropertyUsage>, ImMap<ClassDataProperty, ChangedDataClasses>>, ImMap<Property, UpdateResult>> splitSingleApplyRemove(IsClassProperty classProperty, BaseClass baseClass, SQLSession sql, QueryEnvironment queryEnv, Runnable checkTransaction) throws SQLException, SQLHandledException {
        if(news.isEmpty() || !Settings.get().isEnableApplySingleRemoveClasses())
            return EMPTY_SPLIT;

        CustomClass customClass = (CustomClass) classProperty.getInterfaceClass();
        if(customClass.disableSingleApply())
            return EMPTY_SPLIT;
        
        ImMap<ClassDataProperty, ObjectValueClassSet> classDataProps = BaseUtils.immutableCast(customClass.getUpObjectClassFields());

        // оптимизация проверяем что удаление есть, и что имеет смысл обрабатывать именно этому классу
        
        MSet<ConcreteObjectClass> mOld = null;
        for(int i=0,size=classDataProps.size();i<size;i++) {
            ChangedDataClasses changedDataClasses = changedClasses.get(classDataProps.getKey(i));
            if(changedDataClasses != null && changedDataClasses.newc.contains(baseClass.unknown)) {
                if(mOld == null)
                    mOld = SetFact.mSet();
                mOld.addAll(changedDataClasses.old);
            }
        }
        if(mOld == null)
            return EMPTY_SPLIT;

        OrObjectClassSet oldCustomSet = new OrObjectClassSet(BaseUtils.<ImSet<ConcreteCustomClass>>immutableCast(mOld.immutable().remove(SetFact.singleton(baseClass.unknown)))); // unknown'ы не интересуют
        for(CustomClass childClass : customClass.getChildrenIt())
            if(childClass.getUpSet().containsAll(oldCustomSet, false)) // если есть child который содержит все old'ы пусть и обрабатывает их
                return EMPTY_SPLIT;

        checkTransaction.run();

        return splitSingleApplyRemoveWithChanges(classDataProps, customClass, sql, queryEnv, baseClass);
    }

    @StackMessage("{logics.split.objects.remove.classes}")
    public Pair<Pair<ImMap<ClassDataProperty, SingleKeyPropertyUsage>, ImMap<ClassDataProperty, ChangedDataClasses>>, ImMap<Property, UpdateResult>> splitSingleApplyRemoveWithChanges(ImMap<ClassDataProperty, ObjectValueClassSet> classDataProps, @ParamMessage CustomClass customClass, SQLSession sql, QueryEnvironment queryEnv, BaseClass baseClass) throws SQLException, SQLHandledException {
        KeyExpr keyExpr = new KeyExpr("split");
        Where had = keyExpr.isUpClass(customClass);

        ImFilterValueMap<ClassDataProperty, ModifyResult> mDataChanges = classDataProps.mapFilterValues();
        MMap<Property, UpdateResult> mIsClassChanges = MapFact.mMap(new SymmAddValue<Property, UpdateResult>() {
            public UpdateResult addValue(Property key, UpdateResult prevValue, UpdateResult newValue) {
                return prevValue.or(newValue);
            }});
        ImFilterValueMap<ClassDataProperty, SingleKeyPropertyUsage> mSplitNews = classDataProps.mapFilterValues();
        ImFilterValueMap<ClassDataProperty, ChangedDataClasses> mSplitChangedClasses = classDataProps.mapFilterValues();

        for(int i=0,size=classDataProps.size();i<size;i++) {
            ClassDataProperty dataProperty = classDataProps.getKey(i);
            ObjectValueClassSet dataClassSet = classDataProps.getValue(i);
            
            SingleKeyPropertyUsage dataNews = news.get(dataProperty);
            if (dataNews != null) {
                ChangedDataClasses dataChangedClasses = changedClasses.get(dataProperty);
                if (dataChangedClasses.newc.contains(baseClass.unknown)) {
                    Join<String> join = dataNews.join(keyExpr);
                    Expr newClassExpr = join.getExpr("value");
                    Where where = join.getWhere();

                    Where deleted = newClassExpr.getWhere().not(); // удаления
                    
                    ClassChange classChange = new ClassChange(keyExpr, where.and(deleted).and(had), Expr.NULL());

                    // читаем удаления в отдельную таблицу
                    SingleKeyPropertyUsage splitTable = createChangeTable("split");
                    splitTable.writeRows(sql, classChange.getQuery(), baseClass, queryEnv, SessionTable.matGlobalQuery); // нужно update'ть классы чтобы не было unknown классов в таблице
                    try {
                        if(!splitTable.isEmpty()) {
                            // удаляем все удаления
                            ModifyResult deleteChanged = new ClassChange(classChange.key, classChange.where).modifyRows(dataNews, sql, baseClass, Modify.DELETE, queryEnv, queryEnv.getOpOwner(), SessionTable.matGlobalQuery);

                            ChangedDataClasses splitChanges;
                            if (deleteChanged.dataChanged()) // оптимизация
                                splitChanges = splitChangedDataClasses(dataProperty, customClass, dataClassSet, dataChangedClasses, baseClass);
                            else 
                                splitChanges = ChangedDataClasses.EMPTY;

                            aspectChangeClass(mDataChanges, mIsClassChanges, i, dataProperty, dataNews, deleteChanged, dataChangedClasses, baseClass);

                            mSplitNews.mapValue(i, splitTable);
                            mSplitChangedClasses.mapValue(i, splitChanges);
                        }
                    } catch (Throwable e) {
                        splitTable.drop(sql, queryEnv.getOpOwner());
                        for(SingleKeyPropertyUsage prevSplitTable : mSplitNews.immutableValue().valueIt())
                            prevSplitTable.drop(sql, queryEnv.getOpOwner());
                        throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
                    }
                }
            }
        }
        return new Pair<>(new Pair<>(mSplitNews.immutableValue(), mSplitChangedClasses.immutableValue()), MapFact.addExcl(mDataChanges.immutableValue(), mIsClassChanges.immutable()));
    }

    public ChangedDataClasses splitChangedDataClasses(ClassDataProperty dataProperty, final CustomClass customClass, ObjectValueClassSet dataClassSet, ChangedDataClasses dataChangedClasses, BaseClass baseClass) {
        boolean onlyRemove = dataChangedClasses.newc.size() == 1; // если было только удаление (самый частый случай)
        OrObjectClassSet oldCustomSet = new OrObjectClassSet(BaseUtils.<ImSet<ConcreteCustomClass>>immutableCast(dataChangedClasses.old.remove(SetFact.singleton(baseClass.unknown)))); // unknown'ы не интересуют
        boolean allRemoved = dataClassSet.containsAll(dataProperty.set.and(oldCustomSet), false); // если все удаления отделили (а они должны входить и в old и в dataProperty.set)
        if(onlyRemove || allRemoved) { // эвристика (тут можно еще оптимизировать, не включен частый случай добавления и удаления одновременно)
            ImSet<ConcreteObjectClass> newNewc = dataChangedClasses.newc;
            if(allRemoved) // по идее не из dataProperty.set'а удалений и так в таблице не было (по определению)  
                newNewc = newNewc.removeIncl(baseClass.unknown);
            
            ChangedDataClasses newChangedClasses;
            if(allRemoved && onlyRemove) {
                assert newNewc.isEmpty() && dataChangedClasses.add.isEmpty();
                newChangedClasses = ChangedDataClasses.EMPTY;
            } else
                newChangedClasses = new ChangedDataClasses(dataChangedClasses.add, dataChangedClasses.remove, dataChangedClasses.old, newNewc); 
                
            changedClasses.put(dataProperty, newChangedClasses);
        }
        return new ChangedDataClasses(SetFact.<CustomClass>EMPTY(), dataChangedClasses.remove, null, SetFact.<ConcreteObjectClass>singleton(baseClass.unknown));
    }

}
