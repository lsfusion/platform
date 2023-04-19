package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.PValue;

public interface RequestValueCellEditor extends RequestCellEditor {

    PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException;

    void commitValue(Element parent, PValue value);

    void setDeferredCommitOnBlur(boolean deferredCommitOnBlur);

    void setCancelTheSameValueOnBlur(Object oldValue);
}
