package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;

public class RequestResult {
    public final ImList<ObjectValue> chosenValues;
    public final Type type;
    public final LP targetProp;

    public RequestResult(ImList<ObjectValue> chosenValues, Type type, LP targetProp) {
        this.chosenValues = chosenValues;
        this.type = type;
        this.targetProp = targetProp;
        assert targetProp != null;
    }

    public RequestResult(ObjectValue chosenValue, Type type, LP targetProp) {
        this(ListFact.singleton(chosenValue), type, targetProp);
    }

    @Deprecated
    public static ImList<RequestResult> get(ObjectValue chosenValue, Type type, LP targetProp) {
        if(chosenValue == null) // cancel
            return null;

        return ListFact.singleton(new RequestResult(chosenValue, type, targetProp));
    }

    public static ImList<RequestResult> get(ImList<ObjectValue> chosenValues, Type type, LP targetProp) {
        if(chosenValues == null) // cancel
            return null;

        return ListFact.singleton(new RequestResult(chosenValues, type, targetProp));
    }
}
