# AWS account setup (Step 06)

How the Forgotten Heroes AWS account is locked down, how the command line signs in, and how the spending
alarms work. Everything here was clicked once in the AWS web console (Q31: console first), then written as
code where code exists. No screenshots: the console changes its looks often, the words on the buttons
rarely. If a button has moved, search the page for the word in **bold**.

Plain words used below:

- **AWS account**: one billing boundary with its own resources. Ours has a 12-digit **account ID**.
- **Root user**: the e-mail + password that created the account. It can do anything and cannot be limited.
- **IAM** (Identity and Access Management): the AWS service that holds logins (**users**) and lists of
  allowed actions (**policies**).
- **MFA** (multi-factor authentication): a six-digit code from an authenticator app on your phone, asked for
  at sign-in on top of the password.
- **CLI** (command-line interface): the `aws` command in Git Bash.
- **Budget**: a monthly dollar line that e-mails you when spending crosses it.

## 1. Which plan is this account on

| Fact | Value |
|---|---|
| Account created | ≈ 27 Aug 2026 (six months before the end date below) |
| Plan | **Free plan** (credit-based): AWS cannot bill beyond the credits |
| Credits remaining on 2026-10-14 | $105.56 |
| Free plan ends | **27 Feb 2027** (6 months after creation, or when the credits run out, whichever is first) |
| Calendar reminder | **13 Feb 2027**, two weeks before: **upgrade to the Paid plan** (Q26) |

Where to look: open the **Billing and Cost Management** console (search *Billing* in the top search bar) and
choose **Free Tier** in the left menu. If that page is missing, the **Cost and Usage** box on the console
home page shows the credit balance and the days remaining. The plan rules are in
`knowledge/cost_estimator.md` §1.

While doing this step (14 Oct 2026) the terminal showed the account spending about $23 of credits a month: two
`t3.micro` servers in Oregon (`us-west-2`), left running since 31 August by an AWS beginner tutorial, plus their public IPv4
addresses and disks. They were terminated the same day. Lesson: credits hide spending, so look at **Cost Explorer**
(Billing → Cost Explorer, group by *Service*) once a month even while everything is "free"; the project's guardrail list is
`knowledge/cost_estimator.md` §5, item 12.

## 2. The root user: lock it, then stop using it

Sign in as the **Root user** (e-mail + password) for this section only.

### 2.1 Turn on MFA for the root user

1. Top-right corner: click your account name → **Security credentials**.
2. Section **Multi-Factor Authentication (MFA)** → **Assign MFA device**.
3. **Device name**: `root-phone`. Choose **Authenticator app** → **Next**.
4. Click **Show QR code**. In the authenticator app on your phone (Google Authenticator, Microsoft
   Authenticator, Authy, 1Password…), add an account and scan the code.
5. Type the code the app shows into **MFA code 1**. Wait for the next code (30 seconds) and type it into
   **MFA code 2**. Click **Add MFA**.
6. Keep a backup: click **Show secret key** before closing and store that text in your password manager.
   Losing the phone without a backup means a support ticket to get back in.

### 2.2 Let IAM users see the bill

By default only the root user may open the Billing pages. The everyday user from §3 needs them.

1. Top-right account name → **Account**.
2. Section **IAM user and role access to Billing information** → **Edit**.
3. Tick **Activate IAM access** → **Update**.

### 2.3 Alternate contacts

On the same **Account** page, section **Alternate contacts** → **Edit**. Put your name and e-mail in
**Billing**, **Operations** and **Security**, then **Update**. AWS writes to these when a bill fails, a
service has an outage, or someone reports abuse from your account.

## 3. Your everyday login: the IAM user `bryan`

