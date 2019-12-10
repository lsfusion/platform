// Sample:
// var colList = ["item"];
// var rowList = ["name", "date", "time"];
// var data = [["name", "time", "date", "item", "value"],
//            ["Li", "12", "July 1", "TV", "1050"],
//            ...
//
// rendererOptions: {
//   rowSubtotalDisplay: {
//     disableList: buildDisableList(data, rowList)
//   },
//   colSubtotalDisplay: {
//     disableList: buildDisableList(data, colList)
//   }

function buildDisableList(data, groups) {
  var disableList = [];
  var groupIndex = buildGroupIndex(data, groups);
  for (var i = 0; i+1 < groups.length; ++i) {
    var unique = true;
    var dataMap = Object.create(null);
    for (var row = 1; row < data.length; ++row) {
      var rowLine = [];
      for (var k = 0; k < i+1; ++k) {
        rowLine.push(data[row][groupIndex[groups[k]]]);
      }
      var arrStr = concatStrings(rowLine);
      if ((arrStr in dataMap) && dataMap[arrStr] != data[row][groupIndex[groups[i+1]]]) {
        unique = false;
        break;
      }   
      dataMap[arrStr] = data[row][groupIndex[groups[i+1]]];   
    }
    if (unique) {
      disableList.push(i);
    }
  }
  return disableList;      
} 

function buildGroupIndex(data, groups) {
  var groupIndex = Object.create(null);
  header = data[0]
  for (var i = 0; i < header.length; ++i) {
    if (groups.includes(header[i])) {
      groupIndex[header[i]] = i;
    }
  }
  return groupIndex;
}

function concatStrings(strArray) {
  var result = "";
  for (var i = 0; i < strArray.length; ++i) {
    if (i > 0) {
      result += "@";    
    }
    result += strArray[i];
  }  
  return result;  
}  
