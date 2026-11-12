# Графика домашнего рифа

Создана 30 октября 2026 встроенным инструментом imagegen, по навыку imagegen.
Ресурсы проекта: `app/res/drawable-nodpi/home_reef_buildings.png` (12 построек,
оборонительных элементов и робот) и `home_reef_decor.png` (8 украшений).
Рендерер `HomeReefArt` использует прозрачные атласы, размещает объекты в мире
и показывает тот же риф в обзоре и при строительстве.

## Промпт построек

Use case: stylized-concept. Production sprite atlas for a polished cozy underwater fish game. Create a single transparent PNG, landscape 3:2, EXACT 4 columns x 3 rows of equal square cells, 12 isolated game props, centered in each cell with 12% transparent padding, no overlap, no labels, no grid lines, no background, no floor connecting cells. Front view with slight top-down perspective, painterly clean cartoon 3D shapes, turquoise pearlescent shells, warm pale sand, coral pink, sea-glass cyan, small glowing pearl windows, gold brass accents. Readable silhouette, beautiful intricate details but not clutter. Row1 left-to-right: (1) small mound of golden sand with a tiny shell, (2) cozy shell cottage with a curled conch roof and round pearl window and door, (3) underwater coral garden planter arch with sea flowers, (4) low defensive anemone needle turret on a shell pedestal with three visible spines pointing outward. Row2: (5) beautiful taller shell mansion with two rooms and pearl balcony, (6) defensive tall coral watchtower with a brass needle launcher, (7) underwater colonnade wing with three mother-of-pearl columns and shell canopy, (8) central magnificent sea castle with shell domes, luminous pearl windows, multiple columns and grand arch gate, symmetrical, larger silhouette yet wholly inside its cell. Row3: (9) friendly little court guardian robot shaped like a round brass diving helmet with blue glass face, two articulated claw arms and little fins, isolated no pedestal, (10) elegant glowing pearl fountain with water-bubble arcs, (11) coral garden pavilion with lilac canopy and hanging sea flowers, (12) ornate sea royal gate with two seahorse motifs, pearl crown and small coral shrubs. Consistent rendering and scale. Genuine transparent alpha around each isolated sprite, no text or drop-shadow rectangles. Assets used directly in game.

## Промпт украшений

Use case: stylized-concept. Transparent sprite sheet for a cozy underwater game: EXACT 4 columns and 2 rows, eight separate ornamental garden installations, each centered within its equal cell with generous 15 percent empty transparent margins. No text, no grid, no background. Painterly polished cartoon 3D, turquoise sea glass, warm gold, coral pink, pearly ivory, glowing small lanterns. Slight top-down front view. These are eight successive replacement decorations for the same spot, growing more beautiful: row1 (1) a tiny tasteful collection of pink shells and one starfish, (2) a small coral flower planter in a scallop shell, (3) a lush fan coral garden with dangling pearl lights, (4) a beautiful glowing giant pearl in an open shell on an ornate pedestal. Row2 (5) a sea-glass bubble fountain with coral flowers, (6) an elegant coral arch covered with flowering anemones and suspended pearl lanterns, (7) a luxurious miniature underwater garden pavilion with shell roof, ornamental seahorses and glowing plants, (8) a magnificent sea-royal garden monument with mother-of-pearl columns, gold trims, a giant iridescent pearl centerpiece and cascading luminous bubbles. Keep each asset fully separated and entirely inside its own cell. True transparent alpha, no fish, no characters, no ground plane across cells, no checkerboard painted into picture.

## Коррекция атласа

Сохранить все 12 дизайнов и порядок, выровнять в сетку 4×3. Уменьшить каждый
объект до 65% ширины и высоты своей ячейки, оставив не менее 17,5% прозрачных
полей с каждой стороны. Никакие детали не должны попадать в соседнюю ячейку.
Сохранить прозрачный альфа-канал, исключить подписи и линии сетки.
