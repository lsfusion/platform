
var lsf_events_defined = false;

// input with drop down
function handleInputKeyEvent(isOpen, controller, e, keyDown) {
    if(isOpen) { // is editing
        if(controller.isEditInputKeyEvent(e, true) || (keyDown && (e.key === 'Enter' || e.key === 'Escape')))
            e.stopPropagation()
    } else {
        if(controller.isRenderInputKeyEvent(e, true))
            e.stopPropagation();
    }
}
function handleDropdownKeyEvent(isOpen, e, keyDown) {
    if(isOpen) { // is editing
        if(keyDown && (e.keyCode === 38 || e.keyCode === 40 || e.key === 'Enter' || e.key === 'Escape'))
            e.stopPropagation()
    }
    if(e.keyCode === 32)
        e.stopPropagation();
}

function handleOptionKeyEvent(isButton, e, keyDown, isInGrid) {
    if (keyDown && e.shiftKey && isInGrid && ((isButton && (e.keyCode === 39 || e.keyCode === 37)) || (e.keyCode === 40 || e.keyCode === 38)))
        e.stopPropagation();
}

function selectMultiHTMLInput() {
    return selectMultiInput(); // check how it will work
}

function selectMultiInput() {
    function handleSelectizeKeyEvent(selectize, e, keyDown) {
       handleInputKeyEvent(selectize.isOpen, selectize.controller, e, keyDown);
    }

    if(!lsf_events_defined) {
        Selectize.define('lsf_events', function () {
            let selfKeyDown = this.onKeyDown;
            this.onKeyDown = function (e) {
                handleSelectizeKeyEvent(this, e, true);

                // we're copying suggest + multi line text event handling
                if (e.shiftKey === true && e.key === 'Enter') {
                    this.close();
                    return;
                }

                selfKeyDown.apply(this, arguments);
            }
            let selfKeyPress = this.onKeyPress;
            this.onKeyPress = function (e) {
                handleSelectizeKeyEvent(this, e, false);

                selfKeyPress.apply(this, arguments);
            }
            this.onItemSelect = function (e) {
            }
        });
        lsf_events_defined = true;
    }

    function toOption(object, controller, loaded) {
        return {
            value: controller.getObjectsString(object),
            text: _getName(object),
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
            let isList = controller.isList();

            let selectizeElement = _wrapElementDiv(element, !isList); // it seems that selectize uses parent (because it creates sibling) so for custom cell renderer we need extra div

            // tabindex = -1 to disable "tab" button in the table.
            // selectize at initialisation contains a check for the tabindex attribute of the parent element:
            // if attribute is present, it will be used, if the attribute is not present, then tabindex will be = 0 for all input elements inside selectize component
            selectizeElement.setAttribute('tabindex', controller.isTabFocusable() ? '0' : '-1');

            element.selectizeInstance = $(selectizeElement).selectize({
                dropdownParent: 'body',

                onInitialize: function() {
                    _removeAllPMBInTD(element, this.$control[0]);
                },
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

                        // removing option will make it not possible to select this option once again (it will be removed from the list which is not we want)
                        // this.removeOption(value, true);

                        element.controller.changeProperty('selected', originalObject, null, "remove");
                    }
                },
                onDropdownOpen: function () {
                    // setting auto hide partner to avoid fake blurs
                    this.setCaret(this.items.length);
                    _setIsEditing(element, this.$control[0], true);
                },
                onDropdownClose: function () {
                    _setIsEditing(element, this.$control[0], false);
                },
                openOnFocus: function () {
                    return !lsfUtils.isSuppressOnFocusChange(element);
                },
                respect_word_boundaries: false, // undocumented feature. But for some reason it includes support for searching by Cyrillic characters
                preload: 'focus',
                selectOnTab: false,
                loadThrottle: 0,
                load: function (query, callback) {
                    let self = this;

                    let controller = element.controller;
                    controller.getPropertyValues('name', query, (data) => {
                        clearSearchCaches(self);
                        let options = [];
                        for (let dataElement of data.data)
                            options.push(toOption(controller.createObject({selected : false, name : dataElement.rawString}, dataElement.objects), controller, true));
                        callback(options);
                    }, null, this.items.length);
                },
                plugins: ['remove_button', 'auto_position', 'lsf_events'],
                render: {
                    item: function (item, escape) {
                        return "<div class = 'item'>" + item.text + "</div>";
                    },
                    option: function (item, escape) {
                        return '<div>' + item.text + '</div>'
                    }
                }
            });

            // we need to do it here, not in update to have relevant focusElement
            let selectizeInstance = element.selectizeInstance[0].selectize;
            selectizeInstance.controller = controller; // needed for lsf_events
            selectizeInstance.isInGrid = _isInGrid(element);
            lsfUtils.setFocusElement(element, selectizeInstance.$control_input[0]);
            if(!isList)
                lsfUtils.setReadonlyFnc(element, (readonly) => {
                    if(readonly != null) {
                        if (readonly) {
                            selectizeInstance.disable();
                        } else {
                            selectizeInstance.enable();
                            selectizeInstance.lock();
                        }
                    } else {
                        selectizeInstance.enable();
                    }
                });

            lsfUtils.setOnFocusOutWithDropDownPartner(!isList ? element : selectizeInstance.$control[0], selectizeInstance.$dropdown[0], function(e) {
                selectizeInstance.blur();
            });
        },
        update: function (element, controller, list, extraValue) {
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
            selectizeInstance.controller = controller; // needed for lsf_events
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

            selectizeInstance.settings.placeholder = extraValue != null ? extraValue.placeholder : null;
            selectizeInstance.updatePlaceholder();

            element.silent = false;
        },
        clear: function (element, controller) {
            lsfUtils.clearFocusElement(element);
            lsfUtils.clearReadonlyFnc(element); // !isList check should be here

            controller.clearDiff();

            if(!controller.isList())
                lsfUtils.removeOnFocusOut(element);

            element.selectizeInstance[0].selectize.destroy();
        }
    }
}

