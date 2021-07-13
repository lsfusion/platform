package lsfusion.server.logics.action.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.ExtendContextAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ForAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.classes.change.ClassChange;
import lsfusion.server.logics.classes.user.*;
import lsfusion.server.logics.classes.user.set.OrObjectClassSet;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapEventExec;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapRemove;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static lsfusion.server.logics.property.PropertyFact.createChangeClassAction;

// с открытым 2-м интерфейсом класса уже есть в SystemActionProperty
public class ChangeClassAction<T extends PropertyInterface, I extends PropertyInterface> extends ExtendContextAction<I> {

     public final ObjectClass valueClass; // обозначает класс объекта, на который будем менять
     public final boolean forceDialog; // если класс конкретный и имеет потомков
     private final BaseClass baseClass;
    
     public final PropertyMapImplement<T, I> where;
     private final I changeInterface;

     // когда класс с которого или на который меняется не известен
     public static ImMap<Property, Boolean> aspectChangeBaseExtProps(BaseClass baseClass) {
         return baseClass.getUpAllChangedProps().addExcl(baseClass.getUpDataProps()).toMap(false);
     }

    // вот тут пока эвристика вообще надо на внешний контекст смотреть (там может быть веселье с последействием), но пока будет работать достаточно эффективно
     @Override
     public ImMap<Property, Boolean> aspectChangeExtProps() {
         OrObjectClassSet orSet;
         if(needDialog() || where==null || (orSet = where.mapClassWhere(ClassType.wherePolicy).getOrSet(changeInterface))==null)
             return aspectChangeBaseExtProps(baseClass);

         assert valueClass instanceof ConcreteObjectClass;
         MSet<Property> mResult = SetFact.mSet(); // можно было бы оптимизировать (для exclAdd в частности), но пока не критично
         
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
    public ImMap<Property, Boolean> aspectUsedExtProps() {
        if(where==null)
            return MapFact.EMPTY();
        return getUsedProps(where);
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    protected PropertyMapImplement<?, I> calcGroupWhereProperty() {
        PropertyMapImplement<?, I> result = IsClassProperty.getMapProperty(MapFact.singleton(changeInterface, baseClass));
        if(where!=null)
            result = PropertyFact.createAnd(innerInterfaces, where, result);
        return result;
    }

    public static ChangeClassAction<PropertyInterface, PropertyInterface> create(ObjectClass valueClass, boolean forceDialog, BaseClass baseClass) {
        PropertyInterface propInterface = new PropertyInterface();
        return new ChangeClassAction<>(valueClass, forceDialog, SetFact.singleton(propInterface), SetFact.singletonOrder(propInterface), propInterface, null, baseClass);
    }

    public ChangeClassAction(ObjectClass valueClass, boolean forceDialog, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces, I changeInterface, PropertyMapImplement<T, I> where, BaseClass baseClass) {
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
                if (objectInstance instanceof ObjectInstance) {
                    CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                    if(readClass instanceof UnknownClass || !((CustomClass) readClass).isChild(customObjectInstance.gridClass)) { // если удаляется
                        ImMap<ObjectInstance, DataObject> nearGroupObject = BaseUtils.getNearObject(MapFact.singleton((ObjectInstance) objectInstance, dataObject), customObjectInstance.groupTo.keys.keyOrderSet().toJavaList());
                        nearObject = nearGroupObject != null ? nearGroupObject.singleValue() : null;
                        seekOther = true;
                    }
                }

                context.changeClass(objectInstance, dataObject, readClass);

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
                    context.changeClass(new ClassChange(innerKeys.singleValue(), exprWhere, readClass));
            }

        return FlowResult.FINISH;
    }

    @Override
    public AsyncMapEventExec<PropertyInterface> calculateAsyncEventExec(boolean optimistic, boolean recursive) {
        if ((where == null || BaseUtils.hashEquals(mapInterfaces.valuesSet(),innerInterfaces)) && valueClass instanceof UnknownClass)
            return new AsyncMapRemove<>(interfaces.single());
        return null;
    }

    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> boolean hasPushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        return !ordersNotNull;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> Property getPushWhere(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);
        if(where!=null)
            return where.property;
        return null;
    }
    @Override
    public <T extends PropertyInterface, PW extends PropertyInterface> ActionMapImplement<?, T> pushFor(ImRevMap<PropertyInterface, T> mapping, ImSet<T> context, PropertyMapImplement<PW, T> push, ImOrderMap<PropertyInterfaceImplement<T>, Boolean> orders, boolean ordersNotNull) {
        assert hasPushFor(mapping, context, ordersNotNull);

        return ForAction.pushFor(innerInterfaces, where, mapInterfaces, mapping, context, push, orders, ordersNotNull, (ForAction.PushFor<I, PropertyInterface>) (context1, where, orders1, ordersNotNull1, mapInnerInterfaces) -> createChangeClassAction(context1, mapInnerInterfaces.get(changeInterface), valueClass, forceDialog, where, baseClass, orders1, ordersNotNull1));
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
