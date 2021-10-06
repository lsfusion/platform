function testMultiSelect(element, list, controller) {
    if (element.firstChild == null) { //create
        let selectElement = document.createElement('select');
        element.appendChild(selectElement);

        let selectInstance = $(selectElement).select2({
            ajax: {
                transport: function (params, success, failure) {
                    let value = params.data.term;
                    controller.getValues('name', value == null ? '' : value, success, failure);
                },
                processResults: function (data, params) {
                    return {
                        results: $.map(data.data, function (obj) {
                            obj.id = obj.rawString;
                            obj.text = obj.rawString;
                            obj.disabled = controller.isPropertyReadOnly('selected', obj.key);
                            return obj;
                        })
                    };
                }
            },
            multiple: true,
            placeholder: 'This is placeholder', //todo
            closeOnSelect: false,
            data: $.map(list, function (obj) {
                obj.id = obj.name;
                obj.text = obj.name;
                obj.disabled = controller.isPropertyReadOnly('selected', obj);
                obj.key = controller.getKey(obj);
                return obj;
            }),
            allowClear: true,
            width: 'auto' //todo
        });

        selectInstance.on('select2:select', function (e) {
            controller.changeProperty('selected', e.params.data.key, true);
        });

        selectInstance.on('select2:unselect', function (e) {
            controller.changeProperty('selected', e.params.data.key, null);
        });

    } else { // update
        let selectInstance = $(element.firstChild);
        let selectContext = selectInstance.context;
        let currentSelectedOptions = [];

        Array.from(selectContext.children).forEach(option => {
            if (!option.selected)
                selectContext.removeChild(option);
            else
                currentSelectedOptions.push(option);
        });

        //look items that are in list and unselected and select them
        for (let listElement of list) {
            let selected = false;
            let key = controller.getKey(listElement);
            for (let i = 0; i < currentSelectedOptions.length; i++) {
                if (controller.isEquals(key, currentSelectedOptions[i].key)) {
                    selected = true;
                    currentSelectedOptions.splice(i, 1);
                    break;
                }
            }

            if (!selected && listElement.selected === true) {
                // Append it to the select
                let option = new Option(listElement.name, listElement.name, listElement.selected, listElement.selected);
                option.disabled = controller.isPropertyReadOnly('selected', listElement);
                option.key = key;
                selectInstance.append(option);

                selectInstance.select2('data').at(-1).key = key;
            }
        }

        // check if currentSelectedOptions not empty and remove elements
        currentSelectedOptions.forEach(currentSelectedOption => selectContext.removeChild(currentSelectedOption));
    }
}