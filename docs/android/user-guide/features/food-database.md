# Food Database

The food database is the foundation of MealPlan+. Every meal and diet you create is built from foods stored here.

---

## Browsing Your Foods

From the **Home** screen tap **Foods**, or navigate from any food picker screen.

- Foods are listed alphabetically with their calories per 100g.
- Use the **search bar** at the top to filter by name.
- Tap any food to view its full nutrition breakdown.

---

## Adding a Food Manually

1. Tap the **+** button.
2. Fill in the food details:

| Field | Required | Description |
|-------|----------|-------------|
| Name | Yes | Food name (e.g. "Brown Rice") |
| Calories | Yes | Per 100g |
| Protein | Yes | Grams per 100g |
| Carbohydrates | Yes | Grams per 100g |
| Fat | Yes | Grams per 100g |
| Serving size | No | Default serving amount |
| Serving unit | No | g, ml, piece, cup, tbsp, tsp, slice, scoop |
| Category | No | Used for grocery list grouping |
| Brand | No | Brand name for packaged foods |

3. Tap **Save**.

---

## Barcode Scanner

Quickly add packaged foods by scanning their barcode.

1. Tap the **barcode icon** on the Foods screen.
2. Point your camera at the product barcode.
3. The app queries **OpenFoodFacts** and **USDA** databases.
4. If found, the nutrition data is pre-filled — review and save.
5. If not found, you can fill in the details manually.

> Requires camera permission. Grant it when prompted.

---

## Online Search

Search the USDA and OpenFoodFacts databases directly.

1. Tap the **search/globe icon** on the Foods screen.
2. Type the food name (e.g. "Greek yogurt").
3. Browse results — each shows calories and a brief macro summary.
4. Tap a result to import it into your food database.

---

## Nutrition Units

All nutrition data is stored per **100g**. When you add a food to a meal, you specify the quantity and unit — the app calculates the actual nutrition for that serving.

**Supported units:**
- `g` — grams
- `ml` — millilitres
- `piece` — a whole item (egg, fruit)
- `cup` — standard cup measurement
- `tbsp` — tablespoon
- `tsp` — teaspoon
- `slice` — bread, cheese
- `scoop` — protein powder, supplements

---

## Favoriting Foods

Tap the **heart icon** on any food to mark it as a favourite. Favourites appear at the top of food picker lists for quick access.

---

## Recent Foods

The app tracks recently used foods and shows them at the top of food pickers so you can quickly re-add foods you use regularly.

---

## Editing & Deleting Foods

- Tap any food → tap the **edit icon** to modify nutrition data.
- Tap **delete** to remove it. Foods that are part of existing meals cannot be deleted until removed from those meals.
