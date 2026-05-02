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
      } else if (entry.isFile() && /\.(kt|java|xml|kts|toml)$/.test(entry.name)) {
        results.push(fullPath);
      }
    }
  } catch (e) {}
  return results;
}

// Package prefix replacements (me.rerere.X -> com.eterultimate.eteruee.X)
// EXCLUDE: me.rerere.hugeicons (third-party library, import must stay)
const packageReplacements = [
  // Longer prefixes first
  { old: 'me.rerere.rikkahub', new: 'com.eterultimate.eteruee' },
  { old: 'me.rerere.baselineprofile', new: 'com.eterultimate.eteruee.baselineprofile' },
  { old: 'me.rerere.document', new: 'com.eterultimate.eteruee.document' },
  { old: 'me.rerere.highlight', new: 'com.eterultimate.eteruee.highlight' },
  { old: 'me.rerere.common', new: 'com.eterultimate.eteruee.common' },
  { old: 'me.rerere.search', new: 'com.eterultimate.eteruee.search' },
  { old: 'me.rerere.tts', new: 'com.eterultimate.eteruee.tts' },
  { old: 'me.rerere.ai', new: 'com.eterultimate.eteruee.ai' },
];

const files = walk(cwd);
let changedFiles = 0;
let allChanges = [];

for (const file of files) {
  const rel = path.relative(cwd, file).replace(/\\/g, '/');
  if (rel.includes('_scan_refs') || rel.includes('_fix_imports')) continue;
  
  let content;
  try {
    content = fs.readFileSync(file, 'utf8');
  } catch (e) { continue; }
  
  let original = content;
  
  // Apply package prefix replacements
  // But skip me.rerere.hugeicons lines
  let lines = content.split('\n');
  let newLines = lines.map(line => {
    // Skip hugeicons imports - they're from a third-party library
    if (line.includes('me.rerere.hugeicons')) return line;
    
    let result = line;
    for (const rep of packageReplacements) {
      result = result.split(rep.old).join(rep.new);
    }
    return result;
  });
  content = newLines.join('\n');
  
  // Apply brand name replacements in string contexts
  if (file.endsWith('.xml') || file.endsWith('.kt') || file.endsWith('.java')) {
    content = content.split('RikkaHub').join('EterUee');
    content = content.split('RIKKAHUB').join('ETERUEE');
  }
  
  if (content !== original) {
    fs.writeFileSync(file, content, 'utf8');
    changedFiles++;
    console.log('FIXED: ' + rel);
  }
}

console.log('\nTotal files modified: ' + changedFiles);
