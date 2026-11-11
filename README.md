# Sweet Bloom Patisserie

Bakery Order and Custom Cake Booking Platform for SE1020 Object Oriented Programming.

## Open and run in IntelliJ

1. Clone this repository, or extract the full project ZIP into a new folder.
2. In IntelliJ, choose **File > Open**, select this folder's **pom.xml**, and choose **Open as Project**.
3. Select **JDK 21** when asked, then wait for Maven import and indexing to finish. The project uses Spring Boot 4.1.2.
4. Select the provided **BakeryShopApplication** run configuration and click **Run**.
5. When the console says `Started BakeryShopApplication`, open [http://localhost:8080](http://localhost:8080).

The provided configuration uses the `bakery-shop` Maven module and the main class `com.bakery.bakeryplatform.BakeryShopApplication`.

## After pulling into an existing IntelliJ project

Close the project, then use **File > Open** to open the repository root **pom.xml** as a project again. Wait for Maven import to finish, select **JDK 21**, and choose the shared **BakeryShopApplication** run configuration. An older local configuration named **BakeryPlatformApplication** may still be listed; the application main class is **com.bakery.bakeryplatform.BakeryShopApplication**.

## Start from Windows

With Java 21 available to Maven, double-click **START-BAKERY.cmd** in this folder. This runs the Maven build and starts Spring Boot using the main class configured in `pom.xml`. The first build downloads Maven and the required dependencies, so keep the computer connected to the internet.

## Administrator account and tests

Set `APP_ADMIN_USERNAME`, `APP_ADMIN_EMAIL`, and `APP_ADMIN_PASSWORD` to your own administrator values in the run configuration's environment variables before the first administrator login. Customers can register on the sign-in page.

Run tests with `mvnw.cmd test` on Windows or `bash mvnw test` on Linux or macOS.

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

The complete application is on the `main` branch. Each member branch contains the shared project setup and that member's assigned files. The module commits are published using Pabasara Dissanayaka's account; branch names identify the task assignment.

## Java packages

The full application and its tests use `com.bakery.bakeryplatform`. The source is grouped under `config`, `controller`, `dto`, `exception`, `model`, `repository`, and `service`.
