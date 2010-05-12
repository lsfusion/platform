package platform.server.view.form;

import platform.server.logics.control.ControlInterface;
import platform.server.logics.control.Control;

public class ControlView<T extends ControlInterface, C extends Control<T>, O extends ControlObjectImplement<T,C>> extends CellView {

    public O view;

    // в какой "класс" рисоваться, ессно одмн из Object.GroupTo должен быть ToDraw
    public GroupObjectImplement toDraw;

    public ControlView(int ID, String sID, O view, GroupObjectImplement toDraw) {
        super(ID,sID);
        this.view = view;
        this.toDraw = toDraw;
    }

    public String toString() {
        return view.toString();
    }
}
