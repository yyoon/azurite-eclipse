/**
 * Things should be executed at the beginning.
 */

// Disable selection.
document.unselectable = "on";
document.onselectstart = function () { return false };


/**
 * Constants. (Always use UPPER_CASE.)
 */
var MENU_PANEL_HEIGHT = 75;

var TYPE_INSERT = 0;
var TYPE_DELETE = 1;
var TYPE_REPLACE = 2;

var NUM_TIMESTAMPS = 3;

var MIN_WIDTH = 5;
var ROW_HEIGHT = 30;
var DEFAULT_RATIO = 1000;


// mapping functions
var rectDraw = {};
rectDraw.xFunc = function (d) { return (d.sid + d.t1 - global.startTimestamp) / DEFAULT_RATIO; };
rectDraw.yFunc = function (d) { return ROW_HEIGHT * d.y1 / 100; };
rectDraw.wFunc = function (d) { return Math.max(MIN_WIDTH / global.scaleX, (d.t2 - d.t1) / DEFAULT_RATIO); };
rectDraw.hFunc = function (d) { return Math.max(MIN_WIDTH / global.scaleY, ROW_HEIGHT * (d.y2 - d.y1) / 100); };

var fileDraw = {};
fileDraw.yFunc = function (d, i) { return ROW_HEIGHT * i * global.scaleY + 10; };

/**
 * Global variables. (Always use a pseudo-namespace.)
 */
var global = {};

// variables to remember the last window size
global.lastWindowWidth = null;
global.lastWindowHeight = null;

global.files = [];
global.blocksToDraw = [];
global.selected = [];

// transforms
global.translateX = 0;
global.translateY = 0;
global.scaleX = 1;
global.scaleY = 1;

// last file opened
global.currentFile = null;
global.lastOperation = null;

// Timestamps
global.maxTimestamp = 0;
global.startTimestamp = new Date().valueOf();

// Dragging
global.dragging = false;
global.dragStart = [];


// 5 min, 20 min, 1 hour, 4 hour
var BAR_ZOOM_LEVELS = [300000, 1200000, 3600000, 14400000000];

// height of each file title
var FILE_ZOOM_LEVELS = [30, 40, 50, 60];

// Current index of zoom level
global.barZoomIndex = 0;
global.fileZoomIndex = 0;

// Page index to show based on currrent zoom level
// eg. If zoom level is 0, and if page index is 0, show from 0 to 49. If index is 2, show from 100 to 149
global.barCurIndex = 0;
global.barMaxPageIndex = 0;
global.fileCurIndex = 0;
global.fileMaxPageIndex = 0;

// Min and max timestamp to show at current zoom level
global.minToShow = 0;
global.maxToShow = 0;

// Width of bars
global.xBar;
global.xRule;
global.numXTicks;


// context menu
var cmenu = {};
cmenu.isContextMenuVisible = false;
cmenu.isRightButtonDown = false;
cmenu.isCtrlDown = false;


/**
 * SVG Setup.
 */
var svg = {};

function setupSVG() {
	svg.main = d3.select('#svg_wrapper')
		.append('svg')
		.attr('class', 'svg')

	svg.subFiles = svg.main
		.append('g')
		.attr('id', 'sub_files')
        .attr('clip-path', 'url(#clipFiles)');
		
	svg.subRectsWrap = svg.main
		.append('g')
		.attr('id', 'sub_rects_wrap')
        .attr('clip-path', 'url(#clipRectsWrap)');
    
    svg.subRects = svg.subRectsWrap
        .append('g')
        .attr('id', 'sub_rects');
		
	svg.subTicks = svg.main
		.append('g')
		.attr('id', 'sub_ticks');

	svg.main.append('rect')
		.attr('class', 'selection_box')
		.style('fill', 'yellow')
		.style('opacity', 0.3);
        
    svg.clipFiles = svg.main
        .append('clipPath')
        .attr('id', 'clipFiles')
        .append('rect');
    
    svg.clipRectsWrap = svg.main
        .append('clipPath')
        .attr('id', 'clipRectsWrap')
        .append('rect');
        
    recalculateClipPaths();
}

function recalculateClipPaths() {
    var svgWidth = parseInt(svg.main.style('width'));
    var svgHeight = parseInt(svg.main.style('height'));
    
    var filesPortion = 0.15;
    svg.clipFiles
        .attr('width', (svgWidth * filesPortion) + 'px')
        .attr('height', (svgHeight - 20) + 'px');
    
    svg.subRectsWrap
        .attr('transform', 'translate(' + (svgWidth * filesPortion) + ' 0)');
        
    svg.clipRectsWrap
        .attr('width', (svgWidth * (1.0 - filesPortion)) + 'px')
        .attr('height', (svgHeight - 20) + 'px');
    
    svg.subTicks
        .attr('transform', 'translate(' + (svgWidth * filesPortion) + ' ' + (svgHeight - 20) + ')');
}

