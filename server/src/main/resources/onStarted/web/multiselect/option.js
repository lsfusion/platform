function option() {
    return {
        render: function (element, controller) {
            let options = document.createElement('options');
            options.classList.add("option-container")
            // options.style.borderRadius = 'var(--table-border-radius)';
            // options.style.border = '1px solid var(--grid-separator-border-color)';
            // options.style.padding = 'var(--border-padding) 4px';

            element.appendChild(options);
            element.options = options;
        },
        update: function (element, controller, list) {
            let options = element.options;

            let isList;
            let diff;
            if(controller.getDiff !== undefined) {
                diff = controller.getDiff(list, true);
                isList = true;

                diff.remove.forEach(rawOption => options.removeChild(options.children[rawOption.index]));
            } else {
                if(typeof list === 'string') {
                    list = list.split(",").map((value, index) => { return { name : value, index : index, selected : false }; } )
                } else if(list == null) {
                    list = [];
                }
                diff = { add : list, update : []};
                isList = false;

                options.innerText = ""; // removing all children
            }

            diff.add.forEach(rawOption => {
                let option = document.createElement('div');
                option.innerText = rawOption.name;
                option.selected = rawOption.selected;

                option.classList.add("option-item")
                if(rawOption.selected)
                    option.classList.add("option-item-selected")

                if(isList) {
                    option.key = rawOption;
                    option.addEventListener('click', function () {
                        controller.changeProperty('selected', this.key, !this.selected ? true : null);
                        controller.changeObject(this.key);
                    });
                }

                let currentOptions = options.children;
                if (rawOption.index === (currentOptions.length))
                    options.appendChild(option);
                else
                    options.insertBefore(option, currentOptions[rawOption.index]);
            });

            diff.update.forEach(rawOption => {
                let option = options.children[rawOption.index];
                option.innerText = rawOption.name;
                option.selected = rawOption.selected;

                if(rawOption.selected)
                    option.classList.add("option-item-selected");
                else
                    option.classList.remove("option-item-selected");
            });

            if(isList)
                Array.from(options.children).forEach(option => {
                    if(controller.isCurrent(option.key))
                        option.classList.add("option-item-current");
                    else
                        option.classList.remove("option-item-current");
                });
        }
    }
}