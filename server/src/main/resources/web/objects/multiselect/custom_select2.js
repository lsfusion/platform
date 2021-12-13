// expected properties:
// selected : selection property (automatically sets filter on server)
// name : selection name

function select2() {
    return new Select2().getFunctions();
}

function select2_set() {
    return new Select2_set().getFunctions();
}

class Select2 {
    getFunctions() {
        return {
            render: (element, controller) => this.renderFunction(element, controller),
            update: (element, controller, list) => this.updateFunction(element, controller, list),
        }
    }

    renderFunction(element, controller) {
        let selectElement = document.createElement('select');
        element.appendChild(selectElement);
        let select2Instance = controller.select2Instance = $(selectElement).select2({
            ajax: {
                transport: function (params, success, failure) {
                    let value = params.data.term;
                    controller.getValues('name', value == null ? '' : value, success, failure);
                },
                processResults: function (data) {
                    return {
                        results: $.map(data.data, function (obj) {
                            obj.id = obj.key.toString();
                            obj.text = obj.rawString;
                            obj.async = true;
                            return obj;
                        })
                    };
                }
            },
            multiple: true,
            placeholder: ' ',
            closeOnSelect: false,
            allowClear: true,
            width: '100%'
        });

        select2Instance.on('select2:select', function (e) {
            if (e.params.data.async)
                controller.changeProperty('selected', e.params.data.key, true);
        });

        select2Instance.on('select2:unselect', function (e) {
            let data = e.params.data;
            controller.changeProperty('selected', data.key == null ? data.element.key : data.key, null);
        });
    }

    updateFunction(element, controller, list) {
        if (!controller.booleanFilterSet) {
            controller.setBooleanViewFilter('selected', 1000);
            controller.booleanFilterSet = true;
            return
        }

        let select2Instance = controller.select2Instance;
        let diff = controller.getDiff(list);
        let select2Options = Array.from(select2Instance.context.children);

        diff.add.forEach(option => {
            if (select2Options.filter(o => o.value === this.mapOption(option, controller).value).length === 0)
                select2Instance.append(this.mapOption(option, controller));
        });

        diff.update.forEach(option => {
            this.removeOption(option, select2Instance, controller);
            select2Instance.append(this.mapOption(option, controller));
        });

        diff.remove.forEach(option => this.removeOption(option, select2Instance, controller));
    }

    removeOption(option, select2Instance, controller) {
        let optionsParent = select2Instance.context;
        Array.from(optionsParent.children)
            .filter(o => o.value === this.mapOption(option, controller).value)
            .forEach(child => optionsParent.removeChild(child));
    }

    mapOption(option, controller) {
        let mappedOption = new Option(option.name, controller.getKey(option).toString(), false, true);
        mappedOption.key = option;
        return mappedOption;
    }
}

class Select2_set extends Select2 {
    updateFunction(element, controller, list) {
        let select2Instance = controller.select2Instance;
        let optionsParent = select2Instance.context;
        Array.from(optionsParent.children).forEach(o => optionsParent.removeChild(o));
        list.filter(o => o.selected === true).forEach(selectedOption => select2Instance.append(super.mapOption(selectedOption, controller)));
    }
}