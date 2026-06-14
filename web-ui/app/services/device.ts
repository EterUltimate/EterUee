import api from "~/services/api";

export interface DeviceAgentStatus {
  shizukuInstalled: boolean;
  shizukuRunning: boolean;
  shizukuVersion?: number | null;
  serverUid?: number | null;
  serverMode?: string | null;
  wirelessDebuggingReady: boolean;
  setupHint?: string | null;
  permissionGranted: boolean;
  permissionRationale: boolean;
  canRunAdbShell: boolean;
  message: string;
}

export interface DeviceHardwareInfo {
  manufacturer: string;
  brand: string;
  model: string;
  device: string;
  product: string;
  board: string;
  hardware: string;
  supportedAbis: string[];
  cameraIds: string[];
  screenWidthPx: number;
  screenHeightPx: number;
  density: number;
  memoryTotalBytes: number;
  memoryAvailableBytes: number;
  storageTotalBytes: number;
  storageAvailableBytes: number;
}

export interface DeviceSoftwareInfo {
  androidRelease: string;
  sdkInt: number;
  securityPatch: string;
  incremental: string;
  appPackage: string;
  appVersionName?: string | null;
  appVersionCode?: number | null;
  networkType: string;
  batteryPercent?: number | null;
  batteryCharging?: boolean | null;
}

export interface DeviceInfo {
  hardware: DeviceHardwareInfo;
  software: DeviceSoftwareInfo;
  shizuku: DeviceAgentStatus;
}

export interface DeviceAppInfo {
  packageName: string;
  label: string;
  versionName?: string | null;
  versionCode: number;
  system: boolean;
  enabled: boolean;
}

export interface DeviceShellResult {
  stdout: string;
  stderr: string;
  exitCode: number;
  executor: string;
  shell: string;
  workingDir: string;
  command: string;
  serverUid?: number | null;
  serverMode?: string | null;
}

export interface LinuxEnvironmentStatus {
  distribution: string;
  distributionName: string;
  supportedDistributions: string[];
  installed: boolean;
  rootfsPath: string;
  termuxUsrPath: string;
  prootPath: string;
  prootExecutable: boolean;
  installScriptPath: string;
  rootfsArchivePath?: string | null;
  primaryAbi: string;
  supportedRootfsUrl?: string | null;
  termuxPackageUrls: string[];
  runner?: string | null;
  canExecuteLinux: boolean;
  message: string;
}

export interface LinuxShellResult {
  stdout: string;
  stderr: string;
  exitCode: number;
  executor: string;
  shell: string;
  workingDir: string;
  command: string;
  distribution: string;
  distributionName: string;
  rootfsPath: string;
  prootPath: string;
  fallback: boolean;
}

export interface DeviceShellRequest {
  command: string;
  distribution?: string | null;
  workingDir?: string | null;
  stdin?: string | null;
  timeoutSeconds?: number;
}

export function getDeviceStatus(): Promise<DeviceAgentStatus> {
  return api.get<DeviceAgentStatus>("device/status");
}

export function requestShizukuPermission(): Promise<DeviceAgentStatus> {
  return api.post<DeviceAgentStatus>("device/shizuku/request-permission");
}

export function getDeviceInfo(): Promise<DeviceInfo> {
  return api.get<DeviceInfo>("device/info");
}

export function getInstalledApps(includeSystem = false, limit = 250): Promise<DeviceAppInfo[]> {
  const query = new URLSearchParams({
    includeSystem: String(includeSystem),
    limit: String(limit),
  });
  return api.get<DeviceAppInfo[]>(`device/apps?${query.toString()}`);
}

export function executeDeviceShell(request: DeviceShellRequest): Promise<DeviceShellResult> {
  return api.post<DeviceShellResult>("device/shell", request);
}

export function getLinuxStatus(distribution = "arch"): Promise<LinuxEnvironmentStatus> {
  const query = new URLSearchParams({ distribution });
  return api.get<LinuxEnvironmentStatus>(`device/linux/status?${query.toString()}`);
}

export function prepareLinuxEnvironment(distribution = "arch"): Promise<LinuxEnvironmentStatus> {
  return api.post<LinuxEnvironmentStatus>("device/linux/prepare", { distribution });
}

export function installLinuxEnvironment(distribution = "arch", timeoutSeconds = 600): Promise<LinuxShellResult> {
  return api.post<LinuxShellResult>("device/linux/install", { distribution, timeoutSeconds });
}

export function executeLinuxShell(request: DeviceShellRequest): Promise<LinuxShellResult> {
  return api.post<LinuxShellResult>("device/linux/shell", request);
}
