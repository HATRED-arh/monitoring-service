### Setup
Create `servers.txt` with needed IPs separated with new line and `domains.txt` with domains in folder `resources`.

Set up your `.env` according to `.env.example`.

Run `docker build . -t monitoring:latest` and `docker run monitoring`.

If everything is ok you should see logs. (they are bad, have to setup proper logging)