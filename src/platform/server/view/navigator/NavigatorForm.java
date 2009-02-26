package platform.server.view.navigator;

import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.server.logics.BusinessLogics;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.logics.properties.linear.LP;
import platform.server.view.form.*;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.form.client.FormView;
import platform.server.view.form.report.DefaultJasperDesign;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class NavigatorForm<T extends BusinessLogics<T>> extends NavigatorElement<T> {

    public List<GroupObjectImplement> Groups = new ArrayList();
    public List<PropertyView> propertyViews = new ArrayList();

    Set<Filter> fixedFilters = new HashSet();
    public void addFixedFilter(Filter filter) { fixedFilters.add(filter); }

    public List<RegularFilterGroup> regularFilterGroups = new ArrayList();
    public void addRegularFilterGroup(RegularFilterGroup group) { regularFilterGroups.add(group); }

    // счетчик идентификаторов
    int IDCount = 0;

    public int IDShift(int Offs) {
        IDCount += Offs;
        return IDCount;
    }

    protected ObjectImplement addSingleGroupObjectImplement(RemoteClass baseClass, String caption, List<Property> properties, Object... groups) {

        GroupObjectImplement groupObject = new GroupObjectImplement(IDShift(1));
        ObjectImplement object = new ObjectImplement(IDShift(1), baseClass, caption, groupObject);
        addGroup(groupObject);

        addPropertyView(properties, groups, object);

        return object;
    }

    protected void addGroup(GroupObjectImplement Group) {
        Groups.add(Group);
        Group.Order = Groups.size();
    }

    protected void addPropertyView(ObjectImplement object, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object);
    }

    protected void addPropertyView(ObjectImplement object1, ObjectImplement object2, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object1, object2);
    }

    private void addPropertyView(List<Property> properties, Object[] groups, ObjectImplement... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) continue;

            if (group instanceof AbstractGroup) {
                boolean upClasses = false;
                if ((i+1)<groups.length && groups[i+1] instanceof Boolean) upClasses = (Boolean)groups[i+1];
                addPropertyView(properties, (AbstractGroup)group, upClasses, objects);
            }
            else if (group instanceof LP)
                addPropertyView((LP)group, objects);
        }
    }

    void addPropertyView(List<Property> properties, Boolean upClasses, ObjectImplement... objects) {
        addPropertyView(properties, (AbstractGroup)null, upClasses, objects);
    }

    protected void addPropertyView(List<Property> properties, AbstractGroup group, Boolean upClasses, ObjectImplement... objects) {
        addPropertyView(properties, group, upClasses, null, objects);
    }

    protected void addPropertyView(List<Property> properties, AbstractGroup group, Boolean upClasses, GroupObjectImplement groupObject, ObjectImplement... objects) {

        // приходится делать именно так, так как важен порядок следования свойств

        for (Property property : properties) {

            if (property.getParent() == null) continue;

            if (group == null && !(property instanceof DataProperty)) continue;

            if (group != null && !group.hasChild(property)) continue;

            if (property.interfaces.size() == objects.length) {

                addPropertyView(property, upClasses, groupObject, objects);
            }
        }
    }

    <P extends PropertyInterface<P>> void addPropertyView(Property<P> property, Boolean upClasses, GroupObjectImplement groupObject, ObjectImplement... objects) {

        for (List<P> mapping : new ListPermutations<P>(property.interfaces)) {

            InterfaceClass<P> propertyInterface = new InterfaceClass();
            int interfaceCount = 0;
            for (P iface : mapping) {
                RemoteClass baseClass = objects[interfaceCount++].baseClass;
                propertyInterface.put(iface, (upClasses) ? ClassSet.getUp(baseClass)
                                                         : new ClassSet(baseClass));
            }

            if (!property.getValueClass(propertyInterface).isEmpty()) {
                addPropertyView(new LP<P,Property<P>>(property, mapping), groupObject, objects);
            }
        }
    }

    protected PropertyView addPropertyView(LP property, ObjectImplement... objects) {
        return addPropertyView(property, null, objects);
    }

    PropertyView addPropertyView(LP property, GroupObjectImplement groupObject, ObjectImplement... objects) {

        PropertyObjectImplement propertyImplement = addPropertyObjectImplement(property, objects);
        return addPropertyView(groupObject, propertyImplement);
    }

    PropertyView addPropertyView(GroupObjectImplement groupObject, PropertyObjectImplement propertyImplement) {

        PropertyView propertyView = new PropertyView(IDShift(1),propertyImplement,(groupObject == null) ? propertyImplement.getApplyObject() : groupObject);

        if (propertyImplement.property.sID != null) {

            // придется поискать есть ли еще такие sID, чтобы добиться уникальности sID
            boolean foundSID = false;
            for (PropertyView property : propertyViews)
                if (BaseUtils.compareObjects(property.sID, propertyImplement.property.sID)) {
                    foundSID = true;
                    break;
                }
            propertyView.sID = propertyImplement.property.sID + ((foundSID) ? propertyView.ID : "");
        }

        propertyViews.add(propertyView);

        return propertyView;
    }

    protected PropertyObjectImplement addPropertyObjectImplement(LP property, ObjectImplement... objects) {

        PropertyObjectImplement propertyImplement = new PropertyObjectImplement(property.property);

        ListIterator<PropertyInterface> i = property.listInterfaces.listIterator();
        for(ObjectImplement object : objects) {
            propertyImplement.mapping.put(i.next(), object);
        }

        return propertyImplement;
    }


    PropertyView getPropertyView(PropertyObjectImplement prop) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.view == prop)
                resultPropView = propView;
        }

        return resultPropView;
    }


    protected PropertyView getPropertyView(Property prop) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.view.property == prop)
                resultPropView = propView;
        }

        return resultPropView;
    }

    protected PropertyView getPropertyView(Property prop, GroupObjectImplement groupObject) {

        PropertyView resultPropView = null;
        for (PropertyView propView : propertyViews) {
            if (propView.view.property == prop && propView.toDraw == groupObject)
                resultPropView = propView;
        }

        return resultPropView;
    }

    void addHintsNoUpdate(List<Property> properties, AbstractGroup group) {

        for (Property property : properties) {
            if (group != null && !group.hasChild(property)) continue;
            addHintsNoUpdate(property);
        }
    }

    Collection<Property> hintsNoUpdate = new HashSet();
    Collection<Property> hintsSave = new HashSet();

    protected void addHintsNoUpdate(Property prop) {
        hintsNoUpdate.add(prop);
    }

    void addHintsSave(Property prop) {
        hintsSave.add(prop);
    }

    public boolean isPrintForm;

    protected NavigatorForm(int iID, String caption) { this(iID, caption, false); }
    NavigatorForm(int iID, String caption, boolean iisPrintForm) { this(null, iID, caption, iisPrintForm); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption) { this(parent, iID, caption, false); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) { super(parent, iID, caption); isPrintForm = iisPrintForm; }

    protected FormView richDesign;
    FormView getRichDesign() { if (richDesign == null) return new DefaultFormView(this); else return richDesign; }

    protected JasperDesign reportDesign;
    JasperDesign getReportDesign() { if (reportDesign == null) return new DefaultJasperDesign(getRichDesign()); else return reportDesign; }

    ArrayList<NavigatorElement> relevantElements = new ArrayList();
    public void addRelevantElement(NavigatorElement relevantElement) {
        relevantElements.add(relevantElement);
    }

    public byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(isPrintForm);
    }
}
