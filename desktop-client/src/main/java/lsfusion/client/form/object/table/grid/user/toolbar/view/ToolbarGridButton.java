package lsfusion.client.form.object.table.grid.user.toolbar.view;

import lsfusion.client.base.view.ThemedFlatRolloverButton;

import java.awt.*;

import static lsfusion.client.base.view.SwingDefaults.getComponentHeight;

public class ToolbarGridButton extends ThemedFlatRolloverButton {
    public ToolbarGridButton(String iconPath, String toolTipText) {
        this(null, iconPath, toolTipText, new Dimension(getComponentHeight(), getComponentHeight()));
    }

    public ToolbarGridButton(String caption, String iconPath, String toolTipText, Dimension buttonSize) {
        super(iconPath, caption);
        setAlignmentY(Component.TOP_ALIGNMENT);
        if (buttonSize != null) {
            setMinimumSize(buttonSize);
            setPreferredSize(buttonSize);
            setMaximumSize(buttonSize);
        }
        setFocusable(false);
        if (toolTipText != null) {
            setToolTipText(toolTipText);
        }
        addListener();
    }

    public void addListener() {

    }
}
