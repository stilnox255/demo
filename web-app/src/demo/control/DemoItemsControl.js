import { createAction } from "@reduxjs/toolkit";
import { authenticatedFetch } from "../../auth/control/AuthControl.js";
import { showSuccessToast } from "../../notification/control/NotificationsControl.js";

let storeInstance = null;

export const setStore = (store) => {
    storeInstance = store;
};

export const demoItemsLoadingAction = createAction("demoItemsLoading");
export const demoItemsLoadedAction = createAction("demoItemsLoaded");
export const demoItemsErrorAction = createAction("demoItemsError");

/**
 * Every dispatch in this feature happens here, not in a view (ADR-28). A
 * component that dispatches directly ties its markup to the store's shape, and
 * the next component that needs the same data reimplements the call.
 *
 * Error toasts are not raised here either: `authenticatedFetch` already surfaces
 * a failed response, so a toast per call site would double up (ADR-31). Success
 * is the opposite — it is only worth announcing where the user did something, so
 * it is explicit.
 *
 * Toast titles are catalogue keys, not text: `ToastContainer` resolves them at
 * render time so an open toast follows a locale switch.
 */
export const loadDemoItems = async (page = 1, pageSize = 25) => {
    if (!storeInstance) return;
    storeInstance.dispatch(demoItemsLoadingAction());

    const params = new URLSearchParams({ page, pageSize });
    const response = await authenticatedFetch(`/api/demo-items?${params}`);
    if (response?.ok) {
        storeInstance.dispatch(demoItemsLoadedAction(await response.json()));
    } else {
        storeInstance.dispatch(demoItemsErrorAction());
    }
};

export const createDemoItem = async (name, description) => {
    if (!storeInstance) return false;
    try {
        await authenticatedFetch("/api/demo-items", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, description }),
        }, true);
        showSuccessToast("demo.toast.created", name);
        await loadDemoItems();
        return true;
    } catch {
        storeInstance.dispatch(demoItemsErrorAction());
        return false;
    }
};

/**
 * The version the client last read travels with the update, and the server
 * answers 409 if it is stale. Sending it is the client's half of the contract —
 * without it the last write silently wins.
 */
export const updateDemoItemStatus = async (item, status) => {
    if (!storeInstance) return false;
    try {
        await authenticatedFetch(`/api/demo-items/${item.id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name: item.name,
                description: item.description,
                status,
                expectedVersion: item.version,
            }),
        }, true);
        showSuccessToast("demo.toast.updated", item.name);
        await loadDemoItems(currentPage());
        return true;
    } catch {
        // A 409 lands here too. Reloading is the correct response: the caller now
        // sees the winning version and can decide again.
        await loadDemoItems(currentPage());
        return false;
    }
};

export const deleteDemoItem = async (item) => {
    if (!storeInstance) return false;
    try {
        await authenticatedFetch(`/api/demo-items/${item.id}`, { method: "DELETE" }, true);
        showSuccessToast("demo.toast.deleted", item.name);
        await loadDemoItems(currentPage());
        return true;
    } catch {
        return false;
    }
};

export const attachFileToDemoItem = async (item, file) => {
    if (!storeInstance) return false;
    const form = new FormData();
    form.append("file", file);
    try {
        await authenticatedFetch(`/api/demo-items/${item.id}/attachment`, { method: "POST", body: form }, true);
        showSuccessToast("demo.toast.attached", file.name);
        await loadDemoItems(currentPage());
        return true;
    } catch {
        return false;
    }
};

const currentPage = () => storeInstance?.getState().demoItems.meta.page ?? 1;
