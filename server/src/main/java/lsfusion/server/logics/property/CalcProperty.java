package lsfusion.server.logics.property;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.server.Settings;
import lsfusion.server.caches.*;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.OrClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.classes.sets.ResolveUpClassSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;
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
import lsfusion.server.data.query.stat.TableStatKeys;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.*;
import lsfusion.server.logics.debug.CalcPropertyDebugInfo;
import lsfusion.server.logics.i18n.FormatLocalizedString;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.ChangeEvent;
import lsfusion.server.logics.property.actions.edit.DefaultChangeActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.derived.MaxChangeProperty;
import lsfusion.server.logics.property.derived.OnChangeProperty;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferInfoType;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.table.MapKeysTable;
import lsfusion.server.logics.table.TableFactory;
import lsfusion.server.session.*;
import lsfusion.server.stack.StackMessage;
import lsfusion.server.stack.ThisMessage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static lsfusion.server.context.ThreadLocalContext.localize;

public abstract class CalcProperty<T extends PropertyInterface> extends Property<T> implements MapKeysInterface<T> {

    public static FunctionSet<CalcProperty> getDependsOnSet(final FunctionSet<CalcProperty> check) {
        if(check.isEmpty())
            return check;
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
        return property.getRecDepends().intersect(check);
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
        return Settings.get().isEnableApplySingleStored();
    }
    public boolean isSingleApplyStored() { // нужен для ClassDataProperty, для которого отдельный принцип обработки
        return isStored() && isEnabledSingleApply();
    }

    public String outputStored(boolean outputTable) {
        assert isStored() && field!=null;
        return localize(LocalizedString.create(
                    (this instanceof DataProperty ? "{logics.property.primary}" : "{logics.property.calculated}") 
                    + " {logics.property} : " + caption.getSourceString() + ", " + mapTable.table.outputField(field, outputTable))
        );
    }
    
    public void outClasses(DataSession session, Modifier modifier) throws SQLException, SQLHandledException {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        new Query<>(mapKeys, getExpr(mapKeys, modifier), "value").outClassesSelect(session.sql, session.baseClass);
    }

    // по выражениям проверяет
    public <P extends PropertyInterface> void checkExclusiveness(String caption, CalcProperty<P> property, String propertyCaption, ImRevMap<P, T> map) {
        AlgType.caseCheckType.checkExclusiveness(this, caption, property, propertyCaption, map);
    }

    public <P extends PropertyInterface> void inferCheckExclusiveness(String caption, CalcProperty<P> property, String propertyCaption, ImRevMap<P, T> map, InferType inferType) {
        Inferred<T> classes = inferInterfaceClasses(inferType);
        Inferred<P> propClasses = property.inferInterfaceClasses(inferType);
        if(!classes.and(propClasses.map(map), inferType).isEmpty(inferType))
            throw new ScriptParsingException("signature intersection of property " + caption + " (WHEN " + this +") with previosly defined implementation " + propertyCaption + " (WHEN " + property +") for abstract property " + this + "\n" +
                    "Classes 1 : " + ExClassSet.fromEx(classes.finishEx(inferType)) + ", Classes 2 : " + ExClassSet.fromEx(propClasses.finishEx(inferType)));
    }

