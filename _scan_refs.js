const fs = require('fs');
const path = require('path');
const cwd = 'C:/Users/zacza/Desktop/x/RikkaHub';

function walk(dir) {
  let results = [];
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory() && entry.name !== 'build') {
        results = results.concat(walk(fullPath));
      } else if (entry.isFile() && (entry.name.endsWith('.kt') || entry.name.endsWith('.java') || entry.name.endsWith('.xml'))) {
        results.push(fullPath);
      }
    }
  } catch (e) {}
  return results;
}

const modules = ['ai', 'app', 'common', 'search', 'tts', 'highlight'];
let hits = [];
let totalFiles = 0;

for (const mod of modules) {
  const base = path.join(cwd, mod, 'src');
  if (!fs.existsSync(base)) continue;
  const files = walk(base);
  totalFiles += files.length;
  for (const file of files) {
    try {
      const content = fs.readFileSync(file, 'utf8');
      const lines = content.split('\n');
      lines.forEach((line, idx) => {
        if (line.includes('me.rerere') || line.includes('rikkahub') || line.includes('RikkaHub') || line.includes('RIKKAHUB')) {
          const rel = path.relative(cwd, file).replace(/\\/g, '/');
          hits.push(rel + ':' + (idx + 1) + ': ' + line.trim());
        }
      });
    } catch (e) {}
  }
}

console.log('Total .kt/.java/.xml files scanned: ' + totalFiles);
console.log('Lines with old references: ' + hits.length);
console.log('---');
hits.forEach(h => console.log(h));
