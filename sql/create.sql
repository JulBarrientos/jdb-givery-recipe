CREATE TABLE IF NOT EXISTS recipes (
  id SERIAL PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  making_time VARCHAR(100) NOT NULL,
  serves VARCHAR(100) NOT NULL,
  ingredients VARCHAR(300) NOT NULL,
  cost INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);