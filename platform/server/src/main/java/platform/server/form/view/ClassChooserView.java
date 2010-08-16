package platform.server.form.view;

import platform.server.classes.CustomClass;
import platform.server.form.entity.ObjectEntity;

public class ClassChooserView extends ComponentView  {
    public ClassChooserView(int ID, ObjectEntity view) {
        super(ID);
        this.show = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).children.isEmpty();
    }
}
