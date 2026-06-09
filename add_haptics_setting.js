const fs = require('fs');

function addString(file, key, value) {
    let content = fs.readFileSync(file, 'utf-8');
    content = content.replace('</resources>', `    <string name="${key}">${value}</string>\n</resources>`);
    fs.writeFileSync(file, content, 'utf-8');
}

addString('app/src/main/res/values/strings.xml', 'str_haptic_feedback', 'Haptic Feedback:');
addString('app/src/main/res/values/strings.xml', 'str_enabled', 'Enabled');
addString('app/src/main/res/values/strings.xml', 'str_disabled', 'Disabled');

addString('app/src/main/res/values-sw/strings.xml', 'str_haptic_feedback', 'Mtikisiko (Haptics):');
addString('app/src/main/res/values-sw/strings.xml', 'str_enabled', 'Imewashwa');
addString('app/src/main/res/values-sw/strings.xml', 'str_disabled', 'Imezimwa');

let kt = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf-8');
const search = `            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val soundTheme by viewModel.soundTheme.collectAsState()`;

const inject = `            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
                    Text(stringResource(R.string.str_haptic_feedback), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.setHapticsEnabled(true) }) {
                        RadioButton(
                            selected = hapticsEnabled,
                            onClick = { viewModel.setHapticsEnabled(true) }
                        )
                        Text(stringResource(R.string.str_enabled), fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.setHapticsEnabled(false) }) {
                        RadioButton(
                            selected = !hapticsEnabled,
                            onClick = { viewModel.setHapticsEnabled(false) }
                        )
                        Text(stringResource(R.string.str_disabled), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val soundTheme by viewModel.soundTheme.collectAsState()`;

kt = kt.replace(search, inject);
fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', kt, 'utf-8');
console.log('done');
