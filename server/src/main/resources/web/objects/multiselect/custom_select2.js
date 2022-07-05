// expected properties:
// selected : selection property (automatically sets filter on server)
// name : selection name

function select2() {
    return _select2((element, controller, list, mapOption) => {
        let select2Instance = controller.select2Instance;
        let optionsParent = select2Instance.context;
        Array.from(optionsParent.children).forEach(o => optionsParent.removeChild(o));

        list.forEach(option => select2Instance.append(mapOption(option, controller)));
    });
}

function select2_set() {
    function findOption(optionsParent, controller, rawOption) {
        return Array.from(optionsParent.children).filter(o => o.value === controller.getKey(rawOption).toString())[0];
    }

    return _select2((element, controller, list, mapOption) => {
        let select2Instance = controller.select2Instance;
        let diff = controller.getDiff(list);
        let optionsParent = select2Instance.context;

        diff.remove.forEach(option => removeOption(option));

        diff.add.forEach(option => {
            // When user select option from the list select2 marks it and then changes from the server come in and the option is marked a second time.
            // This checks if the option is already marked
            if (findOption(optionsParent, controller, option) == null)
                select2Instance.append(mapOption(option, controller));
        });

        diff.update.forEach(option => {
            removeOption(option);
            select2Instance.append(mapOption(option, controller));
        });

        function removeOption(option) {
            optionsParent.removeChild(findOption(optionsParent, controller, option));
        }
    });
}

function _select2(updateFunction) {
    function mapOption(option, controller) {
        let mappedOption = new Option(option.name, controller.getKey(option).toString(), false, true);
        mappedOption.key = option;
        return mappedOption;
    }

    return {
        render: (element, controller, event) => {
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
                dropdownParent: element, // this way focus doesn't go to some unpredictable container when selecting item, however there can be some problems with z-indices
                allowClear: true,
                width: '100%'
            });

            select2Instance.on('select2:select', function (e) {
                if (e.params.data.async)
                    controller.changeProperty('selected', e.params.data.key, true);
            });

            setTimeout(() => {

                //check as in GKeyStroke.isCharAddKeyEvent
                if(event != null && event.type === 'keypress' && !event.ctrlKey && !event.shiftKey && !event.altKey && event.key !== 'Enter' && event.key !== 'Escape' && event.charCode !== 0) {
                    //https://stackoverflow.com/a/30552284/2235787
                    select2Instance.select2('open');
                    // Get the search box within the dropdown or the selection
                    // Dropdown = single, Selection = multiple
                    var $search = select2Instance.data('select2').dropdown.$search || select2Instance.data('select2').selection.$search;
                    // This is undocumented and may change in the future
                    $search.val(event.key);
                    $search.trigger('keyup');
                }

                $('.select2-search--inline').keydown(function (e) {
                    if (e.keyCode === 27 || e.key === 'Escape') {
                        e.stopPropagation();
                        element.dispatchEvent(new KeyboardEvent('keydown', {
                            'key' : 'Escape',
                            'keyCode': 27,
                            'bubbles': true
                        }));
                    }
                });
            });

            select2Instance.on('select2:unselect', function (e) {
                let data = e.params.data;
                controller.changeProperty('selected', data.key == null ? data.element.key : data.key, null);
            });
        },
        update: (element, controller, list) => {
            if (!controller.booleanFilterSet && list.length > 0) {
                controller.setBooleanViewFilter('selected', 1000);
                controller.booleanFilterSet = true;
                return;
            }
            updateFunction(element, controller, list, mapOption);
        }
    }
}