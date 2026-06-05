package pl.rsi.cinema;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

public class SeatController {

    public static final String COLOR_FREE = "#808080";
    public static final String COLOR_OCCUPIED = "#393939";
    public static final String COLOR_RESERVED = "#FF0000";

    private BookingController bookingController;

    public void setBookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public void processSeatClick(ActionEvent event) {
        if (!(event.getSource() instanceof Button clickedButton))
            return;

        Integer rowIndex = GridPane.getRowIndex(clickedButton);
        Integer colIndex = GridPane.getColumnIndex(clickedButton);

        int row = (rowIndex == null ? 0 : rowIndex) + 1;
        int col = (colIndex == null ? 0 : colIndex) + 1;
        String seatKey = row + "," + col;

        String style = clickedButton.getStyle();
        if (style.contains(COLOR_OCCUPIED))
            return;

        if (style.contains(COLOR_RESERVED)) {
            updateButtonColor(clickedButton, COLOR_FREE);
            bookingController.removeSeatFromList(seatKey);
        } else {
            updateButtonColor(clickedButton, COLOR_RESERVED);
            bookingController.addSeatToList(seatKey, row, col);
        }
    }

    // Metoda pomocnicza do aktualizacji koloru przez obiekt przycisku
    private void updateButtonColor(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex
                + "; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28; -fx-text-fill: white;");
    }

    // Ustawia wybrane (czerwone) miejsca na kolor ciemnoszary (zajęty)
    public void markSelectedAsOccupied(Set<String> keys) {
        for (String key : keys) {
            Button btn = findButtonByKey(key); // Musisz mieć metodę mapującą klucz "row,col" na Button
            if (btn != null) {
                btn.setStyle("-fx-background-color: " + COLOR_OCCUPIED
                        + "; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
            }
        }
    }

    // Resetuje kolor jednego miejsca z powrotem na szary (wolny)
    public void resetSingleSeatColor(String key) {
        Button btn = findButtonByKey(key);
        if (btn != null) {
            btn.setStyle("-fx-background-color: " + COLOR_FREE
                    + "; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
        }
    }

    private final Map<String, Button> seatButtonsMap = new HashMap<>();

    // 2. Metoda, która przeskanuje GridPane i wypełni mapę
    public void initSeatMap(GridPane seatGrid) {
        seatButtonsMap.clear();
        for (Node node : seatGrid.getChildren()) {
            if (node instanceof Button button) {
                Integer rowIndex = GridPane.getRowIndex(button);
                Integer colIndex = GridPane.getColumnIndex(button);

                // Standaryzujemy indeksy (jeśli null, to 0) + 1 (zgodnie z Twoją logiką
                // seatKey)
                int r = (rowIndex == null ? 0 : rowIndex) + 1;
                int c = (colIndex == null ? 0 : colIndex) + 1;

                seatButtonsMap.put(r + "," + c, button);
            }
        }
    }

    // 3. To jest właśnie ta "metoda mapująca"
    private Button findButtonByKey(String key) {
        return seatButtonsMap.get(key);
    }

    // Czyści całą salę (używane przy zmianie daty)
    public void resetAllSeatsToFree() {
        for (Button btn : seatButtonsMap.values()) {
            btn.setStyle("-fx-background-color: " + COLOR_FREE
                    + "; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28; -fx-text-fill: white;");
        }
    }

    public void markOccupiedOnGrid(Set<String> occupiedKeys) {
        for (String key : occupiedKeys) {
            Button btn = seatButtonsMap.get(key);
            if (btn != null) {
                btn.setStyle("-fx-background-color: " + COLOR_OCCUPIED
                        + "; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
            }
        }
    }

    public void preselectSeatColor(String key) {
        Button btn = seatButtonsMap.get(key);
        if (btn != null) {
            updateButtonColor(btn, COLOR_RESERVED);
        }
    }
}