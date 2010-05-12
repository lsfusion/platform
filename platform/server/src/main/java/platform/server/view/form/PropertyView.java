package platform.server.view.form;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;

// представление св-ва
public class PropertyView<P extends PropertyInterface> extends ControlView<P, Property<P>, PropertyObjectImplement<P>> {

    public PropertyView(int ID, String sID, PropertyObjectImplement<P> view, GroupObjectImplement toDraw) {
        super(ID,sID, view, toDraw);
        this.view = view;
        this.toDraw = toDraw;
    }
}
