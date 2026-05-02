const fs = require('fs');
const path = require('path');
const cwd = 'C:/Users/zacza/Desktop/x/RikkaHub';

function walk(dir) {
  let results = [];
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory() && entry.name !== 'build' && entry.name !== '.gradle' && entry.name !== '.git') {
        results = results.concat(walk(fullPath));
      } else if (entry.isFile() && entry.name.endsWith('.kt')) {
        results.push(fullPath);
      }
    }
  } catch (e) {}
  return results;
}

// Detect mojibake patterns: 馃 followed by various chars is corrupted emoji
const mojibakePattern = /馃[帀専挮帄コ巿巻巼Ж寛Ё巵崿嵀崏崜崚崓キ惐惗惣惎惖А挍挌挋挏嚚審實寧ぉ槅樅樃ぁ挕敟挜殌寵]/;
// Also detect 鉁? and 猸? patterns
const badPattern = /[鉁猸]\?/;

const modules = ['ai', 'app', 'common', 'search', 'tts', 'highlight'];
let corrupted = [];

for (const mod of modules) {
  const base = path.join(cwd, mod, 'src');
  if (!fs.existsSync(base)) continue;
  const files = walk(base);
  for (const file of files) {
    try {
      const content = fs.readFileSync(file, 'utf8');
      if (mojibakePattern.test(content) || badPattern.test(content)) {
        corrupted.push(path.relative(cwd, file).replace(/\\/g, '/'));
      }
    } catch (e) {}
  }
}

console.log('Corrupted files with mojibake emoji: ' + corrupted.length);
corrupted.forEach(f => console.log(f));
