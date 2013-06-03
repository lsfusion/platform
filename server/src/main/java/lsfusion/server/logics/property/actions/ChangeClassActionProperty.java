package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.OrObjectClassSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.CustomObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.ExtendContextActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.flow.ForActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.session.ClassChange;

import java.sql.SQLException;

import static lsfusion.server.logics.property.derived.DerivedProperty.createChangeClassAction;

/**
    * User: DAle
    * Date: 03.04.12
    */

// с открытым 2-м интерфейсом класса уже есть в SystemActionProperty
public class ChangeClassActionProperty<T extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

     public final ObjectClass valueClass; // обозначает класс объекта, на который будем менять
     public final boolean forceDialog; // если класс конкретный и имеет потомков
     private final BaseClass baseClass;
    
     public final CalcPropertyMapImplement<T, I> where;
     private final I changeInterface;

    // вот тут пока эвристика вообще надо на внешний контекст смотреть (там может быть веселье с последействием), но пока будет работать достаточно эффективно
     @Override
     public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
         OrObjectClassSet orSet;
         if(needDialog() || where==null || (orSet = where.mapClassWhere(ClassType.ASIS).getOrSet(changeInterface))==null)
             return baseClass.getChildProps().toMap(false);

         MExclSet<CalcProperty> mResult = SetFact.mExclSet();
         for(CustomClass cls : orSet.up.wheres) {
             cls.fillChangeProps((ConcreteObjectClass)valueClass, mResult);
             mResult.exclAddAll(cls.getChildDropProps((ConcreteObjectClass) valueClass));
         }
         for(CustomClass cls : orSet.set)
            cls.fillChangeProps((ConcreteObjectClass)valueClass, mResult);
         return mResult.immutable().toMap(false);
     }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        if(where==null)
            return MapFact.EMPTY();
        return getUsedProps(where);
    }

    private static String getSID(ObjectClass valueClass) {
         return valueClass instanceof UnknownClass ? "delete" : "CHANGE_CLASS" + (valueClass!=null?" "+((CustomClass)valueClass).getSID():"");
     }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    protected CalcPropertyMapImplement<?, I> getGroupWhereProperty() {
        CalcPropertyMapImplement<?, I> result = IsClassProperty.getMapProperty(MapFact.singleton(changeInterface, (ValueClass) baseClass));
        if(where!=null)
            result = DerivedProperty.createAnd(innerInterfaces, where, result);
        return result;
    }

    public static ChangeClassActionProperty<PropertyInterface, PropertyInterface> create(ObjectClass valueClass, boolean forceDialog, BaseClass baseClass) {
        PropertyInterface propInterface = new PropertyInterface();
        return new ChangeClassActionProperty<PropertyInterface, PropertyInterface>(getSID(valueClass), valueClass, forceDialog, SetFact.singleton(propInterface), SetFact.singletonOrder(propInterface), propInterface, null, baseClass);
    }

    public ChangeClassActionProperty(String sID, ObjectClass valueClass, boolean forceDialog, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, I changeInterface, CalcPropertyMapImplement<T, I> where, BaseClass baseClass) {
         super(sID, ServerResourceBundle.getString(
                 valueClass instanceof UnknownClass ? "logics.property.actions.delete" : "logics.property.actions.changeclass"), innerInterfaces, mapInterfaces);

         this.valueClass = valueClass;
         this.forceDialog = forceDialog;
         this.baseClass = baseClass;
         this.changeInterface = changeInterface;
         this.where = where;

         assert where==null || this.mapInterfaces.valuesSet().merge(changeInterface).containsAll(where.mapping.valuesSet());
     }

     protected boolean needDialog() {
         return valueClass == null || valueClass instanceof AbstractCustomClass || (forceDialog &&
                 valueClass instanceof ConcreteCustomClass && ((ConcreteCustomClass)valueClass).hasChildren());
     }

    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException {
        ConcreteObjectClass readClass;

        if (needDialog()) {
            CustomClass baseClass; CustomClass currentClass;
            CustomObjectInstance object = (CustomObjectInstance) context.getSingleObjectInstance();
            if(object == null) {
                baseClass = this.baseClass;
                currentClass = baseClass;
            } else {
                baseClass = object.baseClass;
                currentClass = object.currentClass;
            }
            
            ObjectValue objectValue = context.requestUserClass(baseClass, currentClass, true);
            if (!(objectValue instanceof DataObject)) { // cancel
                return FlowResult.FINISH;
            }

            readClass = baseClass.getBaseClass().findConcreteClassID((Integer) ((DataObject) objectValue).object);
        } else
            readClass = (ConcreteObjectClass) valueClass;

        boolean singleWhereNotNull = true;
        if(where==null || (innerKeys.isEmpty() && (singleWhereNotNull = where.read(context, innerValues)!=null))) {
            PropertyObjectInterfaceInstance objectInstance = context.getSingleObjectInstance();
            ObjectValue object = innerValues.get(changeInterface);
            if(object instanceof DataObject) {
                DataObject dataObject = (DataObject)object;
                DataObject nearObject = null; // после удаления выбираем соседний объект
                if (objectInstance != null && objectInstance instanceof ObjectInstance) {
                    CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                    if(readClass instanceof UnknownClass || !((CustomClass) readClass).isChild(customObjectInstance.gridClass)) // если удаляется
                        nearObject = BaseUtils.getNearValue((ObjectInstance)objectInstance, dataObject, ListFact.toJavaMapList(customObjectInstance.groupTo.keys.keyOrderSet()));
                }

                context.changeClass(objectInstance, dataObject, (ConcreteObjectClass) readClass);

                if (nearObject != null)
                    ((CustomObjectInstance) objectInstance).groupTo.addSeek(objectInstance, nearObject, false);
            } else
                proceedNullException();
        } else
            if(singleWhereNotNull) { // дебильный кейс, но надо все равно поддержать
                Where exprWhere = where.mapExpr(innerExprs, context.getModifier()).getWhere();
                if(!exprWhere.isFalse()) // оптимизация, важна так как во многих event'ах может учавствовать
                    context.changeClass(new ClassChange(innerKeys.singleValue(), exprWhere, (ConcreteObjectClass)readClass));
            }

        return FlowResult.FINISH;
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        if ((where == null || BaseUtils.hashEquals(mapInterfaces.valuesSet(),innerInterfaces)) && valueClass instanceof UnknownClass)
            return interfaces.single();
        return null;
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return !ordersNotNull;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> CalcProperty getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        if(where!=null)
            return where.property;
        return null;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionPropertyMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, CalcPropertyMapImplement<PW, T> push, ImOrderMap<CalcPropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForActionProperty.pushFor(innerInterfaces, where, mapInterfaces, mapping, context, push, orders, ordersNotNull, new ForActionProperty.PushFor<I, PropertyInterface>() {
            public ActionPropertyMapImplement<?, PropertyInterface> push(ImSet<PropertyInterface> context, CalcPropertyMapImplement<?, PropertyInterface> where, ImOrderMap<CalcPropertyInterfaceImplement<PropertyInterface>, Boolean> orders, boolean ordersNotNull, ImRevMap<I, PropertyInterface> mapInnerInterfaces) {
                return createChangeClassAction(context, mapInnerInterfaces.get(changeInterface), valueClass, forceDialog, where, baseClass, orders, ordersNotNull);
            }
        });
    }
}
