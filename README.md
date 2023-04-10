# Booking Services API

Welcome to our Booking Services API project! Here you will find all the details on how to use our services to manage reservation transactions.

## Endpoints

We have implemented the following endpoints:

- `PUT /bookingservice/transaction`: Adds a new transaction to the transaction map. If any of the required fields are missing, or its parent transaction does not exist or has a different currency, a corresponding error message is returned. If the transaction has a parent ID, the new transaction is added as a child to the parent transaction.
- `GET /bookingservice/transaction/{transaction_id}`: Retrieves the transaction with the given ID from the transaction map, if it exists.
- `GET /bookingservice/types/{type}`: Retrieves a list of transaction IDs for all transactions of the given type.
- `GET /bookingservice/currencies`: Retrieves a list of all distinct currencies for transactions in the map.
- `GET /bookingservice/sum/{transaction_id}`: Retrieves the sum of all transactions with the given ID and its child transactions, in the same currency as the transaction with the given ID. If the transaction with the given ID does not exist, an error message is returned.

## Usage

To start using our services, simply refer to each endpoint's documentation to see how to send requests and receive responses.

We have also included a collection of Postman requests with the endpoints already set up for you to test. You can find the collection in the `postman` folder.

There is a set of auxiliary enpoints that complete the request by turning the API into a CRUD.

In the specific case of the TransactionController, it is an API that uses local memory to store transaction data. This means that the data is temporarily stored in the computer's memory while the application is running, rather than permanently stored in a database or other persistent storage system.

The advantage of using local memory is that it can provide fast data access and high performance, since data is stored in RAM, which is much faster than long-term data storage devices such as disks. hard. In addition, the use of local memory can simplify application design and implementation, which can result in greater development efficiencies and reduced data storage costs.

However, there are some limitations on using a local memory. For example, if the application is stopped or restarted, data stored in local memory will be lost, unless persistence measures have been implemented to save it. Also, the amount of data that can be stored in local memory is limited by the amount of memory available on the computer. Therefore, the use of local memory may be suitable for applications that handle limited amounts of data or do not require persistent storage

## Technologies Used

Our API was built using Java and IntelliJ IDEA. We've also used the Spring Boot framework to implement the API endpoints.


## Contributors

- Pavel Hernandez Chmirenko (chmirenko2@gmail.com)

Thank you for choosing our reservation service :)

