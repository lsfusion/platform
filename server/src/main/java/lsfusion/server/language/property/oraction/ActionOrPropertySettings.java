package lsfusion.server.language.property.oraction;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.property.ClassViewType;

public class ActionOrPropertySettings {
    public String groupName = null;
    public ClassViewType viewType;
    public String customRenderFunction;
    public String customEditorFunction;
    public Boolean flex;
    public Integer charWidth;
    public InputBindingEvent changeKey;
    public Boolean showChangeKey;
    public InputBindingEvent changeMouse;
    public Boolean showChangeMouse;
    public Boolean sticky;
    public Boolean sync;
    public String image;
    public ImSet<String> annotations = SetFact.EMPTY();

    public void addAnnotation(String annotation) {
        annotations = annotations.merge(annotation);
    }
}