function selectList() {
    return _defaultRadioCheckBox('radio', true, true, false, false, false);
}

function selectValueList() {
    return _defaultRadioCheckBox('radio', true, true, false, false, true);
}

function selectHTMLList() {
    return _defaultRadioCheckBox('radio', true, true, false, true, false);
}

function selectHTMLValueList() {
    return _defaultRadioCheckBox('radio', true, true, false, true, true);
}

function selectButton() {
    return _checkBoxRadioButtonToggle('radio', true, true, false, false, false);
}

function selectValueButton() {
    return _checkBoxRadioButtonToggle('radio', true, true, false, false, true);
}

function selectHTMLButton() {
    return _checkBoxRadioButtonToggle('radio', true, true, false, true, false);
}

function selectHTMLValueButton() {
    return _checkBoxRadioButtonToggle('radio', true, true, false, true, true);
}

function selectButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', true, true, false, false, false);
}

function selectValueButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', true, true, false, false, true);
}

function selectHTMLButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', true, true, false, true, false);
}

function selectHTMLValueButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', true, true, false, true, true);
}

function selectMultiList() {
    return _defaultRadioCheckBox('checkbox', false, false, true, false, false);
}

function selectMultiHTMLList() {
    return _defaultRadioCheckBox('checkbox', false, false, true, true, false);
}

function selectMultiButton() {
    return _checkBoxRadioButtonToggle('checkbox', false, false, true, false, false);
}

function selectMultiHTMLButton() {
    return _checkBoxRadioButtonToggle('checkbox', false, false, true, true, false);
}

function selectMultiButtonGroup() {
    return _checkBoxRadioButtonGroup('checkbox', false, false, true, false, false);
}

function selectMultiHTMLButtonGroup() {
    return _checkBoxRadioButtonGroup('checkbox', false, false, true, true, false);
}

function selectNullList() {
    return _defaultRadioCheckBox('radio', false, true, false, false, false);
}

