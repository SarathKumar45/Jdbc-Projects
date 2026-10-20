package inventory_jdbc;

import java.sql.*;
import java.util.Scanner;
import java.math.BigDecimal;

public class InventoryManagementSystem {

    static final String URL =
            "jdbc:mysql://localhost:3306/inventory_db";

    static final String USER = "root";
    static final String PASSWORD = "root";

    // Database Connection
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Create Database and Tables
    static void createDatabase() {

        String serverURL = "jdbc:mysql://localhost:3306/";

        try (Connection con = DriverManager.getConnection(
                    serverURL, USER, PASSWORD);
             Statement st = con.createStatement()) {

            st.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS inventory_db");

        } catch (SQLException e) {
            System.out.println("Database creation error: "
                    + e.getMessage());
            return;
        }

        String[] tables = {

            "CREATE TABLE IF NOT EXISTS suppliers ("
            + "supplier_id INT PRIMARY KEY AUTO_INCREMENT, "
            + "supplier_name VARCHAR(100) NOT NULL)",

            "CREATE TABLE IF NOT EXISTS products ("
            + "product_id INT PRIMARY KEY AUTO_INCREMENT, "
            + "product_name VARCHAR(100) NOT NULL, "
            + "price DECIMAL(10,2) NOT NULL, "
            + "quantity INT NOT NULL DEFAULT 0, "
            + "supplier_id INT NOT NULL, "
            + "FOREIGN KEY (supplier_id) "
            + "REFERENCES suppliers(supplier_id))",

            "CREATE TABLE IF NOT EXISTS sales ("
            + "sale_id INT PRIMARY KEY AUTO_INCREMENT, "
            + "product_id INT NOT NULL, "
            + "quantity_sold INT NOT NULL, "
            + "total_amount DECIMAL(12,2) NOT NULL, "
            + "sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (product_id) "
            + "REFERENCES products(product_id))"
        };

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            for (String sql : tables) {
                st.executeUpdate(sql);
            }

            System.out.println("Database and tables are ready.");

        } catch (SQLException e) {
            System.out.println("Table creation error: "
                    + e.getMessage());
        }
    }

    // Insert Sample Data into All Tables
    static void insertSampleData() {

        try (Connection con = getConnection()) {

            con.setAutoCommit(false);

            try {
                // Insert suppliers only if they do not already exist
                String supplierSQL =
                        "INSERT INTO suppliers (supplier_name) "
                        + "SELECT ? WHERE NOT EXISTS "
                        + "(SELECT 1 FROM suppliers "
                        + "WHERE supplier_name = ?)";

                try (PreparedStatement ps =
                             con.prepareStatement(supplierSQL)) {

                    String[] suppliers = {
                            "Oppo Electronics",
                            "Samsung Traders",
                            "Apple Distributors"
                    };

                    for (String supplier : suppliers) {
                        ps.setString(1, supplier);
                        ps.setString(2, supplier);
                        ps.executeUpdate();
                    }
                }

                // Insert products only if they do not already exist
                String productSQL =
                        "INSERT INTO products "
                        + "(product_name, price, quantity, supplier_id) "
                        + "SELECT ?, ?, ?, supplier_id "
                        + "FROM suppliers WHERE supplier_name = ? "
                        + "AND NOT EXISTS "
                        + "(SELECT 1 FROM products WHERE product_name = ?)";

                try (PreparedStatement ps =
                             con.prepareStatement(productSQL)) {

                    addSampleProduct(ps, "Oppo Reno 16",
                            "95000.00", 50, "Oppo Electronics");

                    addSampleProduct(ps, "Samsung Galaxy S26",
                            "85000.00", 30, "Samsung Traders");

                    addSampleProduct(ps, "iPhone 17",
                            "80000.00", 20, "Apple Distributors");
                }

                // Insert one demonstration sale if no sale exists
                String salesSQL =
                        "INSERT INTO sales "
                        + "(product_id, quantity_sold, total_amount) "
                        + "SELECT product_id, 2, price * 2 "
                        + "FROM products "
                        + "WHERE product_name = 'Oppo Reno 16' "
                        + "AND NOT EXISTS "
                        + "(SELECT 1 FROM sales s "
                        + "WHERE s.product_id = products.product_id)";

                try (Statement st = con.createStatement()) {
                    st.executeUpdate(salesSQL);
                }

                con.commit();

                System.out.println("Sample data is ready.");

            } catch (SQLException e) {

                con.rollback();

                System.out.println("Sample data error: "
                        + e.getMessage());

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    static void addSampleProduct(PreparedStatement ps,
                                 String name,
                                 String price,
                                 int quantity,
                                 String supplier) throws SQLException {

        ps.setString(1, name);
        ps.setBigDecimal(2, new BigDecimal(price));
        ps.setInt(3, quantity);
        ps.setString(4, supplier);
        ps.setString(5, name);

        ps.executeUpdate();
    }

    // Module 1: Add Products
    static void addProduct(Scanner sc) {

        String sql =
                "INSERT INTO products "
                + "(product_name, price, quantity, supplier_id) "
                + "VALUES (?, ?, ?, ?)";

        try {
            System.out.print("Enter product name: ");
            String name = sc.nextLine().trim();

            System.out.print("Enter price: ");
            BigDecimal price = new BigDecimal(sc.nextLine().trim());

            System.out.print("Enter quantity: ");
            int quantity = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter supplier ID: ");
            int supplierId = Integer.parseInt(sc.nextLine().trim());

            if (name.isEmpty()
                    || price.signum() <= 0
                    || quantity < 0
                    || supplierId <= 0) {

                System.out.println("Invalid product details.");
                return;
            }

            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setBigDecimal(2, price);
                ps.setInt(3, quantity);
                ps.setInt(4, supplierId);

                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("Product added successfully.");
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Enter valid numeric values.");

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Module 2: Update Stock
    static void updateStock(Scanner sc) {

        String sql =
                "UPDATE products SET quantity = ? "
                + "WHERE product_id = ?";

        try {
            System.out.print("Enter product ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter new stock quantity: ");
            int quantity = Integer.parseInt(sc.nextLine().trim());

            if (id <= 0 || quantity < 0) {
                System.out.println("Invalid product ID or quantity.");
                return;
            }

            try (Connection con = getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, quantity);
                ps.setInt(2, id);

                if (ps.executeUpdate() > 0) {
                    System.out.println("Stock updated successfully.");
                } else {
                    System.out.println("Product not found.");
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Enter valid numeric values.");

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Module 3: Record Sales
    static void recordSale(Scanner sc) {

        String selectSQL =
                "SELECT price FROM products WHERE product_id = ?";

        String updateSQL =
                "UPDATE products SET quantity = quantity - ? "
                + "WHERE product_id = ? AND quantity >= ?";

        String saleSQL =
                "INSERT INTO sales "
                + "(product_id, quantity_sold, total_amount) "
                + "VALUES (?, ?, ?)";

        try {
            System.out.print("Enter product ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter quantity sold: ");
            int sold = Integer.parseInt(sc.nextLine().trim());

            if (id <= 0 || sold <= 0) {
                System.out.println("Invalid product ID or quantity.");
                return;
            }

            try (Connection con = getConnection()) {

                con.setAutoCommit(false);

                try {
                    BigDecimal price;

                    try (PreparedStatement ps =
                                 con.prepareStatement(selectSQL)) {

                        ps.setInt(1, id);

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()) {
                                throw new SQLException(
                                        "Product not found.");
                            }

                            price = rs.getBigDecimal("price");
                        }
                    }

                    try (PreparedStatement ps =
                                 con.prepareStatement(updateSQL)) {

                        ps.setInt(1, sold);
                        ps.setInt(2, id);
                        ps.setInt(3, sold);

                        if (ps.executeUpdate() == 0) {
                            throw new SQLException(
                                    "Insufficient stock.");
                        }
                    }

                    BigDecimal total = price.multiply(
                            BigDecimal.valueOf(sold));

                    try (PreparedStatement ps =
                                 con.prepareStatement(saleSQL)) {

                        ps.setInt(1, id);
                        ps.setInt(2, sold);
                        ps.setBigDecimal(3, total);

                        ps.executeUpdate();
                    }

                    con.commit();

                    System.out.println("Sale recorded successfully.");
                    System.out.println("Total amount: Rs. " + total);

                } catch (SQLException e) {

                    try {
                        con.rollback();
                    } catch (SQLException rollbackError) {
                        e.addSuppressed(rollbackError);
                    }

                    System.out.println(
                            "Sale failed; changes rolled back: "
                            + e.getMessage());

                } finally {
                    con.setAutoCommit(true);
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Enter valid numeric values.");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // Module 4: Generate Reports
    static void generateReports() {

        String productSQL =
                "SELECT COUNT(*) AS total_products, "
                + "COALESCE(SUM(quantity), 0) AS total_stock, "
                + "COALESCE(SUM(price * quantity), 0) AS stock_value "
                + "FROM products";

        String salesSQL =
                "SELECT COUNT(*) AS total_sales, "
                + "COALESCE(SUM(quantity_sold), 0) AS units_sold, "
                + "COALESCE(SUM(total_amount), 0) AS revenue "
                + "FROM sales";

        try (Connection con = getConnection()) {

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(productSQL)) {

                if (rs.next()) {

                    System.out.println("\n--- INVENTORY REPORT ---");

                    System.out.println("Total products: "
                            + rs.getInt("total_products"));

                    System.out.println("Total stock: "
                            + rs.getLong("total_stock"));

                    System.out.println("Inventory value: Rs. "
                            + rs.getBigDecimal("stock_value"));
                }
            }

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(salesSQL)) {

                if (rs.next()) {

                    System.out.println("\n--- SALES REPORT ---");

                    System.out.println("Total sales: "
                            + rs.getInt("total_sales"));

                    System.out.println("Units sold: "
                            + rs.getLong("units_sold"));

                    System.out.println("Total revenue: Rs. "
                            + rs.getBigDecimal("revenue"));
                }
            }

        } catch (SQLException e) {
            System.out.println("Report error: " + e.getMessage());
        }
    }

    // Main Menu
    public static void main(String[] args) {

        createDatabase();

        // Automatically populate all three tables
        insertSampleData();

        try (Scanner sc = new Scanner(System.in)) {

            while (true) {

                System.out.println(
                        "\n=== INVENTORY MANAGEMENT SYSTEM ===");

                System.out.println("1. Add Products");
                System.out.println("2. Update Stock");
                System.out.println("3. Record Sales");
                System.out.println("4. Generate Reports");
                System.out.println("0. Exit");

                System.out.print("Enter choice: ");

                String input = sc.nextLine().trim();

                try {
                    int choice = Integer.parseInt(input);

                    switch (choice) {

                        case 1:
                            addProduct(sc);
                            break;

                        case 2:
                            updateStock(sc);
                            break;

                        case 3:
                            recordSale(sc);
                            break;

                        case 4:
                            generateReports();
                            break;

                        case 0:
                            System.out.println("Application closed.");
                            return;

                        default:
                            System.out.println("Invalid choice.");
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Enter a valid menu number.");
                }
            }
        }
    }
}
