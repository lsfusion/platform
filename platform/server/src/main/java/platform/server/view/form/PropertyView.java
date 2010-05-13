package platform.server.view.form;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;

// представление св-ва
public class PropertyView<P extends PropertyInterface> extends CellView {

    public PropertyObjectImplement<P> view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public String toString() {
        return view.toString();
    }

    public PropertyView(int ID, String sID, PropertyObjectImplement<P> view, GroupObjectImplement toDraw) {
        super(ID,sID);
        this.view = view;
        this.toDraw = toDraw;
    }
}
