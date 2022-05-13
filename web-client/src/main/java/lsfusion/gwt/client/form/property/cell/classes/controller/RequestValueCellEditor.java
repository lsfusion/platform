package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;

public interface RequestValueCellEditor extends RequestCellEditor {

    String invalid = "INVALID";

    Object getValue(Element parent, Integer contextAction);

    void commitValue(Element parent, Object value);

    void setDeferredCommitOnBlur(boolean deferredCommitOnBlur);
}
