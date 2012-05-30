package platform.server.logics.property.actions;

import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.server.classes.*;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.view.panellocation.ToolbarPanelLocationView;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class SimpleAddObjectActionProperty extends CustomReadClassActionProperty {
    // обозначает класс объекта, который нужно добавить
    private CustomClass valueClass;

    private LCP storeNewObjectProperty;

    public SimpleAddObjectActionProperty(String sID, CustomClass valueClass, LCP storeNewObjectProperty) {
        super(sID, ServerResourceBundle.getString("logics.add"), new ValueClass[0]);
        this.valueClass = valueClass;

        this.storeNewObjectProperty = storeNewObjectProperty;
    }

    protected Read getReadClass(ExecutionContext context) {
        return new Read(valueClass, true);
    }

    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, ObjectClass readClass) throws SQLException {
        DataObject object = context.addObject((ConcreteCustomClass) readClass);

        if (storeNewObjectProperty != null && object != null) {
            storeNewObjectProperty.change(object.getValue(), context);
        }
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
