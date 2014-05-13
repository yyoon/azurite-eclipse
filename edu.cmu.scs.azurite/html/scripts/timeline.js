/*jshint unused:vars, devel:true, jquery:true, globalstrict:true, browser:true */
/*global d3, azurite */

/* Things to be called from Azurite */
/*exported updateOperation, getRightmostTimestamp, addSelectionsByIds, removeSelectionsByIds, showBefore, showAfter, undo, undoEverythingAfterSelection, showAllFiles, showSelectedFile, showAllFilesInProject, jumpToLocation, showAllFilesEditedTogether, showMarkerAtTimestamp, hideMarker, hideFirebugUI, pushCurrentFile, popCurrentFile, addEvent, activateFirebugLite, showAllFilesEditedInRange, openAllFilesEditedInRange, removeAllSelections, showUp, showDown */

/* Things to be called manually when debugging */
/*exported test, testMarker, showEvents */

/**
 * Things should be executed at the beginning.
 */

"use strict";

// Disable selection.
document.unselectable = "on";
document.onselectstart = function() {
    return false;
};

/**
 * Constants. (Always use UPPER_CASE.)
 */
var MENU_PANEL_HEIGHT = 0;
var SVG_WRAPPER_PADDING = 5;

var TYPE_INSERT = 0;
var TYPE_DELETE = 1;
var TYPE_REPLACE = 2;

var RECT_RADIUS = 1;
var MIN_WIDTH = 7;
var MIN_HEIGHT = MIN_WIDTH;
var ROW_HEIGHT = 30;
var DEFAULT_RATIO = 100;

var TICKMARK_SIZE = 6;
var TICKMARK_WIDTH = 1;
var TICKMARK_COLOR = 'white';
var TICK_TEXT_OFFSET = 2;
var TICK_TEXT_COLOR = 'white';
var TICKS_MIN_INTERVAL = 200;
var TICKS_BACKGROUND = 'dimgray';

var FILE_NAME_OFFSET_X = 5;
var FILE_NAME_OFFSET_Y = 5;

var FILES_PORTION = 0.15;

var HIGHLIGHT_WIDTH = 2;

var INDICATOR_WIDTH = 2;

var SCROLLBAR_WIDTH = 10;
var SCROLLBAR_DIST_THRESHOLD = 50;

var MARKER_WIDTH = 1;
var MARKER_SIZE = 10;

var RANGE_START_LINE_COLOR = 'white';
var RANGE_START_LINE_WIDTH = MARKER_WIDTH;
var RANGE_START_LINE_DASH_ARRAY = '5,5';

var EVENT_WIDTH = 1;
var EVENT_ICON_WIDTH = 16;
var EVENT_ICON_HEIGHT = 16;


// When changing one of the following heights, be sure to also change the corresponding css.
// (e.g., vscroll_wrapper's padding values)
// ----------------------------------------
var TICKS_HEIGHT = 30;
var EVENTS_HEIGHT = 20;

var CHART_MARGINS = {
	left : 10,
	top : 10,
	right : 10,
	bottom : 0
};
// ----------------------------------------


var MIN_SCROLL_THUMB_SIZE = 30;

// draw functions

var rectDraw = {};
rectDraw.xFunc = function(d) {
	return (d.t1 - d.session.startAbsTimestamp + d.sid) / DEFAULT_RATIO;
};
rectDraw.yFunc = function(d) {
	if (ROW_HEIGHT * (d.y2 - d.y1) / 100 <= MIN_HEIGHT) {
		return (ROW_HEIGHT - MIN_HEIGHT) * d.y1 / (100 - (d.y2 - d.y1));
	} else {
		return ROW_HEIGHT * d.y1 / 100;
	}
};
rectDraw.wFunc = function(d) {
	return Math.max(MIN_WIDTH / global.scaleX, (d.t2 - d.t1) / DEFAULT_RATIO);
};
rectDraw.hFunc = function(d) {
	return Math.max(MIN_HEIGHT / global.scaleY, ROW_HEIGHT * (d.y2 - d.y1) / 100);
};
rectDraw.fillFunc = function(d) {
	return d.color();
};

var fileDraw = {};
fileDraw.yFunc = function(d, i) {
	return (ROW_HEIGHT * i + global.translateY) * global.scaleY + FILE_NAME_OFFSET_Y;
};

var fileRectDraw = {};
fileRectDraw.yFunc = function (d, i) {
	return (ROW_HEIGHT * i + global.translateY) * global.scaleY;
};
fileRectDraw.wFunc = function (d) {
	return getSvgWidth() * FILES_PORTION;
};
fileRectDraw.hFunc = function (d, i) {
	return ROW_HEIGHT * global.scaleY;
};

var lineDraw = {};
lineDraw.x2Func = function(d) {
	return getSvgWidth() * (1.0 - FILES_PORTION);
};
lineDraw.yFunc = function(d) {
	return (ROW_HEIGHT * d + global.translateY) * global.scaleY;
};

var indicatorDraw = {};
indicatorDraw.y2Func = function() {
	return global.getVisibleFiles().length * ROW_HEIGHT + getSvgHeight();
};
indicatorDraw.wFunc = function() {
	return INDICATOR_WIDTH / global.scaleX;
};

