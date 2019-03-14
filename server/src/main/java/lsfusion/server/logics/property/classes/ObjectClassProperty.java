package lsfusion.server.logics.property.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.BaseClass;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.infer.CalcType;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.change.ChangeClassAction;
import lsfusion.server.logics.event.ChangeEvent;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.PropertyChanges;

import java.sql.SQLException;

public class ObjectClassProperty extends SimpleIncrementProperty<ClassPropertyInterface> {

    private final BaseClass baseClass;

    public ObjectClassProperty(BaseClass baseClass) {
        super(LocalizedString.create("{classes.object.class}"), IsClassProperty.getInterfaces(new ValueClass[]{baseClass}));

        this.baseClass = baseClass;

        finalizeInit();
    }

    public ValueClass getInterfaceClass() {
        return interfaces.single().interfaceClass;
    }
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr prevExpr = joinImplement.singleValue().classExpr(baseClass);;
//        if(!hasChanges(propChanges))
            return prevExpr;

        // реализовано на уровне getPropertyChange (так как здесь не хватает части информации, depends от classdata слишком грубая) 
//        ImSet<ClassDataProperty> upClassDataProps = baseClass.getUpDataProps();
//
//        CaseExprInterface newExprCases = Expr.newCases(true, upClassDataProps.size());
//        Where dataChangedWhere = Where.FALSE;
//        for(ClassDataProperty dataProperty : upClassDataProps) {            
//            if(!dataProperty.hasChanges(propChanges)) // оптимизация
//                continue;
//
//            WhereBuilder dataChangedOpWhere = new WhereBuilder();
//            Expr dataExpr = dataProperty.getExpr(MapFact.singleton(dataProperty.interfaces.single(), joinImplement.singleValue()), calcType, propChanges, dataChangedOpWhere);
//            Expr changedDataExpr = dataExpr.and(dataChangedOpWhere.toWhere());
//            assert IsClassProperty.checkSession(changedDataExpr) && IsClassProperty.checkSession(dataChangedOpWhere.toWhere());
//
//            dataChangedWhere = dataChangedOpWhere.toWhere();
//            newExprCases.add(changedDataExpr.getWhere(), changedDataExpr);
//        }
//
//        if(changedWhere != null)
//            changedWhere.add(dataChangedWhere);
//        return newExprCases.getFinal().ifElse(dataChangedWhere, prevExpr);
    }


    public Expr getExpr(Expr expr, Modifier modifier) throws SQLException, SQLHandledException {
        return getExpr(MapFact.singleton(getInterface(), expr), modifier);
    }

    private ClassPropertyInterface getInterface() {
        return interfaces.single();
    }

//    @Override
//    protected void fillDepends(MSet<Property> depends, boolean events) {
//        depends.addAll(getClassDataProps());
//    }

    public ImSet<Property> getSingleApplyDroppedIsClassProps() {
        ValueClass interfaceClass = getInterfaceClass();
        if(interfaceClass instanceof CustomClass) {
            return baseClass.getAllChildren().mapSetValues(new GetValue<Property, CustomClass>() {
                public Property getMapValue(CustomClass value) {
                    return value.getProperty().getChanged(IncrementType.DROP, ChangeEvent.scope);
                }});
        }
        return SetFact.EMPTY();
    }

    public ImSet<ClassDataProperty> getClassDataProps() {
        return BaseUtils.immutableCast(baseClass.getUpObjectClassFields().keys());
    }

    @Override
    @IdentityStrongLazy // STRONG пришлось поставить из-за использования в политике безопасности
    public ActionPropertyMapImplement<?, ClassPropertyInterface> getDefaultEditAction(String editActionSID, Property filterProperty) {
        if(editActionSID.equals(ServerResponse.EDIT_OBJECT))
            return null;
        return ChangeClassAction.create(null, false, baseClass).getImplement(SetFact.singletonOrder(getInterface()));
    }

    @Override
    public String getChangeExtSID() {
        return "OBJECT";
    }

    @Override
    public boolean aspectDebugHasAlotKeys() { // оптимизация см. CaseUnionProperty 
        return false;
    }
}
