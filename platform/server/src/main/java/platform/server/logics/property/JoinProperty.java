package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
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

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceImplement : implement.mapping.entrySet())
            result.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(joinImplement, modifier, changedWhere));
        return result;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return implement.property.getExpr(getJoinImplements(joinImplement, modifier, changedWhere), modifier, changedWhere);
    }

    @Override
    public void fillDepends(Set<Property> depends, boolean derived) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);       
    }

    // разрешить менять основное свойство
    public final boolean implementChange;
    
    @Override
    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            U result = modifier.newChanges();
            for(Property<?> property : getDepends())
                result = result.add(property.getUsedDataChanges(modifier)).add(property.getUsedChanges(modifier));
            return result;
        }

        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Set<Property> depends = new HashSet<Property>();
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface)
                    implement.mapping.get(andInterface).mapFillDepends(depends);
            Set<Property> implementDepends = new HashSet<Property>();
            implement.mapping.get(andProperty.objectInterface).mapFillDepends(implementDepends);
            return modifier.getUsedDataChanges(implementDepends).add(modifier.getUsedChanges(depends));
        }

        if(implementChange) {
            Set<Property> implementProps = new HashSet<Property>();
            fillDepends(implementProps,implement.mapping.values());
            return implement.property.getUsedDataChanges(modifier).add(modifier.getUsedChanges(implementProps));
        }

        return super.getUsedDataChanges(modifier);
    }

    private static MapDataChanges<Interface> getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier, PropertyInterfaceImplement<Interface> changeImp, PropertyInterfaceImplement<Interface> valueImp) {
        Expr toChangeExpr = valueImp.mapExpr(change.mapKeys, modifier, null);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(change.mapKeys, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то  
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(change.mapKeys, modifier, null),Compare.EQUALS))), changedWhere, modifier);
    }

    @Override
    public MapDataChanges<Interface> getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<PropertyInterfaceImplement<Interface>> i = implement.mapping.values().iterator();
            PropertyInterfaceImplement<Interface> op1 = i.next();
            PropertyInterfaceImplement<Interface> op2 = i.next();

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder changedWhere1 = new WhereBuilder();
            MapDataChanges<Interface> result1 = getDataChanges(change, changedWhere1, modifier, op1, op2);
            if(changedWhere!=null) changedWhere.add(changedWhere1.toWhere());

            return result1.add(getDataChanges(change.and(changedWhere1.toWhere().not()), changedWhere, modifier, op2, op1));
        }

        if(implement.property instanceof AndFormulaProperty) {
            Where where = Where.TRUE;
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    Where andWhere = implement.mapping.get(andInterface).mapExpr(change.mapKeys,modifier,null).getWhere();
                    if(((AndFormulaProperty.AndInterface)andInterface).not)
                        andWhere = andWhere.not();
                    where = where.and(andWhere);
                }
            return implement.mapping.get(andProperty.objectInterface).mapJoinDataChanges(change.mapKeys, change.expr, change.where.and(where), changedWhere, modifier);
        }

        if(implementChange) { // groupBy'им выбирая max
            Map<T, Interface> mapInterfaces = new HashMap<T, Interface>();
            for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceMap : implement.mapping.entrySet())
                if(interfaceMap.getValue() instanceof Interface)
                    mapInterfaces.put(interfaceMap.getKey(), (Interface) interfaceMap.getValue());
            return implement.property.getJoinDataChanges(getJoinImplements(change.mapKeys, modifier, null), change.expr, change.where, modifier, changedWhere).map(mapInterfaces);
        }

        return super.getDataChanges(change, changedWhere, modifier);
    }

    @Override
    public PropertyMapImplement<?,Interface> getChangeImplement() {
        if(implement.mapping.size()==1 && !implementChange)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).mapChangeImplement();
        else
            return super.getChangeImplement();
    }
}
