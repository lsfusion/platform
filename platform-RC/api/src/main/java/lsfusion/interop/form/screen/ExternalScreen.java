package lsfusion.interop.form.screen;

import java.io.Serializable;
import java.util.Map;

public interface ExternalScreen extends Serializable {
    int getID();
    void initialize(ExternalScreenParameters parameters);
    void repaint(Map<ExternalScreenComponent, ExternalScreenConstraints> components);
}
