// expected properties:
// selected : selection property (automatically sets filter on server)
// name : selection name

function select2_set() {
    return {
        render: (element, controller) => _select2().render(element, controller),
        update: (element, controller, list) => _select2().update(element, controller, list,
            (diff, select2Instance, mapOption, removeOption) => {
                if (diff.add.length > 0) {
                    removeOption();
                    Array.from(list).forEach(option => select2Instance.append(mapOption(option)));
                }
            })
    }
}

function select2_list() {
    return {
        render: (element, controller) => _select2().render(element, controller),
        update: (element, controller, list) => _select2().update(element, controller, list,
            (diff, select2Instance, mapOption) => diff.add.forEach(option => { //removeOption в конец
                if (Array.from(select2Instance.context.children).filter(o => o.value === mapOption(option).value).length === 0)
                    select2Instance.append(mapOption(option));
            })
        )
    }
}

function _select2() {
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
        update: (element, controller, list, addFunction) => {
            if (!controller.booleanFilterSet) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return
            }

            let select2Instance = controller.select2Instance;
            let diff = controller.getDiff(list);

            addFunction(diff, select2Instance, mapOption, removeOption);

            diff.update.forEach(option => {
                removeOption(option);
                select2Instance.append(mapOption(option));
            });

            diff.remove.forEach(option => removeOption(option));

            function removeOption(option) {
                let optionsParent = select2Instance.context;
                Array.from(optionsParent.children).forEach(o => {
                    if (option == null || o.value === mapOption(option).value)
                        optionsParent.removeChild(o);
                });
            }

            function mapOption(option) {
                let mappedOption = new Option(option.name, controller.getKey(option).toString(), false, true);
                mappedOption.key = option;
                return mappedOption;
            }
        }
    }
}