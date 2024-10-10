package lsfusion.server.logics.property;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.lambda.CallableWithParam;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.*;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.NullableKeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.pack.PackComplex;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.MapKeysInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.table.*;
import lsfusion.server.data.type.AbstractType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.language.ScriptParsingException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.*;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.changed.SessionProperty;
import lsfusion.server.logics.action.session.table.NoPropertyWhereTableUsage;
import lsfusion.server.logics.action.session.table.PropertyChangeTableUsage;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.*;
import lsfusion.server.logics.classes.data.file.AJSONClass;
import lsfusion.server.logics.classes.struct.ConcatenateValueClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.OrClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.classes.user.set.ResolveUpClassSet;
import lsfusion.server.logics.event.*;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.*;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormSelector;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.property.checked.ConstraintCheckChangeProperty;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.filter.ContextFilterEntity;
import lsfusion.server.logics.form.struct.property.PropertyClassImplement;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.navigator.controller.env.ChangesController;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.*;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.logics.property.value.ValueProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.dev.debug.PropertyDebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import lsfusion.server.physics.exec.db.table.DBTable;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import lsfusion.server.physics.exec.db.table.MapKeysTable;
import lsfusion.server.physics.exec.db.table.TableFactory;
import lsfusion.server.physics.exec.hint.AutoHintsAspect;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;
import static lsfusion.server.logics.property.oraction.ActionOrPropertyUtils.*;

public abstract class Property<T extends PropertyInterface> extends ActionOrProperty<T> implements MapKeysInterface<T> {

    public static Modifier defaultModifier = () -> PropertyChanges.EMPTY;

    public String getTableDebugInfo(String operation) {
        return getClass() + "," + debugInfo + "-" + operation;
    }
    
