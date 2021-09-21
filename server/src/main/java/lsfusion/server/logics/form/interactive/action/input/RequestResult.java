package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;

public class RequestResult {
    public final ObjectValue chosenValue;
    public final Type type;
    public final LP targetProp;

    public RequestResult(ObjectValue chosenValue, Type type, LP targetProp) {
        this.chosenValue = chosenValue;
        this.type = type;
        this.targetProp = targetProp;
        assert targetProp != null;
    }

    public static ImList<RequestResult> get(ObjectValue chosenValue, Type type, LP targetProp) {
        if(chosenValue == null) // cancel
            return null;

        return ListFact.singleton(new RequestResult(chosenValue, type, targetProp));
    }
}
