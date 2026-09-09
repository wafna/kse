# KSE

Kotlin SQL Engine

The goal of this project is to provide a lightweight, performant, and easy-to-use JDBC wrapper written in Kotlin.
It is database agnostic and promotes reuse of SQL queries.

Database agnosticism is achieved by only consuming raw SQL and maintaining no direct knowledge of data types.
Input data are encoded as functions that know how to set their data into prepared statements once given the handle and position.
Output data are read from result sets using JDBC functions directly.

## Params

Parameters are provided to prepared statements as setter functions that operate on the prepared statement.
Extension methods are provided for common types and can be easily added for custom types.

