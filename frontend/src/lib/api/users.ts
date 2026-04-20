import { api } from "./axios";

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
  iban?: string;
  pan?: string;
}

export interface User {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  iban?: string;
  status: string;
  createdAt: string;
}

export const users = {
  create: (body: CreateUserRequest) =>
    api.post<User>("/users/users", body).then((r) => r.data),

  getById: (id: string) =>
    api.get<User>(`/users/users/${id}`).then((r) => r.data),

  getByEmail: (email: string) =>
    api.get<User>(`/users/users?email=${encodeURIComponent(email)}`).then((r) => r.data),

  update: (id: string, body: Partial<CreateUserRequest>) =>
    api.put<User>(`/users/users/${id}`, body).then((r) => r.data),

  changePassword: (id: string, oldPassword: string, newPassword: string) =>
    api.post(`/users/users/${id}/change-password`, { oldPassword, newPassword }).then((r) => r.data),
};
