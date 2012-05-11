package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectValueProperty extends ExecuteClassProperty {

    private final ValueClass typeClass;
    public final ClassPropertyInterface objectInterface;

    public ObjectValueProperty(String SID, ValueClass valueClass) {
        super(SID, ServerResourceBundle.getString("logics.object"), new ValueClass[]{valueClass});

        this.typeClass = valueClass;
        objectInterface = BaseUtils.single(interfaces);

        finalizeInit();
    }

    @Override
    public String getCode() {
        return "objectValue.getLP(baseClass.named)";
    }

    public ValueClass getValueClass() {
        return typeClass;
    }

    public void execute(ExecutionContext context) throws SQLException {
        context.emitExceptionIfNotInFormSession();

        context.getFormInstance().changeObject(context.getSingleObjectInstance(), context.getValue(), context.getActions());
    }

    protected Expr getValueExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        return BaseUtils.singleValue(joinImplement);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(propertyView.entity.propertyObject.mapping);
        if (mapObject instanceof ObjectEntity)
            propertyView.caption = ((ObjectEntity) mapObject).getCaption();
    }

    public Set<Property> getChangeProps() {
        return new HashSet<Property>();
    }

    public Set<Property> getUsedProps() {
        return new HashSet<Property>();
    }
}
