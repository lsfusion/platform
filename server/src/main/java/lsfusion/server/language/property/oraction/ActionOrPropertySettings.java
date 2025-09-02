package lsfusion.server.language.property.oraction;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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
    public String extId;
    public String keyPressKey;
    public ScriptingLogicsModule.LAWithParams keyPressAction;
    public LocalizedString contextMenuEventCaption;
    public ScriptingLogicsModule.LAWithParams contextMenuEventAction;
    public String editEventActionType;
    public Boolean editEventBefore;
    public ScriptingLogicsModule.LAWithParams editEventAction;
    public ImSet<String> annotations = SetFact.EMPTY();

    public void addAnnotation(String annotation) {
        annotations = annotations.merge(annotation);
    }
}
