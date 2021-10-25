function select2() {
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
        update: (element, controller, list) => {
            if (!controller.booleanFilterSet) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return
            }

            let select2Instance = controller.select2Instance;

            let diff = controller.getDiff(list);
            let optionsParent = select2Instance.context;
            let select2Options = Array.from(optionsParent.children);

            for (let option of diff.add) {
                if (select2Options.filter(o => o.value === mapOption(option).value).length === 0)
                    select2Instance.append(mapOption(option));
            }

            for (let option of diff.update) {
                removeOption(option);
                select2Instance.append(mapOption(option));
            }

            for (let option of diff.remove) {
                removeOption(option);
            }

            function removeOption(option) {
                select2Options.forEach(o => {
                    if (o.value === mapOption(option).value)
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