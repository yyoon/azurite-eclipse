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
