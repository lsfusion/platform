// UiToolbarTreeIT's React view of a tree - its rows, drawn as names, or its toolbar and filter controls: it hands what
// it is drawn with, its data and its controller, to the test (window.uiTtrView)
window.UiTtrView = function (props) {
    var e = React.createElement, data = props.data;
    window.uiTtrView = props;
    function rows(group, property) {
        return ((data[group] && data[group].list) || []).map(function (row) {
            return e('div', {key: row.key, className: 'ui-ttr-row'}, row[property].value);
        });
    }
    return e('div', {className: 'ui-ttr-view'}, rows('c', 'catName').concat(rows('i', 'name')));
};
