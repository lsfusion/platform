package platform.server.form.view;

import platform.base.identity.IdentityObject;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;

import java.util.List;

public class CustomFormView extends FormView {

    FormEntity form;

    public CustomFormView(FormEntity form) {
        this.form = form;
    }

    private <T extends IdentityObject> T getEntity(List<T> list, int ID) {
        for (T object : list) {
            if (object.ID == ID) {
                return object;
            }
        }
        return null;
    }

    private PropertyDrawEntity getPropertyEntity(int ID) {
        return (PropertyDrawEntity) getEntity(form.propertyDraws, ID);
    }

    public ContainerView createContainer(String title, String description, String sID) {
        ContainerView container = new ContainerView(idGenerator.idShift());
        container.setTitle(title);
        container.setDescription(description);
        container.setSID(sID);
        return container;
    }

    public FunctionView createFunction() {
        return new FunctionView(idGenerator.idShift());
    }

    public PropertyDrawView createPropertyDraw(int ID) {
        PropertyDrawView view = new PropertyDrawView(getPropertyEntity(ID));
        return view;
    }


}
