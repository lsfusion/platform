package lsfusion.client.form.panel;

import org.jdesktop.swingx.VerticalLayout;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.cell.ActionPanelView;
import lsfusion.client.form.cell.PropertyController;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientActionClass;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

@Deprecated
public class PanelShortcut extends JPopupMenu {
    private ClientFormController form;
    private PanelController panel;

    private ClientPropertyDraw currentProperty;
    private JPanel currentPropertyContainer;

    private Set<PropertyController> properties = new LinkedHashSet<PropertyController>();
    private JPanel propertiesContainer = new JPanel(new VerticalLayout(3));

    public PanelShortcut(ClientFormController form, PanelController panel) {
        super();
        this.form = form;
        this.panel = panel;

        setBackground(PropertyRenderer.SELECTED_ROW_BACKGROUND);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, PropertyRenderer.SELECTED_ROW_BORDER_COLOR,
                getBackground(), Color.GRAY, PropertyRenderer.SELECTED_ROW_BORDER_COLOR));
    }

    public void setCurrentProperty(ClientPropertyDraw property) {
        currentProperty = property;
    }

    public void addProperty(PropertyController property) {
        properties.add(property);
    }

    public void removeProperty(PropertyController property) {
        properties.remove(property);
    }

    public void addPropertiesComponent(JComponent component) {
        component.setAlignmentX(RIGHT_ALIGNMENT);
        propertiesContainer.add(component);
    }

    public void addCurrentPropertyComponent(JComponent component) {
        component.setAlignmentX(RIGHT_ALIGNMENT);
        currentPropertyContainer.add(component);
    }

    private void addCurrentPropertyToMenu() {
        currentPropertyContainer = new JPanel(new VerticalLayout(3));
        //если нужно, здесь через addCurrentPropertyComponent можно добавить всякое, кроме пунктов меню, для currentProperty
//        addCurrentPropertyComponent(new JLabel("Caption: " + currentProperty.caption));

        if (currentPropertyContainer.getComponentCount() != 0) {
            currentPropertyContainer.setOpaque(false);
            add(currentPropertyContainer);
        }

        //с помощью addActionMenuItem добавляем пункты меню с экшнами
//        addActionMenuItem(ClientResourceBundle.getString("form.grid.hide.column"), null, null, new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                currentProperty.hide = true;
//                form.controllers.get(currentProperty.groupObject).grid.update();
//            }
//        });
    }

    private void addActionMenuItem(String caption, Icon icon, String tooltip, ActionListener listener) {
        this.addActionMenuItem(caption, icon, tooltip, listener, false);
    }

    private void addActionMenuItem(String caption, Icon icon, String tooltip, ActionListener listener, boolean defaultItem) {
        JMenuItem item = new JMenuItem(caption, icon);
        item.setOpaque(false);
        item.addActionListener(listener);
        item.setToolTipText(tooltip == null ? caption : tooltip);
        if (defaultItem) {
            item.setFont(item.getFont().deriveFont(Font.BOLD));
        }
        add(item);
    }

    private TreeMap<Integer, ClientPropertyDraw> sortProperties() {
        TreeMap<Integer, ClientPropertyDraw> sortedMap = new TreeMap<Integer, ClientPropertyDraw>(); //расставляем свойства в том же порядке, что и в форме
        for (PropertyController property : properties) {
//            if (property.getKey().drawToShortcut()) {
//                ClientPropertyDraw onlyProperty = ((ClientShortcutPanelLocation) property.getKey().panelLocation).getOnlyProperty();
//                if (onlyProperty == null || onlyProperty.equals(currentProperty)) {
//                    sortedMap.put(form.getPropertyDraws().indexOf(property.getKey()), property.getKey());
//                }
//            }
        }
        return sortedMap;
    }

    private void addPropertiesToMenu() {
        propertiesContainer = new JPanel(new VerticalLayout(3));
        List<PropertyController> actionProperties = new ArrayList<PropertyController>();
        for (ClientPropertyDraw property : sortProperties().values()) {
            for (final PropertyController propertyController : panel.getProperties().get(property).values()) {
                if (property.baseType instanceof ClientActionClass)
                    actionProperties.add(propertyController);
                else
                    addPropertiesComponent(propertyController.getView());
            }
        }

        if ((propertiesContainer.getComponentCount() != 0 || actionProperties.size() != 0) && getComponentCount() != 0)
            addSeparator();

        //добавляем контейнер с простыми свойствами
        if (propertiesContainer.getComponentCount() != 0) {
            propertiesContainer.setOpaque(false);
            add(propertiesContainer);
        }

        //затем - ActionProperties в виде пунктов меню
        boolean defaultSet = false;
        for (PropertyController controller : actionProperties) {
            final ActionPanelView button = (ActionPanelView) controller.getView();

            boolean isDefault = false;
//            if (!defaultSet)
//                isDefault = ((ClientShortcutPanelLocation) controller.getKey().panelLocation).isDefault();
            defaultSet = defaultSet || isDefault;

            addActionMenuItem(button.getText(), button.getIcon(), button.getToolTipText(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button.doClick();
                }
            }, isDefault);
        }
    }

    public void show(Component invoker, Point point) {
        removeAll();

        addCurrentPropertyToMenu(); //сперва добавляем то, что касается текущей колонки
        addPropertiesToMenu(); //затем - свойства, вынесенные в shortcut

        if (getComponentCount() != 0) {
            show(invoker, point.x, point.y);
        }
    }
}
