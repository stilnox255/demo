import {createAction} from "@reduxjs/toolkit";
import {showActionToast, showErrorToast} from "../../notification/control/NotificationsControl.js";

let storeInstance = null;
let sessionExpiredToastShown = false;

export const setStore = (store) => {
    storeInstance = store;
};

export const authConfigLoadedAction = createAction("authConfigLoaded");
export const authConfigLoaded = (config) => {
    if (storeInstance) storeInstance.dispatch(authConfigLoadedAction(config));
};

export const oidcConfigLoadedAction = createAction("oidcConfigLoaded");
export const oidcConfigLoaded = (config) => {
    if (storeInstance) storeInstance.dispatch(oidcConfigLoadedAction(config));
};

export const loginSuccessAction = createAction("loginSuccess");
export const loginSuccess = ({ accessToken, refreshToken, idToken, userInfo }) => {
    localStorage.setItem("access_token", accessToken);
    localStorage.setItem("refresh_token", refreshToken);
    localStorage.setItem("id_token", idToken);
    localStorage.setItem("user_info", JSON.stringify(userInfo));

    if (storeInstance) storeInstance.dispatch(loginSuccessAction({ accessToken, refreshToken, idToken, userInfo }));
    scheduleTokenRefresh(accessToken);
};

export const logoutAction = createAction("logout");
export const logout = () => {
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
    localStorage.removeItem("id_token");
    localStorage.removeItem("user_info");

    cancelTokenRefresh();
    sessionExpiredToastShown = false;
    if (storeInstance) storeInstance.dispatch(logoutAction());
};

export const tokensRefreshedAction = createAction("tokensRefreshed");
export const tokensRefreshed = ({ accessToken, refreshToken, idToken }) => {
    localStorage.setItem("access_token", accessToken);
    if (refreshToken) {
        localStorage.setItem("refresh_token", refreshToken);
    }
    if (idToken) {
        localStorage.setItem("id_token", idToken);
    }

    if (storeInstance) storeInstance.dispatch(tokensRefreshedAction({ accessToken, refreshToken, idToken }));
    scheduleTokenRefresh(accessToken);
    sessionExpiredToastShown = false;
};

const REFRESH_LEAD_TIME_MS = 120_000;
const REFRESH_MIN_DELAY_MS = 30_000;
let scheduledTimer = null;

function scheduleTokenRefresh(accessToken) {
    cancelTokenRefresh();
    try {
        const b64 = accessToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        const padded = b64.padEnd(b64.length + (4 - b64.length % 4) % 4, "=");
        const payload = JSON.parse(atob(padded));
        const expMs = payload.exp * 1000;
        const delay = Math.max(REFRESH_MIN_DELAY_MS, expMs - Date.now() - REFRESH_LEAD_TIME_MS);
        scheduledTimer = setTimeout(() => { refreshTokens(); }, delay);
    } catch (err) {
        console.warn("scheduleTokenRefresh: cannot parse access token exp", err);
    }
}

function cancelTokenRefresh() {
    if (scheduledTimer) {
        clearTimeout(scheduledTimer);
        scheduledTimer = null;
    }
}

function sessionExpired() {
    if (sessionExpiredToastShown) return;
    sessionExpiredToastShown = true;
    cancelTokenRefresh();
    showActionToast({
        type: "error",
        title: "auth.sessionExpired.title",
        detail: "auth.sessionExpired.detail",
        action: { label: "auth.sessionExpired.action", type: "login" }
    });
}

export async function loadAppConfig() {
    try {
        const response = await fetch("/.well-known/app-config");
        const config = await response.json();
        authConfigLoaded(config);
        return config;
    } catch (err) {
        console.error("Failed to load app config:", err);
        const fallback = {
            authConfig: {
                clientId: "frontend-client",
                issuer: "http://localhost:8180/realms/starter"
            }
        };
        authConfigLoaded(fallback);
        return fallback;
    }
}

