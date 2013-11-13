// timeline tests
var testGlobal = {};

buster.testCase("Buster Test", {
  "this should pass if buster is properly set": function() {
    buster.assert.isTrue(true);
  }
});

buster.testCase("addFile Test", {
  "addFile must add an item to the global files array": function() {
    testGlobal.layoutFilesCalled = false;
    layoutFiles = function() {
      testGlobal.layoutFilesCalled = true;
    };

    addFile("dummyProject", "dummyFile");
    buster.assert.same(global.files.length, 1);
    buster.assert.isTrue(testGlobal.layoutFilesCalled);
  }
});
