// UiReactToolbarIT's React view of a group's toolbar - or of the whole group, its rows drawn as names: it hands what it
// is drawn with, its data and its controller, to the test (window.uiRtbView)
window.UiRtbView = function (props) {
    var e = React.createElement, node = props.data.i || {};
    window.uiRtbView = props;
    return e('div', {className: 'ui-rtb-view'},
        e('span', {className: 'ui-rtb-state'}, 'manual: ' + node.manual + ', view: ' + node.listView),
        (node.list || []).map(function (row) {
            return e('div', {key: row.key, className: 'ui-rtb-row'}, row.name ? row.name.value : '');
        }));
};
