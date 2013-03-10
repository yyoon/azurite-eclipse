/**
 * Things should be executed at the beginning.
 */

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

var NUM_TIMESTAMPS = 3;

var MIN_WIDTH = 3;
var ROW_HEIGHT = 30;
var TICKS_HEIGHT = 30;
var DEFAULT_RATIO = 100;

var FILE_NAME_OFFSET_X = 0;
var FILE_NAME_OFFSET_Y = 5;

var FILES_PORTION = 0.15;

var HIGHLIGHT_WIDTH = 3;

var INDICATOR_WIDTH = 2;

var SCROLLBAR_WIDTH = 10;
var SCROLLBAR_DIST_THRESHOLD = 50;

var CHART_MARGINS = {
	left : 10,
	top : 10,
	right : 10,
	bottom : 10
};

var MIN_SCROLL_THUMB_SIZE = 30;

// draw functions

var rectDraw = {};
rectDraw.xFunc = function(d) {
	return (d.t1 - d.session.startAbsTimestamp + d.sid) / DEFAULT_RATIO;
};
rectDraw.yFunc = function(d) {
	return Math.min(ROW_HEIGHT * d.y1 / 100, ROW_HEIGHT - MIN_WIDTH
			/ global.scaleY);
};
rectDraw.wFunc = function(d) {
	return Math.max(MIN_WIDTH / global.scaleX, (d.t2 - d.t1) / DEFAULT_RATIO);
};
rectDraw.hFunc = function(d) {
	return Math
			.max(MIN_WIDTH / global.scaleY, ROW_HEIGHT * (d.y2 - d.y1) / 100);
};

var fileDraw = {};
fileDraw.yFunc = function(d, i) {
	return ROW_HEIGHT * (i + global.translateY) * global.scaleY
			+ FILE_NAME_OFFSET_Y;
};

var lineDraw = {};
lineDraw.x2Func = function(d) {
	return getSvgWidth() * (1.0 - FILES_PORTION);
};
lineDraw.yFunc = function(d) {
	return ROW_HEIGHT * (d + global.translateY) * global.scaleY;
};

var indicatorDraw = {};
indicatorDraw.y2Func = function() {
	return global.files.length * ROW_HEIGHT + getSvgHeight();
};
indicatorDraw.wFunc = function() {
	return INDICATOR_WIDTH / global.scaleX;
};

/**
 * Global variables. (Always use a pseudo-namespace.)
 */
var global = {};

global.operationCompareFunc = function (lhs, rhs) {
	if (lhs.__data__.t1 < rhs.__data__.t1) { return -1; }
	if (lhs.__data__.t1 > rhs.__data__.t1) { return 1; }
	if (lhs.__data__.id < rhs.__data__.id) { return -1; }
	if (lhs.__data__.id > rhs.__data__.id) { return 1; }
	return 0;
};

// variables to remember the last window size
global.lastWindowWidth = null;
global.lastWindowHeight = null;

// arrays to keep
global.files = [];
global.sessions = [];
global.selected = [];

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

global.hscrollArea = {
	left : 0,
	top : 0,
	right : 0,
	bottom : 0
};

global.vscrollArea = {
	left : 0,
	top : 0,
	right : 0,
	bottom : 0
};

global.draggingHScroll = false;
global.draggingVScroll = false;
global.dragStartScrollPos = null;

// context menu
var cmenu = {};
cmenu.isContextMenuVisible = false;
cmenu.isRightButtonDown = false;
global.isCtrlDown = false;

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

	svg.subTicks = svg.main.append('g')
		.attr('id', 'sub_ticks');

	svg.main.append('rect')
		.attr('class', 'selection_box')
		.style('fill', 'yellow')
		.style('opacity', 0.3);

	svg.clipFiles = svg.main.append('clipPath')
		.attr('id', 'clipFiles')
		.append('rect');

	svg.clipRectsWrap = svg.main.append('clipPath')
		.attr('id', 'clipRectsWrap').append('rect');

	recalculateClipPaths();
}

function recalculateClipPaths() {
	var svgWidth = getSvgWidth();
	var svgHeight = getSvgHeight();

	svg.clipFiles
		.attr('width', (svgWidth * FILES_PORTION) + 'px')
		.attr('height', (svgHeight - 20) + 'px');

	svg.subFiles
		.attr('transform',
			'translate(' + CHART_MARGINS.left + ' ' + CHART_MARGINS.top + ')');

	svg.subRectsWrap.attr('transform', 'translate('
			+ (CHART_MARGINS.left + svgWidth * FILES_PORTION) + ' '
			+ CHART_MARGINS.top + ')');

	svg.clipRectsWrap
		.attr('y', '-1')
		.attr('width', (svgWidth * (1.0 - FILES_PORTION)) + 'px')
		.attr('height', (svgHeight - TICKS_HEIGHT + 2) + 'px');

	svg.subTicks.attr('transform', 'translate('
			+ (CHART_MARGINS.left + svgWidth * FILES_PORTION) + ' '
			+ (CHART_MARGINS.top + svgHeight - TICKS_HEIGHT) + ')');
}

