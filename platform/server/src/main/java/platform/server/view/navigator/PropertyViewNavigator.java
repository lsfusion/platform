package platform.server.view.navigator;

import platform.server.logics.property.PropertyInterface;

public class PropertyViewNavigator<P extends PropertyInterface> extends CellViewNavigator {
    
    public PropertyObjectNavigator<P> view;

    public GroupObjectNavigator toDraw;

    public PropertyViewNavigator(int iID, PropertyObjectNavigator<P> iView, GroupObjectNavigator iToDraw) {
        super(iID);
        view = iView;
        toDraw = iToDraw;
    }

    @Override
    public String toString() {
        return view.toString();
    }
}
