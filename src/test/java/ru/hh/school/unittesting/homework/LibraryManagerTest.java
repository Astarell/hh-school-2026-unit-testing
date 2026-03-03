package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  

  // addBook()
  @Test
  void testAddBook_WhenBookIdIsNotPresentAndQuantityIsCorrect_ThenPutSpecifiedQuantity() {
    // prepare
    String bookId = "test_1";
    int quantity = 3;
    libraryManager.addBook(bookId, quantity);

    // check
    assertEquals(quantity, libraryManager.getAvailableCopies(bookId));
  }


  @Disabled("Думаю, необходима валидация null, пустых строк и строк со спец. символами")
  @ParameterizedTest(name = "[bookId={0}, quantity={1}]")
  @CsvSource({
      ", 1", // null
      "'', 1", // пустая строка
      "' ', 1", // строка с пробелом
      "'\n\t', 1", // строка со спец. символами
  })
  void testAddBook_WhenBookIdIsIncorrectAndQuantityIsCorrect_ThenThrowException(
      String bookId,
      Integer quantity
  ) {
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.addBook(bookId, quantity)
    );
    assertEquals("Book's id should not be blank or null", exception.getMessage());
  }

  @Disabled("Думаю, необходима валидация на кол-во книг, которое хотим положить, если quantity <=0")
  @ParameterizedTest(name = "[bookId={0}, quantity={1}]")
  @CsvSource({
      "test_2, 0", // не должны уметь класть 0 книг
      "test_3, -1", // не должны уметь класть -1 книгу
  })
  void testAddBook_WhenBookIdIsCorrectAndQuantityIsIncorrect_ThenThrowException(
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
  void testAddBook_WhenBookIdIsPresentAndQuantityIsCorrect_ThenPutInitialQuantityPlusSpecifiedQuantity() {
    // prepare
    String bookId = "test_1";
    int initialQuantity = 1;
    int quantity = 2;
    libraryManager.addBook(bookId, initialQuantity);
    libraryManager.addBook(bookId, quantity);

    // check
    assertEquals(initialQuantity + quantity, libraryManager.getAvailableCopies(bookId));
  }


  @Disabled("Если положим Integer.MAX_VALUE к имеющемуся кол-ву > 1, то будет переполнение и данные станут некорректными")
  @Test
  void testAddBook_WhenBookIdIsPresentAndQuantityWillOverflowInteger_ThenThrowException() {
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
  void testBorrowBook_WhenUserIsInactive_ThenNotifyUserThatAccountIsInactiveAndReturnFalse() {
    // prepare
    when(userService.isUserActive("default_user")).thenReturn(false);
    libraryManager.addBook("default_book_id", 1);

    // check
    assertFalse(libraryManager.borrowBook("default_book_id", "default_user"));

    verify(notificationService, times(1))
        .notifyUser("default_user", "Your account is not active.");
  }


  @ParameterizedTest(name = "[quantity={0}]")
  @CsvSource({"-1", "0"})
  void testBorrowBook_WhenUserIsActiveAndBookPresentAndBookQuantityEqualOrLessToZero_ThenReturnFalse(
      int quantity
  ) {
    // prepare
    when(userService.isUserActive("default_user")).thenReturn(true);
    libraryManager.addBook("default_book_id", quantity);

    // check
    assertFalse(libraryManager.borrowBook("default_book_id", "default_user"));
  }


  @Test
  void testBorrowBook_WhenUserIsActiveAndBookIsNotPresent_ThenReturnFalse() {
    // prepare
    when(userService.isUserActive("default_user")).thenReturn(true);

    //check
    assertFalse(libraryManager.borrowBook("default_book_id", "default_user"));
  }


  @Test
  void testBorrowBook_WhenUserIsActiveAndBookPresentAndBookQuantityMoreThanZero_ThenReturnTrue() {
    // prepare
    when(userService.isUserActive("default_user")).thenReturn(true);
    libraryManager.addBook("default_book_id", 4);

    // check
    assertTrue(libraryManager.borrowBook("default_book_id", "default_user"));
    assertEquals(3, libraryManager.getAvailableCopies("default_book_id"));

    verify(notificationService, times(1))
        .notifyUser("default_user", "You have borrowed the book: default_book_id");
  }


  // returnBook()
  @Test
  void testReturnBook_WhenBookIsNotBorrowed_ThenReturnFalse(){
    assertFalse(libraryManager.returnBook("default_book_id", "default_user"));
  }


  @Test
  void testReturnBook_WhenBookIsBorrowedAndUserIdIsIncorrect_ThenReturnFalse(){
    // prepare
    when(userService.isUserActive("default_user")).thenReturn(true);

    libraryManager.addBook("default_book_id", 1);
    libraryManager.borrowBook("default_book_id", "default_user");

    // check
    assertFalse(libraryManager.returnBook("default_book_id", "incorrect_user_id"));
  }


  @Test
  void testReturnBook_WhenBookIsBorrowedAndUserIdIsCorrectAndBookInventoryContainsSpecifiedBook_ThenReturnTrue(){
    // prepare
    when(userService.isUserActive("default_user")).thenReturn(true);

    libraryManager.addBook("default_book_id", 10);
    libraryManager.borrowBook("default_book_id", "default_user");

    // check
    assertTrue(libraryManager.returnBook("default_book_id", "default_user"));
    assertEquals(10, libraryManager.getAvailableCopies("default_book_id"));

    verify(notificationService, times(1))
        .notifyUser("default_user", "You have returned the book: " + "default_book_id");
  }


  // getAvailableCopies()
  @Test
  void testGetAvailableCopies_WhenBookIdIsPresent_ThenReturnActualNumberOfCopies(){
    // prepare
    libraryManager.addBook("default_book_id", 1);

    // check
    assertEquals(1, libraryManager.getAvailableCopies("default_book_id"));
  }


  @Test
  void testGetAvailableCopies_WhenBookIdIsNotPresent_ThenReturnZero(){
    assertEquals(0, libraryManager.getAvailableCopies("default_book_id"));
  }


  // calculateDynamicLateFee()
  @Test
  void testCalculateDynamicLateFee_WhenOverdueDaysLessThanZero_ThenThrowException(){
    var exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-1, false, false)
    );
    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }


  @Test
  void testCalculateDynamicLateFee_WhenOverdueDaysIsZero_ThenReturnZero(){
    assertEquals(0, libraryManager.calculateDynamicLateFee(0, false, false));
  }


  @ParameterizedTest(name = "[isBestSeller={0}, isPremiumMember={1}, overdueDays=72, result={2}]")
  @CsvSource({
      "true, false, 54",
      "false, true, 28.8",
      "true, true, 43.2",
      "false, false, 36"
  })
  void testCalculateDynamicLateFee_WhenOverdueDaysMoreThanZeroAndBestsellerIsTrueAndPremiumMemberIsFalse_ThenReturnResult(
      boolean isBestseller,
      boolean isPremiumMember,
      double result
  ){
    assertEquals(result, libraryManager.calculateDynamicLateFee(72, isBestseller, isPremiumMember));
  }
}
