/*global __AZURITE__initialize, __AZURITE__selectiveUndo, __AZURITE__undoEverythingAfterSelection, __AZURITE__jump, __AZURITE__getInfo, __AZURITE__markerMove, __AZURITE__eclipseCommand, __AZURITE__log, __AZURITE__notifySelectionChanged */

// Workaround for console.log problem.
if (!window.console) { window.console = {}; }
if (!window.console.log) { window.console.log = function () { }; }

// Azurite Library
var azurite = { };

try {
    // Being run in an IDE
	azurite.initialize = __AZURITE__initialize;
	azurite.selectiveUndo = __AZURITE__selectiveUndo;
	azurite.jump = __AZURITE__jump;
	azurite.getInfo = __AZURITE__getInfo;
	azurite.markerMove = __AZURITE__markerMove;
	azurite.openAllFilesEditedInRange = __AZURITE__openAllFilesEditedInRange;
    
    azurite.eclipseCommand = __AZURITE__eclipseCommand;
	
	azurite.notifySelectionChanged = __AZURITE__notifySelectionChanged;
	
	azurite.eventColorFunc = __AZURITE__eventColorFunc;
	azurite.eventIconFunc = __AZURITE__eventIconFunc;
	azurite.eventDisplayFunc = __AZURITE__eventDisplayFunc;
    
    azurite.log = __AZURITE__log;
} catch (e) {
    // Being run in a web browser.
    var alertFn;
	
	azurite.suppressGetInfoLogs = true;
	
    if (console.log) {
        console.log("AZURITE: Running Azurite in a non-editor environment.");
        alertFn = function (arg) { console.log(arg); };
    } else {
        alertFn = alert;
    }
	
	azurite.initialize = function () {
		// Do nothing for now.
		alertFn('azurite.initialize() call');
	};
	
	azurite.selectiveUndo = function (arrayOfIds) {
		alertFn('azurite.selectiveUndo() call');
		alertFn(arrayOfIds);
	};
	
	azurite.undoEverythingAfterSelection = function (sid, id) {
		alertFn('azurite.undoEverythingAfterSelection(' + sid + ', ' + id + ') call');
	};
	
	azurite.jump = function (project, path, sid, id) {
		alertFn('azurite.jump(' + project + ', ' + path + ', ' + sid + ', ' + id + ') call');
	};
	
	azurite.getInfo = function (project, path, sid, id) {
		if (!azurite.suppressGetInfoLogs) {
			alertFn('azurite.getInfo(' + project + ', ' + path + ', ' + sid + ', ' + id + ') call');
		}
		
		return "unknown";
	};
	
	azurite.markerMove = function (absTimestamp) {
		alertFn('azurite.markerMove(' + absTimestamp + ') call');
	};

	azurite.openAllFilesEditedInRange = function (arrayOfPaths) {
		alertFn('azurite.openAllFiledEditedInRange() call');
		alertFn(arrayOfPaths);
	};
    
    azurite.eclipseCommand = function(eclipseCmdId) {
        alertFn('azurite.eclipseCommand(' + eclipseCmdId + ') call');
    };
	
	azurite.notifySelectionChanged = function () {
		alertFn('azurite.notifySelectionChanged() call');
	};
	
	azurite.eventColorFunc = function(d) {
		alertFn('azurite.eventColorFunc() call');
		
		switch (d.type) {
			case 'JUnitCommand':
				return 'green';

			case 'RunCommand':
				return 'green';

			case 'Annotation':
				return 'goldenrod';

			case 'org.eclipse.ui.file.save':
				return 'lightskyblue';

			case 'org.eclipse.egit.ui.team.Commit':
			case 'org.eclipse.egit.ui.team.Pull':
			case 'org.eclipse.egit.ui.team.Fetch':
			case 'org.eclipse.egit.ui.team.Push':
			case 'org.eclipse.egit.ui.team.Merge':
			case 'org.eclipse.egit.ui.team.Reset':
			case 'org.eclipse.egit.ui.team.Rebase':
			case 'org.eclipse.egit.ui.CheckoutCommand':
				return 'goldenrod';

			default:
				return 'gold';
		}
	};
	
	azurite.eventIconFunc = function(d) {
		alertFn('azurite.eventIconFunc() call');
		
		switch (d.type) {
			case 'JUnitCommand':
			case 'JUnitCommand(true)':
				return 'images/event_icons/junitsucc.gif';

			case 'JUnitCommand(false)':
				return 'images/event_icons/juniterr.gif';

			case 'RunCommand':
				return 'images/event_icons/run_exc.gif';

			case 'Tag':
				return 'images/event_icons/tag.png';

			case 'Annotation':
				return 'images/event_icons/annotation.png';

			case 'org.eclipse.ui.file.save':
				return 'images/event_icons/save_edit.gif';

			case 'org.eclipse.egit.ui.team.Commit':
				return 'images/event_icons/commit.gif';

			case 'org.eclipse.egit.ui.team.Pull':
				return 'images/event_icons/pull.gif';

			case 'org.eclipse.egit.ui.team.Fetch':
				return 'images/event_icons/fetch.gif';

			case 'org.eclipse.egit.ui.team.Push':
				return 'images/event_icons/push.gif';

			case 'org.eclipse.egit.ui.team.Merge':
				return 'images/event_icons/merge.gif';

			case 'org.eclipse.egit.ui.team.Reset':
				return 'images/event_icons/reset.gif';

			case 'org.eclipse.egit.ui.team.Rebase':
				return 'images/event_icons/rebase.gif';

			case 'org.eclipse.egit.ui.CheckoutCommand':
				return 'images/event_icons/checkout.gif';

			default:
				return 'images/event_icons/error.png';
		}
	};
	
	azurite.eventDisplayFunc = function(d) {
		alertFn('azurite.eventDisplayFunc() call');
		
		return true;
	};
    
    azurite.log = function (msg) {
        alertFn(msg);
    };
}
