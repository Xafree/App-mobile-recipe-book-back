package fr.gymgod.etl.gateway.refdata;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommonService {


    public String getValue(String[] columns, int index) {
        if (index >= columns.length) return null;
        String val = columns[index].trim();
        return val.isEmpty() ? null : val;
    }

    public int getValueInt(String[] columns, int index) {
        if(index >= columns.length || columns[index].isEmpty()){
            return 0;
        }
        return Integer.parseInt(columns[index].trim());
    }

    public double getValueDouble(String[] columns, int index){
        if (index >= columns.length || columns[index].isEmpty()) {
            return 0;
        }
        return Double.parseDouble(columns[index].trim());
    }

    /**
     * Découpe une chaîne par virgules, en ignorant les virgules situées à l'intérieur de parenthèses.
     * Gère les parenthèses imbriquées.
     */
    public List<String> splitIgnoringParentheses(String input) {
        List<String> result = new ArrayList<>();
        if (input == null || input.isEmpty()) return result;

        StringBuilder current = new StringBuilder();
        int parenthesisLevel = 0;

        int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            
            if (c == '(') {
                parenthesisLevel++;
                current.append(c);
            } else if (c == ')') {
                if (parenthesisLevel > 0) {
                    parenthesisLevel--;
                }
                current.append(c);
            } else if (c == ',' && parenthesisLevel == 0) {
                // Check for decimal comma (digit BEFORE and digit AFTER)
                // Example: "14,7" or "<0,1"
                boolean isDecimal = false;
                if (i > 0 && i < length - 1) {
                    if (Character.isDigit(input.charAt(i - 1)) && Character.isDigit(input.charAt(i + 1))) {
                        isDecimal = true;
                    }
                }

                if (isDecimal) {
                    current.append(c);
                } else {
                    // Split only if not a decimal comma
                    addSegment(result, current);
                }
            } else {
                current.append(c);
            }
        }

        // Ajouter le dernier segment
        if (!current.isEmpty()) {
            addSegment(result, current);
        }

        return result;
    }

    private void addSegment(List<String> result, StringBuilder current) {
        String segment = current.toString().trim();
        if (segment.length() > 255) {
            segment = segment.substring(0, 255);
        }
        if (!segment.isEmpty()) {
            result.add(segment);
        }
        current.setLength(0); // Reset du buffer
    }
}
