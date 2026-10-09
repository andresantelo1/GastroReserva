import { useSyncExternalStore } from 'react'
import { getSession, subscribeSession } from '../features/auth/session'
export function useSession() { return useSyncExternalStore(subscribeSession, getSession, getSession) }

