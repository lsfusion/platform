package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;

// needed when action has to be created after the logics is initialized
// actually needed for SELECTOR logics, pretty similar to FormSelector class
// we can get rid of this class if in getSelectorAction we change InputFilterEntity -> FormInputContextFilterSelector
// but since default event actions are created also on demand it's not that obvious that we should do that
public interface ActionObjectSelector {

    ActionObjectEntity<?> getAction(FormEntity formEntity);
}
