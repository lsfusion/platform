package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.Result;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends SimpleIncrementProperty<JoinProperty.Interface> {
    public PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement;

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

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceImplement : implement.mapping.entrySet())
            result.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(joinImplement, propChanges, changedWhere));
        return result;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return implement.property.getExpr(getJoinImplements(joinImplement, propChanges, changedWhere), propChanges, changedWhere);
    }

    @Override
    public void fillDepends(Set<Property> depends, boolean derived) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);       
    }

    // разрешить менять основное свойство
    public final boolean implementChange;
    
    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            QuickSet<Property> result = new QuickSet<Property>();
            for(Property<?> property : getDepends()) {
                result.addAll(property.getUsedDataChanges(propChanges));
                result.addAll(property.getUsedChanges(propChanges));
            }
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
            return QuickSet.add(propChanges.getUsedDataChanges(implementDepends), propChanges.getUsedChanges(depends));
        }

        if(implement.property.isOnlyNotZero)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).property.getUsedDataChanges(propChanges);

        if(implementChange) {
            Set<Property> implementProps = new HashSet<Property>();
            fillDepends(implementProps,implement.mapping.values());
            return QuickSet.add(implement.property.getUsedDataChanges(propChanges), propChanges.getUsedChanges(implementProps));
        }
        if(implement.mapping.size()==1 && !implementChange && implement.property.aggProp) {
            // пока тупо MGProp'им назад
            return QuickSet.add(((PropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property.getUsedDataChanges(propChanges), implement.property.getUsedChanges(propChanges));
        }

        return super.calculateUsedDataChanges(propChanges);
    }

    private static MapDataChanges<Interface> getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges, PropertyInterfaceImplement<Interface> changeImp, PropertyInterfaceImplement<Interface> valueImp) {
        Expr toChangeExpr = valueImp.mapExpr(change.mapKeys, propChanges);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(change.mapKeys, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то  
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(change.mapKeys, propChanges),Compare.EQUALS))), changedWhere, propChanges);
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<PropertyInterfaceImplement<Interface>> i = implement.mapping.values().iterator();
            PropertyInterfaceImplement<Interface> op1 = i.next();
            PropertyInterfaceImplement<Interface> op2 = i.next();

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder changedWhere1 = new WhereBuilder();
            MapDataChanges<Interface> result1 = getDataChanges(change, changedWhere1, propChanges, op1, op2);
            if(changedWhere!=null) changedWhere.add(changedWhere1.toWhere());

            return result1.add(getDataChanges(change.and(changedWhere1.toWhere().not()), changedWhere, propChanges, op2, op1));
        }

        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Where where = Where.TRUE;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    Where andWhere = implement.mapping.get(andInterface).mapExpr(change.mapKeys, propChanges).getWhere();
                    if(((AndFormulaProperty.AndInterface)andInterface).not)
                        andWhere = andWhere.not();
                    where = where.and(andWhere);
                }
            return implement.mapping.get(andProperty.objectInterface).mapJoinDataChanges(change.mapKeys, change.expr, change.where.and(where), changedWhere, propChanges);
        }

        if(implement.property.isOnlyNotZero)
            return ((PropertyMapImplement<?,Interface>)BaseUtils.singleValue(implement.mapping)).mapDataChanges(change, changedWhere, propChanges);

        if(implementChange) { // groupBy'им выбирая max
            Map<T, Interface> mapInterfaces = new HashMap<T, Interface>();
            for(Map.Entry<T,PropertyInterfaceImplement<Interface>> interfaceMap : implement.mapping.entrySet())
                if(interfaceMap.getValue() instanceof Interface)
                    mapInterfaces.put(interfaceMap.getKey(), (Interface) interfaceMap.getValue());
            return implement.property.getJoinDataChanges(getJoinImplements(change.mapKeys, propChanges, null), change.expr, change.where, propChanges, changedWhere).map(mapInterfaces);
        }
        if(implement.mapping.size()==1 && !implementChange && implement.property.aggProp) {
            // пока тупо MGProp'им назад
            PropertyMapImplement<?, Interface> implementSingle = (PropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping);
            KeyExpr keyExpr = new KeyExpr("key");
            Expr groupExpr = GroupExpr.create(Collections.singletonMap(0, implement.property.getExpr(Collections.singletonMap(BaseUtils.single(implement.property.interfaces), keyExpr), propChanges)),
                    keyExpr, keyExpr.isClass(implementSingle.property.getCommonClasses().value.getUpSet()), GroupType.ANY, Collections.singletonMap(0, change.expr));
            return implementSingle.mapDataChanges(
                    new PropertyChange<Interface>(change.mapKeys, groupExpr, change.where), changedWhere, propChanges);
        }

        return super.calculateDataChanges(change, changedWhere, propChanges);
    }

    @Override
    public PropertyMapImplement<?, Interface> modifyChangeImplement(Result<Property> aggProp, Map<Interface, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException {
        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    Object read = implement.mapping.get(andInterface).read(session, interfaceValues, modifier);
                    if((read!=null) == ((AndFormulaProperty.AndInterface)andInterface).not)
                        return null; // не подходит
                }
            return implement.mapping.get(andProperty.objectInterface).mapChangeImplement(interfaceValues, session, modifier);
        }
        if(implement.mapping.size()==1 && !implementChange) {
            aggProp.set(implement.property);
            return BaseUtils.singleValue(implement.mapping).mapChangeImplement(interfaceValues, session, modifier);
        }
        return super.modifyChangeImplement(aggProp, interfaceValues, session, modifier);
    }

    public boolean checkEquals() {
        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>> andImplement
                    = (PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                return ((PropertyMapImplement) objectIface).property.checkEquals();
            }
        }

        return implement.property.checkEquals();
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
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);

        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>> andImplement
                    = (PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                ((PropertyMapImplement) objectIface).property.proceedDefaultDesign(propertyView, view);
            }
        }
    }

    @Override
    public Set<Property> getChangeProps() {
        return implement.property.getChangeProps();
    }
}
