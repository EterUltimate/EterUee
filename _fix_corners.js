const fs = require('fs');
const path = require('path');

function walkDir(dir, ext, results) {
  if (!results) results = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walkDir(full, ext, results);
    else if (entry.name.endsWith(ext)) results.push(full);
  }
  return results;
}

const uiDir = path.join(__dirname, 'app/src/main/java/com/eterultimate/eteruee/ui');
const files = walkDir(uiDir, '.kt');

let totalChanges = 0;
const changedFiles = [];

for (const file of files) {
  // Skip Shape.kt - already has 0.dp
  if (file.endsWith('Shape.kt')) continue;
  
  let content = fs.readFileSync(file, 'utf8');
  let original = content;
  
  // Replace RoundedCornerShape(N.dp) where N > 0 -> 0.dp
  content = content.replace(/RoundedCornerShape\((\d+)\.dp\)/g, function(match, num) {
    if (parseInt(num) === 0) return match;
    return 'RoundedCornerShape(0.dp)';
  });
  
  // Replace RoundedCornerShape(N) where N is pure number > 0 -> 0
  content = content.replace(/RoundedCornerShape\((\d+)\)(?!\s*\.)/g, function(match, num) {
    if (parseInt(num) === 0) return match;
    return 'RoundedCornerShape(0)';
  });
  
  // Replace partial corner params like topStart = 16.dp -> 0.dp
  content = content.replace(/topStart\s*=\s*\d+\.dp/g, 'topStart = 0.dp');
  content = content.replace(/topEnd\s*=\s*\d+\.dp/g, 'topEnd = 0.dp');
  content = content.replace(/bottomStart\s*=\s*\d+\.dp/g, 'bottomStart = 0.dp');
  content = content.replace(/bottomEnd\s*=\s*\d+\.dp/g, 'bottomEnd = 0.dp');
  
  // CardGroup corner constants
  content = content.replace(/private val CardGroupCorner = \d+\.dp/, 'private val CardGroupCorner = 0.dp');
  content = content.replace(/private val CardGroupInnerCorner = \d+\.dp/, 'private val CardGroupInnerCorner = 0.dp');
  
  if (content !== original) {
    const origLines = original.split('\n');
    const newLines = content.split('\n');
    let changes = 0;
    for (let i = 0; i < origLines.length; i++) {
      if (origLines[i] !== newLines[i]) changes++;
    }
    totalChanges += changes;
    const rel = path.relative(__dirname, file);
    changedFiles.push(rel + ' (' + changes + ' lines)');
    fs.writeFileSync(file, content, 'utf8');
  }
}

console.log('Total files changed: ' + changedFiles.length);
console.log('Total lines changed: ' + totalChanges);
console.log('');
changedFiles.forEach(function(f) { console.log(f); });
