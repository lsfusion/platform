package lsfusion.gwt.client.form.object.table.tree.controller;

import lsfusion.gwt.client.form.object.table.controller.GTableController;

// what draws a TREE's rows, one for all its groups, as the tree's filter input controller (GTableController) and its
// toolbar ask them, whoever draws them (GReactFormData.createTreeController): the platform's tree
// (GPlatformTreeController), which gives itself to the toolbar as it is made, or the rows React draws, the tree's grid
// part (GReactFormData.TreeTablePart), which the projection gives it as it makes them (GToolbarController.addRows) - as
// a group's grid's rows (GGridController)
public interface GTreeController extends GTableController {
    // what opens them in bulk - the two buttons of the platform's toolbar, over either, and the members that do what
    // they do where a view draws the rows or the toolbar: open the current node and every node under it, or the tree
    // from the current node's group down - every node of that group under the nodes open above it, and of the groups
    // below, as the platform's button always has -, nothing before a node is current
    void expandCurrent();
    void expandTree();
    // ... and what a VIEW's part of the toolbar opens through members of its own - the same two -, if anything: the
    // platform's tree, itself; nothing over the rows React draws, where the tree has those as members already - its
    // current node, the tree's grid part's expandCurrent(), the tree from a group down, that group's expandAll() - and
    // the toolbar gives them no second time. As a group's grid's rows give a view's part of the toolbar their views,
    // none where React draws them (GGridController.getListViewController)
    GTreeController getOpenedByToolbarMembers();
}