/**
 * An object that keeps track of insert, delete and replace for each file.
 */
function File(path, fileName) {
    this.path = path;
    this.fileName = fileName;
    this.operations = [];
    
    this.g = svg.subRects.append('g');
}

/**
 * An object for representing an operation itself.
 */
function EditOperation(sid, id, t1, t2, y1, y2, type) {
    this.sid = sid;
    this.id = id;
    this.t1 = t1;
    this.t2 = (t2 == null ? t1 : t2);
    this.y1 = y1;
    this.y2 = y2;
    this.type = type;
    this.color;
    
    if(type == TYPE_INSERT) {
        this.color = "green";
    } else if(type == TYPE_DELETE) {
        this.color = "red";
    } else if(type == TYPE_REPLACE) {
        this.color = "blue";
    }
}

/**
 * Called by Azurite.
 * Adds a new row if the given file is not already in the list.
 */
function addFile(path) {
    var fileName = path.match(/[^\\/]+$/)[0];
    
    for(var index in global.files) {
        if(global.files[index].path == path) {
            global.currentFile = global.files[index];
            return;
        }
    }
    
    var newFile = new File(path, fileName);
    newFile.g.attr('transform', 'translate(0 ' + (global.files.length * ROW_HEIGHT) + ')');
    
    global.files.push(newFile);
    global.currentFile = newFile;
    
    svg.subFiles.selectAll('text').data(global.files)
        .enter()
        .append('text')
        .attr('x', '10px')
        .attr('y', fileDraw.yFunc)
        .attr('dy', '1em')
        .attr('fill', 'white')
        .text(function (d) { return d.fileName; });
}

/**
 * Called by Azurite.
 * Sets the start timestamp.
 */
function setStartTimestamp(timestamp) {
    global.startTimestamp = parseInt(timestamp);
    drawRules();
}


/**
 * Called by Azurite.
 * Add an edit operation to the end of the file.
 * Note that this is called immediately after an edit operation is performed.
 */
function addOperation(sid, id, t1, t2, y1, y2, type) {
    var newOp = new EditOperation(
        parseInt(sid),
        parseInt(id),
        parseInt(t1),
        parseInt(t2),
        parseFloat(y1),
        parseFloat(y2),
        parseInt(type)
    );

    var i;
    var fileIndex = -1;
    
    for(i = 0; i < global.files.length; i++) {
        if(global.files[i].path == global.currentFile.path) {
            fileIndex = i;
            break;
        }
    }
    
    if(fileIndex == -1)
        return;
    
    global.currentFile.operations.push(newOp);
    global.lastOperation = newOp;
    
    updateMaxTimestamp(t1, t2);
    
    global.lastRect = global.currentFile.g.selectAll('rect').data( global.currentFile.operations )
        .enter()
        .append('rect')
        .attr('x', rectDraw.xFunc)
        .attr('y', rectDraw.yFunc)
        .attr('width', rectDraw.wFunc)
        .attr('height', rectDraw.hFunc)
        .attr('fill', function (d) { return d.color; });
}

/**
 * Called by Azurite.
 * Update the timestamp2 value for an existing operation, in case multiple
 * operations are merged into one.
 */
function updateOperationTimestamp2(id, t2) {
    if (global.lastOperation == null || global.lastOperation.id != parseInt(id))
        return;
    
    global.lastOperation.t2 = t2;
    
    updateMaxTimestamp(t2, t2);
    
    if (global.lastRect != null) {
        global.lastRect.attr('width', rectDraw.wFunc);
    }
}

function redraw() {
    var svgWidth = parseInt(svg.main.style('width'));
    var svgHeight = parseInt(svg.main.style('height'));
    
    recalculateClipPaths();
    
    svg.subRects.selectAll('rect').remove();
    
    // remove highlights
    global.selected = [];
    svg.subRects.selectAll('polygon').remove();
    
    $('.block').tipsy({ 
        gravity: 'se', 
        html: true, 
        title: function() {
              var d = this.__data__;
              return 'id: ' + d.id;
        }
    });
}

