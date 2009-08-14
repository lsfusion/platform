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

    public DataProperty(String iSID, ValueClass[] classes, ValueClass iValue) {
        super(iSID, getInterfaces(classes));
        value = iValue;
    }

    <C extends DataChanges<C>,U extends UsedChanges<C,U>> U calculateUsedChanges(C changes, Collection<DataProperty> usedDefault, Depends<C, U> depends) {
        U result = depends.newChanges();
        if(defaultData!=null) {
            if(!usedDefault.contains(this)) {
                U defaultChanges = defaultData.getUsedChanges(changes, BaseUtils.add(usedDefault, this), depends);
                if(defaultChanges.hasChanges()) // если есть изменения по default'ам
                    ClassProperty.dependsClasses(interfaces,changes,defaultChanges);
                result.add(defaultChanges, this);
            } else
                result.usedDefault.add(this);
        }
        result.dependsData(changes,this);
        result.dependsRemove(changes, value);
        for(DataPropertyInterface propertyInterface : interfaces)
            result.dependsRemove(changes,propertyInterface.interfaceClass);
        return result;
    }

    @Override
    public MapChangeDataProperty<DataPropertyInterface> getChangeProperty(Map<DataPropertyInterface, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        if(allInInterface(interfaceClasses) && (securityPolicy == null || securityPolicy.checkPermission(this)))
            return new MapChangeDataProperty<DataPropertyInterface>(this,BaseUtils.toMap(interfaces),false);
        else
            return null;
    }

    public DefaultData<?> defaultData;

    public SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {
        // здесь session всегда не null

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
        if(defaultData !=null && !usedDefault.contains(this)) {
            WhereBuilder defaultChanges = new WhereBuilder();
            SourceExpr defaultExpr = defaultData.getSourceExpr(joinImplement,session,BaseUtils.add(usedDefault,this),depends,defaultChanges); 
            cases.add(defaultChanges.toWhere().and(ClassProperty.getIsClassWhere(joinImplement, session, usedDefault, depends, null)),defaultExpr);
        }

        if(changedWhere !=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE,dataExpr);
        return cases.getExpr();
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
}
