import { supabase } from "../lib/supabase";

export type AuthMode = "public" | "optional" | "required";
export type ApiError = {
  code: string;
  message: string;
  details: string[];
};

type FetchWithAuthOptions = RequestInit & {
  authMode?: AuthMode;
};

type ApiErrorPayload = {
  code?: unknown;
  message?: unknown;
  details?: unknown;
};

async function getAccessToken(authMode: AuthMode): Promise<string | null> {
  if (authMode === "public") {
    return null;
  }

  if (!supabase) {
    if (authMode === "required") {
      throw new Error("Sign in to continue.");
    }
    return null;
  }

  const { data, error } = await supabase.auth.getSession();
  if (error) {
    throw error;
  }

  const token = data.session?.access_token ?? null;
  if (authMode === "required" && !token) {
    throw new Error("Sign in to continue.");
  }
  return token;
}

export async function fetchWithAuth(input: string, options: FetchWithAuthOptions = {}) {
  const { authMode = "public", headers, ...rest } = options;
  const token = await getAccessToken(authMode);

  const makeRequest = (bearerToken: string | null) => {
    const nextHeaders = new Headers(headers);
    if (bearerToken) {
      nextHeaders.set("Authorization", `Bearer ${bearerToken}`);
    } else {
      nextHeaders.delete("Authorization");
    }

    return fetch(input, {
      ...rest,
      headers: nextHeaders,
    });
  };

  const response = await makeRequest(token);
  if (authMode === "optional" && token && (response.status === 401 || response.status === 403)) {
    return makeRequest(null);
  }

  return response;
}

export async function readJson<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

export async function toApiError(
  response: Response,
  fallbackMessage = `Request failed (${response.status})`
): Promise<ApiError> {
  try {
    const payload = (await response.json()) as ApiErrorPayload;
    return {
      code: typeof payload.code === "string" && payload.code.length > 0 ? payload.code : "request_failed",
      message:
        typeof payload.message === "string" && payload.message.length > 0
          ? payload.message
          : fallbackMessage,
      details: Array.isArray(payload.details)
        ? payload.details.flatMap((detail) => (typeof detail === "string" ? [detail] : []))
        : [],
    };
  } catch {
    return {
      code: "request_failed",
      message: fallbackMessage,
      details: [],
    };
  }
}

export async function readJsonOrThrowApiError<T>(
  response: Response,
  fallbackMessage = `Request failed (${response.status})`
): Promise<T> {
  if (!response.ok) {
    throw await toApiError(response, fallbackMessage);
  }
  return readJson<T>(response);
}

export async function readJsonOrThrowError<T>(
  response: Response,
  fallbackMessage = `Request failed (${response.status})`
): Promise<T> {
  if (!response.ok) {
    const apiError = await toApiError(response, fallbackMessage);
    throw new Error(apiError.message);
  }
  return readJson<T>(response);
}
