package lsfusion.server.logics.action.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.classes.*;
import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.classes.*;
import lsfusion.server.logics.classes.sets.OrObjectClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.property.classes.ClassDataProperty;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.implement.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.CalcPropertyMapImplement;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.ExtendContextActionProperty;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ForActionProperty;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.action.session.classes.change.ClassChange;

import java.sql.SQLException;

import static lsfusion.server.logics.property.derived.DerivedProperty.createChangeClassAction;

// с открытым 2-м интерфейсом класса уже есть в SystemActionProperty
public class ChangeClassActionProperty<T extends PropertyInterface, I extends PropertyInterface> extends ExtendContextActionProperty<I> {

     public final ObjectClass valueClass; // обозначает класс объекта, на который будем менять
     public final boolean forceDialog; // если класс конкретный и имеет потомков
     private final BaseClass baseClass;
    
     public final CalcPropertyMapImplement<T, I> where;
     private final I changeInterface;

     // когда класс с которого или на который меняется не известен
     public static ImMap<CalcProperty, Boolean> aspectChangeBaseExtProps(BaseClass baseClass) {
         return baseClass.getUpAllChangedProps().addExcl(baseClass.getUpDataProps()).toMap(false);
     }

    // вот тут пока эвристика вообще надо на внешний контекст смотреть (там может быть веселье с последействием), но пока будет работать достаточно эффективно
     @Override
     public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
         OrObjectClassSet orSet;
         if(needDialog() || where==null || (orSet = where.mapClassWhere(ClassType.wherePolicy).getOrSet(changeInterface))==null)
             return aspectChangeBaseExtProps(baseClass);

         assert valueClass instanceof ConcreteObjectClass;
         MSet<CalcProperty> mResult = SetFact.mSet(); // можно было бы оптимизировать (для exclAdd в частности), но пока не критично
         
         MSet<ClassDataProperty> mChangedDataProps = SetFact.mSet();
         if(valueClass instanceof CustomClass)
             mChangedDataProps.add(((ConcreteCustomClass) valueClass).dataProperty);
         for(ConcreteCustomClass cls : orSet.getSetConcreteChildren()) {
             mResult.addAll(cls.getChangeProps((ConcreteObjectClass) valueClass));
             mChangedDataProps.add(cls.dataProperty);
         }
         mResult.add(valueClass.getBaseClass().getObjectClassProperty());

/*         for(CustomClass cls : orSet.up.wheres) {
             cls.fillChangeProps((ConcreteObjectClass)valueClass, mResult);
             mResult.exclAddAll(cls.getChildDropProps((ConcreteObjectClass) valueClass));
         }
         for(ConcreteCustomClass cls : orSet.set)
            cls.fillChangeProps((ConcreteObjectClass)valueClass, mResult);*/
         return mResult.immutable().toMap(false);
     }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        if(where==null)
            return MapFact.EMPTY();
        return getUsedProps(where);
    }

    public ImSet<ActionProperty> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    protected CalcPropertyMapImplement<?, I> calcGroupWhereProperty() {
        CalcPropertyMapImplement<?, I> result = IsClassProperty.getMapProperty(MapFact.singleton(changeInterface, (ValueClass) baseClass));
        if(where!=null)
            result = DerivedProperty.createAnd(innerInterfaces, where, result);
        return result;
    }

    public static ChangeClassActionProperty<PropertyInterface, PropertyInterface> create(ObjectClass valueClass, boolean forceDialog, BaseClass baseClass) {
        PropertyInterface propInterface = new PropertyInterface();
        return new ChangeClassActionProperty<>(valueClass, forceDialog, SetFact.singleton(propInterface), SetFact.singletonOrder(propInterface), propInterface, null, baseClass);
    }

    public ChangeClassActionProperty(ObjectClass valueClass, boolean forceDialog, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, I changeInterface, CalcPropertyMapImplement<T, I> where, BaseClass baseClass) {
         super(LocalizedString.create(
                 valueClass instanceof UnknownClass ? "{logics.delete}" : "{logics.property.actions.changeclass}"), innerInterfaces, mapInterfaces);

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

    protected FlowResult executeExtend(ExecutionContext<PropertyInterface> context, ImRevMap<I, KeyExpr> innerKeys, ImMap<I, ? extends ObjectValue> innerValues, ImMap<I, Expr> innerExprs) throws SQLException, SQLHandledException {
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

            readClass = baseClass.getBaseClass().findConcreteClassID((Long) ((DataObject) objectValue).object);
        } else
            readClass = (ConcreteObjectClass) valueClass;

        boolean singleWhereNotNull = true;
        if(where==null || (innerKeys.isEmpty() && (singleWhereNotNull = where.read(context, innerValues)!=null))) {
            PropertyObjectInterfaceInstance objectInstance = context.getSingleObjectInstance();
            ObjectValue object = innerValues.get(changeInterface);
            if(object instanceof DataObject) {
                DataObject dataObject = (DataObject)object;
                
                boolean seekOther = false;
                DataObject nearObject = null; // после удаления выбираем соседний объект
                if (objectInstance != null && objectInstance instanceof ObjectInstance) {
                    CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                    if(readClass instanceof UnknownClass || !((CustomClass) readClass).isChild(customObjectInstance.gridClass)) { // если удаляется
                        nearObject = BaseUtils.getNearValue((ObjectInstance) objectInstance, dataObject, ListFact.toJavaMapList(customObjectInstance.groupTo.keys.keyOrderSet()));
                        seekOther = true;
                    }
                }

                context.changeClass(objectInstance, dataObject, (ConcreteObjectClass) readClass);

                if(seekOther) {
                    if (nearObject != null)
                        ((CustomObjectInstance) objectInstance).groupTo.addSeek(objectInstance, nearObject, false);
                    else
                        ((CustomObjectInstance) objectInstance).groupTo.seek(UpdateType.FIRST);
                }
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
        return super.getSimpleDelete();
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

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(type.isChange())
            return true;
        return super.hasFlow(type);
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // need this for class breakpoints
    }
}
