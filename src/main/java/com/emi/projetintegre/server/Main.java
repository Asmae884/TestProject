package com.emi.projetintegre.server;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager db = new DatabaseManager(
                "jdbc:mysql://localhost:3306/SecureCommDB",
                "secureapp",
                "THISisFUNNY&&5627"
            );

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== MENU ===");
                System.out.println("1. Add user");
                System.out.println("2. Show all users");
                System.out.println("3. Delete user");
                System.out.println("4. Populate RSA keys for existing users");
                System.out.println("5. Change user password");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // consume leftover newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter username: ");
                        String username = scanner.nextLine();

                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();

                        System.out.print("Enter validity period in months: ");
                        int months = scanner.nextInt();

                        System.out.print("Is admin? (true/false): ");
                        boolean isAdmin = scanner.nextBoolean();

                        db.addUser(username, password, months, isAdmin);
                        break;

                    case 2:
                        db.showUsers();
                        break;

                    case 3:
                        System.out.print("Enter user ID to delete: ");
                        int userId = scanner.nextInt();

                        if (db.deleteUser(userId)) {
                            System.out.println("User deleted successfully.");
                        } else {
                            System.out.println("Failed to delete user. Check ID.");
                        }
                        break;

                    case 4:
                        System.out.println("Populating RSA keys for existing users...");
                        db.populateRSAKeys();
                        break;

                    case 5:
                        System.out.print("Enter user ID to change password: ");
                        int changeUserId = scanner.nextInt();
                        scanner.nextLine(); // consume leftover newline
                        System.out.print("Enter new password: ");
                        String newPassword = scanner.nextLine();

                        if (db.changePassword(changeUserId, newPassword)) {
                            System.out.println("Password changed successfully.");
                        } else {
                            System.out.println("Failed to change password. Check ID.");
                        }
                        break;

                    case 6:
                        System.out.println("Exiting...");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Invalid choice! Please enter 1-6.");
                        break;
                }
            }

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
        }
    }
}