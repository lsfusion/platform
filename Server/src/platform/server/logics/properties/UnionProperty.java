package platform.server.logics.properties;

import platform.base.SetBuilder;
import platform.server.data.PropertyField;
import platform.server.data.Source;
import platform.server.data.Union;
import platform.server.data.query.*;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.LinearExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;

import java.util.*;

abstract public class UnionProperty extends AggregateProperty<PropertyInterface> {

    UnionProperty(TableFactory iTableFactory,Union iOperator) {
        super(iTableFactory);
        operator = iOperator;
    }

    // имплементации св-в (полные)
    public List<PropertyMapImplement<PropertyInterface,PropertyInterface>> operands = new ArrayList<PropertyMapImplement<PropertyInterface, PropertyInterface>>();

    Union operator;
    // коэффициенты
    public Map<PropertyMapImplement<PropertyInterface,PropertyInterface>,Integer> coeffs = new HashMap<PropertyMapImplement<PropertyInterface, PropertyInterface>, Integer>();

    SourceExpr calculateSourceExpr(Map<PropertyInterface,? extends SourceExpr> joinImplement, InterfaceClassSet<PropertyInterface> joinClasses) {

        String valueString = "joinvalue";
        OperationQuery<PropertyInterface,String> resultQuery = new OperationQuery<PropertyInterface,String>(interfaces, operator);
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> operand : operands) {
            if(operand.mapIsInInterface(joinClasses)) {
                JoinQuery<PropertyInterface,String> query = new JoinQuery<PropertyInterface, String>(interfaces);
                query.properties.put(valueString, operand.mapSourceExpr(query.mapKeys, joinClasses));
                resultQuery.add(query, coeffs.get(operand));
            }
        }

        return (new Join<PropertyInterface,String>(resultQuery, joinImplement)).exprs.get(valueString);
    }

    public ClassSet calculateValueClass(InterfaceClass<PropertyInterface> ClassImplement) {
        // в отличии от Relation только когда есть хоть одно св-во
        ClassSet ResultClass = new ClassSet();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            ResultClass.or(Operand.mapValueClass(ClassImplement));
        return ResultClass;
    }

    public InterfaceClassSet<PropertyInterface> calculateClassSet(ClassSet reqValue) {
        // в отличии от Relation игнорируем null
        InterfaceClassSet<PropertyInterface> Result = new InterfaceClassSet<PropertyInterface>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            Result.or(Operand.mapClassSet(reqValue));
        return Result;
    }

    public ValueClassSet<PropertyInterface> calculateValueClassSet() {
        ValueClassSet<PropertyInterface> Result = new ValueClassSet<PropertyInterface>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            Result.or(Operand.mapValueClassSet());
        return Result;
    }

    public boolean fillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        if(ChangedProperties.contains(this)) return true;
        if(NoUpdate.contains(this)) return false;

        boolean Changed = false;

        for(PropertyMapImplement Operand : operands)
            Changed = Operand.mapFillChangedList(ChangedProperties, Changes, NoUpdate) || Changed;

        if(Changed)
            ChangedProperties.add(this);

        return Changed;
    }

    List<PropertyMapImplement<PropertyInterface,PropertyInterface>> getChangedProperties(DataSession Session) {

        List<PropertyMapImplement<PropertyInterface,PropertyInterface>> ChangedProperties = new ArrayList<PropertyMapImplement<PropertyInterface,PropertyInterface>>();
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : operands)
            if(Operand.mapHasChanges(Session)) ChangedProperties.add(Operand);

        return ChangedProperties;
    }

    // определяет ClassSet подмн-ва и что все операнды пересекаются
    ValueClassSet<PropertyInterface> getChangeClassSet(DataSession Session,List<PropertyMapImplement<PropertyInterface,PropertyInterface>> ChangedProps) {

        ValueClassSet<PropertyInterface> Result = new ValueClassSet<PropertyInterface>(new ClassSet(),getUniversalInterface());
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> Operand : ChangedProps)// {
//            if(!intersect(Session, Operand,ChangedProps)) return null;
            Result = Result.and(Operand.mapValueClassSet(Session));
