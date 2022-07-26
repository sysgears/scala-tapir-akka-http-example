CREATE TABLE "orders"(
                        "id" SERIAL PRIMARY KEY,
                        "user_id" int not null,
                        "status" varchar(40) not null,
                        "comment" varchar(240),
                        "created" timestamp DEFAULT (now()),
                        "last_updated" timestamp DEFAULT (now())
);

CREATE TABLE "products"(
    "id" SERIAL PRIMARY KEY,
    "name" varchar(50),
    "description" varchar(300),
    "price" float
);

CREATE TABLE "order_products"(
    "id" SERIAL PRIMARY KEY,
    "order_id" int,
    "product_id" int,
    "quantity" int
);

ALTER TABLE "orders" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "order_products" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id");

ALTER TABLE "order_products" ADD FOREIGN KEY ("product_id") REFERENCES "products" ("id");
