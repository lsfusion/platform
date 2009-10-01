package platform.server.view.navigator;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.properties.LP;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.groups.AbstractGroup;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.form.client.FormView;
import platform.server.view.form.client.report.DefaultJasperDesign;
import platform.server.view.navigator.filter.FilterNavigator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class NavigatorForm<T extends BusinessLogics<T>> extends NavigatorElement<T> {

    public List<GroupObjectNavigator> groups = new ArrayList<GroupObjectNavigator>();
    public List<PropertyViewNavigator> propertyViews = new ArrayList<PropertyViewNavigator>();

    public Set<FilterNavigator> fixedFilters = new HashSet<FilterNavigator>();

    public void addFixedFilter(FilterNavigator filter) {
        fixedFilters.add(filter); 
    }

    public List<RegularFilterGroupNavigator> regularFilterGroups = new ArrayList<RegularFilterGroupNavigator>();
    public void addRegularFilterGroup(RegularFilterGroupNavigator group) { regularFilterGroups.add(group); }

    // счетчик идентификаторов
    int IDCount = 0;

    public int IDShift(int Offs) {
        IDCount += Offs;
        return IDCount;
    }

    protected ObjectNavigator addSingleGroupObjectImplement(ValueClass baseClass, String caption, List<Property> properties, Object... groups) {

        GroupObjectNavigator groupObject = new GroupObjectNavigator(IDShift(1));
        ObjectNavigator object = new ObjectNavigator(IDShift(1), baseClass, caption);
        groupObject.add(object);
        addGroup(groupObject);

        addPropertyView(properties, groups, object);

        return object;
    }

    protected void addGroup(GroupObjectNavigator group) {
        groups.add(group);
    }

    protected void addPropertyView(ObjectNavigator object, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object);
    }

    protected void addPropertyView(ObjectNavigator object1, ObjectNavigator object2, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object1, object2);
    }

    protected void addPropertyView(ObjectNavigator object1, ObjectNavigator object2, ObjectNavigator object3, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object1, object2, object3);
    }

    private void addPropertyView(List<Property> properties, Object[] groups, ObjectNavigator... objects) {

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

    void addPropertyView(List<Property> properties, Boolean upClasses, ObjectNavigator... objects) {
        addPropertyView(properties, (AbstractGroup)null, upClasses, objects);
    }

    protected void addPropertyView(List<Property> properties, AbstractGroup group, Boolean upClasses, ObjectNavigator... objects) {
        addPropertyView(properties, group, upClasses, null, objects);
    }

    protected void addPropertyView(List<Property> properties, AbstractGroup group, Boolean upClasses, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

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

    <P extends PropertyInterface<P>> void addPropertyView(Property<P> property, Boolean upClasses, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

        for (List<P> mapping : new ListPermutations<P>(property.interfaces)) {

            Map<P, AndClassSet> propertyInterface = new HashMap<P, AndClassSet>();
            int interfaceCount = 0;
            for (P iface : mapping) {
                ValueClass propertyClass = objects[interfaceCount++].baseClass;
                propertyInterface.put(iface, propertyClass.getUpSet());
            }
 
            if ((upClasses && property.anyInInterface(propertyInterface)) || (!upClasses && property.allInInterface(propertyInterface)))
                addPropertyView(new LP<P,Property<P>>(property, mapping), groupObject, objects);
        }
    }

    protected PropertyViewNavigator addPropertyView(LP property, ObjectNavigator... objects) {
        return addPropertyView(property, null, objects);
    }

    PropertyViewNavigator addPropertyView(LP property, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

        PropertyObjectNavigator propertyImplement = addPropertyObjectImplement(property, objects);
        return addPropertyView(groupObject, propertyImplement);
    }

    public GroupObjectNavigator getApplyObject(Collection<ObjectNavigator> objects) {
        GroupObjectNavigator result = null;
        for(GroupObjectNavigator group : groups)
            for(ObjectNavigator object : group)
                if(objects.contains(object)) {
                    result = group;
                    break;
                }
        return result;
    }

    PropertyViewNavigator addPropertyView(GroupObjectNavigator groupObject, PropertyObjectNavigator propertyImplement) {

        PropertyViewNavigator propertyView = new PropertyViewNavigator(IDShift(1),propertyImplement,(groupObject == null) ? getApplyObject(propertyImplement.mapping.values()) : groupObject);

        if (propertyImplement.property.sID != null) {

            // придется поискать есть ли еще такие sID, чтобы добиться уникальности sID
            boolean foundSID = false;
            for (PropertyViewNavigator property : propertyViews)
                if (BaseUtils.nullEquals(property.sID, propertyImplement.property.sID)) {
                    foundSID = true;
                    break;
                }
            propertyView.sID = propertyImplement.property.sID + ((foundSID) ? propertyView.ID : "");
        }

        propertyViews.add(propertyView);

        return propertyView;
    }

    protected PropertyObjectNavigator addPropertyObjectImplement(LP property, ObjectNavigator... objects) {

        return new PropertyObjectNavigator(property,objects);
    }


    PropertyViewNavigator getPropertyView(PropertyObjectNavigator prop) {

        PropertyViewNavigator resultPropView = null;
        for (PropertyViewNavigator propView : propertyViews)
            if (propView.view == prop)
                resultPropView = propView;

        return resultPropView;
    }


    protected PropertyViewNavigator getPropertyView(Property prop) {

        PropertyViewNavigator resultPropView = null;
        for (PropertyViewNavigator propView : propertyViews)
            if (propView.view.property == prop)
                resultPropView = propView;

        return resultPropView;
    }

    protected PropertyViewNavigator getPropertyView(Property prop, GroupObjectNavigator groupObject) {

        PropertyViewNavigator resultPropView = null;
        for (PropertyViewNavigator propView : propertyViews)
            if (propView.view.property == prop && propView.toDraw == groupObject)
                resultPropView = propView;

        return resultPropView;
    }

    void addHintsNoUpdate(List<Property> properties, AbstractGroup group) {

        for (Property property : properties) {
            if (group != null && !group.hasChild(property)) continue;
            addHintsNoUpdate(property);
        }
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> hintsSave = new HashSet<Property>();

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
    protected NavigatorForm(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) {
        super(parent, iID, caption);
        System.out.println("Initializing form "+caption+"...");
        isPrintForm = iisPrintForm;
    }

    protected FormView richDesign;
    FormView getRichDesign() { if (richDesign == null) return new DefaultFormView(this); else return richDesign; }

    protected JasperDesign reportDesign;
    JasperDesign getReportDesign() throws JRException { if (reportDesign == null) return new DefaultJasperDesign(getRichDesign()).design; else return reportDesign; }

    ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();
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
