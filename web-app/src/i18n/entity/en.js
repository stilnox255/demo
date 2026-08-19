/**
 * English catalogue — also the fallback locale.
 *
 * Keys are flat strings; the dots are literal characters, not a nesting path.
 * That keeps the lookup a single property access and lets a key be composed at
 * the call site (`demo.status.${item.status}`).
 *
 * These values are asserted verbatim by the Playwright specs in `tests/admin/`.
 * Changing one here is a test change too.
 */
export const en = {
    "app.title": "Starter Admin",
    "app.footer": "Starter — reference stack",

    "nav.menu": "Menu",
    "nav.demoItems": "Demo Items",
    "nav.apiDocumentation": "API Documentation",
    "nav.systemStatus": "System Status",

    "landing.tagline":
        "Reference frontend for the standard stack: web components, Redux Toolkit, " +
        "client-side routing and OIDC — no framework beyond the platform.",
    "landing.signIn": "Sign In",
    "landing.signInAria": "Sign in",
    "landing.initializing": "Initializing…",
    "landing.featuresAria": "Features",
    "landing.feature.demo.title": "Demo Resource",
    "landing.feature.demo.body":
        "One aggregate end to end: paginated list, validation, optimistic locking, " +
        "file attachment and signed downloads.",
    "landing.feature.status.title": "System Status",
    "landing.feature.status.body":
        "Live readiness of the dependencies that decide whether this instance can " +
        "serve — database and object storage.",
    "landing.feature.api.title": "API Documentation",
    "landing.feature.api.body":
        "Generated OpenAPI with Swagger UI, annotated per endpoint rather than left " +
        "as a bare type listing.",

    "auth.loading": "Loading...",
    "auth.loginRequired": "Login Required",
    "auth.login": "Login",
    "auth.logout": "Logout",
    "auth.loggedInAs": "Logged in as:",
    "auth.userFallback": "User",
    "auth.requestFailed": "Request failed",
    "auth.sessionExpired.title": "Session expired",
    "auth.sessionExpired.detail": "Log in again to continue",
    "auth.sessionExpired.action": "Log in again",

    "demo.items.title": "Demo Items",
    "demo.items.loading": "Loading…",
    "demo.column.name": "Name",
    "demo.column.status": "Status",
    "demo.column.attachment": "Attachment",
    "demo.column.version": "Version",
    "demo.attach": "Attach",
    "demo.delete": "Delete",
    "demo.activate": "Activate",
    "demo.archive": "Archive",
    "demo.noAttachment": "—",
    // The status values stay uppercase in English: they double as the assertion
    // target in demo-items.spec.js, and the backend enum is what an operator
    // reading the API sees. German translates them.
    "demo.status.DRAFT": "DRAFT",
    "demo.status.ACTIVE": "ACTIVE",
    "demo.status.ARCHIVED": "ARCHIVED",
    "demo.form.name": "Name",
    "demo.form.namePlaceholder": "What to call it",
    "demo.form.description": "Description",
    "demo.form.descriptionPlaceholder": "Optional",
    "demo.form.create": "Create",
    "demo.toast.created": "Item created",
    "demo.toast.updated": "Item updated",
    "demo.toast.deleted": "Item deleted",
    "demo.toast.attached": "File attached",

    "status.up": "All systems operational",
    "status.down": "System outage detected",
    "status.checking": "Checking system...",
    "status.technicalDetails": "Technical Details",
    "status.service.database.name": "Database",
    "status.service.database.description": "PostgreSQL connection pool",
    "status.service.s3.name": "Object Storage",
    "status.service.s3.description": "S3-compatible file storage",
    "status.label.UP": "Operational",
    "status.label.DOWN": "Offline",
    "status.label.UNKNOWN": "Unknown",

    "api.title": "API Documentation",
    "api.specification": "OpenAPI Specification",
    "api.viewDocumentationAt": "View API documentation at",
    "api.swaggerUiAt": "Swagger UI at",

    "table.noData": "No data",

    "pagination.previous": "‹ Prev",
    "pagination.next": "Next ›",
    "pagination.range": "{start}–{end} of {total} (page {page}/{totalPages})",

    "notification.listAria": "Notifications",
    "notification.dismiss": "Dismiss",

    "theme.toggle": "Toggle theme",

    "locale.switch": "Switch to {language}"
};
