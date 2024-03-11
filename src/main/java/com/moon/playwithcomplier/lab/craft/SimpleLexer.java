package com.moon.playwithcomplier.lab.craft;

import javax.xml.stream.events.DTD;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单词法分析器，能够解析文本，产出Token
 *
 * @author Chanmoey
 * Create at 2024/3/11
 */
public class SimpleLexer {

    public static void main(String[] args) {
        SimpleLexer lexer = new SimpleLexer();

        String script = "int age = 45;";
        System.out.println("parse :" + script);
        SimpleTokenReader tokenReader = lexer.tokenize(script);
        dump(tokenReader);

        //测试inta的解析
        script = "inta age = 45;";
        System.out.println("\nparse :" + script);
        tokenReader = lexer.tokenize(script);
        dump(tokenReader);

        //测试in的解析
        script = "in age = 45;";
        System.out.println("\nparse :" + script);
        tokenReader = lexer.tokenize(script);
        dump(tokenReader);

        //测试>=的解析
        script = "age >= 45;";
        System.out.println("\nparse :" + script);
        tokenReader = lexer.tokenize(script);
        dump(tokenReader);

        //测试>的解析
        script = "age > 45;";
        System.out.println("\nparse :" + script);
        tokenReader = lexer.tokenize(script);
        dump(tokenReader);
    }

    private StringBuilder tokenText;

    private List<Token> tokens;

    private SimpleToken token;

    private boolean isAlpha(int ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
    }

    private boolean isDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isBlank(int ch) {
        return ch == ' ' || ch == '\t' || ch == '\n';
    }

    /**
     * 构建初始状态，或者进入其他状态
     *
     * @param ch
     * @return
     */
    private DfaState initToken(char ch) {

        // 在进入其他状态前，把原来的Token保存
        if (!tokenText.isEmpty()) {
            token.text = tokenText.toString();
            tokens.add(token);

            // 情况Token缓存
            tokenText.setLength(0);
            token = new SimpleToken();
        }

        DfaState newState = DfaState.Initial;
        // 第一符号是字母，可能是变量名，也可能是关键字int
        if (isAlpha(ch)) {
            if (ch == 'i') {
                newState = DfaState.Id_int1;
            } else {
                newState = DfaState.Id;
            }
            token.type = TokenType.Identifier;
            tokenText.append(ch);
        } else if (isDigit(ch)) {
            newState = DfaState.IntLiteral;
            token.type = TokenType.IntLiteral;
            tokenText.append(ch);
        } else if (ch == '>') {
            newState = DfaState.GT;
            token.type = TokenType.GT;
            tokenText.append(ch);
        } else if (ch == '+') {
            newState = DfaState.Plus;
            token.type = TokenType.Plus;
            tokenText.append(ch);
        } else if (ch == '-') {
            newState = DfaState.Minus;
            token.type = TokenType.Minus;
            tokenText.append(ch);
        } else if (ch == '*') {
            newState = DfaState.Star;
            token.type = TokenType.Star;
            tokenText.append(ch);
        } else if (ch == '/') {
            newState = DfaState.Slash;
            token.type = TokenType.Slash;
            tokenText.append(ch);
        } else if (ch == ';') {
            newState = DfaState.SemiColon;
            token.type = TokenType.SemiColon;
            tokenText.append(ch);
        } else if (ch == '(') {
            newState = DfaState.LeftParen;
            token.type = TokenType.LeftParen;
            tokenText.append(ch);
        } else if (ch == ')') {
            newState = DfaState.RightParen;
            token.type = TokenType.RightParen;
            tokenText.append(ch);
        } else if (ch == '=') {
            newState = DfaState.Assignment;
            token.type = TokenType.Assignment;
            tokenText.append(ch);
        }
        return newState;
    }

