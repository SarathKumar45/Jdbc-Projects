package movieTicket_jdbc;

import java.sql.*;
import java.util.Scanner;

public class MovieTicketBookingSystem {

    static final String URL = "jdbc:mysql://localhost:3306/movie_booking";
    static final String USER = "root";
    static final String PASSWORD = "root";

    static Scanner sc = new Scanner(System.in);

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Create Tables
    static void createTables() throws SQLException {

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS movies (" +
                "movie_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "movie_name VARCHAR(100) NOT NULL UNIQUE)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS theaters (" +
                "theater_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "theater_name VARCHAR(100) NOT NULL, " +
                "location VARCHAR(100) NOT NULL, " +
                "UNIQUE(theater_name, location))"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS shows (" +
                "show_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "movie_id INT NOT NULL, " +
                "theater_id INT NOT NULL, " +
                "show_time VARCHAR(30) NOT NULL, " +
                "total_seats INT NOT NULL, " +
                "ticket_price DECIMAL(10,2) NOT NULL, " +
                "FOREIGN KEY(movie_id) REFERENCES movies(movie_id), " +
                "FOREIGN KEY(theater_id) REFERENCES theaters(theater_id), " +
                "UNIQUE(movie_id, theater_id, show_time))"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS bookings (" +
                "booking_id INT PRIMARY KEY AUTO_INCREMENT, " +
                "show_id INT NOT NULL, " +
                "customer_name VARCHAR(100) NOT NULL, " +
                "seat_number INT NOT NULL, " +
                "FOREIGN KEY(show_id) REFERENCES shows(show_id), " +
                "UNIQUE(show_id, seat_number))"
            );
        }
    }

    // Insert Multiple Movies, Theaters and Shows
    static void insertSampleData() throws SQLException {

        try (Connection con = getConnection()) {

            // Movies
            addMovie(con, "Leo");
            addMovie(con, "Jailer");
            addMovie(con, "Vikram");
            addMovie(con, "GOAT");
            addMovie(con, "Amaran");

            // Theaters
            addTheater(con, "PVR Cinemas", "Chennai");
            addTheater(con, "AGS Cinemas", "Chennai");
            addTheater(con, "INOX", "Bangalore");
            addTheater(con, "Cinepolis", "Bangalore");

            // Multiple Shows
            addShow(con, "Leo", "PVR Cinemas", "Chennai",
                    "10:00 AM", 100, 200);

            addShow(con, "Leo", "PVR Cinemas", "Chennai",
                    "6:00 PM", 100, 250);

            addShow(con, "Jailer", "PVR Cinemas", "Chennai",
                    "2:00 PM", 120, 220);

            addShow(con, "Vikram", "AGS Cinemas", "Chennai",
                    "11:00 AM", 100, 180);

            addShow(con, "GOAT", "AGS Cinemas", "Chennai",
                    "6:30 PM", 120, 250);

            addShow(con, "Amaran", "AGS Cinemas", "Chennai",
                    "3:00 PM", 100, 200);

            addShow(con, "Leo", "INOX", "Bangalore",
                    "11:00 AM", 100, 220);

            addShow(con, "Jailer", "INOX", "Bangalore",
                    "6:00 PM", 120, 250);

            addShow(con, "Vikram", "Cinepolis", "Bangalore",
                    "2:00 PM", 100, 200);

            addShow(con, "Amaran", "Cinepolis", "Bangalore",
                    "7:00 PM", 120, 250);
        }
    }

    static void addMovie(Connection con, String movieName)
            throws SQLException {

        String sql =
            "INSERT IGNORE INTO movies(movie_name) VALUES(?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, movieName);
            ps.executeUpdate();
        }
    }

