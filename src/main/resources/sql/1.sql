CREATE TABLE "users"(
    "id" SERIAL PRIMARY KEY,
    "name" varchar(120) not null ,
    "phone_number" varchar(20) not null,
    "email" varchar(60) not null unique,
    "password_hash" varchar(64) not null,
    "zip" varchar(30) not null,
    "city" varchar(150) not null,
    "address" varchar(200) not null,
    "role" varchar(20) not null,
    "created" timestamp DEFAULT (now())
);
