import { api } from "./axios";

export interface Notification {
  id: string;
  transactionId: string;
  channel: "SMS" | "EMAIL" | "PUSH";
  message: string;
  fraudScore?: number;
  sentAt: string;
}

export interface NotifStats {
  total: number;
  byChannel: Record<string, number>;
}

export const notifications = {
  list: () =>
    api.get<Notification[]>("/notif/notifications").then((r) => r.data),

  getByTxn: (txnId: string) =>
    api.get<Notification>(`/notif/notifications/${txnId}`).then((r) => r.data),

  stats: () =>
    api.get<NotifStats>("/notif/notifications/stats").then((r) => r.data),
};
