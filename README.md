# Sweet Bloom Patisserie

Bakery Order and Custom Cake Booking Platform for SE1020 Object Oriented Programming.

<<<<<<< HEAD
## Run the project

1. Clone the repository. Open the root `pom.xml` in IntelliJ as a Maven project and select JDK 21. The project uses Spring Boot 4.1.2.
2. In the `BakeryShopApplication` run configuration, set `APP_ADMIN_USERNAME`, `APP_ADMIN_EMAIL`, and `APP_ADMIN_PASSWORD` to your own administrator values.
3. Run `com.bakery.bakeryplatform.BakeryShopApplication` and open [http://localhost:8080](http://localhost:8080). Customers can register on the sign-in page.
4. Run tests with `mvnw.cmd test` on Windows or `bash mvnw test` on Linux or macOS.

On first run the app creates the local H2 database and seeds the catalogue. Product details are stored in `data/website-items.txt`. H2 stores product categories, stock and the other CRUD records. The `data/` folder is ignored by Git. Place any original product photos under `src/main/resources/static/images/` with their original paths. The site has a placeholder for missing photos.

Checkout records a demonstration payment and opens the thank-you page. The customer can download a PDF invoice after payment. To enable invoice email, set `INVOICE_EMAIL_ENABLED=true` and provide `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` in the local run configuration.

## Team modules

| Member | CRUDs | Branch |
| --- | --- | --- |
| 1 | CartItem, Order, Payment | `member-01-cart-order-payment` |
| 2 | User, Delivery | `member-02-user-delivery` |
| 3 | Product, Category | `member-03-product-category` |
| 4 | Invoice, RefundRequest | `member-04-invoice-refund` |
| 5 | CustomCakeBooking, CakeOption, CakeDesign | `member-05-custom-cake` |

Each member branch contains the shared project setup and that member's assigned files. Combine the five module branches to build and run the complete application. The module commits are published using Pabasara Dissanayaka's account; branch names identify the task assignment.

## Java packages

The full application and its tests use `com.bakery.bakeryplatform`. The source is grouped under `config`, `controller`, `dto`, `exception`, `model`, `repository`, and `service`.
=======


## Team modules

| Member | CRUDs | Branch | MemeberName| MemberID |
| --- | --- | --- | --- | --- |
| 1 | CartItem, Order, Payment | `member-01-cart-order-payment` | Pabasara | IT26100616 |
| 2 | User, Delivery | `member-02-user-delivery` | Sandil | IT26100302 |
| 3 | Product, Category | `member-03-product-category` | Abeetha | IT26100367 |
| 4 | Invoice, RefundRequest | `member-04-invoice-refund` | Suraj | IT26100735 |
| 5 | CustomCakeBooking, CakeOption, CakeDesign | `member-05-custom-cake` | Oshan | IT26100705 |
>>>>>>> 638e6314080a6d8b57c13d5bdd4c0169343485c1
