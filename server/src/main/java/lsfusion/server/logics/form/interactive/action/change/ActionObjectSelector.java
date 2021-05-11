package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;

// needed when action has to be created after the logics is initialized
// actually needed for SELECTOR logics, pretty similar to FormSelector class
public interface ActionObjectSelector {

    ActionObjectEntity<?> getAction(FormEntity formEntity);
}