    public <P extends PropertyInterface> void calcCheckExclusiveness(String caption, CalcProperty<P> property, String propertyCaption, ImMap<P, T> map, CalcClassType calcType) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        if(!calculateExpr(mapKeys, calcType).getWhere().and(property.calculateExpr(map.join(mapKeys), calcType).getWhere()).not().checkTrue())
            throw new ScriptParsingException("signature intersection of property " + caption + " (WHEN " + this +") with previosly defined implementation " + propertyCaption + " (WHEN " + property +") for abstract property " + this + "\n" +
                    "Classes 1 : " + getClassWhere(calcType) + ", Classes 2 : " + property.getClassWhere(calcType));
    }

    public <P extends PropertyInterface> void checkContainsAll(CalcProperty<P> property, String caption, ImRevMap<P, T> map, CalcPropertyInterfaceImplement<T> value) {
        AlgType.caseCheckType.checkContainsAll(this, property, caption, map, value);
    }

    public <P extends PropertyInterface> void inferCheckContainsAll(CalcProperty<P> property, String caption, ImRevMap<P, T> map, InferType inferType, CalcPropertyInterfaceImplement<T> value) {
        ImMap<T, ExClassSet> interfaceClasses = getInferInterfaceClasses(inferType);
        ImMap<T, ExClassSet> interfacePropClasses = map.crossJoin(property.getInferInterfaceClasses(inferType));

        if(!containsAll(interfaceClasses, interfacePropClasses, false))
            throw new ScriptParsingException("wrong signature of implementation " + caption + 
                    " (specified " + ExClassSet.fromEx(interfacePropClasses) + ") for abstract property " + this + " (expected " + ExClassSet.fromEx(interfaceClasses) + ")");

        ResolveClassSet valueClass = ExClassSet.fromEx(inferValueClass(interfaceClasses, inferType));
        ResolveClassSet propValueClass = ExClassSet.fromEx(value.mapInferValueClass(interfacePropClasses, inferType));
        if(!valueClass.containsAll(propValueClass, false))
            throw new ScriptParsingException("wrong value class of implementation " + caption +
                    " (specified " + propValueClass + ") for abstract property " + this + " (expected " + valueClass + ")");
    }
    
    public ExClassSet inferJoinValueClass(ImMap<T, ExClassSet> extContext, boolean useExtContext, InferType inferType) {
        if(!useExtContext && inferType == InferType.RESOLVE) {
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
//            if(inferType == InferType.PREVSAME && !BaseUtils.nullEquals(ExClassSet.fromEx(newValueClass),ExClassSet.fromEx(oldExtContext))) {
//                ResolveClassSet resNew = ExClassSet.fromEx(newValueClass);
//                ResolveClassSet resOld = ExClassSet.fromEx(oldExtContext);
//                if(!(resNew instanceof StringClass && resOld instanceof StringClass) && !(resOld instanceof NumericClass && resNew instanceof NumericClass))
//                    System.out.println((cnt++) + logCaption + " " + BaseUtils.nullToString(resOld) + " -> " + BaseUtils.nullToString(resNew));
//            }
//        }
//        if(inferType == InferType.PREVSAME && !(BaseUtils.nullEquals(ExClassSet.fromEx(newValueClass), ExClassSet.fromEx(newDiffClassSet))))
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

    public <P extends PropertyInterface> void calcCheckContainsAll(CalcProperty<P> property, ImRevMap<P, T> map, CalcClassType calcType, CalcPropertyInterfaceImplement<T> value) {
        ClassWhere<T> classes = getClassWhere(calcType);
        ClassWhere<T> propClasses = new ClassWhere<>(property.getClassWhere(calcType),map);
        
        if(!propClasses.meansCompatible(classes)) {
            throw new ScriptParsingException("wrong signature of implementation " + localize(caption) + " (specified " + propClasses + ") for abstract property " + this + " (expected " + classes + ")");
        }
        
        AndClassSet valueClass = getValueClassSet();
        AndClassSet propValueClass = value.mapValueClassSet(propClasses);
        if(!valueClass.containsAll(propValueClass, false))
            throw new ScriptParsingException("wrong value class of implementation " + localize(caption) +
                    " (specified " + propValueClass + ") for abstract property " + this + " (expected " + valueClass + ")");
    }

    // assert что CaseUnion
    public <P extends PropertyInterface> void checkAllImplementations(ImList<CalcProperty<P>> props, ImList<ImRevMap<P, T>> maps) {
        AlgType.caseCheckType.checkAllImplementations(this, props, maps);
    }

    public <P extends PropertyInterface> void inferCheckAllImplementations(ImList<CalcProperty<P>> props, ImList<ImRevMap<P, T>> maps, InferType calcType) {
        ImMap<T, ExClassSet> classes = getInferInterfaceClasses(calcType);

        if(props.isEmpty())
            throw new ScriptParsingException("Property is not implemented : " + this + " (specified " + classes + ")");

        ImMap<T, ExClassSet> propClasses = null;
        for(int i=0,size=props.size();i<size;i++) {
            CalcProperty<P> prop = props.get(i);
            ImRevMap<P, T> map = maps.get(i);
            ImMap<T, ExClassSet> propCaseClasses = map.crossJoin(prop.getInferInterfaceClasses(calcType));
            if(propClasses == null)
                propClasses = propCaseClasses;
            else
                propClasses = ExClassSet.op(interfaces, propClasses, propCaseClasses, true);
        }

        if(!containsAll(propClasses, classes, true)) {
            throw new CaseUnionProperty.NotFullyImplementedException("Property is not fully implemented : " + this +  ", Calculated : " + propClasses + ", Specified : " + classes, propClasses.toString(), classes.toString());
        }
    }

    public <P extends PropertyInterface> void calcCheckAllImplementations(ImList<CalcProperty<P>> props, ImList<ImRevMap<P, T>> maps, CalcClassType calcType) {
        ClassWhere<T> classes = getClassWhere(calcType);

        ClassWhere<T> propClasses = ClassWhere.FALSE();
        for (int i = 0, size = props.size(); i < size; i++) {
            CalcProperty<P> prop = props.get(i);
            ImRevMap<P, T> map = maps.get(i);
            propClasses = propClasses.or(new ClassWhere<>(prop.getClassWhere(calcType), map));
        }

        if (!classes.meansCompatible(propClasses)) {
            throw new CaseUnionProperty.NotFullyImplementedException("Property is not fully implemented : " + this +  ", Calculated : " + propClasses + ", Specified : " + classes, propClasses.toString(), classes.toString());
        }
    }

    protected CalcProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
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
            return new Pair<>(createChangeTable(), changeTable);
        if(DataSession.fitClasses(this, changeTable))
            return new Pair<>(changeTable, createChangeTable());

        PropertyChange<T> change = SinglePropertyTableUsage.getChange(changeTable);

        ImMap<KeyField, Expr> mapKeys = mapTable.mapKeys.crossJoin(change.getMapExprs());
        Where classWhere = fieldClassWhere.getWhere(MapFact.addExcl(mapKeys, field, change.expr))
                .or(mapTable.table.getClasses().getWhere(mapKeys).and(change.expr.getWhere().not())); // или если меняет на null, assert что fitKeyClasses
        
        if(classWhere.isFalse()) // оптимизация
            return new Pair<>(createChangeTable(), changeTable);
        if(classWhere.isTrue())
            return new Pair<>(changeTable, createChangeTable());

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
        for(CalcProperty<?> property : getDepends(false))
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

    protected Expr getVirtualTableExpr(ImMap<T, ? extends Expr> joinImplement, CalcClassType classType) {
        VirtualTable<T> virtualTable = getVirtualTable(classType);
        return virtualTable.join(virtualTable.mapFields.join(joinImplement)).getExpr(virtualTable.propValue);
    }

    @IdentityStrongLazy
    public VirtualTable<T> getVirtualTable(CalcClassType classType) {
        return new VirtualTable<>(this, classType);
    }

    public boolean usePrevHeur() {
        return getExpr(getMapKeys(), CalcClassType.PREVSAME_KEEPIS).isNull();
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

    public static class VirtualTable<P extends PropertyInterface> extends Table {

        public final ImRevMap<KeyField, P> mapFields;
        public final PropertyField propValue;

        public VirtualTable(final CalcProperty<P> property, CalcClassType classType) {
            super(property.getSID());
            
            ImRevMap<P, KeyField> revMapFields = property.interfaces.mapRevValues(new GetValue<KeyField, P>() {
                public KeyField getMapValue(P value) {
                    return new KeyField(value.getSID(), property.getInterfaceType(value));
                }});
            mapFields = revMapFields.reverse();
            keys = property.getOrderInterfaces().mapOrder(revMapFields);
            
            propValue = new PropertyField("value", property.getType());
            properties = SetFact.singleton(propValue);

            classes = property.getClassWhere(classType).remap(revMapFields);
            propertyClasses = MapFact.singleton(propValue, property.getClassValueWhere(classType).remap(MapFact.<Object, Field>addRevExcl(revMapFields, "value", propValue)));
        }

        public TableStatKeys getTableStatKeys() {
            return getStatKeys(this, 100);
        }

        public ImMap<PropertyField,PropStat> getStatProps() {
            return getStatProps(this, 100);
        }
    }

    // есть assertion, что не должен возвращать изменение null -> null, то есть или старое или новое не null, для подр. см usage
    @LogTime
    @ThisMessage
    public PropertyChange<T> getIncrementChange(Modifier modifier) throws SQLException, SQLHandledException {
        return getIncrementChange(modifier.getPropertyChanges());
    }

    public PropertyChange<T> getIncrementChange(PropertyChanges propChanges) {
        IQuery<T, String> incrementQuery = getQuery(CalcType.EXPR, propChanges, PropertyQueryType.FULLCHANGED, MapFact.<T, Expr>EMPTY());
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
        return new PropertyChange<>(getMapKeys(), CaseExpr.NULL);
    }

    public SinglePropertyTableUsage<T> createChangeTable() {
        return new SinglePropertyTableUsage<>(getOrderInterfaces(), interfaceTypeGetter, getType());
    }

    @StackMessage("message.increment.read.properties")
    @ThisMessage
    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, Modifier modifier, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        return readFixChangeTable(session, getIncrementChange(modifier), baseClass, env);
    }

    public SinglePropertyTableUsage<T> readFixChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        SinglePropertyTableUsage<T> readTable = readChangeTable(session, change, baseClass, env);

        // при вызове readChangeTable, используется assertion (см. assert fitKeyClasses) что если таблица подходит по классам для значения, то подходит по классам и для ключей
        // этот assertion может нарушаться если определилось конкретное значение и оно было null, как правило с комбинаторными event'ами (вообще может нарушиться и если не null, но так как propertyClasses просто вырезаются то не может), соответственно необходимо устранить этот случай
        readTable.fixKeyClasses(getClassWhere(ClassType.materializeChangePolicy));
        readTable.checkClasses(session, null, SessionTable.nonead, env.getOpOwner()); // нужен как раз для проверки fixKeyClasses

        return readTable;
    }

    public SinglePropertyTableUsage<T> readChangeTable(SQLSession session, PropertyChange<T> change, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        SinglePropertyTableUsage<T> changeTable = createChangeTable();
        change.writeRows(changeTable, session, baseClass, env, SessionTable.matGlobalQuery);
        return changeTable;
    }

    @IdentityStrongLazy // just in case
    public <P extends PropertyInterface> MaxChangeProperty<T, P> getMaxChangeProperty(CalcProperty<P> change) {
        return new MaxChangeProperty<>(this, change);
    }
    @IdentityStrongLazy // just in case
    public <P extends PropertyInterface> OnChangeProperty<T, P> getOnChangeProperty(CalcProperty<P> change) {
        return new OnChangeProperty<>(this, change);
    }

    public enum CheckType { CHECK_NO, CHECK_ALL, CHECK_SOME }
    public CheckType checkChange = CheckType.CHECK_NO;
    public ImSet<CalcProperty<?>> checkProperties = null;

    public Collection<MaxChangeProperty<?, T>> getMaxChangeProperties(Collection<CalcProperty> properties) {
        Collection<MaxChangeProperty<?, T>> result = new ArrayList<>();
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
    public <T extends PropertyInterface> void addActionChangeProp(Pair<Property<T>, LinkType> pair) {
        if(((ActionProperty<?>)pair.first).strongUsed.contains(CalcProperty.this)) { // в явную задана связь
            pair = new Pair<>(pair.first, pair.second.decrease()); // ослабим связь, но не удалим, чтобы была в той же компоненте связности, но при этом не было цикла
        }

        if(actionChangeProps==null)
            actionChangeProps = ListFact.mCol();
        actionChangeProps.add(BaseUtils.<Pair<Property<?>, LinkType>>immutableCast(pair));
    }
    public ImCol<Pair<Property<?>, LinkType>> getActionChangeProps() {
        if(actionChangeProps!=null)
            return actionChangeProps.immutableCol();

        return SetFact.EMPTY();
    }
    public void dropActionChangeProps() {
        actionChangeProps = null;
    }

    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean events) {
        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();

        for(CalcProperty depend : getDepends(events))
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

    public boolean noHint = false;
    @IdentityLazy
    public boolean isNoHint() {
        if(noHint)
            return true;

        for(CalcProperty property : getDepends())
            if(property.isNoHint())
                return true;
        return false;
    }

    public static byte SET = 2;
    public static byte DROPPED = 1;
    public static byte getSetDropped(boolean setOrDropped) {
        return setOrDropped ? SET : DROPPED;
    }

    private final AddValue<CalcProperty, Byte> addSetOrDropped = new SymmAddValue<CalcProperty, Byte>() {
        public Byte addValue(CalcProperty key, Byte prevValue, Byte newValue) {
            return (byte)(prevValue | newValue); // раньше and был, но тогда получалась проблема что если есть и SET и DROP, а скажем DROP "идет за" каким-то FINAL изменением, кэши getUsedChanges не совпадают (см. logCaches)
        }
    };

    @IdentityLazy
    public ImMap<CalcProperty, Byte> getSetOrDroppedDepends() {
        ImSet<SessionCalcProperty> sessionDepends = getSessionCalcDepends(true); // нужны и вычисляемые события, так как в логике вычислений (getExpr) используется
        MMap<CalcProperty, Byte> mResult = MapFact.mMap(addSetOrDropped);
        for(int i=0,size=sessionDepends.size();i<size;i++) {
            SessionCalcProperty property = sessionDepends.get(i);
            if(property instanceof ChangedProperty) {
                ChangedProperty changed = (ChangedProperty) property;
                Boolean setOrDropped = changed.getSetOrDropped();
                if(setOrDropped != null)
                    mResult.add(changed.property, getSetDropped(setOrDropped));
            }
        }
        ImMap<CalcProperty, Byte> result = mResult.immutable();
        assert getRecDepends().containsAll(result.keys());
        return result;
    }

    @IdentityStartLazy
    public ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events) {
        MSet<SessionCalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : getDepends(events)) // derived'ы в общем то не интересуют так как используется в singleApply
            mResult.addAll(property.getSessionCalcDepends(events));
        return mResult.immutable();
    }

    // получает базовый класс по сути нужен для определения класса фильтра
    public CustomClass getDialogClass(ImMap<T, DataObject> mapValues, ImMap<T, ConcreteClass> mapClasses) {
        return (CustomClass)getValueClass(ClassType.editPolicy);
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

    // чтобы не было рекурсии так сделано
    public Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType) {
        return calculateExpr(joinImplement, calcType, PropertyChanges.EMPTY, null);
    }

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
        markStored(tableFactory, (ImplementTable)null);
    }

    public void markStored(TableFactory tableFactory, ImplementTable table) {
        MapKeysTable<T> mapTable = null;

        ImOrderMap<T, ValueClass> keyClasses = getOrderTableInterfaceClasses(ClassType.storedPolicy);
        if (table != null) {
            mapTable = table.getMapKeysTable(keyClasses);
            assert mapTable!=null;
        }

        if (mapTable == null) {
            mapTable = tableFactory.getMapTable(keyClasses);
        }

        markStored(tableFactory, mapTable);
    }

    public void markStored(TableFactory tableFactory, MapKeysTable<T> mapTable) {

        PropertyField field = new PropertyField(getDBName(), getType());
        fieldClassWhere = getClassWhere(mapTable, field);
        mapTable.table.addField(field, fieldClassWhere);

        this.mapTable = mapTable;
        this.field = field;
    }


    public void markIndexed(final ImRevMap<T, String> mapping, ImList<CalcPropertyObjectInterfaceImplement<String>> index) {
        assert isStored();

        ImList<Field> indexFields = index.mapListValues(new GetValue<Field, CalcPropertyObjectInterfaceImplement<String>>() {
            public Field getMapValue(CalcPropertyObjectInterfaceImplement<String> indexField) {
                if (indexField instanceof CalcPropertyObjectImplement) {
                    String key = ((CalcPropertyObjectImplement<String>) indexField).object;
                    return mapTable.mapKeys.get(mapping.reverse().get(key));
                } else {
                    CalcProperty property = ((CalcPropertyRevImplement) indexField).property;
                    assert BaseUtils.hashEquals(mapTable.table, property.mapTable.table);
                    return property.field;
                }
            }
        });
        mapTable.table.addIndex(indexFields.toOrderExclSet());
    }

    public AndClassSet getValueClassSet() {
        return getClassValueWhere(ClassType.resolvePolicy).getCommonClass("value");
    }
    
    // для resolve'а
    public ResolveClassSet getResolveClassSet(ImMap<T, ResolveClassSet> classes) {
        ExClassSet set = inferValueClass(ExClassSet.toEx(classes), InferType.RESOLVE);
        if(set != null && set.isEmpty())
            return null;
        return ExClassSet.fromEx(set);
    }

    @IdentityLazy
    public ClassWhere<T> getClassWhere(ClassType type) {
        return getClassValueWhere(type).filterKeys(interfaces); // не полностью, собсно для этого и есть full
    }

    public ClassWhere<Object> getClassValueWhere(final ClassType type) {
        return classToAlg(type, new CallableWithParam<AlgType, ClassWhere<Object>>() {
            public ClassWhere<Object> call(AlgType arg) {
                if(AlgType.checkInferCalc) checkInferClasses(type);
                return getClassValueWhere(arg);
            }
        });
    }

    @IdentityLazy
    public ImMap<T, ValueClass> getInterfaceClasses(ClassType type) {
        return getInterfaceClasses(type, null);
    }

    // упорядоченный map классов для обеспечения детерминированности (связано с ImplementTable.getOrderMapFields)
    public ImOrderMap<T, ValueClass> getOrderTableInterfaceClasses(ClassType type) {
        return getOrderInterfaces().mapOrderMap(getInterfaceClasses(type));
    }

    public ImMap<T, ValueClass> getInterfaceClasses(ClassType type, final ExClassSet valueClasses) {
        return classToAlg(type, new CallableWithParam<AlgType, ImMap<T, ValueClass>>() {
            public ImMap<T, ValueClass> call(AlgType arg) {
                return arg.getInterfaceClasses(CalcProperty.this, valueClasses);
            }
        });
    }

    @IdentityLazy
    public ValueClass getValueClass(ClassType classType) {
        return classToAlg(classType, new CallableWithParam<AlgType, ValueClass>() {
            public ValueClass call(AlgType arg) {
                return arg.getValueClass(CalcProperty.this);
            }
        });
    }

    protected <V> V classToAlg(ClassType type, CallableWithParam<AlgType, V> call) {
        boolean assertFull = false;
        if(type == ClassType.ASSERTFULL_NOPREV) {
            type = ClassType.useInsteadOfAssert;
            assertFull = true;
        }

        AlgType algType = type.getAlg();
        assert !assertFull || isFull(algType.getAlgInfo());
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
//        if(calcType == CalcClassType.PREVSAME && getParseOldDepends().size() == 0) { // PREVSAME классовые свойства подменяет, а там есть очень сложные свойства
//            calcType = CalcType.PREVBASE;
//            inferType = InferType.PREVBASE;
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
    private boolean checkInferNotNull() {
        boolean calcNotNull = calcNotNull(CalcClassType.PREVBASE);
        boolean inferNotNull = inferNotNull(InferType.PREVBASE);
        if(calcNotNull != inferNotNull) {
            System.out.println(this + " NOTNULL, CALC : " + calcNotNull + ", INF : " + inferNotNull);
            return false;
        }
        return true;
    }

    @IdentityStartLazy
    private boolean checkInferEmpty() {
        boolean calcEmpty = calcEmpty(CalcClassType.PREVBASE);
        boolean inferEmpty = inferEmpty(InferType.PREVBASE);
        if(calcEmpty != inferEmpty) {
            System.out.println(this + " EMPTY, CALC : " + calcEmpty + ", INF : " + inferEmpty);
            return false;
        }
        return true;
    }

    @IdentityStartLazy
    private boolean checkInferFull(ImCol<T> checkInterfaces) {
        boolean calcFull = calcFull(checkInterfaces, CalcClassType.PREVBASE);
        boolean inferFull = inferFull(checkInterfaces, InferType.PREVBASE);
        if(calcFull != inferFull) {
            System.out.println(this + " FULL, CALC : " + calcFull + ", INF : " + inferFull);
            return false;
        }
        return true;
    }

    protected abstract ClassWhere<Object> calcClassValueWhere(CalcClassType calcType);

    private static final Checker<ExClassSet> checker = new Checker<ExClassSet>() {
        public boolean checkEquals(ExClassSet expl, ExClassSet calc) {
            ResolveClassSet resExpl = ExClassSet.fromEx(expl);
            ResolveClassSet resCalc = ExClassSet.fromEx(calc);
            if(resExpl == null)
                return resCalc == null;
            if(resCalc == null)
                return false;
            
            AndClassSet explAnd = resExpl.toAnd();
            AndClassSet calcAnd = resCalc.toAnd();
            return explAnd.containsAll(calcAnd, false) && calcAnd.containsAll(explAnd, false);
        }
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

    protected static <T extends PropertyInterface> ImMap<T, ExClassSet> getInferExplicitCalcInterfaces(ImSet<T> interfaces, boolean noOld, InferType inferType, ImMap<T, ResolveClassSet> explicitInterfaces, Callable<ImMap<T,ExClassSet>> calcInterfaces, String caption, Checker<ExClassSet> checker) {
        assert inferType != InferType.RESOLVE;
        return getExplicitCalcInterfaces(interfaces, (inferType == InferType.PREVBASE && !noOld) || explicitInterfaces == null ? null : ExClassSet.toEx(explicitInterfaces), calcInterfaces, caption, checker);
    }

    private ImMap<T, ExClassSet> getInferInterfaceClasses(final InferType inferType) {
        return getInferInterfaceClasses(inferType, null);
    }

    private ImMap<T, ExClassSet> getInferInterfaceClasses(final InferType inferType, final ExClassSet valueClasses) {
        return getInferExplicitCalcInterfaces(interfaces, noOld(), inferType, explicitClasses, new Callable<ImMap<T, ExClassSet>>() {
            public ImMap<T, ExClassSet> call() throws Exception {
                return calcInferInterfaceClasses(inferType, valueClasses);
            }}, "CALC " + this, checker);
    }

    private ImMap<T, ExClassSet> calcInferInterfaceClasses(InferType inferType, ExClassSet valueClasses) {
        return inferInterfaceClasses(valueClasses, inferType).finishEx(inferType);
    }

    public ClassWhere<Field> getClassWhere(MapKeysTable<T> mapTable, PropertyField storedField) {
        return getClassValueWhere(ClassType.storedPolicy).remap(MapFact.<Object, Field>addRevExcl(mapTable.mapKeys, "value", storedField)); //
    }

    public Object read(ExecutionContext context) throws SQLException, SQLHandledException {
        return read(context.getSession().sql, MapFact.<T, ObjectValue>EMPTY(), context.getModifier(), context.getQueryEnv());
    }

    public Object read(SQLSession session, ImMap<T, ? extends ObjectValue> keys, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(SetFact.<T>EMPTY());
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier));
        return readQuery.execute(session, env).singleValue().get(readValue);
    }

    public ObjectValue readClasses(SQLSession session, ImMap<T, Expr> keys, BaseClass baseClass, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(SetFact.<T>EMPTY());
        readQuery.addProperty(readValue, getExpr(keys, modifier));
        return readQuery.executeClasses(session, env, baseClass).singleValue().get(readValue);
    }

    public Pair<ObjectValue, Boolean> readClassesChanged(SQLSession session, ImMap<T, ObjectValue> keys, BaseClass baseClass, Modifier modifier, QueryEnvironment env) throws SQLException, SQLHandledException {
        String readValue = "readvalue"; String readChanged = "readChanged";
        QueryBuilder<T, Object> readQuery = new QueryBuilder<>(SetFact.<T>EMPTY());
        WhereBuilder changedWhere = new WhereBuilder();
        readQuery.addProperty(readValue, getExpr(ObjectValue.getMapExprs(keys), modifier, changedWhere));
        readQuery.addProperty(readChanged, ValueExpr.get(changedWhere.toWhere()));
        ImMap<Object, ObjectValue> result = readQuery.executeClasses(session, env, baseClass).singleValue();
        return new Pair<>(result.get(readValue), !result.get(readChanged).isNull());
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
        return SetFact.EMPTY();
    }

    protected DataChanges getPullDataChanges(PropertyChanges changes, boolean toNull) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        return getDataChanges(new PropertyChange<>(mapKeys, toNull ? CaseExpr.NULL : getChangeExpr(), CompareWhere.compare(mapKeys, getChangeExprs())), changes, null);
    }

    protected DataChanges getJoinDataChanges(ImMap<T, ? extends Expr> implementExprs, Expr expr, Where where, PropertyChanges propChanges, WhereBuilder changedWhere) {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        WhereBuilder changedImplementWhere = cascadeWhere(changedWhere);
        DataChanges result = getDataChanges(new PropertyChange<>(mapKeys,
                GroupExpr.create(implementExprs, expr, where, GroupType.ANY, mapKeys),
                GroupExpr.create(implementExprs, where, mapKeys).getWhere()),
                propChanges, changedImplementWhere);
        if (changedWhere != null)
            changedWhere.add(new Query<>(mapKeys, changedImplementWhere.toWhere()).join(implementExprs).getWhere());// нужно перемаппить назад
        return result;
    }

    @StackMessage("message.core.property.data.changes")
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

    public <D extends PropertyInterface> void setEventChange(CalcPropertyMapImplement<D, T> valueImplement, ImList<CalcPropertyMapImplement<?, T>> whereImplements, ImCol<CalcPropertyMapImplement<?, T>> onChangeImplements) {

        ImCol<CalcPropertyMapImplement<?, T>> onChangeWhereImplements = onChangeImplements.mapColValues(new GetValue<CalcPropertyMapImplement<?, T>, CalcPropertyMapImplement<?, T>>() {
                    public CalcPropertyMapImplement<?, T> getMapValue(CalcPropertyMapImplement<?, T> value) {
                        return value.mapChanged(IncrementType.SETCHANGED, ChangeEvent.scope);
                    }});

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
        setEventChange(null, false, valueImplement, where);
    }

    public <D extends PropertyInterface, W extends PropertyInterface> void setEventChange(LogicsModule lm, boolean action, CalcPropertyInterfaceImplement<T> valueImplement, CalcPropertyMapImplement<W, T> whereImplement) {
        if(action && !Settings.get().isDisableWhenCalcDo()) {
            ActionPropertyMapImplement<?, T> setAction = DerivedProperty.createSetAction(interfaces, getImplement(), valueImplement);
            lm.addEventAction(interfaces, setAction, whereImplement, MapFact.<CalcPropertyInterfaceImplement<T>, Boolean>EMPTYORDER(), false, Event.SESSION, null, true, false, null);
            return;
        }

        if(!((CalcProperty)whereImplement.property).noDB())
            whereImplement = whereImplement.mapChanged(IncrementType.SET, ChangeEvent.scope);

        ChangeEvent<T> event = new ChangeEvent<>(this, valueImplement, whereImplement);
        // запишем в DataProperty
        for(DataProperty dataProperty : getChangeProps()) {
            if(Settings.get().isCheckUniqueEvent() && dataProperty.event!=null)
                throw new RuntimeException(ThreadLocalContext.localize(new FormatLocalizedString("{logics.property.already.has.event}", dataProperty)));
            dataProperty.event = event;
        }
    }

    public void setNotNull(ImMap<T, DataObject> values, ExecutionEnvironment env, ExecutionStack stack, boolean notNull, boolean check) throws SQLException, SQLHandledException {
        if(!check || (read(env.getSession().sql, values, env.getModifier(), env.getQueryEnv())!=null) != notNull) {
            ActionPropertyMapImplement<?, T> action = getSetNotNullAction(notNull);
            if(action!=null)
                action.execute(new ExecutionContext<>(values, env, stack));
        }
    }
    public void setNotNull(ImRevMap<T, KeyExpr> mapKeys, Where where, ExecutionEnvironment env, boolean notNull, ExecutionStack stack) throws SQLException, SQLHandledException {
        for(ImMap<T, DataObject> row : new Query<>(mapKeys, where).executeClasses(env).keys())
            setNotNull(row, env, stack, notNull, true);
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
        return new CalcPropertyMapImplement<>(this, getIdentityInterfaces());
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<T, V> getImplement(ImOrderSet<V> list) {
        return new CalcPropertyMapImplement<>(this, getMapInterfaces(list));
    }

    // важно для подсветки
    public boolean canBeChanged() {
        ImRevMap<T, KeyExpr> mapKeys = getMapKeys();
        Modifier modifier = Property.defaultModifier;
        try {
            Expr changeExpr = getChangeExpr(); // нижнее условие по аналогии с DataProperty
            Where classWhere = getClassProperty().mapExpr(mapKeys, modifier).getWhere();
            CalcPropertyRevImplement<?, String> valueProperty = getValueClassProperty();
            if(valueProperty != null)
                classWhere = classWhere.and(valueProperty.mapExpr(MapFact.singleton("value", changeExpr), modifier).getWhere()); 
            return !getDataChanges(new PropertyChange<>(mapKeys, changeExpr, classWhere), modifier).isEmpty();
        } catch (SQLException e) { // по идее не должно быть, но на всякий случай
            return false;
        } catch (SQLHandledException e) { // по идее не должно быть
            return false;
        }
    }

    @IdentityStrongLazy // STRONG пришлось поставить из-за использования в политике безопасности
    public ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        ImMap<T, ValueClass> interfaceClasses = getInterfaceClasses(ClassType.tryEditPolicy); // так как в определении propertyDraw также используется FULL, а не ASSERTFULL
        if(interfaceClasses.size() < interfaces.size()) // не все классы есть
            return null;

        if(!canBeChanged())
            return null;

        ImOrderSet<T> listInterfaces = interfaceClasses.keys().toOrderSet();
        ImList<ValueClass> listValues = listInterfaces.mapList(interfaceClasses);
        DefaultChangeActionProperty<T> changeActionProperty = new DefaultChangeActionProperty<>(LocalizedString.create("sys"), this, listInterfaces, listValues, editActionSID, filterProperty);
        return changeActionProperty.getImplement(listInterfaces);
    }

    public boolean setNotNull;
    public boolean reflectionNotNull;

    @Override
    public boolean isSetNotNull() {
        return setNotNull;
    }

    protected PropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping) {
        return new CalcPropertyClassImplement<>(this, classes, mapping);
    }

    private LCP logProperty;

    public LCP getLogProperty() {
        return logProperty;
    }

    public void setLogProperty(LCP logProperty) {
        this.logProperty = logProperty;
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
        Inferred<T> result = or ? Inferred.<T>FALSE() : Inferred.<T>EMPTY();
        for(int i=0,size=operands.size();i<size;i++) {
            Inferred<T> operandInferred = operands.get(i);
            if (i == 0)
                result = operandInferred;
            else
                result = result.op(operandInferred, or, inferType);
        }
        return result;
    }

    public static <I, T extends PropertyInterface> Inferred<T> op(ImMap<I, CalcPropertyInterfaceImplement<T>> operands, ImMap<I, ExClassSet> operandClasses, FunctionSet<I> operandNotNulls, InferType inferType, boolean or) {
        Inferred<T> result = Inferred.EMPTY();
        for(int i=0,size=operands.size();i<size;i++) {
            Inferred<T> operandInferred = operands.getValue(i).mapInferInterfaceClasses(operandClasses.get(operands.getKey(i)), inferType);
            if(!operandNotNulls.contains(operands.getKey(i)))
                operandInferred = operandInferred.orAny();
            if (i == 0)
                result = operandInferred;
            else
                result = result.op(operandInferred, or, inferType);
        }
        return result;
    }
    public static <I, T extends PropertyInterface> Inferred<T> op(ImMap<I, CalcPropertyInterfaceImplement<T>> operands, final ImMap<I, ExClassSet> operandClasses, ImSet<I>[] operandNotNulls, final InferType inferType) {
        ImMap<I, Inferred<T>> inferred = mapInfer(operands, operandClasses, inferType);
        return op(operandNotNulls, inferType, inferred);
    }

    public static <I, T extends PropertyInterface> ImMap<I, Inferred<T>> mapInfer(ImMap<I, CalcPropertyInterfaceImplement<T>> operands, final ImMap<I, ExClassSet> operandClasses, final InferType inferType) {
        return operands.mapValues(new GetKeyValue<Inferred<T>, I, CalcPropertyInterfaceImplement<T>>() {
                public Inferred<T> getMapValue(I key, CalcPropertyInterfaceImplement<T> value) {
                    return value.mapInferInterfaceClasses(operandClasses.get(key), inferType);
                }
            });
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

        // докидываем те которые не учавствуют ни в одном notNull
        return result.and(op(inferred.remove(SetFact.mergeSets(operandNotNulls)).values().toList().mapListValues(new GetValue<Inferred<T>, Inferred<T>>() {
            public Inferred<T> getMapValue(Inferred<T> value) {
                return value.orAny();
            }
        }), false, inferType), inferType);
    }

    public static <T extends PropertyInterface> Inferred<T> op(ImList<CalcPropertyInterfaceImplement<T>> operands, ImList<ExClassSet> operandClasses, int operandNotNullCount, int skipNotNull, InferType inferType, boolean or) {
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
    public Inferred<T> inferInterfaceClasses(InferType inferType) { // эвристично определяет классы, для входных значений
        return inferInterfaceClasses(null, inferType);
    }
    @IdentityStartLazy
    public Inferred<T> inferInterfaceClasses(ExClassSet commonValue, InferType inferType) { // эвристично определяет классы, для входных значений
        return calcInferInterfaceClasses(commonValue, inferType);
    }
    protected Inferred<T> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) { // эвристично определяет классы, для входных значений
        assert this instanceof ChangeProperty || getDepends().isEmpty(); // гарантирует "атомарность" метода
        return new Inferred<>(calcClassValueWhere(CalcClassType.PREVBASE).getCommonExClasses(interfaces));
    }
    @IdentityStartLazy
    public ExClassSet inferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) { // эвристично определяет класс выходного значения
        return calcInferValueClass(inferred, inferType);
    }
    public ExClassSet calcInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) { // эвристично определяет класс выходного значения
        assert this instanceof ChangeProperty || getDepends().isEmpty(); // гарантирует "атомарность" метода
        return calcClassValueWhere(CalcClassType.PREVBASE).getCommonExClasses(SetFact.singleton("value")).get("value");
    }
    public static <I extends PropertyInterface> ExClassSet opInferValueClasses(ImCol<? extends CalcPropertyInterfaceImplement<I>> props, ImMap<I, ExClassSet> inferred, boolean or, InferType inferType) {
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

    // костыль для email
    public static <I extends PropertyInterface> ValueClass[] getCommonClasses(ImList<I> mapInterfaces, ImCol<? extends CalcPropertyInterfaceImplement<I>> props, List<ObjectEntity> formObjects) {
        ValueClass[] result = new ValueClass[mapInterfaces.size()];
        int index = 0;
        for(PropertyInterfaceImplement<I> prop : props) {
            ImMap<I, ValueClass> propClasses;

            if(prop instanceof CalcPropertyMapImplement) {
                if (formObjects.get(index) == null) {
                    propClasses = ((CalcPropertyMapImplement<?, I>) prop).mapInterfaceClasses(ClassType.aroundPolicy);
                } else {
                    propClasses = ((CalcPropertyMapImplement<?, I>) prop).mapInterfaceClasses(ClassType.aroundPolicy, new ExClassSet(formObjects.get(index).baseClass.getResolveSet()));
                }
            } else {
                if (formObjects.get(index) != null && prop instanceof PropertyInterface) {
                    propClasses = MapFact.singleton((I)prop, formObjects.get(index).baseClass);
                } else {
                    propClasses = MapFact.EMPTY();
                }
            }

            for(int i=0;i<result.length;i++)
                result[i] = op(result[i], propClasses.get(mapInterfaces.get(i)), true);

            ++index;
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
    @StackMessage("message.core.property.get.expr")
    @ThisMessage
    public IQuery<T, String> getQuery(CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) {
        if(queryType==PropertyQueryType.FULLCHANGED) {
            IQuery<T, String> query = getQuery(calcType, propChanges, PropertyQueryType.RECURSIVE, interfaceValues);
            QueryBuilder<T, String> fullQuery = new QueryBuilder<>(query.getMapKeys());
            Expr newExpr = query.getExpr("value");
            fullQuery.addProperty("value", newExpr);
            
            Expr dbExpr = getExpr(fullQuery.getMapExprs());
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

    @StackMessage("message.core.property.get.expr")
    @PackComplex
    @ThisMessage
    public Expr getJoinExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return aspectGetExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement) {
        return getExpr(joinImplement, PropertyChanges.EMPTY);
    }
    public Expr getExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType) {
        return getExpr(joinImplement, calcType, PropertyChanges.EMPTY, null);
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
            getQuery(CalcType.EXPR, PropertyChanges.EMPTY, PropertyQueryType.FULLCHANGED, MapFact.<T, Expr>EMPTY()).pack();
    }

    @IdentityInstanceLazy
    public CalcPropertyMapImplement<?, T> getClassProperty() {
        return IsClassProperty.getMapProperty(getInterfaceClasses(ClassType.signaturePolicy));
    }

    @IdentityInstanceLazy
    protected CalcPropertyRevImplement<?, String> getValueClassProperty() {
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

    public DrillDownFormEntity getDrillDownForm(LogicsModule LM, String canonicalName) {
        DrillDownFormEntity drillDown = createDrillDownForm(LM, canonicalName);
        if (drillDown != null) {
            LM.addFormEntity(drillDown);
        }
        return drillDown;
    }

    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return null;
    }

    public boolean isFull(AlgInfoType calcType) { // обозначает что можно вывести классы всех параметров, используется в частности для материализации (stored, hints) чтобы знать типы колонок, или даже в subQuery (для статистики)
        return isFull(interfaces, calcType);
    }
    
    public boolean isNotNull(AlgInfoType algType) { // обозначает что при null одном из параметров - null значение
        if(isFull(algType))
            return true;
        
        if(AlgType.checkInferCalc) checkInferNotNull();
        return algType.isNotNull(this);
    }

    protected boolean calcNotNull(CalcInfoType calcType) {
        return true;
    }

    @IdentityLazy
    public boolean inferNotNull(InferInfoType inferType) {
        return inferInterfaceClasses(inferType).isNotNull(interfaces, inferType);
    }

    public boolean isDrillFull() {
        return isFull(AlgType.drillType);
    }

    public boolean isEmpty(AlgInfoType algType) {
        if(AlgType.checkInferCalc) checkInferEmpty();
        return algType.isEmpty(this);
    }

    protected boolean calcEmpty(CalcInfoType calcType) {
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

    @IdentityLazy
    public boolean allowHintIncrement() {
        assert isFull(AlgType.hintType);

        if(!isEmpty(AlgType.hintType))
            for(ValueClass usedClass : getInterfaceClasses(ClassType.materializeChangePolicy).values().toSet().merge(getValueClass(ClassType.materializeChangePolicy)))
                if(usedClass instanceof OrderClass)
                    return false;

        return true;
    }
    
    @IdentityStartLazy
    public long getComplexity() {
        try {
            return getExpr(getMapKeys(), Property.defaultModifier).getComplexity(false);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public void recalculateClasses(SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {
        recalculateClasses(sql, null, baseClass);
    }

    @StackMessage("logics.recalculating.data.classes")
    public void recalculateClasses(SQLSession sql, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        assert isStored();
        
        ImRevMap<KeyField, KeyExpr> mapKeys = mapTable.table.getMapKeys();
        Where where = DataSession.getIncorrectWhere(this, baseClass, mapTable.mapKeys.join(mapKeys));
        Query<KeyField, PropertyField> query = new Query<>(mapKeys, Expr.NULL, field, where);
        sql.updateRecords(env == null ? new ModifyQuery(mapTable.table, query, OperationOwner.unknown, TableOwner.global) : new ModifyQuery(mapTable.table, query, env, TableOwner.global));
    }

    public void setDebugInfo(CalcPropertyDebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    public CalcPropertyDebugInfo getDebugInfo() {
        return (CalcPropertyDebugInfo) debugInfo;
    }

    @Override
    public String toString() {
        String string = super.toString();
        if (debugInfo != null) {
            string = string + " - " + debugInfo;
        }
        return string;
    }

    public void printDepends(boolean events, String tab) {
        System.out.println(tab + this);
        for(CalcProperty prop : getDepends(events)) {
            prop.printDepends(events, tab + '\t');
        }
    }
}
