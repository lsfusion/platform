// #1675: expose the own-filter projection to the browser regression.
window.UiReactViewFilters = function (props) {
    var e = React.createElement, data = props.data, controller = props.controller;
    window.uiReactViewFilters = {data: data, controller: controller};
    return e('div', null, data.i.list.map(function (row) {
        return e('div', {key: row.key, 'data-number': row.number.value},
            [row.qty.value, row.name.value, row.number.value].join('|'));
    }));
};
