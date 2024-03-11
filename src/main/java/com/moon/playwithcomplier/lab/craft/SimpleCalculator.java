package com.moon.playwithcomplier.lab.craft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chanmoey
 * Create at 2024/3/11
 */
public class SimpleCalculator {

    public static void main(String[] args) {
        SimpleCalculator calculator = new SimpleCalculator();

        //测试变量声明语句的解析
        String script = "int a = b+3;";
        System.out.println("解析变量声明语句: " + script);
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(script);
        try {
            SimpleASTNode node = calculator.intDeclare(tokens);
            calculator.dumpAST(node,"");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }

        //测试表达式
        script = "2+3*5";
        System.out.println("\n计算: " + script + "，看上去一切正常。");
        calculator.evaluate(script);

        //测试语法错误
        script = "2+";
        System.out.println("\n: " + script + "，应该有语法错误。");
        calculator.evaluate(script);

        script = "2+3+4";
        System.out.println("\n计算: " + script + "，结合性出现错误。");
        calculator.evaluate(script);
    }


    public void evaluate(String script) {
        try {
            ASTNode tree = parse(script);
            dumpAST(tree, "");
            evaluate(tree, "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public ASTNode parse(String code) throws Exception {
        SimpleLexer lexer = new SimpleLexer();
        TokenReader tokens = lexer.tokenize(code);

        return prog(tokens);
    }

    private int evaluate(ASTNode node, String indent) {
        int result = 0;
        System.out.println(indent + "Calculating: " + node.getType());
        switch (node.getType()) {
            case Programm:
                for (ASTNode child : node.getChildren()) {
                    result = evaluate(child, indent + "\t");
                }
                break;
            case Additive:
                ASTNode child1 = node.getChildren().get(0);
                int result1 = evaluate(child1, indent + "\t");
                ASTNode child2 = node.getChildren().get(1);
                int result2 = evaluate(child2, indent + "\t");
                if (node.getType() == ASTNodeType.Additive) {
                    result = result1 + result2;
                } else {
                    result = result1 - result2;
                }
                break;
            case Multiplicative:
                child1 = node.getChildren().get(0);
                result1 = evaluate(child1, indent + "\t");
                child2 = node.getChildren().get(1);
                result2 = evaluate(child2, indent + "\t");
                if (node.getType() == ASTNodeType.Multiplicative) {
                    result = result1 * result2;
                } else {
                    result = result1 / result2;
                }
                break;
            case IntLiteral:
                result = Integer.parseInt(node.getText());
                break;
            default:
        }
        System.out.println(indent + "Result: " + result);
        return result;
    }

    /**
     * 生成AST的入口，生成一个以ASTNodeType.Programm为根结点的AST Tree
     *
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode prog(TokenReader tokens) throws Exception {
        SimpleASTNode node = new SimpleASTNode(ASTNodeType.Programm, "Calculator");

        SimpleASTNode child = additive(tokens);

        if (child != null) {
            node.addChild(child);
        }

        return node;
    }

    /**
     * 整数赋值解析
     * int a = 1;
     * int b;
     *
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode intDeclare(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        // 预读
        Token token = tokens.peek();

        // token是int标记
        if (token != null && TokenType.Int == token.getType()) {
            // 把int拿出，游标指向下一个节点，此时 token = int
            token = tokens.read();
            if (tokens.peek().getType() != null && tokens.peek().getType() == TokenType.Identifier) {
                // 此时token是变量名
                token = tokens.read();
                // 创建一个AST节点，类型为变量声明类型，text记录了变量的名字
                node = new SimpleASTNode(ASTNodeType.IntDeclaration, token.getText());
                // 预读，看声明语句后是不是跟着赋值语句，此时token是=
                token = tokens.peek();
                if (token != null && TokenType.Assignment == token.getType()) {
                    // 消耗等号
                    tokens.read();
                    // 获取等号后面的表达式，一切表达式开始都任务是加法
                    SimpleASTNode child = additive(tokens);
                    if (child == null) {
                        throw new Exception("赋值语句后面必须要跟着数值表达式");
                    } else {
                        // 表达式ASTNode添加到声明ASTNode的孩子节点中
                        node.addChild(child);
                    }
                }
            } else {
                throw new Exception("int 后面必须要跟变量名");
            }

            // 吃调分号
            token = tokens.peek();
            if (token != null && TokenType.SemiColon == token.getType()) {
                tokens.read();
            } else {
                throw new Exception("必须要;结尾");
            }
        }

        return node;
    }

    /**
     * 语法解析：加法表达式
     * multiplicative
     * multiplicative + additive
     *
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode additive(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = multiplicative(tokens);
        // 加法可能这里结束
        SimpleASTNode node = child1;

        Token token = tokens.peek();
        if (child1 != null && token != null) {
            // 后面跟着 + 或者 -
            if (TokenType.Plus == token.getType() || TokenType.Minus == token.getType()) {
                token = tokens.read();
                SimpleASTNode child2 = additive(tokens);
                if (child2 != null) {
                    node = new SimpleASTNode(ASTNodeType.Additive, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                } else {
                    throw new Exception("+或-号右边的表达式缺失");
                }
            }
        }
        return node;
    }

    /**
     * M = {I : I * M}
     *
     * @param tokens
     * @return
     * @throws Exception
     */
    private SimpleASTNode multiplicative(TokenReader tokens) throws Exception {
        SimpleASTNode child1 = primary(tokens);
        SimpleASTNode node = child1;

        Token token = tokens.peek();
        if (child1 != null && token != null) {
            if (token.getType() == TokenType.Star || token.getType() == TokenType.Slash) {
                token = tokens.read();
                SimpleASTNode child2 = multiplicative(tokens);
                if (child2 != null) {
                    node = new SimpleASTNode(ASTNodeType.Multiplicative, token.getText());
                    node.addChild(child1);
                    node.addChild(child2);
                } else {
                    throw new Exception("*和/左边必须要跟表达式");
                }
            }
        }

        return node;
    }

    private SimpleASTNode primary(TokenReader tokens) throws Exception {
        SimpleASTNode node = null;
        Token token = tokens.peek();
        if (token != null) {
            if (TokenType.IntLiteral == token.getType()) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.IntLiteral, token.getText());
            } else if (token.getType() == TokenType.Identifier) {
                token = tokens.read();
                node = new SimpleASTNode(ASTNodeType.Identifier, token.getText());
            } else if (token.getType() == TokenType.LeftParen) {
                tokens.read();
                node = additive(tokens);
                if (node != null) {
                    token = tokens.peek();
                    if (token != null && token.getType() == TokenType.RightParen) {
                        tokens.read();
                    } else {
                        throw new Exception("expecting right parenthesis");
                    }
                } else {
                    throw new Exception("expecting an additive expression inside parenthesis");
                }
            }
        }

        return node;
    }

    private class SimpleASTNode implements ASTNode {
        SimpleASTNode parent = null;
        List<ASTNode> children = new ArrayList<>();
        List<ASTNode> readonlyChildren = Collections.unmodifiableList(children);
        ASTNodeType nodeType = null;
        String text = null;


        public SimpleASTNode(ASTNodeType nodeType, String text) {
            this.nodeType = nodeType;
            this.text = text;
        }

        @Override
        public ASTNode getParent() {
            return parent;
        }

        @Override
        public List<ASTNode> getChildren() {
            return readonlyChildren;
        }

        @Override
        public ASTNodeType getType() {
            return nodeType;
        }

        @Override
        public String getText() {
            return text;
        }

        public void addChild(SimpleASTNode child) {
            children.add(child);
            child.parent = this;
        }

    }

    /**
     * 打印输出AST的树状结构
     *
     * @param node
     * @param indent 缩进字符，由tab组成，每一级多一个tab
     */
    private void dumpAST(ASTNode node, String indent) {
        System.out.println(indent + node.getType() + " " + node.getText());
        for (ASTNode child : node.getChildren()) {
            dumpAST(child, indent + "\t");
        }
    }
}
