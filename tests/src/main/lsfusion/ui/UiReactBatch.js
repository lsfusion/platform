// UiReactBatchIT's views of uiRb*'s items: two React views - each hands what it is drawn with, its data and its
// controller, to the test (window.uiRbViews.<view>) - and a classic custom view, which hands its controller and its
// rows (window.uiRbClassic)
(function () {
    var e = React.createElement;

    function publish(name, props) {
        (window.uiRbViews = window.uiRbViews || {})[name] = props;
    }

    // the rows, the platform's quantity editor and bump button placed in each, its approve and its comment placed
    window.UiRbBoard = function (props) {
        var Platform = window.lsfusion.Platform;
        publish('board', props);
        return e('div', {className: 'ui-rb-board'},
            e('table', null, e('tbody', null, props.data.i.list.map(function (row) {
                return e('tr', {key: row.key, className: 'ui-rb-row' + (row.isCurrent ? ' ui-rb-current' : '')},
                    e('td', {className: 'ui-rb-name'}, row.name.value),
                    e('td', {className: 'ui-rb-note'}, row.note.value),
                    e('td', {className: 'ui-rb-qty'}, e(Platform, {name: 'i.qty', row: row})),
                    e('td', {className: 'ui-rb-bump'}, e(Platform, {name: 'i.bump', row: row})));
            }))),
            e('div', {className: 'ui-rb-approve'}, e(Platform, {name: 'i.approve'})),
            e('div', {className: 'ui-rb-comment'}, e(Platform, {name: 'comment'})));
    };

    // the note of the current item placed - nothing else of the group
    window.UiRbSide = function (props) {
        var Platform = window.lsfusion.Platform;
        publish('side', props);
        return e('div', {className: 'ui-rb-side'}, e(Platform, {name: 'i.sideNote'}));
    };

    // a classic custom grid view (OBJECTS c = UiRbItem CUSTOM 'UiRbClassic')
    window.UiRbClassic = function () {
        return {
            render: function (element, controller) {
                window.uiRbClassic = {controller: controller, list: []};
                element.textContent = 'classic';
            },
            update: function (element, controller, list) {
                window.uiRbClassic = {controller: controller, list: list};
                element.textContent = list.map(function (row) { return row.name + ': ' + row.note; }).join('; ');
            }
        };
    };
})();
