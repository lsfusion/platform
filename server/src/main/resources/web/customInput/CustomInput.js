//range input type. value type = "DOUBLE"
function customInputRange() {
    return new CustomInputRange().getFunctions();
}

//month input type. value type = "DATE". Min width on form ~140px
function customInputMonth() {
    return new CustomInputMonth().getFunctions();
}

//date input type. value type = "DATE". Min width on form ~120px
function customInputDate() {
    return new CustomInputDate().getFunctions();
}

//time input type. value type = "TIME". Min width on form ~90px
function customInputTime() {
    return new CustomInputTime().getFunctions();
}

class CustomInput {
    constructor(type) {
        this.type = type;
    }

    render(type, element) {
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

    update(element, value, onEventFunction, parsedValue) {
        let inputElement = element.lastElementChild;

        if (inputElement != null) {
            inputElement.value = parsedValue != null ? parsedValue : value;

            onEventFunction(inputElement, value);
        }
    }

    clear(element) {
        while (element.lastElementChild) {
            element.removeChild(element.lastElementChild);
        }
    }

    getRenderFunction() {
        return (element) => this.render(this.type, element);
    }

    getUpdateFunction() {
        return (element, controller, value) => this.update(element, value, this.onEventFunction(controller), this.parseValue(value));
    }

    getClearFunction() {
        return (element) => this.clear(element);
    }

    getFunctions() {
        return {
            render: this.getRenderFunction(),
            update: this.getUpdateFunction(),
            clear: this.getClearFunction()
        }
    }

    parseValue(value) {
        return null;
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onblur = function () {
                let date = new Date(this.value);
                if (date.getTime() !== new Date(value).getTime())
                    controller.changeValue(null, !isNaN(date) ? controller.toDateDTO(date.getFullYear(), date.getMonth() + 1, date.getDate()) : null);
            }
        }
    }

}

class CustomInputRange extends CustomInput {
    constructor() {
        super("range");
    }

    getRenderFunction() {
        return (element) => {
            let input = super.render(this.type, element);
            input.min = "1";
            input.max = "100";
        }
    }

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onmouseup = function () {
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

    parseValue(value) {
        let valueDate = new Date(value);
        return value != null ? valueDate.getFullYear() + '-' + ("0" + (valueDate.getMonth() + 1)).slice(-2) : null;
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

    onEventFunction(controller) {
        return (inputElement, value) => {
            inputElement.onblur = function () {
                let timeParts = this.value.split(':');
                if (value.toString() !== this.value)
                    controller.changeValue(null, timeParts.length === 2 ? controller.toTimeDTO(parseInt(timeParts[0], 10), parseInt(timeParts[1], 10), 0) : null);
            }
        };
    }
}