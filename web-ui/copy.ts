import {
  copyFileSync,
  mkdirSync,
  readdirSync,
  rmSync,
  statSync,
} from "node:fs";
import { join, dirname } from "node:path";

const SOURCE_DIR = "./build/client";
const TARGET_DIR = "../web/src/main/resources/static";

function mkdirSafe(p: string) {
  try { mkdirSync(p, { recursive: true }); } catch (e: any) { if (e.code !== 'EEXIST') throw e; }
}

function copyDirectory(src: string, dest: string) {
  // 确保目标目录存在
  mkdirSafe(dest);

  const entries = readdirSync(src, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = join(src, entry.name);
    const destPath = join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDirectory(srcPath, destPath);
    } else {
      // 确保父目录存在
      mkdirSafe(dirname(destPath));
      copyFileSync(srcPath, destPath);
    }
  }
}

try {
  console.log("📦 Starting build output copy...");
  console.log(`   Source: ${SOURCE_DIR}`);
  console.log(`   Target: ${TARGET_DIR}`);

  // 检查源目录是否存在
  try {
    statSync(SOURCE_DIR);
  } catch {
    console.error(`❌ Source directory not found: ${SOURCE_DIR}`);
    console.error("   Please run build first.");
    process.exit(1);
  }

  // 清理目标目录（如果存在）
  try {
    rmSync(TARGET_DIR, { recursive: true, force: true });
    console.log("🧹 Cleaned target directory");
  } catch (err) {
    // 目标目录不存在，忽略错误
  }

  // 复制文件
  copyDirectory(SOURCE_DIR, TARGET_DIR);

  console.log("✅ Build output copied successfully!");
} catch (error) {
  console.error("❌ Copy failed:", error);
  process.exit(1);
}
