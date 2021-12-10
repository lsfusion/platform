function selectize_set() {
    return {
        render: (element, controller) => _selectize().render(element, controller),
        update: (element, controller, list) => _selectize().update(element, controller, list,
            (diff, selectizeInstance, addSelectizeOption, mapOption) => {
                if (diff.add.length > 0) {
                    selectizeInstance.clearOptions();
                    Array.from(list).forEach(option => addSelectizeOption(mapOption, option, selectizeInstance));
                }
            })
    }
}

function selectize_list() {
    return {
        render: (element, controller) => _selectize().render(element, controller),
        update: (element, controller, list) => _selectize().update(element, controller, list,
            (diff, selectizeInstance, addSelectizeOption, mapOption) =>
                diff.add.forEach(option => addSelectizeOption(mapOption, option, selectizeInstance)))
    }
}

function _selectize() {
    return {
        render: (element, controller) => {
            controller.selectizeInstance = $(element).selectize({
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
                                allowChange: true
                            });
                        }
                        callback(options);
                    }, null);
                },
                onItemAdd: function (value) {
                    let option = this.options[value];
                    if (option.allowChange) // change property only if option from .getValues() method
                        controller.changeProperty('selected', option.originalObject, true);
                },
                onItemRemove: function (value) {
                    let option = this.options[value];
                    if (option != null) {// if option == null so option has been removed by .removeOption() method
                        controller.changeProperty('selected', option.originalObject, null);
                        option.allowChange = true;
                    }
                },
                plugins: ['remove_button']
            });
        },
        update: (element, controller, list, addFunction) => {
            if (!controller.booleanFilterSet) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return
            }

            let selectizeInstance = controller.selectizeInstance[0].selectize;
            let diff = controller.getDiff(list);

            addFunction(diff, selectizeInstance, addSelectizeOption, mapOption);

            diff.update.forEach(option => {
                let selectizeOption = mapOption(option);
                selectizeInstance.updateOption(selectizeOption.value, selectizeOption);
            })

            diff.remove.forEach(option => selectizeInstance.removeItem(mapOption(option).value));

            function addSelectizeOption(mapOption, option, selectizeInstance) {
                let selectizeOption = mapOption(option);
                selectizeInstance.addOption(selectizeOption);
                selectizeInstance.addItem(selectizeOption.value);
            }

            function mapOption(option) {
                return {
                    value: controller.getKey(option).toString(),
                    text: option.name,
                    originalObject: option
                }
            }
        }
    }
}