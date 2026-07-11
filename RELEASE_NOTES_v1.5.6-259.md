## Zindan 1.5.6 (259)

**APK:** `Zindan-1.5.6-(259)-debug.apk` (debug-signed)

### Highlights

- Detects Always-on VPN / lockdown VPN using public Android APIs where Zindan has profile/device-owner access.
- Warns the user that Always-on VPN can interfere with correct operation and offers to open Android VPN settings.
- Falls back to a manual-check instruction when Android does not expose the setting to Zindan in the current profile.
- Does not try to disable Always-on VPN automatically; no root, hidden APIs, or extra permissions.
- Avoids repeated nagging by remembering the last warned state and prompt time.
- User guide updated with the new behavior.

### Install

Download the APK below. Update over an existing install in **both** profiles (personal first, then work) — no need to recreate the work profile when `versionCode` increases.

See [CHANGELOG.md](https://github.com/GiorgioVik/zindan/blob/main/CHANGELOG.md) and [USER_GUIDE.md](https://github.com/GiorgioVik/zindan/blob/main/USER_GUIDE.md).