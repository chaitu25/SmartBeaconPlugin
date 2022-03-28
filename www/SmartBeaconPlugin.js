var exec = require('cordova/exec');

exports.scan = function (success, error) {
    var options = {};
    console.log('plugin java script')
    exec(success, error, 'SmartBeaconPlugin', 'scan', [options]);
};
