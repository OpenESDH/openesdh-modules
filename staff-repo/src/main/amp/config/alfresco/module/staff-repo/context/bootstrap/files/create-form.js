{
    "widgets" : [
        {
            "name": "alfresco/forms/ControlRow",
            "config": {
                "description": "",
                "title": "",
                "widgets": [
                    {
                        "name": "alfresco/forms/controls/HiddenValue",
                        "config": {
                            "name": "alf_destination",
                            "value": "{casesFolderNodeRef}",
                            "label": "",
                            "unitsLabel": "",
                            "description": "",
                            "postWhenHiddenOrDisabled": true,
                            "noValueUpdateWhenHiddenOrDisabled": false,
                            "validationConfig": {
                                "regex": ".*"
                            },
                            "placeHolder": "",
                            "widgets": []
                        },
                        "widthPc": "1"
                    },
                    {
                        "name": "alfresco/forms/controls/HiddenValue",
                        "config": {
                            "fieldId": "edb31ed0-c74a-48f4-8f30-c5atbd748ffb",
                            "name": "caseType",
                            "value": "staff",
                            "label": "",
                            "unitsLabel": "",
                            "description": "",
                            "postWhenHiddenOrDisabled": true,
                            "noValueUpdateWhenHiddenOrDisabled": false,
                            "validationConfig": {
                                "regex": ".*"
                            },
                            "placeHolder": "",
                            "widgets": []
                        },
                        "widthPc": "1"
                    }
                ]
            }
        },
        {
            "name": "alfresco/forms/ControlRow",
            "config": {
                "description": "",
                "title": "",
                "fieldId": "33ed6de4-3a60-46bb-8389-40b04aeddd37",
                "widgets": [
                    {
                        "name": "alfresco/forms/controls/DojoValidationTextBox",
                        "config": {
                            "name": "prop_cm_title",
                            "value": "",
                            "label": "Case Title/Name",
                            "unitsLabel": "",
                            "description": "",
                            "visibilityConfig": {
                                "initialValue": true,
                                "rules": []
                            },
                            "requirementConfig": {
                                "initialValue": false,
                                "rules": []
                            },
                            "disablementConfig": {
                                "initialValue": false,
                                "rules": []
                            },
                            "postWhenHiddenOrDisabled": true,
                            "noValueUpdateWhenHiddenOrDisabled": false,
                            "validationConfig": {
                                "regex": ".*"
                            },
                            "placeHolder": "",
                            "widgets": []
                        },
                        "widthPc": "70"
                    },
                    {
                        "name": "openesdh/common/widgets/controls/Select",
                        "config": {
                            "id": "prop_oe_status",
                            "label": "Case Status",
                            "optionsConfig": {
                                "fixed": "{caseConstraintsList.staff.caseStatusConstraint}"
                            },
                            "unitsLabel": "",
                            "description": "",
                            "name": "prop_oe_status",
                            "widgets": []
                        }
                    }
                ]
            }
        },
        {
            "name": "alfresco/forms/ControlRow",
            "config": {
                "description": "",
                "title": "",
                "fieldId": "88ba8d88-b562-4954-81b9-d34ac564d5ff",
                "widgets": [
                    {
                        "name": "openesdh/common/widgets/controls/AuthorityPicker",
                        "id": "create_case_dialog_auth_picker",
                        "config": {
                            "label": "create-case.label.button.case-owner",
                            "name": "assoc_base_owners_added",
                            "itemKey": "nodeRef",
                            "currentUser": "{currentUser}",
                            "singleItemMode": false,
                            "setDefaultPickedItems": true,
                            "defaultPickedItems": "{currentUser}",
                            "widgetsForControl": [
                                {
                                    "name": "alfresco/layout/VerticalWidgets",
                                    "assignTo": "verticalWidgets",
                                    "config": {
                                        "widgets": [
                                            {
                                                "name": "alfresco/pickers/PickedItems",
                                                "assignTo": "pickedItemsWidget",
                                                "config": {
                                                    "pubSubScope": "{itemSelectionPubSubScope}"
                                                }
                                            },
                                            {
                                                "name": "alfresco/buttons/AlfButton",
                                                "id": "create_case_dialog_auth_picker_button",
                                                "assignTo": "formDialogButton",
                                                "config": {
                                                    "label": "picker.add.label",
                                                    "publishTopic": "ALF_CREATE_DIALOG_REQUEST",
                                                    "publishPayload": {
                                                        "dialogTitle": "auth-picker.select.title",
                                                        "handleOverflow": false,
                                                        "widgetsContent": [
                                                            {
                                                                "name": "{pickerWidget}",
                                                                "config": {}
                                                            }
                                                        ],
                                                        "widgetsButtons": [
                                                            {
                                                                "name": "alfresco/buttons/AlfButton",
                                                                "id": "create_case_dialog_auth_picker_picked_ok_button",
                                                                "config": {
                                                                    "label": "picker.ok.label",
                                                                    "publishTopic": "ALF_ITEMS_SELECTED",
                                                                    "pubSubScope": "{itemSelectionPubSubScope}"
                                                                }
                                                            },
                                                            {
                                                                "name": "alfresco/buttons/AlfButton",
                                                                "id": "create_case_dialog_auth_picker_picked_cancel_button",
                                                                "config": {
                                                                    "label": "picker.cancel.label",
                                                                    "publishTopic": "NO_OP"
                                                                }
                                                            }
                                                        ]
                                                    },
                                                    "publishGlobal": true
                                                }
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
        }
    ]
}
