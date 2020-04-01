package lsfusion.client.form.object.table.grid.user.toolbar.view;

import lsfusion.client.base.view.ThemedFlatRolloverButton;

import java.awt.*;

public class ToolbarGridButton extends ThemedFlatRolloverButton {
    public final static Dimension DEFAULT_SIZE = new Dimension(20, 20);

    public ToolbarGridButton(String iconPath, String toolTipText) {
        this(iconPath, toolTipText, DEFAULT_SIZE);
    }
    public ToolbarGridButton(String iconPath, String toolTipText, Dimension buttonSize) {
        super(iconPath);
        setAlignmentY(Component.TOP_ALIGNMENT);
        setMinimumSize(buttonSize);
        setPreferredSize(buttonSize);
        setMaximumSize(buttonSize);
        setFocusable(false);
        if (toolTipText != null) {
            setToolTipText(toolTipText);
        }
        addListener();
    }

    public void addListener() {

    }
}