function getCursorPosition(e) {
    e = e || window.event;
    
    if(e) {
        if (e.pageX || e.pageX == 0) return [e.pageX,e.pageY];
            var dE = document.documentElement || {};
        
        var dB = document.body || {};
        if ((e.clientX || e.clientX == 0) && ((dB.scrollLeft || dB.scrollLeft == 0) || (dE.clientLeft || dE.clientLeft == 0))) 
            return [e.clientX + (dE.scrollLeft || dB.scrollLeft || 0) - (dE.clientLeft || 0),e.clientY + (dE.scrollTop || dB.scrollTop || 0) - (dE.clientTop || 0)];
    }
    
    return null;
}

/*
 * When the page loads, load a log file
 */
window.onload = function () {
    azurite.initialize();
	$('#svg1_outer_wrap').css('padding-bottom', $.getScrollbarWidth());
	setupSVG();
    initContextMenu();
    
    window.onresize();
    initEventHandlers();
}

window.onresize = function (e) {
    // if window size are different, redraw everything
    if(global.lastWindowWidth != window.innerWidth || global.lastWindowHeight != window.innerHeight) {
        global.lastWindowWidth = window.innerWidth;
        global.lastWindowHeight = window.innerHeight;
        
        var svgWidth = parseInt(svg.main.style('width'));
        var svgHeight = parseInt(svg.main.style('height'));
        
        recalculateClipPaths();
    }
}

/******************************************************************
 MOUSE EVENT FUNCTIONS
 ******************************************************************/
 
function initContextMenu() {
    global.divContext = document.getElementById('context_menu');
    
    global.divContext.onmouseover = function() { mouseOverContext = true; };
    global.divContext.onmouseout = function(e) {
        e = event.toElement || event.relatedTarget;
        
        while(e && e.parentNode && e.parentNode != window) {
            if(e.parentNode == this || e == this) {
                return;
            }
            e = e.parentNode;
        }
        
        // hideContextMenu();
    };
}

function initEventHandlers() {
    svg.main.on("mousewheel", function () {
        scrollRight( d3.event.wheelDelta / 10 );
    });
    
    document.addEventListener("keydown", function(e) {
        if(e.keyCode == 17) 
            cmenu.isCtrlDown = true;
    }
    , false);
    
    document.addEventListener("keyup", function(e) {
        if(e.keyCode == 17)
            cmenu.isCtrlDown = false;
    }
    , false);
    
    var draggableArea = {
        top: 0,
        bottom: 0,
        left: 0,
        right: 0
    };
    
    document.onmousedown =  function(e) {
        if(cmenu.isContextMenuVisible) {
            hideContextMenu();
        }
        
        if ("which" in event) { // Gecko (Firefox), WebKit (Safari/Chrome) & Opera
            cmenu.isRightButtonDown = event.which == 3; 
        } else if ("button" in event) { // IE, Opera 
            cmenu.isRightButtonDown = event.button == 2; 
        }
        
        var mouseX = e.clientX;
        var mouseY = e.clientY - MENU_PANEL_HEIGHT;
        
        if(cmenu.isRightButtonDown || mouseX < draggableArea.left || mouseX > draggableArea.right || mouseY < draggableArea.top || mouseY > draggableArea.bottom) {
            return;
        }
        
        if(global.dragging)
            return;
        
        global.dragging = true;
        
        if(!cmenu.isCtrlDown) {
            global.selected = [];
            svg.subBar.selectAll('polygon').remove();
        }
        
        d3.select('.selection_box')
            .attr('x', mouseX)
            .attr('y', mouseY);
            
        global.dragStart[0] = mouseX;
        global.dragStart[1] = mouseY;
    };
    
    document.onmousemove = function(e) {
        if(!global.dragging)
            return;
        
        var newX, newY;
        
        var mouseX = e.clientX;
        var mouseY = e.clientY - MENU_PANEL_HEIGHT;
        
        if(mouseX < draggableArea.left)
            newX = draggableArea.left;
        else if(mouseX > draggableArea.right) 
            newX = draggableArea.right;
        else
            newX = mouseX;
            
        if(mouseY < draggableArea.top)
            newY = draggableArea.top;
        else if(mouseY > draggableArea.bottom)
            newY = draggableArea.bottom;
        else
            newY = mouseY;
        
        
        if(newX - global.dragStart[0] < 0) {
            d3.select('.selection_box')
                .attr('x', newX)
                .attr('width', global.dragStart[0] - newX);
        } else {
            d3.select('.selection_box')
                .attr('x', global.dragStart[0])
                .attr('width', newX - global.dragStart[0]);
        }
        
        if(newY - global.dragStart[1] < 0) {
            d3.select('.selection_box')
                .attr('y', newY )
                .attr('height', global.dragStart[1] - newY);
        } else {
            d3.select('.selection_box')
                .attr('y', global.dragStart[1])
                .attr('height', newY - global.dragStart[1]);
        }
        
        d3.select('.selection_box')
            .attr('display', 'block');
    };
    
    document.onmouseup = function(e) {
        if(cmenu.isRightButtonDown) {
            showContextMenu(e);
            return;
        }
    
        if(!global.dragging)
            return;
        
        d3.select('.selection_box')
            .attr('display', 'none');
        
        var x1, y1, x2, y2;
        
        var mouseX = e.clientX;
        var mouseY = e.clientY - MENU_PANEL_HEIGHT;
    
        if(global.dragStart[0] <= mouseX) {
            x1 = global.dragStart[0];
            x2 = mouseX;
        } else {
            x1 = mouseX;
            x2 = global.dragStart[0];
        }
        
        if(global.dragStart[1] <= mouseY) {
            y1 = global.dragStart[1];
            y2 = mouseY;
        } else {
            y1 = mouseY;
            y2 = global.dragStart[1];
        }
        addSelections(x1, y1, x2, y2, (CHART_MARGINS.left + global.titleWidth), CHART_MARGINS.top);
    
        global.dragging = false;   
        global.dragStart = [];
    }
}
 
