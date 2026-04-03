import java.sql.*;
try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/letcookgearData?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC","root","ducthinh123");
     Statement s = c.createStatement()) {
    s.execute("ALTER TABLE payments MODIFY COLUMN method VARCHAR(30) NOT NULL");
    System.out.println("ALTER_OK");
}
