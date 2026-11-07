package lsfusion.gwt.client.form.property;

import java.io.Serializable;

// a property's selection (PropertyDrawEntity.Select), as a React view that draws it needs it said, and as its render
// function is given it (CustomCellRenderer) - the platform's own element (select.js) or one given in CUSTOM: whether it
// is of several values, whether its value has to be there, whether the options' views are HTML, whether a pick sends the
// option's view - the input of an explicit change - rather than the option, and of several values the text their value
// joins the options by (Property.Select.separator: its GROUP CONCAT's where it is one given as it is, a union's where all
// its cases join by one, a comma otherwise). The element type the design names is the platform's element's alone, in its
// render function's name: how a view draws the selection is its own
public class GSelect implements Serializable {
    public boolean multi;
    public boolean notNull;
    public boolean html;
    public boolean changeValue;
    public String separator;

    @SuppressWarnings("unused")
    public GSelect() {
    }

    public GSelect(boolean multi, boolean notNull, boolean html, boolean changeValue, String separator) {
        this.multi = multi;
        this.notNull = notNull;
        this.html = html;
        this.changeValue = changeValue;
        this.separator = separator;
    }
}
