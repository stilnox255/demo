import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setStore, authenticatedFetch } from './AuthControl.js';
import { setStore as setNotificationStore, addToastAction } from '../../notification/control/NotificationsControl.js';

describe('handleErrorResponse — field-error toast', () => {
    let dispatched;

    beforeEach(() => {
        dispatched = [];
        const fakeStore = {
            getState: () => ({ auth: { accessToken: 'token' } }),
            dispatch: (action) => dispatched.push(action),
        };
        setStore(fakeStore);
        setNotificationStore(fakeStore);
    });

    it('shows the first field error in the toast detail', async () => {
        globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
            title: 'Validation Failed',
            detail: 'Validation failed',
            errors: [{ field: 'role', message: 'must not be null' }],
        }), { status: 400, headers: { 'content-type': 'application/problem+json' } }));

        await authenticatedFetch('/api/whatever', {});

        const toast = dispatched.find(a => a.type === addToastAction.type);
        expect(toast.payload.detail).toBe('role: must not be null');
    });

    it('falls back to detail/title when there are no field errors', async () => {
        globalThis.fetch = vi.fn(async () => new Response(JSON.stringify({
            title: 'Not Found',
            detail: 'Item not found',
        }), { status: 404, headers: { 'content-type': 'application/problem+json' } }));

        await authenticatedFetch('/api/whatever', {});

        const toast = dispatched.find(a => a.type === addToastAction.type);
        expect(toast.payload.detail).toBe('Item not found');
    });
});