/**
 * An object that keeps track of insert, delete and replace for each file.
 */
function File(project, path, fileName) {
	this.project = project;
	this.path = path;
	this.fileName = fileName;
	
	this.visible = true;
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
			if (this.fileGroups[i].file == file) {
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
	this.t2 = (t2 == null ? t1 : t2);
	this.y1 = y1;
	this.y2 = y2;
	this.type = type;
	this.color;

	if (type == TYPE_INSERT) {
		this.color = "green";
	} else if (type == TYPE_DELETE) {
		this.color = "red";
	} else if (type == TYPE_REPLACE) {
		this.color = "blue";
	}
	
	this.fileGroup = fileGroup;
	this.session = fileGroup.session;
	
	this.isVisible = function () {
		return this.fileGroup.isVisible();
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
 * Called by Azurite.
 * Adds a new row if the given file is not already in the list.
 */
function addFile(project, path) {
	var fileName = path.match(/[^\\\/]+$/)[0];

	for ( var index in global.files) {
		if (global.files[index].path == path) {
			global.currentFile = global.files[index];
			return;
		}
	}

	var newFile = new File(project, path, fileName);

	global.files.push(newFile);
	global.currentFile = newFile;

	var newText = svg.subFiles.append('text');
	newText.datum(newFile);

	newText.attr('x', FILE_NAME_OFFSET_X + 'px')
		.attr('y', fileDraw.yFunc)
		.attr('dy', '1em')
		.attr('fill', 'white')
		.text(function(d) { return d.fileName; });
	
	layoutFiles();

	// Draw indicator
	d3.selectAll('.indicator').attr('y2', indicatorDraw.y2Func);

	// Scrollbar
	updateVScroll();
}

function updateSeparatingLines() {
	svg.subRectsWrap.selectAll('line.separating_line')
		.attr('y1', lineDraw.yFunc)
		.attr('x2', lineDraw.x2Func)
		.attr('y2', lineDraw.yFunc);
}

/**
 * Called by Azurite.
 * Sets the start timestamp.
 */
function setStartTimestamp(timestamp, adjustMaxTimestamp, preservePosition) {
	// Do nothing.
}

/**
 * Called by Azurite.
 * Add an edit operation to the end of the file.
 * Note that this is called immediately after an edit operation is performed.
 */
function addOperation(sid, id, t1, t2, y1, y2, type, scroll, layout, current) {
	if (global.currentFile == null) {
		return;
	}
	
	sid = parseInt(sid);
	id = parseInt(id);
	t1 = parseInt(t1);
	t2 = parseInt(t2);
	y1 = parseFloat(y1);
	y2 = parseFloat(y2);
	type = parseInt(type);
	
	var session = findSession(sid);
	if (session == null) {
		session = new Session(sid, current);
		session.startAbsTimestamp = sid + t1;
		session.endAbsTimestamp = sid + t2;
	}
	
	var fileGroup = session.findFileGroup(global.currentFile);
	if (fileGroup == null) {
		fileGroup = new FileGroup(session, global.currentFile);
	}

	var newOp = new EditOperation(sid, id, t1, t2, y1, y2, type, fileGroup);

	fileGroup.operations.push(newOp);
	global.lastOperation = newOp;
	
	session.startAbsTimestamp = Math.min(session.startAbsTimestamp, sid + t1);
	session.endAbsTimestamp = Math.max(session.endAbsTimestamp, sid + t2);

	var rectToAppend = fileGroup.g.append('rect');
	rectToAppend.datum(newOp);
	rectToAppend
		.attr('id', function(d) { return d.sid + '_' + d.id; })
		.attr('class', 'op_rect')
		.attr('y', rectDraw.yFunc)
		.attr('width', rectDraw.wFunc)
		.attr('height', rectDraw.hFunc)
		.attr('fill', function(d) { return d.color; })
		.attr('vector-effect', 'non-scaling-stroke');
	
	if (layout == true) {
		if (global.layout == LayoutEnum.COMPACT) {
			// Find the last visible rect in this session.
			var rectsInSession = session.g.selectAll('rect.op_rect').filter(function (d) {
				return d.isVisible() && d != newOp;
			})[0].slice();
			rectsInSession.sort(global.operationCompareFunc);
			
			var x = 0;
			if (rectsInSession.length > 0) {
				var bounds = rectsInSession[rectsInSession.length - 1].getBBox();
				x = bounds.x + bounds.width;
			}
			
			rectToAppend.attr('x', x);
		}
		else if (global.layout == LayoutEnum.REALTIME) {
			rectToAppend.attr('x', rectDraw.xFunc);
		}
	}
	
	// Add tipsy.
	$(rectToAppend.node()).tipsy({
        gravity: $.fn.tipsy.autoNS,
        html: true,
        checkFn: function() { return !global.dragging; },
        title: function() {
              var d = this.__data__;
              return d.getInfo();
        }
    });
	
	global.lastRect = rectToAppend;
	
	// Move the indicator.
	if (global.layout == LayoutEnum.COMPACT) {
		var rectBounds = rectToAppend.node().getBBox();
		var indicatorX = rectBounds.x + rectBounds.width;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);
	}
	else {
		var indicatorX = (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);
	}
	
	if (layout == true) {
		updateHScroll();
	}
	
	if (scroll == true) {
		showUntil(global.lastOperation.getAbsT2());
	}
}

/**
 * Called by Azurite.
 * Update the timestamp2 value for an existing operation, in case multiple
 * operations are merged into one.
 */
function updateOperation(sid, id, t2, y1, y2, scroll) {
	var lastOp = global.lastOperation;
	
	if (lastOp == null ||
		lastOp.sid != parseInt(sid) ||
		lastOp.id != parseInt(id)) {
		return;
	}

	lastOp.t2 = t2;
	lastOp.session.endAbsTimestamp = Math.max(lastOp.session.endAbsTimestamp, lastOp.getAbsT2());
	
	lastOp.y1 = y1;
	lastOp.y2 = y2;

	if (global.lastRect != null) {
		global.lastRect
			.attr('width', rectDraw.wFunc)
			.attr('y', rectDraw.yFunc)
			.attr('height', rectDraw.hFunc);
	}
	
	var session = lastOp.session;
	
	// Move the indicator.
	if (global.layout == LayoutEnum.COMPACT) {
		var rectBounds = global.lastRect.node().getBBox();
		var indicatorX = rectBounds.x + rectBounds.width;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);
	}
	else {
		var session = lastOp.session;
		var indicatorX = (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO;
		session.indicator.attr('x1', indicatorX).attr('x2', indicatorX);
	}
	
	if (scroll == true) {
		showUntil(lastOp.getAbsT2());
	}
}

function findSession(sid) {
	var i;
	for (i = 0; i < global.sessions.length; ++i) {
		if (global.sessions[i].sid == sid) {
			return global.sessions[i];
		}
	}
	
	return null;
}

function layout(newLayout) {
	// Remember the current horizontal scroll position.
	var leftmostTimestamp = getLeftmostTimestamp();
	
	// Change the layout, if specified.
	if (newLayout !== undefined) {
		global.layout = newLayout;
	}
	
	global.tempSessionTx = 0;
	
	var i, session;
	for (i = 0; i < global.sessions.length; ++i) {
		session = global.sessions[i];
		
		session.g.attr('transform', 'translate(' + global.tempSessionTx + ' 0)');
		
		// Iterate through all the rects.
		var rects = session.g.selectAll('rect.op_rect')[0].slice();
		rects.sort(global.operationCompareFunc);
		
		// Apply different layout function, depending on the mode.
		global.tempX = 0;
		
		if (global.layout == LayoutEnum.COMPACT) {
			d3.selectAll(rects).attr('x', function (d) {
				if (!d.isVisible()) { return 0; }
				var temp = global.tempX;
				global.tempX += this.getBBox().width;
				return temp;
			});
		}
		else if (global.layout == LayoutEnum.REALTIME) {
			d3.selectAll(rects).attr('x', rectDraw.xFunc);
		}
		
		// TODO if there is no rectangle at all, don't draw the entire session.
		
		// Move the indicator.
		if (global.layout == LayoutEnum.COMPACT) {
			session.indicator.attr('x1', global.tempX);
			session.indicator.attr('x2', global.tempX);
		}
		else if (global.layout == LayoutEnum.REALTIME) {
			session.indicator.attr('x1', (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO);
			session.indicator.attr('x2', (session.endAbsTimestamp - session.startAbsTimestamp) / DEFAULT_RATIO);
		}
		
		global.tempSessionTx += session.g.node().getBBox().width;
	}
	
	updateHighlight();
	updateHScroll();
	
	// Restore the scroll position.
	showFrom(leftmostTimestamp);
}

function layoutFiles() {
	var i;
	var visibleFiles = [];
	
	for (i = 0; i < global.files.length; ++i) {
		var file = global.files[i];
		
		var fileGroups = svg.subRects.selectAll('g.file_group').filter(function (d) {
			return d.file == file;
		});
		
		if (file.visible) {
			fileGroups.style('display', '');
		}
		else {
			fileGroups.style('display', 'none');
			continue;
		}
		
		fileGroups.attr('transform', 'translate(0 ' + (visibleFiles.length * ROW_HEIGHT) + ')');
		
		visibleFiles.push(file);
	}
	
	// Labels
	var labels = svg.subFiles.selectAll('text').data(visibleFiles);
	
	labels.enter().append('text');
	
	labels.exit().remove();
	
	labels.attr('x', FILE_NAME_OFFSET_X + 'px')
		.attr('y', fileDraw.yFunc)
		.attr('dy', '1em')
		.attr('fill', 'white')
		.text(function(d) { return d.fileName; });
	
	// Separating Lines
	var lines = svg.subRectsWrap.selectAll('line.separating_line')
		.data(range(0, visibleFiles.length + 1));
	
	lines.enter().insert('line', ':first-child')
		.attr('class', 'separating_line')
		.attr('x1', '0')
		.attr('stroke', 'gray')
		.attr('stroke-width', '2');
	
	lines.exit().remove();
	
	updateSeparatingLines();
	
	updateVScroll();
}

function getLeftmostTimestamp() {
	if (global.sessions.length == 0) {
		return 0;
	}
	
	if (global.layout == LayoutEnum.COMPACT) {
		var tx = -global.translateX / global.scaleX;
		
		var session = null, dist = 0;
		
		// Get the session including the point,
		// and the dist from the starting of the session group.
		var i, curWidth = 0;
		for (i = 0; i < global.sessions.length; ++i) {
			var width = global.sessions[i].g.node().getBBox().width;
			
			if (curWidth <= tx < curWidth + width) {
				session = global.sessions[i];
				dist = tx - curWidth;
				break;
			}
			
			curWidth += width;
		}
		
		if (session == null) {
			session = global.sessions[i];
			dist = session.g.node().getBBox().width;
		}
		
		// Collect all the visible rects in this session, and assume they are sorted.
		var rects = session.g.selectAll('rect.op_rect')
			.filter(function (d) { return d.isVisible(); })[0];
		
		// Binary search through the rects.
		var startIndex = 0, endIndex = rects.length - 1;
		var midIndex = Math.floor((startIndex + endIndex) / 2);
		while (startIndex <= endIndex) {
			var rect = rects[midIndex];
			var bounds = rect.getBBox();
			
			// The middle rect contains the dist!
			if (bounds.x <= dist && dist <= bounds.x + bounds.width) {
				var data = rect.__data__;
				var result = Math.floor(session.sid + data.t1 + (data.t2 - data.t1) * (dist - bounds.x) / bounds.width);
				
				return result;
			}
			
			if (dist < bounds.x) {
				endIndex = midIndex - 1;
			}
			else {
				startIndex = midIndex + 1;
			}
			
			midIndex = Math.floor((startIndex + endIndex) / 2);
		}
		
		// Couldn't find..
		if (startIndex == 0) {
			return session.startAbsTimestamp;
		}
		else if (startIndex == rects.length) {
			return session.endAbsTimestamp;
		}
		else {
			return Math
					.floor((rects[startIndex - 1].__data__.t2 + rects[startIndex].__data__.t1) / 2)
					+ session.sid;
		}
	}
	else if (global.layout == LayoutEnum.REALTIME) {
		var tx = -global.translateX / global.scaleX;
		
		var session = null, dist = 0;
		
		// Get the session including the point,
		// and the dist from the starting of the session group.
		var i, curWidth = 0;
		for (i = 0; i < global.sessions.length; ++i) {
			var width = global.sessions[i].g.node().getBBox().width;
			
			if (curWidth <= tx < curWidth + width) {
				session = global.sessions[i];
				dist = tx - curWidth;
				break;
			}
			
			curWidth += width;
		}
		
		if (session == null) {
			session = global.sessions[i];
			dist = session.g.node().getBBox().width;
		}
		
		var bounds = session.g.node().getBBox();
		
		// Then just linearly interpolate.
		var result = session.startAbsTimestamp
				+ (session.endAbsTimestamp - session.startAbsTimestamp)
				* (dist - bounds.x) / bounds.width;
		return result;
	}
	
	return 0;
}

/*
 * When the page loads, load a log file
 */
window.onload = function() {
	azurite.initialize();

	setupSVG();
	initContextMenu();

	window.onresize();
	initEventHandlers();
};

window.onresize = function(e) {
	// if window size are different, redraw everything
	if (global.lastWindowWidth != window.innerWidth
			|| global.lastWindowHeight != window.innerHeight) {
		global.lastWindowWidth = window.innerWidth;
		global.lastWindowHeight = window.innerHeight;

		recalculateClipPaths();
		updateSeparatingLines();
		updateDraggableArea();

		updateHScroll();
		updateVScroll();
	}
};

function updateDraggableArea() {
	var svgWidth = getSvgWidth();
	var svgHeight = getSvgHeight();

	global.draggableArea.left = CHART_MARGINS.left + svgWidth * FILES_PORTION;
	global.draggableArea.top = CHART_MARGINS.top;
	global.draggableArea.right = CHART_MARGINS.left + svgWidth;
	global.draggableArea.bottom = CHART_MARGINS.top + svgHeight;

	global.hscrollArea.left = (CHART_MARGINS.left + CHART_MARGINS.right + svgWidth)
			* FILES_PORTION + SCROLLBAR_WIDTH;
	global.hscrollArea.top = CHART_MARGINS.top + CHART_MARGINS.bottom
			+ svgHeight;
	global.hscrollArea.right = CHART_MARGINS.left + CHART_MARGINS.right
			+ svgWidth - SCROLLBAR_WIDTH;
	global.hscrollArea.bottom = CHART_MARGINS.top + CHART_MARGINS.bottom
			+ svgHeight + SCROLLBAR_WIDTH;

	global.vscrollArea.left = CHART_MARGINS.left + CHART_MARGINS.right
			+ svgWidth;
	global.vscrollArea.top = SCROLLBAR_WIDTH;
	global.vscrollArea.right = CHART_MARGINS.left + CHART_MARGINS.right
			+ svgWidth + SCROLLBAR_WIDTH;
	global.vscrollArea.bottom = CHART_MARGINS.top + CHART_MARGINS.bottom
			+ svgHeight - SCROLLBAR_WIDTH;
}

/******************************************************************
 MOUSE EVENT FUNCTIONS
 ******************************************************************/

function initContextMenu() {
	global.divContext = document.getElementById('cssmenu');
}

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
			if (d3.event.wheelDelta > 0) {
				showUp();
			} else {
				showDown();
			}
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
		if (e.keyCode == 17)
			global.isCtrlDown = true;
	}, false);

	document.addEventListener("keyup", function(e) {
		if (e.keyCode == 17)
			global.isCtrlDown = false;
	}, false);
}

