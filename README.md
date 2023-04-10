# apirestlocalmemory
Description of TransactionController
The TransactionController class is a Spring Boot REST controller that provides REST API endpoints for managing transactions. The class has several methods, including addTransaction, getTransaction, getTransactionsByType, getCurrencies, getSumOfTransactions, countTransactions, and deleteTransaction.

The TransactionController class has a transactionMap instance variable, which is a ConcurrentHashMap that stores the transactions. The class also has a LOGGER instance variable, which is used for logging.

The addTransaction method is a PUT endpoint that adds a new transaction to the transactionMap. If the transaction has a parentId, it adds the transaction as a child to the parent transaction. If the transaction is missing any required fields, the method returns a BAD_REQUEST response with an error message.

The getTransaction method is a GET endpoint that retrieves a transaction from the transactionMap by its transactionId. If the transaction is not found, the method returns a NOT_FOUND response.

The getTransactionsByType method is a GET endpoint that retrieves all transactions of a specific type from the transactionMap. The method returns a list of transaction IDs.

The getCurrencies method is a GET endpoint that retrieves all the unique currencies of the transactions from the transactionMap.

The getSumOfTransactions method is a GET endpoint that retrieves the sum of all transactions under a specified transactionId. The method returns a Map with the sum and currency.

The countTransactions method is a GET endpoint that retrieves the number of transactions in the transactionMap.

The deleteTransaction method is a DELETE endpoint that deletes a transaction from the transactionMap by its transactionId. If the transaction is not found, the method returns a NOT_FOUND response. If the transaction has children, it deletes all the children before deleting the transaction.
