// UiComponentViewIT's React views: a grid's, a toolbar's, filter controls', a filter group's and a tree's own, and a
// react container's that places one - each hands what it is drawn with, its data and its controller, to the test
// (window.uiCvViews.<view>)
(function () {
    var e = React.createElement;

    function publish(name, props) {
        (window.uiCvViews = window.uiCvViews || {})[name] = props;
    }

    // the grid's own: its rows, a name each - a click making it current -, and the price the platform draws in each
    window.UiCvGrid = function (props) {
        var Platform = window.lsfusion.Platform, controller = props.controller;
        publish('grid', props);
        return e('div', {className: 'ui-cv-grid'}, props.data.i.list.map(function (row) {
            return e('div', {key: row.key, className: 'ui-cv-grid-row' + (row.isCurrent ? ' ui-cv-current' : '')},
                e('span', {className: 'ui-cv-name', onClick: function () { controller.i.change(row); }}, row.name.value),
                e(Platform, {name: 'i.price', row: row, className: 'ui-cv-price'}));
        }));
    };

    window.UiCvToolbar = function (props) {
        publish('toolbar', props);
        return e('div', {className: 'ui-cv-toolbar'}, 'manual: ' + props.data.i.manual);
    };

    // the filter group's own, and the rows of its group a react container draws, their names joined: each takes the
    // focus on a click, a filter's key going to the form from it
    window.UiCvFilterGroup = function (props) {
        publish('filterGroup', props);
        return e('div', {className: 'ui-cv-filter-group', tabIndex: 0}, 'kind: ' + props.data.i.uiCvKind.value);
    };
    window.UiCvRows = function (props) {
        publish('rows', props);
        return e('div', {className: 'ui-cv-rows', tabIndex: 0}, props.data.i.list.map(function (row) {
            return row.name.value;
        }).join(', '));
    };

    window.UiCvControls = function (props) {
        publish('controls', props);
        return e('div', {className: 'ui-cv-controls'}, 'pending: ' + props.data.i.filtersPending);
    };

    // a react container drawing the rows, which places the toolbar's own view - or not, a button of its own switching
    window.UiCvBoard = function (props) {
        var Platform = window.lsfusion.Platform, placed = React.useState(true);
        publish('board', props);
        return e('div', {className: 'ui-cv-board'},
            e('button', {className: 'ui-cv-place', onClick: function () { placed[1](!placed[0]); }}, placed[0] ? 'Unplace' : 'Place'),
            placed[0] ? e(Platform, {name: 'TOOLBARSYSTEM(i)', className: 'ui-cv-board-host'}) : null,
            e('div', {className: 'ui-cv-board-rows'}, props.data.i.list.map(function (row) {
                return row.name.value;
            }).join(', ')));
    };
    window.UiCvPlacedToolbar = function (props) {
        publish('placedToolbar', props);
        return e('div', {className: 'ui-cv-placed-toolbar'}, 'manual: ' + props.data.i.manual);
    };

    window.UiCvTree = function (props) {
        publish('tree', props);
        return e('div', {className: 'ui-cv-tree'}, props.data.c.list.map(function (row) {
            return row.catName.value;
        }).join(', '));
    };
})();
