package lsfusion.server.logics.scripted.proxy;

import lsfusion.server.form.view.ClassChooserView;

public class ClassChooserViewProxy extends ComponentViewProxy<ClassChooserView> {
    public ClassChooserViewProxy(ClassChooserView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }
}
