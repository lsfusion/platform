package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.Join;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.types.Type;
import platform.server.session.*;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.util.*;

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
    public MapChangeDataProperty<DataPropertyInterface> getChangeProperty(Map<DataPropertyInterface, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        if(allInInterface(interfaceClasses) && (securityPolicy == null || securityPolicy.checkPermission(this)))
            return new MapChangeDataProperty<DataPropertyInterface>(this,BaseUtils.toMap(interfaces),false);
        else
            return null;
    }


    @Override
    protected void fillDepends(Set<Property> depends) {
        if(defaultData!=null) defaultData.fillDepends(depends);
    }

    public DefaultData<?> defaultData = null;

    public SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        SessionChanges session = modifier.getSession();
        assert session!=null;

        SourceExpr dataExpr = getSourceExpr(joinImplement);

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
            cases.add(removeWhere.and(dataExpr.getWhere()),CaseExpr.NULL);

        // свойства по умолчанию
        if(defaultData !=null) {
            WhereBuilder defaultChanges = new WhereBuilder();
            SourceExpr defaultExpr = defaultData.getSourceExpr(joinImplement, modifier,defaultChanges);
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
