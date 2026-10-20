# QR check-in - cu-chi

Generated: 2026-10-20 11:51:23 +07:00

- Mode: **SIGNED_STATIC**
- Static payload format: siteCode:stationCode:0:signature.
- ST01-ST06 are the six Cu Chi stations.

> QR is signed with HMAC-SHA256. Do not change the BE secret after printing; changing it requires regenerating all QR files.

Never commit the secret. manifest.json contains only signed payloads for internal verification.