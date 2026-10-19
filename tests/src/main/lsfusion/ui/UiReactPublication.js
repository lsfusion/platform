// UiReactPublicationIT's React views of UiReactPublication.lsf: two CONTROLLED inputs, each written back through a
// member from its onChange - the note's value, and a condition on the items' name, as the filters' how-to does -,
// beside what only the server's answers show, the note's echo and the rows the condition leaves; and a view taller
// than a window's default content, for a window measured as it is shown
window.UiReactPublicationInputs = function (props) {
    var e = React.createElement, data = props.data, controller = props.controller;
    var note = data.uiReactPublicationNote.value;
    var condition = data.i.filters.filter(function (each) { return each.property === 'uiReactPublicationName'; })[0];
    return e('div', null,
        e('input', {className: 'ui-react-publication-note', value: note === null ? '' : note,
            onChange: function (event) { controller.uiReactPublicationNote.change(event.target.value); }}),
        e('div', {className: 'ui-react-publication-echo'}, data.uiReactPublicationEcho.value),
        e('input', {className: 'ui-react-publication-search', value: condition ? condition.value : '',
            onChange: function (event) {
                controller.i.filters.change({property: 'uiReactPublicationName', compare: '_', value: event.target.value});
            }}),
        e('div', {className: 'ui-react-publication-rows'}, data.i.list.map(function (row) {
            return row.uiReactPublicationName.value;
        }).join(', ')));
};

window.UiReactPublicationTall = function () {
    return React.createElement('div', {className: 'ui-react-publication-tall', style: {width: '520px', height: '420px'}}, 'tall');
};
