package lsfusion.gwt.client.form.property;

// where a GROUP-object reader's attribute is written in the React `data`:
//   ROW   - per row: read at each row key, written onto the row itself (data.<group>.list[].<attr>)
//   GROUP - once for the whole group: read at EMPTY, written onto the group (data.<group>.<attr>)
public enum GGroupAttributeScope {
    ROW,
    GROUP
}