    public static FunctionSet<Property> getDependsOnSet(final FunctionSet<Property> check) {
        if(check.isEmpty())
            return check;
        return new FunctionSet<Property>() {
            public boolean contains(Property element) {
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
   
    public static FunctionSet<Property> getSet(final FunctionSet<SessionDataProperty> set) {
        if(set.isEmpty())
            return SetFact.EMPTY();
        return new FunctionSet<Property>() {
            public boolean contains(Property element) {
                return element instanceof SessionDataProperty && set.contains((SessionDataProperty) element);
            }

            public boolean isEmpty() {
                return set.isEmpty();
            }

            public boolean isFull() {
                return false;
            }
        };
    }

    public static FunctionSet<Property> getDependsFromSet(final ImSet<Property> check) {
        return new FunctionSet<Property>() {
            public boolean contains(Property element) {
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

    public static boolean depends(Property<?> property, Property check) {
        return property.getRecDepends().contains(check);
    }

    public static boolean depends(Property<?> property, Property check, boolean events) {
        return property.calculateRecDepends(events).contains(check);
    }

    public static boolean depends(Property<?> property, FunctionSet<Property> check) {
        return property.getRecDepends().intersectFn(check);
    }

    public static boolean dependsSet(ImSet<Property> properties, FunctionSet<Property> check) {
        for(Property property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static boolean dependsSet(Property<?> property, FunctionSet<Property>... checks) {
        for(FunctionSet<Property> check : checks)
            if(depends(property, check))
                return true;
        return false;
    }

    public static boolean depends(Iterable<Property> properties, ImSet<Property> check) {
        for(Property property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static boolean depends(ImSet<Property> properties, Property check) {
        for(Property property : properties)
            if(depends(property, check))
                return true;
        return false;
    }

    public static <T extends Property> ImSet<T> used(ImSet<T> used, final ImSet<Property> usedIn) {
        return used.filterFn(property -> depends(usedIn, property));
    }

    public static <T extends PropertyInterface> boolean dependsImplement(ImCol<PropertyInterfaceImplement<T>> properties, ImSet<Property> check) {
        for(PropertyInterfaceImplement<T> property : properties)
            if(property instanceof PropertyMapImplement && depends(((PropertyMapImplement)property).property, check))
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
        return localize(LocalizedString.concatList((this instanceof DataProperty ? "{logics.property.primary}" : "{logics.property.calculated}"), 
                    LocalizedString.create(" {logics.property} : "), caption, ", " + mapTable.table.outputField(field, outputTable))
        );
    }
    
    public void outClasses(DataSession session, Modifier modifier) throws SQLException, SQLHandledException {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        new Query<>(mapKeys, getExpr(mapKeys, modifier), "value").outClassesSelect(session.sql, session.baseClass);
    }

    // по выражениям проверяет
    public <P extends PropertyInterface> void checkExclusiveness(String caseInfo, Property<P> property, String propertyInfo, ImRevMap<P, T> map, String abstractInfo) {
        AlgType.caseCheckType.checkExclusiveness(this, caseInfo, property, propertyInfo, map, abstractInfo);
    }

    public <P extends PropertyInterface> void inferCheckExclusiveness(String caseInfo, Property<P> property, String propertyInfo, ImRevMap<P, T> map, InferType inferType, String abstractInfo) {
        Inferred<T> classes = inferInterfaceClasses(inferType);
        Inferred<P> propClasses = property.inferInterfaceClasses(inferType);
        if(!classes.and(propClasses.map(map), inferType).isEmpty(inferType))
            throw new ScriptParsingException("signature intersection of property\n\t" + caseInfo + " (" + this + ") with previosly defined implementation\n\t" + propertyInfo + " (" + property + ")\nfor abstract property " + abstractInfo + 
                    "\n\tClasses 1: " + ExClassSet.fromEx(classes.finishEx(inferType)) + "\n\tClasses 2: " + ExClassSet.fromEx(propClasses.finishEx(inferType)));
    }

    public <P extends PropertyInterface> void calcCheckExclusiveness(String caseInfo, Property<P> property, String propertyInfo, ImMap<P, T> map, CalcClassType calcType, String abstractInfo) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        if(!calculateExpr(mapKeys, calcType).getWhere().and(property.calculateExpr(map.join(mapKeys), calcType).getWhere()).not().checkTrue())
            throw new ScriptParsingException("signature intersection of property\n\t" + caseInfo + " (" + this + ") with previosly defined implementation\n\t" + propertyInfo + " (" + property + ")\nfor abstract property " + abstractInfo +
                    "\n\tClasses 1: " + getClassWhere(calcType) + "\n\tClasses 2: " + property.getClassWhere(calcType));
    }

    public <P extends PropertyInterface> void checkContainsAll(Property<P> property, String caseInfo, ImRevMap<P, T> map, PropertyInterfaceImplement<T> value, String abstractInfo) {
        AlgType.caseCheckType.checkContainsAll(this, property, caseInfo, map, value, abstractInfo);
    }

    public <P extends PropertyInterface> void inferCheckContainsAll(Property<P> property, String caseInfo, ImRevMap<P, T> map, InferType inferType, PropertyInterfaceImplement<T> value, String abstractInfo) {
        ImMap<T, ExClassSet> interfaceClasses = getInferInterfaceClasses(inferType);
        ImMap<T, ExClassSet> interfacePropClasses = map.crossJoin(property.getInferInterfaceClasses(inferType));

        if(!containsAll(interfaceClasses, interfacePropClasses, false))
            throw new ScriptParsingException("wrong signature of implementation " + caseInfo + " (" + property + ")" + " for abstract property " + abstractInfo + 
                    "\n\tspecified: " + ExClassSet.fromEx(interfacePropClasses) + "\n\texpected : " + ExClassSet.fromEx(interfaceClasses) + ")");

        ResolveClassSet valueClass = ExClassSet.fromEx(inferValueClass(interfaceClasses, inferType));
        ResolveClassSet propValueClass = ExClassSet.fromEx(value.mapInferValueClass(interfacePropClasses, inferType));
        if(!valueClass.containsAll(propValueClass, false))
            throw new ScriptParsingException("wrong value class of implementation " + caseInfo + " (" + property + ")" + " for abstract property " + abstractInfo +
                    "\n\tspecified: " + propValueClass + "\n\texpected : " + valueClass);
    }
    
    public ExClassSet inferJoinValueClass(ImMap<T, ExClassSet> extContext, boolean useExtContext, InferType inferType) {
        if(!useExtContext && inferType == InferType.resolve()) {
            assert explicitClasses != null;
            return ExClassSet.toEx(getResolveClassSet(explicitClasses));
        }
        return inferValueClass(extContext, inferType);

//        ImMap<T, ExClassSet> inferred = getInferInterfaceClasses(inferType);
//        ExClassSet newDiffClassSet;
//        if(inferred == null) {
//            newDiffClassSet = null;
//        } else {
//            newDiffClassSet = inferValueClass(inferred, inferType);
//        }
//
//        ExClassSet newValueClass = ExClassSet.toEx(getResolveClassSet());
//        if(useExtContext) {
//            ExClassSet oldNewExtContext = inferValueClass(ExClassSet.op(interfaces, inferred, extContext, false), inferType);
//            if(!BaseUtils.nullEquals(oldExtContext, oldNewExtContext))
//                oldExtContext = oldExtContext;
//        } else {
//            if(inferType == InferType.prevSame() && !BaseUtils.nullEquals(ExClassSet.fromEx(newValueClass),ExClassSet.fromEx(oldExtContext))) {
//                ResolveClassSet resNew = ExClassSet.fromEx(newValueClass);
//                ResolveClassSet resOld = ExClassSet.fromEx(oldExtContext);
//                if(!(resNew instanceof StringClass && resOld instanceof StringClass) && !(resOld instanceof NumericClass && resNew instanceof NumericClass))
//                    System.out.println((cnt++) + logCaption + " " + BaseUtils.nullToString(resOld) + " -> " + BaseUtils.nullToString(resNew));
//            }
//        }
//        if(inferType == InferType.prevSame() && !(BaseUtils.nullEquals(ExClassSet.fromEx(newValueClass), ExClassSet.fromEx(newDiffClassSet))))
//            newValueClass = newValueClass;
//        
//        return newValueClass;
    }
        
    private <P extends PropertyInterface> boolean containsAll(ImMap<T, ExClassSet> interfaceClasses, ImMap<T, ExClassSet> interfacePropClasses, boolean ignoreAbstracts) {
        return ExClassSet.containsAll(interfaces, interfaceClasses, interfacePropClasses, ignoreAbstracts);
    }
    private <P extends PropertyInterface> boolean intersect(ImMap<T, ExClassSet> interfaceClasses, ImMap<T, ExClassSet> interfacePropClasses) {
        return ExClassSet.intersect(interfaces, interfaceClasses, interfacePropClasses);
    }

    public <P extends PropertyInterface> void calcCheckContainsAll(String caseInfo, Property<P> property, ImRevMap<P, T> map, CalcClassType calcType, PropertyInterfaceImplement<T> value, String abstractInfo) {
        ClassWhere<T> classes = getClassWhere(calcType);
        ClassWhere<T> propClasses = new ClassWhere<>(property.getClassWhere(calcType),map);
        
        if(!propClasses.meansCompatible(classes)) {
            throw new ScriptParsingException("wrong signature of implementation " + caseInfo + "(" + this + ")" + " for abstract property " + abstractInfo + "\n\tspecified: " + propClasses + "\n\texpected : "  + classes);
        }
        
        AndClassSet valueClass = getValueClassSet();
        AndClassSet propValueClass = value.mapValueClassSet(propClasses);
        if(!valueClass.containsAll(propValueClass, false))
            throw new ScriptParsingException("wrong value class of implementation " + caseInfo + "(" + this + ")" + " for abstract property " + abstractInfo + 
                    "\n\tspecified: " + propValueClass + "\n\texpected : " + valueClass);
    }

    // assert что CaseUnion
    public <P extends PropertyInterface> void checkAllImplementations(ImList<Property<P>> props, ImList<ImRevMap<P, T>> maps) {
        AlgType.caseCheckType.checkAllImplementations(this, props, maps);
    }

    public <P extends PropertyInterface> void inferCheckAllImplementations(ImList<Property<P>> props, ImList<ImRevMap<P, T>> maps, InferType calcType) {
        ImMap<T, ExClassSet> classes = getInferInterfaceClasses(calcType);

        if(props.isEmpty())
            throw new ScriptParsingException("Property is not implemented : " + this + " (specified " + classes + ")");

        ImMap<T, ExClassSet> propClasses = null;
        for(int i=0,size=props.size();i<size;i++) {
            Property<P> prop = props.get(i);
            ImRevMap<P, T> map = maps.get(i);
            ImMap<T, ExClassSet> propCaseClasses = map.crossJoin(prop.getInferInterfaceClasses(calcType));
            if(propClasses == null)
                propClasses = propCaseClasses;
            else
                propClasses = ExClassSet.op(interfaces, propClasses, propCaseClasses, true);
        }

        if(!containsAll(propClasses, classes, true)) {
            throw new CaseUnionProperty.NotFullyImplementedException("Property is not fully implemented: " + this +  "\n\tCalculated: " + propClasses + "\n\tSpecified : " + classes, propClasses.toString(), classes.toString());
        }
    }

    public <P extends PropertyInterface> void calcCheckAllImplementations(ImList<Property<P>> props, ImList<ImRevMap<P, T>> maps, CalcClassType calcType) {
        ClassWhere<T> classes = getClassWhere(calcType);

        ClassWhere<T> propClasses = ClassWhere.FALSE();
        for (int i = 0, size = props.size(); i < size; i++) {
            Property<P> prop = props.get(i);
            ImRevMap<P, T> map = maps.get(i);
            propClasses = propClasses.or(new ClassWhere<>(prop.getClassWhere(calcType), map));
        }

        if (!classes.meansCompatible(propClasses)) {
            throw new CaseUnionProperty.NotFullyImplementedException("Property is not fully implemented: " + this +  "\n\tCalculated: " + propClasses + "\n\tSpecified : " + classes, propClasses.toString(), classes.toString());
        }
    }

    protected Property(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);

        drawOptions.addProcessor(new DefaultProcessor() {
            @Override
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form, Version version) {
                if(entity.viewType == null)
                    entity.viewType = ClassViewType.LIST;
            }

            @Override
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
            }
        });

    }

    public void change(ExecutionContext context, Boolean value) throws SQLException, SQLHandledException {
        change(context.getEnv(), value);
    }

    public void change(ExecutionContext context, String value) throws SQLException, SQLHandledException {
        change(MapFact.EMPTY(), context.getEnv(), value);
    }

    public void change(ExecutionEnvironment env, Boolean value) throws SQLException, SQLHandledException {
        change(MapFact.EMPTY(), env, value);
    }

    public void change(ExecutionEnvironment env, ObjectValue value) throws SQLException, SQLHandledException {
        change(MapFact.EMPTY(), env, value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, ObjectValue value) throws SQLException, SQLHandledException {
        getImplement().change(keys, env, value);
    }

    public void change(ImMap<T, DataObject> keys, ExecutionEnvironment env, Object value) throws SQLException, SQLHandledException {
        getImplement().change(keys, env, value);
    }

    public Pair<PropertyChangeTableUsage<T>, PropertyChangeTableUsage<T>> splitSingleApplyClasses(String debugInfo, PropertyChangeTableUsage<T> changeTable, SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        assert isStored();

        if(DataSession.notFitKeyClasses(this, changeTable)) // оптимизация
            return new Pair<>(createChangeTable(debugInfo+"-ssas:notfit"), changeTable);
        if(DataSession.fitClasses(this, changeTable))
            return new Pair<>(changeTable, createChangeTable(debugInfo+"-ssas:fit"));

        PropertyChange<T> change = PropertyChangeTableUsage.getChange(changeTable);

        // тут есть вот какая проблема в OrWhere implicitCast=false, и поэтому более узкий тип DataClass в and not не уйдет и упадет exception на getSource, вообще частично проблема решается fitClasses, но если есть и более узкий класс и верхние оптимизации не срабатывают бывает баг
        // соответственно проверяем только объектные классы
        ImMap<KeyField, Expr> mapKeys = mapTable.mapKeys.crossJoin(change.getMapExprs());
        Where classWhere = fieldClassWhere.getWhere(MapFact.addExcl(mapKeys, field, change.expr), true, IsClassType.CONSISTENT)
                .or(mapTable.table.getClasses().getWhere(mapKeys, true, IsClassType.CONSISTENT).and(change.expr.getWhere().not())); // или если меняет на null, assert что fitKeyClasses
        
        if(classWhere.isFalse()) // оптимизация
            return new Pair<>(createChangeTable(debugInfo+"-ssas:false"), changeTable);
        if(classWhere.isTrue())
            return new Pair<>(changeTable, createChangeTable(debugInfo+"-ssas:true"));

        OperationOwner owner = env.getOpOwner();
        try {
            PropertyChangeTableUsage<T> fit = readChangeTable(debugInfo+"-ssas:fit", sql, change.and(classWhere), baseClass, env);
            PropertyChangeTableUsage<T> notFit;
            try {
                notFit = readChangeTable(debugInfo+"-ssas:notfit", sql, change.and(classWhere.not()), baseClass, env);
            } catch (Throwable e) {
                fit.drop(sql, owner);
                throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
            }
            assert DataSession.fitClasses(this, fit);
            assert DataSession.fitKeyClasses(this, fit);

            // это была не совсем правильная эвристика, например если изменение было таблицей с классом X, а свойство принадлежность классу Y, то X and not Y превращался назад в X (если X не равнялся / наследовался от Y)
            // для того чтобы этот assertion продолжил работать надо совершенствовать ClassWhere.andNot, что пока нецелесообразность
            // assert DataSession.notFitClasses(this, notFit);
            return new Pair<>(fit,notFit);
        } finally {
            changeTable.drop(sql, owner);
        }
    }

    public boolean noOld() { // именно так, а не через getSessionCalcDepends, так как может использоваться до инициализации логики
        return getParseOldDepends().isEmpty();
    }
    @IdentityStartLazy
    public ImSet<OldProperty> getParseOldDepends() {
        MSet<OldProperty> mResult = SetFact.mSet();
        for(Property<?> property : getDepends(false))
            mResult.addAll(property.getParseOldDepends());
        return mResult.immutable();
    }
    @IdentityStrongLazy // используется много где
    public OldProperty<T> getOld(PrevScope scope) {
        return new OldProperty<>(this, scope);
    }

    @IdentityStrongLazy // используется в resolve и кое где еще
    public ChangedProperty<T> getChanged(IncrementType type, PrevScope scope) {
        return new ChangedProperty<>(this, type, scope);
    }

    public boolean noDB() {
        return !noOld();
    }

    protected Expr getVirtualTableExpr(ImMap<T, ? extends Expr> joinImplement, AlgType classType) {
        VirtualTable<T> virtualTable = getVirtualTable(classType);
        return virtualTable.join(virtualTable.mapFields.join(joinImplement)).getExpr(virtualTable.propValue);
    }

    @IdentityStrongLazy
    public VirtualTable<T> getVirtualTable(AlgType classType) {
        return new VirtualTable<>(this, classType);
    }

    public boolean usePrevHeur() {
        return getExpr(getMapKeys(), CalcClassType.prevSameKeepIS()).isNull();
    }

    public ImMap<T, ValueClass> calcInterfaceClasses(CalcClassType calcClassType) {
        return getClassWhere(calcClassType).getCommonParent(interfaces);
    }

    public ValueClass calcValueClass(CalcClassType classType) {
        return getClassValueWhere(classType).getCommonParent(SetFact.singleton("value")).get("value");
    }

    public ValueClass inferGetValueClass(InferType inferType) {
        ImMap<T, ExClassSet> inferred = getInferInterfaceClasses(inferType);
        if(inferred == null)
            return null;
        return ExClassSet.fromResolveValue(ExClassSet.fromEx(inferValueClass(inferred, inferType)));
    }

    @IdentityLazy
    public boolean isInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return ClassType.formPolicy.getAlg().isInInterface(this, interfaceClasses, isAny);
    }

    public boolean calcIsInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny, CalcClassType calcClassType) {
        ClassWhere<T> interfaceClassWhere = new ClassWhere<>(interfaceClasses);
        ClassWhere<T> fullClassWhere = getClassWhere(calcClassType); // вообще надо из CalcClassType вытянуть

        if(isAny)
            return !fullClassWhere.andCompatible(interfaceClassWhere).isFalse();
        else
            return interfaceClassWhere.meansCompatible(fullClassWhere);
    }

    public boolean inferIsInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny, InferType inferType) {
        ImMap<T, ExClassSet> exInterfaceClasses = ExClassSet.toEx(ResolveUpClassSet.toResolve((ImMap<T, AndClassSet>) interfaceClasses));
        ImMap<T, ExClassSet> inferredClasses = getInferInterfaceClasses(inferType);

        if(isAny)
            return intersect(inferredClasses, exInterfaceClasses);
        else
            return containsAll(inferredClasses, exInterfaceClasses, true); // тут вопрос с последним параметром, так как при false - A : C MULTI B : C пойдет в панель, с другой стороны при добавлении D : C поведение изменится
    }

    public ImMap<T, ValueClass> inferGetInterfaceClasses(InferType inferType, ExClassSet valueClasses) {
        ImMap<T, ExClassSet> inferred = getInferInterfaceClasses(inferType, valueClasses);
        if(inferred == null)
            return MapFact.EMPTY();
        return ExClassSet.fromExValue(inferred).removeNulls();
    }

    public boolean isChangedWhen(boolean toNull, PropertyInterfaceImplement<T> changeProperty) {
        if(toNull && changeProperty instanceof PropertyInterface && isNotNull(AlgType.actionType))
            return true;

        return getImplement().equalsMap(changeProperty);
    }
    public boolean isNot(PropertyInterfaceImplement<T> map) {
        return false;
    }

    public Pair<PropertyInterfaceImplement<T>, PropertyInterfaceImplement<T>> getIfProp() {
        return null;
    }

    public boolean hasNoGridReadOnly(ImSet<T> gridInterfaces) {
        assert interfaces.containsAll(gridInterfaces);
        return gridInterfaces.isEmpty();
    }

    public static class VirtualTable<P extends PropertyInterface> extends Table {

        protected String name;

        public final ImRevMap<KeyField, P> mapFields;
        public final PropertyField propValue;

        protected ClassWhere<KeyField> classes;
        private final ClassWhere<Field> propertyClass;

        public VirtualTable(final Property<P> property, AlgType algType) {
            super();

            name = property.getSID();
            
            ImRevMap<P, KeyField> revMapFields = property.interfaces.mapRevValues((P value) -> new KeyField(value.getSID(), property.getInterfaceType(value)));
            mapFields = revMapFields.reverse();
            keys = property.getOrderInterfaces().mapOrder(revMapFields);
            
            propValue = new PropertyField("value", property.getType());

            classes = property.getClassWhere(algType).remap(revMapFields);

            propertyClass = property.getClassValueWhere(algType).remap(MapFact.addRevExcl(revMapFields, "value", propValue));
        }

        public ClassWhere<KeyField> getClasses() {
            return classes;
        }

        @Override
        public ClassWhere<Field> getClassWhere(PropertyField property) {
            assert property.equals(propValue);
            return propertyClass;
        }

//        since there is a IdentityStrongLazy
        @Override
        public boolean calcTwins(TwinImmutableObject o) {
            return this == o;
        }

        @Override
        public int immutableHashCode() {
            return System.identityHashCode(this);
        }

        @IdentityLazy
        public TableStatKeys getTableStatKeys() {
            return ImplementTable.ignoreStatPropsNoException(() -> getStatKeys(this, 100));
        }

        @IdentityLazy
        public PropStat getStatProp(PropertyField property) {
            return ImplementTable.ignoreStatPropsNoException(() -> getStatProp(this, property));
        }

        public String toString() {
            return name;
        }

        public String getQuerySource(CompileSource source) {
            assert false; // should not be compiled in theory
            return name;
        }
    }

    // есть assertion, что не должен возвращать изменение null -> null, то есть или старое или новое не null, для подр. см usage
    @ThisMessage
    public PropertyChange<T> getIncrementChange(Modifier modifier) throws SQLException, SQLHandledException {
        return getIncrementChange(modifier.getPropertyChanges());
    }

    public PropertyChange<T> getIncrementChange(PropertyChanges propChanges) {
        IQuery<T, String> incrementQuery = getQuery(CalcType.EXPR, propChanges, PropertyQueryType.FULLCHANGED, MapFact.EMPTY());
        return new PropertyChange<>(incrementQuery.getMapKeys(), incrementQuery.getExpr("value"), incrementQuery.getExpr("changed").getWhere());
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

    public final Type.Getter<T> interfaceTypeGetter = this::getInterfaceType;
    public final Type.Getter<T> interfaceWhereTypeGetter(final ImMap<T, DataObject> mapDataValues) {
        return key -> {
            Type type = getWhereInterfaceType(key);
            if(type == null)
                type = mapDataValues.get(key).getType();
            return type;
        };
    }

    @IdentityInstanceLazy
    public ImRevMap<T, KeyExpr> getMapKeys() {
//        assert isFull();
        return KeyExpr.getMapKeys(interfaces);
    }

    @IdentityInstanceLazy
    public PropertyChange<T> getNoChange() {
        return new PropertyChange<>(getMapKeys(), CaseExpr.NULL());
    }
    
    private static class PropCorrelation<K extends PropertyInterface, T extends PropertyInterface> implements Correlation<T> {
        
        private final Property<K> property;
        private final T mapInterface;
        private final CustomClass customClass;

        public PropCorrelation(Property<K> property, T mapInterface, ClassType classType) {
//            assert property.isAggr(); // могут быть еще getImplements
            this.property = property;
            this.mapInterface = mapInterface;
            this.customClass = (CustomClass) property.getInterfaceClasses(classType).singleValue(); // тут логично читать класс aggr свойства, а не изменяемого (так как по идее класс может менять внутри класса aggr свойства и это разрешено, правда с точки зрения consistency (checkClasses, а пока customClass используется только там) не факт, но пока не принципиально) 
        }

        public Expr getExpr(ImMap<T, ? extends Expr> mapExprs) {
            return property.getExpr(MapFact.singleton(property.interfaces.single(), mapExprs.get(mapInterface)));
        }
        public Expr getExpr(ImMap<T, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException {
            return property.getExpr(MapFact.singleton(property.interfaces.single(), mapExprs.get(mapInterface)), modifier);
        }

        public Type getType() {
            return property.getType();
        }

        public Property<?> getProperty() {
            return property;
        }

        @Override
        public CustomClass getCustomClass() {
            return customClass;
        }
    }
    
    public boolean usesSession() {
        return false;
    }

    private ImOrderSet<T> getOrderInterfaces(ImSet<T> filterInterfaces) {
        return filterInterfaces == null ? getOrderInterfaces() : getOrderInterfaces().filterOrder(filterInterfaces);
    }
    @IdentityLazy
    private <K extends PropertyInterface> ImOrderSet<Correlation<T>> getCorrelations(ImSet<T> filterInterfaces, ClassType classType) {
        // берем классы свойства и докидываем все корреляции, которые найдем
        MOrderExclSet<Correlation<T>> mResult = SetFact.mOrderExclSet();

        ImMap<T, ValueClass> interfaceClasses = getInterfaceClasses(classType);
        ImOrderSet<T> orderInterfaces = getOrderInterfaces(filterInterfaces);
        for(int i=0,size=orderInterfaces.size();i<size;i++) { // для детерминированности на всякий случай
            T propertyInterface = orderInterfaces.get(i);
            ValueClass valueClass = interfaceClasses.get(propertyInterface);
            if(valueClass instanceof CustomClass) {
                CustomClass customClass = (CustomClass) valueClass;
                for(Property<K> aggrProp : customClass.getUpAggrProps())
//                    if(!BaseUtils.hashEquals(aggrProp, this)) // себя тоже надо коррелировать (в общем-то для удаления, как правило удаляется тоже объекты агрегированные в один объект)
                    mResult.exclAdd(new PropCorrelation<>(aggrProp, propertyInterface, classType));
            }
        }

        return mResult.immutableOrder();
    }

    public <K extends PropertyInterface> ImOrderSet<Correlation<T>> getCorrelations(ClassType classType) {
        return getCorrelations(null, classType);
    }    

    public PropertyChangeTableUsage<T> createChangeTable(String debugInfo) {
        return new PropertyChangeTableUsage<>(getCorrelations(ClassType.materializeChangePolicy), getTableDebugInfo(debugInfo), getOrderInterfaces(), interfaceTypeGetter, getType());
    }
    
    public NoPropertyWhereTableUsage<T> createWhereTable(String debugInfo, ImSet<T> filterInterfaces, ImMap<T, DataObject> mapDataValues) {
        return new NoPropertyWhereTableUsage<>(getCorrelations(filterInterfaces, ClassType.wherePolicy), getTableDebugInfo(debugInfo), getOrderInterfaces(filterInterfaces), interfaceWhereTypeGetter(mapDataValues)); // тут по идее можно typeGetter сверху протянуть (как было раньше), но так по идее эффективнее (с точки зрения того что getKeyType не надо выполнять, хотя и не принципиально)  
    }

    @StackMessage("{message.increment.read.properties}")
    @ThisMessage
    public PropertyChangeTableUsage<T> readChangeTable(String group, SQLSession session, Modifier modifier, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        return readFixChangeTable(group, session, getIncrementChange(modifier), baseClass, env);
    }

    public PropertyChangeTableUsage<T> readFixChangeTable(String group, SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        PropertyChangeTableUsage<T> readTable = readChangeTable(group, session, change, baseClass, env);

        // при вызове readChangeTable, используется assertion (см. assert fitKeyClasses) что если таблица подходит по классам для значения, то подходит по классам и для ключей
        // этот assertion может нарушаться если определилось конкретное значение и оно было null, как правило с комбинаторными event'ами (вообще может нарушиться и если не null, но так как propertyClasses просто вырезаются то не может), соответственно необходимо устранить этот случай
        readTable.fixKeyClasses(getClassWhere(ClassType.materializeChangePolicy));
        readTable.checkClasses(session, null, SessionTable.nonead, env.getOpOwner()); // нужен как раз для проверки fixKeyClasses

        return readTable;
    }
    
    public PropertyChange<T> getPrevChange(PropertyChangeTableUsage<T> table, PropertyChanges prevChanges) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        return new PropertyChange<>(mapKeys, getExpr(mapKeys, prevChanges), table.join(mapKeys).getWhere());
    }

    public PropertyChangeTableUsage<T> readChangeTable(String debugInfo, SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        PropertyChangeTableUsage<T> changeTable = createChangeTable(debugInfo);
        change.writeRows(changeTable, session, baseClass, env, SessionTable.matGlobalQuery);
        return changeTable;
    }

    @IdentityStrongLazy // just in case
    private <P extends PropertyInterface> ConstraintCheckChangeProperty<T, P> getConstraintCheckChangeProperty(Property<P> change) {
        return new ConstraintCheckChangeProperty<>(this, change);
    }

    public enum CheckType { CHECK_NO, CHECK_ALL, CHECK_SOME }
    public CheckType checkChange = CheckType.CHECK_NO;
    public ImSet<Property<?>> checkProperties = null;

    public <O extends ObjectSelector> ImSet<ContextFilterEntity<?, T, O>> getCheckFilters(O object) {
        return ThreadLocalContext.getBusinessLogics().getCheckConstrainedProperties(this).filterFn(property -> depends(property, this)).
                mapSetValues(property -> {
                    ConstraintCheckChangeProperty<?, T> changeProperty = ((Property<?>) property).getConstraintCheckChangeProperty(this);
                    Pair<ImRevMap<ConstraintCheckChangeProperty.Interface<T>, T>, ConstraintCheckChangeProperty.Interface<T>> mapInterfaces = changeProperty.getMapInterfaces();
                    return new ContextFilterEntity<>(changeProperty, mapInterfaces.first, MapFact.singletonRev(mapInterfaces.second, object));
                });
    }

    public PropertyChanges getChangeModifier(PropertyChanges changes, boolean toNull) {
        // строим Where для изменения
        return getPullDataChanges(changes, toNull).getPropertyChanges().add(changes);
    }

    private ImSet<Property> recDepends;
    @ManualLazy
    public ImSet<Property> getRecDepends() {
        if(recDepends == null)
            recDepends = calculateRecDepends();
        return recDepends;
    }

    public int getEstComplexity() {
        return getRecDepends().size();
    }

    public ImSet<Property> calculateRecDepends() {
        MSet<Property> mResult = SetFact.mSet();
        for(Property<?> depend : getDepends())
            mResult.addAll(depend.getRecDepends());
        mResult.add(this);
        return mResult.immutable();
    }

    public ImSet<Property> calculateRecDepends(boolean events) {
        MSet<Property> mResult = SetFact.mSet();
        for(Property<?> depend : getDepends(events))
            mResult.addAll(depend.calculateRecDepends(events));
        mResult.add(this);
        return mResult.immutable();
    }

    private MCol<Pair<ActionOrProperty<?>, LinkType>> actionChangeProps; // только у Data и IsClassProperty, чисто для лексикографики
    public <T extends PropertyInterface> void addActionChangeProp(Pair<Action<T>, LinkType> pair) {
        pair.first.checkRecursiveStrongUsed(this);

        if(actionChangeProps==null)
            actionChangeProps = ListFact.mCol();
        actionChangeProps.add(BaseUtils.immutableCast(pair));
    }
    public ImCol<Pair<ActionOrProperty<?>, LinkType>> getActionChangeProps() {
        if(actionChangeProps!=null)
            return actionChangeProps.immutableCol();

        return SetFact.EMPTY();
    }
    public void dropActionChangeProps() {
        actionChangeProps = null;
    }

    protected ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events) {
        MCol<Pair<ActionOrProperty<?>, LinkType>> mResult = ListFact.mCol();

        for(Property depend : getDepends(events))
            mResult.add(new Pair<ActionOrProperty<?>, LinkType>(depend, LinkType.DEPEND));

        return mResult.immutableCol();
    }

    protected void fillDepends(MSet<Property> depends, boolean events) {
    }

    // возвращает от чего "зависят" изменения - с callback'ов, должен коррелировать с getDepends(Rec), который должен включать в себя все calculateUsedChanges
    public ImSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return SetFact.EMPTY();
    }

    public ImSet<Property> getDepends(boolean events) {
        MSet<Property> mDepends = SetFact.mSet();
        fillDepends(mDepends, events);
        return mDepends.immutable();
    }

    public ImSet<Property> getDepends() {
        return getDepends(true);
    }

    private Boolean complex; // also forces preread for value and params
    public void setComplex(Boolean complex) {
        this.complex = complex;
    }
    private boolean preread;
    public void setPreread(boolean preread) {
        this.preread = preread;
    }

    // we use this check instead of isPreread when we've already calculated expr (and all prereads are executed, so we need to ignore them)
    public boolean isComplex() {  
        return complex != null && complex;
    }
    public boolean isPreread() {
        return preread || isComplex();
    }
    @IdentityLazy
    public boolean isOrDependsComplex() {
        if(complex != null)
            return complex;

        for(Property property : getDepends())
            if(property.isOrDependsComplex())
                return true;
        return false;
    }

    public boolean hasPreread(Modifier modifier) throws SQLException, SQLHandledException {
        return hasPreread(modifier.getPropertyChanges());
    }
    public boolean hasPreread(PropertyChanges propertyChanges) {
        return hasPreread(propertyChanges.getStruct());
    }
    public boolean hasPreread(StructChanges structChanges) {
        if(!hasGlobalPreread())
            return false;

        if(Settings.get().isDisableExperimentalFeatures())
            return true;

        return aspectHasPreread(structChanges);
    }
    public boolean aspectHasPreread(StructChanges structChanges) {
        if(isPreread())
            return true;

        // should be consistent with aspectGetExpr
        if(isStored()) {
            ChangeType usedChange = structChanges.getUsedChange(this);
            if((usedChange != null && usedChange.isFinal()) || !hasChanges(structChanges))
                return false; // no hint will be "thrown" (since it requires reading'an expr)
        }

        return calculateHasPreread(structChanges);
    }

    protected boolean calculateHasPreread(StructChanges structChanges) {
        return false;
    }

    public boolean hasGlobalPreread() {
        return hasGlobalPreread(true);
    }

    @IdentityLazy
    public boolean hasGlobalPreread(boolean events) {
        if(isPreread())
            return true;

        return calculateHasGlobalPreread(events);
    }

    protected boolean calculateHasGlobalPreread(boolean events) {
        return false;
    }

    public Boolean hint;
    public void setHint(Boolean hint) {
        this.hint = hint;
    }
    public boolean isHint() {
        return hint != null && hint;
    }
    @IdentityLazy
    public boolean isNoHint() {
        if(hint != null)
            return !hint;

        for(Property property : getDepends())
            if(property.isNoHint())
                return true;
        return false;
    }

    public static byte SET = 2;
    public static byte DROPPED = 1;
    public static byte getSetDropped(boolean setOrDropped) {
        return setOrDropped ? SET : DROPPED;
    }

    private final AddValue<Property, Byte> addSetOrDropped = new SymmAddValue<Property, Byte>() {
        public Byte addValue(Property key, Byte prevValue, Byte newValue) {
            return (byte)(prevValue | newValue); // раньше and был, но тогда получалась проблема что если есть и SET и DROP, а скажем DROP "идет за" каким-то FINAL изменением, кэши getUsedChanges не совпадают (см. logCaches)
        }
    };

    @IdentityLazy
    public ImMap<Property, Byte> getSetOrDroppedDepends() {
        ImSet<SessionProperty> sessionDepends = getSessionCalcDepends(true); // нужны и вычисляемые события, так как в логике вычислений (getExpr) используется
        MMap<Property, Byte> mResult = MapFact.mMap(addSetOrDropped);
        for(int i=0,size=sessionDepends.size();i<size;i++) {
            SessionProperty property = sessionDepends.get(i);
            if(property instanceof ChangedProperty) {
                ChangedProperty changed = (ChangedProperty) property;
                Boolean setOrDropped = changed.getSetOrDropped();
                if(setOrDropped != null)
                    mResult.add(changed.property, getSetDropped(setOrDropped));
            }
        }
        ImMap<Property, Byte> result = mResult.immutable();
        assert getRecDepends().containsAll(result.keys());
        return result;
    }

    @IdentityLazy
    public ImSet<CurrentEnvironmentProperty> getEnvDepends() {
        return BaseUtils.immutableCast(getRecDepends().filterFn(element -> element instanceof CurrentEnvironmentProperty));
    }

    @IdentityStartLazy
    public ImSet<SessionProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionProperty> mResult = SetFact.mSet();
        for(Property<?> property : getDepends(events)) // derived'ы в общем то не интересуют так как используется в singleApply
            mResult.addAll(property.getSessionCalcDepends(events));
        return mResult.immutable();
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(ImMap<T, DataObject> mapValues, ImMap<T, ConcreteClass> mapClasses) {
        return (CustomClass)getValueClass(ClassType.editValuePolicy);
/*        Map<T, Expr> mapExprs = new HashMap<T, Expr>();
        for (Map.Entry<T, DataObject> keyField : mapColValues.entrySet())
            mapExprs.put(keyField.getKey(), new ValueExpr(keyField.getValue().object, mapClasses.get(keyField.getKey())));
        return (CustomClass) new Query<String, String>(new HashMap<String, KeyExpr>(), getClassExpr(mapExprs), "value").
                getClassWhere(Collections.singleton("value")).getSingleWhere("value").getOr().getCommonClass();*/
    }

    public boolean hasChanges(Modifier modifier) throws SQLException, SQLHandledException {
        return hasChanges(modifier, false);
    }
    public boolean hasChanges(Modifier modifier, boolean prevChanges) throws SQLException, SQLHandledException {
        PropertyChanges propertyChanges = modifier.getPropertyChanges();
        if(prevChanges)
            propertyChanges = propertyChanges.getPrev();
        return hasChanges(propertyChanges);
    }
    public boolean hasChanges(PropertyChanges propChanges) {
        return hasChanges(propChanges.getStruct());
    }
    public boolean hasChanges(StructChanges propChanges) {
        return propChanges.hasChanges(getUsedChanges(propChanges));
    }

    public ImSet<Property> getUsedChanges(StructChanges propChanges) {
        if(propChanges.isEmpty()) // чтобы рекурсию разбить
            return SetFact.EMPTY();

        ChangeType modifyChanges = propChanges.getUsedChange(this);
        if(modifyChanges!=null)
            return SetFact.add(SetFact.singleton(this), modifyChanges.isFinal() ? SetFact.EMPTY() : getUsedChanges(propChanges.remove(this)));

        return calculateUsedChanges(propChanges);
    }

    protected Expr aspectCalculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert (AlgType.useCalcForStored && calcType == CalcClassType.prevBase()) || ImplementTable.checkStatProps(null);
        return calculateExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    protected abstract Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere);

    // чтобы не было рекурсии так сделано
    public Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType) {
        return aspectCalculateExpr(joinImplement, calcType, PropertyChanges.EMPTY, null);
    }

    public static <T extends PropertyInterface> ImMap<T, Expr> getJoinValues(ImMap<T, ? extends Expr> joinImplement) {
        return ((ImMap<T, Expr>)joinImplement).filterFnValues(AbstractOuterContext::isValue);
    }

    public static <T extends PropertyInterface> ImMap<T, Expr> onlyComplex(ImMap<T, ? extends Expr> joinImplement) { //assert все Expr.isValue
        return ((ImMap<T, Expr>)joinImplement).filterFnValues(joinExpr -> !(joinExpr instanceof ValueExpr) && !joinExpr.isNull());
    }

    public Expr aspectGetExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        assert joinImplement.size() == interfaces.size();

        ModifyChange<T> modify = propChanges.getModify(this);
        if(modify!=null) {
            if(isPreread()) { // вообще rightJoin, но вдруг случайно мимо AutoHint'а может пройти
                ImMap<T, Expr> joinValues = getJoinValues(joinImplement); Pair<ObjectValue, Boolean> row;
                if(joinValues!=null && (row = modify.preread.readValues.get(new Pair<>(joinValues, hasChanges(propChanges))))!=null) {
                    if(changedWhere!=null) changedWhere.add(row.second ? Where.TRUE() : Where.FALSE());
                    return row.first.getExpr();
                }

                joinImplement = MapFact.override(joinImplement, ObjectValue.getMapExprs(onlyComplex(joinValues).innerJoin(modify.preread.readParams)));
            }

            WhereBuilder changedExprWhere = new WhereBuilder();
            Expr changedExpr = modify.change.getExpr(joinImplement, changedExprWhere);
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), getExpr(joinImplement, calcType, modify.isFinal ? PropertyChanges.EMPTY : propChanges.remove(this), modify.isFinal ? null : changedWhere));
        }

        // we need it before isStored (and use isMarkedStored), to use in InitStoredTask regular list (not graph)
        if(calcType instanceof CalcClassType && (isMarkedStored() || isClassVirtualized((CalcClassType) calcType))) {
            return getVirtualTableExpr(joinImplement, (CalcClassType)calcType);
        }

        // modify == null;
        if(isStored()) {
            if(!hasChanges(propChanges))
                return getStoredExpr(joinImplement);
            if(useSimpleIncrement()) {
                WhereBuilder changedExprWhere = new WhereBuilder();
                Expr changedExpr = aspectCalculateExpr(joinImplement, calcType, propChanges, changedExprWhere);
                if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
                return changedExpr.ifElse(changedExprWhere.toWhere(), getPrevExpr(joinImplement, calcType, propChanges));
            }
        }

        if(calcType.isStatAlot() && explicitClasses != null && this instanceof AggregateProperty && !hasAlotKeys() && getType() != null) {
            assert SystemProperties.lightStart;
            assert !hasChanges(propChanges) && modify == null;
            return getVirtualTableExpr(joinImplement, AlgType.statAlotType); // тут собственно смысл в том чтобы класс брать из сигнатуры и не высчитывать
        }                    

        return aspectCalculateExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    protected Expr getStoredExpr(ImMap<T, ? extends Expr> joinImplement) {
        return mapTable.table.join(mapTable.mapKeys.crossJoin(joinImplement)).getExpr(field);
    }

    public Table.Join.Expr getInconsistentExpr(ImMap<T, ? extends Expr> joinImplement, BaseClass baseClass) {
        DBTable table = baseClass.getInconsistentTable(mapTable.table);
        return (Table.Join.Expr) table.join(mapTable.mapKeys.crossJoin(joinImplement)).getExpr(field);
    }

    public MapKeysTable<T> mapTable; // именно здесь потому как не обязательно persistent
    public PropertyField field;
    public ClassWhere<Field> fieldClassWhere;

    public boolean aggProp;

    public boolean isMarkedStored() {
        return markedStored;
    }
    
    private boolean markedStored;

    public void markStored(ImplementTable table) {
        markedStored = true;

        if (table != null) {
            ImOrderMap<T, ValueClass> keyClasses = getOrderTableInterfaceClasses(AlgType.storedResolveType);
            if(interfaces.size() == 1) { // optimization + hack
                mapTable = new MapKeysTable<>(table, MapFact.singletonRev(interfaces.single(), table.keys.single()));
                assert this instanceof ClassDataProperty || mapTable.equals(table.getMapKeysTable(keyClasses)); // for classdataprops it's a hack, because there can be really different classes inside and their commonParent (in interfaceClasses) can be really different from full table (explicit table for class dataprops)   
            } else {
                mapTable = table.getMapKeysTable(keyClasses);
                assert mapTable != null; // in theory there should be an error table of classes that can't be mapped (it's checked just before markStored)
            }
        }
    }

    public String mapDbName;

    public String getDBName() {
        return field.getName();
    }

    public void initStored(TableFactory tableFactory, DBNamingPolicy policy) {
        if(mapTable == null)
            mapTable = tableFactory.getMapTable(getOrderTableInterfaceClasses(AlgType.storedResolveType), policy);

        String dbName = mapDbName != null ? mapDbName : policy.transformActionOrPropertyCNToDBName(this.canonicalName);

        PropertyField field = new PropertyField(dbName, getType());
        fieldClassWhere = getClassWhere(mapTable, field);
//        if(!fieldClassWhere.filterKeys(mapTable.table.getTableKeys()).meansCompatible(mapTable.table.getClasses()))
//            field = field;
        checkDuplicateFieldNames(field);
        mapTable.table.addField(field, fieldClassWhere);

        this.field = field;
    }

    public static class DuplicateFieldNameException extends RuntimeException {
        DuplicateFieldNameException(String message) {
            super(message);
        }
    }

    private void checkDuplicateFieldNames(PropertyField addedField) {
        assert addedField.getName() != null;
        final String formatStr = "Field '%s' was already added to '%s' table. The reason might be that the field " +
                "names are limited in length, and as a result two different canonical names are converted " +
                "to the same field name due to length truncation. In this case you can either change " +
                "the property name(s) or change the naming policy (see documentation for details)";

        for (PropertyField field : mapTable.table.properties) {
            if (addedField.getName().equals(field.getName())) {
                throw new DuplicateFieldNameException(String.format(formatStr, addedField.getName(), mapTable.table.toString()));
            }
        }
    }

    public void markIndexed(final ImRevMap<T, String> mapping, ImList<PropertyObjectInterfaceImplement<String>> index, IndexType indexType) {
        assert isStored();

        ImList<Field> indexFields = index.mapListValues((PropertyObjectInterfaceImplement<String> indexField) -> {
            if (indexField instanceof PropertyObjectImplement) {
                String key = ((PropertyObjectImplement<String>) indexField).object;
                return mapTable.mapKeys.get(mapping.reverse().get(key));
            } else {
                Property property = ((PropertyRevImplement) indexField).property;
                assert BaseUtils.hashEquals(mapTable.table, property.mapTable.table);
                return property.field;
            }
        });
        mapTable.table.addIndex(indexFields.toOrderExclSet(), indexType);
    }

    public AndClassSet getValueClassSet() {
        return getClassValueWhere(ClassType.resolvePolicy).getCommonClass("value");
    }
    
    // для resolve'а
    public ResolveClassSet getResolveClassSet(ImMap<T, ResolveClassSet> classes) {
        ExClassSet set = inferValueClass(ExClassSet.toEx(classes), InferType.resolve());
        if(set != null && set.isEmpty())
            return null;
        return ExClassSet.fromEx(set);
    }

    @IdentityLazy
    public ClassWhere<T> getClassWhere(ClassType type) {
        return getClassValueWhere(type).filterKeys(interfaces); // не полностью, собсно для этого и есть full
    }

    public ClassWhere<Object> getClassValueWhere(final ClassType type) {
        return classToAlg(type, arg -> {
            if(AlgType.checkInferCalc) checkInferClasses(type);
            return getClassValueWhere(arg);
        });
    }

    @IdentityLazy
    public ImMap<T, ValueClass> getInterfaceClasses(ClassType type) {
        return getInterfaceClasses(type, null);
    }

    public ImOrderMap<T, ValueClass> getOrderTableInterfaceClasses(AlgType type) {
        return getOrderInterfaces().mapOrderMap(getInterfaceClasses(type));
    }

    public ImMap<T, ValueClass> getInterfaceClasses(ClassType type, final ExClassSet valueClasses) {
        return classToAlg(type, arg -> getInterfaceClasses(arg, valueClasses));
    }

    public ImMap<T, ValueClass> getInterfaceClasses(AlgType type) {
        return getInterfaceClasses(type, null);
    }

    public ImMap<T, ValueClass> getInterfaceClasses(AlgType type, final ExClassSet valueClasses) {
        return type.getInterfaceClasses(this, valueClasses);
    }

    public Type getType() {
        ValueClass valueClass = getValueClass(ClassType.typePolicy);
        return valueClass != null ? valueClass.getType() : null;
    }

    public ValueClass getValueClass(ClassType classType) {
        return classToAlg(classType, this::getValueClass);
    }

    @IdentityLazy
    public ValueClass getValueClass(AlgType arg) {
        return arg.getValueClass(this);
    }
    
    // для assertion'а в основном
    @StackMessage("{message.core.property.get.classes}")
    @ThisMessage
    protected <V> V classToAlg(ClassType type, CallableWithParam<AlgType, V> call) {
        boolean assertFull = false;
        if(type == ClassType.ASSERTFULL_NOPREV) {
            type = ClassType.useInsteadOfAssert;
            assertFull = true;
        }

        AlgType algType = type.getAlg();
//        assert !assertFull || isFull(algType.getAlgInfo());
        return call.call(algType);
    }

    @IdentityLazy
    public ClassWhere<T> getClassWhere(AlgType type) {
        return getClassValueWhere(type).filterKeys(interfaces); // не полностью, собсно для этого и есть full
    }

    public ClassWhere<Object> getClassValueWhere(AlgType algType) {
        return algType.getClassValueWhere(this);
    }

    @IdentityStartLazy
    private boolean checkInferClasses(ClassType type) {
        if(this instanceof NullValueProperty)
            return true;

        if(type == ClassType.ASSERTFULL_NOPREV)
            type = ClassType.useInsteadOfAssert;

        CalcClassType calcType = type.getCalc();
        InferType inferType = type.getInfer();
//        if(calcType == CalcClassType.prevSame() && getParseOldDepends().size() == 0) { // PREVSAME классовые свойства подменяет, а там есть очень сложные свойства
//            calcType = CalcType.PREVBASE;
//            inferType = InferType.prevBase();
//        }
            
        ClassWhere<Object> calc = calcClassValueWhere(calcType);
        ClassWhere<Object> inferred = inferClassValueWhere(inferType);
        ImSet<Object> fullInterfaces = calc.getFullInterfaces();
        ClassWhere<Object> cinferred = inferred.filterKeys(fullInterfaces);
        if(!(calc.means(cinferred, false) && cinferred.means(calc, false)) &&
                !calc.toString().equals("{value - Строка (bp) 2000}")
//                !calc.toString().contains("PriceRound_RoundCondition") &&
//                !(calc.toString().contains("System_Object") && getParseOldDepends().size() > 0) //&& 
//                !(calc.getAnds().length > 1 && inferred.getAnds().length==1)
                ) {
            if(!BaseUtils.hashEquals(calc.getCommonParent(fullInterfaces), cinferred.getCommonParent(fullInterfaces))) {
                System.out.println(this + ", CALC : " + calc + ", INF : " + cinferred);
                return false;
            }
        }
        return true;
    }

    @IdentityStartLazy
    private boolean checkInferNotNull(ImSet<T> checkInterfaces) {
        boolean calcNotNull = calcNotNull(checkInterfaces, CalcClassType.prevBase());
        boolean inferNotNull = inferNotNull(checkInterfaces, InferType.prevBase());
        if(calcNotNull != inferNotNull) {
            System.out.println(this + " NOTNULL, CALC : " + calcNotNull + ", INF : " + inferNotNull);
            return false;
        }
        return true;
    }

    @IdentityStartLazy
    private boolean checkInferEmpty() {
        boolean calcEmpty = calcEmpty(CalcClassType.prevBase());
        boolean inferEmpty = inferEmpty(InferType.prevBase());
        if(calcEmpty != inferEmpty) {
            System.out.println(this + " EMPTY, CALC : " + calcEmpty + ", INF : " + inferEmpty);
            return false;
        }
        return true;
    }

    @IdentityStartLazy
    private boolean checkInferFull(ImCol<T> checkInterfaces) {
        boolean calcFull = calcFull(checkInterfaces, CalcClassType.prevBase());
        boolean inferFull = inferFull(checkInterfaces, InferType.prevBase());
        if(calcFull != inferFull) {
            System.out.println(this + " FULL, CALC : " + calcFull + ", INF : " + inferFull);
            return false;
        }
        return true;
    }

    protected boolean isClassVirtualized(CalcClassType calcType) {
        return false;
    }

    public abstract ClassWhere<Object> calcClassValueWhere(CalcClassType calcType);

    private static final Checker<ExClassSet> checker = (expl, calc) -> {
        ResolveClassSet resExpl = ExClassSet.fromEx(expl);
        ResolveClassSet resCalc = ExClassSet.fromEx(calc);
        if(resExpl == null)
            return resCalc == null;
        if(resCalc == null)
            return false;
        
        AndClassSet explAnd = resExpl.toAnd();
        AndClassSet calcAnd = resCalc.toAnd();
        return explAnd.containsAll(calcAnd, false) && calcAnd.containsAll(explAnd, false);
    };

    public ClassWhere<Object> inferClassValueWhere(final InferType inferType) {
        return inferClassValueWhere(inferType, null);
    }

    @IdentityStartLazy
    public ClassWhere<Object> inferClassValueWhere(final InferType inferType, final ExClassSet valueClasses) {
        // если prevBase и есть PREV'ы не используем explicitClasses
        ImMap<T, ExClassSet> inferred = getInferInterfaceClasses(inferType, valueClasses);
        if(inferred == null)
            return ClassWhere.FALSE();
        
        ExClassSet valueCommonClass = inferValueClass(inferred, inferType);
        if (valueCommonClass != null && valueCommonClass.isEmpty()) {
            return ClassWhere.FALSE();
        }
        return new ClassWhere<>(ResolveUpClassSet.toAnd(MapFact.<Object, ResolveClassSet>addExcl(ExClassSet.fromEx(inferred), "value", ExClassSet.fromEx(valueCommonClass))).removeNulls());
    }

    protected static <T extends PropertyInterface> ImMap<T, ExClassSet> getInferExplicitCalcInterfaces(ImSet<T> interfaces, boolean noOld, InferType inferType, ImMap<T, ResolveClassSet> explicitInterfaces, Callable<ImMap<T,ExClassSet>> calcInterfaces, String caption, ActionOrProperty property, Checker<ExClassSet> checker) {
        assert inferType != InferType.resolve();
        return getExplicitCalcInterfaces(interfaces, (inferType == InferType.prevBase() && !noOld) || explicitInterfaces == null ? null : ExClassSet.toEx(explicitInterfaces), calcInterfaces, caption, property, checker);
    }

    private ImMap<T, ExClassSet> getInferInterfaceClasses(final InferType inferType) {
        return getInferInterfaceClasses(inferType, null);
    }

    private ImMap<T, ExClassSet> getInferInterfaceClasses(final InferType inferType, final ExClassSet valueClasses) {
        return getInferExplicitCalcInterfaces(interfaces, noOld(), inferType, explicitClasses, () -> inferInterfaceClasses(valueClasses, inferType).finishEx(inferType), "CALC ", this, checker);
    }

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere(AlgType.storedType).remap(MapFact.addRevExcl(mapTable.mapKeys, "value", storedField)); //
    }

    public ObjectValue readLazyClasses(SQLSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, ChangesController changesController) throws SQLException, SQLHandledException {
        return readLazyClasses(session, keys, modifier, false, changesController);
    }
    public ObjectValue readLazyClasses(SQLSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, boolean prevChanges, ChangesController changesController) throws SQLException, SQLHandledException {
        String annotation = this.annotation;
        if(annotation != null && annotation.equals("lazy") && !hasChanges(modifier, prevChanges) && !session.isInTransaction())
            return changesController.readLazyValue(this, keys);
        if(this instanceof SessionDataProperty && !hasChanges(modifier, prevChanges))
            return NullValue.instance;
        return null;
    }

    public Pair<ObjectValue, Boolean> readClassesChanged(SQLSession session, ImMap<T, ObjectValue> keys, BaseClass baseClass, Modifier modifier, boolean hasChanges, QueryEnvironment env, ChangesController changesController) throws SQLException, SQLHandledException {
        ObjectValue lazyValue = readLazyClasses(session, keys, modifier, !hasChanges, changesController);
        if(lazyValue != null)
            return new Pair<>(lazyValue, false);

        String readValue = "readvalue"; String readChanged = "readChanged";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(SetFact.EMPTY());
        WhereBuilder changedWhere = new WhereBuilder();
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier, !hasChanges, changedWhere));
        readQuery.addProperty(readChanged, ValueExpr.get(changedWhere.toWhere()));
        ImMap<Object, ObjectValue> result = readQuery.executeClasses(session, env, baseClass).singleValue();
        return new Pair<>(result.get(readValue), !result.get(readChanged).isNull());
    }

    public ObjectValue readClasses(SQLSession session, ImMap<T, ? extends ObjectValue> keys, BaseClass baseClass, Modifier modifier, QueryEnvironment env, ChangesController changesController) throws SQLException, SQLHandledException {
        ObjectValue lazyValue = readLazyClasses(session, keys, modifier, changesController);
        if(lazyValue != null)
            return lazyValue;

        return readClasses(session, keys, baseClass, modifier, env);
    }

    public ObjectValue readClasses(SQLSession session, ImMap<T, ? extends ObjectValue> keys, BaseClass baseClass, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(SetFact.EMPTY());
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier));
        return readQuery.executeClasses(session, env, baseClass).singleValue().get(readValue);
    }

    public Object read(SQLSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env, ChangesController changesController) throws SQLException, SQLHandledException {
        ObjectValue lazyValue = readLazyClasses(session, keys, modifier, changesController);
        if(lazyValue != null)
            return lazyValue.getValue();

        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(SetFact.EMPTY());
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier));
        return readQuery.execute(session, env).singleValue().get(readValue);
    }

    public ImMap<ImMap<T, Object>, Object> readAll(SQLSession session, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(interfaces);
        Expr expr = getExpr(readQuery.getMapExprs(), modifier);
        readQuery.addProperty(readValue, expr);
        readQuery.and(expr.getWhere());
        return readQuery.execute(session, env).getMap().mapValues(ImMap::singleValue);
    }

    public ImMap<ImMap<T, DataObject>, DataObject> readAllClasses(SQLSession session, Modifier modifier, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(interfaces);
        Expr expr = getExpr(readQuery.getMapExprs(), modifier);
        readQuery.addProperty(readValue, expr);
        readQuery.and(expr.getWhere());
        return readQuery.executeClasses(session, env, baseClass).getMap().mapValues(value -> (DataObject)value.singleValue());
    }

    public ObjectValue readClasses(ExecutionContext context) throws SQLException, SQLHandledException {
        return readClasses(context.getEnv());
    }
    public ObjectValue readClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return readClasses(env.getSession(), MapFact.EMPTY(), env.getModifier(), env.getQueryEnv());
    }

    public Object read(ExecutionEnvironment env, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return read(env.getSession(), keys, env.getModifier(), env.getQueryEnv());
    }

    public ObjectValue readClasses(FormInstance form, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return readClasses(form.session, keys, form.getModifier(), form.getQueryEnv());
    }
    
    public ObjectValue readClasses(ExecutionContext context, ImMap<T, ? extends ObjectValue> keys) throws SQLException, SQLHandledException {
        return readClasses(context.getSession(), keys, context.getModifier(), context.getQueryEnv());
    }

    public ObjectValue readClasses(DataSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        return readClasses(session.sql, keys, session.baseClass, modifier, env, session.changes);
    }

    public Object read(DataSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        return read(session.sql, keys, modifier, env, session.changes);
    }

    public ImMap<ImMap<T, Object>, Object> readAll(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return readAll(env.getSession().sql, env.getModifier(), env.getQueryEnv());
    }
    public ImMap<ImMap<T, DataObject>, DataObject> readAllClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return readAllClasses(env.getSession().sql, env.getModifier(), env.getQueryEnv(), env.getSession().baseClass);
    }

    // используется для оптимизации - если Stored то попытать использовать это значение
    protected abstract boolean useSimpleIncrement();

    public PropertyChanges getUsedDataChanges(PropertyChanges propChanges, CalcDataType type) {
        return propChanges.filter(getUsedDataChanges(propChanges.getStruct(), type));
    }

    public ImSet<Property> getUsedDataChanges(StructChanges propChanges, CalcDataType type) {
        return calculateUsedDataChanges(propChanges, type);
    }

    public DataChanges getDataChanges(PropertyChange<T> change, Modifier modifier) throws SQLException, SQLHandledException {
        return getDataChanges(change, modifier.getPropertyChanges());
    }

    public DataChanges getDataChanges(PropertyChange<T> change, PropertyChanges propChanges) {
        return getDataChanges(change, propChanges, null);
    }

    public ImSet<DataProperty> getChangeProps() { // дублирует getDataChanges, но по сложности не вытягивает нижний механизм
//        Map<T, KeyExpr> mapKeys = getMapKeys();
//        return getDataChanges(new PropertyChange<T>(mapKeys, toNull ? CaseExpr.NULL() : changeExpr, CompareWhere.compare(mapKeys, getChangeExprs())), changes, null);
        return SetFact.EMPTY();
    }

    protected DataChanges getPullDataChanges(PropertyChanges changes, boolean toNull) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<>(mapKeys, toNull ? CaseExpr.NULL() : getChangeExpr(), CompareWhere.compare(mapKeys, getChangeExprs())), CalcDataType.PULLEXPR, changes, null);
    }

    public DataChanges getJoinDataChanges(ImMap<T, ? extends Expr> implementExprs, Expr expr, Where where, GroupType groupType, PropertyChanges propChanges, CalcDataType type, WhereBuilder changedWhere) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        DataChanges result = getDataChanges(new PropertyChange<>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, groupType, mapKeys),
                GroupExpr.create(implementExprs, where, mapKeys).getWhere()),
                type, propChanges, changedImplementWhere);
        if (changedWhere != null)
            changedWhere.add(new Query<>(mapKeys, changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    public DataChanges getDataChanges(PropertyChange<T> change, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getDataChanges(change, CalcDataType.EXPR, propChanges, changedWhere);
    }

    @StackMessage("{message.core.property.data.changes}")
    @PackComplex
    @ThisMessage
    public DataChanges getDataChanges(PropertyChange<T> change, CalcDataType type, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (change.where.isFalse()) // оптимизация
            return DataChanges.EMPTY;

        return calculateDataChanges(change, type, changedWhere, propChanges);
    }

    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges, CalcDataType type) {
        return SetFact.EMPTY();
    }

    // для оболочки чтобы всем getDataChanges можно было бы timeChanges вставить
    protected DataChanges calculateDataChanges(PropertyChange<T> change, CalcDataType type, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return DataChanges.EMPTY;
    }

    public ImMap<T, Expr> getChangeExprs() {
        return interfaces.mapValues((Function<T, Expr>) PropertyInterface::getChangeExpr);
    }

    // actually it is strong lazy
    @NFLazy
    public Expr getChangeExpr() {
        if(changeExpr == null)
            changeExpr = new PullExpr(-128);
        return changeExpr;
    }
    public Expr changeExpr;

    public <D extends PropertyInterface, W extends PropertyInterface> void setWhenChange(LogicsModule lm, Event actionEvent, PropertyInterfaceImplement<T> valueImplement, PropertyMapImplement<W, T> whereImplement) {
        if(actionEvent != null) {
            ActionMapImplement<?, T> setAction = PropertyFact.createSetAction(interfaces, getImplement(), valueImplement);
            lm.addCheckPrevWhenAction(interfaces, setAction, whereImplement, MapFact.EMPTYORDER(), false, actionEvent, SetFact.EMPTY(), false, null, null);
            return;
        }

        if(!whereImplement.property.noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, ChangeEvent.scope);

        ChangeEvent<T> event = new ChangeEvent<>(this, valueImplement, whereImplement);
        // запишем в DataProperty
        for(DataProperty dataProperty : getChangeProps()) {
            if(Settings.get().isCheckUniqueEvent() && dataProperty.event!=null)
                throw new RuntimeException(ThreadLocalContext.localize(LocalizedString.createFormatted("{logics.property.already.has.event}", dataProperty)));
            dataProperty.event = event;
        }
    }

    public void setNotNull(ImMap<T, DataObject> values, ExecutionEnvironment env, ExecutionStack stack, boolean notNull, boolean check) throws SQLException, SQLHandledException {
        if(!check || (read(env, values)!=null) != notNull) {
            ActionMapImplement<?, T> action = getSetNotNullAction(notNull);
            if(action!=null)
                action.execute(new ExecutionContext<>(values, env, stack));
        }
    }
    public void setNotNull(ImRevMap<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull, ExecutionStack stack) throws SQLException, SQLHandledException {
        for(ImMap<T, DataObject> row : new Query<>(mapKeys, where).executeClasses(env).keys())
            setNotNull(row, env, stack, notNull, true);
    }

    public DataObject getDefaultDataObject() {
        Type type = getType();
        if(type instanceof DataClass) {
            return ((DataClass) type).getDefaultObjectValue();
        } else
            return null;
    }

    public ActionMapImplement<?, T> getSetNotNullAction(boolean notNull) {
        if(notNull) {
            ObjectValue defaultValue = getDefaultDataObject();
            if(defaultValue != null) {
                StaticClass objectClass = (StaticClass) ((DataObject) defaultValue).objectClass;
                return PropertyFact.createSetAction(interfaces, getImplement(), PropertyFact.createStatic(PropertyFact.getValueForProp(defaultValue.getValue(), objectClass), objectClass));
            }
            return null;
        } else
            return PropertyFact.createSetAction(interfaces, getImplement(), PropertyFact.createNull());
    }

    protected boolean assertPropClasses(CalcType calcType, PropertyChanges changes, WhereBuilder changedWhere) {
        return calcType.isExpr() || (changes.isEmpty() && changedWhere==null);
    }

    public PropertyMapImplement<T, T> getImplement() {
        return new PropertyMapImplement<>(this, getIdentityInterfaces());
    }

    public <V> PropertyImplement<T, V> getSingleImplement(V map) {
        return new PropertyImplement<T, V>(this, MapFact.singleton(interfaces.single(), map));
    }

    public <V extends PropertyInterface> PropertyMapImplement<T, V> getImplement(ImOrderSet<V> list) {
        return new PropertyMapImplement<>(this, getMapInterfaces(list));
    }

    public <X extends PropertyInterface> PropertyMapImplement<?, X> getIdentityImplement(ImRevMap<T, X> mapping) {
        return new PropertyMapImplement<>(this, mapping);
    }

    @IdentityLazy
    public boolean canBeGlobalChanged() { // есть не Local'ы changed
        return canBeChanged(true);
    }
    public boolean canBeHeurChanged(boolean global) {
        return false;
    }
    public boolean canBeChanged(boolean global) {
        
        if(Settings.get().isUseHeurCanBeChanged())
            return canBeHeurChanged(global); // в ОЧЕНЬ не большом количестве случаев отличается (а разница в производительности огромная), можно было бы для SecurityManager сделать отдельную ветку (там критична скорость), но пока особого смысла нет, так как разница не большая 
        
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        Modifier modifier = defaultModifier;
        try {
            Expr changeExpr = getChangeExpr(); // нижнее условие по аналогии с DataProperty
            Where classWhere = getClassProperty().mapExpr(mapKeys, modifier).getWhere();
            PropertyRevImplement<?, String> valueProperty = getValueClassProperty();
            if(valueProperty != null)
                classWhere = classWhere.and(valueProperty.mapExpr(MapFact.singleton("value", changeExpr), modifier).getWhere());
            DataChanges dataChanges = getDataChanges(new PropertyChange<>(mapKeys, changeExpr, classWhere), modifier);
            if(global)
                return !dataChanges.isGlobalEmpty();
            else
                return !dataChanges.isEmpty();
        } catch (SQLException e) { // по идее не должно быть, но на всякий случай
            return false;
        } catch (SQLHandledException e) { // по идее не должно быть
            return false;
        }
    }

    public static boolean isDefaultWYSInput(ValueClass valueClass) {
        return valueClass instanceof StringClass;
    }

    private boolean checkViewObjectEvent(ValueClass valueClass, Supplier<Property<?>> viewProperty, ValueUniqueType uniqueType) {
        if (!(valueClass instanceof CustomClass && viewProperty != null))
            return true;

        // optimization of using the Supplier, since isValueFullAndUnique can be rather heavy
        Property<?> property = viewProperty.get();

        //to avoid edit on dblclick
        ValueClass viewValueClass = property.getValueClass(ClassType.editValuePolicy);
        if(viewValueClass instanceof TextClass || viewValueClass instanceof AJSONClass)
            return false;

        if(!Settings.get().isOnlyUniqueObjectEvents())
            return true;

        return property.isValueUnique(MapFact.EMPTY(), uniqueType); // optimistic because otherwise all properties will become readonly
    }

    // needed for 2 purposes: a) optimization b) "setting boolean view filter" for the GROUP CONCAT
    public interface SelectProperty<T extends PropertyInterface> {
        PropertyMapImplement<?, T> get(boolean filterSelected);
    }

    public static class Select<T extends PropertyInterface> {
        public final SelectProperty<T> property;

        public final ImList<InputPropertyValueList> values;

        public final Pair<Integer, Integer> stat; // estimate stat
        public final boolean multi;
        public final boolean html;

        public final boolean notNull;

        public Select(SelectProperty<T> property, Pair<Integer, Integer> stat, ImList<InputPropertyValueList> values, boolean multi, boolean html, boolean notNull) {
            this.property = property;
            this.stat = stat;
            this.values = values;
            this.multi = multi;
            this.html = html;
            this.notNull = notNull;
        }
    }

    @IdentityStrongLazy
    public <I extends PropertyInterface, V extends PropertyInterface, W extends PropertyInterface> Select<T> getSelectProperty(ImList<Property> viewProperties, boolean forceSelect) {
        if(!forceSelect && !canBeChanged(false)) // optimization
            return null; // ? because sometimes can be used to display one of the option

        BaseLogicsModule baseLM = getBaseLM();

        ValueClass valueClass = getValueClass(ClassType.editValuePolicy);

        Property<V> viewProperty;
        if(valueClass instanceof CustomClass && !viewProperties.isEmpty() &&
                ((viewProperty = (Property<V>) PropertyFact.createViewProperty(viewProperties).property).isValueUnique(MapFact.EMPTY(), ValueUniqueType.SELECT) || forceSelect)) {

//            viewProperty or listProperty in InputListEntity => viewProperty ???
//          dialogForm or InputContextSelector
//          this or oldValue => this ??? this надо

            ImSet<T> innerInterfaces = interfaces;
            InputPropertyListEntity<V, T> viewListEntity = new InputPropertyListEntity<>(viewProperty, MapFact.EMPTYREV());
            // assert viewListEntity.orders.isEmpty();
            PropertyMapImplement<T, T> value = getImplement();

            ClassFormSelector formSelector = new ClassFormSelector((CustomClass) valueClass, false);
            Pair<InputFilterEntity<?, T>, ImOrderMap<InputOrderEntity<?, T>, Boolean>> filterAndOrders =
                    new FormInputContextSelector<>(formSelector, getCheckFilters(formSelector.virtualObject), formSelector.virtualObject, MapFact.EMPTYREV()).getFilterAndOrders();

            return getSelectProperty(forceSelect, innerInterfaces, viewListEntity, filterAndOrders.first, filterAndOrders.second, value, false);
        }

        return null;
    }

    public static <T extends PropertyInterface, I extends PropertyInterface> Select<T> getSelectProperty(boolean forceSelect, ImSet<T> innerInterfaces, InputPropertyListEntity<?, T> viewListEntity, InputFilterEntity<?, T> where, ImOrderMap<InputOrderEntity<?, T>, Boolean> orders, PropertyInterfaceImplement<T> value, boolean drawnValue) {
        BaseLogicsModule baseLM = ThreadLocalContext.getBaseLM();

        boolean isNotNull;
        CustomClass customClass;
        if(drawnValue) {
            isNotNull = value.mapIsDrawNotNull();
            customClass = null;
        } else {
            isNotNull = value.mapIsNotNull();
            customClass = (CustomClass) value.mapValueClass(ClassType.editValuePolicy);
        }

        // generation this interfaces + object
        ImRevMap<T, I> mapPropertyInterfaces = innerInterfaces.mapRevValues(() -> (I)new PropertyInterface());
        I objectInterface = (I) new PropertyInterface();

        // name = viewProperty(o)
        PropertyMapImplement<?, I> name = viewListEntity.getProperty(mapPropertyInterfaces, objectInterface);
        // selected = (o = this (x, y, z))
        PropertyMapImplement<PropertyInterface, I> selected = PropertyFact.<I>createCompare(value.map(mapPropertyInterfaces), drawnValue ? name : objectInterface, Compare.EQUALS);

        // FILTER / ORDER
        // there are 2 options : add WHERE to the IntegrationFormEntity, add it to JSONProperty context filters
        // the first option looks "cleaner" (since we need the external context anyway)
        PropertyMapImplement<?, I> mappedWhere = where.map(mapPropertyInterfaces).getWhereProperty(objectInterface);
        ImOrderMap<PropertyMapImplement<?, I>, Boolean> mappedOrders = orders.mapOrderKeys(order -> order.map(mapPropertyInterfaces).getOrderProperty(objectInterface));

        // isFull is checked in the isValueUnique
        return getSelectProperty(baseLM, false, isNotNull, forceSelect, mapPropertyInterfaces, mapPropertyInterfaces.valuesSet().addExcl(objectInterface), name, selected, customClass, mappedWhere, mappedOrders);
    }

    public static <I extends PropertyInterface, T extends PropertyInterface, W extends PropertyInterface>
            Select<T> getSelectProperty(BaseLogicsModule baseLM, boolean multi, boolean notNull, boolean forceSelect, ImRevMap<T, I> mapPropertyInterfaces, ImSet<I> innerInterfaces, PropertyMapImplement<?, I> name, PropertyInterfaceImplement<I> selected, CustomClass customClass, PropertyMapImplement<W, I> where, ImOrderMap<? extends PropertyInterfaceImplement<I>, Boolean> orders) {

        boolean fallbackToFilterSelected = multi || forceSelect;

        ImSet<I> innerMapInterfaces = mapPropertyInterfaces.valuesSet();
        ImSet<W> mapWhereInterfaces = where.mapping.filterValuesRev(innerMapInterfaces).keys();

        if(multi) {
            if(!where.property.isValueFull(mapWhereInterfaces)) // otherwise we'll get "incorrect operation" when reading values
                return null;
        } else
            assert where.property.isValueFull(mapWhereInterfaces); // isValueUnique checks this
        Stat whereStat = where.property.getInterfaceStat(mapWhereInterfaces);
        int whereCount = whereStat.getCount();

        boolean hasAlotValues = whereCount > Settings.get().getMaxInterfaceStatForValueDropdown();
        if(!fallbackToFilterSelected && hasAlotValues) // optimization
            return null;

        InputContextPropertyListEntity readContextEntity = null;
        if(!hasAlotValues) {
            Property readValuesProperty = null;
            if (mapWhereInterfaces.isEmpty())
                readValuesProperty = where.property;
            else if(customClass != null) {
                IsClassProperty classProperty = customClass.getProperty();
                Stat classStat = classProperty.getInterfaceStat(false); // customClass.getUpSet().getCount() could be used instead
                if(classStat.lessEquals(whereStat))
                    readValuesProperty = classProperty;
            }

            if(readValuesProperty != null) {
                InputPropertyListEntity readEntity = new InputPropertyListEntity(name.property, MapFact.EMPTYREV());
                if(!multi)
                    readContextEntity = readEntity.merge(new Pair<>(new InputFilterEntity<>(readValuesProperty, MapFact.EMPTYREV()), MapFact.EMPTYORDER()));
                else {
                    assert readValuesProperty == name.property;
                    readContextEntity = new InputContextPropertyListEntity(readEntity);
                }
            }
        }

        Type nameType = name.property.getType();
        return new Select<>(filterSelected -> {
            if(filterSelected && !fallbackToFilterSelected)
                return null;

            return getSelectProperty(baseLM, mapPropertyInterfaces, innerInterfaces, name, selected, filterSelected, where, orders);
        }, new Pair<>(nameType.getAverageCharLength() * whereCount, whereCount), readContextEntity != null ? ListFact.singleton(readContextEntity.map()) : null, multi, nameType instanceof HTMLStringClass || nameType instanceof HTMLTextClass, notNull);
    }

    private static <I extends PropertyInterface, T extends PropertyInterface, W extends PropertyInterface> PropertyMapImplement<?, T> getSelectProperty(BaseLogicsModule baseLM, ImRevMap<T, I> mapPropertyInterfaces, ImSet<I> innerInterfaces, PropertyMapImplement<?, I> name, PropertyInterfaceImplement<I> selected, boolean filterSelected, PropertyMapImplement<W, I> where, ImOrderMap<? extends PropertyInterfaceImplement<I>, Boolean> orders) {
        if(filterSelected)
            where = (PropertyMapImplement<W, I>) PropertyFact.createAnd(where, selected);
        else
            where = (PropertyMapImplement<W, I>) PropertyFact.createUnion(innerInterfaces, PropertyFact.createNotNull(where), selected); // assert that selected is boolean (but maybe createUnionNotNull should be used)

        ImSet<I> innerMapInterfaces = mapPropertyInterfaces.valuesSet();
        LogicsModule.IntegrationForm<I> integrationForm = getSelectForm(baseLM, innerInterfaces, null, innerMapInterfaces, name, selected, where, orders, true);

        LP<?> jsonProp = baseLM.addFinalJSONFormProp(LocalizedString.NONAME, integrationForm);

        return jsonProp.getImplement(integrationForm.getOrderInterfaces(mapPropertyInterfaces));
    }

    public static <I extends PropertyInterface, W extends PropertyInterface> LogicsModule.IntegrationForm<I> getSelectForm(BaseLogicsModule baseLM, ImSet<I> innerInterfaces, ImMap<I, ValueClass> innerClasses, ImSet<I> innerMapInterfaces, PropertyMapImplement<?, I> name, PropertyInterfaceImplement<I> selected, PropertyMapImplement<W, I> where, ImOrderMap<? extends PropertyInterfaceImplement<I>, Boolean> orders, boolean needObjects) {
        ImOrderSet<I> orderMapInterfaces = innerMapInterfaces.toOrderSet(); // getOrderInterfaces().mapOrder(mapPropertyInterfaces);
        // CLASSES
//            ImList<ValueClass> classes = null; //getInterfaceClasses(ClassType.tryEditPolicy) + customClass;

        // JSON
        MList<PropertyInterfaceImplement<I>> mProperties = ListFact.mList();
        MList<ScriptingLogicsModule.IntegrationPropUsage> mPropUsages = ListFact.mList();

        mProperties.add(selected);
        mPropUsages.add(new ScriptingLogicsModule.IntegrationPropUsage<>("selected", false, (LP)null, null));

        mProperties.add(name);
        mPropUsages.add(new ScriptingLogicsModule.IntegrationPropUsage<>("name", false, (LP)null, null));

        if(needObjects) {
            // x, y, z, o
            for (I orderInterface : innerInterfaces.removeIncl(innerMapInterfaces)) {
                mProperties.add(orderInterface);
                mPropUsages.add(new ScriptingLogicsModule.IntegrationPropUsage(null, false, (LP) null, null, baseLM.objectsGroup));
            }
        }

        // ORDERS
        MOrderExclMap<String, Boolean> mPropOrders = MapFact.mOrderExclMap();
        for(int i = 0, size = orders.size(); i < size; i++) {
            mProperties.add(orders.getKey(i));
            String orderId = "order" + i;
            mPropUsages.add(new ScriptingLogicsModule.IntegrationPropUsage(orderId, false, (LP) null, null));
            mPropOrders.exclAdd(orderId, orders.getValue(i));
        }
        ImOrderMap<String, Boolean> propOrders = mPropOrders.immutableOrder();

        ImList<PropertyInterfaceImplement<I>> properties = mProperties.immutableList();
        ImList<ScriptingLogicsModule.IntegrationPropUsage> propUsages = mPropUsages.immutableList();

        ImOrderSet<I> orderInterfaces = orderMapInterfaces.addOrderExcl(innerInterfaces.removeIncl(innerMapInterfaces).toOrderSet());
        ImList<ValueClass> orderClasses = null;
        if(innerClasses != null)
            orderClasses = orderInterfaces.mapList(innerClasses);
        return baseLM.addFinalIntegrationForm(orderInterfaces, orderClasses, orderMapInterfaces, properties, propUsages, propOrders, where);
    }
    @IdentityStrongLazy // STRONG for using in security policy
    public ActionMapImplement<?, T> getDefaultEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {

        ActionMapImplement<?, T> joinDefaultEventAction = getJoinDefaultEventAction(eventActionSID, defaultChangeEventScope, viewProperties, customChangeFunction);
        if(joinDefaultEventAction != null)
            return joinDefaultEventAction;

        // we want "value unique join edit object" to have "higher priority" than "interface edit object"
        if (eventActionSID.equals(ServerResponse.EDIT_OBJECT) && interfaces.size() == 1) {
            T singleInterface = interfaces.single();
            ValueClass interfaceClass = getInterfaceClasses(ClassType.tryEditPolicy).get(singleInterface);

            if(!checkViewObjectEvent(interfaceClass, () -> PropertyFact.createViewProperty(viewProperties.addList(this)).property, ValueUniqueType.EDIT))
                return null;

            if(interfaceClass != null) {
                LA<?> defaultOpenAction = interfaceClass.getDefaultOpenAction(getBaseLM());
                if (defaultOpenAction != null)
                    return defaultOpenAction.getImplement(singleInterface);
            }
        }

        return null;
    }

    public ActionMapImplement<?, T> getJoinDefaultEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {
        if (eventActionSID.equals(ServerResponse.CHANGE) && !canBeChanged(false)) // optimization
            return null;

        BaseLogicsModule lm = getBaseLM();

        Supplier<Property<?>> viewProperty = !viewProperties.isEmpty() ? () -> PropertyFact.createViewProperty(viewProperties).property : null;

        ValueClass valueClass = getValueClass(ClassType.editValuePolicy);

        boolean isEdit = eventActionSID.equals(ServerResponse.EDIT_OBJECT);

        if(!checkViewObjectEvent(valueClass, viewProperty, isEdit ? ValueUniqueType.EDIT : ValueUniqueType.DIALOG))
            return null;

        if(isEdit) {
            if(valueClass != null) {
                LA<?> defaultOpenAction = valueClass.getDefaultOpenAction(lm);
                if(defaultOpenAction != null)
                    return PropertyFact.createJoinAction(defaultOpenAction.action, getImplement());
            }

            return null;
        } else {
            assert eventActionSID.equals(ServerResponse.CHANGE);

            LP targetProp = lm.getRequestedValueProperty(valueClass);

            // target prop will be used to change this property
            boolean notNull = isNotNull();

            ActionMapImplement<?, T> action;
            if (valueClass instanceof CustomClass) {
                InputPropertyListEntity<?, T> list = viewProperty != null ? new InputPropertyListEntity<>(viewProperty.get(), MapFact.EMPTYREV()) : null;

                // DIALOG LIST valueCLass INPUT object=property(...) CONSTRAINTFILTER
                LP<T> lp = new LP<>(this);
                ImOrderSet<T> orderInterfaces = lp.listInterfaces; // actually we don't need all interfaces in dialog input action itself (only used one in checkfilters), but for now it doesn't matter

                // selectors could be used, but since this method is used after logics initialization, getting form, check properties here is more effective
                LA<?> inputAction = lm.addDialogInputAProp((CustomClass) valueClass, targetProp, BaseUtils.nvl(defaultChangeEventScope, PropertyDrawEntity.DEFAULT_CUSTOMCHANGE_EVENTSCOPE), orderInterfaces, list, objectEntity -> getCheckFilters(objectEntity), customChangeFunction, notNull, MapFact.EMPTYREV());

                action = ((LA<?>) lm.addJoinAProp(inputAction, BaseUtils.add(directLI(lp), getUParams(orderInterfaces.size())))).getImplement(orderInterfaces);
            } else {
                // INPUT valueCLass
                action = lm.addDataInputAProp((DataClass) valueClass, targetProp, false, this, SetFact.EMPTYORDER(),
                        null, null, BaseUtils.nvl(defaultChangeEventScope, PropertyDrawEntity.DEFAULT_DATACHANGE_EVENTSCOPE), ListFact.EMPTY(), customChangeFunction, notNull).getImplement();
            }

            ActionMapImplement<?, T> result = PropertyFact.createRequestAction(interfaces,
                    // adaptive canBeChanged, to provide better ergonomics for abstracts
                    PropertyFact.createListAction(interfaces, PropertyFact.createCheckCanBeChangedAction(interfaces, getImplement()), action),
                    PropertyFact.createSetAction(interfaces, getImplement(), targetProp.getImplement()), null);// INPUT scripted input generates FOR, but now it's not important

            setResetAsync(result);

            return result;
        }
    }


    public boolean tooMuchSelectData(ImMap<T, StaticParamNullableExpr> fixedExprs) {
        return !getSelectCost(fixedExprs).rows.less(new Stat(Settings.get().getAsyncValuesMaxReadDataCompletionCount()));
    }

    private <X extends PropertyInterface> void setResetAsync(ActionMapImplement<X, T> action) {
        PropertyFact.setResetAsync(action.action, new AsyncMapChange<>(new PropertyMapImplement<>(this, action.mapping.reverse()), null, null, null));
    }

    public boolean userNotNull;
    public boolean notNull;

    @Override
    public boolean isDrawNotNull() {
        return isNotNull();
    }
    public boolean isNotNull() {
        return notNull;
    }

    public boolean disableInputList;

    protected ActionOrPropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping) {
        return new PropertyClassImplement<>(this, classes, mapping);
    }

    public boolean autoset;

    public static ValueClass op(ValueClass v1, ValueClass v2, boolean or) {
        if(v1==null)
            return v2;
        if(v2==null)
            return v1;
        OrClassSet or1 = v1.getUpSet().getOr();
        OrClassSet or2 = v2.getUpSet().getOr();
        OrClassSet orResult;
        if(or)
            orResult = or1.or(or2);
        else
            orResult = or1.and(or2);
        return orResult.getCommonClass();
    }

    public static <T extends PropertyInterface> Inferred<T> op(ImList<Inferred<T>> operands, boolean or, InferType inferType) {
        Inferred<T> result = or ? Inferred.FALSE() : Inferred.EMPTY();
        for(int i=0,size=operands.size();i<size;i++) {
            Inferred<T> operandInferred = operands.get(i);
            if (i == 0)
                result = operandInferred;
            else
                result = result.op(operandInferred, or, inferType);
        }
        return result;
    }

    public static <I, T extends PropertyInterface> Inferred<T> op(ImMap<I, PropertyInterfaceImplement<T>> operands, final ImMap<I, ExClassSet> operandClasses, ImSet<I>[] operandNotNulls, final InferType inferType) {
        ImMap<I, Inferred<T>> inferred = mapInfer(operands, operandClasses, inferType);
        return op(operandNotNulls, inferType, inferred);
    }

    public static <I, T extends PropertyInterface> ImMap<I, Inferred<T>> mapInfer(ImMap<I, PropertyInterfaceImplement<T>> operands, final ImMap<I, ExClassSet> operandClasses, final InferType inferType) {
        return operands.mapValues((key, value) -> value.mapInferInterfaceClasses(operandClasses.get(key), inferType));
    }
    
    private static <I, T extends PropertyInterface> Inferred<T> op(ImSet<I>[] operandNotNulls, InferType inferType, ImMap<I, Inferred<T>> inferred) {
        Inferred<T> result = Inferred.FALSE();
        for (int i = 0; i < operandNotNulls.length; i++) {
            ImSet<I> opNotNull = operandNotNulls[i];
            
            Inferred<T> andInferred = Inferred.EMPTY();
            for(int j = 0, size = opNotNull.size(); j < size; j++) {
                Inferred<T> opInferred = inferred.get(opNotNull.get(j));
                if(j == 0)
                    andInferred = opInferred;
                else
                    andInferred = andInferred.and(opInferred, inferType);
            }
            
            if(i == 0)
                result = andInferred;
            else
                result = result.or(andInferred, inferType);
        }

        // add those that don't participate in any notNull
        return result.and(op(inferred.remove(SetFact.mergeSets(operandNotNulls)).values().toList().mapListValues(value -> value.orAny()), false, inferType), inferType);
    }

    public static <T extends PropertyInterface> Inferred<T> op(ImList<PropertyInterfaceImplement<T>> operands, ImList<ExClassSet> operandClasses, int operandNotNullCount, int skipNotNull, InferType inferType, boolean or) {
        ImSet<Integer>[] operandNotNulls;        
        if(or) { // мн-во singleton'ов
            operandNotNulls = new ImSet[operandNotNullCount];
            for(int i=0;i<operandNotNullCount;i++)
                if(i!=skipNotNull)
                    operandNotNulls[i] = SetFact.singleton(i);
        } else {
            operandNotNulls = new ImSet[1];
            MExclSet<Integer> mSet = SetFact.mExclSetMax(operandNotNullCount);
            for(int i=0;i<operandNotNullCount;i++)
                if(i!=skipNotNull)
                    mSet.exclAdd(i);
            operandNotNulls[0] = mSet.immutable();            
        }
        return op(operands.toIndexedMap(), operandClasses.toIndexedMap(), operandNotNulls, inferType);
    }

    // раздельно вычисляет классы по каждому параметру (то есть для каждого параметра отдельно вычисляет его класс, как если остальных бы не было)
    // плюс для prev'ов предполагается что классов объеков не меняются, not'ы не учитываются
    // можно было бы попробовать использовать общий механизм, но там выделить одноместную логику классов гораздо сложнее, поэтому оставим такой механизм
    // вся эта логика по сути дублирует логику plugin'а
    @IdentityStartLazy
    public Inferred<T> inferInterfaceClasses(InferType inferType) {
        return inferInterfaceClasses(null, inferType);
    }
    // make inferInterfaceClasses using explicitClasses (now it works different from plugin) but however it's not evident how it can cause problems in practice
    @IdentityStartLazy
    public Inferred<T> inferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return calcInferInterfaceClasses(commonValue, inferType);
    }
    protected abstract Inferred<T> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType);
    // optimization really important for applyCompared
    @IdentityStartLazy
    public boolean needInferredForValueClass(InferType inferType) {
        return calcNeedInferredForValueClass(inferType);
    }
    @IdentityStartLazy
    public ExClassSet inferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        return calcInferValueClass(inferred, inferType);
    }
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return true;
    } 
    protected abstract ExClassSet calcInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType);
    public static <I extends PropertyInterface> boolean opNeedInferForValueClass(ImCol<? extends PropertyInterfaceImplement<I>> props, InferType inferType) {
        for (int i = 0; i < props.size(); i++) {
            if(props.get(i).mapNeedInferredForValueClass(inferType))
                return true;
        }
        return false;
    }
    public static <I extends PropertyInterface> ExClassSet opInferValueClasses(ImCol<? extends PropertyInterfaceImplement<I>> props, ImMap<I, ExClassSet> inferred, boolean or, InferType inferType) {
        ExClassSet result = null;
        for (int i = 0; i < props.size(); i++) {
            ExClassSet classSet = props.get(i).mapInferValueClass(inferred, inferType);
            if (i == 0)
                result = classSet;
            else
                result = ExClassSet.op(result, classSet, or);
        }
        return result;
    }

    public ImSet<Property> getSetUsedChanges(PropertyChanges propChanges) {
        return getUsedChanges(propChanges.getStruct());
    }

    public PropertyChanges getUsedChanges(PropertyChanges propChanges) {
        return propChanges.filter(getSetUsedChanges(propChanges));
    }

    @PackComplex
    @StackMessage("{message.core.property.get.expr}")
    @ThisMessage
    public IQuery<T, String> getQuery(CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(calcType, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            QueryBuilder<T, String> fullQuery = new QueryBuilder<>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.addProperty("value", newExpr);
            
            Expr dbExpr = getPrevExpr(fullQuery.getMapExprs(), calcType, propChanges);
            Where fullWhere = newExpr.getWhere().or(dbExpr.getWhere());
            if(!DBManager.PROPERTY_REUPDATE && isStored())
                fullWhere = fullWhere.and(newExpr.compare(dbExpr, Compare.EQUALS).not());            

            fullQuery.addProperty("changed", query.getExpr("changed").and(fullWhere));
            return fullQuery.getQuery();
        }

        QueryBuilder<T, String> query = new QueryBuilder<>(getMapKeys().removeRev(interfaceValues.keys()));
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
                    return Expr.NULL();
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

    @StackMessage("{message.core.property.get.expr}")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    protected PropertyChanges getPrevPropChanges(PropertyChanges propChanges) {
        return getPrevPropChanges(CalcType.EXPR, propChanges);
    }
    protected PropertyChanges getPrevPropChanges(CalcType calcType, PropertyChanges propChanges) {
        return PropertyChanges.PREVEXPR(calcType, propChanges);
    }
    public Expr getPrevExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges) {
        return getExpr(joinImplement, calcType, getPrevPropChanges(calcType, propChanges), null);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, CalcType.EXPR);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType) {
        return getExpr(joinImplement, calcType, PropertyChanges.EMPTY, null);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier) throws SQLException, SQLHandledException {
        return getExpr(joinImplement, modifier, null);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier, WhereBuilder changedWhere) throws SQLException, SQLHandledException {
        return getExpr(joinImplement, modifier, false, changedWhere);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, Modifier modifier, boolean prevChanges, WhereBuilder changedWhere) throws SQLException, SQLHandledException {
        PropertyChanges propertyChanges = modifier.getPropertyChanges();
        if(prevChanges)
            propertyChanges = propertyChanges.getPrev();
        return getExpr(joinImplement, propertyChanges, changedWhere);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return getExpr(joinImplement, propChanges, null);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getExpr(joinImplement, CalcType.EXPR, propChanges, changedWhere);
    }

    // в будущем propClasses можно заменить на PropertyTables propTables
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if (isNotNull(calcType.getAlgInfo()) && (Settings.get().isUseQueryExpr() || Query.getMapKeys(joinImplement)!=null))
            return getQueryExpr(joinImplement, calcType, propChanges, changedWhere);
        else
            return getJoinExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    @Override
    public void prereadCaches() {
        super.prereadCaches();
        getRecDepends();
        if(isNotNull(CalcType.EXPR.getAlgInfo()))
            getQuery(CalcType.EXPR, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, MapFact.EMPTY()).pack();
    }

    public PropertyMapImplement<?, T> getClassProperty() {
        return getClassProperty(interfaces);
    }

    @IdentityInstanceLazy
    public PropertyMapImplement<?, T> getClassProperty(ImSet<T> interfaces) {
        return IsClassProperty.getMapProperty(getInterfaceClasses(ClassType.signaturePolicy).filter(interfaces));
    }

    @IdentityInstanceLazy
    protected PropertyRevImplement<?, String> getValueClassProperty() {
        ValueClass valueClass = getValueClass(ClassType.signaturePolicy);
        if(valueClass instanceof ConcatenateValueClass) // getClassProperty not supported
            return null;
        if(valueClass == null)
            return null;
        return IsClassProperty.getProperty(valueClass, "value");
    }

    public boolean isFull(ImCol<T> checkInterfaces, AlgInfoType algType) {
        if(AlgType.checkInferCalc) checkInferFull(checkInterfaces);
        return algType.isFull(this, checkInterfaces);
    }

    public boolean isNotNull(ImSet<T> checkInterfaces, AlgInfoType algType) {
        if(isFull(checkInterfaces, algType))
            return true;

        if(AlgType.checkInferCalc) checkInferNotNull(checkInterfaces);
        return algType.isNotNull(checkInterfaces, this);
    }


    @IdentityLazy
    public boolean inferFull(ImCol<T> checkInterfaces, InferInfoType inferType) {
        return inferInterfaceClasses(inferType).isFull(checkInterfaces, inferType);
    }

    @IdentityLazy
    public boolean calcFull(ImCol<T> checkInterfaces, CalcInfoType calcType) {
        return calcClassValueWhere(calcType).isFull(checkInterfaces);
    }

    public boolean supportsDrillDown() {
        return false;
    }

    public DrillDownFormEntity getDrillDownForm(BaseLogicsModule LM) {
        DrillDownFormEntity drillDown = createDrillDownForm(LM);
        if (drillDown != null) {
            LM.addAutoFormEntity(drillDown);
        }
        return drillDown;
    }

    public DrillDownFormEntity createDrillDownForm(BaseLogicsModule LM) {
        return null;
    }

    public boolean supportsReset() {
        return false;
    }

    public boolean isFull(AlgInfoType calcType) { // обозначает что можно вывести классы всех параметров, используется в частности для материализации (stored, hints) чтобы знать типы колонок, или даже в subQuery (для статистики)
        return isFull(interfaces, calcType);
    }

    public boolean isNotNull(AlgInfoType algType) { // обозначает что при null одном из параметров всегда возвращается null значение
        return isNotNull(interfaces, algType);
    }

    public boolean calcNotNull(ImSet<T> checkInterfaces, CalcInfoType calcType) {
        return true;
    }

    @IdentityLazy
    public boolean inferNotNull(ImSet<T> checkInterfaces, InferInfoType inferType) {
        return inferInterfaceClasses(inferType).isNotNull(checkInterfaces, inferType);
    }

    public boolean isDrillFull() {
        return isFull(AlgType.drillType);
    }

    public boolean isEmpty(AlgInfoType algType) {
        if(AlgType.checkInferCalc) checkInferEmpty();
        return algType.isEmpty(this);
    }

    public boolean calcEmpty(CalcInfoType calcType) {
        return false;
    }

    @IdentityLazy
    public boolean inferEmpty(InferInfoType inferType) {
        // ищем false хоть по одному из параметров
        return inferClassValueWhere(inferType).isFalse();
    }

    public boolean checkAlwaysNull(boolean constraint) {
        return !isEmpty(AlgType.checkType);
    }

    public boolean isExplicitNull() {
        return this instanceof NullValueProperty; // isEmpty can be better, but we just want to emulate NULL to be like NULL caption
    }

    public boolean isExplicitTrue() {
        return this instanceof ValueProperty && ((ValueProperty)this).staticClass instanceof LogicalClass;
    }

    @IdentityLazy
    public boolean allowHintIncrement() {
        assert isFull(AlgType.hintType);

        if(!isEmpty(AlgType.hintType)) {
            ImSet<ValueClass> usedClasses = getInterfaceClasses(ClassType.materializeChangePolicy).values().toSet();
            ValueClass valueClass = getValueClass(ClassType.materializeChangePolicy);
            if(valueClass != null)
                usedClasses = usedClasses.merge(valueClass);
            for(ValueClass usedClass : usedClasses)
                if(usedClass instanceof OrderClass)
                    return false;
            // по идее эта проверка не нужна, так как при кидании hint'а есть проверка на changed.getFullStatKeys().less значения, но там есть проблема с интервалами так как x<=a<=b вернет маленькую статистику, и пропустит такой хинт, после чего возникнет висячий ключ
            // вообще правильнее либо statType специальный сделать, либо поддержку интервалов при компиляции (хотя с double'ами все равно будет проблема)
            // этот фикс решит проблему в большинстве случаев (кроме когда в свойсте явный интервал, что очень редко имеет смысл)
            if(hasAlotKeys())
                return false;
        }

        return true;
    }

    @IdentityStartLazy
    public Long getComplexity(boolean simple) {
        if(simple)
            AutoHintsAspect.pushDisabledComplex();
        try {
            Expr expr = getExpr(getMapKeys(), defaultModifier);
            if(simple && expr == null)
                return null;
            return expr.getComplexity(false);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        } finally {
            if(simple)
                AutoHintsAspect.popDisabledComplex();
        }
    }

    public long getSimpleComplexity() {
        Long complexity = getComplexity(true);
        if(complexity == null)
            return Settings.get().getLimitHintComplexComplexity();
        return complexity;
    }

    public long getComplexity() {
        Long complexity = getComplexity(true);
        if(complexity != null)
            return complexity;

        return getComplexity(false);
    }

    public void recalculateClasses(SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {
        recalculateClasses(sql, null, baseClass);
    }

    @StackMessage("{logics.recalculating.data.classes}")
    public void recalculateClasses(SQLSession sql, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        assert isStored();
        
        ImRevMap<KeyField, KeyExpr> mapKeys = mapTable.table.getMapKeys();
        Where where = DataSession.getIncorrectWhere(this, baseClass, mapTable.mapKeys.join(mapKeys));
        Query<KeyField, PropertyField> query = new Query<>(mapKeys, Expr.NULL(), field, where);
        sql.updateRecords(env == null ? new ModifyQuery(mapTable.table, query, OperationOwner.unknown, TableOwner.global) : new ModifyQuery(mapTable.table, query, env, TableOwner.global));
    }

    public void setDebugInfo(PropertyDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    @Override
    public PropertyDebugInfo getDebugInfo() {
        return (PropertyDebugInfo) debugInfo;
    }

    public void printDepends(boolean events, String tab) {
        System.out.println(tab + this);
        for(Property prop : getDepends(events)) {
            prop.printDepends(events, tab + '\t');
        }
    }

    @IdentityStartLazy
    public ImList<Property> getSortedDepends() {
        return getDepends().sort(BusinessLogics.propComparator());
    }

    // странная конечно эвристика, нужна чтобы f(a) IF g(a) наследовал draw options f(a), возможно в будущем надо убрать
    public ImList<Property> getAndProperties() {
        return ListFact.singleton(this);
    }

    public ActionMapImplement<?, T> logFormAction;

    public boolean isLoggable() {
        return logFormAction != null;
    }

    public void setLogFormAction(ActionMapImplement<?, T> logFormAction) {
        this.logFormAction = logFormAction;
    }

    public ActionMapImplement<?, T> getLogFormAction() {
        return logFormAction;
    }
    
    private boolean aggr;

    @IdentityStartLazy
    public boolean isAggr() {
        if(aggr)
            return true;
        ImSet<Property> anImplements = getImplements();
        if(anImplements.isEmpty())
            return false;
        for(Property implement : anImplements)
            if(!implement.isAggr())
                return false;
        return true;
    }

    public void setAggr(boolean aggr) {
        this.aggr = aggr;
    }
    
    public ImSet<Property> getImplements() {
        return SetFact.EMPTY();
    }
    
    public boolean checkRecursions(ImSet<CaseUnionProperty> abstractPath, ImSet<Property> path, Set<Property> marks) {
        if(path != null)
            path = path.addExcl(this);
        else {
            if(!marks.add(this))
                return false;
        }
        return calculateCheckRecursions(abstractPath, path, marks);
    }

    public boolean calculateCheckRecursions(ImSet<CaseUnionProperty> abstractPath, ImSet<Property> path, Set<Property> marks) {
        return false;
    }

    @Override
    public ApplyCalcEvent getApplyEvent() {
        if(isStored()) {
            if(event == null)
                event = new ApplyStoredEvent(this);
            return (ApplyStoredEvent)event;
        }
        return null;
    }

    private static <T> StatKeys<KeyExpr> getStatRows(ImRevMap<T, KeyExpr> mapKeys, Where where) {
        return where.getFullStatKeys(mapKeys.valuesSet(), StatType.PROP_STATS);
    }

    public Stat getInterfaceStat(boolean alotHeur) {
        return getInterfaceStat(MapFact.EMPTYREV(), alotHeur);
    }
    
    public Stat getInterfaceStat(ImMap<T, StaticParamNullableExpr> fixedExprs) {
        return getInterfaceStat(fixedExprs, false);
    }

    public Cost getInterfaceCost(ImMap<T, StaticParamNullableExpr> fixedExprs) {
        return getInterfaceCostStat(fixedExprs, false).second;
    }

    private Stat getInterfaceStat(ImMap<T, StaticParamNullableExpr> fixedExprs, boolean alotHeur) {
        return getInterfaceCostStat(fixedExprs, alotHeur).first;
    }

    @IdentityStartLazy
    @StackMessage("{message.core.property.get.interface.class.stats}")
    @ThisMessage
    private Pair<Stat, Cost> getInterfaceCostStat(ImMap<T, StaticParamNullableExpr> fixedExprs, boolean alotHeur) {
        ImRevMap<T, KeyExpr> innerKeys = KeyExpr.getMapKeys(interfaces.removeIncl(fixedExprs.keys()));
        ImMap<T, Expr> innerExprs = MapFact.addExcl(innerKeys, fixedExprs); // we need some virtual values

        // we don't need to fight with inconsistent caches, since now finalizeProps goes after initStoredTask (because now there is a dependency finalizeProps -> initIndices to avoid problems with getIndices cache)
        // however it seems that STAT_ALOT is needed to lower complexity in light start mode
        // PS: actually it is still needed to avoid inconsistent caches (since ImplementTable.statProps are used and they are filled in the synchronizeDB)
        Expr expr = alotHeur ? aspectCalculateExpr(innerExprs, CalcType.STAT_ALOT, PropertyChanges.EMPTY, null) : getExpr(innerExprs); // check if is called after stats if filled
//        Expr expr = calculateStatExpr(mapKeys, alotHeur);

        Where where = expr.getWhere();

        ImRevMap<T, KeyExpr> fInnerKeys = innerKeys.filterInclValuesRev(BaseUtils.immutableCast(where.getOuterKeys())); // ignoring "free" keys (having free keys breaks a lot of assertions in statistic calculations)

        StatKeys<KeyExpr> statRows = getStatRows(fInnerKeys, where);
        return new Pair<>(statRows.getRows(), statRows.getCost());
    }

    public Stat getSelectStat(ImMap<T, StaticParamNullableExpr> fixedExprs) {
        // we can't use MATCH here, because there is a bug, that now MATCH, CONTAINS stats is not calculate properly if the expr is not indexed
        // it's not clear how to fix this, because the table join cost is calculated based on stat, without knowing how this stat was obtained
        // for INTERVAL it could be fixed by removing isIndexed check, but for MATCH, CONTAINS we need to know what type of index we should use (it may be solved with some virtual join probably)
        // however here EQUALS is even semantically the right type to use
        return getSelectCostStat(fixedExprs, Compare.EQUALS).first;
    }

    @IdentityStartLazy
    @StackMessage("{message.core.property.get.interface.class.stats}")
    @ThisMessage
    private Pair<Stat, Cost> getSelectCostStat(ImMap<T, StaticParamNullableExpr> fixedExprs, Compare compare) {
        ImRevMap<T, KeyExpr> innerKeys = KeyExpr.getMapKeys(interfaces.removeIncl(fixedExprs.keys()));
        ImMap<T, Expr> innerExprs = MapFact.addExcl(innerKeys, fixedExprs); // we need some virtual values

        Where where = getExpr(innerExprs).compare(getValueParamExpr(), compare);

        innerKeys = innerKeys.filterInclValuesRev(BaseUtils.immutableCast(where.getOuterKeys())); // ignoring "free" keys (having free keys breaks a lot of assertions in statistic calculations)
        StatKeys<KeyExpr> statRows = getStatRows(innerKeys, where);
        return new Pair<>(statRows.getRows(), statRows.getCost());
    }

    public Stat getValueStat(ImMap<T, StaticParamNullableExpr> fixedExprs) {
        return getInterfaceStat(fixedExprs).div(getSelectStat(fixedExprs));
    }

    protected ImRevMap<T, NullableKeyExpr> getMapNotNullKeys() {
        return interfaces.mapRevValues((i, value) -> new NullableKeyExpr(i));
    }

    public Stat getInterfaceStat(ImSet<T> interfaces) {
        return getInterfaceStat(getInterfaceParamExprs(interfaces));
    }

    public Cost getSelectCost(ImMap<T, StaticParamNullableExpr> fixedInterfaces) {
        // the obtained stat will be incorrect here (see getSelectStat comment) but we don't need it anyway
        return getSelectCostStat(fixedInterfaces, Compare.MATCH).second;
    }

    @IdentityInstanceLazy
    public ImRevMap<T, StaticParamNullableExpr> getInterfaceParamExprs(ImSet<T> interfaces) {
        ImMap<T, ValueClass> interfaceClasses = getInterfaceClasses(ClassType.forPolicy);
        return interfaces.mapValues((T anInterface) -> {
            ValueClass valueClass = interfaceClasses.get(anInterface);
            return valueClass != null ? valueClass : AbstractType.getUnknownClassNull();
        }).mapRevValues(StaticParamNullableExpr::new);
    }
    @IdentityInstanceLazy
    public StaticParamNullableExpr getValueParamExpr() {
        // maybe later it makes sense to fill params without classes with some "default" classes
        return new StaticParamNullableExpr(getValueClass(ClassType.forPolicy));
    }


    // it's heuristics anyway, so why not to try to guess uniqueness by name
    private static ImSet<String> predefinedSwitchNames = SetFact.toSet("enable", "disable", "on", "off");

    public boolean isPredefinedSwitch() {
        String name = getName();
//        return name != null && predefinedValueUniqueNames.contains(name);
        return name != null && BaseUtils.findInCamelCase(name, predefinedSwitchNames::contains);
    }

    public enum ValueUniqueType {
        SELECT, // select instead of CHANGE
        INPUT, // input dropdown, CHANGE or SELECTOR

        STICKY, // sticky, DRAW
        // NOTNULL can be probably refactored that way, that isDrawNotNull will use getSelectProperty / getDefaultEventAction and get not null from there
        NOTNULL, // DRAW

        EDIT, // edit param or value, EDIT
        DIALOG; // dialog, CHANGE

        public boolean isOptimistic() {
            switch (this) {
                // it's really undesirable to have false positives
                case SELECT:
                case INPUT:
                // it's not that crucial to have false negatives
                case STICKY:
                    return false;
            }
            // it's not crucial to have false positives
            return true;
        }
    }
    // assert that returns isValueFull property
    public boolean isValueUnique(ImMap<T, StaticParamNullableExpr> fixedExprs, ValueUniqueType type) {
        return isValueUnique(fixedExprs, type.isOptimistic());
    }

    // it's heuristics anyway, so why not to try to guess uniqueness by name
    private static ImSet<String> predefinedValueUniqueNames = SetFact.toSet("name", "id", "number", "caption");

    // actually protected
    public boolean isNameValueUnique() {
        return false;
    }

    // optimistic determines what to do when there is no statistics
    public boolean isValueUnique(ImMap<T, StaticParamNullableExpr> fixedExprs, boolean optimistic) {
        if(!isValueFull(fixedExprs))
            return false;

        if(isNameValueUnique()) {
            String name = getName();
            if(name != null && BaseUtils.findInCamelCase(name, predefinedValueUniqueNames::contains))
                return true;
        }

        // using selectStat (calculation logic), rather than going deep in the property types is better for 2 reasons:
        // 1. can use MATERIALIZED and its statistics
        // 2. can handle complex cases for example OVERRIDE empty ABSTRACT, ...
        if(!optimistic) {
            if(getInterfaceStat(fixedExprs).lessEquals(new Stat(Settings.get().getMinInterfaceStatForValueUnique())))
                return false;
        }

        return getSelectStat(fixedExprs).equals(Stat.ONE);
    }

    public boolean isValueFull(ImMap<T, StaticParamNullableExpr> fixedExprs) {
        return isValueFull(fixedExprs.keys());
    }
    public boolean isValueFull(ImSet<T> fixedExprs) {
        return isFull(interfaces.removeIncl(fixedExprs), AlgType.statAlotType);
    }

    // filter or custom view completion
    public <X extends PropertyInterface> InputPropertyListEntity<?, T> getInputList(ImMap<T, StaticParamNullableExpr> fixedExprs, boolean noJoin) {
        if(isValueFull(fixedExprs) && !tooMuchSelectData(fixedExprs))
            return new InputPropertyListEntity<>(this, fixedExprs.keys().toRevMap());
        return null;
    }

    public boolean hasAlotKeys() {
//        if(1==1) return false;
        if(SystemProperties.lightStart) {
            if (!isFull(AlgType.statAlotType))
                return true;
            if (isStored())
                return false;
            return aspectDebugHasAlotKeys();
        }
       return hasAlotKeys(getInterfaceStat(false));
    }

    protected boolean aspectDebugHasAlotKeys() {
        return hasAlotKeys(getInterfaceStat(true));
    }

    private final static Stat ALOT_THRESHOLD = Stat.ALOT.reduce(2); // ALOT stat can be reduced a little bit, but there still will be ALOT keys, so will take sqrt
    private static boolean hasAlotKeys(Stat stat) {
        return ALOT_THRESHOLD.lessEquals(stat);
    }
}
