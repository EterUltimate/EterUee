const fs = require('fs');
const { execSync } = require('child_process');
const path = require('path');

const cwd = 'C:/Users/zacza/Desktop/x/RikkaHub';

// Files that need restoring from git history (emoji/mojibake issues)
const filesToRestore = [
  {
    gitPath: 'app/src/main/java/me/rerere/rikkahub/ui/pages/setting/SettingAboutPage.kt',
    targetPath: 'app/src/main/java/com/eterultimate/eteruee/ui/pages/setting/SettingAboutPage.kt',
    commit: '7b5eba68'
  }
];

for (const file of filesToRestore) {
  console.log('Restoring: ' + file.targetPath);
  try {
    const content = execSync(`git show ${file.commit}:${file.gitPath}`, {
      cwd: cwd,
      encoding: 'utf8',
      maxBuffer: 10 * 1024 * 1024
    });
    
    // Apply package/brand replacements
    let fixed = content;
    fixed = fixed.split('me.rerere.rikkahub').join('com.eterultimate.eteruee');
    fixed = fixed.split('me.rerere.ai').join('com.eterultimate.eteruee.ai');
    fixed = fixed.split('me.rerere.common').join('com.eterultimate.eteruee.common');
    fixed = fixed.split('me.rerere.search').join('com.eterultimate.eteruee.search');
    fixed = fixed.split('me.rerere.tts').join('com.eterultimate.eteruee.tts');
    fixed = fixed.split('me.rerere.highlight').join('com.eterultimate.eteruee.highlight');
    fixed = fixed.split('me.rerere.document').join('com.eterultimate.eteruee.document');
    fixed = fixed.split('RikkaHub').join('EterUee');
    fixed = fixed.split('RIKKAHUB').join('ETERUEE');
    fixed = fixed.split('rikkahub').join('eteruee');
    
    const target = path.join(cwd, file.targetPath);
    fs.writeFileSync(target, fixed, 'utf8');
    console.log('  OK: ' + file.targetPath);
  } catch (e) {
    console.error('  FAILED: ' + e.message);
  }
}

console.log('Done');
