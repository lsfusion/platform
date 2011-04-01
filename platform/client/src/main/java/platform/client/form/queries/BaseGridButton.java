package platform.client.form.queries;

import javax.swing.*;
import java.awt.*;

/**
 * User: DAle
 * Date: 31.03.11
 * Time: 19:37
 */

public abstract class BaseGridButton extends JButton {
    private final Dimension buttonSize = new Dimension(20, 20);

    public BaseGridButton(String iconPath, String toolTipText) {
        super();
        setIcon(new ImageIcon(getClass().getResource(iconPath)));
        setAlignmentY(Component.TOP_ALIGNMENT);
        setMinimumSize(buttonSize);
        setPreferredSize(buttonSize);
        setMaximumSize(buttonSize);
        setFocusable(false);
        setToolTipText(toolTipText);
        addListener();
    }

    public abstract void addListener();
}
