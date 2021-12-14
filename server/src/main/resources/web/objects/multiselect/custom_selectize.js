function selectize() {
    return _selectize((element, controller, list, mapOption, setBooleanFilter, addOptions) => {
        if (setBooleanFilter(controller)) return;

        let selectizeInstance = controller.selectizeInstance[0].selectize;
        selectizeInstance.clear();

        addOptions(selectizeInstance, () => list.forEach(selectedOption => {
            let selectizeOption = mapOption(selectedOption, controller);
            selectizeInstance.addOption(selectizeOption);
            selectizeInstance.addItem(selectizeOption.value);
        }));
    });
}

function selectize_set() {
    return _selectize((element, controller, list, mapOption, setBooleanFilter, addOptions) => {
        if (setBooleanFilter(controller)) return;

        let selectizeInstance = controller.selectizeInstance[0].selectize;
        let diff = controller.getDiff(list);

        diff.update.forEach(option => {
            let selectizeOption = mapOption(option, controller);
            selectizeInstance.updateOption(selectizeOption.value, selectizeOption);
        });

        addOptions(selectizeInstance, () => diff.add.forEach(option => {
            let selectizeOption = mapOption(option, controller);
            selectizeInstance.addOption(selectizeOption);
            selectizeInstance.addItem(selectizeOption.value);
        }));

        diff.remove.forEach(option => selectizeInstance.removeItem(controller.getKey(option).toString()));
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

    function setBooleanFilter(controller) {
        if (!controller.booleanFilterSet) {
            controller.setBooleanViewFilter('selected', 1000);
            controller.booleanFilterSet = true;
            return true;
        }
    }

    function addOptions(selectizeInstance, addFunction) { //wrapper to avoid calling onItemAdd
        let e = selectizeInstance._events['item_add'];
        delete selectizeInstance._events['item_add'];
        addFunction();
        selectizeInstance._events['item_add'] = e;
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
        update: (element, controller, list) => updateFunction(element, controller, list, mapOption, setBooleanFilter, addOptions)
    }
}