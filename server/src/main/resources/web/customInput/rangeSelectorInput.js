let renderController;
let rangeSelectorElement;

renderRangeSelector = function (element) {
    element.onmousedown = function (ev) {
        ev.stopPropagation();
    }

    rangeSelectorElement = createRangeSelectorElement();
}

updateRangeSelector = function (element, value, controller) {
    if (renderController == null)
        renderController = controller;

    if (element.hasChildNodes(rangeSelectorElement))
        element.firstChild.value = value;
    else
        element.appendChild(rangeSelectorElement);
}

clearRangeSelector = function (element) {
    while (element.lastElementChild) {
        element.removeChild(element.lastElementChild);
    }
}

createRangeSelectorElement = function () {
    let input = document.createElement("input");
    input.type = "range";
    input.min = "1";
    input.max = "100";

    input.onmousedown = function (ev) {
        ev.stopPropagation();
    }

    input.onmouseup = function (ev) {
        if (renderController != null)
            renderController.changeValue(null, parseInt(this.value));
    }

    return input;
}