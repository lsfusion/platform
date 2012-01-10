package platform.client.form.panel;

import org.jdesktop.swingx.VerticalLayout;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.ButtonCellView;
import platform.client.form.cell.PropertyController;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientActionClass;
import platform.interop.ShortcutPanelLocation;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

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

        setBackground(PropertyRendererComponent.SELECTED_ROW_BACKGROUND);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, PropertyRendererComponent.SELECTED_ROW_BORDER_COLOR,
                getBackground(), Color.GRAY, PropertyRendererComponent.SELECTED_ROW_BORDER_COLOR));
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
        JMenuItem item = new JMenuItem(caption, icon);
        item.setOpaque(false);
        item.addActionListener(listener);
        item.setToolTipText(tooltip == null ? caption : tooltip);
        add(item);
    }

    private void addPropertiesToMenu() {
        propertiesContainer = new JPanel(new VerticalLayout(3));
        List<PropertyController> actionProperties = new ArrayList<PropertyController>();
        TreeMap<Integer, ClientPropertyDraw> sortedMap = new TreeMap<Integer, ClientPropertyDraw>(); //расставляем свойства в том же порядке, что и в форме
        for (PropertyController property : properties) {
            if (property.getKey().drawToShortcut()) {
                String onlyPropertySID = ((ShortcutPanelLocation) property.getKey().panelLocation).getOnlyPropertySID();
                if (onlyPropertySID == null || onlyPropertySID.equals(currentProperty.getSID())) {
                    sortedMap.put(form.getPropertyDraws().indexOf(property.getKey()), property.getKey());
                }
            }
        }
        for (ClientPropertyDraw property : sortedMap.values()) {
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
        for (PropertyController controller : actionProperties) {
            final ButtonCellView button = (ButtonCellView) controller.getView();
            addActionMenuItem(button.getText(), button.getIcon(), button.getToolTipText(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    button.doClick();
                }
            });
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
