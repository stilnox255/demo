import { render } from "lit-html";
import store from "./store.js";

export default class BElement extends HTMLElement {
    constructor() {
        super();
        this.state = null;
        this.unsubscribe = null;
        this.updateQueued = false;
    }

    connectedCallback() {
        this.unsubscribe = store.subscribe(() => this.requestViewUpdate());
        this.requestViewUpdate();
    }

    disconnectedCallback() {
        if (this.unsubscribe) {
            this.unsubscribe();
            this.unsubscribe = null;
        }
    }

    requestViewUpdate() {
        if (this.updateQueued) {
            return;
        }

        this.updateQueued = true;

        queueMicrotask(() => {
            this.updateQueued = false;

            if (this.isConnected) {
                this.triggerViewUpdate();
            }
        });
    }

    triggerViewUpdate() {
        if (!this.isConnected) {
            return;
        }

        const reduxState = store.getState();
        this.state = this.extractState(reduxState);
        const template = this.view();
        render(template, this.getRenderTarget());
    }

    extractState(reduxState) {
        return reduxState;
    }

    view() {
        throw new Error("view() must be implemented by subclass");
    }

    getRenderTarget() {
        return this;
    }
}
