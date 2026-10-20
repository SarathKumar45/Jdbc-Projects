package student_jdbc;
import java.sql.*;
import java.math.BigDecimal;
import java.util.Scanner;

public class StudentManagementSystem {

    static final String URL =
            "jdbc:mysql://localhost:3306/student_management";
    static final String USER = "root";
    static final String PASSWORD = "root";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        createTables();

        while (true) {
            System.out.println("\n===== STUDENT MANAGEMENT SYSTEM =====");
            System.out.println("1. Student Registration");
            System.out.println("2. Course Enrollment");
            System.out.println("3. Attendance Management");
            System.out.println("4. Marks Entry");
            System.out.println("5. Result Generation");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            try {
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1: registerStudent(); break;
                    case 2: enrollCourse(); break;
                    case 3: manageAttendance(); break;
                    case 4: enterMarks(); break;
                    case 5: generateResult(); break;
                    case 6:
                        System.out.println("Thank you!");
                        return;
                    default:
                        System.out.println("Invalid choice!");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    static void createTables() {
        String[] queries = {
            """
            CREATE TABLE IF NOT EXISTS students (
                student_id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) NOT NULL UNIQUE,
                phone VARCHAR(15)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS courses (
                course_id INT PRIMARY KEY AUTO_INCREMENT,
                course_name VARCHAR(100) NOT NULL UNIQUE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS enrollments (
                enrollment_id INT PRIMARY KEY AUTO_INCREMENT,
                student_id INT NOT NULL,
                course_id INT NOT NULL,
                enrollment_date DATE NOT NULL,
                FOREIGN KEY (student_id) REFERENCES students(student_id),
                FOREIGN KEY (course_id) REFERENCES courses(course_id),
                UNIQUE(student_id, course_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS attendance (
                attendance_id INT PRIMARY KEY AUTO_INCREMENT,
                enrollment_id INT NOT NULL,
                attendance_date DATE NOT NULL,
                status VARCHAR(10) NOT NULL,
                FOREIGN KEY (enrollment_id)
                    REFERENCES enrollments(enrollment_id),
                UNIQUE(enrollment_id, attendance_date),
                CHECK (status IN ('PRESENT', 'ABSENT'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS marks (
                mark_id INT PRIMARY KEY AUTO_INCREMENT,
                enrollment_id INT NOT NULL,
                exam_name VARCHAR(50) NOT NULL,
                marks_obtained DECIMAL(6,2) NOT NULL,
                max_marks DECIMAL(6,2) NOT NULL,
                FOREIGN KEY (enrollment_id)
                    REFERENCES enrollments(enrollment_id),
                UNIQUE(enrollment_id, exam_name),
                CHECK (max_marks > 0),
                CHECK (marks_obtained >= 0 AND marks_obtained <= max_marks)
            )
            """
        };

        try (Connection con = connect();
             Statement st = con.createStatement()) {

            for (String query : queries) {
                st.executeUpdate(query);
            }

            // Sample courses for enrollment.
            st.executeUpdate("""
                INSERT IGNORE INTO courses(course_name)
                VALUES ('Java'), ('Python'), ('SQL')
                """);

            System.out.println("Tables are ready.");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // MODULE 1: Student Registration
    static void registerStudent() throws SQLException {
        System.out.print("Student name: ");
        String name = sc.nextLine();

        System.out.print("Email: ");
        String email = sc.nextLine();

        System.out.print("Phone: ");
        String phone = sc.nextLine();

        String sql = """
            INSERT INTO students(name, email, phone)
            VALUES (?, ?, ?)
            """;

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    System.out.println(
                            "Student registered. ID: " + rs.getInt(1));
                }
            }
        }
    }

    // MODULE 2: Course Enrollment
    static void enrollCourse() throws SQLException {
        System.out.println("Available courses:");

        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM courses ORDER BY course_id")) {

            while (rs.next()) {
                System.out.println(
                        rs.getInt("course_id") + ". " +
                        rs.getString("course_name"));
            }
        }

        System.out.print("Student ID: ");
        int studentId = Integer.parseInt(sc.nextLine());

        System.out.print("Course ID: ");
        int courseId = Integer.parseInt(sc.nextLine());

        String sql = """
            INSERT INTO enrollments(student_id, course_id, enrollment_date)
            VALUES (?, ?, ?)
            """;

        try (Connection con = connect()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setInt(2, courseId);
                ps.setDate(3, Date.valueOf(java.time.LocalDate.now()));

                ps.executeUpdate();
                con.commit();

                System.out.println("Course enrollment successful.");

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // MODULE 3: Attendance Management
    static void manageAttendance() throws SQLException {
        System.out.print("Enrollment ID: ");
        int enrollmentId = Integer.parseInt(sc.nextLine());

        System.out.print("Date (YYYY-MM-DD): ");
        Date date = Date.valueOf(sc.nextLine());

        System.out.print("Status (PRESENT/ABSENT): ");
        String status = sc.nextLine().trim().toUpperCase();

        if (!status.equals("PRESENT") && !status.equals("ABSENT")) {
            throw new IllegalArgumentException(
                    "Status must be PRESENT or ABSENT.");
        }

        String sql = """
            INSERT INTO attendance(enrollment_id, attendance_date, status)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE status = ?
            """;

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, enrollmentId);
            ps.setDate(2, date);
            ps.setString(3, status);
            ps.setString(4, status);

            ps.executeUpdate();
            System.out.println("Attendance saved successfully.");
        }
    }