//        }

        return Result;
    }


    public Change incrementChanges(DataSession session, int changeType) {

        if(getUnionType()==0 && changeType ==1) changeType = 2;

        //      	0                   1                           2
        //Max(0)	значение,SS,LJ      не может быть               значение,SS,LJ,prevv
        //Sum(1)	значение,SS,LJ      значение,без SS, без LJ     значение,SS,LJ,prevv
        //Override(2)	значение,SS,LJ      старое поле=null,SS, LJ     значение,SS,LJ,prevv

        ValueClassSet<PropertyInterface> resultClass = new ValueClassSet<PropertyInterface>();

        // неструктурно как и все оптимизации
        if(operator == Union.SUM && changeType ==1) {
            OperationQuery<PropertyInterface, PropertyField> resultQuery = new OperationQuery<PropertyInterface, PropertyField>(interfaces, Union.SUM);

            for(PropertyMapImplement<PropertyInterface,PropertyInterface> operand : getChangedProperties(session)) {
                JoinQuery<PropertyInterface, PropertyField> query = new JoinQuery<PropertyInterface, PropertyField>(interfaces);
                JoinExpr changeExpr = operand.mapChangeExpr(session, query.mapKeys, 1);
                query.properties.put(changeTable.value, changeExpr);
                query.and(changeExpr.from.inJoin);
                resultQuery.add(query, coeffs.get(operand));

                resultClass.or(operand.mapValueClassSet(session));
            }

            return new Change(1,resultQuery, resultClass);
        } else {
            ChangeQuery<PropertyInterface,PropertyField> resultQuery = new ChangeQuery<PropertyInterface, PropertyField>(interfaces);
            resultQuery.add(getChange(session, changeType ==1?1:0, changeTable.value,resultClass));
            if(changeType ==2) resultQuery.add(getChange(session,2, changeTable.prevValue,resultClass));

            return new Change(changeType,resultQuery,resultClass);
        }
    }

    Source<PropertyInterface,PropertyField> getChange(DataSession session, int mapType, PropertyField value, ValueClassSet<PropertyInterface> resultClass) {

        ChangeQuery<PropertyInterface, PropertyField> resultQuery = new ChangeQuery<PropertyInterface, PropertyField>(interfaces);

        ListIterator<List<PropertyMapImplement<PropertyInterface,PropertyInterface>>> il = SetBuilder.buildSubSetList(getChangedProperties(session)).listIterator();
        // пропустим пустое подмн-во
        il.next();
        while(il.hasNext()) {
            List<PropertyMapImplement<PropertyInterface, PropertyInterface>> changedProps = il.next();

            // проверим что все попарно пересекаются по классам, заодно строим InterfaceClassSet<T> св-в
            ValueClassSet<PropertyInterface> changeClass = getChangeClassSet(session,changedProps);
            if(changeClass.isEmpty()) continue;

            JoinQuery<PropertyInterface, PropertyField> query = new JoinQuery<PropertyInterface, PropertyField>(interfaces);
            List<SourceExpr> resultOperands = new ArrayList<SourceExpr>();
            // именно в порядке операндов (для Overrid'а важно)
            for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : operands)
                if(changedProps.contains(operand)) {
                    JoinExpr changedExpr = operand.mapChangeExpr(session, query.mapKeys, mapType);
                    resultOperands.add(new LinearExpr(changedExpr, coeffs.get(operand)));
                    query.and(changedExpr.from.inJoin);
                } else { // AND'им как если Join результат
                    ValueClassSet<PropertyInterface> leftClass = changeClass.and(operand.mapValueClassSet());
                    if(!leftClass.isEmpty()) {
                        resultOperands.add(new LinearExpr(operand.mapSourceExpr(query.mapKeys, leftClass.getClassSet(ClassSet.universal)), coeffs.get(operand)));
                        // значит может изменится Value на другое значение
                        changeClass.or(leftClass);
                    }
                }

            query.properties.put(value, OperationQuery.getExpr(resultOperands, operator));

            resultQuery.add(query);
            resultClass.or(changeClass);
        }
        if(resultQuery.where.isFalse())
            resultQuery.properties.put(value, getType().getExpr(null));

        return resultQuery;
    }

    boolean intersect(DataSession Session, PropertyMapImplement<PropertyInterface,PropertyInterface> Operand, Collection<PropertyMapImplement<PropertyInterface,PropertyInterface>> Operands) {
        for(PropertyMapImplement<PropertyInterface,PropertyInterface> IntersectOperand : Operands) {
            if(Operand==IntersectOperand) return true;
            if(!intersect(Session, Operand,IntersectOperand)) return false;
        }
        return true;
    }

    // проверяет пересекаются по классам операнды или нет
    boolean intersect(DataSession Session, PropertyMapImplement<PropertyInterface,PropertyInterface> Operand, PropertyMapImplement<PropertyInterface,PropertyInterface> IntersectOperand) {
        return (Session.changes.addClasses.size() > 0 && Session.changes.removeClasses.size() > 0) ||
               !Operand.mapClassSet(ClassSet.universal).and(IntersectOperand.mapClassSet(ClassSet.universal)).isEmpty();
//        return true;
    }

    public Integer getIncrementType(Collection<Property> ChangedProps, Set<Property> ToWait) {
        return getUnionType();
    }

    Integer getUnionType() {
        return 0;
    }

    public void fillRequiredChanges(Integer IncrementType, Map<Property, Integer> RequiredTypes) {
        if(getUnionType()==0 && IncrementType.equals(1)) IncrementType = 2;

        for(PropertyMapImplement Operand : operands)
            Operand.property.setChangeType(RequiredTypes,IncrementType);
    }

    List<PropertyMapImplement<PropertyInterface, PropertyInterface>> getImplements(Map<PropertyInterface, ObjectValue> Keys, ChangePropertySecurityPolicy securityPolicy) {
        return operands;
    }

    int getCoeff(PropertyMapImplement<?, PropertyInterface> Implement) {
        return coeffs.get(Implement);
    }
}
