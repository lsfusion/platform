package platform.server.form.view;

import platform.base.identity.IdentityObject;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;

import javax.swing.*;
import java.awt.*;
import java.util.*;
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

    public PropertyDrawView createPropertyDraw(LP lp) {
        PropertyDrawEntity property = form.getPropertyDraw(lp);
        return createPropertyDraw(property);
    }

    public PropertyDrawView createPropertyDraw(String caption, LP lp, ObjectEntity... objects) {
        PropertyDrawView view = createPropertyDraw(lp, objects);
        view.caption = caption;
        return view;
    }

    public PropertyDrawView createPropertyDraw(LP lp, ObjectEntity... objects) {
        List<ObjectEntity> list = Arrays.asList(objects);
        Set<ObjectEntity> set = new HashSet<ObjectEntity>(list);
        PropertyDrawEntity propertyEntity = null;

        List<PropertyDrawEntity> props = form.propertyDraws;
        for (PropertyDrawEntity prop : props) {
            if (lp.property.getSID().equals(prop.propertyObject.property.getSID()) && new HashSet<ObjectEntity>(prop.propertyObject.mapping.values()).equals(set)) {
                propertyEntity = prop;
                break;
            }
        }

        return createPropertyDraw(propertyEntity);
    }

    public PropertyDrawView createPropertyDraw(PropertyDrawEntity property) {
        PropertyDrawView view = new PropertyDrawView(property);
        properties.add(view);
        return view;
    }

    public ClassChooserView createClassChooser() {
        ClassChooserView view = new ClassChooserView();
        view.ID = idGenerator.idShift();
        return view;
    }

    public GridView createGrid() {
        GridView view = new GridView();
        view.ID = idGenerator.idShift();
        return view;
    }

    public ShowTypeView createShowType() {
        ShowTypeView view = new ShowTypeView();
        view.ID = idGenerator.idShift();
        view.getConstraints().directions = new SimplexComponentDirections(0.01,0,0.0,0.01);
        return view;
    }

    public RegularFilterGroupView createRegularFilterGroup(RegularFilterGroupEntity filterGroup) {
        RegularFilterGroupView view = new RegularFilterGroupView(filterGroup);
        regularFilters.add(view);
        return view;
    }

    public ContainerView createMainContainer(String sID, String description) {
        ContainerView container = createContainer("Title", description, sID);
        return container;
    }

    public GroupObjectView createGroupObject(GroupObjectEntity groupObject, ShowTypeView showType, GridView grid, ToolbarView toolbar, FilterView filter) {
        GroupObjectView container = new GroupObjectView(idGenerator, groupObject, grid, showType, toolbar, filter);
        return container;
    }

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {
        if (comp1.container != comp2.container)
            throw new RuntimeException(ServerResourceBundle.getString("form.view.forbidden.to.create.the.intersection.of.objects.in.different.containers"));
        comp1.constraints.intersects.put(comp2, cons);
    }

    public void setEditKey(PropertyDrawView property, KeyStroke editKey){
        property.editKey = editKey;
    }

    public void setBackground(ComponentView property, Color color) {
        property.design.background = color;
    }

    public void setForeground(ComponentView property, Color color) {
        property.design.foreground = color;
    }

    public void setFont(ComponentView property, Font font) {
        property.design.font = font;
    }

    public void setHeaderFont(ComponentView property, Font font) {
        property.design.headerFont = font;
    }
}
