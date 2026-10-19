// UiReactRowsChromeIT's React views: they draw the rows of uiRrc*'s items - a name each, the current one marked, a
// click making it current -, place what the platform draws of the group's toolbar and filters back, and hand what they
// are drawn with, their data and their controller, to the test (window.uiRrcView)
(function () {
    var e = React.createElement;

    function rows(props) {
        var node = props.data.i;
        return node.list.map(function (row) {
            return e('div', {key: row.key, className: 'ui-rrc-row' + (row.isCurrent ? ' ui-rrc-current' : ''),
                onClick: function () { props.controller.i.change(row); }}, row.name.value);
        });
    }

    function view(props, children) {
        window.uiRrcView = props;
        return e('div', {className: 'ui-rrc-view'}, children);
    }

    // the rows alone: the platform's toolbar and filters stay in the group's box
    window.UiRrcCards = function (props) {
        return view(props, rows(props));
    };

    // the platform's toolbar and filter box, placed
    window.UiRrcPlaced = function (props) {
        var Platform = window.lsfusion.Platform;
        return view(props, [e(Platform, {key: 'toolbar', name: 'TOOLBARSYSTEM(i)'}), e(Platform, {key: 'filters', name: 'FILTERBOX(i)'})]
            .concat(rows(props)));
    };

    // the platform's filter box, placed - the toolbar is the view's, which draws none
    window.UiRrcFilterBox = function (props) {
        var Platform = window.lsfusion.Platform;
        return view(props, [e(Platform, {key: 'filters', name: 'FILTERBOX(i)'})].concat(rows(props)));
    };

    // the platform's toolbar and filter controls, placed, the conditions the view's: each drawn as it is stated
    window.UiRrcControls = function (props) {
        var Platform = window.lsfusion.Platform;
        return view(props, [e(Platform, {key: 'toolbar', name: 'TOOLBARSYSTEM(i)'}), e(Platform, {key: 'controls', name: 'FILTERCONTROLS(i)'}),
            e('div', {key: 'conditions', className: 'ui-rrc-conditions'}, props.data.i.filters.map(function (condition) {
                return condition.property + ' ' + condition.compare + ' ' + condition.value;
            }).join('; '))].concat(rows(props)));
    };
})();
