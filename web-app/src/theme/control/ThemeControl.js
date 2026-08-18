import { createAction } from "@reduxjs/toolkit";

export const themeChangedAction = createAction("themeChanged");

let storeInstance = null;

export const setStore = (store) => {
    storeInstance = store;
};

export const themeChanged = (theme) => {
    localStorage.setItem("theme", theme);
    document.documentElement.setAttribute("data-theme", theme);
    if (storeInstance) {
        storeInstance.dispatch(themeChangedAction(theme));
    }
};

export const initTheme = () => {
    const savedTheme = localStorage.getItem("theme") || "auto";
    themeChanged(savedTheme);
};

export const toggleTheme = () => {
    if (!storeInstance) return;
    const state = storeInstance.getState();
    const current = state.theme.theme;
    const next = current === "light" ? "dark" : current === "dark" ? "auto" : "light";
    themeChanged(next);
};
