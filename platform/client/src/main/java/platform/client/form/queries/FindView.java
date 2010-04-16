package platform.client.form.queries;

import javax.swing.*;
import java.awt.event.KeyEvent;

class FindView extends QueryView {
    
    protected Icon getApplyIcon() {
        return new ImageIcon(getClass().getResource("/platform/client/form/images/find.gif"));
    }

    protected Icon getAddConditionIcon() {
        return new ImageIcon(getClass().getResource("/platform/client/form/images/findadd.gif"));
    }

    protected int getKeyEvent() {
        return KeyEvent.VK_F3;
    }
}
