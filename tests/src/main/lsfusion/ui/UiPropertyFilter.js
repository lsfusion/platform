// UiPropertyFilterIT's React views of UiPropertyFilter.lsf: a property's filter, FILTER(<property>), is a part of its
// group, data.<group>.filter.<property> - {caption, type, compare, negation, value} -, which its member changes
(function () {
    var e = React.createElement;

    // a filter's value as an input of the view's own, its member changing it as it is typed
    function input(filter, member, className) {
        return e('input', {className: className, value: filter.value != null ? filter.value : '',
            onChange: function (event) { member.change(event.target.value); }});
    }

    // the conditions, in turn: `not` where one is negated, its property, its comparison and its value
    function conditions(group) {
        return e('span', {className: 'ui-pf-conditions'}, group.filters.map(function (condition) {
            return (condition.negation ? 'not ' : '') + condition.property + ' ' + condition.compare + ' ' + condition.value;
        }).join('; '));
    }

    // over the platform's filter panel: the name's filter drawn here while its entry says it is shown, and the city's,
    // the platform's row, placed
    window.UiPfSearch = function (props) {
        var data = props.data, controller = props.controller;
        return e('div', null,
            data['FILTER(uiPfName(i))'].hidden ? e('span', {className: 'ui-pf-hidden'}, 'the name hidden')
                : input(data.i.filter.uiPfName, controller.i.filter.uiPfName, 'ui-pf-name'),
            e(window.lsfusion.Platform, {name: 'FILTER(uiPfCity(i))'}));
    };

    // the whole group: the filters of its properties, its conditions and its rows; the name's filter stated with a
    // comparison and a negation of their own
    window.UiPfBoard = function (props) {
        var group = props.data.i, filter = props.controller.i.filter;
        var value = React.useState('');
        function state(className, options) {
            return e('button', {className: className, onClick: function () { filter.uiPfName.change(value[0], options); }}, className);
        }
        return e('div', null,
            input(group.filter.uiPfCity, filter.uiPfCity, 'ui-pf-city'),
            input(group.filter.uiPfName, filter.uiPfName, 'ui-pf-name'),
            e('input', {className: 'ui-pf-value', value: value[0], onChange: function (event) { value[1](event.target.value); }}),
            state('ui-pf-not', {negation: true}), state('ui-pf-equals', {compare: '=', negation: false}),
            conditions(group),
            e('span', {className: 'ui-pf-rows'}, group.list.map(function (row) { return row.uiPfName.value; }).join(', ')));
    };

    // a filter's own view: its part alone, on its group's node
    window.UiPfOwn = function (props) {
        return input(props.data.i.filter.uiPfName, props.controller.i.filter.uiPfName, 'ui-pf-name');
    };

    // the conditions, and the name's filter declared in their box, under a container's SHOWIF: whether the container's
    // and the filter's entries say they are hidden
    window.UiPfConditions = function (props) {
        var data = props.data, group = data.i;
        return e('div', {'data-conditions-hidden': String(data.conditionsShown.hidden === true),
                'data-name-hidden': String(data['FILTER(uiPfName(i))'].hidden === true)},
            input(group.filter.uiPfName, props.controller.i.filter.uiPfName, 'ui-pf-name'),
            conditions(group));
    };

    // the name's filter and a LOGICAL one's, with no filters box: their values as given, and the LOGICAL's type; buttons
    // setting the LOGICAL's and taking the name's off
    window.UiPfNoBox = function (props) {
        var group = props.data.i, filter = props.controller.i.filter;
        function button(className, member, value) {
            return e('button', {className: className, onClick: function () { member.change(value); }}, className);
        }
        return e('div', null,
            e('span', {className: 'ui-pf-name'}, JSON.stringify(group.filter.uiPfName.value)),
            e('span', {className: 'ui-pf-cheap'}, JSON.stringify(group.filter.uiPfCheap.value)),
            e('span', {className: 'ui-pf-cheap-type'}, group.filter.uiPfCheap.type),
            button('ui-pf-cheap-true', filter.uiPfCheap, true), button('ui-pf-cheap-false', filter.uiPfCheap, false),
            button('ui-pf-cheap-none', filter.uiPfCheap, null), button('ui-pf-name-none', filter.uiPfName, null));
    };
})();
