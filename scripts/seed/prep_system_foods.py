import json, os

foods = json.load(open('data/ingredients.json'))['foods']

# 1) Fix per-piece eggs -> per-100g (USDA)
EGG_FIX = {
    'Egg Whole': dict(calories=155, protein=13.0, carbs=1.1, fat=11.0),
    'Egg White': dict(calories=52,  protein=11.0, carbs=0.7, fat=0.2),
}

# 2) Drop these (supplements, not foods)
DROP = {'Ashwagandha', 'Shilajeet', 'Sea Cod Liver Oil', 'Vitamin D3', 'Vitamin E (Evion 400)'}

# 3) Category per food
CAT = {
 # Vegetable
 'Potato':'Vegetable','Onion':'Vegetable','Tomato':'Vegetable','Capsicum':'Vegetable','Carrot':'Vegetable','Beetroot':'Vegetable',
 'Broccoli':'Vegetable','Cabbage':'Vegetable','Cauliflower':'Vegetable','Mushroom':'Vegetable','Zucchini':'Vegetable',
 'Lauki':'Vegetable','Bhindi':'Vegetable','French Beans':'Vegetable','Lettuce':'Vegetable','Cucumber':'Vegetable',
 'Spinach':'Vegetable','Corn':'Vegetable','Matar':'Vegetable','Red Bell Pepper':'Vegetable','Yellow Bell Pepper':'Vegetable',
 # Fruit
 'Banana':'Fruit','Blueberry':'Fruit','Cantaloupe':'Fruit','Kharbooja':'Fruit','Cranberry':'Fruit','Grapes':'Fruit',
 'Green Apple':'Fruit','Red Apple':'Fruit','Kiwi':'Fruit','Mango':'Fruit','Papaya':'Fruit','Pear':'Fruit','Plum':'Fruit',
 'Raspberry':'Fruit','Strawberry':'Fruit','Watermelon':'Fruit','Avocado':'Fruit','Black Dates':'Fruit','Raisin':'Fruit',
 'Black Olives':'Fruit',
 # Grain
 'BB Toast':'Grain','Corn Flakes':'Grain','Jowar Flour':'Grain','Maida':'Grain','Oats':'Grain','Pasta Penne':'Grain',
 'Poha Raw':'Grain','Ragi Flour':'Grain','Sourdough Bread':'Grain','Spaghetti':'Grain','Wheat Flour':'Grain',
 'Large Tortilla':'Grain','White Rice':'Grain',
 # Legume
 'Black Chickpea':'Legume','Boiled Rajma':'Legume','Chana Dal':'Legume','Chickpea':'Legume','Chickpea Raw':'Legume',
 'Gram Flour':'Legume','Green Moong':'Legume','Yellow Moong Dal':'Legume',
 # Protein
 'Boneless Chicken':'Protein','Egg Whole':'Protein','Egg White':'Protein','Tofu':'Protein','Soya Chunks':'Protein',
 'Whey Isolate':'Supplement',
 # Dairy
 'Milk':'Dairy','Dahi':'Dairy','Paneer':'Dairy','Cheese Slice':'Dairy','Fresh Cream':'Dairy','Fresh Cream Cooking':'Dairy',
 # Nuts & Seeds
 'Almonds':'Nuts & Seeds','Cashew':'Nuts & Seeds','Walnuts':'Nuts & Seeds','Pista':'Nuts & Seeds',
 'Peanut Butter':'Nuts & Seeds','Chia Seeds':'Nuts & Seeds','Roasted Melon Seeds':'Nuts & Seeds',
 # Fats & Oils
 'Olive Oil':'Fats & Oils','Mustard Oil':'Fats & Oils','Desi Ghee':'Fats & Oils','Butter':'Fats & Oils',
 'Mayo':'Fats & Oils','Veggie Mayo':'Fats & Oils',
 # Beverage
 'Black Coffee':'Beverage','Luke Warm Water':'Beverage','Almond Milk':'Beverage',
 # Condiment
 'Soy Sauce':'Condiment','Tomato Ketchup':'Condiment','Hot & Sweet Chilli Sauce':'Condiment',
 'Pizza Pasta Sauce':'Condiment','Honey':'Condiment','Maple Syrup':'Condiment','Cinnamon Powder':'Condiment','Pink Salt':'Condiment',
 # Sweet
 'Chocolate Ice Cream':'Sweet','Brownie':'Sweet','Oreo Biscuits':'Sweet',
}

