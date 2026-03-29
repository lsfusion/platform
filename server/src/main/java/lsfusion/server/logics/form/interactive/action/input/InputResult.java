package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.form.property.cell.UserInputResult;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.DataClass;

public class InputResult {
    public final ImList<ObjectValue> values;
    public final Integer contextAction;

    public InputResult(ImList<ObjectValue> values, Integer contextAction) {
        assert values != null;
        this.values = values;
        this.contextAction = contextAction;
    }

    public ImList<ObjectValue> getValues() {
        return values;
    }

    public ObjectValue getSingleValue() {
        return getValues().single();
    }

    public static InputResult singleValue(Object value, ConcreteClass concreteClass, Integer contextAction) {
        return new InputResult(ListFact.singleton(ObjectValue.getValue(value, concreteClass)), contextAction);
    }

    public static InputResult singleValue(Object value, ConcreteClass concreteClass) {
        return singleValue(value, concreteClass, null);
    }

    public static InputResult get(UserInputResult result, DataClass dataClass) {
        assert !result.isCanceled();

        Object[] inputValues = result.getValues();
        return new InputResult(ListFact.toList(inputValues.length, i -> ObjectValue.getValue(inputValues[i], dataClass)), result.getContextAction());
    }
}
