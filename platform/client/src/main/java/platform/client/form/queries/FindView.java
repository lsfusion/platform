package platform.client.form.queries;

import platform.interop.KeyStrokes;

import javax.swing.*;

public class FindView extends QueryView {
    
    protected Icon getApplyIcon() {
        return new ImageIcon(getClass().getResource("/platform/client/form/images/find.gif"));
    }

    protected Icon getAddConditionIcon() {
        return new ImageIcon(getClass().getResource("/platform/client/form/images/findadd.gif"));
    }

    protected KeyStroke getKeyStroke(int modifier) {
        return KeyStrokes.getFindKeyStroke(modifier);
    }
}
