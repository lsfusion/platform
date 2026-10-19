package lsfusion.interop.form.design;

// the names a react view's projection WRITES, part by part - on a group's node, data.<group> and its member
// controller.<group>, on a row of the group's rows, on a react container's controller -: the server refuses a form
// that would give a property one of them where the part is drawn (FormView.getReservedNodeNames, getReservedRowNames),
// and the web client writes them (GReactFormData, whose test checks that a part writes no other). Each list is one
// constant, its names joined by commas: a compile-time constant, which the GWT client reads too, with no source of
// this module - and which GWT's unit cache does not see change: after a change here, the client's test sees it once
// web-client/target/gwt-unitCache is dropped (a build from scratch has none)
public class ReactProjectionNames {
    // what a react container's controller carries beside its members: the four verbs every controller has, the
    // editing a view declares and the batch
    public static final String CONTROLLER = "exec,eval,evalAction,change,startEditing,stopEditing,isEditing,batch";
    // what every group's node carries, whatever is drawn in its scope: its index, its verb
    public static final String NODE_OWN = "properties,change";
    // the rows' part of a group (RowsPart), a tree's group's too, where the rows are drawn: on the node, and on a row
    public static final String GRID_PART = "list,byKey,keys,options";
    public static final String GRID_ROW = "key,isCurrent,objects,background,foreground,selected";
    // ... and of a group of a tree (TreeRowsPart): its node verbs - not the tree's own part (TreeTablePart), which
    // carries no property -: on the node, and on a row
    public static final String TREE_PART = "expand,collapse,toggle,expandAll,collapseAll";
    public static final String TREE_ROW = "parent,hasChildren,expanded,loading";
    // the sortings' part, of a list's group, with its rows - a tree's are on the tree's node, which carries no property
    public static final String ORDERS_PART = "orders";
    // the user filters' part, where what FILTERS(g) holds is drawn
    public static final String FILTERS_PART = "filters,filterProperties";
    // the part of the FILTERs of a group's properties, where FILTER(<property>) is drawn - on the node of the group of
    // the filter's property, a tree's group's too: one name for all of them, each under it by its property's name
    public static final String FILTER_PART = "filter";
    // their controls' part, where FILTERCONTROLS(g) is drawn
    public static final String FILTER_CONTROLS_PART = "addFilter,applyFilters,resetFilters,filtersPending,filtersManual";
    // the toolbar's part, where TOOLBARSYSTEM(g) is drawn - every button's, whatever the design's flags say, its
    // show... flags being the platform's alone: the count, the sum, the export, the manual update, the filter button,
    // the view switch
    public static final String TOOLBAR_PART = "getCount,getSum,export,manual,updatePending,update,filterControlsVisible,listView,listViews";
}
