function selectize(element, list, controller) {
    if (controller.selectizeInstance == null) {
        controller.selectizeInstance = $(element).selectize({
            preload: 'focus',
            loadThrottle: 0,
            load: function (query) {
                controller.getValues('name', query, (data) => {
                    for (let dataElement of data.data) {
                        let keyValue = dataElement.key.toString();
                        if (!selectize.getOption(keyValue).length) {
                            selectize.addOption({
                                value: keyValue,
                                text: dataElement.rawString,
                                originalObject: dataElement.key
                            });
                        }
                    }
                    selectize.open();
                }, null);
            },
            onItemAdd: function (value) {
                let originalObject = this.options[value].originalObject;
                if (!originalObject.selected)
                    controller.changeProperty('selected', originalObject, true);
            },
            onItemRemove: function (value) {
                controller.changeProperty('selected', this.options[value].originalObject, null);
            },
            plugins: ['remove_button']
        });
    }

    let selectize = controller.selectizeInstance[0].selectize;
    if (list.length - selectize.items.length < 0) {
        selectize.clear(true);
        selectize.clearOptions(true);
    }

    for (let listElement of list) {
        let keyValue = controller.getKey(listElement).toString();
        selectize.addOption({
            value: keyValue,
            text: listElement.name,
            originalObject: listElement
        });
        selectize.addItem(keyValue, true);
    }
}