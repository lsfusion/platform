function select() {

    function mapOption(option, controller) {
        return {
            value: controller.getKey(option).toString(),
            text: option.name,
            originalObject: option
        }
    }

    return {
        render: function (element, controller) {
            element.selectizeInstance = $(element).selectize({
                dropdownParent: 'body',
                onItemAdd: function (value) {
                    //if is a CustomCellRenderer, there is no controller in the render()
                    if (controller == null)
                        controller = element.controller;

                    controller.changeProperty('selected', this.options[value].originalObject, true);
                },
                onItemRemove: function (value) {
                    //if is a CustomCellRenderer, there is no controller in the render()
                    if (controller == null)
                        controller = element.controller;

                    let option = this.options[value];
                    if (option != null) // if option == null so option has been removed by .removeOption() method
                        controller.changeProperty('selected', option.originalObject, null);
                },
                plugins: ['remove_button']
            });

            //controller != null if is a GSimpleStateTableView
            if (controller != null) {
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
            element.controller = controller; // controller is needed in render() to add onItemAdd and onItemRemove listeners. In CustomCellRenderer we cannot pass the controller to render()

            if (controller.booleanFilterSet !== undefined && !controller.booleanFilterSet && list.length > 0) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return;
            }

            let selectizeInstance = element.selectizeInstance[0].selectize;
            let diff = controller.getDiff(list);

            //remove
            diff.remove.forEach(option => selectizeInstance.removeOption(controller.getKey(option).toString()));

            //add
            //wrapper to avoid calling selectize events
            let eventName = 'item_add';
            let e = selectizeInstance._events[eventName];
            delete selectizeInstance._events[eventName];
            diff.add.forEach(option => {
                let selectizeOption = mapOption(option, controller);
                selectizeInstance.addOption(selectizeOption);

                if (option.selected === true)
                    selectizeInstance.addItem(selectizeOption.value);
            })
            selectizeInstance._events[eventName] = e;

            //update
            diff.update.forEach(option => {
                let selectizeOption = mapOption(option, controller);

                //updateOption only changes value and text. selected / unselected need to be manually controlled by addItem / removeItem
                selectizeInstance.updateOption(selectizeOption.value, selectizeOption);
                selectizeInstance[option.selected === true ? 'addItem' : 'removeItem'](selectizeOption.value);
            });
        }
    }
}