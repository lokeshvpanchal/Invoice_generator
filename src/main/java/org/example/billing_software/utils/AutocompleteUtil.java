package org.example.billing_software.utils;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class AutocompleteUtil {

    public static void addAutocomplete(TextField textField, TrieAutocomplete trie) {
        ContextMenu suggestionsPopup = new ContextMenu();
        suggestionsPopup.setAutoHide(true);

        final int[] selectedIndex = { -1 };

        textField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.isEmpty()) {
                suggestionsPopup.hide();
                return;
            }

            List<String> suggestions = trie.getSuggestions(newText);
            if (suggestions.isEmpty()) {
                suggestionsPopup.hide();
                return;
            }

            List<CustomMenuItem> menuItems = new ArrayList<>();
            for (String suggestion : suggestions) {
                Label entryLabel = new Label(capitalizeFirst(suggestion));
                CustomMenuItem item = new CustomMenuItem(entryLabel, true);
                item.setOnAction(e -> {
                    textField.setText(capitalizeFirst(suggestion));
                    suggestionsPopup.hide();
                });
                menuItems.add(item);
            }

            selectedIndex[0] = -1;
            suggestionsPopup.getItems().setAll(menuItems);
            suggestionsPopup.show(textField, Side.BOTTOM, 0, 0);
        });

        // Keyboard navigation
        textField.setOnKeyPressed(event -> {
            if (!suggestionsPopup.isShowing()) return;
            int itemCount = suggestionsPopup.getItems().size();

            switch (event.getCode()) {
                case DOWN -> {
                    if (itemCount == 0) break;
                    selectedIndex[0] = (selectedIndex[0] + 1) % itemCount;
                    highlightItem(suggestionsPopup, selectedIndex[0]);
                    event.consume();
                }
                case UP -> {
                    if (itemCount == 0) break;
                    selectedIndex[0] = (selectedIndex[0] - 1 + itemCount) % itemCount;
                    highlightItem(suggestionsPopup, selectedIndex[0]);
                    event.consume();
                }
                case ENTER -> {
                    if (selectedIndex[0] >= 0 && selectedIndex[0] < itemCount) {
                        MenuItem selectedItem = suggestionsPopup.getItems().get(selectedIndex[0]);
                        selectedItem.fire();
                        event.consume();
                    }
                }
            }
        });

        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) suggestionsPopup.hide();
        });
    }

    private static void highlightItem(ContextMenu popup, int index) {
        for (int i = 0; i < popup.getItems().size(); i++) {
            Node content = ((CustomMenuItem) popup.getItems().get(i)).getContent();
            content.setStyle(i == index ? "-fx-background-color: #BDE4FF;" : "");
        }
    }

    private static String capitalizeFirst(String word) {
        return word.isEmpty() ? word :
                Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

}