    public SimpleTokenReader tokenize(String code) {
        tokens = new ArrayList<>();
        CharArrayReader reader = new CharArrayReader(code.toCharArray());
        tokenText = new StringBuilder();
        token = new SimpleToken();
        int ich = 0;
        char ch = 0;
        DfaState state = DfaState.Initial;
        try {
            while ((ich = reader.read()) != -1) {
                ch = (char) ich;
                switch (state) {
                    case Initial -> {
                        state = initToken(ch);
                    }
                    case Id -> {
                        // 如果是变量，且字符还是字母和数字，则继续是变量
                        if (isAlpha(ch) || isDigit(ch)) {
                            tokenText.append(ch);
                        } else {
                            state = initToken(ch);
                        }
                    }
                    case GT -> {
                        if (ch == '=') {
                            token.type = TokenType.GE;
                            state = DfaState.GE;
                            tokenText.append(ch);
                        } else {
                            state = initToken(ch);
                        }
                    }
                    case GE, Assignment, Plus, Minus, Star, Slash, SemiColon, LeftParen, RightParen -> {
                        state = initToken(ch);
                    }
                    case IntLiteral -> {
                        if (isDigit(ch)) {
                            tokenText.append(ch);
                        } else {
                            state = initToken(ch);
                        }
                    }
                    case Id_int1 -> {
                        if (ch == 'n') {
                            state = DfaState.Id_int2;
                            tokenText.append(ch);
                        } else if (isDigit(ch) || isAlpha(ch)) {
                            state = DfaState.Id;    //切换回Id状态
                            tokenText.append(ch);
                        } else {
                            state = initToken(ch);
                        }
                    }
                    case Id_int2 -> {
                        if (ch == 't') {
                            state = DfaState.Id_int3;
                            tokenText.append(ch);
                        } else if (isDigit(ch) || isAlpha(ch)) {
                            state = DfaState.Id;    //切换回id状态
                            tokenText.append(ch);
                        } else {
                            state = initToken(ch);
                        }
                    }
                    case Id_int3 -> {
                        if (isBlank(ch)) {
                            token.type = TokenType.Int;
                            state = initToken(ch);
                        } else {
                            state = DfaState.Id;    //切换回Id状态
                            tokenText.append(ch);
                        }
                    }
                }
            }
            if (!tokenText.isEmpty()) {
                initToken(ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new SimpleTokenReader(tokens);
    }

    public static void dump(SimpleTokenReader tokenReader){
        System.out.println("text\ttype");
        Token token = null;
        while ((token= tokenReader.read())!=null){
            System.out.println(token.getText()+"\t\t"+token.getType());
        }
    }

    private final class SimpleToken implements Token {

        private TokenType type;

        private String text;

        @Override
        public TokenType getType() {
            return type;
        }

        @Override
        public String getText() {
            return text;
        }
    }

    /**
     * 有限状态机的各种状态
     */
    private enum DfaState {
        Initial,

        If, Id_if1, Id_if2, Else, Id_else1, Id_else2, Id_else3, Id_else4, Int, Id_int1, Id_int2, Id_int3, Id, GT, GE,

        Assignment,

        Plus, Minus, Star, Slash,

        SemiColon,
        LeftParen,
        RightParen,

        IntLiteral
    }

    private class SimpleTokenReader implements TokenReader {
        List<Token> tokens = null;
        int pos = 0;

        public SimpleTokenReader(List<Token> tokens) {
            this.tokens = tokens;
        }

        @Override
        public Token read() {
            if (pos < tokens.size()) {
                return tokens.get(pos++);
            }
            return null;
        }

        @Override
        public Token peek() {
            if (pos < tokens.size()) {
                return tokens.get(pos);
            }
            return null;
        }

        @Override
        public void unread() {
            if (pos > 0) {
                pos--;
            }
        }

        @Override
        public int getPosition() {
            return pos;
        }

        @Override
        public void setPosition(int position) {
            if (position >= 0 && position < tokens.size()) {
                pos = position;
            }
        }
    }
}
