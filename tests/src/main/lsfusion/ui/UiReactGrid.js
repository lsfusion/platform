// UiReactGridIT's React view of a grid's rows: it hands what it is drawn with, its data and its controller, to the
// test (window.uiRgView), and takes the browser's clipboard events as a view does - the text copied given to the
// clipboard once the server has answered
window.UiRgView = function (props) {
    window.uiRgView = props;
    var i = props.controller.i;
    return React.createElement('div', {
        className: 'ui-rg-view', tabIndex: 0,
        onCopy: function (event) {
            event.preventDefault();
            i.copy().then(function (text) { return navigator.clipboard.writeText(text); });
        },
        onPaste: function (event) {
            event.preventDefault();
            i.paste(event.clipboardData.getData('text/plain'));
        }
    }, props.data.i.list.map(function (row) {
        return React.createElement('div', {key: row.key}, [row.number.value, row.name.value, row.qty.value].join(' | '));
    }));
};
