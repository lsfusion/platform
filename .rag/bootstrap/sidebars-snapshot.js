module.exports = {
  install: [
    'Install', 
    {
      collapsed: false,
      type: 'category',
      label: 'Automatic installation',
      link: {type: 'doc', id: 'Automatic_installation'},
      items: [
        'Development_auto', 
        'Execution_auto', 
      ]
    },
    {
      collapsed: false,
      type: 'category',
      label: 'Manual installation',
      link: {type: 'doc', id: 'Manual_installation'},
      items: [
        'Development_manual', 
        'Execution_manual', 
      ]
    },
    'Docker',
  ],
  learn: [
    'Learn', 
    {
      collapsed: false,
      type: 'category',
      label: 'Paradigm',
      link: {type: 'doc', id: 'Paradigm'},
      items: [
        {
          type: 'category',
          label: 'Logical model',
          link: {type: 'doc', id: 'Logical_model'},
          items: [
            {
              type: 'category',
              label: 'Domain logic',
              link: {type: 'doc', id: 'Domain_logic'},
              items: [
                {
                  type: 'category',
                  label: 'Properties',
                  link: {type: 'doc', id: 'Properties'},
                  items: [
                    {
                      type: 'category',
                      label: 'Operators',
                      link: {type: 'doc', id: 'Property_operators_paradigm'},
                      items: [
                        'Data_properties_DATA', 
                        'Composition_JOIN', 
                        'Constant', 
                        {
                          type: 'category',
                          label: 'Operations with primitives',
                          link: {type: 'doc', id: 'Operations_with_primitives'},
                          items: [
                            'Arithmetic_operators_plus_minus_etc',
                            'Logical_operators_AND_OR_NOT_XOR', 
                            'Comparison_operators_=_etc',
                            'Rounding_operator_ROUND',
                            'Extremum_MAX_MIN',
                            'String_operators_plus_CONCAT_SUBSTRING',
                            'Structure_operators_STRUCT', 
                            'Type_conversion', 
                          ]
                        },
                        {
                          type: 'category',
                          label: 'Class operators',
                          link: {type: 'doc', id: 'Class_operators'},
                          items: [
                            'Classification_IS_AS', 
                            'Property_signature_CLASS', 
                          ]
                        },
                        'Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE', 
                        {
                          type: 'category',
                          label: 'Set operations',
                          link: {type: 'doc', id: 'Set_operations'},
                          items: [
                            'Grouping_GROUP', 
                            'Partitioning_sorting_PARTITION_..._ORDER', 
                            'Recursion_RECURSION', 
                            'Distribution_UNGROUP', 
                          ]
                        },
                      ]
                    },
                  ]
                },
                {
                  type: 'category',
                  label: 'Actions',
                  link: {type: 'doc', id: 'Actions'},
                  items: [
                    'Change_sessions', 
                    {
                      type: 'category',
                      label: 'Оperators',
                      link: {type: 'doc', id: 'Action_operators_paradigm'},
                      items: [
                        {
                          type: 'category',
                          label: 'Execution order',
                          link: {type: 'doc', id: 'Execution_order'},
                          items: [
                            'Sequence', 
                            'Call_EXEC', 
                            'Loop_FOR', 
                            'Branching_CASE_IF_MULTI', 
                            'Recursive_loop_WHILE', 
                            'Interruption_BREAK',
                            'Next_iteration_CONTINUE', 
                            'Exit_RETURN', 
                            'New_threads_NEWTHREAD_NEWEXECUTOR', 
                            'Exception_handling_TRY', 
                          ]
                        },
                        {
                          type: 'category',
                          label: 'State change',
                          link: {type: 'doc', id: 'State_change'},
                          items: [
                            'Property_change_CHANGE', 
                            'New_object_NEW', 
                            'Class_change_CHANGECLASS_DELETE', 
                          ]
                        },
                        {
                          type: 'category',
                          label: 'Session management',
                          link: {type: 'doc', id: 'Session_management'},
                          items: [
                            'Apply_changes_APPLY',
                            'Cancel_changes_CANCEL',
                            'New_session_NEWSESSION_NESTEDSESSION',
                            'Previous_value_PREV',
                            'Change_operators_SET_CHANGED_etc',
                          ]
                        },
                        {
                          type: 'category',
                          label: 'User/IS interaction',
                          link: {type: 'doc', id: 'User_IS_interaction'},
                          items: [
                            'Show_message_MESSAGE_ASK', 
                            {
                              type: 'category',
                              label: 'File operators',
                              link: {type: 'doc', id: 'File_operators'},
                              items: [
                                'Read_file_READ', 
                                'Write_file_WRITE', 
                                'Data_import_IMPORT', 
                                'Data_export_EXPORT', 
                              ]
                            },
                            'Send_mail_EMAIL', 
                          ]
                        },
                      ]
                    },
                  ]
                },
                {
                  type: 'category',
                  label: 'Events',
                  link: {type: 'doc', id: 'Events'},
                  items: [
                    'Simple_event', 
                    'Calculated_events', 
                  ]
                },
                {
                  type: 'category',
                  label: 'Constraints',
                  link: {type: 'doc', id: 'Constraints'},
                  items: [
                    'Simple_constraints', 
                    {
                      type: 'category',
                      label: 'Classes',
                      link: {type: 'doc', id: 'Classes'},
                      items: [
                        'Built-in_classes', 
                        'User_classes', 
                        'Static_objects', 
                      ]
                    },
                    'Aggregations', 
                  ]
                },
              ]
            },
            {
              type: 'category',
              label: 'View logic',
              link: {type: 'doc', id: 'View_logic'},
              items: [
                {
                  type: 'category',
                  label: 'Forms',
                  link: {type: 'doc', id: 'Forms'},
                  items: [
                    {
                      type: 'category',
                      label: 'Form structure',
                      link: {type: 'doc', id: 'Form_structure'},
                      items: [
                        'Groups_of_properties_and_actions', 
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Form views',
                      link: {type: 'doc', id: 'Form_views'},
                      items: [
                        {
                          type: 'category',
                          label: 'Interactive view',
                          link: {type: 'doc', id: 'Interactive_view'},
                          items: [
                            {
                                 type: 'category',
                                 label: 'Form design',
                                 link: {type: 'doc', id: 'Form_design'},
                                 items: [
                                    'Icons'
                                 ]
                            },
                            'Form_events', 
                            {
                              type: 'category',
                              label: 'Form operators',
                              link: {type: 'doc', id: 'Form_operators'},
                              items: [
                                {
                                  type: 'category',
                                  label: 'Value input',
                                  link: {type: 'doc', id: 'Value_input'},
                                  items: [
                                    'Primitive_input_INPUT', 
                                    'Value_request_REQUEST', 
                                  ]
                                },
                                {
                                  type: 'category',
                                  label: 'Object group operators',
                                  link: {type: 'doc', id: 'Object_group_operators'},
                                  items: [
                                    'Object_tree_visibility_EXPAND_COLLAPSE',
                                    'Filter_FILTER',
                                    'Order_ORDER',
                                    'View_VIEW',
                                  ]
                                },
                                {
                                  type: 'category',
                                  label: 'Focus operators',
                                  link: {type: 'doc', id: 'Focus_operators'},
                                  items: [
                                    'Activation_ACTIVATE',
                                    'Activity_ACTIVE',
                                  ]
                                },
                                'Capture_SCREENSHOT',
                              ]
                            },
                          ]
                        },
                        {
                          type: 'category',
                          label: 'Static view',
                          link: {type: 'doc', id: 'Static_view'},
                          items: [
                            {
                              type: 'category',
                              label: 'Print view',
                              link: {type: 'doc', id: 'Print_view'},
                              items: [
                                'Report_design', 
                              ]
                            },
                            'Structured_view', 
                          ]
                        },
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Open form',
                      link: {type: 'doc', id: 'Open_form'},
                      items: [
                        'In_an_interactive_view_SHOW_DIALOG', 
                        'In_a_print_view_PRINT', 
                        'In_a_structured_view_EXPORT_IMPORT', 
                      ]
                    },
                  ]
                },
                {
                  type: 'category',
                  label: 'Navigator',
                  link: {type: 'doc', id: 'Navigator'},
                  items: [
                    'Navigator_design', 
                  ]
                },
              ]
            },
          ]
        },
        {
          type: 'category',
          label: 'Physical model',
          link: {type: 'doc', id: 'Physical_model'},
          items: [
            {
              type: 'category',
              label: 'Development',
              link: {type: 'doc', id: 'Development'},
              items: [
                {
                  type: 'category',
                  label: 'Element identification',
                  link: {type: 'doc', id: 'Element_identification'},
                  items: [
                    'Naming', 
                    'Search_', 
                  ]
                },
                {
                  type: 'category',
                  label: 'Modularity',
                  link: {type: 'doc', id: 'Modularity'},
                  items: [
                    'Modules', 
                    'Projects', 
                    {
                      type: 'category',
                      label: 'Extensions',
                      link: {type: 'doc', id: 'Extensions'},
                      items: [
                        'Class_extension', 
                        'Property_extension', 
                        'Action_extension', 
                        'Form_extension', 
                      ]
                    },
                  ]
                },
                'Metaprogramming', 
                {
                  type: 'category',
                  label: 'Integration',
                  link: {type: 'doc', id: 'Integration'},
                  items: [
                    'Access_from_an_external_system',
                    {
                      type: 'category',
                      label: 'Access from an internal system',
                      link: {type: 'doc', id: 'Access_from_an_internal_system'},
                      items: [
                        'Custom_Spring_bean_EventServer',
                        'Java_integration_API',
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Access to an external system (EXTERNAL)',
                      link: {type: 'doc', id: 'Access_to_an_external_system_EXTERNAL'},
                      items: [
                        'New_connection_NEWCONNECTION',
                      ]
                    },
                    {
                      type: 'category',
                      label: 'Access to an internal system (INTERNAL, FORMULA)',
                      link: {type: 'doc', id: 'Access_to_an_internal_system_INTERNAL_FORMULA'},
                      items: [
                        'Internal_call_INTERNAL', 
                        'Custom_formula_FORMULA', 
                      ]
                    },
                    'Eval_EVAL', 
                  ]
                },
                'Migration', 
                'Internationalization', 
              ]
            },
            {
              type: 'category',
              label: 'Execution',
              link: {type: 'doc', id: 'Execution'},
              items: [
                'Materializations', 
                'Indexes', 
                'Tables', 
              ]
            },
            {
              type: 'category',
              label: 'Management',
              link: {type: 'doc', id: 'Management'},
              items: [
                {
                  type: 'category',
                  label: 'System parameters',
                  link: {type: 'doc', id: 'System_parameters'},
                  items: [
                    {
                      type: 'category',
                      label: 'Launch parameters',
                      link: {type: 'doc', id: 'Launch_parameters'},
                      items: [
                        'Launch_events', 
                      ]
                    },
                    'Working_parameters', 
                  ]
                },
                'User_interface', 
                'Interpreter', 
                'Security_policy', 
                'Process_monitor', 
                'Scheduler', 
                'Journals_and_logs', 
                'Profiler', 
                'Chat', 
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
      link: {type: 'doc', id: 'Language'},
      items: [
        'Tokens', 
        'IDs', 
        'Literals', 
        {
          type: 'category',
          label: 'Statements',
          link: {type: 'doc', id: 'Statements'},
          items: [
            'Module_header', 
            {
              type: 'category',
              label: '= statement',
              link: {type: 'doc', id: '=_statement'},
              items: [
                {
                  type: 'category',
                  label: 'Expression',
                  link: {type: 'doc', id: 'Expression'},
                  items: [
                    'Operator_priority', 
                  ]
                },
                {
                  type: 'category',
                  label: 'Property operators',
                  link: {type: 'doc', id: 'Property_operators'},
                  items: [
                    'Arithmetic_operators', 
                    'Brackets_operator', 
                    'ABSTRACT_operator', 
                    'ACTIVE_operator',
                    'AGGR_operator',
                    'CASE_operator', 
                    'CONCAT_operator',
                    'DATA_operator',
                    'EXCLUSIVE_operator',
                    'FORMULA_operator',
                    'GROUP_operator',
                    'IF_operator',
                    'IF_..._THEN_operator',
                    'CLASS_operator',
                    'JOIN_operator',
                    'JSON_operator',
                    'LIKE_operator',
                    'MATCH_operator',
                    'MAX_operator', 
                    'MIN_operator', 
                    'MULTI_operator', 
                    'OVERRIDE_operator', 
                    'PARTITION_operator', 
                    'PREV_operator', 
                    'RECURSION_operator',
                    'ROUND_operator',
                    'STRUCT_operator',
                    'UNGROUP_operator', 
                    'Object_group_operator', 
                    'Type_conversion_operator', 
                    'AND_OR_NOT_XOR_operators', 
                    'IS_AS_operators', 
                    'Change_operators', 
                    'Comparison_operators', 
                  ]
                },
                'Property_options', 
              ]
            },
            {
              type: 'category',
              label: 'ACTION statement',
              link: {type: 'doc', id: 'ACTION_statement'},
              items: [
                {
                  type: 'category',
                  label: 'Action operators',
                  link: {type: 'doc', id: 'Action_operators'},
                  items: [
                    'Braces_operator', 
                    'ABSTRACT_action_operator', 
                    'ACTIVATE_operator',
                    'ACTIVE_operator',
                    'APPLY_operator',
                    'ASK_operator', 
                    'CHANGE_operator', 
                    'BREAK_operator', 
                    'CANCEL_operator', 
                    'CASE_action_operator', 
                    'CHANGECLASS_operator',
                    'COLLAPSE_operator',
                    'CONTINUE_operator', 
                    'INTERNAL_operator', 
                    'DELETE_operator', 
                    'DIALOG_operator', 
                    'EMAIL_operator', 
                    'EVAL_operator', 
                    'EXEC_operator',
                    'EXPAND_operator',
                    'EXPORT_operator', 
                    'EXTERNAL_operator', 
                    'FOR_operator', 
                    'IF_..._THEN_action_operator', 
                    'IMPORT_operator', 
                    'INPUT_operator',
                    'MESSAGE_operator',
                    'MULTI_action_operator', 
                    'NEW_operator', 
                    'NESTEDSESSION_operator',
                    'NEWCONNECTION_operator',
                    'NEWEXECUTOR_operator', 
                    'NEWSESSION_operator', 
                    'NEWTHREAD_operator', 
                    'PRINT_operator',
                    'READ_operator',
                    'REQUEST_operator',
                    'RETURN_operator',
                    'SCREENSHOT_operator',
                    'SHOW_operator',
                    'TRY_operator',
                    'WHILE_operator', 
                    'WRITE_operator', 
                  ]
                },
                'Action_options', 
              ]
            },
            'GROUP_statement', 
            {
              type: 'category',
              label: 'ON statement',
              link: {type: 'doc', id: 'ON_statement'},
              items: [
                'Event_description_block', 
              ]
            },
            'WHEN_statement', 
            'lt-_WHEN_statement', 
            'CONSTRAINT_statement', 
            '=gt_statement', 
            'CLASS_statement', 
            {
              type: 'category',
              label: 'FORM statement',
              link: {type: 'doc', id: 'FORM_statement'},
              items: [
                'Object_blocks', 
                'Properties_and_actions_block', 
                'Filters_and_sortings_block', 
                'Event_block',
                'Pivot_block',
              ]
            },
            'DESIGN_statement', 
            'NAVIGATOR_statement', 
            'WINDOW_statement', 
            'EXTEND_CLASS_statement', 
            'EXTEND_FORM_statement', 
            'plus_equals_statement',
            'ACTION_plus_statement',
            'META_statement', 
            'commat_statement', 
            'TABLE_statement', 
            'INDEX_statement', 
            'BEFORE_statement', 
            'AFTER_statement', 
            'Empty_statement', 
          ]
        },
        'Comments',
        'Coding_conventions', 
      ]
    },
    'IDE', 
    {
      collapsed: false,
      type: 'category',
      label: 'Learning materials',
      link: {type: 'doc', id: 'Learning_materials'},
      items: [
      {
          type: 'category',
          label: 'Examples',
          link: {type: 'doc', id: 'Examples'},
          items: [
            'Score_table',
            'Materials_management',
            ]
        },
        {
          type: 'category',
          label: 'How-to',
          link: {type: 'doc', id: 'How-to'},
          items: [
            {
              type: 'category',
              label: 'How-to: Computations',
              link: {type: 'doc', id: 'How-to_Computations'},
              items: [
                'How-to_GROUP_SUM', 
                'How-to_GROUP_MAX_MIN_AGGR', 
                'How-to_CASE_IF_OVERRIDE', 
                'How-to_GROUP_LAST', 
                'How-to_GROUP_CONCAT', 
                'How-to_PARTITION', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: GUI',
              link: {type: 'doc', id: 'How-to_GUI'},
              items: [
                {
                  type: 'category',
                  label: 'How-to: Interactive forms',
                  link: {type: 'doc', id: 'How-to_Interactive_forms'},
                  items: [
                    'How-to_CRUD', 
                    'How-to_Documents_with_lines', 
                    'How-to_Filtering_and_ordering', 
                    'How-to_Design', 
                    'How-to_Trees', 
                    'How-to_Data_entry', 
                    'How-to_Navigator', 
                    'How-to_Matrix', 
                    'How-to_Table_status',
                    'How-to_Custom_components_properties',
                    'How-to_Custom_components_objects',
                  ]
                },
                'How-to_Reports', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Imperative logic',
              link: {type: 'doc', id: 'How-to_Imperative_logic'},
              items: [
                {
                  type: 'category',
                  label: 'How-to: Actions',
                  link: {type: 'doc', id: 'How-to_Actions'},
                  items: [
                    'How-to_CHANGE', 
                    'How-to_EXEC', 
                    'How-to_NEW', 
                    'How-to_DELETE', 
                    'How-to_FOR', 
                    'How-to_IF_CASE', 
                    'How-to_WHILE', 
                    'How-to_NEWSESSION',
                    'How-to_ACTIVATE',
                  ]
                },
                'How-to_Events', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Declarative logic',
              link: {type: 'doc', id: 'How-to_Declarative_logic'},
              items: [
                'How-to_Constraints', 
                'How-to_Inheritance_and_aggregation', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Searching for elements',
              link: {type: 'doc', id: 'How-to_Searching_for_elements'},
              items: [
                'How-to_Namespaces', 
                'How-to_Explicit_typing', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Extensions',
              link: {type: 'doc', id: 'How-to_Extensions'},
              items: [
                'How-to_Class_extension', 
                'How-to_Property_extension', 
                'How-to_Action_extension', 
                'How-to_Form_extension', 
              ]
            },
            {
              type: 'category',
              label: 'How-to: Integration',
              link: {type: 'doc', id: 'How-to_Integration'},
              items: [
                {
                  type: 'category',
                  label: 'How-to: Working with external formats',
                  link: {type: 'doc', id: 'How-to_Working_with_external_formats'},
                  items: [
                    'How-to_Data_export', 
                    'How-to_Data_import', 
                  ]
                },
                'How-to_Interaction_via_HTTP_protocol',
                'How-to_Frontend',
                {
                  type: 'category',
                  label: 'How-to: Access to internal systems',
                  link: {type: 'doc', id: 'How-to_Access_to_internal_systems'},
                  items: [
                    'How-to_FORMULA',
                    'How-to_INTERNAL',
                  ]
                },
              ]
            },
            {
              type: 'category',
              label: 'How-to: Use Cases',
              link: {type: 'doc', id: 'How-to_Use_Cases'},
              items: [
                'How-to_Working_with_documents', 
                'How-to_Registers', 
                'How-to_Numbering', 
                'How-to_Using_objects_as_templates', 
                'How-to_Overriding_values', 
                'How-to_Binding_properties', 
              ]
            },
            'How-to_Metaprogramming', 
            'How-to_Physical_model', 
            'How-to_Internationalization', 
            'How-to_Adding_New_Fonts',
          ]
        },
      ]
    },
    'Online_demo', 
  ]
};
