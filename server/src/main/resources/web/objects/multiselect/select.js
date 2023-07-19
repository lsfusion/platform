function select() {

    function toOption(object, controller) {
        return {
            value: controller.getObjectsString(object),
            text: object.name,
            originalObject: object // should be parsed by getChangeObjects
        }
    }

    function silentExecute(element, functionToExecute) {
        element.silent = true;
        functionToExecute();
        element.silent = false;
    }

    return {
        render: function (element, controller) {
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
                        element.controller.changeProperty('selected', this.options[value].originalObject, true);

                        //Since we have to keep the order of items, and when selecting an item by mouseclick, the order will be set only after update from server,
                        // we use a hack: when selecting an item, mark it for deletion, and in update delete it and put in the right place.
                        this.valueToRemoveInUpdate = value;

                        //When option selected by mouseclick no possibility to continue the search from the keyboard
                        this.$control_input[0].focus();
                    }
                },
                onItemRemove: function (value) {
                    if(!element.silent)
                        element.controller.changeProperty('selected', this.options[value].originalObject, null);

                    // Selectize stores the history of requests inside itself.
                    // And if, for example, you enter "1" in the text field, click on this item, then delete this item, then enter "1" again - the "load" function will not be called.
                    // Probably this is to avoid sending new requests for repeated characters with the expectation that the options will not be deleted, which, probably, does not work for us.
                    // Clearing the request history can help
                    this.loadedSearches = {};
                },
                onDropdownOpen: function (dropdown) {
                    // setting autoHidePartner to avoid fake blurs
                    dropdown[0].autoHidePartner = element;

                    this.$control_input[0].addEventListener('keydown', function (e) {
                        //End editing by pressing shift+enter
                        if (e.shiftKey === true && e.key === 'Enter') {
                            element.selectizeInstance[0].selectize.close();
                            element.selectizeInstance[0].selectize.blur();
                        }

                        if (e.key === 'Escape') {
                            element.selectizeInstance[0].selectize.blur();
                        }
                    });
                },
                respect_word_boundaries: false, // undocumented feature. But for some reason it includes support for searching by Cyrillic characters
                preload: 'focus',
                loadThrottle: 0,
                load: function (query, callback) {
                    let controller = element.controller;
                    controller.getPropertyValues('name', query, (data) => {
                        let options = [];
                        for (let dataElement of data.data)
                            options.push(toOption(controller.createObject({selected : false, name : dataElement.rawString}, dataElement.objects), controller));
                        callback(options);
                    }, null);
                },
                plugins: ['remove_button', 'auto_position']
            });
        },
        update: function (element, controller, list) {
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

            let selectizeInstance = element.selectizeInstance[0].selectize;

            let valueToRemove = selectizeInstance.valueToRemoveInUpdate;
            if (valueToRemove)
                silentExecute(element, () => {
                    selectizeInstance.removeItem(valueToRemove, true);
                    delete selectizeInstance.valueToRemoveInUpdate;
                });

            controller.diff(list, element, (type, index, object) => {
                let selectizeOption = toOption(object, controller);

                //TODO при сортировке если add приходит раньше чем remove, то item просто удаляется, а не изменяет свое место в списке.
                silentExecute(element, () => {
                    switch(type) {
                        case 'remove':
                            selectizeInstance.removeOption(selectizeOption.value, true);
                            break;
                        case 'add':
                        case 'update':
                            if(type === 'add')
                                selectizeInstance.addOption(selectizeOption);
                            else
                                selectizeInstance.updateOption(selectizeOption.value, selectizeOption);

                            if(isList || object.selected) {
                                selectizeInstance.setCaret(index);
                                selectizeInstance.addItem(selectizeOption.value, true);
                            } else
                                selectizeInstance.removeItem(selectizeOption.value, true);
                    }
                });
            }, true)
        },
        clear: function (element) {
            let selectizeInstance = element.selectizeInstance[0].selectize;
            selectizeInstance.destroy();
        }
    }

}