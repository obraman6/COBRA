const fs = require('fs');

let content = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf-8');

const regex = /if\s*\(\s*isEnglish\s*\)\s*"([^"]+)"\s*else\s*"([^"]+)"/g;

let matches = [];
let match;

let enStrings = {};
let swStrings = {};

let counter = 1;
// First pass, gather all strings and replace with R.string
let newContent = content.replace(regex, (fullMatch, enText, swText) => {
    // create a key based on english text, alphanumeric only
    let keyStr = enText.toLowerCase().replace(/[^a-z0-9]/g, '_').substring(0, 30);
    // remove leading/trailing underscores
    keyStr = keyStr.replace(/^_+|_+$/g, '');
    if (!keyStr) keyStr = "text_" + counter;
    
    let key = "str_" + keyStr;
    // ensure unique key
    while (enStrings[key] && enStrings[key] !== enText) {
        key = "str_" + keyStr + "_" + (++counter);
    }
    
    enStrings[key] = enText;
    swStrings[key] = swText;
    
    return `stringResource(R.string.${key})`;
});

// We need to import stringResource if it's not imported
if (!newContent.includes('import androidx.compose.ui.res.stringResource')) {
    newContent = newContent.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.res.stringResource');
}

fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', newContent, 'utf-8');

let enXml = `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">Chess Pro TZ</string>\n`;
for (let k in enStrings) {
    let escaped = enStrings[k].replace(/'/g, "\\'").replace(/&/g, "&amp;");
    enXml += `    <string name="${k}">${escaped}</string>\n`;
}
enXml += `</resources>\n`;

let swXml = `<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">Chess Pro TZ</string>\n`;
for (let k in swStrings) {
    let escaped = swStrings[k].replace(/'/g, "\\'").replace(/&/g, "&amp;");
    swXml += `    <string name="${k}">${escaped}</string>\n`;
}
swXml += `</resources>\n`;

fs.writeFileSync('app/src/main/res/values/strings.xml', enXml, 'utf-8');

if (!fs.existsSync('app/src/main/res/values-sw')) {
    fs.mkdirSync('app/src/main/res/values-sw', { recursive: true });
}
fs.writeFileSync('app/src/main/res/values-sw/strings.xml', swXml, 'utf-8');

console.log('DONE');
