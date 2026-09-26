# ABCD by Kirti – Google Play upload guide

Everything you need to publish, in the order Play Console asks for it.

## 1. Files in this folder

| Play Console field | File |
|---|---|
| App bundle (upload in *Release*) | `../app/build/outputs/bundle/release/app-release.aab` |
| App icon (512 × 512) | `icon-512.png` |
| Feature graphic (1024 × 500) | `feature-graphic-1024x500.png` |
| Phone screenshots (upload all 4) | `screenshot-1-menu.png` … `screenshot-4-match.png` |
| Privacy policy (put online, paste the link) | `privacy-policy.html` |

To rebuild the images after changing them: edit the HTML in `src/` and ask Claude to re-render.

## 2. Store listing text (copy and paste)

**App name** (30 characters max)
```
ABCD by Kirti – Learn Alphabet
```

**Short description** (80 characters max)
```
Learn ABC and Hindi अ आ इ ई, क ख ग with talking letters, pictures and games!
```

**Full description**
```
ABCD by Kirti helps young children learn the English alphabet and Hindi letters (स्वर and व्यंजन) by playing.

THREE ALPHABETS
• English A to Z
• Hindi vowels – अ आ इ ई उ ऊ … अं अः
• Hindi consonants – क ख ग घ … क्ष त्र ज्ञ

Every letter is a big, colorful toy block that children can tap. A friendly voice says each letter and word out loud in English or Hindi ("अ से अनानास"), so kids learn what letters look like and how they sound.

FOUR GAMES IN ONE
• Learn A–Z – Flip through the alphabet. See "A is for Apple" with a big picture, and hear it spoken.
• Find it – Listen to "Find the letter B!" or "क ढूंढो!" and tap the right block.
• Balloons – Letter balloons float up. Pop them in alphabet order.
• Pictures – See a picture and choose the letter its word starts with.

MADE FOR LITTLE HANDS
• Big buttons that are easy to tap
• Gentle help when an answer is wrong – no fail screens
• Stars and confetti for every right answer – stars are saved
• Works offline – no internet needed

SAFE FOR KIDS
• No ads
• No in-app purchases
• No sign-up and no data collected
• No internet access at all

Great for toddlers, preschool and kindergarten children aged 2 to 6.

The Hindi voice uses the phone's built-in Hindi text-to-speech. If a phone doesn't have it, the app shows a button to install it.
```

**Category:** Education (or Games → Educational)
**Tags:** Alphabet, Kids, Education, Preschool

## 3. App content forms (Policy → App content)

| Form | Answer |
|---|---|
| Privacy policy | Link to your published `privacy-policy.html` |
| Ads | **No**, my app does not contain ads |
| App access | All functionality is available without special access |
| Content rating | Category: *Reference, News, or Educational*. Answer **No** to every violence / fear / sexual / language / drugs / gambling / user-interaction / sharing-location question. Expected result: Everyone / 3+ |
| Target audience | Ages **5 and under** and **6–8**. Choose that the app is designed for children → Families policy applies |
| Data safety | Does your app collect or share user data? **No**. (No internet permission, no SDKs that collect data.) |
| Government apps / Financial features / Health | Not applicable |
| News app | No |

## 4. Release steps

1. **Testing → Closed testing** → create a track → upload `app-release.aab`.
2. Add at least **12 testers** (their Gmail addresses) and send them the opt-in link. They must stay opted in for **14 days**. (Required for new personal developer accounts; check the current numbers in Play Console.)
3. After 14 days: **Dashboard → Apply for production access**.
4. **Production → Create new release** → upload the same `.aab` → roll out.

## 5. Updating the app later

1. In `app/build.gradle.kts`, increase `versionCode` (1 → 2 → 3 …) and update `versionName` ("1.1", …).
2. Build: `./gradlew bundleRelease` (or Android Studio: *Build → Generate Signed App Bundle*).
3. Upload the new `.aab` in a new release.

## 6. Signing key – KEEP SAFE

- Key file: `C:\Users\ABCD\keystores\kirtigames-upload.jks`
- Password: in `C:\Users\ABCD\keystores\keystore-password-BACKUP.txt` (same as `keystore.properties` in the project)
- Back up **both files** to two places (for example Google Drive and a USB stick).
- When Play Console offers **Play App Signing**, accept it (recommended). Then if the upload key is ever lost, Google support can reset it.
