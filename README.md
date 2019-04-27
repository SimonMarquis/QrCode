<div align="center">
  <img src="art/ic_launcher_web.png" alt="" width="96px" height="96px">
</div>
<h3 align="center">QrCode</h3>
<p align="center">
  Scan and create QR Codes with ease.<br>
  <img src="https://travis-ci.com/SimonMarquis/QrCode.svg?branch=master" alt="Build Status">
</p>

<br>

| <a href="https://play.google.com/store/apps/details?id=fr.smarquis.qrcode">Android</a> | <a href="https://simonmarquis.github.io/QrCode/">Web</a> |
|---|---|
| <img src="art/android_scanning.png" width="200px" title="Scanning"> <img src="art/android_scanned.png" width="200px" title="Scanned"> | <img src="art/web.png" width="360px" title="Web"> |
| Barcode processors:<ul><li><a href="https://firebase.google.com/docs/ml-kit/android/read-barcodes#before-you-begin" title="Apart from the initial Firebase ML Kit Barcode Model download">✈️</a> <a href="https://firebase.google.com/docs/ml-kit/read-barcodes">Firebase ML Kit</a></li><li>✈️ <a href="https://github.com/zxing/zxing/">ZXing</a></li></ul> | QR Code generators:<ul><li>✈️ <a href="https://github.com/davidshimjs/qrcodejs">QRCode.js</a></li><li>🌎 <a href="https://zxing.appspot.com/generator/">ZXing</a></li><li>🌎 <a href="https://developers.google.com/chart/infographics/docs/qr_codes">Google Charts</a></li></ul> |

#### Devices without touchscreen

> Mostly based on [android.hardware.touchscreen](https://developer.android.com/reference/android/content/pm/PackageManager.html#hasSystemFeature(java.lang.String))

The scanning behavior is simplified and it will automatically:
- Copy content to clipboard
- Open web links
- Open deeplinks (geo, mail, tel, sms, etc.)

Non-exhaustive list of devices without touch screen support:
- Google: `Glass`, `Glass Enterprise Edition`
- Vuzix: `Blade®`, `M300`, `M300XL`
- Realwear: `HMT-1™`, `HMT-1Z1™`
- …

#### Barcode formats

- Aztec
- Codabar
- Code 39, Code 93, Code 128
- Data Matrix
- EAN-8, EAN-13
- ITF
- MaxiCode
- PDF417
- QR Code
- UPC-A, UPC-E

#### Content types

- Text
- WiFi
- Url
- Sms
- GeoPoint
- ContactInfo
- Email
- Phone
- CalendarEvent

## License

```
Copyright 2019 Simon Marquis

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
