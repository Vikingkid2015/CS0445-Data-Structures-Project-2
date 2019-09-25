package cs445.a2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

/**
 * This class uses two stacks to evaluate an infix arithmetic expression from an
 * InputStream. It should not create a full postfix expression along the way; it
 * should convert and evaluate in a pipelined fashion, in a single pass.
 */
public class InfixExpressionEvaluator {
    // Tokenizer to break up our input into tokens
    StreamTokenizer tokenizer;

    // Stacks for operators (for converting to postfix) and operands (for
    // evaluating)
    StackInterface<Character> operatorStack;
    StackInterface<Double> operandStack;

    /**
     * Initializes the evaluator to read an infix expression from an input
     * stream.
     * @param input the input stream from which to read the expression
     */
    public InfixExpressionEvaluator(InputStream input) {
        // Initialize the tokenizer to read from the given InputStream
        tokenizer = new StreamTokenizer(new BufferedReader(
                        new InputStreamReader(input)));

        // StreamTokenizer likes to consider - and / to have special meaning.
        // Tell it that these are regular characters, so that they can be parsed
        // as operators
        tokenizer.ordinaryChar('-');
        tokenizer.ordinaryChar('/');

        // Allow the tokenizer to recognize end-of-line, which marks the end of
        // the expression
        tokenizer.eolIsSignificant(true);

        // Initialize the stacks
        operatorStack = new ArrayStack<Character>();
        operandStack = new ArrayStack<Double>();
    }

