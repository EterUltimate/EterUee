import api from "~/services/api";
import type { BackupRestoreResponse, BackupStatusDto } from "~/types";

export function getBackupStatus(): Promise<BackupStatusDto> {
  return api.get<BackupStatusDto>("backup/status");
}

export function exportBackup(): Promise<Blob> {
  return api.getBlob("backup/export");
}

export function restoreBackup(file: File): Promise<BackupRestoreResponse> {
  const formData = new FormData();
  formData.append("file", file);
  return api.postMultipart<BackupRestoreResponse>("backup/restore", formData);
}
