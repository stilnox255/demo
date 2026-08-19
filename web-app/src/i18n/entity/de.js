/**
 * German catalogue. Same key set as `en.js` — a missing key here renders the key
 * itself, so parity is the thing to check when adding a string.
 *
 * Phrasing avoids the du/Sie decision where it can (impersonal infinitive), so a
 * project grown from this starter can pick either without rewriting the catalogue.
 */
export const de = {
    "app.title": "Starter Admin",
    "app.footer": "Starter — Referenz-Stack",

    "nav.menu": "Menü",
    "nav.demoItems": "Demo-Objekte",
    "nav.apiDocumentation": "API-Dokumentation",
    "nav.systemStatus": "Systemstatus",

    "landing.tagline":
        "Referenz-Frontend für den Standard-Stack: Web Components, Redux Toolkit, " +
        "clientseitiges Routing und OIDC — kein Framework über die Plattform hinaus.",
    "landing.signIn": "Anmelden",
    "landing.signInAria": "Anmelden",
    "landing.initializing": "Wird initialisiert…",
    "landing.featuresAria": "Funktionen",
    "landing.feature.demo.title": "Demo-Ressource",
    "landing.feature.demo.body":
        "Ein Aggregat von vorne bis hinten: paginierte Liste, Validierung, " +
        "optimistisches Locking, Datei-Anhang und signierte Downloads.",
    "landing.feature.status.title": "Systemstatus",
    "landing.feature.status.body":
        "Live-Bereitschaft der Abhängigkeiten, die darüber entscheiden, ob diese " +
        "Instanz bedienen kann — Datenbank und Objektspeicher.",
    "landing.feature.api.title": "API-Dokumentation",
    "landing.feature.api.body":
        "Generiertes OpenAPI mit Swagger UI, pro Endpunkt annotiert statt als " +
        "nackte Typenliste.",

    "auth.loading": "Wird geladen...",
    "auth.loginRequired": "Anmeldung erforderlich",
    "auth.login": "Anmelden",
    "auth.logout": "Abmelden",
    "auth.loggedInAs": "Angemeldet als:",
    "auth.userFallback": "Benutzer",
    "auth.requestFailed": "Anfrage fehlgeschlagen",
    "auth.sessionExpired.title": "Sitzung abgelaufen",
    "auth.sessionExpired.detail": "Zum Fortfahren erneut anmelden",
    "auth.sessionExpired.action": "Erneut anmelden",

    "demo.items.title": "Demo-Objekte",
    "demo.items.loading": "Wird geladen…",
    "demo.column.name": "Name",
    "demo.column.status": "Status",
    "demo.column.attachment": "Anhang",
    "demo.column.version": "Version",
    "demo.attach": "Anhängen",
    "demo.delete": "Löschen",
    "demo.activate": "Aktivieren",
    "demo.archive": "Archivieren",
    "demo.noAttachment": "—",
    "demo.status.DRAFT": "Entwurf",
    "demo.status.ACTIVE": "Aktiv",
    "demo.status.ARCHIVED": "Archiviert",
    "demo.form.name": "Name",
    "demo.form.namePlaceholder": "Wie soll es heißen",
    "demo.form.description": "Beschreibung",
    "demo.form.descriptionPlaceholder": "Optional",
    "demo.form.create": "Anlegen",
    "demo.toast.created": "Objekt angelegt",
    "demo.toast.updated": "Objekt aktualisiert",
    "demo.toast.deleted": "Objekt gelöscht",
    "demo.toast.attached": "Datei angehängt",

    "status.up": "Alle Systeme betriebsbereit",
    "status.down": "Systemausfall erkannt",
    "status.checking": "System wird geprüft...",
    "status.technicalDetails": "Technische Details",
    "status.service.database.name": "Datenbank",
    "status.service.database.description": "PostgreSQL-Verbindungspool",
    "status.service.s3.name": "Objektspeicher",
    "status.service.s3.description": "S3-kompatibler Dateispeicher",
    "status.label.UP": "Betriebsbereit",
    "status.label.DOWN": "Offline",
    "status.label.UNKNOWN": "Unbekannt",

    "api.title": "API-Dokumentation",
    "api.specification": "OpenAPI-Spezifikation",
    "api.viewDocumentationAt": "API-Dokumentation unter",
    "api.swaggerUiAt": "Swagger UI unter",

    "table.noData": "Keine Daten",

    "pagination.previous": "‹ Zurück",
    "pagination.next": "Weiter ›",
    "pagination.range": "{start}–{end} von {total} (Seite {page}/{totalPages})",

    "notification.listAria": "Benachrichtigungen",
    "notification.dismiss": "Schließen",

    "theme.toggle": "Design umschalten",

    "locale.switch": "Zu {language} wechseln"
};
