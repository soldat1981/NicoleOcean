# Босси: графика, звук и правила появления

> В версии 1.3 частота и отрисовка изменены: общее чередование с Николь, паузы 18–28 секунд, сохранение пропорций частей и отдельный взгляд вниз. См. [NICOLE_AND_EVOLUTION.md](NICOLE_AND_EVOLUTION.md). Описание ниже относится к первоначальной версии 1.2.

## Ресурсы

- `app/res/drawable-nodpi/bossi_atlas.png` — один PNG с настоящим прозрачным alpha-каналом, 1254×1254. Создан встроенным ImageGen по двум фотографиям пользователя: `IMG_5362.jpg` и `IMG_1601.JPG`.
- Атлас содержит мордочку, две передние лапы с белыми носочками и полосатый пушистый хвост. Исходные фото не включены в APK.
- Исходный результат ImageGen скопирован в проект без изменения пикселей. `BossiArt.java` выбирает части атласа прямоугольниками при отрисовке.
- `app/assets/audio/bossi_no.wav` — «Босси, нельзя!».
- `app/assets/audio/bossi_away.wav` — «Босс, отойди от аквариума.».
- `app/assets/audio/bossi_food.wav` — «Босс, пойдём кушать!».
- Эти три фразы синтезированы локально установленным голосом Microsoft Irina и включены в APK. Во время игры нет сетевого TTS и загрузки языковых пакетов. Для повторного создания файлов — `tools/generate-bossi-audio.ps1`.
- `app/assets/audio/bossi_meow.wav` — один короткий синтезированный кошачий звук; это не запись реального кота. Генератор: `tools/generate-meow.py`.
- Все звуки Босси подчиняются общей настройке звука. Воспроизведение останавливается при паузе, сворачивании и завершении плавания.

## Частота и поведение

Первое появление возможно через 75–115 секунд плавания; следующие — через 120–210 секунд после ухода. В безопасной лагуне визит откладывается. Примерно 25% визитов — спокойный хвост, остальные распределяются между левой лапой, правой лапой, головой и головой с лапами.

Охота: плавное появление → предупреждение и отметка цели (1,15 секунды) → бросок → короткая пауза. Цель фиксируется до броска, поэтому рывком можно увернуться. После 3–4 попыток звучит одна из трёх фраз, затем Босси уходит. Рыбы во время визита быстро разбегаются в стороны и не преследуют героя.

Попадание кота снимает 14 HP в обычном режиме и 7 HP в спокойном, с общей защитой от повторного урона. Один бросок не может ударить несколько раз. Лагуна и укрытия сохраняют защитные свойства.

Хвост: одинокое мяу → плавное опускание → 9 секунд неподвижно в воде → плавное извлечение. Хвост не ловит рыб, не наносит урона и не вызывает паники.

Фаза, таймер, позиция, цель броска, число попыток, выбранная фраза и ожидание следующего появления сохраняются вместе с миром. После загрузки звук из прошлого кадра не повторяется; само событие продолжается после снятия паузы.

## Промпт, переданный встроенному ImageGen

```text
Use case: style-transfer. Create ONE production-ready transparent PNG sprite atlas for a cozy 2D Android aquarium game. Both attached photos are identity references of the SAME real cat Bossi, not backgrounds to reproduce. Preserve Bossi's recognizable fluffy brown/charcoal tabby fur, black forehead M stripes, large yellow-green eyes, broad rounded muzzle, white chin and chest, white mitten toes and fluffy dark striped tail. Convert into polished cute hand-painted/cartoon game artwork, clean rounded forms, gentle curious/mischievous expression, no frightening teeth or claws. Exactly 4 isolated parts, arranged in a strict equal 2x2 grid on a square transparent canvas, each entirely inside its own quadrant with ample empty transparent margin and no touching other quadrants: TOP LEFT: front-facing complete head only with both ears and fluffy cheeks, upright orientation, looking downward slightly as if peering over an aquarium edge, ears at top and chin at bottom; TOP RIGHT: one complete long front foreleg and white-mitten paw, shoulder end at top and paw toes pointed straight down, front view of fur side, no pads; BOTTOM LEFT: the other complete front foreleg and paw, mirrored matching companion also vertical toes down; BOTTOM RIGHT: one long fluffy dark tabby tail hanging straight down with a gently curved tip, base at top and tip at bottom. The forelegs must be long enough for reaching-down animation; head is roughly square. No full body, no aquarium, no water, no props, no text, no labels, no border, no watermark. Genuine transparent alpha background everywhere outside the parts. The four parts will be read separately by quadrant in the game.
```
