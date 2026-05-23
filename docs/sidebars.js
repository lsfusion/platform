module.exports = {
  install: [
    'paradigm/Install', 
    {
      collapsed: false,
      type: 'category',
      label: 'Automatic installation',
      link: {type: 'doc', id: 'paradigm/Automatic_installation'},
      items: [
        'paradigm/Development_auto', 
        'paradigm/Execution_auto', 
      ]
    },
    {
      collapsed: false,
      type: 'category',
      label: 'Manual installation',
      link: {type: 'doc', id: 'paradigm/Manual_installation'},
      items: [
        'paradigm/Development_manual', 
        'paradigm/Execution_manual', 
      ]
    },
    'paradigm/Docker',
  ],
  learn: [
    'paradigm/Learn', 
    {
      collapsed: false,
      type: 'category',
      label: 'Paradigm',
      link: {type: 'doc', id: 'paradigm/Paradigm'},
      items: [
        {
          type: 'category',
          label: 'Logical model',
          link: {type: 'doc', id: 'paradigm/Logical_model'},
          items: [
            {
              type: 'category',
              label: 'Domain logic',
              link: {type: 'doc', id: 'paradigm/Domain_logic'},
              items: [
                {
                  type: 'category',
                  label: 'Properties',
                  link: {type: 'doc', id: 'paradigm/Properties'},
                  items: [
                    {
                      type: 'category',
                      label: 'Operators',
                      link: {type: 'doc', id: 'paradigm/Property_operators_paradigm'},
                      items: [
                        'paradigm/Data_properties_DATA', 
                        'paradigm/Composition_JOIN', 
                        'paradigm/Constant', 
                        {
                          type: 'category',
                          label: 'Operations with primitives',
                          link: {type: 'doc', id: 'paradigm/Operations_with_primitives'},
                          items: [
                            'paradigm/Arithmetic_operators_plus_minus_etc',
                            'paradigm/Logical_operators_AND_OR_NOT_XOR', 
                            'paradigm/Comparison_operators_=_etc',
                            'paradigm/Rounding_operator_ROUND',
                            'paradigm/Extremum_MAX_MIN',
                            'paradigm/String_operators_plus_CONCAT_SUBSTRING',
                            'paradigm/Structure_operators_STRUCT', 
                            'paradigm/Type_conversion', 
                          ]
                        },
                        {
                          type: 'category',
                          label: 'Class operators',
                          link: {type: 'doc', id: 'paradigm/Class_operators'},
                          items: [
                            'paradigm/Classification_IS_AS', 
                          ]
                        },
                        'paradigm/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE', 
                        {
                          type: 'category',
                          label: 'Set operations',
                          link: {type: 'doc', id: 'paradigm/Set_operations'},
                          items: [
                            'paradigm/Grouping_GROUP', 
                            'paradigm/Partitioning_sorting_PARTITION_..._ORDER', 
                            'paradigm/Recursion_RECURSION', 
                            'paradigm/Distribution_UNGROUP', 
                          ]
                        },
                      ]
                    },
                  ]
                },
                {
                  type: 'category',
                  label: 'Actions',
                  link: {type: 'doc', id: 'paradigm/Actions'},
                  items: [
                    'paradigm/Change_sessions', 
                    {
                      type: 'category',
                      label: 'Оperators',
                      link: {type: 'doc', id: 'paradigm/Action_operators_paradigm'},
                      items: [
                        {
                          type: 'category',
                          label: 'Execution order',
                          link: {type: 'doc', id: 'paradigm/Execution_order'},
                          items: [
                            'paradigm/Sequence', 
                            'paradigm/Call_EXEC', 
                            'paradigm/Loop_FOR', 
                            'paradigm/Branching_CASE_IF_MULTI', 
                            'paradigm/Recursive_loop_WHILE', 
                            'paradigm/Interruption_BREAK',
                            'paradigm/Next_iteration_CONTINUE', 
                            'paradigm/Exit_RETURN', 
                            'paradigm/New_threads_NEWTHREAD_NEWEXECUTOR', 
                            'paradigm/Exception_handling_TRY', 
                          ]
                        },
                        {
                          type: 'category',
                          label: 'State change',
                          link: {type: 'doc', id: 'paradigm/State_change'},
                          items: [
                            'paradigm/Property_change_CHANGE', 
                            'paradigm/New_object_NEW', 
                            'paradigm/Class_change_CHANGECLASS_DELETE', 
                          ]
                        },
                        {
                          type: 'category',
                          label: 'Session management',
                          link: {type: 'doc', id: 'paradigm/Session_management'},
                          items: [
                            'paradigm/Apply_changes_APPLY',
                            'paradigm/Cancel_changes_CANCEL',
                            'paradigm/New_session_NEWSESSION_NESTEDSESSION',
                            'paradigm/Previous_value_PREV',
                            'paradigm/Change_operators_SET_CHANGED_etc',
                          ]
                        },
                        {
                          type: 'category',
                          label: 'User/IS interaction',
                          link: {type: 'doc', id: 'paradigm/User_IS_interaction'},
                          items: [
                            'paradigm/Show_message_MESSAGE_ASK', 
                            {
                              type: 'category',
                              label: 'File operators',
                              link: {type: 'doc', id: 'paradigm/File_operators'},
                              items: [
                                'paradigm/Read_file_READ', 
                                'paradigm/Write_file_WRITE', 
                                'paradigm/Data_import_IMPORT', 
                                'paradigm/Data_export_EXPORT', 
                              ]
                            },
                            'paradigm/Send_mail_EMAIL', 
                          ]
                        },
                      ]
                    },
                  ]
                },
                {
                  type: 'category',
                  label: 'Events',
                  link: {type: 'doc', id: 'paradigm/Events'},
                  items: [
                    'paradigm/Simple_event', 
                    'paradigm/Calculated_events', 
                  ]
                },
                {
                  type: 'category',
                  label: 'Constraints',
                  link: {type: 'doc', id: 'paradigm/Constraints'},
                  items: [
                    'paradigm/Simple_constraints', 
                    {
                      type: 'category',
                      label: 'Classes',
                      link: {type: 'doc', id: 'paradigm/Classes'},
                      items: [
                        'paradigm/Built-in_classes', 
                        'paradigm/User_classes', 
                        'paradigm/Static_objects', 
                      ]
                    },
                    'paradigm/Aggregations', 
                  ]
                },
              ]
            },
            {
              type: 'category',
              label: 'View logic',
              link: {type: 'doc', id: 'paradigm/View_logic'},
              items: [
                {
                  type: 'category',
                  label: 'Forms',
                  link: {type: 'doc', id: 'paradigm/Forms'},
                  items: [
                    {
                      type: 'category',
                      label: 'Form structure',
                      link: {type: 'doc', id: 'paradigm/Form_structure'},
                      items: [
                        'paradigm/Groups_of_properties_and_actions', 
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Form views',
                      link: {type: 'doc', id: 'paradigm/Form_views'},
                      items: [
                        {
                          type: 'category',
                          label: 'Interactive view',
                          link: {type: 'doc', id: 'paradigm/Interactive_view'},
                          items: [
                            {
                                 type: 'category',
                                 label: 'Form design',
                                 link: {type: 'doc', id: 'paradigm/Form_design'},
                                 items: [
                                    'paradigm/Icons'
                                 ]
                            },
                            'paradigm/Form_events', 
                            {
                              type: 'category',
                              label: 'Form operators',
                              link: {type: 'doc', id: 'paradigm/Form_operators'},
                              items: [
                                {
                                  type: 'category',
                                  label: 'Value input',
                                  link: {type: 'doc', id: 'paradigm/Value_input'},
                                  items: [
                                    'paradigm/Primitive_input_INPUT', 
                                    'paradigm/Value_request_REQUEST', 
                                  ]
                                },
                                {
                                  type: 'category',
                                  label: 'Object group operators',
                                  link: {type: 'doc', id: 'paradigm/Object_group_operators'},
                                  items: [
                                    'paradigm/Object_tree_visibility_EXPAND_COLLAPSE',
                                    'paradigm/Filter_FILTER',
                                    'paradigm/Order_ORDER',
                                    'paradigm/View_VIEW',
                                  ]
                                },
                                {
                                  type: 'category',
                                  label: 'Focus operators',
                                  link: {type: 'doc', id: 'paradigm/Focus_operators'},
                                  items: [
                                    'paradigm/Activation_ACTIVATE',
                                    'paradigm/Activity_ACTIVE',
                                  ]
                                },
                                'paradigm/Capture_SCREENSHOT',
                              ]
                            },
                          ]
                        },
                        {
                          type: 'category',
                          label: 'Static view',
                          link: {type: 'doc', id: 'paradigm/Static_view'},
                          items: [
                            {
                              type: 'category',
                              label: 'Print view',
                              link: {type: 'doc', id: 'paradigm/Print_view'},
                              items: [
                                'paradigm/Report_design', 
                              ]
                            },
                            'paradigm/Structured_view', 
                          ]
                        },
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Open form',
                      link: {type: 'doc', id: 'paradigm/Open_form'},
                      items: [
                        'paradigm/In_an_interactive_view_SHOW_DIALOG', 
                        'paradigm/In_a_print_view_PRINT', 
                        'paradigm/In_a_structured_view_EXPORT_IMPORT', 
                      ]
                    },
                  ]
                },
                {
                  type: 'category',
                  label: 'Navigator',
                  link: {type: 'doc', id: 'paradigm/Navigator'},
                  items: [
                    'paradigm/Navigator_design', 
                  ]
                },
              ]
            },
          ]
        },
        {
          type: 'category',
          label: 'Physical model',
          link: {type: 'doc', id: 'paradigm/Physical_model'},
          items: [
            {
              type: 'category',
              label: 'Development',
              link: {type: 'doc', id: 'paradigm/Development'},
              items: [
                {
                  type: 'category',
                  label: 'Element identification',
                  link: {type: 'doc', id: 'paradigm/Element_identification'},
                  items: [
                    'paradigm/Naming', 
                    'paradigm/Search_', 
                  ]
                },
                {
                  type: 'category',
                  label: 'Modularity',
                  link: {type: 'doc', id: 'paradigm/Modularity'},
                  items: [
                    'paradigm/Modules', 
                    'paradigm/Projects', 
                    {
                      type: 'category',
                      label: 'Extensions',
                      link: {type: 'doc', id: 'paradigm/Extensions'},
                      items: [
                        'paradigm/Class_extension', 
                        'paradigm/Property_extension', 
                        'paradigm/Action_extension', 
                        'paradigm/Form_extension', 
                      ]
                    },
                  ]
                },
                'paradigm/Metaprogramming', 
                {
                  type: 'category',
                  label: 'Integration',
                  link: {type: 'doc', id: 'paradigm/Integration'},
                  items: [
                    'paradigm/Access_from_an_external_system',
                    {
                      type: 'category',
                      label: 'Access from an internal system',
                      link: {type: 'doc', id: 'paradigm/Access_from_an_internal_system'},
                      items: [
                        'paradigm/Custom_Spring_bean_EventServer',
                        'paradigm/Java_integration_API',
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Access to an external system (EXTERNAL)',
                      link: {type: 'doc', id: 'paradigm/Access_to_an_external_system_EXTERNAL'},
                      items: [
                        'paradigm/New_connection_NEWCONNECTION',
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Access to an internal system (INTERNAL, FORMULA)',
                      link: {type: 'doc', id: 'paradigm/Access_to_an_internal_system_INTERNAL_FORMULA'},
                      items: [
                        'paradigm/Internal_call_INTERNAL', 
                        'paradigm/Custom_formula_FORMULA', 
                      ]
                    },
                    'paradigm/Eval_EVAL', 
                  ]
                },
                'paradigm/Migration', 
                'paradigm/Internationalization', 
              ]
            },
            {
              type: 'category',
              label: 'Execution',
              link: {type: 'doc', id: 'paradigm/Execution'},
              items: [
                'paradigm/Materializations', 
                'paradigm/Indexes', 
                'paradigm/Tables', 
              ]
            },
            {
              type: 'category',
              label: 'Management',
              link: {type: 'doc', id: 'paradigm/Management'},
              items: [
                {
                  type: 'category',
                  label: 'System parameters',
                  link: {type: 'doc', id: 'paradigm/System_parameters'},
                  items: [
                    {
                      type: 'category',
                      label: 'Launch parameters',
                      link: {type: 'doc', id: 'paradigm/Launch_parameters'},
                      items: [
                        'paradigm/Launch_events', 
                      ]
                    },
                    'paradigm/Working_parameters', 
                  ]
                },
                'paradigm/User_interface', 
                'paradigm/Interpreter', 
                'paradigm/Security_policy', 
                'paradigm/Process_monitor', 
                'paradigm/Scheduler', 
                'paradigm/Journals_and_logs', 
                'paradigm/Profiler', 
                'paradigm/Chat', 
              ]
            },
          ]
        },
      ]
    },
    {
      collapsed: false,
      type: 'category',
      label: 'Language',
      link: {type: 'doc', id: 'language/Language'},
      items: [
        'language/Tokens', 
        'language/IDs', 
        'language/Literals', 
        {
          type: 'category',
          label: 'Statements',
          link: {type: 'doc', id: 'language/Statements'},
          items: [
            'language/Module_header', 
            {
              type: 'category',
              label: '= statement',
              link: {type: 'doc', id: 'language/=_statement'},
              items: [
                {
                  type: 'category',
                  label: 'Expression',
                  link: {type: 'doc', id: 'language/Expression'},
                  items: [
                    'language/Operator_priority', 
                  ]
                },
                {
                  type: 'category',
                  label: 'Property operators',
                  link: {type: 'doc', id: 'language/Property_operators'},
                  items: [
                    'language/Arithmetic_operators', 
                    'language/Brackets_operator', 
                    'language/ABSTRACT_operator', 
                    'language/ACTIVE_operator',
                    'language/AGGR_operator',
                    'language/CASE_operator', 
                    'language/CONCAT_operator',
                    'language/DATA_operator',
                    'language/EXCLUSIVE_operator',
                    'language/FORMULA_operator',
                    'language/GROUP_operator',
                    'language/IF_operator',
                    'language/IF_..._THEN_operator',
                    'language/JOIN_operator',
                    'language/JSON_operator',
                    'language/LIKE_operator',
                    'language/MATCH_operator',
                    'language/MAX_operator', 
                    'language/MIN_operator', 
                    'language/MULTI_operator', 
                    'language/OVERRIDE_operator', 
                    'language/PARTITION_operator', 
                    'language/PREV_operator', 
                    'language/RECURSION_operator',
                    'language/ROUND_operator',
                    'language/STRUCT_operator',
                    'language/UNGROUP_operator', 
                    'language/Object_group_operator', 
                    'language/Type_conversion_operator', 
                    'language/AND_OR_NOT_XOR_operators', 
                    'language/IS_AS_operators', 
                    'language/Change_operators', 
                    'language/Comparison_operators', 
                  ]
                },
                'language/Property_options', 
              ]
            },
            {
              type: 'category',
              label: 'ACTION statement',
              link: {type: 'doc', id: 'language/ACTION_statement'},
              items: [
                {
                  type: 'category',
                  label: 'Action operators',
                  link: {type: 'doc', id: 'language/Action_operators'},
                  items: [
                    'language/Braces_operator', 
                    'language/ABSTRACT_action_operator', 
                    'language/ACTIVATE_operator',
                    'language/ACTIVE_operator',
                    'language/APPLY_operator',
                    'language/ASK_operator', 
                    'language/CHANGE_operator', 
                    'language/BREAK_operator', 
                    'language/CANCEL_operator', 
                    'language/CASE_action_operator', 
                    'language/CHANGECLASS_operator',
                    'language/COLLAPSE_operator',
                    'language/CONTINUE_operator', 
                    'language/INTERNAL_operator', 
                    'language/DELETE_operator', 
                    'language/DIALOG_operator', 
                    'language/EMAIL_operator', 
                    'language/EVAL_operator', 
                    'language/EXEC_operator',
                    'language/EXPAND_operator',
                    'language/EXPORT_operator', 
                    'language/EXTERNAL_operator', 
                    'language/FOR_operator', 
                    'language/IF_..._THEN_action_operator', 
                    'language/IMPORT_operator', 
                    'language/INPUT_operator',
                    'language/MESSAGE_operator',
                    'language/MULTI_action_operator', 
                    'language/NEW_operator', 
                    'language/NESTEDSESSION_operator',
                    'language/NEWCONNECTION_operator',
                    'language/NEWEXECUTOR_operator', 
                    'language/NEWSESSION_operator', 
                    'language/NEWTHREAD_operator', 
                    'language/PRINT_operator',
                    'language/READ_operator',
                    'language/REQUEST_operator',
                    'language/RETURN_operator',
                    'language/SCREENSHOT_operator',
                    'language/SHOW_operator',
                    'language/TRY_operator',
                    'language/WHILE_operator', 
                    'language/WRITE_operator', 
                  ]
                },
                'language/Action_options', 
              ]
            },
            'language/GROUP_statement', 
            {
              type: 'category',
              label: 'ON statement',
              link: {type: 'doc', id: 'language/ON_statement'},
              items: [
                'language/Event_description_block', 
              ]
            },
            'language/WHEN_statement', 
            'language/lt-_WHEN_statement', 
            'language/CONSTRAINT_statement', 
            'language/=gt_statement', 
            'language/CLASS_statement', 
            {
              type: 'category',
              label: 'FORM statement',
              link: {type: 'doc', id: 'language/FORM_statement'},
              items: [
                'language/Object_blocks', 
                'language/Properties_and_actions_block', 
                'language/Filters_and_sortings_block', 
                'language/Event_block',
                'language/Pivot_block',
              ]
            },
            'language/DESIGN_statement', 
            'language/NAVIGATOR_statement', 
            'language/WINDOW_statement', 
            'language/EXTEND_CLASS_statement', 
            'language/EXTEND_FORM_statement', 
            'language/plus_equals_statement',
            'language/ACTION_plus_statement',
            'language/META_statement', 
            'language/commat_statement', 
            'language/TABLE_statement', 
            'language/INDEX_statement', 
            'language/BEFORE_statement', 
            'language/AFTER_statement', 
            'language/Empty_statement', 
          ]
        },
        'language/Comments',
        'language/Coding_conventions', 
      ]
    },
    'paradigm/IDE', 
    {
      collapsed: false,
      type: 'category',
      label: 'Learning materials',
      link: {type: 'doc', id: 'paradigm/Learning_materials'},
      items: [
      {
          type: 'category',
          label: 'Examples',
          link: {type: 'doc', id: 'paradigm/Examples'},
          items: [
            'paradigm/Score_table',
            'paradigm/Materials_management',
            ]
        },
        {
          type: 'category',
          label: 'How-to',
          link: {type: 'doc', id: 'how-to/How-to'},
          items: [
            {
              type: 'category',
              label: 'How-to: Computations',
              link: {type: 'doc', id: 'how-to/How-to_Computations'},
              items: [
                'how-to/How-to_GROUP_SUM', 
                'how-to/How-to_GROUP_MAX_MIN_AGGR', 
                'how-to/How-to_CASE_IF_OVERRIDE', 
                'how-to/How-to_GROUP_LAST', 
                'how-to/How-to_GROUP_CONCAT', 
                'how-to/How-to_PARTITION', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: GUI',
              link: {type: 'doc', id: 'how-to/How-to_GUI'},
              items: [
                {
                  type: 'category',
                  label: 'How-to: Interactive forms',
                  link: {type: 'doc', id: 'how-to/How-to_Interactive_forms'},
                  items: [
                    'how-to/How-to_CRUD', 
                    'how-to/How-to_Documents_with_lines', 
                    'how-to/How-to_Filtering_and_ordering', 
                    'how-to/How-to_Design', 
                    'how-to/How-to_Trees', 
                    'how-to/How-to_Data_entry', 
                    'how-to/How-to_Navigator', 
                    'how-to/How-to_Matrix', 
                    'how-to/How-to_Table_status',
                    'how-to/How-to_Custom_components_properties',
                    'how-to/How-to_Custom_components_objects',
                  ]
                },
                'how-to/How-to_Reports', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Imperative logic',
              link: {type: 'doc', id: 'how-to/How-to_Imperative_logic'},
              items: [
                {
                  type: 'category',
                  label: 'How-to: Actions',
                  link: {type: 'doc', id: 'how-to/How-to_Actions'},
                  items: [
                    'how-to/How-to_CHANGE', 
                    'how-to/How-to_EXEC', 
                    'how-to/How-to_NEW', 
                    'how-to/How-to_DELETE', 
                    'how-to/How-to_FOR', 
                    'how-to/How-to_IF_CASE', 
                    'how-to/How-to_WHILE', 
                    'how-to/How-to_NEWSESSION',
                    'how-to/How-to_ACTIVATE',
                  ]
                },
                'how-to/How-to_Events', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Declarative logic',
              link: {type: 'doc', id: 'how-to/How-to_Declarative_logic'},
              items: [
                'how-to/How-to_Constraints', 
                'how-to/How-to_Inheritance_and_aggregation', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Searching for elements',
              link: {type: 'doc', id: 'how-to/How-to_Searching_for_elements'},
              items: [
                'how-to/How-to_Namespaces', 
                'how-to/How-to_Explicit_typing', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Extensions',
              link: {type: 'doc', id: 'how-to/How-to_Extensions'},
              items: [
                'how-to/How-to_Class_extension', 
                'how-to/How-to_Property_extension', 
                'how-to/How-to_Action_extension', 
                'how-to/How-to_Form_extension', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Integration',
              link: {type: 'doc', id: 'how-to/How-to_Integration'},
              items: [
                {
                  type: 'category',
                  label: 'How-to: Working with external formats',
                  link: {type: 'doc', id: 'how-to/How-to_Working_with_external_formats'},
                  items: [
                    'how-to/How-to_Data_export', 
                    'how-to/How-to_Data_import', 
                  ]
                },
                'how-to/How-to_Interaction_via_HTTP_protocol',
                'how-to/How-to_Frontend',
                {
                  type: 'category',
                  label: 'How-to: Access to internal systems',
                  link: {type: 'doc', id: 'how-to/How-to_Access_to_internal_systems'},
                  items: [
                    'how-to/How-to_FORMULA',
                    'how-to/How-to_INTERNAL',
                  ]
                },
              ]
            },
            {
              type: 'category',
              label: 'How-to: Use Cases',
              link: {type: 'doc', id: 'how-to/How-to_Use_Cases'},
              items: [
                'how-to/How-to_Working_with_documents', 
                'how-to/How-to_Registers', 
                'how-to/How-to_Numbering', 
                'how-to/How-to_Using_objects_as_templates', 
                'how-to/How-to_Overriding_values', 
                'how-to/How-to_Binding_properties', 
              ]
            },
            'how-to/How-to_Metaprogramming', 
            'how-to/How-to_Physical_model', 
            'how-to/How-to_Internationalization', 
            'how-to/How-to_Adding_New_Fonts',
          ]
        },
      ]
    },
    {
      collapsed: false,
      type: 'category',
      label: 'AI',
      link: {type: 'doc', id: 'paradigm/AI'},
      items: [
        {type: 'doc', id: 'brief/Brief', label: 'Brief'},
        {type: 'doc', id: 'rules/Rules', label: 'Rules'},
      ]
    },
    'paradigm/Online_demo',
  ]
};
