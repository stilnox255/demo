import { appName } from "../../app.config.js";

const STORAGE_KEY = `${appName}-state`;

let storeInstance = null;

export function setStore(store) {
    storeInstance = store;
}

export function save() {
    if (!storeInstance) return;
    try {
        // `demoItems` is an ephemeral fetch cache, re-fetched on every mount. Persisting it
        // lets a stale slice shape survive a reducer refactor and silently break rehydration.
        const { notifications, demoItems, ...state } = storeInstance.getState();
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (error) {
        console.error("Failed to save state to localStorage:", error);
    }
}

export function load() {
    try {
        const serialized = localStorage.getItem(STORAGE_KEY);
        if (!serialized) return undefined;
        const parsed = JSON.parse(serialized);
        // Never rehydrate `demoItems` from storage — drop any legacy slice so the reducer's
        // own initial state (current shape) is used and the list is fetched fresh.
        delete parsed.demoItems;
        return parsed;
    } catch (error) {
        console.error("Failed to load state from localStorage:", error);
        return undefined;
    }
}
