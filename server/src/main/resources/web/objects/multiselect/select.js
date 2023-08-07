function selectMultiInput() {

    function toOption(object, controller, loaded) {
        return {
            value: controller.getObjectsString(object),
            text: object.name,
            originalObject: object, // should be parsed by getChangeObjects
            loaded: loaded //for cache clearing
        }
    }

    function clearSearchCaches(selectize) {
        // Selectize stores the history of requests inside itself.
        // And if, for example, you enter "1" in the text field, click on this item, then delete this item, then enter "1" again - the "load" function will not be called.
        // Probably this is to avoid sending new requests for repeated characters with the expectation that the options will not be deleted, which, probably, does not work for us.
        // Clearing the request history can help
        selectize.loadedSearches = {};

        //clear the all unselected options to fully clear the cache
        for (let [key, value] of Object.entries(selectize.options)) {
            if (value.loaded)
                selectize.removeOption(key, true);
        }
    }

    return {
        render: function (element, controller) {
            Selectize.define('on_key_down', function() {
                var self = this.onKeyDown;
                this.onKeyDown = function (e) {
                    //End editing by pressing shift+enter
                    if (e.shiftKey === true && e.key === 'Enter') {
                        this.close();
                        this.blur();
                    } else if (e.key === 'Escape') { //Esc pressed try to close form. Prevent it and only blur element
                        e.stopPropagation();
                        this.blur();
                    } else
                        self.apply(this, arguments);
                }
            });

            let isList = controller != null;
            let selectizeElement = element;
            if(!isList) { //if is a CustomCellRenderer, there is no controller in the render()
                // it seems that selectize uses parent (because it creates sibling) so for custom cell renderer we need extra div
                selectizeElement = document.createElement('div');
                selectizeElement.classList.add("fill-parent-perc")
                element.appendChild(selectizeElement);
            }

            element.selectizeInstance = $(selectizeElement).selectize({
                dropdownParent: 'body',

                onItemAdd: function (value) {
                    if(!element.silent) {
                        let option = this.options[value];

                        // assert option.loaded;
                        option.loaded = false; // ??? or should be called updateOption

                        //onItemAdd is triggered after the item has been rendered and caret position has moved one ahead,
                        // for changeProperty we need the previous position of caret. for this reason -1
                        element.controller.changeProperty('selected', option.originalObject, true, "add", this.caretPos - 1);

                        //When option selected by mouseclick no possibility to continue the search from the keyboard
                        this.$control_input[0].focus();
                    }
                },
                onItemRemove: function (value) {
                    if(!element.silent) {
                        let originalObject = this.options[value].originalObject;

                        this.removeOption(value, true);

                        element.controller.changeProperty('selected', originalObject, null, "remove");
                    }
                },
                onDropdownOpen: function (dropdown) {
                    // setting autoHidePartner to avoid fake blurs
                    dropdown[0].autoHidePartner = element;
                    this.setCaret(this.items.length);
                },
                respect_word_boundaries: false, // undocumented feature. But for some reason it includes support for searching by Cyrillic characters
                preload: 'focus',
                loadThrottle: 0,
                load: function (query, callback) {
                    clearSearchCaches(this);

                    let controller = element.controller;
                    controller.getPropertyValues('name', query, (data) => {
                        let options = [];
                        for (let dataElement of data.data)
                            options.push(toOption(controller.createObject({selected : false, name : dataElement.rawString}, dataElement.objects), controller, true));
                        callback(options);
                    }, null);
                },
                plugins: ['remove_button', 'auto_position', 'on_key_down']
            });
        },
        update: function (element, controller, list) {
            element.silent = true; // onItemAdd / Remove somewhy is not silenced
            element.controller = controller;

            let isList = controller.isList();
            if (isList) {
                if (!controller.booleanFilterSet && list.length > 0) {
                    controller.setBooleanViewFilter('selected', 1000);
                    controller.booleanFilterSet = true;
                    return;
                }
            } else { // controller is needed in render() to add onItemAdd and onItemRemove listeners. In CustomCellRenderer we cannot pass the controller to render()
                if(list == null)
                    list = [];
            }

            let selectizeInstance = element.selectizeInstance[0].selectize;

            controller.diff(list, (type, index, object) => {
                let selectizeOption = toOption(object, controller, false);
                let optionValue = selectizeOption.value;

                switch(type) {
                    case 'remove':
                        selectizeInstance.removeOption(optionValue, true);
                        break;
                    case 'add':
                    case 'update':
                        //inside selectizeInstance.updateOption if option with value does not exist - do nothing.
                        //we need to update option to set loaded = false for clearSearchCaches
                        selectizeInstance.updateOption(optionValue, selectizeOption);
                        selectizeInstance.addOption(selectizeOption);

                        selectizeInstance.setCaret(index);
                        selectizeInstance.addItem(optionValue, true);
                        selectizeInstance.setCaret(selectizeInstance.items.length);
                }
            }, true, true);

            element.silent = false;
        },
        clear: function (element) {
            element.selectizeInstance[0].selectize.destroy();
        }
    }
}

