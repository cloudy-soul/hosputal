-- Initialization script mounted into MySQL container
CREATE DATABASE IF NOT EXISTS hospital_db;
-- Optional: create a dedicated user instead of using root
-- CREATE USER 'hospital'@'%' IDENTIFIED BY 'hospital_pass';
-- GRANT ALL PRIVILEGES ON hospital_db.* TO 'hospital'@'%';
-- FLUSH PRIVILEGES;
