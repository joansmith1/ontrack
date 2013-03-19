var Properties = function () {

    function propertiesTemplate (entity, entityId) {
        return Template.config({
            url: 'ui/property/{0}/{1}'.format(entity, entityId),
            render: Template.fill(function (properties, append) {
                var html = '<ul class="properties">';
                $.each (properties, function (index, property) {
                    html += '<li>';
                    html += property.html;
                    if (property.editable) {
                        html += '<i class="icon-pencil action" onclick="Properties.editProperty(\'{0}\',\'{1}\')"></i>'.format(
                            property.extension.html(),
                            property.name.html()
                        );
                    }
                    html += '</li>';
                });
                html += '</ul>';
                return html;
            })
        });
    }

    function addProperties () {
        hideEditionBox('property-add');
        $('#property-add-select').val('');
        $('#property-add-section').show();
    }

    function editProperty (extension, name) {
        var entity = $('#entity').val();
        var entityId = $('#entityId').val();
        addProperties();
        $('#property-add-select').val('{0}#{1}'.format(extension, name));
        onPropertySelected($('#property-add-select'));
    }

    function addProperty () {
        var property = $('#property-add-select').val();
        var hash = property.indexOf('#');
        var extension = property.substring(0, hash);
        var name = property.substring(hash + 1);
        var value = $('#extension-{0}-{1}'.format(extension, name)).val();
        var entity = $('#entity').val();
        var entityId = $('#entityId').val();
        // No error
        $('#property-add-error').hide();
        // Loading
        $('#property-add-loading').show();
        // Loading the edition box
		$.ajax({
			type: 'POST',
			url: 'ui/property/{0}/{1}/edit/{2}/{3}'.format(entity, entityId, extension, name),
			contentType: 'application/json',
			data: JSON.stringify({
			    value: value
			}),
			dataType: 'json',
			success: function () {
                // Loading...
                $('#property-add-loading').hide();
                // OK - reloads the property container
                cancelAddProperties();
                Template.reload('properties');
			},
			error: function (jqXHR, textStatus, errorThrown) {
				Application.onAjaxError(jqXHR, textStatus, errorThrown, function (message) {
                    // Error
                    $('#property-add-error-message').text(message);
                    $('#property-add-error').show();
                    // Loading...
                    $('#property-add-loading').hide();
				});
			}
		});
    }

    function cancelAddProperties () {
        hideEditionBox('property-add');
        $('#property-add-section').hide();
    }

    function hideEditionBox (id) {
        $('#' + id + '-loading').hide();
        $('#' + id + '-error').hide();
        $('#' + id + '-field').hide();
    }

    function showEditionBox (config) {
        // No error
        $('#' + config.id + '-error').hide();
        // Loading
        $('#' + config.id + '-loading').show();
        // Loading the edition box
		$.ajax({
			type: 'GET',
			url: 'ui/property/{0}/{1}/edit/{2}/{3}'.format(config.entity, config.entityId, config.extension, config.name),
			dataType: 'html',
			success: function (html) {
                // Loading...
                $('#' + config.id + '-loading').hide();
                // Display
                $('#' + config.id + '-field').html(html);
                // Adjusting the label
                // Showing the edition box
                $('#' + config.id + '-field').show();
			},
			error: function (jqXHR, textStatus, errorThrown) {
				Application.onAjaxError(jqXHR, textStatus, errorThrown, function (message) {
                    // Error
                    $('#' + config.id + '-error-message').text(message);
                    $('#' + config.id + '-error').show();
                    // Loading...
                    $('#' + config.id + '-loading').hide();
				});
			}
		});
    }

    function onPropertySelected (dropbox) {
        var value = $(dropbox).val();
        if (value != "") {
            // Gets the extension and the name
            var hash = value.indexOf('#');
            var extension = value.substring(0, hash);
            var name = value.substring(hash + 1);
            // Entity
            var entity = $('#entity').val();
            var entityId = $('#entityId').val();
            // Prepares the edition box
            showEditionBox({
                id: 'property-add',
                extension: extension,
                name: name,
                entity: entity,
                entityId: entityId
            });
        } else {
            // Hides edition box
            hideEditionBox('property-add');
        }
    }

    return {
        propertiesTemplate: propertiesTemplate,
        addProperties: addProperties,
        cancelAddProperties: cancelAddProperties,
        addProperty: addProperty,
        editProperty: editProperty,
        onPropertySelected: onPropertySelected
    };

} ();