package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.query.Join;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.type.Type;
import platform.server.session.*;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

import java.util.*;

import net.jcip.annotations.Immutable;

@Immutable
public class DataProperty extends Property<ClassPropertyInterface> {

    public ValueClass value;

    public static Collection<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        Collection<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, getInterfaces(classes));
        this.value = value;
    }

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        U result = modifier.newChanges();
        if(derivedChange !=null) result.add(derivedChange.getUsedChanges(modifier));
        modifier.modifyData(result, this);
        modifier.modifyRemove(result, value);
        for(ClassPropertyInterface propertyInterface : interfaces)
            modifier.modifyRemove(result,propertyInterface.interfaceClass);
        return result;
    }

    @Override
    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        U changes = modifier.newChanges();
        ClassProperty.modifyClasses(interfaces,modifier,changes);
        return changes;
    }

    @Override
    public DataChanges getDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, TableModifier<? extends TableChanges> modifier) {
        change = change.and(ClassProperty.getIsClassWhere(change.mapKeys, modifier, null));//.and(DataSession.getIsClassWhere(modifier.getSession(), change.expr, value, null));
        if(changedWhere!=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы

        // изменяет себя, если классы совпадают
        return new DataChanges(this,change);
    }

    @Override
    protected void fillDepends(Set<Property> depends) {
        if(derivedChange !=null) derivedChange.fillDepends(depends);
    }

    public DerivedChange<?,?> derivedChange = null;

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        SessionChanges session = modifier.getSession();
        assert session!=null;

        Expr dataExpr = getExpr(joinImplement);

        // ручные изменения
        ExprCaseList cases = new ExprCaseList();
        DataChangeTable dataChange = session.data.get(this);
        if(dataChange!=null) {
            Join<PropertyField> changedJoin = dataChange.join(BaseUtils.join(dataChange.mapKeys, joinImplement));
            cases.add(changedJoin.getWhere(),changedJoin.getExpr(dataChange.value));
        }

        // блок с удалением
        RemoveClassTable removeTable;
        Where removeWhere = Where.FALSE;
        if(value instanceof CustomClass && (removeTable = session.remove.get((CustomClass) value))!=null)
            removeWhere = removeWhere.or(removeTable.getJoinWhere(dataExpr));

        for(ClassPropertyInterface remove : interfaces)
            if(remove.interfaceClass instanceof CustomClass && (removeTable = session.remove.get((CustomClass) remove.interfaceClass))!=null)
                removeWhere = removeWhere.or(removeTable.getJoinWhere(joinImplement.get(remove)));

        if(!removeWhere.isFalse())
            cases.add(removeWhere.and(dataExpr.getWhere()), Expr.NULL);

        // свойства по умолчанию
        if(derivedChange !=null) {
            PropertyChange<ClassPropertyInterface> defaultChanges = derivedChange.getDataChanges(modifier).get(this);
            if(defaultChanges !=null) {
                Join<String> defaultJoin = defaultChanges.getQuery("value").join(joinImplement);
                cases.add(defaultJoin.getWhere(),defaultJoin.getExpr("value"));
            }
        }

        if(changedWhere !=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE,dataExpr);
        return cases.getExpr();
    }

    protected boolean usePreviousStored() {
        return false;
    }

    protected Map<ClassPropertyInterface, ValueClass> getMapClasses() {
        Map<ClassPropertyInterface, ValueClass> result = new HashMap<ClassPropertyInterface, ValueClass>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Map<Field, AndClassSet> result = new HashMap<Field, AndClassSet>();
        for(Map.Entry<ClassPropertyInterface,KeyField> mapKey : mapTable.mapKeys.entrySet())
            result.put(mapKey.getValue(), mapKey.getKey().interfaceClass.getUpSet());
        result.put(storedField, value.getUpSet());
        return new ClassWhere<Field>(result);
    }

    public ValueClass getValueClass() {
        return value;
    }
    public Type getType() {
        return value.getType();
    }
}