function showContextMenu(event) {   

    var offsetX = 0, offsetY = 0;
    
    if(event.clientX + parseInt(global.divContext.style.width) > global.lastWindowWidth) {
        offsetX = event.clientX + parseInt(global.divContext.style.width) - global.lastWindowWidth
    }
    
    if(event.clientY + parseInt(global.divContext.style.height) > global.lastWindowHeight) {
        offsetY = event.clientY + parseInt(global.divContext.style.height) - global.lastWindowHeight;
    }
    
    global.divContext.style.left = event.clientX - offsetX -10 + 'px';
    global.divContext.style.top = event.clientY - offsetY -10 + 'px';
    global.divContext.style.display = 'block';
    
    cmenu.isContextMenuVisible = true;
}

function hideContextMenu() {
    global.divContext.style.display = 'none';
    cmenu.isContextMenuVisible = false;
}


function addSelectionsByIds(ids, clearPreviousSelection) {
    if (clearPreviousSelection) {
        global.selected = [];
    }
    
    for (var i = 0; i < ids.length; ++i) {
        var id = ids[i];
        global.selected.push(id);
    }
    
    drawHighlight();
}


function addSelections(x1, y1, x2, y2, offsetX, offsetY) {
    var blockLength = global.blocksToDraw.length;
    var somethingAdded = false;
    
    for(var i = 0; i < blockLength; i++) {
        var id = global.blocksToDraw[i].id;
        if ($.inArray(id, global.selected) !== -1) {
            continue;
        }
        
        if(trivialRejectTest(x1, y1, x2, y2, offsetX, offsetY, global.blocksToDraw[i]) == 0) {
            global.selected.push(id);
            somethingAdded = true;
        }
    }
    
    if (somethingAdded) {
        drawHighlight();
    }
}


function drawHighlight() {
    svg.subBar.selectAll('polygon').remove();
    
    var count = global.selected.length;
    var itemsToHighlight = [];
    var prev = null;
    var item;
    
    if(count > 0) {
        var blockLength = global.blocksToDraw.length;
        for (var i = 0; i < blockLength; ++i) {

            var block = global.blocksToDraw[i];
            
            if ($.inArray(block.id, global.selected) !== -1) {
                if(prev == null) {
                    prev = block;
                    item = {startX: prev.x, startY: prev.y, endX: prev.x + prev.width, endY: prev.y + prev.height};
                } else {
                    if(item.startY == block.y &&  Math.abs(item.endX - block.x) <= 8) {
                        item.endX = (item.endX > (block.x + block.width)) ? item.endX : (block.x + block.width);
                    } else {
                        itemsToHighlight.push(item);
                        item = {startX: block.x, startY: block.y, endX: block.x + block.width, endY: block.y + block.height};
                    }
                    prev = block;
                }
            }
        }
        itemsToHighlight.push(item);
            
        var highlight_width = 3;
        
        if(itemsToHighlight != []) {
            svg.blocks.selectAll('polygon')
                .data(itemsToHighlight).enter().append('polygon')
                .attr("points", function(d) { return ((d.startX) + "," + (d.startY) + " \ " + 
                (d.endX) + "," + (d.startY) + " \ " +
                (d.endX) + "," + (d.endY) + " \ " +
                (d.startX) + "," + (d.endY)) })
                .style("stroke", "yellow")
                .style("stroke-width", highlight_width)
                .style("fill-opacity", 0);
        }
    }
}

