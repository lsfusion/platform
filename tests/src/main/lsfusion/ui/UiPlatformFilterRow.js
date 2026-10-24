// UiPlatformFilterRowIT's React view of the conditions of uiPfrOverView's items, FILTERS(i): it lists them - or says it
// is given none to list -, says whether it has a filter of a property and the entry of the city's, states the name's
// condition with the value typed into it, compared by '=', takes them all off or the city's alone, and places the
// city's FILTER, which the platform draws (custom = NULL)
window.UiPfrConditions = function (props) {
    var e = React.createElement, controller = props.controller, data = props.data || {}, node = data.i || {};
    return e('div', null,
        e('span', {className: 'ui-pfr-conditions'}, node.filters ? node.filters.map(function (condition) {
            return condition.property + ' ' + condition.compare + ' ' + condition.value;
        }).join('; ') : 'no conditions'),
        e('span', {className: 'ui-pfr-parts'}, (node.filter ? 'filter' : 'no filter') + ', '
            + (data['FILTER(uiPfrCity(i))'] ? 'city entry' : 'no city entry')),
        e('input', {className: 'ui-pfr-value'}),
        e('button', {className: 'ui-pfr-equals', onClick: function (event) {
            controller.i.filters.change({property: 'uiPfrName', compare: '=', value: event.target.previousSibling.value});
        }}, 'Equals'),
        e('button', {className: 'ui-pfr-clear', onClick: function () {
            controller.i.filters.change([]);
        }}, 'Clear'),
        e('button', {className: 'ui-pfr-city-off', onClick: function () {
            controller.i.filters.change({property: 'uiPfrCity'});
        }}, 'City off'),
        e(window.lsfusion.Platform, {name: 'FILTER(uiPfrCity(i))'}));
};
