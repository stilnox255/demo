// Central credential map. One entry per user group.
// Passwords come from env in CI; dev defaults match the dev Keycloak realm.
export const USERS = {
    admin: { username: 'admin', password: process.env.ADMIN_PASSWORD ?? 'admin' },
    // Add further roles here, then create tests/setup/<role>.setup.js and tests/<role>/.
    // annotator: { username: 'annotator', password: process.env.ANNOTATOR_PASSWORD ?? 'annotator' },
};
