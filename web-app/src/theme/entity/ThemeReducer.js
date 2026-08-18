import { createReducer } from "@reduxjs/toolkit";
import { themeChangedAction } from "../control/ThemeControl.js";

const initialState = {
    theme: "auto"
};

export const theme = createReducer(initialState, (builder) => {
    builder.addCase(themeChangedAction, (state, { payload }) => {
        state.theme = payload;
    });
});
