var ValidationStamps = function () {
	
	function createValidationStamp (project, branch) {
		Application.dialogAndSubmit({
			id: 'validation_stamp-create-dialog',
			title: loc('validation_stamp.create.title'),
			url: 'ui/manage/project/{0}/branch/{1}/validation_stamp'.format(project,branch),
			successFn: function (data) {
				location = 'gui/project/{0}/branch/{1}/validation_stamp/{2}'.format(project,branch,data.name);
			}
		});
	}

	function updateValidationStamp (project, branch, validationStamp) {
	    var url = 'ui/manage/project/{0}/branch/{1}/validation_stamp/{2}'.format(project, branch, validationStamp);
	    AJAX.get({
            url: url,
            successFn: function (summary) {
                Application.dialogAndSubmit({
                    id: 'validation_stamp-update-dialog',
                    title: loc('validation_stamp.update'),
                    url: url,
                    method: 'PUT',
                    openFn: function () {
                        $('#validation_stamp-update-dialog-name').val(summary.name);
                        $('#validation_stamp-update-dialog-description').val(summary.description);
                    },
                    successFn: function (summary) {
                            location = 'gui/project/{0}/branch/{1}/validation_stamp/{2}'.format(summary.branch.project.name, summary.branch.name, summary.name);
                        }
                    });
            }
	    });
	}
	
	function deleteValidationStamp(project, branch, name) {
		Application.deleteEntity('project/{0}/branch/{1}/validation_stamp'.format(project,branch), name, '');
	}

	function validationStampImage (project, branch, stamp) {
	    return '<img class="tooltip-source" width="24" title="{2}" src="gui/project/{0}/branch/{1}/validation_stamp/{2}/image" />'.format(
               					project.html(),
               					branch.html(),
               					stamp.name.html()
               					);
	}
	
	function validationStampTemplate (project, branch) {
	    return Template.config({
	        url: 'ui/manage/project/{0}/branch/{1}/validation_stamp'.format(project,branch),
	        render: Template.asTableTemplate('validationStampTemplate')
	    });
	}
	
	function editImage () {
		$('#validation_stamp-image-form').toggle();
	}
	
	function editImageCancel() {
		$('#validation_stamp-image-form').hide();
	}
	
	return {
		createValidationStamp: createValidationStamp,
		deleteValidationStamp: deleteValidationStamp,
		updateValidationStamp: updateValidationStamp,
		validationStampTemplate: validationStampTemplate,
		validationStampImage: validationStampImage,
		editImage: editImage,
		editImageCancel: editImageCancel
	};
	
} ();