function initMouseDownHandler() {
	document.onmousedown = function(e) {
		if (cmenu.isContextMenuVisible) {
			hideContextMenu();
		}

		if ("which" in e) { // Gecko (Firefox), WebKit (Safari/Chrome) & Opera
			cmenu.isRightButtonDown = e.which == 3;
		} else if ("button" in e) { // IE, Opera 
			cmenu.isRightButtonDown = e.button == 2;
		}

		var mouseX = e.clientX - SVG_WRAPPER_PADDING;
		var mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;

		if (cmenu.isRightButtonDown) {
			return;
		}

		global.dragStart[0] = mouseX;
		global.dragStart[1] = mouseY;

		// Drag select
		if (cursorInArea(mouseX, mouseY, global.draggableArea)) {
			global.dragging = true;
			global.draggingHScroll = false;
			global.draggingVScroll = false;

			if (!global.isCtrlDown) {
				global.selected = [];
				svg.subRects.selectAll('rect.highlight_rect').remove();
			}

			d3.select('.selection_box').attr('x', mouseX).attr('y', mouseY);

			return;
		}
		// HScroll
		else if (cursorInArea(mouseX, mouseY, global.hscrollArea)) {
			var thumbSize = $('#hscroll_thumb').width();
			var thumbStart = parseInt($('#hscroll_thumb').css('left'));
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

				global.dragStartScrollPos = thumbStart;
				return;
			}
		}
		// VScroll
		else if (cursorInArea(mouseX, mouseY, global.vscrollArea)) {
			var thumbSize = $('#vscroll_thumb').height();
			var thumbStart = parseInt($('#vscroll_thumb').css('top'));
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

				global.dragStartScrollPos = thumbStart;
				return;
			}
		}

		global.dragging = false;
		global.draggingHScroll = false;
		global.draggingVScroll = false;
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
			var mousePos = mouseX - global.dragStart[0]
					+ global.dragStartScrollPos;

			// If the mouse position is too far from the scrollbar,
			// then return to the original position.
			if (mouseY < global.hscrollArea.top - SCROLLBAR_DIST_THRESHOLD
					|| mouseY >= global.hscrollArea.bottom
							+ SCROLLBAR_DIST_THRESHOLD) {

				mousePos = global.dragStartScrollPos;
			}

			var trackSize = $('#hscroll_thumbtrack').width();
			var thumbSize = $('#hscroll_thumb').width();

			var ratio = mousePos / (trackSize - thumbSize);
			ratio = clamp(ratio, 0.0, 1.0);

			var newTx = getMinTranslateX() * ratio;
			translateX(newTx);
		}
		// VScroll
		else if (global.draggingVScroll) {
			var mousePos = mouseY - global.dragStart[1]
					+ global.dragStartScrollPos;

			// If the mouse position is too far from the scrollbar,
			// then return to the original position.
			if (mouseX < global.vscrollArea.left - SCROLLBAR_DIST_THRESHOLD
					|| mouseX >= global.vscrollArea.right
							+ SCROLLBAR_DIST_THRESHOLD) {

				mousePos = global.dragStartScrollPos;
			}

			var trackSize = $('#vscroll_thumbtrack').height();
			var thumbSize = $('#vscroll_thumb').height();

			var ratio = mousePos / (trackSize - thumbSize);
			ratio = clamp(ratio, 0.0, 1.0);

			var newTy = getMinTranslateY() * ratio;
			newTy = Math.round(newTy);
			translateY(newTy);
		}
	};
}