function selectList() {
    return _defaultRadioCheckBox('radio', true, true);
}

function selectButton() {
    return _checkBoxRadioButtonToggle('radio', true, true);
}

function selectButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', true, true);
}

function selectMultiList() {
    return _defaultRadioCheckBox('checkbox', false);
}

function selectMultiButton() {
    return _checkBoxRadioButtonToggle('checkbox', false);
}

function selectMultiButtonGroup() {
    return _checkBoxRadioButtonGroup('checkbox', false);
}

function selectNullList() {
    return _defaultRadioCheckBox('radio', false, true);
}

function selectNullButton() {
    return _checkBoxRadioButtonToggle('radio', false, true);
}

function selectNullButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', false, true);
}

function _defaultRadioCheckBox(type, shouldBeSelected, hasName) {
    return _option(type, false, ['form-check'], ['form-check-input'], ['form-check-label', 'option-item'], shouldBeSelected, hasName);
}

function _checkBoxRadioButtonToggle(type, shouldBeSelected, hasName) {
    return _option(type, false, null, ['btn-check'], ['btn', 'btn-outline-secondary', 'option-item'], shouldBeSelected, hasName);
}

function _checkBoxRadioButtonGroup(type, shouldBeSelected, hasName) {
    return _option(type, true, ['btn-group'], ['btn-check'], ['btn', 'btn-outline-secondary', 'option-item'], shouldBeSelected, hasName);
}

function _option(type, isGroup, divClasses, inputClasses, labelClasses, shouldBeSelected, hasName) {
    let isButton = isGroup || divClasses == null;

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

    return {
        render: function (element) {
            element.name = hasName ? _getRandomId() : null; // radiobutton must have a name attribute

            let options = document.createElement('div');
            options.classList.add(isButton ? "option-btn-container" : "option-container");
            options.classList.add("fill-parent-perc")
            if (isGroup) {
                options.setAttribute("role", "group");
                if (divClasses != null)
                    divClasses.forEach(divClass => options.classList.add(divClass));
            }

            element.appendChild(options);
            element.options = options;
        }, update: function (element, controller, list) {
            let isList = controller.isList();

            if(!isList) {
                if (typeof list === 'string') {
                    let strings = list.split(",");
                    list = [];
                    for (let i = 0; i < strings.length; i++) {
                        list.push({name: strings[i], selected: false});
                    }
                } else if (list == null) {
                    list = [];
                }
            }

            let changed = { dropAndNotSetChecked : false}
            let options = element.options;
            controller.diff(list, (changeType, index, object) => {
                switch(changeType) {
                    case 'remove': // clear
                        if (isButton) {
                            options.removeChild(_getOptionElement(options, index, false, false));
                            options.removeChild(_getOptionElement(options, index, true, false));
                        } else {
                            options.removeChild(_getOptionElement(options, index, false, false));
                        }
                        break;
                    case 'add': // render and update
                    case 'update': // update
                        let input, label;
                        if(changeType === 'add') {
                            input = document.createElement('input');
                            inputClasses.forEach(inputClass => input.classList.add(inputClass));
                            input.type = type;
                            input.id = _getRandomId();
                            input.setAttribute("autocomplete", 'off');

                            input.setAttribute('name', element.name);

                            label = document.createElement('label');
                            labelClasses.forEach(labelClass => label.classList.add(labelClass));
                            label.setAttribute('for', input.id);

                            input.addEventListener('change', function () {
                                controller.changeProperty('selected', this.object, this.checked ? true : null);
                                if (isList)
                                    controller.changeObject(this.object);
                            });

                            let currentOptions = options.children;
                            let append = index === (isButton ? currentOptions.length / 2 : currentOptions.length);
                            if (isButton) {
                                if (append) {
                                    options.appendChild(input);
                                    options.appendChild(label);
                                } else {
                                    options.insertBefore(input, currentOptions[index * 2]);
                                    options.insertBefore(label, currentOptions[(index * 2) + 1]);
                                }
                            } else {
                                let div = document.createElement('div');
                                divClasses.forEach(divClass => div.classList.add(divClass));
                                div.appendChild(input);
                                div.appendChild(label);

                                if (append)
                                    options.appendChild(div);
                                else
                                    options.insertBefore(div, currentOptions[index]);
                            }
                        } else {
                            input = _getOptionElement(options, index, true, true);
                            label = _getOptionElement(options, index, false, true);
                        }

                        input.object = object;
                        label.innerText = object.name;
                        // we can't use required for styling, because we want "live validation" and not on submit
                        // input.required = shouldBeSelected;

                        let checked = object.selected != null && object.selected;
                        if(checked)
                            changed.dropAndNotSetChecked = false;
                        else
                        if(input.checked)
                            changed.dropAndNotSetChecked = true;
                        input.checked = checked;
                        break;
                }
            });

            // if we dropped (and not set) checked and there are other selected elements - select them
            let hasSelected = false;
            if(changed.dropAndNotSetChecked || shouldBeSelected) {
                for (let i = 0; i < list.length; i++) {
                    let object = list[i];
                    if(object.selected != null && object.selected) {
                        if(changed.dropAndNotSetChecked) {
                            let input = _getOptionElement(options, i, true, true);
                            input.checked = true;
                        }
                        if(shouldBeSelected)
                            hasSelected = true;
                        break;
                    }
                }
            }

            for (let i = 0; i < list.length; i++){
                let input = _getOptionElement(options, i, true, true);
                let readonly;
                if (isList) {
                    if(controller.isCurrent(input.object))
                        input.classList.add('option-item-current');
                    else
                        input.classList.remove('option-item-current');
                    readonly = controller.isPropertyReadOnly('selected', input.object);
                } else {
                    readonly = controller.isReadOnly();
                }
                if(readonly)
                    input.setAttribute('onclick', 'return false');
                else
                    input.removeAttribute('onclick')
                if(shouldBeSelected) {
                    if(isButton) { // bootstrap doesn't support is-invalid for buttons
                        let label = _getOptionElement(options, i, false, true);
                        if (!hasSelected) {
                            label.classList.add("btn-outline-danger");
                            label.classList.remove("btn-outline-secondary");
                        } else {
                            label.classList.add("btn-outline-secondary");
                            label.classList.remove("btn-outline-danger");
                        }
                    } else {
                        if (!hasSelected)
                            input.classList.add("is-invalid");
                        else
                            input.classList.remove("is-invalid");
                    }
                }
            }
        }
    }
}

