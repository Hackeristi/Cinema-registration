package pl.rsi.cinema;

import javafx.scene.control.Alert;

public class LoginController {

    // 1. UserService nie może być @FXML i nie musi być static (choć może)
    private static final UserService userService = UserService.getInstance();

    // 2. Metody walidacji i logowania zostawiamy jako STATIC,
    // aby BookingController mógł je łatwo wywołać bez tworzenia obiektu.

    public static String validateLogin(String email, String password) {
        if (email == null || email.trim().isEmpty())
            return "Proszę wprowadzić email";
        if (password == null || password.trim().isEmpty())
            return "Proszę wprowadzić hasło";
        return null;
    }

    public static boolean performLogin(String email, String password) {
        return userService.login(email, password);
    }

    public static User getCurrentUser() {
        return userService.getCurrentUser();
    }

    // Metoda pomocnicza do błędów - też static, żeby była pod ręką
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd logowania");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // USUNĘLIŚMY: handleLogin i handleRegisterClick.
    // Dlaczego? Bo te akcje (klikanie) obsługuje teraz BookingController
    // swoimi metodami handleLoginClick() i showRegisterForm().
}