function selectNullValueList() {
    return _defaultRadioCheckBox('radio', false, true, false, false, true);
}

function selectNullHTMLList() {
    return _defaultRadioCheckBox('radio', false, true, false, true, false);
}

function selectNullHTMLValueList() {
    return _defaultRadioCheckBox('radio', false, true, false, true, true);
}

function selectNullButton() {
    return _checkBoxRadioButtonToggle('radio', false, true, false, false, false);
}

function selectNullValueButton() {
    return _checkBoxRadioButtonToggle('radio', false, true, false, false, true);
}

function selectNullHTMLButton() {
    return _checkBoxRadioButtonToggle('radio', false, true, false, true, false);
}

function selectNullHTMLValueButton() {
    return _checkBoxRadioButtonToggle('radio', false, true, false, true, true);
}

function selectNullButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', false, true, false, false, false);
}

function selectNullValueButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', false, true, false, false, true);
}

function selectNullHTMLButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', false, true, false, true, false);
}

function selectNullHTMLValueButtonGroup() {
    return _checkBoxRadioButtonGroup('radio', false, true, false, true, true);
}

function _defaultRadioCheckBox(type, shouldBeSelected, hasName, multi, html, changeValue) {
    return _option(type, false, ['form-check'], ['form-check-input'], ['form-check-label', 'option-item'], shouldBeSelected, hasName, multi, html, changeValue);
}

function _checkBoxRadioButtonToggle(type, shouldBeSelected, hasName, multi, html, changeValue) {
    return _option(type, false, null, ['btn-check'], ['btn', 'btn-outline-secondary', 'option-item'], shouldBeSelected, hasName, multi, html, changeValue);
}

function _checkBoxRadioButtonGroup(type, shouldBeSelected, hasName, multi, html, changeValue) {
    return _option(type, true, ['btn-group'], ['btn-check'], ['btn', 'btn-outline-secondary', 'option-item'], shouldBeSelected, hasName, multi, html, changeValue);
}

function _wrapElementDiv(element, wrap) {
    return _wrapElement(element, () => document.createElement('div'), wrap)
}
function _wrapElement(element, createElement, wrap) {
    let wrapElement = element;
    if(wrap) {
        wrapElement = createElement();
        wrapElement.classList.add("fill-parent-perc")
        element.appendChild(wrapElement);
    }
    return wrapElement;
}

function _removeAllPMBInTD(element, controlElement) {
    if(_isInGrid(element))
        lsfUtils.removeAllPMB(element, controlElement);
}

function _isInGrid(element) {
    return lsfUtils.isTDorTH(element); // because canBeRenderedInTD can be true
}

function _setIsEditing(element, controlElement, add) {
    lsfUtils.setIsEditing(element, controlElement, add);
}
function _isEditing(element, controlElement) {
    return lsfUtils.isEditing(element, controlElement);
}

