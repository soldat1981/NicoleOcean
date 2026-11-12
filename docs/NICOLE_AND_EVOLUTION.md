# Николь, посетители аквариума и превращения — версия 1.3

## Фото Николь

Из пользовательского архива `Photos-1-001.zip` извлечены ровно три фотографии: `IMG_7061.JPG`, `IMG_7062.JPG`, `IMG_7063.JPG`. Оригиналы находятся только в игнорируемой папке `.tools/references/nicole/`; в APK они не включены.

Встроенным ImageGen создан `app/res/drawable-nodpi/nicole_faces.png`: прозрачный атлас из трёх вырезанных голов с разными выражениями лица, в порядке исходных фото. Убраны фон, руки, диплом, бумажная шапочка и плечи. Сами лица сохранены фотографическими. Изображение скопировано в проект без дальнейшего изменения пикселей.

При каждом появлении независимо выбираются одно из трёх лиц и один из трёх вариантов: без снаряжения, плавательные очки, плавательная маска. Очки и маска рисуются средствами Android Canvas поверх глаз. Получается девять комбинаций.

Николь занимает 1,5 ширины экрана. Лицо полупрозрачное, неподвижное, плавно появляется и исчезает через 11–15 секунд. Это слой **за** рыбами, предметами, декорациями и интерфейсом. Она не ловит, не лечит, не наносит урон и не меняет поведение рыб.

## Частота посетителей

Николь и Босси используют один планировщик и чередуются, поэтому их частота одинаковая и пересечения исключены. Первый посетитель выбирается случайно. Первое появление возможно после начальных 20 секунд игры; после ухода каждого посетителя проходит случайная пауза 18–28 секунд, затем появляется другой. В среднем каждый тип встречается примерно раз в минуту-полторы, в зависимости от длительности визита Босси.

Посетители могут появляться и у лагуны, однако её защита от урона Босси сохраняется. Первоначальная защита для освоения управления тоже остаётся.

Все таймеры, выбранное лицо/снаряжение, очередность и фаза появления записываются в сохранение v4. Сохранения v1–v3 читаются; прежнее длинное ожидание кота сокращается без потери героя и достижений.

## Босси

Атлас `bossi_atlas.png` сохранён. Каждая его часть теперь обрезается до прозрачных границ при загрузке и увеличивается с одинаковым масштабом по обеим осям. Для лап и хвоста больше нет независимого растягивания ширины/высоты.

Дополнительный PNG `app/res/drawable-nodpi/bossi_looking_down.png` создан встроенным ImageGen по фото кота и существующему атласу: он смотрит вниз на добычу. Эта поза используется при ловле лапами. Когда Босси смотрит прямо на нас сверху, исходная мордочка рисуется перевёрнутой на 180 градусов. Старые русские фразы, рывки, разбегание рыб и безопасный неподвижный хвост сохраняются.

## Эволюция героя

Максимальный уровень повышен до 45. Форма меняется автоматически после каждых пяти уровней; выбор и отдельное подтверждение не нужны.

| Уровни | Персонаж |
|---|---|
| 1–5 | Рыбка-искорка |
| 6–10 | Русалка |
| 11–15 | Рифовая акула |
| 16–20 | Акула-молот |
| 21–25 | Манта |
| 26–30 | Дельфин |
| 31–35 | Косатка |
| 36–40 | Нарвал |
| 41–45 | Морской дракон |

Новые силуэты нарисованы программно в `HeroArt.java` в стиле исходной игры. Они поворачиваются по направлению движения. В интерфейсе показаны текущая форма и цель следующего превращения. Появляющиеся новые противники учитывают поздние уровни героя, а уже существующие рыбы не повышают уровень внезапно. Размер ограничен, чтобы герой не занял весь экран.

## Промпты встроенного ImageGen

### Три лица

```text
Use case: background-extraction, identity-preserve. Create one transparent PNG sprite atlas containing exactly THREE separate photographic face/head cutouts of the SAME girl Nicole, taken from the three reference photographs, in the same order. This is for a personal family aquarium game: her huge face will appear as a passive background visitor behind the fish. Preserve her real identity, childhood age, skin texture, real face proportions, teeth, eyes and the DISTINCT facial expression in EACH individual photograph. Do not merge or average the three expressions. Left cutout must match photo 1, middle cutout photo 2, right cutout photo 3. Extract only each head/face with ears and natural hair framing the face, ending just below the chin, removing all scenery, shoulders, hands, certificate and graduation paper crown. Reconstruct only the small hair/forehead area previously covered by the paper crown naturally from her visible hair; do not alter facial features. NOT a cartoon or a new portrait, NOT a beauty retouch, NOT an adult. Arrange in THREE equal-width columns across one wide horizontal canvas, all faces similarly scaled, upright, front-facing as in their reference, entirely inside their column with transparent margins all around and clear separation; each should fill most of its column. Genuine alpha transparency outside the heads, clean natural cutout hair edges, no rectangular photo backgrounds. No swimming equipment yet, no hats, no body, no props, no labels, no text or watermark. Eyes remain clearly visible for runtime swimming goggles overlays.
```

### Босси смотрит вниз

```text
Create a single transparent PNG game sprite: only the head of Bossi, the same real fluffy brown/black tabby cat with white chin, yellow-green eyes and M forehead stripes in photo 1. Image 2 is the existing game sprite style/identity reference, not a target atlas to reproduce. New pose: the cat is above an aquarium, actively looking DOWN into the water toward a fish, head tilted forward and downward about 45 degrees. Camera is inside the aquarium below/front, so we can see his rounded forehead, ears, downward-aimed gaze and muzzle pointing toward the bottom edge. Clearly different from a straight-on portrait: both pupils focus low, lowered muzzle, gentle concentrated curious expression. Keep Bossi's characteristic real markings and fluffy cheeks, polished natural illustrative look matching existing head, no scary expression, no teeth or claws. One complete head including ears with generous transparent margins; no paws, body, objects, water, scenery, text or watermark. Genuine transparent alpha background. Upright canvas, ears toward top, nose toward bottom. This pose will appear above reaching paws.
```
