package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class ObjectValueProperty extends ExecuteProperty {

    private final ValueClass typeClass;
    public final ClassPropertyInterface objectInterface;

    public ObjectValueProperty(String SID, ValueClass valueClass) {
        super(SID, ServerResourceBundle.getString("logics.object"), new ValueClass[]{valueClass});

        this.typeClass = valueClass;
        objectInterface = BaseUtils.single(interfaces);
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
        FormInstance<?> remoteForm = context.getFormInstance();
        if(context.getObjectInstanceCount() > 0 && context.getSingleObjectInstance() instanceof ObjectInstance)
            context.addActions(remoteForm.changeObject((ObjectInstance) context.getSingleObjectInstance(), context.getValue(), context.getRemoteForm()));
    }

    @Override
    public void prereadCaches() {
        super.prereadCaches();
        ClassProperty.getIsClassProperty(interfaces).property.prereadCaches();
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return ClassProperty.getIsClassUsed(interfaces, propChanges);
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return BaseUtils.singleValue(joinImplement).and(ClassProperty.getIsClassWhere(joinImplement, propChanges, changedWhere));
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(propertyView.entity.propertyObject.mapping);
        if (mapObject instanceof ObjectEntity)
            propertyView.caption = ((ObjectEntity) mapObject).getCaption();
    }
}
