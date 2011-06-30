package platform.client.code;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.*;
import platform.client.descriptor.editor.base.FlatButton;
import platform.client.descriptor.filter.*;
import platform.client.logics.*;
import platform.interop.ClassViewType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.layout.SimplexConstraints;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CodeGenerator {

    private static String intend;
    private static Map<RegularFilterGroupDescriptor, String> filterGroups;
    private static Map<ClientRegularFilterGroup, RegularFilterGroupDescriptor> filterGroupViews;

    private static void addClassDeclaration(FormDescriptor form, StringBuilder result) {
        result.append(intend + "private class " + form.getSID() + " extends FormEntity {\n");
    }

    private static void addInstanceVariable(FormDescriptor form, StringBuilder result) {
        for (GroupObjectDescriptor group : form.groupObjects) {
            if (group.objects.size() != 1) {
                result.append(intend + "GroupObjectEntity " + group.getVariableName() + ";\n");
            }
            for (ObjectDescriptor object : group.objects) {
                result.append(intend + "ObjectEntity " + object.getVariableName() + ";\n");
            }
        }

        for (PropertyDrawDescriptor prop : form.propertyDraws) {
            if (prop.getPropertyObject().property.isField) {
                result.append(intend + "PropertyDrawEntity prop" + prop.getSID() + ";\n");
            }
        }

        filterGroups = new HashMap<RegularFilterGroupDescriptor, String>();
        filterGroupViews = new HashMap<ClientRegularFilterGroup, RegularFilterGroupDescriptor>();
        for (RegularFilterGroupDescriptor group : form.regularFilterGroups) {
            String name = "filterGroup" + group.getID();
            result.append(intend + "RegularFilterGroupEntity " + name + ";\n");
            filterGroups.put(group, name);
            filterGroupViews.put(group.client, group);
        }
    }

    private static void addConstructorDeclaration(FormDescriptor form, StringBuilder result) {
        result.append(intend + "public " + form.getSID() + "(NavigatorElement parent, int iID, String caption) {\n");
        intend += "    ";
        result.append(intend + "super(parent, iID, caption);\n");
    }

    private static void addGroupObject(GroupObjectDescriptor groupObject, StringBuilder result) {
        String grName = groupObject.getVariableName();

        if (groupObject.objects.size() == 1) {
            ObjectDescriptor object = groupObject.objects.get(0);
            result.append(intend + object.getVariableName() + " = addSingleGroupObject(" +
                    object.getBaseClass().getCode() + ", " + (object.client.getCaption() == null ? null : "\"" + object.client.getCaption() + "\"") + ");\n");
        } else {
            result.append(intend + grName + " = new GroupObjectEntity(genID());\n");
            for (ObjectDescriptor object : groupObject.objects) {
                String objName = object.getVariableName();
                result.append(intend + objName + " = new ObjectEntity(genID(), " +
                        object.getBaseClass().getCode() + ", " + (object.client.getCaption() == null ? null : "\"" + object.client.getCaption() + "\"") + ");\n");
                result.append(intend + grName + ".add(" + objName + ");\n");
            }
            result.append(intend + "addGroup(" + grName + ");\n");
        }
        if (groupObject.getInitClassView() != ClassViewType.GRID) {
            result.append(intend + grName + ".initClassView = ClassViewType." + groupObject.getInitClassView() + ";\n");
        }
    }

    private static void addProperties(FormDescriptor form, StringBuilder result) {
        HashMap<Set<PropertyObjectInterfaceDescriptor>, Collection<PropertyDrawDescriptor>> propertiesInt =
                new HashMap<Set<PropertyObjectInterfaceDescriptor>, Collection<PropertyDrawDescriptor>>();

        for (PropertyDrawDescriptor property : form.propertyDraws) {
            if (property.getPropertyObject().property.isField) {
                result.append(intend + "prop" + property.getSID() + " = addPropertyDraw(" + property.getPropertyObject().property.code);
                for (PropertyObjectInterfaceDescriptor objectInterface : property.getPropertyObject().mapping.values()) {
                    if (objectInterface instanceof ObjectDescriptor) {
                        ObjectDescriptor object = (ObjectDescriptor) objectInterface;
                        result.append(", " + object.getVariableName());
                    }
                }
                result.append(");\n");

                for (GroupObjectDescriptor descriptor : property.getColumnGroupObjects()) {
                    result.append(intend + "prop" + property.getSID() + ".addColumnGroupObject(" + descriptor.getVariableName() + ");\n");
                }

                continue;
            }
            Set<PropertyObjectInterfaceDescriptor> values = new HashSet<PropertyObjectInterfaceDescriptor>(property.getPropertyObject().mapping.values());
            Collection<PropertyDrawDescriptor> props = propertiesInt.get(values);
            if (props == null) {
                props = new ArrayList<PropertyDrawDescriptor>();
            }
            props.add(property);
            propertiesInt.put(values, props);
        }


        for (Set<PropertyObjectInterfaceDescriptor> set : propertiesInt.keySet()) {
            Collection<PropertyDrawDescriptor> propCollection = propertiesInt.get(set);
            StringBuilder propArray = new StringBuilder();
            propArray.append("new LP[]{");

            for (PropertyDrawDescriptor prop : propCollection) {
                propArray.append(prop.getPropertyObject().property.code + ", ");
            }
            propArray.replace(propArray.length() - 2, propArray.length(), "}");

            StringBuilder entityArray = new StringBuilder();
            entityArray.append(", ");
            for (PropertyObjectInterfaceDescriptor objectDescriptorInt : set) {
                if (objectDescriptorInt instanceof ObjectDescriptor) {
                    ObjectDescriptor object = (ObjectDescriptor) objectDescriptorInt;
                    entityArray.append(object.getVariableName() + ", ");
                }
            }
            entityArray.replace(entityArray.length() - 2, entityArray.length(), ")");

            result.append(intend + "addPropertyDraw(" + propArray.toString() + entityArray.toString() + ";\n");
        }
    }

    public static String addFixedFilters(Set<FilterDescriptor> filters, StringBuilder result) {
        for (FilterDescriptor filter : filters) {
            result.append(intend + "addFixedFilter(");
            result.append(filter.getCodeConstructor());
            result.append(");\n");
        }
        return result.toString();
    }

    public static String addRegularFilterGroups(List<RegularFilterGroupDescriptor> groups, StringBuilder result) {
        for (RegularFilterGroupDescriptor group : groups) {
            String groupName = filterGroups.get(group);
            result.append(intend + groupName + " = " + group.getCodeConstructor());
            for (RegularFilterDescriptor filter : group.filters) {
                result.append("\n" + intend + groupName + ".addFilter(" + filter.getCodeConstructor() + ", " + filter.getClient().showKey + ");");
            }
            result.append("\n" + intend + "addRegularFilterGroup(" + groupName + ");");
            result.append("\n");
        }
        return result.toString();
    }

    private static String addContainers(FormDescriptor form, ClientComponent component, String name) {
        StringBuilder temp = new StringBuilder();
        if (component instanceof ClientContainer) {
            temp.append("\n");
            for (ClientComponent child : ((ClientContainer) component).children) {
                String childName = child.getVariableName(form);
                String ifDefault = "";

                if (!(child instanceof ClientRegularFilterGroup)) {
                    ifDefault += child.getCodeConstructor();
                } else {
                    ifDefault +=((ClientRegularFilterGroup) child).getCodeConstructor(filterGroups.get(filterGroupViews.get(child)));
                }

                if (child.shouldBeDeclared() || child instanceof ClientContainer || child instanceof ClientGrid) {
                    temp.append(intend + child.getCodeClass() + " " + childName + " = " + ifDefault + ";\n");

                    temp.append(changeConstraints(child, childName));

                    //группы в колонки
                    if(child instanceof ClientPropertyDraw) {
                        List<ClientGroupObject> groupList = ((ClientPropertyDraw)child).columnGroupObjects;
                        if ( !groupList.isEmpty() ) {
                            for (ClientGroupObject group : groupList) {
                                for (GroupObjectDescriptor obj : form.groupObjects) {
                                    if (obj.client.equals(group)) {
                                        temp.append(intend + childName + ".entity.addColumnGroupObject(" + obj.getVariableName() + ");\n");
                                    }
                                }
                            }
                            temp.append(intend + childName + ".entity.setPropertyCaption(" +
                                    ((ClientPropertyDraw)child).getDescriptor().getPropertyCaption().getInstanceCode());
                            temp.append(");\n");
                        }
                    }
                    temp.append(intend + name + ".add(" + childName + ");\n");
                } else {
                    temp.append(intend + name + ".add(" + ifDefault + ");\n");
                }
                temp.append(addContainers(form, child, childName));
            }
        }
        return temp.toString();
    }

    private static String setEditKeys(FormDescriptor form) {
        String result = "\n";
        for (PropertyDrawDescriptor property : form.propertyDraws) {
            if (property.client.editKey != null) {
                result += intend + property.client.getCodeEditKey(property.client.getVariableName(form));
            }
        }
        return result + "\n";
    }

    private static String changeConstraints(ClientComponent component, String name) {
        StringBuilder toReturn = new StringBuilder();
        SimplexConstraints constraints = component.getDefaultConstraints();

        if (!component.constraints.childConstraints.equals(constraints.childConstraints)) {
            toReturn.append(intend + name + ".constraints.childConstraints = " + component.constraints.childConstraints.getConstraintCode() + ";\n");
        }

        if (component.constraints.fillHorizontal != constraints.fillHorizontal) {
            toReturn.append(intend + name + ".constraints.fillHorizontal = " + component.constraints.fillHorizontal + ";\n");
        }

        if (component.constraints.fillVertical != constraints.fillVertical) {
            toReturn.append(intend + name + ".constraints.fillVertical = " + component.constraints.fillVertical + ";\n");
        }

        Insets insetsInside = component.constraints.insetsInside;
        if (!insetsInside.equals(constraints.insetsInside)) {
            toReturn.append(intend + name + ".constraints.insetsInside = new Insets(" + insetsInside.top + ", " +
                    insetsInside.left + ", " + insetsInside.bottom + ", " + insetsInside.right + ");\n");
        }

        Insets insetsSibling = component.constraints.insetsSibling;
        if (!insetsSibling.equals(constraints.insetsSibling)) {
            toReturn.append(intend + name + ".constraints.insetsSibling = new Insets(" + insetsSibling.top + ", " +
                    insetsSibling.left + ", " + insetsSibling.bottom + ", " + insetsSibling.right + ");\n");
        }

        SimplexComponentDirections directions = component.constraints.directions;
        if (!directions.equals(constraints.directions)) {
            toReturn.append(intend + name + ".constraints.directions = new SimplexComponentDirections(" +
                    directions.T + ", " + directions.L + ", " + directions.B + ", " + directions.R + ");\n");
        }

        if (component.constraints.maxVariables != constraints.maxVariables) {
            toReturn.append(intend + name + ".constraints.maxVariables = " + constraints.maxVariables + ";\n");
        }

        return toReturn.toString();
    }

    private static String changeIntersects(ClientComponent component, FormDescriptor form) {
        StringBuilder toReturn = new StringBuilder();
        Map<ClientComponent, DoNotIntersectSimplexConstraint> map = component.getIntersects();
        if (!map.isEmpty()) {
            for (ClientComponent single : map.keySet()) {
                toReturn.append(intend + "design.addIntersection(" +
                        component.getVariableName(form) + ", " + single.getVariableName(form) + ", " + map.get(single).getConstraintCode() + ");\n");
            }
        }
        if (component instanceof ClientContainer) {
            for (ClientComponent child : ((ClientContainer)component).children) {
                toReturn.append(changeIntersects(child, form));
            }
        }
        return toReturn.toString();
    }

    private static String adjustDesign(ClientComponent component, String name, FormDescriptor form) {
        StringBuilder temp = new StringBuilder();
        if(component instanceof ClientContainer) {
            for (ClientComponent child : ((ClientContainer) component).children) {
                temp.append(adjustDesign(child, child.getVariableName(form), form));
            }
        }
        if (component.design.background != null) {
            temp.append(intend + component.design.getCodeBackground(name));
        }
        if (component.design.foreground != null) {
            temp.append(intend + component.design.getCodeForeground(name));
        }
        if (component.design.font != null) {
            temp.append(intend + component.design.getCodeFont(name));
        }
        if (component.design.headerFont != null) {
            temp.append(intend + component.design.getCodeHeaderFont(name));
        }
        return temp.toString();
    }

    private static void addRichDesign(FormDescriptor form, StringBuilder result) {
        result.append(intend + "@Override\n");
        result.append(intend + "public FormView createDefaultRichDesign() {\n");
        intend += "    ";
        result.append(intend + "CustomFormView design = new CustomFormView(this);\n");
        result.append(intend + "design.setMainContainer(design.createMainContainer(\"" + form.client.mainContainer.getSID() +
                "\", \"" + form.client.mainContainer.getCaption() + "\"));\n");
        result.append(addContainers(form, form.client.mainContainer, "design.mainContainer"));
        result.append(changeConstraints(form.client.mainContainer, "design.mainContainer"));
        result.append("\n" + changeIntersects(form.client.mainContainer, form));

        result.append(setEditKeys(form));
        result.append(adjustDesign(form.client.mainContainer, "design.mainContainer", form)).append("\n");

        for (GroupObjectDescriptor groupObject : form.groupObjects) {
            String name = "groupView" + groupObject.getID();
            result.append(intend + "GroupObjectView " + name + " = design.createGroupObject(" + groupObject.getVariableName() + ", " +
                    groupObject.client.showType.getVariableName(form) + ", " + groupObject.client.grid.getVariableName(form) + ");\n");
            result.append(intend + "design.groupObjects.add(" + name + ");\n");

            for (ObjectDescriptor object : groupObject.objects) {
                result.append(intend + name + ".getObjectView(" + object.getVariableName() + ").changeClassChooserLocation(" +
                        object.client.classChooser.getVariableName(form) + ");\n");

            }
        }

        result.append(intend + "return design;\n");

    }

    public static void addShouldProceed(StringBuilder result) {
        result.append("\n");
        result.append(intend + "@Override\n");
        result.append(intend + "public boolean shouldProceedDefaultDraw() {\n");
        result.append(intend + "    return false;\n");
        result.append(intend + "}\n");
    }

    public static String formDescriptorCode(FormDescriptor form) {
        intend = "";
        StringBuilder result = new StringBuilder();

        addClassDeclaration(form, result);
        intend += "    ";
        addInstanceVariable(form, result);
        result.append("\n");

        addConstructorDeclaration(form, result);

        for (GroupObjectDescriptor groupObject : form.groupObjects) {
            addGroupObject(groupObject, result);
        }

        result.append("\n");

        addProperties(form, result);

        result.append("\n");

        addFixedFilters(form.fixedFilters, result);

        result.append("\n");

        addRegularFilterGroups(form.regularFilterGroups, result);

        intend = "    ";
        result.append(intend + "}\n");

        result.append("\n");
        addRichDesign(form, result);

        intend = "    ";
        result.append(intend + "}\n");

        addShouldProceed(result);

        result.append("}\n");

        return result.toString();
    }

    public static Component getComponent(FormDescriptor form) {
        return new CodeFlatButton(form);
    }

    private static class CodeFlatButton extends FlatButton {
        FormDescriptor form;

        public CodeFlatButton(FormDescriptor form) {
            super(ClientResourceBundle.getString("code.generate.code"));
            this.form = form;
        }

        public void onClick() {
            new CodeDialog(null, formDescriptorCode(form));
        }
    }
}
