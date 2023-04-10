package com.example.trans.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trans.dto.Transaction;
import com.example.trans.dto.TransactionUpdate;

@RestController
@RequestMapping("/bookingservice")
public class TransactionController {

   private Map<Integer, Transaction> transactionMap = new ConcurrentHashMap<>();

   private static final Logger LOGGER = LoggerFactory.getLogger(TransactionController.class);

   private static final String MISSING_FIELDS_ERROR_MESSAGE_TEMPLATE = "One or more required fields are missing: %s";

   private static final String PARENT_TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE = "Parent transaction does not exist with id: %d";

   private static final String CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE = "Currency mismatch with parent transaction with id: %d, must be in: %s";

   private static final String TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE = "Transaction not found with id: %d";

   private static final String TRANSACTION_UPDATED_SUCCESSFULLY_MESSAGE = "Transaction parent updated successfully";

   private static final String CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE2 = "Currency mismatch with child transaction with id: %d, must be in: %s";

   private static final String TRANSACTION_DELETED_SUCCESSFULLY_MESSAGE = "Transaction deleted successfully";

   @PutMapping("/transaction")
   public ResponseEntity<?> addTransaction(@RequestBody Transaction transaction) {

      if (transaction.getAmount() == null || transaction.getCurrency() == null || transaction.getType() == null) {
         String missingFields = String.join(", ", getMissingFields(transaction));
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(String.format(MISSING_FIELDS_ERROR_MESSAGE_TEMPLATE, missingFields));
      }
      Transaction toSave = Transaction.builder().build();
      int transactionId = toSave.getTransactionId();
      Integer parentId = transaction.getParentId();
      if (parentId != null) {
         Optional<Transaction> parentTransaction = Optional.ofNullable(transactionMap.get(parentId));
         if (parentTransaction.isEmpty()) {
            return ResponseEntity
                  .status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(String.format(PARENT_TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE, parentId));
         } else if (!parentTransaction.get().getCurrency().equals(transaction.getCurrency())) {
            return ResponseEntity
                  .status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(String.format(CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE, parentId, parentTransaction.get().getCurrency()));
         }

         parentTransaction.get().addChild(transactionId);
         transactionMap.put(parentId, parentTransaction.get());
      }

      transaction.setTransactionId(transactionId);
      transactionMap.put(transactionId, transaction);
      LOGGER.info("Memory used after adding transaction: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " KB");
      return ResponseEntity.status(HttpStatus.OK).body(transactionMap.get(transactionId));
   }

   @GetMapping("/transaction/{transaction_id}")
   public ResponseEntity<Transaction> getTransaction(@PathVariable("transaction_id") int transactionId) {
      if (!transactionMap.containsKey(transactionId)) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }
      LOGGER.info("Memory used getting transaction: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " KB");
      return ResponseEntity.status(HttpStatus.OK).body(transactionMap.get(transactionId));
   }


   @GetMapping("/types/{type}")

   public ResponseEntity<List<Integer>> getTransactionsByType(@PathVariable("type") String type) {
      List<Integer> transactionIds = transactionMap
            .entrySet()
            .stream()
            .filter(entry -> type.equals(entry.getValue().getType()))
            .map(Map.Entry::getKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      if (transactionIds.isEmpty()) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
      }
      LOGGER.info("Memory used getting types: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " KB");
      return ResponseEntity.status(HttpStatus.OK).body(transactionIds);
   }

   @GetMapping("/currencies")
   public ResponseEntity<List<String>> getCurrencies() {
      List<String> currencies = transactionMap
            .values()
            .parallelStream()
            .map(Transaction::getCurrency)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
      return ResponseEntity.status(HttpStatus.OK).body(currencies);
   }
   @GetMapping("/sum/{transaction_id}")
   public ResponseEntity<Map<String, Object>> getSumOfTransactions(@PathVariable("transaction_id") int transactionId) {
      if (!transactionMap.containsKey(transactionId)) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      }
      String currency = transactionMap.get(transactionId).getCurrency();
      Double sum = calculateSum(transactionId, currency);
      Map<String, Object> response = new HashMap<>();
      response.put("sum", sum);
      response.put("currency", currency);

      LOGGER.info("Memory used after sum transactions: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " KB");
      return ResponseEntity.status(HttpStatus.OK).body(response);
   }

   @GetMapping("/transaction/count")
   public ResponseEntity<Integer> countTransactions() {
      return ResponseEntity.ok(transactionMap.size());
   }

   @DeleteMapping("/transaction/{transaction_id}")
   public ResponseEntity<?> deleteTransaction(@PathVariable("transaction_id") int transactionId) {
      if (!transactionMap.containsKey(transactionId)) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE, transactionId));
      }

      Transaction transaction = transactionMap.get(transactionId);
      deleteAllChildren(transaction);

      if (transaction.hasParent()) {
         Transaction parentTransaction = transactionMap.get(transaction.getParentId());
         parentTransaction.getChildrenIds().remove(Integer.valueOf(transactionId));
         transactionMap.put(transaction.getParentId(), parentTransaction);
      }

