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
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;

import java.sql.SQLException;
import java.util.Map;

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

    @Override
    public CustomClass getDialogClass(Map<ClassPropertyInterface, DataObject> mapValues, Map<ClassPropertyInterface, ConcreteClass> mapClasses, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if (mapObjects.size() > 0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance)
            return ((CustomObjectInstance) BaseUtils.singleValue(mapObjects)).getBaseClass();
        else
            return super.getDialogClass(mapValues, mapClasses, mapObjects);
    }

    public void execute(ExecutionContext context) throws SQLException {
        if (context.isInFormSession()) {
            if (context.getSingleObjectInstance() instanceof ObjectInstance) {
                context.addActions(
                        context.getFormInstance().changeObject(
                                (ObjectInstance) context.getSingleObjectInstance(),
                                context.getValue(),
                                context.getRemoteForm()));
            }
        } else {
            context.emitExceptionIfNotInFormSession();
        }
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
}
