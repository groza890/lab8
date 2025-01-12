package org.example;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/lab8";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner scanner = new Scanner(System.in)) {

            boolean running = true;

            while (running) {
                System.out.println("\n--- Meniu ---");
                System.out.println("1. Adaugă o persoană");
                System.out.println("2. Adaugă o excursie");
                System.out.println("3. Afișează toate persoanele și excursiile lor");
                System.out.println("4. Afișează excursiile unei persoane");
                System.out.println("5. Afișează persoanele care au vizitat o destinație");
                System.out.println("6. Afișează persoanele care au făcut excursii într-un an");
                System.out.println("7. Șterge o excursie");
                System.out.println("8. Șterge o persoană");
                System.out.println("0. Ieșire");
                System.out.print("Alege o opțiune: ");

                int option = scanner.nextInt();
                scanner.nextLine(); // Consumă newline

                switch (option) {
                    case 1 -> adaugaPersoana(connection, scanner);
                    case 2 -> adaugaExcursie(connection, scanner);
                    case 3 -> afisare_Persoane_Excursii(connection);
                    case 4 -> afisare_Excursiile_uneiPersoane(connection, scanner);
                    case 5 -> afisare_persoanele_dupa_o_destinație(connection, scanner);
                    case 6 -> afisare_persoane_dupaAN(connection, scanner);
                    case 7 -> stergere_Excursie(connection, scanner);
                    case 8 -> stergere_persoana(connection, scanner);
                    case 0 -> running = false;
                    default -> System.out.println("Opțiune invalidă. Încearcă din nou.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Eroare la conectarea la baza de date: " + e.getMessage());
        }
    }

    private static void adaugaPersoana(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți numele persoanei: ");
            String name = scanner.nextLine();

            System.out.print("Introduceți vârsta persoanei: ");
            int age = scanner.nextInt();
            scanner.nextLine();

            if (age < 0 || age > 120) {
                throw new IllegalArgumentException("Vârsta trebuie să fie între 0 și 120.");
            }

            String sql = "INSERT INTO persoane (nume, varsta) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setInt(2, age);
                stmt.executeUpdate();
                System.out.println("Persoana a fost adăugată cu succes.");
            }
        } catch (SQLException e) {
            System.out.println("Eroare la adăugarea persoanei: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Eroare: " + e.getMessage());
        }
    }

    private static void adaugaExcursie(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți ID-ul persoanei: ");
            int personId = scanner.nextInt();
            scanner.nextLine();

            String checkSql = "SELECT COUNT(*) FROM persoane WHERE id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, personId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("Persoana cu ID-ul specificat nu există.");
                        return;
                    }
                }
            }

            System.out.print("Introduceți destinația excursiei: ");
            String destination = scanner.nextLine();

            System.out.print("Introduceți anul excursiei: ");
            int year = scanner.nextInt();
            scanner.nextLine();

            if (year < 1900 || year > java.time.Year.now().getValue()) {
                throw new IllegalArgumentException("Anul excursiei trebuie să fie între 1900 și anul curent.");
            }

            String sql = "INSERT INTO excursii (id_persoana, destinatia, anul) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, personId);
                stmt.setString(2, destination);
                stmt.setInt(3, year);
                stmt.executeUpdate();
                System.out.println("Excursia a fost adăugată cu succes.");
            }

        } catch (SQLException e) {
            System.out.println("Eroare la adăugarea excursiei: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Eroare: " + e.getMessage());
        }
    }

    private static void afisare_Persoane_Excursii(Connection connection) {
        String sql = "SELECT p.id, p.nume, p.varsta, e.destinatia, e.anul " +
                "FROM persoane p LEFT JOIN excursii e ON p.id = e.id_persoana";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Map<Integer, String> persons = new HashMap<>();
            while (rs.next()) {
                int personId = rs.getInt("id");
                String name = rs.getString("nume");
                int age = rs.getInt("varsta");
                String destination = rs.getString("destinatia");
                int year = rs.getInt("anul");

                if (!persons.containsKey(personId)) {
                    persons.put(personId, name + " (" + age + " ani)");
                    System.out.println("Persoană: " + persons.get(personId));
                }

                if (destination != null) {
                    System.out.println("  Excursie: " + destination + " - " + year);
                }
            }

        } catch (SQLException e) {
            System.out.println("Eroare la afișarea persoanelor și excursiilor: " + e.getMessage());
        }
    }

    private static void afisare_persoanele_dupa_o_destinație(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți destinația: ");
            String destination = scanner.nextLine();

            String query = """
            SELECT DISTINCT p.nume 
            FROM persoane p
            JOIN excursii e ON p.id = e.id_persoana
            WHERE e.destinatia = ?""";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, destination);
            ResultSet result = stmt.executeQuery();

            if (!result.next()) {
                System.out.println("Nu există persoane care au vizitat destinația: " + destination);
                return;
            }

            System.out.println("Persoanele care au vizitat " + destination + ":");
            do {
                System.out.println("- " + result.getString("nume"));
            } while (result.next());

        } catch (SQLException e) {
            System.out.println("Eroare la afișarea persoanelor: " + e.getMessage());
        }
    }

    private static void afisare_persoane_dupaAN(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți anul: ");
            int year = scanner.nextInt();
            scanner.nextLine(); // Consumăm linia rămasă

            String query = """
            SELECT DISTINCT p.nume 
            FROM persoane p
            JOIN excursii e ON p.id = e.id_persoana
            WHERE e.anul = ?""";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, year);
            ResultSet result = stmt.executeQuery();

            if (!result.next()) {
                System.out.println("Nu există persoane care au făcut excursii în anul: " + year);
                return;
            }

            System.out.println("Persoanele care au făcut excursii în anul " + year + ":");
            do {
                System.out.println("- " + result.getString("nume"));
            } while (result.next());

        } catch (SQLException e) {
            System.out.println("Eroare la afișarea persoanelor: " + e.getMessage());
        }
    }

    private static void stergere_Excursie(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți ID-ul excursiei de șters: ");
            int tripId = scanner.nextInt();
            scanner.nextLine(); // Consumăm linia rămasă

            String query = "DELETE FROM excursii WHERE id_excursie = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, tripId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Excursia cu ID-ul " + tripId + " a fost ștearsă cu succes.");
            } else {
                System.out.println("Excursia cu ID-ul " + tripId + " nu există.");
            }

        } catch (SQLException e) {
            System.out.println("Eroare la ștergerea excursiei: " + e.getMessage());
        }
    }

    private static void stergere_persoana(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți ID-ul persoanei de șters: ");
            int personId = scanner.nextInt();
            scanner.nextLine(); // Consumăm linia rămasă

            connection.setAutoCommit(false); // Începem o tranzacție

            // Ștergem excursiile asociate
            String deleteTripsQuery = "DELETE FROM excursii WHERE id_persoana = ?";
            PreparedStatement deleteTripsStmt = connection.prepareStatement(deleteTripsQuery);
            deleteTripsStmt.setInt(1, personId);
            deleteTripsStmt.executeUpdate();

            // Ștergem persoana
            String deletePersonQuery = "DELETE FROM persoane WHERE id = ?";
            PreparedStatement deletePersonStmt = connection.prepareStatement(deletePersonQuery);
            deletePersonStmt.setInt(1, personId);

            int rowsAffected = deletePersonStmt.executeUpdate();
            if (rowsAffected > 0) {
                connection.commit(); // Confirmăm tranzacția
                System.out.println("Persoana cu ID-ul " + personId + " și excursiile asociate au fost șterse cu succes.");
            } else {
                connection.rollback(); // Anulăm tranzacția dacă persoana nu există
                System.out.println("Persoana cu ID-ul " + personId + " nu există.");
            }

        } catch (SQLException e) {
            try {
                connection.rollback(); // Anulăm tranzacția în caz de eroare
            } catch (SQLException rollbackEx) {
                System.out.println("Eroare la rollback: " + rollbackEx.getMessage());
            }
            System.out.println("Eroare la ștergerea persoanei: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true); // Revenim la AutoCommit
            } catch (SQLException e) {
                System.out.println("Eroare la resetarea AutoCommit: " + e.getMessage());
            }
        }
    }

    private static void afisare_Excursiile_uneiPersoane(Connection connection, Scanner scanner) {
        try {
            System.out.print("Introduceți numele persoanei: ");
            String name = scanner.nextLine();

            String selectPersonQuery = "SELECT id FROM persoane WHERE nume = ?";
            PreparedStatement selectPersonStmt = connection.prepareStatement(selectPersonQuery);
            selectPersonStmt.setString(1, name);
            ResultSet personResult = selectPersonStmt.executeQuery();

            if (!personResult.next()) {
                System.out.println("Persoana cu numele " + name + " nu există.");
                return;
            }

            int personId = personResult.getInt("id");

            String selectTripsQuery = "SELECT destinatia, anul FROM excursii WHERE id_persoana = ?";
            PreparedStatement selectTripsStmt = connection.prepareStatement(selectTripsQuery);
            selectTripsStmt.setInt(1, personId);
            ResultSet tripsResult = selectTripsStmt.executeQuery();

            System.out.println("Excursiile persoanei " + name + ":");
            boolean hasTrips = false;
            while (tripsResult.next()) {
                hasTrips = true;
                String destination = tripsResult.getString("destinatia");
                int year = tripsResult.getInt("anul");
                System.out.println("- Destinație: " + destination+ ", An: " + year);
            }
            if (!hasTrips) {
                System.out.println("Această persoană nu are excursii înregistrate.");
            }

        } catch (SQLException e) {
            System.out.println("Eroare la afișarea excursiilor: " + e.getMessage());
        }
    }
}