function initMouseUpHandler() {
	document.onmouseup = function(e) {
		if (cmenu.isRightButtonDown) {
			showContextMenu(e);
			return;
		}

		if (global.dragging) {
			d3.select('.selection_box').attr('display', 'none');

			var x1, y1, x2, y2;

			var mouseX = e.clientX - SVG_WRAPPER_PADDING;
			var mouseY = e.clientY - MENU_PANEL_HEIGHT - SVG_WRAPPER_PADDING;

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

			addSelections(x1, y1, x2, y2);
			global.dragStart = [];
		}

		global.dragging = false;
		global.draggingHScroll = false;
		global.draggingVScroll = false;
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
			if (datum != undefined && datum != null) {
				var file = datum.fileGroup.file;
				azurite.jump(file.project, file.path, datum.sid, datum.id);
			}
		}
	};
}

function cursorInArea(x, y, area) {
	return x >= area.left && x < area.right && y >= area.top && y < area.bottom;
}

function clampInArea(x, y, area) {
	return [ clamp(x, area.left, area.right - 1),
			clamp(y, area.top, area.bottom - 1) ];
}

function showContextMenu(event) {

	var $contextMenu = $('#cssmenu');
	var w = $contextMenu.outerWidth();
	var h = $contextMenu.outerHeight();

	global.divContext.style.left = Math.min(event.clientX,
			global.lastWindowWidth - w)
			+ 'px';
	global.divContext.style.top = Math.min(event.clientY,
			global.lastWindowHeight - h)
			+ 'px';
	global.divContext.style.display = 'block';

	cmenu.isContextMenuVisible = true;
}

