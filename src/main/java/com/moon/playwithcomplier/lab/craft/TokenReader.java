package com.moon.playwithcomplier.lab.craft;

/**
 * 一个Token流，有Lexer生产，Parser可以从中获取Token
 *
 * @author Chanmoey
 * Create at 2024/3/11
 */
public interface TokenReader {

    Token read();

    Token peek();

    /**
     * 回到上一个Token
     */
    void unread();

    int getPosition();

    void setPosition(int position);
}
