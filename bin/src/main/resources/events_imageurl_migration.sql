-- MySQL migration: increase events.image_url capacity for base64 uploads
ALTER TABLE events MODIFY COLUMN image_url LONGTEXT;
