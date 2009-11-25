package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
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
import platform.server.logics.DataObject;

import java.util.*;

import net.jcip.annotations.Immutable;

@Immutable
public class DataProperty extends Property<DataPropertyInterface> {

    public ValueClass value;

    public static Collection<DataPropertyInterface> getInterfaces(ValueClass[] classes) {
        Collection<DataPropertyInterface> interfaces = new ArrayList<DataPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new DataPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public DataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, getInterfaces(classes));
        this.value = value;

        valueInterface = new DataPropertyInterface(100,value);
    }

    <U extends DataChanges<U>> U calculateUsedChanges(Modifier<U> modifier) {
        U result = modifier.newChanges();
        if(defaultData!=null) {
            U defaultChanges = defaultData.getUsedChanges(modifier);
            if(defaultChanges.hasChanges()) // если есть изменения по default'ам
                ClassProperty.modifyClasses(interfaces, modifier,defaultChanges);
            result.add(defaultChanges);
        }
        modifier.modifyData(result,this);
        modifier.modifyRemove(result, value);
        for(DataPropertyInterface propertyInterface : interfaces)
            modifier.modifyRemove(result,propertyInterface.interfaceClass);
        return result;
    }

    @Override
    public DataChange getChangeProperty(DataSession session, Map<DataPropertyInterface, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        return getJoinChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID);
    }

    @Override
    public PropertyChange getJoinChangeProperty(DataSession session, Map<DataPropertyInterface, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        if(allInInterface(session.getCurrentClasses(interfaceValues)) && (securityPolicy == null || securityPolicy.checkPermission(this)))
            return new DataPropertyChange(this,interfaceValues);
        else
            return null;
    }

    @Override
    protected void fillDepends(Set<Property> depends) {
        if(defaultData!=null) defaultData.fillDepends(depends);
    }

    public DefaultData<?> defaultData = null;

    public Expr calculateExpr(Map<DataPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

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

        for(DataPropertyInterface remove : interfaces)
            if(remove.interfaceClass instanceof CustomClass && (removeTable = session.remove.get((CustomClass) remove.interfaceClass))!=null)
                removeWhere = removeWhere.or(removeTable.getJoinWhere(joinImplement.get(remove)));

        if(!removeWhere.isFalse())
            cases.add(removeWhere.and(dataExpr.getWhere()), Expr.NULL);

        // свойства по умолчанию
        if(defaultData !=null) {
            WhereBuilder defaultChanges = new WhereBuilder();
            Expr defaultExpr = defaultData.getExpr(joinImplement, modifier,defaultChanges);
            cases.add(defaultChanges.toWhere().and(ClassProperty.getIsClassWhere(joinImplement, modifier, null)),defaultExpr);
        }

        if(changedWhere !=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE,dataExpr);
        return cases.getExpr();
    }

    protected boolean usePreviousStored() {
        return false;
    }

    protected Map<DataPropertyInterface, ValueClass> getMapClasses() {
        Map<DataPropertyInterface, ValueClass> result = new HashMap<DataPropertyInterface, ValueClass>();
        for(DataPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Map<Field, AndClassSet> result = new HashMap<Field, AndClassSet>();
        for(Map.Entry<DataPropertyInterface,KeyField> mapKey : mapTable.mapKeys.entrySet())
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

    public final DataPropertyInterface valueInterface;
}
