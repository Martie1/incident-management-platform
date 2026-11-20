FROM nginxinc/nginx-unprivileged:1.31-alpine

# pull security fixes released after the base image was built
USER root
RUN apk upgrade --no-cache
USER 101

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html

EXPOSE 8080
