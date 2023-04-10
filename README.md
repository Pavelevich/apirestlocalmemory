# Booking Services API

Welcome to our Booking Services API project! Here you will find all the details on how to use our services to manage reservation transactions.

## Endpoints

We have implemented the following endpoints:

- `PUT /bookingservice/transaction/{transaction_id}`: Adds a new transaction to the transaction map with the given ID. If the transaction already exists, or any of its required fields are missing, or its parent transaction does not exist or has a different currency, a corresponding error message is returned. If the transaction has a parent ID, the new transaction is added as a child to the parent transaction.
- `GET /bookingservice/transaction/{transaction_id}`: Retrieves the transaction with the given ID from the transaction map, if it exists.
- `GET /bookingservice/types/{type}`: Retrieves a list of transaction IDs for all transactions of the given type.
- `GET /bookingservice/currencies`: Retrieves a list of all distinct currencies for transactions in the map.
- `GET /bookingservice/sum/{transaction_id}`: Retrieves the sum of all transactions with the given ID and its child transactions, in the same currency as the transaction with the given ID. If the transaction with the given ID does not exist, an error message is returned.

## Usage

To start using our services, simply refer to each endpoint's documentation to see how to send requests and receive responses.

We have also included a collection of Postman requests with the endpoints already set up for you to test. You can find the collection in the `postman` folder.

There is a set of auxiliary enpoints that complete the request by turning the API into a CRUD.

## Technologies Used

Our API was built using Java and IntelliJ IDEA. We've also used the Spring Boot framework to implement the API endpoints.

## Contributors

- Pavel Hernandez Chmirenko (chmirenko2@gmail.com)

Thank you for choosing our reservation service :)

