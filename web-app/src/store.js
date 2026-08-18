import { configureStore } from "@reduxjs/toolkit";
import { load, save, setStore as setStorageStore } from "./localstorage/control/StorageControl.js";
import { auth } from "./auth/entity/AuthReducer.js";
import { theme } from "./theme/entity/ThemeReducer.js";
import { health } from "./health/entity/HealthReducer.js";
import { notifications } from "./notification/entity/NotificationsReducer.js";
import { demoItems } from "./demo/entity/DemoItemsReducer.js";
import { setStore as setThemeStore } from "./theme/control/ThemeControl.js";
import { setStore as setAuthStore } from "./auth/control/AuthControl.js";
import { setStore as setHealthStore } from "./health/control/HealthControl.js";
import { setStore as setNotificationsStore } from "./notification/control/NotificationsControl.js";
import { setStore as setDemoItemsStore } from "./demo/control/DemoItemsControl.js";

const reducer = { auth, theme, health, notifications, demoItems };
const preloadedState = load();
const config = preloadedState ? { reducer, preloadedState } : { reducer };
const store = configureStore(config);

// Control modules receive the store rather than importing it: they are imported by
// the reducers' own feature modules, and importing back would close the cycle.
setAuthStore(store);
setThemeStore(store);
setHealthStore(store);
setNotificationsStore(store);
setDemoItemsStore(store);
setStorageStore(store);

store.subscribe(() => save());

export default store;
