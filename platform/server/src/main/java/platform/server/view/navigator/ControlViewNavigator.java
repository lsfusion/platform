package platform.server.view.navigator;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.control.ControlInterface;
import platform.server.logics.control.Control;
import platform.server.view.form.ControlView;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ControlObjectImplement;

public abstract class ControlViewNavigator<T extends ControlInterface, C extends Control<T>, I extends ControlObjectImplement<T,C>, O extends ControlObjectNavigator<T,C,I>> extends CellViewNavigator {

    public O view;

    public GroupObjectNavigator toDraw;

    public ControlViewNavigator(int ID, O view, GroupObjectNavigator toDraw) {
        super(ID);
        this.view = view;
        this.toDraw = toDraw;
    }

    @Override
    public String toString() {
        return view.toString();
    }

    public abstract ControlView createView(int ID, String sID, I implement, GroupObjectImplement toDraw);
}

