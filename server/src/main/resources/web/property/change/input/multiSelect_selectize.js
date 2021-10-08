function selectize(element, list, controller) {
    let mappedList = new Map(Array.from(list).map(listElement => {
        let keyValue = controller.getKey(listElement).toString();
        return [keyValue, {
            value: keyValue,
            text: listElement.name,
            originalObject: listElement,
        }];
    }));

    if (controller.selectizeInstance == null) {
        controller.selectizeInstance = $(element).selectize({
            onFocus: function () {
                controller.getValues('name', '', updateOptions, null);
            },
            load: function (query) {
                controller.getValues('name', query, updateOptions, null);
            },
            onDropdownClose: function () {
                //clean options
                for (let optionsKey in this.options) {
                    if (!this.getItem(optionsKey).length)
                        this.removeOption(optionsKey, true);
                }
            },
            onItemAdd: function (value) {
                //https://github.com/selectize/selectize.js/issues/699
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

    //options	An object containing the entire pool of options. The object is keyed by each object's value.
    // items	An array of selected values.

    //add or update
    let selectize = controller.selectizeInstance[0].selectize;
    for (let option of mappedList.values()) {
        if (selectize.getOption(option.value).length) {
            selectize.updateOption(option.value, option);
        } else {
            selectize.addOption(option);
            selectize.addItem(option.value, true)
        }
    }

    //remove
    selectize.items.forEach(item => {
        if (!mappedList.has(item))
            selectize.removeItem(item, true);
    });

    function updateOptions(data) {
        for (let datum of data.data) {
            let keyValue = datum.key.toString();
            if (!selectize.getOption(keyValue).length) {
                selectize.addOption({
                    value: keyValue,
                    text: datum.rawString,
                    originalObject: datum.key
                });
            }
        }
        selectize.refreshOptions(true);
    }
}