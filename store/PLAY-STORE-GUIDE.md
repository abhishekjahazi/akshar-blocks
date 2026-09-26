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
ABC, अ आ इ ई, बारहखड़ी and 1–100 with talking games, tracing and stickers!
```

**Full description**
```
ABCD by Kirti helps young children learn English letters, Hindi letters and numbers by playing, with a friendly voice that speaks English and Hindi.

SIX THINGS TO LEARN
• English A to Z – "A is for Apple"
• Hindi vowels (स्वर) – अ आ इ ई … अं अः
• Hindi consonants (व्यंजन) – क ख ग … क्ष त्र ज्ञ
• बारहखड़ी – क का कि की कु कू … for every consonant
• Numbers 1 to 100
• हिंदी गिनती – १ से १०० तक

GAMES
• Learn – big letters, pictures and the voice
• Trace – write letters with a finger; English letters show the right stroke order
• Find it – "Find the letter B!" or "क ढूंढो!"
• Balloons – pop the letters in order
• Pictures – which letter does the word start with?
• Build – क + ? = की: choose the right matra
• Count – how many apples? The app counts along with your child

LEARNS WITH YOUR CHILD
• Letters your child finds hard come up more often
• Letters that get mixed up (like b and d, or ि and ी) are practised together
• A sticker for every 5 stars, and a daily streak

FOR PARENTS
• Up to 4 children, each with their own progress
• See what each child knows and which letters need practice
• Slower voice option for the youngest

MADE FOR LITTLE HANDS
• Big buttons, gentle help when an answer is wrong, no fail screens
• Works offline – no internet needed
• Works on Android 7 and newer

SAFE FOR KIDS
• No ads
• No in-app purchases
• No sign-up and no data collected
• No internet access at all

Great for toddlers, preschool and kindergarten children aged 2 to 6.

The English and Hindi voice is built into the app, so it works on every phone, even without a Hindi voice installed.
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
