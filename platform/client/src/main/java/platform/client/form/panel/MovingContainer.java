package platform.client.form.panel;

import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectController;
import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientRegularFilterGroup;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class MovingContainer {
    private Set<PropertyController> movingProps = new HashSet<PropertyController>();
    private Map<ClientRegularFilterGroup, JComponent> movingFilters = new HashMap<ClientRegularFilterGroup, JComponent>();
    GroupObjectController groupController;
    ClientFormLayout formLayout;
    Set<Component> components = new HashSet<Component>();


    public MovingContainer(GroupObjectController groupObject, ClientFormLayout formLayout) {
        this.groupController = groupObject;
        this.formLayout = formLayout;
    }

    public void addProperty(PropertyController property) {
        movingProps.add(property);
        components.add(property.getView());
    }

    public void removeProperty(PropertyController property) {
        movingProps.remove(property);
        components.remove(property.getView());
    }

    public void addFilter(ClientRegularFilterGroup filterGroup, JComponent component) {
        movingFilters.put(filterGroup, component);
        components.add(component);
    }

    public Set<PropertyController> getProps() {
        return movingProps;
    }

    public Set<Map.Entry<ClientRegularFilterGroup, JComponent>> getMovingFilters() {
        return movingFilters.entrySet();
    }

    public void update(ClassViewType viewType) {
        JPanel panel = groupController.grid.getView().movingPropertiesContainer;
        if (panel.getComponents().length == 0) {
            Component glue = Box.createHorizontalGlue();
            panel.add(glue);
            components.add(glue);
        }
        if (viewType.equals(ClassViewType.GRID)) {
            panel.remove(groupController.showType.view);
            for (Component c : panel.getComponents()) {
                if (!components.contains(c))
                    panel.remove(c);
            }

            Set<Component> draw = new HashSet<Component>(Arrays.asList(panel.getComponents()));
            for (Map.Entry<ClientRegularFilterGroup, JComponent> entry : getMovingFilters()) {
                formLayout.remove(entry.getKey(), entry.getValue());
                if (!draw.contains(entry.getValue())) {
                    panel.add(entry.getValue());
                }
            }

            for (PropertyController control : getProps()) {
                control.removeView(formLayout);
                if (!draw.contains(control.getView())) {
                    int index = groupController.getPropertyDraws().indexOf(control.getKey());
                    int curIndex = 0;
                    PropertyController prevProp = null;

                    for (PropertyController curProp : getProps()) {
                        int temp = groupController.getPropertyDraws().indexOf(curProp.getKey());
                        if ((temp < index) && (temp > curIndex)) {
                            curIndex = temp;
                            prevProp = curProp;
                        }
                    }
                    if (prevProp == null) {
                        panel.add(control.getView());
                    } else {
                        panel.add(control.getView(), Arrays.asList(panel.getComponents()).indexOf(prevProp.getView()) + 1);
                    }
                }
                control.getCellView().changeViewType(viewType);
            }
            groupController.showType.removeView(formLayout);
            panel.add(groupController.showType.view);
        } else {
            for (Map.Entry<ClientRegularFilterGroup, JComponent> entry : getMovingFilters()) {
                formLayout.add(entry.getKey(), entry.getValue());
            }

            for (PropertyController control : getProps()) {
                control.addView(formLayout);
                control.getCellView().changeViewType(viewType);
            }
            groupController.showType.addView(formLayout);
        }
    }
}
