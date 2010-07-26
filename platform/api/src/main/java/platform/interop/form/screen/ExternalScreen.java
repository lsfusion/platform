package platform.interop.form.screen;

import platform.interop.form.layout.SimplexConstraints;

import java.io.Serializable;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;

public abstract interface ExternalScreen extends Serializable {

    public int getID();
    public abstract void initialize();
    public abstract void repaint(Map<ExternalScreenComponent, ExternalScreenConstraints> components);
}
