package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ObjectValueProperty extends ExecuteProperty {

    private final ValueClass typeClass;

    public ObjectValueProperty(String SID, ValueClass valueClass) {
        super(SID, "Объект", new ValueClass[]{valueClass});

        this.typeClass = valueClass;
    }

    @Override
    public String getCode() {
        return "objectValue.getLP(baseClass.named)";
    }

    protected ValueClass getValueClass() {
        return typeClass;
    }

    @Override
    public CustomClass getDialogClass(Map<ClassPropertyInterface, DataObject> mapValues, Map<ClassPropertyInterface, ConcreteClass> mapClasses, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) {
        if (mapObjects.size() > 0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance)
            return ((CustomObjectInstance) BaseUtils.singleValue(mapObjects)).getBaseClass();
        else
            return super.getDialogClass(mapValues, mapClasses, mapObjects);
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        FormInstance<?> remoteForm = executeForm.form;
        if(mapObjects.size()>0 && BaseUtils.singleValue(mapObjects) instanceof ObjectInstance)
            actions.addAll(remoteForm.changeObject((ObjectInstance)BaseUtils.singleValue(mapObjects), value, executeForm));
    }

    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return BaseUtils.singleValue(joinImplement).and(ClassProperty.getIsClassWhere(joinImplement, modifier, changedWhere));
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return ClassProperty.getIsClassUsedChanges(interfaces, modifier);
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        PropertyObjectInterfaceEntity mapObject = BaseUtils.singleValue(entity.propertyObject.mapping);
        if (mapObject instanceof ObjectEntity)
            view.get(entity).caption = ((ObjectEntity) mapObject).getCaption();
    }
}
