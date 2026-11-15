# Privacy Policy for KnowNow

**Last updated:** November 16, 2026

KnowNow ("the app") is an offline reader for publicly available ServiceNow documentation sourced from the [ServiceNowDocs](https://github.com/ServiceNow/ServiceNowDocs) repository. This policy explains what information the app accesses, how it is used, and what is stored on your device.

## Summary
- KnowNow does **not** require an account and does **not** collect, sell, or share personal data.
- The app does **not** include advertising, analytics, or crash-reporting SDKs.
- Documentation you download and study data you create (bookmarks, notes, highlights, progress, settings) are stored **locally on your device**.

## Information the app accesses and uses

### Network access
When you use features that require connectivity, the app connects to GitHub services to retrieve documentation metadata and content, including:
- `https://api.github.com` (for example, to list available releases/branches)
- `https://raw.githubusercontent.com` (to fetch publication indexes and markdown documents you choose to download)

These requests are made only to provide the app’s core functionality (browsing and downloading documentation for offline reading). Your network provider and GitHub may process standard connection data (such as IP address) according to their own policies.

### Optional GitHub token
If you choose to provide a GitHub personal access token in Settings, the token is:
- stored locally on your device using Android DataStore,
- used only to authenticate requests to GitHub APIs (for example, to help with rate limits),
- never transmitted by the app to any server other than GitHub as part of those API requests.

You can remove the token at any time by clearing it in Settings or uninstalling the app.

### Locally stored data
KnowNow stores the following data on your device to provide offline and study features:
- **Downloaded content:** markdown files and related metadata for releases/topics you choose to download.
- **Library metadata:** information about releases, publications, and documents.
- **Study data:** bookmarks, reading progress, highlights, notes (including tags and snapshots used to keep notes useful if source content changes), flashcards/study state, and reading streak/timer-related preferences where applicable.
- **App preferences:** theme, font/reading settings, download preferences (such as allowing downloads on metered networks), and other in-app settings.

All of this data remains on your device unless you choose to share it (for example, using the system share/export features) or back up your device using system-level backup services outside the app’s control.

## Data sharing
KnowNow does not share your personal data with the developer or third parties. The app only communicates with GitHub endpoints as needed to fetch documentation content/metadata that you request.

## External links
The app may open external websites (such as `github.com` or `servicenow.com`) in your browser when you tap relevant links (for example, source, canonical documentation, or policy links). Those websites are governed by their own privacy policies and terms.

## Permissions
Depending on your Android version and usage, the app may use permissions such as:
- **Internet / network state:** to browse and download documentation and check connectivity conditions.

The app does not request access to contacts, location, camera, microphone, or SMS.

## Data retention and deletion
- Locally stored data is retained on your device until you delete it within the app (where supported), clear the app’s storage/data, or uninstall the app.
- Uninstalling the app removes data stored in the app’s private storage on your device (subject to Android behavior and any separate device backups you maintain).

## Children’s privacy
KnowNow is a general-purpose documentation reader and is not directed to children. The app does not knowingly collect personal information from anyone.

## Security
The app stores data in its private on-device storage. No method of electronic storage is completely secure, but KnowNow is designed to minimize data exposure by keeping user content local and avoiding third-party analytics/advertising services.

## Changes to this policy
This Privacy Policy may be updated from time to time. Updates will be posted in this file in the project repository with a revised "Last updated" date.

## Contact
For questions or concerns about this Privacy Policy or the app, please open an issue or contact the maintainer via the project repository:
https://github.com/hemanth-jarajapu/KnowNow
