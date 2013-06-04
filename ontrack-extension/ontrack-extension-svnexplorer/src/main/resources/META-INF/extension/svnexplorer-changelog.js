define(['jquery','ajax','render','common'], function ($, ajax, render, common) {

    var revisions = null;
    var issues = null;
    var files = null;
    var info = null;

    function toggleMergedRevision (parentRevision) {
        $('tr[parent="{0}"]'.format(parentRevision)).toggle();
    }

    function displayRevisions (data) {
        // Stores the revisions (local cache for display purpose only)
        revisions = data;
        // Computation for the layout
        var currentLevel = 0;
        $.each (revisions.list, function (index, entry) {
            // Merge management
            entry.merge = false;
            entry.merged = false;
            var level = entry.level;
            if (level > currentLevel) {
                var previous = revisions.list[index - 1];
                // The previous entry is a merge
                previous.merge = true;
                // Parent for this entry
                entry.merged = true;
                entry.mergeParent = previous.revision;
            } else if (level < currentLevel) {
                // Merge section done
            } else {
                // Same level
                // Copies properties from previous item
                if (index > 0) {
                    var previous = revisions.list[index - 1];
                    entry.merged = previous.merged;
                    entry.mergeParent = previous.mergeParent;
                }
            }
            // Change the current level
            currentLevel = level;
        });
        // Rendering
        render.renderInto(
            $('#revisions'),
            'extension/svnexplorer-changelog-revisions',
            revisions,
            function () {
                // Merge actions
                $('#revisions').find('.merge-button').each(function (index, action) {
                    var revision = $(action).attr('revision');
                    $(action).click(function () {
                        toggleMergedRevision(revision);
                    });
                });
                // Tooltips
                common.tooltips();
            }
        );
    }

    function displayIssues (data) {
        // Stores the issues (local cache for display purpose only)
        issues = data;
        // Computed fields
        $.each (issues.list, function (index, changeLogIssue) {
            changeLogIssue.lastRevision = changeLogIssue.revisions[changeLogIssue.revisions.length - 1];
        });
        // Rendering
        render.renderInto(
            $('#issues'),
            'extension/svnexplorer-changelog-issues',
            issues,
            function () {
                // Tooltips
                common.tooltips();
            }
        );
    }

    function displayFiles (data) {
        // Stores the files (local cache for display purpose only)
        files = data;
        // Computed fields
        $.each (files.list, function (index, changeLogFile) {
            $.each (changeLogFile.changes, function (i, changeLogFileChange) {
                var changeType = changeLogFileChange.changeType;
                var icon;
                if (changeType == "modified") {
                    icon = "icon-asterisk";
                } else if (changeType == "added") {
                    icon = "icon-plus-sign";
                } else if (changeType == "deleted") {
                    icon = "icon-minus-sign";
                } else {
                    icon = "icon-question-sign";
                }
                changeLogFileChange.changeIcon = icon;
            });
        });
        // Rendering
        render.renderInto(
            $('#files'),
            'extension/svnexplorer-changelog-files',
            files,
            function () {
                // Tooltips
                common.tooltips();
            }
        );
    }

    function displayInfo (data) {
        // Stores the information (local cache for display purpose only)
        info = data;

        // 1) Issue status

        // Processing, % of width
        var total = 0;
        $.each (info.statuses, function (index, statusInfo) {
            total += statusInfo.count;
        });
        var reference = 400;
        $.each (info.statuses, function (index, statusInfo) {
            var ratio = 1.0 * statusInfo.count / total;
            statusInfo.width = Math.round(ratio * reference);
        });
        // Rendering
        render.renderInto(
            $('#info-status'),
            'extension/svnexplorer-changelog-info-issues',
            info,
            function () {
                // Tooltips
                common.tooltips();
            }
        );

        // 2) Sensible files

        // None
        if (info.sensibleFiles.length == 0) {
            $('<div></div>')
                .addClass('alert')
                .addClass('alert-info')
                .text('svnexplorer.changelog.info.files.none'.loc())
                .appendTo($('#info-files'));
        } else {
            render.renderInto(
                $('#info-files'),
                'extension/svnexplorer-changelog-info-sensible',
                info,
                function () {
                    // Tooltips
                    common.tooltips();
                }
            );
        }
    }

    function loadSummary() {
        // Nothing to load, just adjust the hash
        location.hash = "";
    }

    function loadRevisions () {
        location.hash = "revisions";
        if (revisions == null) {
            // UUID for the change log
            var uuid = $('#changelog').val();
            // Loads the revisions
            ajax.get({
                url: 'ui/extension/svnexplorer/changelog/{0}/revisions'.format(uuid),
                loading: {
                    el: '#revisions',
                    mode: 'appendText'
                },
                successFn: displayRevisions,
                errorFn: changelogErrorFn()
            });
        }
    }

    function loadIssues () {
        location.hash = "issues";
        if (issues == null) {
            // UUID for the change log
            var uuid = $('#changelog').val();
            // Loads the issues
            ajax.get({
                url: 'ui/extension/svnexplorer/changelog/{0}/issues'.format(uuid),
                loading: {
                    el: '#issues',
                    mode: 'appendText'
                },
                successFn: displayIssues,
                errorFn: changelogErrorFn()
            });
        }
    }

    function loadFiles () {
        location.hash = "files";
        if (files == null) {
            // UUID for the change log
            var uuid = $('#changelog').val();
            // Loads the files
            ajax.get({
                url: 'ui/extension/svnexplorer/changelog/{0}/files'.format(uuid),
                loading: {
                    el: '#files',
                    mode: 'appendText'
                },
                successFn: displayFiles,
                errorFn: changelogErrorFn()
            });
        }
    }

    function loadInfo () {
        location.hash = "info";
        if (info == null) {
            // UUID for the change log
            var uuid = $('#changelog').val();
            // Loads the files
            ajax.get({
                url: 'ui/extension/svnexplorer/changelog/{0}/info'.format(uuid),
                loading: {
                    el: '#info',
                    mode: 'appendText'
                },
                successFn: displayInfo,
                errorFn: changelogErrorFn()
            });
        }
    }

    function changelogErrorFn () {
        return ajax.simpleAjaxErrorFn(ajax.elementErrorMessageFn('#changelog-error'));
    }

    function init () {
        $('#summary-tab').on('show', loadSummary);
        $('#revisions-tab').on('show', loadRevisions);
        $('#issues-tab').on('show', loadIssues);
        $('#files-tab').on('show', loadFiles);
        $('#info-tab').on('show', loadInfo);
        // Initial tab
        $(document).ready(function () {
            var hash = location.hash;
            if (hash != '' && hash != '#') {
                var tab = hash.substring(1);
                $('#{0}-tab'.format(tab)).tab('show');
            }
        });
    }

    init();

});