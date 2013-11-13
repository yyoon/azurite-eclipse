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
