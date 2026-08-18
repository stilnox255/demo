import { createReducer } from "@reduxjs/toolkit";
import {
    demoItemsLoadingAction,
    demoItemsLoadedAction,
    demoItemsErrorAction,
} from "../control/DemoItemsControl.js";

const initialState = {
    items: [],
    meta: { page: 1, pageSize: 25, totalItems: 0, totalPages: 0 },
    isLoading: false,
    hasError: false,
};

/**
 * Slice for the demo resource. Holds server state only — no derived values, no
 * copies of what the DOM already knows.
 *
 * The reducer never fetches: the control layer owns side effects and dispatches
 * the result (ADR-28). That split is what keeps this file a pure function of
 * (state, action).
 */
export const demoItems = createReducer(initialState, (builder) => {
    builder
        .addCase(demoItemsLoadingAction, (state) => {
            state.isLoading = true;
            state.hasError = false;
        })
        .addCase(demoItemsLoadedAction, (state, action) => {
            state.items = action.payload.items;
            state.meta = action.payload.meta;
            state.isLoading = false;
            state.hasError = false;
        })
        .addCase(demoItemsErrorAction, (state) => {
            state.isLoading = false;
            state.hasError = true;
            // Deliberately keeps the previous items: a failed refresh should leave
            // the last good list on screen rather than blanking the page.
        });
});