var eventDraw = {};
eventDraw.xFunc = function(d) {
	return timestampToPixel(d.dt);
};
eventDraw.y1Func = function() {
	return -(getSvgHeight() - TICKS_HEIGHT - EVENTS_HEIGHT);
};
eventDraw.colorFunc = function(d) {
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
eventDraw.iconFunc = function(d) {
	switch (d.type) {
		case 'JUnitCommand':
		case 'JUnitCommand(true)':
			return 'images/event_icons/junitsucc.gif';

		case 'JUnitCommand(false)':
			return 'images/event_icons/juniterr.gif';

		case 'RunCommand':
			return 'images/event_icons/run_exc.gif';

		case 'Annotation':
			return 'images/event_icons/tag.png';

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
eventDraw.iconXFunc = function(d) {
	return timestampToPixel(d.dt) - EVENT_ICON_WIDTH / 2;
};

var timeTickDraw = {};
timeTickDraw.xFunc = function(d) {
	return timestampToPixel(d);
};
timeTickDraw.textXFunc = function(d) {
	return timestampToPixel(d) + TICK_TEXT_OFFSET;
};

/**
 * Global variables. (Always use a pseudo-namespace.)
 */
var global = {};

global.operationCompareFunc = function (lhs, rhs) {
	if (lhs.__data__.t1 < rhs.__data__.t1) {
		return -1;
	}
	if (lhs.__data__.t1 > rhs.__data__.t1) {
		return 1;
	}
	if (lhs.__data__.id < rhs.__data__.id) {
		return -1;
	}
	if (lhs.__data__.id > rhs.__data__.id) {
		return 1;
	}
	return 0;
};

global.oidCompareFunc = function (lhs, rhs) {
	if (lhs.sid < rhs.sid) {
		return -1;
	}
	if (lhs.sid > rhs.sid) {
		return 1;
	}
	if (lhs.id < rhs.id) {
		return -1;
	}
	if (lhs.id > rhs.id) {
		return 1;
	}
	return 0;
};

global.rectDistCompareFunc = function (rect) {
	var bounds = rect.getBBox();
	if (bounds.x <= global.targetDist && global.targetDist <= bounds.x + bounds.width) {
		return 0;
	}
	if (global.targetDist < bounds.x) {
		return -1;
	}
	return 1;
};

global.filterVisibleFunc = function (d) {
	return d.isVisible();
};


// variables to remember the last window size
global.lastWindowWidth = null;
global.lastWindowHeight = null;

// arrays to keep
global.files = [];
global.fileStack = [];
global.sessions = [];
global.selectedRects = [];

// should either be null (no selection) or [startPixel, endPixel)
global.selectedPixelRange = null;
// should either be null (no selection) or [startAbsTimestamp, endAbsTimestamp)
global.selectedTimestampRange = null;

global.events = [];

global.ticks = [];
global.dates = [];

global.getVisibleFiles = function () {
	return global.files.filter(function (e, i, a) {
		return e.isVisible();
	});
};

// layout
var LayoutEnum = {
	REALTIME : 0,
	COMPACT : 1
};

// Use COMPACT by default.
// TODO Save this to somewhere in the preferences store.
global.layout = LayoutEnum.COMPACT;

// transforms
global.translateX = 0;
global.translateY = 0;
global.scaleX = 1;
global.scaleY = 1;

// last file opened
global.currentFileIndex = -1;
global.currentFile = null;
global.lastOperation = null;

// Dragging
global.dragging = false;
global.dragStart = [];
global.draggableArea = {
	left : 0,
	top : 0,
	right : 0,
	bottom : 0
};

global.fileArea = {
	left: 0,
	top: 0,
	right: 0,
	bottom: 0
};

global.hscrollArea = {
	left: 0,
	top: 0,
	right: 0,
	bottom: 0
};

global.vscrollArea = {
	left: 0,
	top: 0,
	right: 0,
	bottom: 0
};

global.eventArea = {
	left: 0,
	top: 0,
	right: 0,
	bottom: 0
};

global.timeTickArea = {
	left: 0,
	top: 0,
	right: 0,
	bottom: 0
};

global.draggingHScroll = false;
global.draggingVScroll = false;
global.dragStartScrollPos = null;

global.draggingMarker = false;
global.draggingMarkerInTimeTickArea = false;
global.dragStartMarkerPos = null;
global.draggingMarkerShift = false;
global.diffWhileDraggingMarker = 0;

global.markerTimestamp = 0;
global.markerPos = null;

// profile flag
global.profile = false;

// context menu
var cmenu = {};
cmenu.isRightButtonDown = false;
cmenu.mousePos = [];
cmenu.typeName = '';

global.isCtrlDown = false;
global.isMac = navigator.appVersion.indexOf("Mac") !== -1;

// time scale (initialize with a default scale)
global.timeScale = d3.time.scale()
	.domain([new Date(0), new Date()])
	.range([0, 0]);
global.domainArray = [];
global.rangeArray = [];
global.timeScale.clamp(true);

// Move to front function for d3 selection.
d3.selection.prototype.moveToFront = function() {
	return this.each(function(){
		this.parentNode.appendChild(this);
	});
};

/**
 * SVG Setup.
 */
var svg = {};

function setupSVG() {
	svg.main = d3.select('#svg_inner_wrapper').append('svg')
		.attr('class', 'svg');

	svg.subFiles = svg.main.append('g')
		.attr('id', 'sub_files')
		.attr('clip-path', 'url(#clipFiles)');

	svg.subRectsWrap = svg.main.append('g')
		.attr('id', 'sub_rects_wrap')
		.attr('clip-path', 'url(#clipRectsWrap)');

	svg.subRects = svg.subRectsWrap.append('g')
		.attr('id', 'sub_rects');

	// The order of ticks / events / markers are important!
	// The marker part should appear later than the ticks / events, so that the marker is fully shown
	svg.subTicksWrap = svg.main.append('g')
		.attr('id', 'sub_ticks_wrap')
		.attr('clip-path', 'url(#clipTicksWrap)');
	svg.subTicksBackground = svg.subTicksWrap.append('rect');
	svg.subTicksBackground
		.attr('id', 'ticks_background')
		.attr('fill', TICKS_BACKGROUND);

	svg.subTicks = svg.subTicksWrap.append('g')
		.attr('id', 'sub_ticks');

	svg.subEventsWrap = svg.main.append('g')
		.attr('id', 'sub_events_wrap')
		.attr('clip-path', 'url(#clipEventsWrap)');

	svg.subEvents = svg.subEventsWrap.append('g')
		.attr('id', 'sub_events');
	
	svg.subMarkerWrap = svg.main.append('g')
		.attr('id', 'sub_marker_wrap')
		.attr('clip-path', 'url(#clipMarkerWrap)');
	
	svg.subMarkers = svg.subMarkerWrap.append('g')
		.attr('id', 'sub_markers');

	svg.subRangeBox = svg.subMarkers.append('rect')
		.attr('id', 'range_selection_box')
		.style('display', 'none');

	svg.subRangeStartLine = svg.subMarkers.append('line')
		.attr('id', 'range_start_line')
		.attr('stroke-width', RANGE_START_LINE_WIDTH)
		.attr('stroke-dasharray', RANGE_START_LINE_DASH_ARRAY)
		.attr('stroke', RANGE_START_LINE_COLOR)
		.style('display', 'none');

	svg.subMarker = svg.subMarkers.append('g')
		.attr('id', 'sub_marker')
		.style('display', 'none');	// hidden initially.
	
	svg.subMarker.append('line')
		.attr('id', 'marker_line')
		.attr('class', 'marker')
		.attr('y1', -MARKER_SIZE / 2)
		.attr('stroke-width', MARKER_WIDTH)
	svg.subMarker.append('path')
		.attr('id', 'marker_upper_triangle')
		.attr('class', 'marker')
		.attr('d', 'M 0 0 L ' + (-MARKER_SIZE / 2) + ' ' + (-MARKER_SIZE) + ' ' + (MARKER_SIZE / 2) + ' ' + (-MARKER_SIZE));
/*	svg.subMarker.append('path')
		.attr('id', 'marker_lower_triangle')
		.attr('class', 'marker')
		.attr('d', 'M 0 0 L ' + (-MARKER_SIZE / 2) + ' ' + MARKER_SIZE + ' ' + (MARKER_SIZE / 2) + ' ' + MARKER_SIZE)
		.attr('fill', MARKER_COLOR);*/
	svg.subMarkerText = svg.subMarker.append('text');
	svg.subMarkerText
		.attr('id', 'marker_time_text')
		.attr('class', 'marker')
		.attr('x', MARKER_SIZE)
		.attr('y', global.isMac ? -MARKER_SIZE / 2 : -3)
		.attr('alignment-baseline', 'central')
		.attr('fill', TICK_TEXT_COLOR)
		.attr('font-size', MARKER_SIZE + 'px')
		.text('This is a test! gq');

	svg.main.append('rect')
		.attr('class', 'selection_box')
		.style('fill', 'yellow')
		.style('opacity', 0.3);

	svg.clipFiles = svg.main.append('clipPath')
		.attr('id', 'clipFiles')
		.append('rect');

	svg.clipRectsWrap = svg.main.append('clipPath')
		.attr('id', 'clipRectsWrap').append('rect');
	
	svg.clipMarkerWrap = svg.main.append('clipPath')
		.attr('id', 'clipMarkerWrap').append('rect');

	svg.clipTicksWrap = svg.main.append('clipPath')
		.attr('id', 'clipTicksWrap').append('rect');

	svg.clipEventsWrap = svg.main.append('clipPath')
		.attr('id', 'clipEventsWrap').append('rect');

	recalculateClipPaths();
}

function recalculateClipPaths() {
	var svgWidth = getSvgWidth();
	var svgHeight = getSvgHeight();

	svg.clipFiles
		.attr('x', '-1')
		.attr('y', '-1')
		.attr('width', (svgWidth * FILES_PORTION + 2) + 'px')
		.attr('height', (svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT + 2) + 'px');

	svg.subFiles
		.attr('transform', 'translate(' + CHART_MARGINS.left + ' ' + CHART_MARGINS.top + ')');

	svg.subRectsWrap
		.attr('transform', 'translate(' + (CHART_MARGINS.left + svgWidth * FILES_PORTION) + ' ' + CHART_MARGINS.top + ')');

	svg.clipRectsWrap
		.attr('y', '-1')
		.attr('width', (svgWidth * (1.0 - FILES_PORTION)) + 'px')
		.attr('height', (svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT + 2) + 'px');
	
	svg.subMarkerWrap
		.attr('transform', 'translate(' + (CHART_MARGINS.left + svgWidth * FILES_PORTION) + ' ' + CHART_MARGINS.top + ')');
	svg.subMarker.select('#marker_line').attr('y2', svgHeight);
	svg.subMarker.select('#marker_lower_triangle').attr('transform', 'translate(0, ' + (svgHeight - TICKS_HEIGHT) + ')');

	svg.subRangeBox.attr('height', svgHeight);
	svg.subRangeStartLine.attr('y2', svgHeight);
	
	svg.clipMarkerWrap
		.attr('y', -MARKER_SIZE)
		.attr('width', (svgWidth * (1.0 - FILES_PORTION)))
		.attr('height', (svgHeight + 2 * MARKER_SIZE));

	svg.subTicksWrap
		.attr('transform', 'translate(' + (CHART_MARGINS.left + svgWidth * FILES_PORTION) + ' ' + (CHART_MARGINS.top + svgHeight - TICKS_HEIGHT) + ')');

	svg.clipTicksWrap
		.attr('y', -TICKMARK_SIZE / 2)
		.attr('width', (svgWidth * (1.0 - FILES_PORTION)))
		.attr('height', TICKS_HEIGHT + TICKMARK_SIZE);
	svg.subTicksBackground
		.attr('y', 0)
		.attr('width', (svgWidth * (1.0 - FILES_PORTION)))
		.attr('height', TICKS_HEIGHT);

	svg.subEventsWrap
		.attr('transform', 'translate(' + (CHART_MARGINS.left + svgWidth * FILES_PORTION) + ' ' + (CHART_MARGINS.top + svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT) + ')');
	svg.subEvents.selectAll('.event_line').attr('y1', eventDraw.y1Func);

	svg.clipEventsWrap
		.attr('y', -(svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT))
		.attr('width', (svgWidth * (1.0 - FILES_PORTION)))
		.attr('height', svgHeight - TICKS_HEIGHT);
}

/**
 * An object that keeps track of insert, delete and replace for each file.
 */
function File(project, path, fileName) {
	this.project = project;
	this.path = path;
	this.fileName = fileName;
	
	this.visible = true;
	
	this.isVisible = function () {
		return this.visible;
	};
}

/**
 * A session which keeps track of the session id (starting timestamp) and
 * other meta data.
 */
function Session(sid, current) {
	this.sid = sid;
	this.fileGroups = [];
	
	this.g = svg.subRects.append('g');
	this.g.attr('class', 'session_group')
		.attr('id', 'sg_' + this.sid);
	this.g.datum(this);
	
	var color = current ? 'yellow' : 'gray';
	
	this.indicator = this.g.append('line');
	this.indicator.attr('id', 'indicator_' + sid)
		.attr('class', 'indicator')
		.attr('stroke', color)
		.attr('x1', indicatorDraw.xFunc)
		.attr('x2', indicatorDraw.xFunc)
		.attr('y2', indicatorDraw.y2Func)
		.attr('stroke-width', indicatorDraw.wFunc);
	
	this.startAbsTimestamp = -1;
	this.endAbsTimestamp = -1;
	
	this.findFileGroup = function (file) {
		var i;
		for (i = 0; i < this.fileGroups.length; ++i) {
			if (this.fileGroups[i].file === file) {
				return this.fileGroups[i];
			}
		}
		
		return null;
	};
	
	global.sessions.push(this);
	global.sessions.sort(function (lhs, rhs) {
		return lhs.sid - rhs.sid;
	});
}

function FileGroup(session, file) {
	this.session = session;
	this.file = file;
	this.operations = [];
	
	this.g = this.session.g.append('g');
	this.g.attr('class', 'file_group')
		.attr('id', 'fg_' + this.session.sid + '_' + this.file.path);
	this.g.datum(this);
	
	this.session.fileGroups.push(this);
	
	this.isVisible = function () {
		return this.file.visible;
	};
}

/**
 * An object for representing an operation itself.
 */
function EditOperation(sid, id, t1, t2, y1, y2, type, fileGroup) {
	this.sid = sid;
	this.id = id;
	this.t1 = t1;
	this.t2 = (t2 === null || t1 === t2 ? t1 + 1 : t2);
	this.y1 = y1;
	this.y2 = y2;
	this.type = type;
	this.typeString = function() {
		switch (this.type) {
		case TYPE_INSERT:
			return "type_insert";

		case TYPE_DELETE:
			return "type_delete";

		case TYPE_REPLACE:
			return "type_replace";

		case TYPE_DIFF_INSERT:
			return "type_diff_insert";

		case TYPE_DIFF_DELETE:
			return "type_diff_delete";
		}
	}

	this.color = function() {
		if (this.type === TYPE_INSERT) {
			return "#0a760a";
		} else if (this.type === TYPE_DELETE) {
			return "#ec1313";
		} else if (this.type === TYPE_REPLACE) {
			return "#1313ec";
		}
	};
	
	this.fileGroup = fileGroup;
	this.session = fileGroup.session;
	
	this.visible = true;
	
	this.isVisible = function () {
		return this.fileGroup.isVisible() && this.visible;
	};
	
	this.getAbsT1 = function() {
		return this.sid + this.t1;
	};
	
	this.getAbsT2 = function() {
		return this.sid + this.t2;
	};
	
	this.getInfo = function() {
		var date = new Date(this.sid + this.t1);
		var info = azurite.getInfo(
				this.fileGroup.file.project, this.fileGroup.file.path,
				this.sid, this.id);
		
		return date.toLocaleString() + '<br>' + info;
	};
}

/**
 * A unique identifier for each operation
 */
function OperationId(sid, id) {
	this.sid = sid;
	this.id = id;
}

/**
 * Event object for generic events displayed in the timeline.
 */
function Event(sid, id, t, dt, type, desc) {
	this.sid = sid;
	this.id = id;
	this.t = t;
	this.dt = dt;
	this.type = type;
	this.desc = desc;
}

/**
 * Called by Azurite.
 * Adds a new row if the given file is not already in the list.
 */
function addFile(project, path) {
	var fileName = path.match(/[^\\\/]+$/)[0];

	for (var index = 0; index < global.files.length; ++index) {
		if (global.files[index].path === path) {
			global.currentFile = global.files[index];
			global.currentFileIndex = index;
			
			return;
		}
	}

	var newFile = new File(project, path, fileName);

	global.files.push(newFile);
	global.currentFile = newFile;
	global.currentFileIndex = global.files.length - 1;
	
	layoutFiles();
}

function updateSeparatingLines() {
	svg.subRectsWrap.selectAll('line.separating_line')
		.attr('y1', lineDraw.yFunc)
		.attr('x2', lineDraw.x2Func)
		.attr('y2', lineDraw.yFunc);
}

/**
 * Called by Azurite.
 * Add an edit operation to the end of the file.
 * Note that this is called immediately after an edit operation is performed.
 */
function addOperation(sid, id, t1, t2, y1, y2, type, scroll, autolayout, current) {
	if (global.currentFile === null) {
		return;
	}
	
	// profiling
	var _startTick = new Date().valueOf();
	if (global.profile) {
		console.log("addOperation() start: " + _startTick);
	}
	
	sid = parseInt(sid, 10);
	id = parseInt(id, 10);
	t1 = parseInt(t1, 10);
	t2 = parseInt(t2, 10);
	y1 = parseFloat(y1);
	y2 = parseFloat(y2);
	type = parseInt(type, 10);
	
	var layoutDirty = false;
	var filesLayoutDirty = false;
	
	var session = findSession(sid);
	if (session === null) {
		session = new Session(sid, current);
		session.startAbsTimestamp = sid + t1;
		session.endAbsTimestamp = sid + t2;
		
		if (autolayout === true) {
			layoutDirty = true;
		}
	}
	
	var fileGroup = session.findFileGroup(global.currentFile);
	if (fileGroup === null) {
		fileGroup = new FileGroup(session, global.currentFile);
		filesLayoutDirty = true;
	}

	var newOp = new EditOperation(sid, id, t1, t2, y1, y2, type, fileGroup);

	if (global.currenFile !== global.files[0]) {
		global.files.splice(global.currentFileIndex, 1);
		global.files.splice(0, 0, global.currentFile);
		global.currentFileIndex = 0;
		
		filesLayoutDirty = true;
	}

	fileGroup.operations.push(newOp);
	global.lastOperation = newOp;
	
	session.startAbsTimestamp = Math.min(session.startAbsTimestamp, sid + newOp.t1);
	session.endAbsTimestamp = Math.max(session.endAbsTimestamp, sid + newOp.t2);

	var rectToAppend = fileGroup.g.append('rect');
	rectToAppend.datum(newOp);
	rectToAppend
		.attr('id', function(d) {
			return d.sid + '_' + d.id;
		})
		.attr('class', function(d) {
			return 'op_rect ' + d.typeString();
		})
		.attr('y', rectDraw.yFunc)
		.attr('width', rectDraw.wFunc)
		.attr('height', rectDraw.hFunc)
		.attr('rx', RECT_RADIUS)
		.attr('ry', RECT_RADIUS)
		.attr('vector-effect', 'non-scaling-stroke');
	
	if (autolayout === true) {
		var sessionTx = session.tx;
		if (sessionTx === undefined) {
			sessionTx = 0;
		}

		if (global.layout === LayoutEnum.COMPACT) {
			// Find the last visible rect in this session.
			var rectsInSession = session.g.selectAll('rect.op_rect').filter(function (d) {
				return d.isVisible() && d !== newOp;
			})[0].slice();
			rectsInSession.sort(global.operationCompareFunc);
			
			var x = 0;
			if (rectsInSession.length > 0) {
				var bounds = rectsInSession[rectsInSession.length - 1].getBBox();
				x = bounds.x + bounds.width;
			}
			
			rectToAppend.attr('x', x);

			global.domainArray.push(new Date(sid + newOp.t1));
			global.domainArray.push(new Date(sid + newOp.t2));
			global.rangeArray.push(sessionTx + x);
			global.rangeArray.push(sessionTx + x + rectDraw.wFunc(newOp));
		}
		else if (global.layout === LayoutEnum.REALTIME) {
			rectToAppend.attr('x', rectDraw.xFunc);

			// Assuming this session is the last one...
			if (global.domainArray.length === 0) {
				global.domainArray.push(new Date(session.startAbsTimestamp));
				global.domainArray.push(new Date(session.endAbsTimestamp));
				global.rangeArray.push(sessionTx + rectDraw.xFunc(newOp));
				global.rangeArray.push(sessionTx + rectDraw.xFunc(newOp) + rectDraw.wFunc(newOp));
			}
			else {
				global.domainArray[global.domainArray.length - 1] = new Date(session.endAbsTimestamp);
				global.rangeArray[global.rangeArray.length - 1] = sessionTx + rectDraw.xFunc(newOp) + rectDraw.wFunc(newOp);
			}
		}

		global.timeScale.domain(global.domainArray).range(global.rangeArray);

		// Time ticks
		if (global.ticks.length === 0) {
			updateTicks();
		} else {
			var lastTick = global.ticks[global.ticks.length - 1];
			var currentPixel = timestampToPixel(newOp.t2);
			var lastTickPixel = timeTickDraw.xFunc(lastTick);

			if (currentPixel - lastTickPixel > getTicksMaxInterval()) {
				updateTicks();
			}
		}
	}
	
	// Add tipsy.
	$(rectToAppend.node()).tipsy({
		gravity: $.fn.tipsy.autoNS,
		html: true,
		checkFn: function() {
			return !global.dragging;
		},
		title: function() {
			var d = this.__data__;
			return d.getInfo();
		}
	});
	
	global.lastRect = rectToAppend;
	
	// Move the indicator.
	var indicatorX;
	if (global.layout === LayoutEnum.COMPACT) {
		var rectBounds = rectToAppend.node().getBBox();
		indicatorX = rectBounds.x + rectBounds.width;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);
	}
	else {
		indicatorX = (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);
	}
	
	if (filesLayoutDirty === true) {
		layoutFiles();
	}
	if (layoutDirty === true) {
		layout();
	}
	
	if (autolayout === true) {
		updateHScroll();
	}
	
	if (scroll === true) {
		showUntil(global.lastOperation.getAbsT2());
	}
	
	// profiling
	var _endTick = new Date().valueOf();
	if (global.profile) {
		console.log("addOperation() end: " + _endTick);
		console.log("addOperation() took: " + (_endTick - _startTick));
	}
}

/**
 * Called by Azurite.
 * Update the timestamp2 value for an existing operation, in case multiple
 * operations are merged into one.
 */
function updateOperation(sid, id, t2, y1, y2, type, scroll) {
	var lastOp = global.lastOperation;
	
	if (lastOp === null ||
		lastOp.sid !== parseInt(sid, 10) ||
		lastOp.id !== parseInt(id, 10)) {
		return;
	}

	lastOp.t2 = t2;
	lastOp.session.endAbsTimestamp = Math.max(lastOp.session.endAbsTimestamp, lastOp.getAbsT2());
	
	lastOp.y1 = y1;
	lastOp.y2 = y2;

	lastOp.type = type;

	if (global.lastRect !== null) {
		global.lastRect
			.attr('width', rectDraw.wFunc)
			.attr('y', rectDraw.yFunc)
			.attr('height', rectDraw.hFunc)
			.attr('fill', rectDraw.fillFunc);
	}
	
	var session = lastOp.session;
	var sessionTx = session.tx;
	if (sessionTx === undefined) {
		sessionTx = 0;
	}
	
	// Move the indicator / adjust time scale
	var indicatorX;
	if (global.layout === LayoutEnum.COMPACT) {
		var rectBounds = global.lastRect.node().getBBox();
		indicatorX = rectBounds.x + rectBounds.width;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);

		global.domainArray[global.domainArray.length - 1] = new Date(lastOp.sid + t2);
		global.rangeArray[global.rangeArray.length - 1] = sessionTx + rectBounds.x + rectBounds.width;
	}
	else {
		indicatorX = (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);

		global.domainArray[global.domainArray.length - 1] = new Date(session.endAbsTimestamp);
		global.rangeArray[global.rangeArray.length - 1] = sessionTx + rectDraw.xFunc(lastOp) + rectDraw.wFunc(lastOp);
	}
	global.timeScale.domain(global.domainArray).range(global.rangeArray);

	// Update time ticks if necessary.
	var lastTick = global.ticks[global.ticks.length - 1];
	var currentPixel = timestampToPixel(lastOp.t2);
	var lastTickPixel = timeTickDraw.xFunc(lastTick);

	if (currentPixel - lastTickPixel > getTicksMaxInterval()) {
		updateTicks();
	}
	
	if (scroll === true) {
		showUntil(lastOp.getAbsT2());
	}
}

/**
 * Called by Azurite
 */
function addEvent(sid, id, t, dt, type, desc) {
	var newEvent = new Event(sid, id, t, dt, type, desc);

	// Add it to the global list
	global.events.push(newEvent);

	// Add the line object
	var eventLine = svg.subEvents.append('line').datum(newEvent);
	eventLine
		.attr('class', 'event_line')
		.attr('x1', eventDraw.xFunc)
		.attr('x2', eventDraw.xFunc)
		.attr('y1', eventDraw.y1Func)
		.attr('y2', EVENTS_HEIGHT / 2)
		.attr('stroke-width', EVENT_WIDTH)
		.attr('stroke', eventDraw.colorFunc);

	// Add icon. The size should be 16x16
	var eventIcon = svg.subEvents.append('image').datum(newEvent);
	eventIcon
		.attr('class', 'event_icon')
		.attr('xlink:href', eventDraw.iconFunc)
		.attr('x', eventDraw.iconXFunc)
		.attr('y', (EVENTS_HEIGHT - EVENT_ICON_HEIGHT) / 2)
		.attr('width', EVENT_ICON_WIDTH)
		.attr('height', EVENT_ICON_HEIGHT);

	$(eventIcon.node()).tipsy({
		gravity: 'n',
		html: true,
		title: function() {
			var d = this.__data__;
			return d.desc;
		}
	});
}


function findSession(sid) {
	var i;
	for (i = 0; i < global.sessions.length; ++i) {
		if (global.sessions[i].sid === sid) {
			return global.sessions[i];
		}
	}
	
	return null;
}

function layout(newLayout) {
	// profiling
	var _startTick = new Date().valueOf();
	if (global.profile) {
		console.log("layout() start: " + _startTick);
	}
	
	// Remember the current horizontal scroll position.
	var leftmostTimestamp = getLeftmostTimestamp();
	
	// Change the layout, if specified.
	if (newLayout !== undefined) {
		global.layout = newLayout;
	}
	
	global.tempSessionTx = 0;
	
	var i, session;
	var compactXFunc = function (d) {
		if (!d.isVisible()) {
			return 0;
		}
		
		var temp = global.tempX;
		global.tempX += this.getBBox().width;
		return temp;
	};

	// Domain values / range values for time scale
	global.domainArray = [];
	global.rangeArray = [];
	var addScaleFunc = function (d, i) {
		if (d.isVisible()) {
			global.domainArray.push(new Date(d.sid + d.t1));
			global.domainArray.push(new Date(d.sid + d.t2));
			global.rangeArray.push(global.tempSessionTx + this.getBBox().x);
			global.rangeArray.push(global.tempSessionTx + this.getBBox().x + this.getBBox().width);
		}
	};
	
	for (i = 0; i < global.sessions.length; ++i) {
		session = global.sessions[i];
		
		session.g.attr('transform', 'translate(' + global.tempSessionTx + ' 0)');
		session.tx = global.tempSessionTx;
		
		// Iterate through all the rects.
		var rects = session.g.selectAll('rect.op_rect')[0].slice();
		rects.sort(global.operationCompareFunc);
		
		// Apply different layout function, depending on the mode.
		global.tempX = 0;
		
		if (global.layout === LayoutEnum.COMPACT) {
			d3.selectAll(rects).attr('x', compactXFunc);
		}
		else if (global.layout === LayoutEnum.REALTIME) {
			d3.selectAll(rects).attr('x', rectDraw.xFunc);
		}
		
		// TODO if there is no rectangle at all, don't draw the entire session.
		
		// Move the indicator.
		if (global.layout === LayoutEnum.COMPACT) {
			session.indicator.attr('x1', global.tempX);
			session.indicator.attr('x2', global.tempX);

			// recalculate the time scales here.
			d3.selectAll(rects).each(addScaleFunc);
		}
		else if (global.layout === LayoutEnum.REALTIME) {
			session.indicator.attr('x1', (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO);
			session.indicator.attr('x2', (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO);

			global.domainArray.push(new Date(session.startAbsTimestamp));
			global.domainArray.push(new Date(session.endAbsTimestamp));
			global.rangeArray.push(session.tx);
			global.rangeArray.push(session.tx + (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO);
		}
		
		global.tempSessionTx += session.g.node().getBBox().width;
	}

	if (global.domainArray.length === 0) {
		global.timeScale.domain([new Date(0), new Date()]).range([0, 0]);
	}
	else {
		global.timeScale.domain(global.domainArray).range(global.rangeArray);
	}
	
	updateHighlight();
	updateHScroll();
	updateEvents();
	updateTicks();
	
	// Restore the scroll position.
	showFrom(leftmostTimestamp);

	// Marker / range selection box
	global.markerPos = timestampToPixel(global.markerTimestamp);
	if (global.selectedTimestampRange !== null) {
		global.selectedPixelRange = [
			timestampToPixel(global.selectedTimestampRange[0]),
			timestampToPixel(global.selectedTimestampRange[1])
		];
	}
	updateMarkerPosition(false, true);
	updateRangeSelectionBox();

	// profiling
	var _endTick = new Date().valueOf();
	if (global.profile) {
		console.log("layout() end: " + _endTick);
		console.log("layout() took: " + (_endTick - _startTick));
	}
}

function make_filter(file) {
	return function (d) {
		return d.file === file;
	};
}

function layoutFiles() {
	var i;
	var visibleFiles = [];

	var hiddenFileExists = false;
	
	for (i = 0; i < global.files.length; ++i) {
		var file = global.files[i];
		
		// Use a function maker.. http://stackoverflow.com/questions/3037598/how-to-get-around-the-jslint-error-dont-make-functions-within-a-loop
		var fileGroups = svg.subRects.selectAll('g.file_group').filter(make_filter(file));
		
		if (file.visible) {
			fileGroups.style('display', '');
		}
		else {
			fileGroups.style('display', 'none');
			hiddenFileExists = true;
			continue;
		}
		
		fileGroups.attr('transform', 'translate(0 ' + (visibleFiles.length * ROW_HEIGHT) + ')');
		
		visibleFiles.push(file);
	}
	
	// Rects surrounding the file names.
	var rects = svg.subFiles.selectAll('rect.file_rect').data(visibleFiles);
	rects.enter().insert('rect', ':first-child')
		.attr('class', 'file_rect')
		.attr('fill', 'lightgray')
		.attr('rx', '2')
		.attr('ry', '2')
		.attr('stroke', 'gray')
		.attr('stroke-width', '1')
		.attr('vector-effect', 'non-scaling-stroke');
	
	rects.exit().remove();
	
	rects.attr('x', 0)
		.attr('y', fileRectDraw.yFunc)
		.attr('width', fileRectDraw.wFunc)
		.attr('height', fileRectDraw.hFunc);
	
	// Labels
	var labels = svg.subFiles.selectAll('text').data(visibleFiles);
	labels.enter().append('text');
	labels.exit().remove();
	
	labels.attr('x', FILE_NAME_OFFSET_X)
		.attr('y', fileDraw.yFunc)
		.attr('dy', '1em')
		.attr('fill', 'black')
		.attr('font-family', 'courier')
		.text(function(d) {
			return d.fileName;
		});
	
	// Separating Lines
	var lines = svg.subRectsWrap.selectAll('line.separating_line')
		.data(range(0, visibleFiles.length + 1));
	
	lines.enter().insert('line', ':first-child')
		.attr('class', 'separating_line')
		.attr('x1', '0')
		.attr('stroke', 'gray')
		.attr('stroke-width', '1');
	
	lines.exit().remove();
	
	updateSeparatingLines();
	
	// Draw indicator
	d3.selectAll('.indicator').attr('y2', indicatorDraw.y2Func);

	// VScroll
	updateVScroll();

	// Show or Hide the unhide all button.
	d3.selectAll('#unhide_button').style('display', hiddenFileExists ? 'inline-block' : 'none');
}

function getLeftmostTimestamp() {
	return screenPixelToTimestamp(0);
}

function getRightmostTimestamp() {
	return screenPixelToTimestamp(getSvgWidth() * (1.0 - FILES_PORTION));
}

function screenPixelToTimestamp(screenPixel) {
	return pixelToTimestamp(-global.translateX + screenPixel);
}

function pixelToTimestamp(pixel) {
	return global.timeScale.invert(pixel / global.scaleX).getTime();
}

/*
 * When the page loads, load a log file
 */
function handleResize() {
	// if window size are different, redraw everything
	if (global.lastWindowWidth !== window.innerWidth || global.lastWindowHeight !== window.innerHeight) {
		global.lastWindowWidth = window.innerWidth;
		global.lastWindowHeight = window.innerHeight;

		recalculateClipPaths();
		updateSeparatingLines();
		updateAreas();
		updateTicks();

		updateHScroll();
		updateVScroll();
		
		svg.subFiles.selectAll('rect.file_rect').attr('width', fileRectDraw.wFunc);
	}
}

window.onload = function() {
	azurite.initialize();

	setupSVG();
	
	initEventHandlers();
	
	setTimeout(layout, 100);

	handleResize();
};

window.onresize = function(e) {
	handleResize();
};

function updateAreas() {
	var svgWidth = getSvgWidth();
	var svgHeight = getSvgHeight();

	global.draggableArea.left = CHART_MARGINS.left + svgWidth * FILES_PORTION;
	global.draggableArea.top = CHART_MARGINS.top;
	global.draggableArea.right = CHART_MARGINS.left + svgWidth;
	global.draggableArea.bottom = CHART_MARGINS.top + svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT;
	
	global.fileArea.left = CHART_MARGINS.left;
	global.fileArea.top = CHART_MARGINS.top;
	global.fileArea.right = CHART_MARGINS.left + svgWidth * FILES_PORTION;
	global.fileArea.bottom = CHART_MARGINS.top + svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT;

	global.hscrollArea.left = (CHART_MARGINS.left + CHART_MARGINS.right + svgWidth) * FILES_PORTION + SCROLLBAR_WIDTH;
	global.hscrollArea.top = CHART_MARGINS.top + CHART_MARGINS.bottom + svgHeight;
	global.hscrollArea.right = CHART_MARGINS.left + CHART_MARGINS.right + svgWidth - SCROLLBAR_WIDTH;
	global.hscrollArea.bottom = CHART_MARGINS.top + CHART_MARGINS.bottom + svgHeight + SCROLLBAR_WIDTH;

	global.vscrollArea.left = CHART_MARGINS.left + CHART_MARGINS.right + svgWidth;
	global.vscrollArea.top = CHART_MARGINS.top + SCROLLBAR_WIDTH;
	global.vscrollArea.right = CHART_MARGINS.left + CHART_MARGINS.right + svgWidth + SCROLLBAR_WIDTH;
	global.vscrollArea.bottom = CHART_MARGINS.top + svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT - SCROLLBAR_WIDTH;

	global.eventArea.left = CHART_MARGINS.left + svgWidth * FILES_PORTION;
	global.eventArea.top = CHART_MARGINS.top + svgHeight - TICKS_HEIGHT - EVENTS_HEIGHT;
	global.eventArea.right = CHART_MARGINS.left + svgWidth;
	global.eventArea.bottom = CHART_MARGINS.top + svgHeight - TICKS_HEIGHT;

	global.timeTickArea.left = CHART_MARGINS.left + svgWidth * FILES_PORTION;
	global.timeTickArea.top = CHART_MARGINS.top + svgHeight - TICKS_HEIGHT;
	global.timeTickArea.right = CHART_MARGINS.left + svgWidth;
	global.timeTickArea.bottom = CHART_MARGINS.top + svgHeight;
}

/******************************************************************
MOUSE EVENT FUNCTIONS
******************************************************************/

function initEventHandlers() {
	initMouseWheelHandler();
	initKeyEventHandlers();
	initMouseDownHandler();
	initMouseMoveHandler();
	initMouseUpHandler();
	initDblClickHandler();
}

function initMouseWheelHandler() {
	svg.main.on("mousewheel", function() {
		if (d3.event.shiftKey && d3.event.ctrlKey) {
			if (d3.event.wheelDelta > 0) {
				fileZoomIn();
			} else {
				fileZoomOut();
			}
		} else if (d3.event.shiftKey) {
			scrollDown(d3.event.wheelDelta / 10);
		} else if (d3.event.ctrlKey) {
			if (d3.event.wheelDelta < 0) {
				barZoomOut();
			} else {
				barZoomIn();
			}

			d3.event.preventDefault();
			d3.event.stopPropagation();
		} else {
			scrollRight(d3.event.wheelDelta / 10);
		}
	});
}

function initKeyEventHandlers() {
	document.addEventListener("keydown", function(e) {
		// Consider the cmd key as toggle key on Mac OS X when drag-selecting.
		if (e.keyCode === 17 && global.isMac === false) {
			global.isCtrlDown = true;
		}
		else if ((e.keyCode === 91 || e.keyCode === 93) && global.isMac === true) {
			global.isCtrlDown = true;
		}
		else if (e.keyCode === 107) {
			if (e.shiftKey) {
				fileZoomIn();
			}
			else {
				barZoomIn();
			}
		}
		else if (e.keyCode === 109) {
			if (e.shiftKey) {
				fileZoomOut();
			}
			else {
				barZoomOut();
			}
		}
		
	}, false);

	document.addEventListener("keyup", function(e) {
		// Consider the cmd key as toggle key on Mac OS X when drag-selecting.
		if (e.keyCode === 17 && global.isMac === false) {
			global.isCtrlDown = false;
		}
		else if ((e.keyCode === 91 || e.keyCode === 93) && global.isMac === true) {
			global.isCtrlDown = false;
		}
	}, false);
}

function initMouseDownHandler() {
	document.onmousedown = function(e) {
		if ("which" in e) { // Gecko (Firefox), WebKit (Safari/Chrome) & Opera
			cmenu.isRightButtonDown = e.which === 3;
		} else if ("button" in e) { // IE, Opera 
			cmenu.isRightButtonDown = e.button === 2;
		}

		var mouseX = e.clientX - SVG_WRAPPER_PADDING;
		var mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;

		// Check if the mouse was clicked on an event, in which case the marker should move to that place.
		// (whether or not it was the right button)
		if (cursorInArea(mouseX, mouseY, global.eventArea)) {
			var rect = svg.main.node().createSVGRect();
			rect.x = mouseX;
			rect.y = mouseY;
			rect.width = 1;
			rect.height = 1;

			// Get all the intersecting objects in the SVG.
			var list = svg.main.node().getIntersectionList(rect, null);

			// Filter only the icons.
			d3.selectAll(list).filter('.event_icon').each(function(d) {
				// In this case, this should be notified to the Azurite plug-in.
				showMarkerAtTimestamp(d.dt, true);
			});
		}

		if (cmenu.isRightButtonDown) {
			if (global.isMac) {
				showContextMenu(e);
			}

			return;
		}

		global.dragStart[0] = mouseX;
		global.dragStart[1] = mouseY;

		// Drag select
		if (cursorInArea(mouseX, mouseY, global.draggableArea)) {
			global.dragging = true;
			global.draggingHScroll = false;
			global.draggingVScroll = false;
			global.draggingMarker = false;
			global.draggingMarkerInTimeTickArea = false;

			d3.select('.selection_box').attr('x', mouseX).attr('y', mouseY);

			return;
		}
		// HScroll
		else if (cursorInArea(mouseX, mouseY, global.hscrollArea)) {
			(function () {	// anonymous function for the sake of local variables.
				var thumbSize = $('#hscroll_thumb').width();
				var thumbStart = parseInt($('#hscroll_thumb').css('left'), 10);
				var thumbEnd = thumbStart + thumbSize;

				var mousePos = mouseX - global.hscrollArea.left;

				if (mousePos < thumbStart) {
					showPageBefore();
					return;
				} else if (mousePos >= thumbEnd) {
					showPageAfter();
					return;
				} else {
					global.dragging = false;
					global.draggingHScroll = true;
					global.draggingVScroll = false;
					global.draggingMarker = false;
					global.draggingMarkerInTimeTickArea = false;

					global.dragStartScrollPos = thumbStart;
					return;
				}
			}());
			
			return;
		}
		// VScroll
		else if (cursorInArea(mouseX, mouseY, global.vscrollArea)) {
			(function () {
				var thumbSize = $('#vscroll_thumb').height();
				var thumbStart = parseInt($('#vscroll_thumb').css('top'), 10);
				var thumbEnd = thumbStart + thumbSize;

				var mousePos = mouseY - global.vscrollArea.top;

				if (mousePos < thumbStart) {
					showPageUp();
					return;
				} else if (mousePos >= thumbEnd) {
					showPageDown();
					return;
				} else {
					global.dragging = false;
					global.draggingHScroll = false;
					global.draggingVScroll = true;
					global.draggingMarker = false;
					global.draggingMarkerInTimeTickArea = false;

					global.dragStartScrollPos = thumbStart;
					return;
				}
			}());
			
			return;
		}
		else if (cursorInArea(mouseX, mouseY, global.timeTickArea)) {
			(function () {
				global.dragging = false;
				global.draggingHScroll = false;
				global.draggingVScroll = false;
				global.draggingMarker = false;
				global.draggingMarkerInTimeTickArea = true;

				var pixelPosition = -global.translateX + mouseX - global.timeTickArea.left;

				// Handle shift key and range selection ---------------------------------
				if (event.shiftKey) {
					global.draggingMarkerShift = true;

					if (global.selectedPixelRange !== null) {
						// If there is already a selected range, just update the end pixel
						updateEndPixelRange(pixelPosition);
					}
					else if (global.markerPos !== null) {
						// If there is no range selection, make a new one.
						selectPixelRange(global.markerPos, pixelPosition);
					}
				}
				else {
					global.draggingMarkerShift = false;

					deselectRange();
				}
				// ----------------------------------------------------------------------

				// Move the marker
				showMarkerAtPixel(pixelPosition);
			}());

			return;
		}
		// Check if the marker is clicked
		else if (cursorInMarker(mouseX, mouseY)) {
			global.dragging = false;
			global.draggingHScroll = false;
			global.draggingVScroll = false;
			global.draggingMarker = true;
			global.draggingMarkerInTimeTickArea = false;
			
			// Handle shift key only on mouse move
			if (event.shiftKey) {
				global.draggingMarkerShift = true;
			}
			else {
				global.draggingMarkerShift = false;

				deselectRange();
			}

			global.dragStartMarkerPos = global.markerPos;
			global.diffWhileDraggingMarker = 0;
			return;
		}

		global.dragging = false;
		global.draggingHScroll = false;
		global.draggingVScroll = false;
		global.draggingMarker = false;
		global.draggingMarkerInTimeTickArea = false;
	};
}

function initMouseMoveHandler() {
	document.onmousemove = function(e) {
		var mouseX = e.clientX - SVG_WRAPPER_PADDING;
		var mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;

		if (global.dragging) {
			var clampedPos = clampInArea(mouseX, mouseY, global.draggableArea);
			var newX = clampedPos[0];
			var newY = clampedPos[1];

			if (newX - global.dragStart[0] < 0) {
				d3.select('.selection_box').attr('x', newX).attr('width',
						global.dragStart[0] - newX);
			} else {
				d3.select('.selection_box').attr('x', global.dragStart[0])
						.attr('width', newX - global.dragStart[0]);
			}

			if (newY - global.dragStart[1] < 0) {
				d3.select('.selection_box').attr('y', newY).attr('height',
						global.dragStart[1] - newY);
			} else {
				d3.select('.selection_box').attr('y', global.dragStart[1])
						.attr('height', newY - global.dragStart[1]);
			}

			d3.select('.selection_box').attr('display', 'block');
		}
		// HScroll
		else if (global.draggingHScroll) {
			(function () {
				var mousePos = mouseX - global.dragStart[0] + global.dragStartScrollPos;

				// If the mouse position is too far from the scrollbar,
				// then return to the original position.
				if (mouseY < global.hscrollArea.top - SCROLLBAR_DIST_THRESHOLD || mouseY >= global.hscrollArea.bottom + SCROLLBAR_DIST_THRESHOLD) {
					mousePos = global.dragStartScrollPos;
				}

				var trackSize = $('#hscroll_thumbtrack').width();
				var thumbSize = $('#hscroll_thumb').width();

				var ratio = mousePos / (trackSize - thumbSize);
				ratio = clamp(ratio, 0.0, 1.0);

				var newTx = getMinTranslateX() * ratio;
				translateX(newTx);
			}());
		}
		// VScroll
		else if (global.draggingVScroll) {
			(function () {
				var mousePos = mouseY - global.dragStart[1] + global.dragStartScrollPos;

				// If the mouse position is too far from the scrollbar,
				// then return to the original position.
				if (mouseX < global.vscrollArea.left - SCROLLBAR_DIST_THRESHOLD || mouseX >= global.vscrollArea.right + SCROLLBAR_DIST_THRESHOLD) {
					mousePos = global.dragStartScrollPos;
				}

				var trackSize = $('#vscroll_thumbtrack').height();
				var thumbSize = $('#vscroll_thumb').height();

				var ratio = mousePos / (trackSize - thumbSize);
				ratio = clamp(ratio, 0.0, 1.0);

				var newTy = getMinTranslateY() * ratio;
				// Uncomment this to make vertical scrolling discrete.
				// newTy = Math.round(newTy / ROW_HEIGHT) * ROW_HEIGHT;
				translateY(newTy);
			}());
		}
		else if (global.draggingMarkerInTimeTickArea) {
			var pixelPosition = -global.translateX + mouseX - global.timeTickArea.left;

			// Handle shift key and range selection ---------------------------------
			if (global.draggingMarkerShift) {
				// Assume there is a time range selection.
				updateEndPixelRange(pixelPosition);
			}
			// ----------------------------------------------------------------------

			// Move the marker
			showMarkerAtPixel(pixelPosition);
		}
		else if (global.draggingMarker) {
			var markerPos = mouseX - global.dragStart[0] + global.dragStartMarkerPos + global.diffWhileDraggingMarker;

			// Handle shift key and range selection ---------------------------------
			if (global.draggingMarkerShift) {
				if (global.selectedPixelRange !== null) {
					updateEndPixelRange(markerPos);
				}
				else {
					selectPixelRange(global.dragStartMarkerPos, markerPos);
				}
			}
			// ----------------------------------------------------------------------

			showMarkerAtPixel(markerPos);
		}
	};
}

function initMouseUpHandler() {
	document.onmouseup = function(e) {
		var mouseX, mouseY;
		
		if (cmenu.isRightButtonDown) {
			// When not on a mac, show context menu on mouse up.
			if (global.isMac === false) {
				showContextMenu(e);
			}

			return;
		}

		if (global.dragging) {
			d3.select('.selection_box').attr('display', 'none');

			var x1, y1, x2, y2;

			mouseX = e.clientX - SVG_WRAPPER_PADDING;
			mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;

			if (global.dragStart[0] <= mouseX) {
				x1 = global.dragStart[0];
				x2 = mouseX;
			} else {
				x1 = mouseX;
				x2 = global.dragStart[0];
			}

			if (global.dragStart[1] <= mouseY) {
				y1 = global.dragStart[1];
				y2 = mouseY;
			} else {
				y1 = mouseY;
				y2 = global.dragStart[1];
			}

			addSelections(x1, y1, x2, y2, true, !global.isCtrlDown);
			global.dragStart = [];
		}

		global.dragging = false;
		global.draggingHScroll = false;
		global.draggingVScroll = false;
		global.draggingMarker = false;
		global.draggingMarkerInTimeTickArea = false;
	};
}

function initDblClickHandler() {
	document.ondblclick = function(e) {
		var mouseX = e.clientX - SVG_WRAPPER_PADDING;
		var mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;
		
		var rect = svg.main.node().createSVGRect();
		rect.x = mouseX;
		rect.y = mouseY;
		rect.width = 1;
		rect.height = 1;

		// Get all the intersecting objects in the SVG.
		var list = svg.main.node().getIntersectionList(rect, null);
		var oprect = d3.selectAll(list).filter('.op_rect');
		if (oprect.length > 0 && oprect[0].length > 0) {
			var datum = oprect[0][0].__data__;
			if (datum !== undefined && datum !== null) {
				var file = datum.fileGroup.file;
				azurite.jump(file.project, file.path, datum.sid, datum.id);
			}
		}
	};
}

function cursorInMarker(x, y) {
	var rect = svg.main.node().createSVGRect();
	rect.x = x;
	rect.y = y;
	rect.width = 1;
	rect.height = 1;

	// Get all the intersecting objects in the SVG.
	var list = svg.main.node().getIntersectionList(rect, null);

	return d3.selectAll(list).filter('.marker')[0].length > 0;
}

function cursorInEvent(x, y) {
	var rect = svg.main.node().createSVGRect();
	rect.x = x;
	rect.y = y;
	rect.width = 1;
	rect.height = 1;

	// Get all the intersecting objects in the SVG.
	var list = svg.main.node().getIntersectionList(rect, null);

	return d3.selectAll(list).filter('.event_icon')[0].length > 0;
}

function cursorInRangeSelectionBox(x, y) {
	var rect = svg.main.node().createSVGRect();
	rect.x = x;
	rect.y = y;
	rect.width = 1;
	rect.height = 1;

	// Get all the intersecting objects in the SVG.
	var list = svg.main.node().getIntersectionList(rect, null);

	return d3.selectAll(list).filter('#range_selection_box')[0].length > 0;
}

function cursorInArea(x, y, area) {
	return x >= area.left && x < area.right && y >= area.top && y < area.bottom;
}

function clampInArea(x, y, area) {
	return [ clamp(x, area.left, area.right - 1),
			clamp(y, area.top, area.bottom - 1) ];
}

// This functions sets the necessary information for Eclipse plug-in to show a context menu
function showContextMenu(e) {
	var mouseX = e.clientX - SVG_WRAPPER_PADDING;
	var mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;
	
	cmenu.mousePos = [mouseX, mouseY];
	
	if (cursorInRangeSelectionBox(mouseX, mouseY)) {
		cmenu.typeName = 'time_range';
	}
	else if (cursorInArea(mouseX, mouseY, global.draggableArea)) {
		if (global.selectedRects.length === 0) {
			addSelections(mouseX, mouseY, mouseX + 1, mouseY + 1);
		}
		
		if (global.selectedRects.length === 1) {
			// showContextMenu(e, '#cmenu_main_single');
			cmenu.typeName = 'main_single';
		}
		else if (global.selectedRects.length > 0) {
			// showContextMenu(e, '#cmenu_main');
			cmenu.typeName = 'main_multi';
		}
		else {
			cmenu.typeName = 'main_nothing';
		}
	}
	else if (cursorInArea(mouseX, mouseY, global.fileArea)) {
		var visiblePixels = global.getVisibleFiles().length * ROW_HEIGHT + global.translateY;
		if (mouseY < visiblePixels * global.scaleY) {
			// showContextMenu(e, '#cmenu_file_in');
			cmenu.typeName = 'file_in';
		}
		else {
			// showContextMenu(e, '#cmenu_file_out');
			cmenu.typeName = 'file_out';
		}
	}
	else if (cursorInEvent(mouseX, mouseY, global.eventArea) || cursorInMarker(mouseX, mouseY)) {
		cmenu.typeName = 'marker';
		// "global.selectedTimestamp" will be evaluated from the plug-in side.
		// In this case, use the display timeline, rather than the actual timestamp
		// when this event was occurred.
		global.selectedTimestamp = global.markerTimestamp;
	}
	else {
		cmenu.typeName = 'unknown';
	}
}

function addSelectionsByIds(sids, ids, clearPreviousSelection) {
	// Keep the previous selection in order to notify selection changed.
	global.prevSelectedRects = global.selectedRects.slice(0);

	if (clearPreviousSelection) {
		global.selectedRects = [];
	}

	for ( var i = 0; i < ids.length; ++i) {
		var sid = sids[i];
		var id = ids[i];
		global.selectedRects.push(new OperationId(sid, id));
	}

	updateHighlight();
	checkAndNotifySelectionChanged();
}

function removeSelectionsByIds(sids, ids) {
	global.prevSelectedRects = global.selectedRects.slice(0);

	for (var i = 0; i < ids.length; ++i) {
		var sid = sids[i];
		var id = ids[i];
		
		var index = indexOfSelected(sid, id);
		if (index !== -1) {
			global.selectedRects.splice(index, 1);
		}
	}
	
	updateHighlight();
	checkAndNotifySelectionChanged();
}

function removeAllSelections() {
	global.prevSelectedRects = global.selectedRects.slice(0);

	global.selectedRects = [];
	svg.subRects.selectAll('rect.highlight_rect').remove();

	checkAndNotifySelectionChanged();
}

function addSelections(x1, y1, x2, y2, toggle, clearPrevSelections) {
	global.prevSelectedRects = global.selectedRects.slice(0);

	var rect = svg.main.node().createSVGRect();
	rect.x = x1;
	rect.y = y1;
	rect.width = Math.max(x2 - x1, 1);
	rect.height = Math.max(y2 - y1, 1);

	// Get all the intersecting objects in the SVG.
	var list = svg.main.node().getIntersectionList(rect, null);

	// Filter only the operation rects.
	var opRects = d3.selectAll(list).filter('.op_rect');
	if (clearPrevSelections === true && opRects.empty() === false) {
		global.prevSelectedRects = global.selectedRects.slice(0);
		global.selectedRects = [];
	}

	opRects.each(function(d, i) {
		var sid = d.sid;
		var id = d.id;
		
		var index = indexOfSelected(sid, id);

		if (toggle === true) {
			if (index !== -1) {
				global.selectedRects.splice(index, 1);
			}
			else {
				global.selectedRects.push(new OperationId(sid, id));
			}
		}
		else if (!isSelected(sid, id)) {
			global.selectedRects.push(new OperationId(sid, id));
		}
	});

	updateHighlight();
	checkAndNotifySelectionChanged();
}

function checkAndNotifySelectionChanged() {
	var notify = true;

	if (global.prevSelectedRects instanceof Array) {
		if (global.prevSelectedRects.length === global.selectedRects.length) {
			notify = false;
			
			for (var i = 0; i < global.prevSelectedRects.length; ++i) {
				if (global.prevSelectedRects[i].id !== global.selectedRects[i].id) {
					notify = true;
					break;
				}

				if (global.prevSelectedRects[i].sid !== global.selectedRects[i].sid) {
					notify = true;
					break;
				}
			}
		}
	}

	if (notify === true) {
		azurite.notifySelectionChanged();
	}
}

function isSelected(sid, id) {
	return (indexOfSelected(sid, id) !== -1);
}

function indexOfSelected(sid, id) {
	var i;
	for (i = 0; i < global.selectedRects.length; ++i) {
		if (global.selectedRects[i].sid === sid && global.selectedRects[i].id === id) {
			return i;
		}
	}

	return -1;
}

function updateHighlight() {
	svg.subRects.selectAll('rect.highlight_rect').remove();

	var i, idString;

	for (i = 0; i < global.selectedRects.length; ++i) {
		idString = '#' + global.selectedRects[i].sid + '_' + global.selectedRects[i].id;

		var $ref = $(idString);
		if ($ref.length === 0) {
			continue;
		}
		var refBBox = $ref.get(0).getBBox();

		d3.select($(idString)[0].parentNode)
			.insert('rect', ':first-child')
			.attr('class', 'highlight_rect')
			.attr('fill', 'yellow')
			.attr('x', refBBox.x - (HIGHLIGHT_WIDTH / global.scaleX))
			.attr('y', refBBox.y - (HIGHLIGHT_WIDTH / global.scaleY))
			.attr('rx', RECT_RADIUS)
			.attr('ry', RECT_RADIUS)
			.attr('width', refBBox.width + HIGHLIGHT_WIDTH * 2 / global.scaleX)
			.attr('height', refBBox.height + HIGHLIGHT_WIDTH * 2 / global.scaleY);
	}

	svg.subRects.selectAll('rect.highlight_rect').moveToFront();
	for (i = 0; i < global.selectedRects.length; ++i) {
		idString = '#' + global.selectedRects[i].sid + '_' + global.selectedRects[i].id;
		d3.select($(idString)[0]).moveToFront();
	}
}

/******************************************************************
LISTENER FUNCTIONS
******************************************************************/
function barZoomIn() {
	scaleX(global.scaleX + 0.1);
}

function barZoomOut() {
	scaleX(global.scaleX - 0.1);
}

function showBefore() {
	scrollRight(100);
}

function showAfter() {
	scrollRight(-100);
}

function showPageBefore() {
	var extent = getSvgWidth() * (1.0 - FILES_PORTION);
	scrollRight(extent * 0.9);
}

function showPageAfter() {
	var extent = getSvgWidth() * (1.0 - FILES_PORTION);
	scrollRight(-extent * 0.9);
}

function scrollRight(pixels) {
	translateX(global.translateX + pixels);
}

function scrollDown(pixels) {
	translateY(global.translateY + pixels);
}

function scrollToEnd() {
	translateX(getMinTranslateX());
}

function fileZoomIn() {
	scaleY(global.scaleY + 0.3);
}

function fileZoomOut() {
	scaleY(global.scaleY - 0.3);
}

function showUp() {
	translateY(global.translateY + 1 * ROW_HEIGHT);
}

function showDown() {
	translateY(global.translateY - 1 * ROW_HEIGHT);
}

function showPageUp() {
	var extent = Math.floor(getSvgHeight() / (ROW_HEIGHT * global.scaleY));
	translateY(global.translateY + (extent - 1) * ROW_HEIGHT);
}

function showPageDown() {
	var extent = Math.floor(getSvgHeight() / (ROW_HEIGHT * global.scaleY));
	translateY(global.translateY - (extent - 1) * ROW_HEIGHT);
}

function undo() {
	// close context menu if there is any
	// hideContextMenu();
	var result = getStandardRectSelection();

	if (result.length > 0) {
		azurite.selectiveUndo(result);
	}
}

function getStandardRectSelection() {
	var result = [];

	for ( var i = 0; i < global.selectedRects.length; ++i) {
		result.push([ global.selectedRects[i].sid, global.selectedRects[i].id ]);
	}
	
	return result;
}

function clamp(value, min, max) {
	if (value < min) {
		value = min;
	}
	if (value > max) {
		value = max;
	}

	return value;
}

function range(begin, end) {
	var result = [];
	for ( var i = begin; i < end; ++i) {
		result[i - begin] = i;
	}

	return result;
}

function scaleX(sx) {
	sx = clamp(sx, 0.1, 50);
	global.translateX = global.translateX / global.scaleX * sx;
	global.scaleX = sx;

	updateSubRectsTransform();

	svg.subRects.selectAll('rect.op_rect').attr('width', rectDraw.wFunc);

	updateHighlight();

	d3.selectAll('.indicator').attr('stroke-width', indicatorDraw.wFunc);

	updateHScroll();
	
	if (global.layout === LayoutEnum.COMPACT) {
		layout();
	}
	
	updateMarkerPosition();
}

function scaleY(sy) {
	sy = clamp(sy, 0.1, 10);
	global.scaleY = sy;

	updateSubRectsTransform();

	svg.subRects.selectAll('rect.op_rect').attr('height', rectDraw.hFunc);

	svg.subFiles.selectAll('text').attr('y', fileDraw.yFunc);
	svg.subFiles.selectAll('rect.file_rect')
		.attr('y', fileRectDraw.yFunc)
		.attr('height', fileRectDraw.hFunc);

	updateSeparatingLines();

	updateVScroll();

	updateHighlight();
}

function translateX(tx) {
	// profiling
	var _startTick = new Date().valueOf();
	if (global.profile) {
		console.log("translateX() start: " + _startTick);
	}
	
	var diff = tx - global.translateX;
	
	tx = clamp(tx, getMinTranslateX(), 0);
	global.translateX = tx;

	updateSubRectsTransform();

	updateHScroll();
	
	global.diffWhileDraggingMarker -= diff;

	if (global.dates !== null && global.dates.length > 0) {
		// Restore the last one.
		if (global.lastMovedDate !== undefined && global.lastMovedDate !== null) {
			d3.selectAll('#date_' + global.lastMovedDate.getTime()).attr('x', timeTickDraw.textXFunc(global.lastMovedDate));
		}

		var leftDate = global.dates[0];
		var nextDate = null;
		var leftTimestamp = screenPixelToTimestamp(0);
		for (var i = 0; i < global.dates.length; ++i) {
			if (global.dates[i].getTime() >= leftTimestamp) {
				nextDate = global.dates[i];
				break;
			}

			leftDate = global.dates[i];
		}

		if (nextDate !== null && timestampToPixel(leftTimestamp) + $('#date_' + leftDate.getTime())[0].getBBox().width > timestampToPixel(nextDate.getTime())) {
			d3.selectAll('#date_' + leftDate.getTime()).attr('x', timeTickDraw.textXFunc(leftDate));
		} else {
			d3.selectAll('#date_' + leftDate.getTime()).attr('x', timeTickDraw.textXFunc(new Date(leftTimestamp)));
		}

		global.lastMovedDate = leftDate;
	}
	
	// profiling
	var _endTick = new Date().valueOf();
	if (global.profile) {
		console.log("translateX() end: " + _endTick);
		console.log("translateX() took: " + (_endTick - _startTick));
	}
}

function getMinTranslateX() {
	var result = 0, i;
	for (i = 0; i < global.sessions.length; ++i) {
		var session = global.sessions[i];
		result += session.g.node().getBBox().width;
	}

	var scaled = result * global.scaleX;
	var svgWidth = getSvgWidth();
	var width = svgWidth > 0 ? getSvgWidth() * (1.0 - FILES_PORTION) : 0;
	scaled = Math.max(scaled - width, 0);
	
	return -scaled;
}

function translateY(ty) {
	// profiling
	var _startTick = new Date().valueOf();
	if (global.profile) {
		console.log("translateY() start: " + _startTick);
	}
	
	ty = clamp(ty, getMinTranslateY(), 0);
	global.translateY = ty;

	updateSubRectsTransform();

	svg.subFiles.selectAll('text').attr('y', fileDraw.yFunc);
	svg.subFiles.selectAll('rect.file_rect').attr('y', fileRectDraw.yFunc);

	updateSeparatingLines();
	updateVScroll();
	
	// profiling
	var _endTick = new Date().valueOf();
	if (global.profile) {
		console.log("translateY() end: " + _endTick);
		console.log("translateY() took: " + (_endTick - _startTick));
	}
}

// Use the pixels.
function getMinTranslateY() {
	return Math.min((getSvgHeight() - TICKS_HEIGHT - EVENTS_HEIGHT - (global.getVisibleFiles().length * ROW_HEIGHT * global.scaleY)) / global.scaleY, 0);
}

function updateSubRectsTransform() {
	svg.subRects.attr('transform', 'translate(' + global.translateX + ' ' + (global.translateY * global.scaleY) + ') ' + 'scale(' + global.scaleX + ' ' + global.scaleY + ')');
	svg.subMarkers.attr('transform', 'translate(' + global.translateX + ' 0)');
	svg.subEvents.attr('transform', 'translate(' + global.translateX + ' 0)');
	svg.subTicks.attr('transform', 'translate(' + global.translateX + ' 0)');
}

function showFrom(absTimestamp) {
	showTimestamp(absTimestamp, 0);
}

function showUntil(absTimestamp) {
	showTimestamp(absTimestamp, getSvgWidth() * (1.0 - FILES_PORTION));
}

function showTimestamp(absTimestamp, offsetInPixels) {
	translateX(-timestampToPixel(absTimestamp) + offsetInPixels);
}

function timestampToPixel(absTimestamp) {
	return global.timeScale(new Date(absTimestamp)) * global.scaleX;
}

function updateHScroll() {
	var trackSize = $('#hscroll_thumbtrack').width();

	var extent = getSvgWidth() * (1.0 - FILES_PORTION);
	var thumbSize = Math.max(Math.floor(extent * trackSize / (extent - getMinTranslateX())), MIN_SCROLL_THUMB_SIZE);

	var thumbRelativePos = global.translateX / getMinTranslateX();
	if (global.translateX === 0 || getMinTranslateX() === 0) {
		thumbRelativePos = 0;
	}

	d3.select('#hscroll_thumb')
		.style('width', thumbSize + 'px')
		.style('left', Math.floor((trackSize - thumbSize) * thumbRelativePos) + 'px');
}

function updateVScroll() {
	var trackSize = $('#vscroll_thumbtrack').height();

	var areaHeight = getSvgHeight() - TICKS_HEIGHT - EVENTS_HEIGHT;
	var thumbSize = clamp(trackSize * areaHeight / (global.getVisibleFiles().length * ROW_HEIGHT * global.scaleY), MIN_SCROLL_THUMB_SIZE, trackSize);

	var thumbRelativePos = global.translateY / getMinTranslateY();
	if (global.translateY === 0 || getMinTranslateY() === 0) {
		thumbRelativePos = 0;
	}

	d3.select('#vscroll_thumb').style('height', thumbSize + 'px').style('top',
			Math.floor((trackSize - thumbSize) * thumbRelativePos) + 'px');
}

function updateTicks() {
	svg.subTicks.selectAll('text').remove();
	svg.subTicks.selectAll('line').remove();
	if (global.sessions === undefined || global.sessions.length === 0) {
		return;
	}

	var ticks = [];
	var dates = [];

	var i, j, text;

	var minInterval = TICKS_MIN_INTERVAL;
	var maxInterval = Math.max(getTicksMaxInterval(), TICKS_MIN_INTERVAL * 2);

	// Iterate all the sessions.
	for (i = 0; i < global.sessions.length; ++i) {
		var session = global.sessions[i];

		var start = session.startAbsTimestamp;
		var end = session.endAbsTimestamp;

		var nextSession = i === global.sessions.length - 1 ? null : global.sessions[i + 1];
		var nextSessionStart = nextSession === null ? null : nextSession.startAbsTimestamp;
		var nextSessionStartPixel = nextSession === null ? end : timestampToPixel(nextSessionStart);

		// Always add the start of the session
		ticks.push(new Date(start));
		dates.push(new Date(start));
		var lastDate = dates[dates.length - 1];

		var startPixel = timestampToPixel(start);
		var endPixel = timestampToPixel(end);
		var pixelLength = endPixel - startPixel;

		var scale = d3.time.scale()
			.domain([start, end])
			.range([startPixel, endPixel]);

		// Retrieve the candidate ticks using d3's ticks() function.
		var candidates = scale.ticks(4 * pixelLength / (minInterval + maxInterval));
		var lastTickPixel = timestampToPixel(ticks[ticks.length - 1]);

		// See if each candidate tick can be displayed without violating minInterval.
		for (j = 0; j < candidates.length; ++j) {
			var candidate = candidates[j].getTime();
			var candidatePixel = timestampToPixel(candidate);

			if (candidatePixel - lastTickPixel >= minInterval) {
				if (nextSession === null || nextSessionStartPixel - candidatePixel >= minInterval) {
					ticks.push(candidates[j]);
					lastTickPixel = candidatePixel;

					if (d3.time.day(lastDate).getTime() !== d3.time.day(candidates[j]).getTime()) {
						dates.push(candidates[j]);
						lastDate = candidates[j];
					}
				}
			}
		}
	}

	// Fill in too big gaps
	for (i = 0; i < ticks.length - 1; ++i) {
		var curPixel = timeTickDraw.xFunc(ticks[i]);
		var nextPixel = timeTickDraw.xFunc(ticks[i + 1]);
		var diff = nextPixel - curPixel;

		if (diff > maxInterval) {
			var count = Math.max(Math.floor(diff / ((minInterval + maxInterval) / 2)) - 1, 1);
			var interval = diff / (count + 1);

			// Insert ticks forcefully.
			for (j = 1; j <= count; ++j) {
				var pixelPosition = curPixel + interval * j;
				var correspondingTimestamp = pixelToTimestamp(pixelPosition);

				ticks.splice(i + j, 0, new Date(correspondingTimestamp));
			}

			i += count;
		}
	}

	// Display seconds?
	var displaySeconds = false;
	for (i = 0; i < ticks.length - 1; ++i) {
		if (d3.time.minute(ticks[i]).getTime() === d3.time.minute(ticks[i + 1]).getTime()) {
			displaySeconds = true;
			break;
		}
	}

	var timeFormat = displaySeconds ? '%I:%M:%S %p' : '%I:%M %p';
	var timeFormatter = d3.time.format(timeFormat);

	// Now actually create appropriate svg objects to draw the ticks
	for (i = 0; i < ticks.length; ++i) {
		text = svg.subTicks.append('text');
		text.datum(ticks[i]);

		text.attr('x', timeTickDraw.textXFunc)
			.attr('dy', '1em')
			.attr('fill', TICK_TEXT_COLOR)
			.attr('text-anchor', 'left')
			.text(timeFormatter(ticks[i]));

		var tickMark = svg.subTicks.append('line');
		tickMark.datum(ticks[i]);

		tickMark
			.attr('x1', timeTickDraw.xFunc)
			.attr('x2', timeTickDraw.xFunc)
			.attr('y1', 0)
			.attr('y2', TICKMARK_SIZE)
			.attr('stroke-width', TICKMARK_WIDTH)
			.attr('stroke', TICKMARK_COLOR);
	}

	for (i = 0; i < dates.length; ++i) {
		text = svg.subTicks.append('text');
		text.datum(dates[i]);

		text.attr('x', timeTickDraw.textXFunc)
			.attr('id', 'date_' + dates[i].getTime())
			.attr('dy', '2em')
			.attr('fill', 'white')
			.attr('text-anchor', 'left')
			.text(dateFormatter(dates[i]));
	}

	global.ticks = ticks;
	global.dates = dates;
}

function getTicksMaxInterval() {
	return getSvgWidth() * (1.0 - FILES_PORTION) / 2;
}

function dateFormatter(dateObj) {
	var today = d3.time.day(new Date());
	var target = d3.time.day(dateObj);
	var diffDays = (today.getTime() - target.getTime()) / (24 * 60 * 60 * 1000);

	var days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

	if (diffDays === 0) {
		return 'Today';
	} else if (diffDays === 1) {
		return 'Yesterday';
	} else if (0 < diffDays && diffDays < 7) {
		return 'Last ' + days[target.getDay()];
	} else {
		return d3.time.format('%x')(target);
	}
}

function test(count, dayoff) {
	if (count === undefined) { count = 1; }
	
	for (var i =0; i < count; ++i) {
		var date = new Date();
		if (dayoff !== undefined) {
			date.setDate(date.getDate() - dayoff);
		}

		var sid = date.getTime();
		// This must be bigger than the last known operation timestamp.
		if (global.lastOperation !== null && sid < global.lastOperation.getAbsT2()) {
			sid = global.lastOperation.getAbsT2() + Math.floor(Math.random() * 5000);
		}
		
		addFile('DummyProject', 'Test.java');
		addRandomOperations(sid, 100, true);

		addFile('DummyProject', 'Test2.java');
		addRandomOperations(sid, 200, false);
		
		addFile('OtherProject', 'OtherProject.java');
		addRandomOperations(sid, 40, false);

		addFile('DummyProject', 'Test3.java');
		addRandomOperations(sid, 60, false);

		addFile('DummyProject', 'Test.java');
		addRandomOperations(sid, 100, false);
	}
	
	layout();
	
	scrollToEnd();
}

function addRandomOperations(sid, count, startover) {
	var i = 0;
	var id = -1;
	var t = 0;

	if (global.lastOperation !== null && !startover) {
		id = global.lastOperation.id;
		t = global.lastOperation.t2;
	}

	for (i = 0; i < count; ++i) {
		++id;
		var t1 = t + Math.floor(Math.random() * 15000) + 1000;
		var t2 = t1 + Math.floor(Math.random() * 1000) + 1000;
		t = t2;

		var y1 = Math.floor(Math.random() * 100);
		var y2 = clamp(y1 + Math.floor(Math.random() * 30), y1, 100);
		addOperation(sid, id, t1, t2, y1, y2, Math.floor(Math.random() * 3));
	}
}

function getSvgWidth() {
	return parseInt(svg.main.style('width'), 10) - CHART_MARGINS.left - CHART_MARGINS.right;
}

function getSvgHeight() {
	return parseInt(svg.main.style('height'), 10) - CHART_MARGINS.top - CHART_MARGINS.bottom;
}

function showAllFiles() {
	for (var i = 0; i < global.files.length; ++i) {
		global.files[i].visible = true;
	}
	
	layoutFiles();
	layout();
}

function showSelectedFile() {
	var index = Math.floor(cmenu.mousePos[1] / (ROW_HEIGHT * global.scaleY)) - global.translateY;
	var file = global.getVisibleFiles()[index];
	
	var i;
	for (i = 0; i < global.files.length; ++i) {
		global.files[i].visible = global.files[i] === file;
	}
	
	layoutFiles();
	layout();
}

function showAllFilesInProject() {
	var index = Math.floor(cmenu.mousePos[1] / (ROW_HEIGHT * global.scaleY)) - global.translateY;
	var file = global.getVisibleFiles()[index];
	
	var i;
	for (i = 0; i < global.files.length; ++i) {
		global.files[i].visible = global.files[i].project === file.project;
	}
	
	layoutFiles();
	layout();
}

function jumpToLocation() {
	if (global.selectedRects.length === 0) {
		return;
	}
	
	var selection = global.selectedRects[0];
	var rect = $('rect#' + selection.sid + '_' + selection.id).get(0);
	
	if (rect !== null) {
		var datum = rect.__data__;
		if (datum !== undefined && datum !== null) {
			var file = datum.fileGroup.file;
			azurite.jump(file.project, file.path, datum.sid, datum.id);
		}
	}
}

function showAllFilesEditedTogether() {
	if (global.selectedRects.length <= 1) {
		return;
	}
	
	var sortedSelection = global.selectedRects.slice(0);
	sortedSelection.sort(function (lhs, rhs) {
		if (lhs.sid < rhs.sid) {
			return -1;
		}
		if (lhs.sid > rhs.sid) {
			return 1;
		}
		if (lhs.id < rhs.id) {
			return -1;
		}
		if (lhs.id > rhs.id) {
			return 1;
		}
		return 0;
	});
	
	var start = global.selectedRects[0];
	var end = global.selectedRects[global.selectedRects.length - 1];
	
	var startRect = $('rect#' + start.sid + '_' + start.id).get(0);
	var endRect = $('rect#' + end.sid + '_' + end.id).get(0);
	
	var sortedRects = svg.subRects.selectAll('rect.op_rect')[0].slice(0);
	sortedRects.sort(global.operationCompareFunc);
	
	var startIndex = sortedRects.indexOf(startRect);
	var endIndex = sortedRects.indexOf(endRect);
	
	if (startIndex === -1 || endIndex === -1) {
		return;
	}
	
	// Make all files invisible for the moment.
	var i;
	for (i = 0; i < global.files.length; ++i) {
		global.files[i].visible = false;
	}
	
	for (i = startIndex; i <= endIndex; ++i) {
		sortedRects[i].__data__.fileGroup.file.visible = true;
	}
	
	layoutFiles();
	layout();
}

function showAllFilesEditedInRange() {
	if (global.selectedTimestampRange === null) {
		return;
	}

	var start = global.selectedTimestampRange[0];
	var end = global.selectedTimestampRange[1];

	var rects = svg.subRects.selectAll('rect.op_rect')[0].slice(0);
	var filteredRects = rects.filter(function (element) {
		var absTimestamp = element.__data__.sid + element.__data__.t1;
		return start <= absTimestamp && absTimestamp < end;
	});
	
	// Make all files invisible for the moment.
	var i;
	for (i = 0; i < global.files.length; ++i) {
		global.files[i].visible = false;
	}

	for (i = 0; i < filteredRects.length; ++i) {
		filteredRects[i].__data__.fileGroup.file.visible = true;
	}

	layoutFiles();
	layout();
}

function openAllFilesEditedInRange() {
	azurite.openAllFilesEditedInRange(getAllFilesEditedInRange());
}

function getAllFilesEditedInRange() {
	if (global.selectedTimestampRange === null) {
		return [];
	}

	var start = global.selectedTimestampRange[0];
	var end = global.selectedTimestampRange[1];

	var rects = svg.subRects.selectAll('rect.op_rect')[0].slice(0);
	var filteredRects = rects.filter(function (element) {
		var absTimestamp = element.__data__.sid + element.__data__.t1;
		return start <= absTimestamp && absTimestamp < end;
	});

	var filePaths = [];
	for (var i = 0; i < filteredRects.length; ++i) {
		filePaths.push(filteredRects[i].__data__.fileGroup.file.path);
	}

	return filePaths;
}

function updateEvents() {
	svg.subEvents.selectAll('.event_line')
		.attr('x1', eventDraw.xFunc)
		.attr('x2', eventDraw.xFunc);
	svg.subEvents.selectAll('.event_icon')
		.attr('x', eventDraw.iconXFunc);
}

function showMarkerAtPixel(pixel, notify, noupdate) {
	if (isNaN(pixel) || (noupdate === true && global.markerTimestamp === 0) || global.lastOperation === null) {
		// Don't show the marker at all.
		svg.subMarker.style('display', 'none');
		return;
	}

	global.markerPos = pixel;
	if (noupdate !== true) {
		global.markerTimestamp = pixelToTimestamp(pixel);
	}

	var timeFormat = '%I:%M:%S %p';
	var timeFormatter = d3.time.format(timeFormat);
	
	svg.subMarker.attr('transform', 'translate(' + pixel + ' 0)');
	svg.subMarkerText.text(timeFormatter(new Date(global.markerTimestamp)));

	svg.subMarker.style('display', '');

	if (notify === true || notify === undefined) {
		// Tell Azurite about this marker move!
		azurite.markerMove(global.markerTimestamp);
	}
}

function showMarkerAtTimestamp(absTimestamp, notify) {
	if (absTimestamp !== undefined) {
		global.markerTimestamp = absTimestamp;
	}
	
	updateMarkerPosition(notify);
	svg.subMarker.style('display', '');
}

function updateMarkerPosition(notify, noupdate) {
	var t = global.markerTimestamp;
	var tx = timestampToPixel(t);

	if (notify === undefined) {
		notify = false;
	}

	showMarkerAtPixel(tx, notify, noupdate);
}

function hideMarker() {
	svg.subMarker.style('display', 'none');
}

function selectPixelRange(startPixel, endPixel) {
	if (global.lastOperation !== null) {
		global.selectedPixelRange = [startPixel, endPixel];
		global.selectedTimestampRange = [pixelToTimestamp(startPixel), pixelToTimestamp(endPixel)];

		updateRangeSelectionBox();
	}
}

function updateEndPixelRange(endPixel) {
	if (global.selectedPixelRange !== null) {
		selectPixelRange(global.selectedPixelRange[0], endPixel);
	}
}

function deselectRange() {
	global.selectedPixelRange = null;
	global.selectedTimestampRange = null;

	updateRangeSelectionBox();
}

function updateRangeSelectionBox() {
	if (global.selectedPixelRange !== null) {
		svg.subRangeBox
			.attr('x', (Math.min(global.selectedPixelRange[0], global.selectedPixelRange[1])))
			.attr('width', (Math.abs(global.selectedPixelRange[1] - global.selectedPixelRange[0])))
			.style('display', '');
		svg.subRangeStartLine
			.attr('x1', global.selectedPixelRange[0])
			.attr('x2', global.selectedPixelRange[0])
			.style('display', '');
	}
	else {
		svg.subRangeBox
			.style('display', 'none');
		svg.subRangeStartLine
			.style('display', 'none');
	}
}

function hideFirebugUI() {
	$('#FirebugUI').css('display', 'none');
}

function activateFirebugLite() {
	// Firebug lite 1.3 bookmarklet turned into a function.
	// For some unknown reason, 1.4 doesn't work on Safari.
	(function(F,i,r,e,b,u,g,L,I,T,E) {
		if(F.getElementById(b))
			return;
		E=F[i+'NS']&&F.documentElement.namespaceURI;
		E=E?F[i+'NS'](E,'script'):F[i]('script');
		E[r]('id',b);
		E[r]('src',I+g+T);
		E[r](b,u);
		(F[e]('head')[0]||F[e]('body')[0]).appendChild(E);
		E=new Image();
		E[r]('src',I+L);
	})(document,'createElement','setAttribute','getElementsByTagName','FirebugLite','3','releases/lite/1.3/firebug-lite.js','releases/lite/latest/skin/xp/sprite.png','https://getfirebug.com/','#startOpened');
}

function pushCurrentFile() {
	if (global.currentFile !== null) {
		// push the current file to the file stack.
		global.fileStack.push(global.currentFile);
	}
	else {
		// if the current file is null.. then maybe just add null?
		global.fileStack.push(null);
	}
}

function popCurrentFile() {
	if (global.fileStack.length === 0) {
		azurite.log("pushCurrentFile / popCurrentFile pair mismatch!");
		// do nothing.
	}
	else {
		var popped = global.fileStack.pop();
		if (popped !== null) {
			global.currentFile = popped;
			for (var index = 0; index < global.files.length; ++index) {
				if (global.currentFile[index].path === popped.path) {
					global.currentFileIndex = index;
					break;
				}
			}
		}
		else {
			global.currentFile = null;
			global.currentFileIndex = -1;
		}
	}
}

function testMarker() {
	test();
	showMarkerAtTimestamp();
	translateX(0);
}

function showEvents(show) {
	var displayValue = show === true ? '' : 'none';
	svg.subEventsWrap.style('display', displayValue);
}
