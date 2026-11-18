# The bundle is built and tested by CI (npm run build), this image only serves it.
FROM nginxinc/nginx-unprivileged:1.31-alpine

# install security fixes released after the base image was built (found by the Trivy scan in CI)
USER root
RUN apk upgrade --no-cache
USER 101

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html

EXPOSE 8080
