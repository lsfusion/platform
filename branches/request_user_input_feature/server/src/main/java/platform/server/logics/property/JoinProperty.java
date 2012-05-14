package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.classes.CustomClass;
import platform.server.classes.LogicalClass;
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
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.*;

import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends SimpleIncrementProperty<JoinProperty.Interface> {
    public final CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    public JoinProperty(String sID, String caption, List<Interface> interfaces, boolean implementChange, CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement) {
        super(sID, caption, interfaces);
        this.implement = implement;
        this.implementChange = implementChange;

        finalizeInit();
    }

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getJoinImplements(joinImplement, false, propChanges, changedWhere);
    }

    private Map<T, Expr> getJoinImplements(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<T, Expr> result = new HashMap<T, Expr>();
        for(Map.Entry<T,CalcPropertyInterfaceImplement<Interface>> interfaceImplement : implement.mapping.entrySet())
            result.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(joinImplement, propClasses, propChanges, changedWhere));
        return result;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return implement.property.getExpr(getJoinImplements(joinImplement, propClasses, propChanges, changedWhere), propClasses, propChanges, changedWhere);
    }

    @Override
    public void fillDepends(Set<CalcProperty> depends, boolean events) {
        fillDepends(depends,implement.mapping.values());
        depends.add((CalcProperty) implement.property);
    }

    // разрешить менять основное свойство
    public final boolean implementChange;
    
    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
            for(CalcProperty<?> property : getDepends()) {
                result.addAll(property.getUsedDataChanges(propChanges));
                result.addAll(property.getUsedChanges(propChanges));
            }
            return result;
        }

        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Set<CalcProperty> depends = new HashSet<CalcProperty>();
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface)
                    implement.mapping.get(andInterface).mapFillDepends(depends);
            Set<CalcProperty> implementDepends = new HashSet<CalcProperty>();
            implement.mapping.get(andProperty.objectInterface).mapFillDepends(implementDepends);
            return QuickSet.add(propChanges.getUsedDataChanges(implementDepends), propChanges.getUsedChanges(depends));
        }

        CalcProperty<T> implementProperty = (CalcProperty<T>) implement.property;
        if(implementChange) {
            Set<CalcProperty> implementProps = new HashSet<CalcProperty>();
            fillDepends(implementProps,implement.mapping.values());
            return QuickSet.add(implementProperty.getUsedDataChanges(propChanges), propChanges.getUsedChanges(implementProps));
        }
        if(implement.mapping.size()==1 && !implementChange && implementProperty.aggProp) {
            // пока тупо MGProp'им назад
            return QuickSet.add(((CalcProperty<?>)((CalcPropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property).getUsedDataChanges(propChanges), implementProperty.getUsedChanges(propChanges));
        }

        return super.calculateUsedDataChanges(propChanges);
    }

    private static DataChanges getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges, CalcPropertyInterfaceImplement<Interface> changeImp, CalcPropertyInterfaceImplement<Interface> valueImp) {
        Map<Interface, Expr> mapExprs = change.getMapExprs();
        Expr toChangeExpr = valueImp.mapExpr(mapExprs, propChanges);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(mapExprs, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(mapExprs, propChanges),Compare.EQUALS))), changedWhere, propChanges);
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<CalcPropertyInterfaceImplement<Interface>> i = implement.mapping.values().iterator();
            CalcPropertyInterfaceImplement<Interface> op1 = i.next();
            CalcPropertyInterfaceImplement<Interface> op2 = i.next();

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder changedWhere1 = new WhereBuilder();
            DataChanges result1 = getDataChanges(change, changedWhere1, propChanges, op1, op2);
            if(changedWhere!=null) changedWhere.add(changedWhere1.toWhere());

            return result1.add(getDataChanges(change.and(changedWhere1.toWhere().not()), changedWhere, propChanges, op2, op1));
        }

        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            Map<Interface, Expr> mapExprs = change.getMapExprs();
            Where where = Where.TRUE;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    Where andWhere = implement.mapping.get(andInterface).mapExpr(mapExprs, propChanges).getWhere();
                    if(((AndFormulaProperty.AndInterface)andInterface).not)
                        andWhere = andWhere.not();
                    where = where.and(andWhere);
                }
            return implement.mapping.get(andProperty.objectInterface).mapJoinDataChanges(mapExprs, change.expr, change.where.and(where), changedWhere, propChanges);
        }

        CalcProperty<T> implementProperty = (CalcProperty<T>) implement.property;
        if(implementChange) { // groupBy'им выбирая max
            Map<T, Interface> mapInterfaces = new HashMap<T, Interface>();
            for(Map.Entry<T,CalcPropertyInterfaceImplement<Interface>> interfaceMap : implement.mapping.entrySet())
                if(interfaceMap.getValue() instanceof Interface)
                    mapInterfaces.put(interfaceMap.getKey(), (Interface) interfaceMap.getValue());
            return implementProperty.getJoinDataChanges(getJoinImplements(change.getMapExprs(), propChanges, null), change.expr, change.where, propChanges, changedWhere); //.map(mapInterfaces)
        }
        if(implement.mapping.size()==1 && !implementChange && implementProperty.aggProp) {
            // пока тупо MGProp'им назад
            CalcPropertyMapImplement<?, Interface> implementSingle = (CalcPropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping);
            KeyExpr keyExpr = new KeyExpr("key");
            Expr groupExpr = GroupExpr.create(Collections.singletonMap(0, implementProperty.getExpr(Collections.singletonMap(BaseUtils.single(implementProperty.interfaces), keyExpr), propChanges)),
                    keyExpr, keyExpr.isClass(implementSingle.property.getCommonClasses().value.getUpSet()), GroupType.ANY, Collections.singletonMap(0, change.expr));
            return implementSingle.mapDataChanges(
                    new PropertyChange<Interface>(change, groupExpr), changedWhere, propChanges);
        }

        return super.calculateDataChanges(change, changedWhere, propChanges);
    }

    @Override
    public ActionPropertyMapImplement<Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        if(implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)implement.property;
            List<CalcPropertyInterfaceImplement<Interface>> ands = new ArrayList<CalcPropertyInterfaceImplement<Interface>>();
            List<Boolean> nots = new ArrayList<Boolean>();
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface) {
                    ands.add(implement.mapping.get(andInterface));
                    nots.add(((AndFormulaProperty.AndInterface)andInterface).not);
                }
            ActionPropertyMapImplement<Interface> implementEdit = implement.mapping.get(andProperty.objectInterface).mapEditAction(editActionSID, filterProperty);
            if(implementEdit!=null)
                return DerivedProperty.createIfAction(interfaces, DerivedProperty.createAnd(interfaces, DerivedProperty.<Interface>createStatic(true, LogicalClass.instance), ands, nots),
                    implementEdit, null, false);
        }
        if(implement.mapping.size()==1 && !implementChange) {
            // тут вообще надо что=то типа с join'ить (assertion что filterProperty с одним интерфейсом)
            return BaseUtils.singleValue(implement.mapping).mapEditAction(editActionSID, implement.property);
        }
        return super.getDefaultEditAction(editActionSID, filterProperty);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean checkEquals() {
        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>> andImplement
                    = (CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>>) implement;

            CalcPropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof CalcPropertyMapImplement) {
                return ((CalcPropertyMapImplement) objectIface).property.checkEquals();
            }
        }

        return implement.property.checkEquals();
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<Interface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        if (implement.mapping.size() == 1 && ((CalcPropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property instanceof ObjectClassProperty) {
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
            CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>> andImplement
                    = (CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>>) implement;

            CalcPropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof CalcPropertyMapImplement) {
                ((CalcPropertyMapImplement) objectIface).property.proceedDefaultDesign(propertyView, view);
            }
        }
    }
}
