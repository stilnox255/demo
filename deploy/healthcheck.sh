#!/bin/bash
# Health checks live on the management interface (quarkus.management.port=9000),
# not on the application port — see application.properties.
#
# Liveness, not readiness, on purpose: this decides whether Docker restarts the
# container, and a restart does not fix an unreachable database. Readiness is
# what the load balancer asks for (see the Traefik labels in the compose file);
# conflating them turns a dependency outage into a restart loop.
#
# /dev/tcp instead of curl or wget: neither is in the base image, and adding one
# for a health check widens the image for a single line of shell. The Host header
# is mandatory — without it Vert.x cannot build an absolute request URI and the
# request fails before routing.
exec 3<>/dev/tcp/localhost/9000
echo -e "GET /q/health/live HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3
head -n 1 <&3 | grep -q "200"
exit $?
