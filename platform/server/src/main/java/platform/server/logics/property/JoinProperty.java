package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.interop.Compare;

import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends FunctionProperty<JoinProperty.Interface> {
    public PropertyImplement<PropertyInterfaceImplement<Interface>,T> implement;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    public JoinProperty(String sID, String caption, int intNum, boolean implementChange) {
        super(sID, caption, getInterfaces(intNum));
        this.implementChange = implementChange;
    }

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceImplement : implement.mapping.entrySet())
            result.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(joinImplement, modifier, changedWhere));
        return result;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {
        return implement.property.getExpr(getJoinImplements(joinImplement, modifier, changedWhere), modifier, changedWhere);
    }

    @Override
    public void fillDepends(Set<Property> depends) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);       
    }

    // разрешить менять основное свойство
    public final boolean implementChange;
    
    @Override
    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            U result = modifier.newChanges();
            for(Property<?> property : getDepends()) {
                result.add(property.getUsedDataChanges(modifier));
                result.add(property.getUsedChanges(modifier));
            }
        }

        if(implementChange) {
            U result = modifier.newChanges();
            result.add(implement.property.getUsedDataChanges(modifier));
            Set<Property> implementProps = new HashSet<Property>();
            fillDepends(implementProps,implement.mapping.values());
            result.add(Property.getUsedChanges(implementProps,modifier));
            return result;
        }


        return super.getUsedDataChanges(modifier);
    }

    private static DataChanges getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, TableModifier<? extends TableChanges> modifier, PropertyInterfaceImplement<Interface> changeImp, PropertyInterfaceImplement<Interface> valueImp) {
        return changeImp.mapJoinDataChanges(change.mapKeys,valueImp.mapExpr(change.mapKeys,modifier,null).and(change.expr.getWhere()),change.where,changedWhere,modifier);
    }

    @Override
    public DataChanges getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, TableModifier<? extends TableChanges> modifier) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<PropertyInterfaceImplement<Interface>> i = implement.mapping.values().iterator();
            PropertyInterfaceImplement<Interface> op1 = i.next();
            PropertyInterfaceImplement<Interface> op2 = i.next();

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder changedWhere1 = new WhereBuilder();
            DataChanges result1 = getDataChanges(change, changedWhere1, modifier, op1, op2);
            if(changedWhere!=null) changedWhere.add(changedWhere1.toWhere());

            return new DataChanges(result1, getDataChanges(change.and(changedWhere1.toWhere().not()), changedWhere, modifier, op2, op1));
        }

        if(implementChange) // groupBy'им выбирая max
            return implement.property.getJoinDataChanges(getJoinImplements(change.mapKeys, modifier, null),change.expr,change.where,modifier,changedWhere);

        return super.getDataChanges(change, changedWhere, modifier);
    }

    @Override
    public PropertyValueImplement getChangeProperty(Map<Interface, DataObject> mapValues) {
        if(implement.mapping.size()==1)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).mapChangeProperty(mapValues);
        else
            return super.getChangeProperty(mapValues);
    }

}
