function selectize() {
    return {
        render: (element, controller) => {
            controller.selectizeInstance = $(element).selectize({
                preload: 'focus',
                loadThrottle: 0,
                load: function (query, callback) {
                    controller.getValues('name', query, (data) => {
                        let options = [];
                        for (let dataElement of data.data) {
                            options.push({
                                value: dataElement.key.toString(),
                                text: dataElement.rawString,
                                originalObject: dataElement.key,
                                async: true
                            });
                        }
                        callback(options);
                    }, null);
                },
                onItemAdd: function (value) {
                    let option = this.options[value];
                    if (option.async) // change property only if option from .getValues() method
                        controller.changeProperty('selected', option.originalObject, true);
                },
                onItemRemove: function (value) {
                    let option = this.options[value];
                    if (option != null) // if option == null so option has been removed by .removeOption() method
                        controller.changeProperty('selected', option.originalObject, null);
                },
                plugins: ['remove_button']
            });
        },
        update: (element, controller, list) => {
            let selectizeInstance = controller.selectizeInstance[0].selectize;
            let diff = controller.getDiff(list);

            for (let option of diff.add) {
                let selectizeOption = mapOption(option);
                selectizeInstance.addOption(selectizeOption);
                selectizeInstance.addItem(selectizeOption.value);
            }

            for (let option of diff.update) {
                let selectizeOption = mapOption(option);
                selectizeInstance.updateOption(selectizeOption.value, selectizeOption);
            }

            for (let option of diff.remove) {
                selectizeInstance.removeItem(mapOption(option).value);
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