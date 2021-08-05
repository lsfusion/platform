package lsfusion.server.logics.property.data;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.*;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.action.session.classes.changed.RegisterClassRemove;
import lsfusion.server.logics.action.session.table.PropertyChangeTableUsage;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.event.ChangeEvent;
import lsfusion.server.logics.event.LinkType;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.drilldown.form.DataDrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.Set;

public abstract class DataProperty extends AbstractDataProperty {

    public ValueClass value;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public DataProperty(LocalizedString caption, ValueClass[] classes, ValueClass value) {
        super(caption, IsClassProperty.getInterfaces(classes));
        this.value = value;
    }

    @Override
    protected ClassWhere<Object> getDataClassValueWhere() {
        return new ClassWhere<>(MapFact.addExcl(IsClassProperty.getMapClasses(interfaces), "value", value), true);
    }

    public ChangeEvent<?> event = null;

    protected boolean useSimpleIncrement() {
        return false;
    }

    @IdentityInstanceLazy
    protected PropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        assert !noClasses();
        return IsClassProperty.getProperty(interfaces);
    }

    // для for'а hack, так как там unknown может быть
    protected boolean noClasses() {
        return false;
    }

    @Override
    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        if(noClasses())
            return SetFact.EMPTY();
        
        return propChanges.getUsedChanges(SetFact.toSet(getClassProperty().property, getValueClassProperty().property));
    }

    @Override
    public ImSet<DataProperty> getChangeProps() {
        return SetFact.singleton(this);
    }

    @StackMessage("{message.check.data.classes}")
    @ThisMessage
    public ModifyResult checkClasses(PropertyChangeTableUsage<ClassPropertyInterface> change, SQLSession sql, BaseClass baseClass, QueryEnvironment env, Modifier modifier, OperationOwner owner, Runnable checkTransaction, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        return checkClasses(change, sql, baseClass, env, modifier.getPropertyChanges(), owner, checkTransaction, classRemove, timestamp);        
    }
    
    public ModifyResult checkClasses(PropertyChangeTableUsage<ClassPropertyInterface> change, SQLSession sql, BaseClass baseClass, QueryEnvironment env, PropertyChanges propertyChanges, OperationOwner owner, Runnable checkTransaction, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        if(noClasses())
            return ModifyResult.NO;

        Result<ImSet<ClassPropertyInterface>> checkKeyChanges = new Result<>();
        Result<Boolean> checkValueChange = new Result<>();
        AlgType classType = AlgType.storedType; // actually id doesn't really matter because it is dataproperty
        boolean updatedClasses = change.checkClasses(sql, baseClass, true, owner, true, getInterfaceClasses(classType), getValueClass(classType), checkKeyChanges, checkValueChange, checkTransaction, classRemove, timestamp); // тут фиг поймешь какое policy
        
        ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        Join<String> changeJoin = change.join(mapKeys);
        Expr changeExpr = changeJoin.getExpr("value");
        Where wrongWhere = changeJoin.getWhere().and(getIsClassWhere(propertyChanges, mapKeys.filterInclRev(checkKeyChanges.result), checkValueChange.result ? changeExpr : null).not());
        
        ModifyResult deleted = ModifyResult.NO;
        if(!wrongWhere.isFalse()) { // оптимизация
            if(checkTransaction != null)
                checkTransaction.run();
            deleted = change.modifyRows(sql, new Query<>(mapKeys, change.getWhere(mapKeys).and(wrongWhere)), baseClass, Modify.DELETE, env, false); // только что их собственно обновили
        }
        
        if(updatedClasses)
            return ModifyResult.DATA_SOURCE; // формально в этом случае (если deleted - NO) мог только source изменится, но оптимизировать это особого смысла нет 
        return deleted; 
    }

    @Override
    public boolean canBeHeurChanged(boolean global) {
        if(global)
            return !(this instanceof SessionDataProperty);
        return true;
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(!noClasses()) // нижнее условие по аналогии с canBeChanged
            change = change.and(getIsClassWhere(propChanges, change.getMapExprs(), change.expr));
        
        if(change.where.isFalse()) // чтобы не плодить пустые change'и
            return DataChanges.EMPTY;

        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        return new DataChanges(this, change);
    }

    private Where getIsClassWhere(PropertyChanges propChanges, ImMap<ClassPropertyInterface, ? extends Expr> mapKeys, Expr expr) {
        Where result = getClassProperty(mapKeys.keys()).mapExpr(mapKeys, propChanges, null).getWhere();
        if(expr != null)
            result = result.and(getValueClassProperty().mapExpr(MapFact.singleton("value", expr), propChanges, null).getWhere().or(expr.getWhere().not()));
        return result;
    }

    public ImSet<Property> calculateUsedChanges(StructChanges propChanges) {
        ImSet<Property> result = SetFact.EMPTY();
        
        if(!noClasses()) {
            result = result.merge(value.getProperty().getRemoveUsedChanges(propChanges));
            for (ClassPropertyInterface remove : interfaces)
                result = result.merge(remove.interfaceClass.getProperty().getRemoveUsedChanges(propChanges));
        }
        if (event != null)
            result = result.merge(event.getUsedDataChanges(propChanges));
        return result;
    }

    protected boolean calculateHasPreread(StructChanges structChanges) {
        if (event != null)
            return event.hasPreread(structChanges);
        return false;
    }

    @Override
    public boolean calculateCheckRecursions(ImSet<CaseUnionProperty> abstractPath, ImSet<Property> path, Set<Property> marks) {
        if (event != null)
            return event.where.property.calculateCheckRecursions(abstractPath, path, marks);
        return false;
    }

    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {

        Expr prevExpr = getPrevExpr(joinImplement, calcType, propChanges);

        PropertyChange<ClassPropertyInterface> change = getEventChange(calcType, propChanges, getJoinValues(joinImplement));
        if (change != null) {
            WhereBuilder changedExprWhere = new WhereBuilder();
            Expr changedExpr = change.getExpr(joinImplement, changedExprWhere);
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), prevExpr);
        }

        return prevExpr;
    }

    public PropertyChange<ClassPropertyInterface> getEventChange(CalcType calcType, PropertyChanges changes, ImMap<ClassPropertyInterface, Expr> joinValues) {
        PropertyChange<ClassPropertyInterface> result = null;

        PropertyChange<ClassPropertyInterface> eventChange = null; // до непосредственно вычисления, для хинтов
        if(event!=null)
            eventChange = ((ChangeEvent<ClassPropertyInterface>)event).getDataChanges(changes, event.isData() ? joinValues : MapFact.EMPTY()).get(this);


        if(!noClasses()) {
            PropertyChanges prevPropChanges = getPrevPropChanges(calcType, changes);
            ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
            ImMap<ClassPropertyInterface, Expr> mapExprs = MapFact.override(mapKeys, joinValues);
            Expr prevExpr = null;
            Where removeWhere = Where.FALSE();
            for (ClassPropertyInterface remove : interfaces) {
                IsClassProperty classProperty = remove.interfaceClass.getProperty();
                if (classProperty.hasChanges(changes)) {
                    if (prevExpr == null) // оптимизация
                        prevExpr = getExpr(mapExprs, prevPropChanges);
                    removeWhere = removeWhere.or(classProperty.getDroppedWhere(mapExprs.get(remove), changes).and(prevExpr.getWhere()));
                }
            }
            IsClassProperty classProperty = value.getProperty();
            if (classProperty.hasChanges(changes)) {
                if (prevExpr == null) // оптимизация
                    prevExpr = getExpr(mapExprs, prevPropChanges);
                removeWhere = removeWhere.or(classProperty.getDroppedWhere(prevExpr, changes));
            }
            if (!removeWhere.isFalse())
                result = PropertyChange.addNull(result, new PropertyChange<>(mapKeys, removeWhere, joinValues));
        }

        if(eventChange!=null)
            result = PropertyChange.addNull(result, eventChange);

        return result;
    }

    @Override
    protected void fillDepends(MSet<Property> depends, boolean events) { // для Action'а связь считается слабой
        if(events) {
            if (event != null)
                depends.addAll(event.getDepends());
            depends.addAll(getDroppedDepends());
        }
    }

    public ImSet<Property> getSingleApplyDroppedIsClassProps() {
        MSet<Property> mResult = SetFact.mSet();
        for(ChangedProperty<?> removeDepend : getDroppedDepends())
            mResult.addAll(removeDepend.getSingleApplyDroppedIsClassProps());
        return mResult.immutable();
    }

    public ImSet<ChangedProperty> getDroppedDepends() {
        if (!noClasses()) {
            MSet<ChangedProperty> mResult = SetFact.mSet(); 
            for (ClassPropertyInterface remove : interfaces)
                if (remove.interfaceClass instanceof CustomClass)
                    mResult.add(remove.interfaceClass.getProperty().getChanged(IncrementType.DROP, ChangeEvent.scope));
            if (value instanceof CustomClass)
                mResult.add(value.getProperty().getChanged(IncrementType.DROP, ChangeEvent.scope));
            return mResult.immutable();
        }
        return SetFact.EMPTY();
    }

    @Override
    protected ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events) {
        ImCol<Pair<ActionOrProperty<?>, LinkType>> result = super.calculateLinks(events);
        if(events)
            result = result.mergeCol(getActionChangeProps()); // только у Data и IsClassProperty
        return result;
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<ClassPropertyInterface, V> getMapInterfaces(ImOrderSet<V> list) {
        return getOrderInterfaces().mapSet(list);
    }

    public <V extends PropertyInterface> PropertyMapImplement<ClassPropertyInterface, V> getImplement(ImOrderSet<V> list) {
        return new PropertyMapImplement<>(this, getMapInterfaces(list));
    }

    public <V> PropertyRevImplement<ClassPropertyInterface, V> getRevImplement(ImOrderSet<V> list) {
        return new PropertyRevImplement<>(this, getMapInterfaces(list));
    }

    public boolean depends(ImSet<CustomClass> cls) { // оптимизация
        if(!noClasses()) {
            if (SetFact.contains(value, cls))
                return true;

            for (ClassPropertyInterface propertyInterface : interfaces)
                if (SetFact.contains(propertyInterface.interfaceClass, cls))
                    return true;
        }
            
        return false;
    }

    @Override
    public boolean supportsDrillDown() {
        return event != null;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new DataDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.data}"), this, LM
        );
    }

    @Override
    public boolean supportsReset() {
        return getType() instanceof FileClass;
    }

    @Override
    public String getChangeExtSID() {
        assert false; // так как должно быть canonicalName
        return null;
    }
}
