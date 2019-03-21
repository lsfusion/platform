package lsfusion.gwt.client.base.result;

import net.customware.gwt.dispatch.shared.Result;

public class NumberResult implements Result {
    public Number value;

    @SuppressWarnings({"UnusedDeclaration"})
    public NumberResult() {}

    public NumberResult(Number value) {
        this.value = value;
    }
}