    // MODULE 4: Marks Entry
    static void enterMarks() throws SQLException {
        System.out.print("Enrollment ID: ");
        int enrollmentId = Integer.parseInt(sc.nextLine());

        System.out.print("Exam name: ");
        String exam = sc.nextLine();

        System.out.print("Maximum marks: ");
        BigDecimal max = new BigDecimal(sc.nextLine());

        System.out.print("Marks obtained: ");
        BigDecimal obtained = new BigDecimal(sc.nextLine());

        if (max.compareTo(BigDecimal.ZERO) <= 0 ||
            obtained.compareTo(BigDecimal.ZERO) < 0 ||
            obtained.compareTo(max) > 0) {
            throw new IllegalArgumentException("Invalid marks.");
        }

        String sql = """
            INSERT INTO marks
                (enrollment_id, exam_name, marks_obtained, max_marks)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                marks_obtained = ?, max_marks = ?
            """;

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, enrollmentId);
            ps.setString(2, exam);
            ps.setBigDecimal(3, obtained);
            ps.setBigDecimal(4, max);
            ps.setBigDecimal(5, obtained);
            ps.setBigDecimal(6, max);

            ps.executeUpdate();
            System.out.println("Marks entered successfully.");
        }
    }

    // MODULE 5: Result Generation
    static void generateResult() throws SQLException {
        String sql = """
            SELECT s.student_id, s.name, c.course_name,
                   m.exam_name, m.marks_obtained, m.max_marks,
                   ROUND(m.marks_obtained * 100 / m.max_marks, 2)
                       AS percentage
            FROM marks m
            JOIN enrollments e ON m.enrollment_id = e.enrollment_id
            JOIN students s ON e.student_id = s.student_id
            JOIN courses c ON e.course_id = c.course_id
            ORDER BY s.student_id, c.course_name, m.exam_name
            """;

        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n===== STUDENT RESULTS =====");

            boolean found = false;

            while (rs.next()) {
                found = true;
                BigDecimal percentage = rs.getBigDecimal("percentage");

                String result = percentage.compareTo(
                        new BigDecimal("40")) >= 0 ? "PASS" : "FAIL";

                System.out.println(
                    "Student ID: " + rs.getInt("student_id") +
                    " | Name: " + rs.getString("name") +
                    " | Course: " + rs.getString("course_name") +
                    " | Exam: " + rs.getString("exam_name") +
                    " | Marks: " + rs.getBigDecimal("marks_obtained") +
                    "/" + rs.getBigDecimal("max_marks") +
                    " | Percentage: " + percentage + "%" +
                    " | Result: " + result);
            }

            if (!found) {
                System.out.println("No marks entered yet.");
            }
        }
    }
}