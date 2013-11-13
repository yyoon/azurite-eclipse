// timeline tests

buster.testCase("Buster Test", {
  "this should pass if buster is properly set": function() {
    buster.assert.isTrue(true);
  }
});

buster.testCase("addFile Test", {
  "addFile must add an item to the global files array": function() {
    var layoutFilesCalled = false;

    var tempLayoutFiles = layoutFiles;
    layoutFiles = function() {
      layoutFilesCalled = true;
    };

    addFile("dummyProject", "dummy/path/to/the/file.java");
    buster.assert.same(global.files.length, 1);
    buster.assert.same(global.files[0].project, "dummyProject");
    buster.assert.same(global.files[0].path, "dummy/path/to/the/file.java");
    buster.assert.same(global.files[0].fileName, "file.java");

    buster.assert.isTrue(layoutFilesCalled);

    // Restore "layoutFilesCalled" method
    layoutFiles = tempLayoutFiles;
  }
});

buster.testCase("another test", {
  "layoutFiles() must be restored": function() {
    buster.assert.exception(layoutFiles);
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

    buster.assert.same(binarySearch(testArray, compareFuncGenerator(1)), 0);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(2)), 1);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(3)), 2);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(4)), 3);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(6)), 4);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(7)), 5);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(8)), 6);
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

    buster.assert.same(binarySearch(testArray, compareFuncGenerator(0)), -1);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(5)), -5);
    buster.assert.same(binarySearch(testArray, compareFuncGenerator(9)), -8);
  }
});
