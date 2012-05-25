package platform.server.logics.property;

import platform.base.BaseUtils;
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
    protected void fillDepends(Set<CalcProperty> depends, boolean events) {
        depends.add((CalcProperty) getInterfaceClassProperty().property);
    }

    @Override
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return joinImplement.get(getInterface()).and(getInterfaceClassProperty().mapExpr(joinImplement, propClasses, propChanges, changedWhere).getWhere()); // на тип особого смысла
    }

    @Override
    @IdentityLazy
    public ActionPropertyMapImplement<?, ClassPropertyInterface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        return new ChangeReadObjectActionProperty(null, getInterface().interfaceClass).getImplement(Collections.singletonList(getInterface()));
    }

    private ClassPropertyInterface getInterface() {
        return BaseUtils.single(interfaces);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(propertyView.entity.propertyObject.mapping);
        if (mapObject instanceof ObjectEntity)
            propertyView.caption = ((ObjectEntity) mapObject).getCaption();
    }

    @Override
    public Map<ClassPropertyInterface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null)
            return Collections.singletonMap(getInterface(), commonValue);
        return super.getInterfaceCommonClasses(commonValue); 
    }
}
