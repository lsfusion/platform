package platform.client.form.panel;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientRegularFilterGroup;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class PanelToolbar {
    private Set<PropertyController> properties = new HashSet<PropertyController>();
    private Map<ClientRegularFilterGroup, JComponent> filters = new HashMap<ClientRegularFilterGroup, JComponent>();

    private final ClientFormController form;

    private ClientFormLayout formLayout;
    private Set<Component> components = new HashSet<Component>();

    private JPanel leftContainer;
    private JPanel rightContainer;
    private JPanel mainContainer;

    public PanelToolbar(ClientFormController form, ClientFormLayout formLayout) {
        this.form = form;
        this.formLayout = formLayout;

        initBottomContainer();
    }

    private void initBottomContainer() {
        leftContainer = new JPanel();
        leftContainer.setLayout(new BoxLayout(leftContainer, BoxLayout.X_AXIS));
        leftContainer.setAlignmentY(Component.TOP_ALIGNMENT);

        rightContainer = new JPanel();
        rightContainer.setLayout(new BoxLayout(rightContainer, BoxLayout.X_AXIS));
        rightContainer.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.X_AXIS));
        centerContainer.add(Box.createHorizontalGlue());
        centerContainer.add(rightContainer);

        mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(leftContainer, BorderLayout.WEST);
        mainContainer.add(centerContainer, BorderLayout.EAST);
    }

    public JPanel getView() {
        return mainContainer;
    }

    public void addComponent(JComponent component) {
        addComponent(component, false);
    }

    public void addComponent(JComponent component, boolean toTheRight) {
        if (toTheRight) {
            rightContainer.add(component);
        } else {
            leftContainer.add(component);
        }
    }

    public void removeComponent(JComponent component) {
        leftContainer.remove(component);
        rightContainer.remove(component);
    }

    public void addProperty(PropertyController property) {
        properties.add(property);
        components.add(property.getView());
    }

    public void removeProperty(PropertyController property) {
        properties.remove(property);
        components.remove(property.getView());
    }

    public void addFilter(ClientRegularFilterGroup filterGroup, JComponent component) {
        filters.put(filterGroup, component);
        components.add(component);
    }

    public Set<PropertyController> getProperties() {
        return properties;
    }

    public Set<Map.Entry<ClientRegularFilterGroup, JComponent>> getFilters() {
        return filters.entrySet();
    }

    public void update(ClassViewType viewType) {
        for (Component c : rightContainer.getComponents()) {
            if (!components.contains(c)) {
                rightContainer.remove(c);
            }
        }

        Set<Component> draw = new HashSet<Component>(Arrays.asList(rightContainer.getComponents()));
        for (Map.Entry<ClientRegularFilterGroup, JComponent> entry : getFilters()) {
            formLayout.remove(entry.getKey(), entry.getValue());
            if (!draw.contains(entry.getValue())) {
                rightContainer.add(entry.getValue());
            }
        }

        for (PropertyController control : getProperties()) {
            control.removeView(formLayout);
            if (!draw.contains(control.getView())) {
                int index = form.getPropertyDraws().indexOf(control.getKey());
                int curIndex = 0;
                PropertyController prevProp = null;

                for (PropertyController curProp : getProperties()) {
                    int temp = form.getPropertyDraws().indexOf(curProp.getKey());
                    if ((temp < index) && (temp > curIndex)) {
                        curIndex = temp;
                        prevProp = curProp;
                    }
                }
                if (prevProp == null) {
                    rightContainer.add(control.getView());
                } else {
                    rightContainer.add(control.getView(), Arrays.asList(rightContainer.getComponents()).indexOf(prevProp.getView()) + 1);
                }
            }
            control.getCellView().changeViewType(viewType);
        }
    }
}