    static void addTheater(Connection con, String theaterName,
                           String location) throws SQLException {

        String sql =
            "INSERT IGNORE INTO theaters(theater_name, location) VALUES(?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, theaterName);
            ps.setString(2, location);
            ps.executeUpdate();
        }
    }

    static void addShow(Connection con, String movieName,
                        String theaterName, String location,
                        String showTime, int seats, double price)
            throws SQLException {

        String sql =
            "INSERT IGNORE INTO shows " +
            "(movie_id, theater_id, show_time, total_seats, ticket_price) " +
            "SELECT m.movie_id, t.theater_id, ?, ?, ? " +
            "FROM movies m CROSS JOIN theaters t " +
            "WHERE m.movie_name = ? " +
            "AND t.theater_name = ? AND t.location = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, showTime);
            ps.setInt(2, seats);
            ps.setDouble(3, price);
            ps.setString(4, movieName);
            ps.setString(5, theaterName);
            ps.setString(6, location);

            ps.executeUpdate();
        }
    }

    // MODULE 1: Movie Listing
    static void movieListing() throws SQLException {

        String sql =
            "SELECT DISTINCT m.movie_id, m.movie_name " +
            "FROM movies m JOIN shows s ON m.movie_id = s.movie_id " +
            "ORDER BY m.movie_name";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== MOVIE LISTING ==========");

            while (rs.next()) {
                System.out.println(
                    "Movie ID: " + rs.getInt("movie_id") +
                    " | Movie: " + rs.getString("movie_name")
                );
            }
        }
    }

    // MODULE 2: Theater Management
    static void theaterManagement() throws SQLException {

        String sql =
            "SELECT s.show_id, m.movie_name, t.theater_name, " +
            "t.location, s.show_time, s.total_seats, s.ticket_price " +
            "FROM shows s " +
            "JOIN movies m ON s.movie_id = m.movie_id " +
            "JOIN theaters t ON s.theater_id = t.theater_id " +
            "ORDER BY t.location, t.theater_name, s.show_time";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== THEATER MANAGEMENT ==========");

            while (rs.next()) {
                System.out.println(
                    "Show ID: " + rs.getInt("show_id") +
                    " | Movie: " + rs.getString("movie_name") +
                    " | Theater: " + rs.getString("theater_name") +
                    " | Location: " + rs.getString("location") +
                    " | Time: " + rs.getString("show_time") +
                    " | Seats: " + rs.getInt("total_seats") +
                    " | Price: Rs." + rs.getBigDecimal("ticket_price")
                );
            }
        }
    }

    // MODULE 3: Seat Booking with Transaction and Validation
    static void seatBooking() throws SQLException {

        System.out.print("Enter Show ID: ");
        int showId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Customer Name: ");
        String customerName = sc.nextLine();

        System.out.print("Enter Seat Number: ");
        int seatNumber = sc.nextInt();
        sc.nextLine();

        try (Connection con = getConnection()) {

            con.setAutoCommit(false);

            try {
                int totalSeats;
                double price;

                String checkSql =
                    "SELECT total_seats, ticket_price FROM shows " +
                    "WHERE show_id = ? FOR UPDATE";

                try (PreparedStatement ps = con.prepareStatement(checkSql)) {

                    ps.setInt(1, showId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Show ID not found.");
                        }

                        totalSeats = rs.getInt("total_seats");
                        price = rs.getDouble("ticket_price");
                    }
                }

                if (seatNumber < 1 || seatNumber > totalSeats) {
                    throw new SQLException(
                        "Invalid seat! Choose between 1 and " + totalSeats
                    );
                }

                String insertSql =
                    "INSERT INTO bookings " +
                    "(show_id, customer_name, seat_number) VALUES(?, ?, ?)";

                int bookingId;

                try (PreparedStatement ps = con.prepareStatement(
                        insertSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, showId);
                    ps.setString(2, customerName);
                    ps.setInt(3, seatNumber);

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException("Booking ID not generated.");
                        }

                        bookingId = keys.getInt(1);
                    }
                }

                con.commit();

                System.out.println("\nBooking Successful!");
                System.out.println("Booking ID: " + bookingId);
                System.out.println("Seat Number: " + seatNumber);
                System.out.println("Ticket Price: Rs." + price);

            } catch (SQLException e) {

                con.rollback();

                if (e.getErrorCode() == 1062) {
                    System.out.println("This seat is already booked!");
                } else {
                    System.out.println("Booking Failed: " + e.getMessage());
                }

            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // MODULE 4: Ticket Generation
    static void ticketGeneration() throws SQLException {

        System.out.print("Enter Booking ID: ");
        int bookingId = sc.nextInt();
        sc.nextLine();

        String sql =
            "SELECT b.booking_id, b.customer_name, b.seat_number, " +
            "m.movie_name, t.theater_name, t.location, " +
            "s.show_time, s.ticket_price " +
            "FROM bookings b " +
            "JOIN shows s ON b.show_id = s.show_id " +
            "JOIN movies m ON s.movie_id = m.movie_id " +
            "JOIN theaters t ON s.theater_id = t.theater_id " +
            "WHERE b.booking_id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    System.out.println("\n========== MOVIE TICKET ==========");
                    System.out.println("Booking ID : " + rs.getInt("booking_id"));
                    System.out.println("Customer   : " + rs.getString("customer_name"));
                    System.out.println("Movie      : " + rs.getString("movie_name"));
                    System.out.println("Theater    : " + rs.getString("theater_name"));
                    System.out.println("Location   : " + rs.getString("location"));
                    System.out.println("Show Time  : " + rs.getString("show_time"));
                    System.out.println("Seat       : " + rs.getInt("seat_number"));
                    System.out.println("Price      : Rs." + rs.getBigDecimal("ticket_price"));
                    System.out.println("==================================");

                } else {
                    System.out.println("Booking ID not found!");
                }
            }
        }
    }

    // Main Method
    public static void main(String[] args) {

        try {

            createTables();
            insertSampleData();

            while (true) {

                System.out.println("\n===== MOVIE TICKET BOOKING SYSTEM =====");
                System.out.println("1. Movie Listing");
                System.out.println("2. Theater Management");
                System.out.println("3. Seat Booking");
                System.out.println("4. Ticket Generation");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                int choice = sc.nextInt();
                sc.nextLine();

                try {
                    switch (choice) {

                        case 1:
                            movieListing();
                            break;

                        case 2:
                            theaterManagement();
                            break;

                        case 3:
                            seatBooking();
                            break;

                        case 4:
                            ticketGeneration();
                            break;

                        case 5:
                            System.out.println("Thank you!");
                            sc.close();
                            return;

                        default:
                            System.out.println("Invalid choice!");
                    }

                } catch (SQLException e) {
                    System.out.println("Database Error: " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }
    }
}