package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassWhere;
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

    static Collection<DataPropertyInterface> getInterfaces(ValueClass[] classes) {
        Collection<DataPropertyInterface> interfaces = new ArrayList<DataPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new DataPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    public DataProperty(String iSID, ValueClass[] classes, ValueClass iValue) {
        super(iSID, getInterfaces(classes));
        value = iValue;
    }

    protected boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {

        // если null то значит полный список запрашивают
        if(changes==null) return true;

        if(changes.getProperties().contains(this)) return true;
        if(value instanceof CustomClass && changes.getRemoveClasses().contains((CustomClass)value)) return true;
        for(DataPropertyInterface propertyInterface : interfaces)
            if(changes.getRemoveClasses().contains(propertyInterface.interfaceClass)) return true;

        DefaultData defaultData = defaultProps.get(this);
        return (defaultData!=null && defaultData.property.fillChanges(changedProperties, changes, BaseUtils.removeKey(defaultProps,this), noUpdateProps));
    }

    @Override
    public MapChangeDataProperty<DataPropertyInterface> getChangeProperty(Map<DataPropertyInterface, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        if(allInInterface(new AndClassWhere<DataPropertyInterface>(interfaceClasses)) && (securityPolicy == null || securityPolicy.checkPermission(this)))
            return new MapChangeDataProperty<DataPropertyInterface>(this,BaseUtils.toMap(interfaces),false);
        else
            return null;
    }

    @Override
    public void fillTableChanges(TableChanges fill, TableChanges changes) {
        super.fillTableChanges(fill, changes);
        BaseUtils.putNotNull(this,changes.data.get(this),fill.data);
        if(value instanceof CustomClass)
            BaseUtils.putNotNull((CustomClass) value,changes.remove.get((CustomClass)value),fill.remove);
        for(DataPropertyInterface remove : interfaces)
            if(remove.interfaceClass instanceof CustomClass) // удаление по интерфейсам совпадает
                BaseUtils.putNotNull((CustomClass) remove.interfaceClass,changes.remove.get((CustomClass)remove.interfaceClass),fill.remove);
    }

    public SourceExpr calculateSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {
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
            cases.add(removeWhere.and(dataExpr.getWhere()),new CaseExpr());

        // свойства по умолчанию
        DefaultData defaultData = defaultProps.get(this);
        if(defaultData !=null) {
            WhereBuilder defaultChanges = new WhereBuilder();
            SourceExpr defaultExpr = defaultData.property.getSourceExpr(BaseUtils.join(defaultData.mapping, joinImplement),session, BaseUtils.removeKey(defaultProps,this), noUpdateProps, defaultChanges);
            cases.add(defaultChanges.toWhere(),defaultExpr);
        }

        if(changedWhere !=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE,dataExpr);
        return new CaseExpr(cases);
    }

    protected Map<DataPropertyInterface, ValueClass> getMapClasses() {
        Map<DataPropertyInterface, ValueClass> result = new HashMap<DataPropertyInterface, ValueClass>();
        for(DataPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        AndClassWhere<Field> result = new AndClassWhere<Field>();
        for(Map.Entry<DataPropertyInterface,KeyField> mapKey : mapTable.mapKeys.entrySet())
            result.add(mapKey.getValue(), mapKey.getKey().interfaceClass.getUpSet());
        result.add(storedField, value.getUpSet());
        return new ClassWhere<Field>(result);
    }

    public ValueClass getValueClass() {
        return value;
    }
    public Type getType() {
        return value.getType();
    }
}
