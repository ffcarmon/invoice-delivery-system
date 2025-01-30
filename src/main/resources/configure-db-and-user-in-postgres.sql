

docker exec -it postgres-container psql -U admin

CREATE USER admin WITH SUPERUSER PASSWORD 'my-secret-pw';

CREATE DATABASE myappdb TEMPLATE template0;

\c myappdb admin


