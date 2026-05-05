package pl.rsi.cinema;

import javafx.scene.control.Alert;

public class RegisterController {

    // Metoda statyczna do walidacji - wywoływana przez BookingController
    public static String validateRegister(String name, String surname, String email, String password,
            String confirmPassword) {
        if (name == null || name.trim().isEmpty())
            return "Proszę wprowadzić imię";
        if (surname == null || surname.trim().isEmpty())
            return "Proszę wprowadzić nazwisko";
        if (email == null || !email.contains("@"))
            return "Proszę wprowadzić prawidłowy email";
        if (password == null || password.length() < 4)
            return "Hasło musi mieć min. 4 znaki";
        if (!password.equals(confirmPassword))
            return "Hasła nie są identyczne";
        return null;
    }

    // Pomocnik do wyświetlania komunikatów (Sukces/Błąd)
    public static void showInfo(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}