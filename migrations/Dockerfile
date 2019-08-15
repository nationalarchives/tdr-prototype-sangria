FROM boxfuse/flyway
COPY sql/* /flyway/sql
# Override parent entrypoint, which is "/flyway/flyway". If you use an entrypoint, you have to provide CMD arguments in
# array form, but this is incompatible with --env variables. See https://github.com/moby/moby/issues/5509.
ENTRYPOINT []
CMD /flyway/flyway migrate -url=$DB_URL -user=$DB_USERNAME -password=$DB_PASSWORD -X
