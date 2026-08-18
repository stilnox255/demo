import { createAction } from "@reduxjs/toolkit";

let storeInstance = null;

export const setStore = (store) => {
    storeInstance = store;
};

export const healthLoadingAction = createAction("healthLoading");
export const healthLoadedAction = createAction("healthLoaded");
export const healthErrorAction = createAction("healthError");

export const loadHealth = async () => {
    if (!storeInstance) return;
    storeInstance.dispatch(healthLoadingAction());

    try {
        const response = await fetch("/q/health");
        const data = await response.json();
        storeInstance.dispatch(healthLoadedAction(data));
    } catch (error) {
        storeInstance.dispatch(healthErrorAction(error.message));
    }
};

export const startHealthPolling = (intervalMs = 30000) => {
    loadHealth();
    return setInterval(loadHealth, intervalMs);
};

export const stopHealthPolling = (intervalId) => {
    if (intervalId) {
        clearInterval(intervalId);
    }
};
