package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
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

    public static List<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, getInterfaces(classes));
        this.value = value;
    }

    public <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        SessionChanges session = modifier.getSession();
        return (derivedChange != null ? derivedChange.getUsedChanges(modifier) : modifier.newChanges()).addChanges(
                session.getSessionChanges(this).add(new SessionChanges(session, ClassProperty.getValueClasses(interfaces), true)));
    }

    @Override
    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        return modifier.newChanges().addChanges(new SessionChanges(modifier.getSession(), ClassProperty.getValueClasses(interfaces), false));
    }

    @Override
    public DataChanges getDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
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

    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        SessionChanges session = modifier.getSession();

        Expr dataExpr = getExpr(joinImplement);

        // ручные изменения
        ExprCaseList cases = new ExprCaseList();
        DataChangeTable dataChange = session.data.get(this);
        if(dataChange!=null) {
            Join<PropertyField> changedJoin = dataChange.join(BaseUtils.join(dataChange.mapKeys, joinImplement));
            cases.add(changedJoin.getWhere(),changedJoin.getExpr(dataChange.value));
        }

        // блок с удалением
        Where removeWhere = session.getRemoveWhere(value,dataExpr);
        for(ClassPropertyInterface remove : interfaces)
            removeWhere = removeWhere.or(session.getRemoveWhere(remove.interfaceClass,joinImplement.get(remove)));
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

    public Type getType() {
        return value.getType();
    }
}
