import { createReducer } from "@reduxjs/toolkit";
import { localeChangedAction } from "../control/I18nControl.js";

// `null`, not the fallback locale: it has to be possible to tell a persisted
// choice apart from never having chosen, or `initI18n()` can never reach browser
// detection. See the comment on `detectLocale`.
const initialState = {
    locale: null
};

export const i18n = createReducer(initialState, (builder) => {
    builder.addCase(localeChangedAction, (state, { payload }) => {
        state.locale = payload;
    });
});
