// the React view UiReactOrdersIT uses for uiRorBoard's rows, in plain JS: it says what it is given - the group's
// sortings, its rows, which properties it may not sort by, what it has of the group whose rows the platform draws, the
// user events counted - and hands on its data and controller (window.uiRorBoardState) for the test to call the members
// with
window.UiRorBoard = function (props) {
    var e = React.createElement, data = props.data, o = data.o, p = data.p;
    window.uiRorBoardState = {data: data, controller: props.controller};
    return e('div', null,
        e('div', {className: 'ui-ror-orders'}, o.orders.map(function (order) {
            return order.property + (order.desc ? ' desc' : ' asc');
        }).join(', ')),
        o.list.map(function (row) {
            return e('div', {key: row.key, className: 'ui-ror-row'}, row.name.value + '|' + row.qty.value);
        }),
        e('div', {className: 'ui-ror-nosort'}, ['name', 'price', 'qty'].map(function (property) {
            return property + ' ' + o[property].noSort;
        }).join(', ')),
        e('div', {className: 'ui-ror-properties'}, o.properties.join(', ')),
        e('div', {className: 'ui-ror-other'}, ('orders' in p ? 'orders' : 'no orders') + ', ' + ('list' in p ? 'rows' : 'no rows')
            + ', properties ' + p.properties.join(', ') + ', members ' + Object.keys(props.controller.p).sort().join(', ')),
        e('div', {className: 'ui-ror-events'}, String(data.uiRorOrdersEvents.value)));
};
