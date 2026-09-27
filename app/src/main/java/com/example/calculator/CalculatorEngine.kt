package com.example.calculator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack

data class CalculationResult(
    val resultString: String,
    val numericValue: Double?,
    val isError: Boolean = false
)

object CalculatorEngine {

    private val numberFormatter = DecimalFormat("#,###.########", DecimalFormatSymbols(Locale.US))

    fun evaluate(expression: String): CalculationResult {
        if (expression.isBlank()) return CalculationResult("0", 0.0)

        return try {
            val sanitized = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .trim()

            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) return CalculationResult("0", 0.0)

            val postfix = infixToPostfix(tokens)
            val result = evaluatePostfix(postfix)

            if (result.isInfinite() || result.isNaN()) {
                CalculationResult("Error", null, true)
            } else {
                val formatted = if (result % 1.0 == 0.0 && Math.abs(result) < 1e12) {
                    numberFormatter.format(result.toLong())
                } else {
                    numberFormatter.format(result)
                }
                CalculationResult(formatted, result)
            }
        } catch (_: Exception) {
            CalculationResult("Error", null, true)
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c in "+*/%" -> {
                    tokens.add(c.toString())
                    i++
                }
                c == '-' -> {
                    // Check if unary minus
                    val isUnary = tokens.isEmpty() || tokens.last() in "+-*/%("
                    if (isUnary) {
                        // Read negative number
                        var numStr = "-"
                        i++
                        while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                            numStr += expr[i]
                            i++
                        }
                        if (numStr == "-") {
                            tokens.add("-1")
                            tokens.add("*")
                        } else {
                            tokens.add(numStr)
                        }
                    } else {
                        tokens.add("-")
                        i++
                    }
                }
                c == '(' || c == ')' -> {
                    tokens.add(c.toString())
                    i++
                }
                c.isDigit() || c == '.' -> {
                    var numStr = ""
                    while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                        numStr += expr[i]
                        i++
                    }
                    tokens.add(numStr)
                }
                else -> i++
            }
        }
        return tokens
    }

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/", "%" -> 2
        else -> -1
    }

    private fun infixToPostfix(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val ops = Stack<String>()

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                token == "(" -> ops.push(token)
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != "(") {
                        output.add(ops.pop())
                    }
                    if (ops.isNotEmpty() && ops.peek() == "(") {
                        ops.pop()
                    }
                }
                precedence(token) > 0 -> {
                    while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(token)) {
                        output.add(ops.pop())
                    }
                    ops.push(token)
                }
            }
        }

        while (ops.isNotEmpty()) {
            output.add(ops.pop())
        }

        return output
    }

    private fun evaluatePostfix(postfix: List<String>): Double {
        val stack = Stack<Double>()

        for (token in postfix) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else {
                if (stack.size < 2) continue
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b == 0.0) Double.NaN else a / b
                    "%" -> (a * b) / 100.0
                    else -> 0.0
                }
                stack.push(res)
            }
        }

        return if (stack.isNotEmpty()) stack.pop() else 0.0
    }
}
