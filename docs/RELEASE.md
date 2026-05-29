# 发布指南

本文档记录 EterUee 当前非 Apple 发布流程。发布操作应在 `main` 工作树执行，而不是在 `dev` 工作树直接打 tag。

## 发布线

当前发布工作树：

```text
C:\Users\zacza\Desktop\x\EterUee-main-merge
```

当前开发工作树：

```text
C:\Users\zacza\Desktop\x\EterUee
```

发布前必须确认：

```bash
git worktree list --porcelain
git -C C:\Users\zacza\Desktop\x\EterUee-main-merge status --short --branch
git -C C:\Users\zacza\Desktop\x\EterUee-main-merge log --oneline -5
```

## 发布产物

非 Apple release 当前应包含：

| Asset | 来源 | 说明 |
| --- | --- | --- |
| `EterUee-<version>.exe` | GitHub Actions | Windows desktop package |
| `eteruee_<version>_amd64.deb` | GitHub Actions | Linux desktop package |
| `app-arm64-v8a-release.apk` | Android release build | arm64-v8a split APK |
| `app-x86_64-release.apk` | Android release build | x86_64 split APK |
| `app-universal-release.apk` | Android release build | universal APK |
| `app-release.aab` | Android release build | Play/AppBundle asset |
| `desktop-release-manifest-windows.txt` | GitHub Actions | Windows package metadata |
| `desktop-release-manifest-linux.txt` | GitHub Actions | Linux package metadata |
| `SHA256SUMS.txt` | release verification | checksums for release assets |

## 本地 Android 构建

在 release 工作树中执行：

```bash
./gradlew.bat :app:assembleRelease --no-daemon --console=plain
./gradlew.bat :app:bundleRelease --no-daemon --console=plain
```

输出路径：

```text
app/build/outputs/apk/release/
app/build/outputs/bundle/release/
```

如果 Gradle 生成了无关 build churn，尤其是 `roleplay/build/`，发布验证后应清理生成产物，但不要删除已确认需要上传的 APK/AAB。

## APK 验证

使用 Android SDK build-tools：

```bash
C:\Users\zacza\AppData\Local\Android\Sdk\build-tools\37.0.0\apksigner.bat verify --verbose --print-certs app\build\outputs\apk\release\app-arm64-v8a-release.apk
C:\Users\zacza\AppData\Local\Android\Sdk\build-tools\37.0.0\aapt.exe dump badging app\build\outputs\apk\release\app-arm64-v8a-release.apk
```

对三个 APK 都要检查：

- 签名验证通过。
- `applicationId` 为 `com.eterultimate.eteruee`。
- `versionName` 与 tag 对应。
- `versionCode` 与构建配置对应。
- ABI split 与文件名对应。

## GitHub Release

Tag 命名：

```text
v<version>
```

发布后检查：

```bash
gh release view v<version> --repo EterUltimate/EterUee --json tagName,name,url,targetCommitish,isDraft,isPrerelease,assets
```

要求：

- `isDraft=false`
- `isPrerelease=false`，除非 tag 明确是 alpha/beta/rc
- release URL 指向 `https://github.com/EterUltimate/EterUee/releases/tag/v<version>`
- 资产列表包含非 Apple 全量资产

## Desktop Packages

Windows `.exe` 和 Linux `.deb` 以 GitHub Actions 为准。历史上本地 Windows WiX 打包不稳定，因此本地失败不应反复重试；应让 CI 产出桌面包，再核对 release assets。

需要检查：

```bash
gh run list --repo EterUltimate/EterUee --branch main --limit 10
gh run view <run-id> --repo EterUltimate/EterUee
```

## Android 手动上传兜底

如果 GitHub Actions 因 signing secrets 缺失而没有上传 Android APK/AAB，可以在本地验证后手动上传：

```bash
gh release upload v<version> app/build/outputs/apk/release/app-arm64-v8a-release.apk --repo EterUltimate/EterUee --clobber
gh release upload v<version> app/build/outputs/apk/release/app-x86_64-release.apk --repo EterUltimate/EterUee --clobber
gh release upload v<version> app/build/outputs/apk/release/app-universal-release.apk --repo EterUltimate/EterUee --clobber
gh release upload v<version> app/build/outputs/bundle/release/app-release.aab --repo EterUltimate/EterUee --clobber
```

上传后重新读取 release assets，确认 digest 和 size。

## Checksum

`SHA256SUMS.txt` 应覆盖所有非 checksum release assets。生成后上传：

```bash
gh release upload v<version> SHA256SUMS.txt --repo EterUltimate/EterUee --clobber
```

发布完成后再次执行：

```bash
gh release view v<version> --repo EterUltimate/EterUee --json assets
```

## CI And Static Analysis

发布或发布后修复提交必须验证：

```bash
gh run list --repo EterUltimate/EterUee --branch main --limit 10 --json databaseId,workflowName,displayTitle,headSha,status,conclusion,url
```

当前发布线使用的健康标准：

- `CI` 通过。
- `Static Analysis` 通过。
- CodeQL Java/Kotlin 通过。
- CodeQL JavaScript/TypeScript 通过。
- OSSF Scorecard 可以生成并上传 SARIF；公开发布 Scorecard 结果已关闭以避免 Fulcio/OIDC 签名发布失败。

## 发布后更新

发布后需要同步：

- 根 README 的当前状态。
- [PROJECT_STATUS.md](./PROJECT_STATUS.md) 的版本/状态。
- 必要时同步 `dev` 与 `main` 的 workflow 修复。
- 应用内 update checker 保持指向 GitHub Releases。

## 常见问题

### `gh release view` 找不到 release

发布 workflow 可能尚未完成。先用 `gh run watch <run-id> --exit-status` 等待 release job 完成。

### Android assets 缺失

通常是 signing secrets 不完整或上传步骤跳过。用本地已验证 APK/AAB 手动 `gh release upload --clobber`。

### `git push` 断线但重试显示 Everything up-to-date

先 `git fetch origin main`，再比较 `HEAD` 和 `origin/main`。如果 SHA 一致，第一次 push 可能已经被远端接收。

### Static Analysis 只有 OSSF Scorecard 失败

查看失败日志。如果扫描完成但失败在 Fulcio/OIDC 发布结果签名，保持 `publish_results: false` 并上传 SARIF 即可。