export async function loadOidcConfig(issuer) {
    try {
        const response = await fetch(`${issuer}/.well-known/openid-configuration`);
        const config = await response.json();
        oidcConfigLoaded(config);
        return config;
    } catch (err) {
        console.error("Failed to load OIDC config:", err);
        const fallback = {
            authorization_endpoint: `${issuer}/protocol/openid-connect/auth`,
            token_endpoint: `${issuer}/protocol/openid-connect/token`,
            end_session_endpoint: `${issuer}/protocol/openid-connect/logout`
        };
        oidcConfigLoaded(fallback);
        return fallback;
    }
}

export async function checkAuth() {
    const token = localStorage.getItem("access_token");
    if (!token) {
        logout();
        return false;
    }

    const userInfo = JSON.parse(localStorage.getItem("user_info") || "{}");
    loginSuccess({
        accessToken: token,
        refreshToken: localStorage.getItem("refresh_token"),
        idToken: localStorage.getItem("id_token"),
        userInfo
    });

    try {
        // Any authenticated endpoint will do — this call exists to find out whether
        // the stored token is still accepted, not to fetch anything.
        const response = await fetch("/api/demo-items?pageSize=1", {
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (response.status === 401) {
            const refreshed = await refreshTokens();
            if (!refreshed) {
                logout();
                return false;
            }
            return true;
        }
        // Any other status (2xx, 5xx, etc.) — trust the stored tokens. Backend
        // errors are not auth errors. authenticatedFetch will refresh on 401
        // when the user makes their next API call.
        return true;
    } catch {
        // Network error — assume the token is fine, let next call drive re-auth.
        return true;
    }
}

export async function generatePkce() {
    const verifier = btoa(String.fromCharCode(...crypto.getRandomValues(new Uint8Array(32))))
        .replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
    const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
    const challenge = btoa(String.fromCharCode(...new Uint8Array(digest)))
        .replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
    return { verifier, challenge };
}

export async function login() {
    if (!storeInstance) return;
    const state = storeInstance.getState();
    const { appConfig, oidcConfig } = state.auth;

    if (!oidcConfig || !appConfig) {
        console.error("Config not loaded yet");
        return;
    }

    const { verifier, challenge } = await generatePkce();
    sessionStorage.setItem("pkce_verifier", verifier);

    const params = new URLSearchParams({
        client_id: appConfig.authConfig.clientId,
        redirect_uri: globalThis.location.origin + "/callback.html",
        response_type: "code",
        scope: "openid profile email",
        code_challenge: challenge,
        code_challenge_method: "S256"
    });

    globalThis.location.href = `${oidcConfig.authorization_endpoint}?${params}`;
}

export function performLogout() {
    if (!storeInstance) return;
    const state = storeInstance.getState();
    const { appConfig, oidcConfig, idToken } = state.auth;

    if (!oidcConfig || !appConfig) {
        console.error("Config not loaded yet");
        return;
    }

    logout();

    const params = new URLSearchParams({
        client_id: appConfig.authConfig.clientId,
        post_logout_redirect_uri: globalThis.location.origin
    });

    if (idToken) {
        params.append("id_token_hint", idToken);
    }

    globalThis.location.href = `${oidcConfig.end_session_endpoint}?${params}`;
}

let inFlightRefresh = null;

export async function refreshTokens() {
    if (inFlightRefresh) return inFlightRefresh;
    inFlightRefresh = (async () => {
        try {
            return await doRefresh();
        } finally {
            inFlightRefresh = null;
        }
    })();
    return inFlightRefresh;
}

async function doRefresh() {
    if (!storeInstance) return false;
    const { oidcConfig, appConfig, refreshToken } = storeInstance.getState().auth;
    if (!refreshToken) return false;

    try {
        const response = await fetch(oidcConfig.token_endpoint, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({
                grant_type: "refresh_token",
                refresh_token: refreshToken,
                client_id: appConfig.authConfig.clientId
            })
        });
        if (!response.ok) return false;
        const tokens = await response.json();
        tokensRefreshed({
            accessToken: tokens.access_token,
            refreshToken: tokens.refresh_token,
            idToken: tokens.id_token
        });
        return true;
    } catch (error) {
        console.error("Failed to refresh token:", error);
        return false;
    }
}

async function handleErrorResponse(response, rethrow) {
    const contentType = response.headers.get("content-type") || "";
    let err;
    if (contentType.includes("application/problem+json")) {
        const problem = await response.json();
        const firstFieldError = Array.isArray(problem.errors) && problem.errors.length > 0 && problem.errors[0];
        // `problem.detail` / `problem.title` are the server's own wording and stay
        // as they arrive — the backend has no Accept-Language handling, so this is
        // the one user-visible text a locale switch cannot reach. Only our own
        // fallback is a catalogue key; ToastContainer resolves it and passes the
        // server's text through untouched.
        const detail = firstFieldError
            ? `${firstFieldError.field}: ${firstFieldError.message}`
            : (problem.detail || problem.title || "auth.requestFailed");
        err = new Error(detail);
        err.type = problem.type;
        err.title = problem.title;
        err.status = problem.status;
        err.detail = detail;
        err.errors = problem.errors;
    } else {
        err = new Error(`HTTP ${response.status}: ${response.statusText}`);
        err.title = `HTTP ${response.status}`;
        err.detail = response.statusText;
    }
    showErrorToast(err.title ?? "auth.requestFailed", err.detail ?? null);
    if (rethrow) throw err;
}

export const hasRole = (role) => {
    if (!storeInstance) return false;
    const { accessToken } = storeInstance.getState().auth;
    if (!accessToken) return false;
    try {
        const payload = JSON.parse(atob(accessToken.split(".")[1]));
        return payload.realm_access?.roles?.includes(role) || false;
    } catch {
        return false;
    }
};

export async function authenticatedFetch(url, options = {}, rethrow = false) {
    if (!storeInstance) throw new Error("Store not initialized");
    const state = storeInstance.getState();
    let { accessToken } = state.auth;

    if (!accessToken) {
        throw new Error("Not authenticated");
    }

    const authHeaders = { ...options.headers, "Authorization": `Bearer ${accessToken}` };
    const response = await fetch(url, { ...options, headers: authHeaders });

    if (response.status === 401) {
        const refreshed = await refreshTokens();
        if (!refreshed) {
            sessionExpired();
            throw new Error("Session expired");
        }

        const newState = storeInstance.getState();
        accessToken = newState.auth.accessToken;
        const retryHeaders = { ...options.headers, "Authorization": `Bearer ${accessToken}` };
        const retryResponse = await fetch(url, { ...options, headers: retryHeaders });
        if (!retryResponse.ok) {
            await handleErrorResponse(retryResponse, rethrow);
        }
        return retryResponse;
    }

    if (!response.ok) {
        await handleErrorResponse(response, rethrow);
    }

    return response;
}

const TOKEN_KEYS = new Set(["access_token", "refresh_token", "id_token"]);

if (typeof window !== "undefined") {
    window.addEventListener("storage", (e) => {
        // Note: e.key is null when localStorage.clear() runs in another tab.
        // We intentionally ignore that case — the codebase only ever uses
        // removeItem, so a null key here indicates third-party storage clearing
        // (e.g. devtools) that we should not interpret as a session change.
        if (!TOKEN_KEYS.has(e.key)) return;
        const accessToken = localStorage.getItem("access_token");
        const refreshToken = localStorage.getItem("refresh_token");
        const idToken = localStorage.getItem("id_token");

        if (!accessToken && !refreshToken) {
            logout();
            return;
        }
        if (accessToken) {
            // Safe — per HTML spec, identical-value setItem does not fire a storage
            // event in other tabs, and the storage event never fires in the tab that
            // did the write. No infinite loop possible.
            tokensRefreshed({ accessToken, refreshToken, idToken });
        }
    });
}

