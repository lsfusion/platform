package platform.server.form.view;

import platform.base.identity.IdentityObject;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.linear.LP;

import java.util.List;

public class CustomFormView extends FormView {

    FormEntity form;

    public CustomFormView(FormEntity form) {
        super(form);
        this.form = form;
        caption = form.caption;
    }

    private <T extends IdentityObject> T getEntity(List<T> list, int ID) {
        for (T object : list) {
            if (object.ID == ID) {
                return object;
            }
        }
        return null;
    }

    /*
    private PropertyDrawEntity getPropertyEntity(int ID) {
        List<PropertyDrawEntity> list = form.propertyDraws;
        for (PropertyDrawEntity property : list) {
            if (property.propertyObject.property.ID == ID) {
                return property;
            }
        }
        return null;
    }
    */
    public ContainerView createContainer(String title, String description, String sID) {
        ContainerView container = new ContainerView(idGenerator.idShift());
        container.setTitle(title);
        container.setDescription(description);
        container.setSID(sID);
        return container;
    }

    public FunctionView createFunction(String caption) {
        FunctionView function = new FunctionView(idGenerator.idShift());
        function.setCaption(caption);
        return function;
    }

    public PropertyDrawView createPropertyDraw(LP lp) {
        PropertyDrawEntity property = form.getPropertyDraw(lp);
        PropertyDrawView view = new PropertyDrawView(property);
        properties.add(view);
        order.add(view);
        return view;
    }

    public ClassChooserView createClassChooser(int ID) {
        ClassChooserView view = new ClassChooserView();
        view.ID = idGenerator.idShift();
        return view;
    }

    public GridView createGrid(int ID) {
        GridView view = new GridView();
        view.ID = idGenerator.idShift();
        return view;
    }

    public ShowTypeView createShowType(int ID) {
        ShowTypeView view = new ShowTypeView();
        view.ID = idGenerator.idShift();
        return view;
    }

    public RegularFilterGroupView createRegularFilterGroup(int ID) {
        RegularFilterGroupView view = new RegularFilterGroupView();
        view.ID = idGenerator.idShift();
        return view;
    }

    public ContainerView createMainContainer(String sID, String description) {
        ContainerView container = createContainer("Title", description, sID);
        return container;
    }

    public GroupObjectView createGroupObject(GroupObjectEntity groupObject, ShowTypeView showType, GridView grid) {
        GroupObjectView container = new GroupObjectView(idGenerator, groupObject, grid, showType);
        return container;
    }

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {
        if (comp1.container != comp2.container)
            throw new RuntimeException("Запрещено создавать пересечения для объектов в разных контейнерах");
        comp1.constraints.intersects.put(comp2, cons);
    }
}
