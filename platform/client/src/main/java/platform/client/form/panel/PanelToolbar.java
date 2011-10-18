package platform.client.form.panel;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.cell.PropertyController;
import platform.client.form.queries.ToolbarGridButton;
import platform.client.logics.ClientRegularFilterGroup;
import platform.interop.ClassViewType;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class PanelToolbar {
    private Set<PropertyController> properties = new HashSet<PropertyController>();
    private Map<ClientRegularFilterGroup, JComponent> filters = new LinkedHashMap<ClientRegularFilterGroup, JComponent>();

    private final ClientFormController form;

    private ClientFormLayout formLayout;
    private Set<Component> components = new HashSet<Component>();

    private JPanel leftContainer;
    private JPanel rightContainer;
    private JPanel mainContainer;
    private JPanel bottomContainer;

    private JLabel selectionInfoLabel;

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

        bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.X_AXIS));
        bottomContainer.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel eastContainer = new JPanel();
        eastContainer.setLayout(new BoxLayout(eastContainer, BoxLayout.X_AXIS));
        eastContainer.add(Box.createHorizontalGlue());
        eastContainer.add(rightContainer);

        selectionInfoLabel = new JLabel();
        JPanel centerContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        centerContainer.setMinimumSize(new Dimension(1, ToolbarGridButton.BUTTON_SIZE.height));
        centerContainer.setPreferredSize(new Dimension(centerContainer.getPreferredSize().width, ToolbarGridButton.BUTTON_SIZE.height));
        centerContainer.add(selectionInfoLabel);

        mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(leftContainer, BorderLayout.WEST);
        mainContainer.add(centerContainer, BorderLayout.CENTER);
        mainContainer.add(eastContainer, BorderLayout.EAST);
        mainContainer.add(bottomContainer, BorderLayout.SOUTH);
    }

    public JPanel getView() {
        return mainContainer;
    }

    public void addComponent(Component component) {
        addComponent(component, false);
    }

    public void addComponent(Component component, boolean toTheRight) {
        if (toTheRight) {
            rightContainer.add(component);
        } else {
            leftContainer.add(component);
        }
    }

    public void moveComponent(Component component, int destination) {
        if (destination == SwingConstants.LEFT) {
            bottomContainer.remove(component);
            leftContainer.add(component, 0);
        } else if (destination == SwingConstants.BOTTOM) {
            leftContainer.remove(component);
            bottomContainer.add(component);
        }
    }

    public void removeComponent(Component component) {
        leftContainer.remove(component);
        rightContainer.remove(component);
        bottomContainer.remove(component);
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

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        String text = "";
        text += avg == null ? "" : ClientResourceBundle.getString("form.grid.selection.average") + avg.replace(',', '.') + "  ";
        text += sum == null ? "" : ClientResourceBundle.getString("form.grid.selection.sum") + sum.replace(',', '.') + "  ";
        text += quantity <= 1 ? "" : ClientResourceBundle.getString("form.grid.selection.quantity") + quantity;
        selectionInfoLabel.setText(text);
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
