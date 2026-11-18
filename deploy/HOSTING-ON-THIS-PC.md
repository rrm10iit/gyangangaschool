# Host the school website from this PC with Cloudflare

Result: **https://gyangangaschool.bid** shows the public website. Visitors need nothing installed.
The admin panel (`/admin`) asks for an email code from Cloudflare first, then the normal admin password.
No router settings are changed and no port is opened to the internet.

```
Visitor / admin browser -> Cloudflare -> tunnel (started from this PC) -> Docker on this PC
```

Cost: the domain only. Cloudflare Tunnel and Cloudflare Access (up to 50 people) are free.
When the PC is off, the site is down. Nothing is lost.

## Part A. The domain (already done)

You bought **gyangangaschool.bid** from Cloudflare, so it is already on Cloudflare with Cloudflare nameservers. Nothing to do here.
If you ever move it elsewhere, add it again under **Add a domain** and change the nameservers at the new registrar.

## Part B. Create the tunnel (once)

1. In the Cloudflare dashboard open **Zero Trust** in the left menu. The first time, it asks for a team name (anything,
   for example `gyanganga`) and a plan. Choose the **Free** plan. It may ask for a card to verify you; the plan stays at 0.
2. Go to **Networks, Tunnels** and click **Create a tunnel**. Choose **Cloudflared**, name it `gyanganga-pc`, **Save**.
3. When it asks you to install the connector, choose **Docker**. It shows a command containing `--token` followed by a
   very long text. **Copy only that long token.** Treat it like a password: anyone with it can run your tunnel.
4. Open the tunnel, then the **Routes** tab, and add a **published application route** (do this twice):

   | Subdomain | Domain | Service type | URL |
   | --- | --- | --- | --- |
   | *(leave empty)* | gyangangaschool.bid | HTTP | `http://proxy:80` (some forms want just `proxy:80`) |
   | `www` | gyangangaschool.bid | HTTP | `http://proxy:80` |

   Cloudflare creates the DNS records for you. Do not install anything else it suggests; the project starts the
   connector for you.

## Part C. Set up and start the project on this PC

1. Open PowerShell in the project folder and copy the settings file:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Make three strong secrets (run this three times and use a different result each time):

   ```powershell
   $b = New-Object byte[] 36; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
   ```

3. Edit `.env` (Notepad is fine) and set:

   ```
   DB_PASSWORD=<first secret>
   JWT_SECRET=<second secret>
   ADMIN_PASSWORD=<a strong password you will remember>
   SEED_DEMO_DATA=false
   SITE_ADDRESS=:80
   PUBLIC_URL=https://gyangangaschool.bid
   COOKIE_SECURE=true
   HTTP_PORT=8088
   TUNNEL_TOKEN=<the long token from Part B step 3>
   COMPOSE_FILE=docker-compose.yml;docker-compose.tunnel.yml
   ```

   `SEED_DEMO_DATA=false` matters: it keeps fake students and fake fees out of the real site.
   `COMPOSE_FILE` makes plain `docker compose` commands include the tunnel.

4. Make sure Docker Desktop is running, then start everything:

   ```powershell
   docker compose up -d --build
   docker compose ps
   ```

   All services should say `Up` (backend and frontend `healthy`). Check the tunnel connected:

   ```powershell
   docker compose logs cloudflared --tail 10
   ```

   Look for "Registered tunnel connection". In Cloudflare, the tunnel should show **Healthy**.

5. Open <https://gyangangaschool.bid>. The public site should load with its pictures and numbers.

## Part D. Lock the admin area with an email code (once)

1. In **Zero Trust** go to **Access, Applications, Add an application, Self-hosted**.
2. Name it `School admin`. Under **Public hostname** add these entries, all with domain `gyangangaschool.bid`:

   | Path |
   | --- |
   | `admin` and `admin/*` |
   | `api/admin/*` |
   | `api/auth/*` |

   (If the form takes one path per row, add each as its own row.)
3. Add a policy: **Allow**, rule **Emails**, and list the email addresses of you and your admins.
4. For the login method choose **One-time PIN**. Save.

Now `https://gyangangaschool.bid/admin` first shows a Cloudflare page asking for an email. Only listed addresses get a
6-digit code. After that the normal admin sign-in appears (email `admin@gyanganga.school` and your `ADMIN_PASSWORD`).

**Test it** from a phone on mobile data (not your home Wi-Fi), in a private tab:
- the public pages open without asking for anything;
- `/admin/login` shows the Cloudflare email page first;
- an email that is not on the list gets no code.

## Part E. First things to do in the admin panel

1. Create a separate account for each admin (Staff accounts) and give each the least access needed
   (Teacher, Accountant or Admin). Then change the first admin's password (My account).
2. Fill in **Page content**: phone, email, year founded, principal's name and message, history, seat counts.
3. Upload real photos under Facilities, Photo gallery and Management team.

## Keeping it running from a PC

- **Never let the PC sleep while plugged in.** Settings, System, Power, Screen and sleep: set sleep to Never.
- **Docker Desktop must start at login.** Docker Desktop, Settings, General, "Start Docker Desktop when you sign in".
  After a Windows restart, sign in once; the containers restart by themselves.
- **Updating the site later:** `git pull`, then `docker compose up -d --build`.
- **Backups:** the `backup` service saves the database and uploads every night into the `backups` folder, but that is the
  same PC. Copy that folder to another drive or a cloud folder (OneDrive, Google Drive) every week.
  Restore steps are in the main README.
- **Windows updates** may restart the PC; the site is simply down for a few minutes.

## Safety checklist

- [ ] `SEED_DEMO_DATA=false`, and the sample password `Admin@12345` is nowhere in use
- [ ] `.env` is never emailed, shared or committed (it is already ignored by git)
- [ ] Access email list contains only people who should manage the school data
- [ ] Each admin has their own account (the activity log shows who changed what)
- [ ] Backups copied off this PC
- [ ] Windows is kept updated, and Windows Defender stays on

## If something is wrong

| What you see | Likely cause and fix |
| --- | --- |
| Cloudflare error **1033** | The tunnel is not running. `docker compose ps`, then `docker compose logs cloudflared` |
| Cloudflare error **502 / 521 / 523** | The URL in the public hostname is wrong. It must be `proxy:80` with type HTTP |
| Site opens but shows 0 students and no pictures | The backend is down. `docker compose logs backend` |
| Cannot sign in to admin after the email code | Check `COOKIE_SECURE=true` and `PUBLIC_URL=https://gyangangaschool.bid` in `.env`, then `docker compose up -d` |
| "Too many attempts" when signing in | Five wrong passwords lock that account for 15 minutes |
| `TUNNEL_TOKEN` error when starting | The token line in `.env` is empty or has extra spaces |

To stop everything: `docker compose down` (data is kept). `docker compose down -v` also erases all data. Do not use it on the real site.
