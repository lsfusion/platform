package lsfusion.gwt.client.form.property.cell.view;

import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;
import java.util.Arrays;

public class GUserInputResult implements Serializable {
    private static final PValue[] EMPTY_PVALUES = new PValue[0];

    public static final GUserInputResult canceled = new GUserInputResult(true, EMPTY_PVALUES, null);

    private boolean editCanceled;
    private Serializable[] values;
    private Integer contextAction;

    @SuppressWarnings("UnusedDeclaration")
    public GUserInputResult() {
        this(false, new PValue[] {null}, null);
    }

    private GUserInputResult(boolean canceled, PValue[] values, Integer contextAction) {
        this.editCanceled = canceled;
        assert values != null;
        this.values = PValue.convertFileValuesBack(values);
        this.contextAction = contextAction;
        this.pValue = values.length > 0 ? values[0] : null;
    }

    public static GUserInputResult singleValue(PValue value, Integer contextAction) {
        return new GUserInputResult(false, new PValue[]{value}, contextAction);
    }

    public static GUserInputResult singleValue(PValue value) {
        return singleValue(value, null);
    }

    public boolean isCanceled() {
        return editCanceled;
    }

    public Serializable[] getValues() {
        return values;
    }

    private transient PValue pValue;
    public PValue getPValue() {
        return pValue;
    }

    public Integer getContextAction() {
        return contextAction;
    }

    @Override
    public String toString() {
        return "UserInputResult[editCanceled=" + editCanceled + ", values=" + Arrays.toString(values) + "]";
    }

}
