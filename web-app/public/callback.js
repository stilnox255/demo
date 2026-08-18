// OIDC redirect target. Exchanges the authorization code for tokens and
// hands off to the SPA. Kept as a standalone file in `public/` (copied
// verbatim by Vite) so that CSP `script-src 'self'` can allow it without
// `'unsafe-inline'`. No bundler-resolved imports here — only browser APIs.

const appConfigResponse = await fetch("/.well-known/app-config");
const appConfig = await appConfigResponse.json();

const oidcConfigResponse = await fetch(`${appConfig.authConfig.issuer}/.well-known/openid-configuration`);
const oidcConfig = await oidcConfigResponse.json();

const params = new URLSearchParams(globalThis.location.search);
const code = params.get("code");

if (!code) {
    globalThis.location.href = "/";
}

const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: appConfig.authConfig.clientId,
    code: code,
    redirect_uri: globalThis.location.origin + "/callback.html",
    code_verifier: sessionStorage.getItem("pkce_verifier") ?? "",
});
sessionStorage.removeItem("pkce_verifier");

fetch(oidcConfig.token_endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body,
})
    .then(async (res) => {
        if (!res.ok) {
            const errorText = await res.text();
            console.error("Token exchange failed:", res.status, errorText);
            throw new Error(`Token exchange failed: ${res.status}`);
        }
        return res.json();
    })
    .then((data) => {
        if (!data.access_token) {
            console.error("No access token in response:", data);
            throw new Error("No access token received");
        }

        localStorage.setItem("access_token", data.access_token);
        if (data.refresh_token) {
            localStorage.setItem("refresh_token", data.refresh_token);
        }
        if (data.id_token) {
            localStorage.setItem("id_token", data.id_token);
        }

        const payload = JSON.parse(atob(data.access_token.split(".")[1]));
        localStorage.setItem("user_info", JSON.stringify(payload));

        globalThis.location.href = "/";
    })
    .catch((err) => {
        console.error("Login failed:", err);
        alert("Login failed. Please try again.");
        globalThis.location.href = "/";
    });
