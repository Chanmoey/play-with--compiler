package com.moon.playwithcomplier.lab.craft;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Chanmoey
 * Create at 2024/3/11
 */
public interface ASTNode{
    //父节点
    public ASTNode getParent();

    //子节点
    public List<ASTNode> getChildren();

    //AST类型
    public ASTNodeType getType();

    //文本值
    public String getText();
}
