# Linux Runtime

EterUee can install and run app-private Linux rootfs environments through the bundled proot runtime.
The default execution model follows the RikkaHub 2.3.0 workspace/sandbox shape:

```text
files/workspace/default/
  files/                  # default host shell cwd
  linux/                  # proot runtime, downloads, and rootfs images
  tmp/                    # shell/proot temp files
```

Host shell commands default to `files/workspace/default/files`. Linux commands bind that directory to
`/workspace` and use `/workspace` as the default working directory.

## Supported Distributions

| ID | Name | Rootfs directory | Install helper |
| --- | --- | --- | --- |
| `arch` | Arch Linux | Default sandbox runtime | `files/workspace/default/linux/archlinux` |
| `ubuntu` | Ubuntu 24.04 | Optional `ubuntu-proot` plugin | `files/workspace/default/linux/ubuntu` |

Default distribution: `arch`. Ubuntu is not the default sandbox runtime; install it explicitly as
the `ubuntu-proot` plugin.

Ubuntu rootfs sources use Ubuntu Base 24.04.4 from:

```text
https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/
```

ABI mapping:

| Android ABI | Ubuntu archive | Arch archive |
| --- | --- | --- |
| `arm64-v8a` | `ubuntu-base-24.04.4-base-arm64.tar.gz` | `ArchLinuxARM-aarch64-latest.tar.gz` |
| `armeabi-v7a` | `ubuntu-base-24.04.4-base-armhf.tar.gz` | `ArchLinuxARM-armv7-latest.tar.gz` |
| `x86_64` | `ubuntu-base-24.04.4-base-amd64.tar.gz` | `archlinux-bootstrap-x86_64.tar.zst` |

## Runtime Layout

```text
files/workspace/default/
  files/                  # mounted as /workspace inside proot
  tmp/                    # PROOT_TMP_DIR / TMPDIR
  linux/
  downloads/
  usr/                    # shared Termux proot runtime
  archlinux/              # Arch rootfs
  ubuntu/                 # Ubuntu rootfs
  install-archlinux.sh
  install-ubuntu.sh
```

`usr/` is shared by all distributions and is populated from Termux packages:

- `proot`
- `libandroid-shmem`
- `libtalloc`

Each distribution keeps its own rootfs and status.

## Web API

All Linux endpoints are under `/api/device/linux`.

### Status

```http
GET /api/device/linux/status?distribution=ubuntu
```

Response: `LinuxEnvironmentStatus`.

Important fields:

- `distribution`: `arch` or `ubuntu`.
- `distributionName`: display name.
- `supportedDistributions`: currently `["arch", "ubuntu"]`.
- `sandboxRoot`: app-private workspace sandbox root.
- `workspacePath`: host workspace files directory.
- `workspaceMountPath`: always `/workspace` inside proot.
- `rootfsPath`: app-private rootfs path under the sandbox.
- `prootPath`: shared proot binary path.
- `canExecuteLinux`: command execution readiness.

### Prepare

```http
POST /api/device/linux/prepare
{"distribution":"ubuntu"}
```

Writes the selected install helper script.

### Install

```http
POST /api/device/linux/install
{"distribution":"ubuntu","timeoutSeconds":600}
```

Downloads the shared proot runtime if needed, downloads the selected rootfs if needed, extracts the rootfs, then verifies `/etc/os-release`.

### Shell

```http
POST /api/device/linux/shell
{"distribution":"ubuntu","command":"cat /etc/os-release","workingDir":"/workspace","timeoutSeconds":60}
```

Runs through:

```text
proot -0 -r <rootfs> -b /dev -b /proc -b /sys -b <workspace files>:/workspace -w <workingDir> /usr/bin/env -i ...
```

When `workingDir` is omitted, EterUee uses `/workspace`.

## Local Tool API

`linux_environment` accepts:

```json
{"action":"status","distribution":"ubuntu"}
```

```json
{"action":"prepare","distribution":"ubuntu"}
```

```json
{"action":"install","distribution":"ubuntu","timeout":600}
```

```json
{"action":"exec","distribution":"ubuntu","command":"uname -a && cat /etc/os-release","workingDir":"/workspace","timeout":60}
```

`shell_execute` accepts:

```json
{"environment":"linux","distribution":"ubuntu","command":"python3 --version || true"}
```

`environment` values:

- `auto`: use selected Linux distribution when ready, otherwise Android shell.
- `linux`: require selected Linux distribution; returns Linux status if unavailable.
- `android`: force app-local Android shell.

## Web UI

Device Agent -> Linux Shell provides:

- Distribution selector: Arch Linux / Ubuntu 24.04.
- Status rows for ABI, runner, rootfs, and proot.
- Prepare, install, and run command actions.
- Separate command history entries showing executor and exit code.

## Plugin Capabilities

Plugin gateway capabilities support `distribution`:

- `linux.status`
- `linux.prepare`
- `linux.install`
- `linux.shell`

Example:

```json
{"id":"linux.shell","input":{"distribution":"ubuntu","command":"cat /etc/os-release"}}
```

Ubuntu can also be installed through the plugin installer surface:

```json
{"id":"plugin.install","input":{"id":"ubuntu-proot","timeout":600}}
```

## Notes

- Rootfs files are app-private and are not release artifacts.
- Installing Ubuntu does not overwrite Arch and is treated as an optional plugin/runtime.
- Installing Arch does not overwrite Ubuntu.
- Shell execution is workspace-first; commands must use `/workspace` for shared user files inside proot.
- The runtime is proot-based userland execution, not a privileged container.
- Network access is required for first-time rootfs/proot downloads.
