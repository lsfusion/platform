//range input type. value type = "DOUBLE"
var customInputRange =
    {
        render: (element) => {
            let input = customInputRender("range", element);
            input.min = "1";
            input.max = "100";
        },
        update: function (element, controller, value) {
            return customInputUpdate(element, value, controller, (inputElement) => {
                inputElement.onmouseup = function () {
                    if(value !== parseInt(this.value))
                        controller.changeValue(null, parseInt(this.value));
                }
            });
        },
        clear: (element) => customInputClear(element)
    };

//month input type. value type = "DATE". Min width on form ~140px
var customInputMonth =
    {
        render: (element) => customInputRender("month", element),
        update: function (element, controller, value) {
            return customInputUpdate(element, value, controller,
                (inputElement) => {
                    inputElement.onblur = function () {
                        let date = new Date(this.value);
                        if (date.getTime() !== new Date(value).getTime())
                            controller.changeValue(null, !isNaN(date) ? controller.toDateDTO(date.getFullYear(), date.getMonth() + 1, date.getDate()) : null);
                    }
                },
                (inputElement) => {
                    let valueDate = new Date(value);
                    inputElement.value = value != null ? valueDate.getFullYear() + '-' + ("0" + (valueDate.getMonth() + 1)).slice(-2) : null;
                });
        },
        clear: (element) => customInputClear(element)
    };

//date input type. value type = "DATE". Min width on form ~120px
var customInputDate =
    {
        render: (element) => customInputRender("date", element),
        update: function (element, controller, value) {
            return customInputUpdate(element, value, controller,
                (inputElement) => {
                    inputElement.onblur = function () {
                        let date = new Date(this.value);
                        if (date.getTime() !== new Date(value).getTime())
                            controller.changeValue(null, !isNaN(date) ? controller.toDateDTO(date.getFullYear(), date.getMonth() + 1, date.getDate()) : null);
                    }
                });
        },
        clear: (element) => customInputClear(element)
    };

//time input type. value type = "TIME". Min width on form ~90px
var customInputTime =
    {
        render: (element) => customInputRender("time", element),
        update: function (element, controller, value) {
            return customInputUpdate(element, value, controller,
                (inputElement) => {
                    inputElement.onblur = function () {
                        let timeParts = this.value.split(':');
                        if (value.toString() !== this.value)
                            controller.changeValue(null, timeParts.length === 2 ? controller.toTimeDTO(parseInt(timeParts[0], 10), parseInt(timeParts[1], 10), 0) : null);
                    }
                });
        },
        clear: (element) => customInputClear(element)
    };

function customInputRender(type, element) {
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

    element.appendChild(input);

    return input;
}

function customInputUpdate(element, value, controller, changeValueFunction, setValueFunction) {
    let inputElement = element.lastElementChild;

    if (inputElement != null) {
        if (setValueFunction != null)
            setValueFunction(inputElement);
        else
            inputElement.value = value;

        changeValueFunction(inputElement);
    }
}

function customInputClear(element) {
    while (element.lastElementChild) {
        element.removeChild(element.lastElementChild);
    }
}