function check() {
    return _defaultRadioCheckBox('checkbox');
}

function radio() {
    return _defaultRadioCheckBox('radio', true);
}

function checkButton() {
    return _checkBoxRadioButtonToggle('checkbox');
}

function radioButton() {
    return _checkBoxRadioButtonToggle('radio', true);
}

function checkButtonGroup() {
    return _checkBoxRadioButtonGroup('checkbox');
}

function radioButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', true);
}

function _defaultRadioCheckBox(type, hasName) {
    return _option(type, false, ['form-check'], ['form-check-input'], ['form-check-label', 'option-item'], hasName);
}

function _checkBoxRadioButtonToggle(type, hasName) {
    return _option(type, false, null, ['btn-check'], ['btn', 'btn-outline-primary', 'option-item'], hasName);
}

function _checkBoxRadioButtonGroup(type, hasName) {
    return _option(type, true, ['btn-group'], ['btn-check'], ['btn', 'btn-outline-primary', 'option-item'], hasName);
}

function _option(type, isGroup, divClasses, inputClasses, labelClasses, hasName) {
    let isButton = isGroup || divClasses == null;
    let name = hasName ? _getRandomId() : null; // radiobutton must have a name attribute

    function _getRandomId() {
        return Math.random().toString(36).slice(2);
    }

    /* if isInnerElement==true this is <label> or <input> element.
    * if isInnerElement==false this is <div> element*/
    function _getOptionElement(options, index, isInput, isInnerElement) {
        if (isButton) {
            return options.children[(index * 2) + (isInput ? 0 : 1)]
        } else {
            let option = options.children[index];
            return isInnerElement ? isInput ? option.firstChild : option.lastChild : option;
        }
    }

    function _setChecked(input, rawOption) {
        input.checked = rawOption.selected;
    }

    function _changeProperty(controller, key, selected, isList, event) {
        controller.changeProperty('selected', key, selected ? true : null);
        if (isList)
            controller.changeObject(key);

        if (event != null)
            event.preventDefault(); // is needed to prevent the click event reaching the input element.
    }

    return {
        render: function (element) {
            let options = document.createElement('div');
            options.classList.add(isButton ? "option-btn-container" : "option-container");
            if (isGroup) {
                options.setAttribute("role", "group");
                if (divClasses != null)
                    divClasses.forEach(divClass => options.classList.add(divClass));
            }

            element.appendChild(options);
            element.options = options;
        }, update: function (element, controller, list) {
            let options = element.options;

            let isList = controller.isList();
            let diff = controller.getDiff(list, true);

            diff.remove.forEach(rawOption => {
                if (isButton) {
                    options.removeChild(_getOptionElement(options, rawOption.index, false, false));
                    options.removeChild(_getOptionElement(options, rawOption.index, true, false));
                } else {
                    options.removeChild(_getOptionElement(options, rawOption.index, false, false));
                }
            });

            diff.add.forEach(rawOption => {
                let input = document.createElement('input');
                inputClasses.forEach(inputClass => input.classList.add(inputClass));
                input.type = type;
                input.id = _getRandomId();
                input.key = rawOption;
                input.setAttribute("autocomplete", 'off');

                if (name != null)
                    input.setAttribute('name', name);

                _setChecked(input, rawOption);

                let label = document.createElement('label');
                labelClasses.forEach(labelClass => label.classList.add(labelClass));
                label.setAttribute('for', input.id);
                label.innerText = rawOption.name;

                input.addEventListener('change', function () {
                    _changeProperty(controller, this.key, this.checked, isList);
                });

                let currentOptions = options.children;
                let append = rawOption.index === (isButton ? currentOptions.length / 2 : currentOptions.length);
                if (isButton) {
                    if (append) {
                        options.appendChild(input);
                        options.appendChild(label);
                    } else {
                        options.insertBefore(input, currentOptions[rawOption.index * 2]);
                        options.insertBefore(label, currentOptions[(rawOption.index * 2) + 1]);
                    }
                } else {
                    let div = document.createElement('div');
                    divClasses.forEach(divClass => div.classList.add(divClass));
                    div.appendChild(input);
                    div.appendChild(label);

                    if (append)
                        options.appendChild(div);
                    else
                        options.insertBefore(div, currentOptions[rawOption.index]);
                }
            });

            diff.update.forEach(rawOption => {
                let label = _getOptionElement(options, rawOption.index, false, true);
                label.innerText = rawOption.name;

                let input = _getOptionElement(options, rawOption.index, true, true);
                _setChecked(input, rawOption);
            });

            if (isList)
                Array.from(list).forEach(element => {
                    let option = _getOptionElement(options, element.index, true, true);
                    option.classList[controller.isCurrent(option.key) ? 'add' : 'remove']('option-item-current');
                });
        }
    }
}