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
            Selectize.define('append_item', function() {
                var self = this.addItem;
                this.addItem = function (value, silent) {
                    if (!element.silent)
                        this.setCaret(this.items.length);

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

                        element.controller.changeProperty('selected', option.originalObject, true, "add"); // ?? maybe it is possible to get caret pos and in that case we won't need add_item plugin

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
                plugins: ['remove_button', 'auto_position', 'append_item']
            });

            let selectizeInstance = element.selectizeInstance[0].selectize;
            selectizeInstance.$control_input[0].addEventListener('keydown', function (e) {
                //End editing by pressing shift+enter
                if (e.shiftKey === true && e.key === 'Enter') {
                    selectizeInstance.close();
                    selectizeInstance.blur();
                }

                if (e.key === 'Escape') {
                    selectizeInstance.blur();
                }
            });
        },
        update: function (element, controller, list) {
            element.silent = true; // onItemAdd / Remove somewhy is not silenced

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
            element.controller = controller;

            // assert that all items are selected
//            list = list.filter((item) => item.selected);

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
                        if(type === 'add')
                            selectizeInstance.addOption(selectizeOption);
                        else
                            selectizeInstance.updateOption(optionValue, selectizeOption);

                        selectizeInstance.setCaret(index);
                        selectizeInstance.addItem(optionValue, true);
                }
            }, true, true);

            element.silent = false;
        },
        clear: function (element) {
            element.selectizeInstance[0].selectize.destroy();
        }
    }
}

// todo --- backward compatibility. option() should be removed in future releases ---
function option() {
    return checkButton();
}

function selectMulti() {
    return _defaultRadioCheckBox('checkbox');
}

function select() {
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
    return _option(type, false, null, ['btn-check'], ['btn', 'btn-outline-secondary', 'option-item'], hasName);
}

function _checkBoxRadioButtonGroup(type, hasName) {
    return _option(type, true, ['btn-group'], ['btn-check'], ['btn', 'btn-outline-secondary', 'option-item'], hasName);
}

function _option(type, isGroup, divClasses, inputClasses, labelClasses, hasName) {
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
            if(changed.dropAndNotSetChecked) {
                for (let i = 0; i < list.length; i++) {
                    let object = list[i];
                    if(object.selected != null && object.selected) {
                        let input = _getOptionElement(options, i, true, true);
                        input.checked = true;
                        break;
                    }
                }
            }

            for (let i = 0; i < list.length; i++){
                let input = _getOptionElement(options, i, true, true);
                let readonly;
                if (isList) {
                    input.classList[controller.isCurrent(input.object) ? 'add' : 'remove']('option-item-current');
                    readonly = controller.isPropertyReadOnly('selected', input.object);
                } else {
                    readonly = controller.isReadOnly();
                }
                if(readonly)
                    input.setAttribute('onclick', 'return false');
                else
                    input.removeAttribute('onclick')
            }
        }
    }
}