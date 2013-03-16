// Workaround for console.log problem.
if (!window.console) window.console = {};
if (!window.console.log) window.console.log = function () { };

// Azurite Library
var azurite = { };

try {
    // Being run in an IDE
	azurite.initialize = __AZURITE__initialize;
	azurite.selectiveUndo = __AZURITE__selectiveUndo;
	azurite.undoEverythingAfterSelection = __AZURITE__undoEverythingAfterSelection;
	azurite.jump = __AZURITE__jump;
	azurite.getInfo = __AZURITE__getInfo;
	azurite.markerMove = __AZURITE__markerMove;
    
    azurite.log = __AZURITE__log;
} catch (e) {
    // Being run in a web browser.
    var alertFn;
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
//		alertFn('azurite.getInfo(' + project + ', ' + path + ', ' + sid + ', ' + id + ') call');
		return "unknown";
	};
	
	azurite.markerMove = function (absTimestamp) {
		alertFn('azurite.markerMove(' + absTimestamp + ') call');
	};
    
    azurite.log = function (msg) {
        alertFn(msg);
    };
}
