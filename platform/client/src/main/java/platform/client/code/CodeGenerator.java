package platform.client.code;

import platform.client.descriptor.*;
import platform.client.descriptor.filter.*;
import platform.client.logics.ClientComponent;
import platform.client.logics.ClientContainer;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class CodeGenerator {

    private static String intend;
    private static int id = 1;

    private static int getID() {
        return id++;
    }

    private static void addClassDeclaration(FormDescriptor form, StringBuilder result) {
        result.append(intend + "private class " + form.getSID() + " extends FormEntity {\n");
    }

    private static void addInstanceVariable(FormDescriptor form, StringBuilder result) {
        for (GroupObjectDescriptor group : form.groupObjects) {
            for (ObjectDescriptor object : group.objects) {
                result.append(intend + "ObjectEntity obj" + object.getID() + ";\n");
            }
        }
    }

    private static void addConstructorDeclaration(FormDescriptor form, StringBuilder result) {
        result.append(intend + "public " + form.getSID() + " (NavigatorElement parent, int iID, String caption) {\n");
        intend += "   ";
        result.append(intend + "super(parent, iID, caption);\n");
    }

    private static void addGroupObject(GroupObjectDescriptor groupObject, StringBuilder result) {
        if (groupObject.objects.size() != 1) {
            String grName = "grObj" + getID();
            result.append(intend + "GroupObjectEntity " + grName + " = new GroupObjectEntity(genID());\n");

            for (ObjectDescriptor object : groupObject.objects) {
                String objName = "obj" + object.getID();
                result.append(intend + objName + " = new ObjectEntity(genID(), findValueClass(\"" +
                        object.getBaseClass().getSID() + "\"), " + object.getCaption() + ");\n");
                result.append(intend + grName + ".add(" + objName + ");\n");
            }
            result.append(intend + "addGroup(" + grName + ");\n");
        } else {
            String objName = "obj" + groupObject.objects.get(0).getID();
            result.append(intend + objName + " = addSingleGroupObject(findValueClass(\"" +
                    groupObject.objects.get(0).getBaseClass().getSID() + "\"));\n");

        }
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
                    entityArray.append("obj" + object.getID() + ",");
                }
            }
            entityArray.replace(entityArray.length() - 1, entityArray.length(), ")");

            result.append(intend + "addPropertyDraw(" + propArray.toString() + entityArray.toString() + ";\n");
        }
    }

    public static String addFixedFilters(Set<FilterDescriptor> filters, StringBuilder result) {
        for (FilterDescriptor filter : filters) {
            result.append(intend + "addFixedFilter(");
            result.append(filter.getInstanceCode());
            result.append(");\n");
        }
        return result.toString();
    }

    private static String addContainers(ClientComponent component, String name) {
        StringBuilder temp = new StringBuilder();
        if (component instanceof ClientContainer) {
            for (ClientComponent child : ((ClientContainer) component).children) {
                String childName = "component" + getID();
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
        result.append(intend + form.client.mainContainer.getCodeConstructor("mainContainer") + ";\n");
        result.append(addContainers(form.client.mainContainer, "mainContainer"));

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
