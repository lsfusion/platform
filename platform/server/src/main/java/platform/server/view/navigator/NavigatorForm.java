package platform.server.view.navigator;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JasperDesign;
import platform.base.BaseUtils;
import platform.base.ListPermutations;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.logics.BusinessLogics;
import platform.server.logics.control.Control;
import platform.server.logics.control.ControlInterface;
import platform.server.logics.linear.LC;
import platform.server.logics.linear.LP;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.view.form.client.DefaultFormView;
import platform.server.view.form.client.FormView;
import platform.server.view.navigator.filter.FilterNavigator;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class NavigatorForm<T extends BusinessLogics<T>> extends NavigatorElement<T> {

    public List<GroupObjectNavigator> groups = new ArrayList<GroupObjectNavigator>();
    public List<ControlViewNavigator> controlViews = new ArrayList<ControlViewNavigator>();

    public Set<FilterNavigator> fixedFilters = new HashSet<FilterNavigator>();

    public void addFixedFilter(FilterNavigator filter) {
        fixedFilters.add(filter); 
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

    protected ObjectNavigator addSingleGroupObjectImplement(ValueClass baseClass, String caption, List<Control> controls, Object... groups) {

        GroupObjectNavigator groupObject = new GroupObjectNavigator(IDShift(1));
        ObjectNavigator object = new ObjectNavigator(IDShift(1), baseClass, caption);
        groupObject.add(object);
        addGroup(groupObject);

        addControlView(controls, groups, object);

        return object;
    }

    protected void addGroup(GroupObjectNavigator group) {
        groups.add(group);
    }

    protected void addControlView(ObjectNavigator object, List<Control> controls, Object... groups) {
        addControlView(controls, groups, object);
    }

    protected void addControlView(ObjectNavigator object1, ObjectNavigator object2, List<Control> controls, Object... groups) {
        addControlView(controls, groups, object1, object2);
    }

    protected void addControlView(ObjectNavigator object1, ObjectNavigator object2, ObjectNavigator object3, List<Control> controls, Object... groups) {
        addControlView(controls, groups, object1, object2, object3);
    }

    protected void addControlView(ObjectNavigator object1, ObjectNavigator object2, ObjectNavigator object3, ObjectNavigator object4, List<Control> controls, Object... groups) {
        addControlView(controls, groups, object1, object2, object3, object4);
    }

    private void addControlView(List<Control> controls, Object[] groups, ObjectNavigator... objects) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) continue;

            if (group instanceof AbstractGroup) {
                boolean upClasses = false;
                if ((i+1)<groups.length && groups[i+1] instanceof Boolean) upClasses = (Boolean)groups[i+1];
                addControlView(controls, (AbstractGroup)group, upClasses, objects);
            }
            else if (group instanceof LP)
                addControlView((LP)group, objects);
        }
    }

    void addControlView(List<Control> controls, Boolean upClasses, ObjectNavigator... objects) {
        addControlView(controls, (AbstractGroup)null, upClasses, objects);
    }

    protected void addControlView(List<Control> controls, AbstractGroup group, Boolean upClasses, ObjectNavigator... objects) {
        addControlView(controls, group, upClasses, null, objects);
    }

    protected void addControlView(List<Control> controls, AbstractGroup group, Boolean upClasses, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

        // приходится делать именно так, так как важен порядок следования свойств

        for (Control control : controls) {

            if (control.getParent() == null) continue;

            if (group == null && !(control instanceof DataProperty)) continue;

            if (group != null && !group.hasChild(control)) continue;

            if (control.interfaces.size() == objects.length) {

                addControlView(control, upClasses, groupObject, objects);
            }
        }
    }

    <P extends ControlInterface<P>> void addControlView(Control<P> control, Boolean upClasses, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

        for (List<P> mapping : new ListPermutations<P>(control.interfaces)) {

            Map<P, AndClassSet> propertyInterface = new HashMap<P, AndClassSet>();
            int interfaceCount = 0;
            for (P iface : mapping) {
                ValueClass propertyClass = objects[interfaceCount++].baseClass;
                propertyInterface.put(iface, propertyClass.getUpSet());
            }

            if ((upClasses && control.anyInInterface(propertyInterface)) || (!upClasses && control.allInInterface(propertyInterface)))
                addControlView(control.createLC(mapping), groupObject, objects);
        }
    }

    protected PropertyViewNavigator addControlView(LC control, ObjectNavigator... objects) {
        return addControlView(control, null, objects);
    }

    PropertyViewNavigator addControlView(LC control, GroupObjectNavigator groupObject, ObjectNavigator... objects) {

        return addControlView(groupObject, control.createNavigator(objects));
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

    PropertyViewNavigator addControlView(GroupObjectNavigator groupObject, ControlObjectNavigator controlImplement) {

        PropertyViewNavigator controlView = (PropertyViewNavigator) controlImplement.createView(IDShift(1), (groupObject == null) ? getApplyObject(controlImplement.getObjectImplements()) : groupObject);

        if (controlImplement.property.sID != null) {

            // придется поискать есть ли еще такие sID, чтобы добиться уникальности sID
            boolean foundSID = false;
            for (ControlViewNavigator property : controlViews)
                if (BaseUtils.nullEquals(property.sID, controlImplement.property.sID)) {
                    foundSID = true;
                    break;
                }
            controlView.sID = controlImplement.property.sID + ((foundSID) ? controlView.ID : "");
        }

        controlViews.add(controlView);

        assert richDesign == null && reportDesign == null;

        return controlView;
    }

    protected PropertyObjectNavigator addPropertyObjectImplement(LP property, ObjectNavigator... objects) {

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

    protected PropertyViewNavigator<?> getPropertyView(LP<?> lp, ObjectNavigator object) {
        return getPropertyView(lp.property, object.groupTo);
    }

    PropertyViewNavigator getPropertyView(PropertyObjectNavigator prop) {
        return (PropertyViewNavigator) getControlView(prop);
    }
    
    protected PropertyViewNavigator getPropertyView(Property prop) {
        return (PropertyViewNavigator) getControlView(prop);
    }

    protected PropertyViewNavigator getPropertyView(Property prop, GroupObjectNavigator groupObject) {
        return (PropertyViewNavigator) getControlView(prop, groupObject);
    }

    protected ControlViewNavigator getControlView(ControlObjectNavigator control) {

        ControlViewNavigator resultControlView = null;
        for (ControlViewNavigator propView : controlViews)
            if (propView.view.equals(control))
                resultControlView = propView;

        return resultControlView;
    }

    protected ControlViewNavigator getControlView(Control control) {

        ControlViewNavigator resultControlView = null;
        for (ControlViewNavigator controlView : controlViews)
            if (controlView.view.property == control)
                resultControlView = controlView;

        return resultControlView;
    }

    protected ControlViewNavigator getControlView(Control control, GroupObjectNavigator groupObject) {

        ControlViewNavigator resultControlView = null;
        for (ControlViewNavigator controlView : controlViews)
            if (controlView.view.property.equals(control) && controlView.toDraw.equals(groupObject))
                resultControlView = controlView;

        return resultControlView;
    }

    public void addHintsNoUpdate(List<Control> controls, AbstractGroup group) {

        for (Control control : controls) {
            if ((group == null || group.hasChild(control)) && control instanceof Property) 
                addHintsNoUpdate((Property) control);
        }
    }

    public Collection<Property> hintsNoUpdate = new HashSet<Property>();
    public Collection<Property> hintsSave = new HashSet<Property>();

    protected void addHintsNoUpdate(LP<?> prop) {
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

    protected NavigatorForm(int iID, String caption) { this(iID, caption, false); }
    NavigatorForm(int iID, String caption, boolean iisPrintForm) { this(null, iID, caption, iisPrintForm); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption) { this(parent, iID, caption, false); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption, boolean iisPrintForm) { this(parent, iID, caption, genSID(iID), iisPrintForm); }
    protected NavigatorForm(NavigatorElement parent, int iID, String caption, String isID, boolean iisPrintForm) {
        super(parent, iID, caption);
        System.out.println("Initializing form "+caption+"...");

        sID = isID;
        isPrintForm = iisPrintForm;
    }

    protected FormView richDesign;
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
}
