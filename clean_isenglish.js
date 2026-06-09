const fs = require('fs');
let kt = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf-8');
kt = kt.replace(/    val isEnglish = language == MainViewModel\.Language\.ENGLISH\n/g, '');
fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', kt, 'utf-8');
console.log('done');
