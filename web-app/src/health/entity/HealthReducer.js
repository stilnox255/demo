import { createReducer } from "@reduxjs/toolkit";
import { healthLoadedAction, healthLoadingAction, healthErrorAction } from "../control/HealthControl.js";

const initialState = {
    status: "UNKNOWN",
    checks: [],
    loading: false,
    error: null,
    lastUpdate: null
};

export const health = createReducer(initialState, (builder) => {
    builder
        .addCase(healthLoadingAction, (state) => {
            state.loading = true;
            state.error = null;
        })
        .addCase(healthLoadedAction, (state, { payload }) => {
            state.status = payload.status;
            state.checks = payload.checks || [];
            state.loading = false;
            state.error = null;
            state.lastUpdate = Date.now();
        })
        .addCase(healthErrorAction, (state, { payload }) => {
            state.status = "DOWN";
            state.loading = false;
            state.error = payload;
        });
});
