package platform.client.form.renderer;

import platform.client.form.PropertyRendererComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientColorClass;

import javax.swing.*;
import java.awt.*;

public class ColorPropertyRenderer extends LabelPropertyRenderer implements PropertyRendererComponent {
    Color value;

    public ColorPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        this.value = value == null ? ClientColorClass.getDefaultValue() : (Color) value;
//        if (value == null)
//            setBackground(new Color(0, 0, 0, 0));
//        setOpaque(false);
//        else {
//            setBackground((Color) value);
//        }
        setSelected(isSelected, hasFocus);
    }

    @Override
    public void drawBackground(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBackground(new Color(value.getRGB() & PropertyRendererComponent.FOCUSED_CELL_BACKGROUND.getRGB()));
            } else {
                setBackground(new Color(value.getRGB() & PropertyRendererComponent.SELECTED_ROW_BACKGROUND.getRGB()));
            }
        } else {
            setBackground(value);
        }
    }

    @Override
    public void rateSelected() {
        paintSelected();
    }

    @Override
    public void paintSelected() {
        setBackground(new Color(value.getRGB() & PropertyRendererComponent.SELECTED_CELL_BACKGROUND.getRGB()));
    }
}
