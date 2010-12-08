package platform.client.code;

import platform.client.descriptor.*;
import platform.client.descriptor.filter.*;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexComponentDirections;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CodeGenerator {

    private static String intend;
    private static int id = 1;
    private static Map<ObjectDescriptor, String> objectNames = new HashMap<ObjectDescriptor, String>();

    private static int getID() {
        return id++;
    }

    private static void addClassDeclaration(FormDescriptor form, StringBuilder result) {
        result.append(intend + "private class " + form.getSID() + " extends FormEntity {\n");
    }

    private static void addInstanceVariable(FormDescriptor form, StringBuilder result) {
        for (GroupObjectDescriptor group : form.groupObjects) {
            result.append(intend + "GroupObjectEntity grObj" + group.getSID() + ";\n");
            for (ObjectDescriptor object : group.objects) {
                String name = object.getSID();
                result.append(intend + "ObjectEntity " + name + ";\n");
                objectNames.put(object, name);
            }
        }
    }

    private static void addConstructorDeclaration(FormDescriptor form, StringBuilder result) {
        result.append(intend + "public " + form.getSID() + " (NavigatorElement parent, int iID, String caption) {\n");
        intend += "   ";
        result.append(intend + "super(parent, iID, caption);\n");
    }

    private static void addGroupObject(GroupObjectDescriptor groupObject, StringBuilder result) {
        String grName = "grObj" + groupObject.getSID();
        result.append(intend + grName + " = new GroupObjectEntity(genID());\n");

        for (ObjectDescriptor object : groupObject.objects) {
            String objName = objectNames.get(object);
            result.append(intend + objName + " = new ObjectEntity(genID(), findValueClass(\"" +
                    object.getBaseClass().getSID() + "\"), " + object.getCaption() + ");\n");
            result.append(intend + grName + ".add(" + objName + ");\n");
        }
        result.append(intend + "addGroup(" + grName + ");\n");

    }

    private static void addProperties(FormDescriptor form, StringBuilder result) {
        HashMap<Set<PropertyObjectInterfaceDescriptor>, Collection<PropertyDrawDescriptor>> propertiesInt =
                new HashMap<Set<PropertyObjectInterfaceDescriptor>, Collection<PropertyDrawDescriptor>>();

        for (PropertyDrawDescriptor property : form.propertyDraws) {
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
                propArray.append(prop.getPropertyObject().property.getSID() + ",");
            }
            propArray.replace(propArray.length() - 1, propArray.length(), "}");

            StringBuilder entityArray = new StringBuilder();
            entityArray.append(",");
            for (PropertyObjectInterfaceDescriptor objectDescriptorInt : set) {
                if (objectDescriptorInt instanceof ObjectDescriptor) {
                    ObjectDescriptor object = (ObjectDescriptor) objectDescriptorInt;
                    entityArray.append(objectNames.get(object) + ",");
                }
            }
            entityArray.replace(entityArray.length() - 1, entityArray.length(), ")");

            result.append(intend + "addPropertyDraw(" + propArray.toString() + entityArray.toString() + ";\n");
        }
    }

    public static String addFixedFilters(Set<FilterDescriptor> filters, StringBuilder result) {
        for (FilterDescriptor filter : filters) {
            result.append(intend + "addFixedFilter(");
            result.append(filter.getCodeConstructor(objectNames));
            result.append(");\n");
        }
        return result.toString();
    }

    public static String addRegularFilterGroups(List<RegularFilterGroupDescriptor> groups, StringBuilder result) {
        for (RegularFilterGroupDescriptor filterGroup : groups) {
            String groupName = "filterGroup" + getID();
            result.append(intend + filterGroup.getCodeConstructor(groupName));
            for (RegularFilterDescriptor filter : filterGroup.filters) {
                result.append("\n" + intend + groupName + ".addFilter(" + filter.getCodeConstructor(objectNames) + ");");
            }
            result.append("\n" + intend + "addRegularFilterGroup(" + groupName + ");");
            result.append("\n");
        }
        return result.toString();
    }

    private static String addContainers(ClientComponent component, String name) {
        StringBuilder temp = new StringBuilder();
        if (component instanceof ClientContainer) {
            for (ClientComponent child : ((ClientContainer) component).children) {
                String childName = "component" + child.getID();
                temp.append(intend + child.getCodeConstructor(childName) + ";\n");
                temp.append(changeConstraints(component, childName));
                temp.append(addContainers(child, childName));
                temp.append(intend + name + ".add(" + childName + ");\n");
            }
        }
        return temp.toString();
    }

    private static String changeConstraints(ClientComponent component, String name) {
        StringBuilder toReturn = new StringBuilder();
        SimplexConstraints constraints = component.getDefaultConstraints();

        if (!component.constraints.childConstraints.equals(constraints.childConstraints)) {
            toReturn.append(intend + name + ".constraints.childConstraints = " + getConstraintCode(component.constraints.childConstraints) + ";\n");
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
            toReturn.append(intend + name + ".constraints.maxVariables = " + constraints.maxVariables + ");\n");
        }

        Map<ClientComponent, DoNotIntersectSimplexConstraint> map = component.getIntersects();
        if (!map.isEmpty()) {
            for (ClientComponent single : map.keySet()) {
                toReturn.append(intend + "design.addIntersection(" +
                        component.getSID() + ", " + single.getSID() + ", " + getConstraintCode(map.get(single)) + ");\n");
            }
        }
        
        return toReturn.toString();
    }

    public static String getConstraintCode(DoNotIntersectSimplexConstraint constraint) {
        if(constraint.equals(DoNotIntersectSimplexConstraint.DO_NOT_INTERSECT)) {
            return "DoNotIntersectSimplexConstraint.DO_NOT_INTERSECT";
        } else if (constraint.equals(DoNotIntersectSimplexConstraint.TOTHE_BOTTOM)) {
            return "DoNotIntersectSimplexConstraint.TOTHE_BOTTOM";
        } else if (constraint.equals(DoNotIntersectSimplexConstraint.TOTHE_RIGHT)) {
            return "DoNotIntersectSimplexConstraint.TOTHE_RIGHT";
        } else if (constraint.equals(DoNotIntersectSimplexConstraint.TOTHE_RIGHTBOTTOM)) {
            return "DoNotIntersectSimplexConstraint.TOTHE_RIGHTBOTTOM";
        } else {
            String result = "";
            result += (constraint.forbDir & DoNotIntersectSimplexConstraint.TOP) != 0 ? "DoNotIntersectSimplexConstraint.TOP | " : "";
            result += (constraint.forbDir & DoNotIntersectSimplexConstraint.LEFT) != 0 ? "DoNotIntersectSimplexConstraint.LEFT | " : "";
            result += (constraint.forbDir & DoNotIntersectSimplexConstraint.BOTTOM) != 0 ? "DoNotIntersectSimplexConstraint.BOTTOM | " : "";
            result += (constraint.forbDir & DoNotIntersectSimplexConstraint.RIGHT) != 0 ? "DoNotIntersectSimplexConstraint.RIGHT | " : "";
            return result.substring(0, result.length() - 3);
        }
    }

    private static void addRichDesign(FormDescriptor form, StringBuilder result) {
        result.append(intend + "@Override\n");
        result.append(intend + "public FormView createDefaultRichDesign() {\n");
        intend += "   ";
        result.append(intend + "CustomFormView design = new CustomFormView(this);\n");
        result.append(intend + "design.setMainContainer(design.createMainContainer(\"" + form.client.mainContainer.getSID() +
                "\", \"" + form.client.mainContainer.getCaption() + "\"));\n");
        result.append(addContainers(form.client.mainContainer, "design.mainContainer"));

        for (GroupObjectDescriptor groupObject : form.groupObjects) {
            String name = "groupView" + groupObject.getID();
            result.append(intend + "GroupObjectView " + name + " = design.createGroupObject(grObj" + groupObject.getSID() + ", component" +
                    groupObject.client.showType.getID() + ", component" + groupObject.client.grid.getID() + ");\n");
            result.append(intend + "design.groupObjects.add(" + name + ");\n");

            for (ObjectDescriptor object : groupObject.objects) {
                result.append(intend + name + ".getObjectView(" + objectNames.get(object) + ").changeClassChooserLocation(" +
                        "component" + object.client.classChooser.getID() + ");\n");

            }
        }

        result.append(intend + "return design;\n");

    }

    public static String formDescriptorCode(FormDescriptor form) {
        intend = "";
        StringBuilder result = new StringBuilder();

        addClassDeclaration(form, result);
        intend += "\t";
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

        result.append("\t}\n");
        intend = "\t";

        result.append("\n");
        addRichDesign(form, result);
        result.append("\t}\n");

        result.append("}\n");

        return result.toString();
    }

    public static Component getComponent(FormDescriptor form) {
        Component comp = new JTextArea(formDescriptorCode(form));
        Font font = new Font("Verdana", Font.CENTER_BASELINE, 10);
        comp.setFont(font);
        return new JScrollPane(comp);
    }

}
