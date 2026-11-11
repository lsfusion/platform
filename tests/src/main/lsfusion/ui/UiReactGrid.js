// UiReactGridIT's React view of a grid's rows: it hands what it is drawn with, its data and its controller, to the
// test (window.uiRgView)
window.UiRgView = function (props) {
    window.uiRgView = props;
    return React.createElement('div', {className: 'ui-rg-view'}, props.data.i.list.map(function (row) {
        return React.createElement('div', {key: row.key}, [row.number.value, row.name.value, row.qty.value].join(' | '));
    }));
};
