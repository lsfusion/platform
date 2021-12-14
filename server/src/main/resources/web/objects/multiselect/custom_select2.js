// expected properties:
// selected : selection property (automatically sets filter on server)
// name : selection name

function select2() {
    return _select2((element, controller, list, mapOption, setBooleanFilter) => {
        if (setBooleanFilter(controller)) return;

        let select2Instance = controller.select2Instance;
        let optionsParent = select2Instance.context;
        Array.from(optionsParent.children).forEach(o => optionsParent.removeChild(o));

        list.forEach(option => select2Instance.append(mapOption(option, controller)));
    });
}

function select2_set() {
    return _select2((element, controller, list, mapOption, setBooleanFilter) => {
        if (setBooleanFilter(controller)) return;

        let select2Instance = controller.select2Instance;
        let diff = controller.getDiff(list);
        let select2Options = Array.from(select2Instance.context.children);

        diff.update.forEach(option => {
            removeOption(option);
            select2Instance.append(mapOption(option, controller));
        });

        diff.add.forEach(option => {
            let value = controller.getKey(option).toString();
            // When user select option from the list select2 marks it and then changes from the server come in and the option is marked a second time.
            // This checks if the option is already marked
            if (select2Options.filter(o => o.value === value).length === 0)
                select2Instance.append(mapOption(option, controller));
        });

        diff.remove.forEach(option => removeOption(option));

        function removeOption(option) {
            let optionsParent = select2Instance.context;
            let key = controller.getKey(option).toString();
            Array.from(optionsParent.children).filter(o => o.value === key).forEach(child => optionsParent.removeChild(child));
        }
    });
}

function _select2(updateFunction) {
    function mapOption(option, controller) {
        let mappedOption = new Option(option.name, controller.getKey(option).toString(), false, true);
        mappedOption.key = option;
        return mappedOption;
    }

    function setBooleanFilter(controller) {
        if (!controller.booleanFilterSet) {
            controller.setBooleanViewFilter('selected', 1000);
            controller.booleanFilterSet = true;
            return true;
        }
    }

    return {
        render: (element, controller) => {
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
        },
        update: (element, controller, list) => updateFunction(element, controller, list, mapOption, setBooleanFilter)
    }
}