package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.ServerResponse;
import platform.server.caches.IdentityLazy;
import platform.server.classes.CustomClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.ValueClass;
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
import platform.server.logics.property.actions.edit.AggChangeActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

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

    private static <T extends PropertyInterface> boolean isIdentity(List<Interface> interfaces, CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement) {
        Set<Interface> rest = new HashSet<Interface>(interfaces);
        for(CalcPropertyInterfaceImplement<Interface> impl : implement.mapping.values())
            if(!(impl instanceof Interface && rest.remove((Interface)impl)))
                return false;
        return rest.isEmpty();
    }
    
    public JoinProperty(String sID, String caption, List<Interface> interfaces, boolean implementChange, CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement) {
        super(sID, caption, interfaces);
        this.implement = implement;
        this.implementChange = implementChange || isIdentity(interfaces, implement);

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

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            Set<CalcProperty> implementDepends = new HashSet<CalcProperty>();
            implement.mapping.get(andInterface).mapFillDepends(implementDepends);
            return QuickSet.add(propChanges.getUsedDataChanges(implementDepends), getUsedChanges(propChanges));
        }

        CalcProperty<T> implementProperty = implement.property;
        if(implementChange) {
            Set<CalcProperty> implementProps = new HashSet<CalcProperty>();
            fillDepends(implementProps,implement.mapping.values());
            return QuickSet.add(implementProperty.getUsedDataChanges(propChanges), propChanges.getUsedChanges(implementProps));
        }
        if(implement.mapping.size() == 1 && implementProperty.aggProp) {
            // пока тупо MGProp'им назад
            return QuickSet.add((((CalcPropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property).getUsedDataChanges(propChanges), implementProperty.getUsedChanges(propChanges));
        }

        return super.calculateUsedDataChanges(propChanges);
    }

    // для Compare - data changes, тут чтобы не мусорить в Property
    private static DataChanges getCompareDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges, CalcPropertyInterfaceImplement<Interface> changeImp, CalcPropertyInterfaceImplement<Interface> valueImp) {
        Map<Interface, Expr> mapExprs = change.getMapExprs();
        Expr toChangeExpr = valueImp.mapExpr(mapExprs, propChanges);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(mapExprs, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(mapExprs, propChanges), Compare.EQUALS))), changedWhere, propChanges);
    }
    
    // для And - data changes, тут чтобы не мусорить в Property
    private static <T extends PropertyInterface> T getObjectAndInterface(CalcProperty<T> property) {
        if(property instanceof AndFormulaProperty)
            return (T) ((AndFormulaProperty)property).objectInterface;
        if(property instanceof JoinProperty) {
            CalcPropertyImplement<PropertyInterface, CalcPropertyInterfaceImplement<Interface>> joinImplement = ((JoinProperty<PropertyInterface>) property).implement;
            PropertyInterface andInterface = getObjectAndInterface(joinImplement.property);
            CalcPropertyInterfaceImplement<Interface> andJoinInterface = joinImplement.mapping.get(andInterface);
            if(andJoinInterface instanceof Interface)
                return (T) andJoinInterface;
        }
        return null;
    }
    
    private static <T extends PropertyInterface> Where getAndWhere(CalcProperty<T> property, Map<T, ? extends Expr> mapExprs, PropertyChanges propChanges) {
        if(property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)property;
            Where where = Where.TRUE;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface)
                    where = where.and(mapExprs.get(andInterface).getWhere());
            return where;
        }
        if(property instanceof JoinProperty) {
            CalcPropertyImplement<PropertyInterface, CalcPropertyInterfaceImplement<Interface>> joinImplement = ((JoinProperty<PropertyInterface>) property).implement;
            Map<PropertyInterface, Expr> mapJoinExprs = new HashMap<PropertyInterface, Expr>();
            for(Map.Entry<PropertyInterface, CalcPropertyInterfaceImplement<Interface>> map : joinImplement.mapping.entrySet())
                mapJoinExprs.put(map.getKey(), map.getValue().mapExpr((Map<Interface,? extends Expr>) mapExprs, propChanges));
            return getAndWhere(joinImplement.property, mapJoinExprs, propChanges);
        }
        throw new RuntimeException("should not be");
    }

    @Override
    @IdentityLazy
    public Collection<DataProperty> getChangeProps() {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) {
            assert implement.mapping.size()==2;
            Iterator<CalcPropertyInterfaceImplement<Interface>> i = implement.mapping.values().iterator();
            return BaseUtils.merge(i.next().mapChangeProps(), i.next().mapChangeProps());
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null)
            return implement.mapping.get(andInterface).mapChangeProps();

        if(implementChange)
            return implement.property.getChangeProps();

        if(implement.mapping.size() == 1 && implement.property.aggProp)
            return BaseUtils.singleValue(implement.mapping).mapChangeProps();

        return super.getChangeProps();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<T> i = implement.property.interfaces.iterator();
            CalcPropertyInterfaceImplement<Interface> op1 = implement.mapping.get(i.next());
            CalcPropertyInterfaceImplement<Interface> op2 = implement.mapping.get(i.next());

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder compareChangedWhere = new WhereBuilder();
            DataChanges result = getCompareDataChanges(change, compareChangedWhere, propChanges, op1, op2);
            if(changedWhere!=null) changedWhere.add(compareChangedWhere.toWhere());
            return result.add(getCompareDataChanges(change.and(compareChangedWhere.toWhere().not()), changedWhere, propChanges, op2, op1));
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            Map<Interface, Expr> mapExprs = change.getMapExprs();
            return implement.mapping.get(andInterface).mapJoinDataChanges(mapExprs, change.expr,
                    change.where.and(getAndWhere(this, mapExprs, propChanges)), changedWhere, propChanges);
        }

        if(implementChange) // groupBy'им выбирая max
            return implement.property.getJoinDataChanges(getJoinImplements(change.getMapExprs(), propChanges, null), change.expr, change.where, propChanges, changedWhere); //.map(mapInterfaces)

        if(implement.mapping.size() == 1 && implement.property.aggProp) {
            // пока тупо MGProp'им назад
            CalcPropertyMapImplement<?, Interface> implementSingle = (CalcPropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping);
            KeyExpr keyExpr = new KeyExpr("key");
            Expr groupExpr = GroupExpr.create(Collections.singletonMap(0, implement.property.getExpr(Collections.singletonMap(BaseUtils.single(implement.property.interfaces), keyExpr), propChanges)),
                    keyExpr, keyExpr.isClass(implementSingle.property.getValueClass().getUpSet()), GroupType.ANY, Collections.singletonMap(0, change.expr));
            return implementSingle.mapDataChanges(
                    new PropertyChange<Interface>(change, groupExpr), changedWhere, propChanges);
        }

        return super.calculateDataChanges(change, changedWhere, propChanges);
    }

    @Override
    @IdentityLazy
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        CalcProperty<T> aggProp = implement.property;

        if (aggProp instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty) aggProp;
            List<CalcPropertyInterfaceImplement<Interface>> ands = new ArrayList<CalcPropertyInterfaceImplement<Interface>>();
            for (AndFormulaProperty.Interface andInterface : andProperty.interfaces) {
                if (andInterface != andProperty.objectInterface) {
                    ands.add(implement.mapping.get(andInterface));
                }
            }
            ActionPropertyMapImplement<?, Interface> implementEdit = implement.mapping.get(andProperty.objectInterface).mapEditAction(editActionSID, filterProperty);
            if (implementEdit != null) {
                return DerivedProperty.createIfAction(
                        interfaces,
                        DerivedProperty.createAnd(interfaces, DerivedProperty.<Interface>createTrue(), ands),
                        implementEdit,
                        null,
                        false);
            }
        }

        if (implement.mapping.size() == 1 && !implementChange) {
            if (editActionSID.equals(ServerResponse.CHANGE_WYS)) {
                ActionPropertyMapImplement<?, Interface> changeActionImplement = getEditAction(ServerResponse.CHANGE);
                ValueClass aggClass = ((CalcPropertyMapImplement<?, Interface>) BaseUtils.singleValue(implement.mapping)).property.getValueClass();

                List<Interface> listInterfaces = new ArrayList<Interface>(interfaces);
                AggChangeActionProperty<T> aggChangeActionProperty =
                        new AggChangeActionProperty<T>("AGGCH" + getSID(), "sys", listInterfaces, aggProp, aggClass, changeActionImplement);
                ActionPropertyMapImplement<?, Interface> aggChangeActionImplement = aggChangeActionProperty.getImplement(listInterfaces);

                setEditAction(ServerResponse.CHANGE_WYS, aggChangeActionImplement);

                return aggChangeActionImplement;
            } else {
                // тут вообще надо что=то типа с join'ить (assertion что filterProperty с одним интерфейсом)
                return BaseUtils.singleValue(implement.mapping).mapEditAction(editActionSID, aggProp);
            }
        }
        return super.getDefaultEditAction(editActionSID, filterProperty);
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

    @Override
    public Map<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        Map<Interface, ValueClass> result = super.getInterfaceCommonClasses(commonValue);
        Map<T, ValueClass> commonClasses = implement.property.getInterfaceCommonClasses(commonValue);
        for(Map.Entry<T, CalcPropertyInterfaceImplement<Interface>> mapImp : implement.mapping.entrySet())
            result = or(interfaces, result, mapImp.getValue().mapInterfaceCommonClasses(commonClasses.get(mapImp.getKey())));
        return result;
    }
}
