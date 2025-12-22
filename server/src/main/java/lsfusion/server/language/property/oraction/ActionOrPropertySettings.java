package lsfusion.server.language.property.oraction;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;

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
    public ImSet<EditEvent> editEvents = SetFact.EMPTY();
    public ImSet<String> annotations = SetFact.EMPTY();

    public void addEditEvent(ScriptingLogicsModule.LAWithParams action, String actionType, Boolean before, LocalizedString contextMenuCaption, String keyPress) {
        editEvents = editEvents.merge(new EditEvent(action, actionType, before, contextMenuCaption, keyPress));
    }

    public void addAnnotation(String annotation) {
        annotations = annotations.merge(annotation);
    }

    public static class EditEvent<T> {
        public T action;
        public String actionType;
        public Boolean before;
        public LocalizedString contextMenuCaption;
        public KeyStroke keyPress;

        public EditEvent(T action, String actionType, Boolean before, LocalizedString contextMenuCaption, String keyPress) {
            this.action = action;
            this.actionType = actionType;
            this.before = before;
            this.contextMenuCaption = contextMenuCaption;
            this.keyPress = KeyStroke.getKeyStroke(keyPress);
        }

        public boolean isContextMenu() {
            return ServerResponse.CONTEXTMENU.equals(actionType);
        }
        public boolean isKeyPress() {
            return ServerResponse.KEYPRESS.equals(actionType);
        }
    }
}
