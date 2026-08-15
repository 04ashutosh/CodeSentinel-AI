-- ==============================================================
-- Database Initialization Script
-- ==============================================================
-- This runs ONCE when the PostgreSQL container starts for the
-- first time. It creates separate databases for each microservice.
--
-- WHY separate databases?
-- Each microservice should own its data. This is a core
-- microservice principle called "Database per Service."
-- It prevents tight coupling — services can't directly query
-- each other's tables.
-- ==============================================================

-- Auth Service database: stores users, roles, refresh tokens
CREATE DATABASE auth_db;

-- Ingestion Service database: stores upload jobs, file metadata
CREATE DATABASE ingestion_db;

-- Parser Service database: stores parsed code metadata
CREATE DATABASE parser_db;