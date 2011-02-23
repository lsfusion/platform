package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.session.Changes;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

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
    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
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

        if(implement.property.isOnlyNotZero)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).property.getUsedDataChanges(modifier);

        if(implementChange) {
            Set<Property> implementProps = new HashSet<Property>();
            fillDepends(implementProps,implement.mapping.values());
            return implement.property.getUsedDataChanges(modifier).add(modifier.getUsedChanges(implementProps));
        }

        return super.calculateUsedDataChanges(modifier);
    }

    private static MapDataChanges<Interface> getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier, PropertyInterfaceImplement<Interface> changeImp, PropertyInterfaceImplement<Interface> valueImp) {
        Expr toChangeExpr = valueImp.mapExpr(change.mapKeys, modifier);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(change.mapKeys, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то  
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(change.mapKeys, modifier),Compare.EQUALS))), changedWhere, modifier);
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
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
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Where where = Where.TRUE;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    Where andWhere = implement.mapping.get(andInterface).mapExpr(change.mapKeys,modifier).getWhere();
                    if(((AndFormulaProperty.AndInterface)andInterface).not)
                        andWhere = andWhere.not();
                    where = where.and(andWhere);
                }
            return implement.mapping.get(andProperty.objectInterface).mapJoinDataChanges(change.mapKeys, change.expr, change.where.and(where), changedWhere, modifier);
        }

        if(implement.property.isOnlyNotZero)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).mapDataChanges(change, changedWhere, modifier);

        if(implementChange) { // groupBy'им выбирая max
            Map<T, Interface> mapInterfaces = new HashMap<T, Interface>();
            for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceMap : implement.mapping.entrySet())
                if(interfaceMap.getValue() instanceof Interface)
                    mapInterfaces.put(interfaceMap.getKey(), (Interface) interfaceMap.getValue());
            return implement.property.getJoinDataChanges(getJoinImplements(change.mapKeys, modifier, null), change.expr, change.where, modifier, changedWhere).map(mapInterfaces);
        }

        return super.calculateDataChanges(change, changedWhere, modifier);
    }

    @Override
    public PropertyMapImplement<?,Interface> getChangeImplement() {
        if(implement.mapping.size()==1 && !implementChange)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).mapChangeImplement();
        else
            return super.getChangeImplement();
    }

    @Override
    public Property getFilterProperty() {
        return implement.property;
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<Interface> entity, FormEntity form) {
        super.proceedDefaultDraw(entity, form);
        if (implement.mapping.size() == 1 && ((PropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property instanceof ObjectClassProperty) {
            PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(entity.propertyObject.mapping);
            if (mapObject instanceof ObjectEntity && !((CustomClass) ((ObjectEntity) mapObject).baseClass).hasChildren())
                entity.forceViewType = ClassViewType.HIDE;
        }
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<Interface> entity) {
        super.proceedDefaultDesign(view, entity);

        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<PropertyInterfaceImplement<Interface>, AndFormulaProperty.Interface> andImplement
                    = (PropertyImplement<PropertyInterfaceImplement<Interface>, AndFormulaProperty.Interface>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                ((PropertyMapImplement) objectIface).property.proceedDefaultDesign(view, entity);
            }
        }
    }
}
