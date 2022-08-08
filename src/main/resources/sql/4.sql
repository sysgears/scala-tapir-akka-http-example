ALTER TABLE "order_products" ADD FOREIGN KEY ("order_id") REFERENCES "orders" ("id") on delete cascade on update no action;
