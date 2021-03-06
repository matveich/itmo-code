package parser;

import Types.Type;
import com.sun.istack.internal.NotNull;
import exceptions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Tokenizer<T> {
    private int iterator;
    private String restExpression;
    private TokenType currentToken;
    private T value;
    private int bracketBalance;
    private String varName;
    private HashMap<String, TokenType> binaryOpsCharMap, unaryOpsCharMap;
    private List<String> operations;

    private Type<T> type;

    public Tokenizer(@NotNull String expression, Type<T> type) {
        iterator = 0;
        bracketBalance = 0;
        currentToken = TokenType.BEGIN;
        restExpression = expression;

        binaryOpsCharMap = new HashMap<>();
        binaryOpsCharMap.put("+", TokenType.ADD);
        binaryOpsCharMap.put("-", TokenType.SUB);
        binaryOpsCharMap.put("*", TokenType.MUL);
        binaryOpsCharMap.put("/", TokenType.DIV);
        binaryOpsCharMap.put("min", TokenType.MIN);
        binaryOpsCharMap.put("max", TokenType.MAX);

        unaryOpsCharMap = new HashMap<>();
        unaryOpsCharMap.put("count", TokenType.COUNT);

        operations = Arrays.asList("+", "-", "*", "/", "min", "max", "count");

        this.type = type;
    }

    public TokenType getCurrentToken() {
        return currentToken;
    }

    public int getIterator() {
        return iterator;
    }

    public T getValue() {
        return value;
    }

    public String getVarName() {
        return varName;
    }

    public String getRestExpression() {
        return restExpression;
    }

    private void skipSpaces() {
        while (iterator < restExpression.length() && Character.isWhitespace(restExpression.charAt(iterator))) {
            iterator++;
        }
    }

    private void checkOperand() throws MissingOperandException {
        if (binaryOpsCharMap.containsValue(currentToken)
                || currentToken == TokenType.LP || currentToken == TokenType.BEGIN) {
            throw new MissingOperandException(iterator, restExpression);
        }
    }

    private void checkOperation() throws MissingOperationException {
        if (currentToken == TokenType.RP || currentToken == TokenType.VAR || currentToken == TokenType.CONST) {
            throw new MissingOperationException(iterator, restExpression);
        }
    }

    private boolean belongsToNumber(char c) {
        return Character.isDigit(c) || c == '.' || c == 'e';
    }

    private String getConst() {
        final StringBuilder sb = new StringBuilder();
        while (iterator < restExpression.length() && (belongsToNumber(restExpression.charAt(iterator)) ||
                ((restExpression.charAt(iterator) == '+' || restExpression.charAt(iterator) == '-') && iterator > 0
                && restExpression.charAt(iterator - 1) == 'e'))) {
            sb.append(restExpression.charAt(iterator));
            iterator++;
        }
        iterator--;
        return sb.toString();
    }

    private boolean operationsContainsStr(String s) {
        for (String op : operations) {
            if (op.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private String getOperation() {
        final StringBuilder sb = new StringBuilder();
        while (iterator < restExpression.length() &&
                (sb.toString().isEmpty() || operationsContainsStr(sb.toString()))) {
            sb.append(restExpression.charAt(iterator));
            iterator++;
        }
        iterator -= 2;
        if (iterator < 0) {
            iterator = 0;
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    public void getToken() throws ParserException {
        skipSpaces();
        if (iterator >= restExpression.length()) {
            checkOperand();
            currentToken = TokenType.END;
            return;
        }
        char c = restExpression.charAt(iterator);
        if (Character.isDigit(c)) {
            currentToken = TokenType.CONST;
            String stringValue = getConst();
            try {
                value = type.parseNumber(stringValue);
            }
            catch (InvalidTypeException e) {
                throw new InvalidNumberException(stringValue, iterator - stringValue.length() + 1, restExpression);
            }
            iterator++;
            return;
        }
        switch (c) {
            case '-':
                if (currentToken == TokenType.RP ||
                        currentToken == TokenType.CONST ||
                        currentToken == TokenType.VAR) {
                    currentToken = TokenType.SUB;
                }
                else {
                    if (iterator + 1 >= restExpression.length()) {
                        throw new MissingOperationException(iterator + 1, restExpression);
                    }
                    if (Character.isDigit(restExpression.charAt(iterator + 1))) {
                        iterator++;
                        String stringValue = getConst();
                        currentToken = TokenType.CONST;
                        try {
                            value = type.parseNumber("-" + stringValue);
                        }
                        catch (InvalidTypeException e) {
                            throw new InvalidNumberException("-" + stringValue,
                                    iterator - stringValue.length(), restExpression);
                        }
                    }
                    else {
                        currentToken = TokenType.MINUS;
                    }
                }
                break;
            case 'x':
            case 'y':
            case 'z':
                currentToken = TokenType.VAR;
                varName = String.valueOf(c);
                break;
            case '(':
                checkOperation();
                bracketBalance++;
                currentToken = TokenType.LP;
                break;
            case ')':
                if (binaryOpsCharMap.containsValue(currentToken)) {
                    throw new MissingOperandException(iterator, restExpression);
                }
                bracketBalance--;
                if (bracketBalance < 0) {
                    throw new ParenthesisImbalanceException(iterator, restExpression);
                }
                currentToken = TokenType.RP;
                break;
            default:
                String operation = getOperation();
                if (binaryOpsCharMap.containsKey(operation)) {
                    checkOperand();
                    currentToken = binaryOpsCharMap.get(operation);
                    break;
                }
                else if (unaryOpsCharMap.containsKey(operation)) {
                    currentToken = unaryOpsCharMap.get(operation);
                    break;
                }
                else {
                    throw new UnknownSymbolException(iterator, restExpression);
                }
        }
        iterator++;
    }
}
