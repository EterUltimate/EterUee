import * as React from "react";

import {
  ArrowLeft,
  Boxes,
  Cpu,
  HardDrive,
  Play,
  RefreshCw,
  ShieldCheck,
  ShieldOff,
  Smartphone,
  Terminal,
  Wifi,
} from "lucide-react";
import { Link } from "react-router";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "~/components/ui/card";
import { Checkbox } from "~/components/ui/checkbox";
import { Input } from "~/components/ui/input";
import { ScrollArea } from "~/components/ui/scroll-area";
import { Textarea } from "~/components/ui/textarea";
import {
  executeDeviceShell,
  getDeviceInfo,
  getDeviceStatus,
  getInstalledApps,
  requestShizukuPermission,
  type DeviceAgentStatus,
  type DeviceAppInfo,
  type DeviceInfo,
  type DeviceShellResult,
} from "~/services/device";
import { cn } from "~/lib/utils";

export function meta() {
  return [{ title: "Device Agent - EterUee" }];
}

function formatBytes(value: number | null | undefined): string {
  if (!value || value <= 0) return "-";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let size = value;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit += 1;
  }
  return `${size.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

function StatusBadge({ ready }: { ready: boolean }) {
  return (
    <Badge variant={ready ? "default" : "secondary"} className="gap-1.5">
      {ready ? <ShieldCheck className="size-3" /> : <ShieldOff className="size-3" />}
      {ready ? "Ready" : "Limited"}
    </Badge>
  );
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="grid grid-cols-[8rem_minmax(0,1fr)] gap-3 text-sm">
      <div className="text-muted-foreground">{label}</div>
      <div className="min-w-0 break-words text-foreground">{value}</div>
    </div>
  );
}

export default function AgentRoute() {
  const [status, setStatus] = React.useState<DeviceAgentStatus | null>(null);
  const [info, setInfo] = React.useState<DeviceInfo | null>(null);
  const [apps, setApps] = React.useState<DeviceAppInfo[]>([]);
  const [includeSystemApps, setIncludeSystemApps] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [appsLoading, setAppsLoading] = React.useState(false);
  const [permissionLoading, setPermissionLoading] = React.useState(false);
  const [shellRunning, setShellRunning] = React.useState(false);
  const [command, setCommand] = React.useState("id && getprop ro.build.version.release");
  const [workingDir, setWorkingDir] = React.useState("/data/local/tmp");
  const [shellHistory, setShellHistory] = React.useState<DeviceShellResult[]>([]);

  const refresh = React.useCallback(async () => {
    setLoading(true);
    try {
      const [nextStatus, nextInfo] = await Promise.all([getDeviceStatus(), getDeviceInfo()]);
      setStatus(nextStatus);
      setInfo(nextInfo);
    } catch (error) {
      console.error("Device refresh failed", error);
      toast.error(error instanceof Error ? error.message : "Failed to refresh device state");
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshApps = React.useCallback(async () => {
    setAppsLoading(true);
    try {
      setApps(await getInstalledApps(includeSystemApps, 300));
    } catch (error) {
      console.error("Apps refresh failed", error);
      toast.error(error instanceof Error ? error.message : "Failed to load apps");
    } finally {
      setAppsLoading(false);
    }
  }, [includeSystemApps]);

  React.useEffect(() => {
    void refresh();
  }, [refresh]);

  React.useEffect(() => {
    void refreshApps();
  }, [refreshApps]);

  const requestPermission = React.useCallback(async () => {
    setPermissionLoading(true);
    try {
      const nextStatus = await requestShizukuPermission();
      setStatus(nextStatus);
      toast.info(nextStatus.message);
    } catch (error) {
      console.error("Shizuku permission request failed", error);
      toast.error(error instanceof Error ? error.message : "Failed to request Shizuku permission");
    } finally {
      setPermissionLoading(false);
    }
  }, []);

  const runShell = React.useCallback(async () => {
    const trimmed = command.trim();
    if (!trimmed) return;

    setShellRunning(true);
    try {
      const result = await executeDeviceShell({
        command: trimmed,
        workingDir: workingDir.trim() || undefined,
        timeoutSeconds: 60,
      });
      setShellHistory((previous) => [result, ...previous].slice(0, 20));
    } catch (error) {
      console.error("Device shell failed", error);
      toast.error(error instanceof Error ? error.message : "Command failed");
    } finally {
      setShellRunning(false);
    }
  }, [command, workingDir]);

  const ready = status?.canRunAdbShell === true;

  return (
    <main className="min-h-[100dvh] bg-background text-foreground">
      <div className="mx-auto flex min-h-[100dvh] w-full max-w-7xl flex-col gap-5 px-4 py-4 sm:px-6">
        <header className="flex flex-wrap items-center gap-3 border-b pb-4">
          <Button variant="ghost" size="icon-sm" asChild>
            <Link to="/" aria-label="Back to chat" title="Back to chat">
              <ArrowLeft className="size-4" />
            </Link>
          </Button>
          <div className="min-w-0 flex-1">
            <h1 className="text-xl font-semibold tracking-normal">Device Agent</h1>
            <p className="text-sm text-muted-foreground">
              WebUI access to device software, hardware, apps, and Shizuku ADB shell.
            </p>
          </div>
          {status && <StatusBadge ready={ready} />}
          <Button type="button" variant="outline" onClick={() => void refresh()} disabled={loading}>
            <RefreshCw className={cn("size-4", loading && "animate-spin")} />
            Refresh
          </Button>
        </header>

        <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_26rem]">
          <div className="grid gap-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between gap-3">
                <CardTitle className="flex items-center gap-2 text-base">
                  <ShieldCheck className="size-4" />
                  Shizuku
                </CardTitle>
                <Button
                  type="button"
                  size="sm"
                  onClick={() => void requestPermission()}
                  disabled={permissionLoading || status?.shizukuRunning !== true || ready}
                >
                  Request Permission
                </Button>
              </CardHeader>
              <CardContent className="grid gap-3">
                <InfoRow label="Installed" value={status?.shizukuInstalled ? "Yes" : "No"} />
                <InfoRow label="Running" value={status?.shizukuRunning ? "Yes" : "No"} />
                <InfoRow
                  label="Permission"
                  value={status?.permissionGranted ? "Granted" : "Not granted"}
                />
                <InfoRow label="Server" value={status?.serverMode ?? "-"} />
                <InfoRow label="Version" value={status?.shizukuVersion ?? "-"} />
                <div className="rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">
                  {status?.message ?? "Loading..."}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Terminal className="size-4" />
                  ADB Shell
                </CardTitle>
              </CardHeader>
              <CardContent className="grid gap-3">
                <Input
                  value={workingDir}
                  onChange={(event) => setWorkingDir(event.target.value)}
                  placeholder="/data/local/tmp"
                  autoCapitalize="off"
                  autoCorrect="off"
                  spellCheck={false}
                />
                <div className="rounded-md border bg-[#101418] p-3 text-[#e7ecef]">
                  <div className="mb-2 flex items-center gap-2 text-xs text-[#8bd5a7]">
                    <span>shell@device:{workingDir || "/data/local/tmp"}$</span>
                  </div>
                  <Textarea
                    value={command}
                    onChange={(event) => setCommand(event.target.value)}
                    className="min-h-28 border-0 bg-transparent p-0 font-mono text-sm text-[#e7ecef] shadow-none focus-visible:ring-0"
                    autoCapitalize="off"
                    autoCorrect="off"
                    spellCheck={false}
                    onKeyDown={(event) => {
                      if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
                        event.preventDefault();
                        void runShell();
                      }
                    }}
                  />
                </div>
                <div className="flex items-center justify-between gap-3">
                  <div className="text-xs text-muted-foreground">
                    Requires Shizuku running through wireless or USB debugging.
                  </div>
                  <Button
                    type="button"
                    onClick={() => void runShell()}
                    disabled={!ready || shellRunning}
                  >
                    <Play className="size-4" />
                    Run
                  </Button>
                </div>

                <div className="space-y-3">
                  {shellHistory.map((result, index) => (
                    <div
                      key={`${result.command}-${index}`}
                      className="rounded-md border bg-muted/40"
                    >
                      <div className="flex flex-wrap items-center gap-2 border-b px-3 py-2 text-xs">
                        <Badge variant={result.exitCode === 0 ? "default" : "destructive"}>
                          exit {result.exitCode}
                        </Badge>
                        <span className="min-w-0 flex-1 truncate font-mono">{result.command}</span>
                        <span className="text-muted-foreground">{result.serverMode}</span>
                      </div>
                      <pre className="max-h-72 overflow-auto whitespace-pre-wrap p-3 text-xs">
                        {result.stdout || result.stderr || "(no output)"}
                        {result.stdout && result.stderr ? "\n" : ""}
                        {result.stderr ? `stderr:\n${result.stderr}` : ""}
                      </pre>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="grid content-start gap-4">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Smartphone className="size-4" />
                  Software
                </CardTitle>
              </CardHeader>
              <CardContent className="grid gap-3">
                <InfoRow label="Android" value={info?.software.androidRelease ?? "-"} />
                <InfoRow label="SDK" value={info?.software.sdkInt ?? "-"} />
                <InfoRow label="Patch" value={info?.software.securityPatch ?? "-"} />
                <InfoRow
                  label="Network"
                  value={
                    <span className="inline-flex items-center gap-1">
                      <Wifi className="size-3" /> {info?.software.networkType ?? "-"}
                    </span>
                  }
                />
                <InfoRow
                  label="Battery"
                  value={
                    info?.software.batteryPercent != null
                      ? `${info.software.batteryPercent}%${info.software.batteryCharging ? " charging" : ""}`
                      : "-"
                  }
                />
                <InfoRow
                  label="App"
                  value={`${info?.software.appPackage ?? "-"} ${info?.software.appVersionName ?? ""}`}
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Cpu className="size-4" />
                  Hardware
                </CardTitle>
              </CardHeader>
              <CardContent className="grid gap-3">
                <InfoRow
                  label="Model"
                  value={`${info?.hardware.manufacturer ?? "-"} ${info?.hardware.model ?? ""}`}
                />
                <InfoRow label="Device" value={info?.hardware.device ?? "-"} />
                <InfoRow label="Board" value={info?.hardware.board ?? "-"} />
                <InfoRow label="ABI" value={info?.hardware.supportedAbis.join(", ") ?? "-"} />
                <InfoRow
                  label="Screen"
                  value={
                    info
                      ? `${info.hardware.screenWidthPx} x ${info.hardware.screenHeightPx} @ ${info.hardware.density}`
                      : "-"
                  }
                />
                <InfoRow
                  label="Memory"
                  value={
                    info
                      ? `${formatBytes(info.hardware.memoryAvailableBytes)} / ${formatBytes(info.hardware.memoryTotalBytes)}`
                      : "-"
                  }
                />
                <InfoRow
                  label="Storage"
                  value={
                    info
                      ? `${formatBytes(info.hardware.storageAvailableBytes)} / ${formatBytes(info.hardware.storageTotalBytes)}`
                      : "-"
                  }
                />
                <InfoRow label="Cameras" value={info?.hardware.cameraIds.join(", ") || "-"} />
              </CardContent>
            </Card>
          </div>
        </section>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between gap-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Boxes className="size-4" />
              Installed Apps
            </CardTitle>
            <label className="flex items-center gap-2 text-sm text-muted-foreground">
              <Checkbox
                checked={includeSystemApps}
                onCheckedChange={(checked) => setIncludeSystemApps(checked === true)}
              />
              Include system
            </label>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-[24rem]">
              <div className="divide-y rounded-md border">
                {apps.map((app) => (
                  <div key={app.packageName} className="flex items-center gap-3 px-3 py-2 text-sm">
                    <HardDrive className="size-4 text-muted-foreground" />
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-medium">{app.label}</div>
                      <div className="truncate text-xs text-muted-foreground">
                        {app.packageName}
                      </div>
                    </div>
                    <Badge variant={app.system ? "secondary" : "outline"}>
                      {app.system ? "system" : "user"}
                    </Badge>
                    <span className="hidden text-xs text-muted-foreground sm:inline">
                      {app.versionName ?? app.versionCode}
                    </span>
                  </div>
                ))}
                {apps.length === 0 && (
                  <div className="px-3 py-10 text-center text-sm text-muted-foreground">
                    {appsLoading ? "Loading apps..." : "No apps found"}
                  </div>
                )}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
