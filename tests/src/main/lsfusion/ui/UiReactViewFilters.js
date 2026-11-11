// the React view UiReactViewFiltersIT uses for the group's rows, in plain JS: it draws their numbers, and hands on its
// data and controller (window.uiReactViewFilters) for the test to state the view filters with
window.UiReactViewFilters = function (props) {
    var e = React.createElement;
    window.uiReactViewFilters = {data: props.data, controller: props.controller};
    return e('div', null, props.data.i.list.map(function (row) {
        return e('div', {key: row.key}, String(row.number.value));
    }));
};
