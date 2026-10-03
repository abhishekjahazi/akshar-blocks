# Akshar Blocks – Samsung Galaxy Store listing

Everything needed to list the app in the Samsung Seller Portal (https://seller.samsungapps.com),
ready to copy and paste. After the app is live on Galaxy Store, link it in AdMob
(Apps → Akshar Blocks → App settings → App store details → Add → tick **Samsung Galaxy Store**).

## 1. Files to upload

| What | File | Notes |
|---|---|---|
| App binary | `AksharBlocks-1.3.apk` (GitHub release v1.3, or `app/build/outputs/apk/release/app-release.apk`) | Signed with the Akshar Blocks key. Package `com.aksharblocks.app`, version 1.3 (code 4). |
| Icon | `store/icon-512.png` | 512 × 512 PNG |
| Screenshots | `store/samsung/screenshot-1-home.png` … `screenshot-8-certificate.png` | 8 portrait images, 1080 × 1920 |
| Cover / promotional image (if asked) | `store/feature-graphic-1024x500.png` | 1024 × 500 |

## 2. App information

**App title**
```
Akshar Blocks: ABC Hindi 123
```

**Short description / tagline** (if asked)
```
ABC, अ आ इ ई, बारहखड़ी, मराठी and 1–100 with talking games for kids aged 3–7
```

**Description (English)**
```
Akshar Blocks helps children aged 3 to 7 learn their letters and numbers in English, Hindi and Marathi through play. Every letter is spoken out loud, so even children who can't read yet can play on their own.

WHAT YOUR CHILD LEARNS
• English ABC – capital and small letters, with a picture for each (A for Apple)
• Hindi स्वर and व्यंजन, and the बारहखड़ी (क का कि की …)
• Marathi letters (मराठी अक्षरे) with Marathi words and voice
• Numbers 1 to 100 and Hindi गिनती
• Colors, shapes and animals – named in English and Hindi
• 12 rhymes to sing along, read line by line

TEN WAYS TO PLAY
Learn • Trace letters with the right stroke order • Find it • Balloon pop • Match the picture • Memory pairs • Build words (c-a-t, ज-ल, and with matras: कि-ता-ब) • Count • Build a syllable • Animal sounds ("Who says moo?")

TODAY'S GAMES
One big button starts a short daily path of four games picked from your child's progress: new letters first, then tracing, then practice. The app brings back the letters your child finds hard.

MY NAME
Your child traces their own name, letter by letter.

REWARDS
Stars, a sticker album, a daily streak, and certificates when a section is learned.

FOR PARENTS
• A parent area behind a grown-ups question
• Up to 4 children, each with their own progress
• A report card for each child: letters known, letters to practise, a weekly play-time chart
• A daily play-time limit with a gentle "time to rest" screen
• Share certificates and report cards with family

SAFE FOR CHILDREN
• No accounts and no sign-in; names and progress stay on the phone
• The games work without internet
• One small advertising banner on the home screen only. It is set up for children: ads are child-directed and rated G, never personalized, and the app does not use the advertising ID. There are no ads inside the games and no full-screen ads.
```

**Description (Hindi – add as a second language if the portal allows)**
```
अक्षर ब्लॉक्स 3 से 7 साल के बच्चों के लिए है: अंग्रेज़ी ABC, हिंदी स्वर-व्यंजन, बारहखड़ी, मराठी अक्षर, गिनती 1–100, रंग, आकार और जानवर – सब खेल-खेल में। हर अक्षर बोलकर सुनाया जाता है।

• अक्षर लिखना सीखें (सही क्रम में)
• मात्रा वाले शब्द बनाएँ: कि + ता + ब = किताब
• 12 कविताएँ, लाइन-दर-लाइन
• अपना नाम लिखना सीखें
• रोज़ के 4 खेल, बच्चे की प्रगति के हिसाब से
• सितारे, स्टिकर और सर्टिफ़िकेट

माता-पिता के लिए: हर बच्चे का रिपोर्ट कार्ड, रोज़ खेलने के समय की सीमा। कोई अकाउंट नहीं, बच्चों की जानकारी फ़ोन पर ही रहती है। होम स्क्रीन पर सिर्फ़ एक छोटा विज्ञापन, खेलों के बीच कोई विज्ञापन नहीं।
```

**What's new in this version**
```
Version 1.3: Colors, shapes and animals, Marathi letters, rhymes, "My name" tracing, matra words, certificates, background music and a daily path of games.
```

**Keywords / tags** (if asked)
```
ABC, alphabet, Hindi, Marathi, kids, preschool, tracing, letters, numbers, barakhadi, swar, vyanjan, rhymes, learning
```

## 3. Category, age and content

| Question | Answer |
|---|---|
| Category | **Education** (choose **Kids** / children's sub-category if offered) |
| Price | **Free** |
| Age rating | **For all ages / 0+** (no violence, no scary content, no chat, no user content) |
| Is the app designed for children? | **Yes** – ages 3 to 7 |
| Contains ads? | **Yes** – Google AdMob banner on the home screen, child-directed, G-rated |
| In-app purchases? | **No** |
| Collects personal data? | **No** (child names and progress stay on the device; see privacy policy) |
| Location, camera, microphone, contacts? | **No** |
| Uses the internet? | **Yes**, only to show the advertising banner |
| Languages in the app | English, Hindi, Marathi |
| Countries | **All countries** (or India first, if you prefer) |
| Devices | **All** phones and tablets (Android 7.0+) |

## 4. Contact and links

| Field | Value |
|---|---|
| Support email | abhishekjahaziwork@gmail.com |
| Privacy policy URL | *your Google Sites page* (paste the **version 5** policy there first: `store/privacy-policy.html`) |
| Website (optional) | https://github.com/abhishekjahazi/akshar-blocks |

## 5. After it is live

1. AdMob → Apps → Akshar Blocks → App settings → **App store details → Add** → tick **Samsung Galaxy Store** → Continue → confirm `com.aksharblocks.app`.
2. AdMob then reviews the app; ads start once it is approved.
3. Every new version: raise `versionCode` in `app/build.gradle.kts`, build, and upload the new APK in the Seller Portal (same signing key).
