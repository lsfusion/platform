package lsfusion.server.logics.property;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.form.entity.drilldown.DataDrillDownFormEntity;
import lsfusion.server.form.entity.drilldown.DrillDownFormEntity;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.actions.ChangeEvent;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.session.DataChanges;
import lsfusion.server.session.PropertyChange;
import lsfusion.server.session.PropertyChanges;
import lsfusion.server.session.StructChanges;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public abstract class DataProperty extends CalcProperty<ClassPropertyInterface> {

    public ValueClass value;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public DataProperty(String caption, ValueClass[] classes, ValueClass value) {
        super(caption, IsClassProperty.getInterfaces(classes));
        this.value = value;
    }

    public ClassWhere<Object> calcClassValueWhere(CalcClassType calcType) {
        return new ClassWhere<>(MapFact.<Object, ValueClass>addExcl(IsClassProperty.getMapClasses(interfaces), "value", value), true);
    }

    // перегружаем из-за assertion'а с depends
    public Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(ExClassSet.toExValue(IsClassProperty.getMapClasses(interfaces)));
    }
    public ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toExValue(value);
    }

    public ChangeEvent<?> event = null;

    protected boolean useSimpleIncrement() {
        return false;
    }

    @IdentityInstanceLazy
    protected CalcPropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        assert !noClasses();
        return IsClassProperty.getProperty(interfaces);
    }

    // для for'а hack, так как там unknown может быть
    protected boolean noClasses() {
        return false;
    }

    @Override
    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        if(noClasses())
            return SetFact.EMPTY();
        
        return propChanges.getUsedChanges(SetFact.toSet((CalcProperty) getClassProperty().property, (CalcProperty) getValueClassProperty().property));
    }

    @Override
    public ImSet<DataProperty> getChangeProps() {
        return SetFact.singleton(this);
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(!noClasses()) // нижнее условие по аналогии с canBeChanged
            change = change.and(getClassProperty().mapExpr(change.getMapExprs(), propChanges, null).getWhere().and(getValueClassProperty().mapExpr(MapFact.singleton("value", change.expr), propChanges, null).getWhere().or(change.expr.getWhere().not())));
        
        if(change.where.isFalse()) // чтобы не плодить пустые change'и
            return DataChanges.EMPTY;

        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        return new DataChanges(this, change);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        ImSet<CalcProperty> result = SetFact.EMPTY();
        
        if(!noClasses()) {
            result = result.merge(value.getProperty().getRemoveUsedChanges(propChanges));
            for (ClassPropertyInterface remove : interfaces)
                result = result.merge(remove.interfaceClass.getProperty().getRemoveUsedChanges(propChanges));
        }
        if (event != null)
            result = result.merge(event.getUsedDataChanges(propChanges));
        return result;
    }

    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {

        Expr prevExpr = getExpr(joinImplement);

        PropertyChange<ClassPropertyInterface> change = getEventChange(propChanges, getJoinValues(joinImplement));
        if (change != null) {
            WhereBuilder changedExprWhere = new WhereBuilder();
            Expr changedExpr = change.getExpr(joinImplement, changedExprWhere);
            if (changedWhere != null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), prevExpr);
        }

        return prevExpr;
    }

    public PropertyChange<ClassPropertyInterface> getEventChange(PropertyChanges changes, ImMap<ClassPropertyInterface, Expr> joinValues) {
        PropertyChange<ClassPropertyInterface> result = null;

        PropertyChange<ClassPropertyInterface> eventChange = null; // до непосредственно вычисления, для хинтов
        if(event!=null)
            eventChange = ((ChangeEvent<ClassPropertyInterface>)event).getDataChanges(changes, event.isData() ? joinValues : MapFact.<ClassPropertyInterface, Expr>EMPTY()).get(this);


        if(!noClasses()) {
            ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
            ImMap<ClassPropertyInterface, Expr> mapExprs = MapFact.override(mapKeys, joinValues);
            Expr prevExpr = null;
            Where removeWhere = Where.FALSE;
            for (ClassPropertyInterface remove : interfaces) {
                IsClassProperty classProperty = remove.interfaceClass.getProperty();
                if (classProperty.hasChanges(changes)) {
                    if (prevExpr == null) // оптимизация
                        prevExpr = getExpr(mapExprs);
                    removeWhere = removeWhere.or(classProperty.getRemoveWhere(mapExprs.get(remove), changes).and(prevExpr.getWhere()));
                }
            }
            IsClassProperty classProperty = value.getProperty();
            if (classProperty.hasChanges(changes)) {
                if (prevExpr == null) // оптимизация
                    prevExpr = getExpr(mapExprs);
                removeWhere = removeWhere.or(classProperty.getRemoveWhere(prevExpr, changes));
            }
            if (!removeWhere.isFalse())
                result = PropertyChange.addNull(result, new PropertyChange<>(mapKeys, removeWhere, joinValues));
        }

        if(eventChange!=null)
            result = PropertyChange.addNull(result, eventChange);

        return result;
    }

    @Override
    protected void fillDepends(MSet<CalcProperty> depends, boolean events) { // для Action'а связь считается слабой
        if(events) {
            if (event != null)
                depends.addAll(event.getDepends());
            if (!noClasses()) {
                for (ClassPropertyInterface remove : interfaces)
                    if (remove.interfaceClass instanceof CustomClass)
                        depends.add(((CustomClass) remove.interfaceClass).getProperty().getChanged(IncrementType.DROP, ChangeEvent.scope));
                if (value instanceof CustomClass)
                    depends.add(((CustomClass) value).getProperty().getChanged(IncrementType.DROP, ChangeEvent.scope));
            }
        }
    }

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean events) {
        ImCol<Pair<Property<?>, LinkType>> result = super.calculateLinks(events);
        if(events)
            result = result.mergeCol(getActionChangeProps()); // только у Data и IsClassProperty
        return result;
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<ClassPropertyInterface, V> getMapInterfaces(ImOrderSet<V> list) {
        return getOrderInterfaces().mapSet(list);
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, V> getImplement(ImOrderSet<V> list) {
        return new CalcPropertyMapImplement<>(this, getMapInterfaces(list));
    }

    public <V> CalcPropertyRevImplement<ClassPropertyInterface, V> getRevImplement(ImOrderSet<V> list) {
        return new CalcPropertyRevImplement<>(this, getMapInterfaces(list));
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
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new DataDrillDownFormEntity(
                canonicalName, getString("logics.property.drilldown.form.data"), this, LM
        );
    }

    @Override
    public String getChangeExtSID() {
        assert false; // так как должно быть canonicalName
        return null;
    }
}
