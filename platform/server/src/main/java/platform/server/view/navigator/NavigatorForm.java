package platform.server.view.navigator;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.base.OrderedMap;
import platform.server.classes.ValueClass;
import platform.server.classes.CustomClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.logics.BusinessLogics;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.form.client.FormView;
import platform.server.view.form.RemoteForm;
import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.filter.OrderViewNavigator;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionResult;

import javax.swing.*;
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

    public OrderedMap<OrderViewNavigator, Boolean> fixedOrders = new OrderedMap<OrderViewNavigator, Boolean>();

    public void addFixedOrder(OrderViewNavigator order, boolean descending) {
        fixedOrders.put(order, descending);
    }

    public List<RegularFilterGroupNavigator> regularFilterGroups = new ArrayList<RegularFilterGroupNavigator>();
    public void addRegularFilterGroup(RegularFilterGroupNavigator group) {
        regularFilterGroups.add(group);
    }

    protected RegularFilterGroupNavigator addSingleRegularFilterGroup(FilterNavigator ifilter, String iname, KeyStroke ikey) {

        RegularFilterGroupNavigator filterGroup = new RegularFilterGroupNavigator(IDShift(1));
        filterGroup.addFilter(new RegularFilterNavigator(IDShift(1), ifilter, iname, ikey));
        addRegularFilterGroup(filterGroup);

        return filterGroup;
    }

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

    protected void addPropertyView(ObjectNavigator object1, ObjectNavigator object2, ObjectNavigator object3, ObjectNavigator object4, List<Property> properties, Object... groups) {
        addPropertyView(properties, groups, object1, object2, object3, object4);
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
                this.addPropertyView((LP)group, objects);
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
                addPropertyView(new LP<P>(property, mapping), groupObject, objects);
        }
    }

    public PropertyViewNavigator addPropertyView(LP property, ObjectNavigator... objects) {
        return addPropertyView(property, null, objects);
    }

    <P extends PropertyInterface> PropertyViewNavigator addPropertyView(LP<P> property, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

        return addPropertyView(groupObject, new PropertyObjectNavigator<P>(property, objects));
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

    <P extends PropertyInterface> PropertyViewNavigator<P> addPropertyView(GroupObjectNavigator groupObject, PropertyObjectNavigator<P> propertyImplement) {

        PropertyViewNavigator<P> propertyView = new PropertyViewNavigator<P>(IDShift(1), propertyImplement, (groupObject == null) ? getApplyObject(propertyImplement.getObjectImplements()) : groupObject);

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

        assert richDesign == null && reportDesign == null;

        return propertyView;
    }

    protected PropertyObjectNavigator addPropertyObjectImplement(LP property, PropertyInterfaceNavigator... objects) {

        return new PropertyObjectNavigator(property,objects);
    }


    protected PropertyObjectNavigator getPropertyImplement(LP<?> lp) {
        return getPropertyView(lp).view;
    }

    protected PropertyViewNavigator<?> getPropertyView(LP<?> lp) {
        return getPropertyView(lp.property);
    }

    protected PropertyObjectNavigator getPropertyImplement(LP<?> lp, ObjectNavigator object) {
        return getPropertyView(lp, object).view;
    }

    protected PropertyObjectNavigator getPropertyImplement(LP<?> lp, GroupObjectNavigator groupObject) {
        return getPropertyView(lp, groupObject).view;
    }

    protected PropertyViewNavigator<?> getPropertyView(LP<?> lp, ObjectNavigator object) {
        return getPropertyView(lp.property, object.groupTo);
    }

    protected PropertyViewNavigator<?> getPropertyView(LP<?> lp, GroupObjectNavigator groupObject) {
        return getPropertyView(lp.property, groupObject);
    }

    protected PropertyViewNavigator getPropertyView(PropertyObjectNavigator property) {

        PropertyViewNavigator resultPropertyView = null;
        for (PropertyViewNavigator propView : propertyViews)
            if (propView.view.equals(property))
                resultPropertyView = propView;

        return resultPropertyView;
    }

    public PropertyObjectNavigator getPropertyImplement(Property property) {
        return getPropertyView(property).view;
    }

    protected PropertyViewNavigator getPropertyView(Property property) {

        PropertyViewNavigator resultPropertyView = null;
        for (PropertyViewNavigator<?> propertyView : propertyViews)
            if (propertyView.view.property == property)
                resultPropertyView = propertyView;

        return resultPropertyView;
    }

    protected PropertyViewNavigator getPropertyView(Property property, GroupObjectNavigator groupObject) {

        PropertyViewNavigator resultPropertyView = null;
        for (PropertyViewNavigator<?> propertyView : propertyViews)
            if (propertyView.view.property.equals(property) && propertyView.toDraw.equals(groupObject))
                resultPropertyView = propertyView;

        return resultPropertyView;
    }

    public void addHintsNoUpdate(List<Property> properties, AbstractGroup group) {

        for (Property property : properties) {
            if ((group == null || group.hasChild(property)))
                addHintsNoUpdate(property);
        }
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> hintsSave = new HashSet<Property>();

    protected void addHintsNoUpdate(LP<?> prop) {
        addHintsNoUpdate(prop.property);
    }

    protected void addHintsNoUpdate(Property prop) {
        hintsNoUpdate.add(prop);
    }

    void addHintsSave(Property prop) {
        hintsSave.add(prop);
    }

    private static String genSID(int iID) {
        return "form" + iID;
    }

    public String sID;
    public boolean isPrintForm;

    protected NavigatorForm(int ID, String caption) { this(ID, caption, false); }
    NavigatorForm(int iID, String caption, boolean iisPrintForm) { this(null, iID, caption, iisPrintForm); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption) { this(parent, iID, caption, false); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) { this(parent, iID, caption, genSID(iID), iisPrintForm); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption, String isID, boolean iisPrintForm) {
        super(parent, iID, caption);
        System.out.println("Initializing form "+caption+"...");

        sID = isID;
        isPrintForm = iisPrintForm;
    }

    public FormView richDesign;
    public DefaultFormView createDefaultRichDesign() { return new DefaultFormView(this); }
    public FormView getRichDesign() { if (richDesign == null) return new DefaultFormView(this); else return richDesign; }

    protected JasperDesign reportDesign;
    public JasperDesign getReportDesign() throws JRException { return reportDesign; }

    public ArrayList<NavigatorElement> relevantElements = new ArrayList<NavigatorElement>();

    public byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(isPrintForm);
    }

    public List<CustomClass> barcodeClasses = new ArrayList<CustomClass>();
    public List<PropertyObjectNavigator> barcodeProperties = new ArrayList<PropertyObjectNavigator>();
    public void addBarcode(CustomClass customClass, LP lp) {
        addBarcode(customClass, getPropertyImplement(lp));
    }
    public void addBarcode(CustomClass customClass, PropertyObjectNavigator property) {
        barcodeClasses.add(customClass);
        barcodeProperties.add(property);
    }

    public ConcreteCustomClass barcodeAdd;

    public List<ObjectNavigator> autoActionObjects = new ArrayList<ObjectNavigator>();
    public List<PropertyObjectNavigator> autoActions = new ArrayList<PropertyObjectNavigator>();
    public void addAutoAction(ObjectNavigator object, LP action) {
        addAutoAction(object, getPropertyImplement(action));
    }

    public void addAutoAction(ObjectNavigator object, PropertyObjectNavigator action) {

//        assert action.property instanceof ActionProperty;

        autoActionObjects.add(object);
        autoActions.add(action);
    }

    public List<? extends ClientAction> getApplyActions(RemoteForm<?> form) {
        return null; // будем возвращать именно null, чтобы меньше данных передавалось        
    }

    public String checkApplyActions(int actionID, ClientActionResult result) {
        return null;
    }

    public Map<ObjectNavigator, PropertyViewNavigator> addObjectViews = new HashMap<ObjectNavigator, PropertyViewNavigator>();
    public PropertyViewNavigator addAddObjectView(ObjectNavigator object, ActionProperty property) {
        PropertyViewNavigator propertyView = addPropertyView(new LP<ClassPropertyInterface>(property));
        addObjectViews.put(object, propertyView);
        return propertyView;
    }

    public Map<ObjectNavigator, PropertyViewNavigator> deleteObjectViews = new HashMap<ObjectNavigator, PropertyViewNavigator>();
    public PropertyViewNavigator addDeleteObjectView(ObjectNavigator object, ActionProperty property) {
        PropertyViewNavigator propertyView = addPropertyView(new LP<ClassPropertyInterface>(property), object);
        deleteObjectViews.put(object, propertyView);
        return propertyView;
    }
}
