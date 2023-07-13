function select() {

    function toOption(object, controller) {
        return {
            value: controller.getObjectsString(object),
            text: object.name,
            originalObject: object // should be parsed by getChangeObjects
        }
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
                    if(!element.silent)
                        element.controller.changeProperty('selected', this.options[value].originalObject, true);
                },
                onItemRemove: function (value) {
                    if(!element.silent)
                        element.controller.changeProperty('selected', this.options[value].originalObject, null);
                },
                onDropdownOpen: function (dropdown) {
                    // setting autoHidePartner to avoid fake blurs
                    dropdown[0].autoHidePartner = element;
                },
                plugins: ['remove_button']
            });

            let selectizeInstance = element.selectizeInstance[0].selectize;

            selectizeInstance.settings.preload = 'focus';
            selectizeInstance.settings.loadThrottle = 0;
            selectizeInstance.settings.load = function (query, callback) {
                let controller = element.controller;
                controller.getPropertyValues('name', query, (data) => {
                    let options = [];
                    for (let dataElement of data.data)
                        options.push(toOption(controller.createObject({selected : false, name : dataElement.rawString}, dataElement.objects), controller));
                    callback(options);
                }, null);
            }
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
            controller.diff(list, element, (type, index, object) => {
                let selectizeOption = toOption(object, controller);

                element.silent = true; // onItemAdd / Remove somewhy is not silenced
                switch(type) {
                    case 'remove':
                        selectizeInstance.removeOption(selectizeOption.value);
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
                element.silent = false;
            }, true)
        }
    }

}