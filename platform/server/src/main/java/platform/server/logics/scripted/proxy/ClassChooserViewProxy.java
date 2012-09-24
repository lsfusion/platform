package platform.server.logics.scripted.proxy;

import platform.server.form.view.ClassChooserView;

public class ClassChooserViewProxy extends ComponentViewProxy<ClassChooserView> {
    public ClassChooserViewProxy(ClassChooserView target) {
        super(target);
    }

    public void setVisible(boolean visible) {
        target.visible = visible;
    }
}
