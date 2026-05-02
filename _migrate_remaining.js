// Migrate remaining test/benchmark files from me.rerere to com.eterultimate.eteruee
// Handles: package declarations, imports, string literals, AND directory moves

const fs = require('fs');
const path = require('path');

const ROOT = 'C:\\Users\\zacza\\Desktop\\x\\RikkaHub';

// Module-specific package mappings
const MODULE_PACKAGES = {
  'ai': { old: 'me.rerere.ai', new: 'com.eterultimate.eteruee.ai' },
  'common': { old: 'me.rerere.common', new: 'com.eterultimate.eteruee.common' },
  'document': { old: 'me.rerere.document', new: 'com.eterultimate.eteruee.document' },
  'highlight': { old: 'me.rerere.highlight', new: 'com.eterultimate.eteruee.highlight' },
  'search': { old: 'me.rerere.search', new: 'com.eterultimate.eteruee.search' },
  'tts': { old: 'me.rerere.tts', new: 'com.eterultimate.eteruee.tts' },
  'baselineprofile': { old: 'me.rerere.baselineprofile', new: 'com.eterultimate.eteruee.baselineprofile' },
};

// Cross-module import replacements
const CROSS_MODULE_REPLACEMENTS = [
  { from: 'me.rerere.ai.', to: 'com.eterultimate.eteruee.ai.' },
  { from: 'me.rerere.common.', to: 'com.eterultimate.eteruee.common.' },
  { from: 'me.rerere.highlight.', to: 'com.eterultimate.eteruee.highlight.' },
  { from: 'me.rerere.tts.', to: 'com.eterultimate.eteruee.tts.' },
  { from: 'me.rerere.search.', to: 'com.eterultimate.eteruee.search.' },
  { from: 'me.rerere.document.', to: 'com.eterultimate.eteruee.document.' },
  { from: 'me.rerere.baselineprofile.', to: 'com.eterultimate.eteruee.baselineprofile.' },
];

// Also replace string references
const STRING_REPLACEMENTS = [
  { from: '"me.rerere.ai.', to: '"com.eterultimate.eteruee.ai.' },
  { from: '"me.rerere.common.', to: '"com.eterultimate.eteruee.common.' },
  { from: '"me.rerere.highlight.', to: '"com.eterultimate.eteruee.highlight.' },
  { from: '"me.rerere.tts.', to: '"com.eterultimate.eteruee.tts.' },
  { from: '"me.rerere.search.', to: '"com.eterultimate.eteruee.search.' },
  { from: '"me.rerere.document.', to: '"com.eterultimate.eteruee.document.' },
  { from: '"me.rerere.baselineprofile.', to: '"com.eterultimate.eteruee.baselineprofile.' },
];

// Directories with old package structure
const OLD_DIRS = [];
const NEW_BASE = path.join(ROOT, 'com', 'eterultimate', 'eteruee');

// Collect all files in old me/rerere directories
function findOldFiles() {
  const results = [];
  const modules = ['ai', 'common', 'document', 'highlight', 'search', 'tts'];
  
  for (const mod of modules) {
    for (const srcType of ['test', 'androidTest']) {
      const oldDir = path.join(ROOT, mod, 'src', srcType, 'java', 'me', 'rerere');
      if (fs.existsSync(oldDir)) {
        walkDir(oldDir, results, mod, srcType);
      }
    }
  }
  
  // baselineprofile
  const bpDir = path.join(ROOT, 'app', 'baselineprofile', 'src', 'main', 'java', 'me', 'rerere');
  if (fs.existsSync(bpDir)) {
    walkDir(bpDir, results, 'baselineprofile', 'main');
  }
  
  return results;
}

function walkDir(dir, results, mod, srcType) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkDir(fullPath, results, mod, srcType);
    } else if (entry.name.endsWith('.kt') || entry.name.endsWith('.java')) {
      // Get relative path from me/rerere/
      const baseDir = path.join(ROOT, 
        mod === 'baselineprofile' 
          ? path.join('app', 'baselineprofile', 'src', srcType, 'java', 'me', 'rerere')
          : path.join(mod, 'src', srcType, 'java', 'me', 'rerere')
      );
      const relPath = path.relative(baseDir, fullPath);
      results.push({ fullPath, relPath, mod, srcType });
    }
  }
}

