function checkBox() {
    return _defaultRadioCheckBox('checkbox');
}

function radio() {
    return _defaultRadioCheckBox('radio', true);
}

function toggleButton() {
    return _checkBoxRadioButtonToggle('checkbox');
}

function radioToggleButton() {
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
    let isDefaultView = !isGroup && divClasses != null; // true if standard checkBox or radiobutton
    let name = hasName ? _getRandomId(type) : null; // radiobutton must have a name attribute

    function _getRandomId(postfix) {
        return Math.random().toString(36).slice(2) + '_' + postfix;
    }

    function _getOptionElement(options, index, isInput, isInner) {
        if (isDefaultView) {
            let option = options.children[index];
            return isInner ? isInput ? option.firstChild : option.lastChild : option;
        } else {
            return options.children[(index * 2) + (isInput ? 0 : 1)]
        }
    }

    function _setSelected(input, rawOption) {
        if (rawOption.selected) {
            input.checked = true;
            input.classList.add("active");
        } else {
            input.checked = false;
            input.classList.remove("active");
        }
    }

    function _createStandaloneOptionDivElements(input, label) {
        let div = document.createElement('div');
        divClasses.forEach(divClass => div.classList.add(divClass));
        div.appendChild(input);
        div.appendChild(label);
        return div;
    }

    function _changeProperty(controller, key, selected, event) {
        controller.changeProperty('selected', key, !selected ? true : null);
        controller.changeObject(key);

        if (event != null)
            event.preventDefault(); // is needed to prevent the click event reaching the input element.
    }

    return {
        render: function (element, controller) {
            let options = document.createElement('div');
            options.classList.add(isDefaultView ? "option-container" : "option-btn-container");
            if (isGroup) {
                options.setAttribute("role", "group");
                if (divClasses != null)
                    divClasses.forEach(divClass => options.classList.add(divClass));
            }

            element.appendChild(options);
            element.options = options;
        }, update: function (element, controller, list) {
            let options = element.options;

            let isList;
            let diff;
            if (controller.getDiff !== undefined) {
                diff = controller.getDiff(list, true);
                isList = true;

                diff.remove.forEach(rawOption => {
                    if (isDefaultView) {
                        options.removeChild(_getOptionElement(options, rawOption.index, false, false));
                    } else {
                        options.removeChild(_getOptionElement(options, rawOption.index, false, false));
                        options.removeChild(_getOptionElement(options, rawOption.index, true, false));
                    }
                });
            } else {
                if (typeof list === 'string') {
                    list = list.split(",").map((value, index) => {
                        return {name: value, index: index, selected: false};
                    })
                } else if (list == null) {
                    list = [];
                }
                diff = {add: list, update: []};
                isList = false;

                options.innerText = ""; // removing all children
            }

            diff.add.forEach(rawOption => {
                let input = document.createElement('input');
                inputClasses.forEach(inputClass => input.classList.add(inputClass));
                input.type = type;
                input.id = _getRandomId(rawOption.name);
                input.setAttribute("autocomplete", 'off');

                if (name != null)
                    input.setAttribute('name', name);

                _setSelected(input, rawOption);

                let label = document.createElement('label');
                labelClasses.forEach(labelClass => label.classList.add(labelClass));
                label.setAttribute('for', input.id);
                label.innerText = rawOption.name;
                label.selected = rawOption.selected

                if (isList) {
                    label.key = rawOption;
                    label.addEventListener('click', function (event) {
                        let input = this.previousElementSibling;
                        if (!(input.type === 'radio' && input.checked)) // so that the "checked" attribute is not removed from the radiobutton. because in _setSelected we directly affect the checked attribute
                            _changeProperty(controller, this.key, this.selected, event);
                    });


                    //todo is only used for the standard representation of radiobutton and checkbox
                    if (isDefaultView) {
                        input.addEventListener('click', function () {
                            let label = this.nextSibling;
                            if(!(label.selected && this.checked))
                                _changeProperty(controller, label.key, label.selected);
                        });
                    }
                }

                let currentOptions = options.children;
                let append = rawOption.index === (isDefaultView ? currentOptions.length : currentOptions.length / 2);
                if (isDefaultView) {
                    if (append)
                        options.appendChild(_createStandaloneOptionDivElements(input, label));
                    else
                        options.insertBefore(_createStandaloneOptionDivElements(input, label), currentOptions[rawOption.index]);
                } else {
                    if (append) {
                        options.appendChild(input);
                        options.appendChild(label);
                    } else {
                        options.insertBefore(input, currentOptions[rawOption.index * 2]);
                        options.insertBefore(label, currentOptions[(rawOption.index * 2) + 1]);
                    }
                }
            });

            diff.update.forEach(rawOption => {
                let id = _getRandomId(rawOption.name);

                let label = _getOptionElement(options, rawOption.index, false, true);
                label.innerText = rawOption.name;
                label.selected = rawOption.selected;
                label.setAttribute('for', id);

                let input = _getOptionElement(options, rawOption.index, true, true);
                input.id = id;

                _setSelected(input, rawOption);
            });

            if (isList)
                Array.from(options.children).forEach(label => {
                    if (label.key != null && controller.isCurrent(label.key))
                        label.classList.add("option-item-current");
                    else
                        label.classList.remove("option-item-current");
                });
        }
    }
}