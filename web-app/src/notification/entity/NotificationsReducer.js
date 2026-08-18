import { createReducer } from "@reduxjs/toolkit";
import { addToastAction, removeToastAction } from "../control/NotificationsControl.js";

const initialState = { toasts: [] };

export const notifications = createReducer(initialState, (builder) => {
    builder
        .addCase(addToastAction, (state, { payload }) => {
            state.toasts.push(payload);
        })
        .addCase(removeToastAction, (state, { payload }) => {
            state.toasts = state.toasts.filter(t => t.id !== payload);
        });
});
