// UiReactFilterControlsIT's React views of uiRfc*'s items
(function () {
    var e = React.createElement;

    // a group's filter controls - and, where it is given them, its conditions, each drawn as `property compare value`,
    // and its rows, a name each -; where it is given no conditions, it places the platform's panel of them, FILTERS(i).
    // It hands what it is drawn with, its data and its controller, to the test (window.uiRfcView)
    function view(props, filterHost) {
        var Platform = window.lsfusion.Platform, node = props.data.i;
        window.uiRfcView = props;
        return e('div', {className: 'ui-rfc-view'},
            'filters' in node
                ? e('div', {className: 'ui-rfc-conditions'}, node.filters.map(function (condition) {
                    return condition.property + ' ' + condition.compare + ' ' + condition.value;
                }).join('; '))
                : e(Platform, {name: filterHost}),
            (node.list || []).map(function (row) {
                return e('div', {key: row.key, className: 'ui-rfc-row'}, row.name.value);
            }));
    }

    window.UiRfcView = function (props) { return view(props, 'FILTERS(i)'); };
    window.UiRfcPlatformBoxView = function (props) { return view(props, 'FILTERBOX(i)'); };

    // a container that places the one child the platform draws in it - the filter box, or the panel of the conditions
    window.UiRfcHost = function (props) {
        var Platform = window.lsfusion.Platform;
        return e('div', {className: 'ui-rfc-host'}, Object.keys(props.data).filter(function (name) {
            return name.indexOf('FILTERBOX(') === 0 || name.indexOf('FILTERS(') === 0;
        }).map(function (name) {
            return e(Platform, {key: name, name: name});
        }));
    };
})();
