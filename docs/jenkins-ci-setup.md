# Jenkins CI Setup — Acetoy Gatling Load Test

How to run the `acetoy.AcetoySimulation` load test from Jenkins, using a local
Jenkins in Docker. Pipeline definition lives in [`Jenkinsfile`](../Jenkinsfile)
at the repo root.

---

## Prerequisites

- **Docker Desktop running.** On macOS, Docker is a daemon behind the `docker`
  CLI — if the daemon is stopped, every `docker` command fails and Jenkins
  never starts (this is the usual cause of `ERR_CONNECTION_REFUSED` on
  `localhost:8080`). Start it first:
  ```bash
  open -a Docker
  # wait until ready:
  until docker info >/dev/null 2>&1; do sleep 2; done; echo "Docker ready"
  ```
- Outbound internet (the test hits the live `acetoys.uk` demo; Maven downloads
  dependencies on first run).

No local Java or Maven needed — the Jenkins image ships JDK 17 and the project's
`./mvnw` wrapper downloads Maven itself.

---

## 1. Start Jenkins

```bash
docker run -d --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts-jdk17
```

- `-v jenkins_home:...` persists all Jenkins data in a named volume, so config
  and build history survive container restarts.
- First boot takes ~30–60s.

## 2. Unlock and install plugins

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open <http://localhost:8080>, paste the password, choose **Install suggested
plugins**, then create your admin user.

## 3. Add the Gatling plugin

**Manage Jenkins → Plugins → Available plugins**, search **Gatling**, install,
and tick "Restart Jenkins when installation is complete". This plugin powers
`gatlingArchive()` in the Jenkinsfile — it publishes the HTML report and builds
trend graphs across runs.

## 4. Create the pipeline job

**New Item** → name `acetoy-load-test` → **Pipeline** → OK. In the **Pipeline**
section:

| Field            | Value                                                              |
|------------------|--------------------------------------------------------------------|
| Definition       | Pipeline script from SCM                                           |
| SCM              | Git                                                                |
| Repository URL   | `https://github.com/khanntm/advanced-gatling-web-apps-java.git`    |
| Branch Specifier | `*/main`                                                           |
| Script Path      | `Jenkinsfile`                                                      |

The repo is public, so no credentials are required. Save.

## 5. Run it

1. Click **Build Now** **once**. Jenkins must run the pipeline a first time to
   discover its parameters (it reads them from the `Jenkinsfile`).
2. After that, the job shows **Build with Parameters**:

   | Parameter       | Choices / default                                                | Meaning                                  |
   |-----------------|------------------------------------------------------------------|------------------------------------------|
   | `TEST_TYPE`     | `INSTANT_USERS` / `RAMP_USERS` / `COMPLEX_INJECTION` / `CLOSED_MODEL` | Injection profile (`acetoy.simulation.TestPopulation`) |
   | `BASE_URL`      | `https://acetoys.uk`                                             | Target under test                        |
   | `TEST_DURATION` | `60`                                                             | Scenario loop duration (seconds)         |

3. Open the finished build → **Gatling** (left menu) for the report. Repeated
   runs accumulate a trend graph automatically.

---

## What the pipeline does

1. **Compile** — `./mvnw -B clean test-compile`
2. **Load Test** — `./mvnw gatling:test` for `acetoy.AcetoySimulation`, passing
   the three parameters as `-D` system properties.
3. **Always** — `gatlingArchive()` publishes the report.

`disableConcurrentBuilds()` prevents two load tests hammering the target at the
same time; a 60-minute `timeout` guards against a stuck run.

---

## Managing the container

```bash
docker stop jenkins      # stop (data persists in the volume)
docker start jenkins     # start again
docker logs -f jenkins   # follow the log (boot progress, errors)
docker rm -f jenkins     # remove the container (volume jenkins_home is kept)
docker volume rm jenkins_home   # wipe ALL Jenkins data (full reset)
```

---

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `localhost:8080` → `ERR_CONNECTION_REFUSED` | Docker daemon not running, or container not started. Run `open -a Docker`, wait for it, then `docker start jenkins` (or the `docker run` above). |
| `docker: ... Cannot connect to the Docker daemon` | Docker Desktop is stopped — start it first. |
| Port 8080 already in use | Another app holds 8080. Map a different host port, e.g. `-p 8081:8080`, then browse `localhost:8081`. |
| Job shows only **Build Now**, no parameters | Normal on a brand-new job — run it once so Jenkins reads the parameters from the `Jenkinsfile`. |
| No **Gatling** link on the build | Gatling plugin not installed — see step 3. |
| `./mvnw: Permission denied` in the build | The executable bit on `mvnw` was lost. `git update-index --chmod=+x mvnw` and commit. |

---

## Alternative: GitHub Actions

Jenkins is good for learning CI mechanics and on-prem control. For a hosted,
zero-maintenance option you could instead add a GitHub Actions workflow that
runs the same `./mvnw gatling:test` on a schedule or per-PR and uploads the
report as an artifact. Not set up yet — ask if you want it added.
