package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.property.actions.ChangeReadObjectActionProperty;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.session.PropertyChanges;

public class ObjectValueProperty extends NoIncrementProperty<ClassPropertyInterface> {

    public ObjectValueProperty(ValueClass valueClass) {
        super(ServerResourceBundle.getString("logics.object"), IsClassProperty.getInterfaces(new ValueClass[]{valueClass}));

        finalizeInit();
    }

    private CalcPropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @Override
    protected void fillDepends(MSet<CalcProperty> depends, boolean events) {
        depends.add((CalcProperty) getInterfaceClassProperty().property);
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return joinImplement.get(getInterface()).and(getInterfaceClassProperty().mapExpr(joinImplement, calcType, propChanges, changedWhere).getWhere()); // на тип особого смысла
    }

    @Override
    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, ClassPropertyInterface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return new ChangeReadObjectActionProperty(null, getInterface().interfaceClass).getImplement(SetFact.singletonOrder(getInterface()));
    }

    private ClassPropertyInterface getInterface() {
        return interfaces.single();
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        PropertyObjectInterfaceEntity mapObject = propertyView.entity.propertyObject.mapping.singleValue();
        if (mapObject instanceof ObjectEntity)
            propertyView.caption = ((ObjectEntity) mapObject).getCaption();
    }

    @Override
    public Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return getInterface().mapInferInterfaceClasses(commonValue, inferType).and(
                getInterfaceClassProperty().mapInferInterfaceClasses(ExClassSet.notNull(commonValue), inferType), inferType);
    }
    @Override
    public ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return getInterface().mapInferValueClass(inferred, inferType);
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return true;
    }
}
