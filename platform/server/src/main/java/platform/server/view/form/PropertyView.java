package platform.server.view.form;

import platform.server.logics.property.PropertyInterface;

// представление св-ва
public class PropertyView<P extends PropertyInterface> extends CellView {
    public PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public PropertyView(int ID, String sID, PropertyObjectImplement<P> view, GroupObjectImplement toDraw) {
        super(ID,sID);
        this.view = view;
        this.toDraw = toDraw;
    }

    public PropertyView(int ID, String sID, PropertyObjectImplement<P> view) {
        this(ID,sID,view,view.getApplyObject());
    }

    public String toString() {
        return view.toString();
    }
}
