package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.actions.ChangeReadObjectActionProperty;
import platform.server.session.PropertyChanges;

public class ObjectValueProperty extends NoIncrementProperty<ClassPropertyInterface> {

    public ObjectValueProperty(String SID, ValueClass valueClass) {
        super(SID, ServerResourceBundle.getString("logics.object"), IsClassProperty.getInterfaces(new ValueClass[]{valueClass}));

        finalizeInit();
    }

    @Override
    public String getCode() {
        return "objectValue.getLP(baseClass.named)";
    }

    private CalcPropertyMapImplement<?, ClassPropertyInterface> getInterfaceClassProperty() {
        return IsClassProperty.getProperty(interfaces);
    }

    @Override
    protected void fillDepends(MSet<CalcProperty> depends, boolean events) {
        depends.add((CalcProperty) getInterfaceClassProperty().property);
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return joinImplement.get(getInterface()).and(getInterfaceClassProperty().mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere()); // на тип особого смысла
    }

    @Override
    @IdentityLazy
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
    public ImMap<ClassPropertyInterface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null)
            return MapFact.singleton(getInterface(), commonValue);
        return super.getInterfaceCommonClasses(commonValue); 
    }
}
