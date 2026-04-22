import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import type { AuthChangeEvent, Session, User } from "@supabase/supabase-js";
import { isSupabaseConfigured, supabase } from "../lib/supabase";

type AuthContextValue = {
  isConfigured: boolean;
  isLoading: boolean;
  isRecoveryMode: boolean;
  session: Session | null;
  user: User | null;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<string | null>;
  signOut: () => Promise<void>;
  requestPasswordReset: (email: string) => Promise<void>;
  updatePassword: (password: string) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function isRecoveryHash(hash: string): boolean {
  const normalized = hash.startsWith("#") ? hash.slice(1) : hash;
  const params = new URLSearchParams(normalized);
  return params.get("type") === "recovery";
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<Session | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(isSupabaseConfigured);
  const [isRecoveryMode, setIsRecoveryMode] = useState(
    typeof window !== "undefined" && isRecoveryHash(window.location.hash)
  );

  useEffect(() => {
    if (!supabase) {
      setIsLoading(false);
      setSession(null);
      setUser(null);
      return;
    }

    let isMounted = true;

    void supabase.auth
      .getSession()
      .then(({ data, error }) => {
        if (!isMounted) return;
        if (error) {
          setSession(null);
          setUser(null);
        } else {
          setSession(data.session ?? null);
          setUser(data.session?.user ?? null);
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    const { data } = supabase.auth.onAuthStateChange(
      (event: AuthChangeEvent, nextSession: Session | null) => {
        setSession(nextSession);
        setUser(nextSession?.user ?? null);
        if (event === "PASSWORD_RECOVERY") {
          setIsRecoveryMode(true);
        }
        if (event === "SIGNED_OUT") {
          setIsRecoveryMode(false);
        }
      }
    );

    return () => {
      isMounted = false;
      data.subscription.unsubscribe();
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isConfigured: isSupabaseConfigured,
      isLoading,
      isRecoveryMode,
      session,
      user,
      async signIn(email: string, password: string) {
        if (!supabase) throw new Error("Supabase auth is not configured.");
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
      },
      async signUp(email: string, password: string) {
        if (!supabase) throw new Error("Supabase auth is not configured.");
        const { data, error } = await supabase.auth.signUp({ email, password });
        if (error) throw error;
        return data.session == null ? "Check your email to confirm your account." : null;
      },
      async signOut() {
        if (!supabase) return;
        const { error } = await supabase.auth.signOut();
        if (error) throw error;
      },
      async requestPasswordReset(email: string) {
        if (!supabase) throw new Error("Supabase auth is not configured.");
        const { error } = await supabase.auth.resetPasswordForEmail(email, {
          redirectTo: `${window.location.origin}/`,
        });
        if (error) throw error;
      },
      async updatePassword(password: string) {
        if (!supabase) throw new Error("Supabase auth is not configured.");
        const { error } = await supabase.auth.updateUser({ password });
        if (error) throw error;
        setIsRecoveryMode(false);
        if (typeof window !== "undefined" && window.location.hash.length > 0) {
          window.history.replaceState({}, document.title, window.location.pathname + window.location.search);
        }
      },
    }),
    [isLoading, isRecoveryMode, session, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used within an AuthProvider.");
  }
  return value;
}
