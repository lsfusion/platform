package platform.server.view.navigator;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.ControlView;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.PropertyView;

public class PropertyViewNavigator<P extends PropertyInterface> extends ControlViewNavigator<P, Property<P>, PropertyObjectImplement<P>, PropertyObjectNavigator<P>> {

    public PropertyViewNavigator(int ID, PropertyObjectNavigator<P> view, GroupObjectNavigator toDraw) {
        super(ID,view,toDraw);
    }

    public PropertyView<P> createView(int ID, String sID, PropertyObjectImplement<P> implement, GroupObjectImplement toDraw) {
        return new PropertyView<P>(ID, sID, implement, toDraw);
    }
}