      transactionMap.remove(transactionId);
      return ResponseEntity.status(HttpStatus.OK).body(TRANSACTION_DELETED_SUCCESSFULLY_MESSAGE);
   }

   @PutMapping("/transaction/{transaction_id}/parent/{parent_id}")
   public ResponseEntity<?> updateTransactionParent(@PathVariable("transaction_id") int transactionId, @PathVariable("parent_id") int parentId) {
      Transaction transaction = transactionMap.get(transactionId);
      if (transaction == null) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE, transactionId));
      }
      Transaction parentTransaction = transactionMap.get(parentId);
      if (parentTransaction == null) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(MISSING_FIELDS_ERROR_MESSAGE_TEMPLATE, parentId));
      }

      if (!parentTransaction.getCurrency().equals(transaction.getCurrency())) {
         return ResponseEntity
               .status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(String.format(CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE, parentId, parentTransaction.getCurrency()));
      }

      if (transaction.hasParent()) {
         Transaction oldParentTransaction = transactionMap.get(transaction.getParentId());
         ConcurrentHashMap<Integer, Transaction> newMap = new ConcurrentHashMap<>(transactionMap);
         oldParentTransaction.getChildrenIds().remove(Integer.valueOf(transactionId));
         newMap.put(oldParentTransaction.getTransactionId(), oldParentTransaction);
         transactionMap = newMap;
      }

      Transaction newTransaction = new Transaction(transaction);
      newTransaction.setParentId(parentId);
      parentTransaction.getChildrenIds().add(transactionId);
      ConcurrentHashMap<Integer, Transaction> newMap = new ConcurrentHashMap<>(transactionMap);
      newMap.put(transactionId, newTransaction);
      newMap.put(parentId, parentTransaction);
      transactionMap = newMap;

      return ResponseEntity.status(HttpStatus.OK).body(TRANSACTION_UPDATED_SUCCESSFULLY_MESSAGE);
   }

   @PutMapping("/transaction/update/{transaction_id}")
   public ResponseEntity<?> updateTransaction(@PathVariable("transaction_id") int transactionId, @RequestBody TransactionUpdate requestDto) {
      Double amount = requestDto.getAmount();
      String currency = requestDto.getCurrency();
      String type = requestDto.getType();
      Integer parentId = requestDto.getParentId();
      boolean isNewParent = requestDto.getIsNewParent();

      Transaction transaction = transactionMap.get(transactionId);
      if (transaction == null) {
         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE, transactionId));
      }

      if (amount != null) {
         transaction.setAmount(amount);
      }
      if (currency != null) {
         if (transaction.isParent()) {
            int childId = transaction.getChildrenIds().get(0);
            Transaction childTransaction = transactionMap.get(childId);
            if (!childTransaction.getCurrency().equals(currency)) {
               String errorMessage = String.format(CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE2, childId, childTransaction.getCurrency());
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
            }
         }
         if (transaction.hasParent()) {
            int transactionParentId = transaction.getParentId();
            Transaction parentTransaction = transactionMap.get(transactionParentId);
            if (!parentTransaction.getCurrency().equals(currency)) {
               String errorMessage = String.format(CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE, parentId, parentTransaction.getCurrency());
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
            }
         }
         transaction.setCurrency(currency);
      }

      if (type != null) {
         transaction.setType(type);
      }

      if (parentId != null) {
         Optional<Transaction> parentTransaction = Optional.ofNullable(transactionMap.get(parentId));
         if (parentTransaction.isEmpty()) {
            String errorMessage = String.format(PARENT_TRANSACTION_NOT_FOUND_ERROR_MESSAGE_TEMPLATE, parentId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);

         } else if (!parentTransaction.get().getCurrency().equals(transaction.getCurrency())) {
            String errorMessage = String.format(CURRENCY_MISMATCH_ERROR_MESSAGE_TEMPLATE, parentId, parentTransaction.get().getCurrency());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
         }
         parentTransaction.get().addChild(transactionId);
         transactionMap.put(parentId, parentTransaction.get());
         transaction.setParentId(parentId);

      } else if (transaction.hasParent() && isNewParent) {

         Transaction transactionParent = transactionMap.get(transaction.getParentId());
         transactionParent.getChildrenIds().remove(Integer.valueOf(transactionId));
         transactionMap.put(transactionParent.getTransactionId(), transactionParent);
         transaction.setParentId(null);
      }

      transactionMap.put(transactionId, transaction);
      LOGGER.info("Memory used after update transaction: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 + " KB");
      return ResponseEntity.status(HttpStatus.OK).body(transaction);
   }

   private void deleteAllChildren(Transaction transaction) {
      Iterator<Integer> childIdIterator = transaction.getChildrenIds().iterator();
      while (childIdIterator.hasNext()) {
         int childId = childIdIterator.next();
         Transaction childTransaction = transactionMap.get(childId);
         if (childTransaction.getChildrenIds().size() > 0) {
            deleteAllChildren(childTransaction);
         }
         transactionMap.remove(childId);
         childIdIterator.remove();
      }
   }

   private Double calculateSum(int transactionId, String currency) {
      List<Integer> childIds = getChildIds(transactionId);
      double sum = transactionMap.entrySet().stream().filter(entry -> childIds.contains(entry.getKey())).mapToDouble(entry -> {
         Transaction transaction = entry.getValue();
         if (currency.equals(transaction.getCurrency())) {
            return transaction.getAmount();
         } else {
            throw new RuntimeException("Currency mismatch for transaction id: " + entry.getKey());
         }
      }).sum();
      return sum + transactionMap.get(transactionId).getAmount();
   }

   private List<Integer> getChildIds(int parentId) {
      List<Integer> childIds = new ArrayList<>();
      for (Map.Entry<Integer, Transaction> entry : transactionMap.entrySet()) {
         Integer transactionId = entry.getKey();
         Transaction transaction = entry.getValue();
         if (transaction.getParentId() != null && parentId == transaction.getParentId()) {
            childIds.add(transactionId);
         }
      }
      return childIds;
   }

   private List<String> getMissingFields(Transaction transaction) {
      List<String> missingFields = new ArrayList<>();
      if (transaction.getAmount() == null) {
         missingFields.add("amount");
      }
      if (transaction.getCurrency() == null) {
         missingFields.add("currency");
      }
      if (transaction.getType() == null) {
         missingFields.add("type");
      }
      return missingFields;
   }
}
