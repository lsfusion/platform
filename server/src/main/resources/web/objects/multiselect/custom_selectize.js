function select() {

    function _getKey(object, controller) {
        return controller.getKey(object).toString();
    }

    return {
        render: function (element, controller) {
            let isList = controller != null;
            if(!isList) //if is a CustomCellRenderer, there is no controller in the render()
                controller = element.controller;

            element.selectizeInstance = $(element).selectize({
                dropdownParent: 'body',

                // ??? maybe we have to undo this changes???
                onItemAdd: function (value) {
                    controller.changeProperty('selected', this.options[value].originalObject, true);
                },
                onItemRemove: function (value) {
                    let option = this.options[value];
                    // ???
                    if (option != null) // if option == null so option has been removed by .removeOption() method
                        controller.changeProperty('selected', option.originalObject, null);
                },
                plugins: ['remove_button']
            });

            //controller != null if is a GSimpleStateTableView
            if (isList) {
                let selectizeInstance = element.selectizeInstance[0].selectize;

                selectizeInstance.settings.preload = 'focus';
                selectizeInstance.settings.loadThrottle = 0;
                selectizeInstance.settings.load = function (query, callback) {
                    controller.getValues('name', query, (data) => {
                        let options = [];
                        for (let dataElement of data.data) {
                            options.push({
                                value: dataElement.key.toString(),
                                text: dataElement.rawString,
                                originalObject: dataElement.key,
                            });
                        }
                        callback(options);
                    }, null);
                }
            }
        },
        update: function (element, controller, list) {
            if (controller.booleanFilterSet !== undefined && !controller.booleanFilterSet && list.length > 0) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return;
            }

            let isList = controller.isList();
            if(!isList) // controller is needed in render() to add onItemAdd and onItemRemove listeners. In CustomCellRenderer we cannot pass the controller to render()
                element.controller = controller;

            let selectizeInstance = element.selectizeInstance[0].selectize;
            controller.diff(list, element, (type, index, object) => {
                let selectizeOption = {
                    value: _getKey(object, controller),
                    text: object.name,
                    originalObject: object
                };
                switch(type) {
                    case 'remove':
                        selectizeInstance.removeOption(selectizeOption.value);
                        break;
                    case 'add':
                    case 'update':
                        if(type === 'add')
                            selectizeInstance.addOption(selectizeOption);
                        else
                            selectizeInstance.updateOption(selectizeOption.value)

                        if(isList || selectizeOption.selected) {
                            selectizeInstance.setCaret(index);
                            selectizeInstance.addItem(selectizeOption.value, true);
                        } else
                            selectizeInstance.removeItem(selectizeOption.value, true);
                }
            }, to => _getKey(to, controller))
        }
    }

}