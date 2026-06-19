import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserProfile } from '@/types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserProfile | null
  isAuthenticated: boolean

  setSession: (accessToken: string, refreshToken: string, user: UserProfile) => void
  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: UserProfile | null) => void
  logout: () => void
  hasPermission: (permission: string) => boolean
  hasRole: (role: string) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,

      setSession: (accessToken, refreshToken, user) =>
        set({ accessToken, refreshToken, user, isAuthenticated: true }),

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken }),

      setUser: (user) =>
        set({ user }),

      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false }),

      // Check permission by decoding token claims (roles/permissions are in the JWT)
      hasPermission: (permission: string) => {
        const token = get().accessToken
        if (!token) return false
        try {
          const payload = JSON.parse(atob(token.split('.')[1]))
          const perms: string[] = payload.permissions ?? []
          return perms.includes('*') || perms.includes(permission)
        } catch {
          return false
        }
      },

      hasRole: (role: string) => {
        const user = get().user
        return user?.role === role
      },
    }),
    {
      name: 'sh-auth',
      // Only persist tokens, never the user object (re-fetched from /me on load)
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)
