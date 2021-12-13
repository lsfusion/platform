function selectize() {
    return new Selectize().getFunctions();
}

function selectize_set() {
    return new Selectize_set().getFunctions();
}

class Selectize {
    getFunctions() {
        return {
            render: (element, controller) => this.renderFunction(element, controller),
            update: (element, controller, list) => this.updateFunction(element, controller, list),
        }
    }

    renderFunction(element, controller) {
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
        });
    }

    updateFunction(element, controller, list) {
        if (!controller.booleanFilterSet) {
            controller.setBooleanViewFilter('selected', 1000);
            controller.booleanFilterSet = true;
            return
        }

        let selectizeInstance = controller.selectizeInstance[0].selectize;
        let diff = controller.getDiff(list);

        this.addOptions(selectizeInstance, () => {
            diff.add.forEach(option => {
                let selectizeOption = this.mapOption(option, controller);
                selectizeInstance.addOption(selectizeOption);
                selectizeInstance.addItem(selectizeOption.value);
            });
        });

        diff.update.forEach(option => {
            let selectizeOption = this.mapOption(option, controller);
            selectizeInstance.updateOption(selectizeOption.value, selectizeOption);
        });

        diff.remove.forEach(option => selectizeInstance.removeItem(this.mapOption(option,controller).value));
    }

    mapOption(option, controller) {
        return {
            value: controller.getKey(option).toString(),
            text: option.name,
            originalObject: option
        }
    }

    addOptions(selectizeInstance, addFunction) { //wrapper to avoid calling onItemAdd
        let e = selectizeInstance._events['item_add'];
        delete selectizeInstance._events['item_add'];

        addFunction();

        selectizeInstance._events['item_add'] = e;
    }
}

class Selectize_set extends Selectize {
    updateFunction(element, controller, list) {
        let selectizeInstance = controller.selectizeInstance[0].selectize;
        selectizeInstance.clear();

        this.addOptions(selectizeInstance, () => {
            list.filter(o => o.selected === true).forEach(selectedOption => {
                let selectizeOption = this.mapOption(selectedOption, controller);
                selectizeOption.test = true;
                selectizeInstance.addOption(selectizeOption);
                selectizeInstance.addItem(selectizeOption.value);
            });
        });
    }
}