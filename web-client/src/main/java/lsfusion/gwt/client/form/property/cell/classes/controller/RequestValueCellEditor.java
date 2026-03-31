package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public interface RequestValueCellEditor extends RequestCellEditor {

    PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException;

    default GUserInputResult getCommitResult(Element parent, Integer contextAction) throws InvalidEditException {
        return GUserInputResult.singleValue(getCommitValue(parent, contextAction), contextAction);
    }

    void commitValue(PValue value);

    void setDeferredCommitOnBlur(boolean deferredCommitOnBlur);

    void setCancelTheSameValueOnBlur(Object oldValue);
}
