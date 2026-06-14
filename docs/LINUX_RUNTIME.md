# Linux Runtime

EterUee can install and run app-private Linux rootfs environments through the bundled proot runtime.

## Supported Distributions

| ID | Name | Rootfs directory | Install helper |
| --- | --- | --- | --- |
| `arch` | Arch Linux | `files/linux/archlinux` | `files/linux/install-archlinux.sh` |
| `ubuntu` | Ubuntu 24.04 | `files/linux/ubuntu` | `files/linux/install-ubuntu.sh` |

Default distribution: `arch`.

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
files/linux/
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
- `rootfsPath`: app-private rootfs path.
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
{"distribution":"ubuntu","command":"cat /etc/os-release","workingDir":"/root","timeoutSeconds":60}
```

Runs through:

```text
proot -0 -r <rootfs> -b /dev -b /proc -b /sys -b <app external files>:/mnt/eteruee -w <workingDir> /usr/bin/env -i ...
```

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
{"action":"exec","distribution":"ubuntu","command":"uname -a && cat /etc/os-release","workingDir":"/root","timeout":60}
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

## Notes

- Rootfs files are app-private and are not release artifacts.
- Installing Ubuntu does not overwrite Arch.
- Installing Arch does not overwrite Ubuntu.
- The runtime is proot-based userland execution, not a privileged container.
- Network access is required for first-time rootfs/proot downloads.