    /**
     * Parses and evaluates the expression read from the provided input stream,
     * then returns the resulting value
     * @return the value of the infix expression that was parsed
     */
    public Double evaluate() throws InvalidExpressionException {
        // Get the first token. If an IO exception occurs, replace it with a
        // runtime exception, causing an immediate crash.
        boolean prevTokOperand = false;
        char prevTokChar = ' ';
        try {
            tokenizer.nextToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Continue processing tokens until we find end-of-line
        while (tokenizer.ttype != StreamTokenizer.TT_EOL) {
            // Consider possible token types
            switch (tokenizer.ttype) {
                case StreamTokenizer.TT_NUMBER:
                    // If the token is a number, process it as a double-valued
                    // operand
                    handleOperand((double)tokenizer.nval, prevTokOperand, prevTokChar);
                    prevTokOperand = true;
                    break;
                case '+':
                case '-':
                case '*':
                case '/':
                case '^':
                    // If the token is any of the above characters, process it
                    // is an operator
                    handleOperator((char)tokenizer.ttype, prevTokOperand, prevTokChar);
                    prevTokChar = (char)tokenizer.ttype;
                    prevTokOperand = false;
                    break;
                case '(':
                case '{':
                    // If the token is open bracket, process it as such. Forms
                    // of bracket are interchangeable but must nest properly.
                    handleOpenBracket((char)tokenizer.ttype, prevTokOperand, prevTokChar);
                    prevTokChar = (char)tokenizer.ttype;
                    prevTokOperand = false;
                    break;
                case ')':
                case '}':
                    // If the token is close bracket, process it as such. Forms
                    // of bracket are interchangeable but must nest properly.
                    handleCloseBracket((char)tokenizer.ttype, prevTokOperand);
                    prevTokChar = (char)tokenizer.ttype;
                    prevTokOperand = false;
                    break;
                case StreamTokenizer.TT_WORD:
                    // If the token is a "word", throw an expression error
                    throw new InvalidExpressionException("Unrecognized symbol: " +
                                    tokenizer.sval);
                default:
                    // If the token is any other type or value, throw an
                    // expression error
                    throw new InvalidExpressionException("Unrecognized symbol: " +
                                    String.valueOf((char)tokenizer.ttype));
            }

            // Read the next token, again converting any potential IO exception
            try {
                tokenizer.nextToken();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Almost done now, but we may have to process remaining operators in
        // the operators stack
        handleRemainingOperators();

        // Return the result of the evaluation
        // after handleRemainingOperators() is called, the only thing left on the stack if the answer
        return operandStack.peek();
    }

    /**
     * This method is called when the evaluator encounters an operand. It
     * manipulates operatorStack and/or operandStack to process the operand
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param operand the operand token that was encountered
     */
    void handleOperand(double operand, boolean prevTokOperand, char prevTokChar) throws InvalidExpressionException {
        // operands just get pushed onto the stack
        if(prevTokOperand)
        {
            throw new InvalidExpressionException("Can not have multiple operands in succession.");
        }
        if(prevTokChar == '}' || prevTokChar == ')')
        {
            throw new InvalidExpressionException("Can not have operand following a closing bracket.");
        }
        operandStack.push(operand);
    }

    /**
     * This method is called when the evaluator encounters an operator. It
     * manipulates operatorStack and/or operandStack to process the operator
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param operator the operator token that was encountered
     */
    void handleOperator(char operator, boolean prevTokOperand, char prevTokChar) throws InvalidExpressionException {

        if(!prevTokOperand)
        {
            if(prevTokChar == '(' || prevTokChar == '{')
            {
                throw new InvalidExpressionException("Operator can not follow an open bracket.");
            }
            if(prevTokChar == '^' || prevTokChar == '*' || prevTokChar == '/' || prevTokChar == '+' || prevTokChar == '-')
            {
                throw new InvalidExpressionException("Can not have two operators in succession.");
            }
        }

        // check if there is a previous operator, if there is then compare the precedences and go from there
        boolean order;
        double operand1 = 0.0;
        double operand2 = 0.0;
        double operand3 = 0.0;
        char op = ' ';

        // comparing precedence is only necessary if there is a previous operator to compare to
        if(!operatorStack.isEmpty())
        {
            char prevOp = operatorStack.peek();
            // order is the boolean value of the comparison between the previous and new operators
            order = checkPrec(operator, prevOp);

            // if the previous operator has lower precedence than the new operator, just push the new operator onto the stack
            if(order == false)
            {
                operatorStack.push(operator);
            }

            // if the new operator has lower precedence than the previous operator, resolve the previous operator 
            else
            {
                // pop 2 operands off the stack and 1 operator off the stack
                operand2 = operandStack.pop();
                operand1 = operandStack.pop();
                op = operatorStack.pop();

                // use switch-case to do the appropriate operation for the operator that has been poped
                switch(op)
                {
                    case '^':
                        operand3 = Math.pow(operand1, operand2);
                        break;
                    case '*':
                        operand3 = operand1 * operand2;
                        break;
                    case '/':
                        operand3 = operand1 / operand2;
                        break;
                    case '+':
                        operand3 = operand1 + operand2;
                        break;
                    case '-':
                        operand3 = operand1 - operand2;
                        break;
                }
                // after resolving the previous operator, push the completed operand and 
                // the new operator onto the corect stacks onto the stack
                operandStack.push(operand3);
                operatorStack.push(operator);
            }
        }

        // if there is no previous operator, just push the new operator onto the stack
        else
        {
            operatorStack.push(operator);
        }

    }

    /**
     * This method is called when the evaluator encounters an open bracket. It
     * manipulates operatorStack and/or operandStack to process the open bracket
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param openBracket the open bracket token that was encountered
     */
    void handleOpenBracket(char openBracket, boolean prevTokOperand, char prevTokChar) throws InvalidExpressionException{

        if(prevTokOperand)
        {
            throw new InvalidExpressionException("An open bracket can not an operand.");
        }

        if(prevTokChar == ')' || prevTokChar == '}')
        {
            throw new InvalidExpressionException("An open bracket can not follow a closed bracket.");
        }

        // Open brackets have the lowest precedence so they will always be placed right onto the stack
        operatorStack.push(openBracket);
    }

    /**
     * This method is called when the evaluator encounters a close bracket. It
     * manipulates operatorStack and/or operandStack to process the close
     * bracket according to the Infix-to-Postfix and Postfix-evaluation
     * algorithms.
     * @param closeBracket the close bracket token that was encountered
     */
    void handleCloseBracket(char closeBracket, boolean prevTokOperand) throws InvalidExpressionException {

        if(!prevTokOperand)
        {
            throw new InvalidExpressionException("A closed bracket must be proceeded by an operand");
        }

        // When a closed bracket is found, pop 2 operands and 1 operator off the stack, do the operation,
        // then push the new operand back onto the stack until an open bracket is found
        // pop the previous operator before entering the while loop
        char operator = operatorStack.pop();
        double operand1 = 0.0;
        double operand2 = 0.0;
        double operand3 = 0.0;

        // check that the operator is not an open bracket
        while(operator != '(' && operator != '{')
        {

            // if the operator is not an open bracket, then pop 2 operands and do operation that is specified
            operand2 = operandStack.pop();
            operand1 = operandStack.pop();

            switch(operator)
            {
                case '^':
                    operand3 = Math.pow(operand1, operand2);
                    operandStack.push(operand3);
                    break;
                case '*':
                    operand3 = operand1 * operand2;
                    operandStack.push(operand3);
                    break;
                case '/':
                    operand3 = operand1 / operand2;
                    operandStack.push(operand3);
                    break;
                case '+':
                    operand3 = operand1 + operand2;
                    operandStack.push(operand3);
                    break;
                case '-':
                    operand3 = operand1 - operand2;
                    operandStack.push(operand3);
                    break;
            }

            // pop the next operator, this step is last so that it is the first thing checked at the loop start
            operator = operatorStack.pop();
        }
    }

    /**
     * This method is called when the evaluator encounters the end of an
     * expression. It manipulates operatorStack and/or operandStack to process
     * the operators that remain on the stack, according to the Infix-to-Postfix
     * and Postfix-evaluation algorithms.
     */
    void handleRemainingOperators() {
        // this method is only called when there are no more operands or operators to add,
        // this method just works its way through the stacks until the only thing left is the answer on the operand stack
        // pop 2 operands off the stack and 1 operator off the stack
        double operand2 = operandStack.pop();
        double operand1 = operandStack.pop();
        double operand3 = 0.0;

        while(!operatorStack.isEmpty())
        {
            char op = operatorStack.pop();
            // use switch-case to do the appropriate operation for the operator that has been poped
            switch(op)
            {
                case '^':
                    operand3 = Math.pow(operand1, operand2);
                    break;
                case '*':
                    operand3 = operand1 * operand2;
                    break;
                case '/':
                    operand3 = operand1 / operand2;
                    break;
                case '+':
                    operand3 = operand1 + operand2;
                    break;
                case '-':
                    operand3 = operand1 - operand2;
                    break;
            }
            // after resolving the previous operator, push the completed operand and 
            // the new operator onto the corect stacks onto the stack
            operandStack.push(operand3);
        }
    }

    // compare the precedence of the previous operator to the new operator
    boolean checkPrec(char operator, char prevOp)
    {
        int operatorPrec = 0;
        int prevPrec = 0;
        boolean precedence = false;

        // assign precedence to the new operator
        if(operator == '^')
        {
            operatorPrec = 3;
        }
        if(operator == '*' || operator == '/')
        {
            operatorPrec = 2;
        }
        if(operator == '+' || operator == '-')
        {
            operatorPrec = 1;
        }
        else
        {
            operatorPrec = -1;
        }

        // assign precedence to the previous operator
        if(prevOp == '^')
        {
            prevPrec = 3;
        }
        if(prevOp == '*' || prevOp == '/')
        {
            prevPrec = 2;
        }
        if(prevOp == '+' || prevOp == '-')
        {
            prevPrec = 1;
        }
        else
        {
            prevPrec = -1;
        }

        // if the previous operator has higher precedence than the new operator, change precedence to true
        if(operatorPrec <= prevPrec)
        {
            precedence = true;
        }
        
        //retrun precedence
        return precedence;
    }


    /**
     * Creates an InfixExpressionEvaluator object to read from System.in, then
     * evaluates its input and prints the result.
     * @param args not used
     */
    public static void main(String[] args) {
        System.out.println("Infix expression:");
        InfixExpressionEvaluator evaluator =
                        new InfixExpressionEvaluator(System.in);
        Double value = null;
        try {
            value = evaluator.evaluate();
        } catch (InvalidExpressionException e) {
            System.out.println("Invalid expression: " + e.getMessage());
        }
        if (value != null) {
            System.out.println(value);
        }
    }

}

