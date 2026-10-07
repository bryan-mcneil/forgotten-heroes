# Environment Audit — your machine (7 Oct 2026, updated after the installs)

> Audited before planning, then the missing tools were installed in the same session. Windows 11 Home
> 10.0.26300. **Default shell for this project: Git Bash** (Q18); every script ships as `.sh`.

## 1. Installed and verified
| Tool | Version found | Verdict |
|---|---|---|
| **Java** | **Amazon Corretto 25.0.4.10.1** (LTS) at `C:\Program Files\Amazon Corretto\jdk25.0.4_10`; `JAVA_HOME` set machine-wide by the installer; its `bin` precedes Oracle's `javapath` in `PATH` | ✅ matches the Lambda `java25` runtime. Oracle JDK 26.0.1 is still installed but no longer first |
| **AWS CLI** | 2.37.10 (`C:\Program Files\Amazon\AWSCLIV2`) | ✅ no credentials yet — Step 06 |
| **AWS SAM CLI** | 1.167.0 — ships only as `sam.cmd`; Git Bash runs it as `sam` through the `~/bin/sam` shim (`docs/setup.md` §5, Step 01) | ✅ |
| **ffmpeg** | 9.0.2 (Gyan full build; user PATH via winget) | ✅ audio sprite in Step 50 |
| Node.js / npm | v26.2.0 / 11.13.0 | ✅ Vite 8 / React 19 |
| Git | 2.52.0 (user "Bryan", e-mail set) | ✅ |
| GitHub CLI | 2.96.0, logged in as **bryan-mcneil** | ✅ |
| Docker | 29.6.1 (Docker Desktop) | ✅ **must be running** for backend tests, DynamoDB Local, `sam local` |
| Python | 3.13.14 with Pillow 12.3.0 | ✅ asset pipeline |
| VS Code | 1.140.0 + newly installed: Java pack (`redhat.java`, Maven, debugger, test runner, project manager), Spring Boot dev pack, Prettier, Tailwind, Playwright, AWS Toolkit, EditorConfig, Vitest explorer, YAML — plus your existing ESLint, Python, Dart/Flutter and PHP extensions | ✅ |
| winget | 1.29 | ✅ |
| Git Bash / WSL2 | both available | ✅ Git Bash is the default; WSL2 is a fallback |
| Chrome | installed | ✅ Playwright / Lighthouse |

> ⚠️ **Open a new terminal and restart VS Code** before Step 01: `PATH` and `JAVA_HOME` changes only reach
> processes started after the installs.

## 2. Deliberately not installed
| Tool | Why not |
|---|---|
| Maven (global) | `Apache.Maven` is not in winget, and we do not need it: Step 07 unzips the official **script-only Maven Wrapper** (`mvnw`, `mvnw.cmd`) and writes `.mvn/wrapper/maven-wrapper.properties` pointing at Maven 3.9.16, which the wrapper downloads once. Every build command is `./mvnw …` |
| Aseprite | only if you want to edit pixel art yourself (~$20); not required |

### For a fresh machine (what was run on 7 Oct 2026)
```bash
winget install --id Amazon.Corretto.25.JDK -e
winget install --id Amazon.AWSCLI -e
winget install --id Amazon.SAM-CLI -e
winget install --id Gyan.FFmpeg -e
for e in vscjava.vscode-java-pack vmware.vscode-boot-dev-pack esbenp.prettier-vscode \
         bradlc.vscode-tailwindcss ms-playwright.playwright amazonwebservices.aws-toolkit-vscode \
         editorconfig.editorconfig vitest.explorer redhat.vscode-yaml; do code --install-extension "$e"; done
```

## 3. Accounts
| Account | State | Action |
|---|---|---|
| AWS | created ≈ Aug/Sep 2026 → credit-based **Free plan** (Q1); **no credentials configured** | Step 06: record credits + expiry (reminder to upgrade ≈ Feb/Mar 2027), root MFA, IAM Identity Center user, `aws configure sso` profile `fh`, **us-east-1**, budgets to the account owner's e-mail (kept out of the public repo) |
| GitHub | bryan-mcneil, 14 public repos incl. `portfolio` (Next.js) | Step 02 creates `forgotten-heroes` (public) |
| Hostinger | Business hosting; domains **bryanmcneil.pro** (chosen, Q2) and gadgetdrop.tech | Step 66: `heroes.bryanmcneil.pro` → CloudFront via ACM + Hostinger DNS; Phase 4: `corgi.bryanmcneil.pro` studio page |
| Cognito/Google | none yet | created by the SAM template (Cognito); Google sign-in is a stretch |

## 4. `scripts/check-env.sh` (Step 01 deliverable) prints a table like
```
Tool        Found              Required        OK
java        Corretto 25.0.x    25              ✔
node        26.2.0             >=22            ✔
docker      29.6.1 (running)   running         ✔
aws         2.36.x             >=2.20          ✔
sam         1.164.x            >=1.130         ✔
ffmpeg      7.x                any             ✔
python      3.13.14 + Pillow   >=3.11          ✔
gh          2.96 (bryan-mcneil) logged in      ✔
```
so you never wonder "is my machine set up right?" again.
