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
