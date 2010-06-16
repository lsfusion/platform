package platform.server.view.form;

import platform.server.logics.property.PropertyInterface;

// представление св-ва
public class PropertyView<P extends PropertyInterface> extends CellView {

    public PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public boolean forcePanel;

    public String toString() {
        return view.toString();
    }

    public PropertyView(int ID, String sID, PropertyObjectImplement<P> view, GroupObjectImplement toDraw, boolean forcePanel) {
        super(ID,sID);
        this.view = view;
        this.toDraw = toDraw;
        this.forcePanel = forcePanel;
    }
}
