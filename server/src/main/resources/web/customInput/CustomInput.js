function LSFCustomInput(type) {
    return "LSFCustomInput" + type + ":LSFCustomInputUpdate:LSFCustomInputClear";
}

//range input type. value type = "DOUBLE"
function LSFCustomInputRange (element) {
    let input = _createInputElement("range");
    input.min = "1";
    input.max = "100";

    element.appendChild(input);
}

//month input type. value type = "DATE". Min width on form ~140px
function LSFCustomInputMonth (element) {
    element.appendChild(_createInputElement("month"));
}

//date input type. value type = "DATE". Min width on form ~120px
function LSFCustomInputDate (element) {
    element.appendChild(_createInputElement("date"));
}

//time input type. value type = "TIME". Min width on form ~90px
function LSFCustomInputTime (element) {
    element.appendChild(_createInputElement("time"));
}

function _createInputElement(type) {
    let input = document.createElement("input");
    input.type = type;
    input.style.setProperty("width", "100%");
    input.style.setProperty("height", "100%");
    input.onmousedown = function (ev) {
        ev.stopPropagation();
    }

    input.onkeypress = function (ev) {
        ev.stopPropagation();
    }

    input.onkeydown = function (ev) {
        ev.stopPropagation();
    }
    return input;
}

function LSFCustomInputUpdate (element, value, controller) {
    let rangeSelectorElement = element.lastElementChild;

    if (rangeSelectorElement != null) {
        if (rangeSelectorElement.type !== "month")
            rangeSelectorElement.value = value;

        if (rangeSelectorElement.type === "range") {
            rangeSelectorElement.onmouseup = function () {
                controller.changeValue(null, parseInt(this.value));
            }
        } else if (rangeSelectorElement.type === "month") {
            let valueDate = new Date(value);
            rangeSelectorElement.value = value != null ? valueDate.getFullYear() + '-' + ("0" + (valueDate.getMonth() + 1)).slice(-2) : null;

            _onCustomDateBlur(rangeSelectorElement, controller);
        } else if (rangeSelectorElement.type === "date") {
            _onCustomDateBlur(rangeSelectorElement, controller);

        } else if (rangeSelectorElement.type === "time") {
            rangeSelectorElement.onblur = function () {
                let timeParts = this.value.split(':');
                controller.changeValue(null, timeParts.length === 2 ? controller.toTimeDTO(parseInt(timeParts[0], 10), parseInt(timeParts[1], 10), 0) : null);
            }
        }
    }
}

function _onCustomDateBlur(inputElement, controller) {
    inputElement.onblur = function () {
        let date = new Date(this.value);
        controller.changeValue(null, !isNaN(date) ? controller.toDateDTO(date.getFullYear(), date.getMonth() + 1, date.getDate()) : null);
    }
}

function LSFCustomInputClear(element) {
    while (element.lastElementChild) {
        element.removeChild(element.lastElementChild);
    }
}