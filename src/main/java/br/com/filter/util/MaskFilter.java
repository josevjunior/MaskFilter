package br.com.filter.util;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javax.swing.text.MaskFormatter;

/**
 * A filter to handle user input using the mask symbol format.<br>
 * The mask use the same {@link javax.swing.text.MaskFormatter} characters to
 * represent the content except for "'" which is used just for limit fixed char's
 * @version 0.1
 */
public class MaskFilter implements UnaryOperator<TextFormatter.Change> {

    private String mask;
    private String emptyMask;
    private String placeholder;
    private int maskMaxLegth;

    private List<MaskCharacter> maskCharacters;

    private static final char NUMBER = '#';
    private static final char UPPERCASE = 'U';
    private static final char LOWERCASE = 'L';
    private static final char CHAR_OR_NUMBER = 'A';
    private static final char ANY_CHAR = '?';
    private static final char ANYTHING = '*';
    private static final char HEX = 'H';
    private static final char ESCAPE = '\'';

    public MaskFilter(String mask, char placeholder) {
        this.mask = mask;
        this.placeholder = String.valueOf(placeholder);
        initMaskV2();
    }

    public MaskFilter(String mask) {
        this(mask, ' ');
    }

    private void initMaskV2() {

        maskCharacters = new LinkedList();

        String maskTempWithoutEscape = mask.replaceAll("\\'", "");
        boolean escapeSession = false;
        int escapedCharacters = 0;

        StringBuilder tempEmptyMask = new StringBuilder();

        for (int i = 0; i < mask.length(); i++) {
            char charAt = mask.charAt(i);
            if (isEscape(charAt)) {
                escapeSession = !escapeSession;
                escapedCharacters = escapedCharacters + 2;
                continue;
            }

            MaskCharacter maskCharacter = new MaskCharacter(
                    mask.charAt(i),
                    !escapeSession ? i : i - 1,
                    !isFixedChar(charAt),
                    !escapeSession ? isFixedChar(charAt) : escapeSession,
                    !escapeSession ? (i + 1) == mask.length() : (i + 1 - (escapedCharacters)) == maskTempWithoutEscape.length(),
                    !escapeSession ? i == 0 : i - 1 == 0);

            maskCharacters.add(maskCharacter);

            if (maskCharacter.isPlaceholder()) {
                tempEmptyMask.append(placeholder);
            } else if (maskCharacter.isFixedChar()) {
                tempEmptyMask.append(charAt);
            }

        }

        emptyMask = tempEmptyMask.toString();
        maskMaxLegth = emptyMask.length();
    }

    private boolean isEscape(char charAt) {
        return charAt == ESCAPE;
    }

    private boolean isFixedChar(char charAt) {
        return charAt != NUMBER
                && charAt != UPPERCASE
                && charAt != LOWERCASE
                && charAt != CHAR_OR_NUMBER
                && charAt != ANY_CHAR
                && charAt != ANYTHING
                && charAt != HEX
                && charAt != ESCAPE
                && charAt != placeholder.charAt(0);
    }

    private boolean isPlaceHolder(char charAt) {
        return placeholder.charAt(0) == charAt;
    }

    @Override
    public TextFormatter.Change apply(TextFormatter.Change t) {

        if (t.getControlNewText().isEmpty()) {
            t.setText(emptyMask);
            t.setAnchor(0);
            t.setCaretPosition(0);
            return t;
        }
        
        //Workaround to initial text called by setText()
        if(t.getControlText().isEmpty() && !t.getControlNewText().isEmpty()){
            t.setText(formatWithMaskFormatter(t.getText()));
            t.setAnchor(0);
            t.setCaretPosition(0);
            return t;
        }
        
        if (t.isAdded()) {
            t = adjustAddedText(t);
        } else if (t.isDeleted()) {
            t = adjustDeletedText(t);
        } else {
            t = adjustCaretToCorrectPosition(t);
        }

        return t;
    }

