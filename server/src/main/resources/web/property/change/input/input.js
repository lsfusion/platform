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
                if (date.getTime() !== value?.getTime())
                    controller.change(!isNaN(date) ? date : null);
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
                    controller.change(parseInt(this.value));
            }
        }
    }
}

class CustomInputMonth extends CustomInput {
    constructor() {
        super("month");
    }

    parseValueFunction(value) {
        return value != null ? value.getFullYear() + '-' + super.getTwoDigitsValue(value.getMonth() + 1) : null;
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
                    controller.change(this.value);
            }
        };

    }
}

class CustomInputDate extends CustomInput {
    constructor() {
        super("date");
    }

    parseValueFunction(value) {
        return value != null ? value.getFullYear() + "-" + super.getTwoDigitsValue(value.getMonth() + 1) + "-" + super.getTwoDigitsValue(value.getDate()) : null;
    }
}

class CustomInputTime extends CustomInput {
    constructor() {
        super("time");
    }

    //use only hh:mm format. If use hh:mm:ss format, when seconds is 0, then input cut of the seconds part.
    parseValueFunction(value) {
        return value != null ? super.getTwoDigitsValue(value.getHours()) + ':' + super.getTwoDigitsValue(value.getMinutes()) : null;
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onchange = function () {
                let [hours, minutes] = inputElement.value?.split(':').map(Number) || [];
                if (hours == null || minutes == null) {
                    controller.change(null);
                } else {
                    let newTime = new Date(value);
                    newTime.setHours(hours, minutes, 0, 0);

                    if (value?.getHours() !== hours || value?.getMinutes() !== minutes)
                        controller.change(newTime);
                }
            }
        };
    }
}

class CustomInputDateTime extends CustomInput {
    constructor() {
        super("datetime-local");
    }

    parseValueFunction(value) {
        return value != null ? value.getFullYear() + '-' + super.getTwoDigitsValue(value.getMonth() + 1) + '-' +
            super.getTwoDigitsValue(value.getDate()) + 'T' + super.getTwoDigitsValue(value.getHours()) + ':' + super.getTwoDigitsValue(value.getMinutes()) : null;
    }
}