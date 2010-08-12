package platform.server.view.form.client;

import platform.server.classes.CustomClass;
import platform.server.view.navigator.ObjectNavigator;

public class ClassView extends ComponentView  {
    public ClassView(int ID, ObjectNavigator view) {
        super(ID);
        this.show = view.baseClass instanceof CustomClass && !((CustomClass)view.baseClass).children.isEmpty();
    }
}
