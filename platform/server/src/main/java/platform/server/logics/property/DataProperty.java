package platform.server.logics.property;

import platform.base.Pair;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.form.entity.drilldown.DataDrillDownFormEntity;
import platform.server.form.entity.drilldown.DrillDownFormEntity;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.actions.ChangeEvent;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import static platform.base.BaseUtils.capitalize;
import static platform.server.logics.ServerResourceBundle.getString;

public abstract class DataProperty extends CalcProperty<ClassPropertyInterface> {

    public ValueClass value;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, IsClassProperty.getInterfaces(classes));
        this.value = value;
    }

    @Override
    public ClassWhere<Object> getClassValueWhere(boolean full) {
        return new ClassWhere<Object>(MapFact.<Object, ValueClass>addExcl(IsClassProperty.getMapClasses(interfaces), "value", value), true);
    }

    public ChangeEvent<?> event = null;

    protected boolean useSimpleIncrement() {
        return false;
    }

    @IdentityInstanceLazy
    protected CalcPropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @IdentityInstanceLazy
    protected CalcPropertyRevImplement<?, String> getValueClassProperty() {
        return IsClassProperty.getProperty(value, "value");
    }

    @Override
    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(SetFact.toSet((CalcProperty) getInterfaceClassProperty().property, (CalcProperty) getValueClassProperty().property));
    }

    @Override
    public ImSet<DataProperty> getChangeProps() {
        return SetFact.singleton(this);
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        change = change.and(getInterfaceClassProperty().mapExpr(change.getMapExprs(), propChanges, null).getWhere().and(getValueClassProperty().mapExpr(MapFact.singleton("value", change.expr), propChanges, null).getWhere().or(change.expr.getWhere().not())));
        if(change.where.isFalse()) // чтобы не плодить пустые change'и
            return DataChanges.EMPTY;

        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        return new DataChanges(this, change);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        ImSet<CalcProperty> result = value.getProperty().getUsedChanges(propChanges);
        for(ClassPropertyInterface remove : interfaces)
            result = result.merge(remove.interfaceClass.getProperty().getUsedChanges(propChanges));
        if(event !=null)
            result = result.merge(event.getUsedDataChanges(propChanges));
        return result;
    }

    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        PropertyChange<ClassPropertyInterface> change = getEventChange(propChanges, getJoinValues(joinImplement));

        Expr prevExpr = getExpr(joinImplement);

        if(change!=null) {
            WhereBuilder changedExprWhere = new WhereBuilder();
            Expr changedExpr = change.getExpr(joinImplement, changedExprWhere);
            if(changedWhere!=null) changedWhere.add(changedExprWhere.toWhere());
            return changedExpr.ifElse(changedExprWhere.toWhere(), prevExpr);
        }

        return prevExpr;
    }

    public PropertyChange<ClassPropertyInterface> getEventChange(PropertyChanges changes, ImMap<ClassPropertyInterface, Expr> joinValues) {
        PropertyChange<ClassPropertyInterface> result = null;

        PropertyChange<ClassPropertyInterface> eventChange = null; // до непосредственно вычисления, для хинтов
        if(event!=null)
            eventChange = ((ChangeEvent<ClassPropertyInterface>)event).getDataChanges(changes, event.isData() ? joinValues : MapFact.<ClassPropertyInterface, Expr>EMPTY()).get(this);


        ImRevMap<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        ImMap<ClassPropertyInterface, Expr> mapExprs = MapFact.override(mapKeys, joinValues);
        Expr prevExpr = null;
        Where removeWhere = Where.FALSE;
        for(ClassPropertyInterface remove : interfaces) {
            IsClassProperty classProperty = remove.interfaceClass.getProperty();
            if(classProperty.hasChanges(changes)) {
                if(prevExpr==null) // оптимизация
                    prevExpr = getExpr(mapExprs);
                removeWhere = removeWhere.or(classProperty.getRemoveWhere(mapExprs.get(remove), changes).and(prevExpr.getWhere()));
            }
        }
        IsClassProperty classProperty = value.getProperty();
        if(classProperty.hasChanges(changes)) {
            if(prevExpr==null) // оптимизация
                prevExpr = getExpr(mapExprs);
            removeWhere = removeWhere.or(classProperty.getRemoveWhere(prevExpr, changes));
        }
        if(!removeWhere.isFalse())
            result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, removeWhere, joinValues));

        if(eventChange!=null)
            result = PropertyChange.addNull(result, eventChange);

        return result;
    }

    @Override
    protected void fillDepends(MSet<CalcProperty> depends, boolean events) { // для Action'а связь считается слабой
        if(events && event != null)
            depends.addAll(event.getDepends());
    }

    @Override
    public ImSet<CalcProperty> calculateRecDepends() { // именно в recdepends, потому как в depends "порушиться"
        return super.calculateRecDepends().merge(interfaces.mapMergeSetValues(new GetValue<CalcProperty, ClassPropertyInterface>() {
            public CalcProperty getMapValue(ClassPropertyInterface value) {
                return value.interfaceClass.getProperty();
            }})).merge(value.getProperty());
    }

    @Override
    protected ImCol<Pair<Property<?>, LinkType>> calculateLinks() {
        MCol<Pair<Property<?>, LinkType>> mResult = ListFact.mCol();

        mResult.addAll(getActionChangeProps()); // только у Data и IsClassProperty
        MSet<ChangedProperty> mRemoveDepends = SetFact.mSet();
        for(ClassPropertyInterface remove : interfaces)
            if(remove.interfaceClass instanceof CustomClass)
                mRemoveDepends.add(((CustomClass)remove.interfaceClass).getProperty().getChanged(IncrementType.DROP));
        if(value instanceof CustomClass)
            mRemoveDepends.add(((CustomClass)value).getProperty().getChanged(IncrementType.DROP));
        for(CalcProperty property : mRemoveDepends.immutable())
            mResult.add(new Pair<Property<?>, LinkType>(property, LinkType.EVENTACTION));

        return super.calculateLinks().mergeCol(mResult.immutableCol()); // чтобы удаления классов зацеплять
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<ClassPropertyInterface, V> getMapInterfaces(ImOrderSet<V> list) {
        return getOrderInterfaces().mapSet(list);
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, V> getImplement(ImOrderSet<V> list) {
        return new CalcPropertyMapImplement<ClassPropertyInterface, V>(this, getMapInterfaces(list));
    }
    
    public boolean depends(ImSet<CustomClass> cls) { // оптимизация
        if(SetFact.contains(value, cls))
            return true;

        for(ClassPropertyInterface propertyInterface : interfaces)
            if(SetFact.contains(propertyInterface.interfaceClass, cls))
                return true;

        return false;
    }

    @Override
    public boolean supportsDrillDown() {
        return event != null;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new DataDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.data"), this, BL
        );
    }
}
