package platform.server.logics.property.actions;

import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.view.panellocation.ToolbarPanelLocationView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class SimpleAddObjectActionProperty extends CustomActionProperty {
    // обозначает класс объекта, который нужно добавить
    private CustomClass valueClass;

    private LP storeNewObjectProperty;

    public SimpleAddObjectActionProperty(String sID, CustomClass valueClass, LP storeNewObjectProperty) {
        super(sID, ServerResourceBundle.getString("logics.add") + "(" + valueClass + ")", new ValueClass[0]);
        this.valueClass = valueClass;

        this.storeNewObjectProperty = storeNewObjectProperty;
    }

    public void execute(ExecutionContext context) throws SQLException {
        FormInstance<?> form = context.getFormInstance();

        DataObject object;
        if (valueClass.hasChildren()) {
            // нужен такой чит, поскольку в FlowAction может вызываться ADDOBJ с конкретным классом, у которого есть потомки, но при этом не будет передан context.getValueObject()
            boolean valueProvided = !getValueClass().getDefaultValue().equals(context.getValueObject());
            if (form != null) {
                object = form.addObject((ConcreteCustomClass) (valueProvided ? form.getCustomClass((Integer) context.getValueObject()) : valueClass));
            } else {
                object = context.getSession().addObject((ConcreteCustomClass) (valueProvided ? valueClass.findClassID((Integer) context.getValueObject()) : valueClass), context.getModifier());
            }
        } else {
            if (form != null) {
                object = form.addObject((ConcreteCustomClass) valueClass);
            } else {
                object = context.getSession().addObject((ConcreteCustomClass) valueClass, context.getModifier());
            }
        }

        if (storeNewObjectProperty != null && object != null) {
            context.addActions(
                    storeNewObjectProperty.execute(object.getValue(), context)
            );
        }
    }

    @Override
    public DataClass getValueClass() {
        if (valueClass.hasChildren())
            return valueClass.getActionClass(valueClass);
        else
            return super.getValueClass();
    }


    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
        entity.forceViewType = ClassViewType.PANEL;

        entity.toDraw = form.getObject(valueClass).groupTo;
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getAddActionPropertyKeyStroke();
        propertyView.design.setIconPath("add.png");
        propertyView.showEditKey = false;
        propertyView.setPanelLocation(new ToolbarPanelLocationView());
    }
}
