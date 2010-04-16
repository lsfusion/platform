package platform.client.form.queries;

import javax.swing.*;
import java.awt.event.KeyEvent;

class FilterView extends QueryView {

    protected Icon getApplyIcon() {
        return new ImageIcon(getClass().getResource("/platform/client/form/images/filt.gif"));
    }

    protected Icon getAddConditionIcon() {
        return new ImageIcon(getClass().getResource("/platform/client/form/images/filtadd.gif"));
    }

    protected int getKeyEvent() {
        return KeyEvent.VK_F2;
    }
}
