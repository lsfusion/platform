package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;

// представление св-ва
public class PropertyView<P extends PropertyInterface> extends CellView {
    public PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public PropertyView(int iID, String iSID, PropertyObjectImplement<P> iView, GroupObjectImplement iToDraw) {
        super(iID,iSID);
        view = iView;
        toDraw = iToDraw;
    }

    public String toString() {
        return view.toString();
    }
}
