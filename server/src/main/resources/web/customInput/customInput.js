//range input type. value type = "DOUBLE"
function customInputRange() {
    return new CustomInputRange().getFunctions();
}

//month input type. value type = "DATE". Min width on form ~140px
function customInputMonth() {
    return new CustomInputMonth().getFunctions();
}

//week input type. value type = "STRING". Min width on form ~130px
function customInputWeek() {
    return new CustomInputWeek().getFunctions();
}

//date input type. value type = "DATE". Min width on form ~120px
function customInputDate() {
    return new CustomInputDate().getFunctions();
}

//time input type. value type = "TIME". Min width on form ~90px
function customInputTime() {
    return new CustomInputTime().getFunctions();
}

//dateTime input type. value type = "DATETIME". Min width on form ~180px
function customInputDateTime() {
    return new CustomInputDateTime().getFunctions();
}

class CustomInput {
    constructor(type) {
        this.type = type;
    }

    getFunctions() {
        return {
            render: (element) => {
                let input = document.createElement("input");
                input.type = this.type;
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
            },
            update: (element, controller, value) => {
                let inputElement = element.lastElementChild;
                inputElement.value = this.parseValueFunction(value);
                this.onEventFunction(controller)(inputElement, value);
            },
            clear: (element) => {
                while (element.lastElementChild) {
                    element.removeChild(element.lastElementChild);
                }
            }
        }
    }

    parseValueFunction(value) {
        return value;
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onchange = function () {
                let date = new Date(this.value);
                if (date.getTime() !== new Date(value).getTime())
                    controller.changeValue(null, !isNaN(date) ? controller.toDateDTO(date.getFullYear(), date.getMonth() + 1, date.getDate()) : null);
            }
        }
    }

    getTwoDigitsValue(value) {
        return (("0" + value).slice(-2));
    }
}

class CustomInputRange extends CustomInput {
    constructor() {
        super("range");
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onchange = function () {
                if (value !== parseInt(this.value))
                    controller.changeValue(null, parseInt(this.value));
            }
        }
    }
}

class CustomInputMonth extends CustomInput {
    constructor() {
        super("month");
    }

    parseValueFunction(value) {
        let valueDate = new Date(value);
        return value != null ? valueDate.getFullYear() + '-' + super.getTwoDigitsValue(valueDate.getMonth() + 1) : null;
    }
}

class CustomInputWeek extends CustomInput {
    constructor() {
        super("week");
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onchange = function () {
                if ((value == null ? "" : value).toString() !== this.value)
                    controller.changeValue(null, this.value);
            }
        };

    }
}

class CustomInputDate extends CustomInput {
    constructor() {
        super("date");
    }
}

class CustomInputTime extends CustomInput {
    constructor() {
        super("time");
    }

    //use only hh:mm format. If use hh:mm:ss format, when seconds is 0, then input cut of the seconds part.
    parseValueFunction(value) {
        let valueParts = value != null ? value.toString().split(':') : 0;
        return valueParts.length === 3 ? value.toString().replace(':' + valueParts[2], '') : value;
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onchange = function () {
                let timeParts = this.value.split(':');
                if ((value == null ? "" : value).toString() !== this.value)
                    controller.changeValue(null, timeParts.length === 2 ? controller.toTimeDTO(parseInt(timeParts[0], 10), parseInt(timeParts[1], 10), 0) : null);
            }
        };
    }
}

class CustomInputDateTime extends CustomInput {
    constructor() {
        super("datetime-local");
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onchange = function () {
                let date = new Date(this.value);
                if (date.getTime() !== new Date(value).getTime())
                    controller.changeValue(null, !isNaN(date) ? controller.toDateTimeDTO(date.getFullYear(), date.getMonth() + 1, date.getDate(),
                        date.getHours(), date.getMinutes(), date.getSeconds()) : null);
            }
        };
    }

    parseValueFunction(value) {
        let valueDate = new Date(value);
        return value != null ? valueDate.getFullYear() + '-' + super.getTwoDigitsValue(valueDate.getMonth() + 1) + '-' +
            super.getTwoDigitsValue(valueDate.getDate()) + 'T' + valueDate.getHours() + ':' + super.getTwoDigitsValue(valueDate.getMinutes()) : null;
    }
}