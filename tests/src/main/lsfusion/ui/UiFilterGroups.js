// UiFilterGroupsIT's React view of uiFgr*'s items - of their whole group, its rows drawn as names, or of a filter group
// alone: it hands what it is drawn with, its data and its controller, to the test (window.uiFgrView), and takes the
// focus on a click, a filter's key going to the form from it
window.UiFgrView = function (props) {
    var e = React.createElement, node = props.data.i || {};
    window.uiFgrView = props;
    return e('div', {className: 'ui-fgr-view', tabIndex: 0},
        (node.list || []).map(function (row) {
            return e('div', {key: row.key, className: 'ui-fgr-row'}, row.name.value);
        }));
};
