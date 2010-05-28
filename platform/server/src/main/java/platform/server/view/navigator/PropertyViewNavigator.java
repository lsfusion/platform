package platform.server.view.navigator;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;
import platform.server.view.form.PropertyObjectImplement;

public class PropertyViewNavigator<P extends PropertyInterface> extends CellViewNavigator {

    public PropertyObjectNavigator<P> view;

    public GroupObjectNavigator toDraw;

    public PropertyViewNavigator<P> setToDraw(GroupObjectNavigator toDraw) {
        this.toDraw = toDraw;
        return this;
    }

    public boolean forcePanel = false;
    public void setForcePanel(boolean forcePanel) {
        this.forcePanel = forcePanel;
    }

    @Override
    public String toString() {
        return view.toString();
    }

    public PropertyViewNavigator(int ID, PropertyObjectNavigator<P> view, GroupObjectNavigator toDraw) {
        super(ID);
        this.view = view;
        this.toDraw = toDraw;
    }

}