out, unmapped = [], []
for f in foods:
    n = f['name']
    if n in DROP: continue
    m = dict(f)
    if n in EGG_FIX: m.update(EGG_FIX[n])
    cat = CAT.get(n)
    if cat is None: unmapped.append(n)
    out.append(dict(name=n, category=cat or 'Other',
                    calories=m['calories'], protein=m['protein'], carbs=m['carbs'], fat=m['fat']))

json.dump(out, open(os.path.join(os.path.dirname(__file__), 'prepared_foods.json'),'w'), indent=2)

from collections import Counter, defaultdict
print(f"prepared {len(out)} foods (from {len(foods)}; dropped {len(DROP)} supplements)")
if unmapped: print("!! UNMAPPED ->Other:", unmapped)
print("\ncounts by category:")
for c,n in Counter(x['category'] for x in out).most_common(): print(f"  {c:<14} {n}")
print("\n--- grouped preview (per 100g) ---")
by = defaultdict(list)
for x in out: by[x['category']].append(x)
for c in ['Vegetable','Fruit','Grain','Legume','Protein','Dairy','Nuts & Seeds','Fats & Oils','Beverage','Condiment','Sweet','Other']:
    if c not in by: continue
    print(f"\n[{c}]")
    for x in sorted(by[c], key=lambda i:i['name']):
        print(f"  {x['name']:<22} {x['calories']:>4}kcal  P{x['protein']:<5} C{x['carbs']:<5} F{x['fat']}")

# ── unit overrides: naturally count-based foods -> PIECE + grams per piece ──
PIECE = {'Egg Whole': 50, 'Egg White': 33, 'BB Toast': 30, 'Large Tortilla': 45,
         'Cheese Slice': 20, 'Oreo Biscuits': 11}

def _num(x):
    f = float(x); return str(int(f)) if f == int(f) else str(f)

_rows = []
for _x in out:
    _u = 'PIECE' if _x['name'] in PIECE else 'GRAM'
    _gpp = _num(PIECE[_x['name']]) if _x['name'] in PIECE else 'NULL'
    _n = _x['name'].replace(chr(39), chr(39) * 2)
    _c = _x['category'].replace(chr(39), chr(39) * 2)
    _rows.append(f"  (gen_random_uuid(), '{_n}', '{_c}', {_num(_x['calories'])}, {_num(_x['protein'])}, "
                 f"{_num(_x['carbs'])}, {_num(_x['fat'])}, '{_u}', {_gpp}, true, true, false, now(), now())")

_sql = (
 '-- V3: seed shared system foods (macros per 100g, with category). ' + str(len(out)) + ' items.\n'
 '-- Generated by scripts/seed/prep_system_foods.py from data/ingredients.json\n'
 '-- (eggs corrected to per-100g; 5 supplements excluded; count-based foods use PIECE + grams_per_piece).\n'
 '-- System foods: firebase_uid NULL, is_system_food=true, verified=true; visible to every user.\n'
 'INSERT INTO public.foods\n'
 '  (server_id, name, category, calories_per100, protein_per100, carbs_per100, fat_per100,\n'
 '   unit, grams_per_piece, is_system_food, verified, is_favorite, created_at, updated_at)\n'
 'VALUES\n' + ',\n'.join(_rows) + ';\n'
)
open('backend/src/main/resources/db/migration/V3__seed_system_foods.sql', 'w').write(_sql)
print('regenerated V3 with', len(_rows), 'rows;', len(PIECE), 'piece-based')
