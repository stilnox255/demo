import { createReducer } from "@reduxjs/toolkit";
import {
    authConfigLoadedAction,
    oidcConfigLoadedAction,
    loginSuccessAction,
    logoutAction,
    tokensRefreshedAction
} from "../control/AuthControl.js";

const initialState = {
    isAuthenticated: false,
    accessToken: null,
    refreshToken: null,
    idToken: null,
    userInfo: null,
    appConfig: null,
    oidcConfig: null,
    isLoading: true
};

export const auth = createReducer(initialState, (builder) => {
    builder
        .addCase(authConfigLoadedAction, (state, { payload }) => {
            state.appConfig = payload;
        })
        .addCase(oidcConfigLoadedAction, (state, { payload }) => {
            state.oidcConfig = payload;
            state.isLoading = false;
        })
        .addCase(loginSuccessAction, (state, { payload }) => {
            state.isAuthenticated = true;
            state.accessToken = payload.accessToken;
            state.refreshToken = payload.refreshToken;
            state.idToken = payload.idToken;
            state.userInfo = payload.userInfo;
            state.isLoading = false;
        })
        .addCase(logoutAction, (state) => {
            state.isAuthenticated = false;
            state.accessToken = null;
            state.refreshToken = null;
            state.idToken = null;
            state.userInfo = null;
            state.isLoading = false;
        })
        .addCase(tokensRefreshedAction, (state, { payload }) => {
            state.accessToken = payload.accessToken;
            if (payload.refreshToken) {
                state.refreshToken = payload.refreshToken;
            }
            if (payload.idToken) {
                state.idToken = payload.idToken;
            }
        });
});
