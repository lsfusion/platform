// UiGridIT's render function: draws what it is given as HTML - a cell's image, a footer's text
window.uiGridHtml = function () {
    return {
        render: function (element) {
        },
        update: function (element, controller, value) {
            element.innerHTML = value === null ? '' : value;
        }
    };
};
