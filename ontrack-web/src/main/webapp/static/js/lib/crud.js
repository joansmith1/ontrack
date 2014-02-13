/**
 * Defines a standard behaviour for a CRUD table.
 */
define(['jquery', 'render', 'dialog', 'dynamic'], function ($, render, dialog, dynamic) {

    /**
     * This service
     */
    var self = {};

    /**
     * Internal function used to set-up the table after the rendering of items
     */
    function setupTableAfterRender(renderCfg, items, container, cfg) {
        var row = $('<div></div>').insertAfter(container);
        // Commands?
        if (cfg.commands) {
            $.each(cfg.commands, function (i, command) {
                var btn = $('<button/>').appendTo(row).addClass('btn');
                // Additional classes
                if (command.classes) {
                    $.each(command.classes, function (j, cls) {
                        btn.addClass(cls)
                    })
                }
                // Icon
                if (command.iconCls) {
                    $('<i></i>').addClass(command.iconCls).appendTo(btn)
                }
                // Text
                if (command.title) {
                    btn.append(' ' + command.title)
                }
                // Action
                if (command.action) {
                    btn.click(function () {
                        command.action(btn, cfg)
                    })
                }
            })
        }
    }

    /**
     * Create a CRUD controller.
     *
     * The `config` object has the following parameters:
     * - url: required, base URL used to access the objects:
     *      GET '' - list of items
     *      GET '/{id}' - get one item by id
     *      POST '' - creates an item
     *      PUT '/{id}' - updates an item
     *      DELETE '/{id}' - deletes an item
     * - itemId: optional, defaults to 'id'. Field that contains the ID in an item.
     * - itemName: required, name of the type of item
     * - itemTemplateId: required, template ID (see `render.js`) used to render the row for an item in the table
     * - itemDialogTemplateId: required, template ID used to render the
     * - itemDialogFieldPrefix, optional, defaults to ''. Prefix to add before the property name to get the field ID in the dialog
     * - itemNewFn, optional, defaults to a function that returns an empty object. Function that is called to
     *              get a new item to create
     * - itemDialogWidth, optional, defaults to 600, width of the dialog
     *
     * The client will call the method by doing:
     *
     * <pre>
     *     return crud.create({...})
     * </pre>
     */
    self.create = function (config) {

        /**
         * Defaults
         */
        var cfg = $.extend({}, {
            itemDialogFieldPrefix: '',
            itemNewFn: function () {
                return {}
            },
            itemDialogWidth: 600
        }, config);

        /**
         * Control of requisites
         */
        if (!cfg.url) throw '[crud] "url" parameter is required.';
        if (!cfg.itemTemplateId) throw '[crud] "itemTemplateId" parameter is required.';
        if (!cfg.itemName) throw '[crud] "itemName" parameter is required.';
        if (!cfg.itemDialogTemplateId) throw '[crud] "itemDialogTemplateId" parameter is required.';

        /**
         * Returns a standard `dynamic` configuration:
         * - the URL for the source
         * - the rendering & post-rendering methods
         */
        return {
            url: cfg.url,
            render: render.asTableTemplate(cfg.itemTemplateId, function (renderCfg, items, container) {
                setupTableAfterRender(renderCfg, items, container, cfg)
            })
        }

    };

    /**
     * Creates a 'create' command to use in the `commands` field of the configuration.
     */
    self.createCommand = function (title) {
        return {
            title: title,
            iconCls: 'icon-plus',
            action: function (btn, cfg) {
                self.createItem(btn, cfg)
            }
        }
    };

    /**
     * Create command.
     */
    self.createItem = function (btn, cfg) {
        dialogItem(btn, cfg, {
            data: cfg.itemNewFn()
        })
    };

    /**
     * Generic create/update dialog
     */
    function dialogItem(btn, cfg, dialogConfig) {
        // Shows the dialog
        dialog.show({
            title: cfg.itemName,
            width: cfg.itemDialogWidth,
            templateId: cfg.itemDialogTemplateId,
            initFn: function (dialog) {
                // Gets the data
                var item = dialogConfig.data;
                // Standard fields
                if (cfg.itemFields) {
                    $.each(cfg.itemFields, function (f, itemField) {
                        if ($.isPlainObject(itemField)) {
                            var fieldName = itemField.field;
                            var itemProperty = itemField.property;
                            dialog.form.find('#' + fieldName).val(item[itemProperty]);
                        } else if ($.isFunction(itemField)) {
                            itemField(cfg, dialog, item);
                        } else {
                            dialog.form.find('#' + cfg.itemDialogFieldPrefix + itemField).val(item[itemField]);
                        }
                    })
                }
                // Custom init
                if (cfg.itemDialogInitFn) {
                    cfg.itemDialogInitFn(cfg, dialog, item)
                }
            },
            submitFn: function (dialog) {
            }
        })
    }

    /**
     * Returns the CRUD service.
     */
    return self;

});