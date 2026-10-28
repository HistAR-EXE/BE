ALTER TABLE profiles
    ADD COLUMN full_name varchar(255),
    ADD COLUMN date_of_birth date,
    ADD COLUMN gender varchar(255),
    ADD COLUMN phone varchar(255),
    ADD COLUMN bio varchar(1000),
    ADD COLUMN school varchar(255),
    ADD COLUMN class_name varchar(255),
    ADD COLUMN specialty varchar(255);
