// timeline tests

var assert = buster.assert;
var refute = buster.refute;

buster.testCase("Buster Test", {
  "this should pass if buster is properly set": function() {
    assert.isTrue(true);
    refute.isTrue(false);
  }
});

buster.testCase("addFile Test", {
  "addFile must add an item to the global files array": function() {
    this.stub(window, "layoutFiles");

    addFile("dummyProject", "dummy/path/to/the/file.java");

    assert.same(global.files.length, 1);
    assert.same(global.files[0].project, "dummyProject");
    assert.same(global.files[0].path, "dummy/path/to/the/file.java");
    assert.same(global.files[0].fileName, "file.java");

    assert.calledOnce(window.layoutFiles);
  }
});

buster.testCase("binarySearch Test", {
  "binarySearch should return the indices correctly when target found": function() {
    var testArray = [1, 2, 3, 4, 6, 7, 8];

    var compareFuncGenerator = function(target_value) {
      return function(cur_value) {
        if (target_value < cur_value) { return -1; }
        if (target_value > cur_value) { return 1; }
        return 0;
      };
    };

    assert.same(binarySearch(testArray, compareFuncGenerator(1)), 0);
    assert.same(binarySearch(testArray, compareFuncGenerator(2)), 1);
    assert.same(binarySearch(testArray, compareFuncGenerator(3)), 2);
    assert.same(binarySearch(testArray, compareFuncGenerator(4)), 3);
    assert.same(binarySearch(testArray, compareFuncGenerator(6)), 4);
    assert.same(binarySearch(testArray, compareFuncGenerator(7)), 5);
    assert.same(binarySearch(testArray, compareFuncGenerator(8)), 6);
  },

  "binarySearch should return the correct complement indices when target not found": function() {
    var testArray = [1, 2, 3, 4, 6, 7, 8];

    var compareFuncGenerator = function(target_value) {
      return function(cur_value) {
        if (target_value < cur_value) { return -1; }
        if (target_value > cur_value) { return 1; }
        return 0;
      };
    };

    assert.same(binarySearch(testArray, compareFuncGenerator(0)), -1);
    assert.same(binarySearch(testArray, compareFuncGenerator(5)), -5);
    assert.same(binarySearch(testArray, compareFuncGenerator(9)), -8);
  }
});

buster.testCase("rectDraw.yFunc Test", {
  "rectangles should be placed correctly when they are no taller than the minimum height": function() {
    assert.same(ROW_HEIGHT, 30);
    assert.same(MIN_HEIGHT, 6);

    var op = {};

    op = { y1: 15, y2: 30 };
    assert.near(rectDraw.yFunc(op), 4.24, 0.01);
    assert.same(rectDraw.hFunc(op), MIN_HEIGHT);

    op = { y1: 90, y2: 95 };
    assert.near(rectDraw.yFunc(op), 22.74, 0.01);
    assert.same(rectDraw.hFunc(op), MIN_HEIGHT);
  },

  "rectangles should be placed correctly when they are taller than the minimum height": function() {
    assert.same(ROW_HEIGHT, 30);
    assert.same(MIN_HEIGHT, 6);

    assert.near(rectDraw.yFunc({ y1: 10, y2: 31 }), 3, 0.01);
    assert.near(rectDraw.yFunc({ y1: 0, y2: 100}), 0, 0.01);
    assert.near(rectDraw.yFunc({ y1: 10, y2: 100}), 3, 0.01);
  }
});
