package platform.client.form.queries;

import platform.client.FlatRolloverButton;

import javax.swing.*;
import java.awt.*;

/**
 * User: DAle
 * Date: 31.03.11
 * Time: 19:37
 */

public abstract class ToolbarGridButton extends FlatRolloverButton {
    public final static Dimension BUTTON_SIZE = new Dimension(20, 20);

    public ToolbarGridButton(String iconPath, String toolTipText) {
        super();
        setIcon(new ImageIcon(getClass().getResource(iconPath)));
        setAlignmentY(Component.TOP_ALIGNMENT);
        setMinimumSize(BUTTON_SIZE);
        setPreferredSize(BUTTON_SIZE);
        setMaximumSize(BUTTON_SIZE);
        setFocusable(false);
        setToolTipText(toolTipText);
        addListener();
    }

    public abstract void addListener();
}
