// UiReactBatchChromeIT's React view of a table's toolbar, of a tree, or of a whole group: it hands what it is drawn
// with, its data and its controller, to the test (window.uiRbcView)
window.UiRbcView = function (props) {
    window.uiRbcView = props;
    return React.createElement('div', {className: 'ui-rbc-view', tabIndex: 0}, 'drawn');
};
