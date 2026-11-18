'use client';

import { createContext, useContext } from 'react';

export type Role = 'ADMIN' | 'TEACHER' | 'ACCOUNTANT';
export interface User { id: number; email: string; fullName: string; role: Role }

export const AuthContext = createContext<User | null>(null);

export function useUser(): User {
  const u = useContext(AuthContext);
  if (!u) throw new Error('useUser outside AuthContext');
  return u;
}

export const isAdmin = (u: User) => u.role === 'ADMIN';
export const canSeeFinance = (u: User) => u.role === 'ADMIN' || u.role === 'ACCOUNTANT';
