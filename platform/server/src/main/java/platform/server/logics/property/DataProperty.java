package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.property.actions.ChangeEvent;
import platform.server.session.*;

import java.util.*;

import static platform.base.BaseUtils.add;
import static platform.base.BaseUtils.remove;

public abstract class DataProperty extends CalcProperty<ClassPropertyInterface> {

    public ValueClass value;

    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, IsClassProperty.getInterfaces(classes));
        this.value = value;
    }

    @Override
    public ClassWhere<Object> getClassValueWhere() {
        return new ClassWhere<Object>(BaseUtils.<Object, ValueClass>add(IsClassProperty.getMapClasses(interfaces), "value", value), true);
    }

    public ChangeEvent<?> event = null;

    protected Set<CalcProperty> getEventDepends() {
        return event !=null ? event.getDepends(true) : new HashSet<CalcProperty>();
    }

    protected boolean useSimpleIncrement() {
        return false;
    }

    @IdentityLazy
    protected CalcPropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @IdentityLazy
    protected CalcPropertyImplement<?, String> getValueClassProperty() {
        return IsClassProperty.getProperty(value, "value");
    }

    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(BaseUtils.toSet((CalcProperty) getInterfaceClassProperty().property, (CalcProperty) getValueClassProperty().property));
    }

    @Override
    public Collection<DataProperty> getChangeProps() {
        return Collections.singleton(this);
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        change = change.and(getInterfaceClassProperty().mapExpr(change.getMapExprs(), propChanges, null).getWhere().and(getValueClassProperty().mapExpr(Collections.singletonMap("value", change.expr), propChanges, null).getWhere().or(change.expr.getWhere().not())));
        if(change.where.isFalse()) // чтобы не плодить пустые change'и
            return new DataChanges();

        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        return new DataChanges(this, change);
    }

    public QuickSet<CalcProperty> calculateUsedChanges(StructChanges propChanges, boolean cascade) {
        return QuickSet.EMPTY();
    }

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        throw new RuntimeException("should not be"); // так как stored должен
    }

    public PropertyChange<ClassPropertyInterface> getEventChange(PropertyChanges changes) {
        PropertyChange<ClassPropertyInterface> result = null;

        Map<ClassPropertyInterface, KeyExpr> mapKeys = getMapKeys();
        Expr prevExpr = null;
        for(ClassPropertyInterface remove : interfaces) {
            IsClassProperty classProperty = remove.interfaceClass.getProperty();
            if(classProperty.hasChanges(changes)) {
                if(prevExpr==null) // оптимизация
                    prevExpr = getExpr(mapKeys);
                result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, classProperty.getRemoveWhere(mapKeys.get(remove), changes).and(prevExpr.getWhere())));
            }
        }
        IsClassProperty classProperty = value.getProperty();
        if(classProperty.hasChanges(changes)) {
            if(prevExpr==null) // оптимизация
                prevExpr = getExpr(mapKeys);
            result = PropertyChange.addNull(result, new PropertyChange<ClassPropertyInterface>(mapKeys, classProperty.getRemoveWhere(prevExpr, changes)));
        }

        if(event !=null && event.hasEventChanges(changes)) {
            PropertyChange<ClassPropertyInterface> propertyChange = event.getDataChanges(changes).get(this);
            result = PropertyChange.addNull(result, propertyChange != null ? propertyChange : getNoChange()); // noChange для прикола с stored в aspectgetexpr (там hasChanges стоит, а с null'ом его не будет)
        }

        return result;
    }

    @Override
    public QuickSet<CalcProperty> getUsedEventChange(StructChanges propChanges, boolean cascade) {
        QuickSet<CalcProperty> result = super.getUsedEventChange(propChanges, cascade).merge(value.getProperty().getUsedChanges(propChanges, cascade));
        for(ClassPropertyInterface remove : interfaces)
            result = result.merge(remove.interfaceClass.getProperty().getUsedChanges(propChanges, cascade));
        if(event !=null && event.hasEventChanges(propChanges, cascade))
            result = result.merge(event.getUsedDataChanges(propChanges, cascade));
        return result;
    }

    @Override
    protected void fillDepends(Set<CalcProperty> depends, boolean events) { // для Action'а связь считается слабой
        if(events) depends.addAll(getEventDepends());
    }

    @Override
    public QuickSet<CalcProperty> calculateRecDepends() { // именно в recdepends, потому как в depends "порушиться"
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>(super.calculateRecDepends());
        for(ClassPropertyInterface remove : interfaces)
            result.addAll(remove.interfaceClass.getProperty());
        result.add(value.getProperty());
        return result;
    }

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();

        result.addAll(actionChangeProps); // только у Data и IsClassProperty
        Set<ChangedProperty> removeDepends = new HashSet<ChangedProperty>();
        for(ClassPropertyInterface remove : interfaces)
            if(remove.interfaceClass instanceof CustomClass)
                removeDepends.add(((CustomClass)remove.interfaceClass).getProperty().getChanged(IncrementType.DROP));
        if(value instanceof CustomClass)
            removeDepends.add(((CustomClass)value).getProperty().getChanged(IncrementType.DROP));
        for(CalcProperty property : removeDepends)
            result.add(new Pair<Property<?>, LinkType>(property, LinkType.EVENTACTION));

        return BaseUtils.merge(super.calculateLinks(), result); // чтобы удаления классов зацеплять
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> Map<ClassPropertyInterface, V> getMapInterfaces(List<V> list) {
        int i=0;
        Map<ClassPropertyInterface, V> result = new HashMap<ClassPropertyInterface, V>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface, list.get(i++));
        return result;
    }

    public <V extends PropertyInterface> CalcPropertyMapImplement<ClassPropertyInterface, V> getImplement(List<V> list) {
        return new CalcPropertyMapImplement<ClassPropertyInterface, V>(this, getMapInterfaces(list));
    }
    
    public boolean depends(Set<CustomClass> cls) { // оптимизация
        if(cls.contains(value))
            return true;

        for(ClassPropertyInterface propertyInterface : interfaces)
            if(cls.contains(propertyInterface.interfaceClass))
                return true;

        return false;
    }
}
