package lsfusion.server.logics.form.interactive.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.change.DefaultChangeObjectAction;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyMapImplement;

public class ObjectValueProperty extends NoIncrementProperty<ClassPropertyInterface> {

    private final ObjectEntity object; 
            
    public ObjectValueProperty(ObjectEntity object) { // LocalizedString.create("{logics.object}")
        super(object.getCaption(), IsClassProperty.getInterfaces(new ValueClass[]{object.baseClass}));

        this.object = object;
        
        finalizeInit();
    }

    private PropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        assert !noClasses();
        return IsClassProperty.getProperty(interfaces);
    }

    @Override
    protected void fillDepends(MSet<Property> depends, boolean events) {
        if(!noClasses())
            depends.add(getInterfaceClassProperty().property);
    }

    private boolean noClasses() {
        return object.noClasses();
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr result = joinImplement.get(getInterface());
        if(!noClasses())
            result = result.and(getInterfaceClassProperty().mapExpr(joinImplement, calcType, propChanges, changedWhere).getWhere()); // на тип особого смысла
        return result;
    }

    @Override
    @IdentityStrongLazy // STRONG пришлось поставить из-за использования в политике безопасности
    public ActionMapImplement<?, ClassPropertyInterface> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {
        if(eventActionSID.equals(ServerResponse.EDIT_OBJECT))
            return null;
        return object.getChangeAction().getImplement(SetFact.singletonOrder(getInterface()));
    }

    private ClassPropertyInterface getInterface() {
        return interfaces.single();
    }

    @Override
    public Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        Inferred<ClassPropertyInterface> result = getInterface().mapInferInterfaceClasses(commonValue, inferType);
        if(!noClasses())
            result = result.and(getInterfaceClassProperty().mapInferInterfaceClasses(ExClassSet.notNull(commonValue), inferType), inferType);
        return result;
    }
    @Override
    public ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return getInterface().mapInferValueClass(inferred, inferType);
    }
}
