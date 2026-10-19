// UiCustomContainerIT's React views of UiCustomContainer.lsf and its custom cell renderer; the two no-build views are
// web/init/uiCustomContainer*.jsx

// a block of a fixed size, the whole content of the form uiCustomContainerReactSized
window.UiCustomContainerBlock = function () {
    return React.createElement('div', { className: 'ui-custom-container-block', style: { width: '480px', height: '320px' } });
};

// two fields and two blocks of the view's own, the form's ENTER and ESCAPE in mind: the first of each says nothing, the
// second says the user is editing in it while it has the focus
window.UiCustomContainerFields = function (props) {
    var e = React.createElement, controller = props.controller;
    var declaring = { onFocus: function (event) { controller.startEditing(event.target); },
                      onBlur: function (event) { controller.stopEditing(event.target); } };
    return e('div', { className: 'ui-custom-container-fields' },
        e('input', { className: 'ui-custom-container-undeclared' }),
        e('input', Object.assign({ className: 'ui-custom-container-declared' }, declaring)),
        e(window.UiCustomContainerKeyBlock, { className: 'ui-custom-container-undeclared-block' }),
        e(window.UiCustomContainerKeyBlock,
            Object.assign({ className: 'ui-custom-container-declared-block' }, declaring)));
};

// a block that takes the focus and the characters typed while it has it, which it shows - a line high while it shows
// none: a field the browser does not take for a text control, as a picker or a canvas is
window.UiCustomContainerKeyBlock = function (props) {
    var typing = React.useState(''), typed = typing[0], setTyped = typing[1];
    return React.createElement('div', {
        className: props.className, tabIndex: 0, style: { minHeight: '24px' },
        onFocus: props.onFocus, onBlur: props.onBlur,
        onKeyDown: function (event) {
            if (event.key.length === 1)
                setTyped(function (text) { return text + event.key; });
        } }, typed);
};

// the place of the text the platform draws, in the form uiCustomContainerPlaced
window.UiCustomContainerPlaced = function () {
    return React.createElement('div', { className: 'ui-custom-container-placed' },
        React.createElement(window.lsfusion.Platform, { name: 'uiCustomContainerText' }));
};

// the custom cell renderer of uiCustomContainerCellText: a field of its own in the cell, which says nothing of what is
// edited in it
window.uiCustomContainerCell = function () {
    return {
        render: function (element) {
            var field = document.createElement('input');
            field.className = 'ui-custom-container-cell-field';
            element.appendChild(field);
        },
        update: function () {
        }
    };
};
