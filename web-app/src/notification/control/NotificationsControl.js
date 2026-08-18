import { createAction } from "@reduxjs/toolkit";

let storeInstance = null;

export const setStore = (store) => {
    storeInstance = store;
};

export const addToastAction = createAction("addToastAction");
export const removeToastAction = createAction("removeToastAction");

export const showSuccessToast = (title, detail) =>
    storeInstance?.dispatch(addToastAction({ id: Date.now(), type: "success", title, detail: detail ?? null }));

export const showErrorToast = (title, detail) =>
    storeInstance?.dispatch(addToastAction({ id: Date.now(), type: "error", title, detail: detail ?? null }));

export const showWarningToast = (title, detail) =>
    storeInstance?.dispatch(addToastAction({ id: Date.now(), type: "warning", title, detail: detail ?? null }));

export const dismissToast = (id) =>
    storeInstance?.dispatch(removeToastAction(id));

const actionHandlers = {};

export const registerToastActionHandler = (type, handler) => {
    if (actionHandlers[type]) {
        console.warn(`Toast action handler for type "${type}" is being overwritten`);
    }
    actionHandlers[type] = handler;
};

export const runToastAction = (type) => {
    actionHandlers[type]?.();
};

export const showActionToast = ({ type, title, detail, action }) =>
    storeInstance?.dispatch(addToastAction({
        id: Date.now(),
        type,
        title,
        detail: detail ?? null,
        action: action ?? null
    }));

// T-05 debug hook — exposed so the toast-action.spec.js Playwright spec can
// drive the action-toast path before a real consumer (T-06) lands. Deletable
// alongside that spec. Gated to attach only under (a) Vite DEV builds or
// (b) when running on localhost/127.0.0.1 — covers both the local dev server
// and the Playwright run against Quarkus dev (BASE_URL=http://localhost:8080).
// Deployed production (Quarkus on a real hostname) gets neither condition →
// hook is not installed.
if (typeof window !== "undefined") {
    const isDev = import.meta.env?.DEV;
    const isLocalhost = window.location?.hostname === "localhost" || window.location?.hostname === "127.0.0.1";
    if (isDev || isLocalhost) {
        window.__appTestHooks = {
            showActionToast,
            registerToastActionHandler,
            runToastAction
        };
    }
}
