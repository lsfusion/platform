// UiReactFiltersIT's React view of a group's user filters - or of the whole group, or of a tree: it draws the
// conditions it is given, each as `property compare value`, and the rows, a name each; it hands what it is drawn with,
// its data and its controller, to the test (window.uiRflView), and keeps, render by render, the conditions it was drawn
// with (window.uiRflDrawn) and how many times their list was another one (window.uiRflListsDrawn)
window.UiRflView = function (props) {
    var e = React.createElement, data = props.data, node = data.cats || data.i;
    var conditions = node.filters.map(function (condition) {
        return condition.property + ' ' + condition.compare + ' ' + condition.value;
    }).join('; ');
    window.uiRflView = props;
    (window.uiRflDrawn = window.uiRflDrawn || []).push(conditions);
    if (node.filters !== window.uiRflList) {
        window.uiRflList = node.filters;
        window.uiRflListsDrawn = (window.uiRflListsDrawn || 0) + 1;
    }
    return e('div', {className: 'ui-rfl-view'},
        e('div', {className: 'ui-rfl-conditions'}, conditions),
        ((data.i && data.i.list) || []).map(function (row) {
            return e('div', {key: row.key, className: 'ui-rfl-row'}, row.name.value);
        }));
};
