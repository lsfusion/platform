package lsfusion.gwt.client.form.property;

// the target bucket of a group-object reader's presentation in the React `data.meta`:
//   ROW  - per-row: read at each row key, emitted into data.<group>.list[].meta.row
//   NODE - group-scoped: read once at EMPTY, emitted into data.<group>.meta (the group node)
public enum GMetaScope {
    ROW,
    NODE
}
