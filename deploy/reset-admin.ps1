<#
.SYNOPSIS
  Sets a new password (and optionally a new email) for the first administrator account, without losing any data.

.DESCRIPTION
  Use this when you cannot sign in to the admin panel. Changing ADMIN_EMAIL or ADMIN_PASSWORD in .env does nothing
  once the account exists, because the backend only creates the account on an empty database.
  Needs Docker Desktop and the stack running (docker compose up -d).

.EXAMPLE
  .\deploy\reset-admin.ps1
  .\deploy\reset-admin.ps1 -Email principal@example.com
#>
param(
    [string]$Email,
    [string]$Project,       # only for testing against another compose project
    [switch]$NoRestart      # skip restarting the backend (the restart clears a sign-in lockout)
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if ($Email -and $Email -notmatch '^[^\s''";\\]+@[^\s''";\\]+\.[^\s''";\\]+$') {
    throw "That does not look like a valid email address: $Email"
}

function Read-Plain($prompt) {
    $s = Read-Host $prompt -AsSecureString
    $p = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($p) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p) }
}

$plain = Read-Plain 'New password (at least 10 characters)'
if ($plain.Length -lt 10) { throw 'The password must be at least 10 characters.' }
if ($plain -ne (Read-Plain 'Type it again')) { throw 'The two passwords do not match.' }

# bcrypt hash, the same kind the backend uses. The password is passed on stdin so it never appears in a command line.
$raw = ($plain | docker run --rm -i httpd:2.4-alpine htpasswd -niBC 12 admin | Out-String).Trim()
if ($raw -notmatch '^admin:\$2y\$12\$') { throw "Could not create the password hash: $raw" }
$hash = $raw.Substring($raw.IndexOf(':') + 1).Replace('$2y$', '$2a$')

$compose = @('compose'); if ($Project) { $compose += @('-p', $Project) }
$setEmail = if ($Email) { ", email = '" + $Email.ToLower() + "'" } else { '' }
$sql = "update app_user set password_hash = '$hash', enabled = true$setEmail " +
       "where id = (select min(id) from app_user where role = 'ADMIN') returning email, role, enabled;"

$result = $sql | & docker @compose exec -T db psql -U gyanganga -d gyanganga -t -A -F ' | '
if ($LASTEXITCODE -ne 0 -or -not ($result -match '@')) { throw "The database was not updated. $result" }

Write-Host ''
Write-Host "Done. Administrator account: $(($result | Select-Object -First 1).Trim())" -ForegroundColor Green

if (-not $NoRestart) {
    & docker @compose restart backend | Out-Null
    Write-Host 'Backend restarted (this also clears any sign-in lockout). Wait about 30 seconds, then sign in.'
}
else {
    Write-Host 'Sign in with the new password.'
}
