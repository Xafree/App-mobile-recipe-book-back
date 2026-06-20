package fr.gymgod.app.nutrition.domain;

import java.util.ArrayList;
import java.util.List;

/// Génère les variantes plausibles d'un code-barres scanné/saisi pour
/// compenser les erreurs de format courantes (zéro initial UPC-A manquant,
/// chiffre de contrôle EAN-13 omis) avant de chercher en base ou sur OFF.
///
/// Exemple réel : `68113179958` (11 chiffres, saisi sans le zéro initial ni
/// le chiffre de contrôle) correspond en réalité à `0681131799584`
/// (EAN-13). {@link #candidates(String)} reconstruit cette forme.
public final class BarcodeNormalizer {

    private BarcodeNormalizer() {
    }

    /// Renvoie le code tel que fourni, suivi des variantes normalisées à
    /// essayer dans l'ordre — sans doublons.
    public static List<String> candidates(String code) {
        List<String> result = new ArrayList<>();
        if (code == null) {
            return result;
        }
        result.add(code);

        if (!code.matches("\\d+")) {
            return result;
        }

        switch (code.length()) {
            case 12 -> addIfAbsent(result, "0" + code); // UPC-A → EAN-13
            case 11 -> {
                // Zéro initial UPC-A manquant ET chiffre de contrôle omis :
                // on reconstruit les 12 chiffres puis on calcule le 13e.
                String twelve = "0" + code;
                addIfAbsent(result, twelve + eanCheckDigit(twelve));
            }
            case 13 -> {
                if (code.startsWith("0")) {
                    addIfAbsent(result, code.substring(1)); // EAN-13 → UPC-A
                }
            }
            default -> {
            }
        }

        return result;
    }

    /// Chiffre de contrôle EAN-13/GTIN-13 pour les 12 premiers chiffres
    /// (pondération 1-3 alternée de gauche à droite).
    private static int eanCheckDigit(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = twelveDigits.charAt(i) - '0';
            sum += digit * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - (sum % 10)) % 10;
    }

    private static void addIfAbsent(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }
}
