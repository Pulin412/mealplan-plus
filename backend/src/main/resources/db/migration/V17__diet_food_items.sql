-- V17: diet_food_items — allows adding individual foods directly into a diet slot
--      without wrapping them in a meal first.
--      Complements the existing diet_meals table (which links whole meals into slots).

CREATE TABLE diet_food_items (
    id       BIGSERIAL        PRIMARY KEY,
    diet_id  BIGINT           NOT NULL REFERENCES diets(id) ON DELETE CASCADE,
    food_id  BIGINT           NOT NULL REFERENCES foods(id),
    slot     VARCHAR(50)      NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 1,
    unit     VARCHAR(50)      NOT NULL DEFAULT 'GRAM'
);

CREATE INDEX idx_diet_food_items_diet_id ON diet_food_items (diet_id);