    private String formatWithMaskFormatter(String initialText){
        try {
            MaskFormatter maskFormatter = new MaskFormatter(mask);
            maskFormatter.setPlaceholder(placeholder);
            maskFormatter.setValueContainsLiteralCharacters(false);
            
            return maskFormatter.valueToString(initialText);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private TextFormatter.Change adjustDeletedText(TextFormatter.Change change) {
        int deleteStart = change.getRangeStart();
        int deleteEnd = change.getRangeEnd();
        int index = deleteEnd - 1;
        StringBuilder newDeletedText = new StringBuilder();
        for (int i = deleteEnd; i > deleteStart; i--, index--) {

            if (index < 0) {
                break;
            }
            MaskCharacter maskCharacter = maskCharacters.get(index);
            if (maskCharacter.isFixedChar()) {
                newDeletedText.append(maskCharacter.getValidationChar());
                if ((deleteEnd - deleteStart) == 1) {
                    i++;
                }
            } else {
                newDeletedText.append(placeholder);
            }
        }

        change.setText(newDeletedText.reverse().toString());
        change.setRange(deleteEnd - newDeletedText.length(), deleteEnd);
        if ((deleteEnd - deleteStart) == 1) {
            change.setCaretPosition(change.getControlCaretPosition() - newDeletedText.length());
            change.setAnchor(change.getControlAnchor() - newDeletedText.length());
        }

        change = adjustCaretToCorrectPosition(change);

        return change;
    }

    private TextFormatter.Change adjustAddedText(TextFormatter.Change change) {

        int addStart = change.getRangeStart();

        String addedText = change.getText();

        StringBuilder newAddedText = new StringBuilder();

        int addEnd = (addStart + addedText.length());
        for (int i = addStart, j = 0; i < addEnd; i++, j++) {

            if (i < maskMaxLegth) {

                MaskCharacter maskChar = maskCharacters.get(i);

                if (j < addedText.length()) {
                    char addedChar = change.getText().charAt(j);
                    addedChar = maskChar.transform(addedChar);

                    boolean isValid = maskChar.isValid(addedChar);
                    boolean isFixedChar = maskChar.isFixedChar();

                    if (isValid || isFixedChar) {
                        if (maskChar.isFixedChar()
                                && addedChar != maskChar.getValidationChar()) {
                            addEnd++;
                            j--;
                            newAddedText.append(maskChar.getValidationChar());
                            continue;
                        }

                        if (!maskChar.isValid(addedChar)
                                && addedChar != maskChar.getValidationChar()) {
                            if (addedText.length() == 1) {
                                return null;
                            }
                        }

                        if (isValid || addedChar == maskChar.getValidationChar()) {
                            newAddedText.append(addedChar);
                        }

                    } else {
                        return null;
                    }
                } else {
                    if (maskChar.isFixedChar()) {
                        newAddedText.append(maskChar.getValidationChar());
                    } else if (maskChar.isPlaceholder()) {
                        newAddedText.append(placeholder);
                    }
                }

            }
        }
        
        change.setText(newAddedText.toString());
        change.setRange(change.getRangeStart(), Math.min(maskMaxLegth, change.getRangeStart() + newAddedText.length()));

        change.setAnchor(change.getRangeEnd());
        change.setCaretPosition(change.getRangeEnd());

        change = adjustCaretToCorrectPosition(change);

        return change;
    }

    private TextFormatter.Change adjustCaretToCorrectPosition(TextFormatter.Change change) {
        //System.out.println("New caret position: " + change.getCaretPosition());
        if (change.getCaretPosition() > change.getControlCaretPosition()
                && change.getCaretPosition() + 1 <= change.getControlText().length()) {

            char charAt = emptyMask.charAt(change.getCaretPosition());

            if (!isPlaceHolder(charAt)) {
                if (change.getSelection().getLength() == 0) {
                    change.setAnchor(change.getAnchor() + 1);
                }
                change.setCaretPosition(change.getCaretPosition() + 1);
                adjustCaretToCorrectPosition(change);
            }

        } else if (change.getCaretPosition() < change.getControlCaretPosition()
                && change.getCaretPosition() <= change.getAnchor()) {
            MaskCharacter maskChar = maskCharacters.get(change.getCaretPosition());
            //char charAt = emptyMask.charAt(change.getCaretPosition());
            if (maskChar.isFixedChar() && change.getCaretPosition() > 0) {

                change.setCaretPosition(change.getCaretPosition() - 1);
                if (change.getSelection().getLength() == 0) {
                    change.setAnchor(change.getCaretPosition());
                }

                adjustCaretToCorrectPosition(change);

            }
        }
        return change;
    }

    /**
     * A representation of a character in the mask
     */
    private final class MaskCharacter {

        private final Character validationChar;
        private final boolean placeholder;
        private final int charPosition;
        private final boolean fixedChar;
        private final boolean lastChar;
        private final boolean firstChar;

        public MaskCharacter(Character validationChar, int charPosition, boolean isPlaceholder, boolean isFixedChar, boolean isLastChar, boolean isFirstChar) {
            this.validationChar = validationChar;
            this.charPosition = charPosition;
            this.placeholder = isPlaceholder;
            this.fixedChar = isFixedChar;
            this.lastChar = isLastChar;
            this.firstChar = isFirstChar;
        }

        /**
         * Verify if the character is a placeholder in initial mask format
         *
         * @return
         */
        public boolean isPlaceholder() {
            return placeholder;
        }

        /**
         * The content of the char itself
         *
         * @return # or U or L or A or ? or H or *
         */
        public Character getValidationChar() {
            return validationChar;
        }

        /**
         * Validate if the character is the correct representation of
         * validationChar()
         *
         * @param contentChar
         * @return
         */
        public boolean isValid(char contentChar) {
            switch (validationChar) {
                case ANYTHING:
                    return true;
                case NUMBER:
                    return Character.isDigit(contentChar);
                case LOWERCASE:
                case UPPERCASE:
                    return Character.isLetter(contentChar);
                case CHAR_OR_NUMBER:
                    return Character.isLetter(contentChar) || Character.isDigit(contentChar);
                case ANY_CHAR:
                    return Character.isLetter(contentChar);
                case HEX:
                    return Character.digit(contentChar, 16) != -1;
                default:
                    return false;
            }
        }

        /**
         * Used in U and L validation char e tranform the char in correct case
         *
         * @param contentChar
         * @return
         */
        private char transform(char contentChar) {
            switch (validationChar) {
                case LOWERCASE:
                    return Character.toLowerCase(contentChar);
                case UPPERCASE:
                    return Character.toUpperCase(contentChar);
                default:
                    return contentChar;
            }
        }

        /**
         * Verify if char is fixed in mask
         *
         * @return
         */
        public boolean isFixedChar() {
            return fixedChar;
        }

        public int getCharPosition() {
            return charPosition;
        }

        public boolean isLastChar() {
            return lastChar;
        }

        public boolean isFirstChar() {
            return firstChar;
        }

        @Override
        public String toString() {
            return validationChar + "";
        }

    }

}
