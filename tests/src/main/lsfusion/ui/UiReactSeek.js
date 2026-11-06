// UiReactSeekIT's view of UiReactSeek.lsf: the rows in a box of its own, 200px high, 20px a row, followed by
// useSeekOnScroll; each seek the hook issues is recorded, in an array the re-renders keep, and handed on with the data
// and the controller to window.uiReactSeek
(function () {
    var e = React.createElement;
    window.UiReactSeekCards = function (props) {
        var data = props.data, controller = props.controller, seeks = React.useRef([]).current;
        var seekRef = window.lsfusion.useSeekOnScroll(controller.i, {onSeek: function (row) {
            seeks.push(row.number.value);
        }});
        window.uiReactSeek = {data: data, controller: controller, seeks: seeks};
        return e('div', {className: 'ui-react-seek-scroller', style: {height: '200px', overflowY: 'auto'}},
            data.i.list.map(function (row) {
                return e('div', {key: row.key, ref: seekRef(row), 'data-number': row.number.value,
                    className: 'ui-react-seek-row' + (row.isCurrent ? ' ui-react-seek-current' : ''),
                    style: {height: '20px', lineHeight: '20px'}}, String(row.number.value));
            }));
    };
})();
