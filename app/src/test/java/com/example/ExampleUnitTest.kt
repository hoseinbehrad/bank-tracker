package com.example

import com.example.data.model.BankCardEntity
import com.example.data.model.TransactionEntity
import com.example.sms.BankSmsParser
import com.example.sms.SmsInboxScanner
import com.example.util.BackupManager
import com.example.util.CurrencyType
import com.example.util.CurrencyUtils
import com.example.util.PersianCalendarUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testUserExactMellatSmsParsing() {
    val sms = """
حساب1234567890
برداشت10,850,000
مانده98,765,432
05/05/05-12:57
    """.trimIndent()

    val parsed = BankSmsParser.parse(sms, sender = "20000")
    assertTrue(parsed.isValid)
    assertEquals("1234567890", parsed.accountNumber)
    assertEquals("EXPENSE", parsed.type)
    assertEquals(10850000L, parsed.amountRials)
    assertEquals(220929061L, parsed.balanceRials)
    assertEquals("05/05/05-12:57", parsed.dateString)
    assertEquals("بانک ملت", parsed.bankName)

    // Currency conversion test: 10,850,000 Rials = 1,085,000 Tomans
    val tomanFormatted = CurrencyUtils.formatAmount(
      parsed.amountRials,
      CurrencyType.TOMAN,
      usePersianDigits = false,
      includeUnit = false
    )
    assertEquals("1,085,000", tomanFormatted)

    val rialFormatted = CurrencyUtils.formatAmount(
      parsed.amountRials,
      CurrencyType.RIAL,
      usePersianDigits = false,
      includeUnit = false
    )
    assertEquals("10,850,000", rialFormatted)
  }

  @Test
  fun testPersianDigitsNormalizerAndFormatter() {
    val persianInput = "۱۰,۸۵۰,۰۰۰"
    val asciiParsed = CurrencyUtils.parseAmount(persianInput)
    assertEquals(10850000L, asciiParsed)

    val persianDisplay = CurrencyUtils.toPersianDigits("10,850,000")
    assertEquals("۱۰,۸۵۰,۰۰۰", persianDisplay)
  }

  @Test
  fun testDeduplicationLogic() {
    val existing = TransactionEntity(
      id = 1,
      accountNumber = "1234567890",
      bankName = "بانک ملت",
      type = "EXPENSE",
      amountRials = 10850000L,
      balanceRials = 220929061L,
      dateString = "05/05/05-12:57",
      rawSms = "حساب1234567890\nبرداشت10,850,000\nمانده98,765,432\n05/05/05-12:57",
      timestamp = 1000000L
    )

    // Exact duplicate candidate
    val isDup = SmsInboxScanner.isDuplicateTransaction(
      existing = existing,
      candidateAmount = 10850000L,
      candidateType = "EXPENSE",
      candidateDate = "05/05/05-12:57",
      candidateTimestamp = 1000000L,
      candidateAccount = "1234567890",
      candidateRawSms = existing.rawSms
    )
    assertTrue("Should be detected as duplicate", isDup)

    // Different amount -> Not duplicate
    val notDupAmount = SmsInboxScanner.isDuplicateTransaction(
      existing = existing,
      candidateAmount = 5000000L,
      candidateType = "EXPENSE",
      candidateDate = "05/05/05-12:57",
      candidateTimestamp = 1000000L,
      candidateAccount = "1234567890",
      candidateRawSms = ""
    )
    assertFalse("Different amount is not duplicate", notDupAmount)

    // Different date & time -> Not duplicate
    val notDupDate = SmsInboxScanner.isDuplicateTransaction(
      existing = existing,
      candidateAmount = 10850000L,
      candidateType = "EXPENSE",
      candidateDate = "05/05/06-14:20",
      candidateTimestamp = 999999999L,
      candidateAccount = "1234567890",
      candidateRawSms = "different sms"
    )
    assertFalse("Different date is not duplicate", notDupDate)
  }

  @Test
  fun testPersianMonthProgression() {
    assertEquals("شهریور", PersianCalendarUtils.getNextMonthName("مرداد"))
    assertEquals("مهر", PersianCalendarUtils.getNextMonthName("شهریور"))
    assertEquals("فروردین", PersianCalendarUtils.getNextMonthName("اسفند"))
  }

  @Test
  fun testBackupExportAndImport() {
    val cards = listOf(
      BankCardEntity(
        id = 1,
        accountNumber = "1234567890",
        bankName = "بانک ملت",
        cardTitle = "کارت حقوق",
        cardColorHex = 0xFFDC2626L,
        balanceRials = 220929061L
      )
    )

    val txList = listOf(
      TransactionEntity(
        id = 1,
        accountNumber = "1234567890",
        bankName = "بانک ملت",
        type = "EXPENSE",
        amountRials = 10850000L,
        balanceRials = 220929061L,
        category = "SHOPPING",
        note = "خرید سوپرمارکت",
        dateString = "05/05/05-12:57",
        monthSheet = "مرداد"
      )
    )

    val json = BackupManager.exportToJson(cards, txList, "مرداد")
    assertTrue(json.contains("1234567890"))
    assertTrue(json.contains("10850000"))

    val parsed = BackupManager.parseFromJson(json)
    assertEquals("مرداد", parsed.activeMonth)
    assertEquals(1, parsed.cards.size)
    assertEquals("بانک ملت", parsed.cards[0].bankName)
    assertEquals(220929061L, parsed.cards[0].balanceRials)
    assertEquals(1, parsed.transactions.size)
    assertEquals(10850000L, parsed.transactions[0].amountRials)
  }
}
