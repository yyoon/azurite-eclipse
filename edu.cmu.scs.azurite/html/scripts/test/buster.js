var config = module.exports;

config["My tests"] = {
  rootPath: "../",
  environment: "browser",
  sources: [
    "azurite.js",
    "d3.v3.js",
    "jquery-1.8.2.js",
    "jquery.tipsy.js",
    "timeline.js"
  ],
  tests: [
    "test/*-test.js"
  ]
};
