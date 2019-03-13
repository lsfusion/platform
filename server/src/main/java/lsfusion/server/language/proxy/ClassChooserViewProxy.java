package lsfusion.server.language.proxy;

import lsfusion.server.form.view.ClassChooserView;

public class ClassChooserViewProxy extends ComponentViewProxy<ClassChooserView> {
    public ClassChooserViewProxy(ClassChooserView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }
}