// buttons / radios / selects
function _option(type, isGroup, divClasses, inputClasses, labelClasses, shouldBeSelected, hasName, multi, html, changeValue) {
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
        render: function (element, controller) {
            let isList = controller.isList();

            let options = _wrapElementDiv(element, isButton);

            element.options = options;

            element.name = hasName ? _getRandomId() : null; // radiobutton must have a name attribute

            options.classList.add(isButton ? "option-btn-container" : "option-container");
            if (isGroup) {
                options.setAttribute("role", "group");
                if (divClasses != null)
                    divClasses.forEach(divClass => options.classList.add(divClass));
            }

            lsfUtils.setFocusElement(element, null);
            if(!isList)
                lsfUtils.setReadonlyFnc(element, null);

            // allow to navigate in custom button group component in grid cell by pressed shift + left / right or shift + up / down
            // for some reason it doesn't work with ctrl button
            let isInGrid = _isInGrid(element);

            options.addEventListener('keydown', function (e) {
                handleOptionKeyEvent(isButton, e, true, isInGrid);
            });

        }, update: function (element, controller, list, extraValue) {
            let isList = controller.isList();
            list = _convertList(isList, list);
            // need this dropAndNotSetChecked to properly handle situations when there are 2 selected in non-multi select
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
                            input = createFocusElement('input');
                            if(controller.isTabFocusable())
                                input.tabIndex = 0;
                            inputClasses.forEach(inputClass => input.classList.add(inputClass));
                            input.type = type;
                            input.id = _getRandomId();
                            input.setAttribute("autocomplete", 'off');

                            input.setAttribute('name', element.name);

                            label = document.createElement('label');
                            labelClasses.forEach(labelClass => label.classList.add(labelClass));
                            label.setAttribute('for', input.id);

                            input.addEventListener('click', function () {
                                let object = this.object;
                                let set;
                                if (!multi && !shouldBeSelected && element.prevSelected === object)
                                    set = false;
                                else
                                    set = this.checked;

                                if(!multi) {
                                    element.prevSelected = set ? object : null;
                                    if(!set) // radio button won't uncheck itself
                                        this.checked = false;
                                }
                                if(multi)
                                    controller.changeProperty('selected', object, set ? true : null);
                                else
                                    _changeSingleProperty(controller, object, set, changeValue);
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
                        _setSelectName(label, _getName(object), html);
                        // we can't use required for styling, because we want "live validation" and not on submit
                        // input.required = shouldBeSelected;

                        let checked = object.selected;
                        if(!multi) {
                            if (checked) {
                                changed.dropAndNotSetChecked = false;
                                element.prevSelected = object;
                            } else if (input.checked)
                                changed.dropAndNotSetChecked = true;
                        }
                        input.checked = checked;
                        break;
                }
            });

            // if we dropped (and not set) checked and there are other selected elements - select them
            let hasSelected = false;
            if (!multi && (changed.dropAndNotSetChecked || shouldBeSelected)) {
                for (let i = 0; i < list.length; i++) {
                    let object = list[i];
                    if (object.selected != null && object.selected) {
                        if(changed.dropAndNotSetChecked) {
                            let input = _getOptionElement(options, i, true, true);
                            input.checked = true;
                            element.prevSelected = object;
                        }
                        hasSelected = true;
                        break;
                    }
                }
                if(changed.dropAndNotSetChecked && !hasSelected)
                    element.prevSelected = null;
            }

            let focusInput = null;
            for (let i = 0; i < list.length; i++) {
                let input = _getOptionElement(options, i, true, true);
                if (isList) {
                    if (controller.isCurrent(input.object))
                        input.classList.add('option-item-current');
                    else
                        input.classList.remove('option-item-current');

                    _setGroupListReadonly(input, controller, input.object);
                } else {
                    _setGroupReadonly(input, extraValue != null ? extraValue.readonly : null);

                    // fixes the problem of always setting the focus on the first select option in the grid cell
                    if(list[i].selected)
                        focusInput = input;
                }

                if (!multi && shouldBeSelected) {
                    if (isButton) { // bootstrap doesn't support is-invalid for buttons
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
            lsfUtils.setFocusElement(element, focusInput);
        },
        clear: function (element, controller) {
            lsfUtils.clearFocusElement(element);

            controller.clearDiff();
        }
    }
}

// todo --- backward compatibility. option() should be removed in future releases ---
function option() {
    return selectNullButton();
}

function selectNullDropdown() {
    return _selectDropdown(false, false);
}

function selectNullValueDropdown() {
    return _selectDropdown(false, true);
}

function selectDropdown() {
    return _selectDropdown(true, false);
}

function selectValueDropdown() {
    return _selectDropdown(true, true);
}

function selectMultiDropdown() {
    return _selectPicker(true, false, false, false);
}

function selectNullHTMLDropdown() {
    return _selectPicker(false, true, false, false);
}

function selectNullHTMLValueDropdown() {
    return _selectPicker(false, true, false, true);
}

function selectHTMLDropdown() {
    return _selectPicker(false, true, true, false);
}

function selectHTMLValueDropdown() {
    return _selectPicker(false, true, true, true);
}

function selectMultiHTMLDropdown() {
    return _selectPicker(true, true, false, false);
}

function _selectPicker(multi, html, shouldBeSelected, changeValue) {
    if (lsfUtils.useBootstrap()) { //check if bootstrap loaded
        function formatState (state) {
            if (!html) {
                let id = state.id;
                return id == null ? state.text : id;
            } else {
                let element = state.element;
                if (element != null) { // element may be null because select2 draws an empty option at the top of options.
                    let content = element.getAttribute('data-content');
                    let $content = $(content);
                    return $content.length > 0 ? $content : content; //$content.length > 0 is content validity check
                }
                return '';
            }
        }

        function changeProperty(e, element, value) {
            let object = e.params.data.element.object;
            if (multi)
                element.controller.changeProperty('selected', object, value);
            else
                _changeSingleDropdownProperty(object, element, changeValue)
        }

        return _dropDown({}, (element) => {
                let select = element.select;
                let selectElement = $(select);
                selectElement.select2({
                    theme: "bootstrap-5",
                    multiple: multi,
                    closeOnSelect: !multi,
                    width: '100%',
                    templateResult: formatState, // html support
                    templateSelection: formatState,
                    selectionCssClass: ':all:' // move css-classes into select2 :button"
                });

                selectElement.on('select2:select', function (e) {
                    changeProperty(e, element, true);
                });

                selectElement.on('select2:unselect', function (e) {
                    changeProperty(e, element, false);
                });

                let select2 = selectElement.data('select2');

                let containerElement = select2.$container;

                lsfUtils.setOnFocusOutWithDropDownPartner(element, select2.$results[0], function() {
                    selectElement.select2('close');
                });

                if (!_isInGrid(element))
                    containerElement.addClass('form-control');

                selectElement.hide(); // there is a bug that select2 uses position absolute 0 to hide the element and exclude it from the layouting, but there is no position relative so it can stick to any container
                containerElement.css('height', '100%'); //height 100% is needed to keep a cell clickable if no options are selected.

                let selectionElement = select2.$selection;
                let selection = selectionElement[0]

                selectElement.on('select2:open', function () {
                    _setIsEditing(element, selection, true);
                });

                selectElement.on('select2:close', function () {
                    // The timeout is needed because select2:close is triggered before key handler,
                    // and pressing escape closes the whole form, not the dropdown
                    setTimeout(() => _setIsEditing(element, selection, false));
                });

                return selection;
            },
            multi, shouldBeSelected, html, true, true);
    } else {
        return _dropDown(multi ? {'multiple': ''} : {},
            (element) => {
                let select = element.select;
                let selectElement = $(select);
                selectElement.multipleSelect({
                    container: 'body',
                    selectAll: false,
                    position: 'bottom', // todo. bottom is default, but at the bottom of the screen dropdown is hidden and need to be 'top'
                    // position: 'top',
                    onClick: function (view) {
                        let selectedOption = Array.from(select.options).find(option => option.value === view.value);
                        let object = selectedOption.object;
                        if (multi)
                            element.controller.changeProperty('selected', object, view.selected ? true : null);
                        else
                            _changeSingleDropdownProperty(object, element, changeValue)
                    },
                    onOpen: function () {
                        // needed here because refresh recreates dropdown
                        lsfUtils.addDropDownPartner(element, selectElement.multipleSelect('getDropdown')[0]);
                        element.silent = true; // Because "refresh" is called after every update, which removes the dropdown
                        _setIsEditing(element, select, true);
                    },
                    onClose: function () {
                        element.silent = false;// Because "refresh" is called after every update, which removes the dropdown
                        _setIsEditing(element, select, false);
                    }
                });
                lsfUtils.setOnFocusOut(element, function() {
                    selectElement.multipleSelect('close');
                });
                select.addEventListener('keypress', function (e) {
                    if (e.keyCode === 32)
                        selectElement.multipleSelect('open');
                });

                return select;
            }, multi, shouldBeSelected, html, false, true);
    }
}

function _selectDropdown(shouldBeSelected, changeValue) {
    return _dropDown({},
        (element) => {
            let select = element.select;

            select.classList.add("form-select");

            select.addEventListener('keydown', function (e) {
                if (e.keyCode === 32)
                    _setIsEditing(element, select, true);
                else if (e.key === 'Escape')
                   _setIsEditing(element, select, false);
            });

            select.addEventListener('change', function () {
                _changeSingleDropdownProperty(this.selectedOptions[0].object, element, changeValue);

                _setIsEditing(element, select, false);
            })

            select.addEventListener('blur', function () {
                _setIsEditing(element, select, false);
            });

            select.addEventListener('mousedown', function (e) {
                // dropdown can be prevented if !isChangeOnSingleClick
                // if(!element.controller.previewEvent(select, e))
                //     return;
                setTimeout(function() {
                    if(!e.defaultPrevented)
                        _setIsEditing(element, select, !_isEditing(element, select));
                });
            });

            return select;
        }, false, shouldBeSelected);
}

function _setDropdownName(option, name, html, isBootstrap) {
    if(html && isBootstrap)
        option.setAttribute("data-content", name);
    else
        _setSelectName(option, name, html);
}

function _setSelectName(option, name, html) {
    setDataHtmlOrText(option, name, html);
}

function _dropDown(selectAttributes, render, multi, shouldBeSelected, html, isBootstrap, picker) {
    return {
        render: function (element, controller) {
            let isList = controller.isList();

            let select = _wrapElement(element, () => createFocusElement('select'), element.tagName.toLowerCase() !== 'select');
            element.select = select;

            Object.keys(selectAttributes).forEach(key => select.setAttribute(key, selectAttributes[key]));

            // Because there is no default way to reset the value of the drop-down list to null, we must create a null-option
            // if there are no selected options, the selected option becomes a null-option.
            // It is also necessary to make offset when searching by index
            if (!multi) {
                let option = document.createElement('option');
                option.hidden = shouldBeSelected;
                if(!picker)
                    option.classList.add("option-null")
                select.appendChild(option);
            }

            let mainElement = render(element);

            _removeAllPMBInTD(element, mainElement);

            lsfUtils.setFocusElement(element, mainElement);
            if(!isList) {
                lsfUtils.setReadonlyFnc(element, (readonly) => {
                    _setSelectReadonly(mainElement, readonly);
                });
            }

            mainElement.addEventListener('keydown', function (e) {
                handleDropdownKeyEvent(_isEditing(element, mainElement), e, true);
            });

            // press on space button on dropdown element in grid-cell opens dropdown instead of adding a filter.
            mainElement.addEventListener('keypress', function (e) {
                handleDropdownKeyEvent(_isEditing(element, mainElement), e, false);
            });
        },
        update: function (element, controller, list, extraValue) {
            element.controller = controller;
            let isList = controller.isList();
            list = _convertList(isList, list);
            let select = element.select;
            // need this dropAndNotSetChecked to properly handle situations when there are 2 selected in non-multi select
            let changed = { dropAndNotSetChecked : false}
            let offset = multi ? 0 : 1;
            controller.diff(list, (changeType, index, object) => {
                switch(changeType) {
                    case 'remove':
                        select.removeChild(select.children[index + offset]);
                        break;
                    case 'add':
                    case 'update':
                        let option;
                        if(changeType === 'add') {
                            option = document.createElement('option');
                            let currentOptions = select.children;
                            if (index === currentOptions.length + offset)
                                select.appendChild(option);
                            else
                                select.insertBefore(option, currentOptions[index + offset]);

                        } else {
                            option = select.children[index + offset];
                        }

                        option.object = object;
                        _setDropdownName(option, _getName(object), html, isBootstrap);

                        let checked = object.selected;
                        if(!multi) {
                            if (checked) {
                                changed.dropAndNotSetChecked = false;
                                element.prevSelected = object;
                            } else if (option.selected)
                                changed.dropAndNotSetChecked = true;
                        }
                        option.selected = checked;

                        if (!isBootstrap)
                            option.value = controller.getObjectsString(object);
                        else if (!multi && html)
                            option.value = object.name; //bootstrap-select in "singleselect" mode requires that the value field in <option> to be set

                        break;
                }
            });

            let placeholder = extraValue != null ? extraValue.placeholder : null;
            if (!multi) {
                let hasSelected = false;
                for (let i = 0; i < list.length; i++) {
                    let object = list[i];
                    if (object.selected) {
                        if(changed.dropAndNotSetChecked) {
                            select.options[i + offset].selected = true;
                            element.prevSelected = object;
                        }
                        hasSelected = true;
                        break;
                    }
                }
                if(changed.dropAndNotSetChecked && !hasSelected) {
                    select.options[0].selected = true; //select the null option
                    element.prevSelected = null;
                }

                if (shouldBeSelected) {
                    if (hasSelected) {
                        if (picker) {
                            $(select).removeClass("is-invalid");
                            $(select).parent().removeClass("is-invalid"); //for unknown reasons when adding class to $(select) the same class is added to the parent of this element(parent is not an element from render()), but when deleting this class is not deleted
                        } else {
                            select.classList.remove("is-invalid");
                        }
                    } else {
                        if (picker)
                            $(select).addClass("is-invalid");
                        else
                            select.classList.add("is-invalid");
                    }
                }

                if(!picker) {
                    if(!hasSelected)
                        select.classList.add("text-based-value-null")
                    else
                        select.classList.remove("text-based-value-null")
                }

                _setDropdownName(select.options[0], placeholder != null ? placeholder : '', html, isBootstrap)
            } else {
                // assert picker;
                select.setAttribute(isBootstrap ? "title" : "placeholder", placeholder != null ? placeholder : '');
            }

            if (isList) {
                for (let i = 0; i < list.length; i++) {
                    let option = select.options[i + offset];
                    option.setAttribute("readonly", "");
                    _setGroupListReadonly(option, controller, option.object);
                }
            } // else { // we don't need this since we're using the readonlyFnc
            //     _setReadonly(select, controller.isReadOnly());
            // }

            if (picker) {
                if (!isBootstrap) {
                    if (!element.silent)
                        $(select).multipleSelect('refresh'); //Because "refresh" is called after every update, which removes the dropdown
                } else {
                    $(select).trigger('change.select2');
                }
            }
        },
        clear: function (element, controller) {
            lsfUtils.clearFocusElement(element);
            lsfUtils.clearReadonlyFnc(element); // !isList check should be here

            controller.clearDiff();

            lsfUtils.removeOnFocusOut(element);
        }
    }
}

// input in radio group / option in select
function _setGroupListReadonly(element, controller, object) {
    _setGroupReadonly(element, controller.isPropertyReadOnly('selected', object));
}
function _setGroupReadonly(element, readonly) {
    _setReadonly(element, readonly);
}
// select
function _setSelectReadonly(element, readonly) {
    _setReadonly(element, readonly);
}
//
function _setReadonly(element, readonly) {
    // select, option, input supports disabled
    setDisabledNative(element, readonly != null && readonly);
    setReadonlyClass(element, readonly != null && !readonly);
    setReadonlyHeur(element,readonly != null && !readonly);
}

function _convertList(isList, list) {
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
    return list;
}

function _changeSingleDropdownProperty(object, element, changeValue) {
    let set;
    if (!object) {
        object = element.prevSelected;
        if (!object) { /* was null and remained null */
            // assert false;
            return;
        }

        set = false;
    } else
        set = true;

    element.prevSelected = set ? object : null;
    _changeSingleProperty(element.controller, object, set, changeValue);
}

function _changeSingleProperty(controller, object, set, changeValue) {
    controller.changeProperty('selected', object, set ? true : null, undefined, undefined, changeValue ? (set ? _getName(object) : null) : undefined);
}

function _getName(object) {
    return object.name != null ? String(object.name) : '';
}