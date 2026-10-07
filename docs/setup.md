# Developer machine setup

How to get a Windows 11 machine ready for Forgotten Heroes, and how to prove it is ready. The proof
is one command:

```bash
./scripts/check-env.sh    # every row ✔ and exit code 0 = you are set up
```

Run it at the start of a session, or whenever something feels off. It prints one row per tool, fails
loudly (exit code 1) on any miss, and never installs anything itself. The shell for this project is
**Git Bash**; every command below is bash.

## 1. The toolchain

| Tool | Required | Why this version |
|---|---|---|
| Amazon Corretto JDK | **25** (LTS), first `java` on the PATH | AWS Lambda's `java25` runtime. Code compiled for Java 26 will not run there. |
| `JAVA_HOME` | the Corretto 25 folder | Maven (via the wrapper, Step 07) and VS Code's Java extension find the JDK through it, independently of the PATH. |
| Node.js + npm | ≥ 22 (LTS) | Vite 8 / React 19 tooling for the frontend. |
| Docker Desktop | installed **and running** | throwaway DynamoDB Local for backend tests; `sam local`. |
| AWS CLI | ≥ 2.20 | talks to the AWS account (`aws configure sso`, Step 06). |
| AWS SAM CLI | ≥ 1.130 | validates, builds and deploys `infra/template.yaml`; runs Lambda locally. |
| ffmpeg | any | builds the audio sprite (Step 50). |
| Python + Pillow | ≥ 3.11, Pillow any | the asset pipeline (Step 39). |
| GitHub CLI (`gh`) | logged in | repo, issues, board and PRs from the terminal. |
| Git | any recent | you know this one. |
| VS Code | + the nine extensions in §2 | the editor. |

Deliberately **not** installed: a global Maven. Step 07 adds the script-only Maven Wrapper (`./mvnw`),
which downloads the right Maven once. Aseprite is optional (~$20) and not required.

## 2. Fresh machine: install in this order

Run from Git Bash (`winget` is a Windows installer; it works fine from Git Bash):

```bash
winget install --id Git.Git -e                 # Git for Windows (brings Git Bash)
winget install --id GitHub.cli -e
winget install --id Amazon.Corretto.25.JDK -e  # sets JAVA_HOME and the PATH for you
winget install --id OpenJS.NodeJS.LTS -e
winget install --id Docker.DockerDesktop -e    # launch it once and finish its setup wizard
winget install --id Amazon.AWSCLI -e
winget install --id Amazon.SAM-CLI -e
winget install --id Gyan.FFmpeg -e
winget install --id Python.Python.3.13 -e      # python.org build; the Microsoft Store build works too
python -m pip install Pillow
gh auth login                                  # GitHub.com → HTTPS → log in with a browser
```

VS Code (`winget install --id Microsoft.VisualStudioCode -e`) and its nine extensions:

```bash
for e in vscjava.vscode-java-pack vmware.vscode-boot-dev-pack esbenp.prettier-vscode \
         bradlc.vscode-tailwindcss ms-playwright.playwright amazonwebservices.aws-toolkit-vscode \
         editorconfig.editorconfig vitest.explorer redhat.vscode-yaml; do
  code --install-extension "$e"
done
```

Make Git Bash the editor's default terminal once: *Terminal → Select Default Profile → Git Bash*.

## 3. Open a new terminal

Installers change the PATH and `JAVA_HOME` in the Windows registry, but every program that is
already running keeps the copy it read at start-up. So after any install: close the terminal (and
VS Code), open a new one, and only then run the check script. "Command not found" right after an
install almost always means "old terminal".

## 4. Switching Java

Windows finds `java` by walking the PATH from left to right and taking the first match. With two
JDKs installed (Corretto 25 and Oracle 26 on this machine), the order decides which one wins.

```bash
java -version          # must say "Corretto-25..."
echo "$JAVA_HOME"      # C:\Program Files\Amazon Corretto\jdk25.0.4_10
where java             # every java.exe on the PATH; the first one wins
```

If `java -version` says 26: *Start → "Edit the system environment variables" → Environment
Variables… → System variables → Path → Edit*, move `C:\Program Files\Amazon Corretto\jdk25.0.4_10\bin`
above `C:\Program Files\Common Files\Oracle\Java\javapath`, OK, then open a new terminal.
`JAVA_HOME` lives in the same dialog if it ever needs correcting.

## 5. SAM CLI in Git Bash (Windows only)

The SAM installer ships one file, `sam.cmd`. PowerShell and cmd run it when you type `sam`;
**Git Bash does not** look for `.cmd` files, so `sam` says "command not found" while `sam.cmd` works.
Every command in the plan says `sam`, so add a tiny shim that does what `sam.cmd` does. Git Bash
puts `~/bin` on the PATH automatically (see `/etc/profile.d/env.sh`):

```bash
mkdir -p ~/bin
cat > ~/bin/sam <<'SHIM'
#!/bin/sh
# Git Bash cannot run "sam.cmd" by its bare name. Forward "sam" to the SAM CLI's own Python,
# exactly as sam.cmd does. (winget installs the SAM CLI under C:\Program Files\Amazon\AWSSAMCLI.)
exec "/c/Program Files/Amazon/AWSSAMCLI/runtime/python.exe" -m samcli "$@"
SHIM
chmod +x ~/bin/sam
sam --version          # SAM CLI, version 1.167.0   (new terminal if ~/bin did not exist before)
```

## 6. Docker must be running

Docker has two halves: the `docker` command, which is always there, and the engine, which only runs
while Docker Desktop is open. Backend tests, DynamoDB Local and `sam local` all need the engine.
Start Docker Desktop before a backend session and leave it running; the check script's `docker` row
shows ✘ "daemon stopped" until the engine is up.

## 7. Reading the check script's output

```text
Tool       Found                          Required         OK
java       Corretto 25.0.4.10.1           Corretto 25      ✔
JAVA_HOME  jdk25.0.4_10 (Java 25)         Corretto 25 dir  ✔
node       26.2.0                         >=22             ✔
npm        11.13.0                        any              ✔
docker     29.6.1 (running)               running          ✔
aws        2.37.10                        >=2.20           ✔
sam        1.167.0                        >=1.130          ✔
ffmpeg     9.0.2                          any              ✔
python     3.13.14 + Pillow 12.3.0        >=3.11 + Pillow  ✔
gh         2.96.0 (bryan-mcneil)          logged in        ✔
git        2.52.0                         any              ✔

Toolchain OK — every row is ✔.
```

`echo $?` right after the script prints its **exit code**: `0` means success, `1` means at least one
✘, and the lines under "How to fix" say what to do. CI will rely on that same number later.

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| `java -version` says 26 | §4: move Corretto above Oracle in the PATH, open a new terminal |
| `sam: command not found` but `sam.cmd --version` works | §5: create the `~/bin/sam` shim |
| `docker` row says *daemon stopped* | §6: start Docker Desktop and wait for the whale icon to stop animating |
| `python` opens the Microsoft Store | the Store alias is a stub; install Python (§2) or turn the alias off under *Settings → Apps → Advanced app settings → App execution aliases* |
| `gh` row says *not logged in* | `gh auth login` |
| anything says *not found* right after an install | §3: open a new terminal |
| `Permission denied` running the script (Linux/macOS/CI) | `chmod +x scripts/check-env.sh`; Windows has no executable bit, so Git records it with `git update-index --chmod=+x scripts/check-env.sh` |
