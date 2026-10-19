// UiReactPagesIT's React views of the pages of UiReactPages.lsf, each writing out the rows of its group, their names
// joined: the first page's, the second page's with a button running the form's CANCEL through its member, and the
// second page's with its rows alone
(function () {
    function rows(className, list, property) {
        return React.createElement('div', {className: className}, list.map(function (row) {
            return row[property].value;
        }).join(', '));
    }

    window.UiReactPagesFirst = function (props) {
        return rows('ui-react-pages-first', props.data.a.list, 'aName');
    };

    window.UiReactPagesCancel = function (props) {
        return React.createElement('div', null,
            React.createElement('button', {onClick: function () { props.controller.cancel.exec(); }}, 'React cancel'),
            rows('ui-react-pages-second', props.data.b.list, 'bName'));
    };

    window.UiReactPagesSecond = function (props) {
        return rows('ui-react-pages-second', props.data.b.list, 'bName');
    };
})();
