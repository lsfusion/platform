package platform.client.form.queries;

import platform.interop.KeyStrokes;

import javax.swing.*;

public class FindView extends QueryView {

    FindView(QueryController controller) {
        super(controller);
    }

    protected Icon getApplyIcon() {
        return new ImageIcon(getClass().getResource("/images/find.gif"));
    }

    protected Icon getAddConditionIcon() {
        return new ImageIcon(getClass().getResource("/images/findadd.gif"));
    }

    protected KeyStroke getKeyStroke(int modifier) {
        return KeyStrokes.getFindKeyStroke(modifier);
    }
}
