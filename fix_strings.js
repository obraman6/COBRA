const fs = require('fs');

let kt = fs.readFileSync('app/src/main/java/com/example/MainActivity.kt', 'utf-8');
let en = fs.readFileSync('app/src/main/res/values/strings.xml', 'utf-8');
let sw = fs.readFileSync('app/src/main/res/values-sw/strings.xml', 'utf-8');

// Replacements object. key: existing str_ key.
// Kotlin replacement: stringResource(...) -> stringResource(id, arg1, arg2...)
// EN replacement: string -> format string
// SW replacement: string -> format string

const fixes = [
    {
        key: "str_averagedurationmins_m___aver",
        kt: "stringResource(R.string.str_averagedurationmins_m___aver, averageDurationMins, averageDurationRemainderSecs)",
        en_orig: "${averageDurationMins}m ${averageDurationRemainderSecs}s",
        en: "%1$dm %2$ds",
        sw_orig: "Dk ${averageDurationMins} Sek ${averageDurationRemainderSecs}",
        sw: "Dk %1$d Sek %2$d"
    },
    {
        key: "str_matches_played___totalgames",
        kt: "stringResource(R.string.str_matches_played___totalgames, totalGames)",
        en_orig: "Matches Played: $totalGames",
        en: "Matches Played: %1$d",
        sw_orig: "Mechi Ulizocheza: $totalGames",
        sw: "Mechi Ulizocheza: %1$d"
    },
    {
        key: "str_wins___wins",
        kt: "stringResource(R.string.str_wins___wins, wins)",
        en_orig: "Wins: $wins",
        en: "Wins: %1$d",
        sw_orig: "Ushindi: $wins",
        sw: "Ushindi: %1$d"
    },
    {
        key: "str_losses___losses",
        kt: "stringResource(R.string.str_losses___losses, losses)",
        en_orig: "Losses: $losses",
        en: "Losses: %1$d",
        sw_orig: "Kushindwa: $losses",
        sw: "Kushindwa: %1$d"
    },
    {
        key: "str_won____wallet__wincoins____0",
        kt: "stringResource(R.string.str_won____wallet__wincoins____0, wallet?.winCoins ?: 0)",
        en_orig: "Won: ${wallet?.winCoins ?: 0}",
        en: "Won: %1$d",
        sw_orig: "Ushindi: ${wallet?.winCoins ?: 0}",
        sw: "Ushindi: %1$d"
    },
    {
        key: "str_lost____wallet__lostcoins____0",
        kt: "stringResource(R.string.str_lost____wallet__lostcoins____0, wallet?.lostCoins ?: 0)",
        en_orig: "Lost: ${wallet?.lostCoins ?: 0}",
        en: "Lost: %1$d",
        sw_orig: "Hasara: ${wallet?.lostCoins ?: 0}",
        sw: "Hasara: %1$d"
    },
    {
        key: "str_room_id____roomstate__roomid",
        kt: "stringResource(R.string.str_room_id____roomstate__roomid, roomState?.roomId ?: \"\")",
        en_orig: "Room ID: ${roomState?.roomId}",
        en: "Room ID: %1$s",
        sw_orig: "Chumba Namba: ${roomState?.roomId}",
        sw: "Chumba Namba: %1$s"
    },
    {
        key: "str_winner_is___mshindiname__ud83c",
        kt: "stringResource(R.string.str_winner_is___mshindiname__ud83c, mshindiName)",
        en_orig: "Winner is: $mshindiName \\uD83C\\uDF89",
        en: "Winner is: %1$s \uD83C\uDF89",
        sw_orig: "Mshindi ni: $mshindiName \\uD83C\\uDF89",
        sw: "Mshindi ni: %1$s \uD83C\uDF89"
    }
];

fixes.forEach(f => {
    // 1. replace in kt
    // Warning: stringResource(...) is a literal string if the original didn't match.
    // In our first script we literally replaced it with `stringResource(R.string.str_...)`
    let ktSearch = `stringResource(R.string.${f.key})`;
    kt = kt.split(ktSearch).join(f.kt);
    
    // 2. replace in EN
    let enSearch = `>${f.en_orig}<`;
    en = en.split(enSearch).join(`>${f.en}<`);

    // 3. replace in SW
    let swSearch = `>${f.sw_orig}<`;
    sw = sw.split(swSearch).join(`>${f.sw}<`);
});

fs.writeFileSync('app/src/main/java/com/example/MainActivity.kt', kt, 'utf-8');
fs.writeFileSync('app/src/main/res/values/strings.xml', en, 'utf-8');
fs.writeFileSync('app/src/main/res/values-sw/strings.xml', sw, 'utf-8');

console.log("DONE FIXES");
