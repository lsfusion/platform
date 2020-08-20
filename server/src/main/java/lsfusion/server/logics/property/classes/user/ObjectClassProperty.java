package lsfusion.server.logics.property.classes.user;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.change.ChangeClassAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.IncrementType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.event.ChangeEvent;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.SimpleIncrementProperty;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ObjectClassProperty extends SimpleIncrementProperty<ClassPropertyInterface> {

    private final BaseClass baseClass;

    public ObjectClassProperty(BaseClass baseClass) {
        super(LocalizedString.create("{classes.object.class}"), IsClassProperty.getInterfaces(new ValueClass[]{baseClass}));

        this.baseClass = baseClass;

        finalizeInit();
    }

    @Override
    protected Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(ExClassSet.toExValue(IsClassProperty.getMapClasses(interfaces)));
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return false;
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toExValue(baseClass.objectClass);
    }

    public ValueClass getInterfaceClass() {
        return interfaces.single().interfaceClass;
    }
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr prevExpr = joinImplement.singleValue().classExpr(baseClass, IsClassProperty.getIsClassType(calcType));
//        if(!hasChanges(propChanges))
            return prevExpr;

        // реализовано на уровне getPropertyChange (так как здесь не хватает части информации, depends от classdata слишком грубая) 
//        ImSet<ClassDataProperty> upClassDataProps = baseClass.getUpDataProps();
//
//        CaseExprInterface newExprCases = Expr.newCases(true, upClassDataProps.size());
//        Where dataChangedWhere = Where.FALSE();
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
            return baseClass.getAllChildren().mapSetValues(value -> value.getProperty().getChanged(IncrementType.DROP, ChangeEvent.scope));
        }
        return SetFact.EMPTY();
    }

    public ImSet<ClassDataProperty> getClassDataProps() {
        return BaseUtils.immutableCast(baseClass.getUpObjectClassFields().keys());
    }

    // now it's not relevant since we've supported IsClassType.VIRTUAL
//    // we don't want real tables to be used for classes (because it will lead to too early cache reading)
//    @Override
//    protected boolean isClassVirtualized(CalcClassType calcType) {
//        return true;
//    }

    @Override
    @IdentityStrongLazy // STRONG пришлось поставить из-за использования в политике безопасности
    public ActionMapImplement<?, ClassPropertyInterface> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {
        if(eventActionSID.equals(ServerResponse.EDIT_OBJECT))
            return null;
        return ChangeClassAction.create(null, false, baseClass).getImplement(SetFact.singletonOrder(getInterface()));
    }

    @Override
    public String getChangeExtSID() {
        return "OBJECT";
    }

    @Override
    public boolean usesSession() {
        return true;
    }

    @Override
    public boolean aspectDebugHasAlotKeys() { // оптимизация см. CaseUnionProperty 
        return false;
    }
}