// Replace content in a file
function replaceContent(content, mod) {
  let result = content;
  
  // Module-specific package declaration
  const modPkg = MODULE_PACKAGES[mod];
  if (modPkg) {
    // Replace package declarations (me.rerere.xxx -> com.eterultimate.eteruee.xxx)
    const regex = new RegExp(`package ${modPkg.old.replace(/\./g, '\\.')}`, 'g');
    result = result.replace(regex, `package ${modPkg.new}`);
  }
  
  // Cross-module imports
  for (const rep of CROSS_MODULE_REPLACEMENTS) {
    const regex = new RegExp(rep.from.replace(/\./g, '\\.'), 'g');
    result = result.replace(regex, rep.to);
  }
  
  // String literal replacements
  for (const rep of STRING_REPLACEMENTS) {
    result = result.split(rep.from).join(rep.to);
  }
  
  // Generic me.rerere.rikkahub -> com.eterultimate.eteruee
  result = result.replace(/me\.rerere\.rikkahub/g, 'com.eterultimate.eteruee');
  
  return result;
}

// Main
const files = findOldFiles();
console.log(`Found ${files.length} files in old me/rerere directories`);

let processed = 0;
let moved = 0;

for (const file of files) {
  // Read and replace content
  const content = fs.readFileSync(file.fullPath, 'utf-8');
  const newContent = replaceContent(content, file.mod);
  
  if (content !== newContent) {
    fs.writeFileSync(file.fullPath, newContent, 'utf-8');
    processed++;
    console.log(`Updated content: ${file.relPath} (${file.mod})`);
  }
  
  // Determine new path
  let basePath;
  if (file.mod === 'baselineprofile') {
    basePath = path.join(ROOT, 'app', 'baselineprofile', 'src', file.srcType, 'java');
  } else {
    basePath = path.join(ROOT, file.mod, 'src', file.srcType, 'java');
  }
  
  const newPath = path.join(basePath, 'com', 'eterultimate', 'eteruee', file.relPath);
  const newDir = path.dirname(newPath);
  
  // Create new directory structure
  if (!fs.existsSync(newDir)) {
    fs.mkdirSync(newDir, { recursive: true });
  }
  
  // Move file
  if (file.fullPath !== newPath) {
    fs.copyFileSync(file.fullPath, newPath);
    fs.unlinkSync(file.fullPath);
    moved++;
    console.log(`Moved: ${file.relPath} -> com/eterultimate/eteruee/${file.relPath}`);
  }
}

// Also update all files in the new com/eterultimate/eteruee directories that still have me.rerere references
console.log('\n--- Scanning all .kt/.java files for remaining me.rerere references ---');
let extraFixed = 0;

function scanAndFix(dir) {
  if (!fs.existsSync(dir)) return;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory() && entry.name !== 'build') {
      scanAndFix(fullPath);
    } else if (entry.name.endsWith('.kt') || entry.name.endsWith('.java')) {
      const content = fs.readFileSync(fullPath, 'utf-8');
      if (content.includes('me.rerere.') && !content.includes('me.rerere.hugeicons')) {
        const newContent = replaceContent(content, '');
        if (content !== newContent) {
          fs.writeFileSync(fullPath, newContent, 'utf-8');
          extraFixed++;
          console.log(`Fixed references: ${path.relative(ROOT, fullPath)}`);
        }
      }
    }
  }
}

const modules = ['ai', 'common', 'document', 'highlight', 'search', 'tts'];
for (const mod of modules) {
  scanAndFix(path.join(ROOT, mod, 'src'));
}
scanAndFix(path.join(ROOT, 'app', 'baselineprofile', 'src'));
scanAndFix(path.join(ROOT, 'app', 'src'));

console.log(`\nSummary:`);
console.log(`  Content updated: ${processed}`);
console.log(`  Files moved: ${moved}`);
console.log(`  Extra references fixed: ${extraFixed}`);
