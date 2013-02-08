/**
 * Things should be executed at the beginning.
 */

// Workaround for console.log problem.
if (!window.console) window.console = {};
if (!window.console.log) window.console.log = function () { };

// Disable selection.
document.unselectable = "on";
document.onselectstart = function () { return false };


/**
 * Constants. (Always use UPPER_CASE.)
 */
var MENU_PANEL_HEIGHT = 75;
var CHART_MARGINS = {
    left: 5,
    right:5,
    top: 15,
    bottom: 30
};

var TYPE_INSERT = 0;
var TYPE_DELETE = 1;
var TYPE_REPLACE = 2;

var NUM_TIMESTAMPS = 3;


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

svg.main = d3.select('#svg_wrapper')
    .append('svg')
    .attr('class', 'svg')
    .attr('id', 'svg');

svg.subFile = svg.main
    .append('g')
    .attr('class', 'sub_file');
    
svg.subBar = svg.main
    .append('g')
    .attr('id', 'sub_bar');
    
svg.blocks = svg.subBar
    .append('g')
    .attr('class', 'blocks');
    
svg.fileLines = svg.subBar
    .append('g')
    .attr('class', 'file_lines');
    
svg.rules = svg.subBar
    .append('g')
    .attr('class', 'rules');

svg.main.append('rect')
    .attr('class', 'selection_box')
    .style('fill', 'yellow')
    .style('opacity', 0.3);

/**
 * An object that keeps track of insert, delete and replace for each file.
 */
function File(path, fileName) {
    this.path = path;
    this.fileName = fileName;
    this.operations = [];
}

/**
 * An object for representing an operation itself.
 */