function hideContextMenu() {
	global.divContext.style.display = 'none';
	cmenu.isContextMenuVisible = false;
}

function addSelectionsByIds(sids, ids, clearPreviousSelection) {
	if (clearPreviousSelection) {
		global.selected = [];
	}

	for ( var i = 0; i < ids.length; ++i) {
		var sid = sids[i];
		var id = ids[i];
		global.selected.push(new OperationId(sid, id));
	}

	updateHighlight();
}

function addSelections(x1, y1, x2, y2) {
	var rect = svg.main.node().createSVGRect();
	rect.x = x1;
	rect.y = y1;
	rect.width = Math.max(x2 - x1, 1);
	rect.height = Math.max(y2 - y1, 1);

	// Get all the intersecting objects in the SVG.
	var list = svg.main.node().getIntersectionList(rect, null);

	// Filter only the operation rects.
	d3.selectAll(list).filter('.op_rect').each(function(d, i) {
		var sid = d.sid;
		var id = d.id;

		if (!isSelected(sid, id)) {
			global.selected.push(new OperationId(sid, id));
		}
	});

	updateHighlight();
}

function isSelected(sid, id) {
	var i;
	for (i = 0; i < global.selected.length; ++i) {
		if (global.selected[i].sid == sid && global.selected[i].id == id) {
			return true;
		}
	}

	return false;
}

