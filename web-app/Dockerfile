FROM nginx:1.27-alpine
COPY dist /usr/share/nginx/html
# templates/, not conf.d/: the image renders these at start-up so the CSP's
# auth origin comes from the environment. See nginx.conf.template.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV NGINX_ENVSUBST_FILTER=^AUTH_
ENV AUTH_ORIGIN=""
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/healthz || exit 1
EXPOSE 80
