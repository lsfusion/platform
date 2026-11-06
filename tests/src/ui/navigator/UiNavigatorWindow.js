// UiNavigatorWindowIT's System.toolbar (EXTEND WINDOW System.toolbar CUSTOM 'UiNavigatorWindow'): for each element, what
// its entry says of `hidden`, and the place the platform's button goes into - as tall as a button, so that a place left
// empty takes that room. Both are named by the element's name in its module
window.UiNavigatorWindow = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform, data = props.data;
    return e('div', { className: 'ui-nw' }, data.root.map(function (name) {
        var entry = data.byName[name], element = name.substring(name.indexOf('.') + 1);
        return e('div', { key: name },
            e('span', { className: 'ui-nw-entry ' + element },
                entry.hidden === true ? 'hidden' : entry.hidden === false ? 'shown' : 'no hidden'),
            e(Platform, { name: name, className: 'ui-nw-place ' + element, style: { height: '40px', margin: '4px' } }));
    }));
};
