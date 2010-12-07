package platform.client.code;

import platform.client.descriptor.*;
import platform.client.descriptor.filter.*;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;

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
                String name = "obj" + object.getSID();
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
                temp.append(addContainers(child, childName));
                temp.append(intend + name + ".add(" + childName + ");\n");
            }
        }
        return temp.toString();
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