**Why not IAM Identity Center?** The plan first said Identity Center (AWS's single-sign-on service). It
needs an *AWS Organization* (a group of accounts under one parent), and AWS states that creating an
Organization on a Free-plan account upgrades it to the Paid plan at once and expires the credits. So
Identity Center waits for the Paid-plan upgrade (§7, Q32). Until then one IAM user with MFA and **no
access keys** is the safe choice: the CLI signs in through the browser (§4), so no permanent secret is
ever written to disk.

### 3.1 Create the user

Still signed in as root.

1. Search *IAM* in the top bar → open **IAM** → left menu **Users** → **Create user**.
2. **User name**: `bryan`.
3. Tick **Provide user access to the AWS Management Console**. When asked who the access is for, choose
   **I want to create an IAM user** (not the Identity Center option).
4. **Console password**: **Custom password**. Type a long one and store it in your password manager.
   Untick **Users must create a new password at next sign-in** (the user is you). **Next**.
5. **Set permissions**: choose **Attach policies directly**. In the search box find and tick
   **AdministratorAccess**, then clear the box, find and tick **SignInLocalDevelopmentAccess**
   (the one that allows `aws login`). **Next**.
6. **Review and create** → **Create user**.
7. The **Retrieve password** page shows the **Console sign-in URL**
   (`https://<account-id>.signin.aws.amazon.com/console`). Bookmark it: that is where `bryan` signs in.

### 3.2 Give the user MFA

1. **Users** → click **bryan** → tab **Security credentials**.
2. Section **Multi-factor authentication (MFA)** → **Assign MFA device**.
3. **Device name**: `bryan-phone`, **Authenticator app**, then the same QR-code dance as §2.1.
4. On the same tab, the section **Access keys** must stay **empty**, forever. The review checklist checks it.

### 3.3 Switch to the new user

Sign out of root (top-right → **Sign out**). Open the bookmarked sign-in URL and sign in as **bryan** with
the password and the MFA code. Every console visit from now on is `bryan`; root is for §2 only.

## 4. The CLI: `aws login`

`aws login` (AWS CLI 2.32 and newer) opens the browser, lets you sign in as `bryan` the normal way and
hands the terminal **temporary** credentials that last up to 12 hours. Nothing permanent is saved.

A **profile** is a named block in `~/.aws/config` that remembers the region and which sign-in to use. Ours
is `fh`. Setting `AWS_PROFILE=fh` in the shell picks it without typing `--profile fh` on every command.

```bash
aws --version                       # must be 2.32 or newer
aws login --profile fh              # asks "AWS Region [us-east-1]:" → Enter; the browser opens
```

In the browser, pick the **bryan** session (sign in if asked) and allow the request. Back in the terminal
the command ends with a success line. Then:

```bash
aws configure set output json --profile fh            # answers come back as JSON
echo 'export AWS_PROFILE=fh' >> ~/.bashrc && source ~/.bashrc
aws sts get-caller-identity                           # "Arn": "arn:aws:iam::<account-id>:user/bryan"
```

`~/.aws/config` now holds:

```text
[profile fh]
login_session = arn:aws:iam::<account-id>:user/bryan
region = us-east-1
output = json
```

When a command fails with an expired-session message, run `aws login` again. `aws logout` ends the session
early. The cached credentials live in `~/.aws/login/cache`, never in the repo.

## 5. Budgets: by hand once, then as code

### 5.1 By hand (`manual-5`, then `manual-20`)

1. **Billing and Cost Management** → left menu **Budgets** → **Create budget**.
2. **Budget setup**: **Customize (advanced)**. **Budget types**: **Cost budget** → **Next**.
3. **Budget name**: `manual-5`. **Period**: **Monthly**. **Budget renewal type**: **Recurring budget**.
   **Budgeting method**: **Fixed**. Budgeted amount: `5`. Leave the scope and advanced options alone → **Next**.
4. **Add an alert threshold** four times:
   - `50`, **% of budgeted amount**, **Actual**
   - `80`, **% of budgeted amount**, **Actual**
   - `100`, **% of budgeted amount**, **Actual**
   - `100`, **% of budgeted amount**, **Forecasted**

   **Email recipients** on each: your e-mail. → **Next**.
5. **Attach actions**: none → **Next** → check the summary → **Create budget**.
6. Repeat for `manual-20` with the amount `20`.

### 5.2 Verify the e-mail address

Since 2025 a new recipient receives nothing until it is verified once per account. Open the e-mail from an
`@aws.com` address and click its link while signed in as `bryan`; the link is valid for 12 hours. On the
budget's page the recipient then shows **Active**. One verification covers every budget in the account,
including the ones the template creates next.

### 5.3 As code: `infra/budgets.yaml`

CloudFormation turns a YAML **template** into a **stack** (one named deployment that it creates, updates
or deletes as a unit). The e-mail is a **parameter**, typed at deploy time, so it never enters the public repo.

```bash
aws cloudformation deploy --stack-name forgotten-heroes-budgets --template-file infra/budgets.yaml \
  --parameter-overrides Email=<your-email>
aws budgets describe-budgets --account-id "$(aws sts get-caller-identity --query Account --output text)" \
  --query 'Budgets[].BudgetName'
```

The second command lists `forgotten-heroes-5` and `forgotten-heroes-20` next to the two manual ones. To change
an alarm later, edit the file and run the first command again; CloudFormation applies the difference.

### 5.4 Delete the hand-made budgets

**Budgets** → tick `manual-5` → **Actions** → **Delete** → confirm. Same for `manual-20`. The template is
now the only description of the alarms.

Forecast alerts stay silent until the account has about five weeks of usage history; the actual alerts
work from day one.

## 6. The cost allocation tag (a reminder for Step 34)

Every resource the SAM template creates carries the tag `project=forgotten-heroes`. A tag key becomes a
filter in Cost Explorer only after it is **activated** on **Billing → Cost allocation tags → User-defined
cost allocation tags**, and it appears on that page only up to 24 hours after a tagged resource has
produced a bill line. Step 34 deploys the first such resource; activate `project` the day after.

## 7. When the Free plan ends (27 Feb 2027)

Two weeks before the date in §1: **Billing → Free Tier → Upgrade** to the Paid plan (Q26). Then, and only
then: enable **IAM Identity Center** (organization instance), move the CLI to `aws configure sso`, and
delete the IAM user `bryan`. The Step 77 checklist repeats this.

## 8. Checklist

- [ ] Root user has MFA; its secret key is backed up.
- [ ] **Activate IAM access** is on; alternate contacts are filled in.
- [ ] IAM user `bryan` exists with MFA, `AdministratorAccess` + `SignInLocalDevelopmentAccess`, and **no access keys**
      (`aws iam list-access-keys --user-name bryan` prints an empty list).
- [ ] `aws sts get-caller-identity` shows `user/bryan`; profile `fh` is `us-east-1`, output `json`.
- [ ] Stack `forgotten-heroes-budgets` exists; `manual-5` and `manual-20` are deleted; the recipient is **Active**.
- [ ] §1 is filled in and the calendar reminder exists.
