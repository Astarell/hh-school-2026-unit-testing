package ru.hh.school.unittesting.homework;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;
  @InjectMocks
  private LibraryManager libraryManager;

  private final String defaultUserId = "default_user";
  private final String defaultBookId = "default_bookId";
  private final String bookInventoryFieldName = "bookInventory";
  private final String borrowedBooksFieldName = "borrowedBooks";

  @BeforeEach
  public void initEach() throws NoSuchFieldException, IllegalAccessException {
    // Очищаю bookInventory перед каждым тестом
    Field bookInventory = libraryManager.getClass().getDeclaredField(bookInventoryFieldName);
    bookInventory.setAccessible(true);
    bookInventory.set(libraryManager, new HashMap<>());
    bookInventory.setAccessible(false);

    // Очищаю borrowedBooks перед каждым тестом
    Field borrowedBooks = libraryManager.getClass().getDeclaredField(borrowedBooksFieldName);
    borrowedBooks.setAccessible(true);
    borrowedBooks.set(libraryManager, new HashMap<>());
    borrowedBooks.setAccessible(false);
  }

  // addBook()
  @ParameterizedTest(name = "[bookId={0}, quantity={1}]")
  @CsvSource({
      "test_1, 2",
      "кириллица_2, 3",
  })
  public void testAddBook_WhenBookIdIsNotPresentAndQuantityIsCorrect_ThenPutSpecifiedQuantity(
      String bookId,
      Integer quantity
  ) {
    // prepare
    libraryManager.addBook(bookId, quantity);

    // check
    assertThat(libraryManager)
        .withFailMessage("Некорректное состояние bookInventory после операции")
        .extracting(bookInventoryFieldName, as(InstanceOfAssertFactories.map(String.class, Integer.class)))
        .containsOnly(entry(bookId, quantity));
  }


  // Ошибка выбрасываться не будет, но думаю, что нужно
  @ParameterizedTest(name = "[bookId={0}, quantity={1}]")
  @CsvSource({
      ", 1", // null
      "'', 1", // пустая строка
      "' ', 1", // строка с пробелом
      "'\n\t', 1", // строка со спец. символами
  })
  public void testAddBook_WhenBookIdIsIncorrectAndQuantityIsCorrect_ThenThrowException(
      String bookId,
      Integer quantity
  ) {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.addBook(bookId, quantity)
    );
    assertEquals("Book's id should not be blank or null", exception.getMessage());
  }

  // Ошибка выбрасываться не будет, но думаю, что нужно
  @ParameterizedTest(name = "[bookId={0}, quantity={1}]")
  @CsvSource({
      "test_2, 0", // не должны уметь класть 0 книг
      "test_3, -1", // не должны уметь класть -1 книгу
  })
  public void testAddBook_WhenBookIdIsCorrectAndQuantityIsIncorrect_ThenThrowException(
      String bookId,
      Integer quantity
  ) {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.addBook(bookId, quantity)
    );
    assertEquals("Book's quantity should be greater or equal to 1", exception.getMessage());
  }


  @Test
  public void testAddBook_WhenBookIdIsPresentAndQuantityIsCorrect_ThenPutInitialQuantityPlusSpecifiedQuantity() {
    // prepare
    String bookId = "test_1";
    int initialQuantity = 1;
    int quantity = 2;
    libraryManager.addBook(bookId, initialQuantity);
    libraryManager.addBook(bookId, quantity);

    // check
    assertThat(libraryManager)
        .withFailMessage("Некорректное состояние bookInventory после операции")
        .extracting(bookInventoryFieldName, as(InstanceOfAssertFactories.map(String.class, Integer.class)))
        .hasSize(1)
        .matches(item -> item.containsKey(bookId)
            && item.get(bookId) == initialQuantity + quantity);
  }


  // Если положим Integer.MAX_VALUE к имеющемуся кол-ву > 1, то будет переполнение и данные станут некорректными
  // Нужна проверка на беке на переполнение
  @Test
  public void testAddBook_WhenBookIdIsPresentAndQuantityWillOverflowInteger_ThenThrowException() {
    // prepare
    libraryManager.addBook("test_1", 1);

    // check
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.addBook("test_1", Integer.MAX_VALUE)
    );
    assertEquals("Overflow exception", exception.getMessage());
  }


  // borrowBook()
  @Test
  public void testBorrowBook_WhenUserIsInactive_ThenNotifyUserThatAccountIsInactiveAndReturnFalse() {
    // prepare
    when(userService.isUserActive(defaultUserId)).thenReturn(false);
    libraryManager.addBook(defaultBookId, 1);

    // check
    assertFalse(libraryManager.borrowBook(defaultBookId, defaultUserId));

    verify(notificationService, times(1))
        .notifyUser(defaultUserId, "Your account is not active.");
  }


  @ParameterizedTest(name = "[quantity={0}]")
  @CsvSource({"-1", "0"})
  public void testBorrowBook_WhenUserIsActiveAndBookPresentAndBookQuantityEqualOrLessToZero_ThenReturnFalse(
      int quantity
  ) {
    // prepare
    when(userService.isUserActive(defaultUserId)).thenReturn(true);
    libraryManager.addBook(defaultBookId, quantity);

    // check
    assertFalse(libraryManager.borrowBook(defaultBookId, defaultUserId));
  }


  @Test
  public void testBorrowBook_WhenUserIsActiveAndBookIsNotPresent_ThenReturnFalse() {
    // prepare
    when(userService.isUserActive(defaultUserId)).thenReturn(true);

    //check
    assertFalse(libraryManager.borrowBook(defaultBookId, defaultUserId));
  }


  @ParameterizedTest(name = "[quantity={0}]")
  @CsvSource({
      "1", "4", "2147483647"
  })
  public void testBorrowBook_WhenUserIsActiveAndBookPresentAndBookQuantityMoreThanZero_ThenReturnTrue(int quantity) {
    // prepare
    when(userService.isUserActive(defaultUserId)).thenReturn(true);
    libraryManager.addBook(defaultBookId, quantity);

    // check
    assertTrue(libraryManager.borrowBook(defaultBookId, defaultUserId));

    assertThat(libraryManager)
        .withFailMessage("Некорректное состояние bookInventory после операции")
        .extracting(bookInventoryFieldName, as(InstanceOfAssertFactories.map(String.class, Integer.class)))
        .containsOnly(entry(defaultBookId, quantity - 1));

    assertThat(libraryManager)
        .withFailMessage("Некорректное состояние borrowedBooks после операции")
        .extracting(borrowedBooksFieldName, as(InstanceOfAssertFactories.map(String.class, String.class)))
        .containsOnly(entry(defaultBookId, defaultUserId));

    verify(notificationService, times(1))
        .notifyUser(defaultUserId, "You have borrowed the book: " + defaultBookId);
  }


  // returnBook()
  @Test
  public void testReturnBook_WhenBookIsNotBorrowed_ThenReturnFalse(){
    assertFalse(libraryManager.returnBook(defaultBookId, defaultUserId));
  }


  @Test
  public void testReturnBook_WhenBookIsBorrowedAndUserIdIsIncorrect_ThenReturnFalse(){
    // prepare
    when(userService.isUserActive(defaultUserId)).thenReturn(true);

    libraryManager.addBook(defaultBookId, 1);
    libraryManager.borrowBook(defaultBookId, defaultUserId);

    // check
    assertFalse(libraryManager.returnBook(defaultBookId, "incorrect_user_id"));
  }


  @ParameterizedTest(name = "[quantity={0}]")
  @CsvSource({
      "1", "10", "2147483647"
  })
  public void testReturnBook_WhenBookIsBorrowedAndUserIdIsCorrectAndBookInventoryContainsSpecifiedBook_ThenReturnTrue(
      int quantity
  ){
    // prepare
    when(userService.isUserActive(defaultUserId)).thenReturn(true);

    libraryManager.addBook(defaultBookId, quantity);
    libraryManager.borrowBook(defaultBookId, defaultUserId);

    // check
    assertTrue(libraryManager.returnBook(defaultBookId, defaultUserId));

    assertThat(libraryManager)
        .withFailMessage("Некорректное состояние bookInventory после операции")
        .extracting(bookInventoryFieldName, as(InstanceOfAssertFactories.map(String.class, Integer.class)))
        .containsOnly(entry(defaultBookId, quantity));

    assertThat(libraryManager)
        .withFailMessage("Некорректное состояние borrowedBooks после операции")
        .extracting(borrowedBooksFieldName, as(InstanceOfAssertFactories.map(String.class, String.class)))
        .isEmpty();

    verify(notificationService, times(1))
        .notifyUser(defaultUserId, "You have returned the book: " + defaultBookId);
  }


  // getAvailableCopies()
  @Test
  public void testGetAvailableCopies_WhenBookIdIsPresent_ThenReturnActualNumberOfCopies(){
    // prepare
    libraryManager.addBook(defaultBookId, 1);

    // check
    assertEquals(1, libraryManager.getAvailableCopies(defaultBookId));
  }


  @Test
  public void testGetAvailableCopies_WhenBookIdIsNotPresent_ThenReturnZero(){
    assertEquals(0, libraryManager.getAvailableCopies(defaultBookId));
  }


  // Ошибка выбрасываться не будет, но думаю, что нужно
  @ParameterizedTest(name = "[bookId={0}]")
  @CsvSource({
      "''", // пустая строка
      "' '", // строка с пробелом
      "'\n\t'", // строка со спец. символами
  })
  public void testGetAvailableCopies_WhenBookIdIsIncorrect_ThenThrowException(String bookId){
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.getAvailableCopies(bookId)
    );
    assertEquals("Book's id should not be blank or null", exception.getMessage());
  }


  // calculateDynamicLateFee()
  @Test
  public void testCalculateDynamicLateFee_WhenOverdueDaysLessThanZero_ThenThrowException(){
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }


  @Test
  public void testCalculateDynamicLateFee_WhenOverdueDaysIsZero_ThenReturnZero(){
    assertEquals(0, libraryManager.calculateDynamicLateFee(0, false, false));
  }


  @ParameterizedTest(name = "[overdueDays={0}, result={1}]")
  @CsvSource({
      "3, 2.25", "72, 54", "354, 265.5"
  })
  public void testCalculateDynamicLateFee_WhenOverdueDaysMoreThanZeroAndBestsellerIsTrueAndPremiumMemberIsFalse_ThenReturnResult(
      int overdueDays,
      double result
  ){
    assertEquals(result, libraryManager.calculateDynamicLateFee(overdueDays, true, false));
  }


  @ParameterizedTest(name = "[overdueDays={0}, result={1}]")
  @CsvSource({
      "3, 1.2", "72, 28.8", "354, 141.6"
  })
  public void testCalculateDynamicLateFee_WhenOverdueDaysMoreThanZeroAndBestsellerIsFalseAndPremiumMemberIsTrue_ThenReturnResult(
      int overdueDays,
      double result
  ){
    assertEquals(result, libraryManager.calculateDynamicLateFee(overdueDays, false, true));
  }


  @ParameterizedTest(name = "[overdueDays={0}, result={1}]")
  @CsvSource({
      "3, 1.8", "72, 43.2", "354, 212.4"
  })
  public void testCalculateDynamicLateFee_WhenOverdueDaysMoreThanZeroAndBestsellerIsTrueAndPremiumMemberIsTrue_ThenReturnResult(
      int overdueDays,
      double result
  ){
    assertEquals(result, libraryManager.calculateDynamicLateFee(overdueDays, true, true));
  }


  @ParameterizedTest(name = "[overdueDays={0}, result={1}]")
  @CsvSource({
      "3, 1.5", "72, 36", "354, 177"
  })
  public void testCalculateDynamicLateFee_WhenOverdueDaysMoreThanZeroAndBestsellerIsFalseAndPremiumMemberIsFalse_ThenReturnResult(
      int overdueDays,
      double result
  ){
    assertEquals(result, libraryManager.calculateDynamicLateFee(overdueDays, false, false));
  }
}