function updateHighlight() {
	svg.subRects.selectAll('rect.highlight_rect').remove();

	for ( var i = 0; i < global.selected.length; ++i) {
		var idString = '#' + global.selected[i].sid + '_'
				+ global.selected[i].id;

		var $ref = $(idString);
		if ($ref.length == 0) {
			continue;
		}
		var refBBox = $ref.get(0).getBBox();

		d3.select($(idString)[0].parentNode).insert('rect', ':first-child')
				.attr('class', 'highlight_rect').attr('fill', 'yellow').attr(
						'x', refBBox.x - (HIGHLIGHT_WIDTH / global.scaleX))
				.attr('y', refBBox.y - (HIGHLIGHT_WIDTH / global.scaleY)).attr(
						'width',
						refBBox.width + HIGHLIGHT_WIDTH * 2 / global.scaleX)
				.attr('height',
						refBBox.height + HIGHLIGHT_WIDTH * 2 / global.scaleY);
	}
}

/******************************************************************
 LISTENER FUNCTIONS
 ******************************************************************/
function barZoomIn() {
	scaleX(global.scaleX + 0.5);
}

function barZoomOut() {
	scaleX(global.scaleX - 0.5);
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

function scrollToEnd() {
	translateX(getMinTranslateX() + getSvgWidth() * (1.0 - FILES_PORTION));
}

function fileZoomIn() {
	scaleY(global.scaleY + 0.3);
}

function fileZoomOut() {
	scaleY(global.scaleY - 0.3);
}

function showUp() {
	translateY(global.translateY + 1);
}

function showDown() {
	translateY(global.translateY - 1);
}

function showPageUp() {
	var extent = Math.floor(getSvgHeight() / (ROW_HEIGHT * global.scaleY));
	translateY(global.translateY + extent - 1);
}

function showPageDown() {
	var extent = Math.floor(getSvgHeight() / (ROW_HEIGHT * global.scaleY));
	translateY(global.translateY - extent + 1);
}

function undo() {
	// close context menu if there is any
	hideContextMenu();
	var result = [];

	for ( var i = 0; i < global.selected.length; ++i) {
		result.push([ global.selected[i].sid, global.selected[i].id ]);
	}

	if (result.length > 0)
		azurite.selectiveUndo(result);
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
	var result = new Array();
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

	updateTicks();
	updateHScroll();
}

function scaleY(sy) {
	sy = clamp(sy, 0.1, 10);
	global.scaleY = sy;

	updateSubRectsTransform();

	svg.subRects.selectAll('rect.op_rect').attr('height', rectDraw.hFunc);

	svg.subFiles.selectAll('text').attr('y', fileDraw.yFunc);

	updateSeparatingLines();

	updateHighlight();
}

function translateX(tx) {
	tx = clamp(tx, getMinTranslateX(), 0);
	global.translateX = tx;

	updateSubRectsTransform();

	updateTicks();
	updateHScroll();
}

function getMinTranslateX() {
	var result = 0, i;
	for (i = 0; i < global.sessions.length; ++i) {
		var session = global.sessions[i];
		result += session.g.node().getBBox().width;
	}
	
	return -result * global.scaleX;
}

function translateY(ty) {
	ty = clamp(ty, getMinTranslateY(), 0);
	global.translateY = ty;

	updateSubRectsTransform();

	svg.subFiles.selectAll('text').attr('y', fileDraw.yFunc);

	updateSeparatingLines();
	updateVScroll();
}

function getMinTranslateY() {
	return 1 - global.files.length;
}

function updateSubRectsTransform() {
	svg.subRects.attr('transform', 'translate(' + global.translateX + ' '
			+ (global.translateY * ROW_HEIGHT * global.scaleY) + ') '
			+ 'scale(' + global.scaleX + ' ' + global.scaleY + ')');
}

function showFrom(absTimestamp) {
	showTimestamp(absTimestamp, 0);
}

function showUntil(absTimestamp) {
	showTimestamp(absTimestamp, getSvgWidth() * (1.0 - FILES_PORTION));
}

function showTimestamp(absTimestamp, offsetInPixels) {
	if (global.sessions.length == 0) {
		translateX(0);
		return;
	}
	
	// binary search through the sessions.
	var startIndex = 0; var endIndex = global.sessions.length - 1;
	var midIndex = Math.floor((startIndex + endIndex) / 2);
	
	while (startIndex <= endIndex) {
		var midSession = global.sessions[midIndex];
		
		// found it.
		if (midSession.startAbsTimestamp <= absTimestamp &&
				absTimestamp <= midSession.endAbsTimestamp) {
			
			// Get the tx offset.
			var txOffset = midSession.g.node().getBBox().x;
			
			if (global.layout == LayoutEnum.COMPACT) {
				// again, binary search through the rects.
				var rects = midSession.g.selectAll('rect.op_rect').filter(function (d) {
					return d.isVisible();
				})[0];
				
				var startRectIndex = 0, endRectIndex = rects.length - 1;
				var midRectIndex = Math.floor((startRectIndex + endRectIndex) / 2);
				
				while (startRectIndex <= endRectIndex) {
					var midRect = rects[midRectIndex];
					var data = midRect.__data__;
					var absT1 = midSession.sid + data.t1;
					var absT2 = midSession.sid + data.t2;
					var rectBounds = midRect.getBBox();
					
					// found it.
					if (absT1 <= absTimestamp && absTimestamp <= absT2) {
						var tx = txOffset + rectBounds.x + rectBounds.width * (absTimestamp - absT1) / (absT2 - absT1);
						translateX(-tx * global.scaleX + offsetInPixels);
						return;
					}
					
					if (absTimestamp < absT1) {
						endRectIndex = midRectIndex - 1;
					}
					else {
						startRectIndex = midRectIndex + 1;
					}
					
					midRectIndex = Math.floor((startRectIndex + endRectIndex) / 2);
				}
				
				// Not in the middle of a rect.
				if (startRectIndex == 0) {
					translateX(-txOffset * global.scaleX + offsetInPixels);
				}
				else if (startRectIndex == rects.length) {
					translateX(-(txOffset + midSession.g.node().getBBox().width) * global.scaleX + offsetInPixels);
				}
				else {
					translateX(-(txOffset + rects[startIndex].getBBox().x) * global.scaleX + offsetInPixels);
				}
				
				return;
			}
			else if (global.layout == LayoutEnum.REALTIME) {
				var sessionBounds = midSession.g.node().getBBox();
				var tx = txOffset + sessionBounds.width * (absTimestamp - midSession.startAbsTimestamp) / (midSession.endAbsTimestamp - midSession.startAbsTimestamp);
				translateX(-tx * global.scaleX + offsetInPixels);
				
				return;
			}
		}
		
		if (absTimestamp < midSession.startAbsTimestamp) {
			endIndex = midIndex - 1;
		}
		else {
			startIndex = midIndex + 1;
		}
		
		midIndex = Math.floor((startIndex + endIndex) / 2);
	}
	
	// Couldn't found. Somewhere in the middle of sessions.
	if (startIndex == 0) {
		translateX(0);
	}
	else if (startIndex == global.sessions.length) {
		translateX(getMinTranslateX());
	}
	else {
		translateX(-global.sessions[startIndex].g.node().getBBox().x + offsetInPixels);
	}
}

function translateXToTimestamp(tx) {
	return -global.translateX * DEFAULT_RATIO / global.scaleX;
}

function updateHScroll() {
	var trackSize = $('#hscroll_thumbtrack').width();

	var extent = getSvgWidth() * (1.0 - FILES_PORTION);
	var thumbSize = Math.max(Math.floor(extent * trackSize
			/ (extent - getMinTranslateX())), MIN_SCROLL_THUMB_SIZE);

	var thumbRelativePos = global.translateX / getMinTranslateX();
	if (global.translateX == 0 || getMinTranslateX() == 0) {
		thumbRelativePos = 0;
	}

	d3.select('#hscroll_thumb').style('width', thumbSize + 'px').style('left',
			Math.floor((trackSize - thumbSize) * thumbRelativePos) + 'px');
}

function updateVScroll() {
	var trackSize = $('#vscroll_thumbtrack').height();

	var extent = Math.floor(getSvgHeight() / (ROW_HEIGHT * global.scaleY));
	var thumbSize = clamp(Math.floor(trackSize * extent
			/ (global.files.length + extent - 1)), MIN_SCROLL_THUMB_SIZE,
			trackSize);

	var thumbRelativePos = global.translateY / getMinTranslateY();
	if (global.translateY == 0 || getMinTranslateY() == 0) {
		thumbRelativePos = 0;
	}

	d3.select('#vscroll_thumb').style('height', thumbSize + 'px').style('top',
			Math.floor((trackSize - thumbSize) * thumbRelativePos) + 'px');
}

function updateTicks() {
	svg.subTicks.selectAll('text').remove();

/*	var start = global.startTimestamp - (global.translateX * DEFAULT_RATIO)
			/ global.scaleX;
	var end = start + getSvgWidth() * (1.0 - FILES_PORTION) * DEFAULT_RATIO
			/ global.scaleX;

	var timeScale = d3.time.scale().domain([ new Date(start), new Date(end) ])
			.range([ 0, getSvgWidth() * (1.0 - FILES_PORTION) ]);

	var ticks = timeScale.ticks(NUM_TIMESTAMPS);

	d3.selectAll(ticks).each(
			function(d) {
				svg.subTicks.append('text').attr('x', timeScale(this)).attr(
						'dy', '1em').attr('fill', 'white').attr('text-anchor',
						'middle').text(d3.time.format('%I:%M %p')(this));

				svg.subTicks.append('text').attr('x', timeScale(this)).attr(
						'dy', '2em').attr('fill', 'white').attr('text-anchor',
						'middle').text(d3.time.format('%x')(this));
			});*/
}

function test() {
	var sid = new Date().valueOf();
	// This must be bigger than the last known operation timestamp.
	if (global.lastOperation != null && sid < global.lastOperation.getAbsT2()) {
		sid = global.lastOperation.getAbsT2() + Math.floor(Math.random() * 5000);
	}
	
	addFile('DummyProject', 'Test.java');
	addRandomOperations(sid, 100, true);

	addFile('DummyProject', 'Test2.java');
	addRandomOperations(sid, 200, false);

	addFile('DummyProject', 'Test3.java');
	addRandomOperations(sid, 50, false);

	addFile('DummyProject', 'Test.java');
	addRandomOperations(sid, 100, false);
	
	layout();
	
	scrollToEnd();
}

function addRandomOperations(sid, count, startover) {
	var i = 0;
	var id = -1;
	var t = 0;

	if (global.lastOperation != null && !startover) {
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
	return parseInt(svg.main.style('width')) - CHART_MARGINS.left
			- CHART_MARGINS.right;
}

function getSvgHeight() {
	return parseInt(svg.main.style('height')) - CHART_MARGINS.top
			- CHART_MARGINS.bottom;
}
