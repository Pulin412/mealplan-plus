-- V1: Squashed baseline schema for MealPlan+ v2.
-- Generated from pg_dump of the full V1..V24 migration chain (validated against Postgres 16).
-- The original 24 incremental migrations were removed here; they remain in git history
-- (see the commit that introduced this file). Flyway runs ONLY on Postgres (local/docker/prod);
-- the H2 dev profile builds its schema from JPA entities instead.

--
-- PostgreSQL database dump
--


-- Dumped from database version 16.14 (Debian 16.14-1.pgdg12+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg12+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- Name: activity_level_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.activity_level_enum AS ENUM (
    'SEDENTARY',
    'LIGHTLY_ACTIVE',
    'MODERATELY_ACTIVE',
    'VERY_ACTIVE',
    'EXTRA_ACTIVE'
);


--
-- Name: gender_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.gender_enum AS ENUM (
    'MALE',
    'FEMALE',
    'OTHER'
);


--
-- Name: goal_type_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.goal_type_enum AS ENUM (
    'LOSE_WEIGHT',
    'MAINTAIN',
    'GAIN_MUSCLE',
    'GAIN_WEIGHT'
);


--
-- Name: tag_entity_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tag_entity_type AS ENUM (
    'DIET',
    'EXERCISE',
    'MEAL',
    'FOOD'
);


--
-- Name: units_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.units_enum AS ENUM (
    'METRIC',
    'IMPERIAL'
);


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: custom_metric_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custom_metric_types (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    unit character varying(50) NOT NULL,
    icon character varying(100),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: custom_metric_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.custom_metric_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: custom_metric_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.custom_metric_types_id_seq OWNED BY public.custom_metric_types.id;


--
-- Name: daily_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.daily_logs (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    date date NOT NULL,
    notes character varying(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: daily_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.daily_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: daily_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.daily_logs_id_seq OWNED BY public.daily_logs.id;


--
-- Name: day_plans; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.day_plans (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    date date NOT NULL,
    diet_id bigint,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: day_plans_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.day_plans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: day_plans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.day_plans_id_seq OWNED BY public.day_plans.id;


--
-- Name: diet_food_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.diet_food_items (
    id bigint NOT NULL,
    diet_id bigint NOT NULL,
    food_id bigint NOT NULL,
    slot character varying(50) NOT NULL,
    quantity double precision DEFAULT 1 NOT NULL,
    unit character varying(50) DEFAULT 'GRAM'::character varying NOT NULL
);


--
-- Name: diet_food_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.diet_food_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: diet_food_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.diet_food_items_id_seq OWNED BY public.diet_food_items.id;


--
-- Name: diet_meals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.diet_meals (
    id bigint NOT NULL,
    diet_id bigint NOT NULL,
    meal_id bigint NOT NULL,
    day_of_week integer NOT NULL,
    slot character varying(50) NOT NULL,
    instructions character varying(1000)
);


--
-- Name: diet_meals_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.diet_meals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: diet_meals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.diet_meals_id_seq OWNED BY public.diet_meals.id;


--
-- Name: diets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.diets (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(1000),
    target_calories double precision,
    target_protein double precision,
    target_carbs double precision,
    target_fat double precision,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL,
    is_favorite boolean DEFAULT false NOT NULL
);


--
-- Name: diets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.diets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: diets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.diets_id_seq OWNED BY public.diets.id;


--
-- Name: entity_embeddings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.entity_embeddings (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    entity_type character varying(100) NOT NULL,
    entity_server_id uuid NOT NULL,
    embedding public.vector(1536),
    content text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: entity_embeddings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.entity_embeddings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entity_embeddings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.entity_embeddings_id_seq OWNED BY public.entity_embeddings.id;


--
-- Name: entity_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.entity_tags (
    tag_id bigint NOT NULL,
    entity_type public.tag_entity_type NOT NULL,
    entity_id bigint NOT NULL
);


--
-- Name: exercises; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exercises (
    id bigint NOT NULL,
    firebase_uid character varying(255),
    name character varying(255) NOT NULL,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL,
    description character varying(255)
);


--
-- Name: exercises_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.exercises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exercises_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.exercises_id_seq OWNED BY public.exercises.id;


--
-- Name: food_user_prefs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.food_user_prefs (
    firebase_uid character varying(255) NOT NULL,
    food_id bigint NOT NULL,
    is_favorite boolean DEFAULT false NOT NULL
);


--
-- Name: foods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foods (
    id bigint NOT NULL,
    firebase_uid character varying(255),
    name character varying(255) NOT NULL,
    brand character varying(255),
    barcode character varying(255),
    calories_per100 double precision DEFAULT 0 NOT NULL,
    protein_per100 double precision DEFAULT 0 NOT NULL,
    carbs_per100 double precision DEFAULT 0 NOT NULL,
    fat_per100 double precision DEFAULT 0 NOT NULL,
    grams_per_piece double precision,
    grams_per_cup double precision,
    grams_per_tbsp double precision,
    grams_per_tsp double precision,
    glycemic_index integer,
    is_system_food boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL,
    is_favorite boolean DEFAULT false NOT NULL,
    verified boolean DEFAULT false NOT NULL,
    unit character varying(16) DEFAULT 'GRAM'::character varying NOT NULL
);


--
-- Name: foods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.foods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.foods_id_seq OWNED BY public.foods.id;


--
-- Name: grocery_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grocery_items (
    id bigint NOT NULL,
    grocery_list_id bigint NOT NULL,
    food_id bigint,
    name character varying(255) NOT NULL,
    quantity double precision DEFAULT 1 NOT NULL,
    unit character varying(50) NOT NULL,
    category character varying(100),
    done boolean DEFAULT false NOT NULL
);


--
-- Name: grocery_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.grocery_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: grocery_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.grocery_items_id_seq OWNED BY public.grocery_items.id;


--
-- Name: grocery_lists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grocery_lists (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    diet_id bigint,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: grocery_lists_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.grocery_lists_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: grocery_lists_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.grocery_lists_id_seq OWNED BY public.grocery_lists.id;


--
-- Name: health_metrics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.health_metrics (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    type character varying(100) NOT NULL,
    sub_type character varying(100),
    metric_value double precision DEFAULT 0 NOT NULL,
    secondary_value double precision,
    unit character varying(50) NOT NULL,
    recorded_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: health_metrics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.health_metrics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: health_metrics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.health_metrics_id_seq OWNED BY public.health_metrics.id;


--
-- Name: logged_foods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.logged_foods (
    id bigint NOT NULL,
    daily_log_id bigint NOT NULL,
    food_id bigint NOT NULL,
    meal_slot character varying(50) NOT NULL,
    quantity double precision DEFAULT 0 NOT NULL,
    unit character varying(50) NOT NULL
);


--
-- Name: logged_foods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.logged_foods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: logged_foods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.logged_foods_id_seq OWNED BY public.logged_foods.id;


--
-- Name: logged_meal_slots; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.logged_meal_slots (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    date date NOT NULL,
    slot character varying(50) NOT NULL,
    is_logged boolean DEFAULT false NOT NULL
);


--
-- Name: logged_meal_slots_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.logged_meal_slots_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: logged_meal_slots_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.logged_meal_slots_id_seq OWNED BY public.logged_meal_slots.id;


--
-- Name: meal_food_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meal_food_items (
    id bigint NOT NULL,
    meal_id bigint NOT NULL,
    food_id bigint NOT NULL,
    quantity double precision DEFAULT 0 NOT NULL,
    unit character varying(50) NOT NULL,
    notes character varying(500)
);


--
-- Name: meal_food_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meal_food_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meal_food_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meal_food_items_id_seq OWNED BY public.meal_food_items.id;


--
-- Name: meals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meals (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL,
    is_favorite boolean DEFAULT false NOT NULL,
    slots text DEFAULT '[]'::text NOT NULL
);


--
-- Name: meals_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meals_id_seq OWNED BY public.meals.id;


--
-- Name: planned_workouts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.planned_workouts (
    id bigint NOT NULL,
    day_plan_id bigint NOT NULL,
    workout_template_id bigint,
    activity_name character varying(255) NOT NULL
);


--
-- Name: planned_workouts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.planned_workouts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: planned_workouts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.planned_workouts_id_seq OWNED BY public.planned_workouts.id;


--
-- Name: tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tags (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    color character varying(20),
    firebase_uid character varying(255) DEFAULT ''::character varying,
    entity_type public.tag_entity_type NOT NULL
);


--
-- Name: tags_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tags_id_seq OWNED BY public.tags.id;


--
-- Name: template_exercise_sets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.template_exercise_sets (
    id bigint NOT NULL,
    template_exercise_id bigint NOT NULL,
    set_number integer DEFAULT 0 NOT NULL,
    reps integer,
    weight_kg double precision
);


--
-- Name: template_exercise_sets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.template_exercise_sets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: template_exercise_sets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.template_exercise_sets_id_seq OWNED BY public.template_exercise_sets.id;


--
-- Name: template_exercises; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.template_exercises (
    id bigint NOT NULL,
    template_id bigint NOT NULL,
    exercise_id bigint NOT NULL,
    order_index integer DEFAULT 0 NOT NULL,
    notes character varying(500)
);


--
-- Name: template_exercises_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.template_exercises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: template_exercises_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.template_exercises_id_seq OWNED BY public.template_exercises.id;


--
-- Name: tombstones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tombstones (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    entity_type character varying(100) NOT NULL,
    server_id uuid NOT NULL,
    deleted_at timestamp with time zone NOT NULL
);


--
-- Name: tombstones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tombstones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tombstones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tombstones_id_seq OWNED BY public.tombstones.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    email character varying(255),
    display_name character varying(255),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    age integer,
    weight_kg double precision,
    height_cm double precision,
    gender public.gender_enum,
    activity_level public.activity_level_enum,
    target_calories integer,
    goal_type public.goal_type_enum,
    target_weight_kg double precision,
    target_protein integer,
    target_carbs integer,
    target_fat integer,
    preferred_units public.units_enum DEFAULT 'METRIC'::public.units_enum NOT NULL
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: workout_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workout_sessions (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    date date NOT NULL,
    duration_minutes integer,
    notes character varying(1000),
    is_completed boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: workout_sessions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workout_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workout_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workout_sessions_id_seq OWNED BY public.workout_sessions.id;


--
-- Name: workout_sets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workout_sets (
    id bigint NOT NULL,
    session_id bigint NOT NULL,
    exercise_id bigint NOT NULL,
    set_number integer DEFAULT 0 NOT NULL,
    reps integer,
    weight_kg double precision,
    duration_seconds integer,
    distance_meters double precision,
    notes character varying(500)
);


--
-- Name: workout_sets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workout_sets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workout_sets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workout_sets_id_seq OWNED BY public.workout_sets.id;


--
-- Name: workout_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.workout_templates (
    id bigint NOT NULL,
    firebase_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    category character varying(50) DEFAULT 'STRENGTH'::character varying NOT NULL,
    notes character varying(1000),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    server_id uuid NOT NULL
);


--
-- Name: workout_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.workout_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: workout_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.workout_templates_id_seq OWNED BY public.workout_templates.id;


--
-- Name: custom_metric_types id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_metric_types ALTER COLUMN id SET DEFAULT nextval('public.custom_metric_types_id_seq'::regclass);


--
-- Name: daily_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_logs ALTER COLUMN id SET DEFAULT nextval('public.daily_logs_id_seq'::regclass);


--
-- Name: day_plans id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.day_plans ALTER COLUMN id SET DEFAULT nextval('public.day_plans_id_seq'::regclass);


--
-- Name: diet_food_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_food_items ALTER COLUMN id SET DEFAULT nextval('public.diet_food_items_id_seq'::regclass);


--
-- Name: diet_meals id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_meals ALTER COLUMN id SET DEFAULT nextval('public.diet_meals_id_seq'::regclass);


--
-- Name: diets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diets ALTER COLUMN id SET DEFAULT nextval('public.diets_id_seq'::regclass);


--
-- Name: entity_embeddings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_embeddings ALTER COLUMN id SET DEFAULT nextval('public.entity_embeddings_id_seq'::regclass);


--
-- Name: exercises id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exercises ALTER COLUMN id SET DEFAULT nextval('public.exercises_id_seq'::regclass);


--
-- Name: foods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foods ALTER COLUMN id SET DEFAULT nextval('public.foods_id_seq'::regclass);


--
-- Name: grocery_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grocery_items ALTER COLUMN id SET DEFAULT nextval('public.grocery_items_id_seq'::regclass);


--
-- Name: grocery_lists id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grocery_lists ALTER COLUMN id SET DEFAULT nextval('public.grocery_lists_id_seq'::regclass);


--
-- Name: health_metrics id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_metrics ALTER COLUMN id SET DEFAULT nextval('public.health_metrics_id_seq'::regclass);


--
-- Name: logged_foods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_foods ALTER COLUMN id SET DEFAULT nextval('public.logged_foods_id_seq'::regclass);


--
-- Name: logged_meal_slots id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_meal_slots ALTER COLUMN id SET DEFAULT nextval('public.logged_meal_slots_id_seq'::regclass);


--
-- Name: meal_food_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meal_food_items ALTER COLUMN id SET DEFAULT nextval('public.meal_food_items_id_seq'::regclass);


--
-- Name: meals id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meals ALTER COLUMN id SET DEFAULT nextval('public.meals_id_seq'::regclass);


--
-- Name: planned_workouts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.planned_workouts ALTER COLUMN id SET DEFAULT nextval('public.planned_workouts_id_seq'::regclass);


--
-- Name: tags id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags ALTER COLUMN id SET DEFAULT nextval('public.tags_id_seq'::regclass);


--
-- Name: template_exercise_sets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_exercise_sets ALTER COLUMN id SET DEFAULT nextval('public.template_exercise_sets_id_seq'::regclass);


--
-- Name: template_exercises id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_exercises ALTER COLUMN id SET DEFAULT nextval('public.template_exercises_id_seq'::regclass);


--
-- Name: tombstones id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tombstones ALTER COLUMN id SET DEFAULT nextval('public.tombstones_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: workout_sessions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sessions ALTER COLUMN id SET DEFAULT nextval('public.workout_sessions_id_seq'::regclass);


--
-- Name: workout_sets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sets ALTER COLUMN id SET DEFAULT nextval('public.workout_sets_id_seq'::regclass);


--
-- Name: workout_templates id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_templates ALTER COLUMN id SET DEFAULT nextval('public.workout_templates_id_seq'::regclass);


--
-- Name: custom_metric_types custom_metric_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_metric_types
    ADD CONSTRAINT custom_metric_types_pkey PRIMARY KEY (id);


--
-- Name: custom_metric_types custom_metric_types_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_metric_types
    ADD CONSTRAINT custom_metric_types_server_id_key UNIQUE (server_id);


--
-- Name: daily_logs daily_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_logs
    ADD CONSTRAINT daily_logs_pkey PRIMARY KEY (id);


--
-- Name: daily_logs daily_logs_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_logs
    ADD CONSTRAINT daily_logs_server_id_key UNIQUE (server_id);


--
-- Name: day_plans day_plans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.day_plans
    ADD CONSTRAINT day_plans_pkey PRIMARY KEY (id);


--
-- Name: day_plans day_plans_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.day_plans
    ADD CONSTRAINT day_plans_server_id_key UNIQUE (server_id);


--
-- Name: diet_food_items diet_food_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_food_items
    ADD CONSTRAINT diet_food_items_pkey PRIMARY KEY (id);


--
-- Name: diet_meals diet_meals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_meals
    ADD CONSTRAINT diet_meals_pkey PRIMARY KEY (id);


--
-- Name: diets diets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diets
    ADD CONSTRAINT diets_pkey PRIMARY KEY (id);


--
-- Name: diets diets_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diets
    ADD CONSTRAINT diets_server_id_key UNIQUE (server_id);


--
-- Name: entity_embeddings entity_embeddings_firebase_uid_entity_type_entity_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_embeddings
    ADD CONSTRAINT entity_embeddings_firebase_uid_entity_type_entity_server_id_key UNIQUE (firebase_uid, entity_type, entity_server_id);


--
-- Name: entity_embeddings entity_embeddings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_embeddings
    ADD CONSTRAINT entity_embeddings_pkey PRIMARY KEY (id);


--
-- Name: entity_tags entity_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_tags
    ADD CONSTRAINT entity_tags_pkey PRIMARY KEY (tag_id, entity_type, entity_id);


--
-- Name: exercises exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_pkey PRIMARY KEY (id);


--
-- Name: exercises exercises_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_server_id_key UNIQUE (server_id);


--
-- Name: food_user_prefs food_user_prefs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.food_user_prefs
    ADD CONSTRAINT food_user_prefs_pkey PRIMARY KEY (firebase_uid, food_id);


--
-- Name: foods foods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foods
    ADD CONSTRAINT foods_pkey PRIMARY KEY (id);


--
-- Name: foods foods_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foods
    ADD CONSTRAINT foods_server_id_key UNIQUE (server_id);


--
-- Name: grocery_items grocery_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grocery_items
    ADD CONSTRAINT grocery_items_pkey PRIMARY KEY (id);


--
-- Name: grocery_lists grocery_lists_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grocery_lists
    ADD CONSTRAINT grocery_lists_pkey PRIMARY KEY (id);


--
-- Name: grocery_lists grocery_lists_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grocery_lists
    ADD CONSTRAINT grocery_lists_server_id_key UNIQUE (server_id);


--
-- Name: health_metrics health_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_metrics
    ADD CONSTRAINT health_metrics_pkey PRIMARY KEY (id);


--
-- Name: health_metrics health_metrics_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_metrics
    ADD CONSTRAINT health_metrics_server_id_key UNIQUE (server_id);


--
-- Name: logged_foods logged_foods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_foods
    ADD CONSTRAINT logged_foods_pkey PRIMARY KEY (id);


--
-- Name: logged_meal_slots logged_meal_slots_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_meal_slots
    ADD CONSTRAINT logged_meal_slots_pkey PRIMARY KEY (id);


--
-- Name: meal_food_items meal_food_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meal_food_items
    ADD CONSTRAINT meal_food_items_pkey PRIMARY KEY (id);


--
-- Name: meals meals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meals
    ADD CONSTRAINT meals_pkey PRIMARY KEY (id);


--
-- Name: meals meals_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meals
    ADD CONSTRAINT meals_server_id_key UNIQUE (server_id);


--
-- Name: planned_workouts planned_workouts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.planned_workouts
    ADD CONSTRAINT planned_workouts_pkey PRIMARY KEY (id);


--
-- Name: tags tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);


--
-- Name: template_exercise_sets template_exercise_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_exercise_sets
    ADD CONSTRAINT template_exercise_sets_pkey PRIMARY KEY (id);


--
-- Name: template_exercises template_exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_exercises
    ADD CONSTRAINT template_exercises_pkey PRIMARY KEY (id);


--
-- Name: tombstones tombstones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tombstones
    ADD CONSTRAINT tombstones_pkey PRIMARY KEY (id);


--
-- Name: daily_logs uq_daily_logs_uid_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.daily_logs
    ADD CONSTRAINT uq_daily_logs_uid_date UNIQUE (firebase_uid, date);


--
-- Name: day_plans uq_day_plans_uid_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.day_plans
    ADD CONSTRAINT uq_day_plans_uid_date UNIQUE (firebase_uid, date);


--
-- Name: logged_meal_slots uq_logged_slot_uid_date_slot; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_meal_slots
    ADD CONSTRAINT uq_logged_slot_uid_date_slot UNIQUE (firebase_uid, date, slot);


--
-- Name: workout_sessions uq_workout_session_uid_date_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sessions
    ADD CONSTRAINT uq_workout_session_uid_date_name UNIQUE (firebase_uid, date, name);


--
-- Name: users users_firebase_uid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_firebase_uid_key UNIQUE (firebase_uid);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: workout_sessions workout_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sessions
    ADD CONSTRAINT workout_sessions_pkey PRIMARY KEY (id);


--
-- Name: workout_sessions workout_sessions_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sessions
    ADD CONSTRAINT workout_sessions_server_id_key UNIQUE (server_id);


--
-- Name: workout_sets workout_sets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sets
    ADD CONSTRAINT workout_sets_pkey PRIMARY KEY (id);


--
-- Name: workout_templates workout_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_templates
    ADD CONSTRAINT workout_templates_pkey PRIMARY KEY (id);


--
-- Name: workout_templates workout_templates_server_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_templates
    ADD CONSTRAINT workout_templates_server_id_key UNIQUE (server_id);


--
-- Name: idx_daily_logs_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_daily_logs_uid ON public.daily_logs USING btree (firebase_uid);


--
-- Name: idx_day_plans_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_day_plans_date ON public.day_plans USING btree (date);


--
-- Name: idx_day_plans_firebase_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_day_plans_firebase_uid ON public.day_plans USING btree (firebase_uid);


--
-- Name: idx_diet_food_items_diet_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_diet_food_items_diet_id ON public.diet_food_items USING btree (diet_id);


--
-- Name: idx_diets_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_diets_uid ON public.diets USING btree (firebase_uid);


--
-- Name: idx_dm_diet_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dm_diet_id ON public.diet_meals USING btree (diet_id);


--
-- Name: idx_entity_embeddings_hnsw; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_embeddings_hnsw ON public.entity_embeddings USING hnsw (embedding public.vector_cosine_ops);


--
-- Name: idx_entity_embeddings_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_embeddings_lookup ON public.entity_embeddings USING btree (firebase_uid, entity_type);


--
-- Name: idx_entity_tags_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_tags_entity ON public.entity_tags USING btree (entity_type, entity_id);


--
-- Name: idx_exercises_firebase_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exercises_firebase_uid ON public.exercises USING btree (firebase_uid);


--
-- Name: idx_foods_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foods_uid ON public.foods USING btree (firebase_uid);


--
-- Name: idx_fup_firebase_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fup_firebase_uid ON public.food_user_prefs USING btree (firebase_uid);


--
-- Name: idx_gi_grocery_list_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_gi_grocery_list_id ON public.grocery_items USING btree (grocery_list_id);


--
-- Name: idx_grocery_lists_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grocery_lists_uid ON public.grocery_lists USING btree (firebase_uid);


--
-- Name: idx_health_metrics_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_health_metrics_uid ON public.health_metrics USING btree (firebase_uid);


--
-- Name: idx_health_uid_type_ts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_health_uid_type_ts ON public.health_metrics USING btree (firebase_uid, type, recorded_at DESC);


--
-- Name: idx_lf_daily_log_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lf_daily_log_id ON public.logged_foods USING btree (daily_log_id);


--
-- Name: idx_logged_meal_slots_uid_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_logged_meal_slots_uid_date ON public.logged_meal_slots USING btree (firebase_uid, date);


--
-- Name: idx_meals_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meals_uid ON public.meals USING btree (firebase_uid);


--
-- Name: idx_mfi_meal_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mfi_meal_id ON public.meal_food_items USING btree (meal_id);


--
-- Name: idx_planned_workouts_day_plan_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_planned_workouts_day_plan_id ON public.planned_workouts USING btree (day_plan_id);


--
-- Name: idx_template_exercise_sets_te; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_exercise_sets_te ON public.template_exercise_sets USING btree (template_exercise_id);


--
-- Name: idx_template_exercises_template_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_exercises_template_id ON public.template_exercises USING btree (template_id);


--
-- Name: idx_tombstones_server_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tombstones_server_id ON public.tombstones USING btree (server_id);


--
-- Name: idx_tombstones_uid_ts; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tombstones_uid_ts ON public.tombstones USING btree (firebase_uid, deleted_at);


--
-- Name: idx_workout_sessions_firebase_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workout_sessions_firebase_uid ON public.workout_sessions USING btree (firebase_uid);


--
-- Name: idx_workout_sets_session_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workout_sets_session_id ON public.workout_sets USING btree (session_id);


--
-- Name: idx_workout_templates_firebase_uid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_workout_templates_firebase_uid ON public.workout_templates USING btree (firebase_uid);


--
-- Name: day_plans day_plans_diet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.day_plans
    ADD CONSTRAINT day_plans_diet_id_fkey FOREIGN KEY (diet_id) REFERENCES public.diets(id) ON DELETE CASCADE;


--
-- Name: diet_food_items diet_food_items_diet_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_food_items
    ADD CONSTRAINT diet_food_items_diet_id_fkey FOREIGN KEY (diet_id) REFERENCES public.diets(id) ON DELETE CASCADE;


--
-- Name: diet_food_items diet_food_items_food_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_food_items
    ADD CONSTRAINT diet_food_items_food_id_fkey FOREIGN KEY (food_id) REFERENCES public.foods(id);


--
-- Name: entity_tags entity_tags_tag_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_tags
    ADD CONSTRAINT entity_tags_tag_id_fkey FOREIGN KEY (tag_id) REFERENCES public.tags(id) ON DELETE CASCADE;


--
-- Name: diet_meals fk_dm_diet; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_meals
    ADD CONSTRAINT fk_dm_diet FOREIGN KEY (diet_id) REFERENCES public.diets(id) ON DELETE CASCADE;


--
-- Name: diet_meals fk_dm_meal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.diet_meals
    ADD CONSTRAINT fk_dm_meal FOREIGN KEY (meal_id) REFERENCES public.meals(id);


--
-- Name: grocery_items fk_gi_list; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grocery_items
    ADD CONSTRAINT fk_gi_list FOREIGN KEY (grocery_list_id) REFERENCES public.grocery_lists(id) ON DELETE CASCADE;


--
-- Name: logged_foods fk_lf_food; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_foods
    ADD CONSTRAINT fk_lf_food FOREIGN KEY (food_id) REFERENCES public.foods(id);


--
-- Name: logged_foods fk_lf_log; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.logged_foods
    ADD CONSTRAINT fk_lf_log FOREIGN KEY (daily_log_id) REFERENCES public.daily_logs(id) ON DELETE CASCADE;


--
-- Name: meal_food_items fk_mfi_food; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meal_food_items
    ADD CONSTRAINT fk_mfi_food FOREIGN KEY (food_id) REFERENCES public.foods(id);


--
-- Name: meal_food_items fk_mfi_meal; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meal_food_items
    ADD CONSTRAINT fk_mfi_meal FOREIGN KEY (meal_id) REFERENCES public.meals(id) ON DELETE CASCADE;


--
-- Name: food_user_prefs food_user_prefs_food_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.food_user_prefs
    ADD CONSTRAINT food_user_prefs_food_id_fkey FOREIGN KEY (food_id) REFERENCES public.foods(id) ON DELETE CASCADE;


--
-- Name: planned_workouts planned_workouts_day_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.planned_workouts
    ADD CONSTRAINT planned_workouts_day_plan_id_fkey FOREIGN KEY (day_plan_id) REFERENCES public.day_plans(id) ON DELETE CASCADE;


--
-- Name: planned_workouts planned_workouts_workout_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.planned_workouts
    ADD CONSTRAINT planned_workouts_workout_template_id_fkey FOREIGN KEY (workout_template_id) REFERENCES public.workout_templates(id) ON DELETE SET NULL;


--
-- Name: template_exercise_sets template_exercise_sets_template_exercise_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_exercise_sets
    ADD CONSTRAINT template_exercise_sets_template_exercise_id_fkey FOREIGN KEY (template_exercise_id) REFERENCES public.template_exercises(id) ON DELETE CASCADE;


--
-- Name: template_exercises template_exercises_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_exercises
    ADD CONSTRAINT template_exercises_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.workout_templates(id) ON DELETE CASCADE;


--
-- Name: workout_sets workout_sets_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.workout_sets
    ADD CONSTRAINT workout_sets_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.workout_sessions(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