function trivialRejectTest(x1, y1, x2, y2, offsetX, offsetY, block) {
    var result0 = 0, result1= 0;
    var left = 1;
    var right = 2;
    var bottom = 4;
    var top = 8;
    
    
    if(x1 < (block.x + offsetX)) {
        result0 = result0 | left;
    } else if(x1 > (block.x + block.width + offsetX)) {
        result0 = result0 | right;
    }
    
    if(y1 < (block.y + offsetY)) {
        result0 = result0 | top;
    } else if(y1 > (block.y + block.height + offsetY)) {
        result0 = result0 | bottom;
    }
    
    if(x2 < (block.x + offsetX)) {
        result1 = result1 | left;
    } else if(x2 > (block.x + block.width + offsetX)) {
        result1 = result1 | right;
    }
    
    if(y2 < (block.y + offsetY)) {
        result1 = result1 | top;
    } else if(y2 > (block.y + block.height + offsetY)) {
        result1 = result1 | bottom;
    }
    
    return (result0 & result1);
}

    
/******************************************************************
 LISTENER FUNCTIONS
 ******************************************************************/
function barZoomIn() {
    scaleX( global.scaleX + 0.1 );
}

function barZoomOut() {
    scaleX( global.scaleX - 0.1 );
}

function showBefore() {
    scrollRight( 100 );
}

function showAfter() {
    scrollRight( -100 );
}

function scrollRight(pixels) {
    translateX( global.translateX + pixels / global.scaleX );
}

function fileZoomIn() {
}

function fileZoomOut() {
}

function showUp() {
}

function showDown() {
}


function undo() {
    // close context menu if there is any
    hideContextMenu();
    var result = [];
    
    for(var i in global.selected) {
        result.push(global.selected[i]);
    }
    
    if(result.length > 0)
        azurite.selectiveUndo(result);
}
    

function updateMaxTimestamp(timestamp, timestamp2) {
    // update global.maxTimestamp if necessary
    if (timestamp2 == null && timestamp > global.maxTimestamp) {
        global.maxTimestamp = timestamp;
    } else if (timestamp2 != null && timestamp2 > global.maxTimestamp) {
        global.maxTimestamp = timestamp2;
    }
    
    // recalculate page index and number of global.files to draw
    global.barMaxPageIndex = Math.ceil(
        global.maxTimestamp / BAR_ZOOM_LEVELS[global.barZoomIndex]) - 1;
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
    for (var i = begin; i < end; ++i) {
        result[i - begin] = i;
    }
    
    return result;
}

function scaleX(sx) {
    sx = clamp( sx, 0.1, 50 );
    global.scaleX = sx;
    
    updateSubRectsTransform();
    
    svg.subRects.selectAll('rect')
        .attr('width', rectDraw.wFunc);
}

function scaleY(sy) {
    sy = clamp( sy, 0.1, 10 );
    global.scaleY = sy;
    
    updateSubRectsTransform();
    
    svg.subRects.selectAll('rect')
        .attr('height', rectDraw.hFunc);
        
    svg.subFiles.selectAll('text')
        .attr('y', fileDraw.yFunc);
}

function translateX(tx) {
    tx = Math.min( tx, 0 );
    global.translateX = tx;
    
    updateSubRectsTransform();
}

function updateSubRectsTransform() {
    svg.subRects
        .attr('transform',
            'translate(' + global.translateX + ' ' + global.translateY + ') ' +
            'scale(' + global.scaleX + ' ' + global.scaleY + ')');
}

function showFrom(timestamp) {
    var translateX = -(timestamp - global.startTimestamp) / DEFAULT_RATIO;
}

function test() {
	addFile('Test.java');
	addRandomOperations(100);
    
    addFile('Test2.java');
    addRandomOperations(200);
    
	addFile('Test.java');
	addRandomOperations(100);
}

function addRandomOperations(count) {
	var i = 0;
	var id = -1;
	var t = 0;
	
	if (global.lastOperation != null) {
		id = global.lastOperation.id;
        t = global.lastOperation.t2;
	}
	
	for (i = 0; i < count; ++i) {
		++id;
		var t1 = t + Math.floor(Math.random() * 5000) + 5000;
		var t2 = t1 + Math.floor(Math.random() * 5000) + 5000;
		t = t2;
        
        var y1 = Math.floor(Math.random() * 100);
        var y2 = clamp(y1 + Math.floor(Math.random() * 30), y1, 100);
		addOperation(global.startTimestamp, id, t1, t2, y1, y2, Math.floor(Math.random() * 3));
	}
}
