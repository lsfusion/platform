// UiPivotIT's React view of UiPivot.lsf: the toolbar of a group in manual update, TOOLBARSYSTEM(i) moved into a header
// over the platform's grid - what it shows of the group (the manual update, whether the server holds changes back, the
// view type), and buttons for the members that update the rows, switch the view type and turn the manual update off
window.UiPivotHeader = function (props) {
    var e = React.createElement, items = props.data.i, buttons = props.controller.i;
    return e('div', null,
        e('span', {className: 'ui-pivot-state'}, 'manual ' + items.manual + ', pending ' + items.updatePending + ', ' + items.listView),
        e('button', {onClick: function () { buttons.update(); }}, 'Update rows'),
        e('button', {onClick: function () { buttons.listView.change('pivot'); }}, 'To pivot'),
        e('button', {onClick: function () { buttons.listView.change('grid'); }}, 'To grid'),
        e('button', {onClick: function () { buttons.manual.change(false); }}, 'Automatic'));
};
