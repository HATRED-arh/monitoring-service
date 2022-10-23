### Setup
Create `servers.txt` with needed IPs separated with new line and `domains.txt` with domains in folder `resources`.

Set up your environment according to `.env.example`. (program does not load `.env` files yet)

Run `docker build . -t monitoring:latest` and `docker run monitoring`.

If everything is ok you should see logs. (they are bad, have to setup proper logging)

To check domains, domain needs `/healthcheck` route.