function selectize() {
    return _selectize((element, controller, list, mapOption, addOptions, removeOptions) => {
        let selectizeInstance = controller.selectizeInstance[0].selectize;
        removeOptions(selectizeInstance);
        addOptions(selectizeInstance, list, controller);
    });
}

function selectize_set() {
    return _selectize((element, controller, list, mapOption, addOptions) => {
        let selectizeInstance = controller.selectizeInstance[0].selectize;
        let diff = controller.getDiff(list);

        diff.remove.forEach(option => selectizeInstance.removeItem(controller.getKey(option).toString()));

        addOptions(selectizeInstance, diff.add, controller);

        diff.update.forEach(option => {
            let selectizeOption = mapOption(option, controller);
            selectizeInstance.updateOption(selectizeOption.value, selectizeOption);
        });
    });
}

function _selectize(updateFunction) {
    function mapOption(option, controller) {
        return {
            value: controller.getKey(option).toString(),
            text: option.name,
            originalObject: option
        }
    }

    //wrapper to avoid calling selectize events
    function silentExecute(selectizeInstance, eventName, func) {
        let e = selectizeInstance._events[eventName];
        delete selectizeInstance._events[eventName];

        func();

        selectizeInstance._events[eventName] = e;
    }

    function addOptions(selectizeInstance, optionsList, controller) {
        silentExecute(selectizeInstance, 'item_add', () => optionsList.forEach(option => {
            let selectizeOption = mapOption(option, controller);
            selectizeInstance.addOption(selectizeOption);
            selectizeInstance.addItem(selectizeOption.value);
        }));
    }

    function removeOptions(selectizeInstance) {
        silentExecute(selectizeInstance, 'item_remove',
            () => Object.keys(selectizeInstance.options).forEach(key => selectizeInstance.removeOption(key)));
    }

    return {
        render: (element, controller) => controller.selectizeInstance = $(element).selectize({
            preload: 'focus',
            loadThrottle: 0,
            dropdownParent: 'body',
            load: function (query, callback) {
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
            },
            onItemAdd: function (value) {
                controller.changeProperty('selected', this.options[value].originalObject, true);
            },
            onItemRemove: function (value) {
                let option = this.options[value];
                if (option != null) // if option == null so option has been removed by .removeOption() method
                    controller.changeProperty('selected', option.originalObject, null);
            },
            plugins: ['remove_button']
        }),
        update: (element, controller, list) => {
            if (!controller.booleanFilterSet) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return;
            }
            updateFunction(element, controller, list, mapOption, addOptions, removeOptions);
        }
    }
}