function EditOperation(id, timestamp, timestamp2, type) {
    this.id = id;
    this.timestamp = timestamp;
    this.timestamp2 = timestamp2;
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
 * Block object to be drawn on the screen.
 */
function Block(id, type, width, height, x, y, color, timestamp, timestamp2) {
    this.id = id;
    this.type = type;
    this.width = width;
    this.height = height;
    this.x = x;
    this.y = y;
    this.color = color;
    this.timestamp = timestamp;
    this.timestamp2 = timestamp2;
}


/**
 * Sets the given file as the current file.
 * If the given file does not exist in the global.files list, adds it.
 */
function checkFileList(path, fileName) {
    for(var k in global.files) {
        if(global.files[k].fileName === fileName) {
            global.currentFile = global.files[k];
            return;
        }
    }
    
    var file = new File(path, fileName);
    global.files.push(file);
    global.currentFile = file;
}

/**
 * Called by Azurite.
 * Adds a new row if the given file is not already in the list.
 */
function addFile(path) {
    var fileName = path.match(/[^\\/]+$/)[0];
    
    global.lastOperation = null;
    
    for(var index in global.files) {
        if(global.files[index].path == path) {
            global.currentFile = global.files[index];
            return;
        }
    }
    
    var newFile = new File(path, fileName);
    global.files.push(newFile);
    global.currentFile = newFile;
    
    drawFiles();
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
function addOperation(id, timestamp1, timestamp2, type, drawImmediately) {
    var newOp = new EditOperation(
        parseInt(id),
        parseInt(timestamp1),
        parseInt(timestamp2),
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
    
    updateMaxTimestamp(timestamp1, timestamp2);
    
    if (drawImmediately) {
        var numMaxFilesToDraw = getMaxFilesToDraw();
        var offset = numMaxFilesToDraw * global.fileCurIndex;
        
        if (offset <= fileIndex && fileIndex < offset + numMaxFilesToDraw) {
            var rowIndex = fileIndex - offset;
            
            if (createBlockFromOperation(newOp, rowIndex)) {
                var block = global.blocksToDraw[global.blocksToDraw.length - 1];
                
                // This block should be drawn immediately!
                svg.blocks.append('rect')
                    .attr('class', 'block')
                    .attr('width', block.width)
                    .attr('height', block.height)
                    .attr('x', block.x)
                    .attr('y', block.y)
                    .style('fill', block.color);
            }
        }
    }
    
    drawIndicator();
}

/**
 * Called by Azurite.
 * Update the timestamp2 value for an existing operation, in case multiple
 * operations are merged into one.
 */
function updateOperationTimestamp2(id, timestamp2) {
    if (global.lastOperation == null || global.lastOperation.id != parseInt(id))
        return;
    
    global.lastOperation.timestamp2 = timestamp2;
    
    updateMaxTimestamp(timestamp2, null);
    
    redraw();
}

function redraw() {
    var svgWidth = parseInt(svg.main.style('width'));
    var svgHeight = parseInt(svg.main.style('height'));
    
    svg.subBar.selectAll('rect').remove();
    
    // remove highlights
    global.selected = [];
    svg.subBar.selectAll('polygon').remove();

    // translate subFile and subBar
    svg.subFile.attr('transform', 'translate(' + CHART_MARGINS.left + ',' + CHART_MARGINS.top + ')');
    svg.subBar.attr('transform', 'translate(' + (CHART_MARGINS.left + global.titleWidth) + ',' + CHART_MARGINS.top + ')');
    
    var filesToDraw = getFilesToDraw();
    
    drawMenu();
    drawFiles(filesToDraw);
    drawBars(filesToDraw);
    drawRules();
    drawScrollbar();
    drawHighlight();
    drawIndicator();
    
    $('.block').tipsy({ 
        gravity: 'se', 
        html: true, 
        title: function() {
              var d = this.__data__;
              return 'id: ' + d.id;
        }
    });
}

function getFilesToDraw() {
    // using the current svg height, determine the number of global.files to draw
    var numMaxFilesToDraw = getMaxFilesToDraw();
    global.fileMaxPageIndex = Math.ceil(global.files.length / numMaxFilesToDraw) - 1;
    if (global.fileMaxPageIndex < 0) {
        global.fileMaxPageIndex = 0;
    }
    
    // calculate the number of global.files to draw
    var filesToDraw = [];
    var offset = numMaxFilesToDraw * global.fileCurIndex;
    
    for(var i = 0; i < numMaxFilesToDraw && global.files[offset+i] != null; i++)
        filesToDraw.push(global.files[offset+i]);
    
    return filesToDraw;
}

function getMaxFilesToDraw() {
    return Math.floor((parseInt(svg.main.style('height')) - CHART_MARGINS.top - CHART_MARGINS.bottom) / FILE_ZOOM_LEVELS[global.fileZoomIndex]);
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

function drawMenu() {
    // draw timeline bar control
    var barZoomLevel = document.getElementById('bar_zoom_level');
    var barPageIndex = document.getElementById('bar_page_index');
    
    barZoomLevel.innerHTML = "Zoom Index : " + global.barZoomIndex + "/" + (BAR_ZOOM_LEVELS.length-1);
    barPageIndex.innerHTML = "Page Index : " + global.barCurIndex + "/" + global.barMaxPageIndex;
    
    // draw file list control
    var fileZoomLevel = document.getElementById('file_zoom_level');
    var filePageIndex = document.getElementById('file_page_index');
    
    fileZoomLevel.innerHTML = "Zoom Index : " + global.fileZoomIndex + "/" + (FILE_ZOOM_LEVELS.length-1);
    filePageIndex.innerHTML = "Page Index : " + global.fileCurIndex + "/" + global.fileMaxPageIndex;
}

function drawFiles(filesToDraw) {
    svg.subFile.selectAll('text').remove();
    
    if (filesToDraw == null) {
        filesToDraw = getFilesToDraw();
    }
    
    // draw file titles.
    svg.subFile.selectAll('.fileTitles')
        .data(filesToDraw)
        .enter().append('text')
        .text(function(d) { return d.fileName; })
        .attr('x', 0)
        .attr('y', function(d,i) { return (FILE_ZOOM_LEVELS[global.fileZoomIndex] * i) +  FILE_ZOOM_LEVELS[global.fileZoomIndex]/2} )
        .attr('dy', '0.5ex')
        .attr('text-anchor', 'start')
        .attr('class', 'file_title')
        .attr('fill', 'white')
        .style('-moz-user-select', 'none')
        .style('-webkit-user-select', 'none')
        .attr('onselectstart', false);
    
    // Lines
    svg.fileLines.selectAll('line').remove();
    
    // if there's no file, then don't draw a line.
    if (filesToDraw.length == 0) {
        return;
    }
    
    // draw file lines
    for(var i = 0; i < filesToDraw.length + 1; i++) {
        svg.fileLines.append('line')
            .attr('x1', 0)
            .attr('y1', FILE_ZOOM_LEVELS[global.fileZoomIndex] * i)
            .attr('x2', parseInt(svg.main.style('width')) * 0.85)
            .attr('y2', FILE_ZOOM_LEVELS[global.fileZoomIndex] * i)
            .attr('stroke', 'lightgray' )
            .style('stroke-width', 1);
    }
}

function drawBars(filesToDraw) {
    global.xRule = d3.scale.linear()
        .domain([0, NUM_TIMESTAMPS - 1])
        .range([global.minToShow, global.maxToShow]);
    
    global.xBar = d3.time.scale()
        .domain([global.minToShow, global.maxToShow])
        .range([0, global.barWidth]);
    
    // filter blocks to draw
    global.blocksToDraw = [];
    
    for(var i = 0; i < filesToDraw.length; i++) {
        var file = filesToDraw[i];
        var operations = file.operations;
        var length = operations.length;
        
        for(var j = 0; j < length; j++) {
            createBlockFromOperation(operations[j], i);
        }
    }
    
    svg.blocks.selectAll("rect")
        .data(global.blocksToDraw).enter().append("rect")
        .attr('class', 'block')
        .attr("width", function(d) { return d.width; })
        .attr("height", function(d) { return d.height; })
        .attr("x", function(d) { return d.x; })
        .attr("y", function(d) { return d.y; })
        .style("fill", function(d) { return d.color; });
}

function createBlockFromOperation(operation, rowIndex) {
    var timestamp = operation.timestamp;
    var timestamp2 = operation.timestamp2;
    
    // there should always be a timestamp.
    if (timestamp == null) {
        return false;
    }
    
    // if timestamp2 is not defined, just use the timestamp value.
    if (timestamp2 == null) {
        timestamp2 = timestamp;
    }
    
    // the block is outside of the current view.
    if (timestamp2 < global.minToShow || timestamp > global.maxToShow) {
        return false;
    }
    
    timestamp = clamp(timestamp, global.minToShow, global.maxToShow);
    timestamp2 = clamp(timestamp2, global.minToShow, global.maxToShow);
    
    blockWidth = global.xBar(timestamp2) - global.xBar(timestamp);
    if (blockWidth < global.minBlockWidth) {
        blockWidth = global.minBlockWidth;
    }
    
    if (timestamp2 >= global.maxTimestamp) {
        global.maxTimestamp = global.xBar.invert( global.xBar(timestamp) + blockWidth );
    }
    
    global.blocksToDraw.push(new Block(
        operation.id,
        operation.type,
        blockWidth,
        FILE_ZOOM_LEVELS[global.fileZoomIndex], 
        global.xBar(timestamp), 
        (FILE_ZOOM_LEVELS[global.fileZoomIndex] * rowIndex),
        operation.color, 
        timestamp, 
        timestamp2)
    );
    
    return true;
}

function drawRules() {
    svg.rules.selectAll('text').remove();
    
    svg.rules.selectAll(".rule")
        .data(range(0, NUM_TIMESTAMPS))
        .enter()
        .append("text")
        .attr("x", function(d,i) { return global.xBar(global.xRule(i)); })
        .attr("y", global.chartHeight + 15)
        .attr("text-anchor", function(d,i) {
            if(i == 0)
                return "start";
            else if(i == NUM_TIMESTAMPS - 1)
                return "end";
            else 
                return "middle";
        })
        .attr('fill', 'white')
        .text(function(d,i) { return dateFormat(new Date(global.startTimestamp + global.xRule(i)), 'hh:MMTT mm/dd') })
        .style('-moz-user-select', 'none')
        .style('-webkit-user-select', 'none')
        .attr('onselectstart', false);
}

function drawScrollbar() {
    // Only show the scrollbar when needed.
    if (global.barMaxPageIndex == null || global.barMaxPageIndex <= 0) {
        $('#x_scrollbar').css({"display": "none"});
        return;
    }

    $('#x_scrollbar').css({
        "position": 'absolute',
        "left": global.titleWidth + "px",
        "top": global.chartHeight + MENU_PANEL_HEIGHT + 50 + "px",
        "width": global.barWidth + "px",
        "display": "block"
    });
    
    var $scrollX = $('#x_scrollbar').slider({
        value: global.barCurIndex,
        min: 0,
        max: global.barMaxPageIndex,
        step: 1,
        orientation: 'horizontal',

        slide: function(event, ui) {
            //debugger;
            setBarCurIndex(ui.value);
            redraw();
        }
    });
    
    var handle_size = $scrollX.width() / (global.barMaxPageIndex + 1);
    var handle_margin_left = -handle_size * global.barCurIndex / global.barMaxPageIndex;
    if (global.barMaxPageIndex == 0) handle_margin_left = 0;
    handle_margin_left -= 1;    // I don't know why, but it's off by 1 px by default.
    
    $scrollX.find('.ui-slider-handle').css({
        "width": handle_size + "px",
        "margin-left": handle_margin_left + "px"
    });
    
}

function drawIndicator() {
    d3.select('.indicator').remove();
    
    // draw the "now" indicator only when needed.
    if (global.minToShow <= global.maxTimestamp && global.maxTimestamp <= global.maxToShow) {
        svg.subBar.append('line')
            .attr('class', 'indicator')
            .attr('x1', global.xBar(global.maxTimestamp))
            .attr('y1', 0)
            .attr('x2', global.xBar(global.maxTimestamp))
            .attr('y2', global.chartHeight)
            .attr('stroke', 'yellow' )
            .style('stroke-width', 2);
    }
}


/*
 * When the page loads, load a log file
 */
window.onload = function () {
    __AZURITE__initialize();
    initContextMenu();
    
    setBarCurIndex(0);
    setFileCurIndex(0);
    
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
        
        global.chartWidth = svgWidth - CHART_MARGINS.left - CHART_MARGINS.right;
        global.chartHeight = svgHeight - CHART_MARGINS.top - CHART_MARGINS.bottom;
        global.titleWidth = global.chartWidth * 0.15;
        global.barWidth = global.chartWidth * 0.85;
        
        global.minBlockWidth = global.barWidth * 0.005;
        
        redraw();
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
        top: CHART_MARGINS.top,
        bottom: CHART_MARGINS.top + global.chartHeight,
        left: (CHART_MARGINS.left + global.titleWidth),
        right:  (CHART_MARGINS.left + global.titleWidth + global.barWidth)
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
    setBarZoomIndex(global.barZoomIndex - 1);
    redraw();
}

function barZoomOut() {
    setBarZoomIndex(global.barZoomIndex + 1);
    redraw();
}

function showBefore() {
    setBarCurIndex(global.barCurIndex - 1);
    redraw();
}

function showAfter() {
    setBarCurIndex(global.barCurIndex + 1);
    redraw();
}

function fileZoomIn() {
    if(global.fileZoomIndex != 0) {
        global.fileZoomIndex--;
        redraw();
    }
}

function fileZoomOut() {
    if(global.fileZoomIndex != (FILE_ZOOM_LEVELS.length - 1)) {
        global.fileZoomIndex++;
        redraw();
    }
}

function showUp() {
    setFileCurIndex(global.fileCurIndex - 1);
    redraw();
}

function showDown() {
    setFileCurIndex(global.fileCurIndex + 1);
    redraw();
}

function setBarZoomIndex(newIndex) {
    global.barZoomIndex = clamp(newIndex, 0, BAR_ZOOM_LEVELS.length - 1);
    
    drawMenu();
}

function setBarCurIndex(newIndex) {
    global.barCurIndex = clamp(newIndex, 0, global.barMaxPageIndex);
    
    global.minToShow = global.barCurIndex * BAR_ZOOM_LEVELS[global.barZoomIndex];
    global.maxToShow = global.minToShow + BAR_ZOOM_LEVELS[global.barZoomIndex];
    
    drawMenu();
}

function setFileZoomIndex(newIndex) {
    global.fileZoomIndex = clamp(newIndex, 0, FILE_ZOOM_LEVELS.length - 1);
    
    drawMenu();
}

function setFileCurIndex(newIndex) {
    global.fileCurIndex = clamp(newIndex, 0, global.fileMaxPageIndex);
    
    drawMenu();
}


function undo() {
    // close context menu if there is any
    hideContextMenu();
    var result = [];
    
    for(var i in global.selected) {
        result.push(global.selected[i]);
    }
    
    if(result.length > 0)
        __AZURITE__selectiveUndo(result);
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


if (!__AZURITE__initialize) {
    __AZURITE__initialize = function () {
        console.log('__AZURITE__initialize call');
    }
}

if (!__AZURITE__selectiveUndo) {
    __AZURITE__selectiveUndo = function () {
        console.log('__AZURITE_selectiveUndo call');
    }
}
