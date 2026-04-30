import { api } from "./axios";

export interface Notification {
  txnId: string;
  paymentStatus: string;
  channel: string;
  subject: string;
  message: string;
  notifStatus: string;
  createdAt: string;
}

export interface NotifStats {
  total: number;
  byChannel: Record<string, number>;
  byPaymentStatus: Record<string, number>;
}

export const notifications = {
  list: () =>
    api.get<Notification[]>("/notif/notifications").then((r) => r.data),

  getByTxn: (txnId: string) =>
    api.get<Notification[]>(`/notif/notifications/${txnId}`).then((r) => r.data),

  stats: () =>
    api.get<NotifStats>("/notif/notifications/stats").then((r) => r.data),
};