// todo --- backward compatibility. option() should be removed in future releases ---
function option() {
    return checkButton();
}

function selectNullDropdown() {
    return _selectDropdown((element) => {
        if (element.defaultValue == null) {
            let option = document.createElement('option');
            option.value = null;
            element.select.appendChild(option);
            element.defaultValue = option;
        }
    });
}

function selectDropdown() {
    return _selectDropdown(() => {});
}

function selectMultiDropdown() {
    return _dropDown(['multi-select'],
        {'data-container': 'body', 'multiple': ''},
        (element) => {
            $(element.select).selectpicker();
            $(element.select).on('changed.bs.select', function (e, clickedIndex, isSelected, previousValue) {
            if (clickedIndex != null && isSelected != null) { //documentation:  If the select's value has been changed either via the .selectpicker('val'), .selectpicker('selectAll'), or .selectpicker('deselectAll') methods, clickedIndex and isSelected will be null.
                element.controller.changeProperty('selected', this.children[clickedIndex].originalObject, isSelected ? true : null);
            }
        })},
        () => {},
        (select) => $(select).selectpicker('val', Array.from(select.children).filter(c => c.selected).map(c => c.value)),
        (select) => $(select).selectpicker('refresh'));
}

function _selectDropdown(emptyDefaultValue) {
    return _dropDown(['form-select'],
        {},
        (element) => {
            element.select.addEventListener('change', function () {
                element.changeEvent = true;
                let prevSelected = element.prevList.find(child => child.selected);
                if (prevSelected)
                    element.controller.changeProperty('selected', prevSelected, null);

                let currentSelected = this.selectedOptions[0].originalObject;
                if (currentSelected)
                    element.controller.changeProperty('selected', currentSelected, true);
            })
        },
        emptyDefaultValue,
        (select, element) => {
            if (!element.changeEvent) {
                let find = Array.from(select.children).find(child => child.originalObject && child.originalObject.selected);
                select.value = find ? find.value : null;
            } else {
                element.changeEvent = false;
            }},
        () => {});
}

function _dropDown(cssClasses, selectAttributes, onChange, emptyDefaultValue, selectOption, refresh) {
    return {
        render: function (element, controller) {
            let select = document.createElement('select');
            cssClasses.forEach(cssClass => select.classList.add(cssClass));

            Object.keys(selectAttributes).forEach(key => select.setAttribute(key, selectAttributes[key]));

            element.appendChild(select);
            element.select = select;

            onChange(element);
        },
        update: function (element, controller, list) {
            element.controller = controller;

            let isList = controller.isList();

            if(!isList) {
                if (typeof list === 'string') {
                    let strings = list.split(",");
                    list = [];
                    for (let i = 0; i < strings.length; i++) {
                        list.push({name: strings[i], selected: false});
                    }
                } else if (list == null) {
                    list = [];
                }
            }

            let select = element.select;
            emptyDefaultValue(element);

            controller.diff(list, (changeType, index, object) => {
                switch(changeType) {
                    case 'remove':
                        select.removeChild(select.children[index]);
                        break;
                    case 'add':
                    case 'update':
                        let option;
                        if(changeType === 'add') {
                            option = document.createElement('option');
                            option.value = controller.getObjectsString(object);

                            let currentOptions = select.children;
                            if (index === currentOptions.length)
                                select.appendChild(option);
                            else
                                select.insertBefore(option, currentOptions[index]);

                        } else {
                            option = select.children[index];
                        }

                        option.originalObject = object;
                        option.selected = object.selected;
                        option.innerText = object.name;

                        break;
                }
                refresh(select);
                selectOption(select, element);
            });
        },
        clear: function (element) {

        }
    }
}
