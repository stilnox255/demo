import { describe, it, expect, beforeEach } from 'vitest';
import { setStore, hasRole } from './AuthControl.js';

function jwtWith(payload) {
    const b64 = (obj) => btoa(JSON.stringify(obj))
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    return `${b64({ alg: 'none' })}.${b64(payload)}.signature`;
}

describe('hasRole', () => {
    beforeEach(() => {
        setStore({ getState: () => ({ auth: { accessToken: null } }) });
    });

    it('returns false when no access token is set', () => {
        expect(hasRole('admin')).toBe(false);
    });

    it('returns true when the access token grants the role', () => {
        const token = jwtWith({ realm_access: { roles: ['admin', 'user'] } });
        setStore({ getState: () => ({ auth: { accessToken: token } }) });
        expect(hasRole('admin')).toBe(true);
    });

    it('returns false when the access token does not grant the role', () => {
        const token = jwtWith({ realm_access: { roles: ['user'] } });
        setStore({ getState: () => ({ auth: { accessToken: token } }) });
        expect(hasRole('admin')).toBe(false);
    });
});
